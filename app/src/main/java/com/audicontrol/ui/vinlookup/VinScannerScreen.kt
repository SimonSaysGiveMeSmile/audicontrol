package com.audicontrol.ui.vinlookup

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.audicontrol.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

private val VIN_REGEX = Regex("[A-HJ-NPR-Z0-9]{17}")

@Composable
fun VinScannerScreen(
    onVinDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    var hasPermission by remember { mutableStateOf(false) }
    var detectedVin by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (detectedVin != null) {
        LaunchedEffect(detectedVin) {
            onVinDetected(detectedVin!!)
        }
        return
    }

    if (!hasPermission) {
        Box(
            Modifier.fillMaxSize().background(AudiBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required", color = AudiWhite)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = AudiRed)
                ) {
                    Text("GRANT PERMISSION")
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        CameraPreviewWithAnalysis { vin ->
            if (detectedVin == null) {
                detectedVin = vin
            }
        }

        ScannerOverlay()

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SCAN VIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = AudiWhite,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AudiWhite)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Point camera at the VIN on windshield or door jamb",
                style = MaterialTheme.typography.bodySmall,
                color = AudiGreyLight
            )
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(onVinFound: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            processImage(imageProxy, recognizer, onVinFound)
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onVinFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            val fullText = result.text.replace("\\s".toRegex(), "").uppercase()
            val match = VIN_REGEX.find(fullText)
            if (match != null) {
                onVinFound(match.value)
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

@Composable
private fun ScannerOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val overlayColor = Color.Black.copy(alpha = 0.6f)
        drawRect(overlayColor)

        val boxWidth = size.width * 0.85f
        val boxHeight = size.height * 0.08f
        val left = (size.width - boxWidth) / 2
        val top = size.height * 0.4f

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(8f, 8f),
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = Color(0xFFBB1E10),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 3f)
        )
    }
}
