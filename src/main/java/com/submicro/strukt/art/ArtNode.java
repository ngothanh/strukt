package com.submicro.strukt.art;

public interface ArtNode<V> {

    V get(long key);

    ArtNode<V> insert(long key, V value);

    ArtNode<V> delete(long key);
}
