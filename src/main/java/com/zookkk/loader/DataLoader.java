package com.zookkk.loader;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.allOf;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.Uninterruptibles;
import com.zookkk.context.DataContext;
import com.zookkk.provider.DataProviderHelper;

/**
 * @author zookkk <1349054970@qq.com>
 */
public class DataLoader {

    private static final Map<DataKey<?, ?>, Collection<CompletableFuture<Map<?, ?>>>> dataKeyFutures = new HashMap<>();
    private static final Map<DataKey<?, ?>, Map<DataContext<?, ?>, DataProviderHelper<?, ?>>> lazyDataKeyHelpers =
            new HashMap<>();

    /*
        约定：Context一样，则同样的Key得到的结果是相同的,针对Context做缓存，按submit时间顺序覆盖数据
        如果不满足上述条件请不要开启缓存
     */
    public static <Key, Value> void submit(
            @Nonnull DataKey<Key, Value> dataKey,
            @Nonnull Collection<Key> keys, @Nonnull DataContext<Key, Value> context
    ) {
        DataProviderHelper<Key, Value> providerHelper = new DataProviderHelper<>(dataKey, context);
        providerHelper.dispatch(keys);
        Collection<CompletableFuture<Map<?, ?>>> futures =
                dataKeyFutures.computeIfAbsent(dataKey, (dk) -> new LinkedList<>());
        futures.add(CompletableFuture.supplyAsync(providerHelper::doProvide));
    }

    @SuppressWarnings("unchecked")
    public static <Key, Value> void submit(
            @Nonnull DataKey<Key, Value> dataKey,
            @Nonnull Collection<Key> keys
    ) {
        submit(dataKey, keys, DataContext.EMPTY_CACHEABLE_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    public static <Key, Value> void lazySubmit(
            @Nonnull DataKey<Key, Value> dataKey,
            @Nonnull Collection<Key> keys, @Nonnull DataContext<Key, Value> context
    ) {
        DataProviderHelper<Key, Value> providerHelper = (DataProviderHelper<Key, Value>) lazyDataKeyHelpers
                .computeIfAbsent(dataKey, dk -> new LinkedHashMap<>())
                .computeIfAbsent(context, c -> new DataProviderHelper<>(dataKey, (DataContext<Key, Value>) c));
        providerHelper.dispatch(keys);
    }

    public static <Key, Value> void lazySubmit(@Nonnull DataKey<Key, Value> dataKey, @Nonnull Collection<Key> keys) {
        lazySubmit(dataKey, keys, DataContext.EMPTY_CACHEABLE_CONTEXT);
    }

    @Nonnull
    public static <Key, Value> Map<Key, Value> getData(@Nonnull DataKey<Key, Value> dataKey) {
        Map<Key, Value> data = dataKey.data;
        try {
            data.putAll(doGetData(dataKey));
        } catch (Throwable e) {
            if (dataKey.onThrowingSupplier == null) {
                throw new RuntimeException(e);
            }
            data.putAll(dataKey.onThrowingSupplier.get());
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static <Key, Value> Map<Key, Value> doGetData(DataKey<Key, Value> dataKey)
            throws ExecutionException, TimeoutException {
        Map<Key, Value> finalResult = new HashMap<>();
        Collection<CompletableFuture<? extends Map<?, ?>>> lazyFutures =
                lazyDataKeyHelpers.getOrDefault(dataKey, emptyMap())
                        .values()
                        .stream()
                        .map(helper -> CompletableFuture.supplyAsync(helper::doProvide))
                        .collect(Collectors.toList());
        Collection<CompletableFuture<Map<?, ?>>> futures = dataKeyFutures.getOrDefault(dataKey, emptyList());
        if (!futures.isEmpty()) {
            CompletableFuture<List<Map<?, ?>>> allOfFuture = allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(fn -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                    );
            List<Map<?, ?>> result = Uninterruptibles.getUninterruptibly(allOfFuture, Duration.ofSeconds(5));
            result.forEach(map -> finalResult.putAll((Map<Key, Value>) map));
            futures.clear();
        }
        if (!lazyFutures.isEmpty()) {
            CompletableFuture<List<Map<?, ?>>> allOfLazyFuture = allOf(lazyFutures.toArray(new CompletableFuture[0]))
                    .thenApply(fn -> lazyFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                    );
            List<Map<?, ?>> lazyResult = Uninterruptibles.getUninterruptibly(allOfLazyFuture, Duration.ofSeconds(5));
            lazyResult.forEach(map -> finalResult.putAll((Map<Key, Value>) map));
            lazyDataKeyHelpers.get(dataKey).clear();
        }
        return finalResult;
    }
}
