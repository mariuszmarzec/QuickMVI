package com.marzec.core

import com.marzec.mvi.Store3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
fun <STATE: Any> runStoreTest(
    dispatcher: TestDispatcher,
    defaultState: STATE,
    block: suspend StoreTest<STATE>.() -> Unit
) = StoreTest(dispatcher, defaultState).test(block)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest<STATE: Any>(val dispatcher: TestDispatcher, defaultState: STATE) {

    private val job = Job()
    private val storeScope = CoroutineScope(dispatcher + job)
    val store = Store3(storeScope, defaultState)
    lateinit var values: TestCollector<STATE>
    lateinit var testScope: TestScope

    fun test(block: suspend StoreTest<STATE>.() -> Unit) = runTest(dispatcher) {
        testScope = this
        store.init()
        values = store.state.test(this)

        block()

        job.cancel()
    }
}