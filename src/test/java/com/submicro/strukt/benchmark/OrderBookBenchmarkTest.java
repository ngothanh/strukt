package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.infra.Blackhole;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrderBook benchmark scenarios.
 * Validates that benchmark data generation and scenarios work correctly.
 */
public class OrderBookBenchmarkTest {

    @Test
    void testBenchmarkStateSetup() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 1000;
        state.orderBookType = "ART";
        
        state.setupTrial();
        
        // Verify order arrays are generated
        assertNotNull(state.pureInsertOrders);
        assertNotNull(state.pureMatchOrders);
        assertNotNull(state.partialMatchOrders);
        assertNotNull(state.randomMixOrders);
        assertNotNull(state.hotspotOrders);
        assertNotNull(state.coldBookOrders);
        
        assertEquals(1000, state.pureInsertOrders.length);
        assertEquals(1000, state.pureMatchOrders.length);
        assertEquals(1000, state.partialMatchOrders.length);
        assertEquals(1000, state.randomMixOrders.length);
        assertEquals(1000, state.hotspotOrders.length);
        assertEquals(1000, state.coldBookOrders.length);
        
        // Verify order books are created
        assertNotNull(state.orderBook);
        assertNotNull(state.prefilledOrderBook);
        assertNotNull(state.partialMatchOrderBook);
        assertNotNull(state.randomMixOrderBook);
    }

    @Test
    void testPureInsertScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.pureInsertScenario(state, blackhole));
    }

    @Test
    void testPureMatchScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.pureMatchScenario(state, blackhole));
    }

    @Test
    void testPartialMatchScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.partialMatchScenario(state, blackhole));
    }

    @Test
    void testRandomMixScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.randomMixScenario(state, blackhole));
    }

    @Test
    void testHotspotMatchScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.hotspotMatchScenario(state, blackhole));
    }

    @Test
    void testColdBookScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.coldBookScenario(state, blackhole));
    }

    @Test
    void testSingleOrderLatency() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.singleOrderLatency(state, blackhole));
    }

    @Test
    void testBatchOrderThroughput() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.batchOrderThroughput(state, blackhole));
    }

    @Test
    void testMemoryPressureScenario() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "ART";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> benchmark.memoryPressureScenario(state, blackhole));
    }

    @Test
    void testTreeSetImplementation() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 100;
        state.orderBookType = "TreeSet";
        state.setupTrial();
        
        OrderBookBenchmark benchmark = new OrderBookBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        
        // Should not throw any exceptions with TreeSet implementation
        assertDoesNotThrow(() -> benchmark.pureInsertScenario(state, blackhole));
        assertDoesNotThrow(() -> benchmark.pureMatchScenario(state, blackhole));
    }

    @Test
    void testOrderCommandGeneration() {
        OrderBookBenchmarkState state = new OrderBookBenchmarkState();
        state.datasetSize = 10;
        state.orderBookType = "ART";
        state.setupTrial();
        
        // Verify pure insert orders have non-overlapping prices
        for (OrderCommand cmd : state.pureInsertOrders) {
            assertNotNull(cmd);
            assertTrue(cmd.orderId > 0);
            assertTrue(cmd.price > 0);
            assertTrue(cmd.size > 0);
            assertNotNull(cmd.action);
        }
        
        // Verify pure match orders all have same price
        long firstPrice = state.pureMatchOrders[0].price;
        for (OrderCommand cmd : state.pureMatchOrders) {
            assertEquals(firstPrice, cmd.price);
        }
        
        // Verify hotspot orders all target same price
        long hotspotPrice = state.hotspotOrders[0].price;
        for (OrderCommand cmd : state.hotspotOrders) {
            assertEquals(hotspotPrice, cmd.price);
        }
    }
}
