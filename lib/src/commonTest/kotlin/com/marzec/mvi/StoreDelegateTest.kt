package com.marzec.mvi

import com.marzec.core.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class StoreDelegateTest {

    val scope = TestScope()
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(scope.testScheduler)

    @BeforeTest
    fun setUp() {
        Store4Impl.stateThread = dispatcher
    }

    @Test
    fun `check if store delegate works properly`() = scope.runTest {
        val store = TestStore(scope, 0, TestDelegateImpl())
        val values = store.state.test(scope, dispatcher)

        store.doThings()

        values.isEqualTo(0, 1, 12, 123)
    }
}

class TestStore(
    scope: CoroutineScope,
    defaultState: Int,
    testDelegate: TestDelegate
) : Store4Impl<Int>(scope, defaultState), TestDelegate by testDelegate {

    init {
        delegates(testDelegate)
    }
}

interface TestDelegate {

    fun doThings()
}

class TestDelegateImpl : StoreDelegate<Int>(), TestDelegate {

    override fun doThings() = intent<Int> {
        onTrigger { flowOf(1, 2, 3) }

        reducer { state * 10 + resultNonNull() }
    }
}