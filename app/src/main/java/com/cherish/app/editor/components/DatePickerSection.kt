package com.cherish.app.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * Identifier for the currently active/focused column in the date picker.
 */
enum class DatePickerColumn {
    YEAR,
    MONTH,
    DAY,
}

/**
 * Dedicated date picker screen for Wear OS adhering to the Samsung One UI Watch pattern.
 *
 * Design and interaction characteristics:
 * - Single Active Focus: Exactly one column (Year, Month, or Day) owns the focus at any time.
 * - Hardware Crown/Bezel & Gesture Routing: The Rotary crown and touch scrolling explicitly
 *   drive the currently focused column.
 * - Touch Conflict Elimination: Inactive columns are read-only previews that cleanly switch
 *   focus on tap, avoiding multiple scroll containers fighting for finger gestures.
 * - Non-nested Fixed Layout: No outer vertical scroll container prevents page jumping.
 * - Persistent Bottom Confirmation: Always accessible with a single tap.
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
    val haptics = rememberShirokoWearHaptics()
    val colors = ShirokoWearTheme.colors

    var focusedColumn by remember { mutableStateOf(DatePickerColumn.DAY) }

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

    val yearState = rememberLazyListState(initialFirstVisibleItemIndex = yearIndex)
    val monthState = rememberLazyListState(initialFirstVisibleItemIndex = monthIndex)
    val dayState = rememberLazyListState(initialFirstVisibleItemIndex = dayIndex)

    val focusRequester = remember { FocusRequester() }
    val activeScrollState = when (focusedColumn) {
        DatePickerColumn.YEAR -> yearState
        DatePickerColumn.MONTH -> monthState
        DatePickerColumn.DAY -> dayState
    }

    val rotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = activeScrollState,
        hapticFeedbackEnabled = true,
    )

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .requestFocusOnHierarchyActive()
            .focusRequester(focusRequester)
            .focusable()
            .rotaryScrollable(
                behavior = rotaryBehavior,
                focusRequester = focusRequester,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top: Calendar Type and Optional Leap Month Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShirokoWearSelectableButton(
                selected = !isLunar,
                onClick = {
                    haptics.tick()
                    onDateTypeChange(false)
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.RadioButton
                        selected = !isLunar
                        contentDescription = "公历${if (!isLunar) "，已选中" else ""}"
                    },
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                Text("公历", style = MaterialTheme.typography.labelSmall)
            }

            ShirokoWearSelectableButton(
                selected = isLunar,
                onClick = {
                    haptics.tick()
                    onDateTypeChange(true)
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.RadioButton
                        selected = isLunar
                        contentDescription = "农历${if (isLunar) "，已选中" else ""}"
                    },
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                Text("农历", style = MaterialTheme.typography.labelSmall)
            }

            if (isLeapMonthSupported) {
                ShirokoWearSelectableButton(
                    selected = lunarDate.isLeapMonth,
                    onClick = {
                        haptics.tick()
                        val sanitized = DatePickerSanitizer.sanitizeLunar(
                            lunarDate.year,
                            lunarDate.month,
                            lunarDate.day,
                            !lunarDate.isLeapMonth,
                            calendar,
                        )
                        onLunarDateChange(sanitized)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Checkbox
                            selected = lunarDate.isLeapMonth
                            contentDescription = "当年农历闰${lunarDate.month}月开关，当前${if (lunarDate.isLeapMonth) "开启" else "关闭"}"
                        },
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text(
                        text = if (lunarDate.isLeapMonth) "闰月" else "平月",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lunarDate.isLeapMonth) colors.accentGold else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Center: Three-column Wheel Picker with Single Active Focus
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableWheelColumn(
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
                isFocused = focusedColumn == DatePickerColumn.YEAR,
                onFocusRequest = {
                    if (focusedColumn != DatePickerColumn.YEAR) {
                        haptics.tick()
                        focusedColumn = DatePickerColumn.YEAR
                    }
                },
                labelProvider = { "$it" },
                lazyListState = yearState,
                columnName = "年份",
                modifier = Modifier.weight(1.3f),
            )

            FocusableWheelColumn(
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
                isFocused = focusedColumn == DatePickerColumn.MONTH,
                onFocusRequest = {
                    if (focusedColumn != DatePickerColumn.MONTH) {
                        haptics.tick()
                        focusedColumn = DatePickerColumn.MONTH
                    }
                },
                labelProvider = {
                    if (isLunar && lunarDate.isLeapMonth && it == lunarDate.month) {
                        "闰%02d月".format(it)
                    } else {
                        "%02d月".format(it)
                    }
                },
                lazyListState = monthState,
                columnName = "月份",
                modifier = Modifier.weight(1.0f),
            )

            FocusableWheelColumn(
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
                isFocused = focusedColumn == DatePickerColumn.DAY,
                onFocusRequest = {
                    if (focusedColumn != DatePickerColumn.DAY) {
                        haptics.tick()
                        focusedColumn = DatePickerColumn.DAY
                    }
                },
                labelProvider = { "%02d日".format(it) },
                lazyListState = dayState,
                columnName = "日期",
                modifier = Modifier.weight(1.0f),
            )
        }

        // Bottom: Subtle date preview and fixed confirm button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val previewText = if (isLunar) {
                val leapPrefix = if (lunarDate.isLeapMonth) "闰" else ""
                "农历 %04d年%s%02d月%02d日".format(lunarDate.year, leapPrefix, lunarDate.month, lunarDate.day)
            } else {
                "公历 %04d年%02d月%02d日".format(solarDate.year, solarDate.month, solarDate.day)
            }
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = colors.accentGold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(34.dp)
                    .semantics {
                        contentDescription = "确定日期设置，$previewText"
                    },
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                Text(
                    text = "确定",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentGold,
                )
            }
        }
    }
}

/**
 * A wheel column supporting Single Active Focus and Wear OS touch isolation.
 *
 * When [isFocused] is true:
 * - Highlights the row with a copper/gold slot border.
 * - Text scales up with full opacity.
 * - Vertical drags and rotary input interact with this column.
 *
 * When [isFocused] is false:
 * - Rendered as a dimmed read-only preview.
 * - Entire column area captures taps to seamlessly transition focus.
 */
