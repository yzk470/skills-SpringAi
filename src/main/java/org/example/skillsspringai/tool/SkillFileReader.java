package org.example.skillsspringai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
@RequiredArgsConstructor
public class SkillFileReader {

    private final SkillRegistry skillRegistry;

    @Tool(description = "读取某个技能(skill)包目录下的文件内容。参数 skillName 是技能名称(如 pptx)，relativePath 是技能目录下的相对路径(如 pptxgenjs.md 或 editing.md)")
    public String readFileInSkill(
            @ToolParam(description = "技能名称，例如 pptx") String skillName,
            @ToolParam(description = "技能目录下的相对文件路径，例如 pptxgenjs.md、editing.md、scripts/thumbnail.py") String relativePath) {

        Skill skill = skillRegistry.get(skillName);
        if (skill == null) {
            return "错误：未找到技能 [" + skillName + "]。可用技能：" + skillRegistry.getAll().stream().map(Skill::getName).toList();
        }

        String basePath = skill.getResourcePath();
        if (basePath == null) {
            basePath = skill.getPackagePath();
        }
        if (basePath == null) {
            return "错误：技能 [" + skillName + "] 没有配置资源路径。";
        }

        try {
            Path filePath = Paths.get(basePath, relativePath).normalize();
            if (!filePath.startsWith(Paths.get(basePath).normalize())) {
                return "错误：路径穿越攻击被阻止 [" + relativePath + "]";
            }
            if (!Files.exists(filePath)) {
                return "错误：文件不存在 [" + relativePath + "] 在技能 [" + skillName + "] 目录下。";
            }
            String content = Files.readString(filePath);
            log.info("读取技能文件: {}/{} → {} 字符", skillName, relativePath, content.length());
            return content;
        } catch (Exception e) {
            return "读取文件失败 [" + relativePath + "]：" + e.getMessage();
        }
    }

    @Tool(description = "列出某个技能(skill)包目录下的所有文件。参数 skillName 是技能名称(如 pptx)")
    public String listFilesInSkill(
            @ToolParam(description = "技能名称，例如 pptx") String skillName) {

        Skill skill = skillRegistry.get(skillName);
        if (skill == null) {
            return "错误：未找到技能 [" + skillName + "]";
        }

        String basePath = skill.getResourcePath();
        if (basePath == null) {
            basePath = skill.getPackagePath();
        }
        if (basePath == null) {
            return "错误：技能 [" + skillName + "] 没有配置资源路径。";
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("技能 [").append(skillName).append("] 的文件列表：\n");
            final Path root = Paths.get(basePath);
            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .forEach(f -> sb.append("  ").append(f).append("\n"));
            return sb.toString();
        } catch (Exception e) {
            return "列出文件失败：" + e.getMessage();
        }
    }
}
