package com.riton.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.riton.constants.Constants;
import com.riton.dto.Result;
import com.riton.entity.Shop;
import com.riton.mapper.ShopMapper;
import com.riton.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.bloom.BloomFilterFactory;
import com.riton.utils.CacheClient;
import com.riton.constants.RedisConstants;
import com.riton.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;

    private final CacheClient cacheClient;

    private final BloomFilterFactory bloomFilterFactory;

    private final TypeReference<Shop> type = new TypeReference<Shop>() {};

    private static final Cache<Long, Shop> SHOP_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Autowired
    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate, CacheClient cacheClient, BloomFilterFactory bloomFilterFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheClient = cacheClient;
        this.bloomFilterFactory = bloomFilterFactory;
    }



    /**
     * 根据id查询商铺信息
     * @param id id
     * @return Result<Shop>
     */
    @Override
    public Result queryById(Long id) {
        // 先查Caffeine
        Shop localShop = SHOP_LOCAL_CACHE.getIfPresent(id);
        if (localShop != null) {
            log.info("查询商铺 本地缓存命中 商铺 : {}",localShop);
            return Result.ok(localShop);
        }

        // 布隆过滤器判断空值
        if (bloomFilterFactory.getBloomFilter(Constants.BLOOM_FILTER_HANDLER_SHOP).contains(String.valueOf(id))) {
            log.info("查询商铺 布隆过滤器判断不存在 商铺id : {}",id);
            return Result.fail("店铺不存在！");
        }

        // 缓存空数据解决缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
         Shop shop = cacheClient
                 .queryWithMutex(RedisConstants.CACHE_SHOP_KEY, id, type, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }

        // 存到Caffeine
        SHOP_LOCAL_CACHE.put(id, shop);

        // 7.返回
        return Result.ok(shop);
    }

    /**
     * 更新商铺信息
     * @param shop 新的商铺信息
     * @return
     */
    @Transactional
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺ID不能为空");
        }
        //先更新数据库
        updateById(shop);
        //再删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        //Caffeine里的也要删除
        SHOP_LOCAL_CACHE.invalidate(id);
        return Result.ok();
    }

    /**
     * 查询给定种类下的店铺列表（如果有位置信息只查询5000米内的）
     * @param typeId 店铺类型id
     * @param current 当前页数
     * @param x 经度
     * @param y 维度
     * @return 返回的店铺列表
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1.判断查询请求是否包含坐标信息，不包含时正常数据库查询
        if(x == null || y == null){
            Page<Shop> shops = query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(shops.getRecords());
        }

        //2.需要坐标查询，查询Redis

        //2.1 手动计算分页参数
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;//start代表跳过的记录条数
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        //2.2查询Redis
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> search = stringRedisTemplate.opsForGeo().search(
                key,//参数 1：Redis 中的 key
                GeoReference.fromCoordinate(x, y),// 参数 2：搜索参考点（经纬度）
                new Distance(5000),// 参数 3：搜索范围（单位默认为米）
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)// 参数 4：搜索附加参数（包含与参考点的位置信息，最多搜索end条数据）
        );

        //3.根据Redis查询结果构建返回List
        if (search == null) {
            //不存在查询结果
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = search.getContent();
        if (content.size() <= start) {
            //查询的页超过了现有数据的范围
            return Result.ok(Collections.emptyList());
        }
        //3.1截取start - end部分
        List<Long> ids = new ArrayList<>(SystemConstants.DEFAULT_PAGE_SIZE);
        Map<Long,Double> distanceMap = new HashMap<>(SystemConstants.DEFAULT_PAGE_SIZE);
        content.stream().skip(start).forEach(r -> {
            //这里的r是GeoResult，调用getContent()后才是GeoLocation,也就是Spring里储存Redis的Geo单元信息的类
            String idStr = r.getContent().getName();//获取单元的名称
            Long id = Long.parseLong(idStr);
            ids.add(id);
            distanceMap.put(id,r.getDistance().getValue());
        });
        //3.2根据ids从数据库查询商铺的全部信息
        String idStr = StrUtil.join(",",ids);
        List<Shop> shops = query().in("id",ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        //注意数据库是没有距离信息的，需要实时计算
        shops.forEach(shop -> shop.setDistance(distanceMap.get(shop.getId())));
        return Result.ok(shops);
    }
}
