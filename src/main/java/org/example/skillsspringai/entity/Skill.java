package org.example.skillsspringai.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Skill {
    private String name;
    private String description;
    private String instructions;
    private String fullContent;
}
