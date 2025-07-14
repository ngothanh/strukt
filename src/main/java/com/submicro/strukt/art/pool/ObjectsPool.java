package com.submicro.strukt.art.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectsPool {
    public static final int ORDER = 0;

    public static final int DIRECT_ORDER = 1;
    public static final int DIRECT_BUCKET = 2;
    public static final int ART_NODE_4 = 8;
    public static final int ART_NODE_16 = 9;
    public static final int ART_NODE_48 = 10;
    public static final int ART_NODE_256 = 11;

    public static final int SYMBOL_POSITION_RECORD = 12;

    private final ArrayStack[] pools;

    public static ObjectsPool createDefaultTestPool() {
        final Map<Integer, Integer> objectsPoolConfig = new HashMap<>();
        objectsPoolConfig.put(ObjectsPool.DIRECT_ORDER, 512);
        objectsPoolConfig.put(ObjectsPool.DIRECT_BUCKET, 256);
        objectsPoolConfig.put(ObjectsPool.ART_NODE_4, 256);
        objectsPoolConfig.put(ObjectsPool.ART_NODE_16, 128);
        objectsPoolConfig.put(ObjectsPool.ART_NODE_48, 64);
        objectsPoolConfig.put(ObjectsPool.ART_NODE_256, 32);

        return new ObjectsPool(objectsPoolConfig);
    }

    public ObjectsPool(final Map<Integer, Integer> sizesConfig) {
        Set<Integer> keys = sizesConfig.keySet();

        int maxStack = 0;
        for (int type : keys) {
            if (type > maxStack) {
                maxStack = type;
            }
        }

        this.pools = new ArrayStack[maxStack + 1];

        for (int type : keys) {
            this.pools[type] = new ArrayStack(sizesConfig.get(type));
        }
    }

    private final static class ArrayStack {
        private int size = 0;
        private final Object[] elements;

        ArrayStack(int fixedSize) {
            this.elements = new Object[fixedSize];
            this.size = 0;
        }

        void add(Object element) {
            if (size == elements.length) return;
            elements[size++] = element;
        }

        Object pop() {
            if (size == 0) return null;
            size--;
            var element = elements[size];
            elements[size] = null;
            return element;
        }
    }
}
