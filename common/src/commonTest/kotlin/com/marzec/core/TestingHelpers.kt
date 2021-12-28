package com.marzec.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.test.assertEquals

fun <T> Flow<T>.test(scope: CoroutineScope): TestCollector<T> {
    return TestCollector(scope, this).test()
}

class TestCollector<T>(
    private val scope: CoroutineScope,
    private val flow: Flow<T>
) {
    private val values = mutableListOf<T>()
    private lateinit var job: Job

    fun test(): TestCollector<T> {
        job = scope.launch {
            flow.collect { values.add(it) }
        }
        return this
    }

    fun values(): List<T> {
        cancelIfActive()
        return values
    }

    fun isEqualTo(expected: List<T>) {
        cancelIfActive()
        assertEquals(expected, values)
    }

    fun isEqualTo(vararg expected: T) {
        cancelIfActive()
        assertEquals(expected.toList(), values)
    }

    private fun cancelIfActive() {
        if (job.isActive) {
            job.cancel()
        }
    }
}
