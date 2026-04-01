package com.philiprehberger.cachekit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * A lightweight, coroutine-aware in-memory cache with TTL and LRU eviction.
 *
 * Supports:
 * - Maximum size with LRU eviction
 * - Time-to-live based on write time or last access time
 * - Concurrent load deduplication via [getOrLoad]
 * - Hit/miss/eviction statistics
 *
 * @param K The key type.
 * @param V The value type.
 * @param config The cache configuration.
 */
public class Cache<K, V> internal constructor(
    private val config: CacheConfig<K, V>,
) {
    private val entries = LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true)
    private val lock = Any()
    private val loadMutexes = ConcurrentHashMap<K, Mutex>()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)

    /**
     * Returns the cached value for [key], or `null` if not present or expired.
     *
     * @param key The cache key.
     * @return The cached value, or `null`.
     */
    public fun get(key: K): V? {
        synchronized(lock) {
            val entry = entries[key]
            if (entry == null) {
                missCount.incrementAndGet()
                return null
            }
            if (isExpired(entry)) {
                evict(key, entry)
                missCount.incrementAndGet()
                return null
            }
            entry.lastAccessedAt = System.currentTimeMillis()
            hitCount.incrementAndGet()
            return entry.value
        }
    }

    /**
     * Stores a value in the cache.
     *
     * If the cache is at maximum capacity, the least recently used entry is evicted.
     *
     * @param key The cache key.
     * @param value The value to cache.
     */
    public fun put(key: K, value: V) {
        synchronized(lock) {
            val existing = entries.remove(key)
            if (existing != null) {
                config.onEvict?.invoke(key, existing.value)
            }
            entries[key] = CacheEntry(value)
            evictIfOverSize()
        }
    }

    /**
     * Returns the cached value for [key], loading it via [loader] if absent or expired.
     *
     * Concurrent calls for the same key are deduplicated: only one [loader] invocation
     * occurs, and all waiters receive the same result.
     *
     * @param key The cache key.
     * @param loader Suspend function that loads the value for the given key.
     * @return The cached or freshly loaded value.
     */
    public suspend fun getOrLoad(key: K, loader: suspend (K) -> V): V {
        // Fast path: check if value exists
        get(key)?.let { return it }

        // Deduplicate concurrent loads for the same key
        val mutex = loadMutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            // Double-check after acquiring lock
            get(key)?.let { return@withLock it }

            val value = loader(key)
            put(key, value)
            loadMutexes.remove(key)
            value
        }
    }

    /**
     * Removes a specific entry from the cache.
     *
     * @param key The key to invalidate.
     */
    public fun invalidate(key: K) {
        synchronized(lock) {
            val entry = entries.remove(key)
            if (entry != null) {
                config.onEvict?.invoke(key, entry.value)
            }
        }
    }

    /**
     * Removes all entries from the cache.
     */
    public fun clear() {
        synchronized(lock) {
            entries.clear()
        }
    }

    /**
     * Returns a snapshot of the current cache statistics.
     *
     * @return A [CacheStats] instance.
     */
    public fun stats(): CacheStats {
        synchronized(lock) {
            return CacheStats(
                hits = hitCount.get(),
                misses = missCount.get(),
                evictions = evictionCount.get(),
                size = entries.size,
            )
        }
    }

    /**
     * Stores a value only if the key is not already present (or is expired).
     *
     * @param key The cache key.
     * @param value The value to cache.
     * @return The existing value if the key was already present, or null if the value was stored.
     */
    public fun putIfAbsent(key: K, value: V): V? {
        synchronized(lock) {
            val existing = entries[key]
            if (existing != null && !isExpired(existing)) {
                existing.lastAccessedAt = System.currentTimeMillis()
                hitCount.incrementAndGet()
                return existing.value
            }
            if (existing != null) {
                evict(key, existing)
            }
            entries[key] = CacheEntry(value)
            evictIfOverSize()
            return null
        }
    }

    /**
     * Returns cached values for all given [keys] that are present and not expired.
     *
     * @param keys The keys to look up.
     * @return A map of found entries.
     */
    public fun getAll(keys: Collection<K>): Map<K, V> {
        synchronized(lock) {
            val result = mutableMapOf<K, V>()
            for (key in keys) {
                val entry = entries[key]
                if (entry != null && !isExpired(entry)) {
                    entry.lastAccessedAt = System.currentTimeMillis()
                    hitCount.incrementAndGet()
                    result[key] = entry.value
                } else {
                    if (entry != null) evict(key, entry)
                    missCount.incrementAndGet()
                }
            }
            return result
        }
    }

    /**
     * Removes all entries matching the given [predicate].
     *
     * @param predicate A function that receives the key and value; returns true to invalidate.
     * @return The number of entries removed.
     */
    public fun invalidateIf(predicate: (K, V) -> Boolean): Int {
        synchronized(lock) {
            val toRemove = entries.entries
                .filter { (key, entry) -> predicate(key, entry.value) }
                .map { it.key }
            for (key in toRemove) {
                val entry = entries.remove(key)
                if (entry != null) {
                    evictionCount.incrementAndGet()
                    config.onEvict?.invoke(key, entry.value)
                }
            }
            return toRemove.size
        }
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return The cache size.
     */
    public fun size(): Int {
        synchronized(lock) {
            return entries.size
        }
    }

    private fun isExpired(entry: CacheEntry<V>): Boolean {
        val now = System.currentTimeMillis()
        config.expireAfterWrite?.let { ttl ->
            if (now - entry.createdAt >= ttl.inWholeMilliseconds) return true
        }
        config.expireAfterAccess?.let { ttl ->
            if (now - entry.lastAccessedAt >= ttl.inWholeMilliseconds) return true
        }
        return false
    }

    private fun evict(key: K, entry: CacheEntry<V>) {
        entries.remove(key)
        evictionCount.incrementAndGet()
        config.onEvict?.invoke(key, entry.value)
    }

    private fun evictIfOverSize() {
        while (entries.size > config.maxSize) {
            val eldest = entries.entries.firstOrNull() ?: break
            entries.remove(eldest.key)
            evictionCount.incrementAndGet()
            config.onEvict?.invoke(eldest.key, eldest.value.value)
        }
    }
}
