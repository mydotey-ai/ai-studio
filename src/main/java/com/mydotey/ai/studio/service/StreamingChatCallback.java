package com.mydotey.ai.studio.service;

/**
 * 流式聊天回调接口
 */
public interface StreamingChatCallback {
    /**
     * 接收到内容片段
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
