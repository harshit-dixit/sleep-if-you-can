package com.infusion.sleepifyoucan.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.infusion.sleepifyoucan.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotoMissionScreen(
    requiredObject: String,
    onPhotoTaken: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var showCamera by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            showCamera = true
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            showCamera = true
        }
    }
    
    if (!hasPermission) {
        // Permission request screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Charcoal)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineMedium,
                color = Terracotta,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "We need camera access to verify your photo mission",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = TextOnAccent
                )
            ) {
                Text("Grant Camera Permission")
            }
        }
    } else if (isProcessing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Charcoal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Terracotta)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Analyzing Photo...", color = Terracotta)
        }
    } else if (feedbackMessage != null) {
        // Verification Failed Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Charcoal)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Try Again!",
                style = MaterialTheme.typography.headlineLarge,
                color = DustyRose,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = feedbackMessage ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    feedbackMessage = null
                    showCamera = true 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = TextOnAccent
                )
            ) {
                Text("Retake Photo")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Required: $requiredObject",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        // Camera View
        CameraView(
            requiredObject = requiredObject,
            onImageCaptured = { file ->
                isProcessing = true
                showCamera = false
                verifyImage(context, file, requiredObject) { success, topLabel ->
                    isProcessing = false
                    if (success) {
                        onPhotoTaken()
                    } else {
                        feedbackMessage = "That doesn't look like a $requiredObject.\n" +
                                (if (topLabel != null) "We saw a $topLabel." else "Could not identify object.")
                    }
                }
            },
            onError = { 
                // Handle capture error by showing camera again
                showCamera = true
            }
        )
    }
}

private fun verifyImage(
    context: Context,
    file: File,
    requiredObject: String,
    onResult: (Boolean, String?) -> Unit
) {
    val image: InputImage
    try {
        image = InputImage.fromFilePath(context, Uri.fromFile(file))
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false, null)
        return
    }

    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    
    labeler.process(image)
        .addOnSuccessListener { labels ->
            val topLabel = labels.maxByOrNull { it.confidence }?.text
            
            // Flexible matching: check if any label contains the required object string
            // or if the required object contains the label (e.g. required="Cup" label="Coffee Cup")
            val isMatch = labels.any { label -> 
                label.text.contains(requiredObject, ignoreCase = true) || 
                requiredObject.contains(label.text, ignoreCase = true)
            }
            
            onResult(isMatch, topLabel)
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            onResult(false, null)
        }
}

@Composable
private fun CameraView(
    requiredObject: String,
    onImageCaptured: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        // Handle error
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(WarmBlack.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take a photo of:",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = requiredObject,
                color = Terracotta,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        // Capture Button
        Button(
            onClick = {
                takePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                contentColor = TextOnAccent
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Photo",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun createOutputFile(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, "SleepIfYouCan").apply { mkdirs() }
    }
    return File(
        mediaDir ?: context.filesDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onImageCaptured: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val imageCapture = imageCapture ?: return

    val outputFile = createOutputFile(context)
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Pass the file, not just URI, because ML Kit handles Files easily with fromFilePath
                onImageCaptured(outputFile)
            }
            
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
