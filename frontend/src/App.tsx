/**
 * Canva Integration MVP - Main Application
 * 
 * This is a minimal proof-of-concept demonstrating:
 * 1. Canva OAuth authentication
 * 2. Listing user's Canva designs
 * 3. Importing designs as PNG images
 * 4. Displaying imported images
 * 
 * IMPORTANT: All Canva API calls go through our backend.
 * The frontend NEVER handles OAuth tokens directly.
 */

import { useState, useEffect, useCallback } from 'react';
import {
  checkAuthStatus,
  getLoginUrl,
  logout,
  listDesigns,
  importDesign,
  getImageUrl
} from './api';
import type { Design, ImportResult } from './types';
import './App.css';

function App() {
  // Authentication state
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authUrl, setAuthUrl] = useState<string>('');

  // Design state
  const [designs, setDesigns] = useState<Design[]>([]);
  const [selectedDesign, setSelectedDesign] = useState<Design | null>(null);

  // UI state
  const [loading, setLoading] = useState(true);
  const [importing, setImporting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  /**
   * Check auth status on mount and after OAuth redirect
   */
  const checkAuth = useCallback(async () => {
    try {
      setLoading(true);
      const status = await checkAuthStatus();
      setIsAuthenticated(status.authenticated);

      if (!status.authenticated && status.authUrl) {
        setAuthUrl(status.authUrl);
      }

      // If authenticated, fetch designs
      if (status.authenticated) {
        await fetchDesigns();
      }
    } catch (err) {
      console.error('Auth check failed:', err);
      setError('Failed to check authentication status');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Handle OAuth redirect callback
   */
  useEffect(() => {
    // Check for auth callback in URL
    const params = new URLSearchParams(window.location.search);
    const authResult = params.get('auth');

    if (authResult === 'success') {
      console.log('OAuth successful, checking auth...');
      // Clear URL params
      window.history.replaceState({}, document.title, window.location.pathname);
    } else if (authResult === 'failed') {
      setError('Canva authentication failed');
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    // Always check auth status on mount
    checkAuth();
  }, [checkAuth]);

  /**
   * Fetch user's Canva designs
   */
  const fetchDesigns = async () => {
    try {
      const designList = await listDesigns();
      setDesigns(designList);
      console.log(`Fetched ${designList.length} designs`);
    } catch (err) {
      console.error('Failed to fetch designs:', err);
      setError('Failed to load designs from Canva');
    }
  };

  /**
   * Handle login button click - redirect to Canva OAuth
   */
  const handleLogin = async () => {
    try {
      // Get fresh auth URL
      const url = authUrl || await getLoginUrl();
      console.log('Redirecting to Canva OAuth...');
      // Redirect to Canva login
      window.location.href = url;
    } catch (err) {
      console.error('Login failed:', err);
      setError('Failed to initiate login');
    }
  };

  /**
   * Handle logout
   */
  const handleLogout = async () => {
    try {
      await logout();
      setIsAuthenticated(false);
      setDesigns([]);
      setSelectedDesign(null);
      console.log('Logged out');
    } catch (err) {
      console.error('Logout failed:', err);
    }
  };

  /**
   * Handle design import
   */
  const handleImport = async (design: Design) => {
    if (importing) return; // Prevent multiple imports

    try {
      setImporting(design.designId);
      setError(null);
      console.log(`Importing design: ${design.designId}`);

      const result: ImportResult = await importDesign(design.designId);

      if (result.success && result.imageUrl) {
        console.log(`Import successful: ${result.imageUrl}`);

        // Update design in list
        setDesigns(prev => prev.map(d =>
          d.designId === design.designId
            ? { ...d, imported: true, localImageUrl: result.imageUrl }
            : d
        ));

        // Update selected design if it's the one we imported
        if (selectedDesign?.designId === design.designId) {
          setSelectedDesign({ ...design, imported: true, localImageUrl: result.imageUrl });
        }
      } else {
        console.error('Import failed:', result.error);
        setError(result.error || 'Import failed');
      }
    } catch (err) {
      console.error('Import error:', err);
      setError('Failed to import design');
    } finally {
      setImporting(null);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="app">
        <div className="loading">Loading...</div>
      </div>
    );
  }

  // Not authenticated - show login
  if (!isAuthenticated) {
    return (
      <div className="app">
        <header className="header">
          <h1>Canva Design Importer</h1>
          <p>Import your Canva designs as PNG images</p>
        </header>

        <main className="main">
          <div className="login-container">
            <p>Connect your Canva account to get started</p>
            <button className="btn btn-primary" onClick={handleLogin}>
              Login with Canva
            </button>
            {error && <p className="error">{error}</p>}
          </div>
        </main>
      </div>
    );
  }

  // Authenticated - show designs
  return (
    <div className="app">
      <header className="header">
        <h1>Canva Design Importer</h1>
        <button className="btn btn-secondary" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <main className="main">
        {/* Design list */}
        <section className="design-list">
          <h2>Your Designs ({designs.length})</h2>
          <button className="btn btn-small" onClick={fetchDesigns}>
            Refresh
          </button>

          {designs.length === 0 ? (
            <p className="empty">No designs found in your Canva account</p>
          ) : (
            <ul className="designs">
              {designs.map(design => (
                <li
                  key={design.designId}
                  className={`design-item ${selectedDesign?.designId === design.designId ? 'selected' : ''}`}
                  onClick={() => setSelectedDesign(design)}
                >
                  {design.thumbnailUrl && (
                    <img
                      src={design.thumbnailUrl}
                      alt={design.title}
                      className="thumbnail"
                    />
                  )}
                  <div className="design-info">
                    <span className="design-title">{design.title}</span>
                    {design.imported && (
                      <span className="badge imported">Imported</span>
                    )}
                  </div>
                  <button
                    className="btn btn-small"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleImport(design);
                    }}
                    disabled={importing === design.designId}
                  >
                    {importing === design.designId ? 'Importing...' :
                      design.imported ? 'Re-import' : 'Import'}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Selected design preview */}
        <section className="preview">
          <h2>Preview</h2>
          {selectedDesign ? (
            <div className="preview-content">
              <h3>{selectedDesign.title}</h3>
              <p className="design-id">ID: {selectedDesign.designId}</p>

              {selectedDesign.imported && selectedDesign.localImageUrl ? (
                <div className="imported-image">
                  <h4>Imported PNG</h4>
                  <img
                    src={getImageUrl(selectedDesign.localImageUrl)}
                    alt={selectedDesign.title}
                    className="full-image"
                  />
                </div>
              ) : selectedDesign.thumbnailUrl ? (
                <div className="thumbnail-preview">
                  <h4>Thumbnail (from Canva)</h4>
                  <img
                    src={selectedDesign.thumbnailUrl}
                    alt={selectedDesign.title}
                    className="full-image"
                  />
                  <p className="note">
                    Click "Import" to download as PNG
                  </p>
                </div>
              ) : (
                <p>No preview available</p>
              )}
            </div>
          ) : (
            <p className="empty">Select a design to preview</p>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;
