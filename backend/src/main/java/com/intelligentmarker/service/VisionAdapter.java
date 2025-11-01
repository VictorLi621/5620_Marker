package com.intelligentmarker.service;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * VisionAdapter interface - AI image content understanding
 * Beyond basic OCR, provides advanced recognition for charts, formulas, handwriting, etc.
 */
public interface VisionAdapter {

    /**
     * Analyze image content
     * @param imageBytes Image byte array
     * @return Image analysis result
     */
    ImageAnalysisResult analyzeImage(byte[] imageBytes);

    /**
     * Detect mathematical formulas in image
     * @param imageBytes Image byte array
     * @return Formula list (LaTeX format)
     */
    List<String> detectFormulas(byte[] imageBytes);

    /**
     * Recognize handwritten text
     * @param imageBytes Image byte array
     * @return Recognized text
     */
    String recognizeHandwriting(byte[] imageBytes);

    /**
     * Recognize charts (bar, pie, line, etc.)
     * @param imageBytes Image byte array
     * @return Chart data
     */
    ChartData recognizeChart(byte[] imageBytes);

    /**
     * Image analysis result
     */
    @Data
    class ImageAnalysisResult {
        private String description;           // Image description
        private List<String> detectedObjects; // Detected objects
        private Map<String, Double> confidence; // Confidence levels
        private String extractedText;         // Extracted text (OCR)
        private List<String> formulas;        // Mathematical formulas
        private boolean containsChart;        // Whether contains chart
        private ChartData chartData;          // Chart data
    }

    /**
     * Chart data
     */
    @Data
    class ChartData {
        private String chartType;             // Chart type (bar, pie, line, etc.)
        private String title;                 // Chart title
        private List<String> labels;          // Labels
        private List<Double> values;          // Values
        private Map<String, Object> metadata; // Other metadata
    }
}

