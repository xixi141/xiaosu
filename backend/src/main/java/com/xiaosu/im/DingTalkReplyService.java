package com.xiaosu.im;

import com.dingtalk.open.app.api.chatbot.BotReplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 通过会话级 webhook 回复钉钉 Markdown 消息（SDK 内置 BotReplier） */
@Service
@Slf4j
public class DingTalkReplyService {

    public void sendMarkdown(String sessionWebhook, String title, String markdownText) {
        if (sessionWebhook == null || sessionWebhook.isBlank()) {
            log.warn("sessionWebhook 为空，无法回复");
            return;
        }
        try {
            BotReplier.fromWebhook(sessionWebhook).replyMarkdown(title, markdownText);
        } catch (Exception e) {
            log.error("钉钉回复失败: {}", e.getMessage());
        }
    }
}
