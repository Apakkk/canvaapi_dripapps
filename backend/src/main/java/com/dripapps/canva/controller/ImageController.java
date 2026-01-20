package com.dripapps.canva.controller;

import com.dripapps.canva.service.CanvaApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

/**
 * Controller for serving imported PNG images.
 * 
 * Serves images from the local upload directory.
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final CanvaApiService canvaApiService;

    public ImageController(CanvaApiService canvaApiService) {
        this.canvaApiService = canvaApiService;
    }

    /**
     * Serves an imported PNG image by design ID.
     * 
     * URL format: /api/images/{designId}.png
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        log.info("Requested image: {}", filename);

        // Extract design ID from filename (remove .png extension)
        String designId = filename.replace(".png", "");

        Path imagePath = canvaApiService.getLocalImagePath(designId);

        if (imagePath == null || !imagePath.toFile().exists()) {
            log.warn("Image not found: {}", designId);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath.toFile());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
