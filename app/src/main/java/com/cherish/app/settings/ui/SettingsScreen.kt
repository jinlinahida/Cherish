package com.cherish.app.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.settings.model.AppContentScale
import com.cherish.app.settings.model.AppSettings
import com.cherish.app.ui.AccessibilityPresentation
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSettingsItem
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.ShirokoWearToggleCard
import io.github.jinlinahida.shirokowear.ui.UnstableShirokoWearApi
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

import androidx.activity.compose.BackHandler

enum class SettingsSubScreen {
    MAIN,
    CONTENT_SCALE,
}

/**
 * Settings screen for Cherish on Wear OS.
 *
 * Provides control over:
 * - Home display layout (List / Grid)
 * - Event ordering
 * - Content scaling (font & layout)
 * - Tactile haptics master switch
 * - About Cherish
 */
@OptIn(UnstableShirokoWearApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    eventCount: Int,
    onUpdateSettings: (AppSettings) -> Unit,
    onNavigateToEventOrder: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var draftScale by remember(settings.contentScale) { mutableStateOf(settings.contentScale) }
    val haptics = rememberShirokoWearHaptics()

    BackHandler(enabled = activeSubScreen != SettingsSubScreen.MAIN) {
        haptics.back()
        activeSubScreen = SettingsSubScreen.MAIN
    }

    ShirokoWearAmbient(spotlightKey = "cherish_settings") {
        AnimatedContent(
            targetState = activeSubScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settingsSubScreens",
            modifier = modifier.fillMaxSize(),
        ) { subScreen ->
            when (subScreen) {
                SettingsSubScreen.MAIN -> {
                    ShirokoWearScalingRotaryColumn(
                        modifier = Modifier.fillMaxSize(),
                        itemSpacing = 6.dp,
                        contentPadding = ShirokoWearTheme.dimens.screenPadding,
                    ) {
                        item(key = "title") {
                            ShirokoWearScreenTitle(
                                text = "设置",
                                modifier = Modifier.semantics { heading() },
                            )
                        }

                        // Display Group Header
                        item(key = "group_display_header") {
                            Text(
                                text = "显示",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShirokoWearTheme.colors.accentGold,
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 2.dp)
                                    .semantics { heading() },
                            )
                        }

                        // Home Display Mode
                        item(key = "item_display_mode") {
                            val modeLabel = if (settings.homeViewMode == HomeViewMode.LIST) "单列列表" else "双列网格"
                            ShirokoWearSettingsItem(
                                title = "首页布局",
                                subtitle = modeLabel,
                                onClick = {
                                    haptics.click()
                                    val nextMode = if (settings.homeViewMode == HomeViewMode.LIST) {
                                        HomeViewMode.GRID
                                    } else {
                                        HomeViewMode.LIST
                                    }
                                    onUpdateSettings(settings.copy(homeViewMode = nextMode))
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "首页布局",
                                        currentValue = modeLabel,
                                        actionHint = "点击切换",
                                    )
                                },
                            )
                        }

                        // Event Order
                        item(key = "item_event_order") {
                            ShirokoWearSettingsItem(
                                title = "事件排序",
                                subtitle = "$eventCount 个事件",
                                onClick = {
                                    haptics.click()
                                    onNavigateToEventOrder()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "事件排序，共 $eventCount 个事件，点击调整顺序"
                                },
                            )
                        }

                        // Content Scale
                        item(key = "item_content_scale") {
                            val scaleLabel = when (settings.contentScale) {
                                AppContentScale.SMALL -> "小 (90%)"
                                AppContentScale.STANDARD -> "标准 (100%)"
                                AppContentScale.LARGE -> "大 (110%)"
                            }
                            ShirokoWearSettingsItem(
                                title = "内容缩放",
                                subtitle = scaleLabel,
                                onClick = {
                                    haptics.click()
                                    draftScale = settings.contentScale
                                    activeSubScreen = SettingsSubScreen.CONTENT_SCALE
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = AccessibilityPresentation.buildEditorFieldAccessibilityDescription(
                                        fieldName = "内容缩放",
                                        currentValue = scaleLabel,
                                        actionHint = "点击选择",
                                    )
                                },
                            )
                        }

                        // Interaction Group Header
                        item(key = "group_interaction_header") {
                            Text(
                                text = "交互",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShirokoWearTheme.colors.accentGold,
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 4.dp)
                                    .semantics { heading() },
                            )
                        }

                        // Haptics Master Switch
                        item(key = "item_haptics") {
                            ShirokoWearToggleCard(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings(settings.copy(hapticsEnabled = enabled))
                                },
                                label = "触觉震动",
                                secondaryLabel = if (settings.hapticsEnabled) "按键与滚轮微震反馈" else "已静音",
                                confirmEnableAudibly = true,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.semantics {
                                    role = Role.Switch
                                    stateDescription = AccessibilityPresentation.buildSettingsHapticsStateDescription(settings.hapticsEnabled)
                                    contentDescription = "触觉震动"
                                },
                            )
                        }

                        // Other Group Header
                        item(key = "group_other_header") {
                            Text(
                                text = "其他",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShirokoWearTheme.colors.accentGold,
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 4.dp)
                                    .semantics { heading() },
                            )
                        }

                        // About
                        item(key = "item_about") {
                            ShirokoWearSettingsItem(
                                title = "关于 Cherish",
                                subtitle = "版本 0.1.0",
                                onClick = {
                                    haptics.click()
                                    onNavigateToAbout()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "关于 Cherish，版本 0.1.0，点击查看详情"
                                },
                            )
                        }

                        // Back Button
                        item(key = "action_back") {
                            Spacer(modifier = Modifier.height(4.dp))
                            ShirokoWearCardButton(
                                onClick = {
                                    haptics.back()
                                    onBack()
                                },
                                modifier = Modifier.semantics {
                                    role = Role.Button
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

                SettingsSubScreen.CONTENT_SCALE -> {
                    ContentScalePickerSection(
                        currentScale = draftScale,
                        onScaleChange = { draftScale = it },
                        onConfirm = {
                            haptics.click()
                            onUpdateSettings(settings.copy(contentScale = draftScale))
                            activeSubScreen = SettingsSubScreen.MAIN
                        },
                    )
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun SettingsScreenRoundStandardPreview() {
    val sampleSettings = AppSettings(
        homeViewMode = HomeViewMode.LIST,
        contentScale = AppContentScale.STANDARD,
        hapticsEnabled = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.STANDARD,
        screenShape = ShirokoWearScreenShape.ROUND,
    ) {
        SettingsScreen(
            settings = sampleSettings,
            eventCount = 8,
            onUpdateSettings = {},
            onNavigateToEventOrder = {},
            onNavigateToAbout = {},
            onBack = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun SettingsScreenRoundLargePreview() {
    val sampleSettings = AppSettings(
        homeViewMode = HomeViewMode.LIST,
        contentScale = AppContentScale.LARGE,
        hapticsEnabled = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.ROUND,
    ) {
        SettingsScreen(
            settings = sampleSettings,
            eventCount = 8,
            onUpdateSettings = {},
            onNavigateToEventOrder = {},
            onNavigateToAbout = {},
            onBack = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun SettingsScreenSquareStandardPreview() {
    val sampleSettings = AppSettings(
        homeViewMode = HomeViewMode.GRID,
        contentScale = AppContentScale.STANDARD,
        hapticsEnabled = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.STANDARD,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        SettingsScreen(
            settings = sampleSettings,
            eventCount = 8,
            onUpdateSettings = {},
            onNavigateToEventOrder = {},
            onNavigateToAbout = {},
            onBack = {},
        )
    }
}

@Preview(device = "id:wearos_rect", showSystemUi = true)
@Composable
private fun SettingsScreenSquareLargePreview() {
    val sampleSettings = AppSettings(
        homeViewMode = HomeViewMode.GRID,
        contentScale = AppContentScale.LARGE,
        hapticsEnabled = true,
    )
    ShirokoWearTheme(
        contentScale = ShirokoWearContentScale.LARGE,
        screenShape = ShirokoWearScreenShape.SQUARE,
    ) {
        SettingsScreen(
            settings = sampleSettings,
            eventCount = 8,
            onUpdateSettings = {},
            onNavigateToEventOrder = {},
            onNavigateToAbout = {},
            onBack = {},
        )
    }
}

