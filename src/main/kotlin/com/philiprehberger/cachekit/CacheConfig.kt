package com.philiprehberger.cachekit

import kotlin.time.Duration

/**
 * Configuration for a [Cache] instance, built via DSL.
 *
 * @param K The key type.
 * @param V The value type.
 */
class CacheConfig<K, V> {
    /** Maximum number of entries. When exceeded, the least recently used entry is evicted. */
    var maxSize: Int = Int.MAX_VALUE

    /** Duration after which an entry expires since it was written. Null means no write-based expiry. */
    var expireAfterWrite: Duration? = null

    /** Duration after which an entry expires since it was last accessed. Null means no access-based expiry. */
    var expireAfterAccess: Duration? = null

    /** Callback invoked when an entry is evicted. */
    var onEvict: ((K, V) -> Unit)? = null
}

/**
 * Creates a [Cache] using a configuration DSL.
 *
 * ```kotlin
 * val cache = cache<String, User> {
 *     maxSize = 1000
 *     expireAfterWrite = 5.minutes
 *     onEvict = { key, value -> println("Evicted $key") }
 * }
 * ```
 *
 * @param K The key type.
 * @param V The value type.
 * @param block Configuration block.
 * @return A configured [Cache] instance.
 */
fun <K, V> cache(block: CacheConfig<K, V>.() -> Unit): Cache<K, V> {
    val config = CacheConfig<K, V>().apply(block)
    return Cache(config)
}
