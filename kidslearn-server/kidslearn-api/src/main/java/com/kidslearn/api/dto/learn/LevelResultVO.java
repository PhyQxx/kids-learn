package com.kidslearn.api.dto.learn;

import lombok.Data;

@Data
public class LevelResultVO {
    private Integer score;
    private Integer stars;
    private Integer gold;
    private Integer exp;
    private Integer wrongCount;
    private Boolean isPass;
    private Boolean unlockedNextLevel;
    private Long stickerId;
    private String stickerName;
    private String stickerUrl;
}
