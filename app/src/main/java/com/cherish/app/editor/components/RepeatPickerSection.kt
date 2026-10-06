package com.cherish.app.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * Repeat rule selection screen for Wear OS.
 *
 * Supports:
 * - Standard cycles: None (不重复), Daily (每天), Monthly (每月), Yearly (每年)
 * - Arbitrary custom intervals: Every N Days, Weeks, Months, or Years (e.g. 每 2 周, 每 100 天)
 */
@Composable
fun RepeatPickerSection(
    currentRule: RepeatRule,
    onRuleChange: (RepeatRule) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberShirokoWearHaptics()

    // Determine initial custom interval & unit
    var customInterval by remember(currentRule) {
        val initialInt = when (currentRule) {
            is RepeatRule.Custom -> currentRule.interval
            else -> 1
        }
        mutableIntStateOf(initialInt)
    }
    var customUnit by remember(currentRule) {
        val initialU = when (currentRule) {
            is RepeatRule.Custom -> currentRule.unit
            else -> RepeatUnit.WEEK
        }
        mutableStateOf(initialU)
    }

    val isCustomActive = currentRule is RepeatRule.Custom

    val standardOptions = listOf(
        Pair(RepeatRule.None, "不重复"),
        Pair(RepeatRule.Daily, "每天重复"),
        Pair(RepeatRule.Monthly, "每月重复"),
        Pair(RepeatRule.Yearly, "每年重复"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "重复规则",
                modifier = Modifier.semantics { heading() },
            )
        }

        // Standard Options
        items(
            count = standardOptions.size,
            key = { standardOptions[it].second },
        ) { index ->
            val (rule, label) = standardOptions[index]
            val isSelected = currentRule == rule
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = {
                    haptics.click()
                    onRuleChange(rule)
                },
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

        // Custom Repeat Interval Section
        item(key = "custom_repeat_header") {
            Text(
                text = "自定义间隔",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.accentGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { heading() },
            )
        }

        item(key = "custom_repeat_card") {
            ShirokoWearCard(
                shape = ShirokoWearShapes.cardCompact,
                highlighted = isCustomActive,
                highlightColor = ShirokoWearTheme.colors.accentGold,
                innerPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isCustomActive
                    contentDescription = "自定义重复间隔${if (isCustomActive) "，已选中" else ""}"
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Interval Stepper Row (- / +)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShirokoWearSelectableButton(
                            selected = false,
                            onClick = {
                                if (customInterval > 1) {
                                    haptics.click()
                                    val newInterval = customInterval - 1
                                    customInterval = newInterval
                                    onRuleChange(RepeatRule.Custom(newInterval, customUnit))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "减少间隔数值"
                            },
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "每 $customInterval ${formatUnitName(customUnit)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomActive) ShirokoWearTheme.colors.accentGold else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "当前自定义间隔：每 $customInterval ${formatUnitName(customUnit)}"
                            },
                        )

                        ShirokoWearSelectableButton(
                            selected = false,
                            onClick = {
                                if (customInterval < 365) {
                                    haptics.click()
                                    val newInterval = customInterval + 1
                                    customInterval = newInterval
                                    onRuleChange(RepeatRule.Custom(newInterval, customUnit))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "增加间隔数值"
                            },
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Unit Selection Buttons: 天 / 周 / 月 / 年
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        val units = listOf(
                            Pair(RepeatUnit.DAY, "天"),
                            Pair(RepeatUnit.WEEK, "周"),
                            Pair(RepeatUnit.MONTH, "月"),
                            Pair(RepeatUnit.YEAR, "年"),
                        )
                        for ((u, label) in units) {
                            val isUnitSelected = isCustomActive && customUnit == u
                            ShirokoWearSelectableButton(
                                selected = isUnitSelected,
                                onClick = {
                                    haptics.click()
                                    customUnit = u
                                    onRuleChange(RepeatRule.Custom(customInterval, u))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = if (isUnitSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Quick number chips for wearable taps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        val quickIntervals = listOf(1, 2, 7, 30, 100)
                        for (quickInt in quickIntervals) {
                            val isChipSelected = isCustomActive && customInterval == quickInt
                            ShirokoWearSelectableButton(
                                selected = isChipSelected,
                                onClick = {
                                    haptics.click()
                                    customInterval = quickInt
                                    onRuleChange(RepeatRule.Custom(quickInt, customUnit))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp),
                            ) {
                                Text(
                                    text = "$quickInt",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Rule Preview
        item(key = "rule_preview") {
            Text(
                text = "已选规则：" + formatRulePreview(currentRule),
                style = MaterialTheme.typography.bodySmall,
                color = ShirokoWearTheme.colors.accentGold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            )
        }

        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    contentDescription = "确定重复规则"
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

private fun formatUnitName(unit: RepeatUnit): String = when (unit) {
    RepeatUnit.DAY -> "天"
    RepeatUnit.WEEK -> "周"
    RepeatUnit.MONTH -> "个月"
    RepeatUnit.YEAR -> "年"
}

private fun formatRulePreview(rule: RepeatRule): String = when (rule) {
    RepeatRule.None -> "不重复"
    RepeatRule.Daily -> "每天重复"
    RepeatRule.Monthly -> "每月重复"
    RepeatRule.Yearly -> "每年重复"
    is RepeatRule.Custom -> when (rule.unit) {
        RepeatUnit.DAY -> "每 ${rule.interval} 天"
        RepeatUnit.WEEK -> if (rule.interval == 1) "每周" else "每 ${rule.interval} 周"
        RepeatUnit.MONTH -> "每 ${rule.interval} 个月"
        RepeatUnit.YEAR -> "每 ${rule.interval} 年"
    }
}
