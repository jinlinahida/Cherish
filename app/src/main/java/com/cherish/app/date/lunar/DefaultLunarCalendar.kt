package com.cherish.app.date.lunar

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.Solar
import kotlin.math.abs

/**
 * Standard implementation of [LunarCalendar] backed by cn.6tail:lunar.
 */
class DefaultLunarCalendar : LunarCalendar {

    override fun solarToLunar(date: SolarDate): LunarDate {
        val solar = Solar.fromYmd(date.year, date.month, date.day)
        val lunar = solar.lunar
        val isLeap = lunar.month < 0
        val nominalMonth = abs(lunar.month)
        return LunarDate(
            year = lunar.year,
            month = nominalMonth,
            day = lunar.day,
            isLeapMonth = isLeap,
        )
    }

    override fun lunarToSolar(date: LunarDate): SolarDate {
        val internalMonth = if (date.isLeapMonth) -date.month else date.month
        val lunar = Lunar(date.year, internalMonth, date.day)
        val solar = lunar.solar
        return SolarDate(
            year = solar.year,
            month = solar.month,
            day = solar.day,
        )
    }

    override fun getLeapMonth(lunarYear: Int): Int {
        val ly = com.nlf.calendar.LunarYear.fromYear(lunarYear)
        return ly.leapMonth
    }

    override fun getDaysInMonth(lunarYear: Int, lunarMonth: Int, isLeapMonth: Boolean): Int {
        val internalMonth = if (isLeapMonth) -lunarMonth else lunarMonth
        val lm = LunarMonth.fromYm(lunarYear, internalMonth) ?: return 30
        return lm.dayCount
    }
}
