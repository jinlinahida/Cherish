package com.cherish.app.detail.mapper

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDetailMapperTest {

    private val referenceToday = SolarDate(2026, 10, 5)

    @Test
    fun `maps future solar event to COUNTDOWN status with hero details`() {
        val event = CountdownEvent(
            id = "solar-future",
            title = "妈妈生日",
            emoji = "🎂",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
            repeatRule = RepeatRule.None,
            isPinned = true,
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertEquals(10, uiModel.daysCount)
        assertEquals("10", uiModel.heroNumberText)
        assertEquals("天", uiModel.heroUnitText)
        assertEquals("2026年10月15日", uiModel.targetDateDescription)
        assertEquals("公历 2026年10月15日", uiModel.calendarTypeDescription)
        assertEquals("不重复", uiModel.recurrenceDescription)
        assertEquals("生日", uiModel.categoryDescription)
        assertTrue(uiModel.isPinned)
    }

    @Test
    fun `maps today event to TODAY status with special hero label`() {
        val event = CountdownEvent(
            id = "solar-today",
            title = "今天的重要时刻",
            emoji = "🌟",
            eventDate = EventDate.Solar(referenceToday),
            repeatRule = RepeatRule.None,
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.TODAY, uiModel.status)
        assertEquals(0, uiModel.daysCount)
        assertEquals("今天", uiModel.heroNumberText)
        assertEquals("TODAY", uiModel.heroUnitText)
    }

    @Test
    fun `maps past non-recurring event to PAST status with days count`() {
        val event = CountdownEvent(
            id = "solar-past",
            title = "毕业纪念",
            emoji = "🎓",
            category = EventCategory.LIFE,
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
            repeatRule = RepeatRule.None,
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.PAST, uiModel.status)
        assertEquals(4, uiModel.daysCount)
        assertEquals("4", uiModel.heroNumberText)
        assertEquals("天前", uiModel.heroUnitText)
        assertEquals("生活", uiModel.categoryDescription)
    }

    @Test
    fun `maps lunar recurring event calculating next occurrence`() {
        // Lunar 2026-08-15 is Solar 2026-09-25, which is before referenceToday (2026-10-05)
        // With yearly repeat, next occurrence rolls over to 2027
        val event = CountdownEvent(
            id = "lunar-midautumn",
            title = "中秋佳节",
            emoji = "🥮",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(LunarDate(2026, 8, 15)),
            repeatRule = RepeatRule.Yearly,
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertTrue(uiModel.daysCount > 0)
        assertEquals("每年重复", uiModel.recurrenceDescription)
        assertEquals("节日", uiModel.categoryDescription)
        assertTrue(uiModel.calendarTypeDescription.contains("农历 2026年8月15日"))
    }

    @Test
    fun `maps custom recurrence rule interval and unit correctly`() {
        val event = CountdownEvent(
            id = "custom-repeat",
            title = "项目双周迭代",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
            repeatRule = RepeatRule.Custom(interval = 2, unit = RepeatUnit.WEEK),
            category = EventCategory.WORK,
        )

        val uiModel = EventDetailMapper.toUiModel(event, referenceToday)

        assertEquals("每 2 周", uiModel.recurrenceDescription)
        assertEquals("工作", uiModel.categoryDescription)
    }
}
