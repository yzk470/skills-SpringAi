package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class FileSystemMarkdownSkillSource implements SkillSource {

    private final String basePath;

    public FileSystemMarkdownSkillSource(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public List<Skill> load() {
        List<Skill> skills = new ArrayList<>();

        Path dirPath = Paths.get(basePath);
        if (!Files.exists(dirPath)) {
            log.info("技能目录不存在，已跳过: {}", basePath);
            return skills;
        }

        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("SKILL.md") || p.toString().endsWith(".SKILL.md"))
                    .forEach(skillFile -> {
                        try {
                            String content = Files.readString(skillFile, StandardCharsets.UTF_8);
                            Skill skill = SkillParser.parse(content);
                            if (skill != null) {
                                skill.setResourcePath(skillFile.getParent().toString());
                                skill.setPackagePath(skillFile.getParent().toString());
                                skills.add(skill);
                                log.debug("从文件系统加载技能: {}", skill.getName());
                            }
                        } catch (Exception e) {
                            log.error("加载技能文件失败: {}", skillFile, e);
                        }
                    });
            log.info("文件系统加载完成: {} 个技能 (path: {})", skills.size(), basePath);
        } catch (Exception e) {
            log.error("文件系统技能加载失败: {}", basePath, e);
        }

        return skills;
    }
}
