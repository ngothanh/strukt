package com.submicro.strukt.benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)

@State(Scope.Thread)
public class Node48LookupBenchmark {

    byte[] searchKeys;
    byte[] keyArray;
    byte[] sortedKeys;
    byte[] indexArray;
    int[] values;

    @Setup(Level.Iteration)
    public void setup() {
        searchKeys = new byte[1_000_000];
        Random r = new Random(42);
        for (int i = 0; i < searchKeys.length; i++) {
            searchKeys[i] = (byte) r.nextInt(256);
        }

        keyArray = new byte[48];
        sortedKeys = new byte[48];
        values = new int[48];
        indexArray = new byte[256];

        Set<Byte> used = new HashSet<>();
        int i = 0;
        while (used.size() < 48) {
            byte k = (byte) r.nextInt(256);
            if (used.add(k)) {
                keyArray[i] = k;
                values[i] = r.nextInt(1000);
                indexArray[k & 0xFF] = (byte) (i + 1);
                i++;
            }
        }

        System.arraycopy(keyArray, 0, sortedKeys, 0, 48);
        Arrays.sort(sortedKeys);
    }

    @Benchmark
    public int linearScan() {
        int result = 0;
        for (byte k : searchKeys) {
            for (int i = 0; i < 48; i++) {
                if (keyArray[i] == k) {
                    result += values[i];
                    break;
                }
            }
        }
        return result;
    }

    @Benchmark
    public int binarySearch() {
        int result = 0;
        for (byte k : searchKeys) {
            int lo = 0, hi = 47;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                byte mk = sortedKeys[mid];
                if (mk == k) {
                    result += values[mid];
                    break;
                } else if ((mk & 0xFF) < (k & 0xFF)) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
        }
        return result;
    }

    @Benchmark
    public int indexedArray() {
        int result = 0;
        for (byte k : searchKeys) {
            int idx = indexArray[k & 0xFF];
            if (idx != 0) {
                result += values[idx - 1];
            }
        }
        return result;
    }
}
