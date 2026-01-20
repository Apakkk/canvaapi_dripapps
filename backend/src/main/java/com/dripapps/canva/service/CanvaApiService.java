package com.dripapps.canva.service;

import com.dripapps.canva.config.CanvaConfig;
import com.dripapps.canva.dto.DesignDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with Canva Connect API endpoints.
 * 
 * CANVA API LIMITATIONS:
 * - Rate limits apply to all endpoints (handle 429 responses in production)
 * - Thumbnail URLs are temporary and may expire
 * - Export is asynchronous - must poll for job completion
 * - Multi-page designs export as ZIP (we handle first page only in MVP)
 * - Export URLs are valid for only 24 hours
 * - Requires authenticated user with proper scopes
 */
@Service
public class CanvaApiService {

    private static final Logger log = LoggerFactory.getLogger(CanvaApiService.class);

    private final CanvaConfig canvaConfig;
    private final CanvaAuthService authService;
    private final WebClient webClient;

    @Value("${upload.directory}")
    private String uploadDirectory;

    // Track which designs have been imported
    private final Set<String> importedDesigns = ConcurrentHashMap.newKeySet();

    // Store local paths of imported images
    private final ConcurrentHashMap<String, String> localImagePaths = new ConcurrentHashMap<>();

    public CanvaApiService(CanvaConfig canvaConfig, CanvaAuthService authService, WebClient webClient) {
        this.canvaConfig = canvaConfig;
        this.authService = authService;
        this.webClient = webClient;
    }

    /**
     * Fetches the user's Canva designs.
     * 
     * CANVA API: GET /v1/designs
     * Required scope: design:meta:read
     * 
     * Note: This returns paginated results. MVP fetches first page only.
     * Production should handle pagination using 'continuation' token.
     */
    public List<DesignDto> listDesigns() {
        String accessToken = authService.getAccessToken();
        if (accessToken == null) {
            log.error("Cannot list designs: not authenticated");
            throw new RuntimeException("Not authenticated with Canva");
        }

        try {
            log.info("Fetching designs from Canva API...");

            JsonNode response = webClient.get()
                    .uri(canvaConfig.getBaseUrl() + "/designs")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<DesignDto> designs = new ArrayList<>();

            if (response != null && response.has("items")) {
                JsonNode items = response.get("items");
                for (JsonNode item : items) {
                    DesignDto dto = new DesignDto();

                    // Extract design ID
                    dto.setDesignId(item.get("id").asText());

                    // Extract title (may not exist for all designs)
                    dto.setTitle(item.has("title") ? item.get("title").asText() : "Untitled Design");

                    // Extract thumbnail URL
                    // LIMITATION: Canva may provide thumbnail in different formats
                    if (item.has("thumbnail") && item.get("thumbnail").has("url")) {
                        dto.setThumbnailUrl(item.get("thumbnail").get("url").asText());
                    }

                    // Check if already imported
                    dto.setImported(importedDesigns.contains(dto.getDesignId()));
                    if (dto.isImported()) {
                        dto.setLocalImageUrl("/api/images/" + dto.getDesignId() + ".png");
                    }

                    designs.add(dto);
                }
            }

            log.info("Retrieved {} designs from Canva", designs.size());

            // Log if there are more pages (production should handle this)
            if (response != null && response.has("continuation")) {
                log.warn("More designs available - MVP only fetches first page");
            }

            return designs;

        } catch (Exception e) {
            log.error("Failed to fetch designs from Canva", e);
            throw new RuntimeException("Failed to fetch designs: " + e.getMessage(), e);
        }
    }

