package com.documentmanager.service;

import com.documentmanager.dto.DocumentRequest;
import com.documentmanager.dto.DocumentResponse;
import com.documentmanager.entity.Document;
import com.documentmanager.entity.FileAttachment;
import com.documentmanager.entity.User;
import com.documentmanager.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OcrService ocrService;

    public Page<DocumentResponse> getDocumentsByUser(User user, String search, Pageable pageable) {
        Page<Document> documents;
        if (search.isEmpty()) {
            documents = documentRepository.findByUser(user, pageable);
        } else {
            documents = documentRepository.findByUserAndSearch(user, search, pageable);
        }
        return documents.map(this::convertToResponse);
    }

    public Page<DocumentResponse> getOcrProcessedDocuments(User user, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserAndOcrProcessed(user, pageable);
        return documents.map(this::convertToResponse);
    }

    public Page<DocumentResponse> searchInOcrText(User user, String query, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserAndOcrTextContaining(user, query, pageable);
        return documents.map(this::convertToResponse);
    }

    public DocumentResponse createDocument(DocumentRequest request, MultipartFile file, User user, boolean processOcr) throws IOException {
        Document document = new Document(request.getTitle(), request.getNumber(), request.getDate(), request.getDescription(), user);

        if (file != null && !file.isEmpty()) {
            FileAttachment fileAttachment = fileStorageService.storeFile(file, document);
            document.setFileAttachment(fileAttachment);
        }

        Document savedDocument = documentRepository.save(document);

        // Process OCR if requested and file is present
        if (processOcr && savedDocument.getFileAttachment() != null) {
            processOcrAsync(savedDocument);
        }

        return convertToResponse(savedDocument);
    }

    public DocumentResponse updateDocument(Long id, DocumentRequest request, MultipartFile file, User user, boolean processOcr) throws IOException {
        Document document = getDocumentEntityById(id, user);

        document.setTitle(request.getTitle());
        document.setNumber(request.getNumber());
        document.setDate(request.getDate());
        document.setDescription(request.getDescription());

        if (file != null && !file.isEmpty()) {
            // Delete old file if exists
            if (document.getFileAttachment() != null) {
                fileStorageService.deleteFile(document.getFileAttachment());
            }

            FileAttachment fileAttachment = fileStorageService.storeFile(file, document);
            document.setFileAttachment(fileAttachment);

            // Reset OCR status when file is updated
            document.resetOcrStatus();
        }

        Document savedDocument = documentRepository.save(document);

        // Process OCR if requested and file is present
        if (processOcr && savedDocument.getFileAttachment() != null && !savedDocument.getOcrProcessed()) {
            processOcrAsync(savedDocument);
        }

        return convertToResponse(savedDocument);
    }

    public void deleteDocument(Long id, User user) {
        Document document = getDocumentEntityById(id, user);

        if (document.getFileAttachment() != null) {
            try {
                fileStorageService.deleteFile(document.getFileAttachment());
            } catch (IOException e) {
                logger.error("Error deleting file for document {}: {}", id, e.getMessage());
            }
        }

        documentRepository.delete(document);
    }

    public DocumentResponse uploadFile(Long id, MultipartFile file, User user, boolean processOcr) throws IOException {
        Document document = getDocumentEntityById(id, user);

        if (document.getFileAttachment() != null) {
            fileStorageService.deleteFile(document.getFileAttachment());
        }

        FileAttachment fileAttachment = fileStorageService.storeFile(file, document);
        document.setFileAttachment(fileAttachment);
        document.resetOcrStatus(); // Reset OCR status when new file is uploaded

        Document savedDocument = documentRepository.save(document);

        if (processOcr) {
            processOcrAsync(savedDocument);
        }

        return convertToResponse(savedDocument);
    }

    public Resource downloadFile(Long id, User user) throws IOException {
        Document document = getDocumentEntityById(id, user);

        if (document.getFileAttachment() == null) {
            throw new RuntimeException("No file attached to document");
        }

        return fileStorageService.loadFileAsResource(document.getFileAttachment());
    }

    public DocumentResponse deleteFile(Long id, User user) throws IOException {
        Document document = getDocumentEntityById(id, user);

        if (document.getFileAttachment() != null) {
            fileStorageService.deleteFile(document.getFileAttachment());
            document.setFileAttachment(null);
            document.resetOcrStatus(); // Reset OCR status when file is deleted
        }

        Document savedDocument = documentRepository.save(document);
        return convertToResponse(savedDocument);
    }

    public Document getDocumentEntityById(Long id, User user) {
        return documentRepository.findById(id)
                .filter(doc -> doc.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    // OCR-specific methods
    public DocumentResponse processOcrForDocument(Long id, User user) {
        Document document = getDocumentEntityById(id, user);

        if (document.getFileAttachment() == null) {
            throw new RuntimeException("No file attached to document");
        }

        if (!ocrService.isOcrSupported(document.getFileAttachment().getContentType())) {
            throw new RuntimeException("OCR not supported for this file type");
        }

        try {
            Path filePath = fileStorageService.getFilePath(document.getFileAttachment());
            String ocrText = ocrService.extractOcrFromFilePath(filePath, document.getFileAttachment().getContentType());

            document.markOcrAsProcessed(ocrText);
            Document savedDocument = documentRepository.save(document);

            logger.info("OCR processed for document {}: {} characters extracted", id,
                    ocrText != null ? ocrText.length() : 0);

            return convertToResponse(savedDocument);
        } catch (Exception e) {
            logger.error("Error processing OCR for document {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to process OCR", e);
        }
    }

    public String getOcrText(Long id, User user) {
        Document document = getDocumentEntityById(id, user);
        return document.getOcrText();
    }

    public Map<String, Object> getOcrStatistics(User user) {
        long totalDocuments = documentRepository.count();
        long ocrProcessedCount = documentRepository.countByUserAndOcrProcessed(user);
        long ocrPendingCount = documentRepository.countByUserAndOcrPending(user);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", totalDocuments);
        stats.put("ocrProcessedCount", ocrProcessedCount);
        stats.put("ocrPendingCount", ocrPendingCount);
        stats.put("ocrProcessedPercentage", totalDocuments > 0 ? (double) ocrProcessedCount / totalDocuments * 100 : 0);

        return stats;
    }

    public int batchProcessOcr(User user) {
        List<Document> pendingDocuments = documentRepository.findByUserAndOcrNotProcessed(user);

        int processedCount = 0;
        for (Document document : pendingDocuments) {
            try {
                processOcrAsync(document);
                processedCount++;
            } catch (Exception e) {
                logger.error("Error processing OCR for document {}: {}", document.getId(), e.getMessage());
            }
        }

        logger.info("Batch OCR processing initiated for {} documents for user {}", processedCount, user.getUsername());
        return processedCount;
    }

    private void processOcrAsync(Document document) {
        CompletableFuture.runAsync(() -> {
            try {
                if (document.getFileAttachment() != null &&
                        ocrService.isOcrSupported(document.getFileAttachment().getContentType())) {

                    Path filePath = fileStorageService.getFilePath(document.getFileAttachment());
                    String ocrText = ocrService.extractOcrFromFilePath(filePath, document.getFileAttachment().getContentType());

                    document.markOcrAsProcessed(ocrText);
                    documentRepository.save(document);

                    logger.info("OCR processed asynchronously for document {}: {} characters extracted",
                            document.getId(), ocrText != null ? ocrText.length() : 0);
                }
            } catch (Exception e) {
                logger.error("Error in async OCR processing for document {}: {}", document.getId(), e.getMessage(), e);
            }
        });
    }

    private DocumentResponse convertToResponse(Document document) {
        FileAttachment fileAttachment = document.getFileAttachment();
        boolean hasFile = fileAttachment != null;
        boolean ocrSupported = hasFile && ocrService.isOcrSupported(fileAttachment.getContentType());

        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getNumber(),
                document.getDate(),
                document.getDescription(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                hasFile,
                hasFile ? fileAttachment.getOriginalFilename() : null,
                hasFile ? fileAttachment.getContentType() : null,
                hasFile ? fileAttachment.getFileSize() : null,
                hasFile ? fileAttachment.getUploadedAt() : null,
                document.getOcrText(),
                document.getOcrProcessed() != null ? document.getOcrProcessed() : false,
                document.getOcrProcessedAt(),
                ocrSupported
        );
    }
}