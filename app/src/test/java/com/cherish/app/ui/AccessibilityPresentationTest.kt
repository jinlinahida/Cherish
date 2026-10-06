package com.cherish.app.ui

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeEventUiModel
import com.cherish.app.home.model.HomeViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying TalkBack accessibility semantic presentation builders:
 * - Event card descriptions across pinned, unpinned, today, future, past, lunar, solar, repeat combinations
 * - Curated emoji naming and category naming
 * - Event ordering action and position descriptions
 * - Settings toggle and mode switch descriptions
 * - Long title and empty title edge cases
 */
class AccessibilityPresentationTest {

    @Test
    fun `pinned future solar event produces unified cohesive speech summary`() {
        val result = AccessibilityPresentation.buildEventCardAccessibilityDescription(
            title = "Stella",
            daysCount = 99,
            status = CountdownDisplayStatus.COUNTDOWN,
            targetSolarDate = SolarDate(2027, 1, 3),
            category = EventCategory.BIRTHDAY,
            isPinned = true,
            repeatRule = RepeatRule.None,
            isLunar = false,
        )

        assertEquals("Stella，99 天后，2027年1月3日，生日，已置顶", result)
    }

    @Test
    fun `unpinned today event with yearly repeat produces expected speech summary`() {
        val result = AccessibilityPresentation.buildEventCardAccessibilityDescription(
            title = "结婚纪念日",
            daysCount = 0,
            status = CountdownDisplayStatus.TODAY,
            targetSolarDate = SolarDate(2026, 10, 6),
            category = EventCategory.ANNIVERSARY,
            isPinned = false,
            repeatRule = RepeatRule.Yearly,
            isLunar = false,
        )

        assertEquals("结婚纪念日，今天，2026年10月6日，每年重复，纪念日", result)
    }

    @Test
    fun `overdue past event produces overdue status speech`() {
        val result = AccessibilityPresentation.buildEventCardAccessibilityDescription(
            title = "项目截稿",
            daysCount = 5,
            status = CountdownDisplayStatus.PAST,
            targetSolarDate = SolarDate(2026, 10, 1),
            category = EventCategory.WORK,
            isPinned = false,
            repeatRule = RepeatRule.None,
            isLunar = false,
        )

        assertEquals("项目截稿，已过 5 天，2026年10月1日，工作", result)
    }

    @Test
    fun `lunar calendar event includes lunar date speech with leap month indicator`() {
        val leapLunar = LunarDate(year = 2026, month = 6, day = 15, isLeapMonth = true)
        val result = AccessibilityPresentation.buildEventCardAccessibilityDescription(
            title = "外婆生日",
            daysCount = 42,
            status = CountdownDisplayStatus.COUNTDOWN,
            targetSolarDate = SolarDate(2026, 8, 1),
            category = EventCategory.BIRTHDAY,
            isPinned = true,
            repeatRule = RepeatRule.Yearly,
            isLunar = true,
            lunarDate = leapLunar,
        )

        assertEquals("外婆生日，42 天后，农历 2026年闰6月15日，每年重复，生日，已置顶", result)
    }

