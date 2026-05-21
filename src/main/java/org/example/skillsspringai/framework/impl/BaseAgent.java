package org.example.skillsspringai.framework.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.framework.Agent;
import org.example.skillsspringai.framework.SkillPackage;
import org.example.skillsspringai.tool.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public abstract class BaseAgent implements Agent {

    protected String id;
    protected String name;
    protected String description;
    protected List<SkillPackage> skillPackages = new ArrayList<>();
    protected ChatClient chatClientWithSkills;
    protected File skillsDirectory;

    public BaseAgent(String name, String desc, ChatClient chatClient, ChatClient.Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = desc;

        if (builder != null) {
            this.chatClientWithSkills = builder.defaultSystem(desc).build();
        } else {
            this.chatClientWithSkills = chatClient;
        }
    }

    public void setSkillsDirectory(File skillsDirectory) {
        this.skillsDirectory = skillsDirectory;
        try {
            var skillsTool = SkillsTool.builder()
                    .addSkillsDirectory(skillsDirectory.getAbsolutePath())
                    .build();

            this.chatClientWithSkills = chatClientWithSkills.mutate()
                    .defaultSystem(description)
                    .defaultTools(skillsTool)
                    .build();

        } catch (Exception e) {
            log.error("初始化SkillsTool失败", e);
        }
    }

    @Override
    public Flux<String> processStream(String message, Map<String, Object> context) {
        if (!isAvailable()) {
            return Flux.just("当前智能体不可用");
        }

        String sessionId = (String) context.get("sessionId");
        Object tools = context.get("tools");

        var prompt = chatClientWithSkills.prompt().user(message);

        if (tools != null) {
            prompt.tools(tools);
        }
        if (sessionId != null) {
            prompt.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));
        }

        return prompt.stream().content();
    }

    public abstract boolean isAvailable();
}
