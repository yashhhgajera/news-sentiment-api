package com.newsanalyzer.api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.newsanalyzer.api.models.NewsArticle;
import com.newsanalyzer.api.services.NewsService;

// controllers/NewsController.java
@RestController
@RequestMapping("/api/news")
public class NewsController {
    
    @Autowired
    private NewsService newsService;
    
    // GET /api/news?country=us&sentiment=positive
    @GetMapping
    public List<NewsArticle> getNews(@RequestParam String country, 
                                   @RequestParam(required = false) String sentiment) {
                                    return null;
        // This will return filtered news
    }
    
    // GET /api/news/countries - get available countries
    @GetMapping("/countries")
    public List<String> getAvailableCountries() {
        return null;
        // Return list of supported countries
    }
}
