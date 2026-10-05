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
import io.github.jinlinahida.shirokowear.ui.ShirokoWearDetailField
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * Event Detail Screen for Cherish.
 *
 * Displays:
 * - Emoji and title header
 * - Hero countdown card (countdown number, status, target date)
 * - Attribute fields (calendar type, repeat rule, category, pin status, notes)
 * - Edit & Delete action buttons with a non-destructive delete confirmation step
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

    ShirokoWearAmbient(spotlightKey = "cherish_detail") {
        ShirokoWearScalingRotaryColumn(
            modifier = modifier.fillMaxSize(),
            itemSpacing = 8.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
        ) {
            // Header: Emoji + Title
            item(key = "detail_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (uiModel.event.emoji.isNotBlank()) {
                        Text(
                            text = uiModel.event.emoji,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
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
                    )
                }
            }

            // Hero Countdown Card
            item(key = "detail_hero_card") {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.card,
                    highlighted = uiModel.isPinned,
                    innerPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiModel.heroNumberText,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShirokoWearTheme.colors.accentGold,
                            textAlign = TextAlign.Center,
                            lineHeight = 42.sp,
                        )
                        Text(
                            text = uiModel.heroUnitText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiModel.targetDateDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
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
                        )
                        ShirokoWearDetailField(
                            label = "重复规则",
                            value = uiModel.recurrenceDescription,
                        )
                        ShirokoWearDetailField(
                            label = "分类",
                            value = uiModel.categoryDescription,
                        )
                        ShirokoWearDetailField(
                            label = "置顶状态",
                            value = if (uiModel.isPinned) "已置顶 📌" else "未置顶",
                        )
                        if (uiModel.notes.isNotBlank()) {
                            ShirokoWearDetailField(
                                label = "备注",
                                value = uiModel.notes,
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
