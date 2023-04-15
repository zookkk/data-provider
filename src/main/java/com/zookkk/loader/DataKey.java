package com.zookkk.loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.zookkk.provider.DataProvider;

/**
 * @author zookkk <1349054970@qq.com>
 */
public final class DataKey<Key, Value> {

    private final String keyName;
    final DataProvider<Key, Value> dataProvider;
    final Supplier<Map<Key, Value>> onThrowingSupplier;

    ConcurrentHashMap<Key, Value> data = new ConcurrentHashMap<>(); // TODO 线程级作用域

    private DataKey(@Nonnull String keyName, @Nonnull DataProvider<Key, Value> dataProvider) {
        this(keyName, dataProvider, null);
    }

    private DataKey(
            @Nonnull String keyName, @Nonnull DataProvider<Key, Value> dataProvider,
            @Nonnull Supplier<Map<Key, Value>> onThrowingSupplier
    ) {
        this.keyName = keyName;
        this.dataProvider = dataProvider;
        this.onThrowingSupplier = onThrowingSupplier;
    }

    public String getKeyName() {
        return this.keyName;
    }

    public DataProvider<Key, Value> getDataProvider() {
        return this.dataProvider;
    }

    public static <Key, Value> DataKey<Key, Value> newDataKey(
            @Nonnull String keyName,
            @Nonnull DataProvider<Key, Value> dataProvider
    ) {
        return new DataKey<>(keyName, dataProvider);
    }


    public static <Key, Value> DataKey<Key, Value> withThrowingSupplier(
            @Nonnull String keyName,
            @Nonnull DataProvider<Key, Value> dataProvider, @Nonnull Supplier<Map<Key, Value>> onThrowingSupplier
    ) {

        return new DataKey<>(keyName, dataProvider, onThrowingSupplier);
    }

}
