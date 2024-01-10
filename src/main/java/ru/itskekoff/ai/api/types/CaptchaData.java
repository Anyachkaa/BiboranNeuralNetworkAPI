package ru.itskekoff.ai.api.types;

import lombok.Data;

@Data
public class CaptchaData {
    private byte[] imageData;
    private CaptchaType captchaType;
    private String answer;

    public CaptchaData(byte[] imageData, CaptchaType captchaType) {
        this.imageData = imageData;
        this.captchaType = captchaType;
    }
}
