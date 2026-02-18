package com.marzec.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector

fun <ParentState : Any, ChildState : Any> Store4<ParentState>.getChildStore(
    scope: CoroutineScope,
    initialState: ChildState,
    parentToChild: (ParentState) -> ChildState,
    childToParent: ParentState.(ChildState) -> ParentState
): Store4<ChildState> {
    val parentStore = this
    val childStore = Store(scope, initialState)
    childStore.stateInitializer = {
        val parentStateFlow = parentStore.state as? StateContainer<ParentState>
            ?: throw IllegalStateException("Parent store state must be StateContainer")

        MappedStateFlow(
            parentStateFlow,
            parentToChild = parentToChild,
            childToParent = childToParent,
        )
    }
    return childStore
}

private class MappedStateFlow<T, R>(
    private val origin: StateContainer<T>,
    private val parentToChild: (T) -> R,
    private val childToParent: T.(R) -> T
) : StateContainer<R> {
    override val value: R
        get() = parentToChild(origin.value)

    override suspend fun update(function: (R) -> R) {
        origin.update { parentState ->
            val currentChildState = parentToChild(parentState)
            val newChildState = function(currentChildState)
            parentState.childToParent(newChildState)
        }
    }

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        origin.collect { value ->
            collector.emit(parentToChild(value))
        }
    }

    override val replayCache: List<R>
        get() = origin.replayCache.map { parentToChild(it) }
}
