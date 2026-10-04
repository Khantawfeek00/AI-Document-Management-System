package it.polito.wa2.aiprocessingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiProcessingService {
    private final Logger logger = LoggerFactory.getLogger(AiProcessingService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ChatClient chatClient;

    public AiProcessingService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private String chat(String prompt) {
        String content = chatClient.prompt().user(prompt).call().content();
        return content != null ? content : "";
    }

    public String generateSummary(String content) {
        String prompt = "You are a helpful assistant that creates concise summaries. Summarize: " + content;
        String response = chat(prompt);
        return response.isBlank() ? "Unable to generate summary" : response;
    }

    public List<String> extractTags(String content) {
        String prompt = "You are a helpful assistant that extracts relevant tags from text. Return only tags separated by commas. Text: " + content;
        String response = chat(prompt);
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String classifySensitivity(String content) {
        String prompt = "You are a document classifier. Classify the sensitivity level as: PUBLIC, INTERNAL, CONFIDENTIAL, or RESTRICTED. Text: " + content;
        String response = chat(prompt).toUpperCase();
        if (response.contains("PUBLIC")) return "PUBLIC";
        if (response.contains("CONFIDENTIAL")) return "CONFIDENTIAL";
        if (response.contains("RESTRICTED")) return "RESTRICTED";
        return "INTERNAL";
    }

    public AiMetadata generateMetadata(String content) {
        String prompt = "Analyze the following document and provide:\n" +
                "1. A brief summary (max 200 words)\n" +
                "2. Relevant tags (comma-separated)\n" +
                "3. Sensitivity classification (PUBLIC, INTERNAL, CONFIDENTIAL, or RESTRICTED)\n\n" +
                "Return ONLY a valid JSON object with keys: summary (string), tags (array), sensitivity (string)\n" +
                "Document:\n" +
                content;

        String raw = chat(prompt);
        logger.info("AI raw response: " + (raw.length() > 200 ? raw.substring(0, 200) : raw));
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        
        try {
            return mapper.readValue(cleaned, AiMetadata.class);
        } catch (Exception e) {
            logger.warn("Failed to parse AI JSON, returning fallback", e);
            return new AiMetadata(
                generateSummary(content),
                extractTags(content),
                classifySensitivity(content)
            );
        }
    }
}
