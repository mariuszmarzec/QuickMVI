package com.marzec.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun <ParentState : Any, ChildState : Any> Store4<ParentState>.getChildStore(
    scope: CoroutineScope,
    initialState: ChildState,
    parentToChild: (ParentState) -> ChildState,
    childToParent: ParentState.(ChildState) -> ParentState
): Store4<ChildState> {
    val parentStore = this
    val childStore = Store(scope, initialState)
    childStore.stateInitializer = {
        val parentStateFlow = parentStore.state as? MutableStateFlow<ParentState>
            ?: throw IllegalStateException("Parent store state must be MutableStateFlow")

        MappedStateFlow(
            parentStateFlow,
            parentToChild = parentToChild,
            childToParent = childToParent,
        )
    }
    return childStore
}

private class MappedStateFlow<T, R>(
    private val origin: MutableStateFlow<T>,
    private val parentToChild: (T) -> R,
    private val childToParent: T.(R) -> T
) : MutableStateFlow<R> {
    override var value: R
        get() = parentToChild(origin.value)
        set(v) { origin.value = origin.value.childToParent(v) }

    override val subscriptionCount: StateFlow<Int> = origin.subscriptionCount

    override suspend fun emit(value: R) {
        origin.emit(origin.value.childToParent(value))
    }

    override fun tryEmit(value: R): Boolean {
        return origin.tryEmit(origin.value.childToParent(value))
    }

    override fun compareAndSet(expect: R, update: R): Boolean {
        // This is tricky because we need the current parent state to calculate the new parent state
        // But compareAndSet is atomic.
        // If we use origin.value, it might change between read and compareAndSet.
        // However, MutableStateFlow.compareAndSet compares the *new* value with the *current* value.
        // Here we are mapping expectations.
        // A strict implementation of compareAndSet for mapped flow with state dependency is hard/impossible without a lock or loop.
        // But for StateFlow, compareAndSet is usually used to update if current value matches expect.

        // Let's try a best effort approach or throw if not supported.
        // Given the requirement "childToParent should mutate previous parent state",
        // it implies we need the previous state.

        // If we assume single threaded dispatcher for state updates (which MVI usually does),
        // we might get away with reading origin.value.
        val currentParent = origin.value
        val expectedParent = currentParent.childToParent(expect)
        val newParent = currentParent.childToParent(update)
        return origin.compareAndSet(expectedParent, newParent)
    }

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        origin.collect { value ->
            collector.emit(parentToChild(value))
        }
    }

    override val replayCache: List<R>
        get() = origin.replayCache.map { parentToChild(it) }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        origin.resetReplayCache()
    }
}
