package dev.strix.core.ui.image

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import coil.ImageLoader
import coil.memory.MemoryCache

/**
 * Builds the app-wide Coil [ImageLoader] with a bitmap memory cache sized from
 * the device heap via [MemoryCacheSizer].
 *
 * Channel logos are decoded straight to their display size by Coil (it samples
 * down to the target bounds), so the cache only ever holds small bitmaps — key
 * for thousands of logos on a low-RAM TV.
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
        }.build()
}

private const val DEFAULT_MEMORY_CLASS_MB = 64
