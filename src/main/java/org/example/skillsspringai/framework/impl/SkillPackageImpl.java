package org.example.skillsspringai.framework.impl;

import lombok.Builder;
import lombok.Data;
import org.example.skillsspringai.framework.SkillPackage;

import java.util.Map;

@Data
@Builder
public class SkillPackageImpl implements SkillPackage {
    private String name;
    private String description;
    private String content;
    private boolean available;
    private Map<String, Object> properties;
}
