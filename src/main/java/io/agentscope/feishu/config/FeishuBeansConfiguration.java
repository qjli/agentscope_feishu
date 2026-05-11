package io.agentscope.feishu.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.event.cardcallback.P2CardActionTriggerHandler;
import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import com.lark.oapi.service.im.ImService.P2MessageReceiveV1Handler;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import io.agentscope.feishu.crm.CrmApiProperties;
import io.agentscope.feishu.lark.FeishuMessageEventService;
import io.agentscope.feishu.lark.PendingApprovalService;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties({FeishuProperties.class, CrmApiProperties.class})
public class FeishuBeansConfiguration {

    @Bean
    public Session agentscopeJsonSession(FeishuProperties properties) {
        String dir = properties.getSessionDir();
        if (dir != null && dir.contains("${user.home}")) {
            dir = dir.replace("${user.home}", System.getProperty("user.home"));
        }
        if (dir == null || dir.isBlank()) {
            dir = Path.of(System.getProperty("user.home"), ".agentscope-feishu", "sessions").toString();
        }
        return new JsonSession(Path.of(dir));
    }

    @Bean
    public Client larkClient(FeishuProperties properties) {
        return Client.newBuilder(properties.getAppId(), properties.getAppSecret()).build();
    }

    @Bean
    public Cache<String, Boolean> processedMessageIds(FeishuProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(60, properties.getIdempotencyTtlSeconds())))
                .maximumSize(500_000)
                .build();
    }

    /**
     * 飞书事件处理线程池：回调线程仅入队，避免超过平台处理时限。
     */
    @Bean(name = "feishuEventExecutor")
    public Executor feishuEventExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("feishu-event-");
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(32);
        ex.setQueueCapacity(2000);
        ex.initialize();
        return ex;
    }

    /**
     * 单会话 Agent 调用串行化（同一 session 不并发进入 ReActAgent）。
     */
    @Bean(name = "sessionAgentExecutor")
    public Executor sessionAgentExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "session-agent");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public EventDispatcher feishuEventDispatcher(
            FeishuProperties properties,
            FeishuMessageEventService messageEventService,
            PendingApprovalService pendingApprovalService) {
        return EventDispatcher.newBuilder(properties.getVerificationToken(), properties.getEncryptKey())
                .onP2MessageReceiveV1(new P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        messageEventService.handleMessageEvent(event);
                    }
                })
                .onP2CardActionTrigger(new P2CardActionTriggerHandler() {
                    @Override
                    public P2CardActionTriggerResponse handle(P2CardActionTrigger event) throws Exception {
                        return pendingApprovalService.handleCardAction(event);
                    }
                })
                .build();
    }
}
