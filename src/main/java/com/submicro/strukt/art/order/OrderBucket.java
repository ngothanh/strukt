package com.submicro.strukt.art.order;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class OrderBucket {
    long totalVolume;
    int numOrders;
    Order tail;
    Order head;
    long price;

    public OrderBucket() {
    }

    public OrderBucket(long price) {
        this.price = price;
    }

    public boolean isEmpty() {
        return numOrders == 0 || head == null;
    }

    public void put(Order order) {
        order.parent = this;
        totalVolume += order.availableAmount();
        numOrders++;

        if (head == null) {
            head = tail = order;
            order.next = order.prev = null;
        } else {
            // Add to tail (FIFO order)
            tail.next = order;
            order.prev = tail;
            order.next = null;
            tail = order;
        }
    }

    public Iterable<Order> getOrders() {
        return () -> new OrderIterator();
    }

    private class OrderIterator implements Iterator<Order> {
        private Order current = head;
        private Order lastReturned = null;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Order next() {
            if (current == null) {
                throw new NoSuchElementException();
            }
            lastReturned = current;
            current = current.next;
            return lastReturned;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            removeOrder(lastReturned);
            lastReturned = null;
        }
    }

    private void removeOrder(Order order) {
        numOrders--;
        totalVolume -= order.availableAmount();

        if (order.prev != null) {
            order.prev.next = order.next;
        } else {
            head = order.next;
        }

        if (order.next != null) {
            order.next.prev = order.prev;
        } else {
            tail = order.prev;
        }

        order.next = order.prev = null;
        order.parent = null;
    }
}
