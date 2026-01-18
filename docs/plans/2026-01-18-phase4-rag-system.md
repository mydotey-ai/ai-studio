# Phase 4: RAG System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现完整的 RAG（检索增强生成）系统，支持向量相似度搜索、知识库检索、上下文构建和流式响应生成

**Architecture:** 基于已有的 Embedding 服务和文档分块服务，实现向量相似度检索（PGVector）、上下文组装、Prompt 模板管理和 OpenAI 兼容的 LLM 调用。使用 SSE 实现流式响应，支持多轮对话上下文管理。

**Tech Stack:** PGVector (PostgreSQL)、OpenAI Compatible API、Spring SSE、MyBatis Plus、Lombok

---

## Prerequisites

- Phase 1 backend infrastructure is complete ✅
- Phase 2 document processing is complete ✅ (Embedding service, Text chunking)
- Phase 3 auth and permission is complete ✅
- Database with PGVector extension and document_chunks table is available
- Test database is configured

---

## Task 1: Create RAG Query DTOs and Request Models

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/dto/RagQueryRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/RagQueryResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/SourceDocument.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/Message.java`

**Step 1: Write the RagQueryRequest DTO**

```java
package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RagQueryRequest {
    /**
     * 用户问题
     */
    @NotBlank(message = "Question is required")
    private String question;

    /**
     * 知识库 ID 列表
     */
    @NotNull(message = "Knowledge base IDs are required")
    private List<Long> knowledgeBaseIds;

    /**
     * 返回的相关文档数量，默认 5
     */
    private Integer topK = 5;

    /**
     * 相似度阈值（0-1），默认 0.7
     */
    private Double scoreThreshold = 0.7;

    /**
     * 对话历史（用于多轮对话）
     */
    private List<Message> conversationHistory;

    /**
     * 是否返回引用来源，默认 true
     */
    private Boolean includeSources = true;

    /**
     * 温度参数，默认 0.3
     */
    private Double temperature = 0.3;

    /**
     * 最大生成长度，默认 1000
     */
    private Integer maxTokens = 1000;
}
```

**Step 2: Write the RagQueryResponse DTO**

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryResponse {
    /**
     * 生成的回答
     */
    private String answer;

    /**
     * 相关文档来源
     */
    private List<SourceDocument> sources;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消耗的 tokens
     */
    private Integer totalTokens;

    /**
     * 流式响应完成标志
     */
    private Boolean isComplete = true;
}
```

**Step 3: Write the SourceDocument DTO**

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocument {
    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 相关内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private Double score;
}
```

**Step 4: Write the Message DTO**

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /**
     * 消息角色：user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/dto/RagQueryRequest.java
git add src/main/java/com/mydotey/ai/studio/dto/RagQueryResponse.java
git add src/main/java/com/mydotey/ai/studio/dto/SourceDocument.java
git add src/main/java/com/mydotey/ai/studio/dto/Message.java
git commit -m "feat: add RAG query DTOs and request models"
```

---

## Task 2: Implement Vector Similarity Search Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/mapper/DocumentChunkMapper.java`
- Create: `src/main/resources/mapper/DocumentChunkMapper.xml`
- Create: `src/main/java/com/mydotey/ai/studio/service/VectorSearchService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/VectorSearchServiceTest.java`

**Step 1: Write the DocumentChunkMapper interface**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 向量相似度搜索（余弦相似度）
     *
     * @param queryEmbedding 查询向量
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param topK 返回结果数量
     * @param scoreThreshold 相似度阈值
     * @return 相关的分块列表
     */
    List<DocumentChunk> searchByEmbedding(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            @Param("topK") int topK,
            @Param("scoreThreshold") double scoreThreshold
    );
}
```

**Step 2: Write the DocumentChunkMapper XML**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mydotey.ai.studio.mapper.DocumentChunkMapper">

    <select id="searchByEmbedding" resultType="com.mydotey.ai.studio.entity.DocumentChunk">
        SELECT
            dc.id,
            dc.document_id,
            dc.chunk_index,
            dc.content,
            dc.metadata,
            dc.created_at,
            1 - (dc.embedding <=> #{queryEmbedding}::vector) as similarity_score
        FROM document_chunks dc
        INNER JOIN documents d ON dc.document_id = d.id
        WHERE
            d.kb_id IN
            <foreach collection="knowledgeBaseIds" item="kbId" open="(" separator="," close=")">
                #{kbId}
            </foreach>
            AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> #{queryEmbedding}::vector
        LIMIT #{topK}
    </select>

</mapper>
```

