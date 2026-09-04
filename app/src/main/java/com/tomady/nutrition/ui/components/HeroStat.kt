package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomady.nutrition.ui.theme.TomadyColors

/**
 * One large number with a caption underneath and an optional thin progress
 * meter — the "single big number per screen" hero stat (remaining calories
 * on Dashboard, a food/dish's kcal on its detail screen).
 */
@Composable
fun HeroStat(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueFontSize: androidx.compose.ui.unit.TextUnit = 52.sp,
    progress: Float? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Light,
            color = TomadyColors.ink
        )
        Text(
            caption,
            style = MaterialTheme.typography.bodyMedium,
            color = TomadyColors.muted,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(3.dp)
                    .background(TomadyColors.line, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(TomadyColors.ink, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
