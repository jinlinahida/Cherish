package com.cherish.app.detail

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.model.SolarDate
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.detail.model.DetailCountdownTypographyTier
import com.cherish.app.detail.model.EventDetailUiModel
import com.cherish.app.detail.model.resolveDetailCountdownTypographyTier
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.storage.EventImageStorage
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearButtonDefaults
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearDetailField
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics
import java.io.File

/**
 * Event Detail Screen for Cherish.
 *
 * Product design baseline:
 * - Immersive event background (Color, Gradient, Image) fills the page.
 * - Massive countdown number is the undisputed visual focal center.
 * - Event name sits at the top; target date sits below the number.
 * - Free-floating typography without card enclosures around the countdown hero.
 * - Secondary attributes and edit/delete actions live below the fold without competing
 *   for initial visual prominence.
 */
@Composable
fun EventDetailScreen(
    uiModel: EventDetailUiModel,
    onEditClick: (String) -> Unit,
    onDeleteConfirm: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageStorage: EventImageStorage? = null,
) {
    val haptics = rememberShirokoWearHaptics()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back during delete confirmation
    BackHandler(enabled = showDeleteConfirm) {
        haptics.back()
        showDeleteConfirm = false
    }

    val typographyTier = remember(uiModel.heroNumberText, uiModel.status) {
        resolveDetailCountdownTypographyTier(uiModel.heroNumberText, uiModel.status)
    }
    val heroTextStyle = resolveDetailTextStyle(typographyTier)

    val heroCardA11yDescription = remember(
        uiModel.status,
        uiModel.heroNumberText,
        uiModel.heroUnitText,
        uiModel.targetDateDescription,
    ) {
        AccessibilityPresentation.buildDetailHeroAccessibilityDescription(
            status = uiModel.status,
            heroNumberText = uiModel.heroNumberText,
            heroUnitText = uiModel.heroUnitText,
            targetDateDescription = uiModel.targetDateDescription,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Immersive Event Background
        EventDetailBackground(
            background = uiModel.background,
            imageStorage = imageStorage,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Rotary & Scrolling Content Stream
        ShirokoWearScalingRotaryColumn(
            modifier = Modifier.fillMaxSize(),
            itemSpacing = 8.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
            autoCentering = AutoCenteringParams(itemIndex = 0),
        ) {
            // Primary Visual Focus: Uncaged Countdown Hero Section
            item(key = "detail_hero_section") {
                DetailHeroSection(
                    uiModel = uiModel,
                    heroTextStyle = heroTextStyle,
                    accessibilityDescription = heroCardA11yDescription,
                )
            }

            // Secondary Info: Attributes Card (sleek frosted surface)
            item(key = "detail_attributes_card") {
                DetailAttributesCard(uiModel = uiModel)
            }

            // Actions: Edit & Delete Buttons (below the fold)
            if (showDeleteConfirm) {
                item(key = "detail_delete_confirm_card") {
                    DetailDeleteConfirmCard(
                        title = uiModel.event.title,
                        onConfirm = {
                            haptics.click()
                            onDeleteConfirm(uiModel.event.id)
                        },
                        onCancel = {
                            haptics.back()
                            showDeleteConfirm = false
                        },
                    )
                }
            } else {
                item(key = "detail_actions") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ShirokoWearCardButton(
                            onClick = {
                                haptics.click()
                                onEditClick(uiModel.event.id)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "编辑事件：${uiModel.event.title}"
                            },
                        ) {
                            Text(
                                text = "编辑事件",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ShirokoWearTheme.colors.accentGold,
                            )
                        }

                        ShirokoWearCardButton(
                            onClick = {
                                haptics.click()
                                showDeleteConfirm = true
                            },
                            border = ShirokoWearButtonDefaults.highlightedBorderStroke(
                                ShirokoWearTheme.colors.accentCopper,
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "删除事件：${uiModel.event.title}"
                            },
                        ) {
                            Text(
                                text = "删除事件",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShirokoWearTheme.colors.accentCopper,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Free-floating countdown hero section without boxed card boundaries.
 * Lets the massive countdown number breathe in the center of the watch display.
 */
@Composable
private fun DetailHeroSection(
    uiModel: EventDetailUiModel,
    heroTextStyle: TextStyle,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Top: Emoji + Event Title
        if (uiModel.event.emoji.isNotBlank()) {
            Text(
                text = uiModel.event.emoji,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Text(
            text = uiModel.event.title,
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 8f),
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Center: Monumental Countdown Number
        val isToday = uiModel.status == CountdownDisplayStatus.TODAY
        if (isToday) {
            Text(
                text = "今天",
                style = heroTextStyle.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.9f), blurRadius = 12f),
                ),
                fontWeight = FontWeight.Black,
                color = ShirokoWearTheme.colors.accentGold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Text(
                text = uiModel.heroNumberText,
                style = heroTextStyle.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.95f), blurRadius = 14f),
                ),
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = uiModel.heroUnitText,
                style = MaterialTheme.typography.labelMedium.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 6f),
                ),
                fontWeight = FontWeight.Bold,
                color = if (uiModel.status == CountdownDisplayStatus.PAST) {
                    ShirokoWearTheme.colors.accentCopper
                } else {
                    Color.White.copy(alpha = 0.85f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom: Target Date
        Text(
            text = uiModel.targetDateDescription,
            style = MaterialTheme.typography.bodySmall.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 6f),
            ),
            color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Secondary attributes card with frosted semi-translucent background,
 * placed below the hero fold to avoid cluttering the visual focal center.
 */
@Composable
private fun DetailAttributesCard(
    uiModel: EventDetailUiModel,
    modifier: Modifier = Modifier,
) {
    ShirokoWearCard(
        modifier = modifier,
        shape = ShirokoWearShapes.card,
        backgroundColor = Color(0xFF14171A).copy(alpha = 0.65f),
        innerPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ShirokoWearDetailField(
                label = "日历类型",
                value = uiModel.calendarTypeDescription,
                modifier = Modifier.semantics(mergeDescendants = true) { },
            )
            ShirokoWearDetailField(
                label = "重复规则",
                value = uiModel.recurrenceDescription,
                modifier = Modifier.semantics(mergeDescendants = true) { },
            )
            ShirokoWearDetailField(
                label = "分类",
                value = uiModel.categoryDescription,
                modifier = Modifier.semantics(mergeDescendants = true) { },
            )
            ShirokoWearDetailField(
                label = "置顶状态",
                value = if (uiModel.isPinned) "已置顶 📌" else "未置顶",
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "置顶状态：${if (uiModel.isPinned) "已置顶" else "未置顶"}"
                },
            )
            if (uiModel.notes.isNotBlank()) {
                ShirokoWearDetailField(
                    label = "备注",
                    value = uiModel.notes,
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                )
            }
        }
    }
}

/**
 * Delete confirmation card with non-destructive cancel option.
 */
@Composable
private fun DetailDeleteConfirmCard(
    title: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShirokoWearCard(
        modifier = modifier,
        shape = ShirokoWearShapes.card,
        highlighted = true,
        highlightColor = ShirokoWearTheme.colors.accentCopper,
        innerPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "确定删除事件？",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ShirokoWearTheme.colors.accentCopper,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "「$title」",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = onConfirm,
                border = ShirokoWearButtonDefaults.highlightedBorderStroke(
                    ShirokoWearTheme.colors.accentCopper,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "确认删除事件：$title"
                },
            ) {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShirokoWearTheme.colors.accentCopper,
                )
            }
            ShirokoWearCardButton(
                onClick = onCancel,
                modifier = Modifier.semantics {
                    contentDescription = "取消删除"
                },
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Full-screen visual background implementation supporting all [EventBackground] variants.
 * Includes protective dark scrims and edge vignettes to guarantee text legibility.
 */
@Composable
internal fun EventDetailBackground(
    background: EventBackground,
    imageStorage: EventImageStorage? = null,
    modifier: Modifier = Modifier,
) {
    when (background) {
        EventBackground.Default -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF232732),
                                Color(0xFF101217),
                                Color(0xFF08090C),
                            ),
                            radius = 450f,
                        ),
                    ),
            )
        }

        is EventBackground.Color -> {
            val baseColor = Color(background.argb)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.55f),
                                baseColor.copy(alpha = 0.22f),
                                Color(0xFF0A0C0E),
                            ),
                            radius = 450f,
                        ),
                    ),
            )
        }

        is EventBackground.Gradient -> {
            val start = Color(background.startColor)
            val end = Color(background.endColor)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                start.copy(alpha = 0.65f),
                                end.copy(alpha = 0.35f),
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.80f),
                            ),
                            radius = 420f,
                        ),
                    ),
            )
        }

        is EventBackground.Pattern -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2C3240),
                                Color(0xFF14171E),
                                Color(0xFF090A0D),
                            ),
                            radius = 450f,
                        ),
                    ),
            )
        }

        is EventBackground.Image -> {
            val imageBitmap = remember(background.path, imageStorage) {
                val file = imageStorage?.getImageFile(background.path)
                    ?: File(background.path).takeIf { it.exists() }
                if (file != null && file.isFile) {
                    runCatching {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }.getOrNull()
                } else {
                    null
                }
            }

            if (imageBitmap != null) {
                Box(modifier = modifier.fillMaxSize()) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Dimming scrim protecting text readability on bright/white photos
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = maxOf(0.40f, background.dimAlpha))),
                    )
                    // Radial bezel vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.85f),
                                    ),
                                    radius = 420f,
                                ),
                            ),
                    )
                }
            } else {
                // Graceful fallback to Default background if file is absent
                EventDetailBackground(
                    background = EventBackground.Default,
                    imageStorage = imageStorage,
                    modifier = modifier,
                )
            }
        }
    }
}

private fun resolveDetailTextStyle(typographyTier: DetailCountdownTypographyTier): TextStyle = when (typographyTier) {
    DetailCountdownTypographyTier.HERO_LARGE -> TextStyle(
        fontSize = 52.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 56.sp,
    )
    DetailCountdownTypographyTier.HERO_MEDIUM -> TextStyle(
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 42.sp,
    )
    DetailCountdownTypographyTier.HERO_COMPACT -> TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
    )
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun EventDetailScreenPreview() {
    val sampleEvent = CountdownEvent(
        id = "preview-1",
        title = "妈妈生日",
        emoji = "🎂",
        eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
        isPinned = true,
        notes = "记得提前买花",
        background = EventBackground.Gradient(0xFF8E2DE2L, 0xFF4A00E0L),
    )
    val uiModel = EventDetailMapper.toUiModel(sampleEvent, SolarDate(2026, 10, 5))

    ShirokoWearTheme {
        EventDetailScreen(
            uiModel = uiModel,
            onEditClick = {},
            onDeleteConfirm = {},
        )
    }
}
