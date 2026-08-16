package com.xiaosu.im;

import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 机器人消息回调：本任务先 echo 验证链路（Task 18 接业务处理）。
 * 注意：同步快速返回（SDK 回调即 ACK），业务处理在 Task 18 改为异步。
 */
@Component
@Slf4j
public class DingTalkMessageListener implements OpenDingTalkCallbackListener<ChatbotMessage, ChatbotMessage> {

    private final DingTalkReplyService replyService;

    public DingTalkMessageListener(DingTalkReplyService replyService) {
        this.replyService = replyService;
    }

    @Override
    public ChatbotMessage execute(ChatbotMessage message) {
        String text = (message.getText() == null || message.getText().getContent() == null)
                ? "" : message.getText().getContent();
        log.info("收到钉钉消息: sender={} conversation={} text={}",
                message.getSenderStaffId(), message.getConversationId(), text);
        if (!text.isBlank()) {
            replyService.sendMarkdown(message.getSessionWebhook(), "小苏",
                    "收到你的消息（echo 冒烟）：\n\n> " + text);
        }
        return message;
    }
}
