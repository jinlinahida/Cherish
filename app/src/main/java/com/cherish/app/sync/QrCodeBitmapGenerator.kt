package com.cherish.app.sync

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utility for generating high-contrast QR Code bitmaps for Wear OS display.
 */
object QrCodeBitmapGenerator {

    fun generateQrMatrix(
        content: String,
        size: Int = 240,
    ): com.google.zxing.common.BitMatrix {
        require(size > 0) { "Size must be positive" }

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )

        return QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints,
        )
    }

    /**
     * Encodes [content] into a square [Bitmap] of dimensions [size]x[size].
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 240,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
    ): Bitmap {
        val bitMatrix = generateQrMatrix(content, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
