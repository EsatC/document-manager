package com.documentmanager.service.impl;

import com.documentmanager.entity.Document;
import com.documentmanager.entity.FileAttachment;
import com.documentmanager.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage location initialized: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new IOException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public FileAttachment storeFile(MultipartFile file, Document document) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename.contains("..")) {
            throw new IOException("Sorry! Filename contains invalid path sequence " + originalFilename);
        }

        // Generate unique filename
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = generateUniqueFilename(document, fileExtension);

        // Create user-specific directory
        Path userDirectory = this.fileStorageLocation.resolve(document.getUser().getId().toString());
        Files.createDirectories(userDirectory);

        // Copy file to the target location
        Path targetLocation = userDirectory.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        logger.info("File stored successfully: {}", targetLocation);

        return new FileAttachment(
                originalFilename,
                storedFilename,
                file.getContentType(),
                file.getSize(),
                targetLocation.toString(),
                document
        );
    }

    @Override
    public Resource loadFileAsResource(FileAttachment fileAttachment) throws IOException {
        try {
            Path filePath = Paths.get(fileAttachment.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new IOException("File not found " + fileAttachment.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            throw new IOException("File not found " + fileAttachment.getOriginalFilename(), ex);
        }
    }

    @Override
    public void deleteFile(FileAttachment fileAttachment) throws IOException {
        try {
            Path filePath = Paths.get(fileAttachment.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            logger.info("File deleted successfully: {}", filePath);
        } catch (IOException ex) {
            logger.error("Error deleting file: {}", fileAttachment.getFilePath(), ex);
            throw new IOException("Could not delete file " + fileAttachment.getOriginalFilename(), ex);
        }
    }

    @Override
    public Path getFilePath(FileAttachment fileAttachment) throws IOException {
        Path filePath = Paths.get(fileAttachment.getFilePath()).normalize();

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileAttachment.getOriginalFilename());
        }

        return filePath;
    }

    private String generateUniqueFilename(Document document, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String docNumber = document.getNumber().replaceAll("[^a-zA-Z0-9]", "_");

        return String.format("%s_%s_%s_%s%s",
                docNumber,
                document.getId() != null ? document.getId() : "new",
                timestamp,
                uuid,
                extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}