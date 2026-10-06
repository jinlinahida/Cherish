package com.cherish.app.settings.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearButtonDefaults
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * Screen allowing users to manually adjust the natural repository order of events.
 *
 * Provides Wear OS friendly "上移" / "下移" button controls on each event card,
 * with rotary scroll support and tactile haptic feedback.
 */
@Composable
fun EventOrderScreen(
    events: List<CountdownEvent>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberShirokoWearHaptics()

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

            if (events.isEmpty()) {
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
                    count = events.size,
                    key = { events[it].id },
                ) { index ->
                    val event = events[index]
                    EventOrderItemCard(
                        event = event,
                        index = index,
                        totalCount = events.size,
                        onMoveUp = {
                            haptics.click()
                            onMoveUp(index)
                        },
                        onMoveDown = {
                            haptics.click()
                            onMoveDown(index)
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShirokoWearCard(
        modifier = modifier,
        fillMaxWidth = true,
        innerPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        outerPadding = PaddingValues(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val itemDescription = AccessibilityPresentation.buildEventOrderItemDescription(
                title = event.title,
                index = index,
                totalCount = totalCount,
                isPinned = event.isPinned,
            )
            // Event info: Emoji + Title + Pin badge + Position
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = itemDescription
                    },
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

            Spacer(modifier = Modifier.width(6.dp))

            // Action buttons: Move Up & Move Down
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier
                        .size(34.dp)
                        .semantics {
                            contentDescription = AccessibilityPresentation.buildEventOrderMoveUpDescription(event.title)
                        },
                    contentPadding = PaddingValues(0.dp),
                    colors = ShirokoWearButtonDefaults.buttonColors(),
                    border = ShirokoWearButtonDefaults.borderStroke,
                ) {
                    Text(
                        text = "▲",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }

                Button(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier
                        .size(34.dp)
                        .semantics {
                            contentDescription = AccessibilityPresentation.buildEventOrderMoveDownDescription(event.title)
                        },
                    contentPadding = PaddingValues(0.dp),
                    colors = ShirokoWearButtonDefaults.buttonColors(),
                    border = ShirokoWearButtonDefaults.borderStroke,
                ) {
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}
