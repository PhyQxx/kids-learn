package com.kidslearn.api.dto.order;

import lombok.Data;

@Data
public class CreateOrderDTO {
    private Integer planType;
    private String payChannel;
}

