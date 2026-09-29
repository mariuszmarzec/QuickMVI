package com.marzec.core

import com.marzec.mvi.Store4Impl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.*

fun <STATE : Any, STORE : Store4Impl<STATE>> runStoreTest(
    block: suspend StoreTest<STATE, STORE>.() -> Unit
) = StoreTest<STATE, STORE>().run(block)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest<STATE : Any, STORE : Store4Impl<STATE>> {

    val scope = TestScope()
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(scope.testScheduler)
    lateinit var store: STORE
    lateinit var values: TestCollector<STATE>

    fun run(block: suspend StoreTest<STATE, STORE>.() -> Unit): Unit = scope.runTest {
        Store4Impl.stateThread = dispatcher

        block()
    }

    suspend fun STORE.test(block: suspend STORE.() -> Unit) {
        init()
        values = store.state.test(scope, dispatcher)

        store.block()
    }
}