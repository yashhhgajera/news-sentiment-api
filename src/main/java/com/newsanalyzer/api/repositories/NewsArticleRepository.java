package com.newsanalyzer.api.repositories;

import com.newsanalyzer.api.models.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    
    // Find articles by country
    List<NewsArticle> findByCountry(String country);
    
    // Find articles by country and sentiment
    List<NewsArticle> findByCountryAndSentiment(String country, String sentiment);
    
    // Find recent articles (last 24 hours)
    List<NewsArticle> findByPublishedAtAfter(LocalDateTime dateTime);
    
    // Find articles by country published after a certain time
    List<NewsArticle> findByCountryAndPublishedAtAfter(String country, LocalDateTime dateTime);
    
    // Custom query to get sentiment counts for a country
    @Query("SELECT a.sentiment, COUNT(a) FROM NewsArticle a WHERE a.country = :country GROUP BY a.sentiment")
    List<Object[]> countSentimentsByCountry(@Param("country") String country);
    
    // Delete old articles (older than 24 hours) - for cleanup
    @Modifying
    @Transactional
    @Query("DELETE FROM NewsArticle a WHERE a.publishedAt < :cutoffTime")
    void deleteOldArticles(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Check if article already exists (to avoid duplicates)
    boolean existsByUrlAndCountry(String url, String country);
}