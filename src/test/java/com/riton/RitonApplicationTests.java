package com.riton;

import com.riton.entity.Shop;
import com.riton.service.IShopService;
import com.riton.constants.RedisConstants;
import com.riton.utils.RedisIdWorker;
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
class RitonApplicationTests {

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
        Map<Long,List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));//对商铺分组

        for(Map.Entry<Long,List<Shop>> entry : map.entrySet()){
            String key = RedisConstants.SHOP_GEO_KEY + entry.getKey();//同类型的商铺是同一个key
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> geo = new ArrayList<>(value.size());
            for(Shop shop : value){
                geo.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
            }
            // 写入Redis
            stringRedisTemplate.opsForGeo().add(key, geo);
        }
    }

    @Test
    public void testHyperLogLog(){
        //测试HLL算法统计UV次数(User Visit用户访问量）
        String[] users = new String[1000];//模拟1000个用户
        int index = 0;
        for (int i = 1; i <= 1000000; i++) {
            users[index++] = "user_" + i;
            if(i % 1000 == 0){
                //每1000次提交整组用户
                index = 0;
                stringRedisTemplate.opsForHyperLogLog().add("hll1",users);
            }
        }
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll1");
        System.out.println("size=" + size);
    }

}
