package com.zookkk.provider;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import com.zookkk.context.DataContext;

/**
 * @author zookkk <1349054970@qq.com>
 */
public interface DataProvider<Key, Value> {
    Map<Key, Value> provide(@Nonnull Collection<Key> keys, @Nonnull DataContext<Key, Value> context);
}
