package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    private ExecutorService es = Executors.newFixedThreadPool(16);

    @Autowired
    private RedisIdWorker redisIdWorker;

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
}
