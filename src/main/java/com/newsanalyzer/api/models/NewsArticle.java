package com.newsanalyzer.api.models;

import java.time.LocalDateTime;

// models/NewsArticle.java
public class NewsArticle {
    private String title;
    private String description;
    private String url;
    private String country;
    private String sentiment; // "POSITIVE", "NEGATIVE", "NEUTRAL"
    private LocalDateTime publishedAt;
    
    // Constructor, getters, setters
}
