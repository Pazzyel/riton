package com.riton.mq;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderCreationEvent {
    private Long voucherId;
    private Long userId;
    private Long orderId;
    private Boolean isSeckillOrder;
}
