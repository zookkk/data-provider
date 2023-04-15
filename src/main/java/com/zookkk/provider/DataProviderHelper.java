package com.zookkk.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.zookkk.context.DataContext;
import com.zookkk.loader.DataKey;

/**
 * @author zookkk <1349054970@qq.com>
 */

public class DataProviderHelper<Key, Value> {
    private final Set<Key> unresolvedKeys = Sets.newHashSet();
    private final DataKey<Key, Value> dataKey;


    private final DataContext<Key, Value> context;

    public DataProviderHelper(@Nonnull DataKey<Key, Value> dataKey, @Nonnull DataContext<Key, Value> context) {
        this.dataKey = dataKey;
        this.context = context;
    }

    public void dispatch(@Nonnull Collection<Key> keys) {
        if (context.isEnableCache()) {
            Map<Key, Object> resolvingKeys = context.getResolvingKeys();
            keys.forEach(key -> {
                if (!context.getCaches().containsKey(key) && !resolvingKeys.containsKey(key)) {
                    resolvingKeys.put(key, 1);
                    unresolvedKeys.add(key);
                }
            });
        } else {
            unresolvedKeys.addAll(keys);
        }
    }

    public Map<Key, Value> doProvide() {
        if (CollectionUtils.isEmpty(unresolvedKeys)) {
            if (context.isEnableCache()) {
                return context.getCaches();
            }
            return Collections.emptyMap();
        }
        Map<Key, Value> dataResult = dataKey.getDataProvider().provide(unresolvedKeys, context);
        Map<Key, Value> caches = context.getCaches();
        caches.putAll(dataResult);
        unresolvedKeys.clear();
        if (context.isEnableCache()) {
            return caches;
        }
        Map<Key, Object> resolvingKeys = context.getResolvingKeys();
        dataResult.keySet().forEach(resolvingKeys::remove);
        return dataResult;
    }
}
