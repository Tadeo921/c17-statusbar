// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment

/** Local feedback for the status-bar order editor only; no app-wide page or dialog transitions. */
internal object C17Motion {
    const val ExpandMillis = 220
    const val FastMillis = 160
    val expandEnter: EnterTransition get() = fadeIn(tween(FastMillis)) +
        expandVertically(tween(ExpandMillis, easing = FastOutSlowInEasing), expandFrom = Alignment.Top)
    val expandExit: ExitTransition get() = fadeOut(tween(FastMillis)) +
        shrinkVertically(tween(ExpandMillis, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top)
    fun <T> placementSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = .9f, stiffness = 400f)
}
