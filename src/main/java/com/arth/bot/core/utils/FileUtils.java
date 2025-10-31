package com.arth.bot.core.utils;

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
}
