@file:OptIn(ExperimentalTypeInference::class)

package com.marzec.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
fun <State : Any> Store(
    scope: CoroutineScope,
    defaultState: State,
    onNewStateCallback: (State) -> Unit = { }
) = Store4Impl(scope, defaultState, onNewStateCallback)

interface Store4<State : Any> {

    val state: StateFlow<State>

    val identifier: Any

    suspend fun init(initialAction: suspend () -> Unit = {})
    fun cancelAll()

    suspend fun onNewState(newState: State)
    fun <Result : Any> intent(id: String? = null, @BuilderInference buildFun: IntentBuilder<State, Result>.() -> Unit)
    fun <Result : Any> intent(@BuilderInference buildFun: IntentBuilder<State, Result>.() -> Unit)
    fun <Result : Any> intent(id: String? = null, builder: IntentBuilder<State, Result>)
    fun <Result : Any> intent(builder: IntentBuilder<State, Result>)
    fun <Result : Any> triggerIntent(func: suspend IntentContext<State, Result>.() -> Flow<Result>?)

    @Deprecated("Will be removed", replaceWith = ReplaceWith("triggerIntent(func)"))
    fun <Result : Any> onTrigger(
        @BuilderInference func: suspend IntentContext<State, Result>.() -> Flow<Result>? = { null }
    ): IntentBuilder<State, Result>

    fun reducerIntent(func: IntentContext<State, Unit>.() -> State)

    @Deprecated("Will be removed", replaceWith = ReplaceWith("reducerIntent(func)"))
    fun reduce(func: IntentContext<State, Unit>.() -> State): IntentBuilder<State, Unit>

    @Deprecated("Will be removed", replaceWith = ReplaceWith("sideEffectIntent(func)"))
    fun sideEffect(func: suspend IntentContext<State, Unit>.() -> Unit)
    fun sideEffectIntent(func: suspend IntentContext<State, Unit>.() -> Unit)
    fun <Result : Any> run(intent: Intent3<State, Result>)
    fun <Result : Any> run(id: String?, intent: Intent3<State, Result>)
}

