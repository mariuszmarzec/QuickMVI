# QuickMVI

Small piece of code store class with necessary builders 
to provide some architecture pattern to manage better 
state changes for kotlin multiplatform and compose 
based projects or android 
[(check ViewModel wrapper in cheat day app](https://github.com/mariuszmarzec/cheatDay).

## Quick start

Add dependency in gradle:

```kotlin
repositories {
   mavenCentral()
}
//   ...
dependencies {
    // ...
   implementation("io.github.mariuszmarzec:quickmvi:1.1.0")
   // compose utils
   implementation("io.github.mariuszmarzec:quickmvi-compose:1.1.0")
   // ...
}
```

1. Create store, you, pass default state as argument and scope.
2. Declare intent action as below, in reducer mutate your current state to get new one and update UI.
3. In sideEffect action you can declare action which will be fired on every triggered update.
```kotlin
class TickerCounterStore(scope: CoroutineScope) : Store3<Int>(scope, 0) {

    fun bumpCounter() = intent<Int> {

        reducer { state.inc() }

        sideEffect { println("Bumped to $state") }
    }
}
```
4. Create your Composable method which takes store as argument
5. Collect state with `collectState` extension, you can pass optional initial action
6. Build your UI basing on state.
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

Parameter `id` is empty by default. If it is not empty, triggered action will be 
remembered and if next action with same `id` will be fired and previous one is still 
pending, it will be killed.

You can also cancel actions with method `fun cancel(vararg ids: String)` as below:

```kotlin
    fun cancel() = sideEffect {
        cancel(
            INTENT_SLOW_TIMER_ID,
            INTENT_MEDIUM_TIMER_ID,
            INTENT_QUICK_TIMER_ID
        )
    }
```

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

