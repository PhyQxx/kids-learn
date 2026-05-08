package com.kidslearn.api.dto.order;

import lombok.Data;

@Data
public class PaymentCallbackDTO {
    private String orderNo;
    private Integer payStatus;
}