@Composable
private fun <T> FocusableWheelColumn(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    isFocused: Boolean,
    onFocusRequest: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex),
    ),
    itemHeight: Dp = 34.dp,
    visibleItemsCount: Int = 3,
    columnName: String = "",
    labelProvider: (T) -> String = { it.toString() },
) {
    if (items.isEmpty()) return

    val haptics = rememberShirokoWearHaptics()
    val colors = ShirokoWearTheme.colors
    val totalHeight = itemHeight * visibleItemsCount
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    val centerIndexForCurrentLayout = {
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) {
            lazyListState.firstVisibleItemIndex
        } else {
            val centerOffset = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            visibleItems.minByOrNull { item ->
                abs((item.offset + item.size / 2) - centerOffset)
            }?.index ?: lazyListState.firstVisibleItemIndex
        }
    }

    // Only broadcast value changes when focused to prevent feedback loops
    LaunchedEffect(lazyListState, isFocused) {
        if (!isFocused) return@LaunchedEffect
        snapshotFlow { centerIndexForCurrentLayout() }
            .distinctUntilChanged()
            .filter { it in items.indices }
            .collect { centerIndex ->
                if (centerIndex != selectedIndex) {
                    onSelectedIndexChanged(centerIndex)
                    haptics.tick()
                }
            }
    }

    // Smoothly scroll to external selectedIndex when not currently being dragged by user
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && !lazyListState.isScrollInProgress) {
            if (centerIndexForCurrentLayout() != selectedIndex) {
                lazyListState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(totalHeight)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (!isFocused) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFocusRequest,
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${columnName}选择器"
                stateDescription = if (selectedIndex in items.indices) {
                    "${labelProvider(items[selectedIndex])}，${if (isFocused) "已激活" else "未激活，点按以选中"}"
                } else {
                    ""
                }
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        // Focus indicator slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isFocused) colors.wheelSlotBackground else Color.Transparent
                )
                .border(
                    width = if (isFocused) 1.2.dp else 0.dp,
                    color = if (isFocused) colors.accentCopper.copy(alpha = 0.75f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ),
        )

        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            userScrollEnabled = isFocused,
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                count = items.size,
                key = { index -> index },
            ) { index ->
                val isSelected by remember(index, selectedIndex) {
                    derivedStateOf { index == selectedIndex }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            scaleX = if (isFocused) {
                                if (isSelected) 1.08f else 0.88f
                            } else {
                                if (isSelected) 1.0f else 0.82f
                            }
                            scaleY = if (isFocused) {
                                if (isSelected) 1.08f else 0.88f
                            } else {
                                if (isSelected) 1.0f else 0.82f
                            }
                            alpha = if (isFocused) {
                                if (isSelected) 1.0f else 0.42f
                            } else {
                                if (isSelected) 0.50f else 0.20f
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelProvider(items[index]),
                        style = if (isSelected && isFocused) {
                            MaterialTheme.typography.titleMedium
                        } else if (isSelected) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        fontWeight = if (isSelected && isFocused) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected && isFocused) {
                            colors.accentGold
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Top and bottom dissolve gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.85f),
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )
    }
}
