package dev.strix.feature.channels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.strix.core.designsystem.focus.focusRing
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.feature.channels.R

/**
 * The category filter rail ("Toutes" + canonical categories). Reads only the category
 * list and the current selection, so it is frozen during list scrolling and focus.
 */
@Composable
fun CategoryRail(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth().padding(top = 18.dp),
    ) {
        item {
            CategoryChip(
                label = stringResource(R.string.channels_category_all),
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories) { category ->
            CategoryChip(label = category, selected = selected == category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier = Modifier.focusRing(cornerRadius = 20.dp),
    ) {
        val fill =
            if (selected) {
                Modifier.background(StrixPalette.Primary, RoundedCornerShape(20.dp))
            } else {
                Modifier.glass(RoundedCornerShape(20.dp))
            }
        Box(modifier = fill.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = if (selected) StrixPalette.OnPrimary else StrixPalette.OnBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
