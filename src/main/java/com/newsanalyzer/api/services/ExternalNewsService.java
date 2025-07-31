package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.external.NewsApiResponse;
import com.newsanalyzer.api.models.external.NewsApiArticle;
import com.newsanalyzer.api.models.NewsArticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalNewsService {
    
    private final WebClient webClient;
    
    @Value("${newsapi.key}")
    private String apiKey;
    
    @Value("${newsapi.url}")
    private String apiUrl;
    
    @Value("${newsapi.pageSize}")
    private int pageSize;
    
    // Constructor - Spring will inject WebClient
    public ExternalNewsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public List<NewsArticle> fetchNewsByCountry(String country) {
        try {
            // Build the API URL with parameters
            String url = String.format("%s?country=%s&pageSize=%d&apiKey=%s", 
                                     apiUrl, country, pageSize, apiKey);
            
            // Make HTTP GET request
            NewsApiResponse response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(NewsApiResponse.class)
                    .block(); // Block to wait for response (synchronous)
            
            if (response != null && response.getArticles() != null) {
                // Transform NewsApiArticle -> NewsArticle
                return response.getArticles().stream()
                        .map(apiArticle -> transformToNewsArticle(apiArticle, country))
                        .filter(article -> article.getTitle() != null) // Remove null titles
                        .collect(Collectors.toList());
            }
            
            return List.of(); // Return empty list if no data
            
        } catch (Exception e) {
            System.err.println("Error fetching news for country: " + country + " - " + e.getMessage());
            return List.of(); // Return empty list on error
        }
    }
    
    // Transform external API data to our internal model
    private NewsArticle transformToNewsArticle(NewsApiArticle apiArticle, String country) {
        NewsArticle article = new NewsArticle();
        
        article.setTitle(apiArticle.getTitle());
        article.setDescription(apiArticle.getDescription());
        article.setUrl(apiArticle.getUrl());
        article.setCountry(country.toLowerCase());
        article.setSource(apiArticle.getSource() != null ? apiArticle.getSource().getName() : "Unknown");
        
        // Parse publishedAt string to LocalDateTime
        if (apiArticle.getPublishedAt() != null) {
            try {
                LocalDateTime publishedAt = LocalDateTime.parse(
                    apiArticle.getPublishedAt().replace("Z", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );
                article.setPublishedAt(publishedAt);
            } catch (Exception e) {
                article.setPublishedAt(LocalDateTime.now()); // Fallback
            }
        } else {
            article.setPublishedAt(LocalDateTime.now());
        }
        
        // We'll add sentiment analysis later - for now set as NEUTRAL
        article.setSentiment("NEUTRAL");
        
        return article;
    }
}