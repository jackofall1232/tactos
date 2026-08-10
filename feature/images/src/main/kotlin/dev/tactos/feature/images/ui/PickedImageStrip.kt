package dev.tactos.feature.images.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tactos.feature.images.engine.ImageLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thumbnails of the picked images with name and dimensions. Decoding is a
 * tiny downsampled BitmapFactory pass per item — a handful of user-picked
 * images doesn't warrant an image-loading library (deliberate no-Coil
 * decision, ADR-0004).
 */
@Composable
internal fun PickedImageStrip(
    picked: List<Uri>,
    infos: Map<Uri, ImageLoading.Info?>,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(picked, key = { it.toString() }) { uri ->
            val context = LocalContext.current
            var thumb by remember(uri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(uri) {
                thumb = withContext(Dispatchers.IO) {
                    ImageLoading.decode(context, uri, maxDimension = THUMB_DIMENSION)
                }
            }
            val info = infos[uri]
            Column(modifier = Modifier.width(96.dp)) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    thumb?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = info?.displayName ?: "Picked image",
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Text(
                    text = info?.displayName ?: "…",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                info?.let {
                    Text(
                        text = "${it.width}×${it.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val THUMB_DIMENSION = 256
