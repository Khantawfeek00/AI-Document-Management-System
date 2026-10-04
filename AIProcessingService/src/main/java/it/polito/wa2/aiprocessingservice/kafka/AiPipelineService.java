package it.polito.wa2.aiprocessingservice.kafka;

import it.polito.wa2.aiprocessingservice.clients.ServiceAccountTokenProvider;
import it.polito.wa2.aiprocessingservice.service.AiMetadata;
import it.polito.wa2.aiprocessingservice.service.AiProcessingService;
import it.polito.wa2.aiprocessingservice.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AiPipelineService {
    private final Logger logger = LoggerFactory.getLogger(AiPipelineService.class);

    private final RestTemplate restTemplate;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String fileServiceUrl;
    private final AiProcessingService aiProcessingService;
    private final RagService ragService;

    public AiPipelineService(
            RestTemplate restTemplate,
            ServiceAccountTokenProvider tokenProvider,
            @Value("${file.service.url}") String fileServiceUrl,
            AiProcessingService aiProcessingService,
            RagService ragService) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.fileServiceUrl = fileServiceUrl;
        this.aiProcessingService = aiProcessingService;
        this.ragService = ragService;
    }

    public void process(FileUploadEventDTO event) {
        String token = tokenProvider.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        if (isAlreadyProcessed(UUID.fromString(event.getFileVersionId()), headers)) {
            logger.info("FileVersion {} already processed. Skipping AI pipeline.", event.getFileVersionId());
            return;
        }

        logger.info("Starting AI pipeline for fileVersionId={}", event.getFileVersionId());

        byte[] contentBytes = downloadFileContent(event.getFileVersionId(), headers);
        String contentText = extractText(contentBytes, event.getContentType());

        AiMetadata aiMetadata = aiProcessingService.generateMetadata(contentText);

        saveMetadata(event.getFileVersionId(), aiMetadata, headers);

        try {
            ragService.storeDocumentEmbeddings(
                    event.getFileVersionId(),
                    event.getFileId(),
                    event.getOwnerUserId(),
                    event.getFilename(),
                    event.getContentType(),
                    aiMetadata.getSensitivity(),
                    contentText
            );
            logger.info("Embeddings successfully generated for fileVersionId={}", event.getFileVersionId());
        } catch (Exception embeddingError) {
            logger.error("Error while generating embeddings for fileVersionId={}", event.getFileVersionId(), embeddingError);
        }
    }

    private String extractText(byte[] contentBytes, String contentType) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(contentBytes);
            org.springframework.ai.reader.pdf.PagePdfDocumentReader pdfReader = new org.springframework.ai.reader.pdf.PagePdfDocumentReader(resource);
            java.util.List<org.springframework.ai.document.Document> docs = pdfReader.get();
            StringBuilder sb = new StringBuilder();
            for (org.springframework.ai.document.Document doc : docs) {
                sb.append(doc.getText()).append("\n");
            }
            return sb.toString();
        }
        return new String(contentBytes, StandardCharsets.UTF_8);
    }

    private byte[] downloadFileContent(String fileVersionId, HttpHeaders headers) {
        String downloadUri = fileServiceUrl + "/api/v1/files/" + fileVersionId + "/download";
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUri,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        byte[] contentBytes = response.getBody();
        if (contentBytes == null) {
            throw new RuntimeException("Downloaded file is empty");
        }

        logger.info("File content downloaded for fileVersionId={}", fileVersionId);
        return contentBytes;
    }

    private void saveMetadata(String fileVersionId, AiMetadata aiMetadata, HttpHeaders headers) {
        String metadataUri = fileServiceUrl + "/api/v1/files/" + fileVersionId + "/ai-metadata";
        HttpEntity<AiMetadata> request = new HttpEntity<>(aiMetadata, headers);

        restTemplate.postForEntity(metadataUri, request, Void.class);
        logger.info("AI metadata saved for fileVersionId={}", fileVersionId);
    }

    private boolean isAlreadyProcessed(UUID fileVersionId, HttpHeaders headers) {
        String statusUri = fileServiceUrl + "/api/v1/files/" + fileVersionId + "/ai-status";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<AiProcessingStatus> response = restTemplate.exchange(
                    statusUri,
                    HttpMethod.GET,
                    entity,
                    AiProcessingStatus.class
            );
            return response.getBody() == AiProcessingStatus.COMPLETED;
        } catch (Exception ex) {
            return false;
        }
    }
}
