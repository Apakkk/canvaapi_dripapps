package com.dripapps.canva.controller;

import com.dripapps.canva.dto.AuthStatusDto;
import com.dripapps.canva.service.CanvaAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller handling Canva OAuth authentication flow.
 * 
 * AUTHENTICATION FLOW:
 * 1. Frontend calls GET /api/canva/auth/status - gets auth URL if not
 * authenticated
 * 2. User is redirected to Canva for login (in a popup or new tab)
 * 3. Canva redirects back to GET /api/canva/callback with auth code
 * 4. Backend exchanges code for token (securely stored)
 * 5. User is redirected back to frontend with success status
 * 
 * SECURITY NOTES:
 * - Tokens are NEVER exposed to the frontend
 * - PKCE is used for additional security
 * - State parameter prevents CSRF attacks
 */
@RestController
@RequestMapping("/api/canva")
public class CanvaAuthController {

    private static final Logger log = LoggerFactory.getLogger(CanvaAuthController.class);

    private final CanvaAuthService authService;

    public CanvaAuthController(CanvaAuthService authService) {
        this.authService = authService;
    }

    /**
     * Check authentication status and get auth URL if not authenticated.
     * Frontend polls this to determine if user needs to login.
     */
    @GetMapping("/auth/status")
    public ResponseEntity<AuthStatusDto> getAuthStatus() {
        boolean authenticated = authService.isAuthenticated();

        AuthStatusDto status = new AuthStatusDto();
        status.setAuthenticated(authenticated);

        if (!authenticated) {
            // Generate fresh authorization URL with PKCE
            status.setAuthUrl(authService.generateAuthorizationUrl());
        }

        log.info("Auth status check: authenticated={}", authenticated);
        return ResponseEntity.ok(status);
    }

    /**
     * OAuth callback endpoint - Canva redirects here after user authorizes.
     * 
     * Query Parameters:
     * - code: Authorization code to exchange for tokens
     * - state: State parameter for CSRF protection
     * 
     * After successful token exchange, redirects to frontend.
     */
    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {

        log.info("OAuth callback received with state: {}", state);

        boolean success = authService.exchangeCodeForToken(code, state);

        if (success) {
            log.info("OAuth flow completed successfully");
            // Redirect to frontend with success indicator
            response.sendRedirect("http://127.0.0.1:5173?auth=success");
        } else {
            log.error("OAuth flow failed");
            // Redirect to frontend with error indicator
            response.sendRedirect("http://127.0.0.1:5173?auth=failed");
        }
    }

    /**
     * Logout endpoint - clears the current session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.clearSession();
        log.info("User logged out");
        return ResponseEntity.ok().build();
    }

    /**
     * Start login - returns the authorization URL.
     * Frontend should open this URL in a popup or redirect.
     */
    @GetMapping("/auth/login")
    public ResponseEntity<AuthStatusDto> initiateLogin() {
        String authUrl = authService.generateAuthorizationUrl();

        AuthStatusDto authStatus = new AuthStatusDto();
        authStatus.setAuthenticated(false);
        authStatus.setAuthUrl(authUrl);

        log.info("Login initiated, auth URL generated");
        return ResponseEntity.ok(authStatus);
    }
}
