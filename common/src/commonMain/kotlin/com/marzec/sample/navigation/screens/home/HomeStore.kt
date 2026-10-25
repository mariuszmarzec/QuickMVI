package com.marzec.sample.navigation.screens.home

import com.marzec.sample.navigation.NavigationExampleDestination
import com.marzec.mvi.Store4Impl
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.SubFlow
import com.marzec.navigation.next
import kotlinx.coroutines.CoroutineScope

class HomeStore(
    private val scope: CoroutineScope,
    private val navigationStore: NavigationStore
) : Store4Impl<Unit>(scope, Unit) {

    fun startOtherGraph() = sideEffectIntent {
        navigationStore.next(SubFlow(NavigationExampleDestination.A, "subflow"))
    }
}
