package com.marzec.mvi

import com.marzec.core.TestCollector
import com.marzec.core.runStoreTest
import com.marzec.core.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest


@OptIn(ExperimentalCoroutinesApi::class)
class Store3Test {

    val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checks if trigger, reducer and side effect are called`() = runStoreTest(dispatcher, 0) {
        val func = mockk<suspend IntentBuilder.IntentContext<Int, Int>.() -> Unit>(relaxed = true)

        store.intent<Int> {
            onTrigger { flowOf(1) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        testScope.advanceUntilIdle()

        store.intent<Int> {
            onTrigger { flowOf(2) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        values.isEqualTo(0, 1, 2)
        coEvery { func.invoke(any()) }
    }
}
