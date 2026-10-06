package com.cherish.app.editor.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.event.model.EventBackground
import com.cherish.app.storage.EventImageStorage
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background style selection screen for Wear OS.
 *
 * Exclusively configures the visual background of the Event Detail screen.
 * Supports:
 * - Default deep vignette
 * - Solid color tints
 * - Duo-tone gradients
 * - Custom photo wallpapers from device storage with dimming scrim adjustments
 */
@Composable
fun BackgroundPickerSection(
    currentBackground: EventBackground,
    onBackgroundChange: (EventBackground) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    imageStorage: EventImageStorage? = null,
    eventId: String? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberShirokoWearHaptics()
    var imagePickError by remember { mutableStateOf<String?>(null) }

    // Launcher for system photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val targetId = eventId ?: "editor_draft"
                        val filename = imageStorage?.saveImage(
                            eventId = targetId,
                            inputStream = inputStream,
                            extension = "jpg",
                        ) ?: uri.toString()

                        withContext(Dispatchers.Main) {
                            haptics.click()
                            imagePickError = null
                            onBackgroundChange(
                                EventBackground.Image(
                                    path = filename,
                                    dimAlpha = 0.45f,
                                ),
                            )
                        }
                    } ?: error("无法打开所选图片文件")
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        haptics.back()
                        imagePickError = e.message ?: "读取图片失败"
                    }
                }
            }
        }
    }

    val presetStyles = listOf(
        Pair(EventBackground.Default, "默认 (墨色微光)"),
        Pair(EventBackground.Color(0xFFE91E63L), "粉红微光"),
        Pair(EventBackground.Color(0xFFFFA000L), "琥珀金光"),
        Pair(EventBackground.Color(0xFF009688L), "青空翡翠"),
        Pair(EventBackground.Color(0xFF1E88E5L), "星河深蓝"),
        Pair(EventBackground.Color(0xFF9C27B0L), "紫罗兰光"),
        Pair(EventBackground.Gradient(0xFFFFA000L, 0xFFE91E63L), "日落渐变"),
        Pair(EventBackground.Gradient(0xFF009688L, 0xFF1E88E5L), "极光渐变"),
        Pair(EventBackground.Gradient(0xFF1E88E5L, 0xFF9C27B0L), "星云渐变"),
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "详情背景",
                modifier = Modifier.semantics { heading() },
            )
        }

        item(key = "hint_banner") {
            Text(
                text = "进入事件详情页后的全屏背景\n不影响首页卡片",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clearAndSetSemantics { },
            )
        }

        // Section: Custom Photo Wallpaper
        item(key = "custom_image_header") {
            Text(
                text = "自定义图片背景",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.accentGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .semantics { heading() },
            )
        }

        if (imagePickError != null) {
            item(key = "image_pick_error") {
                Text(
                    text = imagePickError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShirokoWearTheme.colors.accentCopper,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "custom_image_action") {
            val isImageActive = currentBackground is EventBackground.Image
            ShirokoWearCard(
                shape = ShirokoWearShapes.cardCompact,
                highlighted = isImageActive,
                highlightColor = ShirokoWearTheme.colors.accentGold,
                innerPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                onClick = {
                    haptics.click()
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = if (isImageActive) "当前已选择自定义图片，点击更换" else "从相册选择图片作为背景"
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (isImageActive) "🖼️ 更换自定义图片" else "🖼️ 从相册选择图片",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isImageActive) ShirokoWearTheme.colors.accentGold else Color.White,
                    )
                    if (isImageActive) {
                        val currentImg = currentBackground as EventBackground.Image
                        Text(
                            text = "已启用图片背景",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // When image background is active, allow adjusting text-protecting dim alpha
        if (currentBackground is EventBackground.Image) {
            val currentImg = currentBackground
            item(key = "dim_alpha_adjuster") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "遮罩暗度 (保护文字可读性)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val levels = listOf(
                            Pair(0.30f, "轻度 30%"),
                            Pair(0.45f, "标准 45%"),
                            Pair(0.60f, "深色 60%"),
                        )
                        for ((alpha, label) in levels) {
                            val isLevelSelected = kotlin.math.abs(currentImg.dimAlpha - alpha) < 0.05f
                            ShirokoWearSelectableButton(
                                selected = isLevelSelected,
                                onClick = {
                                    haptics.click()
                                    onBackgroundChange(currentImg.copy(dimAlpha = alpha))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Presets
        item(key = "presets_header") {
            Text(
                text = "预设背景风格",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.accentGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { heading() },
            )
        }

        items(
            count = presetStyles.size,
            key = { presetStyles[it].second },
        ) { index ->
            val (bg, label) = presetStyles[index]
            val isSelected = currentBackground == bg
            ShirokoWearSelectableButton(
                selected = isSelected,
                onClick = { onBackgroundChange(bg) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isSelected
                    contentDescription = "$label${if (isSelected) "，已选中" else ""}"
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BackgroundSwatch(
                        background = bg,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "确定详情背景选择"
                },
            ) {
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

@Composable
private fun BackgroundSwatch(
    background: EventBackground,
    modifier: Modifier = Modifier,
) {
    val brush = when (background) {
        EventBackground.Default -> Brush.radialGradient(
            colors = listOf(Color(0xFF232732), Color(0xFF08090C)),
        )
        is EventBackground.Color -> {
            val c = Color(background.argb)
            Brush.radialGradient(
                colors = listOf(c.copy(alpha = 0.85f), Color(0xFF0A0C0E)),
            )
        }
        is EventBackground.Gradient -> {
            val start = Color(background.startColor)
            val end = Color(background.endColor)
            Brush.linearGradient(
                colors = listOf(start.copy(alpha = 0.90f), end.copy(alpha = 0.50f)),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
        is EventBackground.Pattern -> Brush.radialGradient(
            colors = listOf(Color(0xFF2C3240), Color(0xFF090A0D)),
        )
        is EventBackground.Image -> Brush.radialGradient(
            colors = listOf(Color(0xFF556677), Color(0xFF112233)),
        )
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    )
}
