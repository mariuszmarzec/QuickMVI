package com.marzec.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

typealias ScreenProvider = @Composable (destination: Destination, cacheKey: String) -> Unit

typealias Router = (Destination) -> ScreenProvider

/**
 * Creates a [Router] backed by a map of [Destination] classes to screen providers.
 *
 * Routing is keyed by the destination class ([KClass]) instead of destination
 * instances, so destinations carrying parameters (e.g. `TaskDetails(taskId)`)
 * still resolve to the same screen provider. The provider receives the concrete
 * [Destination] instance and can extract its parameters.
 *
 * An unknown destination fails fast with an error, same as the previous
 * exhaustive `when` based router.
 */
fun createRouter(
    vararg routes: Pair<KClass<out Destination>, ScreenProvider>
): Router {
    val routesByDestinationClass: Map<KClass<out Destination>, ScreenProvider> = routes.toMap()
    return { destination ->
        routesByDestinationClass[destination::class]
            ?: throw error("Unknown destination: $destination")
    }
}
