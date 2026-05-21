package org.example.skillsspringai.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkillLoaderService {

    private final FileSystemSkillRepository repository;

    @PostConstruct
    public void loadAllSkills() {
        // 1. 加载内置技能（只读）
        int classpathCount = repository.loadFromPath("classpath:skills/**/*SKILL.md");

        // 2. 加载外部技能（可覆盖）
        File dynamicDir = new File("data/skills");
        int fileCount = 0;
        if (dynamicDir.exists()) {
            fileCount = repository.loadFromPath("file:data/skills/**/*SKILL.md");
        }

        log.info("技能加载完成：内置{}个，外部{}个", classpathCount, fileCount);
    }

    public Optional<Skill> findByName(String skillName) {
        return Optional.ofNullable(repository.getSkill(skillName));
    }
}
