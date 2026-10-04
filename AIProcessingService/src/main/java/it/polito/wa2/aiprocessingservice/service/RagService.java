package it.polito.wa2.aiprocessingservice.service;

import it.polito.wa2.aiprocessingservice.dto.DocumentQueryResponse;
import it.polito.wa2.aiprocessingservice.dto.DocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final TokenTextSplitter textSplitter = new TokenTextSplitter(800, 100, 5, 10000, true);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    public void storeDocumentEmbeddings(
            String fileVersionId,
            String fileId,
            String ownerUserId,
            String filename,
            String contentType,
            String sensitivity,
            String content) {
        
        logger.info("Storing embeddings for fileVersionId={}, filename={}", fileVersionId, filename);

        try {
            ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            TextReader textReader = new TextReader(resource);
            List<Document> documents = textReader.get();
            List<Document> chunks = textSplitter.apply(documents);

            logger.info("Document split into {} chunks", chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                chunk.getMetadata().put("fileVersionId", fileVersionId);
                chunk.getMetadata().put("fileId", fileId);
                chunk.getMetadata().put("ownerUserId", ownerUserId);
                chunk.getMetadata().put("filename", filename);
                chunk.getMetadata().put("contentType", contentType);
                chunk.getMetadata().put("sensitivity", sensitivity);
                chunk.getMetadata().put("chunkIndex", String.valueOf(i));
            }

            vectorStore.add(chunks);
            logger.info("Successfully stored {} chunks for fileVersionId={}", chunks.size(), fileVersionId);

        } catch (Exception e) {
            logger.error("Failed to store embeddings: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public DocumentQueryResponse queryDocuments(
            String question,
            String ownerUserId,
            List<String> accessibleFileIds,
            String sensitivity,
            int topK) {
        
        logger.info("RAG query: question='{}', ownerUserId={}, accessibleFiles={}, topK={}", 
                question, ownerUserId, accessibleFileIds.size(), topK);

        if (accessibleFileIds.isEmpty()) {
            logger.warn("No accessible files for user {}", ownerUserId);
            return new DocumentQueryResponse(
                    "You don't have access to any documents yet. Please upload some files first.",
                    List.of()
            );
        }

        try {
            logger.info("Performing similarity search...");
            List<Document> similarDocuments;
            try {
                similarDocuments = vectorStore.similaritySearch(question);
            } catch (Exception e) {
                logger.error("VectorStore similarity search failed: " + e.getMessage(), e);
                return new DocumentQueryResponse(
                        "Search service is temporarily unavailable. Please ensure Ollama is running with the nomic-embed-text model.",
                        List.of()
                );
            }

            logger.info("Found {} similar documents before filtering", similarDocuments.size());

            if (similarDocuments.isEmpty()) {
                logger.info("No documents found in vector store");
                return new DocumentQueryResponse(
                        "No documents have been indexed yet. Please upload and wait for AI processing to complete.",
                        List.of()
                );
            }

            List<Document> filteredDocuments = similarDocuments.stream()
                    .filter(doc -> {
                        String docFileId = (String) doc.getMetadata().get("fileId");
                        String docSensitivity = (String) doc.getMetadata().get("sensitivity");

                        boolean hasAccess = docFileId != null && accessibleFileIds.contains(docFileId);
                        boolean sensitivityMatch = sensitivity == null || sensitivity.equals(docSensitivity);

                        return hasAccess && sensitivityMatch;
                    })
                    .limit(topK)
                    .collect(Collectors.toList());

            if (filteredDocuments.isEmpty()) {
                logger.warn("No relevant documents found for ownerUserId={} with access to {} files", ownerUserId, accessibleFileIds.size());
                return new DocumentQueryResponse(
                        "No relevant documents found to answer your question.",
                        List.of()
                );
            }

            logger.info("Found {} similar chunks after security filtering", filteredDocuments.size());

            String context = filteredDocuments.stream()
                    .map(doc -> "Document: " + doc.getMetadata().get("filename") + "\n" + (doc.getText() != null ? doc.getText() : ""))
                    .collect(Collectors.joining("\n\n"));

            String ragPrompt = "You are an AI assistant that answers questions based ONLY on the information provided in the documents.\n\n" +
                    "Reference documents:\n" +
                    context + "\n\n" +
                    "User question: " + question + "\n\n" +
                    "Instructions:\n" +
                    "1. Answer ONLY based on the information in the provided documents\n" +
                    "2. If the documents don't contain enough information, state it clearly\n" +
                    "3. Cite the document name when possible\n" +
                    "4. Be concise but complete\n\n" +
                    "Answer:";

            String answer;
            try {
                String response = chatClient.prompt().user(ragPrompt).call().content();
                answer = response != null ? response : "Unable to generate an answer.";
            } catch (Exception e) {
                logger.error("Chat model failed: " + e.getMessage(), e);
                answer = "Unable to generate an answer. The AI service may be temporarily unavailable.";
            }

            List<DocumentSource> sources = java.util.stream.IntStream.range(0, filteredDocuments.size())
                    .mapToObj(index -> {
                        Document doc = filteredDocuments.get(index);
                        String text = doc.getText() != null ? doc.getText() : "";
                        String truncatedText = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                        String fileVersionId = doc.getMetadata().containsKey("fileVersionId") ? (String) doc.getMetadata().get("fileVersionId") : "unknown";
                        String filename = doc.getMetadata().containsKey("filename") ? (String) doc.getMetadata().get("filename") : "unknown";
                        
                        return new DocumentSource(
                                fileVersionId,
                                filename,
                                truncatedText,
                                1.0 - ((double) index / topK)
                        );
                    })
                    .collect(Collectors.toList());

            logger.info("Generated RAG answer with {} sources", sources.size());

            return new DocumentQueryResponse(answer, sources);

        } catch (Exception e) {
            logger.error("Failed to query documents: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