    @Test
    fun `custom repeat rules format correct time interval units`() {
        assertEquals("每天重复", AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Daily))
        assertEquals(
            "每周重复",
            AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Custom(1, RepeatUnit.WEEK)),
        )
        assertEquals("每月重复", AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Monthly))
        assertEquals("每年重复", AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Yearly))
        assertEquals(
            "每 2 周 重复",
            AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Custom(2, RepeatUnit.WEEK)),
        )
        assertEquals(
            "每 30 天 重复",
            AccessibilityPresentation.formatRepeatRuleAccessibility(RepeatRule.Custom(30, RepeatUnit.DAY)),
        )
    }

    @Test
    fun `home event ui model convenience overload correctly converts domain models`() {
        val event = CountdownEvent(
            id = "test-1",
            title = "全量回归测试",
            emoji = "🧪",
            eventDate = EventDate.Solar(SolarDate(2026, 12, 31)),
            repeatRule = RepeatRule.None,
            category = EventCategory.WORK,
            isPinned = true,
            background = EventBackground.Default,
        )
        val uiModel = HomeEventUiModel(
            event = event,
            targetSolarDate = SolarDate(2026, 12, 31),
            daysCount = 86,
            status = CountdownDisplayStatus.COUNTDOWN,
            unitLabel = "DAYS",
            targetDateFormatted = "2026.12.31",
            isPinned = true,
            background = EventBackground.Default,
        )

        val description = AccessibilityPresentation.buildEventCardAccessibilityDescription(uiModel)
        assertEquals("全量回归测试，86 天后，2026年12月31日，工作，已置顶", description)
    }

    @Test
    fun `blank or whitespace event title falls back gracefully`() {
        val result = AccessibilityPresentation.buildEventCardAccessibilityDescription(
            title = "   ",
            daysCount = 1,
            status = CountdownDisplayStatus.COUNTDOWN,
            targetSolarDate = SolarDate(2026, 10, 7),
            category = EventCategory.OTHER,
            isPinned = false,
        )

        assertTrue(result.startsWith("未命名事件，1 天后"))
    }

    @Test
    fun `preset emojis map to natural Chinese speech names while unknown emojis pass through`() {
        assertEquals("蛋糕", AccessibilityPresentation.getEmojiAccessibilityName("🎂"))
        assertEquals("爱心", AccessibilityPresentation.getEmojiAccessibilityName("💗"))
        assertEquals("毕业帽", AccessibilityPresentation.getEmojiAccessibilityName("🎓"))
        assertEquals("飞机", AccessibilityPresentation.getEmojiAccessibilityName("✈️"))
        assertEquals("烟花", AccessibilityPresentation.getEmojiAccessibilityName("🎆"))
        assertEquals("星星", AccessibilityPresentation.getEmojiAccessibilityName("🌟"))
        assertEquals("公文包", AccessibilityPresentation.getEmojiAccessibilityName("💼"))
        assertEquals("沙滩", AccessibilityPresentation.getEmojiAccessibilityName("🏖️"))
        assertEquals("汽车", AccessibilityPresentation.getEmojiAccessibilityName("🚗"))
        assertEquals("礼物", AccessibilityPresentation.getEmojiAccessibilityName("🎁"))
        assertEquals("日历", AccessibilityPresentation.getEmojiAccessibilityName("📅"))
        assertEquals("戒指", AccessibilityPresentation.getEmojiAccessibilityName("💍"))
        assertEquals("🛸", AccessibilityPresentation.getEmojiAccessibilityName("🛸"))
    }

    @Test
    fun `category accessibility names contain no emoji or punctuation`() {
        assertEquals("通用", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.GENERAL))
        assertEquals("生日", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.BIRTHDAY))
        assertEquals("纪念日", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.ANNIVERSARY))
        assertEquals("节日", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.HOLIDAY))
        assertEquals("工作", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.WORK))
        assertEquals("生活", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.LIFE))
        assertEquals("其他", AccessibilityPresentation.getCategoryAccessibilityName(EventCategory.OTHER))
    }

    @Test
    fun `event ordering move descriptions clearly identify event and intent`() {
        assertEquals("将「生日」向上移动", AccessibilityPresentation.buildEventOrderMoveUpDescription("生日"))
        assertEquals("将「生日」向下移动", AccessibilityPresentation.buildEventOrderMoveDownDescription("生日"))
        assertEquals(
            "第 1 项，共 5 项：生日，已置顶",
            AccessibilityPresentation.buildEventOrderItemDescription("生日", 0, 5, isPinned = true),
        )
        assertEquals(
            "第 3 项，共 5 项：纪念日",
            AccessibilityPresentation.buildEventOrderItemDescription("纪念日", 2, 5, isPinned = false),
        )
    }

    @Test
    fun `settings descriptions format toggle and mode change semantics correctly`() {
        assertEquals(
            "开启，按键与滚轮微震反馈",
            AccessibilityPresentation.buildSettingsHapticsStateDescription(true),
        )
        assertEquals(
            "关闭，已静音",
            AccessibilityPresentation.buildSettingsHapticsStateDescription(false),
        )

        assertEquals(
            "切换为网格视图",
            AccessibilityPresentation.buildViewModeToggleDescription(HomeViewMode.LIST),
        )
        assertEquals(
            "切换为列表视图",
            AccessibilityPresentation.buildViewModeToggleDescription(HomeViewMode.GRID),
        )

        assertEquals(
            "目标日期，当前为 公历 2026.10.06，点击修改日期",
            AccessibilityPresentation.buildEditorFieldAccessibilityDescription("目标日期", "公历 2026.10.06", "点击修改日期"),
        )
    }

    @Test
    fun `detail hero descriptions describe countdown, today, and past states`() {
        assertEquals(
            "今天，目标日期：2026年10月6日",
            AccessibilityPresentation.buildDetailHeroAccessibilityDescription(
                CountdownDisplayStatus.TODAY,
                "0",
                "DAYS",
                "2026年10月6日",
            ),
        )
        assertEquals(
            "倒计时 99DAYS，目标日期：2027年1月3日",
            AccessibilityPresentation.buildDetailHeroAccessibilityDescription(
                CountdownDisplayStatus.COUNTDOWN,
                "99",
                "DAYS",
                "2027年1月3日",
            ),
        )
        assertEquals(
            "已过去 10 天，起始日期：2026年9月26日",
            AccessibilityPresentation.buildDetailHeroAccessibilityDescription(
                CountdownDisplayStatus.PAST,
                "10",
                "DAYS",
                "2026年9月26日",
            ),
        )
    }
}