    /**
     * Imports a design as PNG by triggering export and downloading the result.
     * 
     * CANVA EXPORT FLOW:
     * 1. POST /v1/exports - Start export job
     * 2. GET /v1/exports/{jobId} - Poll until status is "completed"
     * 3. Download URL from response
     * 4. Save locally
     * 
     * LIMITATIONS:
     * - Export is async, may take several seconds
     * - Multi-page designs return ZIP file
     * - PNG export options (transparency, compression) may require Canva Pro
     */
    public String importDesignAsPng(String designId) {
        String accessToken = authService.getAccessToken();
        if (accessToken == null) {
            log.error("Cannot import design: not authenticated");
            throw new RuntimeException("Not authenticated with Canva");
        }

        try {
            log.info("Starting PNG export for design: {}", designId);

            // Step 1: Create export job
            String exportJobId = createExportJob(accessToken, designId);
            log.info("Export job created: {}", exportJobId);

            // Step 2: Poll for completion
            String downloadUrl = pollExportJob(accessToken, exportJobId);
            log.info("Export completed, download URL obtained");

            // Step 3: Download PNG
            Path localPath = downloadPng(downloadUrl, designId);
            log.info("PNG saved to: {}", localPath);

            // Step 4: Mark as imported
            importedDesigns.add(designId);
            String imageUrl = "/api/images/" + designId + ".png";
            localImagePaths.put(designId, localPath.toString());

            return imageUrl;

        } catch (Exception e) {
            log.error("Failed to import design {} as PNG", designId, e);
            throw new RuntimeException("Failed to import design: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an export job for a design.
     * 
     * CANVA API: POST /v1/exports
     * Required scope: design:content:read
     */
    private String createExportJob(String accessToken, String designId) {
        // Build export request body
        String requestBody = String.format("""
                {
                    "design_id": "%s",
                    "format": {
                        "type": "png"
                    }
                }
                """, designId);

        JsonNode response = webClient.post()
                .uri(canvaConfig.getBaseUrl() + "/exports")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null && response.has("job") && response.get("job").has("id")) {
            return response.get("job").get("id").asText();
        }

        throw new RuntimeException("Failed to create export job: " + response);
    }

    /**
     * Polls the export job until completion.
     * 
     * CANVA API: GET /v1/exports/{exportId}
     * 
     * Response format:
     * - status: "in_progress", "success", or "failed"
     * - result.downloadUrls: array of download URLs (when success)
     * - error: { code, message } (when failed)
     * 
     * LIMITATION: We poll with simple retry logic.
     * Production should implement exponential backoff.
     */
    private String pollExportJob(String accessToken, String jobId) throws InterruptedException {
        int maxAttempts = 60; // Max 60 attempts (60 seconds) - exports can take time
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;

            JsonNode response = webClient.get()
                    .uri(canvaConfig.getBaseUrl() + "/exports/" + jobId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            log.info("Export poll response (attempt {}): {}", attempt, response);

            if (response != null && response.has("job")) {
                JsonNode job = response.get("job");
                String status = job.has("status") ? job.get("status").asText() : "unknown";

                log.info("Export job status: {} (attempt {})", status, attempt);

                // Check for success (Canva uses "success" not "completed")
                if ("success".equals(status)) {
                    // Get download URL from result.downloadUrls
                    if (job.has("result") && job.get("result").has("downloadUrls")) {
                        JsonNode urls = job.get("result").get("downloadUrls");
                        if (urls.isArray() && urls.size() > 0) {
                            String downloadUrl = urls.get(0).asText();
                            log.info("Export download URL obtained: {}", downloadUrl);
                            return downloadUrl;
                        }
                    }
                    // Fallback: check for urls array directly
                    if (job.has("urls") && job.get("urls").isArray() && job.get("urls").size() > 0) {
                        return job.get("urls").get(0).asText();
                    }
                    throw new RuntimeException("Export completed but no download URL found. Response: " + response);
                }

                if ("failed".equals(status)) {
                    String error = "Unknown error";
                    if (job.has("error")) {
                        JsonNode errorNode = job.get("error");
                        if (errorNode.has("message")) {
                            error = errorNode.get("message").asText();
                        } else {
                            error = errorNode.toString();
                        }
                    }
                    throw new RuntimeException("Export failed: " + error);
                }

                // Still in progress, wait and retry
                Thread.sleep(1000);
            } else {
                log.warn("Invalid export job response: {}", response);
                Thread.sleep(1000);
            }
        }

        throw new RuntimeException("Export job timed out after " + maxAttempts + " attempts");
    }

    /**
     * Downloads the PNG from Canva's temporary URL and saves it locally.
     * Note: Canva URLs may be double-encoded, so we decode them first.
     */
    private Path downloadPng(String downloadUrl, String designId) throws IOException {
        // Ensure upload directory exists
        Path uploadDir = Paths.get(uploadDirectory);
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(designId + ".png");

        // Decode URL if it's double-encoded
        String decodedUrl = downloadUrl;
        try {
            // Check if URL is double-encoded (contains %25 which is encoded %)
            if (downloadUrl.contains("%25")) {
                decodedUrl = java.net.URLDecoder.decode(downloadUrl, java.nio.charset.StandardCharsets.UTF_8);
                log.info("Decoded URL from: {} to: {}", downloadUrl.substring(0, Math.min(100, downloadUrl.length())),
                        decodedUrl.substring(0, Math.min(100, decodedUrl.length())));
            }
        } catch (Exception e) {
            log.warn("Failed to decode URL, using original: {}", e.getMessage());
        }

        log.info("Downloading PNG from: {}", decodedUrl.substring(0, Math.min(100, decodedUrl.length())) + "...");

        // Download using WebClient with proper URI handling
        try {
            DataBuffer buffer = webClient.get()
                    .uri(java.net.URI.create(decodedUrl))
                    .retrieve()
                    .bodyToMono(DataBuffer.class)
                    .block();

            if (buffer != null) {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);

                Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("Downloaded PNG: {} bytes to {}", bytes.length, filePath);
            } else {
                throw new IOException("Failed to download PNG - empty response");
            }
        } catch (Exception e) {
            log.error("WebClient download failed, trying with HttpURLConnection: {}", e.getMessage());

            // Fallback: use HttpURLConnection for more reliable download
            java.net.URL url = java.net.URI.create(decodedUrl).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.InputStream in = conn.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    log.info("Downloaded PNG via HttpURLConnection: {} bytes to {}", bytes.length, filePath);
                }
            } else {
                throw new IOException("HTTP " + responseCode + " from download URL");
            }
        }

        return filePath;
    }

    /**
     * Gets the local file path for an imported design.
     */
    public Path getLocalImagePath(String designId) {
        String pathStr = localImagePaths.get(designId);
        if (pathStr != null) {
            return Paths.get(pathStr);
        }
        // Fallback: check if file exists on disk
        Path path = Paths.get(uploadDirectory, designId + ".png");
        if (Files.exists(path)) {
            return path;
        }
        return null;
    }

    /**
     * Checks if a design has been imported.
     */
    public boolean isDesignImported(String designId) {
        return importedDesigns.contains(designId);
    }
}
