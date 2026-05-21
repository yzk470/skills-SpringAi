package org.example.skillsspringai.tool;


import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class SkillsTool implements ToolCallbackProvider {

    private final List<String> skillsDirectories;

    private SkillsTool(List<String> skillsDirectories) {
        this.skillsDirectories = skillsDirectories != null ? skillsDirectories : new ArrayList<>();
    }

    public static SkillsToolBuilder builder() {
        return new SkillsToolBuilder();
    }

    public static class SkillsToolBuilder {
        private final List<String> skillsDirectories = new ArrayList<>();

        public SkillsToolBuilder addSkillsDirectory(String directory) {
            this.skillsDirectories.add(directory);
            return this;
        }

        public SkillsTool build() {
            return new SkillsTool(this.skillsDirectories);
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (String dir : skillsDirectories) {
            try {
                loadSkillsFromDirectory(dir, callbacks);
            } catch (Exception e) {
                log.error("加载技能目录失败: {}", dir, e);
            }
        }

        return callbacks.toArray(new ToolCallback[0]);
    }

    private void loadSkillsFromDirectory(String directory, List<ToolCallback> callbacks) throws Exception {
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            log.warn("技能目录不存在: {}", directory);
            return;
        }

        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("SKILL.md"))
                    .forEach(skillFile -> {
                        try {
                            String content = Files.readString(skillFile);
                            Skill skill = SkillParser.parse(content);

                            if (skill != null) {
                                ToolCallback callback = FunctionToolCallback.builder(
                                                skill.getName(),
                                                (java.util.function.Function<Object, String>) input -> skill.getInstructions()
                                        )
                                        .description(skill.getDescription())
                                        .build();
                                callbacks.add(callback);
                                log.info("加载技能: {}", skill.getName());
                            }
                        } catch (Exception e) {
                            log.error("加载技能文件失败: {}", skillFile, e);
                        }
                    });
        }
    }
}



