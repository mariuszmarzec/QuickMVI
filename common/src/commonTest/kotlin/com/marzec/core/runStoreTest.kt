package com.marzec.core

import com.marzec.mvi.Store
import com.marzec.mvi.Store4
import com.marzec.mvi.Store4Impl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.*

fun <STATE: Any> runStoreTest(
    defaultState: STATE,
    block: suspend StoreTest<STATE>.() -> Unit
) = StoreTest(defaultState).test(block)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest<STATE: Any>(defaultState: STATE) {

    val scope = TestScope()
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(scope.testScheduler)
    val store = Store(scope, defaultState)
    lateinit var values: TestCollector<STATE>

    fun test(block: suspend StoreTest<STATE>.() -> Unit) = scope.runTest {
        Store4Impl.stateThread = dispatcher
        store.init()
        values = store.state.test(this, dispatcher)

        block()
    }
}