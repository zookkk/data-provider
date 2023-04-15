package com.zookkk.data.model;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.zookkk.context.DataContext;
import com.zookkk.provider.DataProvider;


/**
 * @author longkai03 <longkai03@kuaishou.com>
 * Created on 2023-04-11
 */
public class TestProvider implements DataProvider<Integer, String> {
    @Override
    public Map<Integer, String> provide(
            @Nonnull Collection<Integer> keys, @Nonnull DataContext<Integer, String> context
    ) {
        long currentTime = System.nanoTime();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        Map<Integer, String> result = keys.stream()
                .collect(Collectors.toMap(Function.identity(), key -> key + "_" + Thread.currentThread().getName()));
        System.out.println("cost:" + Duration.ofNanos(System.nanoTime() - currentTime).toMillis() + ", data:" + result);
        return result;
    }
}
