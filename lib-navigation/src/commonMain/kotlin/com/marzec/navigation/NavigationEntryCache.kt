package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

class NavigationEntryCache(
    private val navigationStore: NavigationStore,
    private val cache: NavigationCache
) : NavigationCache {

    override suspend fun put(key: String, value: Any?) {
        if (navigationStore.state.value.contains(key)) {
            cache.put(key, value)
        }
    }

    override suspend fun <T> get(key: String): T? = cache.get(key)

    override suspend fun remove(key: String) = cache.remove(key)

    override suspend fun <T> observe(key: String): Flow<T?> = cache.observe(key)

    override suspend fun toMap(): Map<String, Any?> = cache.toMap()

    private fun NavigationState.contains(cacheKey: String) =
        backStack.any { it.contains(cacheKey) }

    private fun NavigationEntry.contains(cacheKey: String): Boolean =
        cacheKey == this.cacheKey || subFlow?.contains(cacheKey) == true
}
