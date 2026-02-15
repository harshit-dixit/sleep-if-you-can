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
import androidx.lifecycle.LifecycleOwner
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCancellableCoroutine

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
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
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
                .background(BlackMute)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineMedium,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "We need camera access to take photos for the mission",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Text("Grant Camera Permission")
            }
        }
    } else if (capturedImageUri != null) {
        // Photo captured screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackMute)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Photo Captured!",
                style = MaterialTheme.typography.headlineLarge,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Make sure your photo shows: $requiredObject",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        capturedImageUri = null
                        showCamera = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangeAccent
                    )
                ) {
                    Text("Retake Photo")
                }
                
                Button(
                    onClick = onPhotoTaken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Confirm Photo")
                }
            }
        }
    } else if (showCamera) {
        // Camera screen
        CameraView(
            onImageCaptured = { uri ->
                capturedImageUri = uri
                showCamera = false
            },
            onError = { error ->
                // Handle error - for now just hide camera
                showCamera = false
            }
        )
    } else {
        // Initial screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackMute)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Take a Photo!",
                style = MaterialTheme.typography.headlineLarge,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Take a photo that shows: $requiredObject",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { showCamera = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Text("Open Camera")
            }
        }
    }
}

@Composable
private fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    // Preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    // Image capture
                    imageCapture = ImageCapture.Builder().build()
                    
                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    // Bind to lifecycle
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
    
    // Camera controls overlay
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
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
                .padding(32.dp)
                .size(80.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = androidx.compose.ui.graphics.Color.Black
            )
        ) {
            Text("📸", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val imageCapture = imageCapture ?: return
    
    // Create output file
    val outputFile = createOutputFile(context)
    
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(outputFile)
                onImageCaptured(savedUri)
            }
            
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun createOutputFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.cacheDir
    return File(storageDir, "IMG_$timeStamp.jpg")
}
