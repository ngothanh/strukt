package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public interface ArtNode<V> {

    long getNodeKey();

    int getNodeLevel();

    byte getNumChildren();

    ArtNode<V> put(long key, int level, V value);

    V get(long key, int level);

    ObjectsPool getObjectsPool();

    String printDiagram(String prefix, int level);
}