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
import com.cherish.app.event.model.EventCategory
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Event category selection screen for Wear OS.
 */
@Composable
fun CategoryPickerSection(
    currentCategory: EventCategory,
    onCategoryChange: (EventCategory) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf(
        Pair(EventCategory.GENERAL, "📅 通用"),
        Pair(EventCategory.BIRTHDAY, "🎂 生日"),
        Pair(EventCategory.ANNIVERSARY, "💗 纪念日"),
        Pair(EventCategory.HOLIDAY, "🎆 节日"),
        Pair(EventCategory.WORK, "💼 工作"),
        Pair(EventCategory.LIFE, "🌟 生活"),
        Pair(EventCategory.OTHER, "📝 其他"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(text = "事件分类")
        }

        items(
            count = categories.size,
            key = { categories[it].first.name },
        ) { index ->
            val (category, label) = categories[index]
            val isSelected = currentCategory == category
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onCategoryChange(category) },
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
