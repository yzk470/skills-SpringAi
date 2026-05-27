package org.example.skillsspringai.agent;

import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.framework.SkillPackage;
import org.example.skillsspringai.framework.impl.BaseAgent;
import org.example.skillsspringai.tool.SkillFileReader;
import org.example.skillsspringai.tool.SkillLoaderService;
import org.example.skillsspringai.tool.SkillScriptRunner;
import org.example.skillsspringai.tool.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SuperFinancialAdvisorAgent extends BaseAgent {

    public SuperFinancialAdvisorAgent(
            ChatClient.Builder chatClientBuilder,
            SkillLoaderService skillLoaderService,
            SkillFileReader skillFileReader,
            SkillScriptRunner skillScriptRunner) {
        super(
                "全能金融顾问大师",
                "你是一个专业助手。回复时直接给出结果，不要叙述你的内部步骤。不要输出思考过程。始终用中文回复，保持简洁专业。" +
                "生成PPT时：优先使用技能包中已有的.pptx模板+Python脚本(unpack→编辑XML→clean→pack)，不要从零编写超长JS脚本。" +
                "writeFileInSkill禁止写入超过3000字符的内容，长脚本必须拆分为多个小文件。",
                chatClientBuilder.build(),
                chatClientBuilder
        );

        SkillsTool skillsTool = SkillsTool.builder()
                .registry(skillLoaderService.getRegistry())
                .build();
        setSkillsTool(skillsTool);

        addToolObject(skillFileReader);
        addToolObject(skillScriptRunner);

        log.info("全能金融顾问大师已初始化 (技能: {}, 通用工具: {})",
                skillsTool.getToolCallbacks().length, toolObjects.size());
    }

    @Override
    public void addSkillPackage(SkillPackage skillPackage) {
        this.skillPackages.add(skillPackage);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
