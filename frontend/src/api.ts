/**
 * API client for communicating with the backend.
 * All Canva API interactions go through our backend - 
 * the frontend NEVER handles OAuth tokens directly.
 */

import type { AuthStatus, Design, ImportResult } from './types';

const API_BASE = 'http://127.0.0.1:8080/api';

/**
 * Check if user is authenticated with Canva.
 * Returns auth URL if not authenticated.
 */
export async function checkAuthStatus(): Promise<AuthStatus> {
    const response = await fetch(`${API_BASE}/canva/auth/status`, {
        credentials: 'include', // Include cookies for session
    });

    if (!response.ok) {
        throw new Error('Failed to check auth status');
    }

    return response.json();
}

/**
 * Get the login URL to redirect user to Canva OAuth.
 */
export async function getLoginUrl(): Promise<string> {
    const response = await fetch(`${API_BASE}/canva/auth/login`, {
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error('Failed to get login URL');
    }

    const data: AuthStatus = await response.json();
    return data.authUrl || '';
}

/**
 * Logout from Canva (clear backend session).
 */
export async function logout(): Promise<void> {
    await fetch(`${API_BASE}/canva/logout`, {
        method: 'POST',
        credentials: 'include',
    });
}

/**
 * Fetch list of user's Canva designs.
 * Requires authenticated session.
 */
export async function listDesigns(): Promise<Design[]> {
    const response = await fetch(`${API_BASE}/designs`, {
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error('Failed to fetch designs');
    }

    return response.json();
}

/**
 * Import a design as PNG.
 * This is an async operation that may take several seconds.
 * 
 * @param designId - The Canva design ID to import
 */
export async function importDesign(designId: string): Promise<ImportResult> {
    const response = await fetch(`${API_BASE}/designs/${designId}/import`, {
        method: 'POST',
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error('Failed to import design');
    }

    return response.json();
}

/**
 * Get the full URL for a local image.
 */
export function getImageUrl(path: string): string {
    if (path.startsWith('http')) {
        return path;
    }
    return `http://127.0.0.1:8080${path}`;
}
