package com.intelligentmarker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * OpenAI Vision API adapter
 * Uses GPT-4 Vision for image content understanding
 * Supports real API calls and Mock mode
 */
@Service
@Slf4j
public class OpenAIVisionAdapter implements VisionAdapter {
    
    private final String apiKey;
    private final boolean visionEnabled;
    private final String visionModel;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAIVisionAdapter(
        @Value("${openai.api-key:}") String apiKey,
        @Value("${openai.vision.enabled:true}") boolean visionEnabled,
        @Value("${openai.vision.model:gpt-4o-mini}") String visionModel
    ) {
        this.apiKey = apiKey;
        this.visionEnabled = visionEnabled;
        this.visionModel = visionModel;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        // Check configuration at startup (enabled by default, prioritize real API)
        if (!visionEnabled) {
            log.warn("⚠️ Vision API explicitly disabled in configuration.");
            log.warn("Set openai.vision.enabled=true to enable Vision API.");
        } else if (!isValidApiKey(apiKey)) {
            log.error("❌ OpenAI API Key not configured or invalid!");
            log.error("Vision API will fallback to mock mode. Configure OPENAI_API_KEY to use real API.");
            log.error("For production use, please contact technical team to configure API key.");
        } else {
            log.info("✅ OpenAI Vision API enabled with model: {}", visionModel);
            log.info("Real API calls will be prioritized. Mock is fallback only.");
        }
    }
    
    private boolean isValidApiKey(String key) {
        return key != null && !key.isEmpty() 
            && !key.equals("mock-key") 
            && !key.equals("your-openai-key");
    }
    
    @Override
    public ImageAnalysisResult analyzeImage(byte[] imageBytes) {
        log.info("🔍 Analyzing image using Vision AI ({} bytes)", imageBytes.length);
        
        // Prioritize real API
        if (visionEnabled && isValidApiKey(apiKey)) {
            try {
                log.info("📡 Attempting to use real OpenAI Vision API...");

                // Convert image to Base64
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Build request
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", visionModel);
                requestBody.put("max_tokens", 1000);
                
                List<Map<String, Object>> messages = new ArrayList<>();
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                
                List<Object> content = new ArrayList<>();
                content.add(Map.of("type", "text", 
                    "text", "Analyze this image. Extract all text, identify any mathematical formulas, " +
                            "detect charts (bar/pie/line), and describe handwriting if present. " +
                            "Return response in JSON format with fields: description, extractedText, formulas, containsChart, chartType."));
                content.add(Map.of("type", "image_url", 
                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)));
                
                message.put("content", content);
                messages.add(message);
                requestBody.put("messages", messages);

                // Send request
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
                log.info("📤 Sending request to OpenAI Vision API...");
                @SuppressWarnings({"rawtypes", "unchecked"})
                ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
                );

                // Parse response
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                if (responseBody != null && responseBody.containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
                        String responseContent = (String) messageResponse.get("content");


                        log.info("✅ Successfully received real Vision API response ({} chars)", responseContent.length());

                        // Parse AI returned result
                        return parseVisionResponse(responseContent);
                    }
                }

