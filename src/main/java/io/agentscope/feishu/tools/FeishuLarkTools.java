package io.agentscope.feishu.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.enums.CreateMessageReceiveIdTypeEnum;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.GetChatReq;
import com.lark.oapi.service.im.v1.model.GetChatResp;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.stereotype.Component;

/**
 * 将飞书 Open API 封装为 Agent 可调用的异步工具。
 */
@Component
public class FeishuLarkTools {

    private static final Logger log = LoggerFactory.getLogger(FeishuLarkTools.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final Client client;

    public FeishuLarkTools(Client client) {
        this.client = client;
    }

    @Tool(
            name = "feishu_ping",
            description = "连通性检查：返回预设标记，用于演示 preset 参数（对模型不可见）。")
    public Mono<String> feishuPing(@ToolParam(name = "marker", description = "服务端注入的标记") String marker) {
        return Mono.just("feishu tools ok, marker=" + marker);
    }

    @Tool(description = "获取当前飞书会话（群或单聊）的名称与描述等元数据。")
    public Mono<String> getCurrentChatMetadata(FeishuToolContext ctx) {
        return Mono.fromCallable(() -> {
                    GetChatReq req = GetChatReq.newBuilder().chatId(ctx.chatId()).build();
                    GetChatResp resp = client.im().chat().get(req);
                    if (!resp.success()) {
                        return "get_chat 失败: " + resp.getCode() + " " + resp.getMsg();
                    }
                    var data = resp.getData();
                    if (data == null) {
                        return "无数据";
                    }
                    return "name=" + data.getName() + ", description=" + data.getDescription()
                            + ", chatMode=" + data.getChatMode() + ", chatType=" + data.getChatType();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("getCurrentChatMetadata", e))
                .onErrorResume(e -> Mono.just("错误: " + e.getMessage()));
    }

    @Tool(description = "向当前飞书会话发送一条额外的纯文本说明消息（用于向用户同步进度或结论）。")
    public Mono<String> sendFollowUpTextToCurrentChat(
            @ToolParam(name = "text", description = "要发送的纯文本，建议简短") String text, FeishuToolContext ctx) {
        return Mono.fromCallable(() -> {
                    String body = OM.createObjectNode().put("text", text).toString();
                    CreateMessageReq req = CreateMessageReq.newBuilder()
                            .receiveIdType(CreateMessageReceiveIdTypeEnum.CHAT_ID)
                            .createMessageReqBody(CreateMessageReqBody.newBuilder()
                                    .receiveId(ctx.chatId())
                                    .msgType("text")
                                    .content(body)
                                    .build())
                            .build();
                    var resp = client.im().message().create(req);
                    if (!resp.success()) {
                        return "send 失败: " + resp.getCode() + " " + resp.getMsg();
                    }
                    return "已发送 follow-up 消息";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("sendFollowUpTextToCurrentChat", e))
                .onErrorResume(e -> Mono.just("错误: " + e.getMessage()));
    }
}
