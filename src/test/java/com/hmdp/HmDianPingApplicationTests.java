package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {

    private ExecutorService es = Executors.newFixedThreadPool(16);

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private IShopService shopService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(() -> {
               for (int j = 0; j < 100; j++) {
                   long id = redisIdWorker.nextId("order");
                   System.out.println("id:" + id);
               }
               latch.countDown();
            });
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    public void loadShopData() {
        List<Shop> shops = shopService.list();
        Map<Long,List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getId));//对商铺分组

        for(Map.Entry<Long,List<Shop>> entry : map.entrySet()){
            String key = RedisConstants.SHOP_GEO_KEY + entry.getKey();//同类型的商铺是同一个key
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> geo = new ArrayList<>(value.size());
            for(Shop shop : value){
                geo.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
            }
            //写入Redis
            stringRedisTemplate.opsForGeo().add(key, geo);
        }
    }

}
