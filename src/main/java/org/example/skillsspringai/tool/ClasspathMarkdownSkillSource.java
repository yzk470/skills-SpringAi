package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClasspathMarkdownSkillSource implements SkillSource {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final String pattern;

    public ClasspathMarkdownSkillSource(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public List<Skill> load() {
        List<Skill> skills = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Skill skill = SkillParser.parse(content);
                    if (skill != null) {
                        try {
                            Path parent = Paths.get(resource.getURI()).getParent();
                            if (parent != null) {
                                skill.setResourcePath(parent.toString());
                                skill.setPackagePath(parent.toString());
                            }
                        } catch (Exception ignored) {
                            skill.setResourcePath(extractClasspathDir(resource));
                        }
                        skills.add(skill);
                        log.debug("从 classpath 加载技能: {}", skill.getName());
                    }
                }
            }
            log.info("classpath 加载完成: {} 个技能 (pattern: {})", skills.size(), pattern);
        } catch (Exception e) {
            log.error("classpath 技能加载失败: {}", pattern, e);
        }
        return skills;
    }

    private String extractClasspathDir(Resource resource) {
        // 从 pattern 中提取基础目录，例如 classpath:skills.ppt/**/*SKILL.md → skills.ppt
        String p = pattern;
        int colon = p.indexOf(':');
        if (colon > 0) p = p.substring(colon + 1);
        int star = p.indexOf('*');
        if (star > 0) p = p.substring(0, star);
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }
}
