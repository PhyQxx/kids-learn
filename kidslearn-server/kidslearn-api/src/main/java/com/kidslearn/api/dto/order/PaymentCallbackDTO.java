package com.kidslearn.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCallbackDTO {
    @NotBlank(message = "orderNo is required")
    private String orderNo;

    @NotNull(message = "payStatus is required")
    private Integer payStatus;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    @NotBlank(message = "signature is required")
    private String signature;
}
