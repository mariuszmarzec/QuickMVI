# QuickMVI

QuickMVI is a small state-management library built around stores and intent builders. It helps structure state changes in Kotlin Multiplatform, Jetpack Compose, and Android applications. See the [ViewModel wrapper in the cheatDay app](https://github.com/mariuszmarzec/cheatDay).

## Quick start

Add dependency in gradle:

```kotlin
repositories {
   mavenCentral()
}
//   ...
dependencies {
    // ...
   implementation("io.github.mariuszmarzec:quickmvi:1.3.0")
   // compose utils
   implementation("io.github.mariuszmarzec:quickmvi-compose:1.3.0")
   // navigation utils
   implementation("io.github.mariuszmarzec:quickmvi-navigation:1.3.0")
   // ...
}
```

1. Create a store by passing its initial state and a `CoroutineScope`. Choose a scope whose lifetime matches the store.
2. Declare an intent. Its `reducer` calculates the next state from the current state and the emitted result.
3. Use `sideEffect` for work that should run after each processed result, such as logging or navigation.
```kotlin
class TickerCounterStore(scope: CoroutineScope) : Store4Impl<Int>(scope, 0) {

    fun bumpCounter() = intent<Int> {

        reducer { state.inc() }

        sideEffect { println("Bumped to $state") }
    }
}
```
4. Create a Composable that receives the store.
5. Collect state with the `collectState` extension; you can provide an optional initial action.
6. Render the UI from the collected state.
```kotlin
@Composable
fun TickerCounter(store: TickerCounterStore) {

    val state: Int by store.collectState {
        // initial action
    }

    Button(
        modifier = Modifier.padding(16.dp),
        onClick = { store.bumpCounter() }) {
        Text("$state")
    }
}
```

## Artifacts

- `quickmvi` — core store and intent APIs.
- `quickmvi-compose` — Compose state collection helpers, including `collectState`.
- `quickmvi-navigation` — store-based navigation, including `NavigationStore` and `NavigationHost`.

Add only the artifacts your app uses.

## Intent action
In `onTrigger` method you can define action, which will be executed after calling your intent method.
It should always return flow. 

Method `reducer` as mentioned above contains mutations of your state, called every
time when `onTrigger` emit any value, if `onTrigger` is not set, `reducer` and `sideEffect` action will be called only 
once. 

In below example, action declared in `sideEffect` is called every time
when `onTrigger` flow emit value. Result of calling below code, will make state change
from current to 3 in every second until reaching 3. When 2 and 3 values
will be emitted, `sideEffect` action will print value in console.

```kotlin
    fun runAction() = intent<Int>(id = "id") {
        onTrigger {
            flow {
                delay(1000)
                emit(1)
                delay(1000)
                emit(2)
                delay(1000)
                emit(3)
            }
        }
    
        reducer { state = resultNonNull() }

        sideEffect { 
            if (result == 2 || result == 3) {
                println("Value: $state")
            }
        }
    }
```

`id` is optional. When an ID is provided, the store tracks the running intent under that ID. Starting another intent with the same ID cancels the previous one if it is still running. Without an ID, each invocation gets a generated ID and does not replace other intents.

The store can also cancel intents explicitly by ID with `cancel(vararg ids: String)`, as shown below:

```kotlin
    fun cancel() = sideEffect {
        cancel(
            INTENT_SLOW_TIMER_ID,
            INTENT_MEDIUM_TIMER_ID,
            INTENT_QUICK_TIMER_ID
        )
    }
```

### Conditional cancellation

Use `cancelTrigger` when an emitted result should stop the intent before the reducer processes that result. The predicate can inspect both the emitted result and the current state. By default, the side effect is skipped for the result that triggered cancellation; set `runSideEffectAfterCancel = true` to run it anyway.

```kotlin
fun runUntilThree() = intent<Int>(id = "counter") {
    onTrigger {
        flowOf(1, 2, 3, 4)
    }

    cancelTrigger(runSideEffectAfterCancel = true) {
        resultNonNull() == 3
    }

    reducer {
        resultNonNull()
    }

    sideEffect {
        println("Received $result; state is $state")
    }
}
```

In this example, the intent processes `1` and `2`, then cancels when `3` is emitted. Because cancellation is checked before the reducer, the state remains `2`; the side effect still runs for result `3` because `runSideEffectAfterCancel` is enabled. Result `4` is never processed.

## Store Delegate

Logic inside intent could be extracted into separate classes and reuse in more than one store.

1. Declare common interface for delegated methods:
    ```kotlin
   interface TestDelegate {

        fun doThings()
    }
    ```
2. Implement new interface and open class StoreDelegate:
   ```kotlin
    class TestDelegateImpl : StoreDelegate<Int>(), TestDelegate {
    
        override fun doThings() = intent<Int> {
            onTrigger { flowOf(1, 2, 3) }
    
            reducer { state * 10 + resultNonNull() }
        }
    }
    ```
3. Make your store implement your interface and implement methods by delegation in your store. Using delegate extension
install delegate in store

   ```kotlin
    class TestStore(
        scope: CoroutineScope,
        defaultState: Int,
        testDelegate: TestDelegate
    ) : Store4Impl<Int>(scope, defaultState), TestDelegate by testDelegate {

        init {
            delegates(testDelegate)
        }
    }
   
    // ...
     val store = TestStore(scope, 0, TestDelegateImpl())

     store.doThings()
   ```

## Parent-Child Store Relation

You can create a child store derived from a parent store. This allows for state hoisting and synchronized state management.
The child store is linked to the parent store via two mappers.

```kotlin
data class ParentState(val count: Int, val name: String)
data class ChildState(val count: Int)

val parentStore = Store(scope, ParentState(0, "Parent"))

val childStore = parentStore.getChildStore(
    scope = childScope,
    initialState = ChildState(0),
    parentToChild = { parent -> ChildState(parent.count) },
    childToParent = { child -> copy(count = child.count) } // 'this' is ParentState
)
```
   
## QuickMVI - Navigation

The `quickmvi-navigation` library provides navigation utilities for QuickMVI, including a store-based navigation state management solution with support for nested flows, result passing, and back-stack manipulation.

### Creating a navigation store

```kotlin
val navigationStore = navigationStore(
    scope = scope,
    stateCache = stateCache,
    cacheKeyProvider = { "root" },
    navigationStoreCacheKey = "navigationStore",
    defaultDestination = HomeScreen,
    resultCache = resultCache
)
```

### Navigating

```kotlin
navigationStore.next(Destination.Details)
navigationStore.navigateBack(result)
```

### Rendering screens

```kotlin
@Composable
fun App(navigationStore: NavigationStore) {
    NavigationHost(
        navigationStore = navigationStore,
        router = { destination ->
            when (destination) {
                is HomeScreen -> { { _, _ -> HomeScreen() } }
                is DetailsScreen -> { { _, _ -> DetailsScreen() } }
            }
        }
    )
}
```

## QuickMVI - Compose
### Collecting state

In quickmvi-compose package there is an extension for easy collecting state in compose with initial action could 
be at screen creation time.
```kotlin
    val state: State<TasksScreenState> by store.collectState {
        store.loadList()
        store.onScheduleSelectedRequest()
    }
```

For more use cases check:
 - Timers' code [App.kt](common/src/commonMain/kotlin/com/marzec/common/App.kt)
 - Test cases code [Store3Test.kt](common/src/commonTest/kotlin/com/marzec/mvi/Store3Test.kt)
 - Test cases code [StoreDelegateTest.kt](common/src/commonTest/kotlin/com/marzec/mvi/StoreDelegateTest.kt)

# License

    Copyright 2025 Mariusz Marzec

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
