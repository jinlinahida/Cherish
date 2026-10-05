package com.cherish.app.date.lunar

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate

/**
 * Adapter interface decoupling domain date logic from the underlying calendar engine.
 */
interface LunarCalendar {
    /**
     * Converts a Gregorian (Solar) date to a traditional Chinese Lunar date.
     */
    fun solarToLunar(date: SolarDate): LunarDate

    /**
     * Converts a traditional Chinese Lunar date to a Gregorian (Solar) date.
     */
    fun lunarToSolar(date: LunarDate): SolarDate

    /**
     * Returns the intercalary leap month (1..12) for the given lunar year, or 0 if no leap month.
     */
    fun getLeapMonth(lunarYear: Int): Int

    /**
     * Returns the number of days in the specified lunar month (typically 29 or 30).
     */
    fun getDaysInMonth(lunarYear: Int, lunarMonth: Int, isLeapMonth: Boolean): Int
}
