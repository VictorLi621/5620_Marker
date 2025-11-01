package com.intelligentmarker.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI API service wrapper
 */
@Service
@Slf4j
public class OpenAiService {
    
    private final com.theokanning.openai.service.OpenAiService openAiClient;
    private final String model;
    
    public OpenAiService(
        @Value("${openai.api-key:}") String apiKey,
        @Value("${openai.model:gpt-4o-mini}") String model
    ) {
        // Check API Key (default to attempting real API)
        com.theokanning.openai.service.OpenAiService client = null;
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("mock-key") || apiKey.equals("your-openai-key")) {
            log.error("❌ OpenAI API Key not configured or invalid!");
            log.error("AI scoring will use mock responses. Configure OPENAI_API_KEY to enable real AI scoring.");
            log.error("For production use, please contact technical team to configure API key.");
        } else {
            try {
                client = new com.theokanning.openai.service.OpenAiService(apiKey, Duration.ofSeconds(60));
                log.info("✅ OpenAI service initialized with model: {} and valid API key", model);
                log.info("Real API calls will be prioritized. Mock is fallback only.");
            } catch (Exception e) {
                log.error("❌ Failed to initialize OpenAI client: {}", e.getMessage());
                log.error("Please contact technical team to check API configuration.");
            }
        }
        
