package com.philiprehberger.cachekit

/**
 * Internal wrapper for cached values, tracking creation and access times.
 *
 * @param V The type of the cached value.
 * @property value The cached value.
 * @property createdAt The epoch millisecond timestamp when this entry was created.
 * @property lastAccessedAt The epoch millisecond timestamp of the most recent access.
 */
internal data class CacheEntry<V>(
    val value: V,
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessedAt: Long = System.currentTimeMillis(),
)
