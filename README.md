# QuickMVI

Small piece of code store class with necessary builders 
to provide some architecture pattern to manage better 
state changes for kotlin multiplatform and compose 
based projects or android 
[(check ViewModel wrapper in cheat day app](https://github.com/mariuszmarzec/cheatDay).

## Quick start

I'm not going to publish it anywhere now, if you want to use it, 
copy to your project, just [mvi.kt](common/src/commonMain/kotlin/com.marzec.mvi/mvi.kt) file.

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

For more use cases check:
 - Timers' code [App.kt](common/src/commonMain/kotlin/com.marzec.common/App.kt)
 - Test cases code [Store3Test.kt](common/src/commonTest/kotlin/com/marzec/mvi/Store3Test.kt)