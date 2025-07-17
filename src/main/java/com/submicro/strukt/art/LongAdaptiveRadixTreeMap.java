package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public class LongAdaptiveRadixTreeMap<V> {
    private static final int INITIAL_LEVEL = 56;

    private ArtNode<V> root = null;

    private final ObjectsPool objectsPool;

    public LongAdaptiveRadixTreeMap(ObjectsPool objectsPool) {
        this.objectsPool = objectsPool;
    }

    public LongAdaptiveRadixTreeMap() {
        objectsPool = ObjectsPool.createDefaultTestPool();
    }

    public V get(final long key) {
        return root != null
                ? root.get(key, INITIAL_LEVEL)
                : null;
    }

    public void put(final long key, final V value) {
        if (root == null) {
            final ArtNode4<V> node = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
            node.initFirstKey(key, value);
            root = node;
        } else {

            final ArtNode<V> upSizedNode = root.put(key, INITIAL_LEVEL, value);
            if (upSizedNode != null) {
                // TODO put old into the pool
                root = upSizedNode;
            }
        }
    }

    static <V> ArtNode<V> branchIfRequired(final long key, final V value, final long nodeKey, final int nodeLevel, final ArtNode<V> caller) {

        final long keyDiff = key ^ nodeKey;

        // check if there is common part
        if ((keyDiff & (-1L << nodeLevel)) == 0) {
            return null;
        }

        // on which level
        final int newLevel = (63 - Long.numberOfLeadingZeros(keyDiff)) & 0xF8;
        if (newLevel == nodeLevel) {
            return null;
        }

        final ObjectsPool objectsPool = caller.getObjectsPool();
        final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
        newSubNode.initFirstKey(key, value);

        final ArtNode4<V> newNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
        newNode.initTwoKeys(nodeKey, caller, key, newSubNode, newLevel);

        return newNode;
    }
}
