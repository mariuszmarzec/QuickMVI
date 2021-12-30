package com.marzec.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

data class TimersState(
    val slow: Float = 0f,
    val medium: Float = 0f,
    val quick: Float = 0f,
    val initiallyStarted: Float = 0f
)

@Composable
fun App() {

    val scope = rememberCoroutineScope()
    val store = TimersStore(scope)

    Screen(store)
}

@Composable
private fun Screen(store: TimersStore) {
    val state by store.collectState {
        store.initialTimer()
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

        Text(
            text = "Timer started with program: ${String.format("%.3f", state.initiallyStarted)}",
            modifier = Modifier.padding(16.dp)
        )

        Row(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    store.cancel()
                }) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    store.startAll()
                }) {
                Text("Start All")
            }
        }
    }
}

class TimersStore(scope: CoroutineScope) : Store3<TimersState>(scope, TimersState()) {

    fun startSlowTimer() = intent<TimerEvent>(INTENT_SLOW_TIMER_ID) {
        onTrigger {
            timer(timeInMillis = 10 * 1000)
        }

        reducer {
            when (val timer = resultNonNull()) {
                TimerEvent.Done -> state
                is TimerEvent.Progress -> state.copy(slow = timer.value)
            }
        }

        sideEffect {
            if (resultNonNull() is TimerEvent.Done) {
                println("SLOW FINISHED")
            }
        }
    }

    fun startMediumTimer() = intent<TimerEvent>(INTENT_MEDIUM_TIMER_ID) {
        onTrigger {
            timer(timeInMillis = 5 * 1000)
        }

        reducer {
            when (val timer = resultNonNull()) {
                TimerEvent.Done -> state
                is TimerEvent.Progress -> state.copy(medium = timer.value)
            }
        }

        sideEffect {
            if (resultNonNull() is TimerEvent.Done) {
                println("MEDIUM FINISHED")
            }
        }
    }

    fun startQuickTimer() = intent<TimerEvent>(INTENT_QUICK_TIMER_ID) {
        onTrigger {
            timer(timeInMillis = 3 * 1000)
        }

        reducer {
            when (val timer = resultNonNull()) {
                TimerEvent.Done -> state
                is TimerEvent.Progress -> state.copy(quick = timer.value)
            }
        }

        sideEffect {
            if (resultNonNull() is TimerEvent.Done) {
                println("QUICK FINISHED")
            }
        }
    }

    fun cancel() = sideEffect {
        cancel(
            INTENT_SLOW_TIMER_ID,
            INTENT_MEDIUM_TIMER_ID,
            INTENT_QUICK_TIMER_ID
        )
    }

    fun startAll() = sideEffect {
        startSlowTimer()
        startMediumTimer()
        startQuickTimer()
    }

    fun initialTimer() = intent(
        onTrigger {
            timer(timeInMillis = 100 * 1000)
        }.reducer {
            when (val timer = resultNonNull()) {
                TimerEvent.Done -> state
                is TimerEvent.Progress -> state.copy(initiallyStarted = timer.value)
            }
        }.sideEffect {
            if (resultNonNull() is TimerEvent.Done) {
                println("INITIALLY STARTED FINISHED")
            }
        }
    )

    companion object {
        private const val INTENT_SLOW_TIMER_ID = "slow timer"
        private const val INTENT_MEDIUM_TIMER_ID = "medium timer"
        private const val INTENT_QUICK_TIMER_ID = "quick timer"
    }
}

sealed class TimerEvent {
    class Progress(val value: Float) : TimerEvent()
    object Done : TimerEvent()
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
    emit(TimerEvent.Progress(progress))
    while (progress < maxValue) {
        delay(timeBetweenEmissions)
        progress = progress.plus(step).let {
            if (it >= maxValue) {
                maxValue
            } else {
                it
            }
        }
        emit(TimerEvent.Progress(progress))
    }
    emit(TimerEvent.Done)
}
