package dev.strix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.strix.core.ui.image.strixImageLoader

/**
 * Application entry point. Hilt's object graph is rooted here.
 *
 * Implements [ImageLoaderFactory] so Coil's singleton — used by every `AsyncImage`
 * across the app — is the heap-sized [strixImageLoader], giving one shared memory
 * and disk cache instead of a separate loader per screen.
 */
@HiltAndroidApp
class StrixApplication :
    Application(),
    ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = strixImageLoader(this)
}
