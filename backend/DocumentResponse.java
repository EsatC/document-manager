package com.documentmanager.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String title;
    private String number;
    private LocalDate date;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasFile;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    private String ocrText;
    private boolean ocrProcessed;
    private LocalDateTime ocrProcessedAt;
    private boolean ocrSupported;

    public DocumentResponse() {}

    public DocumentResponse(Long id, String title, String number, LocalDate date, String description,
                            LocalDateTime createdAt, LocalDateTime updatedAt, boolean hasFile,
                            String originalFilename, String contentType, Long fileSize, LocalDateTime uploadedAt,
                            String ocrText, boolean ocrProcessed, LocalDateTime ocrProcessedAt, boolean ocrSupported) {
        this.id = id;
        this.title = title;
        this.number = number;
        this.date = date;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.hasFile = hasFile;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.ocrText = ocrText;
        this.ocrProcessed = ocrProcessed;
        this.ocrProcessedAt = ocrProcessedAt;
        this.ocrSupported = ocrSupported;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public boolean isOcrProcessed() {
        return ocrProcessed;
    }

    public void setOcrProcessed(boolean ocrProcessed) {
        this.ocrProcessed = ocrProcessed;
    }

    public LocalDateTime getOcrProcessedAt() {
        return ocrProcessedAt;
    }

    public void setOcrProcessedAt(LocalDateTime ocrProcessedAt) {
        this.ocrProcessedAt = ocrProcessedAt;
    }

    public boolean isOcrSupported() {
        return ocrSupported;
    }

    public void setOcrSupported(boolean ocrSupported) {
        this.ocrSupported = ocrSupported;
    }
}