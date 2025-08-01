package com.newsanalyzer.api.config;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Define cache names and configurations
        cacheManager.setCacheNames(List.of("sentimentCache", "newsCache"));
        cacheManager.setAllowNullValues(false); // Don't cache null results
        
        return cacheManager;
    }
}