package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                root = upSizedNode;
            }
        }
        // log.debug(printDiagram()); // Commented out for now
    }

    public V remove(final long key) {
        if (root == null) {
            return null;
        }

        V removedValue = root.get(key, INITIAL_LEVEL);
        if (removedValue != null) {
            // Use the proper remove method
            ArtNode<V> newRoot = root.remove(key, INITIAL_LEVEL);
            root = newRoot; // newRoot can be null if the tree becomes empty
        }
        return removedValue;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public String printDiagram() {
        if (root != null) {
            return root.printDiagram("", INITIAL_LEVEL);
        } else {
            return "";
        }
    }

    static <V> ArtNode<V> branchIfRequired(
            final long key,
            final V value,
            final ArtNode<V> node,
            long nodeKey,
            int nodeLevel
    ) {
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

        final ObjectsPool objectsPool = node.getObjectsPool();
        final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
        newSubNode.initFirstKey(key, value);

        final ArtNode4<V> newNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
        newNode.initTwoKeys(nodeKey, node, key, newSubNode, newLevel);

        return newNode;
    }
}
