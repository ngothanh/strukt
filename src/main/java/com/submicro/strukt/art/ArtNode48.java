package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public class ArtNode48<V> implements ArtNode<V> {
    private final ObjectsPool objectsPool;

    long nodeKey;

    int nodeLevel;

    byte numChildren;

    public ArtNode48(ObjectsPool objectsPool) {
        this.objectsPool = objectsPool;
    }

    @Override
    public ArtNode<V> put(long key, int level, V value) {
        return null;
    }

    @Override
    public V get(long key, int level) {
        return null;
    }

    @Override
    public ObjectsPool getObjectsPool() {
        return null;
    }

    @Override
    public String printDiagram(String prefix, int level) {
        return "";
    }

    @Override
    public void recycle() {

    }

    public void initFromNode16(ArtNode16<V> node16, short keyByte, Object newElement) {

    }
}
