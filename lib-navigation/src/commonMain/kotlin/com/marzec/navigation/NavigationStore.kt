package com.marzec.navigation

import com.marzec.mvi.IntentContext
import com.marzec.mvi.Store4Impl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NavigationStore(
    scope: CoroutineScope,
    private val stateCache: NavigationStateCache,
    private val resultCache: ResultCache,
    private val cacheKey: String,
    private val cacheKeyProvider: () -> String,
    initialState: NavigationState,
    private val overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    private val onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
) : Store4Impl<NavigationState>(scope, stateCache.get(cacheKey) ?: initialState) {

    fun next(
        action: NavigationAction,
        requestId: Int? = null,
        secondaryId: Int? = null,
        id: String = ""
    ) =
        navigate(
            action = action,
            id = id,
            requestId = requestId,
            options = secondaryId?.let {
                mapOf<String, Any>(SECONDARY_ID to it)
            } ?: emptyMap()
        )

    fun nextWithOptionRequest(
        action: NavigationAction,
        requestId: Int? = null,
        options: Map<String, Any>? = null,
        id: String = ""
    ) = navigate(action, id, requestId, options)

    private fun navigate(
        action: NavigationAction,
        id: String,
        requestId: Int?,
        options: Map<String, Any>?
    ) = navigate {
        state.cleanResultCacheForCurrentScreen()

        val requestKey = requestKey(requestId, options)

        val poppedScreens = mutableListOf<NavigationEntry>()
        val newState = state.popScreens(action, poppedScreens)
            .addNextScreen(action, id, requestKey)

        NavigationUpdate(newState, poppedScreens)
    }

    private fun IntentContext<NavigationState, NavigationUpdate>.requestKey(
        requestId: Int?,
        options: Map<String, Any>?
    ): RequestKey? {
        val requesterKey = state.currentScreen()?.cacheKey
        return if (requestId != null && requesterKey != null) {
            RequestKey(
                requesterKey = requesterKey,
                requestId = requestId,
                options = options.orEmpty()
            )
        } else {
            null
        }
    }

    fun goBack(result: Any? = null) = navigate {
        state.backStack.currentScreen()?.requestKey?.let { requestKey ->
            resultCache.save(requestKey, result)
        }
        if (state.screenCount == 1 && overrideLastClose != null) {
            overrideLastClose.invoke(state)
        } else {
            val poppedScreens = mutableListOf<NavigationEntry>()
            val newState = state.popScreens(PopEntryTarget.ScreenCount(count = 1), poppedScreens)
            NavigationUpdate(newState, poppedScreens)
        }
    }

    fun closeFlow(result: Any? = null) = navigate {
        val currentFlow = state.currentFlow()
        currentFlow.backStack.firstOrNull()?.requestKey?.let { requestKey ->
            resultCache.save(requestKey, result)
        }
        if (currentFlow.isRootFlow() && overrideLastClose != null) {
            overrideLastClose.invoke(state)
        } else {
            val poppedScreens = mutableListOf<NavigationEntry>()
            val newState = state.popScreens(PopEntryTarget.ToFlow(currentFlow.id, popToInclusive = true), poppedScreens)
            NavigationUpdate(newState, poppedScreens)
        }
    }

    private fun navigate(stateTransform: suspend IntentContext<NavigationState, NavigationUpdate>.() -> NavigationUpdate) {
        intent<NavigationUpdate> {
            onTrigger {
                flowOf(stateTransform())
            }

            reducer { resultNonNull().newState }

            sideEffect {
                onAfterClosed?.let {
                    val update = resultNonNull()
                    update.closedEntries.forEach {
                        onAfterClosed.invoke(it)
                    }
                }
            }
        }
    }

    private fun NavigationState.addNextScreen(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): NavigationFlow = copy(
        backStack = backStack.addNextScreen(action, id, requestKey)
    )

    private fun List<NavigationEntry>.addNextScreen(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): List<NavigationEntry> = toMutableList().apply {
        val last = lastOrNull()
        if (last?.subFlow != null) {
            val newLast = last.copy(
                subFlow = last.subFlow.addNextScreen(action, id, requestKey)
            )
            remove(last)
            add(newLast)
        } else {
            add(createNavigationEntry(action, id, requestKey))
        }
    }

    private fun MutableList<NavigationEntry>.createNavigationEntry(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): NavigationEntry {
        val cacheKey = cacheKeyProvider()
        val subFlow = (action.destination as? SubFlow)?.let { subFlow ->
            NavigationFlow(
                backStack = listOf(
                    createNavigationEntry(
                        action = NavigationAction(subFlow.startDestination),
                        id = id,
                        requestKey = requestKey
                    )
                ),
                id = subFlow.id
            )
        }
        return NavigationEntry(
            destination = action.destination,
            cacheKey = cacheKey,
            id = id,
            requestKey = requestKey,
            subFlow = subFlow
        )
    }

    private fun isTargetDestination(
        flow: NavigationFlow,
        entry: NavigationEntry,
        target: PopEntryTarget,
        poppedEntries: MutableList<NavigationEntry>
    ) = when (target) {
        is PopEntryTarget.ScreenCount -> entry.subFlow == null && target.count <= poppedEntries.size
        is PopEntryTarget.ToDestination -> entry.destination == target.popTo
        is PopEntryTarget.ToFlow -> entry.subFlow?.id == target.id || !target.popToInclusive && entry.subFlow == null && flow.id == target.id
        is PopEntryTarget.ToId -> entry.id.takeIf { it.isNotBlank() } == target.id
    }

    private suspend fun NavigationState.popScreens(
        action: NavigationAction,
        poppedScreens: MutableList<NavigationEntry>
    ): NavigationFlow =
        popScreens(popEntryTarget = action.options?.popTo, poppedScreens)

    private suspend fun NavigationState.popScreens(
        popEntryTarget: PopEntryTarget?,
        poppedScreens: MutableList<NavigationEntry>
    ): NavigationFlow {
        val newBackStack = popEntryTarget?.let {
            backStack.toMutableList().apply {
                popScreens(
                    flow = this@popScreens,
                    popEntryTarget = it,
                    poppedScreens = poppedScreens
                )
            }
        } ?: backStack
        return copy(backStack = newBackStack)
    }

    private suspend fun MutableList<NavigationEntry>.popScreens(
        flow: NavigationFlow,
        popEntryTarget: PopEntryTarget,
        poppedScreens: MutableList<NavigationEntry>
    ): Boolean {
        while (size > 0) {
            val entry = last()
            val isTargetDestination =
                isTargetDestination(flow, entry, popEntryTarget, poppedScreens)
            when {
                entry.subFlow == null && !isTargetDestination -> {
                    poppedScreens.add(entry)
                    removeEntryWithCacheClear(entry)
                }

                entry.subFlow == null && isTargetDestination -> {
                    if (popEntryTarget.popToInclusive) {
                        poppedScreens.add(entry)
                        removeEntryWithCacheClear(entry)
                    }
                    return true
                }

                else -> {
                    val reachedPopTarget = handlePopScreenInSubFlow(
                        popEntryTarget,
                        isTargetDestination,
                        poppedScreens,
                        entry
                    )
                    if (reachedPopTarget) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private suspend fun MutableList<NavigationEntry>.handlePopScreenInSubFlow(
        popEntryTarget: PopEntryTarget,
        isTargetDestination: Boolean,
        poppedScreens: MutableList<NavigationEntry>,
        entry: NavigationEntry
    ): Boolean {
        if (entry.subFlow != null) {
            val newBackStack = entry.subFlow.backStack.toMutableList()
            val reachedTargetInSubFlow =
                newBackStack.popScreens(entry.subFlow, popEntryTarget, poppedScreens)

            val reachedPopTarget = reachedTargetInSubFlow || isTargetDestination

            val isTargetReachedButPoppingExclusive =
                reachedPopTarget && !popEntryTarget.popToInclusive && isTargetDestination

            val subFlowShouldBeKept =
                newBackStack.isNotEmpty() || isTargetReachedButPoppingExclusive
            if (subFlowShouldBeKept) {
                val newSubFlow = entry.subFlow.copy(backStack = newBackStack)
                remove(entry)
                add(entry.copy(subFlow = newSubFlow))
            } else {
                removeEntryWithCacheClear(entry)
            }
            return reachedPopTarget
        } else {
            return false
        }
    }

    private suspend fun MutableList<NavigationEntry>.removeEntryWithCacheClear(
        entry: NavigationEntry
    ) {
        remove(entry)
        clearCache(entry)
    }

    private suspend fun clearCache(entry: NavigationEntry) {
        entry.subFlow?.backStack?.forEach {
            clearCache(it)
        }
        stateCache.remove(entry.cacheKey)
        resultCache.remove(entry.cacheKey)
    }

    private suspend fun NavigationState.cleanResultCacheForCurrentScreen() {
        currentScreen()?.cacheKey?.let { requesterKey -> resultCache.remove(requesterKey) }
    }

    @Suppress("unchecked_cast")
    suspend fun <T : Any> observe(requestId: Int): Flow<T>? =
        state.value.backStack.currentScreen()?.let {
            resultCache.observe(it.cacheKey, requestId).map { cache -> cache?.data as? T }
                .filterNotNull()
        }

    suspend fun <T : Any> observeResult(requestId: Int): Flow<ResultValue<T>>? =
        state.value.backStack.currentScreen()?.let { entry ->
            resultCache.observe(entry.cacheKey, requestId)
                .filterIsInstance<ResultCacheValue>()
                .filter { it.data != null && it.requestKey.options.isNotEmpty() }
                .map {
                    ResultValue(
                        requestId = it.requestKey.requestId,
                        data = it.data as T,
                        options = it.requestKey.options
                    )
                }
        }

    override suspend fun onNewState(newState: NavigationState) {
        super.onNewState(newState)
        onNewStateCallback?.invoke(newState)
        stateCache.set(cacheKey, newState)
    }
}

fun NavigationStore.next(destination: Destination) =
    next(NavigationAction(destination))

data class ResultValue<T>(
    val requestId: Int,
    val options: Map<String, Any> = emptyMap(),
    val data: T
)

private val NavigationFlow.screenCount: Int
    get() = backStack.fold(0) { acc, entry ->
        acc + (entry.subFlow?.screenCount ?: 1)
    }

val ResultValue<*>.secondaryIdValue: Any
    get() = options.getValue(SECONDARY_ID)

fun List<NavigationEntry>.currentScreen(): NavigationEntry? = lastOrNull()?.let { entry ->
    entry.subFlow?.backStack?.currentScreen() ?: entry
}

fun NavigationFlow.currentScreen(): NavigationEntry? = backStack.currentScreen()

fun NavigationFlow.currentFlow(): NavigationFlow =
    backStack.lastOrNull()?.let {
        it.subFlow?.currentFlow()
    } ?: this

fun NavigationFlow.isRootFlow() = this.id == NavigationFlow.ROOT_FLOW

data class NavigationUpdate(
    val newState: NavigationState,
    val closedEntries: List<NavigationEntry>
)
