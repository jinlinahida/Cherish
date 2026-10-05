package com.cherish.app.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.EventBackground
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeEventUiModel
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Core event presentation card built strictly upon ShirokoWearUI's [ShirokoWearCard].
 *
 * Designed specifically for Wear OS small displays:
 * - Large countdown number acts as the primary focal point.
 * - Title and emoji provide context without overpowering the number.
 * - Supports both List (full-width) and Grid (compact half-width) presentation modes.
 * - Subtle animated highlight border when [HomeEventUiModel.isPinned] is true.
 */
@Composable
fun CountdownEventCard(
    uiModel: HomeEventUiModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val highlightColor = when (val bg = uiModel.background) {
        is EventBackground.Color -> Color(bg.argb)
        else -> ShirokoWearTheme.colors.cardHighlight
    }

    val cardShape = if (isCompact) ShirokoWearShapes.cardCompact else ShirokoWearShapes.card

    ShirokoWearCard(
        modifier = modifier,
        shape = cardShape,
        highlighted = uiModel.isPinned,
        highlightColor = highlightColor,
        onClick = onClick,
        onLongClick = onLongClick,
        innerPadding = if (isCompact) {
            PaddingValues(horizontal = 6.dp, vertical = 6.dp)
        } else {
            PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        },
        outerPadding = if (isCompact) {
            PaddingValues(2.dp)
        } else {
            PaddingValues(vertical = 3.dp)
        },
    ) {
        if (isCompact) {
            CompactCardContent(uiModel)
        } else {
            StandardCardContent(uiModel)
        }
    }
}

@Composable
private fun StandardCardContent(uiModel: HomeEventUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiModel.event.emoji,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = uiModel.event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ShirokoWearTheme.colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (uiModel.isPinned) {
                Text(
                    text = "📌",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Large Number + Unit
        if (uiModel.status == CountdownDisplayStatus.TODAY) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = ShirokoWearTheme.colors.accentGold,
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                val numberColor = if (uiModel.isPinned) {
                    ShirokoWearTheme.colors.cardHighlight
                } else {
                    ShirokoWearTheme.colors.contentPrimary
                }

                Text(
                    text = "${uiModel.daysCount}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = numberColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = uiModel.unitLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
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
        // Compact Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiModel.event.emoji,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = uiModel.event.title,
                style = MaterialTheme.typography.labelMedium,
                color = ShirokoWearTheme.colors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Compact Number
        if (uiModel.status == CountdownDisplayStatus.TODAY) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ShirokoWearTheme.colors.accentGold,
            )
        } else {
            val numberColor = if (uiModel.isPinned) {
                ShirokoWearTheme.colors.cardHighlight
            } else {
                ShirokoWearTheme.colors.contentPrimary
            }
            Text(
                text = "${uiModel.daysCount}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = numberColor,
            )
            Text(
                text = uiModel.unitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
