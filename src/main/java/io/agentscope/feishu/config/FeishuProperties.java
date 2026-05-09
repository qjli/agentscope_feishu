package io.agentscope.feishu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentscope.feishu")
public class FeishuProperties {

    /**
     * 飞书自建应用 App ID。
     */
    private String appId = "";

    /**
     * 飞书自建应用 App Secret。
     */
    private String appSecret = "";

    /**
     * 事件订阅 Verification Token。
     */
    private String verificationToken = "";

    /**
     * Encrypt Key（启用加密时必填）。
     */
    private String encryptKey = "";

    /**
     * AgentScope 会话文件目录（JsonSession）。
     */
    /** 留空则使用 {@code ~/.agentscope-feishu/sessions}。 */
    private String sessionDir = "";

    /**
     * 已处理 message_id 去重缓存过期时间（秒）。
     */
    private int idempotencyTtlSeconds = 600;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getEncryptKey() {
        return encryptKey;
    }

    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public String getSessionDir() {
        return sessionDir;
    }

    public void setSessionDir(String sessionDir) {
        this.sessionDir = sessionDir;
    }

    public int getIdempotencyTtlSeconds() {
        return idempotencyTtlSeconds;
    }

    public void setIdempotencyTtlSeconds(int idempotencyTtlSeconds) {
        this.idempotencyTtlSeconds = idempotencyTtlSeconds;
    }
}
