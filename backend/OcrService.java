
        package com.documentmanager.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final ITesseract tesseract;

    // Define supported MIME types for OCR
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/tiff",
            "image/tif"
    );

    public OcrService() {
        tesseract = new Tesseract();
        // You might need to set the path to your Tesseract installation and tessdata directory
        // For example: tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        // Or if you have it in your project structure:
        tesseract.setDatapath("C:\\Users\\Esat\\Desktop\\TesseractData\\tessdata"); // Adjust path as needed
        tesseract.setLanguage("tur+eng"); // Set default language, can be configured or passed as param
        tesseract.setPageSegMode(1);
    }

    /**
     * Checks if the given MIME type is supported for OCR extraction.
     *
     * @param mimeType The MIME type of the file.
     * @return true if supported, false otherwise.
     */
    public boolean isOcrSupported(String mimeType) {
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    /**
     * Extracts OCR text from a given MultipartFile if its type is supported.
     * The file is temporarily saved and then deleted.
     *
     * @param multipartFile The file to perform OCR on.
     * @return The extracted OCR text, or null if OCR is not supported or an error occurs.
     */
    public String extractOcrFromMultipartFile(MultipartFile multipartFile) {
        String mimeType = multipartFile.getContentType();
        if (!isOcrSupported(mimeType)) {
            logger.info("OCR not supported for file type: {}", mimeType);
            return null;
        }

        File tempFile = null;
        Path tempDirPath = null;
        try {
            // Create a temporary directory and file to process with Tesseract or PDFBox
            tempDirPath = Files.createTempDirectory("ocr_temp");
            tempFile = tempDirPath.resolve(multipartFile.getOriginalFilename()).toFile();
            multipartFile.transferTo(tempFile);

            logger.info("Starting OCR extraction for file: {}", multipartFile.getOriginalFilename());

            String ocrResult;
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                ocrResult = extractOcrFromPdf(tempFile.toPath());
            } else {
                // For images, use Tesseract directly
                ocrResult = tesseract.doOCR(tempFile);
            }

            logger.info("OCR extraction completed for file: {}", multipartFile.getOriginalFilename());
            return ocrResult;
        } catch (IOException e) {
            logger.error("Error saving temporary file for OCR: {}", e.getMessage(), e);
            return null;
        } catch (TesseractException e) {
            logger.error("Error during Tesseract OCR extraction: {}", e.getMessage(), e);
            return null;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    logger.warn("Could not delete temporary OCR file: {}", e.getMessage());
                }
            }
            if (tempDirPath != null && Files.exists(tempDirPath)) {
                try {
                    Files.delete(tempDirPath); // Delete the temp directory
                    logger.info("Temporary OCR directory deleted.");
                } catch (IOException e) {
                    logger.warn("Could not delete temporary OCR directory: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Extracts OCR text from a file given its Path.
     *
     * @param filePath The path to the file to perform OCR on.
     * @param mimeType The MIME type of the file.
     * @return The extracted OCR text, or null if OCR is not supported or an error occurs.
     */
    public String extractOcrFromFilePath(Path filePath, String mimeType) {
        if (!isOcrSupported(mimeType)) {
            logger.info("OCR not supported for file type: {}", mimeType);
            return null;
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            logger.warn("File not found for OCR extraction: {}", filePath);
            return null;
        }

        try {
            logger.info("Starting OCR extraction for file from path: {}", filePath);
            String ocrResult;
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                ocrResult = extractOcrFromPdf(filePath);
            } else {
                ocrResult = tesseract.doOCR(file);
            }
            logger.info("OCR extraction completed for file from path: {}", filePath);
            return ocrResult;
        } catch (TesseractException e) {
            logger.error("Error during Tesseract OCR extraction for file {}: {}", filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts OCR text from a PDF file using PDFBox to render pages as images,
     * then Tesseract to perform OCR on those images.
     *
     * @param pdfFilePath The path to the PDF file.
     * @return The extracted OCR text from all pages.
     */
    private String extractOcrFromPdf(Path pdfFilePath) throws TesseractException {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFilePath.toFile());
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder ocrTextBuilder = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                // Render each page as a BufferedImage (300 DPI is a good resolution for OCR)
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

                // Perform OCR on the rendered image
                String pageText = tesseract.doOCR(bim);
                ocrTextBuilder.append(pageText).append("\n\n"); // Append page text with a separator
            }
            return ocrTextBuilder.toString();
        } catch (IOException e) {
            logger.error("Error processing PDF file {}: {}", pdfFilePath, e.getMessage(), e);
            throw new TesseractException("Error processing PDF file for OCR", e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    logger.warn("Could not close PDF document {}: {}", pdfFilePath, e.getMessage());
                }
            }
        }
    }
}
