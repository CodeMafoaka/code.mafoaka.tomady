package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/**
 * One chat message bubble: a solid pill (tail on the bottom-right) for the
 * user, a bordered card (tail on the bottom-left) for the assistant.
 */
@Composable
fun ChatBubble(text: String, isUser: Boolean, modifier: Modifier = Modifier) {
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 20.dp)
    }
    val background = if (isUser) TomadyColors.line else TomadyColors.card

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(background, shape)
                .then(if (!isUser) Modifier.border(1.dp, TomadyColors.line, shape) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TomadyColors.inkSoft)
        }
    }
}
