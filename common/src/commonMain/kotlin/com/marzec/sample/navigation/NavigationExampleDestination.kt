package com.marzec.sample.navigation

import com.marzec.navigation.Destination

internal sealed class NavigationExampleDestination : Destination {
    data object HomeScreen : NavigationExampleDestination()

    data object A : NavigationExampleDestination()

    data object B : NavigationExampleDestination()
}