@ExperimentalCoroutinesApi
open class Store4Impl<State : Any>(
    private val scope: CoroutineScope,
    private val defaultState: State,
    private val onNewStateCallback: (State) -> Unit = {}
) : Store4<State> {

    private var _state = MutableStateFlow(defaultState)

    override val state: StateFlow<State>
        get() = _state

    override val identifier: Any = Unit

    private val jobs = hashMapOf<String, IntentJob<State, out Any>>()

    override suspend fun init(initialAction: suspend () -> Unit) {
        initialAction()
    }

    override fun cancelAll() {
        jobs.forEach { it.value.cancelJob() }
    }

    override suspend fun onNewState(newState: State) {
        onNewStateCallback(newState)
    }

    override fun <Result : Any> intent(id: String?, @BuilderInference buildFun: IntentBuilder<State, Result>.() -> Unit) {
        intentByBuilderInternal(id, buildFun)
    }

    override fun <Result : Any> intent(@BuilderInference buildFun: IntentBuilder<State, Result>.() -> Unit) {
        intentByBuilderInternal(id = null, buildFun)
    }

    override fun <Result : Any> intent(id: String?, builder: IntentBuilder<State, Result>) {
        intentByBuilderInternal(id, builder)
    }

    override fun <Result : Any> intent(builder: IntentBuilder<State, Result>) {
        intentByBuilderInternal(id = null, builder)
    }

    override fun <Result : Any> triggerIntent(func: suspend IntentContext<State, Result>.() -> Flow<Result>?) {
        intentByBuilderInternal<Result> { onTrigger(func) }
    }

    @Deprecated("Will be removed", replaceWith = ReplaceWith("triggerIntent(func)"))
    override fun <Result : Any> onTrigger(
        @BuilderInference func: suspend IntentContext<State, Result>.() -> Flow<Result>?
    ): IntentBuilder<State, Result> {
        return IntentBuilder<State, Result>().apply { onTrigger(func) }
    }

    override fun reducerIntent(func: IntentContext<State, Unit>.() -> State) {
        intentByBuilderInternal<Unit> { reducer(func) }
    }

    @Deprecated("Will be removed", replaceWith = ReplaceWith("reducerIntent(func)"))
    override fun reduce(func: IntentContext<State, Unit>.() -> State): IntentBuilder<State, Unit> {
        return IntentBuilder<State, Unit>().apply { reducer(func) }
    }

    @Deprecated("Will be removed", replaceWith = ReplaceWith("sideEffectIntent(func)"))
    override fun sideEffect(func: suspend IntentContext<State, Unit>.() -> Unit) {
        intentByBuilderInternal<Unit> { sideEffect(func) }
    }

    override fun sideEffectIntent(func: suspend IntentContext<State, Unit>.() -> Unit) {
        intentByBuilderInternal<Unit> { sideEffect(func) }
    }

    private fun <Result : Any> intentByBuilderInternal(
        id: String? = null,
        buildFun: IntentBuilder<State, Result>.() -> Unit
    ) {
        val builder = IntentBuilder<State, Result>().apply { buildFun() }
        intentByBuilderInternal(id, builder)
    }

    private fun <Result : Any> intentByBuilderInternal(id: String? = null, builder: IntentBuilder<State, Result>) {
        val intent = builder.build()
        run(id, intent)
    }

    override fun <Result : Any> run(intent: Intent3<State, Result>) {
        run(id = null, intent)
    }

    override fun <Result : Any> run(id: String?, intent: Intent3<State, Result>) {
        val newJobId = id ?: System.nanoTime().toString()
        jobs[newJobId]?.cancelJob()

        val identifier = Random.nextLong()
        val job = launchNewJob(intent, newJobId)

        job.invokeOnCompletion {
            if (jobs[newJobId]?.identifier == identifier) {
                jobs.remove(newJobId)
            }
        }
        jobs[newJobId] = IntentJob(identifier, intent, job)
        job.start()
    }

    private fun <Result : Any> launchNewJob(
        intent: Intent3<State, Result>,
        jobId: String
    ): Job = scope.launch(start = CoroutineStart.LAZY) {
        val flow = intent.onTrigger(_state.value) ?: flowOf(null)

        flow.collect { result ->
            processTriggeredValue(intent, result, jobId)
        }
    }

    private suspend fun <Result : Any> processTriggeredValue(
        intent: Intent3<State, Result>,
        result: Result?,
        jobId: String
    ) {
        val shouldCancel = intent.cancelTrigger?.invoke(result, _state.value) ?: false

        if (shouldCancel) {
            runCancellationAndSideEffectIfNeeded(result, intent, jobId)
        } else {
            withContext(stateThread) {
                val oldStateValue = _state.value
                _state.update { intent.reducer(result, oldStateValue) }
                onNewState(_state.value)
            }
            intent.sideEffect?.invoke(result, _state.value)
        }
    }

    private fun <Result : Any> runCancellationAndSideEffectIfNeeded(
        result: Result?,
        intent: Intent3<State, Result>,
        jobId: String
    ) {
        cancel(jobId)
        if (intent.runSideEffectAfterCancel) {
            intentByBuilderInternal<Unit> {
                sideEffect {
                    intent.sideEffect?.invoke(result, state)
                }
            }
        }
    }

    protected fun cancel(vararg ids: String) {
        ids.forEach { jobs[it]?.cancelJob() }
    }

    private fun IntentJob<State, out Any>.cancelJob() {
        job.cancel()
    }

    companion object {
        var stateThread: CoroutineDispatcher = newSingleThreadContext("mvi")
    }
}

private data class IntentJob<State : Any, Result : Any>(
    val identifier: Long,
    val intent: Intent3<State, Result>,
    val job: Job
)

data class Intent3<State, Result : Any>(
    val onTrigger: suspend (state: State) -> Flow<Result>? = { _ -> null },
    val cancelTrigger: (suspend (result: Result?, state: State) -> Boolean)? = null,
    val reducer: (result: Result?, state: State) -> State = { _, state -> state },
    val sideEffect: (suspend (result: Result?, state: State) -> Unit)? = null,
    val runSideEffectAfterCancel: Boolean = false
)

