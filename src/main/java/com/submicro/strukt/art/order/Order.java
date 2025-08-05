package com.submicro.strukt.art.order;

public class Order {

    public long id;

    public long price;

    public long size;

    public long filled;

    public long uid;

    public long timestamp;

    public OrderAction action;

    public long availableAmount() {
        return size - filled;
    }
}
