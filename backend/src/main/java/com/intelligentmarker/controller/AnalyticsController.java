package com.intelligentmarker.controller;

import com.intelligentmarker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Analytics Dashboard API
 */
@RestController
@RequestMapping("/api/analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    /**
     * Get score distribution
     */
    @GetMapping("/assignments/{assignmentId}/distribution")
    public ResponseEntity<?> getScoreDistribution(@PathVariable Long assignmentId) {
        try {
            var distribution = analyticsService.getScoreDistribution(assignmentId);
            
            return ResponseEntity.ok(Map.of(
                "scores", distribution.scores(),
                "mean", distribution.mean() != null ? distribution.mean() : 0,
                "median", distribution.median() != null ? distribution.median() : 0,
                "stdDev", distribution.stdDev() != null ? distribution.stdDev() : 0,
                "sampleSize", distribution.sampleSize()
            ));
            
        } catch (Exception e) {
            log.error("Failed to get distribution", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get error heatmap
     */
    @GetMapping("/assignments/{assignmentId}/heatmap")
    public ResponseEntity<?> getErrorHeatmap(@PathVariable Long assignmentId) {
        try {
            var heatmap = analyticsService.getErrorHeatmap(assignmentId);
            
            return ResponseEntity.ok(heatmap);
            
        } catch (Exception e) {
            log.error("Failed to get heatmap", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get knowledge point statistics
     */
    @GetMapping("/assignments/{assignmentId}/knowledge-points")
    public ResponseEntity<?> getKnowledgePointStats(@PathVariable Long assignmentId) {
        try {
            var stats = analyticsService.getKnowledgePointStats(assignmentId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get knowledge point stats", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

