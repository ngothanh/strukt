package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;
import lombok.Getter;

import java.util.Arrays;

public class ArtNode256<V> implements ArtNode<V> {

    private static final int NODE48_SWITCH_THRESHOLD = 37;
    final Object[] nodes = new Object[256];

    long nodeKey;
    int nodeLevel;
    short numChildren;

    @Getter
    private final ObjectsPool objectsPool;

    public ArtNode256(ObjectsPool objectsPool) {
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
        if (nodes[idx] == null) {
            // new object will be inserted
            numChildren++;
        }

        if (nodeLevel == 0) {
            nodes[idx] = value;
        } else {
            ArtNode<V> node = (ArtNode<V>) nodes[idx];
            if (node != null) {
                final ArtNode<V> resizedNode = node.put(key, nodeLevel - 8, value);
                if (resizedNode != null) {
                    nodes[idx] = resizedNode;
                }
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                nodes[idx] = newSubNode;
            }
        }

        return null;
    }

    @Override
    public V get(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return null;
        }
        final short idx = (short) ((key >>> nodeLevel) & 0xFF);
        final Object node = nodes[idx];
        if (node != null) {
            return nodeLevel == 0
                    ? (V) node
                    : ((ArtNode<V>) node).get(key, nodeLevel - 8);
        }
        return null;
    }

    @Override
    public String printDiagram(String prefix, int level) {
        return "";
    }

    @Override
    public void recycle() {
        Arrays.fill(nodes, null);
        objectsPool.put(ObjectsPool.ART_NODE_256, this);
    }

    public void initFromNode48(ArtNode48<V> node48, short keyByte, Object newElement) {
        this.nodeLevel = node48.nodeLevel;
        this.nodeKey = node48.nodeKey;
        final int sourceSize = 48;
        for (short i = 0; i < 256; i++) {
            final byte index = node48.indexes[i];
            if (index != -1) {
                this.nodes[i] = node48.nodes[index];
            }
        }
        this.nodes[keyByte] = newElement;
        this.numChildren = sourceSize + 1;
        node48.recycle();
    }
}
