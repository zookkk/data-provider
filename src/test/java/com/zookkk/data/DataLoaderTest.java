package com.zookkk.data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.google.common.collect.ImmutableMap;
import com.zookkk.context.DataContext;
import com.zookkk.data.model.TestProvider;
import com.zookkk.loader.DataKey;
import com.zookkk.loader.DataLoader;
import com.zookkk.provider.DataProvider;

/**
 * @author longkai03 <longkai03@kuaishou.com>
 * Created on 2023-04-11
 */
@SpringBootTest
@Import(TestConfig.class)
public class DataLoaderTest {

    private static final Logger logger = LoggerFactory.getLogger(DataLoaderTest.class);
    private DataProvider<Integer,String> dataProvider;
    private DataKey<Integer,String> dataKey ;
    private DataContext<Integer,String> dataContext;
    private DataContext<Integer,String> dataContext2;
    private Collection<Integer> testKeys1 = new ArrayList<>();
    private Collection<Integer> testKeys2 = new ArrayList<>();
    private Collection<Integer> testKeys3 = new ArrayList<>();

    @BeforeEach
    public void initData(){
        dataProvider = new TestProvider();
        dataKey = DataKey.newDataKey("testKey",dataProvider);
        dataContext = DataContext.newBuilder()
                .environment(ImmutableMap.builder()
                        .put("visitorId",1)
                        .put("photoId",2)
                        .build()
                )
                .enableCache(false)
                .build();
        testKeys1.add(1);testKeys1.add(2);
        testKeys2.add(2);testKeys2.add(4);
        testKeys3.add(5);testKeys3.add(6);
    }

    @Test
    public void submitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.submit(dataKey,testKeys1);
        DataLoader.submit(dataKey,testKeys2);
        DataLoader.submit(dataKey,testKeys3);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        // 预期结果：主线程耗时1s，三个线程各执行一次任务，keys2只构建key=2的值，finalData包含 key={1,2,4,5,6}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", data:" + result);
    }
    @Test
    public void lazySubmitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.lazySubmit(dataKey,testKeys1);
        DataLoader.lazySubmit(dataKey,testKeys2);
        DataLoader.lazySubmit(dataKey,testKeys3);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        // 预期结果：主线程耗时2s，1个线程执行一次任务，finalData包含 key={1,2,4,5,6}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", data:" + result);
    }

    @Test
    public void mixedSubmitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.submit(dataKey,testKeys1);
        DataLoader.submit(dataKey,testKeys3);
        DataLoader.lazySubmit(dataKey,testKeys2);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        // 预期结果：主线程耗时2s，三个线程各执行一次任务，keys2只构建key=4的值，finalData包含 key={1,2,4,5,6}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", finalData:" + result);
    }

    @Test
    public void diffContextSubmitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.submit(dataKey,testKeys1);
        DataLoader.submit(dataKey,testKeys2,DataContext.EMPTY_NON_CACHEABLE_CONTEXT);
        DataLoader.submit(dataKey,testKeys3,dataContext);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        // 预期结果：主线程耗时1s，三个线程各执行一次任务，finalData包含 key={1,2,4,5,6}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", finalData:" + result);
    }
    @Test
    public void diffContextLazySubmitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.lazySubmit(dataKey,testKeys1);
        DataLoader.lazySubmit(dataKey,testKeys2,dataContext);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        // 预期结果：主线程耗时2s，两个线程各执行一次任务，keys2构建全部key的数据，finalData包含 key={1,2,4}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", finalData:" + result);
    }

    @Test
    public void cacheSubmitTest() throws InterruptedException {
        long currentTime = System.nanoTime();
        DataLoader.submit(dataKey,testKeys1);
        DataLoader.submit(dataKey,testKeys2);
        Thread.sleep(1000);
        Map<Integer,String> result = DataLoader.getData(dataKey);
        //预期结果： 主线长耗时1s，key2只构建key=4的值，finalData包含 key={1,2,4}的数据
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", finalData:" + result);
    }

}
