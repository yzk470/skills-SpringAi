package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;
import org.example.skillsspringai.entity.Skill;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class UrlSkillSource implements SkillSource {

    private final String url;
    private final String cacheDir;

    public UrlSkillSource(String url, String cacheDir) {
        this.url = url;
        this.cacheDir = cacheDir;
    }

    @Override
    public List<Skill> load() {
        List<Skill> skills = new ArrayList<>();

        try {
            Path cachePath = Paths.get(cacheDir);
            Files.createDirectories(cachePath);

            String fileName = extractFileName(url);
            Path localFile = cachePath.resolve(fileName);

            if (!Files.exists(localFile)) {
                log.info("下载技能包: {} -> {}", url, localFile);
                downloadFile(url, localFile);
            } else {
                log.info("使用缓存的技能包: {}", localFile);
            }

            skills = loadFromZip(localFile);

        } catch (Exception e) {
            log.error("URL 技能加载失败: {}", url, e);
        }

        return skills;
    }

    private List<Skill> loadFromZip(Path zipFile) {
        List<Skill> skills = new ArrayList<>();

        Path extractDir = Paths.get(cacheDir, baseName(zipFile.getFileName().toString()));
        try {
            Files.createDirectories(extractDir);

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    Path entryPath = sanitizePath(extractDir, entry.getName());
                    Files.createDirectories(entryPath.getParent());

                    try (FileOutputStream fos = new FileOutputStream(entryPath.toFile())) {
                        zis.transferTo(fos);
                    }

                    if (entry.getName().equals("skill.json")) {
                        try (InputStream is = Files.newInputStream(entryPath)) {
                            Skill skill = SkillParser.parsePackageDescriptor(is);
                            if (skill != null) {
                                skill.setPackagePath(extractDir.toString());
                                skills.add(skill);
                                log.info("从 ZIP 解析技能包: {}", skill.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解压技能包失败: {}", zipFile, e);
        }

        return skills;
    }

    private void downloadFile(String url, Path target) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "SkillsSpringAi/1.0");

        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(target.toFile())) {
            is.transferTo(fos);
        } finally {
            conn.disconnect();
        }

        log.info("下载完成: {} ({} bytes)", target, Files.size(target));
    }

    private String extractFileName(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        int queryIdx = name.indexOf('?');
        if (queryIdx > 0) {
            name = name.substring(0, queryIdx);
        }
        if (name.isBlank()) {
            name = "skill-package.zip";
        }
        return name;
    }

    private String baseName(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }

    private Path sanitizePath(Path base, String entryName) {
        Path resolved = base.resolve(entryName).normalize();
        if (!resolved.startsWith(base)) {
            throw new SecurityException("Zip 路径穿越攻击: " + entryName);
        }
        return resolved;
    }
}
