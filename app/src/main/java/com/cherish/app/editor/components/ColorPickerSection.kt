package com.cherish.app.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.EventColor
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Event Color Picker section for Cherish.
 *
 * Exclusively controls the subtle gradient color scheme of Home cards.
 * Decoupled completely from Detail page's [com.cherish.app.event.model.EventBackground].
 */
@Composable
fun ColorPickerSection(
    currentColor: EventColor,
    onColorChange: (EventColor) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorOptions = listOf(
        Pair(EventColor.Default, "默认 (墨黑)"),
        Pair(EventColor.Amber, "琥珀金"),
        Pair(EventColor.Rose, "玫瑰粉"),
        Pair(EventColor.Emerald, "翡翠绿"),
        Pair(EventColor.Ocean, "深海蓝"),
        Pair(EventColor.Violet, "紫罗兰"),
        Pair(EventColor.Coral, "珊瑚橙"),
        Pair(EventColor.Sunset, "落日渐变"),
        Pair(EventColor.Aurora, "极光渐变"),
        Pair(EventColor.Lavender, "薰衣草渐变"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "卡片颜色",
                modifier = Modifier.semantics { heading() },
            )
        }

        item(key = "hint_banner") {
            Text(
                text = "用于首页卡片的淡淡渐变色\n不影响详情页背景",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clearAndSetSemantics { },
            )
        }

        items(
            count = colorOptions.size,
            key = { colorOptions[it].second },
        ) { index ->
            val (color, label) = colorOptions[index]
            val isSelected = currentColor == color

            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onColorChange(color) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isSelected
                    contentDescription = "$label${if (isSelected) "，已选中" else ""}"
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Color Preview Swatch
                    ColorSwatch(
                        color = color,
                        modifier = Modifier.size(20.dp),
                    )

                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "确定卡片颜色选择"
                },
            ) {
                Text(
                    text = "确定",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShirokoWearTheme.colors.accentGold,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: EventColor,
    modifier: Modifier = Modifier,
) {
    val brush = when (color) {
        EventColor.Default -> Brush.radialGradient(
            colors = listOf(Color(0xFF2C3240), Color(0xFF14171E)),
        )
        is EventColor.Single -> {
            val c = Color(color.argb)
            Brush.linearGradient(
                colors = listOf(c.copy(alpha = 0.85f), c.copy(alpha = 0.40f)),
            )
        }
        is EventColor.Gradient -> {
            val start = Color(color.startColor)
            val end = Color(color.endColor)
            Brush.linearGradient(
                colors = listOf(start.copy(alpha = 0.90f), end.copy(alpha = 0.60f)),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    )
}
