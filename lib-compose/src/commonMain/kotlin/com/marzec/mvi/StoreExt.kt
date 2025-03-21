
package com.marzec.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlin.coroutines.CoroutineContext

@Composable
fun <T : Any> Store4<T>.collectState(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
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
