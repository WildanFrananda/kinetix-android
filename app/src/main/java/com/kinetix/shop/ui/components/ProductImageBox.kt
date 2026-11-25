package com.kinetix.shop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kinetix.shop.R
import com.kinetix.shop.core.config.ImageUrlSanitizer

@Composable
fun ProductImageBox(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val safeUrl = ImageUrlSanitizer.sanitizeHttpsUrl(imageUrl)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small,
            ),
    ) {
        if (safeUrl != null) {
            AsyncImage(
                model = safeUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_kinetix_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(0.25f),
            )
        }
    }
}
