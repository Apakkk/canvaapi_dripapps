package com.dripapps.canva.dto;

/**
 * DTO for authentication status response.
 */
public class AuthStatusDto {

    /**
     * Whether the user is authenticated with Canva
     */
    private boolean authenticated;

    /**
     * OAuth authorization URL to redirect user for login
     */
    private String authUrl;

    public AuthStatusDto() {
    }

    public AuthStatusDto(boolean authenticated, String authUrl) {
        this.authenticated = authenticated;
        this.authUrl = authUrl;
    }

    // Getters and Setters

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }
}
