package com.mydotey.ai.studio.service.webcrawl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedResult {
    private String url;
    private String title;
    private String content;
    private List<String> links;
}
