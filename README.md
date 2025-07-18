# Strukt

A Java library for advanced data structures, including a comprehensive implementation of the Adaptive Radix Tree (ART).

## Features

- **Long-keyed Adaptive Radix Tree**: A space-efficient trie data structure optimized for 64-bit integer keys that adapts its internal representation based on the number of children at each node.

## Long Adaptive Radix Tree (LongAdaptiveRadixTreeMap)

The ART implementation provides a high-performance map interface for long keys with automatic memory management through object pooling.

### Core Operations
- **O(k) complexity** for search, insert operations (where k = 8 bytes for long keys)
- **Byte-level key processing** with automatic path compression
- **Object pooling** for memory efficiency and reduced GC pressure

### Current Implementation Status

#### Completed Features
- **LongAdaptiveRadixTreeMap<V>**: Main map interface for long keys
- **ArtNode4**: Handles nodes with 1-4 children using linear search with early termination optimization
- **Automatic node growth**: ArtNode4 automatically upgrades to ArtNode16 when capacity is exceeded
- **Path compression**: Reduces memory usage by compressing single-child paths
- **Early exit optimization**: Skips subtree traversal when keys differ at higher byte levels
- **Object pooling**: Efficient memory management with reusable node instances
- **Debug visualization**: Tree structure printing for development and debugging

#### Node Types
- **ArtNode4**: For nodes with 1-4 children (linear search with sorted keys)
- **ArtNode16**: For nodes with 5-16 children (placeholder - not yet implemented)
- **ArtNode48**: For nodes with 17-48 children (not yet implemented)
- **ArtNode256**: For nodes with 49-256 children (not yet implemented)

### Key Features
- **Byte-level processing**: Processes 64-bit keys byte-by-byte from most to least significant
- **Adaptive branching**: Creates branch nodes only when keys diverge at specific byte positions
- **Sorted key storage**: Maintains keys in sorted order for early search termination
- **Level-based traversal**: Uses bit-level operations for efficient byte extraction

## Interface Design

The `ArtNode<V>` interface provides the core operations:

```java
// Core operations
ArtNode<V> put(long key, int level, V value);
V get(long key, int level);

// Utility operations
ObjectsPool getObjectsPool();
String printDiagram(String prefix, int level);
```

The `LongAdaptiveRadixTreeMap<V>` provides the public API:

```java
// Map operations
V get(long key);
void put(long key, V value);

// Debug operations
String printDiagram();
```

## Usage Example

```java
// Create a map with default object pool
LongAdaptiveRadixTreeMap<String> map = new LongAdaptiveRadixTreeMap<>();

// Insert key-value pairs
map.put(0x123456789ABCDEF0L, "first_value");
map.put(0x123456789ABCDE01L, "second_value");
map.put(0x123456789ABC1234L, "third_value");

// Retrieve values
String value = map.get(0x123456789ABCDEF0L); // "first_value"

// Print tree structure for debugging
System.out.println(map.printDiagram());
```

## Building

This project uses Maven. To build:

```bash
mvn clean compile
```

To run tests:

```bash
mvn test
```

To create a JAR:

```bash
mvn package
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- Lombok (for code generation)

## Implementation Notes

### Performance Optimizations
- **Early exit optimization**: Detects key mismatches at higher byte levels to skip entire subtrees
- **Sorted key arrays**: Enables early termination in linear search when search key is smaller than current key
- **Object pooling**: Reduces garbage collection pressure through node reuse
- **Bit manipulation**: Uses efficient bitwise operations for byte extraction and level calculations

### Current Limitations
- Only ArtNode4 is fully implemented
- No delete operations yet
- No iteration support
- Limited to ArtNode4 capacity (will upgrade to ArtNode16 but ArtNode16 is not implemented)

### Future Enhancements
- Complete ArtNode16, ArtNode48, and ArtNode256 implementations
- Add delete operations with node shrinking
- Implement iteration and range query support
- Add minimum/maximum key operations
- Support for prefix-based queries
