package com.riton.mq.consumer;

import com.riton.constants.MQConstants;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.Shop;
import com.riton.mq.ShopUpdateEvent;
import com.riton.service.IShopSearchService;
import com.riton.service.IShopService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@RocketMQMessageListener(
        topic = MQConstants.SHOP_UPDATE_TOPIC,
        consumerGroup = MQConstants.SHOP_UPDATE_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class ShopUpdateConsumer implements RocketMQListener<ShopUpdateEvent> {

    private final IShopService shopService;

    private final IShopSearchService shopSearchService;

    @Override
    public void onMessage(ShopUpdateEvent event) {
        if (event == null || event.getShopId() == null) {
            log.warn("receive invalid shop update event");
            return;
        }
        Shop shop = shopService.getById(event.getShopId());
        if (shop == null) {
            log.warn("shop not found when syncing to es, shopId={}", event.getShopId());
            return;
        }
        Result result = shopSearchService.updateShopDoc(shop);
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            throw new IllegalStateException("sync shop to es failed, shopId=" + event.getShopId());
        }
    }
}
