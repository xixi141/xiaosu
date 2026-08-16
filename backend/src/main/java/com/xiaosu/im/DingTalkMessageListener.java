package com.xiaosu.im;

import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 机器人消息回调：同步快速 ACK，业务处理委托异步 processor
 * （防止大模型生成耗时导致回调超时与消息堆积）。
 */
@Component
@Slf4j
public class DingTalkMessageListener implements OpenDingTalkCallbackListener<ChatbotMessage, ChatbotMessage> {

    private final DingTalkMessageProcessor processor;

    public DingTalkMessageListener(DingTalkMessageProcessor processor) {
        this.processor = processor;
    }

    @Override
    public ChatbotMessage execute(ChatbotMessage message) {
        String text = (message.getText() == null || message.getText().getContent() == null)
                ? "" : message.getText().getContent().trim();
        if (text.isEmpty()) {
            return message;
        }
        String sender = message.getSenderStaffId();
        String conversation = message.getConversationId();
        log.info("收到钉钉消息: sender={} conversation={} text={}", sender, conversation, text);
        processor.process(sender, conversation, text, message.getSessionWebhook());
        return message;
    }
}
