package com.xiaosu.im;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.xiaosu.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 钉钉 Stream 模式长连接（WebSocket 出站，无需公网 IP）。
 * 仅在 xiaosu.dingtalk.enabled=true 时启动，本地开发无凭证时应用照常运行。
 */
@Component
@ConditionalOnProperty(prefix = "xiaosu.dingtalk", name = "enabled", havingValue = "true")
@Slf4j
public class DingTalkClientStarter {

    private final AppProperties props;
    private final DingTalkMessageListener listener;
    private OpenDingTalkClient client;

    public DingTalkClientStarter(AppProperties props, DingTalkMessageListener listener) {
        this.props = props;
        this.listener = listener;
    }

    @PostConstruct
    public void start() throws Exception {
        client = OpenDingTalkStreamClientBuilder.custom()
                .credential(new AuthClientCredential(props.dingtalk().clientId(), props.dingtalk().clientSecret()))
                .registerCallbackListener("/v1.0/im/bot/messages/get", listener)
                .build();
        client.start();
        log.info("钉钉 Stream 长连接已启动");
    }

    @PreDestroy
    public void stop() throws Exception {
        if (client != null) {
            client.stop();
        }
    }
}
