package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.Message;
import com.mydotey.ai.studio.dto.MessageRole;
import com.mydotey.ai.studio.dto.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("上下文构建服务测试")
@ExtendWith(MockitoExtension.class)
class ContextBuilderServiceTest {

    @InjectMocks
    private ContextBuilderService contextBuilderService;

    @Test
    @DisplayName("应该构建包含相关文档的上下文")
    void testBuildContextWithSources() {
        String question = "什么是人工智能？";
        List<SourceDocument> sources = List.of(
                SourceDocument.builder()
                        .documentId(1L)
                        .documentName("AI简介.pdf")
                        .chunkIndex(0)
                        .content("人工智能是计算机科学的一个分支")
                        .score(0.95)
                        .build()
        );

        String context = contextBuilderService.buildContext(question, sources, null);

        assertNotNull(context);
        assertTrue(context.contains("AI简介.pdf"));
        assertTrue(context.contains("人工智能是计算机科学的一个分支"));
    }

    @Test
    @DisplayName("应该包含对话历史上下文")
    void testBuildContextWithHistory() {
        String question = "它有什么应用？";
        List<Message> history = List.of(
                Message.builder()
                        .role(MessageRole.USER)
                        .content("什么是人工智能？")
                        .build(),
                Message.builder()
                        .role(MessageRole.ASSISTANT)
                        .content("人工智能是计算机科学的一个分支")
                        .build()
        );

        String context = contextBuilderService.buildContext(question, List.of(), history);

        assertNotNull(context);
        assertTrue(context.contains("什么是人工智能？"));
    }
}
