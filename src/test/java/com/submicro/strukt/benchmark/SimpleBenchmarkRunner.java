package com.submicro.strukt.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Simple benchmark runner that uses in-process execution to avoid classpath issues.
 * This is a fallback when forked execution fails.
 */
public class SimpleBenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println("=".repeat(100));
        System.out.println("SIMPLE ORDERBOOK BENCHMARK RUNNER - IN-PROCESS EXECUTION");
        System.out.println("=".repeat(100));
        
        // Ensure results directory exists
        Files.createDirectories(Paths.get("target/benchmark-results"));
        
        // Run a quick benchmark suite with in-process execution
        runQuickBenchmarks();
        
        // Generate analysis from any existing results
        BenchmarkAnalyzer.generateComprehensiveReport();
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("SIMPLE BENCHMARK COMPLETED!");
        System.out.println("Check target/benchmark-results/ for reports");
        System.out.println("=".repeat(100));
    }
    
    private static void runQuickBenchmarks() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Running Quick Performance Tests (In-Process)");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "10000")
                .param("implementation", "ART", "TreeSet")
                .include("scenario1_scaleThreshold_sequentialInsert")
                .include("scenario2_keyDistribution_sequential")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(0) // In-process execution
                .warmupIterations(1)
                .measurementIterations(2)
                .result("target/benchmark-results/simple-benchmark.json")
                .build();

        System.out.println("Running simple benchmarks...");
        new Runner(opt).run();
    }
    
    /**
     * Run a focused test on specific scenarios
     */
    public static void runFocusedTest() throws RunnerException {
        System.out.println("Running focused performance test...");
        
        Options opt = new OptionsBuilder()
                .include("scenario1_scaleThreshold_sequentialInsert")
                .param("datasetSize", "1000", "5000")
                .param("implementation", "ART", "TreeSet")
                .forks(0) // In-process execution
                .warmupIterations(1)
                .measurementIterations(2)
                .result("target/benchmark-results/focused-test.json")
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run memory efficiency comparison
     */
    public static void runMemoryTest() throws RunnerException {
        System.out.println("Running memory efficiency test...");
        
        Options opt = new OptionsBuilder()
                .include(MemoryPressureBenchmark.class.getSimpleName())
                .param("datasetSize", "10000")
                .param("implementation", "ART", "TreeSet")
                .forks(0) // In-process execution
                .warmupIterations(1)
                .measurementIterations(2)
                .result("target/benchmark-results/memory-test.json")
                .build();

        new Runner(opt).run();
    }
}
