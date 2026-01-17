package com.mydotey.ai.studio.service.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PDF 文档解析器测试")
class PdfDocumentParserTest {

    private final PdfDocumentParser parser = new PdfDocumentParser();

    @Test
    @DisplayName("应该支持 .pdf 文件")
    void testSupportsPdfFile() {
        assertTrue(parser.supports("document.pdf"));
        assertTrue(parser.supports("DOCUMENT.PDF"));
    }

    @Test
    @DisplayName("不应该支持非 PDF 文件")
    void testDoesNotSupportNonPdfFiles() {
        assertFalse(parser.supports("document.docx"));
        assertFalse(parser.supports("document.txt"));
    }
}
