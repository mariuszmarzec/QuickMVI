package com.marzec.navigation

data class NavigationAction(
    val destination: Destination,
    val options: NavigationOptions? = null
)

data class NavigationOptions(
    val popTo: PopEntryTarget
)

interface Destination

data class SubFlow(val startDestination: Destination, val id: String) : Destination

sealed class PopEntryTarget(
    open val popToInclusive: Boolean
) {

    data class ScreenCount(val count: Int) : PopEntryTarget(popToInclusive = false)

    data class ToDestination(
        val popTo: Destination,
        override val popToInclusive: Boolean
    ) : PopEntryTarget(popToInclusive = popToInclusive)

    data class ToFlow(
        val id: String,
        override val popToInclusive: Boolean
    ) : PopEntryTarget(popToInclusive = popToInclusive)

    data class ToId(
        val id: String,
        override val popToInclusive: Boolean
    ) : PopEntryTarget(popToInclusive = popToInclusive)
}