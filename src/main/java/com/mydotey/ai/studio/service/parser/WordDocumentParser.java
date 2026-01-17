package com.mydotey.ai.studio.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Word 文档解析器 (.docx)
 */
@Component
public class WordDocumentParser implements DocumentParser {

    private static final String DOCX_EXTENSION = ".docx";

    @Override
    public String extractText(InputStream inputStream, String fileName) throws Exception {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(DOCX_EXTENSION);
    }
}
