package com.marzec.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import kotlinx.coroutines.CoroutineScope

class TickerCounterStore(scope: CoroutineScope) : Store3<Int>(scope, 0) {

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