@Suppress("UNCHECKED_CAST")
class IntentBuilder<State : Any, Result : Any>(
    private var onTrigger: suspend (state: State) -> Flow<Result>? = { _ -> null },
    private var cancelTrigger: (suspend (result: Result?, state: State) -> Boolean)? = null,
    private var reducer: (result: Result?, state: State) -> State = { _, state -> state },
    private var sideEffect: (suspend (result: Result?, state: State) -> Unit)? = null,
    private var runSideEffectAfterCancel: Boolean = false
) {

    fun onTrigger(
        func: suspend IntentContext<State, Result>.() -> Flow<Result>? = { null }
    ): IntentBuilder<State, Result> {
        onTrigger = { state ->
            IntentContext<State, Result>(state, null).func()
        }
        return this
    }

    fun cancelTrigger(
        runSideEffectAfterCancel: Boolean = false,
        func: suspend IntentContext<State, Result>.() -> Boolean = { false }
    ): IntentBuilder<State, Result> {
        this.runSideEffectAfterCancel = runSideEffectAfterCancel
        cancelTrigger = { result: Result?, state ->
            IntentContext(state, result).func()
        }
        return this
    }

    fun reducer(func: IntentContext<State, Result>.() -> State): IntentBuilder<State, Result> {
        reducer = { result: Result?, state ->
            IntentContext(state, result).func()
        }
        return this
    }

    fun sideEffect(func: suspend IntentContext<State, Result>.() -> Unit): IntentBuilder<State, Result> {
        sideEffect = { result: Result?, state ->
            IntentContext(state, result).func()
        }
        return this
    }

    fun build(): Intent3<State, Result> = Intent3(
        onTrigger = onTrigger,
        cancelTrigger = cancelTrigger,
        reducer = reducer,
        sideEffect = sideEffect,
        runSideEffectAfterCancel = runSideEffectAfterCancel
    )

    companion object {
        fun <State : Any, Result : Any> build(buildFun: IntentBuilder<State, Result>.() -> Unit): Intent3<State, Result> =
            IntentBuilder<State, Result>().apply(buildFun).build()
    }
}

fun <State : Any, Result : Any> intent(buildFun: IntentBuilder<State, Result>.() -> Unit): Intent3<State, Result> =
    IntentBuilder.build(buildFun)

data class IntentContext<State : Any, Result>(
    val state: State,
    val result: Result?
) {
    fun resultNonNull(): Result = result!!
}

fun <State : Any, Result : Any> Intent3<State, Result>.rebuild(
    buildFun: IntentBuilder<State, Result>.(Intent3<State, Result>) -> Unit
) = IntentBuilder(
    onTrigger = onTrigger,
    cancelTrigger = cancelTrigger,
    reducer = reducer,
    sideEffect = sideEffect,
    runSideEffectAfterCancel = runSideEffectAfterCancel
).apply { buildFun(this@rebuild) }.build()

@Composable
fun <T : Any> Store4<T>.collectState(
    context: CoroutineContext = EmptyCoroutineContext,
    onStoreInitAction: suspend () -> Unit = { }
): androidx.compose.runtime.State<T> {

    val state = state.collectAsState(state.value, context)
    LaunchedEffect(key1 = identifier) {
        init {
            onStoreInitAction()
        }
    }
    return state
}

fun <OutState : Any, InState : Any, Result : Any> Intent3<InState, Result>.map(
    stateReducer: IntentContext<OutState, Result>.(newInState: InState) -> OutState,
    stateMapper: (OutState) -> InState?,
    setUp: IntentBuilder<OutState, Result>.(innerIntent: Intent3<InState, Result>) -> Unit = { }
): Intent3<OutState, Result> =
    let { inner ->
        intent {
            onTrigger { stateMapper(state)?.let { inner.onTrigger(it) } }

            cancelTrigger(inner.runSideEffectAfterCancel) {
                inner.cancelTrigger?.let { cancelTrigger ->
                    stateMapper(state)?.let { cancelTrigger(result, it) } ?: false
                } ?: false
            }

            reducer {
                stateMapper(state)?.let { newInState ->
                    stateReducer(inner.reducer(result, newInState))
                } ?: state

            }

            sideEffect {
                inner.sideEffect?.let { sideEffect ->
                    stateMapper(state)?.let { sideEffect(result, it) }
                }
            }

            setUp(inner)
        }
    }

fun <OutState : Any, InState : Any, Result : Any> Intent3<InState, Result>.mapInnerReducer(
    stateReducer: IntentContext<OutState, Result>.((result: Result?, state: InState) -> InState) -> OutState,
    stateMapper: (OutState) -> InState?,
    setUp: IntentBuilder<OutState, Result>.(innerIntent: Intent3<InState, Result>) -> Unit = { }
): Intent3<OutState, Result> =
    let { inner ->
        intent {
            onTrigger { stateMapper(state)?.let { inner.onTrigger(it) } }

            cancelTrigger(inner.runSideEffectAfterCancel) {
                inner.cancelTrigger?.let { cancelTrigger ->
                    stateMapper(state)?.let { cancelTrigger(result, it) } ?: false
                } ?: false
            }

            reducer {
                stateReducer(inner.reducer)
            }

            sideEffect {
                inner.sideEffect?.let { sideEffect ->
                    stateMapper(state)?.let { sideEffect(result, it) }
                }
            }

            setUp(inner)
        }
    }

fun <State : Any, Result : Any> Intent3<State, Result>.composite(
    setUp: IntentBuilder<State, Result>.(innerIntent: Intent3<State, Result>) -> Unit = { }
): Intent3<State, Result> =
    map(stateReducer = { it }, stateMapper = { it }, setUp = setUp)
