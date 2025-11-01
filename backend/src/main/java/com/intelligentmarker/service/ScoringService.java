package com.intelligentmarker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentmarker.model.*;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.RubricRepository;
import com.intelligentmarker.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI scoring service
 * Uses OpenAI API for intelligent scoring
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScoringService {
    
    private final GradeRepository gradeRepository;
    private final RubricRepository rubricRepository;
    private final OpenAiService openAiService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    @Value("${app.scoring.confidence-threshold:0.85}")
    private double confidenceThreshold;
    
    /**
     * Perform AI scoring on submitted assignment
     */
    @Transactional
    /**
     * Scoring (without Vision analysis)
     */
    public Grade scoreSubmission(Submission submission) {
        return scoreSubmission(submission, null);
    }
    
    /**
     * Scoring (with optional Vision analysis as additional context)
     * @param submission Submission record
     * @param visionAnalysis Vision API analysis result (can be null)
     */
    public Grade scoreSubmission(Submission submission, String visionAnalysis) {
        log.info("Starting AI scoring for submission {}", submission.getId());
        if (visionAnalysis != null) {
            log.info("📸 Vision analysis available, will be used as additional context");
        }
        
        try {
            // 1. Get grading criteria (use default if not available)
            List<Rubric> rubrics = rubricRepository.findByAssignment(submission.getAssignment());

            // 2. Call OpenAI for scoring (supports scoring without rubric, includes Vision analysis)
            ScoringResult result = performAIScoring(
                submission.getAnonymizedText(),
                rubrics,
                submission.getAssignment(),
                visionAnalysis  // Pass Vision analysis result
            );

            // 3. Create Grade record
            Grade grade = new Grade();
            grade.setSubmission(submission);
            grade.setAiScore(result.getScore());
            grade.setAiConfidence(result.getConfidence());
            grade.setAiFeedback(result.getFeedbackJson());
            
            // 4. Set status based on confidence level
            if (result.getConfidence().doubleValue() >= confidenceThreshold) {
                grade.setStatus(Grade.GradeStatus.HIGH_CONFIDENCE);
                log.info("High confidence score: {} ({})", result.getScore(), result.getConfidence());
            } else {
                grade.setStatus(Grade.GradeStatus.NEEDS_REVIEW);
                log.info("Low confidence, requires teacher review: {} ({})", 
                        result.getScore(), result.getConfidence());
                
                // Notify teacher that review is needed
                notificationService.notifyTeacherReviewNeeded(submission);
            }

            grade = gradeRepository.save(grade);

            // 5. Record audit log
            auditLogService.log(
                null, // System operation
                "AI_SCORE",
                "GRADE",
                grade.getId(),
                Map.of(
                    "submissionId", submission.getId(),
                    "aiScore", result.getScore(),
                    "confidence", result.getConfidence(),
                    "status", grade.getStatus(),
                    "hasVisionAnalysis", visionAnalysis != null
                )
            );
            
            log.info("AI scoring completed for submission {}: score={}, confidence={}", 
                    submission.getId(), result.getScore(), result.getConfidence());
            
            return grade;
            
        } catch (Exception e) {
            log.error("AI scoring failed for submission {}", submission.getId(), e);
            throw new RuntimeException("Scoring failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform AI scoring (call OpenAI)
     */
    private ScoringResult performAIScoring(String studentAnswer, List<Rubric> rubrics, Assignment assignment, String visionAnalysis) {
        // Build prompt (including Vision analysis)
        String prompt = buildScoringPrompt(studentAnswer, rubrics, assignment, visionAnalysis);

        // Call OpenAI
        String aiResponse = openAiService.chat(prompt);

        // Parse AI response
        return parseAIResponse(aiResponse);
    }

    /**
     * Build OpenAI prompt (including optional Vision analysis)
     */
    private String buildScoringPrompt(String studentAnswer, List<Rubric> rubrics, Assignment assignment, String visionAnalysis) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a professional teaching assessment assistant. Please grade the student's answer according to the following grading criteria.\n\n");
        
        prompt.append("## Assignment Information\n");
        prompt.append("Title: ").append(assignment.getTitle()).append("\n");
        prompt.append("Description: ").append(assignment.getDescription()).append("\n");
        prompt.append("Total Marks: ").append(assignment.getTotalMarks()).append("\n\n");
        
        if (rubrics == null || rubrics.isEmpty()) {
            // If no rubric, use default general grading criteria
            prompt.append("## Grading Criteria (General Standards)\n");
            prompt.append("Since no specific grading criteria have been set, please grade according to the following general dimensions:\n\n");
            prompt.append("1. **Content Completeness** (40 points)\n");
            prompt.append("   - Whether the assignment requirements have been fully addressed\n");
            prompt.append("   - Whether key knowledge points are covered\n\n");
            prompt.append("2. **Accuracy** (30 points)\n");
            prompt.append("   - Whether concepts are correctly understood\n");
            prompt.append("   - Whether logical reasoning is reasonable\n\n");
            prompt.append("3. **Expression Quality** (20 points)\n");
            prompt.append("   - Whether language expression is clear\n");
            prompt.append("   - Whether the structure is reasonable\n\n");
            prompt.append("4. **Innovation** (10 points)\n");
            prompt.append("   - Whether there are unique insights\n");
            prompt.append("   - Depth of thinking\n\n");
        } else {
            // Use custom rubric
            prompt.append("## Grading Criteria (Rubric)\n");
            for (Rubric rubric : rubrics) {
                prompt.append("### ").append(rubric.getQuestionId()).append(" (").append(rubric.getWeight()).append(" points)\n");
                prompt.append("Criteria: ").append(rubric.getCriteria()).append("\n");
                
                if (rubric.getKeyPoints() != null) {
                    prompt.append("Key Points: ").append(rubric.getKeyPoints()).append("\n");
                }
                
                if (rubric.getSampleAnswer() != null) {
                    prompt.append("Sample Answer: ").append(rubric.getSampleAnswer()).append("\n");
                }
                prompt.append("\n");
            }
        }
        
        // If there is Vision analysis, add it to the prompt
        if (visionAnalysis != null && !visionAnalysis.isEmpty()) {
            prompt.append("## Image Analysis Results (AI Vision Recognition)\n");
            prompt.append("**Note**: The following is a deep analysis of the submitted image by OpenAI Vision API, including formula recognition, chart detection, handwriting recognition, etc.\n");
            prompt.append("Please combine this additional information for more accurate grading.\n\n");
            prompt.append(visionAnalysis).append("\n");
        }
        
        prompt.append("## Student Answer (OCR Extracted Text)\n");
        prompt.append(studentAnswer).append("\n\n");
        
        prompt.append("## Grading Requirements\n");
        prompt.append("1. Grade each question according to the grading criteria\n");
        prompt.append("2. Identify key points found in the student's answer\n");
        prompt.append("3. Provide a total score and confidence level (0.0-1.0)\n");
        prompt.append("4. **Provide COMPREHENSIVE and DETAILED feedback**: You MUST provide in-depth analysis of strengths, weaknesses, and improvement suggestions\n\n");

        prompt.append("## Detailed Feedback Guidelines\n");
        prompt.append("**IMPORTANT**: Your feedback must be thorough, specific, and actionable. Follow these guidelines:\n\n");

        prompt.append("### Strengths Analysis (Minimum 3-5 detailed points)\n");
        prompt.append("- Identify SPECIFIC aspects where the student performed well\n");
        prompt.append("- Quote or reference exact parts of the answer that demonstrate excellence\n");
        prompt.append("- Explain WHY each strength is important and how it contributes to the overall quality\n");
        prompt.append("- Cover multiple dimensions: content accuracy, logical reasoning, expression quality, creativity, technical skills, etc.\n");
        prompt.append("- Example: Instead of \"Good code structure\", write \"Well-organized code with clear separation of concerns: the data validation logic is properly isolated in a separate validator class, making the code more maintainable and testable\"\n\n");

        prompt.append("### Weaknesses Analysis (Minimum 3-5 detailed points)\n");
        prompt.append("- Identify SPECIFIC problems or gaps in the student's answer\n");
        prompt.append("- Explain the IMPACT of each weakness on the overall quality\n");
        prompt.append("- Provide CONTEXT about why this matters (e.g., industry standards, best practices, common pitfalls)\n");
        prompt.append("- Prioritize weaknesses by severity (critical issues first)\n");
        prompt.append("- Example: Instead of \"Missing error handling\", write \"Critical absence of error handling for network requests: the code lacks try-catch blocks for API calls, which could cause the application to crash when the server is unavailable. This violates the principle of defensive programming and creates poor user experience\"\n\n");

        prompt.append("### Improvement Suggestions (Minimum 3-5 detailed action items)\n");
        prompt.append("For EACH suggestion, you MUST provide ALL four components:\n");
        prompt.append("- **issue**: Clearly describe the specific problem (be precise and reference the code/answer)\n");
        prompt.append("- **suggestion**: Provide a concrete, actionable recommendation for improvement\n");
        prompt.append("- **why**: Explain the educational value and importance of this improvement (connect to learning objectives, industry practices, or theoretical concepts)\n");
        prompt.append("- **howToImprove**: Give step-by-step guidance or code examples showing exactly how to implement the improvement\n\n");

        prompt.append("### Quality Standards for Feedback\n");
        prompt.append("✓ Each strength/weakness should be 2-4 sentences minimum\n");
        prompt.append("✓ Use specific examples from the student's work\n");
        prompt.append("✓ Avoid generic statements like \"good job\" or \"needs improvement\"\n");
        prompt.append("✓ Connect feedback to learning objectives and real-world applications\n");
        prompt.append("✓ Be constructive and encouraging while remaining honest about areas needing improvement\n");
        prompt.append("✓ Suggestions should include concrete examples, code snippets, or reference materials when applicable\n\n");
        
        prompt.append("Please return the grading results in the following JSON format:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"totalScore\": <0-").append(assignment.getTotalMarks()).append(">,\n");
        prompt.append("  \"confidence\": <0.0-1.0>,\n");
        prompt.append("  \"breakdown\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"questionId\": \"Q1\",\n");
        prompt.append("      \"score\": <actual score>,\n");
        prompt.append("      \"maxScore\": <maximum score>,\n");
        prompt.append("      \"keyPointsFound\": [\"Found point 1\", \"Found point 2\"],\n");
        prompt.append("      \"keyPointsMissing\": [\"Missing point 1\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"feedback\": {\n");
        prompt.append("    \"strengths\": [\n");
        prompt.append("      \"Detailed strength 1: Comprehensive explanation with specific examples from the student's work, explaining why this is a strength and how it demonstrates mastery of the concept.\",\n");
        prompt.append("      \"Detailed strength 2: Another thorough analysis with specific references...\",\n");
        prompt.append("      \"(Provide 3-5 detailed strengths, each being 2-4 sentences)\"\n");
        prompt.append("    ],\n");
        prompt.append("    \"weaknesses\": [\n");
        prompt.append("      \"Detailed weakness 1: Specific problem identified in the student's work, explaining the impact and why it matters, with reference to standards or best practices.\",\n");
        prompt.append("      \"Detailed weakness 2: Another comprehensive analysis of a gap or issue...\",\n");
        prompt.append("      \"(Provide 3-5 detailed weaknesses, each being 2-4 sentences)\"\n");
        prompt.append("    ],\n");
        prompt.append("    \"suggestions\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"issue\": \"Precise description of the specific problem found in the answer, with reference to exact location or content\",\n");
        prompt.append("        \"suggestion\": \"Clear, actionable recommendation for how to address this issue\",\n");
        prompt.append("        \"why\": \"Comprehensive explanation of why this improvement matters: connect to learning objectives, industry practices, theoretical concepts, or real-world applications. Should be 2-3 sentences.\",\n");
        prompt.append("        \"howToImprove\": \"Step-by-step guidance with concrete examples: include code snippets, pseudocode, reference materials, or specific techniques the student should learn. Should be detailed enough to actually help the student improve.\"\n");
        prompt.append("      },\n");
        prompt.append("      \"(Provide 3-5 detailed suggestions with ALL four components for each)\"\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"confidenceReason\": \"Confidence explanation\"\n");
        prompt.append("}\n");
        prompt.append("```\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse AI response
     */
    private ScoringResult parseAIResponse(String aiResponse) {
        try {
            // Extract JSON part (remove markdown code block markers)
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                int start = aiResponse.indexOf("```json") + 7;
                int end = aiResponse.lastIndexOf("```");
                jsonStr = aiResponse.substring(start, end).trim();
            }

            // Parse JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
            
            Number totalScore = (Number) parsed.get("totalScore");
            Number confidence = (Number) parsed.get("confidence");
            
            ScoringResult result = new ScoringResult();
            result.setScore(BigDecimal.valueOf(totalScore.doubleValue()).setScale(2, RoundingMode.HALF_UP));
            result.setConfidence(BigDecimal.valueOf(confidence.doubleValue()).setScale(2, RoundingMode.HALF_UP));
            result.setFeedbackJson(jsonStr);
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);

            // Fallback: return median score with low confidence
            ScoringResult fallback = new ScoringResult();
            fallback.setScore(BigDecimal.valueOf(50.0));
            fallback.setConfidence(BigDecimal.valueOf(0.3));
            fallback.setFeedbackJson("{\"error\": \"AI parsing failed\"}");

            return fallback;
        }
    }

    /**
     * Scoring result DTO
     */
    @lombok.Data
    private static class ScoringResult {
        private BigDecimal score;
        private BigDecimal confidence;
        private String feedbackJson;
    }
}

