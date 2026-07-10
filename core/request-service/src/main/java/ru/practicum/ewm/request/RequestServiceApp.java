package ru.practicum.ewm.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.ewm.request.interaction")
@SpringBootApplication(scanBasePackages = "ru.practicum.ewm.request")
public class RequestServiceApp {
    public static void main(String[] args) { SpringApplication.run(RequestServiceApp.class, args); }
}
