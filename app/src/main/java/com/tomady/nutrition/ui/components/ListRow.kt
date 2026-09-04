package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/**
 * One line in a row-list — Journal entries, Catalogue/search results, recipe
 * ingredients: a title (+ optional subtitle) on the left, an optional
 * trailing value+unit and/or a chevron on the right. Rows in the same
 * [SectionCard] are meant to be stacked directly with [showDivider] true on
 * every row but the first, so they read as one continuous list.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingValue: String? = null,
    trailingUnit: String? = null,
    showChevron: Boolean = false,
    titleColor: Color = TomadyColors.ink,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TomadyColors.line))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TomadyColors.muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (trailingValue != null) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                    Text(trailingValue, style = MaterialTheme.typography.bodyLarge, color = TomadyColors.ink)
                    if (trailingUnit != null) {
                        Text(trailingUnit, style = MaterialTheme.typography.labelSmall, color = TomadyColors.muted)
                    }
                }
            }
            if (showChevron) {
                Text(
                    "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = TomadyColors.muted,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
