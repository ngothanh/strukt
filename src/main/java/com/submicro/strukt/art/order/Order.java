package com.submicro.strukt.art.order;

import lombok.Getter;

public class Order {

    public long id;

    public long price;

    public long size;

    public long filled;

    public long uid;

    public long timestamp;

    public long availableAmount() {
        return size - filled;
    }
}
