// FileAttachmentRepository.java
package com.documentmanager.repository;

import com.documentmanager.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
}