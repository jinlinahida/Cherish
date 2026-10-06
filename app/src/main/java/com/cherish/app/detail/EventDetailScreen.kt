package com.cherish.app.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.date.model.SolarDate
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.detail.model.EventDetailUiModel
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearButtonDefaults
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearDetailField
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.cherish.app.ui.AccessibilityPresentation
import com.cherish.app.detail.model.DetailCountdownTypographyTier
import com.cherish.app.detail.model.resolveDetailCountdownTypographyTier
import com.cherish.app.home.model.CountdownDisplayStatus

/**
 * Event Detail Screen for Cherish.
 *
 * Displays:
 * - Emoji and title header with bezel protection
 * - Hero countdown card with adaptive typography tier and accessibility announcements
 * - Attribute fields (calendar type, repeat rule, category, pin status, notes)
 * - Edit & Delete action buttons with a non-destructive delete confirmation step
 *   intercepted by a local BackHandler.
 */
@Composable
fun EventDetailScreen(
    uiModel: EventDetailUiModel,
    onEditClick: (String) -> Unit,
    onDeleteConfirm: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    val heroTextStyle = when (typographyTier) {
        DetailCountdownTypographyTier.HERO_LARGE -> MaterialTheme.typography.displayMedium
        DetailCountdownTypographyTier.HERO_MEDIUM -> MaterialTheme.typography.displaySmall
        DetailCountdownTypographyTier.HERO_COMPACT -> MaterialTheme.typography.titleLarge
    }

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

    ShirokoWearAmbient(spotlightKey = "cherish_detail") {
        ShirokoWearScalingRotaryColumn(
            modifier = modifier.fillMaxSize(),
            itemSpacing = 8.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
        ) {
            // Header: Emoji + Title (with horizontal padding protecting round bezels)
            item(key = "detail_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (uiModel.event.emoji.isNotBlank()) {
                        Text(
                            text = uiModel.event.emoji,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = uiModel.event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShirokoWearTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }

            // Hero Countdown Card (with adaptive typography tier and single-line overflow safety)
            item(key = "detail_hero_card") {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.card,
                    highlighted = uiModel.isPinned,
                    innerPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = heroCardA11yDescription
                    },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiModel.heroNumberText,
                            style = heroTextStyle,
                            fontWeight = FontWeight.Bold,
                            color = ShirokoWearTheme.colors.accentGold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiModel.heroUnitText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiModel.targetDateDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Information Attributes Card
            item(key = "detail_attributes_card") {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.card,
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

            // Operations & Actions Section
            if (showDeleteConfirm) {
                item(key = "detail_delete_confirm_card") {
                    ShirokoWearCard(
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
                                text = "「${uiModel.event.title}」",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ShirokoWearCardButton(
                                onClick = {
                                    haptics.click()
                                    onDeleteConfirm(uiModel.event.id)
                                },
                                border = ShirokoWearButtonDefaults.highlightedBorderStroke(
                                    ShirokoWearTheme.colors.accentCopper,
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "确认删除事件：${uiModel.event.title}"
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
                                onClick = {
                                    haptics.back()
                                    showDeleteConfirm = false
                                },
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
            } else {
                item(key = "detail_btn_edit") {
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
                }

                item(key = "detail_btn_delete") {
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

                item(key = "detail_btn_back") {
                    ShirokoWearCardButton(
                        onClick = {
                            haptics.back()
                            onBackClick()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "返回上一页"
                        },
                    ) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
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
    )
    val uiModel = EventDetailMapper.toUiModel(sampleEvent, SolarDate(2026, 10, 5))

    ShirokoWearTheme {
        EventDetailScreen(
            uiModel = uiModel,
            onEditClick = {},
            onDeleteConfirm = {},
            onBackClick = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun EventDetailScreenRoundLargePreview() {
    val sampleEvent = CountdownEvent(
        id = "preview-round-large",
        title = "这是一个超长标题用来验证小屏幕自动省略展示效果",
        emoji = "🪐",
        eventDate = EventDate.Solar(SolarDate(2030, 1, 1)),
        isPinned = true,
        notes = "超长备注信息在圆屏放大下的排版表现",
    )
    val uiModel = EventDetailMapper.toUiModel(sampleEvent, SolarDate(2026, 10, 5))

    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.ROUND,
    ) {
        EventDetailScreen(
            uiModel = uiModel,
            onEditClick = {},
            onDeleteConfirm = {},
            onBackClick = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun EventDetailScreenSquareStandardPreview() {
    val sampleEvent = CountdownEvent(
        id = "preview-square-standard",
        title = "妈妈生日",
        emoji = "🎂",
        eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
        isPinned = true,
        notes = "方表标准排版测试",
    )
    val uiModel = EventDetailMapper.toUiModel(sampleEvent, SolarDate(2026, 10, 5))

    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.STANDARD,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        EventDetailScreen(
            uiModel = uiModel,
            onEditClick = {},
            onDeleteConfirm = {},
            onBackClick = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun EventDetailScreenSquareLargePreview() {
    val sampleEvent = CountdownEvent(
        id = "preview-square-large",
        title = "今天的重要时刻",
        emoji = "🌟",
        eventDate = EventDate.Solar(SolarDate(2026, 10, 5)),
        isPinned = false,
        notes = "方表大字体排版测试",
    )
    val uiModel = EventDetailMapper.toUiModel(sampleEvent, SolarDate(2026, 10, 5))

    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        EventDetailScreen(
            uiModel = uiModel,
            onEditClick = {},
            onDeleteConfirm = {},
            onBackClick = {},
        )
    }
}

