package com.intelligentmarker.service;

import com.intelligentmarker.model.*;
import com.intelligentmarker.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Submission service
 * Coordinates the entire submission processing workflow: upload → OCR → anonymization → scoring
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {
    
    private final SubmissionRepository submissionRepository;
    private final AliyunOssService ossService;
    private final OCRService ocrService;
    private final AnonymizationService anonymizationService;
    private final ScoringService scoringService;
    private final AuditLogService auditLogService;
    private final VisionAdapter visionAdapter; // Advanced image understanding

    /**
     * Create submission and upload file
     */
    @Transactional
    public Submission createSubmission(User student, Assignment assignment, MultipartFile file) {
        log.info("Creating submission for student {} and assignment {}",
                student.getId(), assignment.getId());

        try {
            // 1. Upload original file to OSS
            String originalUrl = ossService.uploadFile(file, "submissions/original");

            // 2. Create Submission record
            Submission submission = new Submission();
            submission.setStudent(student);
            submission.setAssignment(assignment);
            submission.setOriginalDocUrl(originalUrl);
            submission.setOriginalFileName(file.getOriginalFilename());
            submission.setFileType(getFileExtension(file.getOriginalFilename()));
            submission.setStatus(Submission.SubmissionStatus.UPLOADED);

            submission = submissionRepository.save(submission);

            // 3. Record audit log
            auditLogService.log(
                student,
                "UPLOAD",
                "SUBMISSION",
                submission.getId(),
                Map.of("fileName", file.getOriginalFilename(), "fileSize", file.getSize())
            );
            
            return submission;
            
        } catch (Exception e) {
            log.error("Failed to create submission", e);
            throw new RuntimeException("Submission failed: " + e.getMessage());
        }
    }
    
    /**
     * Trigger async processing (call after transaction commit)
     */
    public void triggerAsyncProcessing(Long submissionId) {
        processSubmissionAsync(submissionId);
    }

    /**
     * Async process submission (OCR → Vision analysis → Anonymization → Scoring)
     * Uses REQUIRES_NEW propagation level, runs in new transaction to avoid affecting committed outer transaction
     */
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processSubmissionAsync(Long submissionId) {
        log.info("🚀 Starting async processing for submission {}", submissionId);

        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));

        try {
            // Step 1: Text extraction (images use Vision API, documents use OCR)
            updateStatus(submission, Submission.SubmissionStatus.OCR_PROCESSING);
            String ocrText = "";
            String visionAnalysis = null;

            if (isImageFile(submission.getFileType())) {
                // Image: prioritize OpenAI Vision API (no need for Tesseract)
                log.info("📸 Detected image file, using Vision API for text extraction...");
                visionAnalysis = performVisionAnalysis(submission);
                if (visionAnalysis != null && !visionAnalysis.isEmpty()) {
                    log.info("✅ Vision analysis completed: {} chars", visionAnalysis.length());
                    // Vision analysis result as OCR text (includes more advanced understanding)
                    ocrText = visionAnalysis;
                } else {
                    log.warn("⚠️ Vision API failed, trying fallback OCR...");
                    try {
                        ocrText = performOCR(submission);
                    } catch (Exception e) {
                        log.error("❌ Both Vision and OCR failed", e);
                        throw new RuntimeException("Text extraction failed: " + e.getMessage());
                    }
                }
            } else {
                // Document: use traditional OCR
                log.info("📄 Non-image file, using traditional OCR...");
                ocrText = performOCR(submission);
            }

            submission.setOcrText(ocrText);
            submissionRepository.save(submission);
            log.info("✅ Text extraction completed: {} characters", ocrText.length());

            // Step 2: Anonymization
            updateStatus(submission, Submission.SubmissionStatus.ANONYMIZING);
            String anonymizedText = performAnonymization(submission, ocrText);
            submission.setAnonymizedText(anonymizedText);

            // Upload anonymized text to OSS
            String anonymizedUrl = ossService.uploadText(
                anonymizedText,
                "submissions/anonymized",
                "anonymized_" + submission.getId() + ".txt"
            );
            submission.setAnonymizedDocUrl(anonymizedUrl);
            submissionRepository.save(submission);
            log.info("✅ Anonymization completed");

            // Step 3: AI scoring (including Vision analysis as additional context)
            updateStatus(submission, Submission.SubmissionStatus.SCORING);
            scoringService.scoreSubmission(submission, visionAnalysis);
            log.info("✅ AI scoring completed");

            // Step 4: Complete
            updateStatus(submission, Submission.SubmissionStatus.SCORED);
            
            log.info("🎉 Submission processing completed: {}", submissionId);
            
        } catch (Exception e) {
            log.error("❌ Submission processing failed: {}", submissionId, e);
            
            submission.setStatus(Submission.SubmissionStatus.FAILED);
            submission.setProcessingError(e.getMessage());
            submissionRepository.save(submission);
        }
    }
    
    /**
     * Perform OCR extraction
     */
    private String performOCR(Submission submission) {
        try {
            byte[] fileBytes = ossService.downloadFile(submission.getOriginalDocUrl());

            // If it's an image, check quality first
            if (isImageFile(submission.getFileType())) {
                if (!ocrService.checkImageQuality(fileBytes)) {
                    throw new RuntimeException("Image quality too low for OCR");
                }
            }

            String text = ocrService.extractText(fileBytes, submission.getFileType());

            log.info("OCR extracted {} characters for submission {}",
                    text.length(), submission.getId());

            return text;

        } catch (Exception e) {
            log.error("OCR failed for submission {}", submission.getId(), e);
            throw new RuntimeException("OCR failed: " + e.getMessage());
        }
    }

    /**
     * Perform Vision analysis (image only)
     * Uses OpenAI Vision API for advanced image understanding
     */
    private String performVisionAnalysis(Submission submission) {
        try {
            log.info("🔍 Starting Vision analysis for submission {}", submission.getId());

            // Download original image
            byte[] imageBytes = ossService.downloadFile(submission.getOriginalDocUrl());

            // Call Vision API to analyze image
            VisionAdapter.ImageAnalysisResult result = visionAdapter.analyzeImage(imageBytes);

            // Build analysis report (as additional context for AI scoring)
            StringBuilder analysis = new StringBuilder();
            analysis.append("\n=== Vision Analysis ===\n");

            if (result.getDescription() != null) {
                analysis.append("Description: ").append(result.getDescription()).append("\n");
            }

            if (result.getFormulas() != null && !result.getFormulas().isEmpty()) {
                analysis.append("Mathematical Formulas Detected:\n");
                for (String formula : result.getFormulas()) {
                    analysis.append("  - ").append(formula).append("\n");
                }
            }

            if (result.isContainsChart()) {
                analysis.append("Chart Detected: Yes\n");
                if (result.getChartData() != null) {
                    analysis.append("  Chart Type: ").append(result.getChartData().getChartType()).append("\n");
                }
            }

            if (result.getDetectedObjects() != null && !result.getDetectedObjects().isEmpty()) {
                analysis.append("Detected Objects: ").append(String.join(", ", result.getDetectedObjects())).append("\n");
            }

            analysis.append("=== End Vision Analysis ===\n");

            String analysisText = analysis.toString();
            log.info("✅ Vision analysis generated: {} chars", analysisText.length());

            return analysisText;

        } catch (Exception e) {
            log.warn("⚠️ Vision analysis failed (non-critical): {}. Continuing without Vision context.", e.getMessage());
            // Vision analysis failure doesn't affect main process, return null
            return null;
        }
    }

    /**
     * Perform anonymization
     */
    private String performAnonymization(Submission submission, String text) {
        User student = submission.getStudent();

        return anonymizationService.anonymize(
            text,
            student.getFullName(),
            student.getStudentId()
        );
    }

    /**
     * Update submission status
     */
    private void updateStatus(Submission submission, Submission.SubmissionStatus status) {
        submission.setStatus(status);
        submissionRepository.save(submission);
        log.info("Submission {} status updated to {}", submission.getId(), status);
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(String fileType) {
        return fileType.matches("jpg|jpeg|png|bmp|gif");
    }

    /**
     * Get submission details
     */
    public Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Submission not found: " + id));
    }

    /**
     * Get all submissions for an assignment
     */
    public List<Submission> getSubmissionsByAssignment(Assignment assignment) {
        return submissionRepository.findByAssignment(assignment);
    }
}

