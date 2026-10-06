package com.cherish.app.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Empty state component displayed when no events exist in the repository.
 *
 * Provides a clean placeholder with ShirokoWear styling, indicating the app is ready
 * for countdown events without presenting a broken or barren UI.
 */
@Composable
fun HomeEmptyState(
    modifier: Modifier = Modifier,
    onAddPlaceholderClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🗓️",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.clearAndSetSemantics { },
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "还没有倒数事件",
            style = MaterialTheme.typography.bodyMedium,
            color = ShirokoWearTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "记录每一个珍贵瞬间",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Visual placeholder button for future event creation
        ShirokoWearCard(
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "添加事件"
                role = Role.Button
            },
            shape = ShirokoWearShapes.cardCompact,
            innerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            outerPadding = PaddingValues(0.dp),
            fillMaxWidth = false,
            onClick = onAddPlaceholderClick,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "添加事件",
                style = MaterialTheme.typography.labelSmall,
                color = ShirokoWearTheme.colors.cardHighlight,
            )
        }
    }
}
