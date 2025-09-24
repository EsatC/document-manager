package com.documentmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Number is required")
    @Column(nullable = false)
    private String number;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FileAttachment fileAttachment;

    // OCR-related fields
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)

    @Lob
    @Column(name = "ocr_text", columnDefinition = "LONGTEXT")
    private String ocrText;

    @Column(name = "ocr_processed")
    private Boolean ocrProcessed = false;

    @Column(name = "ocr_processed_at")
    private LocalDateTime ocrProcessedAt;

    // Constructors
    public Document() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.ocrProcessed = false;
    }

    public Document(String title, String number, LocalDate date, String description, User user) {
        this();
        this.title = title;
        this.number = number;
        this.date = date;
        this.description = description;
        this.user = user;
    }

    // Getters and Setters
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FileAttachment getFileAttachment() {
        return fileAttachment;
    }

    public void setFileAttachment(FileAttachment fileAttachment) {
        this.fileAttachment = fileAttachment;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public Boolean getOcrProcessed() {
        return ocrProcessed;
    }

    public void setOcrProcessed(Boolean ocrProcessed) {
        this.ocrProcessed = ocrProcessed;
    }

    public LocalDateTime getOcrProcessedAt() {
        return ocrProcessedAt;
    }

    public void setOcrProcessedAt(LocalDateTime ocrProcessedAt) {
        this.ocrProcessedAt = ocrProcessedAt;
    }

    public void markOcrAsProcessed(String extractedText) {
        this.ocrText = extractedText;
        this.ocrProcessed = true;
        this.ocrProcessedAt = LocalDateTime.now();
    }

    public void resetOcrStatus() {
        this.ocrText = null;
        this.ocrProcessed = false;
        this.ocrProcessedAt = null;
    }
}