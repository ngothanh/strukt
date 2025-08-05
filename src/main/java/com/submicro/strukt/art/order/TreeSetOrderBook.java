package com.submicro.strukt.art.order;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

public class TreeSetOrderBook implements OrderBook {

    private final NavigableMap<Long, OrderBucket> askBuckets = new TreeMap<>();

    private final NavigableMap<Long, OrderBucket> bidBuckets = new TreeMap<>(Comparator.reverseOrder());

    private final LongObjectHashMap<Order> idMap = new LongObjectHashMap<>();


    @Override
    public void newOrder(OrderCommand cmd) {

    }
}
