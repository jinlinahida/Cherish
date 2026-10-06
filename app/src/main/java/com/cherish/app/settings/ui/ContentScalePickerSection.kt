package com.cherish.app.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.settings.model.AppContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Picker screen for choosing content scale (font & padding scale).
 */
@Composable
fun ContentScalePickerSection(
    currentScale: AppContentScale,
    onScaleChange: (AppContentScale) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        Pair(AppContentScale.SMALL, "小 (90%)"),
        Pair(AppContentScale.STANDARD, "标准 (100%)"),
        Pair(AppContentScale.LARGE, "大 (110%)"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "内容缩放",
                modifier = Modifier.semantics { heading() },
            )
        }

        items(
            count = options.size,
            key = { options[it].first.name },
        ) { index ->
            val (scale, label) = options[index]
            val isSelected = currentScale == scale
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onScaleChange(scale) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isSelected
                    contentDescription = "$label${if (isSelected) "，已选中" else ""}"
                },
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
            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "确定内容缩放选择"
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
