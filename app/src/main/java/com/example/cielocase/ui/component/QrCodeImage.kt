package com.example.cielocase.ui.component

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cielocase.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
fun QrCodeImage(payload: String, modifier: Modifier = Modifier, size: Dp = 176.dp) {
    val sizeInPixels = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(payload, sizeInPixels) { encodeQrCode(payload, sizeInPixels) }
        ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = stringResource(R.string.ticket_qr_description),
        modifier = modifier.size(size),
    )
}

private fun encodeQrCode(payload: String, sizeInPixels: Int): Bitmap? = runCatching {
    val matrix: BitMatrix = MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        sizeInPixels,
        sizeInPixels,
        mapOf(EncodeHintType.MARGIN to 1),
    )
    createBitmap(matrix.width, matrix.height).apply {
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                this[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
    }
}.getOrNull()
