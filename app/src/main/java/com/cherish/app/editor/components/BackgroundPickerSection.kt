package com.cherish.app.editor.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.cherish.app.storage.CuratedWallpaperGenerator
import com.cherish.app.storage.CuratedWallpaperPreset
import com.cherish.app.storage.EventImageStorage
import com.cherish.app.storage.LocalWatchImage
import com.cherish.app.storage.WatchStorageImageScanner
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
import java.io.File

/**
 * Background style selection screen for Wear OS.
 *
 * Exclusively configures the visual background of the Event Detail screen.
 * Supports:
 * - Default deep vignette
 * - Solid color tints
 * - Duo-tone gradients
 * - Watch-native curated photographic wallpapers (100% offline, standalone on watch)
 * - Local watch storage scan (/sdcard/Pictures/)
 * - System photo picker & file picker with crash-proof fallback
 * - Adjustable dimming scrim (30%, 45%, 60%) with real-time preview
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
    var scannedLocalImages by remember { mutableStateOf<List<LocalWatchImage>?>(null) }
    var isScanningStorage by remember { mutableStateOf(false) }

    fun processImageUri(uri: Uri) {
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

    // Primary launcher: Android photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    // Secondary fallback launcher: System GET_CONTENT
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    fun launchSystemPickerSafely() {
        haptics.click()
        imagePickError = null

        val photoPickerLaunched = runCatching {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }.isSuccess

        if (!photoPickerLaunched) {
            val getContentLaunched = runCatching {
                getContentLauncher.launch("image/*")
            }.isSuccess

            if (!getContentLaunched) {
                haptics.back()
                imagePickError = "手表未安装相册或文件应用，请选用下方的「精选壁纸」或「扫描手表图片」"
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

        // Section: Custom Photo Wallpaper Header
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = ShirokoWearTheme.colors.accentCopper,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
        }

        // Active image preview & controls
        if (currentBackground is EventBackground.Image) {
            val currentImg = currentBackground
            item(key = "image_active_preview") {
                ImageActivePreviewCard(
                    currentImage = currentImg,
                    imageStorage = imageStorage,
                    onPickNewPhoto = { launchSystemPickerSafely() },
                    onClearImage = {
                        haptics.click()
                        onBackgroundChange(EventBackground.Default)
                    },
                )
            }

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
        } else {
            // No custom image active - Launch button
            item(key = "custom_image_action") {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.cardCompact,
                    highlighted = false,
                    innerPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    onClick = { launchSystemPickerSafely() },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "从相册选择图片作为背景"
                    },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "🖼️ 从相册选择图片",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "支持系统相册与文件管理器",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Section: Curated Watch Wallpapers (100% offline, watch-tailored photo studio)
        item(key = "curated_wallpapers_header") {
            Text(
                text = "手表精选壁纸",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.accentGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { heading() },
            )
        }

        items(
            count = CuratedWallpaperPreset.entries.size,
            key = { "curated_${CuratedWallpaperPreset.entries[it].presetId}" },
        ) { index ->
            val preset = CuratedWallpaperPreset.entries[index]
            val isCurrentWallpaper = currentBackground is EventBackground.Image &&
                currentBackground.path.contains(preset.presetId)

            ShirokoWearSelectableButton(
                selected = isCurrentWallpaper,
                onClick = {
                    haptics.click()
                    val targetId = eventId ?: "editor_draft"
                    coroutineScope.launch(Dispatchers.IO) {
                        if (imageStorage != null) {
                            val filename = CuratedWallpaperGenerator.saveCuratedWallpaper(
                                preset = preset,
                                eventId = targetId,
                                imageStorage = imageStorage,
                            )
                            withContext(Dispatchers.Main) {
                                onBackgroundChange(
                                    EventBackground.Image(
                                        path = filename,
                                        dimAlpha = 0.45f,
                                    ),
                                )
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = isCurrentWallpaper
                    contentDescription = "${preset.title}，${preset.description}${if (isCurrentWallpaper) "，已选用" else ""}"
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(preset.dominantColor),
                                        Color(preset.secondaryColor),
                                    ),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrentWallpaper) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentWallpaper) ShirokoWearTheme.colors.accentGold else Color.White,
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Section: Watch Local Pictures Scan
        item(key = "local_scan_header") {
            Text(
                text = "手表存储导入",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.accentGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { heading() },
            )
        }

        val localList = scannedLocalImages
        if (localList == null) {
            item(key = "btn_scan_storage") {
                ShirokoWearCardButton(
                    onClick = {
                        haptics.click()
                        isScanningStorage = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val images = WatchStorageImageScanner.scanLocalImages(context)
                            withContext(Dispatchers.Main) {
                                isScanningStorage = false
                                scannedLocalImages = images
                            }
                        }
                    },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "扫描手表存储中的图片"
                    },
                ) {
                    Text(
                        text = if (isScanningStorage) "正在扫描手表..." else "🔍 扫描手表存储图片",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else if (localList.isEmpty()) {
            item(key = "scan_empty_note") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "未在手表存储中找到图片 (/sdcard/Pictures)\n可用 ADB 推送或直接选用上方壁纸",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(
                count = localList.size,
                key = { "local_${localList[it].file.absolutePath}" },
            ) { index ->
                val localImg = localList[index]
                ShirokoWearSelectableButton(
                    selected = false,
                    onClick = {
                        haptics.click()
                        val targetId = eventId ?: "editor_draft"
                        coroutineScope.launch(Dispatchers.IO) {
                            if (imageStorage != null) {
                                val filename = WatchStorageImageScanner.importImage(
                                    image = localImg,
                                    eventId = targetId,
                                    imageStorage = imageStorage,
                                )
                                withContext(Dispatchers.Main) {
                                    onBackgroundChange(
                                        EventBackground.Image(
                                            path = filename,
                                            dimAlpha = 0.45f,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "导入本地图片：${localImg.displayName}"
                    },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = localImg.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${localImg.sizeBytes / 1024} KB",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Section: Presets (Solid & Gradient)
        item(key = "presets_header") {
            Text(
                text = "预设单色与渐变风格",
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
                onClick = {
                    haptics.click()
                    onBackgroundChange(bg)
                },
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

/**
 * Preview card for active custom image background, showing actual thumbnail,
 * dimming scrim simulation, and live typography contrast.
 */
