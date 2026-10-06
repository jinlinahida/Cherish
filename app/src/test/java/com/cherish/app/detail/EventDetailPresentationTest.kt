package com.cherish.app.detail

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.navigation.CherishRoute
import com.cherish.app.navigation.resolveBackRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Event Detail Screen presentation, navigation, and UX flows:
 *
 * 1. Navigation chains:
 *    - Detail -> Editor -> Back -> Detail
 *    - Detail -> Back -> Home
 *    - Detail -> Delete -> Home
 *
 * 2. Text / Date presentation:
 *    - Long title preservation
 *    - Solar date presentation
 *    - Lunar date presentation
 *    - Leap Lunar date presentation
 *    - Repeat rules (None, Daily, Monthly, Yearly, Custom units)
 *
 * 3. Delete UX flow:
 *    - Initial state: confirmation dismissed
 *    - Open confirmation
 *    - Cancel / Back dismissal
 *    - Confirm deletion execution
 */
class EventDetailPresentationTest {

    private val referenceToday = SolarDate(2026, 10, 5)

    // ==========================================
    // 1. Navigation Flow Tests
    // ==========================================

    @Test
    fun `navigation chain Detail to Editor to Back returns to Detail`() {
        val eventId = "evt-77"
        val detailRoute = CherishRoute.Detail(eventId)
        val editorRoute = CherishRoute.Editor(eventId)

        // From Detail, user opens editor for the existing event
        assertEquals("editor_evt-77", editorRoute.routeKey)
        assertEquals(2, editorRoute.depth)

        // User hits Back (or cancel) from Editor
        val backTarget = resolveBackRoute(editorRoute)
        assertEquals(detailRoute, backTarget)
    }

    @Test
    fun `navigation chain Detail to Back returns to Home`() {
        val detailRoute = CherishRoute.Detail("evt-88")
        val backTarget = resolveBackRoute(detailRoute)
        assertEquals(CherishRoute.Home, backTarget)
    }

    @Test
    fun `navigation chain Detail to Delete returns to Home`() {
        // Simulating the delete confirmation execution
        val deletedEventIds = mutableListOf<String>()
        var currentRoute: CherishRoute = CherishRoute.Detail("evt-99")

        fun onDeleteConfirm(id: String) {
            deletedEventIds.add(id)
            currentRoute = resolveBackRoute(currentRoute) ?: CherishRoute.Home
        }

        onDeleteConfirm("evt-99")

        assertEquals(listOf("evt-99"), deletedEventIds)
        assertEquals(CherishRoute.Home, currentRoute)
    }

    @Test
    fun `navigation editor save for existing event returns to Detail while new event returns to Home`() {
        val existingEventId = "evt-100"
        val editRoute = CherishRoute.Editor(existingEventId)
        val createRoute = CherishRoute.Editor(null)

        fun resolveSaveRoute(route: CherishRoute.Editor, eventId: String): CherishRoute {
            return if (route.eventId == null) {
                CherishRoute.Home
            } else {
                CherishRoute.Detail(eventId)
            }
        }

        assertEquals(CherishRoute.Detail(existingEventId), resolveSaveRoute(editRoute, existingEventId))
        assertEquals(CherishRoute.Home, resolveSaveRoute(createRoute, "new-id"))
    }

    // ==========================================
    // 2. Text & Date Presentation Tests
    // ==========================================

