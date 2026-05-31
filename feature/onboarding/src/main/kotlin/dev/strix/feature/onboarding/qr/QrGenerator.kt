package dev.strix.feature.onboarding.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Renders a URL as a QR code [ImageBitmap] for the onboarding screen. */
object QrGenerator {
    fun encode(
        text: String,
        sizePx: Int = DEFAULT_SIZE_PX,
    ): ImageBitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap.asImageBitmap()
    }

    private const val DEFAULT_SIZE_PX = 512
}
