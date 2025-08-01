package com.newsanalyzer.api.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.newsanalyzer.api.services.SentimentAnalysisService.SentimentResult;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity  // JPA annotation - this class maps to a database table
@Table(name = "news_articles")  // Table name in database
public class NewsArticle {
    
    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment ID
    private Long id;
    
    @Column(name = "title", length = 500)  // Column configuration
    private String title;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "url", length = 500)
    private String url;
    
    @Column(name = "country", length = 10)
    private String country;
    
    @Column(name = "sentiment", length = 20)
    private String sentiment;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "source", length = 100)
    private String source;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // JPA callback to set createdAt automatically
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}