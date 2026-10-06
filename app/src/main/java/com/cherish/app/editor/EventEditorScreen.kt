package com.cherish.app.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.components.BackgroundPickerSection
import com.cherish.app.editor.components.CategoryPickerSection
import com.cherish.app.editor.components.DatePickerSection
import com.cherish.app.editor.components.RepeatPickerSection
import com.cherish.app.editor.components.TitleEmojiPickerSection
import com.cherish.app.editor.model.EventEditorState
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearButtonDefaults
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSettingsItem
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.ShirokoWearToggleCard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

import androidx.activity.compose.BackHandler

enum class EditorSubScreen {
    MAIN,
    TITLE_EMOJI,
    DATE,
    REPEAT,
    CATEGORY,
    BACKGROUND,
}

/**
 * Unified Event Editor Screen for Cherish.
 *
 * Supports both creating a new event and editing an existing one.
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun EventEditorScreen(
    initialState: EventEditorState,
    onSave: (EventEditorState) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(initialState) }
    var activeSubScreen by remember { mutableStateOf(EditorSubScreen.MAIN) }
    val haptics = rememberShirokoWearHaptics()

    BackHandler(enabled = activeSubScreen != EditorSubScreen.MAIN) {
        haptics.back()
        activeSubScreen = EditorSubScreen.MAIN
    }

    ShirokoWearAmbient(spotlightKey = "cherish_editor") {
        AnimatedContent(
            targetState = activeSubScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "editorSubScreens",
            modifier = modifier.fillMaxSize(),
        ) { subScreen ->
            when (subScreen) {
                EditorSubScreen.MAIN -> {
                    ShirokoWearScalingRotaryColumn(
                        modifier = Modifier.fillMaxSize(),
                        itemSpacing = 6.dp,
                        contentPadding = ShirokoWearTheme.dimens.screenPadding,
                    ) {
                        item(key = "title") {
                            ShirokoWearScreenTitle(
                                text = if (state.isCreateMode) "新建倒数日" else "编辑倒数日",
                                modifier = Modifier.semantics { heading() },
                            )
                        }

                        // Error Banner if validation failed
                        if (state.errorMessage != null) {
                            item(key = "error_banner") {
                                ShirokoWearCard(
                                    shape = ShirokoWearShapes.cardCompact,
                                    highlighted = true,
                                    highlightColor = ShirokoWearTheme.colors.accentCopper,
                                    innerPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = "错误提示：${state.errorMessage}"
                                    },
                                ) {
                                    Text(
                                        text = state.errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ShirokoWearTheme.colors.accentCopper,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        // Name & Emoji Entry
                        item(key = "entry_name") {
                            val displaySubtitle = if (state.title.isBlank()) {
                                "点击输入名称"
                            } else {
                                "${state.emoji} ${state.title}"
                            }
                            ShirokoWearSettingsItem(
                                title = "事件名称",
                                subtitle = displaySubtitle,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "事件名称",
                                        currentValue = if (state.title.isBlank()) "未设置" else state.title,
                                        actionHint = "点击修改名称与图标",
                                    )
                                },
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.TITLE_EMOJI
                                },
                            )
                        }

                        // Date Entry
                        item(key = "entry_date") {
                            val dateSubtitle = if (state.isLunar) {
                                val leapStr = if (state.lunarDate.isLeapMonth) "闰" else ""
                                "农历 %04d年%s%02d月%02d日".format(
                                    state.lunarDate.year,
                                    leapStr,
                                    state.lunarDate.month,
                                    state.lunarDate.day,
                                )
                            } else {
                                "公历 %04d.%02d.%02d".format(
                                    state.solarDate.year,
                                    state.solarDate.month,
                                    state.solarDate.day,
                                )
                            }
                            ShirokoWearSettingsItem(
                                title = "目标日期",
                                subtitle = dateSubtitle,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "目标日期",
                                        currentValue = dateSubtitle,
                                        actionHint = "点击修改目标日期",
                                    )
                                },
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.DATE
                                },
                            )
                        }

                        // Repeat Rule Entry
                        item(key = "entry_repeat") {
                            val repeatSubtitle = formatRepeatRule(state.repeatRule)
                            ShirokoWearSettingsItem(
                                title = "重复规则",
                                subtitle = repeatSubtitle,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "重复规则",
                                        currentValue = repeatSubtitle,
                                        actionHint = "点击修改重复规则",
                                    )
                                },
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.REPEAT
                                },
                            )
                        }

                        // Category Entry
                        item(key = "entry_category") {
                            val categorySubtitle = formatCategory(state.category)
                            val categorySpeech = AccessibilityPresentation.getCategoryAccessibilityName(state.category)
                            ShirokoWearSettingsItem(
                                title = "事件分类",
                                subtitle = categorySubtitle,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "事件分类",
                                        currentValue = categorySpeech,
                                        actionHint = "点击修改分类",
                                    )
                                },
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.CATEGORY
                                },
                            )
                        }

                        // Background Entry
                        item(key = "entry_bg") {
                            val bgSubtitle = formatBackground(state.background)
                            ShirokoWearSettingsItem(
                                title = "背景风格",
                                subtitle = bgSubtitle,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "背景风格",
                                        currentValue = bgSubtitle,
                                        actionHint = "点击修改背景风格",
                                    )
                                },
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.BACKGROUND
                                },
                            )
                        }

                        // Pinned Toggle Card
                        item(key = "entry_pinned") {
                            ShirokoWearToggleCard(
                                checked = state.isPinned,
                                onCheckedChange = { checked ->
                                    state = state.copy(isPinned = checked)
                                },
                                label = "置顶显示",
                                secondaryLabel = if (state.isPinned) "已置顶于首页顶部" else "普通排序",
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.semantics {
                                    role = Role.Switch
                                    stateDescription = if (state.isPinned) "开启，已置顶于首页顶部" else "关闭，普通排序"
                                },
                            )
                        }

                        // Action Buttons: Save & Cancel
                        item(key = "action_save") {
                            Spacer(modifier = Modifier.height(4.dp))
                            ShirokoWearCardButton(
                                onClick = {
                                    val validation = DatePickerSanitizer.validateAndBuildEvent(state)
                                    if (validation.isFailure) {
                                        state = state.copy(
                                            errorMessage = validation.exceptionOrNull()?.message ?: "保存失败",
                                        )
                                        haptics.back()
                                    } else {
                                        state = state.copy(errorMessage = null)
                                        haptics.click()
                                        onSave(state)
                                    }
                                },
                                border = ShirokoWearButtonDefaults.highlightedBorderStroke(
                                    ShirokoWearTheme.colors.cardHighlight,
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "保存事件"
                                },
                            ) {
                                Text(
                                    text = "保存事件",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ShirokoWearTheme.colors.accentGold,
                                )
                            }
                        }

                        item(key = "action_cancel") {
                            ShirokoWearCardButton(
                                onClick = {
                                    haptics.back()
                                    onCancel()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "取消编辑"
                                },
                            ) {
                                Text(
                                    text = "取消",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                EditorSubScreen.TITLE_EMOJI -> {
                    TitleEmojiPickerSection(
                        title = state.title,
                        emoji = state.emoji,
                        onTitleChange = { state = state.copy(title = it, errorMessage = null) },
                        onEmojiChange = { state = state.copy(emoji = it) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.DATE -> {
                    DatePickerSection(
                        isLunar = state.isLunar,
                        solarDate = state.solarDate,
                        lunarDate = state.lunarDate,
                        onDateTypeChange = { isLunar -> state = state.copy(isLunar = isLunar) },
                        onSolarDateChange = { solar -> state = state.copy(solarDate = solar) },
                        onLunarDateChange = { lunar -> state = state.copy(lunarDate = lunar) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.REPEAT -> {
                    RepeatPickerSection(
                        currentRule = state.repeatRule,
                        onRuleChange = { rule -> state = state.copy(repeatRule = rule) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.CATEGORY -> {
                    CategoryPickerSection(
                        currentCategory = state.category,
                        onCategoryChange = { cat -> state = state.copy(category = cat) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.BACKGROUND -> {
                    BackgroundPickerSection(
                        currentBackground = state.background,
                        onBackgroundChange = { bg -> state = state.copy(background = bg) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }
            }
        }
    }
}

private fun formatRepeatRule(rule: RepeatRule): String = when (rule) {
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

private fun formatCategory(category: EventCategory): String = when (category) {
    EventCategory.GENERAL -> "📅 通用"
    EventCategory.BIRTHDAY -> "🎂 生日"
    EventCategory.ANNIVERSARY -> "💗 纪念日"
    EventCategory.HOLIDAY -> "🎆 节日"
    EventCategory.WORK -> "💼 工作"
    EventCategory.LIFE -> "🌟 生活"
    EventCategory.OTHER -> "📝 其他"
}

private fun formatBackground(bg: EventBackground): String = when (bg) {
    EventBackground.Default -> "默认 (墨色微光)"
    is EventBackground.Color -> "经典配色"
    is EventBackground.Gradient -> "渐变微光"
    is EventBackground.Pattern -> "纹理风格"
    is EventBackground.Image -> "自定义图片"
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun EventEditorScreenPreview() {
    val sampleState = EventEditorState.createDefault(
        today = SolarDate(2026, 10, 5),
        initialLunar = LunarDate(2026, 8, 25),
    )
    ShirokoWearTheme {
        EventEditorScreen(
            initialState = sampleState,
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun EventEditorScreenRoundLargePreview() {
    val sampleState = EventEditorState.createDefault(
        today = SolarDate(2026, 10, 5),
        initialLunar = LunarDate(2026, 8, 25),
    ).copy(
        title = "这是一个超长标题用来验证编辑页小圆屏大字体排版表现",
        emoji = "🎂",
        isPinned = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.ROUND,
    ) {
        EventEditorScreen(
            initialState = sampleState,
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun EventEditorScreenSquareStandardPreview() {
    val sampleState = EventEditorState.createDefault(
        today = SolarDate(2026, 10, 5),
        initialLunar = LunarDate(2026, 8, 25),
    ).copy(
        title = "方屏标准排版",
        emoji = "✨",
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.STANDARD,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        EventEditorScreen(
            initialState = sampleState,
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun EventEditorScreenSquareLargePreview() {
    val sampleState = EventEditorState.createDefault(
        today = SolarDate(2026, 10, 5),
        initialLunar = LunarDate(2026, 8, 25),
    ).copy(
        title = "方屏大字阶排版测试",
        emoji = "🎯",
        isPinned = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        EventEditorScreen(
            initialState = sampleState,
            onSave = {},
            onCancel = {},
        )
    }
}

