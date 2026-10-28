// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.ButtonDefaults as CouiButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card as CouiCard
import io.github.suqi8.coui.kmp.basic.CardDefaults as CouiCardDefaults
import io.github.suqi8.coui.kmp.basic.IconButton as CouiIconButton
import io.github.suqi8.coui.kmp.basic.Scaffold as CouiScaffold
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.ScrollBehavior as CouiScrollBehavior
import io.github.suqi8.coui.kmp.basic.TopAppBarState as CouiTopAppBarState
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarState as rememberCouiTopAppBarState
import io.github.suqi8.coui.kmp.basic.Slider as CouiSlider
import io.github.suqi8.coui.kmp.basic.Text as CouiText
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar as CouiSmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Switch as CouiSwitch
import io.github.suqi8.coui.kmp.basic.TextButton as CouiTextButton
import io.github.suqi8.coui.kmp.basic.TextButtonColors as CouiTextButtonColors
import io.github.suqi8.coui.kmp.basic.TextField as CouiTextField
import io.github.suqi8.coui.kmp.basic.TextFieldDefaults as CouiTextFieldDefaults
import io.github.suqi8.coui.kmp.basic.TextFieldMode as CouiTextFieldMode
import io.github.suqi8.coui.kmp.basic.TopAppBar as CouiTopAppBar
import io.github.suqi8.coui.kmp.layout.DialogDefaults as CouiDialogDefaults
import io.github.suqi8.coui.kmp.overlay.OverlayDialog as CouiOverlayDialog
import io.github.suqi8.coui.kmp.preference.RadioButtonLocation as CouiRadioButtonLocation
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference as CouiRadioButtonPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference as CouiSwitchPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor as CouiContentColor
import io.github.suqi8.coui.kmp.utils.PressFeedbackType as CouiPressFeedbackType
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults as MiuixBasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarState as MiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.layout.DialogDefaults as MiuixDialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog as MiuixOverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference as MiuixRadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference as MiuixSliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference as MiuixSwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixContentColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** Keep both overlay hosts available; only the active scaffold owns bars and insets. */
@Composable
fun C17Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    val coui = LocalC17Coui.current
    // This outer call stays at the same composition position when the UI style changes.
    MiuixScaffold(
        modifier = modifier,
        topBar = { if (!coui) topBar() },
        bottomBar = { if (!coui) bottomBar() },
        containerColor = if (coui) Color.Transparent else containerColor,
        contentWindowInsets = if (coui) WindowInsets(0) else contentWindowInsets,
    ) { padding ->
        if (coui) {
            CouiScaffold(
                topBar = topBar,
                bottomBar = bottomBar,
                containerColor = containerColor,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        } else {
            content(padding)
        }
    }
}

/** Each component family retains its own header position across style changes and page navigation. */
@Stable
class C17TopAppBarState internal constructor(
    internal val miuix: MiuixTopAppBarState,
    internal val coui: CouiTopAppBarState,
)

@Composable
fun rememberC17TopAppBarState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
): C17TopAppBarState {
    val miuix = rememberMiuixTopAppBarState(initialHeightOffsetLimit = initialHeightOffsetLimit)
    val coui = rememberCouiTopAppBarState(initialHeightOffsetLimit = initialHeightOffsetLimit)
    return remember(miuix, coui) { C17TopAppBarState(miuix, coui) }
}

@Stable
class C17ScrollBehavior internal constructor(
    internal val miuix: ScrollBehavior,
    internal val coui: CouiScrollBehavior,
    val nestedScrollConnection: NestedScrollConnection,
)

/** Only the active family's connection handles gestures; the inactive header does not consume scroll. */
@Composable
fun rememberC17ScrollBehavior(
    state: C17TopAppBarState = rememberC17TopAppBarState(),
): C17ScrollBehavior {
    val miuix = MiuixScrollBehavior(state = state.miuix)
    val coui = COUIScrollBehavior(state = state.coui)
    val useCoui = LocalC17Coui.current
    return remember(miuix, coui, useCoui) {
        C17ScrollBehavior(miuix, coui, if (useCoui) coui.nestedScrollConnection else miuix.nestedScrollConnection)
    }
}

