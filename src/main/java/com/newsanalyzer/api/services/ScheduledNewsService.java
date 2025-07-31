package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScheduledNewsService {
    
    @Autowired
    private ExternalNewsService externalNewsService;
    
    @Autowired
    private SentimentAnalysisService sentimentAnalysisService; // We'll create this next
    
    // Thread-safe collections for storing news data
    private final ConcurrentHashMap<String, List<NewsArticle>> newsCache = new ConcurrentHashMap<>();
    private final List<String> supportedCountries = Arrays.asList("us", "gb", "ca", "au", "in", "de", "fr");
    
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    // Run every 15 minutes (900,000 milliseconds)
    @Scheduled(fixedRate = 900000) 
    public void fetchNewsForAllCountries() {
        System.out.println("🔄 Starting scheduled news fetch at: " + LocalDateTime.now());
        
        for (String country : supportedCountries) {
            try {
                // Fetch news from external API
                List<NewsArticle> articles = externalNewsService.fetchNewsByCountry(country);
                
                // Apply sentiment analysis to each article
                List<NewsArticle> processedArticles = articles.stream()
                        .map(this::processSentiment)
                        .filter(article -> isRecentArticle(article))
                        .toList();
                
                // Store in cache
                newsCache.put(country, new CopyOnWriteArrayList<>(processedArticles));
                
                System.out.println("✅ Fetched " + processedArticles.size() + " articles for " + country.toUpperCase());
                
                // Small delay between countries to be nice to the API
                Thread.sleep(1000);
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching news for " + country + ": " + e.getMessage());
            }
        }
        
        lastUpdated = LocalDateTime.now();
        System.out.println("🎉 News fetch completed at: " + lastUpdated);
    }
    
    // Run once on startup
    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void initialNewsLoad() {
        System.out.println("🚀 Initial news load starting...");
        fetchNewsForAllCountries();
    }
    
    // Helper method to process sentiment
    private NewsArticle processSentiment(NewsArticle article) {
        try {
            String sentiment = sentimentAnalysisService.analyzeSentiment(article.getTitle());
            article.setSentiment(sentiment);
        } catch (Exception e) {
            System.err.println("Error processing sentiment: " + e.getMessage());
            article.setSentiment("NEUTRAL"); // Fallback
        }
        return article;
    }
    
    // Helper method to filter recent articles (last 24 hours)
    private boolean isRecentArticle(NewsArticle article) {
        if (article.getPublishedAt() == null) return true;
        return article.getPublishedAt().isAfter(LocalDateTime.now().minusHours(24));
    }
    
    // Public methods for other services to access cached data
    public List<NewsArticle> getCachedNews(String country) {
        return newsCache.getOrDefault(country, List.of());
    }
    
    public List<String> getSupportedCountries() {
        return supportedCountries;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    // Get all cached news
    public ConcurrentHashMap<String, List<NewsArticle>> getAllCachedNews() {
        return newsCache;
    }
}