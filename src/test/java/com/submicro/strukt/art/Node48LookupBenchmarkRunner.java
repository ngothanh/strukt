package com.submicro.strukt.art;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Node48LookupBenchmarkRunner {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Node48LookupBenchmark.class.getSimpleName())
                .forks(0) // Disable forking to avoid classpath issues
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        new Runner(opt).run();
    }
}
