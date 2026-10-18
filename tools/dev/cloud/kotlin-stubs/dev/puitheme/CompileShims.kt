package dev.puitheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
/** Compile-only: Compose 1.8.2 desktop lacks the 1.12 window size property and the Android Bitmap conversion. */
val WindowInfo.containerDpSize: DpSize get() = DpSize(400.dp, 900.dp)
fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = throw UnsupportedOperationException()
