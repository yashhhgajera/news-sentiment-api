package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.services.SentimentAnalysisService.SentimentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class ScheduledNewsService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledNewsService.class);

    @Autowired
private MockNewsService mockNewsService;
    
    @Autowired
    private ExternalNewsService externalNewsService;
    
    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;
    
    @Autowired
    private NewsService newsService;

    @Autowired
    private AsyncSentimentService asyncSentimentService;
    
    private final ConcurrentHashMap<String, List<NewsArticle>> newsCache = new ConcurrentHashMap<>();
    private final List<String> supportedCountries = Arrays.asList("us", "gb", "ca", "au", "in", "de", "fr");
    
    private LocalDateTime lastUpdated = LocalDateTime.now();
    private final Map<String, Integer> processingStats = new ConcurrentHashMap<>();
    
@Scheduled(fixedRate = 900000) // Every 15 minutes
public void fetchNewsForAllCountries() {
    System.out.println("🔄 Starting scheduled news fetch with MOCK DATA at: " + LocalDateTime.now());
    
    // Clean up old articles first
    newsService.cleanupOldArticles();
    
    // Reset processing stats
    processingStats.clear();
    
    // NEW: Use mock data instead of real API
    try {
        List<NewsArticle> mockArticles = mockNewsService.getMockNewsForAllCountries();
        System.out.println("📰 Generated " + mockArticles.size() + " mock articles");
        
        // Process each mock article (same as before)
        for (NewsArticle article : mockArticles) {
            processIndividualArticle(article);
        }
        
    } catch (Exception e) {
        logger.error("Error fetching mock news", e);
        System.out.println("❌ Error generating mock news: " + e.getMessage());
    }
    
    lastUpdated = LocalDateTime.now();
    printProcessingStats();
    System.out.println("🎉 Mock news fetch completed at: " + lastUpdated);
}


/**
 * Process a single article (replaces the country-based processing)
 */