@Composable
fun C17TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: C17ScrollBehavior? = null,
) {
    if (LocalC17Coui.current) {
        CouiTopAppBar(title = title, modifier = modifier, color = color,
            navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior?.coui,
            defaultWindowInsetsPadding = false)
    } else {
        MiuixTopAppBar(title = title, modifier = modifier, color = color,
            navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior?.miuix)
    }
}

@Composable
fun C17SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: C17ScrollBehavior? = null,
) {
    if (LocalC17Coui.current) {
        CouiSmallTopAppBar(title = title, modifier = modifier, color = color,
            navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior?.coui,
            defaultWindowInsetsPadding = false)
    } else {
        MiuixSmallTopAppBar(title = title, modifier = modifier, color = color,
            navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior?.miuix)
    }
}

@Composable
fun C17Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    insideMargin: PaddingValues = MiuixCardDefaults.InsideMargin,
    colors: CardColors? = null,
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalC17Coui.current) {
        val couiColors = if (colors == null) CouiCardDefaults.defaultColors() else {
            CouiCardDefaults.defaultColors(
                color = colors.color,
                contentColor = colors.contentColor,
                pressedColor = lerp(colors.color, colors.contentColor, .06f),
            )
        }
        CouiCard(
            modifier = modifier,
            cornerRadius = cornerRadius ?: CouiCardDefaults.CornerRadius,
            insideMargin = insideMargin,
            colors = couiColors,
            pressFeedbackType = when (pressFeedbackType) {
                PressFeedbackType.Sink -> CouiPressFeedbackType.Sink
                PressFeedbackType.Tilt -> CouiPressFeedbackType.Tilt
                PressFeedbackType.None -> CouiPressFeedbackType.Tint
            },
            onClick = onClick,
            content = content,
        )
    } else {
        MiuixCard(modifier = modifier, cornerRadius = cornerRadius ?: MiuixCardDefaults.CornerRadius,
            insideMargin = insideMargin, colors = colors ?: MiuixCardDefaults.defaultColors(),
            pressFeedbackType = pressFeedbackType, onClick = onClick, content = content)
    }
}

@Composable
fun C17Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalC17Coui.current) {
        Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(value = checked, enabled = enabled && onCheckedChange != null,
                role = Role.Switch, onValueChange = { if (enabled) onCheckedChange?.invoke(it) }),
            contentAlignment = Alignment.Center) {
            CouiSwitch(checked = checked, onCheckedChange = null, enabled = enabled,
                modifier = Modifier.clearAndSetSemantics {})
        }
    } else {
        MiuixSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    }
}

@Composable
fun C17Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (LocalC17Coui.current) {
        // Upstream 1.1.0 still installs setProgress when disabled. Guard callbacks and remove that action.
        val semantics = if (enabled) Modifier else Modifier.clearAndSetSemantics {
            disabled()
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
        }
        CouiSlider(value = value, onValueChange = { if (enabled) onValueChange(it) },
            modifier = modifier.then(semantics).heightIn(min = 48.dp), enabled = enabled,
            valueRange = valueRange, steps = steps,
            onValueChangeFinished = { if (enabled) onValueChangeFinished?.invoke() })
    } else {
        MiuixSlider(value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
            valueRange = valueRange, steps = steps, onValueChangeFinished = onValueChangeFinished)
    }
}

@Composable
fun C17IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val appearance = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    if (LocalC17Coui.current) {
        val tint = if (enabled) MiuixContentColor.current else COUITheme.colorScheme.disabledOnSurface
        CompositionLocalProvider(CouiContentColor provides tint, MiuixContentColor provides tint) {
            CouiIconButton(onClick = onClick, modifier = appearance.semantics { if (!enabled) disabled() },
                enabled = enabled, minHeight = 48.dp, minWidth = 48.dp, content = content)
        }
    } else {
        MiuixIconButton(onClick = onClick, modifier = appearance, enabled = enabled, content = content)
    }
}

