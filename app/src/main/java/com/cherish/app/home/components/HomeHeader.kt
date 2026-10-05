package com.cherish.app.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.home.model.HomeViewMode
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

/**
 * Header section of the Cherish Home screen.
 *
 * Displays the page title adapted to bezel shapes via [ShirokoWearScreenTitle],
 * along with a subtle view mode toggle (List / Grid) with haptic feedback.
 */
@Composable
fun HomeHeader(
    viewMode: HomeViewMode,
    hasEvents: Boolean,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier,
    onAddEventClick: (() -> Unit)? = null,
) {
    val haptics = rememberShirokoWearHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShirokoWearScreenTitle(
            text = "Cherish",
            marquee = true,
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onAddEventClick != null) {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.cardCompact,
                    innerPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    outerPadding = PaddingValues(0.dp),
                    fillMaxWidth = false,
                    onClick = {
                        haptics.click()
                        onAddEventClick()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+ 添加",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShirokoWearTheme.colors.accentGold,
                    )
                }
            }

            if (hasEvents) {
                ShirokoWearCard(
                    shape = ShirokoWearShapes.cardCompact,
                    innerPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    outerPadding = PaddingValues(0.dp),
                    fillMaxWidth = false,
                    onClick = {
                        haptics.click()
                        onToggleViewMode()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    val modeLabel = if (viewMode == HomeViewMode.LIST) "⊞ 网格" else "☰ 列表"
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShirokoWearTheme.colors.accentGold,
                    )
                }
            }
        }
    }
}
