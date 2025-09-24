// document_controller.java
package com.documentmanager.controller;

import com.documentmanager.dto.DocumentRequest;
import com.documentmanager.dto.DocumentResponse;
import com.documentmanager.entity.Document;
import com.documentmanager.entity.User;
import com.documentmanager.service.DocumentService;
import com.documentmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "http://localhost:3000")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getAllDocuments(
            Authentication authentication,
            @RequestParam(defaultValue = "") String search,
            Pageable pageable) {

        User user = userService.findByUsername(authentication.getName());
        Page<DocumentResponse> documents = documentService.getDocumentsByUser(user, search, pageable);
        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Specify consumed media type
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestPart("document") @Valid DocumentRequest documentRequest, // Use @RequestPart for JSON part
            @RequestPart(value = "file", required = false) MultipartFile file, // Use @RequestPart for file
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            DocumentResponse createdDocument = documentService.createDocument(documentRequest, file, user, true);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDocument);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Specify consumed media type
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestPart("document") @Valid DocumentRequest documentRequest, // Use @RequestPart for JSON part
            @RequestPart(value = "file", required = false) MultipartFile file, // Use @RequestPart for file
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            DocumentResponse updatedDocument = documentService.updateDocument(id, documentRequest, file, user,true);
            return ResponseEntity.ok(updatedDocument);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }

    // The existing uploadFile endpoint can remain for standalone file uploads,
    // but the new create/update methods will handle files during initial creation/update.
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file, // Use @RequestParam for simple file upload
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            DocumentResponse document = documentService.uploadFile(id, file, user,true);
            return ResponseEntity.ok(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(authentication.getName());
            Resource resource = documentService.downloadFile(id, user);

            Document document = documentService.getDocumentEntityById(id, user);
            String filename = document.getFileAttachment().getOriginalFilename();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getFileAttachment().getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/file")
    public ResponseEntity<DocumentResponse> deleteFile(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(authentication.getName());
            DocumentResponse document = documentService.deleteFile(id, user);
            return ResponseEntity.ok(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/{id}/ocr/process")
    public ResponseEntity<DocumentResponse> processOcr(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            DocumentResponse document = documentService.processOcrForDocument(id, user);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/ocr/text")
    public ResponseEntity<Map<String, Object>> getOcrText(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            String ocrText = documentService.getOcrText(id, user);
            return ResponseEntity.ok(Map.of(
                    "documentId", id,
                    "ocrText", ocrText != null ? ocrText : "",
                    "hasOcrText", ocrText != null && !ocrText.trim().isEmpty()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ocr/search")
    public ResponseEntity<Page<DocumentResponse>> searchInOcrText(
            Authentication authentication,
            @RequestParam String query,
            Pageable pageable) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Page<DocumentResponse> documents = documentService.searchInOcrText(user, query, pageable);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ocr/stats")
    public ResponseEntity<Map<String, Object>> getOcrStats(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Map<String, Object> stats = documentService.getOcrStatistics(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/ocr/batch-process")
    public ResponseEntity<Map<String, Object>> batchProcessOcr(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            int processedCount = documentService.batchProcessOcr(user);
            return ResponseEntity.ok(Map.of(
                    "message", "Batch OCR processing initiated",
                    "documentsProcessed", processedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}