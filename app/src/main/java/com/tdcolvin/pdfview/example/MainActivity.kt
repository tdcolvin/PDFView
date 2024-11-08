package com.tdcolvin.pdfview.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tdcolvin.pdfview.PDFView
import com.tdcolvin.pdfview.example.theme.ui.PDFViewerTheme
import java.io.File
import java.io.FileOutputStream


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



