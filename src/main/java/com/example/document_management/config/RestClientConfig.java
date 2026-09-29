package com.example.document_management.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder()
                .baseUrl("http://localhost:8000")
                .defaultHeader("X-API-Key", "AWOJAIOFNOSINASVNASJKVNAJKFA6464612324NWFAOFIJOAFASAOIJAIOSA")
                .requestFactory(factory)
                .build();
    }
}
