package com.tdcolvin.pdfview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun PDFPage(
    pdfRenderer: PdfRenderer,
    pageNum: Int,
    approximateAspectRatio: Float,
    mutexForOnePageAtATime: Mutex,
    onErrorRendering: (Exception) -> Unit = { _ -> }
) {
    var pageImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(pdfRenderer, pageNum) {
        withContext(Dispatchers.IO) {
            Log.v("pdfv", "Opening page $pageNum")
            try {
                mutexForOnePageAtATime.withLock {
                    pdfRenderer.openPage(pageNum).use {
                        val pageBitmap =
                            Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
                        it.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageImage = pageBitmap.asImageBitmap()
                    }
                }
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                Log.e("pdfv", "Failed on page $pageNum (${e.message})")

                onErrorRendering(e)
            }
        }
        Log.v("pdfv", "Bitmap created for p$pageNum")
    }

    val currentPageImage = pageImage

    if (currentPageImage != null){
        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = currentPageImage,
            contentDescription = "Page $pageNum"
        )
    }
    else {
        Box(modifier = Modifier.aspectRatio(approximateAspectRatio))
    }
}