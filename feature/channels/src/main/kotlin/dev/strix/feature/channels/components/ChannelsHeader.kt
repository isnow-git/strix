package dev.strix.feature.channels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.strix.core.designsystem.focus.focusRing
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette

/**
 * The frozen top bar: title + channel count, search field and change-source button. It
 * reads only the search query and count, so list scrolling and focus changes never
 * recompose it.
 */
@Composable
fun ChannelsHeader(
    query: String,
    channelCount: Int,
    searchFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    onChangeSource: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text(text = "Strix", color = StrixPalette.OnBackground, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (channelCount > 0) {
                Text(text = "$channelCount chaînes", color = StrixPalette.Muted, fontSize = 13.sp)
            }
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(searchFocus)
                    .onFocusChanged { onSearchFocusChanged(it.isFocused) },
        )
        GlassButton(label = "Guide TV", onClick = onOpenGuide)
        GlassButton(label = "Changer la source", onClick = onChangeSource)
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = StrixPalette.OnBackground, fontSize = 16.sp),
        cursorBrush = SolidColor(StrixPalette.OnBackground),
        modifier =
            modifier
                .focusRing(cornerRadius = 12.dp)
                .glass(RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(text = "Rechercher une chaîne", color = StrixPalette.Muted, fontSize = 16.sp)
            }
            inner()
        },
    )
}
