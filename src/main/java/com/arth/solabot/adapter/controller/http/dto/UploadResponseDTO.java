package com.arth.solabot.adapter.controller.http.dto;

import lombok.Data;

@Data
public class UploadResponseDTO {

    public boolean success;

    public String message;

    public long suiteTimestamp;

    public UploadResponseDTO() {
    }

    public UploadResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UploadResponseDTO(boolean success, String message, long suiteTimestamp) {
        this.success = success;
        this.message = message;
        this.suiteTimestamp = suiteTimestamp;
    }

    public static UploadResponseDTO build(boolean success, String message) {
        return new UploadResponseDTO(success, message);
    }

    public static UploadResponseDTO build(boolean success, String message, long suiteTimestamp) {
        return new UploadResponseDTO(success, message, suiteTimestamp);
    }
}
