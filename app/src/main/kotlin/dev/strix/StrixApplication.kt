package dev.strix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. Hilt's object graph is rooted here. */
@HiltAndroidApp
class StrixApplication : Application()
