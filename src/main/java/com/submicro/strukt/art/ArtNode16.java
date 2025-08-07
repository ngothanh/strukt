package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

import java.util.Arrays;

public class ArtNode16<V> implements ArtNode<V> {

    private static final int NODE4_SWITCH_THRESHOLD = 3;

    private final ObjectsPool objectsPool;

    long nodeKey;

    int nodeLevel;

    byte numChildren;

    final short[] keys = new short[16];

    final Object[] nodes = new Object[16];

    public ArtNode16(ObjectsPool objectsPool) {
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

        final short nodeIndex = (short) ((key >>> nodeLevel) & 0xFF);
        int pos = 0;
        while (pos < numChildren) {
            if (nodeIndex == keys[pos]) {
                if (nodeLevel == 0) {
                    nodes[pos] = value;
                } else {
                    final ArtNode<V> resizedNode = ((ArtNode<V>) nodes[pos]).put(key, nodeLevel - 8, value);
                    if (resizedNode != null) {
                        nodes[pos] = resizedNode;
                    }
                }
                return null;
            }
            if (nodeIndex < keys[pos]) {
                // can give up searching because keys are in sorted order
                break;
            }
            pos++;
        }

        if (numChildren != 16) {
            final int copyLength = numChildren - pos;
            if (copyLength != 0) {
                System.arraycopy(keys, pos, keys, pos + 1, copyLength);
                System.arraycopy(nodes, pos, nodes, pos + 1, copyLength);
            }
            keys[pos] = nodeIndex;
            if (nodeLevel == 0) {
                nodes[pos] = value;
            } else {
                final ArtNode4<V> newSubNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
                newSubNode.initFirstKey(key, value);
                nodes[pos] = newSubNode;
                newSubNode.put(key, nodeLevel - 8, value);
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

            ArtNode48<V> node48 = objectsPool.get(ObjectsPool.ART_NODE_48, ArtNode48::new);
            node48.initFromNode16(this, nodeIndex, newElement);

            return node48;
        }
    }

    @Override
    public V get(long key, int level) {
        if (level != nodeLevel && ((key ^ nodeKey) & (-1L << (nodeLevel + 8))) != 0) {
            return null;
        }
        final short keyByte = (short) ((key >>> nodeLevel) & 0xFF);
        for (int i = 0; i < numChildren; i++) {
            final short branchByte = keys[i];
            if (branchByte == keyByte) {
                final Object node = nodes[i];
                return nodeLevel == 0
                        ? (V) node
                        : ((ArtNode<V>) node).get(key, nodeLevel - 8);
            }
            if (keyByte < branchByte) {
                break;
            }
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

        final short nodeIndex = (short) ((key >>> nodeLevel) & 0xFF);

        for (int i = 0; i < numChildren; i++) {
            final short index = keys[i];
            if (index == nodeIndex) {
                final V res = nodeLevel == 0
                        ? (V) nodes[i]
                        : ((ArtNode<V>) nodes[i]).getCeilingValue(key, nodeLevel - 8);
                if (res != null) {
                    return res;
                }
            }
            if (index > nodeIndex) {
                return nodeLevel == 0
                        ? (V) nodes[i]
                        : ((ArtNode<V>) nodes[i]).getCeilingValue(0, nodeLevel - 8); // take lowest existing key
            }
        }
        return null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public V getFloorValue(long key, int level) {
        if ((level != nodeLevel)) {
            final long mask = -1L << (nodeLevel + 8);
            final long keyWithMask = key & mask;
            final long nodeKeyWithMask = nodeKey & mask;
            if (nodeKeyWithMask > keyWithMask) {
                return null;
            } else if (keyWithMask != nodeKeyWithMask) {
                key = Long.MAX_VALUE;
            }
        }

        final short nodeIndex = (short) ((key >>> nodeLevel) & 0xFF);

        for (int i = numChildren - 1; i >= 0; i--) {
            final short index = keys[i];
            if (index == nodeIndex) {
                final V res = nodeLevel == 0
                        ? (V) nodes[i]
                        : ((ArtNode<V>) nodes[i]).getFloorValue(key, nodeLevel - 8);
                if (res != null) {
                    return res;
                }
            }
            if (index < nodeIndex) {
                return nodeLevel == 0
                        ? (V) nodes[i]
                        : ((ArtNode<V>) nodes[i]).getFloorValue(Long.MAX_VALUE, nodeLevel - 8); // take highest existing key
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
        final short nodeIndex = (short) ((key >>> nodeLevel) & 0xFF);
        Object node = null;
        int pos = 0;
        while (pos < numChildren) {
            if (nodeIndex == keys[pos]) {
                // found
                node = nodes[pos];
                break;
            }
            if (nodeIndex < keys[pos]) {
                // can give up searching because keys are in sorted order
                return this;
            }
            pos++;
        }

        if (node == null) {
            // not found
            return this;
        }

        // removing
        if (nodeLevel == 0) {
            removeElementAtPos(pos);
        } else {
            final ArtNode<V> resizedNode = ((ArtNode<V>) node).remove(key, nodeLevel - 8);
            if (resizedNode != node) {
                // update resized node if capacity has decreased
                nodes[pos] = resizedNode;
                if (resizedNode == null) {
                    removeElementAtPos(pos);
                }
            }
        }

        // switch to ArtNode4 if too small
        if (numChildren == NODE4_SWITCH_THRESHOLD) {
            final ArtNode4<V> newNode = objectsPool.get(ObjectsPool.ART_NODE_4, ArtNode4::new);
            newNode.initFromNode16(this);
            return newNode;
        } else {
            return this;
        }
    }

    private void removeElementAtPos(final int pos) {
        final int ppos = pos + 1;
        final int copyLength = numChildren - ppos;
        if (copyLength != 0) {
            System.arraycopy(keys, ppos, keys, pos, copyLength);
            System.arraycopy(nodes, ppos, nodes, pos, copyLength);
        }
        numChildren--;
        nodes[numChildren] = null;
    }

    @Override
    public ObjectsPool getObjectsPool() {
        return objectsPool;
    }

    @Override
    public String printDiagram(String prefix, int level) {
        StringBuilder sb = new StringBuilder();

        sb.append(prefix)
                .append("ArtNode16[level=").append(nodeLevel)
                .append(", key=0x").append(Long.toHexString(nodeKey))
                .append(", children=").append(numChildren).append("]\n");

        for (int i = 0; i < numChildren; i++) {
            sb.append(prefix).append("├─ key[").append(i).append("]=0x")
                    .append(String.format("%02X", keys[i] & 0xFF));

            if (nodeLevel == 0) {
                sb.append(" → ").append(nodes[i]).append("\n");
            } else {
                sb.append("\n");
                String childPrefix = prefix + "│  ";
                sb.append(((ArtNode<V>) nodes[i]).printDiagram(childPrefix, nodeLevel - 8));
            }
        }

        return sb.toString();
    }

    @Override
    public void recycle() {
        Arrays.fill(nodes, null);
        objectsPool.put(ObjectsPool.ART_NODE_16, this);
    }

    public void initFromNode4(ArtNode4<V> node4, short subKey, Object newElement) {
        final byte sourceSize = node4.numChildren;
        this.nodeLevel = node4.nodeLevel;
        this.nodeKey = node4.nodeKey;
        this.numChildren = (byte) (sourceSize + 1);
        int inserted = 0;
        for (int i = 0; i < sourceSize; i++) {
            final int key = node4.keys[i];
            if (inserted == 0 && key > subKey) {
                keys[i] = subKey;
                nodes[i] = newElement;
                inserted = 1;
            }
            keys[i + inserted] = node4.keys[i];
            nodes[i + inserted] = node4.nodes[i];
        }
        if (inserted == 0) {
            keys[sourceSize] = subKey;
            nodes[sourceSize] = newElement;
        }

        node4.recycle();
    }

    public void initFromNode48(ArtNode48<V> node48) {
        this.nodeLevel = node48.nodeLevel;
        this.nodeKey = node48.nodeKey;
        this.numChildren = (byte) node48.numChildren;

        // Copy non-null entries from node48
        int index = 0;
        for (short i = 0; i < 256 && index < numChildren; i++) {
            byte nodeIndex = node48.indexes[i];
            if (nodeIndex != -1) {
                keys[index] = i;
                nodes[index] = node48.nodes[nodeIndex];
                index++;
            }
        }

        node48.recycle();
    }
}
