@file:OptIn(ExperimentalCoroutinesApi::class)

package com.marzec.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store
import com.marzec.mvi.Store4
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.collectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class TimersState(
    val slow: Float = 0f,
    val medium: Float = 0f,
    val quick: Float = 0f,
    val initiallyStarted: Float = 0f
)

@Composable
fun App(
    scope: CoroutineScope = rememberCoroutineScope(),
    tickerCounter: TickerCounterStore = tickerCounterStore(scope)
) {

    val store = TimersStore(scope)
    val textStore = Store(scope, "")
    val autoCancelStore = Store(scope, 0)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Timers(store)
        Spacer(modifier = Modifier.height(16.dp))
        TickerCounter(tickerCounter)
        Spacer(modifier = Modifier.height(16.dp))
        TextFieldExample(textStore)
        Spacer(modifier = Modifier.height(16.dp))
        AutoCancelExample(autoCancelStore)
    }
}

@Composable
private fun Timers(store: TimersStore) {
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

class TimersStore(scope: CoroutineScope) : Store4Impl<TimersState>(scope, TimersState()) {

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

    fun cancel() = sideEffectIntent {
        cancel(
            INTENT_SLOW_TIMER_ID,
            INTENT_MEDIUM_TIMER_ID,
            INTENT_QUICK_TIMER_ID
        )
    }

    fun startAll() = sideEffectIntent {
        startSlowTimer()
        startMediumTimer()
        startQuickTimer()
    }

    fun initialTimer() = intent<TimerEvent> {
        onTrigger {
            timer(timeInMillis = 100 * 1000)
        }

        reducer {
            when (val timer = resultNonNull()) {
                TimerEvent.Done -> state
                is TimerEvent.Progress -> state.copy(initiallyStarted = timer.value)
            }
        }
        sideEffect {
            if (resultNonNull() is TimerEvent.Done) {
                println("INITIALLY STARTED FINISHED")
            }
        }
    }

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

@Composable
private fun TextFieldExample(store: Store4<String>) {

    val state = store.state.collectAsState(Dispatchers.Default)

    Box(Modifier.padding(horizontal = 16.dp)) {

        TextFieldStateful(state.value, onValueChange = {
            store.intent<Unit> {
                reducer {
                    it
                }
            }
        })
    }
}

@Composable
fun TextFieldStateful(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    var state by remember { mutableStateOf(value) }

    snapshotFlow { state }
        .mapLatest {
            onValueChange(it)
        }
        .stateIn(
            scope = rememberCoroutineScope(),
            started = SharingStarted.Lazily,
            initialValue = state
        )

    TextField(
        value = state,
        onValueChange = { newValue ->
            state = newValue
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@Composable
fun AutoCancelExample(autoCancelStore: Store4<Int>) {
    val lastValue = 10
    val state = autoCancelStore.state.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button({
            autoCancelStore.intent<Int> {
                onTrigger {
                    (1..20).asFlow().onEach { delay(50) }
                }

                cancelTrigger(runSideEffectAfterCancel = true) { resultNonNull() == lastValue }

                reducer {
                    resultNonNull()
                }

                sideEffect {
                    if (resultNonNull() == lastValue) {
                        println("END on $lastValue")
                    }
                }
            }
        }) {
            Text("Start, will be canceled at $lastValue")
        }
        Spacer(Modifier.width(16.dp))
        Text("Counter: ${state.value}")
    }
}