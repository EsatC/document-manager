package com.documentmanager.service;

import com.documentmanager.entity.Document;
import com.documentmanager.entity.FileAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Store a file and create a FileAttachment entity
     */
    FileAttachment storeFile(MultipartFile file, Document document) throws IOException;

    /**
     * Load a file as a Resource for download
     */
    Resource loadFileAsResource(FileAttachment fileAttachment) throws IOException;

    /**
     * Delete a file from storage
     */
    void deleteFile(FileAttachment fileAttachment) throws IOException;

    /**
     * Get the file path for OCR processing
     */
    Path getFilePath(FileAttachment fileAttachment) throws IOException;

    /**
     * Initialize storage location
     */
    void init() throws IOException;
}