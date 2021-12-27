package com.marzec.mvi

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.random.Random

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

    private val jobs = hashMapOf<String, Pair<Long, Job>>()

    suspend fun init(initialAction: suspend () -> Unit = {}) {
        pause.emit(false)
        scope.launch {
            _intentContextFlow
                .runningReduce { old, new ->
                    val reducedState = if (!new.paused) {
                        new.reducer(new.result, old.state!!)
                    } else {
                        old.state
                    }
                    old.copy(
                        state = reducedState,
                        result = new.result,
                        sideEffect = new.sideEffect,
                        paused = new.paused,
                        isCancellableFlowTrigger = new.isCancellableFlowTrigger,
                        runSideEffectAfterCancel = new.runSideEffectAfterCancel
                    )
                }.onEach {
                    onNewState(it.state!!)
                    if (!it.paused || it.runSideEffectAfterCancel) {
                        it.sideEffect?.invoke(it.result, it.state)
                    }
                }.collect {
                    println("jobs: ${jobs.size}")
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

    open suspend fun onNewState(newState: State) = Unit

    fun <Result : Any> intent(id: String = "", buildFun: IntentBuilder<State, Result>.() -> Unit) {
        intentInternal(id, buildFun)
    }

    fun sideEffectIntent(func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit) {
// TODO
//        scope.launch {
//            actions.emit(IntentBuilder<State, Unit>().apply { sideEffect(func) }.build())
//        }
    }

    private fun <Result : Any> intentInternal(id: String = "", buildFun: IntentBuilder<State, Result>.() -> Unit) {
        jobs[id]?.second?.cancel()

        val identifier = Random.nextLong()
        val job = launchNewJob(buildFun)
        if (id.isNotEmpty()) {
            job.invokeOnCompletion {
                if (jobs[id]?.first == identifier) {
                    jobs.remove(id)
                }
            }
            if (job.isActive) {
                jobs[id] = identifier to job
            }
        }
    }

    private fun <Result : Any> launchNewJob(buildFun: IntentBuilder<State, Result>.() -> Unit): Job = scope.launch {
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
