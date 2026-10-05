package com.cherish.app.date.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RepeatRuleTest {

    @Test
    fun testRuleInstantiations() {
        val none = RepeatRule.None
        val daily = RepeatRule.Daily
        val monthly = RepeatRule.Monthly
        val yearly = RepeatRule.Yearly
        val custom = RepeatRule.Custom(3, RepeatUnit.WEEK)

        assertNotNull(none)
        assertNotNull(daily)
        assertNotNull(monthly)
        assertNotNull(yearly)
        assertEquals(3, custom.interval)
        assertEquals(RepeatUnit.WEEK, custom.unit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCustomRuleRejectsNonPositiveInterval() {
        RepeatRule.Custom(0, RepeatUnit.DAY)
    }
}
