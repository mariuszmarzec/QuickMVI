package com.marzec.navigation

/**
 * Navigation-owned interface for state persistence.
 * Combines reading and writing capabilities needed by NavigationStore.
 * Clients implement this interface to provide their own state storage.
 */
interface NavigationStateCache {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}