package com.cherish.app.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.ShirokoWearToggleCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearWheelPicker
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi

/**
 * Dedicated date picker screen for Wear OS.
 *
 * Provides:
 * - Segmented toggle between Solar (公历) and Lunar (农历)
 * - Three synchronized [ShirokoWearWheelPicker] columns for Year, Month, and Day
 * - Automatic leap-year and month-length clamping
 * - Lunar intercalary leap month (闰月) toggle when supported by the year
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun DatePickerSection(
    isLunar: Boolean,
    solarDate: SolarDate,
    lunarDate: LunarDate,
    onDateTypeChange: (Boolean) -> Unit,
    onSolarDateChange: (SolarDate) -> Unit,
    onLunarDateChange: (LunarDate) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    calendar: LunarCalendar = remember { DefaultLunarCalendar() },
) {
    val years = remember { (1990..2050).toList() }
    val months = remember { (1..12).toList() }

    val currentYear = if (isLunar) lunarDate.year else solarDate.year
    val currentMonth = if (isLunar) lunarDate.month else solarDate.month
    val currentDay = if (isLunar) lunarDate.day else solarDate.day

    val maxDays = remember(isLunar, currentYear, currentMonth, lunarDate.isLeapMonth) {
        if (isLunar) {
            DatePickerSanitizer.getDaysInLunarMonth(currentYear, currentMonth, lunarDate.isLeapMonth, calendar)
        } else {
            DatePickerSanitizer.getDaysInSolarMonth(currentYear, currentMonth)
        }
    }
    val days = remember(maxDays) { (1..maxDays).toList() }

    val yearIndex = years.indexOf(currentYear).coerceAtLeast(0)
    val monthIndex = months.indexOf(currentMonth).coerceAtLeast(0)
    val dayIndex = days.indexOf(currentDay.coerceAtMost(maxDays)).coerceAtLeast(0)

    val actualLunarLeapMonth = remember(lunarDate.year) { calendar.getLeapMonth(lunarDate.year) }
    val isLeapMonthSupported = isLunar && actualLunarLeapMonth == lunarDate.month

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(text = "设置日期")
        }

        // Calendar type toggle: 公历 / 农历
        item(key = "type_selector") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ShirokoWearSelectableButton(
                    selected = !isLunar,
                    onClick = { onDateTypeChange(false) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text("公历", style = MaterialTheme.typography.labelSmall)
                }
                ShirokoWearSelectableButton(
                    selected = isLunar,
                    onClick = { onDateTypeChange(true) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text("农历", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Date Wheel Pickers
        item(key = "wheel_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Year Wheel
                ShirokoWearWheelPicker(
                    items = years,
                    selectedIndex = yearIndex,
                    onSelectedIndexChanged = { idx ->
                        val selectedYear = years[idx]
                        if (isLunar) {
                            val sanitized = DatePickerSanitizer.sanitizeLunar(
                                selectedYear,
                                lunarDate.month,
                                lunarDate.day,
                                lunarDate.isLeapMonth,
                                calendar,
                            )
                            onLunarDateChange(sanitized)
                        } else {
                            val sanitized = DatePickerSanitizer.sanitizeSolar(
                                selectedYear,
                                solarDate.month,
                                solarDate.day,
                            )
                            onSolarDateChange(sanitized)
                        }
                    },
                    labelProvider = { "$it" },
                    modifier = Modifier.weight(1.3f),
                )

                // Month Wheel
                ShirokoWearWheelPicker(
                    items = months,
                    selectedIndex = monthIndex,
                    onSelectedIndexChanged = { idx ->
                        val selectedMonth = months[idx]
                        if (isLunar) {
                            val sanitized = DatePickerSanitizer.sanitizeLunar(
                                lunarDate.year,
                                selectedMonth,
                                lunarDate.day,
                                lunarDate.isLeapMonth,
                                calendar,
                            )
                            onLunarDateChange(sanitized)
                        } else {
                            val sanitized = DatePickerSanitizer.sanitizeSolar(
                                solarDate.year,
                                selectedMonth,
                                solarDate.day,
                            )
                            onSolarDateChange(sanitized)
                        }
                    },
                    labelProvider = { "%02d月".format(it) },
                    modifier = Modifier.weight(1.0f),
                )

                // Day Wheel
                ShirokoWearWheelPicker(
                    items = days,
                    selectedIndex = dayIndex,
                    onSelectedIndexChanged = { idx ->
                        val selectedDay = days[idx]
                        if (isLunar) {
                            val sanitized = DatePickerSanitizer.sanitizeLunar(
                                lunarDate.year,
                                lunarDate.month,
                                selectedDay,
                                lunarDate.isLeapMonth,
                                calendar,
                            )
                            onLunarDateChange(sanitized)
                        } else {
                            val sanitized = DatePickerSanitizer.sanitizeSolar(
                                solarDate.year,
                                solarDate.month,
                                selectedDay,
                            )
                            onSolarDateChange(sanitized)
                        }
                    },
                    labelProvider = { "%02d日".format(it) },
                    modifier = Modifier.weight(1.0f),
                )
            }
        }

        // Lunar Leap Month Toggle
        if (isLeapMonthSupported) {
            item(key = "lunar_leap_toggle") {
                ShirokoWearToggleCard(
                    checked = lunarDate.isLeapMonth,
                    onCheckedChange = { checked ->
                        val sanitized = DatePickerSanitizer.sanitizeLunar(
                            lunarDate.year,
                            lunarDate.month,
                            lunarDate.day,
                            checked,
                            calendar,
                        )
                        onLunarDateChange(sanitized)
                    },
                    label = "闰月",
                    secondaryLabel = "当年农历闰${lunarDate.month}月",
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        // Preview label
        item(key = "date_preview") {
            val previewText = if (isLunar) {
                val leapPrefix = if (lunarDate.isLeapMonth) "闰" else ""
                "已选：农历 %04d年%s%02d月%02d日".format(lunarDate.year, leapPrefix, lunarDate.month, lunarDate.day)
            } else {
                "已选：公历 %04d年%02d月%02d日".format(solarDate.year, solarDate.month, solarDate.day)
            }
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodySmall,
                color = ShirokoWearTheme.colors.accentGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Confirm button
        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(onClick = onConfirm) {
                Text(
                    text = "确定",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShirokoWearTheme.colors.accentGold,
                )
            }
        }
    }
}
