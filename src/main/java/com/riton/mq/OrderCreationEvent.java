package com.riton.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreationEvent {
    private Long voucherId;
    private Long userId;
    private Long orderId;
    private Boolean isSeckillOrder;
}
