package com.riton.mq.consumer;

import com.riton.constants.MQConstants;
import com.riton.constants.OrderStatutesConstants;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.mq.OrderCloseEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@RocketMQMessageListener(topic = MQConstants.ORDER_CLOSE_TOPIC, consumerGroup = MQConstants.ORDER_CLOSE_CONSUMER_GROUP, maxReconsumeTimes = 3)
public class OrderCloseConsumer implements RocketMQListener<OrderCloseEvent> {

    private final VoucherOrderMapper voucherOrderMapper;

    @Override
    public void onMessage(OrderCloseEvent event) {
        Long orderId = event.getOrderId();
        Integer state = voucherOrderMapper.getOrderStatus(orderId);
        if (OrderStatutesConstants.UNPAID.equals(state)) {
            Long count = voucherOrderMapper.closeOrderByIdIfUnpaid(orderId);
            if (count > 0) {
                log.info("发现超时订单，orderId={}，已经成功关闭", orderId);
            }
        }
    }
}
