package com.tomady.nutrition.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.theme.TomadyColors

/** Fully-rounded pill search bar — the search affordance used on LogMeal and Catalogue. */
@Composable
fun PillSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TomadyColors.muted) },
        singleLine = true,
        shape = RoundedCornerShape(999.dp),
        colors = tomadyFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}