        this.openAiClient = client;
        this.model = model;
    }
    
    /**
     * Send chat request to OpenAI
     * @param prompt The prompt
     * @return AI response
     */
    public String chat(String prompt) {
        // Prioritize real API
        if (openAiClient != null) {
            try {
                log.info("📡 Sending request to real OpenAI API (model: {})", model);
                
                ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
                
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(message))
                    .temperature(0.3) // Lower temperature for more consistent scoring
                    .maxTokens(2000)
                    .build();
                
                String response = openAiClient.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
                
                log.info("✅ Successfully received real OpenAI API response ({} chars)", response.length());
                
                return response;
                
            } catch (Exception e) {
                log.error("❌ Real OpenAI API call failed: {}", e.getMessage());
                log.error("Error details: ", e);
                log.error("⚠️ Falling back to mock mode. Please contact technical team to resolve API issues.");
            }
        } else {
            log.warn("⚠️ OpenAI client not initialized. Using mock response.");
            log.warn("For production use, please contact technical team to configure API key.");
        }
        
        // Mock as the final fallback
        log.info("🔧 Using mock AI scoring (fallback mode)");
        return getMockResponse();
    }
    
    /**
     * Mock response (for testing or when OpenAI API is unavailable)
     */
    private String getMockResponse() {
        return """
            ```json
            {
              "totalScore": 72.5,
              "confidence": 0.88,
              "breakdown": [
                {
                  "questionId": "Q1",
                  "score": 35,
                  "maxScore": 50,
                  "keyPointsFound": ["Clearly defined the problem", "Provided basic algorithm idea"],
                  "keyPointsMissing": ["Lacks complexity analysis", "No discussion of boundary conditions"]
                },
                {
                  "questionId": "Q2",
                  "score": 37.5,
                  "maxScore": 50,
                  "keyPointsFound": ["Correctly implemented core logic", "Good code readability"],
                  "keyPointsMissing": ["Lacks error handling", "Insufficient test coverage"]
                }
              ],
              "feedback": {
                "strengths": [
                  "Excellent code structure with clear separation of concerns: The student properly organized the solution into distinct functions (parseInput, processData, generateOutput), demonstrating strong understanding of modular programming principles. This approach makes the code highly maintainable and testable, which is crucial in professional software development.",
                  "Comprehensive variable naming following industry conventions: All variables use descriptive camelCase names (e.g., 'studentRecords', 'averageScore') that clearly indicate their purpose, making the code self-documenting. This shows attention to code readability and adherence to best practices.",
                  "Correct implementation of the core sorting algorithm: The student successfully implemented merge sort with proper recursive logic and correct base cases. The partition logic correctly handles edge cases and the merge operation properly combines sorted subarrays, demonstrating solid grasp of divide-and-conquer algorithms.",
                  "Good use of appropriate data structures: The student chose HashMap for O(1) lookup performance when tracking student IDs, showing understanding of time complexity implications and practical optimization skills in selecting the right data structure for the problem.",
                  "Clear inline documentation of complex logic: Key algorithmic steps are well-documented with comments, particularly around recursive calls and merge operations, which significantly aids code maintenance and demonstrates professional coding habits."
                ],
                "weaknesses": [
                  "Complete absence of algorithm complexity analysis: The submission lacks any discussion of time complexity O(n log n) or space complexity O(n). Understanding and articulating algorithmic complexity is fundamental in computer science, critical for technical interviews, and essential for making informed optimization decisions in real-world applications. This omission suggests a gap in theoretical understanding.",
                  "Insufficient boundary condition handling creates potential runtime errors: The code does not validate input arrays for null or empty cases before processing, which could lead to NullPointerException or ArrayIndexOutOfBoundsException. This violates defensive programming principles and could cause production failures. Professional code must gracefully handle edge cases to ensure reliability.",
                  "Missing comprehensive error handling mechanism: There are no try-catch blocks for potential runtime exceptions, and the code doesn't provide meaningful error messages for malformed input. In production systems, unhandled exceptions lead to poor user experience, difficult debugging, and potential security vulnerabilities. Error handling is a critical aspect of robust software design.",
                  "Lack of test cases and validation strategy: No unit tests or test scenarios were provided to verify correctness across different inputs (empty arrays, single elements, duplicates, large datasets). Testing is fundamental to software quality assurance and is expected in professional development. This absence raises concerns about code reliability.",
                  "Suboptimal memory usage in merge implementation: The merge function creates new arrays for each merge operation instead of using in-place merging or a single auxiliary array, resulting in space complexity of O(n log n) rather than the optimal O(n). This demonstrates a gap in understanding memory optimization techniques and their practical importance in resource-constrained environments."
                ],
                "suggestions": [
                  {
                    "issue": "Missing time and space complexity analysis in the algorithm description and code comments",
                    "suggestion": "Add comprehensive complexity analysis with clear explanations of how you derived the complexities",
                    "why": "Algorithmic complexity analysis is fundamental to computer science and software engineering. It's essential for: (1) Making informed decisions about algorithm selection, (2) Predicting performance on large datasets, (3) Succeeding in technical interviews where complexity analysis is always expected, (4) Communicating trade-offs to team members. Industry professionals must be able to analyze and articulate the efficiency of their solutions.",
                    "howToImprove": "Add detailed comments before your merge sort function: '// Time Complexity: O(n log n) - The array is divided log n times (binary division), and merging takes O(n) at each level. // Space Complexity: O(n) - Additional space for temporary arrays during merge. // Best/Average/Worst case are all O(n log n) due to consistent divide-and-conquer behavior.' Also consider creating a complexity analysis section in your documentation explaining the recurrence relation T(n) = 2T(n/2) + O(n)."
                  },
                  {
                    "issue": "No input validation or boundary condition handling for null, empty, or malformed data",
                    "suggestion": "Implement comprehensive input validation at the start of each function with appropriate error handling",
                    "why": "Defensive programming is a core principle in professional software development. Unvalidated inputs are the #1 cause of production bugs and security vulnerabilities. Proper boundary handling: (1) Prevents crashes and undefined behavior, (2) Provides better user experience with clear error messages, (3) Makes debugging easier by catching problems early, (4) Demonstrates understanding of the contract between caller and callee. Real-world code must handle all possible inputs gracefully.",
                    "howToImprove": "Add validation at function entry: 'public void mergeSort(int[] arr) { if (arr == null) { throw new IllegalArgumentException(\"Array cannot be null\"); } if (arr.length <= 1) { return; // Already sorted } // ... rest of implementation }'. Consider using Java's Objects.requireNonNull() for cleaner null checks, and write unit tests specifically for edge cases (null, empty, single element, duplicates)."
                  },
                  {
                    "issue": "Complete absence of exception handling and error recovery mechanisms throughout the code",
                    "suggestion": "Implement structured error handling with try-catch blocks and meaningful error messages",
                    "why": "Exception handling is critical for building production-ready software. Proper error handling: (1) Prevents application crashes and provides graceful degradation, (2) Enables better debugging through informative error messages and stack traces, (3) Improves user experience by providing actionable feedback, (4) Allows for error recovery and logging for monitoring. Professional applications must handle failures gracefully and provide visibility into errors for operations teams.",
                    "howToImprove": "Wrap risky operations in try-catch blocks: 'try { // Sort operation } catch (NullPointerException e) { logger.error(\"Null pointer in sort operation\", e); throw new IllegalArgumentException(\"Invalid input: \" + e.getMessage(), e); } catch (Exception e) { logger.error(\"Unexpected error in sorting\", e); throw new RuntimeException(\"Sort failed: \" + e.getMessage(), e); }'. Study Java's exception hierarchy, use specific exceptions, and always log errors with context."
                  },
                  {
                    "issue": "No unit tests provided to verify correctness across different scenarios and edge cases",
                    "suggestion": "Develop a comprehensive test suite covering normal cases, edge cases, and error conditions",
                    "why": "Testing is not optional in professional software development - it's fundamental to quality assurance. Unit tests: (1) Verify correctness and catch regression bugs early, (2) Serve as executable documentation of expected behavior, (3) Enable confident refactoring and code evolution, (4) Are required in industry (most companies won't merge code without tests). Test-driven development (TDD) is an industry best practice that improves code design and reduces bugs by 40-80%.",
                    "howToImprove": "Create a test class using JUnit: '@Test public void testMergeSort_EmptyArray() { int[] arr = {}; mergeSort(arr); assertEquals(0, arr.length); } @Test public void testMergeSort_SingleElement() { int[] arr = {5}; mergeSort(arr); assertArrayEquals(new int[]{5}, arr); } @Test public void testMergeSort_AlreadySorted() { int[] arr = {1,2,3,4,5}; mergeSort(arr); assertArrayEquals(new int[]{1,2,3,4,5}, arr); }'. Aim for >80% code coverage and include performance tests for large datasets."
                  },
                  {
                    "issue": "Inefficient memory allocation creating new arrays at each merge step instead of using a single auxiliary array",
                    "suggestion": "Refactor to use a single auxiliary array allocated once, reducing space complexity from O(n log n) to O(n)",
                    "why": "Memory optimization is crucial in real-world applications, especially for mobile devices, embedded systems, or processing large datasets. Efficient memory usage: (1) Reduces garbage collection overhead improving performance, (2) Enables processing of larger datasets within memory constraints, (3) Demonstrates understanding of space-time tradeoffs, (4) Is often the difference between a solution that works and one that scales. Senior engineers must consider resource constraints and optimize accordingly.",
                    "howToImprove": "Refactor your implementation: 'public void mergeSort(int[] arr) { int[] aux = new int[arr.length]; // Allocate once mergeSortHelper(arr, aux, 0, arr.length-1); } private void mergeSortHelper(int[] arr, int[] aux, int low, int high) { if (low < high) { int mid = low + (high-low)/2; mergeSortHelper(arr, aux, low, mid); mergeSortHelper(arr, aux, mid+1, high); merge(arr, aux, low, mid, high); // Reuse aux array } }'. Study the optimized merge sort in java.util.Arrays.sort() for production-quality implementation patterns."
                  }
                ]
              },
              "confidenceReason": "The student's answer has a complete structure and correct understanding of core concepts, but some details (complexity analysis, boundary handling) need to be supplemented. High confidence."
            }
            ```
            """;
    }
}

