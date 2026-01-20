package com.dripapps.canva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Canva Integration MVP application.
 * 
 * This application demonstrates the Canva → PNG → Our System flow:
 * 1. User authenticates with Canva via OAuth 2.0
 * 2. User can list their existing Canva designs
 * 3. User can import any design as a flattened PNG image
 * 4. Imported PNGs are stored locally and can be viewed
 * 
 * IMPORTANT CANVA API LIMITATIONS:
 * - All Canva API interactions require valid OAuth tokens
 * - Exports are asynchronous - we must poll for completion
 * - Multi-page designs export as ZIP files with individual PNGs
 * - Export URLs are valid for only 24 hours
 * - Rate limits apply to all API endpoints
 */
@SpringBootApplication
public class CanvaIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvaIntegrationApplication.class, args);
    }
}
