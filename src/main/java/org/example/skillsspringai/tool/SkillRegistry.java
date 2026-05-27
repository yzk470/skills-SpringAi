package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SkillRegistry {

    private final Map<String, Skill> skillCache = new ConcurrentHashMap<>();

    public void register(Skill skill) {
        if (skill == null || skill.getName() == null) {
            return;
        }
        Skill existing = skillCache.get(skill.getName());
        if (existing != null) {
            log.info("技能 [{}] 已存在，将被覆盖 (旧版本: {}, 新版本: {})",
                    skill.getName(),
                    existing.getVersion() != null ? existing.getVersion() : "N/A",
                    skill.getVersion() != null ? skill.getVersion() : "N/A");
        }
        skillCache.put(skill.getName(), skill);
        log.debug("注册技能: {} (类型: {})", skill.getName(), skill.getType());
    }

    public void registerAll(List<Skill> skills) {
        for (Skill skill : skills) {
            register(skill);
        }
    }

    public Skill get(String name) {
        return skillCache.get(name);
    }

    public Optional<Skill> findByName(String name) {
        return Optional.ofNullable(skillCache.get(name));
    }

    public List<Skill> getAll() {
        return new ArrayList<>(skillCache.values());
    }

    public List<Skill> getByType(Skill.SkillType type) {
        return skillCache.values().stream()
                .filter(s -> s.getType() == type)
                .toList();
    }

    public int count() {
        return skillCache.size();
    }
}
