package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

import java.util.Arrays;

public class ArtNode48<V> implements ArtNode<V> {

    private final ObjectsPool objectsPool;

    long nodeKey;

    int nodeLevel;

    byte numChildren;

    final byte[] indexes = new byte[256];  // Must be 256 to handle all possible byte values
    final Object[] nodes = new Object[48]; // Only 48 actual storage slots
    long freeBitMask; // Package-private for testing

    public ArtNode48(ObjectsPool objectsPool) {
        this.objectsPool = objectsPool;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArtNode<V> put(final long key, final int level, final V value) {
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

        // not found, put new element

        if (numChildren != 48) {
            // capacity less than 48 - can simply insert node
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
        final int idx = (int) ((key >>> nodeLevel) & 0xFF);
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
    public ArtNode<V> remove(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return this; // Key not found, return unchanged
        }

        final int idx = (int) ((key >>> nodeLevel) & 0xFF);
        final byte nodeIndex = indexes[idx];
        if (nodeIndex != -1) {
            if (nodeLevel == 0) {
                // Leaf level - remove this entry
                removeChildAt(idx, nodeIndex);

                // Check if we should shrink to ArtNode16
                if (numChildren <= 16) {
                    ArtNode16<V> node16 = objectsPool.get(ObjectsPool.ART_NODE_16, ArtNode16::new);
                    node16.initFromNode48(this);
                    return node16;
                }
                return numChildren == 0 ? null : this;
            } else {
                // Internal node - recurse
                ArtNode<V> childNode = (ArtNode<V>) nodes[nodeIndex];
                ArtNode<V> newChild = childNode.remove(key, nodeLevel - 8);

                if (newChild == null) {
                    // Child was removed completely
                    removeChildAt(idx, nodeIndex);

                    // Check if we should shrink to ArtNode16
                    if (numChildren <= 16) {
                        ArtNode16<V> node16 = objectsPool.get(ObjectsPool.ART_NODE_16, ArtNode16::new);
                        node16.initFromNode48(this);
                        return node16;
                    }
                    return numChildren == 0 ? null : this;
                } else if (newChild != childNode) {
                    // Child was replaced
                    nodes[nodeIndex] = newChild;
                }
                return this;
            }
        }
        return this; // Key not found, return unchanged
    }

    private void removeChildAt(int idx, byte nodeIndex) {
        // Remove the mapping
        indexes[idx] = -1;
        nodes[nodeIndex] = null;
        numChildren--;
        freeBitMask ^= (1L << nodeIndex); // mark as free
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


    public void initFromNode16(ArtNode16<V> node16, int keyByte, Object newElement) {
        final byte sourceSize = 16;
        Arrays.fill(this.indexes, (byte) -1);  // Initialize all 256 indexes to -1
        this.numChildren = sourceSize + 1;
        this.nodeLevel = node16.nodeLevel;
        this.nodeKey = node16.nodeKey;

        for (byte i = 0; i < sourceSize; i++) {
            this.indexes[node16.keys[i] & 0xFF] = i;  // Ensure proper indexing
            this.nodes[i] = node16.nodes[i];
        }

        this.indexes[keyByte & 0xFF] = sourceSize;  // Ensure proper indexing
        this.nodes[sourceSize] = newElement;
        this.freeBitMask = (1L << (sourceSize + 1)) - 1;

        Arrays.fill(node16.nodes, null);
        objectsPool.put(ObjectsPool.ART_NODE_16, node16);
    }

    public void initFromNode256(ArtNode256<V> node256) {
        this.nodeLevel = node256.nodeLevel;
        this.nodeKey = node256.nodeKey;
        this.numChildren = (byte) node256.numChildren;

        // Initialize all 256 indexes to -1
        Arrays.fill(indexes, (byte) -1);

        // Copy non-null entries from node256
        int nodeIndex = 0;
        for (short i = 0; i < 256 && nodeIndex < numChildren; i++) {
            if (node256.nodes[i] != null) {
                indexes[i] = (byte) nodeIndex;
                nodes[nodeIndex] = node256.nodes[i];
                nodeIndex++;
            }
        }

        this.freeBitMask = (1L << numChildren) - 1;

        node256.recycle();
    }
}