@Composable
private fun ImageActivePreviewCard(
    currentImage: EventBackground.Image,
    imageStorage: EventImageStorage?,
    onPickNewPhoto: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(currentImage.path, imageStorage) {
        val file = imageStorage?.getImageFile(currentImage.path)
            ?: File(currentImage.path).takeIf { it.exists() }
        if (file != null && file.isFile) {
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    ShirokoWearCard(
        shape = ShirokoWearShapes.card,
        highlighted = true,
        highlightColor = ShirokoWearTheme.colors.accentGold,
        innerPadding = PaddingValues(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Live Preview Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B1E24)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Simulated dimming scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentImage.dimAlpha.coerceIn(0.20f, 0.85f))),
                )
                // Simulated text for live legibility validation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "10 天",
                        style = MaterialTheme.typography.titleMedium.copy(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.95f), blurRadius = 8f),
                        ),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        text = "文字清晰度预览",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.95f), blurRadius = 6f),
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            // Actions row: Replace or Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ShirokoWearSelectableButton(
                    selected = false,
                    onClick = onPickNewPhoto,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text(
                        text = "更换图片",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ShirokoWearTheme.colors.accentGold,
                    )
                }
                ShirokoWearSelectableButton(
                    selected = false,
                    onClick = onClearImage,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text(
                        text = "恢复默认",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ShirokoWearTheme.colors.accentCopper,
                    )
                }
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
