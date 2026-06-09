package dev.strix

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import dev.strix.core.designsystem.image.strixImageLoader

/**
 * Application entry point. Hilt's object graph is rooted here, and the app-wide Coil 3
 * singleton is installed via [SingletonImageLoader.Factory] so every `AsyncImage`
 * shares one heap-sized memory + disk cache instead of building its own.
 */
@HiltAndroidApp
class StrixApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader = strixImageLoader(this)
}
