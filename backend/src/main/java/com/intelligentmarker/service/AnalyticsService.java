package com.intelligentmarker.service;

import com.intelligentmarker.model.Assignment;
import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.Submission;
import com.intelligentmarker.repository.AssignmentRepository;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics service
 * Provides class grade distribution, error heatmap, and other analysis features
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    private final OpenAiService openAiService;
    
    /**
     * Get grade distribution for assignment
     */
    public ScoreDistribution getScoreDistribution(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));

        List<Submission> submissions = submissionRepository.findByAssignment(assignment);

        if (submissions.isEmpty()) {
            return new ScoreDistribution(Collections.emptyList(), null, null, null, 0);
        }

        // Collect all scores
        List<BigDecimal> scores = submissions.stream()
            .map(sub -> gradeRepository.findBySubmission(sub))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(grade -> grade.getTeacherScore() != null
                ? grade.getTeacherScore()
                : grade.getAiScore())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return new ScoreDistribution(Collections.emptyList(), null, null, null, 0);
        }

        // Calculate statistical metrics
        BigDecimal mean = scores.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        List<BigDecimal> sortedScores = scores.stream().sorted().collect(Collectors.toList());
        BigDecimal median = sortedScores.get(sortedScores.size() / 2);

        // Calculate standard deviation
        double variance = scores.stream()
            .mapToDouble(s -> Math.pow(s.subtract(mean).doubleValue(), 2))
            .average()
            .orElse(0.0);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance))
            .setScale(2, RoundingMode.HALF_UP);
        
        return new ScoreDistribution(scores, mean, median, stdDev, scores.size());
    }
    
    /**
     * Get error heatmap (frequently incorrect knowledge points)
     */
    public List<ErrorHeatmapItem> getErrorHeatmap(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));

        List<Submission> submissions = submissionRepository.findByAssignment(assignment);

        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all AI feedback
        List<String> allFeedbacks = new ArrayList<>();
        int totalSubmissions = 0;
        
        for (Submission sub : submissions) {
            Optional<Grade> gradeOpt = gradeRepository.findBySubmission(sub);
            if (gradeOpt.isPresent()) {
                Grade grade = gradeOpt.get();
                String feedback = grade.getAiFeedback();
                if (feedback != null && !feedback.isEmpty()) {
                    allFeedbacks.add(feedback);
                    totalSubmissions++;
                }
            }
        }
        
        if (allFeedbacks.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Use AI to analyze all feedback and extract common issues
        try {
            log.info("🔍 Using AI to analyze common issues from {} submissions", totalSubmissions);

            String analysisPrompt = buildCommonIssuesPrompt(allFeedbacks, assignment.getTitle());
            String aiResponse = openAiService.chat(analysisPrompt);

            // Parse AI returned common issues list
            return parseCommonIssues(aiResponse, totalSubmissions);

        } catch (Exception e) {
            log.error("AI analysis for heatmap failed, falling back to keyword matching", e);
            // Fallback: simple keyword matching
            return fallbackKeywordAnalysis(submissions);
        }
    }

    /**
     * Build AI analysis prompt
     */
    private String buildCommonIssuesPrompt(List<String> feedbacks, String assignmentTitle) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a teaching analysis expert. Below is a summary of all student AI grading feedback for an assignment (").append(assignmentTitle)
              .append(").\n\n");

        prompt.append("## All Student Feedback (").append(feedbacks.size()).append(" total)\n\n");

        // Limit each feedback length to avoid excessively long prompts
        for (int i = 0; i < feedbacks.size() && i < 50; i++) {
            String feedback = feedbacks.get(i);
            if (feedback.length() > 500) {
                feedback = feedback.substring(0, 500) + "...";
            }
            prompt.append("**Student ").append(i + 1).append(" Feedback:**\n")
                  .append(feedback).append("\n\n");
        }

        prompt.append("## Analysis Task\n");
        prompt.append("Please analyze all the feedback above and summarize 5-10 **most common issues**, sorted by frequency.\n\n");
        prompt.append("Please return in the following JSON format:\n");
        prompt.append("```json\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"issue\": \"Issue description (concise and clear)\",\n");
        prompt.append("    \"count\": Estimated occurrence count (integer),\n");
        prompt.append("    \"description\": \"Detailed description of the issue (optional)\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * Parse AI returned common issues
     */
    private List<ErrorHeatmapItem> parseCommonIssues(String aiResponse, int totalSubmissions) {
        try {
            // Extract JSON part
            String json = aiResponse;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();

            // Parse JSON (simplified version, using Jackson)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(json);
            
            List<ErrorHeatmapItem> items = new ArrayList<>();
            
            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                    String issue = node.get("issue").asText();
                    int count = node.get("count").asInt();
                    double percentage = (double) count / totalSubmissions * 100;
                    
                    items.add(new ErrorHeatmapItem(issue, count, percentage));
                }
            }
            
            log.info("✅ AI identified {} common issues", items.size());
            return items;
            
        } catch (Exception e) {
            log.error("Failed to parse AI response for common issues", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Fallback: simple keyword matching (when AI analysis fails)
     */
    private List<ErrorHeatmapItem> fallbackKeywordAnalysis(List<Submission> submissions) {
        Map<String, Integer> issueCount = new HashMap<>();
        
        for (Submission sub : submissions) {
            Optional<Grade> gradeOpt = gradeRepository.findBySubmission(sub);
            if (gradeOpt.isPresent()) {
                Grade grade = gradeOpt.get();
                String feedback = grade.getAiFeedback();
                
                if (feedback != null) {
                    if (feedback.contains("complexity analysis") || feedback.contains("Complexity analysis")) issueCount.merge("Lack of complexity analysis", 1, Integer::sum);
                    if (feedback.contains("boundary") || feedback.contains("Boundary")) issueCount.merge("Insufficient boundary condition handling", 1, Integer::sum);
                    if (feedback.contains("error handling") || feedback.contains("Error handling")) issueCount.merge("Lack of error handling", 1, Integer::sum);
                    if (feedback.contains("test") || feedback.contains("Test")) issueCount.merge("Insufficient test coverage", 1, Integer::sum);
                    if (feedback.contains("comment") || feedback.contains("Comment")) issueCount.merge("Lack of code comments", 1, Integer::sum);
                }
            }
        }
        
        return issueCount.entrySet().stream()
            .map(e -> new ErrorHeatmapItem(
                e.getKey(),
                e.getValue(),
                (double) e.getValue() / submissions.size() * 100
            ))
            .sorted(Comparator.comparing(ErrorHeatmapItem::count).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Get knowledge point statistics
     */
    public List<KnowledgePointStat> getKnowledgePointStats(Long assignmentId) {
        // Simplified implementation: return common knowledge points
        return List.of(

            new KnowledgePointStat("Algorithm Design", 75.5, "Medium"),
            new KnowledgePointStat("Complexity Analysis", 62.3, "Weak"),
            new KnowledgePointStat("Code Implementation", 82.1, "Good"),
            new KnowledgePointStat("Error Handling", 58.7, "Weak"),
            new KnowledgePointStat("Test Case Design", 68.9, "Medium")
        );
    }
    
    /**
     * Score distribution DTO
     */
    public record ScoreDistribution(
        List<BigDecimal> scores,
        BigDecimal mean,
        BigDecimal median,
        BigDecimal stdDev,
        int sampleSize
    ) {}

    /**
     * Error heatmap item DTO
     */
    public record ErrorHeatmapItem(
        String issue,
        int count,
        double percentage
    ) {}

    /**
     * Knowledge point statistics DTO
     */
    public record KnowledgePointStat(
        String knowledgePoint,
        double averageScore,
        String level
    ) {}
}

