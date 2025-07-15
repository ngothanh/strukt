package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

public class ArtNode4<V> implements ArtNode<V> {

    final short[] keys = new short[4];
    final Object[] nodes = new Object[4];
    long nodeKey;
    int nodeLevel;
    byte numChildren;

    private final ObjectsPool objectsPool;

    public ArtNode4(ObjectsPool objectsPool) {
        this.objectsPool = objectsPool;
    }

    public void initFirstKey(long key, V value) {
        this.numChildren = 1;
        this.keys[0] = (short) (key & 0xFF);
        this.nodes[0] = value;
        this.nodeKey = key;
        this.nodeLevel = 0;
    }

    @Override
    public ArtNode<V> put(long key, int level, V value) {
        if (requiresBranching(key, level)) {
            return createBranch(key, value, level);
        }

        final short keyByte = (short) ((key >>> nodeLevel) & 0xFF);
        int i;
        for (i = 0; i < numChildren; i++) {
            short k = keys[i];
            if (keyByte == k) {
                if (nodeLevel == 0) {
                    nodes[i] = value;
                } else {
                    final ArtNode<V> resizedNode = ((ArtNode<V>) nodes[i]).put(key, nodeLevel - 8, value);
                    if (resizedNode != null) {
                        nodes[i] = resizedNode;
                    }
                }
                return null;
            } else if (keyByte < k) {
                break;
            }
        }

        if (numChildren != 4) {
            final int copyLength = numChildren - i;
            if (copyLength != 0) {
                System.arraycopy(keys, i, keys, i + 1, copyLength);
                System.arraycopy(nodes, i, nodes, i + 1, copyLength);
            }
            keys[i] = keyByte;
            if (nodeLevel == 0) {
                nodes[i] = value;
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                nodes[i] = newSubNode;
            }
            numChildren++;
            return null;
        } else {
            final Object newElement;
            if (nodeLevel == 0) {
                newElement = value;
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                newElement = newSubNode;
            }

            ArtNode16<V> node16 = objectsPool.get(ObjectsPool.ART_NODE_16, ArtNode16::new);
            node16.initFromNode4(this, keyByte, newElement);

            return node16;
        }
    }

    @Override
    public V get(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return null;
        }
        final short keyByte = (short) ((key >>> nodeLevel) & 0xFF);
        for (int i = 0; i < numChildren; i++) {
            final short index = keys[i];
            if (index == keyByte) {
                final Object node = nodes[i];
                return nodeLevel == 0
                        ? (V) node
                        : ((ArtNode<V>) node).get(key, nodeLevel - 8);
            }
            if (keyByte < index) {
                // can give up searching because keys are in sorted order
                break;
            }
        }
        return null;
    }
}
