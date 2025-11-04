package com.arth.bot.plugin.custom.pjsk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    /**
     * 文件夹不存在时自动创建，文件夹已存在时跳过
     * @param path
     * @throws IOException
     */
    public static void createFolders(Path path) throws IOException {
        Files.createDirectories(path);
    }

    /**
     * 文件不存在时自动创建，文件已存在时获取该文件
     * @param path
     * @throws IOException
     */
    public static File getOrCreateFile(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
        return path.toFile();
    }

    public static boolean fileExists(Path path) {
        return Files.exists(path);
    }

}
