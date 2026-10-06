package com.cherish.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeBitmapGeneratorTest {

    @Test
    fun `generateQrMatrix constructs valid square BitMatrix`() {
        val testUrl = "http://192.168.1.105:8080/?token=abc123xyz"
        val size = 200
        val matrix = QrCodeBitmapGenerator.generateQrMatrix(testUrl, size)

        assertNotNull(matrix)
        assertEquals(size, matrix.width)
        assertEquals(size, matrix.height)

        // Find at least one dark and one light cell
        var hasDark = false
        var hasLight = false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) hasDark = true else hasLight = true
            }
        }
        assertTrue("QR code must have dark modules", hasDark)
        assertTrue("QR code must have light modules", hasLight)
    }
}
