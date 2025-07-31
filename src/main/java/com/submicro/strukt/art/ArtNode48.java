package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

import java.util.Arrays;

public class ArtNode48<V> implements ArtNode<V> {

    private final ObjectsPool objectsPool;

    long nodeKey;

    int nodeLevel;

    byte numChildren;

    final byte[] indexes = new byte[48];
    final Object[] nodes = new Object[48];
    private long freeBitMask;

    public ArtNode48(ObjectsPool objectsPool) {
        this.objectsPool = objectsPool;
    }

    @Override
    public ArtNode<V> put(long key, int level, V value) {
        if (level != nodeLevel) {
            final ArtNode<V> branch = LongAdaptiveRadixTreeMap.branchIfRequired(key, value, this, nodeKey, nodeLevel);
            if (branch != null) {
                return branch;
            }
        }

        final short idx = (short) ((key >>> nodeLevel) & 0xFF);
        final byte nodeIndex = indexes[idx];
        if (nodeIndex != -1) {
            // found
            if (nodeLevel == 0) {
                nodes[nodeIndex] = value;
            } else {
                final ArtNode<V> resizedNode = ((ArtNode<V>) nodes[nodeIndex]).put(key, nodeLevel - 8, value);
                if (resizedNode != null) {
                    nodes[nodeIndex] = resizedNode;
                }
            }
            return null;
        }

        if (numChildren != 48) {
            final byte freePosition = (byte) Long.numberOfTrailingZeros(~freeBitMask);
            indexes[idx] = freePosition;

            if (nodeLevel == 0) {
                nodes[freePosition] = value;
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                nodes[freePosition] = newSubNode;
            }
            numChildren++;
            freeBitMask = freeBitMask ^ (1L << freePosition);
            return null;

        } else {
            // no space left, create a ArtNode256 containing a new item
            final Object newElement;
            if (nodeLevel == 0) {
                newElement = value;
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                newElement = newSubNode;
            }

            ArtNode256<V> node256 = objectsPool.get(ObjectsPool.ART_NODE_256, ArtNode256::new);
            node256.initFromNode48(this, idx, newElement);

            return node256;
        }
    }

    @Override
    public V get(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return null;
        }
        final short idx = (short) ((key >>> nodeLevel) & 0xFF);
        final byte nodeIndex = indexes[idx];
        if (nodeIndex != -1) {
            final Object node = nodes[nodeIndex];
            return nodeLevel == 0
                    ? (V) node
                    : ((ArtNode<V>) node).get(key, nodeLevel - 8);
        }
        return null;
    }

    @Override
    public ObjectsPool getObjectsPool() {
        return objectsPool;
    }

    @Override
    public String printDiagram(String prefix, int level) {
        return "";
    }

    @Override
    public void recycle() {
        Arrays.fill(nodes, null);
        Arrays.fill(indexes, (byte) -1);
        objectsPool.put(ObjectsPool.ART_NODE_48, this);
    }


    public void initFromNode16(ArtNode16<V> node16, short keyByte, Object newElement) {
        final byte sourceSize = 16;
        Arrays.fill(this.indexes, (byte) -1);
        this.numChildren = sourceSize + 1;
        this.nodeLevel = node16.nodeLevel;
        this.nodeKey = node16.nodeKey;

        for (byte i = 0; i < sourceSize; i++) {
            this.indexes[node16.keys[i]] = i;
            this.nodes[i] = node16.nodes[i];
        }

        this.indexes[keyByte] = sourceSize;
        this.nodes[sourceSize] = newElement;
        this.freeBitMask = (1L << (sourceSize + 1)) - 1;

        Arrays.fill(node16.nodes, null);
        objectsPool.put(ObjectsPool.ART_NODE_16, node16);
    }
}
