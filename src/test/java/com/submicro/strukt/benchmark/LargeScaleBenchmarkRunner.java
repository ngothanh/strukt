package com.submicro.strukt.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Specialized benchmark runner for large-scale performance testing (1M+ keys).
 * Designed to test ART's potential advantages in memory-intensive scenarios.
 */
public class LargeScaleBenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println("=".repeat(100));
        System.out.println("LARGE-SCALE ORDERBOOK BENCHMARK RUNNER (1M+ KEYS)");
        System.out.println("Testing ART's potential advantages in memory-intensive scenarios");
        System.out.println("=".repeat(100));
        
        // Ensure results directory exists
        Files.createDirectories(Paths.get("target/benchmark-results"));
        
        // Run progressive scale tests
        runProgressiveScaleTests();
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("LARGE-SCALE BENCHMARK COMPLETED!");
        System.out.println("Check target/benchmark-results/ for detailed reports");
        System.out.println("=".repeat(100));
    }
    
    private static void runProgressiveScaleTests() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Running Progressive Scale Tests (1M, 2M, 5M keys)");
        System.out.println("=".repeat(80));

        // Test with 1M keys first
        runScaleTest("1000000", "1M");
        
        // Test with 2M keys
        runScaleTest("2000000", "2M");
        
        // Test with 5M keys (if system can handle it)
        try {
            runScaleTest("5000000", "5M");
        } catch (Exception e) {
            System.err.println("5M key test failed (likely memory constraints): " + e.getMessage());
        }
    }
    
    private static void runScaleTest(String datasetSize, String label) throws RunnerException {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Testing with " + label + " keys (" + datasetSize + ")");
        System.out.println("-".repeat(60));

        Options opt = new OptionsBuilder()
                .include(LargeScaleBenchmark.class.getSimpleName())
                .param("datasetSize", datasetSize)
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(0) // In-process to avoid memory issues
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx8g", "-XX:+UseG1GC", "-XX:+UnlockExperimentalVMOptions", "-XX:G1NewSizePercent=20")
                .result("target/benchmark-results/large-scale-" + label + ".json")
                .build();

        System.out.println("Running " + label + " benchmark...");
        new Runner(opt).run();
    }
    
    /**
     * Run memory-focused benchmark to test ART's memory efficiency
     */
    public static void runMemoryFocusedTest() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Running Memory-Focused Test");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(LargeScaleBenchmark.class.getSimpleName())
                .include("scenario4_memoryIntensiveOperations")
                .param("datasetSize", "1000000", "2000000")
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(0) // In-process execution
                .warmupIterations(1)
                .measurementIterations(3)
                .jvmArgs("-Xmx4g", "-XX:+UseG1GC", "-XX:+PrintGC", "-XX:+PrintGCDetails")
                .result("target/benchmark-results/memory-focused.json")
                .build();

        System.out.println("Running memory-focused benchmarks...");
        new Runner(opt).run();
    }
    
    /**
     * Run prefix-focused benchmark to test ART's prefix compression advantages
     */
    public static void runPrefixFocusedTest() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Running Prefix-Focused Test");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(LargeScaleBenchmark.class.getSimpleName())
                .include("scenario2_prefixHeavyOperations")
                .param("datasetSize", "1000000", "2000000")
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(0) // In-process execution
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx6g", "-XX:+UseG1GC")
                .result("target/benchmark-results/prefix-focused.json")
                .build();

        System.out.println("Running prefix-focused benchmarks...");
        new Runner(opt).run();
    }
    
    /**
     * Quick test to verify large-scale setup works
     */
    public static void runQuickLargeScaleTest() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Running Quick Large-Scale Test (1M keys)");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(LargeScaleBenchmark.class.getSimpleName())
                .include("scenario1_massiveRandomLookup")
                .param("datasetSize", "1000000")
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(0) // In-process execution
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx4g", "-XX:+UseG1GC")
                .result("target/benchmark-results/quick-large-scale.json")
                .build();

        System.out.println("Running quick large-scale test...");
        new Runner(opt).run();
    }
}
