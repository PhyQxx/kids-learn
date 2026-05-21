package com.kidslearn.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCallbackDTO {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付状态不能为空")
    private Integer payStatus;
}

