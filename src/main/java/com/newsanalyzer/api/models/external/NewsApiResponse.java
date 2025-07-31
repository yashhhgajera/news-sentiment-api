package com.newsanalyzer.api.models.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class NewsApiResponse {
    private String status;
    private int totalResults;
    private List<NewsApiArticle> articles;
}