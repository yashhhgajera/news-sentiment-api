package com.newsanalyzer.api.controllers;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.services.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "http://localhost:4200") // For Angular frontend
public class NewsController {
    
    @Autowired
    private NewsService newsService;
    
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
}