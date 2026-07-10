package ru.practicum.ewm.additional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.ewm.additional.interaction")
@EntityScan(basePackages = {
        "ru.practicum.ewm.categories.model", "ru.practicum.ewm.comments.model",
        "ru.practicum.ewm.compilation.model"
})
@EnableJpaRepositories(basePackages = {
        "ru.practicum.ewm.categories.repository", "ru.practicum.ewm.comments.repository",
        "ru.practicum.ewm.compilation.repository"
})
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.ewm.additional", "ru.practicum.ewm.comments", "ru.practicum.ewm.compilation",
        "ru.practicum.ewm.categories"
})
public class CategoryServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(CategoryServiceApp.class, args);
    }
}
