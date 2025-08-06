package com.submicro.strukt.art.order;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TreeSetOrderBook implements OrderBook {

    private final NavigableMap<Long, OrderBucket> askBuckets = new TreeMap<>();

    private final NavigableMap<Long, OrderBucket> bidBuckets = new TreeMap<>(Comparator.reverseOrder());

    private final LongObjectHashMap<Order> idMap = new LongObjectHashMap<>();

    private Order bestAskOrder = null;

    private Order bestBidOrder = null;

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

        OrderBucket bucket = getPlacedBucket(action)
                .computeIfAbsent(price, OrderBucket::new);
        bucket.put(orderRecord);

        idMap.put(newOrderId, orderRecord);

        // Update best order tracking
        updateBestOrder(orderRecord);
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

        // Get the best order to match against
        Order bestOrder = getBestMatchingOrder(command.action);

        while (bestOrder != null && remainingSize > 0 && canMatch(command, bestOrder)) {
            long availableAmount = bestOrder.availableAmount();
            if (availableAmount == 0) {
                // Remove fully filled order and get next
                removeOrder(bestOrder);
                bestOrder = getBestMatchingOrder(command.action);
                continue;
            }

            long matchSize = Math.min(remainingSize, availableAmount);

            // Execute the match
            bestOrder.filled += matchSize;
            bestOrder.parent.totalVolume -= matchSize;

            remainingSize -= matchSize;
            filled += matchSize;

            if (bestOrder.availableAmount() == 0) {
                // Order is fully filled, remove it
                removeOrder(bestOrder);
                bestOrder = getBestMatchingOrder(command.action);
            }
        }

        return filled;
    }

    private Order getBestMatchingOrder(OrderAction incomingAction) {
        // For incoming ASK (sell), match against best BID (buy)
        // For incoming BID (buy), match against best ASK (sell)
        return incomingAction == OrderAction.ASK ? bestBidOrder : bestAskOrder;
    }

    private boolean canMatch(OrderCommand command, Order order) {
        if (command.action == OrderAction.ASK) {
            // Selling: can match if bid price >= ask price
            return order.price >= command.price;
        } else {
            // Buying: can match if ask price <= bid price
            return order.price <= command.price;
        }
    }

    private void removeOrder(Order order) {
        idMap.remove(order.id);
        OrderBucket bucket = order.parent;

        // Remove from bucket's linked list
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

        // Update best order tracking
        updateBestOrderAfterRemoval(order);

        // Clean up bucket if empty
        if (bucket.isEmpty()) {
            getPlacedBucket(order.action).remove(order.price);
        }

        order.next = order.prev = null;
        order.parent = null;
    }

    private void updateBestOrder(Order newOrder) {
        if (newOrder.action == OrderAction.ASK) {
            // For asks, best is lowest price
            if (bestAskOrder == null || newOrder.price < bestAskOrder.price) {
                bestAskOrder = newOrder;
            }
        } else {
            // For bids, best is highest price
            if (bestBidOrder == null || newOrder.price > bestBidOrder.price) {
                bestBidOrder = newOrder;
            }
        }
    }

    private void updateBestOrderAfterRemoval(Order removedOrder) {
        if (removedOrder == bestAskOrder) {
            bestAskOrder = findNextBestAsk();
        }
        if (removedOrder == bestBidOrder) {
            bestBidOrder = findNextBestBid();
        }
    }

    private Order findNextBestAsk() {
        // Find the order with the lowest price in ask buckets
        for (var entry : askBuckets.entrySet()) {
            OrderBucket bucket = entry.getValue();
            if (!bucket.isEmpty()) {
                return bucket.head; // First order in the bucket (FIFO)
            }
        }
        return null;
    }

    private Order findNextBestBid() {
        // Find the order with the highest price in bid buckets
        for (var entry : bidBuckets.entrySet()) {
            OrderBucket bucket = entry.getValue();
            if (!bucket.isEmpty()) {
                return bucket.head; // First order in the bucket (FIFO)
            }
        }
        return null;
    }
}
