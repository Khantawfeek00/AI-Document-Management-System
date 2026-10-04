package it.polito.wa2.aiprocessingservice.dto;

import java.util.List;

public class DocumentQueryResponse {
    private String answer;
    private List<DocumentSource> sources;

    public DocumentQueryResponse() {}

    public DocumentQueryResponse(String answer, List<DocumentSource> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<DocumentSource> getSources() { return sources; }
    public void setSources(List<DocumentSource> sources) { this.sources = sources; }
}
