package com.marzec.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store
import com.marzec.mvi.Store4
import com.marzec.mvi.collectState
import kotlinx.coroutines.CoroutineScope

fun TickerCounterStore(
    scope: CoroutineScope,
    defaultState: Int = 0,
    onNewStateCallback: (Int) -> Unit = {}
) = TickerCounterStore(Store(scope, defaultState).apply {
    this.onNewStateCallback = onNewStateCallback
})

class TickerCounterStore(
    private val store: Store4<Int>
) : Store4<Int> by store {

    fun bumpCounter() = intent<Int> {

        reducer { state.inc() }

        sideEffect { println("Bumped to $state") }
    }
}

@Composable
fun TickerCounter(store: TickerCounterStore) {

    val state: Int by store.collectState()

    Button(
        modifier = Modifier.padding(16.dp),
        onClick = { store.bumpCounter() }) {
        Text("$state")
    }
}
