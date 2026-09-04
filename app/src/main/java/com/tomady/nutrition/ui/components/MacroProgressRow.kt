package com.tomady.nutrition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/** A labeled macro (protein/carbs/fat) row with a progress bar, e.g. "Protéines 64/120g". */
@Composable
fun MacroProgressRow(
    label: String,
    consumed: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = if (goal > 0) (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TomadyColors.ink)
            Text(
                text = "$consumed/${goal}g",
                style = MaterialTheme.typography.bodySmall,
                color = TomadyColors.muted
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp)
                .height(3.dp)
                .background(TomadyColors.line, RoundedCornerShape(2.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
