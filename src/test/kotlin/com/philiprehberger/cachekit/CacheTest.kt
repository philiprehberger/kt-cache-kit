package com.philiprehberger.cachekit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CacheTest {

    @Test
    fun `get and put basic operations`() {
        val c = cache<String, Int> {}
        assertNull(c.get("a"))
        c.put("a", 1)
        assertEquals(1, c.get("a"))
    }

    @Test
    fun `TTL expiry after write`() {
        val c = cache<String, String> {
            expireAfterWrite = 50.milliseconds
        }
        c.put("key", "value")
        assertEquals("value", c.get("key"))

        Thread.sleep(60)
        assertNull(c.get("key"), "Entry should be expired")
    }

    @Test
    fun `LRU eviction when maxSize exceeded`() {
        val evicted = mutableListOf<String>()
        val c = cache<String, Int> {
            maxSize = 3
            onEvict = { key, _ -> evicted.add(key) }
        }
        c.put("a", 1)
        c.put("b", 2)
        c.put("c", 3)
        assertEquals(3, c.stats().size)

        // Adding a 4th should evict the LRU ("a")
        c.put("d", 4)
        assertEquals(3, c.stats().size)
        assertNull(c.get("a"), "Oldest entry 'a' should be evicted")
        assertEquals(4, c.get("d"))
        assertEquals("a", evicted.first())
    }

    @Test
    fun `LRU eviction respects access order`() {
        val c = cache<String, Int> {
            maxSize = 3
        }
        c.put("a", 1)
        c.put("b", 2)
        c.put("c", 3)

        // Access "a" to make it recently used
        c.get("a")

        // Adding "d" should evict "b" (least recently used)
        c.put("d", 4)
        assertEquals(1, c.get("a"), "'a' should still be present")
        assertNull(c.get("b"), "'b' should be evicted")
    }

    @Test
    fun `getOrLoad loads on miss`() = runTest {
        val c = cache<String, Int> {}
        val result = c.getOrLoad("key") { 42 }
        assertEquals(42, result)
        assertEquals(42, c.get("key"))
    }

    @Test
    fun `getOrLoad deduplicates concurrent loads`() = runTest {
        val loadCount = AtomicInteger(0)
        val c = cache<String, String> {}

        val results = (1..10).map { _ ->
            async {
                c.getOrLoad("shared") { key ->
                    loadCount.incrementAndGet()
                    delay(50)
                    "loaded-$key"
                }
            }
        }

        val values = results.map { it.await() }
        values.forEach { assertEquals("loaded-shared", it) }
        assertEquals(1, loadCount.get(), "Loader should have been called exactly once")
    }

    @Test
    fun `stats tracking`() {
        val c = cache<String, Int> {}
        c.put("a", 1)
        c.get("a")  // hit
        c.get("a")  // hit
        c.get("b")  // miss

        val stats = c.stats()
        assertEquals(2, stats.hits)
        assertEquals(1, stats.misses)
        assertEquals(1, stats.size)
        assertEquals(2.0 / 3.0, stats.hitRate, 0.001)
    }

    @Test
    fun `invalidate removes entry`() {
        val c = cache<String, Int> {}
        c.put("a", 1)
        assertEquals(1, c.get("a"))
        c.invalidate("a")
        assertNull(c.get("a"))
    }

    @Test
    fun `clear removes all entries`() {
        val c = cache<String, Int> {}
        c.put("a", 1)
        c.put("b", 2)
        c.put("c", 3)
        assertEquals(3, c.stats().size)
        c.clear()
        assertEquals(0, c.stats().size)
        assertNull(c.get("a"))
    }

    @Test
    fun `expireAfterAccess extends lifetime on read`() {
        val c = cache<String, String> {
            expireAfterAccess = 80.milliseconds
        }
        c.put("key", "value")

        // Access at 40ms to extend lifetime
        Thread.sleep(40)
        assertEquals("value", c.get("key"))

        // At 90ms from start (50ms from last access), should still be alive
        Thread.sleep(50)
        assertEquals("value", c.get("key"))

        // At 170ms from start (80ms+ from last access), should be expired
        Thread.sleep(90)
        assertNull(c.get("key"))
    }

    @Test
    fun `stats hitRate is zero when no requests`() {
        val c = cache<String, Int> {}
        assertEquals(0.0, c.stats().hitRate)
    }
}
