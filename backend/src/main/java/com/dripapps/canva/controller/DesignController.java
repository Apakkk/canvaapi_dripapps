package com.dripapps.canva.controller;

import com.dripapps.canva.dto.DesignDto;
import com.dripapps.canva.dto.ImportResultDto;
import com.dripapps.canva.service.CanvaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Canva design operations.
 * 
 * ENDPOINTS:
 * - GET /api/designs - List user's Canva designs
 * - POST /api/designs/{designId}/import - Import design as PNG
 * 
 * All endpoints require authenticated Canva session.
 */
@RestController
@RequestMapping("/api/designs")
public class DesignController {

    private static final Logger log = LoggerFactory.getLogger(DesignController.class);

    private final CanvaApiService canvaApiService;

    public DesignController(CanvaApiService canvaApiService) {
        this.canvaApiService = canvaApiService;
    }

    /**
     * Lists all designs from the user's Canva account.
     * 
     * Returns simplified design objects with:
     * - designId
     * - title
     * - thumbnailUrl
     * - imported status
     * - localImageUrl (if imported)
     */
    @GetMapping
    public ResponseEntity<List<DesignDto>> listDesigns() {
        log.info("Listing designs from Canva");

        try {
            List<DesignDto> designs = canvaApiService.listDesigns();
            log.info("Returning {} designs", designs.size());
            return ResponseEntity.ok(designs);
        } catch (Exception e) {
            log.error("Failed to list designs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Imports a specific design as PNG.
     * 
     * PROCESS:
     * 1. Triggers export job on Canva
     * 2. Polls until export completes
     * 3. Downloads PNG file
     * 4. Saves to local filesystem
     * 5. Returns local URL for viewing
     * 
     * This is a blocking operation that may take several seconds.
     */
    @PostMapping("/{designId}/import")
    public ResponseEntity<ImportResultDto> importDesign(@PathVariable String designId) {
        log.info("Importing design: {}", designId);

        try {
            String imageUrl = canvaApiService.importDesignAsPng(designId);
            log.info("Design imported successfully: {} -> {}", designId, imageUrl);

            return ResponseEntity.ok(ImportResultDto.success(designId, imageUrl));

        } catch (Exception e) {
            log.error("Failed to import design: {}", designId, e);
            return ResponseEntity.ok(ImportResultDto.failure(designId, e.getMessage()));
        }
    }

    /**
     * Gets a specific design's details.
     */
    @GetMapping("/{designId}")
    public ResponseEntity<DesignDto> getDesign(@PathVariable String designId) {
        log.info("Getting design details: {}", designId);

        try {
            // For MVP, we get all designs and filter
            // Production should call Canva's GET /designs/{id} endpoint
            List<DesignDto> designs = canvaApiService.listDesigns();

            return designs.stream()
                    .filter(d -> d.getDesignId().equals(designId))
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Failed to get design: {}", designId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
