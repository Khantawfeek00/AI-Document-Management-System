package it.polito.wa2.aiprocessingservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class FileUploadListener {
    private final Logger logger = LoggerFactory.getLogger(FileUploadListener.class);
    private final AiPipelineService aiPipelineService;
    private final ObjectMapper objectMapper;

    public FileUploadListener(AiPipelineService aiPipelineService, ObjectMapper objectMapper) {
        this.aiPipelineService = aiPipelineService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {"file-upload.completed", "file-upload.completed.public.files_outbox_events"},
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFileUpload(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        
        logger.info("Received Kafka message on topic={}, offset={}", topic, offset);
        logger.debug("Raw message: {}", message);

        try {
            FileUploadEventDTO event = parseMessage(message);

            if (event == null) {
                logger.warn("Could not parse message as FileUploadEventDTO");
                return;
            }

            logger.info("Parsed event: fileVersionId={}, filename={}, contentType={}",
                    event.getFileVersionId(), event.getFilename(), event.getContentType());

            if (event.getContentType() == null || (!event.getContentType().startsWith("text/") && !event.getContentType().equalsIgnoreCase("application/pdf"))) {
                logger.info("Skipping non-text fileVersionId={} with contentType={}",
                        event.getFileVersionId(), event.getContentType());
                return;
            }

            aiPipelineService.process(event);

        } catch (Exception ex) {
            logger.error("Error processing Kafka message: {}", ex.getMessage(), ex);
        }
    }

    private FileUploadEventDTO parseMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            if (jsonNode.has("payload") && jsonNode.get("payload").isTextual()) {
                String payloadStr = jsonNode.get("payload").asText();
                return objectMapper.readValue(payloadStr, FileUploadEventDTO.class);
            } else if (jsonNode.has("payload") && jsonNode.get("payload").isObject()) {
                return objectMapper.treeToValue(jsonNode.get("payload"), FileUploadEventDTO.class);
            } else {
                return objectMapper.readValue(message, FileUploadEventDTO.class);
            }
        } catch (Exception e) {
            logger.error("Failed to parse message: {}", e.getMessage());
            return null;
        }
    }
}
