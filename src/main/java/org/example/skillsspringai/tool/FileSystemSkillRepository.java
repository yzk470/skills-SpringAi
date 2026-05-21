package org.example.skillsspringai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemSkillRepository {

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final Map<String, Skill> skillCache = new ConcurrentHashMap<>();

    // 从路径加载所有 SKILL.md
    public int loadFromPath(String pattern) {
        int count = 0;
        try {
            Resource[] resources = resourceResolver.getResources(pattern);
            for (Resource resource : resources) {
                String content = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
                Skill skill = SkillParser.parse(content);

                if (skill != null) {
                    skillCache.put(skill.getName(), skill);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("加载技能失败：{}", pattern, e);
        }
        return count;
    }

    // 获取技能
    public Skill getSkill(String name) {
        return skillCache.get(name);
    }

    // 获取所有技能
    public Map<String, Skill> getAllSkills() {
        return skillCache;
    }
}
