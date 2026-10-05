package com.cherish.app.home.preview

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate

/**
 * Curated sample events for Compose Previews, design reviews, and local verification.
 *
 * NOTE: These events are strictly for preview / demonstration and are NEVER written
 * into production storage automatically.
 */
object DemoEvents {

    fun samples(referenceToday: SolarDate = SolarDate(2026, 10, 5)): List<CountdownEvent> = listOf(
        // 1. Pinned Lunar Holiday
        CountdownEvent(
            id = "demo-midautumn",
            title = "中秋佳节",
            emoji = "🥮",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(LunarDate(2026, 8, 15)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFFFA000),
            notes = "阖家团圆赏月",
        ),
        // 2. Solar Birthday with yearly repeat
        CountdownEvent(
            id = "demo-birthday",
            title = "生日快乐",
            emoji = "🎂",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 11, 20)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFE91E63),
        ),
        // 3. Event happening TODAY
        CountdownEvent(
            id = "demo-today",
            title = "今天的重要时刻",
            emoji = "🌟",
            category = EventCategory.LIFE,
            eventDate = EventDate.Solar(referenceToday),
            repeatRule = RepeatRule.None,
            isPinned = false,
        ),
        // 4. Future milestone
        CountdownEvent(
            id = "demo-newyear",
            title = "2027 元旦倒计时",
            emoji = "🎆",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Solar(SolarDate(2027, 1, 1)),
            repeatRule = RepeatRule.None,
            isPinned = false,
        ),
        // 5. Lunar Spring Festival
        CountdownEvent(
            id = "demo-spring",
            title = "农历除夕与春节",
            emoji = "🧨",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(LunarDate(2027, 1, 1)),
            repeatRule = RepeatRule.Yearly,
            isPinned = false,
        ),
        // 6. Lunar leap month event
        CountdownEvent(
            id = "demo-leap",
            title = "农历闰月纪念",
            emoji = "🎋",
            category = EventCategory.ANNIVERSARY,
            eventDate = EventDate.Lunar(LunarDate(2028, 5, 5, isLeapMonth = true)),
            repeatRule = RepeatRule.Yearly,
            isPinned = false,
        ),
        // 7. Custom repeat rule (Sprint review every 2 weeks)
        CountdownEvent(
            id = "demo-sprint",
            title = "项目双周迭代验收",
            emoji = "🚀",
            category = EventCategory.WORK,
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
            repeatRule = RepeatRule.Custom(interval = 2, unit = RepeatUnit.WEEK),
            isPinned = false,
        ),
        // 8. Long title to test truncation and small screen resilience
        CountdownEvent(
            id = "demo-long",
            title = "这是一个超长标题用来验证小屏幕自动省略展示效果",
            emoji = "📝",
            category = EventCategory.OTHER,
            eventDate = EventDate.Solar(SolarDate(2026, 12, 31)),
            repeatRule = RepeatRule.None,
            isPinned = false,
        ),
        // 9. Past event with days ago
        CountdownEvent(
            id = "demo-past",
            title = "国庆节假期",
            emoji = "🇨🇳",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
            repeatRule = RepeatRule.None,
            isPinned = false,
        ),
    )
}
