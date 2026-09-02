package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/** Rounded, bordered container used for grouped content across every screen. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TomadyColors.card, RoundedCornerShape(20.dp))
            .border(1.dp, TomadyColors.line, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}
