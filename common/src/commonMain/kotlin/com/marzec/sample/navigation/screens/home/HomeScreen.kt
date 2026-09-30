package com.marzec.sample.navigation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marzec.mvi.collectState

@Composable
fun HomeScreen(store: HomeStore) {

    val state = store.collectState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button({
            store.startOtherGraph()
        }) {
            Text("Start other navigation graph")
        }
    }
}
