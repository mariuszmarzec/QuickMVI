package com.marzec.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResultCache(private val memoryCache: NavigationCache) {

    suspend fun observe(
        requesterKey: String,
        requestId: Int
    ): Flow<ResultCacheValue?> {
        val stringKey = createKey(requesterKey, requestId)
        return memoryCache.observe(stringKey)
    }

    suspend fun save(requestKey: RequestKey, value: Any?) {
        memoryCache.put(
            key = createKey(requestKey),
            value = ResultCacheValue(
                requestKey = requestKey,
                data = value
            )
        )
    }

    suspend fun remove(requesterKey: String) {
        allKeys().filter { it.requesterKey == requesterKey }.forEach {
            val stringKey = createKey(it)
            memoryCache.remove(stringKey)
        }
    }

    private suspend fun allKeys() =
        memoryCache.toMap().keys.map { Json.decodeFromString<ResultKey>(it) }

    private fun createKey(requestKey: RequestKey): String =
        createKey(ResultKey(requestKey.requesterKey, requestKey.requestId))

    private fun createKey(resultKey: ResultKey): String =
        Json.encodeToString(resultKey)

    private fun createKey(requesterKey: String, requestId: Int): String =
        Json.encodeToString(ResultKey(requesterKey, requestId))
}

data class ResultCacheValue(
    val requestKey: RequestKey,
    val data: Any?
)