package it.polito.wa2.aiprocessingservice.controller;

import it.polito.wa2.aiprocessingservice.dto.DocumentQueryRequest;
import it.polito.wa2.aiprocessingservice.dto.DocumentQueryResponse;
import it.polito.wa2.aiprocessingservice.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {
    private final Logger logger = LoggerFactory.getLogger(RagController.class);
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<DocumentQueryResponse> queryDocuments(@RequestBody DocumentQueryRequest request) {
        logger.info("RAG query received - ownerUserId={}, accessibleFiles={}, question='{}'", 
            request.getOwnerUserId(), 
            request.getAccessibleFileIds() != null ? request.getAccessibleFileIds().size() : 0, 
            request.getQuestion());

        DocumentQueryResponse response = ragService.queryDocuments(
            request.getQuestion(),
            request.getOwnerUserId(),
            request.getAccessibleFileIds(),
            request.getSensitivity(),
            request.getTopK()
        );

        return ResponseEntity.ok(response);
    }
}
