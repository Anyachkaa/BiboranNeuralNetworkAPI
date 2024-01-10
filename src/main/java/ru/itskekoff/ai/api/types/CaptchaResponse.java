package ru.itskekoff.ai.api.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CaptchaResponse {
    @JsonProperty("status")
    private int status;

    @JsonProperty("request")
    private String request;
}