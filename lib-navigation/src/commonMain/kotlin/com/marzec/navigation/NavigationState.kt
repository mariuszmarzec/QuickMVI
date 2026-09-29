package com.marzec.navigation

typealias NavigationState = NavigationFlow

data class NavigationFlow(
    val backStack: List<NavigationEntry>,
    val id: String
) {
    companion object {
        const val ROOT_FLOW = "ROOT_FLOW"
    }
}

fun navigationState(
    backStack: List<NavigationEntry>,
    id: String = NavigationFlow.ROOT_FLOW
) = NavigationFlow(backStack, id)