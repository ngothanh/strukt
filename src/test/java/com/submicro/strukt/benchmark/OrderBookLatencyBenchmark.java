package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * OrderBook Latency Benchmark Suite
 * 
 * Measures individual operation latency distributions using SampleTime mode.
 * Provides detailed percentile analysis (p50, p90, p95, p99, p99.9, p99.99, max).
 * 
 * Configuration: 5 forks, 5 warmup, 10 measurement iterations
 * Mode: SampleTime for detailed latency distribution analysis
 * Dataset sizes: 10K, 100K, 1M orders for scaling analysis
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5, jvmArgs = {"-Xmx8G", "-Xms8G", "-XX:+UseG1GC", "-XX:+AlwaysPreTouch"})
@Threads(1)
public class OrderBookLatencyBenchmark {

    /**
     * Latency: Pure Insert - Single order insertion without matches
     * Measures individual newOrder() call performance for insert operations
     */
    @Benchmark
    public long latency01_PureInsert(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Execute single insert operation
        OrderCommand order = state.pureInsertOrders.get(0);
        book.newOrder(order);
        
        // DCE prevention
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Pure Match - Single order matching against pre-filled book
     * Measures individual newOrder() call performance for match operations
     */
    @Benchmark
    public long latency02_PureMatch(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with opposing liquidity
        OrderCommand preFill = new OrderCommand();
        preFill.orderId = 999L;
        preFill.action = OrderAction.BID;
        preFill.price = state.PRICE_MID;
        preFill.size = 1000L;
        preFill.uid = 1000L;
        preFill.timestamp = System.nanoTime();
        preFill.symbol = 1;
        book.newOrder(preFill);
        
        // Execute single match operation
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 100L;
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Partial Match - Single order with partial fill
     * Measures individual newOrder() call performance for partial match operations
     */
    @Benchmark
    public long latency03_PartialMatch(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with small opposing order
        OrderCommand preFill = new OrderCommand();
        preFill.orderId = 999L;
        preFill.action = OrderAction.BID;
        preFill.price = state.PRICE_MID;
        preFill.size = 50L; // Smaller than incoming order
        preFill.uid = 1000L;
        preFill.timestamp = System.nanoTime();
        preFill.symbol = 1;
        book.newOrder(preFill);
        
        // Execute partial match operation
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 200L; // Larger than pre-fill
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Random Mix - Single order in realistic mixed environment
     * Measures individual newOrder() call performance in realistic trading environment
     */
    @Benchmark
    public long latency04_RandomMix(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with mixed book (realistic spread)
        for (int i = 0; i < 50; i++) {
            OrderCommand bid = new OrderCommand();
            bid.orderId = i * 2;
            bid.action = OrderAction.BID;
            bid.price = state.PRICE_MID - 10 - i;
            bid.size = 100L;
            bid.uid = 1000L;
            bid.timestamp = System.nanoTime();
            bid.symbol = 1;
            book.newOrder(bid);
            
            OrderCommand ask = new OrderCommand();
            ask.orderId = i * 2 + 1;
            ask.action = OrderAction.ASK;
            ask.price = state.PRICE_MID + 10 + i;
            ask.size = 100L;
            ask.uid = 1000L;
            ask.timestamp = System.nanoTime();
            ask.symbol = 1;
            book.newOrder(ask);
        }
        
        // Execute single order in mixed environment
        OrderCommand order = state.randomMixOrders.get(0);
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Hotspot Single Price - Order at concentrated price level
     * Measures individual newOrder() call performance when orders concentrate at same price
     */
    @Benchmark
    public long latency05_HotspotSinglePrice(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with many orders at hotspot price
        for (int i = 0; i < 100; i++) {
            OrderCommand hotspotOrder = new OrderCommand();
            hotspotOrder.orderId = i;
            hotspotOrder.action = OrderAction.BID;
            hotspotOrder.price = state.PRICE_MID;
            hotspotOrder.size = 100L;
            hotspotOrder.uid = 1000L;
            hotspotOrder.timestamp = System.nanoTime();
            hotspotOrder.symbol = 1;
            book.newOrder(hotspotOrder);
        }
        
        // Execute order at hotspot price
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 100L;
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Cold Book Sparse - Order in sparse price environment
     * Measures individual newOrder() call performance in sparse order book
     */
    @Benchmark
    public long latency07_ColdBookSparse(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with sparse levels
        for (long price = state.PRICE_MID - state.PRICE_SPREAD; 
             price <= state.PRICE_MID + state.PRICE_SPREAD; 
             price += state.SPARSE_LEVEL_SPACING) {
            if (price != state.PRICE_MID) {
                OrderCommand sparseOrder = new OrderCommand();
                sparseOrder.orderId = price;
                sparseOrder.action = price < state.PRICE_MID ? OrderAction.BID : OrderAction.ASK;
                sparseOrder.price = price;
                sparseOrder.size = 100L;
                sparseOrder.uid = 1000L;
                sparseOrder.timestamp = System.nanoTime();
                sparseOrder.symbol = 1;
                book.newOrder(sparseOrder);
            }
        }
        
        // Execute order at new sparse level
        OrderCommand order = state.coldBookSparseOrders.get(0);
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Wide Dense Book - Order in dense price environment
     * Measures individual newOrder() call performance in dense order book
     */
    @Benchmark
    public long latency08_WideDenseBook(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with dense levels
        for (long price = state.PRICE_MID - 100; price <= state.PRICE_MID + 100; price += state.TICK_SIZE) {
            if (price != state.PRICE_MID) {
                OrderCommand denseOrder = new OrderCommand();
                denseOrder.orderId = price;
                denseOrder.action = price < state.PRICE_MID ? OrderAction.BID : OrderAction.ASK;
                denseOrder.price = price;
                denseOrder.size = 10L;
                denseOrder.uid = 1000L;
                denseOrder.timestamp = System.nanoTime();
                denseOrder.symbol = 1;
                book.newOrder(denseOrder);
            }
        }
        
        // Execute order in dense environment
        OrderCommand order = state.wideDenseBookOrders.get(0);
        book.newOrder(order);
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        
        return order.orderId;
    }

    /**
     * Latency: Duplicate Submit - Order with duplicate ID (should be rejected)
     * Measures individual newOrder() call performance for duplicate order handling
     */
    @Benchmark
    public long latency09_DuplicateSubmit(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Submit original order
        OrderCommand original = new OrderCommand();
        original.orderId = 1000L;
        original.action = OrderAction.ASK;
        original.price = state.PRICE_MID + 10;
        original.size = 100L;
        original.uid = 1000L;
        original.timestamp = System.nanoTime();
        original.symbol = 1;
        book.newOrder(original);
        
        // Submit duplicate (should be rejected)
        OrderCommand duplicate = new OrderCommand();
        duplicate.orderId = 1000L; // Same ID
        duplicate.action = OrderAction.BID;
        duplicate.price = state.PRICE_MID - 10;
        duplicate.size = 200L;
        duplicate.uid = 2000L;
        duplicate.timestamp = System.nanoTime();
        duplicate.symbol = 1;
        book.newOrder(duplicate);
        
        bh.consume(duplicate.orderId);
        bh.consume(duplicate.price);
        bh.consume(duplicate.size);
        
        return duplicate.orderId;
    }
}
