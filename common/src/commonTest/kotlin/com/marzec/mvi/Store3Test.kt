package com.marzec.mvi

import com.marzec.core.runStoreTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


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

        store.intent {
            onTrigger { flowOf(1) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        testScope.advanceUntilIdle()

        store.intent {
            onTrigger { flowOf(2) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        values.isEqualTo(0, 1, 2)
        coEvery { func.invoke(any()) }
    }

    @Test
    fun `checks calling second time intent with id, kills previous one`() = runStoreTest(dispatcher, 0) {
        store.intent("id") {
            onTrigger {
                flow {
                    delay(1000)
                    emit(1)
                    delay(1000)
                    emit(-1)
                }
            }

            reducer { resultNonNull() }
        }

        testScope.advanceTimeBy(1100)

        store.intent("id") {
            onTrigger { flowOf(2) }

            reducer { resultNonNull() }
        }
        testScope.advanceTimeBy(1000)

        values.isEqualTo(0, 1, 2)
    }

    @Test
    fun `checks if running few intents in side effect works`() = runStoreTest(dispatcher, 0) {

        store.sideEffectIntent {

            store.reducerIntent { 1 }

            testScope.advanceUntilIdle()

            store.intent<Unit> {
                reducer { 2 }
            }
        }

        values.isEqualTo(0, 1, 2)
    }

    @Test
    fun `checks if initial action is called`() = runStoreTest(dispatcher, 0) {
        store.init {
            store.intent<Unit> {
                reducer { 1 }
            }
            testScope.advanceUntilIdle()
        }

        values.isEqualTo(0, 1)
    }
}
