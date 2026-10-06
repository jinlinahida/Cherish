package com.cherish.app.ui

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeEventUiModel
import com.cherish.app.home.model.HomeViewMode

/**
 * Pure Kotlin presentation builders for accessible TalkBack descriptions and semantics.
 *
 * Decoupled completely from Android UI / Framework classes to ensure 100% JVM unit testability.
 */
object AccessibilityPresentation {

    /**
     * Maps known curated preset emojis to concise, natural Chinese descriptions so TalkBack
     * does not read verbose Unicode codepoints or English names.
     */
    fun getEmojiAccessibilityName(emoji: String): String = when (emoji.trim()) {
        "🎂" -> "蛋糕"
        "💗" -> "爱心"
        "🎓" -> "毕业帽"
        "✈️", "✈" -> "飞机"
        "🎆" -> "烟花"
        "🌟" -> "星星"
        "💼" -> "公文包"
        "🏖️", "🏖" -> "沙滩"
        "🚗" -> "汽车"
        "🎁" -> "礼物"
        "📅" -> "日历"
        "💍" -> "戒指"
        else -> emoji.trim()
    }

    /**
     * Maps event category enums to concise Chinese speech labels without emoji prefix.
     */
    fun getCategoryAccessibilityName(category: EventCategory): String = when (category) {
        EventCategory.GENERAL -> "通用"
        EventCategory.BIRTHDAY -> "生日"
        EventCategory.ANNIVERSARY -> "纪念日"
        EventCategory.HOLIDAY -> "节日"
        EventCategory.WORK -> "工作"
        EventCategory.LIFE -> "生活"
        EventCategory.OTHER -> "其他"
    }

    /**
     * Formats a recurrence rule into concise natural Chinese speech.
     * Returns null if no recurrence is configured.
     */
    fun formatRepeatRuleAccessibility(repeatRule: RepeatRule): String? = when (repeatRule) {
        RepeatRule.None -> null
        RepeatRule.Daily -> "每天重复"
        RepeatRule.Monthly -> "每月重复"
        RepeatRule.Yearly -> "每年重复"
        is RepeatRule.Custom -> {
            if (repeatRule.interval == 1 && repeatRule.unit == RepeatUnit.WEEK) {
                "每周重复"
            } else {
                val unitStr = when (repeatRule.unit) {
                    RepeatUnit.DAY -> "天"
                    RepeatUnit.WEEK -> "周"
                    RepeatUnit.MONTH -> "月"
                    RepeatUnit.YEAR -> "年"
                }
                "每 ${repeatRule.interval} $unitStr 重复"
            }
        }
    }

    /**
     * Builds a comprehensive, unified semantic description for a [CountdownEventCard].
     * Reads title, days countdown status, target date, recurrence, category, and pinned status
     * as a single cohesive sentence.
     */
    fun buildEventCardAccessibilityDescription(
        title: String,
        daysCount: Int,
        status: CountdownDisplayStatus,
        targetSolarDate: SolarDate,
        category: EventCategory,
        isPinned: Boolean,
        repeatRule: RepeatRule = RepeatRule.None,
        isLunar: Boolean = false,
        lunarDate: LunarDate? = null,
    ): String {
        val parts = mutableListOf<String>()

        val displayTitle = title.trim().ifBlank { "未命名事件" }
        parts.add(displayTitle)

        when (status) {
            CountdownDisplayStatus.TODAY -> parts.add("今天")
            CountdownDisplayStatus.COUNTDOWN -> parts.add("${daysCount} 天后")
            CountdownDisplayStatus.PAST -> parts.add("已过 ${daysCount} 天")
        }

        if (isLunar && lunarDate != null) {
            val leapPrefix = if (lunarDate.isLeapMonth) "闰" else ""
            parts.add("农历 ${lunarDate.year}年${leapPrefix}${lunarDate.month}月${lunarDate.day}日")
        } else {
            parts.add("${targetSolarDate.year}年${targetSolarDate.month}月${targetSolarDate.day}日")
        }

        val repeatLabel = formatRepeatRuleAccessibility(repeatRule)
        if (repeatLabel != null) {
            parts.add(repeatLabel)
        }

        parts.add(getCategoryAccessibilityName(category))

        if (isPinned) {
            parts.add("已置顶")
        }

        return parts.joinToString("，")
    }

    /**
     * Convenience overload for [HomeEventUiModel].
     */
    fun buildEventCardAccessibilityDescription(uiModel: HomeEventUiModel): String {
        val event = uiModel.event
        val isLunar = event.eventDate is EventDate.Lunar
        val lunarDate = (event.eventDate as? EventDate.Lunar)?.date

        return buildEventCardAccessibilityDescription(
            title = event.title,
            daysCount = uiModel.daysCount,
            status = uiModel.status,
            targetSolarDate = uiModel.targetSolarDate,
            category = event.category,
            isPinned = uiModel.isPinned,
            repeatRule = event.repeatRule,
            isLunar = isLunar,
            lunarDate = lunarDate,
        )
    }

    /**
     * Builds semantic speech for the Event Detail hero card.
     */
    fun buildDetailHeroAccessibilityDescription(
        status: CountdownDisplayStatus,
        heroNumberText: String,
        heroUnitText: String,
        targetDateDescription: String,
    ): String = when (status) {
        CountdownDisplayStatus.TODAY -> "今天，目标日期：$targetDateDescription"
        CountdownDisplayStatus.COUNTDOWN -> "倒计时 $heroNumberText$heroUnitText，目标日期：$targetDateDescription"
        CountdownDisplayStatus.PAST -> "已过去 $heroNumberText 天，起始日期：$targetDateDescription"
    }

    /**
     * Description for Home list/grid view mode toggle button.
     */
    fun buildViewModeToggleDescription(currentMode: HomeViewMode): String = when (currentMode) {
        HomeViewMode.LIST -> "切换为网格视图"
        HomeViewMode.GRID -> "切换为列表视图"
    }

    /**
     * Description for event order move up action.
     */
    fun buildEventOrderMoveUpDescription(title: String): String =
        "将「$title」向上移动"

    /**
     * Description for event order move down action.
     */
    fun buildEventOrderMoveDownDescription(title: String): String =
        "将「$title」向下移动"

    /**
     * State description for item position in reorderable list.
     */
    fun buildEventOrderItemStateDescription(index: Int, totalCount: Int): String =
        "第 ${index + 1} 项，共 $totalCount 项"

    /**
     * Description for event order item information card.
     */
    fun buildEventOrderItemDescription(
        title: String,
        index: Int,
        totalCount: Int,
        isPinned: Boolean,
    ): String = "第 ${index + 1} 项，共 $totalCount 项：$title${if (isPinned) "，已置顶" else ""}"

    /**
     * Announcement after an event is reordered.
     */
    fun buildEventOrderMovedAnnouncement(title: String, newIndex: Int): String =
        "已将「$title」移动至第 ${newIndex + 1} 项"

    /**
     * State description for tactile haptics master switch.
     */
    fun buildSettingsHapticsStateDescription(enabled: Boolean): String =
        if (enabled) "开启，按键与滚轮微震反馈" else "关闭，已静音"

    /**
     * Structured description for editor field entries.
     */
    fun buildEditorFieldAccessibilityDescription(
        fieldName: String,
        currentValue: String,
        actionHint: String = "点击修改",
    ): String = "$fieldName，当前为 $currentValue，$actionHint"
}
