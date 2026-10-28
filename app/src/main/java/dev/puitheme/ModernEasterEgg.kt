// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.isActive
import kotlin.random.Random

private const val EGG_TAPS = 5
private const val EGG_TAP_GAP_MS = 1500L

/** The About icon: five quick taps, no hint at all, start one of the themed shows (or, rarely, the hidden one). */
@Composable
internal fun EasterEggIcon(icon: ImageBitmap, description: String, modifier: Modifier) {
    var taps by remember { mutableIntStateOf(0) }
    var last by remember { mutableLongStateOf(0L) }
    // The playing theme and the previous regular one survive rotation; NONE means no show.
    var theme by rememberSaveable { mutableIntStateOf(EasterEggLottery.NONE) }
    var lastRegular by rememberSaveable { mutableIntStateOf(EasterEggLottery.NONE) }
    Image(icon, description, modifier.pointerInput(Unit) {
        detectTapGestures {
            val now = SystemClock.uptimeMillis()
            taps = if (now - last <= EGG_TAP_GAP_MS) taps + 1 else 1
            last = now
            if (taps >= EGG_TAPS && theme == EasterEggLottery.NONE) {
                taps = 0
                val next = EasterEggLottery.pick(Random.nextDouble(), Random.nextDouble(), lastRegular)
                if (EasterEggLottery.isRegular(next)) lastRegular = next
                theme = next
            }
        }
    })
    if (EasterEggLottery.isValid(theme)) EasterEggShow(icon, theme) { theme = EasterEggLottery.NONE }
}

internal fun drawableBitmap(drawable: android.graphics.drawable.Drawable, size: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

@Composable
private fun EasterEggShow(icon: ImageBitmap, theme: Int, onDone: () -> Unit) {
    val measurer = rememberTextMeasurer(cacheSize = 48)
    val context = LocalContext.current
    // Only the hidden show uses it: the local first-install time for "Llevas N días con C17" and its serial.
    val installMillis = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }.getOrDefault(0L)
    }
    val scene = remember(theme, icon, measurer) { eggScene(theme, icon, measurer, installMillis) }
    var elapsedNanos by rememberSaveable { mutableLongStateOf(0L) }
    val playback = remember(theme) { EasterEggPlayback(elapsedNanos, EasterEggLottery.durationNanos(theme)) }
    val seconds = playback.durationNanos() / 1_000_000_000f
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var running by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var failedDraw by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onDone)
    DisposableEffect(lifecycle, playback) {
        val observer = LifecycleEventObserver { _, _ ->
            val resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            playback.setResumed(resumed)
            running = resumed
        }
        playback.setResumed(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        lifecycle.addObserver(observer)
        onDispose {
            playback.setResumed(false)
            lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(playback, running, failedDraw) {
        if (playback.canExit()) {
            if (playback.claimExit()) finish()
            return@LaunchedEffect
        }
        if (!running) return@LaunchedEffect
        while (isActive && !playback.canExit()) {
            elapsedNanos = withFrameNanos { now ->
                playback.setResumed(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                playback.frame(now)
            }
        }
        if (playback.claimExit()) finish()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false,
        dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { /* Play the complete show. */ } }) {
            Canvas(Modifier.fillMaxSize()) {
                // A radial gradient with radius 0 throws (device code162: the dialog's first layout may be 0x0).
                if (!failedDraw && size.minDimension >= 1f) try {
                    val t = elapsedNanos / 1_000_000_000f
                    val fade = 1f - eggEase((t - (seconds - .8f)) / .8f)
                    withEggViewport { with(scene) { draw(t, fade) } }
                } catch (error: RuntimeException) {
                    // A broken frame must release the modal window, without catching coroutine cancellation.
                    Log.w("C17EasterEgg", "Unable to draw the show", error)
                    playback.fail()
                    failedDraw = true
                }
            }
        }
    }
}

private fun eggScene(theme: Int, icon: ImageBitmap, measurer: androidx.compose.ui.text.TextMeasurer, installMillis: Long): EggScene = when (theme) {
    EasterEggLottery.CYBERPUNK -> CyberpunkEgg(icon, measurer)
    EasterEggLottery.PIXEL -> PixelEgg(icon, measurer)
    EasterEggLottery.ANIME -> AnimeEgg(icon, measurer)
    EasterEggLottery.INK -> InkEgg(icon, measurer)
    EasterEggLottery.COSMOS -> CosmosEgg(icon, measurer)
    else -> LegendEgg(icon, measurer, EasterEggMemento.days(installMillis, System.currentTimeMillis()),
        EasterEggMemento.serial(installMillis))
}
