package com.arth.bot.core.config;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenCvConfig {

    @PostConstruct
    public void init() {
        Loader.load(opencv_core.class);
        log.info("[OpenCV] native library loaded successfully");
    }
}
