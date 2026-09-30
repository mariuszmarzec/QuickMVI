package com.marzec.sample.navigation.screens.b

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.common.TextFieldStateful
import com.marzec.mvi.collectState

@Composable
fun ScreenB(store: BStore) {

    val state = store.collectState {
        store.load()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Gray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TextFieldStateful(value = state.value, onValueChange = { store.onMessageChange(it) })

        Spacer(Modifier.height(32.dp))

        Button({ store.onLeaveSubFlowClick() }) {
            Text("Leave sub flow")
        }
    }
}