package com.arth.bot.plugins.scripts;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class ScriptRunner implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(ScriptRunner.class);

    @Override
    public void run(String... args) throws Exception {
        String scriptPath = "src/main/java/com/arth/bot/plugins/scripts/mysekai/run.py";
        runPythonScript(scriptPath);
    }

    private void runPythonScript(String scriptPath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[script] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("[script] script executed successfully: {}", scriptPath);
            } else {
                logger.error("[script] script execution failed, exit code: {}", exitCode);
            }
        } catch (IOException e) {
            logger.error("[script] IO exception occurred while executing script: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("[script] script execution interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("[script] unknown exception occurred: {}", e.getMessage(), e);
        }
    }
}