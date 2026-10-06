package com.cherish.app.editor.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
 * Notes / remarks editor section for Wear OS.
 */
@Composable
fun NotesPickerSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quickNotes = listOf(
        "提前准备礼物",
        "预订餐厅",
        "买花送祝福",
        "提前准备材料",
        "重要提醒",
        "清空备注",
    )

    ShirokoWearScalingRotaryColumn(
        modifier = modifier.fillMaxSize(),
        itemSpacing = 6.dp,
        contentPadding = ShirokoWearTheme.dimens.screenPadding,
    ) {
        item(key = "title") {
            ShirokoWearScreenTitle(
                text = "事件备注",
                modifier = Modifier.semantics { heading() },
            )
        }

        item(key = "notes_input") {
            ShirokoWearCard(
                shape = ShirokoWearShapes.cardCompact,
                innerPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                BasicTextField(
                    value = notes,
                    onValueChange = { onNotesChange(it.take(100)) },
                    textStyle = TextStyle(
                        color = ShirokoWearTheme.colors.contentPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(ShirokoWearTheme.colors.accentGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "事件备注输入框"
                        },
                    decorationBox = { innerTextField ->
                        if (notes.isEmpty()) {
                            Text(
                                text = "输入事件备注 (选填)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }
        }

        item(key = "quick_notes_header") {
            Text(
                text = "快捷备注",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .semantics { heading() },
            )
        }

        val chunks = quickNotes.chunked(2)
        items(
            count = chunks.size,
            key = { "quick_note_row_$it" },
        ) { chunkIdx ->
            val chunk = chunks[chunkIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (item in chunk) {
                    val isSelected = notes == item
                    ShirokoWearSelectableButton(
                        selected = isSelected,
                        onClick = {
                            if (item == "清空备注") {
                                onNotesChange("")
                            } else {
                                onNotesChange(item)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.RadioButton
                                selected = isSelected
                                contentDescription = item
                            },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        item(key = "confirm_btn") {
            Spacer(modifier = Modifier.height(4.dp))
            ShirokoWearCardButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    contentDescription = "确定备注"
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