@Composable
fun C17TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TextButtonColors? = null,
) {
    if (LocalC17Coui.current) {
        val couiColors = colors?.let {
            CouiTextButtonColors(color = it.color, disabledColor = it.disabledColor,
                textColor = it.textColor, disabledTextColor = it.disabledTextColor)
        } ?: CouiButtonDefaults.textButtonColors()
        CouiTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled,
            colors = couiColors, minHeight = 48.dp, insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            textStyle = COUITheme.textStyles.button.copy(textAlign = TextAlign.Center))
    } else {
        MiuixTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled,
            colors = colors ?: MiuixButtonDefaults.textButtonColors())
    }
}

@Composable
fun C17OverlayDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color? = null,
    summary: String? = null,
    summaryColor: Color? = null,
    backgroundColor: Color? = null,
    onDismissRequest: (() -> Unit)? = null,
    insideMargin: DpSize? = null,
    outsideMargin: DpSize? = null,
    content: @Composable () -> Unit,
) {
    if (LocalC17Coui.current) {
        val contentMargin = insideMargin ?: CouiDialogDefaults.insideMargin
        CouiOverlayDialog(show = show, modifier = modifier,
            titleColor = titleColor ?: CouiDialogDefaults.titleColor(),
            summaryColor = summaryColor ?: CouiDialogDefaults.summaryColor(),
            backgroundColor = backgroundColor ?: CouiDialogDefaults.backgroundColor(),
            onDismissRequest = onDismissRequest,
            insideMargin = contentMargin, outsideMargin = outsideMargin ?: CouiDialogDefaults.outsideMargin) {
            C17CouiDialogContent(title = title, summary = summary,
                titleColor = titleColor ?: CouiDialogDefaults.titleColor(),
                summaryColor = summaryColor ?: CouiDialogDefaults.summaryColor(),
                insideMargin = contentMargin, content = content)
        }
    } else {
        MiuixOverlayDialog(show = show, modifier = modifier, title = title, summary = summary,
            titleColor = titleColor ?: MiuixDialogDefaults.titleColor(),
            summaryColor = summaryColor ?: MiuixDialogDefaults.summaryColor(),
            backgroundColor = backgroundColor ?: MiuixDialogDefaults.backgroundColor(),
            onDismissRequest = onDismissRequest,
            insideMargin = insideMargin ?: MiuixDialogDefaults.insideMargin,
            outsideMargin = outsideMargin ?: MiuixDialogDefaults.outsideMargin, content = content)
    }
}

@Composable
fun C17TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize? = null,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    val appearance = modifier.semantics {
        if (!enabled) disabled()
        if (isError) error(errorMessage ?: "El contenido introducido no es válido.")
    }
    if (LocalC17Coui.current) {
        CouiTextField(value = value, onValueChange = onValueChange, modifier = appearance,
            insideMargin = insideMargin ?: CouiTextFieldDefaults.insideMargin(CouiTextFieldMode.Line),
            label = label, useLabelAsPlaceholder = useLabelAsPlaceholder, enabled = enabled,
            singleLine = singleLine, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            leadingIcon = leadingIcon, isError = isError, showClearButton = singleLine)
    } else {
        MiuixTextField(value = value, onValueChange = onValueChange, modifier = appearance,
            insideMargin = insideMargin ?: MiuixTextFieldDefaults.InsideMargin,
            label = label, useLabelAsPlaceholder = useLabelAsPlaceholder, enabled = enabled,
            singleLine = singleLine, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            leadingIcon = leadingIcon)
    }
}

@Composable
fun C17BasicComponent(
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (LocalC17Coui.current) {
        C17CouiPreferenceRow(modifier = modifier,
            startAction = startAction, endActions = endActions, bottomAction = bottomAction,
            insideMargin = insideMargin ?: PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            onClick = onClick, enabled = enabled) {
            C17CouiPreferenceLabel(title, summary, enabled)
        }
    } else {
        MiuixBasicComponent(modifier = modifier, title = title, summary = summary,
            startAction = startAction, endActions = endActions, bottomAction = bottomAction,
            insideMargin = insideMargin ?: MiuixBasicComponentDefaults.InsideMargin,
            onClick = onClick, enabled = enabled)
    }
}

