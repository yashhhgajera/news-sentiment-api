package com.newsanalyzer.api.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {
    private String title;
    private String description;
    private String url;
    private String country;
    private String sentiment; // "POSITIVE", "NEGATIVE", "NEUTRAL"
    private LocalDateTime publishedAt;
    private String source;
}