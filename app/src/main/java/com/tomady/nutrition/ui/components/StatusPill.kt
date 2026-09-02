package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/** Small rounded pill used for status indicators (AI model state, food safety, ...). */
@Composable
fun StatusPill(
    text: String,
    tint: Color,
    tintBackground: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
    loading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .background(tintBackground, RoundedCornerShape(999.dp))
            .then(clickableModifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(10.dp), color = tint, strokeWidth = 1.5.dp)
        } else if (showDot) {
            Box(modifier = Modifier.size(6.dp).background(tint, CircleShape))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/** Convenience presets matching the RN app's AIStatusBadge states. */
object AiStatusPillPresets {
    @Composable
    fun Ready() = StatusPill(
        text = "IA locale prête",
        tint = TomadyColors.violetDeep,
        tintBackground = TomadyColors.violetTint
    )

    @Composable
    fun Mock(onDownload: () -> Unit) = StatusPill(
        text = "Mode démo — télécharger",
        tint = TomadyColors.amberDeep,
        tintBackground = TomadyColors.amberTint,
        onClick = onDownload
    )

    @Composable
    fun Downloading(progress: Float) = StatusPill(
        text = "Téléchargement… ${(progress * 100).toInt()}%",
        tint = TomadyColors.violetDeep,
        tintBackground = TomadyColors.violetTint,
        loading = true,
        showDot = false
    )

    @Composable
    fun Unavailable(onRetry: () -> Unit) = StatusPill(
        text = "Modèle indisponible",
        tint = TomadyColors.coral,
        tintBackground = TomadyColors.coralTint,
        onClick = onRetry
    )
}
