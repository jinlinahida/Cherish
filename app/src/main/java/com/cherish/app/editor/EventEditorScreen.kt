package com.cherish.app.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.cherish.app.editor.components.ColorPickerSection
import com.cherish.app.editor.components.DatePickerSection
import com.cherish.app.editor.components.NotesPickerSection
import com.cherish.app.editor.components.RepeatPickerSection
import com.cherish.app.editor.components.TitleEmojiPickerSection
import com.cherish.app.editor.model.EventEditorState
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventColor
import com.cherish.app.storage.EventImageStorage
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearButtonDefaults
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.ShirokoWearToggleCard
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

enum class EditorSubScreen {
    MAIN,
    TITLE_EMOJI,
    DATE,
    REPEAT,
    COLOR,
    BACKGROUND,
    CATEGORY,
    NOTES,
}

/**
 * Unified Event Editor Screen for Cherish.
 *
 * Product design principles:
 * - Clear Wear OS hierarchy: Live Hero preview card -> Schedule -> Visual Styling -> Preferences.
 * - Decoupled styling: Event Color (Home card subtle gradient) vs Event Background (Detail fullscreen background).
 * - Full support for custom intervals, solar/lunar dates, custom image backgrounds, and priority pinning.
 * - Replaces mechanical SettingsItem stacking with an intuitive wearable layout.
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun EventEditorScreen(
    initialState: EventEditorState,
    onSave: (EventEditorState) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    imageStorage: EventImageStorage? = null,
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

                        // 1. Hero Event Preview Card (Interactive)
                        item(key = "hero_preview_card") {
                            EditorHeroCard(
                                title = state.title,
                                emoji = state.emoji,
                                color = state.color,
                                isLunar = state.isLunar,
                                solarDate = state.solarDate,
                                lunarDate = state.lunarDate,
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.TITLE_EMOJI
                                },
                            )
                        }

                        // Section 1: 时间与周期 (Schedule & Recurrence)
                        item(key = "section_schedule_label") {
                            EditorSectionHeader(text = "目标日期与周期")
                        }

                        // Date Card
                        item(key = "entry_date") {
                            val dateLabel = if (state.isLunar) {
                                val leapStr = if (state.lunarDate.isLeapMonth) "闰" else ""
                                "农历 %04d年%s%02d月%02d日".format(
                                    state.lunarDate.year,
                                    leapStr,
                                    state.lunarDate.month,
                                    state.lunarDate.day,
                                )
                            } else {
                                "公历 %04d年%02d月%02d日".format(
                                    state.solarDate.year,
                                    state.solarDate.month,
                                    state.solarDate.day,
                                )
                            }
                            EditorActionCard(
                                icon = if (state.isLunar) "🌙" else "📅",
                                title = "目标日期",
                                value = dateLabel,
                                actionHint = "点击修改日期与公农历",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.DATE
                                },
                            )
                        }

                        // Repeat Rule Card
                        item(key = "entry_repeat") {
                            val repeatLabel = formatRepeatRule(state.repeatRule)
                            EditorActionCard(
                                icon = "🔁",
                                title = "重复规则",
                                value = repeatLabel,
                                actionHint = "点击修改重复周期",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.REPEAT
                                },
                            )
                        }

                        // Section 2: 视觉风格 (Visual Appearance)
                        item(key = "section_style_label") {
                            EditorSectionHeader(text = "视觉风格")
                        }

                        // Home Card Color (Event Color)
                        item(key = "entry_card_color") {
                            val colorName = formatColorName(state.color)
                            EditorActionCard(
                                icon = "🎨",
                                title = "首页卡片颜色",
                                value = colorName,
                                actionHint = "用于首页卡片的淡淡渐变色",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.COLOR
                                },
                            )
                        }

                        // Detail Screen Background (Event Background)
                        item(key = "entry_detail_bg") {
                            val bgName = formatBackgroundName(state.background)
                            EditorActionCard(
                                icon = "🖼️",
                                title = "详情页背景",
                                value = bgName,
                                actionHint = "用于进入详情后的全屏背景",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.BACKGROUND
                                },
                            )
                        }

                        // Section 3: 分类与排序 (Organization & Preferences)
                        item(key = "section_options_label") {
                            EditorSectionHeader(text = "分类与排序")
                        }

                        // Category Card
                        item(key = "entry_category") {
                            val categoryName = formatCategory(state.category)
                            EditorActionCard(
                                icon = "🏷️",
                                title = "事件分类",
                                value = categoryName,
                                actionHint = "点击修改分类",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.CATEGORY
                                },
                            )
                        }

                        // Pin Toggle Card
                        item(key = "entry_pinned") {
                            ShirokoWearToggleCard(
                                checked = state.isPinned,
                                onCheckedChange = { checked ->
                                    state = state.copy(isPinned = checked)
                                },
                                label = "首页置顶",
                                secondaryLabel = if (state.isPinned) "在首页最优先排在最前列" else "普通排序",
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.semantics {
                                    role = Role.Switch
                                    stateDescription = if (state.isPinned) "开启，已置顶于首页顶部" else "关闭，普通排序"
                                },
                            )
                        }

                        // Notes Card
                        item(key = "entry_notes") {
                            EditorActionCard(
                                icon = "📝",
                                title = "备注说明",
                                value = if (state.notes.isNotBlank()) state.notes else "未设置备注 (选填)",
                                actionHint = "点击修改备注内容",
                                onClick = {
                                    haptics.click()
                                    activeSubScreen = EditorSubScreen.NOTES
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

                EditorSubScreen.COLOR -> {
                    ColorPickerSection(
                        currentColor = state.color,
                        onColorChange = { color -> state = state.copy(color = color) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.BACKGROUND -> {
                    BackgroundPickerSection(
                        currentBackground = state.background,
                        onBackgroundChange = { bg -> state = state.copy(background = bg) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                        imageStorage = imageStorage,
                        eventId = state.eventId,
                    )
                }

                EditorSubScreen.CATEGORY -> {
                    CategoryPickerSection(
                        currentCategory = state.category,
                        onCategoryChange = { cat -> state = state.copy(category = cat) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }

                EditorSubScreen.NOTES -> {
                    NotesPickerSection(
                        notes = state.notes,
                        onNotesChange = { state = state.copy(notes = it) },
                        onConfirm = { activeSubScreen = EditorSubScreen.MAIN },
                    )
                }
            }
        }
    }
}

/**
 * Interactive Hero Card displaying live preview of the event's appearance.
 */
