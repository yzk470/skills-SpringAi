package org.example.skillsspringai.tool;

import org.example.skillsspringai.entity.Skill;

import java.util.List;

public interface SkillSource {
    List<Skill> load();
}
