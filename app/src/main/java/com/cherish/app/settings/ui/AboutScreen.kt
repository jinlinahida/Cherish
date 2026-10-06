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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * About screen presenting version, design system, and technical metadata.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = "0.1.0",
) {
    val haptics = rememberShirokoWearHaptics()

    ShirokoWearAmbient(spotlightKey = "cherish_about") {
        ShirokoWearScalingRotaryColumn(
            modifier = modifier.fillMaxSize(),
            itemSpacing = 6.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
        ) {
            item(key = "title") {
                ShirokoWearScreenTitle(
                    text = "关于",
                    modifier = Modifier.semantics { heading() },
                )
            }

            // App Identity Header Card
            item(key = "header_card") {
                ShirokoWearCard(
                    fillMaxWidth = true,
                    innerPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    outerPadding = PaddingValues(vertical = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Cherish，Wear OS 优雅倒数日"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "⌛ Cherish",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ShirokoWearTheme.colors.accentGold,
                        )
                        Text(
                            text = "Wear OS 优雅倒数日",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Specs / Details Card
            item(key = "details_card") {
                ShirokoWearCard(
                    fillMaxWidth = true,
                    innerPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    outerPadding = PaddingValues(vertical = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AboutDetailRow(label = "版本", value = "v$versionName")
                        AboutDetailRow(label = "平台", value = "Wear OS")
                        AboutDetailRow(label = "设计系统", value = "ShirokoWearUI")
                        AboutDetailRow(label = "存储架构", value = "本地原子 JSON")
                        AboutDetailRow(label = "开源协议", value = "MIT")
                    }
                }
            }

            // Back Action Button
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
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label：$value"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = ShirokoWearTheme.colors.contentPrimary,
        )
    }
}
