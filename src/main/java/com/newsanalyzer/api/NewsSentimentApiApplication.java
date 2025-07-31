package com.newsanalyzer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class NewsSentimentApiApplication {

	public static void main(String[] args) {

		// Load .env file
        Dotenv dotenv = Dotenv.load();

        // Set as system property so Spring can read it using ${NEWSAPI_KEY}
        System.setProperty("NEWSAPI_KEY", dotenv.get("NEWSAPI_KEY"));

		SpringApplication.run(NewsSentimentApiApplication.class, args);
	}

}
