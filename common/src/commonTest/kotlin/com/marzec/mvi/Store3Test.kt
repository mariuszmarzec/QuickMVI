package com.marzec.mvi

import com.marzec.core.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class Store3Test {

    @Test
    fun `checks if trigger, reducer and side effect are called`() = runTest {
        val func = mockk<suspend IntentBuilder.IntentContext<Int, Int>.() -> Unit>(relaxed = true)
        val store = Store3(this, 0)
        store.init()

        val test = store.state.test(this)

        store.intent<Int> {
            onTrigger { flowOf(1) }

            reducer { resultNonNull() }

            sideEffect(func)
        }

        test.isEqualTo(1)
        coEvery { func.invoke(any()) }
    }
}