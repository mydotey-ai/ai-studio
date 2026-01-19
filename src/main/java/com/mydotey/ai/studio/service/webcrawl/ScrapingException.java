package com.mydotey.ai.studio.service.webcrawl;

public class ScrapingException extends Exception {
    public ScrapingException(String message) {
        super(message);
    }

    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
