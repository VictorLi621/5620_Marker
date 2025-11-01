package com.intelligentmarker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.LearningPlan;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.LearningPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearningPlanService {

    private final LearningPlanRepository learningPlanRepository;
    private final GradeRepository gradeRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    /**
     * Generate a personalized learning plan based on assignment feedback
     */
    @Transactional
    public LearningPlan generateLearningPlan(Long gradeId, Long studentId) {
        log.info("Generating learning plan for grade {} and student {}", gradeId, studentId);

        // Check if plan already exists for this grade
        Optional<LearningPlan> existingPlan = learningPlanRepository.findByGradeId(gradeId);
        if (existingPlan.isPresent()) {
            log.info("Learning plan already exists for grade {}, returning existing plan", gradeId);
            return existingPlan.get();
        }

        // Get the grade and feedback
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));

        // Parse AI feedback to extract improvement suggestions
        List<Map<String, String>> suggestions = extractSuggestions(grade.getAiFeedback());

        if (suggestions.isEmpty()) {
            throw new RuntimeException("No improvement suggestions found in the feedback");
        }

        // Generate learning plan using AI
        String planJson = generatePlanWithAI(grade, suggestions);

        // Create and save learning plan
        LearningPlan learningPlan = new LearningPlan();
        learningPlan.setStudentId(studentId);
        learningPlan.setGradeId(gradeId);
        learningPlan.setAssignmentTitle(grade.getSubmission().getAssignment().getTitle());
        learningPlan.setPlanContent(planJson);
        learningPlan.setObjective("Targeted improvement based on assignment feedback");
        learningPlan.setEstimatedDuration("4-6 weeks");
        learningPlan.setStatus("ACTIVE");

        learningPlan = learningPlanRepository.save(learningPlan);
        log.info("Learning plan created successfully with ID {}", learningPlan.getId());

        return learningPlan;
    }

    /**
     * Get all learning plans for a student
     */
    public List<LearningPlan> getStudentLearningPlans(Long studentId) {
        return learningPlanRepository.findByStudentId(studentId);
    }

    /**
     * Extract improvement suggestions from AI feedback JSON
     */
    private List<Map<String, String>> extractSuggestions(String aiFeedbackJson) {
        List<Map<String, String>> suggestions = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> feedback = objectMapper.readValue(aiFeedbackJson, Map.class);

            Object feedbackObj = feedback.get("feedback");
            if (feedbackObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> feedbackMap = (Map<String, Object>) feedbackObj;

                Object suggestionsObj = feedbackMap.get("suggestions");
                if (suggestionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> suggestionsList = (List<Map<String, String>>) suggestionsObj;
                    suggestions.addAll(suggestionsList);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI feedback", e);
        }

        return suggestions;
    }

    /**
     * Generate learning plan using OpenAI
     */
    private String generatePlanWithAI(Grade grade, List<Map<String, String>> suggestions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an educational consultant creating a personalized, structured learning plan for a student.\n\n");

        prompt.append("## Student Context\n");
        prompt.append("Assignment: ").append(grade.getSubmission().getAssignment().getTitle()).append("\n");
        prompt.append("Student Score: ").append(grade.getTeacherScore() != null ? grade.getTeacherScore() : grade.getAiScore()).append("/100\n\n");

        prompt.append("## Areas for Improvement (from assignment feedback)\n");
        for (int i = 0; i < suggestions.size(); i++) {
            Map<String, String> suggestion = suggestions.get(i);
            prompt.append((i + 1)).append(". **Issue**: ").append(suggestion.get("issue")).append("\n");
            prompt.append("   **Why it matters**: ").append(suggestion.get("why")).append("\n\n");
        }

        prompt.append("## Task\n");
        prompt.append("Create a comprehensive, actionable learning plan with the following structure:\n\n");

        prompt.append("**Requirements**:\n");
        prompt.append("1. Divide the plan into 3-4 progressive phases (e.g., Foundation, Practice, Advanced Application)\n");
        prompt.append("2. Each phase should have:\n");
        prompt.append("   - Clear focus area (targeting specific issues from feedback)\n");
        prompt.append("   - Estimated duration (be realistic: 1-2 weeks per phase)\n");
        prompt.append("   - 3-5 specific, actionable tasks/activities\n");
        prompt.append("   - 2-4 recommended learning resources (books, tutorials, courses, documentation)\n");
        prompt.append("   - 1-3 measurable success criteria/milestones\n");
        prompt.append("3. Tasks should be concrete and specific (avoid vague statements like 'learn more')\n");
        prompt.append("4. Include a mix of: theory study, hands-on practice, and real-world application\n");
        prompt.append("5. Progress from foundational concepts to advanced topics\n\n");

        prompt.append("Return the learning plan in the following JSON format:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"objective\": \"Brief overall objective of this learning plan (2-3 sentences)\",\n");
        prompt.append("  \"estimatedDuration\": \"Total duration (e.g., '4-6 weeks')\",\n");
        prompt.append("  \"phases\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"phase\": \"Phase 1: Foundation Building\",\n");
        prompt.append("      \"duration\": \"Week 1-2\",\n");
        prompt.append("      \"focusArea\": \"Specific topic or skill this phase targets (linked to feedback issues)\",\n");
        prompt.append("      \"tasks\": [\n");
        prompt.append("        \"Specific task 1 with clear action items\",\n");
        prompt.append("        \"Specific task 2 with clear action items\",\n");
        prompt.append("        \"...\"\n");
        prompt.append("      ],\n");
        prompt.append("      \"resources\": [\n");
        prompt.append("        \"Resource 1: Book/Course/Tutorial name with brief description\",\n");
        prompt.append("        \"Resource 2: ...\",\n");
        prompt.append("        \"...\"\n");
        prompt.append("      ],\n");
        prompt.append("      \"milestones\": [\n");
        prompt.append("        \"Measurable success criterion 1\",\n");
        prompt.append("        \"Measurable success criterion 2\",\n");
        prompt.append("        \"...\"\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"phase\": \"Phase 2: ...\",\n");
        prompt.append("      \"...\": \"...\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n");

        // Call OpenAI
        String aiResponse = openAiService.chat(prompt.toString());

        // Extract JSON from response
        return extractJsonFromResponse(aiResponse);
    }

    /**
     * Extract JSON from AI response (remove markdown code blocks if present)
     */
    private String extractJsonFromResponse(String response) {
        String json = response.trim();

        // Remove markdown code block markers
        if (json.contains("```json")) {
            int start = json.indexOf("```json") + 7;
            int end = json.lastIndexOf("```");
            if (end > start) {
                json = json.substring(start, end).trim();
            }
        } else if (json.contains("```")) {
            int start = json.indexOf("```") + 3;
            int end = json.lastIndexOf("```");
            if (end > start) {
                json = json.substring(start, end).trim();
            }
        }

        return json;
    }
}
