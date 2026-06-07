package com.liquidnote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.rememberBackdrop

@Composable
fun LiquidSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 16.dp,
    containerAlpha: Float = 0.35f,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = rememberBackdrop()
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isLight) Color(0xFFFAFAFA).copy(containerAlpha) else Color(0xFF1C1C1E).copy(containerAlpha)

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(blurRadius.toPx(), blurRadius.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backdrop = rememberBackdrop()
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isLight) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF1C1C1E).copy(0.4f)

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(blurRadius.toPx(), blurRadius.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backdrop = rememberBackdrop()
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isLight) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF1C1C1E).copy(0.4f)

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    lens(12.dp.toPx(), 12.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun LiquidFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backdrop = rememberBackdrop()
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isLight) Color(0xFF007AFF).copy(0.8f) else Color(0xFF0A84FF).copy(0.8f)

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = CircleShape
                clip = true
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                    lens(16.dp.toPx(), 16.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun LiquidSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val backdrop = rememberBackdrop()
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isLight) Color(0xFFE5E5EA).copy(0.5f) else Color(0xFF2C2C2E).copy(0.5f)

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(12.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(8.dp.toPx(), 8.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Search,
            contentDescription = placeholder,
            tint = if (isLight) Color(0xFF8E8E93) else Color(0xFF8E8E93),
            modifier = Modifier.padding(end = 8.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                color = if (isLight) Color.Black else Color.White
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = if (isLight) Color(0xFF8E8E93) else Color(0xFF8E8E93)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}
