package it.polito.wa2.aiprocessingservice.service;

import java.util.List;

public class AiMetadata {
    private String summary;
    private List<String> tags;
    private String sensitivity;

    public AiMetadata() {}

    public AiMetadata(String summary, List<String> tags, String sensitivity) {
        this.summary = summary;
        this.tags = tags;
        this.sensitivity = sensitivity;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }
}
