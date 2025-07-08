package com.submicro.strukt.art;

import java.util.Optional;

public interface ArtNode<V> {

    Optional<V> get(byte[] key);

    ArtNode<V> insert(byte[] key, V value);

    ArtNode<V> delete(byte[] key);

    ArtNodeType getNodeType();
}
