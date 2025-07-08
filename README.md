# Strukt

A Java library for advanced data structures, including a comprehensive implementation of the Adaptive Radix Tree (ART).

## Features

- **Adaptive Radix Tree (ART)**: A space-efficient trie data structure that adapts its internal representation based on the number of children at each node.

## Adaptive Radix Tree (ART)

The ART implementation provides:

### Core Operations
- **O(k) complexity** for search, insert, and delete operations (where k is the key length)
- **Minimum/Maximum lookups** for finding the smallest/largest keys
- **Prefix-based iteration** for efficient range queries
- **Ordered iteration** over all key-value pairs

### Adaptive Node Types
- **ART4**: For nodes with 1-4 children (linear search)
- **ART16**: For nodes with 5-16 children (linear search with SIMD optimization)
- **ART48**: For nodes with 17-48 children (indexed array lookup)
- **ART256**: For nodes with 49-256 children (direct array access)
- **LEAF**: For nodes that store values

### Key Features
- Space-efficient storage with automatic node type adaptation
- Support for variable-length byte keys
- Path compression to reduce memory usage
- Comprehensive iteration and query capabilities

## Interface Design

The `IArtNode<V>` interface provides a complete API for ART operations:

```java
// Core search operations
Optional<V> search(byte[] key);
boolean contains(byte[] key);

// Modification operations
IArtNode<V> insert(byte[] key, V value);
IArtNode<V> delete(byte[] key);

// Min/max operations
Optional<ArtEntry<V>> minimum();
Optional<ArtEntry<V>> maximum();

// Iteration operations
Iterator<ArtEntry<V>> iterator();
Iterator<ArtEntry<V>> prefixIterator(byte[] prefix);

// Node information
ArtNodeType getNodeType();
int getChildCount();
boolean isLeaf();
long size();
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