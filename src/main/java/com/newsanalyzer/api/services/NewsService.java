package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {
    
    // Mock data for now - we'll replace this with real API calls later
    private List<NewsArticle> mockNews;
    
    // Constructor - runs when Spring creates this service
    public NewsService() {
        initializeMockData();
    }
    
    public List<NewsArticle> getNewsByCountryAndSentiment(String country, String sentiment) {
        List<NewsArticle> filteredNews = mockNews.stream()
                .filter(article -> article.getCountry().equalsIgnoreCase(country))
                .collect(Collectors.toList());
        
        // If sentiment filter is provided, apply it
        if (sentiment != null && !sentiment.isEmpty()) {
            filteredNews = filteredNews.stream()
                    .filter(article -> article.getSentiment().equalsIgnoreCase(sentiment))
                    .collect(Collectors.toList());
        }
        
        return filteredNews;
    }
    
    public List<String> getAvailableCountries() {
        return mockNews.stream()
                .map(NewsArticle::getCountry)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    public Map<String, Long> getSentimentCounts(String country) {
        return mockNews.stream()
                .filter(article -> article.getCountry().equalsIgnoreCase(country))
                .collect(Collectors.groupingBy(
                    NewsArticle::getSentiment,
                    Collectors.counting()
                ));
    }
    
    // Initialize some fake news data for testing
    private void initializeMockData() {
        mockNews = Arrays.asList(
            new NewsArticle(
                "Tech stocks rally amid AI optimism",
                "Major technology companies see significant gains...",
                "https://example.com/tech-rally",
                "us",
                "POSITIVE",
                LocalDateTime.now().minusHours(2),
                "TechNews"
            ),
            new NewsArticle(
                "Climate change impacts worsen globally",
                "New report shows accelerating environmental damage...",
                "https://example.com/climate-report",
                "us",
                "NEGATIVE",
                LocalDateTime.now().minusHours(4),
                "Environmental News"
            ),
            new NewsArticle(
                "New infrastructure bill passes parliament",
                "Government approves major infrastructure spending...",
                "https://example.com/infrastructure",
                "us",
                "NEUTRAL",
                LocalDateTime.now().minusHours(1),
                "Political News"
            ),
            new NewsArticle(
                "Indian economy shows strong growth",
                "GDP figures exceed expectations this quarter...",
                "https://example.com/india-economy",
                "india",
                "POSITIVE",
                LocalDateTime.now().minusHours(3),
                "Economic Times"
            ),
            new NewsArticle(
                "Monsoon delays affect crop yields",
                "Farmers report concerns about delayed rainfall...",
                "https://example.com/monsoon-delays",
                "india",
                "NEGATIVE",
                LocalDateTime.now().minusHours(6),
                "Agricultural News"
            )
        );
    }
}