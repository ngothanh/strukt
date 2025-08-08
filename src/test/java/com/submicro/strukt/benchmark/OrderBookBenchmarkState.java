package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Shared state and data generation utilities for OrderBook benchmarks.
 * Provides realistic order data generation for various testing scenarios.
 */
@State(Scope.Benchmark)
public class OrderBookBenchmarkState {
    
    // Benchmark parameters
    @Param({"10000", "100000", "1000000"})
    public int datasetSize;
    
    @Param({"ART", "TreeSet"})
    public String orderBookType;
    
    // Order data arrays
    public OrderCommand[] pureInsertOrders;
    public OrderCommand[] pureMatchOrders;
    public OrderCommand[] partialMatchOrders;
    public OrderCommand[] randomMixOrders;
    public OrderCommand[] hotspotOrders;
    public OrderCommand[] coldBookOrders;
    
    // OrderBook instances
    public OrderBook orderBook;
    public OrderBook prefilledOrderBook;
    public OrderBook partialMatchOrderBook;
    public OrderBook randomMixOrderBook;
    
    // Constants for realistic price generation
    private static final long BASE_PRICE = 100_000L; // $1000.00 in cents
    private static final long PRICE_RANGE = 10_000L; // $100.00 spread
    private static final long MIN_SIZE = 100L;
    private static final long MAX_SIZE = 10_000L;
    
    private Random random;
    private long orderIdCounter;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        random = new Random(42); // Fixed seed for reproducibility
        orderIdCounter = 1L;
        
        // Create OrderBook instance based on parameter
        orderBook = createOrderBook();
        
        // Generate order data for all scenarios
        generatePureInsertOrders();
        generatePureMatchOrders();
        generatePartialMatchOrders();
        generateRandomMixOrders();
        generateHotspotOrders();
        generateColdBookOrders();
        
