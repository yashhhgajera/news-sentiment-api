package com.newsanalyzer.api.services;

import com.newsanalyzer.api.models.NewsArticle;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MockNewsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockNewsService.class);
    
    // Mock headlines organized by sentiment
private final Map<String, List<String>> mockHeadlines = new HashMap<>();

// Constructor to initialize mock data
public MockNewsService() {
    initializeMockHeadlines();
}

private void initializeMockHeadlines() {
    // Positive news headlines
    List<String> positiveHeadlines = Arrays.asList(
        "Scientists discover breakthrough treatment for diabetes",
        "Local school receives $2 million donation for new technology lab",
        "Unemployment rate drops to lowest level in decades",
        "New renewable energy plant powers 50,000 homes",
        "Medical team successfully performs rare life-saving surgery",
        "Tech company announces 1,000 new jobs in city center",
        "Environmental cleanup project restores local river ecosystem",
        "Students win international robotics competition",
        "New vaccine shows 95% effectiveness in clinical trials",
        "Community garden project brings neighbors together"
    );

    // Negative news headlines  
    List<String> negativeHeadlines = Arrays.asList(
        "Factory explosion injures dozens of workers",
        "Major data breach affects millions of users",
        "Severe flooding forces thousands to evacuate homes",
        "Stock market crashes amid economic uncertainty",
        "Hospital faces critical shortage of medical supplies",
        "Cyber attack shuts down city's power grid",
        "Massive wildfire destroys hundreds of homes",
        "Company announces layoffs affecting 2,000 employees",
        "Bridge collapse causes major transportation disruption",
        "Food safety recall affects popular restaurant chain"
    );

    // Neutral news headlines
    List<String> neutralHeadlines = Arrays.asList(
        "City council meeting scheduled for next Tuesday",
        "Weather forecast predicts moderate temperatures this week",
        "New traffic light installed at busy intersection",
        "Library extends weekend hours starting next month",
        "Annual budget report released by finance department",
        "Public transportation schedule changes take effect Monday",
        "Census data shows population growth in suburban areas",
        "Municipal elections scheduled for November ballot",
        "Road construction project begins on Main Street",
        "Government releases quarterly economic statistics"
    );

    mockHeadlines.put("POSITIVE", positiveHeadlines);
    mockHeadlines.put("NEGATIVE", negativeHeadlines);
    mockHeadlines.put("NEUTRAL", neutralHeadlines);
    
    logger.info("Mock headlines initialized: {} positive, {} negative, {} neutral", 
        positiveHeadlines.size(), negativeHeadlines.size(), neutralHeadlines.size());
}

    // Supported countries (same as your real API)
private final List<String> countries = Arrays.asList("us", "gb", "ca", "au", "in", "de", "fr");

// Random number generator for realistic data
private final Random random = new Random();

/**
 * Generate a realistic news URL
 */
private String generateMockUrl(String country, String headline) {
    String domain = getCountryDomain(country);
    String slug = headline.toLowerCase()
        .replaceAll("[^a-z0-9\\s]", "")  // Remove special characters
        .replaceAll("\\s+", "-")         // Replace spaces with hyphens
        .substring(0, Math.min(50, headline.length())); // Limit length
    
    return "https://" + domain + "/news/" + slug + "-" + random.nextInt(10000);
}

/**
 * Get realistic domain name for country
 */
private String getCountryDomain(String country) {
    switch (country.toLowerCase()) {
        case "us": return "cnn.com";
        case "gb": return "bbc.co.uk";
        case "ca": return "cbc.ca";
        case "au": return "abc.net.au";
        case "in": return "timesofindia.com";
        case "de": return "spiegel.de";
        case "fr": return "lemonde.fr";
        default: return "news.com";
    }
}

/**
 * Generate mock news articles for a specific country
 */