@Composable
fun C17BasicComponent(
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalC17Coui.current) {
        C17CouiPreferenceRow(modifier = modifier, startAction = startAction, endActions = endActions,
            bottomAction = bottomAction, insideMargin = insideMargin ?: PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            onClick = onClick, enabled = enabled, content = content)
    } else {
        MiuixBasicComponent(modifier = modifier, startAction = startAction, endActions = endActions,
            bottomAction = bottomAction, insideMargin = insideMargin ?: MiuixBasicComponentDefaults.InsideMargin,
            onClick = onClick, enabled = enabled, content = content)
    }
}

@Composable
fun C17ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    insideMargin: PaddingValues? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (LocalC17Coui.current) {
        C17CouiPreferenceRow(modifier = modifier, startAction = startAction,
            endActions = { endActions(); C17CouiArrow(enabled) },
            insideMargin = insideMargin ?: PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            onClick = onClick, enabled = enabled) {
            C17CouiPreferenceLabel(title, summary, enabled)
        }
    } else {
        MiuixArrowPreference(title = title, modifier = modifier, summary = summary,
            startAction = startAction, endActions = endActions,
            insideMargin = insideMargin ?: MiuixBasicComponentDefaults.InsideMargin,
            onClick = onClick, enabled = enabled)
    }
}

@Composable
fun C17SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (LocalC17Coui.current) {
        CouiSwitchPreference(checked = checked, onCheckedChange = { if (enabled) onCheckedChange(it) }, title = title,
            modifier = modifier.heightIn(min = 56.dp).semantics { if (!enabled) disabled() },
            summary = summary, startAction = startAction, enabled = enabled,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp))
    } else {
        MiuixSwitchPreference(checked = checked, onCheckedChange = onCheckedChange, title = title,
            modifier = modifier, summary = summary, startAction = startAction, enabled = enabled)
    }
}

@Composable
fun C17SliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    valueText: String? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (LocalC17Coui.current) {
        C17CouiPreferenceRow(modifier = modifier, enabled = enabled, bottomAction = {
            Column(Modifier.fillMaxWidth()) {
                if (valueText != null) CouiText(valueText, Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                    style = COUITheme.textStyles.body2,
                    color = if (enabled) COUITheme.colorScheme.primary else COUITheme.colorScheme.disabledOnSecondaryVariant)
                C17Slider(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
                    enabled = enabled, valueRange = valueRange, steps = steps, onValueChangeFinished = onValueChangeFinished)
            }
        }) { C17CouiPreferenceLabel(title, summary, enabled) }
    } else {
        MiuixSliderPreference(value = value, onValueChange = onValueChange, modifier = modifier,
            title = title, summary = summary, valueText = valueText, enabled = enabled,
            valueRange = valueRange, steps = steps, onValueChangeFinished = onValueChangeFinished)
    }
}

@Composable
fun C17RadioButtonPreference(
    title: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    radioButtonLocation: RadioButtonLocation = RadioButtonLocation.Start,
    insideMargin: PaddingValues? = null,
    enabled: Boolean = true,
) {
    if (LocalC17Coui.current) {
        CouiRadioButtonPreference(title = title, selected = selected, onClick = { if (enabled) onClick?.invoke() },
            modifier = modifier.heightIn(min = 56.dp).semantics { if (!enabled) disabled() }, summary = summary, startAction = startAction,
            radioButtonLocation = when (radioButtonLocation) {
                RadioButtonLocation.Start -> CouiRadioButtonLocation.Start
                RadioButtonLocation.End -> CouiRadioButtonLocation.End
            },
            insideMargin = insideMargin ?: PaddingValues(horizontal = 16.dp, vertical = 12.dp), enabled = enabled)
    } else {
        MiuixRadioButtonPreference(title = title, selected = selected, onClick = onClick,
            modifier = modifier, summary = summary, startAction = startAction,
            radioButtonLocation = radioButtonLocation,
            insideMargin = insideMargin ?: MiuixBasicComponentDefaults.InsideMargin, enabled = enabled)
    }
}
