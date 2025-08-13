package com.submicro.strukt.art.order;

import com.submicro.strukt.art.LongAdaptiveRadixTreeMap;
import com.submicro.strukt.art.pool.ObjectsPool;

public class ArtOrderBook implements OrderBook {

    private final LongAdaptiveRadixTreeMap<OrderBucket> askBuckets;
    private final LongAdaptiveRadixTreeMap<OrderBucket> bidBuckets;
    private final LongAdaptiveRadixTreeMap<Order> idMap;

    private long   bestAskPrice = Long.MAX_VALUE;
    private Order  bestAskOrder = null;

    private long   bestBidPrice = Long.MIN_VALUE;
    private Order  bestBidOrder = null;

    public ArtOrderBook() {
        this(ObjectsPool.createDefaultTestPool());
    }

    public ArtOrderBook(ObjectsPool pool) {
        this.askBuckets = new LongAdaptiveRadixTreeMap<>(pool);
        this.bidBuckets = new LongAdaptiveRadixTreeMap<>(pool);
        this.idMap      = new LongAdaptiveRadixTreeMap<>(pool);
    }

    @Override
    public void newOrder(OrderCommand cmd) {
        final long orderId = cmd.orderId;
        if (idMap.get(orderId) != null) return;

        final long size  = cmd.size;
        if (size <= 0) return;

        final long filled = tryMatchInstantly(cmd);
        if (filled == size) return;

        final long price = cmd.price;
        final Order order = new Order(orderId, price, size, filled, cmd.action, cmd.uid, cmd.timestamp);

        final LongAdaptiveRadixTreeMap<OrderBucket> side = placedSide(cmd.action);
        OrderBucket bucket = side.get(price);
        if (bucket == null) {
            bucket = new OrderBucket(price);
            side.put(price, bucket);
        }
        bucket.put(order);
        idMap.put(orderId, order);

        if (cmd.action == OrderAction.ASK) {
            if (bestAskOrder == null || price < bestAskPrice) {
                bestAskPrice = price;
                bestAskOrder = bucket.head;
            }
        } else {
            if (bestBidOrder == null || price > bestBidPrice) {
                bestBidPrice = price;
                bestBidOrder = bucket.head;
            }
        }
    }

    private LongAdaptiveRadixTreeMap<OrderBucket> placedSide(OrderAction a) {
        return (a == OrderAction.ASK) ? askBuckets : bidBuckets;
    }

    private long tryMatchInstantly(final OrderCommand cmd) {
        long remaining = cmd.size;
        if (remaining <= 0) return 0;

        if (cmd.action == OrderAction.ASK) {
            while (remaining > 0 && bestBidOrder != null && bestBidPrice >= cmd.price) {
                Order cur = bestBidOrder;
                long avail = cur.availableAmount();
                if (avail == 0) { removeOrder(cur); continue; }

                long match = (remaining < avail) ? remaining : avail;
                cur.filled += match;
                cur.parent.totalVolume -= match;

                remaining -= match;

                if (cur.availableAmount() == 0) {
                    removeOrder(cur);
                }
            }
        } else {
            while (remaining > 0 && bestAskOrder != null && bestAskPrice <= cmd.price) {
                Order cur = bestAskOrder;
                long avail = cur.availableAmount();
                if (avail == 0) { removeOrder(cur); continue; }

                long match = (remaining < avail) ? remaining : avail;
                cur.filled += match;
                cur.parent.totalVolume -= match;

                remaining -= match;

                if (cur.availableAmount() == 0) {
                    removeOrder(cur);
                }
            }
        }
        return cmd.size - remaining;
    }

    private void removeOrder(Order order) {
        final OrderBucket bucket = order.parent;
        final boolean wasBestAsk = (order == bestAskOrder);
        final boolean wasBestBid = (order == bestBidOrder);
        final long avail = order.availableAmount();

        idMap.remove(order.id);

        if (order.prev != null) order.prev.next = order.next;
        else bucket.head = order.next;
        if (order.next != null) order.next.prev = order.prev;
        else bucket.tail = order.prev;

        bucket.numOrders--;
        bucket.totalVolume -= avail;

        final boolean emptied = bucket.isEmpty();
        if (emptied) {
            placedSide(order.action).remove(order.price);
        }

        if (wasBestAsk) {
            if (!emptied) {
                bestAskOrder = bucket.head;   // same price level, O(1)
                bestAskPrice = bucket.price;
            } else {
                final Order next = findNextBestAsk();
                bestAskOrder = next;
                bestAskPrice = (next != null) ? next.price : Long.MAX_VALUE;
            }
        }
        if (wasBestBid) {
            if (!emptied) {
                bestBidOrder = bucket.head;
                bestBidPrice = bucket.price;
            } else {
                final Order next = findNextBestBid();
                bestBidOrder = next;
                bestBidPrice = (next != null) ? next.price : Long.MIN_VALUE;
            }
        }

        order.prev = order.next = null;
        order.parent = null;
    }

    private Order findNextBestAsk() {
        final Order[] out = new Order[1];
        askBuckets.forEach((p, b) -> {
            if (b.head != null) { out[0] = b.head; }
        }, 1);
        return out[0];
    }

    private Order findNextBestBid() {
        final Order[] out = new Order[1];
        bidBuckets.forEachDesc((p, b) -> {
            if (b.head != null) { out[0] = b.head; }
        }, 1);
        return out[0];
    }
}
