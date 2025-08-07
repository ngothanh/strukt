package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

import java.util.Arrays;

public class ArtNode256<V> implements ArtNode<V> {

    private static final int NODE48_SWITCH_THRESHOLD = 37;
    final Object[] nodes = new Object[256];

    long nodeKey;
    int nodeLevel;
    short numChildren;

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
    @SuppressWarnings("unchecked")
    public V getCeilingValue(long key, int level) {
        if ((level != nodeLevel)) {
            final long mask = -1L << (nodeLevel + 8);
            final long keyWithMask = key & mask;
            final long nodeKeyWithMask = nodeKey & mask;
            if (nodeKeyWithMask < keyWithMask) {
                return null;
            } else if (keyWithMask != nodeKeyWithMask) {
                key = 0;
            }
        }

        short idx = (short) ((key >>> nodeLevel) & 0xFF);
        Object node = nodes[idx];
        if (node != null) {
            final V res = nodeLevel == 0 ? (V) node : ((ArtNode<V>) node).getCeilingValue(key, nodeLevel - 8);
            if (res != null) {
                return res;
            }
        }

        while (++idx < 256) {
            node = nodes[idx];
            if (node != null) {
                return (nodeLevel == 0)
                        ? (V) node
                        : ((ArtNode<V>) node).getCeilingValue(0, nodeLevel - 8);// find first lowest key
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getFloorValue(long key, int level) {
        if ((level != nodeLevel)) {
            // try first
            final long mask = -1L << (nodeLevel + 8);
            final long keyWithMask = key & mask;
            final long nodeKeyWithMask = nodeKey & mask;
            if (nodeKeyWithMask > keyWithMask) {
                return null;
            } else if (keyWithMask != nodeKeyWithMask) {
                key = Long.MAX_VALUE;
            }
        }

        short idx = (short) ((key >>> nodeLevel) & 0xFF);
        Object node = nodes[idx];
        if (node != null) {
            final V res = nodeLevel == 0 ? (V) node : ((ArtNode<V>) node).getFloorValue(key, nodeLevel - 8);
            if (res != null) {
                return res;
            }
        }

        while (--idx >= 0) {
            node = nodes[idx];
            if (node != null) {
                return (nodeLevel == 0)
                        ? (V) node
                        : ((ArtNode<V>) node).getFloorValue(Long.MAX_VALUE, nodeLevel - 8);// find first highest key
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArtNode<V> remove(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return this;
        }
        final short idx = (short) ((key >>> nodeLevel) & 0xFF);
        if (nodes[idx] == null) {
            return this;
        }

        if (nodeLevel == 0) {
            nodes[idx] = null;
            numChildren--;
        } else {
            final ArtNode<V> node = (ArtNode<V>) nodes[idx];
            final ArtNode<V> resizedNode = node.remove(key, nodeLevel - 8);
            if (resizedNode != node) {
                nodes[idx] = resizedNode;
                if (resizedNode == null) {
                    numChildren--;
                }
            }
        }

        if (numChildren == NODE48_SWITCH_THRESHOLD) {
            final ArtNode48<V> newNode = objectsPool.get(ObjectsPool.ART_NODE_48, ArtNode48::new);
            newNode.initFromNode256(this);
            return newNode;
        } else {
            return this;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int forEach(LongObjConsumer<V> consumer, int limit) {
        if (nodeLevel == 0) {
            final long keyBase = (nodeKey >>> 8) << 8;
            int numFound = 0;
            for (short i = 0; i < 256; i++) {
                if (numFound == limit) {
                    return numFound;
                }
                final V node = (V) nodes[i];
                if (node != null) {
                    consumer.accept(keyBase + i, node);
                    numFound++;
                }
            }
            return numFound;
        } else {
            int numLeft = limit;
            for (short i = 0; i < 256 && numLeft > 0; i++) {
                final ArtNode<V> node = (ArtNode<V>) nodes[i];
                if (node != null) {
                    numLeft -= node.forEach(consumer, numLeft);
                }
            }
            return limit - numLeft;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int forEachDesc(LongObjConsumer<V> consumer, int limit) {
        if (nodeLevel == 0) {
            final long keyBase = (nodeKey >>> 8) << 8;
            int numFound = 0;
            for (short i = 255; i >= 0; i--) {
                if (numFound == limit) {
                    return numFound;
                }
                final V node = (V) nodes[i];
                if (node != null) {
                    consumer.accept(keyBase + i, node);
                    numFound++;
                }
            }
            return numFound;
        } else {
            int numLeft = limit;
            for (short i = 255; i >= 0 && numLeft > 0; i--) {
                final ArtNode<V> node = (ArtNode<V>) nodes[i];
                if (node != null) {
                    numLeft -= node.forEachDesc(consumer, numLeft);
                }
            }
            return limit - numLeft;
        }
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

    @Override
    public ObjectsPool getObjectsPool() {
        return objectsPool;
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
