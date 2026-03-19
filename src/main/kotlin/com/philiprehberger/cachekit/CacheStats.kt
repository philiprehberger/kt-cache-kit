package com.philiprehberger.cachekit

/**
 * Statistics for a [Cache] instance.
 *
 * @property hits The number of cache hits.
 * @property misses The number of cache misses.
 * @property evictions The number of entries evicted (due to size limits or expiry).
 * @property size The current number of entries in the cache.
 */
public data class CacheStats(
    public val hits: Long,
    public val misses: Long,
    public val evictions: Long,
    public val size: Int,
) {
    /**
     * The cache hit rate as a value between 0.0 and 1.0.
     *
     * Returns 0.0 if there have been no requests.
     */
    public val hitRate: Double
        get() {
            val total = hits + misses
            return if (total == 0L) 0.0 else hits.toDouble() / total
        }
}
