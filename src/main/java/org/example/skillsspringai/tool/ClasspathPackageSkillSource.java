package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClasspathPackageSkillSource implements SkillSource {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final String pattern;

    public ClasspathPackageSkillSource(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public List<Skill> load() {
        List<Skill> skills = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Skill skill = SkillParser.parsePackageDescriptor(is);
                    if (skill != null) {
                        String packagePath = extractPackagePath(resource);
                        skill.setPackagePath(packagePath);
                        skills.add(skill);
                        log.debug("从 classpath 加载技能包: {}", skill.getName());
                    }
                }
            }
            log.info("classpath 包加载完成: {} 个技能包", skills.size());
        } catch (Exception e) {
            log.error("classpath 技能包加载失败: {}", pattern, e);
        }
        return skills;
    }

    private String extractPackagePath(Resource resource) {
        try {
            return resource.getURL().getPath();
        } catch (Exception e) {
            return null;
        }
    }
}
