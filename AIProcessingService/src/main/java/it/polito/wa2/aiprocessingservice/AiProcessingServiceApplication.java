package it.polito.wa2.aiprocessingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;

@SpringBootApplication
public class AiProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiProcessingServiceApplication.class, args);
    }
}
