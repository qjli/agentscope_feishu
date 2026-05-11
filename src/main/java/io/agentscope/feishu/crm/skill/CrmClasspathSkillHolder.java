package io.agentscope.feishu.crm.skill;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 从 classpath {@code skills/} 加载 {@code feishu_crm} 技能包（与 AgentScope {@link ClasspathSkillRepository} 约定一致）。
 */
@Component
public class CrmClasspathSkillHolder {

    private static final Logger log = LoggerFactory.getLogger(CrmClasspathSkillHolder.class);
    private static final String SKILL_ID = "feishu_crm";
    private static final String CLASSPATH_ROOT = "skills";

    private volatile AgentSkill feishuCrmSkill;

    @PostConstruct
    void load() throws IOException {
        try (ClasspathSkillRepository repo = new ClasspathSkillRepository(CLASSPATH_ROOT)) {
            if (!repo.skillExists(SKILL_ID)) {
                throw new IllegalStateException(
                        "classpath 下未找到技能 '"
                                + SKILL_ID
                                + "'，请检查 src/main/resources/"
                                + CLASSPATH_ROOT
                                + "/"
                                + SKILL_ID
                                + "/SKILL.md");
            }
            this.feishuCrmSkill = repo.getSkill(SKILL_ID);
        }
        log.info("已从 classpath 加载 AgentSkill: {}", SKILL_ID);
    }

    public AgentSkill feishuCrmSkill() {
        AgentSkill s = feishuCrmSkill;
        if (s == null) {
            throw new IllegalStateException("CRM 技能尚未完成加载");
        }
        return s;
    }
}
