package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;
import lombok.Getter;

import java.util.Arrays;

public class ArtNode16<V> implements ArtNode<V> {

    @Getter
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
}
