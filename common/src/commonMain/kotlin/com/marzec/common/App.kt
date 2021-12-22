package com.marzec.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun App() {

    ProgressRun(
        progress = 0.6f,
        onStartButtonClick = { }
    )
}

@Composable
fun ProgressRun(
    progress: Float,
    onStartButtonClick: () -> Unit
) {
    Column {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                onStartButtonClick.invoke()
            }) {
                Text("Start")
            }
            Spacer(Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = progress,
                color = Color.LightGray,
                backgroundColor = Color.DarkGray,
                modifier = Modifier.width(500.dp).height(50.dp)
            )
        }
    }
}