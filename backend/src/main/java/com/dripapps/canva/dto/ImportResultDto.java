package com.dripapps.canva.dto;

/**
 * DTO for import operation response.
 */
public class ImportResultDto {

    /**
     * Whether the import was successful
     */
    private boolean success;

    /**
     * Design ID that was imported
     */
    private String designId;

    /**
     * URL to access the imported PNG image
     */
    private String imageUrl;

    /**
     * Error message if import failed
     */
    private String error;

    public ImportResultDto() {
    }

    public ImportResultDto(boolean success, String designId, String imageUrl, String error) {
        this.success = success;
        this.designId = designId;
        this.imageUrl = imageUrl;
        this.error = error;
    }

    public static ImportResultDto success(String designId, String imageUrl) {
        return new ImportResultDto(true, designId, imageUrl, null);
    }

    public static ImportResultDto failure(String designId, String error) {
        return new ImportResultDto(false, designId, null, error);
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDesignId() {
        return designId;
    }

    public void setDesignId(String designId) {
        this.designId = designId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
