package com.github.sebastianp265.aitrainer.shared.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        action()
    }
}

@Composable
internal fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun InfoNote(title: String?, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f))
            action?.invoke()
        }
    }
}

@Composable
internal fun ChipButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) { Text(text) }
}

@Composable
internal fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().heightIn(min = 52.dp),
            placeholder = placeholder?.let { { Text(it) } },
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.outline,
                unfocusedContainerColor = colors.surface,
                focusedContainerColor = colors.surface,
            ),
        )
    }
}

@Composable
internal fun CoachButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Icon(DesignIcons.Coach, contentDescription = null, Modifier.padding(end = 8.dp))
        Text(text)
    }
}

@Composable
internal fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(modifier, shape = MaterialTheme.shapes.small, color = containerColor, contentColor = contentColor) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun ListRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Column((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).then(modifier).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
internal fun DesignOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        content = content,
    )
}

@Composable
internal fun DesignFilterChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.height(36.dp),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            labelColor = colors.onSurface,
            selectedContainerColor = colors.secondaryContainer,
            selectedLabelColor = colors.onSecondaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outline,
            selectedBorderColor = colors.secondaryContainer,
        ),
    )
}

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                Modifier.height(52.dp).background(colors.surfaceContainer, CircleShape).padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DesignIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    field()
                }
            }
        },
    )
}

@Composable
internal fun DashedButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier.fillMaxWidth().height(48.dp)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(content = content)
    }
}

@Composable
internal fun GroupRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).then(modifier).fillMaxWidth()
            .heightIn(min = 60.dp).padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
internal fun IconCircle(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
