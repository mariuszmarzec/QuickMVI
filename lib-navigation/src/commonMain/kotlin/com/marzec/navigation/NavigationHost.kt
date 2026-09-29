package com.marzec.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.marzec.mvi.collectState
import com.marzec.navigation.NavigationStateCache
import kotlinx.coroutines.CoroutineScope

@Composable
fun NavigationHost(
    navigationStore: NavigationStore,
    router: (Destination) -> @Composable (destination: Destination, cacheKey: String) -> Unit
) {
    val state: NavigationState by navigationStore.collectState()

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        state.backStack.currentScreen()?.apply {
            val screenProvider = router(destination)
            screenProvider(destination, cacheKey)
        }
    }
}

@Composable
fun NavigationHost(
    navigationStore: NavigationStore,
    router: (Destination, String) -> @Composable (destination: Destination, cacheKey: String) -> Unit
) {
    val state: NavigationState by navigationStore.collectState()

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        val currentFlow = state.currentFlow()
        currentFlow.currentScreen()?.apply {
            val screenProvider = router(destination, currentFlow.id)
            screenProvider(destination, cacheKey)
        }
    }
}

fun navigationStore(
    scope: CoroutineScope,
    stateCache: NavigationStateCache,
    cacheKeyProvider: () -> String,
    navigationStoreCacheKey: String,
    defaultDestination: Destination,
    resultCache: NavigationCache,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
): NavigationStore = navigationStore(
    scope = scope,
    stateCache = stateCache,
    navigationStoreCacheKey = navigationStoreCacheKey,
    cacheKeyProvider = cacheKeyProvider,
    initialState = initialState(defaultDestination, cacheKeyProvider),
    resultCache = resultCache,
    overrideLastClose = overrideLastClose,
    onNewStateCallback = onNewStateCallback,
    onAfterClosed = onAfterClosed
)

fun navigationStore(
    scope: CoroutineScope,
    stateCache: NavigationStateCache,
    navigationStoreCacheKey: String,
    cacheKeyProvider: () -> String,
    initialState: NavigationFlow,
    resultCache: NavigationCache,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
) = NavigationStore(
    scope = scope,
    stateCache = stateCache,
    resultCache = ResultCache(resultCache),
    cacheKey = navigationStoreCacheKey,
    cacheKeyProvider = cacheKeyProvider,
    initialState = initialState,
    overrideLastClose = overrideLastClose,
    onAfterClosed = onAfterClosed
).apply {
    if (onNewStateCallback != null) {
        this.onNewStateCallback = onNewStateCallback
    }
}

fun initialState(
    defaultDestination: Destination,
    cacheKeyProvider: () -> String
) = navigationState(
    backStack = listOf(
        NavigationEntry(
            destination = defaultDestination,
            cacheKey = cacheKeyProvider()
        )
    )
)
