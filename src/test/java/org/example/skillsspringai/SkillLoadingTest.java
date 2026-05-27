package org.example.skillsspringai;

import org.example.skillsspringai.entity.Skill;
import org.example.skillsspringai.tool.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLoadingTest {

    @Test
    void shouldLoadPptxSkillFromClasspath() {
        ClasspathMarkdownSkillSource source = new ClasspathMarkdownSkillSource("classpath:skills.ppt/**/*SKILL.md");
        var skills = source.load();

        assertThat(skills).isNotEmpty();
        assertThat(skills.get(0).getName()).isEqualTo("pptx");
        assertThat(skills.get(0).getDescription()).contains("pptx");
        assertThat(skills.get(0).getType()).isEqualTo(Skill.SkillType.TEXT);
        assertThat(skills.get(0).getInstructions()).contains("PPTX Skill");
    }

    @Test
    void shouldLoadFinancialSkillsFromClasspath() {
        ClasspathMarkdownSkillSource source = new ClasspathMarkdownSkillSource("classpath:skills/**/*SKILL.md");
        var skills = source.load();

        assertThat(skills).hasSize(3);
        assertThat(skills.stream().map(Skill::getName))
                .containsExactlyInAnyOrder("technical-analysis", "risk-calculator", "portfolio-optimizer");
    }

    @Test
    void shouldCreateTextToolCallbackAndInvoke() {
        Skill skill = Skill.builder()
                .name("pptx")
                .description("PPTX creation and editing skill")
                .instructions("## PPTX Skill\n\nCreate presentations using python scripts.")
                .fullContent("---\nname: pptx\ndescription: PPTX skill\n---\n\n## PPTX Skill\n\nCreate presentations.")
                .type(Skill.SkillType.TEXT)
                .build();

        SkillRegistry registry = new SkillRegistry();
        registry.register(skill);

        SkillsTool tool = SkillsTool.builder().registry(registry).build();
        ToolCallback[] callbacks = tool.getToolCallbacks();

        assertThat(callbacks).hasSize(1);
        assertThat(callbacks[0].getToolDefinition().name()).isEqualTo("pptx");
        assertThat(callbacks[0].getToolDefinition().description()).contains("PPTX");

        String result = callbacks[0].call("{}");
        assertThat(result).contains("PPTX Skill");
        assertThat(result).contains("Create presentations");
    }

    @Test
    void shouldCreateRegistryAndRegisterAllSources() {
        SkillRegistry registry = new SkillRegistry();

        ClasspathMarkdownSkillSource classpathSource =
                new ClasspathMarkdownSkillSource("classpath:skills/**/*SKILL.md");
        ClasspathMarkdownSkillSource pptxSource =
                new ClasspathMarkdownSkillSource("classpath:skills.ppt/**/*SKILL.md");

        registry.registerAll(classpathSource.load());
        registry.registerAll(pptxSource.load());

        assertThat(registry.count()).isEqualTo(4);

        Skill pptxSkill = registry.get("pptx");
        assertThat(pptxSkill).isNotNull();
        assertThat(pptxSkill.getName()).isEqualTo("pptx");
        assertThat(pptxSkill.getType()).isEqualTo(Skill.SkillType.TEXT);
    }

    @Test
    void shouldParseSkillJsonDescriptor() throws Exception {
        String json = """
                {
                    "name": "weather-tools",
                    "description": "Weather query tools",
                    "version": "1.0.0",
                    "mainClass": "com.example.weather.WeatherTools",
                    "toolMethodNames": ["getWeather", "getForecast"]
                }
                """;

        Skill skill = SkillParser.parsePackageDescriptor(
                new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertThat(skill).isNotNull();
        assertThat(skill.getName()).isEqualTo("weather-tools");
        assertThat(skill.getType()).isEqualTo(Skill.SkillType.PACKAGE);
        assertThat(skill.getMainClass()).isEqualTo("com.example.weather.WeatherTools");
        assertThat(skill.getToolMethodNames()).containsExactly("getWeather", "getForecast");
        assertThat(skill.getVersion()).isEqualTo("1.0.0");
    }
}
