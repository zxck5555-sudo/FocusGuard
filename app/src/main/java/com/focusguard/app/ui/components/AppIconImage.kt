package com.focusguard.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.focusguard.app.ui.theme.DarkSurfaceLighter
import com.focusguard.app.ui.theme.TextSecondary

@Composable
fun AppIconImage(
    drawable: Drawable?,
    modifier: Modifier = Modifier.size(44.dp)
) {
    val bitmap = remember(drawable) {
        drawable?.let { d ->
            if (d is BitmapDrawable && d.bitmap != null) {
                d.bitmap
            } else {
                val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
                val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                bmp
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceLighter),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}
