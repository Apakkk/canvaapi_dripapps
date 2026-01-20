package com.dripapps.canva.service;

import com.dripapps.canva.config.CanvaConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service handling OAuth 2.0 authentication with Canva.
 * 
 * CANVA OAUTH FLOW NOTES:
 * - Canva requires PKCE (Proof Key for Code Exchange) with S256 method
 * - Access tokens expire and must be refreshed
 * - Tokens are stored in-memory (production should use secure storage)
 * - Only backend handles tokens; frontend never sees them directly
 */
@Service
public class CanvaAuthService {

    private static final Logger log = LoggerFactory.getLogger(CanvaAuthService.class);

    private final CanvaConfig canvaConfig;
    private final WebClient webClient;

    // In-memory token storage (MVP only - use secure storage in production)
    // Key: state parameter, Value: access token
    private final ConcurrentHashMap<String, String> accessTokens = new ConcurrentHashMap<>();

    // PKCE code verifiers (temporary storage during auth flow)
    private final ConcurrentHashMap<String, String> codeVerifiers = new ConcurrentHashMap<>();

    // Current active session (MVP simplification - single user)
    private String currentSessionId = null;

    public CanvaAuthService(CanvaConfig canvaConfig, WebClient webClient) {
        this.canvaConfig = canvaConfig;
        this.webClient = webClient;
    }

    /**
     * Generates the OAuth authorization URL for Canva login.
     * Includes PKCE challenge as required by Canva API.
     */
    public String generateAuthorizationUrl() {
        try {
            // Generate state for CSRF protection
            String state = generateRandomString(32);

            // Generate PKCE code verifier and challenge
            String codeVerifier = generateRandomString(64);
            String codeChallenge = generateCodeChallenge(codeVerifier);

            // Store code verifier for later token exchange
            codeVerifiers.put(state, codeVerifier);

            // Build authorization URL with all required parameters
            String authUrl = String.format(
                    "%s?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
                    canvaConfig.getAuthorizationUrl(),
                    URLEncoder.encode(canvaConfig.getClientId(), StandardCharsets.UTF_8),
                    URLEncoder.encode(canvaConfig.getRedirectUri(), StandardCharsets.UTF_8),
                    URLEncoder.encode(canvaConfig.getScopes(), StandardCharsets.UTF_8),
                    state,
                    codeChallenge);

            log.info("Generated Canva authorization URL with state: {}", state);
            return authUrl;

        } catch (Exception e) {
            log.error("Failed to generate authorization URL", e);
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    /**
     * Exchanges authorization code for access token.
     * Called when Canva redirects back to our callback URL.
     */
    public boolean exchangeCodeForToken(String code, String state) {
        try {
            String codeVerifier = codeVerifiers.remove(state);
            if (codeVerifier == null) {
                log.error("No code verifier found for state: {}", state);
                return false;
            }

            log.info("Exchanging authorization code for access token...");

            // Make token request to Canva
            JsonNode response = webClient.post()
                    .uri(canvaConfig.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("code", code)
                            .with("redirect_uri", canvaConfig.getRedirectUri())
                            .with("client_id", canvaConfig.getClientId())
                            .with("client_secret", canvaConfig.getClientSecret())
                            .with("code_verifier", codeVerifier))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("access_token")) {
                String accessToken = response.get("access_token").asText();

                // Store token and set as current session
                accessTokens.put(state, accessToken);
                currentSessionId = state;

                log.info("Successfully obtained access token for session: {}", state);

                // Log refresh token info (not used in MVP but important for production)
                if (response.has("refresh_token")) {
                    log.info("Refresh token also received (not stored in MVP)");
                }
                if (response.has("expires_in")) {
                    log.info("Token expires in: {} seconds", response.get("expires_in").asInt());
                }

                return true;
            } else {
                log.error("Token response missing access_token: {}", response);
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            return false;
        }
    }

    /**
     * Checks if we have a valid access token for the current session.
     */
    public boolean isAuthenticated() {
        return currentSessionId != null && accessTokens.containsKey(currentSessionId);
    }

    /**
     * Gets the current access token.
     * Returns null if not authenticated.
     */
    public String getAccessToken() {
        if (currentSessionId == null) {
            return null;
        }
        return accessTokens.get(currentSessionId);
    }

    /**
     * Clears the current session (logout).
     */
    public void clearSession() {
        if (currentSessionId != null) {
            accessTokens.remove(currentSessionId);
            currentSessionId = null;
        }
        log.info("Session cleared");
    }

    /**
     * Generates a cryptographically secure random string.
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    /**
     * Generates PKCE code challenge from code verifier using S256 method.
     * Required by Canva OAuth implementation.
     */
    private String generateCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
