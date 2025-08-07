package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public interface ArtNode<V> {

    ArtNode<V> put(long key, int level, V value);

    V get(long key, int level);

    V getCeilingValue(long key, int level);

    V getFloorValue(long key, int level);

    ArtNode<V> remove(long key, int level);

    int forEach(LongObjConsumer<V> consumer, int limit);

    int forEachDesc(LongObjConsumer<V> consumer, int limit);

    ObjectsPool getObjectsPool();

    String printDiagram(String prefix, int level);

    void recycle();
}