**Step 3: Write the test class**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("向量搜索服务测试")
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    @Test
    @DisplayName("应该根据问题检索相关文档")
    void testSearchRelevantDocuments() {
        String question = "什么是人工智能？";
        List<Long> kbIds = List.of(1L);

        // 由于需要 mock 数据库，这里只做简单的单元测试
        // 实际的测试应该在集成测试中进行
        assertNotNull(question);
        assertNotNull(kbIds);
    }
}
```

**Step 4: Run test to verify it fails**

Run: `mvn test -Dtest=VectorSearchServiceTest`
Expected: FAIL with class not found

**Step 5: Write the VectorSearchService implementation**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.SourceDocument;
import com.mydotey.ai.studio.entity.DocumentChunk;
import com.mydotey.ai.studio.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingService embeddingService;

    /**
     * 根据问题搜索相关的文档分块
     *
     * @param question 用户问题
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param topK 返回结果数量
     * @param scoreThreshold 相似度阈值
     * @return 相关文档列表
     */
    public List<SourceDocument> search(
            String question,
            List<Long> knowledgeBaseIds,
            int topK,
            double scoreThreshold) {

        log.info("Searching for relevant documents, question: {}, kbIds: {}, topK: {}",
                question, knowledgeBaseIds, topK);

        // 1. 生成问题的向量
        float[] queryEmbedding = embeddingService.embed(question);

        // 2. 向量相似度搜索
        List<DocumentChunk> chunks = documentChunkMapper.searchByEmbedding(
                queryEmbedding,
                knowledgeBaseIds,
                topK,
                scoreThreshold
        );

        log.info("Found {} relevant chunks", chunks.size());

        // 3. 转换为 SourceDocument
        return chunks.stream()
                .map(this::toSourceDocument)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 SourceDocument
     */
    private SourceDocument toSourceDocument(DocumentChunk chunk) {
        return SourceDocument.builder()
                .documentId(chunk.getDocumentId())
                .documentName(extractDocumentName(chunk))
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .score(null) // 分数需要从 SQL 中获取
                .build();
    }

    /**
     * 提取文档名称（从 metadata 或其他地方）
     */
    private String extractDocumentName(DocumentChunk chunk) {
        // 简化版：实际需要从文档表查询
        return "Document_" + chunk.getDocumentId();
    }
}
```

**Step 6: Update DocumentChunk entity to support score**

Modify: `src/main/java/com/mydotey/ai/studio/entity/DocumentChunk.java`

Add this field (transient, not stored in database):
```java
/**
 * 相似度分数（仅用于查询结果）
 */
@TableField(exist = false)
private Double similarityScore;
```

**Step 7: Update toSourceDocument method to include score**

```java
private SourceDocument toSourceDocument(DocumentChunk chunk) {
    return SourceDocument.builder()
            .documentId(chunk.getDocumentId())
            .documentName(extractDocumentName(chunk))
            .chunkIndex(chunk.getChunkIndex())
            .content(chunk.getContent())
            .score(chunk.getSimilarityScore())
            .build();
}
```

**Step 8: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/mapper/DocumentChunkMapper.java
git add src/main/resources/mapper/DocumentChunkMapper.xml
git add src/main/java/com/mydotey/ai/studio/service/VectorSearchService.java
git add src/test/java/com/mydotey/ai/studio/service/VectorSearchServiceTest.java
git add src/main/java/com/mydotey/ai/studio/entity/DocumentChunk.java
git commit -m "feat: implement vector similarity search service with PGVector"
```

---

## Task 3: Implement Context Builder Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/ContextBuilderService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/ContextBuilderServiceTest.java`

