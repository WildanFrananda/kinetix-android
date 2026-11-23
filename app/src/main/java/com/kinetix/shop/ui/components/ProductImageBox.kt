package com.kinetix.shop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kinetix.shop.core.config.ImageUrlSanitizer
import com.kinetix.shop.ui.theme.KinetixPalette

@Composable
fun ProductImageBox(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val safeUrl = ImageUrlSanitizer.sanitizeHttpsUrl(imageUrl) ?: return
    AsyncImage(
        model = safeUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(KinetixPalette.ElevatedSlate),
    )
}