private void processIndividualArticle(NewsArticle article) {
    try {
        String country = article.getCountry();
        
        // Save to database
        NewsArticle savedArticle = newsService.saveArticle(article);
        
        // Update stats
        processingStats.merge(country + "_fetched", 1, Integer::sum);
        
        System.out.println("✅ Processed: " + article.getTitle().substring(0, Math.min(50, article.getTitle().length())) + "...");
        
    } catch (Exception e) {
        logger.error("Error processing article: {}", article.getTitle(), e);
        System.out.println("❌ Failed to process: " + article.getTitle());
    }
}
    
    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void initialNewsLoad() {
        System.out.println("🚀 Initial news load starting...");
        fetchNewsForAllCountries();
    }
    
    private void processCountryNews(String country) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Fetch news from external API
            List<NewsArticle> articles = externalNewsService.fetchNewsByCountry(country);
            
            if (articles.isEmpty()) {
                System.out.println("⚠️ No articles fetched for " + country.toUpperCase());
                return;
            }
            
            // Filter recent articles
            List<NewsArticle> recentArticles = articles.stream()
                    .filter(this::isRecentArticle)
                    .collect(Collectors.toList());
            
            // Save articles to database immediately (without sentiment)
            // This ensures we don't lose data if sentiment processing fails
            List<NewsArticle> savedArticles = saveArticlesWithoutSentiment(recentArticles);
            
            // Update cache immediately with basic data
            updateCacheForCountry(country);
            
            // Process sentiment asynchronously (non-blocking)
            if (!savedArticles.isEmpty()) {
                asyncSentimentService.processArticlesSentimentAsync(savedArticles, country)
                    .thenRun(() -> {
                        // Update cache again after sentiment processing is complete
                        updateCacheForCountry(country);
                        System.out.println("🧠 Sentiment processing completed for " + country.toUpperCase());
                    })
                    .exceptionally(throwable -> {
                        System.err.println("❌ Async sentiment processing failed for " + country + ": " + throwable.getMessage());
                        return null;
                    });
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Update processing stats
            processingStats.put(country + "_fetched", articles.size());
            processingStats.put(country + "_saved", savedArticles.size());
            processingStats.put(country + "_time", (int) processingTime);
            
            System.out.println("⚡ " + country.toUpperCase() + ": " + 
                            "Fetched=" + articles.size() + 
                            ", Saved=" + savedArticles.size() + 
                            ", Time=" + processingTime + "ms (sentiment processing async)");
            
            Thread.sleep(1000);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing news for " + country + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add these helper methods:
    private List<NewsArticle> saveArticlesWithoutSentiment(List<NewsArticle> articles) {
        // Set default sentiment values for immediate saving
        articles.forEach(article -> {
            if (article.getSentiment() == null) {
                article.setSentimentData("PROCESSING", 0.0, 0.0);
            }
        });
        
        return newsService.saveArticles(articles);
    }

    private void updateCacheForCountry(String country) {
        try {
            List<NewsArticle> allCountryArticles = newsService.getRecentNews(country);
            newsCache.put(country, new CopyOnWriteArrayList<>(allCountryArticles));
        } catch (Exception e) {
            System.err.println("Error updating cache for " + country + ": " + e.getMessage());
        }
    }

    private List<NewsArticle> processSentimentBatch(List<NewsArticle> articles) {
        try {
            // Extract titles for batch sentiment analysis
            List<String> titles = articles.stream()
                    .map(NewsArticle::getTitle)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Batch analyze sentiment
            Map<String, SentimentResult> sentimentResults = sentimentAnalysisService.analyzeBatch(titles);
            
            // Apply sentiment results to articles
            return articles.stream()
                    .map(article -> applySentimentToArticle(article, sentimentResults))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            System.err.println("Error in batch sentiment processing: " + e.getMessage());
            // Fallback to individual processing
            return articles.stream()
                    .map(this::processSentimentIndividual)
                    .collect(Collectors.toList());
        }
    }
    
    private NewsArticle applySentimentToArticle(NewsArticle article, Map<String, SentimentResult> sentimentResults) {
        if (article.getTitle() != null && sentimentResults.containsKey(article.getTitle())) {
            SentimentResult result = sentimentResults.get(article.getTitle());
            article.setSentimentData(result.getSentiment(), result.getScore(), result.getConfidence());
        } else {
            // Fallback for articles without titles
            article.setSentimentData("NEUTRAL", 0.0, 0.5);
        }
        return article;
    }
    
    private NewsArticle processSentimentIndividual(NewsArticle article) {
        try {
            String textToAnalyze = buildAnalysisText(article);
            SentimentResult result = sentimentAnalysisService.analyzeSentiment(textToAnalyze);
            article.setSentimentData(result.getSentiment(), result.getScore(), result.getConfidence());
        } catch (Exception e) {
            System.err.println("Error processing sentiment for article: " + e.getMessage());
            article.setSentimentData("NEUTRAL", 0.0, 0.5);
        }
        return article;
    }
    
    private String buildAnalysisText(NewsArticle article) {
        StringBuilder text = new StringBuilder();
        
        if (article.getTitle() != null) {
            text.append(article.getTitle());
        }
        
        if (article.getDescription() != null && !article.getDescription().isEmpty()) {
            text.append(" ").append(article.getDescription());
        }
        
        return text.toString().trim();
    }
    
    private boolean isRecentArticle(NewsArticle article) {
        if (article.getPublishedAt() == null) return true;
        return article.getPublishedAt().isAfter(LocalDateTime.now().minusHours(24));
    }
    
    private void printProcessingStats() {
        System.out.println("\n📊 Processing Statistics:");
        for (String country : supportedCountries) {
            int fetched = processingStats.getOrDefault(country + "_fetched", 0);
            int saved = processingStats.getOrDefault(country + "_saved", 0);
            int time = processingStats.getOrDefault(country + "_time", 0);
            
            if (fetched > 0) {
                System.out.println("   " + country.toUpperCase() + 
                                 ": Fetched=" + fetched + 
                                 ", New=" + saved + 
                                 ", Time=" + time + "ms");
            }
        }
        System.out.println();
    }
    
    // Public methods for accessing cached data
    public List<NewsArticle> getCachedNews(String country) {
        return newsCache.getOrDefault(country, List.of());
    }
    
    public List<String> getSupportedCountries() {
        return supportedCountries;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public Map<String, Integer> getProcessingStats() {
        return new HashMap<>(processingStats);
    }
    
    public ConcurrentHashMap<String, List<NewsArticle>> getAllCachedNews() {
        return newsCache;
    }
}