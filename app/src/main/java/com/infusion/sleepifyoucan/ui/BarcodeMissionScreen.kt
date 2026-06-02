package com.infusion.sleepifyoucan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.infusion.sleepifyoucan.ui.theme.AccentBlue
import com.infusion.sleepifyoucan.ui.theme.Ash
import com.infusion.sleepifyoucan.ui.theme.Body
import com.infusion.sleepifyoucan.ui.theme.Canvas
import com.infusion.sleepifyoucan.ui.theme.Hairline
import com.infusion.sleepifyoucan.ui.theme.Ink
import com.infusion.sleepifyoucan.ui.theme.Mute
import com.infusion.sleepifyoucan.ui.theme.Surface
import com.infusion.sleepifyoucan.ui.theme.SurfaceCard
import com.infusion.sleepifyoucan.ui.theme.SurfaceElevated
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun BarcodeMissionScreen(
    expectedBarcode: String,
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

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedCode = result.contents ?: return@rememberLauncherForActivityResult
        scanResult = scannedCode
        onBarcodeScanned(scannedCode)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            scanLauncher.launch(barcodeScanOptions())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Hairline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.QrCodeScanner else Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (hasPermission) "Scan your code" else "Camera access needed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (hasPermission) {
                            "Match the registered barcode to stop the alarm."
                        } else {
                            "Barcode missions need the camera before the scanner can open."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Body,
                        textAlign = TextAlign.Center
                    )
                }

                CodePreview(expectedBarcode)

                Button(
                    onClick = {
                        if (hasPermission) {
                            scanLauncher.launch(barcodeScanOptions())
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Canvas
                    )
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.QrCodeScanner else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasPermission) "Scan code" else "Grant access")
                }

                scanResult?.let { result ->
                    LastScanResult(result)
                }
            }
        }
    }
}

@Composable
private fun CodePreview(expectedBarcode: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Registered code",
            style = MaterialTheme.typography.labelMedium,
            color = Mute
        )
        Text(
            text = expectedBarcode,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LastScanResult(result: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last scan",
            style = MaterialTheme.typography.labelMedium,
            color = Ash,
            modifier = Modifier.weight(0.34f)
        )
        Text(
            text = result,
            style = MaterialTheme.typography.bodyMedium,
            color = Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.66f)
        )
    }
}

private fun barcodeScanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
    setPrompt("Scan the registered code")
    setCameraId(0)
    setBeepEnabled(true)
    setBarcodeImageEnabled(false)
    setOrientationLocked(false)
}
