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
    public String formatSources(List<SourceDocument> sources) {
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
    public String formatHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return NO_HISTORY_MESSAGE;
        }

        // 只保留最近 N 轮对话，避免上下文过长
        int maxHistoryTurns = 5;
        List<Message> recentHistory = history.size() > maxHistoryTurns * 2
                ? history.subList(history.size() - maxHistoryTurns * 2, history.size())
                : history;

        return recentHistory.stream()
                .map(msg -> String.format("**%s:** %s", msg.getRole().getValue(), msg.getContent()))
                .collect(Collectors.joining("\n\n"));
    }
}