public List<NewsArticle> generateMockNews(String country, int numberOfArticles) {
    logger.info("Generating {} mock articles for country: {}", numberOfArticles, country);
    
    List<NewsArticle> articles = new ArrayList<>();
    
    for (int i = 0; i < numberOfArticles; i++) {
        // Randomly pick a sentiment category
        String sentiment = getRandomSentiment();
        
        // Get headlines for that sentiment
        List<String> headlines = mockHeadlines.get(sentiment);
        
        // Pick a random headline
        String headline = headlines.get(random.nextInt(headlines.size()));
        
        // Create the article
        NewsArticle article = createMockArticle(country, headline, sentiment);
        articles.add(article);
    }
    
    logger.info("Generated {} mock articles for {}", articles.size(), country);
    return articles;
}

/**
 * Randomly select a sentiment (with realistic distribution)
 */
private String getRandomSentiment() {
    int rand = random.nextInt(100);
    
    // Realistic news distribution: more negative news typically
    if (rand < 50) {
        return "NEGATIVE";  // 50% negative (news tends to be negative)
    } else if (rand < 75) {
        return "NEUTRAL";   // 25% neutral
    } else {
        return "POSITIVE";  // 25% positive
    }
}

/**
 * Create a single mock NewsArticle
 */
private NewsArticle createMockArticle(String country, String headline, String expectedSentiment) {
    NewsArticle article = new NewsArticle();
    
    // Basic article info
    article.setTitle(headline);
    article.setDescription(generateDescription(headline));
    article.setUrl(generateMockUrl(country, headline));
    article.setCountry(country.toUpperCase());
    
    // Timestamp (random time in last 6 hours)
    LocalDateTime publishTime = LocalDateTime.now()
        .minusMinutes(random.nextInt(360)); // 0-360 minutes ago
    article.setPublishedAt(publishTime);
    
    // Source info
    article.setSource(getSourceName(country));
    
    // We'll let the sentiment analysis service analyze these later
    // (Don't set sentiment here - let your real service do it)
    
    return article;
}

/**
 * Generate a realistic description from the headline
 */
private String generateDescription(String headline) {
    String[] descriptionTemplates = {
        "Breaking news: " + headline + ". More details to follow as story develops.",
        "Local authorities report: " + headline + ". Investigation ongoing.",
        "Officials confirm: " + headline + ". Full story available online.",
        "Latest update: " + headline + ". Residents advised to stay informed.",
        "According to sources: " + headline + ". Further information expected soon."
    };
    
    return descriptionTemplates[random.nextInt(descriptionTemplates.length)];
}

/**
 * Get realistic source name for country
 */
private String getSourceName(String country) {
    switch (country.toLowerCase()) {
        case "us": return "CNN News";
        case "gb": return "BBC News";
        case "ca": return "CBC News";
        case "au": return "ABC News Australia";
        case "in": return "Times of India";
        case "de": return "Der Spiegel";
        case "fr": return "Le Monde";
        default: return "International News";
    }
}

/**
 * Get mock news for all countries (replaces your NewsAPI call)
 */
public List<NewsArticle> getMockNewsForAllCountries() {
    logger.info("Generating mock news for all countries");
    
    List<NewsArticle> allArticles = new ArrayList<>();
    
    for (String country : countries) {
        // Generate 3-7 articles per country (random for realism)
        int articlesPerCountry = 3 + random.nextInt(5);
        List<NewsArticle> countryArticles = generateMockNews(country, articlesPerCountry);
        allArticles.addAll(countryArticles);
    }
    
    logger.info("Generated total {} mock articles across all countries", allArticles.size());
    return allArticles;
}

/**
 * Get mock news for a specific country
 */
public List<NewsArticle> getMockNewsForCountry(String country) {
    logger.info("Generating mock news for country: {}", country);
    
    // Generate 5-10 articles for the country
    int numberOfArticles = 5 + random.nextInt(6);
    return generateMockNews(country, numberOfArticles);
}

/**
 * Get list of supported countries
 */
public List<String> getSupportedCountries() {
    return new ArrayList<>(countries);
}

}