                log.error("❌ Unexpected Vision API response format");
                log.error("Please contact technical team to check API configuration.");

            } catch (Exception e) {
                log.error("❌ Real Vision API call failed: {}", e.getMessage());
                log.error("Error details: ", e);
                log.error("⚠️ Falling back to mock mode. Please contact technical team to resolve API issues.");
            }
        } else {
            if (!visionEnabled) {
                log.warn("⚠️ Vision API is disabled in configuration.");
            } else {
                log.warn("⚠️ OpenAI API Key not configured or invalid.");
            }
            log.warn("Using mock analysis. For production use, please contact technical team.");
        }

        // Mock as the final fallback
        log.info("🔧 Using mock Vision analysis (fallback mode)");
        return getMockAnalysisResult();
    }

    @Override
    public List<String> detectFormulas(byte[] imageBytes) {
        log.info("📐 Detecting mathematical formulas in image");
        ImageAnalysisResult result = analyzeImage(imageBytes);
        return result.getFormulas() != null ? result.getFormulas() : getMockFormulas();
    }
    
    @Override
    public String recognizeHandwriting(byte[] imageBytes) {
        log.info("✍️ Recognizing handwriting");
        ImageAnalysisResult result = analyzeImage(imageBytes);
        return result.getExtractedText() != null ? result.getExtractedText() : getMockHandwritingResult();
    }
    
    @Override
    public ChartData recognizeChart(byte[] imageBytes) {
        log.info("📊 Recognizing chart in image");
        ImageAnalysisResult result = analyzeImage(imageBytes);
        return result.getChartData() != null ? result.getChartData() : getMockChartData();
    }
    
    /**
     * Parse Vision API returned result
     */
    private ImageAnalysisResult parseVisionResponse(String content) {
        try {
            log.info("🔍 Parsing Vision API response ({} chars)", content.length());

            // Try to parse JSON response
            if (content.contains("{") && content.contains("}")) {
                // Extract JSON part
                int start = content.indexOf("{");
                int end = content.lastIndexOf("}") + 1;
                String jsonStr = content.substring(start, end);
                
                log.debug("📋 Extracted JSON: {}", jsonStr.length() > 500 ? jsonStr.substring(0, 500) + "..." : jsonStr);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
                
                ImageAnalysisResult result = new ImageAnalysisResult();

                // Safely parse description
                result.setDescription(safeGetString(parsed, "description"));

                // Safely parse extractedText (can be string or array)
                String extractedText = safeGetText(parsed, "extractedText");
                result.setExtractedText(extractedText);

                // Parse formulas
                if (parsed.containsKey("formulas")) {
                    Object formulasObj = parsed.get("formulas");
                    if (formulasObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> formulas = (List<String>) formulasObj;
                        result.setFormulas(formulas);
                    } else if (formulasObj instanceof String) {
                        result.setFormulas(List.of((String) formulasObj));
                    }
                }

                // Parse containsChart
                Object containsChartObj = parsed.get("containsChart");
                if (containsChartObj instanceof Boolean) {
                    result.setContainsChart((Boolean) containsChartObj);
                } else if (containsChartObj instanceof String) {
                    result.setContainsChart(Boolean.parseBoolean((String) containsChartObj));
                }
                
                log.info("✅ Successfully parsed Vision response: description={} chars, text={} chars, formulas={}", 
                    result.getDescription() != null ? result.getDescription().length() : 0,
                    result.getExtractedText() != null ? result.getExtractedText().length() : 0,
                    result.getFormulas() != null ? result.getFormulas().size() : 0);
                
                return result;
            } else {
                // If it's a text description, build result
                log.info("📝 No JSON found, treating entire response as extracted text");
                ImageAnalysisResult result = new ImageAnalysisResult();
                result.setDescription(content);
                result.setExtractedText(content);
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Failed to parse Vision API response", e);
            log.error("📄 Raw response (first 1000 chars): {}", content.length() > 1000 ? content.substring(0, 1000) + "..." : content);

            // Fallback: use entire response as text instead of returning mock
            ImageAnalysisResult result = new ImageAnalysisResult();
            result.setDescription("AI analysis completed (raw response due to parsing error)");
            result.setExtractedText(content);
            return result;
        }
    }

    /**
     * Safely get string value from Map
     */
    private String safeGetString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
    
    /**
     * Safely get text from Map (can be string or array)
     */
    private String safeGetText(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            return list.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining("\n"));
        }
        return value.toString();
    }
    
    // ========== Mock data (for demo and testing) ==========

    private ImageAnalysisResult getMockAnalysisResult() {
        ImageAnalysisResult result = new ImageAnalysisResult();
        result.setDescription("Student submitted homework image containing handwritten solutions and mathematical formulas");
        result.setDetectedObjects(Arrays.asList("handwriting", "mathematical_formula", "diagram"));
        
        Map<String, Double> confidence = new HashMap<>();
        confidence.put("text_detection", 0.92);
        confidence.put("formula_detection", 0.88);
        confidence.put("chart_detection", 0.0);
        result.setConfidence(confidence);
        

        result.setExtractedText("Question 1: Solve the equation x^2 + 3x + 2 = 0\nSolution: Using the quadratic formula...");
        result.setFormulas(getMockFormulas());
        result.setContainsChart(false);
        
        return result;
    }
    
    private List<String> getMockFormulas() {
        return Arrays.asList(
            "x^2 + 3x + 2 = 0",
            "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
            "\\int_{0}^{1} x^2 dx = \\frac{1}{3}"
        );
    }
    
    private String getMockHandwritingResult() {
        return "This is the handwriting recognition result:\n" +
               "1. First, analyze the problem conditions\n" +
               "2. Apply relevant theorems\n" +
               "3. Draw conclusions";
    }
    
    private ChartData getMockChartData() {
        ChartData chartData = new ChartData();
        chartData.setChartType("bar");
        chartData.setTitle("Student Score Distribution");
        chartData.setLabels(Arrays.asList("0-60", "60-70", "70-80", "80-90", "90-100"));
        chartData.setValues(Arrays.asList(5.0, 12.0, 25.0, 35.0, 23.0));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("unit", "Number of Students");
        metadata.put("total", 100);
        chartData.setMetadata(metadata);

        return chartData;
    }
}

