package dev.strix.core.ui.image

/**
 * Derives the in-memory bitmap cache size from the device's per-app heap budget
 * (`ActivityManager.memoryClass`, in MB).
 *
 * On low-RAM TVs the default Coil cache (a fixed share of the heap) can still be
 * too aggressive, so we take a conservative fraction and clamp it, trading a few
 * extra decodes for headroom that keeps GC pauses (and zapping jank) down.
 */
object MemoryCacheSizer {
    private const val BYTES_PER_MB = 1024 * 1024
    private const val DEFAULT_FRACTION = 0.125 // 1/8 of the heap
    private const val MIN_BYTES = 4 * BYTES_PER_MB

    /**
     * @param memoryClassMb value from `ActivityManager.memoryClass` (MB).
     * @param fraction share of the heap to devote to the bitmap cache, in (0, 1].
     * @return cache size in bytes, at least [MIN_BYTES] and within `Int` range.
     */
    fun bitmapCacheBytes(
        memoryClassMb: Int,
        fraction: Double = DEFAULT_FRACTION,
    ): Int {
        require(memoryClassMb > 0) { "memoryClassMb must be > 0, was $memoryClassMb" }
        require(fraction > 0.0 && fraction <= 1.0) { "fraction must be in (0, 1], was $fraction" }
        val raw = memoryClassMb.toLong() * BYTES_PER_MB * fraction
        return raw.toLong().coerceIn(MIN_BYTES.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }
}
