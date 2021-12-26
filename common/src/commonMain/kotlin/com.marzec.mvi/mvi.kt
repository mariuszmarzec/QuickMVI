package com.marzec.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
open class Store2<State : Any>(
    private val scope: CoroutineScope,
    private val defaultState: State
) {

    private val _intentContextFlow =
        MutableSharedFlow<Intent2<State, Any>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    private var _state = MutableStateFlow(defaultState)

    val state: StateFlow<State>
        get() = _state

    open val identifier: Any = Unit

    private var pause = MutableStateFlow(false)

    suspend fun init(initialAction: suspend () -> Unit = {}) {
        pause.emit(false)
//        scope.launch {
//            actions.onSubscription { initialAction() }
//                .flatMapMerge { intent ->
//                    debug("actions flatMapMerge intent: $intent")
//                    val currentState = _state.value
//                    val flow = intent.onTrigger(currentState) ?: flowOf(null)
//                    flow.map {
//                        debug("actions onTrigger state: ${intent.state} result: $it")
//                        intent.copy(
//                            state = _state.value,
//                            result = it,
//                            sideEffect = intent.sideEffect
//                        )
//                    }.makeCancellableIfNeeded(intent.isCancellableFlowTrigger)
//                }.collect {
//                    debug("actions collect intent: $it")
//                    _intentContextFlow.emit(it)
//                }
//        }
        scope.launch {
            _intentContextFlow
                .runningReduce { old, new ->
                    val reducedState = if (!new.paused) {
                        new.reducer(new.result, old.state!!)
                    } else {
                        old.state
                    }
                    debug("_intentContextFlow runningReduce state: $reducedState")
                    old.copy(
                        state = reducedState,
                        result = new.result,
                        sideEffect = new.sideEffect,
                        paused = new.paused,
                        isCancellableFlowTrigger = new.isCancellableFlowTrigger,
                        runSideEffectAfterCancel = new.runSideEffectAfterCancel
                    )
                }.onEach {
                    debug("_intentContextFlow onEach intent: $it")
                    onNewState(it.state!!)

                    if (!it.paused || it.runSideEffectAfterCancel) {
                        it.sideEffect?.invoke(it.result, it.state)
                    }
                }.collect {
                    debug("_intentContextFlow collect state: ${it.state}")
                    _state.emit(it.state!!)
                }
        }

        _intentContextFlow.emit(Intent2(state = defaultState, result = null))
    }

    private fun Flow<Intent2<State, Any>>.makeCancellableIfNeeded(
        isCancellableFlowTrigger: Boolean,
    ) = if (isCancellableFlowTrigger) {
        combine(pause) { intent, paused ->
            intent.copy(paused = paused)
        }
    } else {
        this
    }

    protected fun <T> Flow<T>.cancelFlowsIf(function: (T) -> Boolean): Flow<T> =
        onEach {
            if (function.invoke(it)) {
                cancelFlows()
            }
        }

    protected fun cancelFlows() {
        scope.launch {
            pause.emit(true)
        }
    }

    fun <Result : Any> intent(buildFun: IntentBuilder<State, Result>.() -> Unit) {
        scope.launch {
            val intent = IntentBuilder<State, Result>().apply { buildFun() }.build()

            val flow = intent.onTrigger(_state.value) ?: flowOf(null)
            flow.collect {
                _intentContextFlow.emit(
                    intent.copy(
                        state = _state.value,
                        result = it,
                        sideEffect = intent.sideEffect
                    )
                )
            }
        }
    }

    fun sideEffectIntent(func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit) {
// TODO
//        scope.launch {
//            actions.emit(IntentBuilder<State, Unit>().apply { sideEffect(func) }.build())
//        }
    }

    open suspend fun onNewState(newState: State) = Unit

    fun debug(string: String) {
        if (false) {
            println("MVI-${defaultState.javaClass.simpleName} $string")
        }
    }
}

data class Intent2<State, out Result : Any>(
    val onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null },
    val reducer: suspend (result: Any?, stateParam: State) -> State = { _, stateParam -> stateParam },
    val sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null,
    val state: State?,
    val result: Result?,
    val isCancellableFlowTrigger: Boolean = false,
    val runSideEffectAfterCancel: Boolean = false,
    val paused: Boolean = false
)

@Suppress("UNCHECKED_CAST")
class IntentBuilder<State : Any, Result : Any>(
    private var onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null },
    private var reducer: suspend (result: Any?, stateParam: State) -> State = { _, stateParam -> stateParam },
    private var sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null,
    private var isCancellableFlowTrigger: Boolean = false,
    private var runSideEffectAfterCancel: Boolean = false
) {

    fun onTrigger(
        isCancellableFlowTrigger: Boolean = false,
        runSideEffectAfterCancel: Boolean = false,
        func: suspend IntentContext<State, Result>.() -> Flow<Result>? = { null }
    ): IntentBuilder<State, Result> {
        this.isCancellableFlowTrigger = isCancellableFlowTrigger
        this.runSideEffectAfterCancel = runSideEffectAfterCancel
        onTrigger = { state ->
            IntentContext<State, Result>(state, null).func()
        }
        return this
    }

    fun reducer(func: suspend IntentContext<State, Result>.() -> State): IntentBuilder<State, Result> {
        reducer = { result: Any?, state ->
            val res = result as? Result
            IntentContext(state, res).func()
        }
        return this
    }

    fun sideEffect(func: suspend IntentContext<State, Result>.() -> Unit): IntentBuilder<State, Result> {
        sideEffect = { result: Any?, state ->
            val res = result as? Result
            IntentContext(state, res).func()
        }
        return this
    }

    fun build(): Intent2<State, Result> = Intent2(
        onTrigger = onTrigger,
        reducer = reducer,
        sideEffect = sideEffect,
        state = null,
        result = null,
        isCancellableFlowTrigger = isCancellableFlowTrigger,
        runSideEffectAfterCancel = runSideEffectAfterCancel
    )

    data class IntentContext<State, Result>(
        val state: State,
        val result: Result?
    ) {
        fun resultNonNull(): Result = result!!
    }
}

fun <State : Any, Result : Any> Intent2<State, Result>.rebuild(
    buildFun: IntentBuilder<State, Result>.(Intent2<State, Result>) -> Unit
) =
    IntentBuilder(
        onTrigger = onTrigger,
        reducer = reducer,
        sideEffect = sideEffect,
        isCancellableFlowTrigger = isCancellableFlowTrigger,
        runSideEffectAfterCancel = runSideEffectAfterCancel
    ).apply { buildFun(this@rebuild) }.build()
