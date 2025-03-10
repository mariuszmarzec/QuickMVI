package com.marzec.mvi

import com.marzec.core.runStoreTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals


@OptIn(ExperimentalCoroutinesApi::class)
class Store3Test {

    val scope = TestScope()
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(scope.testScheduler)

    @Test
    fun `checks if trigger, state is updated in classic way`() = scope.runTest {
        Store4Impl.stateThread = dispatcher

        val store = Store<List<Int>>(scope, listOf(0))
        val states = mutableListOf<Any>()

        val job = launch(dispatcher) { store.state.toCollection(states) }

        store.intent<Int> {
            onTrigger {
                flowOf(1, 2)
            }
            reducer {
                state + resultNonNull()
            }
        }

        advanceUntilIdle()

        assertEquals(listOf<Any>(listOf(0), listOf(0, 1), listOf(0, 1, 2)), states)

        job.cancel()
    }

    @Test
    fun `checks if trigger, state is updated`() = runStoreTest(listOf(0)) {

        store.intent<Int> {
            onTrigger { flowOf(1, 2) }
            reducer { state + resultNonNull() }
        }

        values.isEqualTo(listOf(0), listOf(0, 1), listOf(0, 1, 2))
    }

    @Test
    fun `checks if action run, when intent passed, then state is updated`() = runStoreTest(listOf(0)) {

        val intent = intent<List<Int>, Int> {
            onTrigger { flowOf(1, 2) }

            reducer { state + resultNonNull() }
        }

        store.run(intent)

        values.isEqualTo(listOf(0), listOf(0, 1), listOf(0, 1, 2))
    }

    @Test
    fun `checks if trigger, reducer and side effect are called`() = runStoreTest(0) {
        val func = mockk<suspend IntentContext<Int, Int>.() -> Unit>(relaxed = true)

        store.intent {
            onTrigger { flowOf(1) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        store.intent {
            onTrigger { flowOf(2) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        values.isEqualTo(0, 1, 2)
        coEvery { func.invoke(any()) }
    }

    @Test
    fun `checks calling second time intent with id, kills previous one`() = runStoreTest(0) {
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

        scope.advanceTimeBy(1100)

        store.intent("id") {
            onTrigger { flowOf(2) }

            reducer { resultNonNull() }
        }
        scope.advanceTimeBy(1000)

        values.isEqualTo(0, 1, 2)
    }

    @Test
    fun `checks if running few intents in side effect works`() = runStoreTest(0) {

        store.sideEffectIntent {

            store.reducerIntent { 1 }

            store.reducerIntent { 2 }
        }

        values.isEqualTo(0, 1, 2)
    }

    @Test
    fun `checks if initial action is called`() = runStoreTest(0) {
        store.init {
            store.intent<Unit> {
                reducer { 1 }
            }
            scope.advanceUntilIdle()
        }

        values.isEqualTo(0, 1)
    }

    @Test
    fun `run sideEffect after cancelling job`() = runStoreTest(0) {
        var sideEffectResult: Int = -1

        store.intent("intent") {
            onTrigger {
                flow {
                    emit(1)
                    delay(500)
                    emit(2)
                    delay(500)
                    emit(3)
                    delay(100)
                    emit(4)
                }
            }

            cancelTrigger(runSideEffectAfterCancel = true) {
                resultNonNull() == 2
            }

            reducer { resultNonNull() }

            sideEffect {
                if (resultNonNull() == 2) {
                    sideEffectResult = 2
                }
            }
        }

        scope.advanceTimeBy(2000)

        values.isEqualTo(0, 1)
        assertEquals(2, sideEffectResult)
    }

    @Test
    fun `run mapped intent`() = runStoreTest(Result.success(1)) {
        var sideValue = 0
        val innerIntent = intent<Int, Int> {
            onTrigger { flowOf(state * 2) }

            reducer { resultNonNull() }

            sideEffect { sideValue = state * 10 }
        }

        val mappedIntent = innerIntent.map<Result<Int>, Int, Int>(
            stateReducer = { newInState: Int ->
                state.map { newInState }
            },
            stateMapper = { it.getOrNull() }
        )

        store.run(mappedIntent)

        values.isEqualTo(
            Result.success(1),
            Result.success(2)
        )
        assertEquals(20, sideValue)
    }

    @Test
    fun `run mapped intent with access to inner reducer`() = runStoreTest(Result.success(1)) {
        var sideValue = 0
        val innerIntent = intent<Int, Int> {
            onTrigger { flowOf(state * 2) }

            reducer { resultNonNull() }

            sideEffect { sideValue = state * 10 }
        }

        val mappedIntent = innerIntent.mapInnerReducer<Result<Int>, Int, Int>(
            stateReducer = { innerReducer ->
                state.map { innerReducer(resultNonNull(), it) }
            },
            stateMapper = { it.getOrNull() }
        )

        store.run(mappedIntent)

        values.isEqualTo(
            Result.success(1),
            Result.success(2)
        )
        assertEquals(20, sideValue)
    }

    @Test
    fun `run composite intent`() = runStoreTest(10) {
        val sideValues = mutableListOf<String>()
        val innerIntent = intent<Int, Int> {
            onTrigger { flowOf( 2, 3) }

            reducer { state + resultNonNull() }

            sideEffect { sideValues.add(state.toString()) }
        }

        val mappedIntent = innerIntent.composite { inner ->
            onTrigger {
                innerIntent.onTrigger(state)?.map { it * 10 }
            }

            reducer {
                inner.reducer(result, state) + 1
            }
        }

        store.run(mappedIntent)

        values.isEqualTo(10, 31, 62)
        assertEquals(listOf("31", "62"), sideValues)
    }
}
