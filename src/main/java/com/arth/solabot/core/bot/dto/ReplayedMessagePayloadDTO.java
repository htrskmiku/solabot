package com.arth.solabot.core.bot.dto;

import com.arth.solabot.core.bot.dto.replay.AudioRef;
import com.arth.solabot.core.bot.dto.replay.ImageRef;
import com.arth.solabot.core.bot.dto.replay.MfaceRef;
import com.arth.solabot.core.bot.dto.replay.VideoRef;
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
    private List<MfaceRef> mfaces = new ArrayList<>();

    @Builder.Default
    private List<AudioRef> audios = new ArrayList<>();

    @Builder.Default
    private List<VideoRef> videos = new ArrayList<>();

    // 元信息
    private Long messageId;

    private Long userId;

    private Long groupId;

    private Long timeEpochSec;

    public void addText(String s) {
        if (s != null && !s.isEmpty()) texts.add(s);
    }

    public void addImage(ImageRef r) {
        if (r != null) images.add(r);
    }

    public void addMface(MfaceRef r) {
        if (r != null) mfaces.add(r);
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

    public void addAllMfaces(Collection<MfaceRef> cs) {
        if (cs != null) mfaces.addAll(cs);
    }

    public void addAllAudios(Collection<AudioRef> cs) {
        if (cs != null) audios.addAll(cs);
    }

    public void addAllVideos(Collection<VideoRef> cs) {
        if (cs != null) videos.addAll(cs);
    }
}