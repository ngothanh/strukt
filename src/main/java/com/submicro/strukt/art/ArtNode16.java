package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public class ArtNode16<V> implements ArtNode<V> {

    private final ObjectsPool objectsPool;

    public ArtNode16(ObjectsPool objectsPool) {
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
        return objectsPool;
    }

    public void initFromNode4(ArtNode4<V> node4, short subKey, Object newElement) {

    }
}
