package com.marzec.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import androidx.compose.runtime.LaunchedEffect

data class TimersState(
    val slow: Float = 0f,
    val medium: Float = 0f,
    val quick: Float = 0f
)

@Composable
fun App() {

    val scope = rememberCoroutineScope()
    val store = TimersStore(scope)

    Screen(store)
}

@Composable
private fun Screen(store: TimersStore) {
    val state by store.state.collectAsState()

    LaunchedEffect(key1 = Unit) {
        store.init()
    }

    Column {

        ProgressRun(
            progress = state.slow,
            onStartButtonClick = { store.startSlowTimer() }
        )
        ProgressRun(
            progress = state.medium,
            onStartButtonClick = { store.startMediumTimer() }
        )
        ProgressRun(
            progress = state.quick,
            onStartButtonClick = { store.startQuickTimer() }
        )
    }
}

class TimersStore(scope: CoroutineScope) : Store2<TimersState>(scope, TimersState()) {

    fun startSlowTimer() = intent<Float> {
        onTrigger {
            timer(timeInMillis = 10 * 1000)
        }

        reducer {
            state.copy(slow = resultNonNull())
        }
    }

    fun startMediumTimer() = intent<Float> {
        onTrigger {
            timer(timeInMillis = 5 * 1000)
        }

        reducer {
            state.copy(medium = resultNonNull())
        }
    }

    fun startQuickTimer() = intent<Float> {
        onTrigger {
            timer(timeInMillis = 3 * 1000)
        }

        reducer {
            state.copy(quick = resultNonNull())
        }


    }

    var i = 0

    override suspend fun onNewState(newState: TimersState) {
        println("NEW STATE: $i $newState")
        i = i.inc()
    }
}

@Composable
fun ProgressRun(
    progress: Float,
    onStartButtonClick: () -> Unit
) {
    Column {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                onStartButtonClick.invoke()
            }) {
                Text("Start")
            }
            Spacer(Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = progress,
                color = Color.LightGray,
                backgroundColor = Color.DarkGray,
                modifier = Modifier.width(500.dp).height(50.dp)
            )
        }
    }
}

private fun timer(timeInMillis: Long) = flow {
    val maxValue = 1f
    val timeBetweenEmissions = 100L
    val step = maxValue / (timeInMillis / timeBetweenEmissions)
    var progress = 0f
    emit(progress)
    while (progress < maxValue) {
        delay(timeBetweenEmissions)
        progress = progress.plus(step).let {
            if (it >= maxValue) {
                maxValue
            } else {
                it
            }
        }
        emit(progress)
    }
}
