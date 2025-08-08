#!/bin/bash

# OrderBook Benchmark Runner Script
# Usage: ./run-benchmarks.sh [scenario] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SCENARIO=".*"
DATASET_SIZE="10000"
ORDER_BOOK_TYPE="ART,TreeSet"
WARMUP_ITERATIONS=3
MEASUREMENT_ITERATIONS=3
FORKS=1
THREADS=1

# Function to print usage
usage() {
    echo -e "${BLUE}OrderBook Benchmark Runner${NC}"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -s, --scenario PATTERN       Benchmark scenario pattern (default: all)"
    echo "  -d, --dataset-size SIZE       Dataset size (default: 10000)"
    echo "  -t, --type TYPE               OrderBook type: ART,TreeSet (default: both)"
    echo "  -w, --warmup ITERATIONS       Warmup iterations (default: 3)"
    echo "  -m, --measurement ITERATIONS  Measurement iterations (default: 3)"
    echo "  -f, --forks FORKS             Number of forks (default: 1)"
    echo "  --threads THREADS             Number of threads (default: 1)"
    echo "  -q, --quick                   Quick test (1 warmup, 1 measurement)"
    echo "  -h, --help                    Show this help"
    echo ""
    echo "Scenarios:"
    echo "  pureInsert        - Pure insert scenario (no matches)"
    echo "  pureMatch         - Pure match scenario (100% matched)"
    echo "  partialMatch      - Partial match scenario (50% matched)"
    echo "  randomMix         - Random mix scenario (70% unmatched, 30% matched)"
    echo "  hotspotMatch      - Hotspot match scenario (same price level)"
    echo "  coldBook          - Cold book scenario (empty book)"
    echo "  singleOrder       - Single order latency"
    echo "  batchOrder        - Batch order throughput"
    echo "  memoryPressure    - Memory pressure scenario"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all scenarios"
    echo "  $0 -s pureInsert                     # Run only pure insert scenario"
    echo "  $0 -s pureInsert -d 100000           # Pure insert with 100K orders"
    echo "  $0 -t ART -q                         # Quick test with ART only"
    echo "  $0 -s \"pureInsert|pureMatch\" -t TreeSet  # Multiple scenarios with TreeSet"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--scenario)
            SCENARIO="$2"
            shift 2
            ;;
        -d|--dataset-size)
            DATASET_SIZE="$2"
            shift 2
            ;;
        -t|--type)
            ORDER_BOOK_TYPE="$2"
            shift 2
            ;;
        -w|--warmup)
            WARMUP_ITERATIONS="$2"
            shift 2
            ;;
        -m|--measurement)
            MEASUREMENT_ITERATIONS="$2"
            shift 2
            ;;
        -f|--forks)
            FORKS="$2"
            shift 2
            ;;
        --threads)
            THREADS="$2"
            shift 2
            ;;
        -q|--quick)
            WARMUP_ITERATIONS=1
            MEASUREMENT_ITERATIONS=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Ensure target directory exists
mkdir -p target/benchmark-results

# Compile if needed
echo -e "${YELLOW}Compiling project...${NC}"
mvn clean compile test-compile -q

# Build classpath
echo -e "${YELLOW}Building classpath...${NC}"
CLASSPATH="target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)"

# Prepare JMH command
JMH_CMD="java -cp \"$CLASSPATH\" org.openjdk.jmh.Main"
JMH_CMD="$JMH_CMD \"OrderBookBenchmark.$SCENARIO\""
JMH_CMD="$JMH_CMD -p datasetSize=$DATASET_SIZE"
JMH_CMD="$JMH_CMD -p orderBookType=$ORDER_BOOK_TYPE"
JMH_CMD="$JMH_CMD -wi $WARMUP_ITERATIONS"
JMH_CMD="$JMH_CMD -i $MEASUREMENT_ITERATIONS"
JMH_CMD="$JMH_CMD -f $FORKS"
JMH_CMD="$JMH_CMD -t $THREADS"

# Add result file with timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="target/benchmark-results/orderbook-benchmark-$TIMESTAMP.json"
JMH_CMD="$JMH_CMD -rf json -rff $RESULT_FILE"

# Add GC logging
GC_LOG="target/benchmark-results/gc-$TIMESTAMP.log"
JMH_CMD="$JMH_CMD -jvmArgs \"-Xmx4G -Xms4G -XX:+UseG1GC -Xlog:gc*:$GC_LOG\""

echo -e "${GREEN}Running OrderBook benchmarks...${NC}"
echo -e "${BLUE}Scenario:${NC} $SCENARIO"
echo -e "${BLUE}Dataset Size:${NC} $DATASET_SIZE"
echo -e "${BLUE}OrderBook Type:${NC} $ORDER_BOOK_TYPE"
echo -e "${BLUE}Warmup Iterations:${NC} $WARMUP_ITERATIONS"
echo -e "${BLUE}Measurement Iterations:${NC} $MEASUREMENT_ITERATIONS"
echo -e "${BLUE}Forks:${NC} $FORKS"
echo -e "${BLUE}Threads:${NC} $THREADS"
echo -e "${BLUE}Results will be saved to:${NC} $RESULT_FILE"
echo ""

# Execute the benchmark
eval $JMH_CMD

echo ""
echo -e "${GREEN}Benchmark completed!${NC}"
echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
echo -e "${BLUE}GC log saved to:${NC} $GC_LOG"

# Show quick summary if results file exists
if [ -f "$RESULT_FILE" ]; then
    echo ""
    echo -e "${YELLOW}Quick Summary:${NC}"
    echo "Check the JSON file for detailed results and percentiles."
fi
