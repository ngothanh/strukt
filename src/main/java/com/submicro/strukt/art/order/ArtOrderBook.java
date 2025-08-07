package com.submicro.strukt.art.order;

import com.submicro.strukt.art.LongAdaptiveRadixTreeMap;
import com.submicro.strukt.art.LongObjConsumer;
import com.submicro.strukt.art.pool.ObjectsPool;

public class ArtOrderBook implements OrderBook {

    private final LongAdaptiveRadixTreeMap<OrderBucket> askBuckets;
    private final LongAdaptiveRadixTreeMap<OrderBucket> bidBuckets;
    private final LongAdaptiveRadixTreeMap<Order> idMap;

    private Order bestAskOrder = null;
    private Order bestBidOrder = null;

    public ArtOrderBook() {
        this(ObjectsPool.createDefaultTestPool());
    }

    public ArtOrderBook(ObjectsPool objectsPool) {
        this.askBuckets = new LongAdaptiveRadixTreeMap<>(objectsPool);
        this.bidBuckets = new LongAdaptiveRadixTreeMap<>(objectsPool);
        this.idMap = new LongAdaptiveRadixTreeMap<>(objectsPool);
    }

    @Override
    public void newOrder(OrderCommand cmd) {
        final OrderAction action = cmd.action;
        final long price = cmd.price;
        final long size = cmd.size;

        final long filledSize = tryMatchInstantly(cmd);
        if (filledSize == size) return;

        long newOrderId = cmd.orderId;
        if (idMap.get(newOrderId) != null) return;

        final Order order = new Order(newOrderId, price, size, filledSize, action, cmd.uid, cmd.timestamp);

        final LongAdaptiveRadixTreeMap<OrderBucket> placed = getPlacedBucket(action);
        OrderBucket bucket = placed.get(price);
        if (bucket == null) {
            bucket = new OrderBucket(price);
            placed.put(price, bucket);
        }
        bucket.put(order);

        idMap.put(newOrderId, order);

        updateBestOrder(action);
    }

    private LongAdaptiveRadixTreeMap<OrderBucket> getMatchedBucket(OrderAction action) {
        return action == OrderAction.ASK ? bidBuckets : askBuckets;
    }

    private LongAdaptiveRadixTreeMap<OrderBucket> getPlacedBucket(OrderAction action) {
        return action == OrderAction.ASK ? askBuckets : bidBuckets;
    }

    private long tryMatchInstantly(final OrderCommand cmd) {
        long remainingSize = cmd.size;
        long filled = 0;

        Order current = getBestMatchingOrder(cmd.action);
        while (current != null && remainingSize > 0 && canMatch(cmd, current)) {
            long available = current.availableAmount();
            if (available == 0) {
                removeOrder(current);
                current = getBestMatchingOrder(cmd.action);
                continue;
            }

            long matchSize = Math.min(remainingSize, available);
            current.filled += matchSize;
            current.parent.totalVolume -= matchSize;

            remainingSize -= matchSize;
            filled += matchSize;

            if (current.availableAmount() == 0) {
                removeOrder(current);
                current = getBestMatchingOrder(cmd.action);
            }
        }

        return filled;
    }

    private Order getBestMatchingOrder(OrderAction incomingAction) {
        return incomingAction == OrderAction.ASK ? bestBidOrder : bestAskOrder;
    }

    private boolean canMatch(OrderCommand command, Order order) {
        return command.action == OrderAction.ASK ? order.price >= command.price : order.price <= command.price;
    }

    private void removeOrder(Order order) {
        idMap.remove(order.id);
        OrderBucket bucket = order.parent;

        if (order.prev != null) {
            order.prev.next = order.next;
        } else {
            bucket.head = order.next;
        }

        if (order.next != null) {
            order.next.prev = order.prev;
        } else {
            bucket.tail = order.prev;
        }

        bucket.numOrders--;
        bucket.totalVolume -= order.availableAmount();

        if (bucket.isEmpty()) {
            getPlacedBucket(order.action).remove(order.price);
        }

        order.prev = order.next = null;
        order.parent = null;

        updateBestOrder(order.action);
    }

    private void updateBestOrder(OrderAction action) {
        LongAdaptiveRadixTreeMap<OrderBucket> source = getPlacedBucket(action);
        if (action == OrderAction.ASK) {
            bestAskOrder = null;
            source.forEach((price, bucket) -> {
                if (bestAskOrder == null && !bucket.isEmpty()) {
                    bestAskOrder = bucket.head;
                }
            }, 1);
        } else {
            bestBidOrder = null;
            source.forEachDesc((price, bucket) -> {
                if (bestBidOrder == null && !bucket.isEmpty()) {
                    bestBidOrder = bucket.head;
                }
            }, 1);
        }
    }
}
