package it.polito.wa2.aiprocessingservice.dto;

import java.util.List;

public class DocumentQueryRequest {
    private String ownerUserId;
    private List<String> accessibleFileIds;
    private String question;
    private String sensitivity;
    private int topK = 5;

    public DocumentQueryRequest() {}

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public List<String> getAccessibleFileIds() { return accessibleFileIds; }
    public void setAccessibleFileIds(List<String> accessibleFileIds) { this.accessibleFileIds = accessibleFileIds; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
