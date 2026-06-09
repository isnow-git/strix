package dev.strix.core.designsystem.image

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MemoryCacheSizerTest {
    @Test
    fun `uses one eighth of the heap by default`() {
        // 256 MB heap -> 32 MB cache.
        assertThat(MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 256))
            .isEqualTo(32 * 1024 * 1024)
    }

    @Test
    fun `honours a custom fraction`() {
        assertThat(MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 200, fraction = 0.25))
            .isEqualTo(50 * 1024 * 1024)
    }

    @Test
    fun `clamps to a minimum floor for tiny heaps`() {
        // 16 MB heap * 1/8 = 2 MB, below the 4 MB floor.
        assertThat(MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 16))
            .isEqualTo(4 * 1024 * 1024)
    }

    @Test
    fun `clamps huge values into Int range`() {
        val bytes = MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 40_000, fraction = 1.0)
        assertThat(bytes).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `rejects non-positive memory class`() {
        assertFailsWithIae { MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 0) }
    }

    @Test
    fun `rejects out-of-range fraction`() {
        assertFailsWithIae { MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 100, fraction = 0.0) }
        assertFailsWithIae { MemoryCacheSizer.bitmapCacheBytes(memoryClassMb = 100, fraction = 1.5) }
    }

    private fun assertFailsWithIae(block: () -> Unit) {
        assertThat(runCatching { block() }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
