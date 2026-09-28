package com.marzec.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationCacheImpl : NavigationCache {

    private val store = mutableMapOf<String, MutableStateFlow<Any?>>()

    override suspend fun put(key: String, value: Any?) {
        updateFlow(key) { value }
    }

    override suspend fun <T> get(key: String): T? = (store[key]?.value as? T?)

    override suspend fun remove(key: String) {
        store.remove(key)
    }

    override suspend fun <T> observe(key: String): StateFlow<T?> {
        val flow = store.getOrPut(key) { MutableStateFlow(null) }
        return flow.asStateFlow() as StateFlow<T?>
    }

    override suspend fun toMap(): Map<String, Any?> = store.mapValues { it.value.value }

    private fun updateFlow(key: String, value: () -> Any?) {
        val flow = store.getOrPut(key) { MutableStateFlow(null) }
        flow.value = value()
    }
}
