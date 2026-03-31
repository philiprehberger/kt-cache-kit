# cache-kit

[![Tests](https://github.com/philiprehberger/kt-cache-kit/actions/workflows/publish.yml/badge.svg)](https://github.com/philiprehberger/kt-cache-kit/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.philiprehberger/cache-kit.svg)](https://central.sonatype.com/artifact/com.philiprehberger/cache-kit)
[![Last updated](https://img.shields.io/github/last-commit/philiprehberger/kt-cache-kit)](https://github.com/philiprehberger/kt-cache-kit/commits/main)

Lightweight coroutine-aware in-memory cache with TTL and LRU eviction.

## Installation

### Gradle (Kotlin DSL)

```kotlin
implementation("com.philiprehberger:cache-kit:0.1.5")
```

### Maven

```xml
<dependency>
    <groupId>com.philiprehberger</groupId>
    <artifactId>cache-kit</artifactId>
    <version>0.1.5</version>
</dependency>
```

## Usage

```kotlin
import com.philiprehberger.cachekit.*
import kotlin.time.Duration.Companion.minutes

val userCache = cache<String, User> {
    maxSize = 1000
    expireAfterWrite = 5.minutes
    onEvict = { key, _ -> println("Evicted user $key") }
}

// Basic get/put
userCache.put("user-1", loadUser("user-1"))
val user = userCache.get("user-1")
```

### Lazy Loading with Deduplication

```kotlin
// Concurrent calls for the same key only trigger one load
val user = userCache.getOrLoad("user-1") { key ->
    fetchUserFromDatabase(key)
}
```

### Access-Based Expiry

```kotlin
val sessionCache = cache<String, Session> {
    expireAfterAccess = 30.minutes  // Extends lifetime on each read
}
```

### Cache Statistics

```kotlin
val stats = userCache.stats()
println("Hit rate: ${stats.hitRate}")
println("Hits: ${stats.hits}, Misses: ${stats.misses}")
println("Evictions: ${stats.evictions}, Size: ${stats.size}")
```

## API

| Class / Function | Description |
|------------------|-------------|
| `cache { }` | DSL builder for creating cache instances |
| `Cache<K, V>` | Main cache class with get, put, getOrLoad, invalidate, clear, stats |
| `CacheConfig<K, V>` | Configuration: maxSize, expireAfterWrite, expireAfterAccess, onEvict |
| `CacheStats` | Data class with hits, misses, evictions, size, and hitRate |

## Development

```bash
./gradlew test       # Run tests
./gradlew check      # Run all checks
./gradlew build      # Build JAR
```

## Support

If you find this project useful:

⭐ [Star the repo](https://github.com/philiprehberger/kt-cache-kit)

🐛 [Report issues](https://github.com/philiprehberger/kt-cache-kit/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

💡 [Suggest features](https://github.com/philiprehberger/kt-cache-kit/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement)

❤️ [Sponsor development](https://github.com/sponsors/philiprehberger)

🌐 [All Open Source Projects](https://philiprehberger.com/open-source-packages)

💻 [GitHub Profile](https://github.com/philiprehberger)

🔗 [LinkedIn Profile](https://www.linkedin.com/in/philiprehberger)

## License

[MIT](LICENSE)
