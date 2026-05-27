package org.example.skillsspringai.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkillLoaderService {

    private final SkillRegistry skillRegistry;

    private final List<SkillSource> sources = new ArrayList<>();

    @PostConstruct
    public void loadAllSkills() {
        sources.add(new ClasspathMarkdownSkillSource("classpath:skills/**/*SKILL.md"));
        sources.add(new ClasspathMarkdownSkillSource("classpath:skills.ppt/**/*SKILL.md"));

        File dynamicDir = new File("data/skills");
        if (dynamicDir.exists()) {
            sources.add(new FileSystemMarkdownSkillSource("data/skills"));
        }

        int total = 0;
        for (SkillSource source : sources) {
            List<Skill> skills = source.load();
            skillRegistry.registerAll(skills);
            total += skills.size();
        }

        log.info("技能加载完成：共 {} 个技能 (内置{}个)", total, skillRegistry.count());
    }

    public Optional<Skill> findByName(String skillName) {
        return skillRegistry.findByName(skillName);
    }

    public void addRemoteSource(String url, String cacheDir) {
        UrlSkillSource source = new UrlSkillSource(url, cacheDir);
        sources.add(source);
        skillRegistry.registerAll(source.load());
        log.info("远程技能已加载: {}", url);
    }

    public SkillRegistry getRegistry() {
        return skillRegistry;
    }
}
