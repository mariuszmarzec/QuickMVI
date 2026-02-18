package com.marzec.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StoreParentChildTest {
    private lateinit var parentScope: CoroutineScope
    private lateinit var childScope: CoroutineScope

    data class ParentState(val value: Int, val otherValue: String = "")
    data class ChildState(val parentValue: Int)

    private lateinit var parentStore: Store4<ParentState>
    private lateinit var childStore: Store4<ChildState>

    @BeforeTest
    fun setUp() {
        Store4Impl.stateThread = Dispatchers.Unconfined
        parentScope = CoroutineScope(Dispatchers.Unconfined)
        childScope = CoroutineScope(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        parentScope.cancel()
        childScope.cancel()
    }

    @Test
    fun testGetChildStore() = runTest {
        parentStore = Store(parentScope, ParentState(0))
        childStore = parentStore.getChildStore(
            scope = childScope,
            initialState = ChildState(0),
            parentToChild = { ChildState(it.value) },
            childToParent = { copy(value = it.parentValue) }
        )

        // Initialize stores
        val parentState = parentStore.state
        val childState = childStore.state

        assertEquals(ParentState(0), parentState.value)
        assertEquals(ChildState(0), childState.value)

        // When child state is updated, change is applied in parent
        childStore.intent<Unit> {
            reducer {
                ChildState(1)
            }
        }

        assertEquals(ChildState(1), childStore.state.value)
        assertEquals(ParentState(1), parentStore.state.value)

        // When parent change state, child also get notification about state change
        parentStore.intent<Unit> {
            reducer {
                ParentState(2)
            }
        }

        assertEquals(ParentState(2), parentStore.state.value)
        assertEquals(ChildState(2), childStore.state.value)
    }

    @Test
    fun testGetChildStoreWithComplexState() = runTest {
        parentStore = Store(parentScope, ParentState(0, "initial"))
        childStore = parentStore.getChildStore(
            scope = childScope,
            initialState = ChildState(0),
            parentToChild = { ChildState(it.value) },
            childToParent = { copy(value = it.parentValue) }
        )

        // Initialize stores
        val parentState = parentStore.state
        val childState = childStore.state

        assertEquals(ParentState(0, "initial"), parentState.value)
        assertEquals(ChildState(0), childState.value)

        // When child state is updated, change is applied in parent but other fields are preserved
        childStore.intent<Unit> {
            reducer {
                ChildState(1)
            }
        }

        assertEquals(ChildState(1), childStore.state.value)
        assertEquals(ParentState(1, "initial"), parentStore.state.value)

        // When parent change state, child also get notification about state change
        parentStore.intent<Unit> {
            reducer {
                ParentState(2, "updated")
            }
        }

        assertEquals(ParentState(2, "updated"), parentStore.state.value)
        assertEquals(ChildState(2), childStore.state.value)
    }
}
