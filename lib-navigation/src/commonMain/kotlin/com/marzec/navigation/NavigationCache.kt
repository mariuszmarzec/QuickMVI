package com.marzec.navigation


import kotlinx.coroutines.flow.Flow

interface NavigationCache {

    suspend fun put(key: String, value: Any?)

    suspend fun <T> get(key: String): T?

    suspend fun remove(key: String)

    suspend fun <T> observe(key: String): Flow<T?>

    suspend fun toMap(): Map<String, Any?>
}

