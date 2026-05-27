package org.example.skillsspringai.framework.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.framework.Agent;
import org.example.skillsspringai.framework.SkillPackage;
import org.example.skillsspringai.tool.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

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
    protected SkillsTool skillsTool;
    protected final List<Object> toolObjects = new ArrayList<>();

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

    public void setSkillsTool(SkillsTool skillsTool) {
        this.skillsTool = skillsTool;
        log.info("技能工具已初始化，共 {} 个工具回调", skillsTool.getToolCallbacks().length);
    }

    public void addToolObject(Object toolObject) {
        this.toolObjects.add(toolObject);
        log.info("添加通用工具: {}", toolObject.getClass().getSimpleName());
    }

    @Override
    public Flux<String> processStream(String message, Map<String, Object> context) {
        if (!isAvailable()) {
            return Flux.just("当前智能体不可用");
        }

        String sessionId = (String) context.get("sessionId");
        Object tools = context.get("tools");

        var prompt = chatClientWithSkills.prompt().user(message);

        String skillsContext = buildSkillsContext();
        if (!skillsContext.isEmpty()) {
            prompt.system(skillsContext);
        }

        if (skillsTool != null) {
            prompt.toolCallbacks(skillsTool.getToolCallbacks());
        }
        if (tools != null) {
            prompt.tools(tools);
        }
        for (Object toolObj : toolObjects) {
            prompt.tools(toolObj);
        }
        if (sessionId != null) {
            prompt.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));
        }

        return prompt.stream().content();
    }

    private String buildSkillsContext() {
        if (skillsTool == null) {
            return "";
        }
        ToolCallback[] callbacks = skillsTool.getToolCallbacks();
        if (callbacks.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你有以下技能(skill)可用。调用技能名称对应的工具获取如何使用该技能的详细指引:\n\n");
        for (ToolCallback callback : callbacks) {
            sb.append("- **").append(callback.getToolDefinition().name())
                    .append("**: ").append(callback.getToolDefinition().description()).append("\n");
        }
        sb.append("\n");
        sb.append("重要: 调用技能工具后会返回该技能的文件列表和使用指引。");
        sb.append("然后你需要使用 readFileInSkill、writeFileInSkill、runInSkill 等工具来完成实际工作。");
        sb.append("不要只重复调用技能工具，而是要按照指引逐步执行。");
        sb.append("回复时直接展示最终结果，不要叙述你的操作过程（如'我先读取文件'、'现在我来编写脚本'等）。\n");
        sb.append("技术约束: writeFileInSkill的content参数有严格的长度限制(约20000字符)。");
        sb.append("不要在一次调用中写入超长脚本！如果内容超过3000字符，必须拆分成多个小文件。");
        return sb.toString();
    }

    public abstract boolean isAvailable();
}
