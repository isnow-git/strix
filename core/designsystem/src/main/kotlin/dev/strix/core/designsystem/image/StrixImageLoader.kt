package dev.strix.core.designsystem.image

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

/**
 * Builds the app-wide Coil 3 [ImageLoader], installed once via the Application's
 * `SingletonImageLoader.Factory` so every `AsyncImage` shares one memory + disk cache.
 *
 * Tuned for thousands of channel logos on a low-RAM TV:
 * - The bitmap memory cache is sized from the device heap ([MemoryCacheSizer]) rather
 *   than Coil's default share, trading a few extra decodes for GC headroom.
 * - Logos are persisted on disk so scrolling back through the catalogue never re-hits
 *   the network — the dominant first-pass scroll cost.
 * - Coil 3 already decodes each image down to the composable's constraints, so cached
 *   bitmaps stay small; crossfade is off so a fast D-pad sweep doesn't queue per-item
 *   fade animations that cause jank.
 */
fun strixImageLoader(context: Context): ImageLoader {
    val memoryClassMb = context.getSystemService<ActivityManager>()?.memoryClass ?: DEFAULT_MEMORY_CLASS_MB
    val cacheBytes = MemoryCacheSizer.bitmapCacheBytes(memoryClassMb)
    return ImageLoader
        .Builder(context)
        .memoryCache {
            MemoryCache
                .Builder()
                .maxSizeBytes(cacheBytes.toLong())
                .build()
        }.diskCache {
            DiskCache
                .Builder()
                .directory(context.cacheDir.resolve("strix_logos").toOkioPath())
                .maxSizeBytes(LOGO_DISK_CACHE_BYTES)
                .build()
        }.crossfade(false)
        .build()
}

private const val DEFAULT_MEMORY_CLASS_MB = 64
private const val LOGO_DISK_CACHE_BYTES = 48L * 1024 * 1024
