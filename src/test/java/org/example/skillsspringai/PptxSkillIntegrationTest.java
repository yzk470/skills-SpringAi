package org.example.skillsspringai;

import org.example.skillsspringai.entity.Skill;
import org.example.skillsspringai.tool.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class PptxSkillIntegrationTest {

    @Test
    void shouldLoadPptxSkillAndInvokeCallback() {
        // 1. 从 classpath 加载 pptx 技能
        ClasspathMarkdownSkillSource source = new ClasspathMarkdownSkillSource("classpath:skills.ppt/**/*SKILL.md");
        var skills = source.load();

        assertThat(skills).hasSize(1);
        Skill pptxSkill = skills.get(0);
        assertThat(pptxSkill.getName()).isEqualTo("pptx");
        assertThat(pptxSkill.getDescription()).contains("pptx");
        assertThat(pptxSkill.getType()).isEqualTo(Skill.SkillType.TEXT);

        // 2. 注册到 SkillRegistry
        SkillRegistry registry = new SkillRegistry();
        registry.register(pptxSkill);

        // 3. 构建 SkillsTool
        SkillsTool tool = SkillsTool.builder().registry(registry).build();
        ToolCallback[] callbacks = tool.getToolCallbacks();

        assertThat(callbacks).hasSize(1);

        // 4. 调用技能回调（模拟 AI 调用工具）
        String result = callbacks[0].call("{}");

        // 5. 验证技能返回了正确的指令内容
        assertThat(result).contains("PPTX Skill");
        assertThat(result).contains("python");
        assertThat(result).contains("pptxgenjs");
        assertThat(result).contains("Converting to Images");
    }

    @Test
    void shouldLoadAllSkillsTogether() {
        SkillRegistry registry = new SkillRegistry();

        var financialSkills = new ClasspathMarkdownSkillSource("classpath:skills/**/*SKILL.md").load();
        var pptxSkills = new ClasspathMarkdownSkillSource("classpath:skills.ppt/**/*SKILL.md").load();

        registry.registerAll(financialSkills);
        registry.registerAll(pptxSkills);

        assertThat(registry.count()).isEqualTo(4);

        // 验证所有技能都存在
        assertThat(registry.get("pptx")).isNotNull();
        assertThat(registry.get("technical-analysis")).isNotNull();
        assertThat(registry.get("risk-calculator")).isNotNull();
        assertThat(registry.get("portfolio-optimizer")).isNotNull();

        // 构建工具回调
        SkillsTool tool = SkillsTool.builder().registry(registry).build();
        ToolCallback[] callbacks = tool.getToolCallbacks();

        assertThat(callbacks).hasSize(4);

        // 调用 pptx 技能
        ToolCallback pptxCallback = java.util.Arrays.stream(callbacks)
                .filter(c -> c.getToolDefinition().name().equals("pptx"))
                .findFirst()
                .orElseThrow();

        String response = pptxCallback.call("{}");
        assertThat(response).contains("PPTX Skill");
        assertThat(response).contains("pip install");
    }
}
