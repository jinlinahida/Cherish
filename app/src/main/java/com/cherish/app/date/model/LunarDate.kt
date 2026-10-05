package com.cherish.app.date.model

/**
 * Pure Kotlin representation of a traditional Chinese Lunar date.
 *
 * @property year Lunar year (e.g. 2026)
 * @property month Lunar nominal month (1..12)
 * @property day Lunar day of month (1..30)
 * @property isLeapMonth Whether this month is an intercalary leap month (闰月)
 */
data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false,
) : Comparable<LunarDate> {

    init {
        require(year in 1..9999) { "Lunar year must be between 1 and 9999, was: $year" }
        require(month in 1..12) { "Lunar month must be between 1 and 12, was: $month" }
        require(day in 1..30) { "Lunar day must be between 1 and 30, was: $day" }
    }

    override fun compareTo(other: LunarDate): Int {
        val yearDiff = year.compareTo(other.year)
        if (yearDiff != 0) return yearDiff
        val monthDiff = month.compareTo(other.month)
        if (monthDiff != 0) return monthDiff
        val leapDiff = isLeapMonth.compareTo(other.isLeapMonth)
        if (leapDiff != 0) return leapDiff
        return day.compareTo(other.day)
    }

    override fun toString(): String {
        val leapPrefix = if (isLeapMonth) "闰" else ""
        return "农历%04d年%s%02d月%02d日".format(year, leapPrefix, month, day)
    }
}
