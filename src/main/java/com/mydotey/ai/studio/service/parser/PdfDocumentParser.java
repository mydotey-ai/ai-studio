package com.mydotey.ai.studio.service.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * PDF 文档解析器
 */
@Component
public class PdfDocumentParser implements DocumentParser {

    private static final String PDF_EXTENSION = ".pdf";

    @Override
    public String extractText(InputStream inputStream, String fileName) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(PDF_EXTENSION);
    }
}
