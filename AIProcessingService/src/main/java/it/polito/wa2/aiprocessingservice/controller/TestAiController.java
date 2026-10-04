package it.polito.wa2.aiprocessingservice.controller;

import it.polito.wa2.aiprocessingservice.service.AiMetadata;
import it.polito.wa2.aiprocessingservice.service.AiProcessingService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestAiController {
    private final AiProcessingService aiProcessingService;

    public TestAiController(AiProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @GetMapping("/chat")
    public Map<String, String> testChat(@RequestParam("message") String message) {
        String summary = aiProcessingService.generateSummary(message);
        Map<String, String> response = new HashMap<>();
        response.put("input", message);
        response.put("response", summary);
        return response;
    }

    @PostMapping("/summary")
    public Map<String, String> testSummary(@RequestBody TestRequest request) {
        String summary = aiProcessingService.generateSummary(request.getContent());
        Map<String, String> response = new HashMap<>();
        response.put("summary", summary);
        return response;
    }

    @PostMapping("/tags")
    public Map<String, List<String>> testTags(@RequestBody TestRequest request) {
        List<String> tags = aiProcessingService.extractTags(request.getContent());
        Map<String, List<String>> response = new HashMap<>();
        response.put("tags", tags);
        return response;
    }

    @PostMapping("/sensitivity")
    public Map<String, String> testSensitivity(@RequestBody TestRequest request) {
        String sensitivity = aiProcessingService.classifySensitivity(request.getContent());
        Map<String, String> response = new HashMap<>();
        response.put("sensitivity", sensitivity);
        return response;
    }

    @PostMapping("/metadata")
    public Map<String, Object> testMetadata(@RequestBody TestRequest request) {
        AiMetadata metadata = aiProcessingService.generateMetadata(request.getContent());
        Map<String, Object> response = new HashMap<>();
        response.put("summary", metadata.getSummary());
        response.put("tags", metadata.getTags());
        response.put("sensitivity", metadata.getSensitivity());
        return response;
    }
}

class TestRequest {
    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
