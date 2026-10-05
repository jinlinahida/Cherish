package com.cherish.app.date.model

import java.time.LocalDate

/**
 * Pure Kotlin representation of a Gregorian (Solar) date.
 */
data class SolarDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<SolarDate> {

    init {
        require(year in 1..9999) { "Year must be between 1 and 9999, was: $year" }
        require(month in 1..12) { "Month must be between 1 and 12, was: $month" }
        require(day in 1..31) { "Day must be between 1 and 31, was: $day" }
    }

    fun toLocalDate(): LocalDate = LocalDate.of(year, month, day)

    override fun compareTo(other: SolarDate): Int {
        val yearDiff = year.compareTo(other.year)
        if (yearDiff != 0) return yearDiff
        val monthDiff = month.compareTo(other.month)
        if (monthDiff != 0) return monthDiff
        return day.compareTo(other.day)
    }

    override fun toString(): String = "%04d-%02d-%02d".format(year, month, day)

    companion object {
        fun fromLocalDate(localDate: LocalDate): SolarDate =
            SolarDate(localDate.year, localDate.monthValue, localDate.dayOfMonth)
    }
}
