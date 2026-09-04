package com.tomady.nutrition.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/** Shared rounded-corner shape for text fields, matching the redesign's card-toned fields. */
val TomadyFieldShape = RoundedCornerShape(16.dp)

/** Shared text field colors: hairline border + card-toned fill in both states. */
@Composable
fun tomadyFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TomadyColors.line,
    unfocusedBorderColor = TomadyColors.line,
    focusedContainerColor = TomadyColors.card,
    unfocusedContainerColor = TomadyColors.card
)
