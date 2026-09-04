package com.tomady.nutrition.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/** A rounded, right-aligned-value text field — the editable field style used on Profile. */
@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        shape = TomadyFieldShape,
        colors = tomadyFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}
