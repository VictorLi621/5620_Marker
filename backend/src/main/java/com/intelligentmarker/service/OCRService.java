package com.intelligentmarker.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * OCR text extraction service
 * Supports PDF, Word, and image formats
 */
@Service
@Slf4j
public class OCRService {
    
    private final Tesseract tesseract;
    
    public OCRService() {
        this.tesseract = new Tesseract();
        // Set Tesseract data path (need to download language pack)
        // tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setLanguage("eng+chi_sim"); // English + Simplified Chinese
    }

    /**
     * Extract text based on file type
     * @param fileBytes File byte array
     * @param fileType File type (pdf, docx, jpg, png)
     * @return Extracted text
     */
    public String extractText(byte[] fileBytes, String fileType) {
        try {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> extractFromPdf(fileBytes);
                case "doc", "docx" -> extractFromWord(fileBytes);
                case "jpg", "jpeg", "png", "bmp" -> extractFromImage(fileBytes);
                default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
            };
        } catch (Exception e) {
            log.error("OCR extraction failed for file type: {}", fileType, e);
            throw new RuntimeException("OCR failed: " + e.getMessage());
        }
    }
    
    /**
     * Extract text from PDF
     */
    private String extractFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // If PDF text is empty or too little, might be scanned image, try OCR
            if (text.trim().length() < 100) {
                log.info("PDF has little text, may be scanned image, attempting OCR...");
                // Simplified handling here, should convert PDF to image then OCR
                return text + "\n[Note: PDF may contain images requiring OCR]";
            }

            return text;
        }
    }

    /**
     * Extract text from Word document
     */
    private String extractFromWord(byte[] fileBytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            return extractor.getText();
        }
    }

    /**
     * Extract text from image (using Tesseract OCR)
     */
    private String extractFromImage(byte[] fileBytes) throws Exception {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));

            if (image == null) {
                throw new RuntimeException("Failed to read image");
            }

            String text = tesseract.doOCR(image);

            log.info("OCR extracted {} characters from image", text.length());
            return text;

        } catch (TesseractException e) {
            log.error("Tesseract OCR failed", e);
            throw new RuntimeException("OCR processing failed: " + e.getMessage());
        }
    }

    /**
     * Check if image quality is sufficient for OCR
     * @return true if quality is sufficient
     */
    public boolean checkImageQuality(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (image == null) {
                return false;
            }

            // Simple check: whether width and height are sufficient
            int width = image.getWidth();
            int height = image.getHeight();

            // Minimum requirement: 300x300 pixels
            boolean qualityOk = width >= 300 && height >= 300;

            log.info("Image quality check: {}x{} - {}", width, height,
                    qualityOk ? "PASS" : "FAIL (too small)");

            return qualityOk;

        } catch (Exception e) {
            log.error("Failed to check image quality", e);
            return false;
        }
    }
}

