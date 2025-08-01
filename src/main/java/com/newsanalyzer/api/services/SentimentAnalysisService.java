package com.newsanalyzer.api.services;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SentimentAnalysisService {
    
    // Enhanced word lists with weights
    private final Map<String, Double> positiveWords = new ConcurrentHashMap<>();
    private final Map<String, Double> negativeWords = new ConcurrentHashMap<>();
    private final Set<String> intensifiers = new HashSet<>();
    private final Set<String> negators = new HashSet<>();
    
    // Text preprocessing patterns
    private final Pattern urlPattern = Pattern.compile("https?://\\S+");
    private final Pattern mentionPattern = Pattern.compile("@\\w+");
    private final Pattern hashtagPattern = Pattern.compile("#\\w+");
    private final Pattern punctuationPattern = Pattern.compile("[^\\w\\s]");
    
    @PostConstruct
    public void initializeLexicons() {
        loadPositiveWords();
        loadNegativeWords();
        loadIntensifiers();
        loadNegators();
        System.out.println("✅ Sentiment analysis lexicons loaded successfully");
    }
    
    @Cacheable("sentimentCache")
    public SentimentResult analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new SentimentResult("NEUTRAL", 0.0, 0.5);
        }
        
        // Preprocess text
        String cleanText = preprocessText(text);
        List<String> tokens = tokenize(cleanText);
        
        // Calculate sentiment scores
        double positiveScore = calculatePositiveScore(tokens);
        double negativeScore = calculateNegativeScore(tokens);
        double neutralScore = 1.0 - Math.abs(positiveScore - negativeScore);
        
        // Determine overall sentiment
        String sentiment = determineSentiment(positiveScore, negativeScore);
        double confidence = calculateConfidence(positiveScore, negativeScore, neutralScore);
        double score = positiveScore - negativeScore; // Range: -1 to +1
        
        return new SentimentResult(sentiment, score, confidence);
    }
    
    // Batch processing for better performance
    public Map<String, SentimentResult> analyzeBatch(List<String> texts) {
        return texts.stream()
                .parallel() // Process in parallel for better performance
                .collect(Collectors.toConcurrentMap(
                    text -> text,
                    this::analyzeSentiment
                ));
    }
    
    private String preprocessText(String text) {
        // Remove URLs, mentions, hashtags
        String cleaned = urlPattern.matcher(text).replaceAll(" ");
        cleaned = mentionPattern.matcher(cleaned).replaceAll(" ");
        cleaned = hashtagPattern.matcher(cleaned).replaceAll(" ");
        
        // Unescape HTML entities
        cleaned = StringEscapeUtils.unescapeHtml4(cleaned);
        
        // Convert to lowercase
        cleaned = cleaned.toLowerCase();
        
        // Remove extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    private List<String> tokenize(String text) {
        return Arrays.stream(text.split("\\s+"))
                .map(word -> punctuationPattern.matcher(word).replaceAll(""))
                .filter(word -> !word.isEmpty())
                .filter(word -> word.length() > 1) // Remove single characters
                .collect(Collectors.toList());
    }
    
    private double calculatePositiveScore(List<String> tokens) {
        double score = 0.0;
        
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            if (positiveWords.containsKey(token)) {
                double wordScore = positiveWords.get(token);
                
                // Check for intensifiers (very good, extremely positive)
                if (i > 0 && intensifiers.contains(tokens.get(i - 1))) {
                    wordScore *= 1.5;
                }
                
                // Check for negators (not good, don't like)
                if (i > 0 && negators.contains(tokens.get(i - 1))) {
                    wordScore *= -0.5;
                }
                
                score += wordScore;
            }
        }
        
        return Math.min(score / tokens.size(), 1.0); // Normalize to 0-1
    }
    
    private double calculateNegativeScore(List<String> tokens) {
        double score = 0.0;
        
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            if (negativeWords.containsKey(token)) {
                double wordScore = negativeWords.get(token);
                
                // Check for intensifiers
                if (i > 0 && intensifiers.contains(tokens.get(i - 1))) {
                    wordScore *= 1.5;
                }
                
                // Check for negators (not bad = positive)
                if (i > 0 && negators.contains(tokens.get(i - 1))) {
                    wordScore *= -0.5;
                }
                
                score += wordScore;
            }
        }
        
        return Math.min(score / tokens.size(), 1.0);
    }
    
    private String determineSentiment(double positiveScore, double negativeScore) {
        double threshold = 0.1; // Minimum difference for non-neutral
        
        if (positiveScore - negativeScore > threshold) {
            return "POSITIVE";
        } else if (negativeScore - positiveScore > threshold) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }
    
    private double calculateConfidence(double positiveScore, double negativeScore, double neutralScore) {
        double maxScore = Math.max(positiveScore, Math.max(negativeScore, neutralScore));
        double totalScore = positiveScore + negativeScore + neutralScore;
        
        return totalScore > 0 ? maxScore / totalScore : 0.5;
    }
    
    // Load enhanced word lexicons
    private void loadPositiveWords() {
        // High impact positive words (weight 1.0)
        String[] highPositive = {
            "excellent", "outstanding", "amazing", "fantastic", "wonderful", "brilliant",
            "superb", "magnificent", "extraordinary", "exceptional", "remarkable", "incredible"
        };
        
        // Medium impact positive words (weight 0.7)
        String[] mediumPositive = {
            "good", "great", "nice", "positive", "happy", "pleased", "satisfied",
            "success", "win", "gain", "improve", "better", "best", "love", "like"
        };
        
        // Low impact positive words (weight 0.4)
        String[] lowPositive = {
            "okay", "fine", "decent", "adequate", "acceptable", "fair", "reasonable"
        };
        
        loadWordsWithWeight(positiveWords, highPositive, 1.0);
        loadWordsWithWeight(positiveWords, mediumPositive, 0.7);
        loadWordsWithWeight(positiveWords, lowPositive, 0.4);
    }
    
    private void loadNegativeWords() {
        // High impact negative words (weight 1.0)
        String[] highNegative = {
            "terrible", "awful", "horrible", "disgusting", "hate", "despise",
            "disaster", "catastrophe", "crisis", "failure", "worst", "pathetic"
        };
        
        // Medium impact negative words (weight 0.7)
        String[] mediumNegative = {
            "bad", "poor", "negative", "sad", "angry", "disappointed", "upset",
            "problem", "issue", "concern", "worry", "decline", "drop", "lose"
        };
        
        // Low impact negative words (weight 0.4)  
        String[] lowNegative = {
            "meh", "bland", "boring", "dull", "mediocre", "subpar", "lacking"
        };
        
        loadWordsWithWeight(negativeWords, highNegative, 1.0);
        loadWordsWithWeight(negativeWords, mediumNegative, 0.7);
        loadWordsWithWeight(negativeWords, lowNegative, 0.4);
    }
    
    private void loadIntensifiers() {
        intensifiers.addAll(Arrays.asList(
            "very", "extremely", "incredibly", "absolutely", "completely", "totally",
            "really", "quite", "highly", "tremendously", "enormously", "exceptionally"
        ));
    }
    
    private void loadNegators() {
        negators.addAll(Arrays.asList(
            "not", "no", "never", "none", "nothing", "nobody", "nowhere",
            "don't", "doesn't", "didn't", "won't", "wouldn't", "can't", "couldn't"
        ));
    }
    
    private void loadWordsWithWeight(Map<String, Double> wordMap, String[] words, double weight) {
        for (String word : words) {
            wordMap.put(word, weight);
        }
    }
    
    // Inner class for structured sentiment results
    public static class SentimentResult {
        private final String sentiment;
        private final double score;
        private final double confidence;
        
        public SentimentResult(String sentiment, double score, double confidence) {
            this.sentiment = sentiment;
            this.score = score;
            this.confidence = confidence;
        }
        
        // Getters
        public String getSentiment() { return sentiment; }
        public double getScore() { return score; }
        public double getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("SentimentResult{sentiment='%s', score=%.3f, confidence=%.3f}", 
                               sentiment, score, confidence);
        }
    }
}