package com.marzec.sample.navigation.screens.b

import com.marzec.mvi.Store4Impl
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import com.marzec.navigation.navigationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class BStore(
    private val navigationStore: NavigationStore,
    private val message: MutableStateFlow<String>,
    scope: CoroutineScope
) : Store4Impl<String>(scope, message.value) {

    fun load() = intent {
        onTrigger {
            message
        }

        reducer {
            resultNonNull()
        }
    }

    fun onMessageChange(newValue: String) = intent<String> {
        onTrigger {
            flow {
                message.value = newValue
                emit(newValue)
            }
        }

        reducer {
            resultNonNull()
        }
    }

    fun onLeaveSubFlowClick() = sideEffectIntent {
        navigationStore.closeFlow()
    }
}