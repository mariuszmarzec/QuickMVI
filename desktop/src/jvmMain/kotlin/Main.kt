import com.marzec.common.App
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        state = rememberWindowState(size = DpSize(800.dp, 1000.dp)),
        onCloseRequest = ::exitApplication
    ) {
        MaterialTheme {
            App()
        }
    }
}