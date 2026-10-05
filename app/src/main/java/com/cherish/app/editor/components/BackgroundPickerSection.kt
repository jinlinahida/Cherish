package com.cherish.app.editor.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.EventBackground
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Background style selection screen for Wear OS.
 */
@Composable
fun BackgroundPickerSection(
    currentBackground: EventBackground,
    onBackgroundChange: (EventBackground) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles = listOf(
        Pair(EventBackground.Default, "默认 (墨色微光)"),
        Pair(EventBackground.Color(0xFFE91E63), "粉红微光"),
        Pair(EventBackground.Color(0xFFFFA000), "琥珀金光"),
        Pair(EventBackground.Color(0xFF009688), "青空翡翠"),
        Pair(EventBackground.Color(0xFF1E88E5), "星河深蓝"),
        Pair(EventBackground.Color(0xFF9C27B0), "紫罗兰光"),
        Pair(EventBackground.Gradient(0xFFFFA000, 0xFFE91E63), "日落渐变"),
        Pair(EventBackground.Gradient(0xFF009688, 0xFF1E88E5), "极光渐变"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(text = "背景风格")
        }

        items(
            count = styles.size,
            key = { styles[it].second },
        ) { index ->
            val (bg, label) = styles[index]
            val isSelected = currentBackground == bg
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onBackgroundChange(bg) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(onClick = onConfirm) {
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
