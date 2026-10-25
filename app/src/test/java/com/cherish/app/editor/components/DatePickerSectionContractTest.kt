package com.cherish.app.editor.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatePickerSectionContractTest {

    @Test
    fun `date picker column enum defines distinct columns for year month day`() {
        val columns = DatePickerColumn.values()
        assertEquals(3, columns.size)
        assertTrue(columns.contains(DatePickerColumn.YEAR))
        assertTrue(columns.contains(DatePickerColumn.MONTH))
        assertTrue(columns.contains(DatePickerColumn.DAY))
    }

    @Test
    fun `date picker column enum ordinal and naming order`() {
        assertEquals("YEAR", DatePickerColumn.YEAR.name)
        assertEquals("MONTH", DatePickerColumn.MONTH.name)
        assertEquals("DAY", DatePickerColumn.DAY.name)
    }
}
