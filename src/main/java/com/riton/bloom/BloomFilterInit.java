package com.riton.bloom;

import com.riton.constants.Constants;
import com.riton.mapper.ShopMapper;
import com.riton.utils.BloomFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 把数据库的内容加载到布隆过滤器
 */
@Component
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
        BloomFilter shopBloomFilter = bloomFilterFactory.getBloomFilter(Constants.BLOOM_FILTER_HANDLER_SHOP);
        for (Long shopId : shopIds) {
            shopBloomFilter.add(String.valueOf(shopId));
        }
    }
}
