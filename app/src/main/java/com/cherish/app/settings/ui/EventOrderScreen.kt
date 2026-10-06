package com.cherish.app.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.settings.model.EventReorderHelper
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * Screen allowing users to naturally reorder events via drag-and-drop gestures on Wear OS.
 *
 * Supported interactions:
 * - Instant drag by grabbing the right-hand grip handle.
 * - Long-press drag anywhere on the event card.
 * - Rotary crown scrolling when not dragging.
 * - Haptic feedback: impact on pickup, crisp graduation ticks on item swap, click on drop.
 * - TalkBack accessibility: Custom accessibility actions ("向上移动", "向下移动") on each card.
 */
@Composable
fun EventOrderScreen(
    events: List<CountdownEvent>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberShirokoWearHaptics()
    var localEvents by remember(events) { mutableStateOf(events) }

    var draggedEventId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    val startDrag: (String) -> Unit = { eventId ->
        haptics.impact(multiple = false)
        draggedEventId = eventId
        dragOffsetY = 0f
    }

    val updateDrag: (Float) -> Unit = { deltaY ->
        val activeId = draggedEventId
        if (activeId != null) {
            dragOffsetY += deltaY
            val currentIndex = localEvents.indexOfFirst { it.id == activeId }
            if (currentIndex != -1) {
                val effectiveHeight = if (itemHeightPx > 0f) itemHeightPx else 140f
                val targetIndex = EventReorderHelper.calculateTargetIndex(
                    startIndex = currentIndex,
                    dragOffsetY = dragOffsetY,
                    itemHeightPx = effectiveHeight,
                    totalItems = localEvents.size,
                )
                if (targetIndex != currentIndex) {
                    localEvents = EventReorderHelper.reorder(localEvents, currentIndex, targetIndex)
                    dragOffsetY -= (targetIndex - currentIndex) * effectiveHeight
                    haptics.tick()
                }
            }
        }
    }

    val endDrag: () -> Unit = {
        val activeId = draggedEventId
        if (activeId != null) {
            haptics.click()
            val originalIndex = events.indexOfFirst { it.id == activeId }
            val finalIndex = localEvents.indexOfFirst { it.id == activeId }
            if (originalIndex != -1 && finalIndex != -1 && originalIndex != finalIndex) {
                onReorder(originalIndex, finalIndex)
            }
        }
        draggedEventId = null
        dragOffsetY = 0f
    }

    val cancelDrag: () -> Unit = {
        localEvents = events
        draggedEventId = null
        dragOffsetY = 0f
    }

    ShirokoWearAmbient(spotlightKey = "cherish_event_order") {
        ShirokoWearScalingRotaryColumn(
            modifier = modifier.fillMaxSize(),
            itemSpacing = 6.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
        ) {
            item(key = "title") {
                ShirokoWearScreenTitle(
                    text = "事件排序",
                    modifier = Modifier.semantics { heading() },
                )
            }

            if (localEvents.isEmpty()) {
                item(key = "empty_state") {
                    Text(
                        text = "暂无事件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(
                    count = localEvents.size,
                    key = { localEvents[it].id },
                ) { index ->
                    val event = localEvents[index]
                    val isDragging = draggedEventId == event.id

                    EventOrderItemCard(
                        event = event,
                        index = index,
                        totalCount = localEvents.size,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onStartDrag = { startDrag(event.id) },
                        onDragDelta = updateDrag,
                        onEndDrag = endDrag,
                        onCancelDrag = cancelDrag,
                        onReorderStep = { fromIdx, toIdx ->
                            haptics.tick()
                            localEvents = EventReorderHelper.reorder(localEvents, fromIdx, toIdx)
                            onReorder(fromIdx, toIdx)
                        },
                        onMeasuredHeight = { heightPx ->
                            if (itemHeightPx == 0f) {
                                itemHeightPx = heightPx
                            }
                        },
                    )
                }
            }

            item(key = "action_done") {
                Spacer(modifier = Modifier.height(4.dp))
                ShirokoWearCardButton(
                    onClick = {
                        haptics.back()
                        onBack()
                    },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "完成排序"
                    },
                ) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShirokoWearTheme.colors.accentGold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventOrderItemCard(
    event: CountdownEvent,
    index: Int,
    totalCount: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onStartDrag: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onReorderStep: (fromIndex: Int, toIndex: Int) -> Unit,
    onMeasuredHeight: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemStateDescription = AccessibilityPresentation.buildEventOrderItemStateDescription(index, totalCount)
    val itemContentDescription = "${event.title}${if (event.isPinned) "，已置顶" else ""}"

    val customActions = remember(event.id, index, totalCount) {
        val actions = mutableListOf<CustomAccessibilityAction>()
        if (index > 0) {
            actions.add(
                CustomAccessibilityAction(
                    label = AccessibilityPresentation.buildEventOrderMoveUpDescription(event.title)
                ) {
                    onReorderStep(index, index - 1)
                    true
                }
            )
        }
        if (index < totalCount - 1) {
            actions.add(
                CustomAccessibilityAction(
                    label = AccessibilityPresentation.buildEventOrderMoveDownDescription(event.title)
                ) {
                    onReorderStep(index, index + 1)
                    true
                }
            )
        }
        if (index > 1) {
            actions.add(
                CustomAccessibilityAction(label = "移至顶部") {
                    onReorderStep(index, 0)
                    true
                }
            )
        }
        if (index < totalCount - 2) {
            actions.add(
                CustomAccessibilityAction(label = "移至末尾") {
                    onReorderStep(index, totalCount - 1)
                    true
                }
            )
        }
        actions
    }

    val cardModifier = modifier
        .zIndex(if (isDragging) 10f else 1f)
        .graphicsLayer {
            if (isDragging) {
                translationY = dragOffsetY
                scaleX = 1.04f
                scaleY = 1.04f
                shadowElevation = 8f
            }
        }
        .onSizeChanged { size ->
            onMeasuredHeight(size.height.toFloat() + 16f)
        }
        .semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = itemContentDescription
            stateDescription = itemStateDescription
            this.customActions = customActions
        }
        .pointerInput(event.id, index) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onStartDrag() },
                onDragEnd = { onEndDrag() },
                onDragCancel = { onCancelDrag() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.y)
                }
            )
        }

    ShirokoWearCard(
        modifier = cardModifier,
        highlighted = isDragging || event.isPinned,
        highlightColor = if (isDragging) ShirokoWearTheme.colors.accentGold else ShirokoWearTheme.colors.cardHighlight,
        fillMaxWidth = true,
        innerPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        outerPadding = PaddingValues(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Event info: Emoji + Title + Pin badge + Position
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = event.emoji,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShirokoWearTheme.colors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (event.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📌",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }

                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Precision drag grip handle (instant touch drag)
            ReorderGripHandle(
                isDragging = isDragging,
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp)
                    .clearAndSetSemantics { }
                    .pointerInput(event.id, index) {
                        detectDragGestures(
                            onDragStart = { onStartDrag() },
                            onDragEnd = { onEndDrag() },
                            onDragCancel = { onCancelDrag() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.y)
                            }
                        )
                    }
            )
        }
    }
}

/**
 * Three horizontal rounded bars representing an ergonomic drag grip handle for Wear OS.
 */
@Composable
private fun ReorderGripHandle(
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val barColor = if (isDragging) {
        ShirokoWearTheme.colors.accentGold
    } else {
        ShirokoWearTheme.colors.cardBorder.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(barColor)
                )
            }
        }
    }
}
