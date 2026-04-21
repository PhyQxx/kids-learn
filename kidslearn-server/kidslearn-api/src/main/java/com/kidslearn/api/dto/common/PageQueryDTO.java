package com.kidslearn.api.dto.common;

import lombok.Data;

@Data
public class PageQueryDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
}
