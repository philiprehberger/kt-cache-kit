package com.philiprehberger.cachekit

/**
 * Statistics for a [Cache] instance.
 *
 * @property hits The number of cache hits.
 * @property misses The number of cache misses.
 * @property evictions The number of entries evicted (due to size limits or expiry).
 * @property size The current number of entries in the cache.
 */
data class CacheStats(
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val size: Int,
) {
    /**
     * The cache hit rate as a value between 0.0 and 1.0.
     *
     * Returns 0.0 if there have been no requests.
     */
    val hitRate: Double
        get() {
            val total = hits + misses
            return if (total == 0L) 0.0 else hits.toDouble() / total
        }
}
