package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.repositories.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}