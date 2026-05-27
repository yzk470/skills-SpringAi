package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class PackageClassLoader extends URLClassLoader {

    public PackageClassLoader(Path packageDir) {
        super(collectJarUrls(packageDir), PackageClassLoader.class.getClassLoader());
    }

    private static URL[] collectJarUrls(Path packageDir) {
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(packageDir.toUri().toURL());
            if (Files.exists(packageDir)) {
                try (Stream<Path> files = Files.walk(packageDir)) {
                    files.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".jar"))
                            .forEach(p -> {
                                try {
                                    urls.add(p.toUri().toURL());
                                } catch (Exception e) {
                                    log.warn("无法添加 JAR 到 classpath: {}", p, e);
                                }
                            });
                }
            }
        } catch (Exception e) {
            log.error("收集 JAR URL 失败: {}", packageDir, e);
        }
        return urls.toArray(new URL[0]);
    }

    public Class<?> loadToolClass(String className) throws ClassNotFoundException {
        return loadClass(className);
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            log.warn("关闭 PackageClassLoader 失败", e);
        }
    }
}
