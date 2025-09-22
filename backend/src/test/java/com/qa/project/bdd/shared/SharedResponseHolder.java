package com.qa.project.bdd.shared;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class SharedResponseHolder {
    private ResponseEntity<String> lastResponse;
    
    public void setLastResponse(ResponseEntity<String> response) {
        this.lastResponse = response;
    }
    
    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }
    
    public String getLastResponseBody() {
        return lastResponse != null ? lastResponse.getBody() : null;
    }
    
    public int getLastResponseStatus() {
        return lastResponse != null ? lastResponse.getStatusCode().value() : 0;
    }
    
    public void clear() {
        lastResponse = null;
    }
}