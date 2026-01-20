package com.dripapps.canva.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Canva API integration.
 * Values are loaded from application.yml and environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "canva.api")
public class CanvaConfig {

    /**
     * Canva Connect API base URL
     */
    private String baseUrl;

    /**
     * OAuth authorization URL (where users are redirected to grant access)
     */
    private String authorizationUrl;

    /**
     * Token endpoint for exchanging auth code for tokens
     */
    private String tokenUrl;

    /**
     * Client ID from Canva Developer Portal
     */
    private String clientId;

    /**
     * Client Secret from Canva Developer Portal
     * IMPORTANT: Never expose this to the frontend!
     */
    private String clientSecret;

    /**
     * OAuth callback URI registered in Canva Developer Portal
     */
    private String redirectUri;

    /**
     * Space-separated OAuth scopes required for our MVP:
     * - design:meta:read: List user's designs
     * - design:content:read: Export designs as PNG
     */
    private String scopes;

    // Getters and Setters

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }
}
