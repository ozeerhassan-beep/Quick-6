package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.CartItemEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.util.EnumMap

object CartBarcodeEncoder {

    /**
     * Builds a structured JSON payload encoding cart item identifiers and quantities.
     * Compact schema:
     * {
     *   "v": 1,
     *   "t": totalItemsCount,
     *   "ts": timestamp,
     *   "items": [
     *     {"id": 101, "sku": "CAT-101", "q": 2, "p": 45.0, "n": "Lait Candia"},
     *     ...
     *   ]
     * }
     */
    fun createCartPayload(cartItems: List<CartItemEntity>): String {
        val root = JSONObject()
        root.put("v", 1)
        root.put("total", cartItems.sumOf { it.quantity })
        root.put("timestamp", System.currentTimeMillis())

        val itemsArray = JSONArray()
        for (item in cartItems) {
            val itemObj = JSONObject()
            itemObj.put("id", item.productId)
            itemObj.put("sku", "${item.catalogType}-${item.productId}")
            itemObj.put("qty", item.quantity)
            itemObj.put("name", item.productName.take(24))
            itemObj.put("price", item.unitPrice)
            itemsArray.put(itemObj)
        }
        root.put("items", itemsArray)
        return root.toString()
    }

    /**
     * Generates a high-contrast Bitmap from a string content (QR Code or Aztec / DataMatrix).
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        format: BarcodeFormat = BarcodeFormat.QR_CODE
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 2)
                if (format == BarcodeFormat.QR_CODE) {
                    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                }
            }

            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(content, format, width, height, hints)

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
