package com.aweagle.arrowbloom.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.TextButton as M3TextButton

/**
 * Press feedback for everything tappable: a small dip under the finger plus a haptic tick,
 * so a tap is answered the moment it lands rather than when its result arrives. Compose
 * 1.6 indications may only draw, not resize, so these wrappers shadow the Material buttons;
 * a screen swaps one import and every button in it gains the behaviour.
 */
private const val PRESSED_SCALE = 0.945f
private const val ICON_PRESSED_SCALE = 0.86f

/** Fired on finger-down; MainActivity provides the game's own haptic so the setting is honoured. */
val LocalPressHaptic: ProvidableCompositionLocal<() -> Unit> = compositionLocalOf { {} }

/** False when the player has animations off system-wide; provided by MainActivity. */
val LocalReduceMotion: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Composable
fun rememberPressScale(interaction: MutableInteractionSource, pressedScale: Float = PRESSED_SCALE): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalPressHaptic.current
    LaunchedEffect(pressed) { if (pressed) haptic() }
    return animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh),
        label = "press",
    )
}

private fun Modifier.pressScale(scale: State<Float>) = graphicsLayer {
    val s = scale.value
    scaleX = s
    scaleY = s
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    M3Button(
        onClick = onClick,
        modifier = modifier.pressScale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    M3TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, ICON_PRESSED_SCALE)
    M3IconButton(
        onClick = onClick,
        modifier = modifier.pressScale(scale),
        enabled = enabled,
        colors = colors,
        interactionSource = interaction,
        content = content,
    )
}

/** For cards and rows that are tappable without being a Button: the same dip, ripple kept. */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    return this
        .pressScale(scale)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
}
