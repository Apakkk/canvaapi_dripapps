package com.dripapps.canva.dto;

/**
 * DTO representing a simplified Canva design for frontend display.
 * Contains only the fields needed for our MVP.
 */
public class DesignDto {

    /**
     * Unique identifier of the design in Canva
     */
    private String designId;

    /**
     * Human-readable title of the design
     */
    private String title;

    /**
     * URL to the design's thumbnail image
     * Note: Canva thumbnail URLs are temporary and may expire
     */
    private String thumbnailUrl;

    /**
     * Whether this design has been imported to our system
     */
    private boolean imported;

    /**
     * Local URL of the imported PNG (null if not imported)
     */
    private String localImageUrl;

    public DesignDto() {
    }

    public DesignDto(String designId, String title, String thumbnailUrl, boolean imported, String localImageUrl) {
        this.designId = designId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.imported = imported;
        this.localImageUrl = localImageUrl;
    }

    // Getters and Setters

    public String getDesignId() {
        return designId;
    }

    public void setDesignId(String designId) {
        this.designId = designId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getLocalImageUrl() {
        return localImageUrl;
    }

    public void setLocalImageUrl(String localImageUrl) {
        this.localImageUrl = localImageUrl;
    }
}