@Composable
private fun EditorHeroCard(
    title: String,
    emoji: String,
    color: EventColor,
    isLunar: Boolean,
    solarDate: SolarDate,
    lunarDate: LunarDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundBrush = when (color) {
        EventColor.Default -> Brush.radialGradient(
            colors = listOf(Color(0xFF262C38), Color(0xFF14171E)),
        )
        is EventColor.Single -> {
            val c = Color(color.argb)
            Brush.linearGradient(
                colors = listOf(c.copy(alpha = 0.35f), Color(0xFF12151B)),
            )
        }
        is EventColor.Gradient -> {
            val start = Color(color.startColor)
            val end = Color(color.endColor)
            Brush.linearGradient(
                colors = listOf(start.copy(alpha = 0.35f), end.copy(alpha = 0.15f)),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
    }

    val displayTitle = if (title.isBlank()) "点击输入事件名称..." else title
    val dateSummary = if (isLunar) {
        val leapStr = if (lunarDate.isLeapMonth) "闰" else ""
        "农历 %04d.%s%02d.%02d".format(lunarDate.year, leapStr, lunarDate.month, lunarDate.day)
    } else {
        "公历 %04d.%02d.%02d".format(solarDate.year, solarDate.month, solarDate.day)
    }

    ShirokoWearCard(
        shape = ShirokoWearShapes.card,
        highlighted = true,
        highlightColor = ShirokoWearTheme.colors.accentGold,
        innerPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "事件预览与名称：$emoji $title，点击修改"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush, ShirokoWearShapes.card),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Large Emoji Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                // Title & Subtitle Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$dateSummary • 点击编辑 ✏️",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = ShirokoWearTheme.colors.accentGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = ShirokoWearTheme.colors.accentGold,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun EditorActionCard(
    icon: String,
    title: String,
    value: String,
    actionHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShirokoWearCard(
        shape = ShirokoWearShapes.cardCompact,
        innerPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title：$value，$actionHint"
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = icon,
                fontSize = 16.sp,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

private fun formatColorName(color: EventColor): String = when (color) {
    EventColor.Default -> "默认 (墨黑)"
    EventColor.Rose -> "玫瑰粉"
    EventColor.Amber -> "琥珀金"
    EventColor.Emerald -> "翡翠绿"
    EventColor.Ocean -> "深海蓝"
    EventColor.Violet -> "紫罗兰"
    EventColor.Coral -> "珊瑚橙"
    EventColor.Sunset -> "落日渐变"
    EventColor.Aurora -> "极光渐变"
    EventColor.Lavender -> "薰衣草渐变"
    is EventColor.Single -> "自定义单色"
    is EventColor.Gradient -> "自定义渐变"
}

private fun formatBackgroundName(bg: EventBackground): String = when (bg) {
    EventBackground.Default -> "默认微光"
    is EventBackground.Color -> "纯色微光"
    is EventBackground.Gradient -> "渐变微光"
    is EventBackground.Pattern -> "纹理风格"
    is EventBackground.Image -> "自定义图片 🖼️"
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
        color = EventColor.Amber,
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
        color = EventColor.Sunset,
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