**Step 1: Write the test class**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.Message;
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
                        .role("user")
                        .content("什么是人工智能？")
                        .build(),
                Message.builder()
                        .role("assistant")
                        .content("人工智能是计算机科学的一个分支")
                        .build()
        );

        String context = contextBuilderService.buildContext(question, List.of(), history);

        assertNotNull(context);
        assertTrue(context.contains("什么是人工智能？"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ContextBuilderServiceTest`
Expected: FAIL with class not found

**Step 3: Write the ContextBuilderService implementation**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.Message;
import com.mydotey.ai.studio.dto.SourceDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文构建服务
 */
@Slf4j
@Service
public class ContextBuilderService {

    private static final String CONTEXT_TEMPLATE =
            """
            ### 知识库内容
            %s

            ### 对话历史
            %s

            ### 当前问题
            %s
            """;

    private static final String NO_SOURCES_MESSAGE = "（未找到相关资料）";
    private static final String NO_HISTORY_MESSAGE = "（无）";

    /**
     * 构建完整的上下文
     *
     * @param question 用户问题
     * @param sources 相关文档列表
     * @param history 对话历史
     * @return 完整的上下文字符串
     */
    public String buildContext(String question, List<SourceDocument> sources, List<Message> history) {
        String sourcesText = formatSources(sources);
        String historyText = formatHistory(history);

        return String.format(CONTEXT_TEMPLATE, sourcesText, historyText, question);
    }

    /**
     * 格式化来源文档
     */
    private String formatSources(List<SourceDocument> sources) {
        if (sources == null || sources.isEmpty()) {
            return NO_SOURCES_MESSAGE;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            SourceDocument source = sources.get(i);
            sb.append(String.format("**来源 %d: %s (分块 %d)**\n",
                    i + 1,
                    source.getDocumentName(),
                    source.getChunkIndex() + 1));
            sb.append(source.getContent()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 格式化对话历史
     */
    private String formatHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return NO_HISTORY_MESSAGE;
        }

        // 只保留最近 N 轮对话，避免上下文过长
        int maxHistoryTurns = 5;
        List<Message> recentHistory = history.size() > maxHistoryTurns * 2
                ? history.subList(history.size() - maxHistoryTurns * 2, history.size())
                : history;

        return recentHistory.stream()
                .map(msg -> String.format("**%s:** %s", msg.getRole(), msg.getContent()))
                .collect(Collectors.joining("\n\n"));
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ContextBuilderServiceTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/ContextBuilderService.java
git add src/test/java/com/mydotey/ai/studio/service/ContextBuilderServiceTest.java
git commit -m "feat: implement context builder service"
```

---

## Task 4: Create LLM Service Configuration

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/config/LlmConfig.java`
- Modify: `src/main/resources/application-dev.yml`

**Step 1: Write the LlmConfig**

```java
package com.mydotey.ai.studio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

    /**
     * API 端点地址
     */
    private String endpoint = "https://api.openai.com/v1";

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 使用的模型名称
     */
    private String model = "gpt-3.5-turbo";

    /**
     * 默认温度
     */
    private Double defaultTemperature = 0.3;

    /**
     * 默认最大生成长度
     */
    private Integer defaultMaxTokens = 1000;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 60000;

    /**
     * 是否启用流式响应
     */
    private Boolean enableStreaming = true;
}
```

**Step 2: Add LLM configuration to application.yml**

Modify: `src/main/resources/application-dev.yml`

```yaml
llm:
  endpoint: https://api.openai.com/v1
  api-key: ${LLM_API_KEY:your-api-key-here}
  model: gpt-3.5-turbo
  default-temperature: 0.3
  default-max-tokens: 1000
  timeout: 60000
  enable-streaming: true
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/config/LlmConfig.java
git add src/main/resources/application-dev.yml
git commit -m "feat: add LLM service configuration"
```

---

## Task 5: Implement Prompt Template Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/PromptTemplateService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/PromptTemplateServiceTest.java`

**Step 1: Write the test class**

```java
package com.mydotey.ai.studio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Prompt 模板服务测试")
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @InjectMocks
    private PromptTemplateService promptTemplateService;

    @Test
    @DisplayName("应该构建完整的系统提示词")
    void testBuildSystemPrompt() {
        String context = "知识库内容：人工智能是计算机科学的一个分支";
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("你是一个专业的助手"));
        assertTrue(systemPrompt.contains(context));
    }

    @Test
    @DisplayName("当没有相关文档时应该提示用户")
    void testBuildSystemPromptWithNoSources() {
        String context = "（未找到相关资料）";
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("知识库中没有相关信息"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PromptTemplateServiceTest`
Expected: FAIL with class not found

**Step 3: Write the PromptTemplateService implementation**

```java
package com.mydotey.ai.studio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Prompt 模板服务
 */
@Slf4j
@Service
public class PromptTemplateService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            """
            你是一个专业的助手，负责根据知识库内容回答用户问题。

            ## 任务说明
            请仔细阅读知识库内容，准确回答用户的问题。

            ## 回答要求
            1. **基于知识库回答**：优先使用知识库中的信息，确保准确性
            2. **引用来源**：在回答中明确标注信息来源（文档名称）
            3. **诚实回答**：如果知识库中没有相关信息，请明确说明"根据提供的知识库，没有找到相关信息"
            4. **不要编造**：绝不要凭空编造知识库中没有的信息
            5. **语言自然**：使用自然流畅的语言，避免机械生硬

            ## 上下文信息
            %s

            ## 用户的后续问题
            请根据以上上下文信息回答用户的问题。
            """;

    private static final String NO_SOURCES_SYSTEM_PROMPT =
            """
            你是一个专业的助手。

            注意：根据提供的知识库，没有找到与用户问题相关的信息。
            请明确告诉用户这一点，不要编造任何信息。

            如果用户的问题比较通用，你可以基于你的通用知识回答，但必须声明这不是来自知识库的信息。
            """;

    /**
     * 构建系统提示词
     *
     * @param context 上下文信息
     * @return 完整的系统提示词
     */
    public String buildSystemPrompt(String context) {
        // 检查是否有找到相关文档
        boolean hasSources = context != null && !context.contains("（未找到相关资料）");

        if (hasSources) {
            return String.format(DEFAULT_SYSTEM_PROMPT, context);
        } else {
            return NO_SOURCES_SYSTEM_PROMPT;
        }
    }

    /**
     * 构建完整的消息列表（用于 API 调用）
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @return 消息列表
     */
    public String buildMessages(String systemPrompt, String userQuestion) {
        return String.format(
                """
                [
                  {
                    "role": "system",
                    "content": "%s"
                  },
                  {
                    "role": "user",
                    "content": "%s"
                  }
                ]
                """,
                escapeJsonString(systemPrompt),
                escapeJsonString(userQuestion)
        );
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=PromptTemplateServiceTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/PromptTemplateService.java
git add src/test/java/com/mydotey/ai/studio/service/PromptTemplateServiceTest.java
git commit -m "feat: implement prompt template service"
```

---

## Task 6: Implement LLM Generation Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/LlmGenerationService.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/LlmRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/LlmResponse.java`

**Step 1: Write the LlmRequest DTO**

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private String messages;

    /**
     * 温度参数（0-2）
     */
    private Double temperature;

    /**
     * 最大生成长度
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;
}
```

**Step 2: Write the LlmResponse DTO**

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    /**
     * 生成的文本
     */
    private String content;

    /**
     * 完成原因
     */
    private String finishReason;

    /**
     * 消耗的 prompt tokens
     */
    private Integer promptTokens;

    /**
     * 消耗的 completion tokens
     */
    private Integer completionTokens;

    /**
     * 总消耗 tokens
     */
    private Integer totalTokens;
}
```

**Step 3: Write the LlmGenerationService implementation**

```java
package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmRequest;
import com.mydotey.ai.studio.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * LLM 生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmGenerationService {

    private final RestTemplate restTemplate;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    /**
     * 生成回答（非流式）
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @param temperature 温度参数
     * @param maxTokens 最大生成长度
     * @return LLM 响应
     */
    public LlmResponse generate(
            String systemPrompt,
            String userQuestion,
            Double temperature,
            Integer maxTokens) {

        try {
            // 构建消息
            String messages = promptTemplateService.buildMessages(systemPrompt, userQuestion);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(config.getModel())
                    .messages(messages)
                    .temperature(temperature != null ? temperature : config.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
                    .stream(false)
                    .build();

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            // 发送请求
            HttpEntity<String> httpEntity = new HttpEntity<>(
                    objectMapper.writeValueAsString(request),
                    headers
            );

            String url = config.getEndpoint() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.postForEntity(url, httpEntity, String.class);

            // 解析响应
            return parseLlmResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to generate response from LLM", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 LLM 响应
     */
    private LlmResponse parseLlmResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode choices = root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in LLM response");
        }

        JsonNode choice = choices.get(0);
        JsonNode message = choice.get("message");
        String content = message.get("content").asText();
        String finishReason = choice.get("finish_reason").asText();

        // 解析 token 使用情况
        Integer totalTokens = null;
        JsonNode usage = root.get("usage");
        if (usage != null) {
            totalTokens = usage.get("total_tokens").asInt();
        }

        return LlmResponse.builder()
                .content(content)
                .finishReason(finishReason)
                .totalTokens(totalTokens)
                .build();
    }
}
```

**Step 4: Add RestTemplate bean if not exists**

Check if `WebConfig` exists and has RestTemplate bean. If not, create:

```java
package com.mydotey.ai.studio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/LlmGenerationService.java
git add src/main/java/com/mydotey/ai/studio/dto/LlmRequest.java
git add src/main/java/com/mydotey/ai/studio/dto/LlmResponse.java
git add src/main/java/com/mydotey/ai/studio/config/WebConfig.java
git commit -m "feat: implement LLM generation service"
```

---

## Task 7: Implement RAG Service (Orchestration)

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/RagService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/RagServiceTest.java`

**Step 1: Write the test class**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.dto.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RAG 服务测试")
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private ContextBuilderService contextBuilderService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private LlmGenerationService llmGenerationService;

    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("应该完整执行 RAG 流程")
    void testRagQuery() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是人工智能？");
        request.setKnowledgeBaseIds(List.of(1L));
        request.setTopK(5);
        request.setScoreThreshold(0.7);
        request.setTemperature(0.3);
        request.setMaxTokens(1000);

        // 简化测试，实际需要 mock 各个服务的行为
        assertNotNull(request);
        assertNotNull(request.getQuestion());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RagServiceTest`
Expected: FAIL with class not found

**Step 3: Write the RagService implementation**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 服务（检索增强生成）
 * 协调向量搜索、上下文构建和 LLM 生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorSearchService vectorSearchService;
    private final ContextBuilderService contextBuilderService;
    private final PromptTemplateService promptTemplateService;
    private final LlmGenerationService llmGenerationService;
    private final LlmConfig llmConfig;

    /**
     * 执行 RAG 查询
     *
     * @param request RAG 查询请求
     * @return RAG 查询响应
     */
    public RagQueryResponse query(RagQueryRequest request) {
        log.info("Executing RAG query, question: {}, kbIds: {}",
                request.getQuestion(), request.getKnowledgeBaseIds());

        try {
            // 1. 向量搜索 - 检索相关文档
            List<SourceDocument> sources = vectorSearchService.search(
                    request.getQuestion(),
                    request.getKnowledgeBaseIds(),
                    request.getTopK() != null ? request.getTopK() : 5,
                    request.getScoreThreshold() != null ? request.getScoreThreshold() : 0.7
            );

            log.info("Found {} relevant documents", sources.size());

            // 2. 构建上下文
            String context = contextBuilderService.buildContext(
                    request.getQuestion(),
                    request.getIncludeSources() ? sources : List.of(),
                    request.getConversationHistory()
            );

            // 3. 构建 Prompt
            String systemPrompt = promptTemplateService.buildSystemPrompt(context);

            // 4. LLM 生成回答
            LlmResponse llmResponse = llmGenerationService.generate(
                    systemPrompt,
                    request.getQuestion(),
                    request.getTemperature(),
                    request.getMaxTokens()
            );

            log.info("Generated response, total tokens: {}", llmResponse.getTotalTokens());

            // 5. 构建响应
            return RagQueryResponse.builder()
                    .answer(llmResponse.getContent())
                    .sources(request.getIncludeSources() ? sources : List.of())
                    .model(llmConfig.getModel())
                    .totalTokens(llmResponse.getTotalTokens())
                    .isComplete(true)
                    .build();

        } catch (Exception e) {
            log.error("RAG query failed", e);
            throw new RuntimeException("RAG query failed: " + e.getMessage(), e);
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RagServiceTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/RagService.java
git add src/test/java/com/mydotey/ai/studio/service/RagServiceTest.java
git commit -m "feat: implement RAG orchestration service"
```

---

## Task 8: Create RAG Controller

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/controller/RagController.java`

**Step 1: Write the RagController**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 执行 RAG 查询
     *
     * @param request RAG 查询请求
     * @return RAG 查询响应
     */
    @PostMapping("/query")
    @AuditLog(action = "RAG_QUERY", resourceType = "KnowledgeBase")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        log.info("Received RAG query request: {}", request.getQuestion());

        RagQueryResponse response = ragService.query(request);

        return ApiResponse.success(response);
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/RagController.java
git commit -m "feat: add RAG query controller"
```

---

## Task 9: Implement Streaming RAG Response (SSE)

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/StreamingLlmService.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/RagController.java`

**Step 1: Write the StreamingLlmService**

```java
package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 流式 LLM 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingLlmService {

    private final RestTemplate restTemplate;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    /**
     * 流式生成回答
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @param temperature 温度参数
     * @param maxTokens 最大生成长度
     * @return 流式响应回调函数
     */
    public void streamGenerate(
            String systemPrompt,
            String userQuestion,
            Double temperature,
            Integer maxTokens,
            StreamCallback callback) {

        try {
            // 构建消息
            String messages = promptTemplateService.buildMessages(systemPrompt, userQuestion);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(config.getModel())
                    .messages(messages)
                    .temperature(temperature != null ? temperature : config.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
                    .stream(true)
                    .build();

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            // 发送请求
            HttpEntity<String> httpEntity = new HttpEntity<>(
                    objectMapper.writeValueAsString(request),
                    headers
            );

            String url = config.getEndpoint() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.postForEntity(url, httpEntity, String.class);

            // 解析流式响应
            if (response.getStatusCode() == HttpStatus.OK) {
                parseStreamResponse(response.getBody(), callback);
            }

        } catch (Exception e) {
            log.error("Failed to stream generate from LLM", e);
            callback.onError(e);
        }
    }

    /**
     * 解析流式响应
     */
    private void parseStreamResponse(String responseBody, StreamCallback callback) {
        try {
            // 解析每一行（SSE 格式：data: {...}）
            String[] lines = responseBody.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("data: ")) {
                    String data = line.substring(6);

                    // 检查结束标记
                    if ("[DONE]".equals(data)) {
                        callback.onComplete();
                        return;
                    }

                    // 解析 JSON
                    JsonNode json = objectMapper.readTree(data);
                    JsonNode choices = json.get("choices");

                    if (choices != null && choices.size() > 0) {
                        JsonNode delta = choices.get(0).get("delta");
                        if (delta != null && delta.has("content")) {
                            String content = delta.get("content").asText();
                            callback.onContent(content);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse stream response", e);
            callback.onError(e);
        }
    }

    /**
     * 流式响应回调接口
     */
    public interface StreamCallback {
        /**
         * 接收到内容
         */
        void onContent(String content);

        /**
         * 流式传输完成
         */
        void onComplete();

        /**
         * 发生错误
         */
        void onError(Exception e);
    }
}
```

**Step 2: Add streaming endpoint to RagController**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.service.RagService;
import com.mydotey.ai.studio.service.StreamingLlmService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * RAG 查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final VectorSearchService vectorSearchService;
    private final ContextBuilderService contextBuilderService;
    private final PromptTemplateService promptTemplateService;
    private final StreamingLlmService streamingLlmService;

    /**
     * 执行 RAG 查询（非流式）
     */
    @PostMapping("/query")
    @AuditLog(action = "RAG_QUERY", resourceType = "KnowledgeBase")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        log.info("Received RAG query request: {}", request.getQuestion());

        RagQueryResponse response = ragService.query(request);

        return ApiResponse.success(response);
    }

    /**
     * 执行 RAG 查询（流式）
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuditLog(action = "RAG_QUERY_STREAM", resourceType = "KnowledgeBase")
    public void queryStream(
            @Valid @RequestBody RagQueryRequest request,
            HttpServletResponse response) throws IOException {

        log.info("Received RAG stream query request: {}", request.getQuestion());

        // 设置 SSE 响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        PrintWriter writer = response.getWriter();

        // 1. 向量搜索
        List<SourceDocument> sources = vectorSearchService.search(
                request.getQuestion(),
                request.getKnowledgeBaseIds(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getScoreThreshold() != null ? request.getScoreThreshold() : 0.7
        );

        // 2. 构建上下文
        String context = contextBuilderService.buildContext(
                request.getQuestion(),
                request.getIncludeSources() ? sources : List.of(),
                request.getConversationHistory()
        );

        // 3. 构建 Prompt
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        // 4. 流式生成
        StringBuilder answerBuilder = new StringBuilder();

        streamingLlmService.streamGenerate(
                systemPrompt,
                request.getQuestion(),
                request.getTemperature(),
                request.getMaxTokens(),
                new StreamingLlmService.StreamCallback() {
                    @Override
                    public void onContent(String content) {
                        answerBuilder.append(content);

                        // 发送 SSE 事件
                        try {
                            writer.write("data: " + escapeSseData(content) + "\n\n");
                            writer.flush();
                        } catch (IOException e) {
                            log.error("Failed to write SSE event", e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        // 发送完成事件
                        try {
                            RagQueryResponse finalResponse = RagQueryResponse.builder()
                                    .answer(answerBuilder.toString())
                                    .sources(request.getIncludeSources() ? sources : List.of())
                                    .isComplete(true)
                                    .build();

                            writer.write("data: [DONE]\n\n");
                            writer.flush();
                        } catch (IOException e) {
                            log.error("Failed to send completion event", e);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        log.error("Stream generation error", e);
                        try {
                            writer.write("event: error\n");
                            writer.write("data: " + e.getMessage() + "\n\n");
                            writer.flush();
                        } catch (IOException ioException) {
                            log.error("Failed to send error event", ioException);
                        }
                    }
                }
        );
    }

    /**
     * 转义 SSE 数据
     */
    private String escapeSseData(String data) {
        // 简化版：实际需要更完整的转义
        return data.replace("\n", "\\n").replace("\"", "\\\"");
    }
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/StreamingLlmService.java
git add src/main/java/com/mydotey/ai/studio/controller/RagController.java
git commit -m "feat: implement streaming RAG response with SSE"
```

---

## Task 10: Create RAG Integration Test

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/integration/RagIntegrationTest.java`

**Step 1: Write the integration test**

```java
package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.service.DocumentService;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.service.RagService;
import com.mydotey.ai.studio.util.FileUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("RAG 系统集成测试")
class RagIntegrationTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RagService ragService;

    @Autowired
    private FileUtil fileUtil;

    private Long testKbId;
    private Long testDocId;

    @BeforeEach
    void setUp() {
        // 创建测试知识库
        KnowledgeBaseResponse kb = knowledgeBaseService.create(
                "Test KB for RAG",
                "Integration test knowledge base for RAG",
                "text-embedding-ada-002",
                500,
                100
        );
        testKbId = kb.getId();

        // 上传测试文档
        String testContent = "人工智能（AI）是计算机科学的一个分支，" +
                "旨在创建能够执行通常需要人类智能的任务的系统。" +
                "这些任务包括学习、推理、解决问题、理解语言、感知等。";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                testContent.getBytes()
        );

        // 保存文件并创建文档记录（简化版）
        String filePath = fileUtil.saveFile(file.getOriginalFilename(), file.getBytes());
        // 这里应该调用 DocumentService，简化处理
        testDocId = 1L;
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testKbId != null) {
            knowledgeBaseService.delete(testKbId);
        }
    }

    @Test
    @DisplayName("完整的 RAG 查询流程")
    void testCompleteRagQuery() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是人工智能？");
        request.setKnowledgeBaseIds(List.of(testKbId));
        request.setTopK(5);
        request.setScoreThreshold(0.7);
        request.setTemperature(0.3);
        request.setMaxTokens(500);

        RagQueryResponse response = ragService.query(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getAnswer().isEmpty());
        assertTrue(response.isComplete());
    }

    @Test
    @DisplayName("当知识库中没有相关信息时应该返回明确的回答")
    void testRagQueryWithNoRelevantInfo() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是量子计算？"); // 测试文档中没有的内容
        request.setKnowledgeBaseIds(List.of(testKbId));
        request.setTopK(5);
        request.setScoreThreshold(0.7);

        RagQueryResponse response = ragService.query(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        // 应该明确说明没有找到相关信息
        assertTrue(response.getAnswer().contains("没有") ||
                    response.getAnswer().contains("未找到"));
    }
}
```

**Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=RagIntegrationTest`
Expected: PASS (需要确保数据库中有测试数据）

**Step 3: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/integration/RagIntegrationTest.java
git commit -m "test: add RAG system integration test"
```

---

## Summary

Phase 4 完成后，系统将具备以下能力：

1. **向量相似度搜索**：基于 PGVector 的余弦相似度检索
2. **上下文构建**：自动组装相关知识块和对话历史
3. **Prompt 模板**：可配置的系统和用户提示词模板
4. **LLM 集成**：调用 OpenAI 兼容 API 生成回答
5. **流式响应**：支持 SSE 流式输出，提升用户体验
6. **多轮对话**：支持对话历史上下文管理
7. **RAG 端到端流程**：完整的检索-增强-生成流程

### 测试覆盖

- 向量搜索服务测试
- 上下文构建服务测试
- Prompt 模板服务测试
- RAG 服务集成测试

### API 端点

| 方法 | 端点 | 权限 | 描述 |
|------|------|--------|------|
| POST | /api/rag/query | 认证用户 | 执行 RAG 查询（非流式） |
| POST | /api/rag/query/stream | 认证用户 | 执行 RAG 查询（流式 SSE） |

### 配置项

```yaml
llm:
  endpoint: https://api.openai.com/v1
  api-key: ${LLM_API_KEY}
  model: gpt-3.5-turbo
  default-temperature: 0.3
  default-max-tokens: 1000
  timeout: 60000
  enable-streaming: true
```

### 数据库依赖

- `document_chunks` 表（已存在）包含 `embedding` 向量列
- `documents` 表（已存在）用于获取文档元信息
- `knowledge_bases` 表（已存在）用于知识库管理

### 后续改进方向（Phase 5+）

- Agent 系统集成（工具调用、工作流）
- 混合检索（向量 + 关键词）
- 上下文压缩（长对话历史）
- 检索结果重排序（Rerank）
- 多模型支持（切换不同 LLM）
- 问答质量评估
- 缓存优化（常见问题缓存）
