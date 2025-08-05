package com.submicro.strukt.art.order;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Collection;

public class OrderBucket {

    @Getter
    private final long price;

    private final LinkedHashMap<Long, Order> entries;

    @Getter
    long totalVolume; // package-private for direct access from TreeSetOrderBook

    public OrderBucket(final long price) {
        this.price = price;
        this.entries = new LinkedHashMap<>();
        this.totalVolume = 0;
    }

    public void put(Order order) {
        entries.put(order.id, order);
        totalVolume += order.availableAmount();
    }

    public void remove(Order order) {
        var entry = entries.get(order.id);
        if (entry == null || entry.uid != order.uid) {
            return;
        }
        entries.remove(order.id);
        totalVolume -= entry.availableAmount();
    }

    public Collection<Order> getOrders() {
        return entries.values();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
