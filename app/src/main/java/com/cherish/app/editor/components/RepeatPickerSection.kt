package com.cherish.app.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Repeat rule selection screen for Wear OS.
 */
@Composable
fun RepeatPickerSection(
    currentRule: RepeatRule,
    onRuleChange: (RepeatRule) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        Pair(RepeatRule.None, "不重复"),
        Pair(RepeatRule.Daily, "每天重复"),
        Pair(RepeatRule.Custom(1, RepeatUnit.WEEK), "每周重复"),
        Pair(RepeatRule.Monthly, "每月重复"),
        Pair(RepeatRule.Yearly, "每年重复"),
        Pair(RepeatRule.Custom(2, RepeatUnit.WEEK), "每 2 周重复"),
        Pair(RepeatRule.Custom(30, RepeatUnit.DAY), "每 30 天重复"),
        Pair(RepeatRule.Custom(100, RepeatUnit.DAY), "每 100 天重复"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(text = "重复规则")
        }

        items(
            count = options.size,
            key = { options[it].second },
        ) { index ->
            val (rule, label) = options[index]
            val isSelected = currentRule == rule
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onRuleChange(rule) },
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
