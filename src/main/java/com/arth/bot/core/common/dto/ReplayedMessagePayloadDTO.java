package com.arth.bot.core.common.dto;

import com.arth.bot.core.common.dto.replay.AudioRef;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.common.dto.replay.VideoRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 请求获取引用消息内容的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayedMessagePayloadDTO {
    // 文本
    private String rawMessage;

    @Builder.Default
    private List<String> texts = new ArrayList<>();

    // 媒体
    @Builder.Default
    private List<ImageRef> images = new ArrayList<>();

    @Builder.Default
    private List<AudioRef> audios = new ArrayList<>();

    @Builder.Default
    private List<VideoRef> videos = new ArrayList<>();

    // 元信息
    private Long msgId;
    private Long userId;
    private Long groupId;
    private Long timeEpochSec;

    public void addText(String s) {
        if (s != null && !s.isEmpty()) texts.add(s);
    }

    public void addImage(ImageRef r) {
        if (r != null) images.add(r);
    }

    public void addAudio(AudioRef r) {
        if (r != null) audios.add(r);
    }

    public void addVideo(VideoRef r) {
        if (r != null) videos.add(r);
    }

    public void addAllImages(Collection<ImageRef> cs) {
        if (cs != null) images.addAll(cs);
    }

    public void addAllAudios(Collection<AudioRef> cs) {
        if (cs != null) audios.addAll(cs);
    }

    public void addAllVideos(Collection<VideoRef> cs) {
        if (cs != null) videos.addAll(cs);
    }
}