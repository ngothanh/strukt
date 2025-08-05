package com.submicro.strukt.art.order;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TreeSetOrderBook implements OrderBook {

    private final NavigableMap<Long, OrderBucket> askBuckets = new TreeMap<>();

    private final NavigableMap<Long, OrderBucket> bidBuckets = new TreeMap<>(Comparator.reverseOrder());

    private final LongObjectHashMap<Order> idMap = new LongObjectHashMap<>();


    @Override
    public void newOrder(OrderCommand cmd) {
        final OrderAction action = cmd.action;
        final long price = cmd.price;
        final long size = cmd.size;

        final long filledSize = tryMatchInstantly(cmd);
        if (filledSize == size) {
            // order was matched completely - nothing to place - can just return
            return;
        }

        long newOrderId = cmd.orderId;
        if (idMap.containsKey(newOrderId)) {
            // duplicate order id - can match, but can not place
            return;
        }

        final Order orderRecord = new Order(
                newOrderId,
                price,
                size,
                filledSize,
                action,
                cmd.uid,
                cmd.timestamp);

        getPlacedBucket(action)
                .computeIfAbsent(price, OrderBucket::new)
                .put(orderRecord);

        idMap.put(newOrderId, orderRecord);
    }

    private NavigableMap<Long, OrderBucket> getMatchedBucket(OrderAction action) {
        return action == OrderAction.ASK ? bidBuckets : askBuckets;
    }

    private NavigableMap<Long, OrderBucket> getPlacedBucket(OrderAction action) {
        return action == OrderAction.ASK ? askBuckets : bidBuckets;
    }

    private long tryMatchInstantly(final OrderCommand command) {
        long remainingSize = command.size;
        long filled = 0;

        var matchedBucket = getMatchedBucket(command.action);
        var matchingBuckets = matchedBucket.headMap(command.price, true);
        var bucketIterator = matchingBuckets.entrySet().iterator();

        while (bucketIterator.hasNext() && remainingSize > 0) {
            var bucketEntry = bucketIterator.next();
            OrderBucket bucket = bucketEntry.getValue();

            if (bucket.isEmpty()) {
                bucketIterator.remove();
                continue;
            }

            var orderIterator = bucket.getOrders().iterator();

            while (orderIterator.hasNext() && remainingSize > 0) {
                Order order = orderIterator.next();

                long availableAmount = order.availableAmount();
                if (availableAmount == 0) {
                    orderIterator.remove();
                    continue;
                }

                long matchSize = Math.min(remainingSize, availableAmount);

                order.filled += matchSize;
                bucket.totalVolume -= matchSize;

                remainingSize -= matchSize;
                filled += matchSize;

                if (order.availableAmount() == 0) {
                    orderIterator.remove();
                    idMap.remove(order.id);
                }
            }

            if (bucket.isEmpty()) {
                bucketIterator.remove();
            }
        }

        return filled;
    }
}
