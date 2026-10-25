package com.marzec.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.collectState
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationCacheImpl
import com.marzec.navigation.NavigationHost
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStateCache
import com.marzec.navigation.navigationStore
import com.marzec.sample.navigation.NavigationExampleDestination
import com.marzec.sample.navigation.screens.a.AStore
import com.marzec.sample.navigation.screens.a.ScreenA
import com.marzec.sample.navigation.screens.b.BStore
import com.marzec.sample.navigation.screens.b.ScreenB
import com.marzec.sample.navigation.screens.home.HomeScreen
import com.marzec.sample.navigation.screens.home.HomeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

class MemoryNavigationStateCache : NavigationStateCache {
    private val map = HashMap<String, Any>()
    override fun <T> get(key: String): T? = map[key] as? T
    override fun set(key: String, value: Any) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

private fun NavigationState.flows(): Set<String> = backStack.fold(mutableSetOf()) { acc, entry ->
    entry.subFlow?.let {
        acc.add(it.id)
        acc.addAll(it.flows())
    }
    acc
}

@Composable
fun NavigationSample(onEmptyBackStack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val stateCache = remember { MemoryNavigationStateCache() }
    val cacheKeyProvider = remember { { Random.nextInt(Int.MAX_VALUE).toString() } }
    val navigationStoreCacheKey = remember { cacheKeyProvider.invoke() }
    val flowsScope = remember { mutableMapOf<String, MutableStateFlow<String>>() }
    val currentFlows = remember { mutableSetOf<String>() }

    val onNewStateCallback: (NavigationState) -> Unit = { state ->
        val newFlowsList = state.flows()
        val flowsScopesToRemove = currentFlows - newFlowsList
        val flowsScopeToCreate = newFlowsList - currentFlows

        currentFlows.clear()
        currentFlows.addAll(newFlowsList)

        flowsScopesToRemove.forEach { flowsScope.remove(it) }
        flowsScopeToCreate.forEach {
            flowsScope[it] = MutableStateFlow("default value")
        }
    }

    val navigationStore = remember {
        navigationStore(
            scope = scope,
            stateCache = stateCache,
            cacheKeyProvider = cacheKeyProvider,
            navigationStoreCacheKey = navigationStoreCacheKey,
            defaultDestination = NavigationExampleDestination.HomeScreen,
            resultCache = NavigationCacheImpl(),
            onNewStateCallback = onNewStateCallback,
            onAfterClosed = { onEmptyBackStack() }
        )
    }

    val router: (Destination, String) -> @Composable (destination: Destination, cacheKey: String) -> Unit = { destination, flowId ->
        when (destination as NavigationExampleDestination) {
            NavigationExampleDestination.HomeScreen -> @Composable { _, _ ->
                val store = HomeStore(scope, navigationStore)
                HomeScreen(store)
            }
            is NavigationExampleDestination.A -> @Composable { _, _ ->
                val store = AStore(navigationStore, flowsScope.getOrPut(flowId) { MutableStateFlow("default value") }, scope)
                ScreenA(store)
            }
            is NavigationExampleDestination.B -> @Composable { _, _ ->
                val store = BStore(navigationStore, flowsScope.getOrPut(flowId) { MutableStateFlow("default value") }, scope)
                ScreenB(store)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            modifier = Modifier.padding(16.dp),
            onClick = {
                navigationStore.goBack()
            }) {
            Text("Back")
        }

        NavigationHost(navigationStore, router)
    }
}