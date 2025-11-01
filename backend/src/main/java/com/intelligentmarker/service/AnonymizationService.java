package com.intelligentmarker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Anonymization Service
 * Removes student names, student IDs, emails and other personally identifiable information
 */
@Service
@Slf4j
public class AnonymizationService {
    
    // Common student ID format: number + letter combination, 6-10 digits
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile(
        "\\b[0-9]{6,10}\\b|\\b[A-Z][0-9]{5,9}\\b"
    );

    // Email format
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    // Phone number format (China)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b1[3-9]\\d{9}\\b"
    );
    
    /**
     * Anonymize text
     * @param text Original text
     * @param studentName Student name (known)
     * @param studentId Student ID (known)
     * @return Anonymized text
     */
    public String anonymize(String text, String studentName, String studentId) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String anonymized = text;

        // 1. Remove known student name
        if (studentName != null && !studentName.isEmpty()) {
            anonymized = anonymized.replaceAll("(?i)" + Pattern.quote(studentName), "[STUDENT_NAME]");
        }

        // 2. Remove known student ID
        if (studentId != null && !studentId.isEmpty()) {
            anonymized = anonymized.replaceAll(Pattern.quote(studentId), "[STUDENT_ID]");
        }

        // 3. Remove student ID patterns
        Matcher sidMatcher = STUDENT_ID_PATTERN.matcher(anonymized);
        anonymized = sidMatcher.replaceAll("[STUDENT_ID]");

        // 4. Remove emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(anonymized);
        anonymized = emailMatcher.replaceAll("[EMAIL]");

        // 5. Remove phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(anonymized);
        anonymized = phoneMatcher.replaceAll("[PHONE]");

        // 6. Remove common PII identifiers
        anonymized = anonymized.replaceAll("(?i)(name|姓名)\\s*[:：]\\s*\\S+", "$1: [REDACTED]");
        anonymized = anonymized.replaceAll("(?i)(student\\s*id|学号)\\s*[:：]\\s*\\S+", "$1: [REDACTED]");
        
        log.info("Text anonymized: {} characters processed", text.length());
        
        return anonymized;
    }
    
    /**
     * Generate anonymization preview (for student confirmation)
     * Show before and after comparison
     */
    public AnonymizationPreview generatePreview(String original, String anonymized) {
        return new AnonymizationPreview(
            original.substring(0, Math.min(500, original.length())),
            anonymized.substring(0, Math.min(500, anonymized.length())),
            countRedactions(anonymized)
        );
    }
    
    /**
     * Count number of redacted PIIs
     */
    private int countRedactions(String text) {
        int count = 0;
        count += countOccurrences(text, "[STUDENT_NAME]");
        count += countOccurrences(text, "[STUDENT_ID]");
        count += countOccurrences(text, "[EMAIL]");
        count += countOccurrences(text, "[PHONE]");
        count += countOccurrences(text, "[REDACTED]");
        return count;
    }
    
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * Anonymization Preview DTO
     */
    public record AnonymizationPreview(
        String originalPreview,
        String anonymizedPreview,
        int redactionCount
    ) {}
}

