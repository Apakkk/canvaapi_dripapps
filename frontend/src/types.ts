/**
 * TypeScript interfaces for the Canva Integration MVP
 */

/**
 * Authentication status from backend
 */
export interface AuthStatus {
  authenticated: boolean;
  authUrl?: string;
}

/**
 * Simplified Canva design representation
 */
export interface Design {
  designId: string;
  title: string;
  thumbnailUrl?: string;
  imported: boolean;
  localImageUrl?: string;
}

/**
 * Result of import operation
 */
export interface ImportResult {
  success: boolean;
  designId: string;
  imageUrl?: string;
  error?: string;
}
