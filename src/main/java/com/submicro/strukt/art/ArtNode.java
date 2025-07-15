package com.submicro.strukt.art;

public interface ArtNode<V> {

    ArtNode<V> put(long key, int level, V value);

    default boolean requiresBranching(long key, int level) {
        //TODO
    }

    default ArtNode<V> createBranch(long key, V value, int level) {
        //TODO
    }
}