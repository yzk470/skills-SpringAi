package org.example.skillsspringai.entity;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Skill {
    private String name;
    private String description;
    private String instructions;
    private String fullContent;

    @Builder.Default
    private SkillType type = SkillType.TEXT;

    private String sourceUrl;
    private String version;
    private String mainClass;
    private String packagePath;
    private String resourcePath;

    @Singular
    private List<String> toolMethodNames;

    public enum SkillType {
        TEXT,
        PACKAGE
    }
}
