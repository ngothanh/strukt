package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public interface ArtNode<V> {

    ArtNode<V> put(long key, int level, V value);

    V get(long key, int level);

    ObjectsPool getObjectsPool();
}