package dev.strix.core.ui.image

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache

/**
 * Builds the app-wide Coil [ImageLoader] with a bitmap memory cache sized from
 * the device heap via [MemoryCacheSizer].
 *
 * Channel logos are decoded straight to their display size by Coil (it samples
 * down to the target bounds), so the cache only ever holds small bitmaps — key
 * for thousands of logos on a low-RAM TV. [allowRgb565] halves the memory of
 * opaque logos (transparent ones keep their alpha), and crossfade is disabled so
 * fast D-pad scrolling doesn't queue per-item animations that cause jank.
 */
fun strixImageLoader(context: Context): ImageLoader {
    val memoryClassMb = context.getSystemService<ActivityManager>()?.memoryClass ?: DEFAULT_MEMORY_CLASS_MB
    val cacheBytes = MemoryCacheSizer.bitmapCacheBytes(memoryClassMb)
    return ImageLoader
        .Builder(context)
        .memoryCache {
            MemoryCache
                .Builder(context)
                .maxSizeBytes(cacheBytes)
                .build()
        }.allowRgb565(true)
        .crossfade(false)
        .build()
}

/**
 * Returns the app-wide Coil loader (installed by the `Application` via
 * `ImageLoaderFactory` — see `StrixApplication`), so every screen and every
 * `AsyncImage` share one memory and disk cache instead of each building its own.
 * Where no factory is installed (previews/tests) Coil's default loader is used.
 */
@Composable
fun rememberStrixImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) { context.applicationContext.imageLoader }
}

private const val DEFAULT_MEMORY_CLASS_MB = 64
