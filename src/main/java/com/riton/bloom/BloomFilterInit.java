package com.riton.bloom;

import com.riton.constants.Constants;
import com.riton.mapper.ShopMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 把数据库的内容加载到布隆过滤器
 */
@Component
@Slf4j
public class BloomFilterInit {

    private final ShopMapper shopMapper;
    private final BloomFilterFactory bloomFilterFactory;

    @Autowired
    public BloomFilterInit(ShopMapper shopMapper, BloomFilterFactory bloomFilterFactory) {
        this.shopMapper = shopMapper;
        this.bloomFilterFactory = bloomFilterFactory;
    }

    @PostConstruct
    public void initBloomFilters() {
        List<Long> shopIds = shopMapper.getAllShopIds();
        log.info("初始化布隆过滤器：店铺ids: {}", shopIds);
        BloomFilter shopBloomFilter = bloomFilterFactory.getBloomFilter(Constants.BLOOM_FILTER_HANDLER_SHOP);
        for (Long shopId : shopIds) {
            shopBloomFilter.add(String.valueOf(shopId));
        }
    }
}
