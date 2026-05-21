package com.kidslearn.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderDTO {
    @NotNull(message = "套餐类型不能为空")
    private Integer planType;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;
}