        // Setup pre-filled order books for specific scenarios
        setupPrefilledOrderBooks();
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Reset order book for each iteration
        orderBook = createOrderBook();
    }
    
    public OrderBook createOrderBook() {
        switch (orderBookType) {
            case "ART":
                return new ArtOrderBook();
            case "TreeSet":
                return new TreeSetOrderBook();
            default:
                throw new IllegalArgumentException("Unknown order book type: " + orderBookType);
        }
    }
    
    /**
     * Scenario 1: Pure Insert - No matches, all orders added to book
     * ASK orders above best bid, BID orders below best ask
     */
    private void generatePureInsertOrders() {
        pureInsertOrders = new OrderCommand[datasetSize];
        
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = (i % 2 == 0) ? OrderAction.ASK : OrderAction.BID;
            long price;
            
            if (action == OrderAction.ASK) {
                // ASK orders above base price to avoid matches
                price = BASE_PRICE + PRICE_RANGE + (i % 1000);
            } else {
                // BID orders below base price to avoid matches
                price = BASE_PRICE - PRICE_RANGE - (i % 1000);
            }
            
            pureInsertOrders[i] = createOrderCommand(action, price, randomSize());
        }
    }
    
    /**
     * Scenario 2: Pure Match - 100% matched orders
     * Pre-fill with large opposing orders, then send matching orders
     */
    private void generatePureMatchOrders() {
        pureMatchOrders = new OrderCommand[datasetSize];
        
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = (i % 2 == 0) ? OrderAction.BID : OrderAction.ASK;
            long price = BASE_PRICE; // All at same price to guarantee matches
            
            pureMatchOrders[i] = createOrderCommand(action, price, MIN_SIZE);
        }
    }
    
    /**
     * Scenario 3: Partial Match - 50% matched, remainder inserted
     * Each order partially matched, overflow gets inserted
     */
    private void generatePartialMatchOrders() {
        partialMatchOrders = new OrderCommand[datasetSize];
        
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = (i % 2 == 0) ? OrderAction.BID : OrderAction.ASK;
            long price = BASE_PRICE + (random.nextInt(21) - 10); // ±10 price levels
            long size = MIN_SIZE * 2; // Larger orders for partial matching
            
            partialMatchOrders[i] = createOrderCommand(action, price, size);
        }
    }
    
    /**
     * Scenario 4: Random Mix - 70% unmatched, 30% matched
     * Random price ranges to simulate realistic L2 activity
     */
    private void generateRandomMixOrders() {
        randomMixOrders = new OrderCommand[datasetSize];
        
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price;
            
            if (random.nextDouble() < 0.3) {
                // 30% chance of matching order
                price = BASE_PRICE + (random.nextInt(11) - 5); // ±5 levels around base
            } else {
                // 70% chance of non-matching order
                if (action == OrderAction.ASK) {
                    price = BASE_PRICE + PRICE_RANGE/2 + random.nextInt((int)PRICE_RANGE/2);
                } else {
                    price = BASE_PRICE - PRICE_RANGE/2 - random.nextInt((int)PRICE_RANGE/2);
                }
            }
            
            randomMixOrders[i] = createOrderCommand(action, price, randomSize());
        }
    }
    
    /**
     * Scenario 5: Hotspot Match - All orders target same price level
     * High write pressure on one bucket
     */
    private void generateHotspotOrders() {
        hotspotOrders = new OrderCommand[datasetSize];
        long hotspotPrice = BASE_PRICE;
        
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = (i % 2 == 0) ? OrderAction.BID : OrderAction.ASK;
            hotspotOrders[i] = createOrderCommand(action, hotspotPrice, MIN_SIZE);
        }
    }
    
    /**
     * Scenario 6: Cold Book - Empty every time
     * Insert → immediately matched → removed, book size always ~0
     */
    private void generateColdBookOrders() {
        coldBookOrders = new OrderCommand[datasetSize];
        
        for (int i = 0; i < datasetSize; i++) {
            // Alternating orders that will match each other
            OrderAction action = (i % 2 == 0) ? OrderAction.ASK : OrderAction.BID;
            long price = BASE_PRICE;
            
            coldBookOrders[i] = createOrderCommand(action, price, MIN_SIZE);
        }
    }
    
    private void setupPrefilledOrderBooks() {
        // Setup for pure match scenario
        prefilledOrderBook = createOrderBook();
        // Add large opposing orders
        prefilledOrderBook.newOrder(createOrderCommand(OrderAction.ASK, BASE_PRICE, datasetSize * MAX_SIZE));
        prefilledOrderBook.newOrder(createOrderCommand(OrderAction.BID, BASE_PRICE, datasetSize * MAX_SIZE));
        
        // Setup for partial match scenario
        partialMatchOrderBook = createOrderBook();
        for (int i = 0; i < 100; i++) {
            long price = BASE_PRICE + (i % 21 - 10);
            partialMatchOrderBook.newOrder(createOrderCommand(OrderAction.ASK, price, MIN_SIZE));
            partialMatchOrderBook.newOrder(createOrderCommand(OrderAction.BID, price, MIN_SIZE));
        }
        
        // Setup for random mix scenario
        randomMixOrderBook = createOrderBook();
        for (int i = 0; i < 50; i++) {
            long askPrice = BASE_PRICE + PRICE_RANGE/4 + (i % 100);
            long bidPrice = BASE_PRICE - PRICE_RANGE/4 - (i % 100);
            randomMixOrderBook.newOrder(createOrderCommand(OrderAction.ASK, askPrice, randomSize()));
            randomMixOrderBook.newOrder(createOrderCommand(OrderAction.BID, bidPrice, randomSize()));
        }
    }
    
    private OrderCommand createOrderCommand(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = orderIdCounter;
        cmd.orderId = orderIdCounter++;
        cmd.action = action;
        cmd.price = price;
        cmd.size = size;
        cmd.uid = random.nextLong();
        cmd.timestamp = System.currentTimeMillis();
        cmd.reserveBidPrice = 0L;
        cmd.symbol = 1;
        return cmd;
    }
    
    private long randomSize() {
        return MIN_SIZE + random.nextInt((int)(MAX_SIZE - MIN_SIZE));
    }
}
