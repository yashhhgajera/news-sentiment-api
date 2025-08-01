package com.newsanalyzer.api.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "news_articles")
public class NewsArticle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", length = 500)
    private String title;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "url", length = 500)
    private String url;
    
    @Column(name = "country", length = 10)
    private String country;
    
    @Column(name = "sentiment", length = 20)
    private String sentiment;
    
    // NEW: Enhanced sentiment fields
    @Column(name = "sentiment_score")
    private Double sentimentScore; // Range: -1.0 to +1.0
    
    @Column(name = "sentiment_confidence")
    private Double sentimentConfidence; // Range: 0.0 to 1.0
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "source", length = 100)
    private String source;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Convenience method to set all sentiment data at once
    public void setSentimentData(String sentiment, Double score, Double confidence) {
        this.sentiment = sentiment;
        this.sentimentScore = score;
        this.sentimentConfidence = confidence;
    }
}