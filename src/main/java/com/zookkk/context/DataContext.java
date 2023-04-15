package com.zookkk.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;

/**
 * @author zookkk <1349054970@qq.com>
 */

public class DataContext<Key, Value> {
    public static final DataContext EMPTY_CACHEABLE_CONTEXT = new DataContext<>(ImmutableMap.builder().build(), true);
    public static final DataContext EMPTY_NON_CACHEABLE_CONTEXT = new DataContext<>(ImmutableMap.builder().build(), false);
    private final ImmutableMap<Object, Object> environment;

    private final boolean enableCache;

    private final ConcurrentHashMap<Key, Value> caches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, Object> resolvingKeys = new ConcurrentHashMap<>();

    protected DataContext(@Nonnull ImmutableMap<Object, Object> environment, boolean enableCache) {
        this.environment = environment;
        this.enableCache = enableCache;
    }

    public Map<Object, Object> getEnvironment() {
        return environment;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public Map<Key, Value> getCaches() {
        return caches;
    }

    public ConcurrentHashMap<Key, Object> getResolvingKeys() {
        return resolvingKeys;
    }

    public static class Builder {

        private ImmutableMap<Object, Object> environment;

        private boolean enableCache;

        public Builder environment(@Nonnull ImmutableMap<Object, Object> environment) {
            this.environment = environment;
            return this;
        }

        public Builder enableCache(boolean enableCache) {
            this.enableCache = enableCache;
            return this;
        }

        public <Key, Value> DataContext<Key, Value> build() {
            return new DataContext<>(environment, enableCache);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
