package com.cherish.app.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.EventColor
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.CountdownTypographyTier
import com.cherish.app.home.model.HomeEventUiModel
import com.cherish.app.home.model.resolveCountdownTypographyTier
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.clickVfx

/**
 * Resolves the subtle background gradient brush and matching border stroke brush
 * based strictly on the event's [EventColor].
 *
 * Guarantees:
 * - Subtle translucent gradient (15-25% start, 5-8% end) preserving dark OLED background.
 * - Entire screen is NEVER tinted; only the individual card surface has its own identity.
 * - Decoupled completely from Detail screen's EventBackground.
 */
internal fun resolveCardGradients(color: EventColor): Pair<Brush, Brush> {
    return when (color) {
        is EventColor.Single -> {
            val base = Color(color.argb)
            val bgBrush = Brush.linearGradient(
                colors = listOf(
                    base.copy(alpha = 0.22f),
                    base.copy(alpha = 0.05f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    base.copy(alpha = 0.38f),
                    base.copy(alpha = 0.12f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            Pair(bgBrush, borderBrush)
        }
        is EventColor.Gradient -> {
            val start = Color(color.startColor)
            val end = Color(color.endColor)
            val bgBrush = Brush.linearGradient(
                colors = listOf(
                    start.copy(alpha = 0.24f),
                    end.copy(alpha = 0.07f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    start.copy(alpha = 0.38f),
                    end.copy(alpha = 0.14f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            Pair(bgBrush, borderBrush)
        }
        EventColor.Default -> {
            val bgBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.02f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
            Pair(bgBrush, borderBrush)
        }
    }
}

/**
 * Core event presentation card for the Cherish Home screen.
 *
 * Design principles:
 * - Huge countdown number is the primary focal center.
 * - Title and emoji provide context (emoji default in front of title).
 * - Pin only represents priority/ordering; it does not turn the card into a Hero or distort hierarchy.
 * - Subtle translucent gradient derived from [HomeEventUiModel.color] gives each card its own visual identity.
 * - Supports both List (full-width) and Grid (compact 2-column) presentation modes.
 */
@Composable
fun CountdownEventCard(
    uiModel: HomeEventUiModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val (bgGradient, borderBrush) = remember(uiModel.color) {
        resolveCardGradients(uiModel.color)
    }

    val cardShape = if (isCompact) ShirokoWearShapes.cardCompact else ShirokoWearShapes.card
    val cardA11yDescription = remember(uiModel) {
        AccessibilityPresentation.buildEventCardAccessibilityDescription(uiModel)
    }
    val customActions = remember(uiModel.event.id, onLongClick) {
        if (onLongClick != null) {
            listOf(
                CustomAccessibilityAction(label = "编辑事件") {
                    onLongClick()
                    true
                },
            )
        } else {
            emptyList()
        }
    }

    val clickable = onClick != null || onLongClick != null
    val innerPadding = if (isCompact) {
        PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    } else {
        PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    }
    val outerPadding = if (isCompact) {
        PaddingValues(2.dp)
    } else {
        PaddingValues(vertical = 3.dp)
    }

    Box(
        modifier = modifier
            .padding(outerPadding)
            .then(
                if (clickable) {
                    Modifier.clickVfx(
                        enabled = true,
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() },
                    )
                } else {
                    Modifier
                },
            )
            .clip(cardShape)
            .border(
                width = 1.dp,
                shape = cardShape,
                brush = borderBrush,
            )
            .background(color = ShirokoWearTheme.colors.cardBackground)
            .background(brush = bgGradient)
            .padding(innerPadding)
            .then(if (isCompact) Modifier else Modifier.fillMaxWidth())
            .semantics(mergeDescendants = true) {
                contentDescription = cardA11yDescription
                role = Role.Button
                if (onClick != null) {
                    onClick(label = "查看事件详情") {
                        onClick()
                        true
                    }
                }
                if (customActions.isNotEmpty()) {
                    this.customActions = customActions
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isCompact) {
            CompactCardContent(uiModel)
        } else {
            StandardCardContent(uiModel)
        }
    }
}

@Composable
private fun CountdownTypographyTier.toTextStyle(): TextStyle = when (this) {
    CountdownTypographyTier.DISPLAY_MEDIUM -> MaterialTheme.typography.displayMedium
    CountdownTypographyTier.DISPLAY_SMALL -> MaterialTheme.typography.displaySmall
    CountdownTypographyTier.TITLE_LARGE -> MaterialTheme.typography.titleLarge
    CountdownTypographyTier.TITLE_MEDIUM -> MaterialTheme.typography.titleMedium
}

@Composable
private fun StandardCardContent(uiModel: HomeEventUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Title Row: Emoji + Title + Pin Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiModel.event.emoji.isNotBlank()) {
                    Text(
                        text = uiModel.event.emoji,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = uiModel.event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ShirokoWearTheme.colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (uiModel.isPinned) {
                Text(
                    text = "📌",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clearAndSetSemantics { },
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val isToday = uiModel.status == CountdownDisplayStatus.TODAY
        val typographyTier = resolveCountdownTypographyTier(
            isCompact = false,
            isToday = isToday,
            daysCount = uiModel.daysCount,
        )

        // Large Countdown Number + Unit
        if (isToday) {
            Text(
                text = "TODAY",
                style = typographyTier.toTextStyle(),
                fontWeight = FontWeight.Black,
                color = ShirokoWearTheme.colors.accentGold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${uiModel.daysCount}",
                    style = typographyTier.toTextStyle(),
                    fontWeight = FontWeight.Black,
                    color = ShirokoWearTheme.colors.contentPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = uiModel.unitLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        // Target Date
        Text(
            text = uiModel.targetDateFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactCardContent(uiModel: HomeEventUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // Compact Title Row: Emoji + Title + Pin Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiModel.event.emoji.isNotBlank()) {
                Text(
                    text = uiModel.event.emoji,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = uiModel.event.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = ShirokoWearTheme.colors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (uiModel.isPinned) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "📌",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }

        val isToday = uiModel.status == CountdownDisplayStatus.TODAY
        val typographyTier = resolveCountdownTypographyTier(
            isCompact = true,
            isToday = isToday,
            daysCount = uiModel.daysCount,
        )

        // Compact Number + Unit
        if (isToday) {
            Text(
                text = "TODAY",
                style = typographyTier.toTextStyle(),
                fontWeight = FontWeight.Bold,
                color = ShirokoWearTheme.colors.accentGold,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Text(
                text = "${uiModel.daysCount}",
                style = typographyTier.toTextStyle(),
                fontWeight = FontWeight.Bold,
                color = ShirokoWearTheme.colors.contentPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = uiModel.unitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
