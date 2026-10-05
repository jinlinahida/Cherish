package com.cherish.app.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.ShirokoWearSelectableButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearShapes
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Event title and emoji editor screen for Wear OS.
 *
 * Provides:
 * - Direct text entry for custom title
 * - Quick-pick title presets optimized for watch tap input
 * - Curated preset emoji pills
 */
@Composable
fun TitleEmojiPickerSection(
    title: String,
    emoji: String,
    onTitleChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quickTitles = listOf("生日", "纪念日", "新年", "考试", "旅行", "发薪日")
    val quickEmojis = listOf("🎂", "💗", "🎓", "✈️", "🎆", "🌟", "💼", "🏖️", "🚗", "🎁", "📅", "💍")

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(text = "名称与图标")
        }

        // Text input card
        item(key = "text_input") {
            ShirokoWearCard(
                shape = ShirokoWearShapes.cardCompact,
                innerPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    BasicTextField(
                        value = title,
                        onValueChange = { onTitleChange(it.take(50)) },
                        textStyle = TextStyle(
                            color = ShirokoWearTheme.colors.contentPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(ShirokoWearTheme.colors.accentGold),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "输入事件名称...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
        }

        // Quick titles label
        item(key = "quick_titles_label") {
            Text(
                text = "快捷名称",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // Quick title chips
        val titleChunks = quickTitles.chunked(3)
        items(
            count = titleChunks.size,
            key = { "title_row_$it" },
        ) { chunkIdx ->
            val chunk = titleChunks[chunkIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (preset in chunk) {
                    ShirokoWearSelectableButton(
                        selected = title == preset,
                        onClick = { onTitleChange(preset) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Quick emojis label
        item(key = "quick_emojis_label") {
            Text(
                text = "选择图标",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // Quick emoji chips
        val emojiChunks = quickEmojis.chunked(4)
        items(
            count = emojiChunks.size,
            key = { "emoji_row_$it" },
        ) { chunkIdx ->
            val chunk = emojiChunks[chunkIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (item in chunk) {
                    ShirokoWearSelectableButton(
                        selected = emoji == item,
                        onClick = { onEmojiChange(item) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = item,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Confirm button
        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(onClick = onConfirm) {
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
