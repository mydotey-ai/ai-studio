package com.mydotey.ai.studio.service.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Word 文档解析器测试")
class WordDocumentParserTest {

    private final WordDocumentParser parser = new WordDocumentParser();

    @Test
    @DisplayName("应该支持 .docx 文件")
    void testSupportsDocxFile() {
        assertTrue(parser.supports("document.docx"));
        assertTrue(parser.supports("DOCUMENT.DOCX"));
    }

    @Test
    @DisplayName("不应该支持非 Word 文件")
    void testDoesNotSupportNonWordFiles() {
        assertFalse(parser.supports("document.pdf"));
        assertFalse(parser.supports("document.txt"));
    }
}
