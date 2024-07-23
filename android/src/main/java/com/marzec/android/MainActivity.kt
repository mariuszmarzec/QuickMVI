package com.marzec.android

import com.marzec.common.App
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marzec.common.TickerCounterStore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tickerViewModel: TickerViewModel by viewModels()

        setContent {
            MaterialTheme {
                App(tickerCounter = tickerViewModel.store)
            }
        }
    }
}

class TickerViewModel(private val state: SavedStateHandle) : ViewModel() {

    val store = TickerCounterStore(viewModelScope, state["counter"] ?: 0) { newState ->
        state["counter"] = newState
        println("NEW STATE: $newState")
    }
}