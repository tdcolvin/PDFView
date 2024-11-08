package com.tdcolvin.pdfviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.tdcolvin.pdfview.PDFView
import com.tdcolvin.pdfviewer.ui.theme.PDFViewerTheme
import de.mr_pine.zoomables.DragGestureMode
import de.mr_pine.zoomables.Zoomable
import de.mr_pine.zoomables.ZoomableState
import de.mr_pine.zoomables.rememberZoomableState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.sqrt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFViewerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PDFViewExample(modifier = Modifier.padding(innerPadding))
                    //ZoomablesSample(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PDFViewExample(modifier: Modifier = Modifier) {
    var pdfFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val cacheFile = File.createTempFile("sample-pdf", ".pdf", context.cacheDir)

        context.resources.openRawResource(R.raw.a17flightplan).use { pdfInputStream ->
            FileOutputStream(cacheFile).use { cacheOutputStream ->
                val bytes = ByteArray(524288)
                var bytesRead: Int
                do {
                    bytesRead = pdfInputStream.read(bytes, 0, bytes.size)
                    if (bytesRead > 0) {
                        cacheOutputStream.write(bytes, 0, bytesRead)
                    }
                } while (bytesRead > 0)
            }
        }

        pdfFile = cacheFile
    }

    pdfFile?.let {
        PDFView(modifier = modifier, it)
    }
}



