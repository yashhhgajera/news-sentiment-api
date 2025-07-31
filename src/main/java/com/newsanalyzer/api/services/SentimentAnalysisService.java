package com.newsanalyzer.api.services;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class SentimentAnalysisService {
    
    // Simple keyword-based sentiment analysis (we'll improve this later)
    private final List<String> positiveWords = Arrays.asList(
        "success", "growth", "improve", "win", "gain", "rise", "boost", 
        "celebrate", "achieve", "breakthrough", "excellent", "positive", "good"
    );
    
    private final List<String> negativeWords = Arrays.asList(
        "crisis", "crash", "decline", "fail", "loss", "drop", "worsen",
        "concern", "damage", "threat", "problem", "negative", "bad", "worst"
    );
    
    public String analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "NEUTRAL";
        }
        
        String lowerText = text.toLowerCase();
        
        long positiveCount = positiveWords.stream()
                .mapToLong(word -> countOccurrences(lowerText, word))
                .sum();
        
        long negativeCount = negativeWords.stream()
                .mapToLong(word -> countOccurrences(lowerText, word))
                .sum();
        
        if (positiveCount > negativeCount) {
            return "POSITIVE";
        } else if (negativeCount > positiveCount) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }
    
    private long countOccurrences(String text, String word) {
        return Arrays.stream(text.split("\\s+"))
                .mapToLong(w -> w.contains(word) ? 1 : 0)
                .sum();
    }
}