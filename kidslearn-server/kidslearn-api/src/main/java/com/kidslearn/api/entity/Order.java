package com.kidslearn.api.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kidslearn.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Integer productType;
    private Long productId;
    private BigDecimal amount;
    private String payChannel;
    private Integer payStatus;
    private LocalDateTime payTime;
}
