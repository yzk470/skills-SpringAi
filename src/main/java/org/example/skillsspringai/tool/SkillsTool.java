package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SkillsTool implements ToolCallbackProvider {

    private final SkillRegistry skillRegistry;
    private final Map<String, PackageClassLoader> classLoaderCache = new ConcurrentHashMap<>();
    private final Map<String, Object> toolInstanceCache = new ConcurrentHashMap<>();

    private SkillsTool(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    public static SkillsToolBuilder builder() {
        return new SkillsToolBuilder();
    }

    public void addSkill(Skill skill) {
        skillRegistry.register(skill);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (Skill skill : skillRegistry.getAll()) {
            try {
                List<ToolCallback> created = createCallbacks(skill);
                callbacks.addAll(created);
            } catch (Exception e) {
                log.error("创建工具回调失败: {}", skill.getName(), e);
            }
        }

        log.info("SkillsTool 加载完成: {} 个工具回调", callbacks.size());
        return callbacks.toArray(new ToolCallback[0]);
    }

    private List<ToolCallback> createCallbacks(Skill skill) {
        if (skill.getType() == Skill.SkillType.PACKAGE) {
            return createPackageCallbacks(skill);
        } else {
            return List.of(createTextCallback(skill));
        }
    }

    private ToolCallback createTextCallback(Skill skill) {
        String conciseInstructions = buildConciseInstructions(skill);
        return FunctionToolCallback.builder(
                        skill.getName(),
                        (java.util.function.Function<SkillInput, String>) input -> conciseInstructions
                )
                .description(skill.getDescription())
                .inputType(SkillInput.class)
                .build();
    }

    private String buildConciseInstructions(Skill skill) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(skill.getName()).append(" 技能已激活 ===\n\n");
        sb.append("描述: ").append(skill.getDescription()).append("\n\n");
        sb.append("请使用以下工具与此技能交互:\n");
        sb.append("- listFilesInSkill('").append(skill.getName()).append("') 查看所有可用文件\n");
        sb.append("- readFileInSkill('").append(skill.getName()).append("', '<相对路径>') 读取具体指南文件\n");
        sb.append("- runInSkill('").append(skill.getName()).append("', '<解释器>', '<脚本或代码>') 执行脚本\n");
        sb.append("- writeFileInSkill('").append(skill.getName()).append("', '<相对路径>', '<内容>') 写入文件\n\n");
        sb.append("关键文件(先用 readFileInSkill 读取相关指南):\n");

        if (skill.getResourcePath() != null) {
            try {
                java.nio.file.Path root = java.nio.file.Paths.get(skill.getResourcePath());
                if (java.nio.file.Files.exists(root)) {
                    java.nio.file.Files.walk(root, 2)
                            .filter(java.nio.file.Files::isRegularFile)
                            .filter(p -> !p.getFileName().toString().equals("SKILL.md")
                                    && !p.getFileName().toString().endsWith(".pyc"))
                            .map(p -> root.relativize(p).toString())
                            .sorted()
                            .limit(20)
                            .forEach(f -> sb.append("  - ").append(f).append("\n"));
                }
            } catch (Exception ignored) {
                // fallback: don't list files
            }
        }

        sb.append("\n工作流程: 先读取相关指南文件了解API和用法，然后编写脚本，最后用runInSkill执行。\n");
        sb.append("\n=== 关键约束 ===\n");
        sb.append("1. writeFileInSkill 只适合小型文件(<500字符配置/辅助脚本)。禁止在一次调用中写入超长脚本！\n");
        sb.append("   如果脚本很长，必须拆分成多个小文件分别写入。\n");
        sb.append("2. 生成PPT时，优先复用技能包中已有的.pptx模板文件，配合Python脚本工作流：\n");
        sb.append("   thumbnail.py → unpack.py → 编辑XML → clean.py → pack.py (详见editing.md)\n");
        sb.append("3. 只有确认没有合适的模板时才从零创建，且JS脚本必须拆成多个不超过3000字符的小文件。\n");
        sb.append("\n=== 输出配置 ===\n");
        sb.append("生成的输出文件(.pptx/.pdf等)必须保存到 SKILL_OUTPUT_DIR 目录。\n");
        sb.append("在脚本中通过环境变量 SKILL_OUTPUT_DIR 获取输出目录绝对路径。\n");
        sb.append("示例: const outDir = process.env.SKILL_OUTPUT_DIR; pptx.writeFile({ fileName: outDir + 'output.pptx' })");
        return sb.toString();
    }

    private List<ToolCallback> createPackageCallbacks(Skill skill) {
        String mainClass = skill.getMainClass();
        String packagePath = skill.getPackagePath();

        if (mainClass == null || packagePath == null) {
            log.warn("技能包 [{}] 缺少 mainClass 或 packagePath，回退为 TEXT 模式", skill.getName());
            return List.of(createTextCallback(skill));
        }

        List<ToolCallback> callbacks = new ArrayList<>();
        try {
            Object toolInstance = toolInstanceCache.computeIfAbsent(skill.getName(), name -> {
                try {
                    PackageClassLoader cl = new PackageClassLoader(Paths.get(packagePath));
                    classLoaderCache.put(name, cl);
                    Class<?> clazz = cl.loadToolClass(mainClass);
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    log.error("实例化工具类失败: {}", mainClass, e);
                    return null;
                }
            });

            if (toolInstance == null) {
                return callbacks;
            }

            Class<?> clazz = toolInstance.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                org.springframework.ai.tool.annotation.Tool toolAnn =
                        method.getAnnotation(org.springframework.ai.tool.annotation.Tool.class);
                if (toolAnn != null) {
                    ToolCallback callback = MethodToolCallback.builder()
                            .toolDefinition(DefaultToolDefinition.builder()
                                    .name(skill.getName() + "." + method.getName())
                                    .description(toolAnn.description())
                                    .build())
                            .toolMethod(method)
                            .toolObject(toolInstance)
                            .build();
                    callbacks.add(callback);
                    log.info("注册包工具方法: {} (来自 {})", method.getName(), skill.getName());
                }
            }

            if (callbacks.isEmpty()) {
                log.warn("技能包 [{}] 中未找到 @Tool 注解方法", skill.getName());
            }

        } catch (Exception e) {
            log.error("创建包工具回调失败: {}", skill.getName(), e);
        }

        return callbacks;
    }

    public void close() {
        classLoaderCache.values().forEach(cl -> {
            try {
                cl.close();
            } catch (Exception e) {
                log.warn("关闭 ClassLoader 失败", e);
            }
        });
        classLoaderCache.clear();
        toolInstanceCache.clear();
    }

    public static class SkillsToolBuilder {
        private SkillRegistry skillRegistry;
        private final List<String> skillsDirectories = new ArrayList<>();

        public SkillsToolBuilder registry(SkillRegistry registry) {
            this.skillRegistry = registry;
            return this;
        }

        public SkillsToolBuilder addSkillsDirectory(String directory) {
            this.skillsDirectories.add(directory);
            return this;
        }

        public SkillsTool build() {
            if (skillRegistry == null) {
                skillRegistry = new SkillRegistry();
            }
            SkillsTool tool = new SkillsTool(skillRegistry);

            for (String dir : skillsDirectories) {
                FileSystemMarkdownSkillSource source = new FileSystemMarkdownSkillSource(dir);
                skillRegistry.registerAll(source.load());
            }

            return tool;
        }
    }
}
