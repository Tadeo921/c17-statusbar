// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The homepage entrance and its detail use the same font metrics and leading. */
internal object C17CardTypography {
    val label = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Normal, platformStyle = PlatformTextStyle(includeFontPadding = false))
    val title = TextStyle(fontFamily = FontFamily.Default, fontSize = 24.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.Bold, platformStyle = PlatformTextStyle(includeFontPadding = false))
    val supporting = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Normal, platformStyle = PlatformTextStyle(includeFontPadding = false))
}
