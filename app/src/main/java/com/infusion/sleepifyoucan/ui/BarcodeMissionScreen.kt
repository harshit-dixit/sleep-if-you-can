package com.infusion.sleepifyoucan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent

@Composable
fun BarcodeMissionScreen(
    expectedBarcode: String?,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var scanResult by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            startBarcodeScan(context) { result ->
                scanResult = result
                isScanning = false
            }
        }
    }
    
    // Handle scan result
    LaunchedEffect(scanResult) {
        scanResult?.let { code ->
            onBarcodeScanned(code)
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
                text = "We need camera access to scan QR codes and barcodes",
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
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackMute)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scan QR Code or Barcode",
                style = MaterialTheme.typography.headlineLarge,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val instructionText = if (expectedBarcode != null) {
                "Scan a code that matches: $expectedBarcode"
            } else {
                "Scan any QR code or barcode"
            }
            
            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isScanning) {
                CircularProgressIndicator(
                    color = OrangeAccent,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Opening camera scanner...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            } else {
                Button(
                    onClick = {
                        isScanning = true
                        startBarcodeScan(context) { result ->
                            scanResult = result
                            isScanning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    modifier = Modifier.size(120.dp)
                ) {
                    Text(
                        text = "📱\nScan",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Show last scanned result if any
            scanResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.DarkGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanned Code:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OrangeAccent
                        )
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.ui.graphics.Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Position the QR code or barcode within the camera frame",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun startBarcodeScan(
    context: android.content.Context,
    onResult: (String?) -> Unit
) {
    val integrator = IntentIntegrator(context as android.app.Activity)
    integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
    integrator.setPrompt("Scan a QR code or barcode")
    integrator.setCameraId(0) // Use back camera
    integrator.setBeepEnabled(true)
    integrator.setBarcodeImageEnabled(false)
    integrator.setOrientationLocked(false)
    
    // Set up result handler
    integrator.initiateScan()
    
    // Note: In a real implementation, you'd handle the result in onActivityResult
    // For this demo, we'll simulate a successful scan
    // In production, you'd need to integrate with the activity's onActivityResult
}
