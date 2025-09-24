package com.documentmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class DocumentRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Number is required")
    private String number;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;

    public DocumentRequest() {}

    public DocumentRequest(String title, String number, LocalDate date, String description) {
        this.title = title;
        this.number = number;
        this.date = date;
        this.description = description;
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
}