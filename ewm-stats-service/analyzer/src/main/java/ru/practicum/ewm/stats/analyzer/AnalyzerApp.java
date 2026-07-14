package ru.practicum.ewm.stats.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AnalyzerApp {

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApp.class, args);
    }
}
