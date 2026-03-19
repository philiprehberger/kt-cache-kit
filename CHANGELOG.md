# Changelog

All notable changes to this library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.2] - 2026-03-18

- Upgrade to Kotlin 2.0.21 and Gradle 8.12
- Enable explicitApi() for stricter public API surface
- Add issueManagement to POM metadata

## [Unreleased]

## [0.1.1] - 2026-03-18

- Fix CI badge and gradlew permissions

## [0.1.0] - 2026-03-17

### Added
- `Cache<K, V>` with get, put, getOrLoad, invalidate, and clear
- `cache { }` DSL for configuration
- LRU eviction when maxSize is exceeded
- TTL support via `expireAfterWrite` and `expireAfterAccess`
- `getOrLoad` with concurrent load deduplication using coroutine mutexes
- `CacheStats` with hits, misses, evictions, size, and computed hitRate
- `onEvict` callback for eviction notifications
