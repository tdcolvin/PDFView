package com.tdcolvin.pdfview

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import java.io.File
import kotlin.math.max
import kotlin.math.sqrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import de.mr_pine.zoomables.DragGestureMode
import de.mr_pine.zoomables.Zoomable
import de.mr_pine.zoomables.ZoomableState
import de.mr_pine.zoomables.rememberZoomableState

@Composable
fun PDFView(
    modifier: Modifier = Modifier,
    file: File,
    approximateAspectRatioForPage: (Int) -> Float = { sqrt(2f) },   //the default is the aspect ratio for A* paper
    onErrorLoadingFile: (Exception) -> Unit = { _ -> },
    onErrorRenderingPage: (Int, Exception) -> Unit = { _, _ -> }
) {
    var openPDFRendererJob by remember { mutableStateOf<Job?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    // The PDFRenderer library requires that we only open a single page at any one time. So the
    // PDFPages each need to wait their turn, and they share this object to facilitate that.
    val mutexForOnePageAtATime = Mutex()

    DisposableEffect(file) {
        openPDFRendererJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                renderer = PdfRenderer(
                    ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                )

                yield()

                pageCount = renderer?.pageCount ?: 0
            }
            catch (e: Exception) {
                Log.e("pdfview", "Error loading PDF file $file", e)

                onErrorLoadingFile(e)
            }
        }

        onDispose {
            openPDFRendererJob?.cancel()
            renderer?.close()
            renderer = null
        }
    }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val zoomableState = rememberZoomableState(
        rotationBehavior = ZoomableState.Rotation.DISABLED,
        onTransformation = { zoomChange: Float, panChange: Offset, _: Float ->
            scale.value = max(1f, scale.value * zoomChange)
            offset.value += Offset(panChange.x, 0f)
            scope.launch { lazyListState.scrollBy(-panChange.y) }
        }
    )

    Zoomable(
        coroutineScope = scope,
        zoomableState = zoomableState,
        dragGestureMode = { DragGestureMode.PAN }
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            userScrollEnabled = false,
            state = lazyListState
        ) {
            items(pageCount) { pageNum ->
                renderer?.let { renderer ->
                    PDFPage(
                        pdfRenderer = renderer,
                        pageNum = pageNum,
                        approximateAspectRatio = approximateAspectRatioForPage(pageNum),
                        mutexForOnePageAtATime = mutexForOnePageAtATime,
                        onErrorRendering = { onErrorRenderingPage(pageNum, it) }
                    )
                }
            }
        }
    }
}