package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.repositories.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsService {
    
    @Autowired
    private NewsArticleRepository newsRepository;
    
    public List<NewsArticle> getNewsByCountryAndSentiment(String country, String sentiment) {
        if (sentiment != null && !sentiment.isEmpty()) {
            return newsRepository.findByCountryAndSentiment(country, sentiment);
        } else {
            return newsRepository.findByCountry(country);
        }
    }
    
    public List<String> getAvailableCountries() {
        // Get distinct countries from database
        return newsRepository.findAll().stream()
                .map(NewsArticle::getCountry)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    public Map<String, Long> getSentimentCounts(String country) {
        List<Object[]> results = newsRepository.countSentimentsByCountry(country);
        
        return results.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],  // sentiment
                    row -> (Long) row[1]     // count
                ));
    }
    
    // Get recent articles (last 24 hours)
    public List<NewsArticle> getRecentNews(String country) {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        return newsRepository.findByCountryAndPublishedAtAfter(country, yesterday);
    }
    
    // Save articles to database
    public List<NewsArticle> saveArticles(List<NewsArticle> articles) {
        // Filter out duplicates
        List<NewsArticle> newArticles = articles.stream()
                .filter(article -> !newsRepository.existsByUrlAndCountry(article.getUrl(), article.getCountry()))
                .collect(Collectors.toList());
        
        return newsRepository.saveAll(newArticles);
    }
    
    // Clean up old articles
    public void cleanupOldArticles() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        newsRepository.deleteOldArticles(cutoff);
    }

    // Add these methods to NewsService.java

    // Get articles with high confidence sentiment
    public List<NewsArticle> getHighConfidenceNews(String country, String sentiment) {
        return newsRepository.findByCountryAndSentiment(country, sentiment).stream()
                .filter(article -> article.getSentimentConfidence() != null && article.getSentimentConfidence() > 0.7)
                .sorted((a, b) -> Double.compare(b.getSentimentConfidence(), a.getSentimentConfidence()))
                .collect(Collectors.toList());
    }

    // Get sentiment statistics with scores
    public Map<String, Object> getDetailedSentimentStats(String country) {
        List<NewsArticle> articles = newsRepository.findByCountry(country);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Count by sentiment
        Map<String, Long> counts = articles.stream()
                .collect(Collectors.groupingBy(
                    NewsArticle::getSentiment,
                    Collectors.counting()
                ));
        
        // Average scores by sentiment
        Map<String, Double> avgScores = articles.stream()
                .filter(a -> a.getSentimentScore() != null)
                .collect(Collectors.groupingBy(
                    NewsArticle::getSentiment,
                    Collectors.averagingDouble(NewsArticle::getSentimentScore)
                ));
        
        // Average confidence by sentiment
        Map<String, Double> avgConfidence = articles.stream()
                .filter(a -> a.getSentimentConfidence() != null)
                .collect(Collectors.groupingBy(
                    NewsArticle::getSentiment,
                    Collectors.averagingDouble(NewsArticle::getSentimentConfidence)
                ));
        
        stats.put("counts", counts);
        stats.put("averageScores", avgScores);
        stats.put("averageConfidence", avgConfidence);
        stats.put("totalArticles", articles.size());
        
        return stats;
    }

    // Save single article to database
public NewsArticle saveArticle(NewsArticle article) {
    return newsRepository.save(article);
}
}