package org.example.skillsspringai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SkillParser {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*[\\r\\n]+name:\\s*(.+?)\\s*[\\r\\n]+description:\\s*(.+?)\\s*[\\r\\n]+---\\s*[\\r\\n]+(.*)$",
            Pattern.DOTALL
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Skill parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            return Skill.builder()
                    .name(matcher.group(1).trim())
                    .description(matcher.group(2).trim())
                    .instructions(matcher.group(3).trim())
                    .fullContent(content)
                    .type(Skill.SkillType.TEXT)
                    .build();
        }

        log.warn("技能解析失败");
        return null;
    }

    public static Skill parsePackageDescriptor(InputStream jsonStream) {
        try {
            PackageDescriptor descriptor = objectMapper.readValue(jsonStream, PackageDescriptor.class);
            return Skill.builder()
                    .name(descriptor.getName())
                    .description(descriptor.getDescription())
                    .version(descriptor.getVersion())
                    .mainClass(descriptor.getMainClass())
                    .sourceUrl(descriptor.getSourceUrl())
                    .type(Skill.SkillType.PACKAGE)
                    .toolMethodNames(descriptor.getToolMethodNames())
                    .build();
        } catch (Exception e) {
            log.error("解析 skill.json 失败", e);
            return null;
        }
    }

    @lombok.Data
    public static class PackageDescriptor {
        private String name;
        private String description;
        private String version;
        private String mainClass;
        private String sourceUrl;
        private java.util.List<String> toolMethodNames;
        private java.util.List<String> dependencies;
    }
}