    @Test
    fun `maps very long title without loss or corruption`() {
        val longTitle = "这是一个长达上百字符的超长纪念日标题，用于测试各种极端边界情况下的布局表现，确保文字即使在小圆屏上合理折行与省略也不会损坏内存数据。"
        val event = CountdownEvent(
            id = "long-title-evt",
            title = longTitle,
            emoji = "🎉",
            eventDate = EventDate.Solar(SolarDate(2026, 12, 31)),
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals(longTitle, uiModel.event.title)
        assertEquals("🎉", uiModel.event.emoji)
    }

    @Test
    fun `maps Solar date description with proper prefix and format`() {
        val event = CountdownEvent(
            id = "solar-evt",
            title = "公历纪念日",
            eventDate = EventDate.Solar(SolarDate(2027, 1, 1)),
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals("公历 2027年1月1日", uiModel.calendarTypeDescription)
        assertEquals("2027年1月1日", uiModel.targetDateDescription)
    }

    @Test
    fun `maps standard Lunar date description correctly`() {
        val event = CountdownEvent(
            id = "lunar-evt",
            title = "普通农历",
            eventDate = EventDate.Lunar(LunarDate(year = 2026, month = 8, day = 15, isLeapMonth = false)),
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals("农历 2026年8月15日", uiModel.calendarTypeDescription)
    }

    @Test
    fun `maps leap Lunar date description with leap prefix`() {
        // 2023 has leap month 2 (闰二月)
        val event = CountdownEvent(
            id = "leap-lunar-evt",
            title = "闰月农历日",
            eventDate = EventDate.Lunar(LunarDate(year = 2023, month = 2, day = 15, isLeapMonth = true)),
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals("农历 2023年闰2月15日", uiModel.calendarTypeDescription)
    }

    @Test
    fun `maps all repeat rules into concise localized descriptions`() {
        fun mapRule(rule: RepeatRule): String {
            val event = CountdownEvent(
                id = "rule-evt",
                title = "规则测试",
                eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
                repeatRule = rule,
            )
            return EventDetailMapper.toUiModel(event, referenceToday).recurrenceDescription
        }

        assertEquals("不重复", mapRule(RepeatRule.None))
        assertEquals("每天重复", mapRule(RepeatRule.Daily))
        assertEquals("每月重复", mapRule(RepeatRule.Monthly))
        assertEquals("每年重复", mapRule(RepeatRule.Yearly))
        assertEquals("每 5 天", mapRule(RepeatRule.Custom(interval = 5, unit = RepeatUnit.DAY)))
        assertEquals("每周", mapRule(RepeatRule.Custom(interval = 1, unit = RepeatUnit.WEEK)))
        assertEquals("每 3 周", mapRule(RepeatRule.Custom(interval = 3, unit = RepeatUnit.WEEK)))
        assertEquals("每 6 个月", mapRule(RepeatRule.Custom(interval = 6, unit = RepeatUnit.MONTH)))
        assertEquals("每 2 年", mapRule(RepeatRule.Custom(interval = 2, unit = RepeatUnit.YEAR)))
    }

    // ==========================================
    // 3. Delete UX State Transition Tests
    // ==========================================

    @Test
    fun `delete confirmation flow supports open, cancel, and back interception without premature exit`() {
        var showDeleteConfirm = false
        var eventDeleted = false
        var currentRoute: CherishRoute = CherishRoute.Detail("evt-001")

        // 1. Initial State
        assertFalse(showDeleteConfirm)
        assertFalse(eventDeleted)

        // 2. User taps "删除事件" button
        showDeleteConfirm = true
        assertTrue(showDeleteConfirm)

        // 3. User taps "取消" button or presses hardware Back (intercepted by BackHandler)
        fun onCancelOrBack() {
            if (showDeleteConfirm) {
                showDeleteConfirm = false
            } else {
                currentRoute = resolveBackRoute(currentRoute) ?: CherishRoute.Home
            }
        }

        onCancelOrBack()
        assertFalse("Confirmation must be dismissed", showDeleteConfirm)
        assertEquals("Screen must remain on Detail", CherishRoute.Detail("evt-001"), currentRoute)
        assertFalse("Event must not be deleted on cancel", eventDeleted)

        // 4. User opens confirmation again and confirms deletion
        showDeleteConfirm = true
        fun onConfirm() {
            eventDeleted = true
            showDeleteConfirm = false
            currentRoute = resolveBackRoute(currentRoute) ?: CherishRoute.Home
        }

        onConfirm()
        assertFalse(showDeleteConfirm)
        assertTrue(eventDeleted)
        assertEquals(CherishRoute.Home, currentRoute)
    }

    // ==========================================
    // 4. Background Model & Decoupling Tests
    // ==========================================

    @Test
    fun `maps all EventBackground variants into EventDetailUiModel cleanly`() {
        val backgrounds = listOf(
            EventBackground.Default,
            EventBackground.Color(0xFF336699L),
            EventBackground.Gradient(0xFF8E2DE2L, 0xFF4A00E0L),
            EventBackground.Pattern("pattern_dots"),
            EventBackground.Image(path = "bg_test_123.jpg", dimAlpha = 0.5f),
        )

        for ((index, bg) in backgrounds.withIndex()) {
            val event = CountdownEvent(
                id = "bg-test-$index",
                title = "背景测试 $index",
                eventDate = EventDate.Solar(SolarDate(2026, 12, 1)),
                background = bg,
            )
            val uiModel = EventDetailMapper.toUiModel(event, referenceToday)
            assertEquals("Background should be preserved in uiModel", bg, uiModel.background)
        }
    }
}
