package com.newsanalyzer.api.controllers;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.services.ExternalNewsService;
import com.newsanalyzer.api.services.NewsService;
import com.newsanalyzer.api.services.ScheduledNewsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    
    @Autowired
    private NewsService newsService;

    @Autowired
    private ExternalNewsService externalNewsService;
    
    // GET /api/news?country=us&sentiment=positive
    @GetMapping
    public List<NewsArticle> getNews(
            @RequestParam(defaultValue = "us") String country,
            @RequestParam(required = false) String sentiment) {
        
        return newsService.getNewsByCountryAndSentiment(country, sentiment);
    }
    
    // GET /api/news/countries - get available countries
    @GetMapping("/countries")
    public List<String> getAvailableCountries() {
        return newsService.getAvailableCountries();
    }
    
    // GET /api/news/sentiments - get sentiment counts
    @GetMapping("/sentiments")
    public Object getSentimentCounts(@RequestParam(defaultValue = "us") String country) {
        return newsService.getSentimentCounts(country);
    }
    
    // Test endpoint to make sure everything works
    @GetMapping("/test")
    public String test() {
        return "News API is working! 🚀";
    }

    // Add this to NewsController.java
    @GetMapping("/external-test")
    public List<NewsArticle> testExternalApi() {
        return externalNewsService.fetchNewsByCountry("us");
    }

    // Add these to NewsController.java
    @Autowired
    private ScheduledNewsService scheduledNewsService;

    @GetMapping("/cached")
    public List<NewsArticle> getCachedNews(@RequestParam(defaultValue = "us") String country) {
        return scheduledNewsService.getCachedNews(country);
    }

    @GetMapping("/last-updated")
    public String getLastUpdated() {
        return "Last updated: " + scheduledNewsService.getLastUpdated();
    }

    @GetMapping("/force-refresh")
    public String forceRefresh() {
        scheduledNewsService.fetchNewsForAllCountries();
        return "News refresh triggered!";
    }
}