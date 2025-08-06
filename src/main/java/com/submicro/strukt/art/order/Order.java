package com.submicro.strukt.art.order;

public class Order {

    public long id;

    public long price;

    public long size;

    public long filled;

    public long uid;

    public long timestamp;

    public OrderAction action;

    OrderBucket parent;

    // next order (towards the matching direction, price grows for asks)
    Order next;

    // previous order (to the tail of the queue, lower priority and worst price, towards the matching direction)
    Order prev;

    public Order() {
    }

    public Order(long id, long price, long size, long filled, OrderAction action, long uid, long timestamp) {
        this.id = id;
        this.price = price;
        this.size = size;
        this.filled = filled;
        this.action = action;
        this.uid = uid;
        this.timestamp = timestamp;
    }

    public long availableAmount() {
        return size - filled;
    }
}
