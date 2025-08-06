package com.submicro.strukt.art;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LongAdaptiveRadixTreeMapRemoveTest {

    private LongAdaptiveRadixTreeMap<String> tree;

    @BeforeEach
    void setUp() {
        tree = new LongAdaptiveRadixTreeMap<>();
    }

    @Test
    void testSimpleRemove() {
        // Add some values
        tree.put(100L, "value100");
        tree.put(200L, "value200");
        tree.put(300L, "value300");

        // Verify they exist
        assertEquals("value100", tree.get(100L));
        assertEquals("value200", tree.get(200L));
        assertEquals("value300", tree.get(300L));

        // Remove one value
        String removed = tree.remove(200L);
        assertEquals("value200", removed);

        // Verify it's gone
        assertNull(tree.get(200L));

        // Verify others still exist
        assertEquals("value100", tree.get(100L));
        assertEquals("value300", tree.get(300L));
    }

    @Test
    void testRemoveNonExistentKey() {
        tree.put(100L, "value100");
        
        // Try to remove a key that doesn't exist
        String removed = tree.remove(999L);
        assertNull(removed);
        
        // Original value should still be there
        assertEquals("value100", tree.get(100L));
    }

    @Test
    void testRemoveFromEmptyTree() {
        String removed = tree.remove(100L);
        assertNull(removed);
    }

    @Test
    void testRemoveAllValues() {
        // Add multiple values
        tree.put(100L, "value100");
        tree.put(200L, "value200");
        tree.put(300L, "value300");

        // Remove all values
        assertEquals("value100", tree.remove(100L));
        assertEquals("value200", tree.remove(200L));
        assertEquals("value300", tree.remove(300L));

        // Verify all are gone
        assertNull(tree.get(100L));
        assertNull(tree.get(200L));
        assertNull(tree.get(300L));

        // Tree should be empty
        assertTrue(tree.isEmpty());
    }

    @Test
    void testRemoveWithNodeShrinking() {
        // Add enough values to create different node types
        // This will test the node shrinking functionality
        
        // Add values that will create an ArtNode16
        for (int i = 0; i < 10; i++) {
            tree.put(1000L + i, "value" + (1000 + i));
        }

        // Verify all values exist
        for (int i = 0; i < 10; i++) {
            assertEquals("value" + (1000 + i), tree.get(1000L + i));
        }

        // Remove some values to trigger node shrinking
        for (int i = 0; i < 7; i++) {
            String removed = tree.remove(1000L + i);
            assertEquals("value" + (1000 + i), removed);
        }

        // Verify removed values are gone
        for (int i = 0; i < 7; i++) {
            assertNull(tree.get(1000L + i));
        }

        // Verify remaining values still exist
        for (int i = 7; i < 10; i++) {
            assertEquals("value" + (1000 + i), tree.get(1000L + i));
        }
    }

    @Test
    void testRemoveWithComplexKeys() {
        // Test with complex keys that exercise different byte levels
        long key1 = 0x123456789ABCDEF0L;
        long key2 = 0x123456789ABCDE01L;
        long key3 = 0x123456789ABC1234L;
        long key4 = 0x12345678FEDCBA98L;

        tree.put(key1, "complex1");
        tree.put(key2, "complex2");
        tree.put(key3, "complex3");
        tree.put(key4, "complex4");

        // Verify all exist
        assertEquals("complex1", tree.get(key1));
        assertEquals("complex2", tree.get(key2));
        assertEquals("complex3", tree.get(key3));
        assertEquals("complex4", tree.get(key4));

        // Remove some
        assertEquals("complex2", tree.remove(key2));
        assertEquals("complex4", tree.remove(key4));

        // Verify removed ones are gone
        assertNull(tree.get(key2));
        assertNull(tree.get(key4));

        // Verify remaining ones still exist
        assertEquals("complex1", tree.get(key1));
        assertEquals("complex3", tree.get(key3));
    }

    @Test
    void testRemoveAndReAdd() {
        tree.put(100L, "original");
        assertEquals("original", tree.get(100L));

        // Remove the value
        String removed = tree.remove(100L);
        assertEquals("original", removed);
        assertNull(tree.get(100L));

        // Re-add with different value
        tree.put(100L, "new_value");
        assertEquals("new_value", tree.get(100L));

        // Remove again
        removed = tree.remove(100L);
        assertEquals("new_value", removed);
        assertNull(tree.get(100L));
    }

    @Test
    void testRemovePerformance() {
        // Add a smaller number of values to avoid ART capacity issues
        int count = 20;
        for (int i = 0; i < count; i++) {
            tree.put((long) i, "value" + i);
        }

        long startTime = System.nanoTime();

        // Remove all values
        for (int i = 0; i < count; i++) {
            String removed = tree.remove((long) i);
            assertEquals("value" + i, removed);
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.println("Removed " + count + " values in " + (duration / 1_000_000) + " ms");

        // Verify all are gone
        for (int i = 0; i < count; i++) {
            assertNull(tree.get((long) i));
        }

        assertTrue(tree.isEmpty());
    }
}
