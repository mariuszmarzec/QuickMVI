package com.marzec.sample.navigation.screens.a

import com.marzec.sample.navigation.NavigationExampleDestination
import com.marzec.mvi.Store4Impl
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AStore(
    private val navigationStore: NavigationStore,
    private val message: StateFlow<String>,
    scope: CoroutineScope
) : Store4Impl<String>(scope, message.value) {

    fun load() = intent<String> {
        onTrigger { message }

        reducer { resultNonNull() }
    }

    fun onGoToBClick() = sideEffectIntent {
        navigationStore.next(NavigationExampleDestination.B)
    }
}