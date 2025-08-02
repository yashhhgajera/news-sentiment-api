package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.repositories.NewsArticleRepository;
import com.newsanalyzer.api.services.SentimentAnalysisService.SentimentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AsyncSentimentService {
    
    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;
    
    @Autowired
    private NewsArticleRepository newsRepository;
    
    // Track processing status
    private final Map<String, Integer> processingStatus = new ConcurrentHashMap<>();
    
    @Async("sentimentTaskExecutor")
    public CompletableFuture<Void> processArticlesSentimentAsync(List<NewsArticle> articles, String country) {
        try {
            System.out.println("🧠 Starting async sentiment processing for " + articles.size() + " articles (" + country.toUpperCase() + ")");
            
            long startTime = System.currentTimeMillis();
            processingStatus.put(country + "_processing", articles.size());
            
            // Process in chunks for better memory management
            int chunkSize = 20;
            int totalChunks = (articles.size() + chunkSize - 1) / chunkSize;
            
            for (int i = 0; i < articles.size(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, articles.size());
                List<NewsArticle> chunk = articles.subList(i, endIndex);
                
                processChunk(chunk, (i / chunkSize) + 1, totalChunks, country);
                
                // Small pause between chunks to prevent overwhelming the system
                Thread.sleep(100);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            processingStatus.put(country + "_completed", articles.size());
            processingStatus.put(country + "_time", (int) processingTime);
            
            System.out.println("✅ Async sentiment processing completed for " + country.toUpperCase() + 
                             " (" + articles.size() + " articles, " + processingTime + "ms)");
            
        } catch (Exception e) {
            System.err.println("❌ Error in async sentiment processing for " + country + ": " + e.getMessage());
            processingStatus.put(country + "_error", 1);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private void processChunk(List<NewsArticle> chunk, int chunkNumber, int totalChunks, String country) {
        try {
            // Extract text for batch analysis
            List<String> texts = chunk.stream()
                    .map(this::buildAnalysisText)
                    .collect(Collectors.toList());
            
            // Batch analyze sentiment
            Map<String, SentimentResult> results = sentimentAnalysisService.analyzeBatch(texts);
            
            // Apply results to articles
            for (int i = 0; i < chunk.size(); i++) {
                NewsArticle article = chunk.get(i);
                String text = texts.get(i);
                
                if (results.containsKey(text)) {
                    SentimentResult result = results.get(text);
                    article.setSentimentData(result.getSentiment(), result.getScore(), result.getConfidence());
                } else {
                    // Fallback
                    article.setSentimentData("NEUTRAL", 0.0, 0.5);
                }
            }
            
            // Batch update database
            newsRepository.saveAll(chunk);
            
            System.out.println("   📝 Processed chunk " + chunkNumber + "/" + totalChunks + " for " + country.toUpperCase());
            
        } catch (Exception e) {
            System.err.println("Error processing chunk " + chunkNumber + " for " + country + ": " + e.getMessage());
        }
    }
    
    @Async("sentimentTaskExecutor")
    public CompletableFuture<SentimentResult> analyzeSingleAsync(String text) {
        try {
            SentimentResult result = sentimentAnalysisService.analyzeSentiment(text);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in async single analysis: " + e.getMessage());
            return CompletableFuture.completedFuture(new SentimentResult("NEUTRAL", 0.0, 0.5));
        }
    }
    
    @Async("newsTaskExecutor")
    public CompletableFuture<Void> reprocessCountrySentiment(String country) {
        try {
            System.out.println("🔄 Reprocessing sentiment for " + country.toUpperCase());
            
            // Get all articles for country that need reprocessing
            List<NewsArticle> articles = newsRepository.findByCountry(country).stream()
                    .filter(article -> article.getSentimentConfidence() == null || article.getSentimentConfidence() < 0.3)
                    .collect(Collectors.toList());
            
            if (!articles.isEmpty()) {
                return processArticlesSentimentAsync(articles, country);
            } else {
                System.out.println("✅ No articles need reprocessing for " + country.toUpperCase());
                return CompletableFuture.completedFuture(null);
            }
            
        } catch (Exception e) {
            System.err.println("Error reprocessing sentiment for " + country + ": " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private String buildAnalysisText(NewsArticle article) {
        StringBuilder text = new StringBuilder();
        
        if (article.getTitle() != null && !article.getTitle().trim().isEmpty()) {
            text.append(article.getTitle());
        }
        
        if (article.getDescription() != null && !article.getDescription().trim().isEmpty()) {
            if (text.length() > 0) text.append(" ");
            text.append(article.getDescription());
        }
        
        return text.toString().trim();
    }
    
    // Public methods for monitoring
    public Map<String, Integer> getProcessingStatus() {
        return new ConcurrentHashMap<>(processingStatus);
    }
    
    public void clearProcessingStatus() {
        processingStatus.clear();
    }
}