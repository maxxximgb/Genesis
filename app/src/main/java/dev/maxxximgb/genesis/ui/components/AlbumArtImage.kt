package dev.maxxximgb.genesis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import dev.maxxximgb.genesis.data.media.AlbumArt
import dev.maxxximgb.genesis.ui.theme.Corner
import dev.maxxximgb.genesis.ui.theme.Sizes

@Composable
fun AlbumArtImage(
    albumId: Long?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.albumArtSmall,
    /** Fraction of the tile filled by the music-note placeholder when art is missing. */
    placeholderFraction: Float = 0.5f,
) {
    val shape = RoundedCornerShape(Corner.sm)
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder lives in the outer Box's centered slot so its `size()` modifier
        // actually constrains it. SubcomposeAsyncImage forces fillMaxSize on its content
        // slot, which would otherwise stretch the placeholder to the full tile.
        PlaceholderIcon(fraction = placeholderFraction)
        if (albumId != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AlbumArt.uri(albumId))
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderIcon(fraction: Float) {
    // Proportional sizing: a wrapper Box claims [fraction] of the tile (fillMaxSize sets
    // both min and max), then the inner Icon fills that Box. We can't apply
    // fillMaxSize(fraction) directly to Icon — its internal paint() modifier prefers the
    // painter's intrinsic 24dp size and ignores the fraction unless the surrounding layout
    // forces a fixed box.
    Box(
        modifier = Modifier.fillMaxSize(fraction),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
