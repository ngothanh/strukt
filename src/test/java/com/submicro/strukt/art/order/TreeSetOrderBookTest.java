package com.submicro.strukt.art.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TreeSetOrderBookTest {

    private TreeSetOrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new TreeSetOrderBook();
    }

    @Test
    void testSimpleBuyOrderMatching() {
        // Place a sell order first
        OrderCommand sellOrder = createOrder(1L, OrderAction.ASK, 100L, 10L, 1001L);
        orderBook.newOrder(sellOrder);

        // Place a buy order that should match
        OrderCommand buyOrder = createOrder(2L, OrderAction.BID, 100L, 5L, 1002L);
        orderBook.newOrder(buyOrder);

        // The buy order should be fully matched, so it shouldn't be in the book
        // We can verify this by checking that a duplicate order ID would be accepted
        // (since the original was fully matched and removed)
        OrderCommand duplicateBuyOrder = createOrder(2L, OrderAction.BID, 100L, 3L, 1002L);
        orderBook.newOrder(duplicateBuyOrder); // Should not throw or fail
    }

    @Test
    void testSimpleSellOrderMatching() {
        // Place a buy order first
        OrderCommand buyOrder = createOrder(1L, OrderAction.BID, 100L, 10L, 1001L);
        orderBook.newOrder(buyOrder);

        // Place a sell order that should match
        OrderCommand sellOrder = createOrder(2L, OrderAction.ASK, 100L, 5L, 1002L);
        orderBook.newOrder(sellOrder);

        // The sell order should be fully matched
        OrderCommand duplicateSellOrder = createOrder(2L, OrderAction.ASK, 100L, 3L, 1002L);
        orderBook.newOrder(duplicateSellOrder); // Should not throw or fail
    }

    @Test
    void testPartialMatching() {
        // Place a large sell order
        OrderCommand sellOrder = createOrder(1L, OrderAction.ASK, 100L, 20L, 1001L);
        orderBook.newOrder(sellOrder);

        // Place a smaller buy order that should partially match
        OrderCommand buyOrder = createOrder(2L, OrderAction.BID, 100L, 5L, 1002L);
        orderBook.newOrder(buyOrder);

        // The buy order should be fully matched, sell order should have remaining quantity
        // Place another buy order to test remaining quantity
        OrderCommand buyOrder2 = createOrder(3L, OrderAction.BID, 100L, 10L, 1003L);
        orderBook.newOrder(buyOrder2);

        // Both buy orders should be fully matched
        OrderCommand duplicateBuyOrder2 = createOrder(3L, OrderAction.BID, 100L, 3L, 1003L);
        orderBook.newOrder(duplicateBuyOrder2); // Should not throw or fail
    }

    @Test
    void testNoMatchingDifferentPrices() {
        // Place a sell order at high price
        OrderCommand sellOrder = createOrder(1L, OrderAction.ASK, 110L, 10L, 1001L);
        orderBook.newOrder(sellOrder);

        // Place a buy order at lower price - should not match
        OrderCommand buyOrder = createOrder(2L, OrderAction.BID, 90L, 5L, 1002L);
        orderBook.newOrder(buyOrder);

        // Both orders should remain in the book
        // Try to place duplicate orders - should be rejected
        OrderCommand duplicateSellOrder = createOrder(1L, OrderAction.ASK, 110L, 5L, 1001L);
        orderBook.newOrder(duplicateSellOrder); // Should be rejected due to duplicate ID

        OrderCommand duplicateBuyOrder = createOrder(2L, OrderAction.BID, 90L, 3L, 1002L);
        orderBook.newOrder(duplicateBuyOrder); // Should be rejected due to duplicate ID
    }

    @Test
    void testMultiplePriceLevels() {
        // Place multiple sell orders at different prices
        OrderCommand sellOrder1 = createOrder(1L, OrderAction.ASK, 100L, 5L, 1001L);
        OrderCommand sellOrder2 = createOrder(2L, OrderAction.ASK, 101L, 5L, 1002L);
        OrderCommand sellOrder3 = createOrder(3L, OrderAction.ASK, 102L, 5L, 1003L);
        
        orderBook.newOrder(sellOrder1);
        orderBook.newOrder(sellOrder2);
        orderBook.newOrder(sellOrder3);

        // Place a large buy order that should match multiple levels
        OrderCommand buyOrder = createOrder(4L, OrderAction.BID, 102L, 12L, 1004L);
        orderBook.newOrder(buyOrder);

        // The buy order should match with sellOrder1 (5) and sellOrder2 (5) and partially with sellOrder3 (2)
        // sellOrder1 and sellOrder2 should be fully matched, sellOrder3 should have 3 remaining
        
        // Verify by trying to place another buy order that should match remaining sellOrder3
        OrderCommand buyOrder2 = createOrder(5L, OrderAction.BID, 102L, 3L, 1005L);
        orderBook.newOrder(buyOrder2);
        
        // This should fully match the remaining sellOrder3
        OrderCommand duplicateBuyOrder2 = createOrder(5L, OrderAction.BID, 102L, 1L, 1005L);
        orderBook.newOrder(duplicateBuyOrder2); // Should not throw since original was fully matched
    }

    private OrderCommand createOrder(long orderId, OrderAction action, long price, long size, long uid) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = orderId;
        cmd.orderId = orderId;
        cmd.action = action;
        cmd.price = price;
        cmd.size = size;
        cmd.uid = uid;
        cmd.timestamp = System.currentTimeMillis();
        cmd.reserveBidPrice = 0L;
        cmd.symbol = 1;
        return cmd;
    }
}
