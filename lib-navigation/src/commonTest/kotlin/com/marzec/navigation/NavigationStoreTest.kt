package com.marzec.navigation

import com.marzec.core.StoreTest
import com.marzec.core.runStoreTest
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class NavigationStoreTest {

    val requestId = 1

    val stateWithCurrentFlow = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "2"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "3",
                            requestKey = RequestKey(
                                requesterKey = "1",
                                requestId = requestId
                            ),
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "4",
                                        requestKey = RequestKey(
                                            requesterKey = "1",
                                            requestId = requestId
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    val stateWithSubFlow = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "2",
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "3"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            NavigationEntry(destination = TestDestination.A, "4"),
        )
    )

    val stateWithSubFlowsAndEntryIds = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                id = "entry",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "2",
                            id = "entry2",
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        id = "entry3",
                                        destination = TestDestination.B,
                                        cacheKey = "3"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            NavigationEntry(
                destination = TestDestination.A,
                cacheKey = "4",
                id = "entry4",
            ),
        )
    )

    var keyProviderIncrement = 0

    val stateCache: NavigationStateCache = mockk(relaxed = true)
    val resultCache: ResultCache = mockk(relaxed = true)
    val cacheKey: String = "navigation_cache_key"
    val cacheKeyProvider: () -> String = { keyProviderIncrement++.toString() }
    val defaultState: NavigationState = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, cacheKeyProvider())
        )
    )
    val overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null

    @Before
    fun setUp() {
        coEvery { stateCache.get<NavigationState>(any()) } returns null
    }

    @Test
    fun initialization() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                )
            )

            coVerify {
                stateCache.get<NavigationState>("navigation_cache_key")
                resultCache wasNot called
            }
        }
    }


    @Test
    fun next_Destination() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(TestDestination.B)

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1")
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }

        }
    }

    @Test
    fun next_Destination_WithRequest() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)
        val requestId = 1

        store.test {

            next(
                NavigationAction(destination = TestDestination.B),
                requestId = requestId
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = 1
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_Destination_WithOptions() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)
        val requestId = 1

        store.test {

            nextWithOptionRequest(
                action = NavigationAction(destination = TestDestination.B),
                requestId = requestId,
                options = mapOf("Options" to 1)
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = 1,
                                options = mapOf("Options" to 1)
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_Destination_WithRequestAndSecondaryId() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)
        val requestId = 1
        val secondaryId = 101

        store.test {

            next(
                NavigationAction(destination = TestDestination.B),
                requestId = requestId,
                secondaryId = secondaryId
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = requestId,
                                options = mapOf(SECONDARY_ID to secondaryId)
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_PopToAInclusive_andPutNewAScreen() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(destination = TestDestination.B, "1"),
                NavigationEntry(destination = TestDestination.B, "2")
            )
        )
        keyProviderIncrement = initialState.backStack.size
        store = navigationStore(initialState = initialState, overrideLastClose = overrideLastClose)


        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.A,
                    options = NavigationOptions(
                        PopEntryTarget.ToDestination(
                            popTo = TestDestination.A,
                            popToInclusive = true
                        )
                    )
                )
            )

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "3")
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                stateCache.remove("2")
                resultCache.remove("2")
                stateCache.remove("1")
                resultCache.remove("1")
                stateCache.remove("0")
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_PopToAExclusive_andPutNewAScreen() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(destination = TestDestination.B, "1"),
                NavigationEntry(destination = TestDestination.B, "2")
            )
        )
        keyProviderIncrement = initialState.backStack.size
        store = navigationStore(initialState = initialState)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.B,
                    options = NavigationOptions(
                        PopEntryTarget.ToDestination(
                            popTo = TestDestination.A,
                            popToInclusive = false
                        )
                    )
                )
            )

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "3")
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                stateCache.remove("2")
                resultCache.remove("2")
                stateCache.remove("1")
                resultCache.remove("1")
            }
        }
    }

    @Test
    fun goBack() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(destination = TestDestination.B, "1"),
                NavigationEntry(destination = TestDestination.B, "2")
            )
        )
        keyProviderIncrement = initialState.backStack.size
        store = navigationStore(initialState = initialState)

        store.test {

            goBack()

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1"),
                    )
                )
            )

            coVerify {
                resultCache.remove("2")
                stateCache.remove("2")
            }
            coVerify(exactly = 0) { resultCache.save(any(), any()) }
        }
    }

    @Test
    fun goBack_withOverrideClosingLast() = runTest {
        var calledLastClose = false
        val overrideLastClose: NavigationState.() -> NavigationUpdate =
            { calledLastClose = true; NavigationUpdate(this, emptyList()) }
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0")
            )
        )

        store = navigationStore(initialState, overrideLastClose)

        store.test {

            goBack()

            values.isEqualTo(initialState)

            assertTrue(calledLastClose)
        }
    }

    @Test
    fun goBack_WithResult() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(
                    destination = TestDestination.B, "1",
                    requestKey = RequestKey(
                        requesterKey = "0",
                        requestId = 1
                    )
                )
            )
        )
        keyProviderIncrement = initialState.backStack.size
        store = navigationStore(initialState = initialState, overrideLastClose = overrideLastClose)

        store.test {

            goBack("result")

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                    )
                )
            )

            coVerify {
                resultCache.save(
                    requestKey = RequestKey(
                        requesterKey = "0",
                        requestId = 1
                    ),
                    value = "result"
                )
                resultCache.remove("1")
                stateCache.remove("1")
            }
        }
    }

    @Test
    fun next_OpenNewFlow() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(SubFlow(TestDestination.B, "subflow"))

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                backStack = listOf(NavigationEntry(TestDestination.B, "2")),
                                id = "subflow"
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }

        }
    }

    @Test
    fun next_OpenNewFlow_withRequestKey() = runTest {
        val initialState: NavigationState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0")
            )
        )
        val requestId = 1
        keyProviderIncrement = 1

        store = navigationStore(initialState)

        store.test {

            next(
                action = NavigationAction(SubFlow(TestDestination.B, "subflow")),
                requestId = requestId
            )

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "2",
                                        requestKey = RequestKey(
                                            requestId = requestId,
                                            requesterKey = "0"
                                        )
                                    )
                                ),
                                id = "subflow"
                            ),
                            requestKey = RequestKey(
                                requestId = requestId,
                                requesterKey = "0"
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }

        }
    }

    @Test
    fun nextWithOptions_OpenNewFlow_withRequestKey() = runTest {
        val initialState: NavigationState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0")
            )
        )
        val requestId = 1
        keyProviderIncrement = 1

        store = navigationStore(initialState)

        store.test {

            nextWithOptionRequest(
                action = NavigationAction(SubFlow(TestDestination.B, "subflow")),
                requestId = requestId,
                options = mapOf("option_key" to "option_value")
            )

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "2",
                                        requestKey = RequestKey(
                                            requestId = requestId,
                                            requesterKey = "0",
                                            options = mapOf("option_key" to "option_value")
                                        )
                                    )
                                ),
                                id = "subflow"
                            ),
                            requestKey = RequestKey(
                                requestId = requestId,
                                requesterKey = "0",
                                options = mapOf("option_key" to "option_value")
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())
                resultCache.remove("0")
            }

        }
    }

    @Test
    fun next_OpenNewFlow_withInclusivePopping() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlow, overrideLastClose)

        store.test {

            next(
                NavigationAction(
                    destination = SubFlow(TestDestination.C, "subflow3"),
                    options = NavigationOptions(
                        PopEntryTarget.ToDestination(
                            popTo = SubFlow(TestDestination.B, "subflow"),
                            popToInclusive = true
                        )
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.C, "subflow3"),
                            cacheKey = "5",
                            subFlow = NavigationFlow(
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.C,
                                        cacheKey = "6"
                                    )
                                ),
                                id = "subflow3"
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")

                resultCache.remove("2")
                stateCache.remove("2")

                resultCache.remove("1")
                stateCache.remove("1")
            }
        }
    }

    @Test
    fun next_OpenNewFlow_withExclusivePopping() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlow, overrideLastClose)

        store.test {

            next(
                NavigationAction(
                    destination = SubFlow(TestDestination.C, "subflow3"),
                    options = NavigationOptions(
                        PopEntryTarget.ToDestination(
                            popTo = SubFlow(TestDestination.B, "subflow"),
                            popToInclusive = false
                        )
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = SubFlow(TestDestination.C, "subflow3"),
                                        cacheKey = "5",
                                        subFlow = NavigationFlow(
                                            backStack = listOf(
                                                NavigationEntry(
                                                    destination = TestDestination.C,
                                                    cacheKey = "6"
                                                )
                                            ),
                                            id = "subflow3"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")

                resultCache.remove("2")
                stateCache.remove("2")
            }
        }
    }

    @Test
    fun goBack_WithSubFlow() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(
                    destination = SubFlow(TestDestination.B, "subflow"),
                    cacheKey = "1",
                    subFlow = NavigationFlow(
                        id = "subflow",
                        backStack = listOf(
                            NavigationEntry(
                                destination = SubFlow(TestDestination.C, "subflow2"),
                                cacheKey = "2",
                                subFlow = NavigationFlow(
                                    id = "subflow2",
                                    backStack = listOf(
                                        NavigationEntry(
                                            destination = TestDestination.A,
                                            cacheKey = "3"
                                        ),
                                        NavigationEntry(
                                            destination = TestDestination.B,
                                            cacheKey = "4"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        store = navigationStore(initialState)

        store.test {

            goBack()

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = SubFlow(TestDestination.C, "subflow2"),
                                        cacheKey = "2",
                                        subFlow = NavigationFlow(
                                            id = "subflow2",
                                            backStack = listOf(
                                                NavigationEntry(
                                                    destination = TestDestination.A,
                                                    cacheKey = "3"
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.remove("4")
                stateCache.remove("4")
            }
        }
    }

    @Test
    fun goBack_WithSubFlow_CloseFlowIfEmpty() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(
                    destination = SubFlow(TestDestination.B, "subflow"),
                    cacheKey = "1",
                    subFlow = NavigationFlow(
                        id = "subflow",
                        backStack = listOf(
                            NavigationEntry(
                                destination = SubFlow(TestDestination.C, "subflow2"),
                                cacheKey = "2",
                                subFlow = NavigationFlow(
                                    id = "subflow2",
                                    backStack = listOf(
                                        NavigationEntry(
                                            destination = TestDestination.A,
                                            cacheKey = "3"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        keyProviderIncrement = 5
        store = navigationStore(initialState)

        store.test {

            goBack()

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.remove("3")
                stateCache.remove("3")

                resultCache.remove("2")
                stateCache.remove("2")

                resultCache.remove("1")
                stateCache.remove("1")
            }
        }
    }

    @Test
    fun goBack_withOverrideClosingLast_withFlows_shouldNot_subFlowHas2Screens() = runTest {
        var calledLastClose = false
        val overrideLastClose: NavigationState.() -> NavigationUpdate =
            { calledLastClose = true; NavigationUpdate(this, emptyList()) }
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(
                    destination = TestDestination.A, "0",
                    subFlow = NavigationFlow(
                        id = "subflow2",
                        backStack = listOf(
                            NavigationEntry(
                                destination = TestDestination.A,
                                cacheKey = "3"
                            ),
                            NavigationEntry(
                                destination = TestDestination.C,
                                cacheKey = "4"
                            )
                        )
                    )

                )
            )
        )

        store = navigationStore(initialState, overrideLastClose)

        store.test {

            goBack()

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(
                            destination = TestDestination.A, "0",
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.A,
                                        cacheKey = "3"
                                    )
                                )
                            )

                        )
                    )
                )
            )

            assertFalse(calledLastClose)
        }
    }

    @Test
    fun goBack_withResult_closeLastEntryInSubFlow() = runTest {
        val result = "result"
        val requestId = 1
        val requestKey = RequestKey(
            requestId = requestId,
            requesterKey = "0"
        )
        val initialState: NavigationState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(
                    destination = SubFlow(TestDestination.B, "subflow"),
                    cacheKey = "1",
                    subFlow = NavigationFlow(
                        backStack = listOf(
                            NavigationEntry(
                                destination = TestDestination.B,
                                cacheKey = "2",
                                requestKey = requestKey
                            )
                        ),
                        id = "subflow"
                    ),
                    requestKey = requestKey
                )
            )
        )

        store = navigationStore(initialState)

        store.test {

            goBack(result)

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                )
            )

            coVerify {
                resultCache.save(requestKey, result)
            }

        }
    }

    @Test
    fun next_OpenNewFlow_withExclusivePoppingToFlowId() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlow)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.C,
                    options = NavigationOptions(
                        popTo = PopEntryTarget.ToFlow("subflow", popToInclusive = false),
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.C, "5")
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun next_OpenNewFlow_withExclusivePoppingToFlowId_doNotPopScreensOfTargetSubFlow() = runTest {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(destination = TestDestination.A, "0"),
                NavigationEntry(
                    destination = SubFlow(TestDestination.B, "subflow"),
                    cacheKey = "1",
                    subFlow = NavigationFlow(
                        id = "subflow",
                        backStack = listOf(
                            NavigationEntry(destination = TestDestination.A, "2"),
                            NavigationEntry(
                                destination = SubFlow(TestDestination.B, "subflow2"),
                                cacheKey = "3",
                                subFlow = NavigationFlow(
                                    id = "subflow2",
                                    backStack = listOf(
                                        NavigationEntry(
                                            destination = TestDestination.B,
                                            cacheKey = "4"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        keyProviderIncrement = 5
        store = navigationStore(initialState)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.C,
                    options = NavigationOptions(
                        popTo = PopEntryTarget.ToFlow("subflow", popToInclusive = false),
                    )
                )
            )

            values.isEqualTo(
                initialState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.A, "2"),
                                    NavigationEntry(destination = TestDestination.C, "5")
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun next_OpenNewFlow_withInclusivePoppingToFlowId() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlow)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.C,
                    options = NavigationOptions(
                        popTo = PopEntryTarget.ToFlow("subflow", popToInclusive = true),
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.C, "5")
                    )
                )
            )
        }
    }

    @Test
    fun next_OpenNewFlow_withExclusivePoppingToId() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlowsAndEntryIds)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.C,
                    options = NavigationOptions(
                        popTo = PopEntryTarget.ToId("entry", popToInclusive = false),
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlowsAndEntryIds,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            id = "entry",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.C,
                                        cacheKey = "5"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun next_OpenNewFlow_withInclusivePoppingToId() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithSubFlowsAndEntryIds)

        store.test {

            next(
                NavigationAction(
                    destination = TestDestination.C,
                    options = NavigationOptions(
                        popTo = PopEntryTarget.ToId("entry", popToInclusive = true),
                    )
                )
            )

            values.isEqualTo(
                stateWithSubFlowsAndEntryIds,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.C,
                            cacheKey = "5"
                        )
                    )
                )
            )
        }
    }

    @Test
    fun closeFlow() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithCurrentFlow)

        store.test {

            closeFlow()

            values.isEqualTo(
                stateWithCurrentFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.A, "2")
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")
            }
        }
    }

    @Test
    fun closeFlow_withResult() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithCurrentFlow)

        store.test {

            closeFlow("result")

            values.isEqualTo(
                stateWithCurrentFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.A, "2")
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.set("navigation_cache_key", any())

                resultCache.save(RequestKey(
                    requesterKey = "1",
                    requestId = requestId
                ), "result")

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")
            }
        }
    }

    @Test
    fun closeFlow_overrideIfRootFlow() {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(
                    TestDestination.A,
                    "0"
                )
            )
        )
        var calledLastClose = false
        val overrideLastClose: NavigationState.() -> NavigationUpdate =
            {
                calledLastClose = true
                NavigationUpdate(this, emptyList())
            }
        runTest {
        store = navigationStore(initialState, overrideLastClose)

        store.test {

            closeFlow()

            values.isEqualTo(initialState)

            assertTrue(calledLastClose)
        }
    }
}

    private fun StoreTest<NavigationState, NavigationStore>.navigationStore(
        initialState: NavigationState = defaultState,
        overrideLastClose: (NavigationState.() -> NavigationUpdate)? = this@NavigationStoreTest.overrideLastClose
    ) =
        NavigationStore(
            scope = scope,
            stateCache = stateCache,
            resultCache = resultCache,
            cacheKey = cacheKey,
            cacheKeyProvider = cacheKeyProvider,
            initialState = initialState,
            overrideLastClose = overrideLastClose,
        )

    private fun runTest(block: suspend StoreTest<NavigationState, NavigationStore>.() -> Unit) {
        runStoreTest(block)
    }
}

sealed class TestDestination : Destination {

    object A : Destination

    object B : Destination

    object C : Destination
}