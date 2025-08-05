package com.submicro.strukt.art.order;

import lombok.Getter;

public class OrderCommand {
    @Getter
    public long id;

    public int symbol;

    @Getter
    public long price;

    @Getter
    public long size;

    @Getter
    public long uid;

    @Getter
    public long timestamp;
}
