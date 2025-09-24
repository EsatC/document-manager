package com.documentmanager.repository;

import com.documentmanager.entity.Document;
import com.documentmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByUser(User user, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.user = :user AND " +
            "(LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.number) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.ocrText) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Document> findByUserAndSearch(@Param("user") User user, @Param("search") String search, Pageable pageable);

    // Find documents that need OCR processing
    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.ocrProcessed = false AND d.fileAttachment IS NOT NULL")
    List<Document> findByUserAndOcrNotProcessed(@Param("user") User user);

    // Find documents with OCR text
    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.ocrProcessed = true AND d.ocrText IS NOT NULL")
    Page<Document> findByUserAndOcrProcessed(@Param("user") User user, Pageable pageable);

    // Search specifically in OCR text
    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.ocrProcessed = true AND " +
            "LOWER(d.ocrText) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Document> findByUserAndOcrTextContaining(@Param("user") User user, @Param("search") String search, Pageable pageable);

    // Count documents by OCR status
    @Query("SELECT COUNT(d) FROM Document d WHERE d.user = :user AND d.ocrProcessed = true")
    long countByUserAndOcrProcessed(@Param("user") User user);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.user = :user AND d.ocrProcessed = false AND d.fileAttachment IS NOT NULL")
    long countByUserAndOcrPending(@Param("user") User user);
}