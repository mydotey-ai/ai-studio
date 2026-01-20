package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.chatbot.*;
import com.mydotey.ai.studio.service.ChatService;
import com.mydotey.ai.studio.service.ChatbotService;
import com.mydotey.ai.studio.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chatbots")
@RequiredArgsConstructor
@Tag(name = "聊天机器人", description = "聊天机器人和对话管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ConversationService conversationService;
    private final ChatService chatService;

    /**
     * 创建聊天机器人
     */
    @PostMapping
    @AuditLog(action = "CHATBOT_CREATE", resourceType = "Chatbot")
    @Operation(summary = "创建聊天机器人", description = "创建新的聊天机器人")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<ChatbotResponse> create(
            @Valid @RequestBody CreateChatbotRequest request,
            @RequestAttribute("userId") Long userId) {
        ChatbotResponse response = chatbotService.create(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取聊天机器人详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取聊天机器人详情", description = "获取指定聊天机器人的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "聊天机器人不存在")
    public ApiResponse<ChatbotResponse> getById(@PathVariable Long id) {
        ChatbotResponse response = chatbotService.getById(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取我的聊天机器人列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的聊天机器人列表", description = "获取当前用户创建的所有聊天机器人")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<ChatbotResponse>> getMyChatbots(
            @RequestAttribute("userId") Long userId) {
        List<ChatbotResponse> response = chatbotService.getByOwner(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取已发布的聊天机器人列表
     */
    @GetMapping("/published")
    @Operation(summary = "获取已发布的聊天机器人列表", description = "获取所有已公开发布的聊天机器人")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<ChatbotResponse>> getPublishedChatbots() {
        List<ChatbotResponse> response = chatbotService.getPublishedChatbots();
        return ApiResponse.success(response);
    }

    /**
     * 更新聊天机器人
     */
    @PutMapping("/{id}")
    @AuditLog(action = "CHATBOT_UPDATE", resourceType = "Chatbot", resourceIdParam = "id")
    @Operation(summary = "更新聊天机器人", description = "更新聊天机器人信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "聊天机器人不存在")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChatbotRequest request,
            @RequestAttribute("userId") Long userId) {
        chatbotService.update(id, request, userId);
        return ApiResponse.<Void>success("Chatbot updated successfully", null);
    }

    /**
     * 删除聊天机器人
     */
    @DeleteMapping("/{id}")
    @AuditLog(action = "CHATBOT_DELETE", resourceType = "Chatbot", resourceIdParam = "id")
    @Operation(summary = "删除聊天机器人", description = "删除指定的聊天机器人")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "聊天机器人不存在")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        chatbotService.delete(id, userId);
        return ApiResponse.<Void>success("Chatbot deleted successfully", null);
    }

    /**
     * 获取对话列表
     */
    @GetMapping("/{chatbotId}/conversations")
    @Operation(summary = "获取对话列表", description = "获取指定聊天机器人的所有对话记录")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "聊天机器人不存在")
    public ApiResponse<List<ConversationResponse>> getConversations(
            @PathVariable Long chatbotId,
            @RequestAttribute("userId") Long userId) {
        List<ConversationResponse> response = conversationService.getByChatbotAndUser(chatbotId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取对话详情
     */
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "获取对话详情", description = "获取指定对话的详细信息和消息历史")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "对话不存在")
    public ApiResponse<ConversationResponse> getConversation(@PathVariable Long conversationId) {
        ConversationResponse response = conversationService.getById(conversationId);
        return ApiResponse.success(response);
    }

    /**
     * 创建新对话
     */
    @PostMapping("/{chatbotId}/conversations")
    @Operation(summary = "创建新对话", description = "为指定聊天机器人创建新的对话会话")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "聊天机器人不存在")
    public ApiResponse<ConversationResponse> createConversation(
            @PathVariable Long chatbotId,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        ConversationResponse response = conversationService.create(chatbotId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 删除对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除对话", description = "删除指定的对话及其所有消息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "对话不存在")
    public ApiResponse<Void> deleteConversation(@PathVariable Long conversationId) {
        conversationService.delete(conversationId);
        return ApiResponse.<Void>success("Conversation deleted successfully", null);
    }

    /**
     * 发送消息（非流式）
     */
    @PostMapping("/chat")
    @AuditLog(action = "CHATBOT_CHAT", resourceType = "Chatbot")
    @Operation(summary = "发送消息", description = "向聊天机器人发送消息并获取回复")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        ChatResponse response = chatService.chat(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 发送消息（流式）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuditLog(action = "CHATBOT_CHAT_STREAM", resourceType = "Chatbot")
    @Operation(summary = "发送消息（流式）", description = "向聊天机器人发送消息并以流式方式获取回复")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public void chatStream(
            @Valid @RequestBody ChatRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletResponse response) throws IOException {

        log.info("Received stream chat request, chatbot: {}", request.getChatbotId());

        // 设置 SSE 响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        PrintWriter writer = response.getWriter();

        // 简化版：直接调用非流式接口
        // 实际实现需要通过 AgentExecutionService 的流式接口
        ChatResponse chatResponse = chatService.chat(request, userId);

        // 发送完整响应
        writer.write("data: " + escapeSseData(chatResponse.getAnswer()) + "\n\n");
        writer.write("data: [DONE]\n\n");
        writer.flush();
    }

    /**
     * 转义 SSE 数据
     */
    private String escapeSseData(String data) {
        if (data == null) {
            return "";
        }
        return data.replace("\n", "\\n").replace("\"", "\\\"");
    }
}
