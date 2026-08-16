package com.xiaosu.memory;

import org.springframework.stereotype.Component;

/** 会话隔离键生成：userId#conversationId（私聊无 conversationId 时用固定后缀） */
@Component
public class ChatSessionService {

    public String sessionKeyOf(String userId, String conversationId) {
        String user = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        String conv = (conversationId == null || conversationId.isBlank()) ? "direct" : conversationId;
        return user + "#" + conv;
    }
}
