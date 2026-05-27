package org.example.skillsspringai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.example.skillsspringai.entity.ScriptResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class SkillScriptRunner {

    private final SkillRegistry skillRegistry;
    private static final int TIMEOUT_SECONDS = 60;

    /** 输出目录：项目根目录下的 ppt 文件夹 */
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "ppt";

    @Tool(description = "获取文件输出目录的绝对路径。生成的.pptx等输出文件应保存到此目录。返回绝对路径，如 D:\\project\\ppt\\")
    public String getOutputDir() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath() + File.separator;
    }

    @Tool(description = """
            在某个技能(skill)包的工作目录下执行脚本或命令。
            参数 skillName 是技能名称(如 pptx)，
            参数 interpreter 是解释器，如 python、python3、node、bash、sh，
            参数 scriptOrCode 是脚本文件名(相对于技能目录，如 scripts/thumbnail.py)或内联代码，
            参数 args 是脚本参数(可选)。
            示例：runInSkill('pptx', 'python', 'scripts/thumbnail.py', 'input.pptx')
            示例：runInSkill('pptx', 'node', '-e', 'console.log(1+1)')
            """)
    public String runInSkill(
            @ToolParam(description = "技能名称，例如 pptx") String skillName,
            @ToolParam(description = "解释器: python, python3, node, bash, sh") String interpreter,
            @ToolParam(description = "脚本文件名(相对路径，如 scripts/thumbnail.py)或 -e/--eval 时表示内联代码") String scriptOrCode,
            @ToolParam(description = "脚本参数或额外的代码内容，多个参数用逗号分隔") String... args) {

        Skill skill = skillRegistry.get(skillName);
        if (skill == null) {
            return "错误：未找到技能 [" + skillName + "]";
        }

        String basePath = skill.getResourcePath();
        if (basePath == null) {
            basePath = skill.getPackagePath();
        }
        if (basePath == null) {
            return "错误：技能 [" + skillName + "] 没有配置工作目录。";
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(resolveInterpreter(interpreter));

            if ("-e".equals(scriptOrCode) || "--eval".equals(scriptOrCode)) {
                command.add("-e");
                if (args != null && args.length > 0) {
                    command.add(String.join(" ", args));
                }
            } else if ("-c".equals(scriptOrCode)) {
                command.add("-c");
                if (args != null && args.length > 0) {
                    command.add(String.join(" ", args));
                }
            } else {
                Path scriptPath = Paths.get(basePath, scriptOrCode).normalize();
                if (!scriptPath.startsWith(Paths.get(basePath).normalize())) {
                    return "错误：路径穿越攻击被阻止 [" + scriptOrCode + "]";
                }
                command.add(scriptPath.toString());
                if (args != null) {
                    for (String arg : args) {
                        command.add(arg);
                    }
                }
            }

            log.info("执行脚本: {} (工作目录: {})", String.join(" ", command), basePath);

            // 记录执行前已存在的文件，用于检测新生成的输出文件
            Set<String> beforeFiles = listOutputFiles(basePath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(basePath));
            pb.redirectErrorStream(true);

            // Ensure global npm modules are accessible
            String nodePath = System.getenv("NODE_PATH");
            if (nodePath == null || nodePath.isEmpty()) {
                String npmRoot = System.getProperty("user.home") + "\\AppData\\Roaming\\npm\\node_modules";
                if (new File(npmRoot).exists()) {
                    pb.environment().put("NODE_PATH", npmRoot);
                }
            }

            // Pass output directory to subprocess (ensure trailing separator)
            pb.environment().put("SKILL_OUTPUT_DIR", OUTPUT_DIR + File.separator);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "错误：脚本执行超时 (" + TIMEOUT_SECONDS + "秒)";
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                log.info("脚本执行成功: {} → {} 字符输出", scriptOrCode, output.length());
                // 自动将新生成的文件移到输出目录
                String movedInfo = moveNewOutputFiles(basePath, beforeFiles);
                if (!movedInfo.isEmpty()) {
                    output = (output.length() > 0 ? output + "\n" : "") + movedInfo;
                }
                return output.length() > 0 ? output : "(执行成功，无输出)";
            } else {
                log.warn("脚本执行失败 (exit={}): {}", exitCode, output);
                return "错误 (exit=" + exitCode + "):\n" + output;
            }

        } catch (Exception e) {
            log.error("脚本执行异常: {}", scriptOrCode, e);
            return "执行失败：" + e.getMessage();
        }
    }

    @Tool(description = """
            在技能(skill)包目录下写入一个文件。用于创建脚本、配置文件等。
            参数 skillName 是技能名称，relativePath 是相对路径，content 是文件内容。
            """)
    public String writeFileInSkill(
            @ToolParam(description = "技能名称") String skillName,
            @ToolParam(description = "相对于技能目录的文件路径") String relativePath,
            @ToolParam(description = "要写入的文件内容") String content) {

        Skill skill = skillRegistry.get(skillName);
        if (skill == null) {
            return "错误：未找到技能 [" + skillName + "]";
        }

        String basePath = skill.getResourcePath();
        if (basePath == null) {
            basePath = skill.getPackagePath();
        }
        if (basePath == null) {
            return "错误：技能没有配置目录";
        }

        try {
            Path filePath = Paths.get(basePath, relativePath).normalize();
            if (!filePath.startsWith(Paths.get(basePath).normalize())) {
                return "错误：路径穿越攻击被阻止";
            }
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            log.info("写入文件: {} → {}", relativePath, filePath);
            return "文件已写入: " + relativePath + " (" + content.length() + " 字符)";
        } catch (Exception e) {
            return "写入文件失败: " + e.getMessage();
        }
    }

    private static final String[] OUTPUT_EXTENSIONS = {".pptx", ".pdf", ".xlsx", ".docx", ".png", ".jpg", ".zip"};

    /** 列出工作目录下所有输出类型文件 */
    private Set<String> listOutputFiles(String dir) {
        Set<String> files = new HashSet<>();
        File workDir = new File(dir);
        if (!workDir.exists()) return files;
        File[] children = workDir.listFiles();
        if (children == null) return files;
        for (File f : children) {
            if (!f.isFile()) continue;
            String name = f.getName().toLowerCase();
            for (String ext : OUTPUT_EXTENSIONS) {
                if (name.endsWith(ext)) {
                    files.add(f.getName());
                    break;
                }
            }
        }
        return files;
    }

    /** 将新生成的输出文件移到 ppt/ 目录，同时修复根目录下误拼的文件(如 pptxxx.pptx) */
    private String moveNewOutputFiles(String basePath, Set<String> beforeFiles) {
        StringBuilder sb = new StringBuilder();
        File outDir = new File(OUTPUT_DIR);
        if (!outDir.exists()) outDir.mkdirs();

        File workDir = new File(basePath);

        // 1. 扫描工作目录下新生成的文件
        moveNewFilesInDir(workDir, outDir, beforeFiles, sb);

        // 2. 扫描项目根目录下误拼的文件 (如 pptxxx.pptx)
        File projectRoot = new File(System.getProperty("user.dir"));
        File[] rootFiles = projectRoot.listFiles();
        if (rootFiles != null) {
            for (File f : rootFiles) {
                if (!f.isFile()) continue;
                String name = f.getName().toLowerCase();
                // 匹配以 "ppt" 开头但没有正确路径分隔的文件
                if (!name.startsWith("ppt")) continue;
                boolean isOutput = false;
                for (String ext : OUTPUT_EXTENSIONS) {
                    if (name.endsWith(ext)) { isOutput = true; break; }
                }
                if (!isOutput) continue;

                // 修复文件名: ppt人工智能.pptx → 人工智能.pptx
                String correctedName = name.substring(3); // remove "ppt" prefix
                File dest = new File(outDir, correctedName);
                try {
                    Files.move(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    sb.append("[文件已保存] ").append(dest.getAbsolutePath()).append("\n");
                    log.info("修复误拼文件: {} → {}", f.getAbsolutePath(), dest.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("修复误拼文件失败: {}", f.getName(), e);
                }
            }
        }

        return sb.toString();
    }

    private void moveNewFilesInDir(File workDir, File outDir, Set<String> beforeFiles, StringBuilder sb) {
        File[] children = workDir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (!f.isFile()) continue;
            String name = f.getName().toLowerCase();
            boolean isOutput = false;
            for (String ext : OUTPUT_EXTENSIONS) {
                if (name.endsWith(ext)) { isOutput = true; break; }
            }
            if (!isOutput) continue;
            if (beforeFiles.contains(f.getName())) continue;

            File dest = new File(outDir, f.getName());
            try {
                Files.move(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                sb.append("[文件已保存] ").append(dest.getAbsolutePath()).append("\n");
                log.info("输出文件已移动: {} → {}", f.getAbsolutePath(), dest.getAbsolutePath());
            } catch (Exception e) {
                log.warn("移动输出文件失败: {}", f.getName(), e);
                try {
                    Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    sb.append("[文件已复制] ").append(dest.getAbsolutePath()).append("\n");
                } catch (Exception e2) {
                    sb.append("[警告] 无法移动文件: ").append(f.getName()).append("\n");
                }
            }
        }
    }

    /** 已知的 Python 安装路径（按优先级排列） */
    private static final String[] KNOWN_PYTHON_PATHS = {
        "D:\\python\\python3.10.8\\python.exe",
        "D:\\python\\python3.14.2\\python.exe"
    };

    private String resolveInterpreter(String name) {
        if (name == null) return "python";
        return switch (name.toLowerCase()) {
            case "python3", "python" -> findPython();
            case "node", "nodejs" -> "node";
            case "bash" -> "bash";
            case "sh" -> "sh";
            default -> name;
        };
    }

    /** 查找可用的 Python 解释器，优先使用已知路径 */
    private String findPython() {
        for (String path : KNOWN_PYTHON_PATHS) {
            if (new File(path).exists()) {
                return path;
            }
        }
        if (testCommand("python3")) return "python3";
        if (testCommand("python")) return "python";
        return "python";
    }

    private boolean testCommand(String command) {
        try {
            return new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start().waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }
}
