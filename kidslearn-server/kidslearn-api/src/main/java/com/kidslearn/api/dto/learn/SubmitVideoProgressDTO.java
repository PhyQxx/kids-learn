package com.kidslearn.api.dto.learn;

import lombok.Data;

@Data
public class SubmitVideoProgressDTO {
    private Long videoId;
    private Integer progressSeconds;
    private Integer durationSeconds;
}
