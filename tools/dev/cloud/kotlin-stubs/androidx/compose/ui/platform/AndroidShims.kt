package androidx.compose.ui.platform
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
val LocalContext = staticCompositionLocalOf<Context> { error("compile-only") }
class ComposeView(context: Context) : android.widget.FrameLayout(context) {
    var consumeWindowInsets: Boolean = true
    fun setContent(content: @Composable () -> Unit) { }
}
