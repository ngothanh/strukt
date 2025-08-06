package com.submicro.strukt.art;

import com.submicro.strukt.art.pool.ObjectsPool;

import java.util.Arrays;

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
        if (level != nodeLevel) {
            final ArtNode<V> branch = LongAdaptiveRadixTreeMap.branchIfRequired(key, value, this, nodeKey, nodeLevel);
            if (branch != null) {
                return branch;
            }
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
                nodes[pos] = resizedNode;
                if (resizedNode == null) {
                    removeElementAtPos(pos);
                    if (numChildren == 1) {
                        ArtNode<V> lastNode = (ArtNode<V>) nodes[0];
                        return lastNode;
                    }
                }
            }
        }

        if (numChildren == 0) {
            // indicate if removed last one
            Arrays.fill(nodes, null);
            objectsPool.put(ObjectsPool.ART_NODE_4, this);
            return null;
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

        sb.append(prefix).append("ArtNode4[level=").append(nodeLevel)
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
        objectsPool.put(ObjectsPool.ART_NODE_4, this);
    }

    void initTwoKeys(final long key1, final Object value1, final long key2, final Object value2, final int level) {
        this.numChildren = 2;
        final short idx1 = (short) ((key1 >> level) & 0xFF);
        final short idx2 = (short) ((key2 >> level) & 0xFF);
        if (key1 < key2) {
            this.keys[0] = idx1;
            this.nodes[0] = value1;
            this.keys[1] = idx2;
            this.nodes[1] = value2;
        } else {
            this.keys[0] = idx2;
            this.nodes[0] = value2;
            this.keys[1] = idx1;
            this.nodes[1] = value1;
        }
        this.nodeKey = key1;
        this.nodeLevel = level;
    }

    void initFromNode16(ArtNode16<V> node16) {
        this.nodeLevel = node16.nodeLevel;
        this.nodeKey = node16.nodeKey;
        this.numChildren = node16.numChildren;

        // Copy keys and nodes from node16
        System.arraycopy(node16.keys, 0, this.keys, 0, numChildren);
        System.arraycopy(node16.nodes, 0, this.nodes, 0, numChildren);

        node16.recycle();
    }
}
