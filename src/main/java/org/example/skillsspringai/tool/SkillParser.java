package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SkillParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^---\\s*[\\r\\n]+name:\\s*(.+?)\\s*[\\r\\n]+description:\\s*(.+?)\\s*[\\r\\n]+---\\s*[\\r\\n]+(.*)$",
            Pattern.DOTALL
    );

    public static Skill parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(content);
        if (matcher.find()) {
            return Skill.builder()
                    .name(matcher.group(1).trim())
                    .description(matcher.group(2).trim())
                    .instructions(matcher.group(3).trim())
                    .fullContent(content)
                    .build();
        }

        log.warn("技能解析失败");
        return null;
    }
}
