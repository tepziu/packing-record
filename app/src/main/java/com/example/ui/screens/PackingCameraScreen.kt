package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.barcode.BarcodeAnalyzer
import com.example.domain.model.PackingSession
import com.example.recording.RecordingState
import com.example.ui.components.OrderDetailDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.StatusRecording
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PackingCameraScreen(
    viewModel: CameraViewModel,
    onNavigateToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val location by viewModel.locationCoordinates.collectAsState()

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (permissionsState.allPermissionsGranted) {
            // CameraX Preview and ImageAnalysis binding
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    BarcodeAnalyzer { barcodeResult ->
                                        viewModel.onBarcodeScanned(barcodeResult.rawValue)
                                    }
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraInstance = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis,
                                viewModel.videoRecorderManager.videoCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Torch controller effect
            LaunchedEffect(uiState.isTorchOn) {
                cameraInstance?.cameraControl?.enableTorch(uiState.isTorchOn)
            }

            // Zoom controller effect
            LaunchedEffect(uiState.zoomRatio) {
                try {
                    cameraInstance?.cameraControl?.setZoomRatio(uiState.zoomRatio)
                } catch (_: Exception) {}
            }
        } else {
            // Permission request fallback UI
            PermissionFallback(
                onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
            )
        }

        var showManualInputDialog by remember { mutableStateOf(false) }

        // Realtime Laser Scanner Reticle (Center Guide)
        ScannerReticleOverlay(
            isRecording = recordingState is RecordingState.RecordingInProgress,
            modifier = Modifier.align(Alignment.Center)
        )

        // Top Watermark (Chỉ hiển thị ngày giờ ở góc, không che nội dung quay)
        TopWatermarkHud(
            timeString = uiState.liveClockString,
            isRecording = recordingState is RecordingState.RecordingInProgress,
            orderCode = when (val s = recordingState) {
                is RecordingState.RecordingInProgress -> s.orderCode
                else -> null
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(10.dp)
        )

        // Top-Right Quick Action Buttons (Zoom, Manual Input, Torch, Mic)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Quick Zoom Button (1x / 2x)
            IconButton(
                onClick = {
                    val nextZoom = if (uiState.zoomRatio >= 2.0f) 1.0f else 2.0f
                    viewModel.setZoom(nextZoom)
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(42.dp)
            ) {
                Text(
                    text = "${uiState.zoomRatio.toInt()}x",
                    color = if (uiState.zoomRatio > 1.0f) AccentAmber else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Manual Barcode Input Button
            IconButton(
                onClick = { showManualInputDialog = true },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Manual Code",
                    tint = PrimaryBlueLight
                )
            }

            // Torch Button
            IconButton(
                onClick = { viewModel.toggleTorch(!uiState.isTorchOn) },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = if (uiState.isTorchOn) AccentAmber else Color.White
                )
            }

            // Audio Mute/Unmute
            IconButton(
                onClick = { viewModel.toggleAudio() },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Audio",
                    tint = if (uiState.isAudioEnabled) StatusSuccess else Color.White
                )
            }
        }

        // Bottom State & Control Panel
        BottomControlHud(
            recordingState = recordingState,
            minDebounceSec = userSession.minRecordDebounceSeconds,
            onManualStop = { viewModel.manualStopRecording() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Mismatched Barcode Warning Banner
        AnimatedVisibility(
            visible = uiState.mismatchWarning != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.mismatchWarning?.let { warning ->
                MismatchWarningCard(
                    warning = warning,
                    onDismiss = { viewModel.dismissMismatchWarning() }
                )
            }
        }

        // Manual Order Input Dialog
        if (showManualInputDialog) {
            ManualOrderInputDialog(
                initialValue = uiState.lastScannedCode ?: "",
                isRecording = recordingState is RecordingState.RecordingInProgress,
                onConfirm = { code ->
                    showManualInputDialog = false
                    viewModel.startManualRecording(code)
                },
                onDismiss = { showManualInputDialog = false }
            )
        }

        var previewSessionForDialog by remember { mutableStateOf<PackingSession?>(null) }

        // Success Dialog on Session Completed
        if (uiState.showSuccessDialog && uiState.latestCompletedSession != null) {
            CompletedSessionDialog(
                session = uiState.latestCompletedSession!!,
                onDismiss = { viewModel.dismissSuccessDialog() },
                onViewQueue = {
                    viewModel.dismissSuccessDialog()
                    onNavigateToQueue()
                },
                onPreviewVideo = {
                    val completed = uiState.latestCompletedSession
                    viewModel.dismissSuccessDialog()
                    previewSessionForDialog = completed
                }
            )
        }

        // Preview Video with Watermark Dialog
        previewSessionForDialog?.let { session ->
            OrderDetailDialog(
                session = session,
                onDismiss = { previewSessionForDialog = null }
            )
        }
    }
}

@Composable
fun TopWatermarkHud(
    timeString: String,
    isRecording: Boolean,
    orderCode: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xB3000000) // Semi-transparent black background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (isRecording) Color(0xFFEF4444) else Color(0xFF34D399),
                        CircleShape
                    )
            )
            Text(
                text = timeString,
                color = Color(0xFFFDE047), // Gold Yellow
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (!orderCode.isNullOrBlank()) {
                Text(
                    text = "• $orderCode",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ScannerReticleOverlay(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    val frameColor = if (isRecording) StatusRecording else PrimaryBlueLight

    Box(
        modifier = modifier
            .size(260.dp, 160.dp)
            .border(
                width = 2.dp,
                color = frameColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Red / Cyan Laser Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (isRecording) Color.Red else Color.Cyan,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun BottomControlHud(
    recordingState: RecordingState,
    minDebounceSec: Int,
    onManualStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD0F172A)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(Color(0x6638BDF8), Color(0x221E293B))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (recordingState) {
                is RecordingState.Idle -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CHỜ QUÉT MÃ ĐƠN ĐỂ QUAY TỰ ĐỘNG",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Đưa mã vạch / QR đơn hàng vào khung hình",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                is RecordingState.RecordingInProgress -> {
                    val durationSec = recordingState.durationMillis / 1000
                    val minutes = durationSec / 60
                    val seconds = durationSec % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(StatusRecording, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "REC: ${recordingState.orderCode}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (recordingState.canStop)
                                        "✓ Quét lại mã này để TỰ ĐỘNG DỪNG"
                                    else
                                        "Đang quay tối thiểu (${minDebounceSec}s)...",
                                    color = if (recordingState.canStop) StatusSuccess else AccentAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = timeFormatted,
                            color = StatusRecording,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onManualStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusRecording
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DỪNG & LƯU VIDEO (THỦ CÔNG)")
                    }
                }

                is RecordingState.Processing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryBlueLight,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Đang nhúng Watermark & tạo mã băm SHA-256...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                is RecordingState.Completed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đã lưu video thành công!",
                            color = StatusSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is RecordingState.Error -> {
                    Text(
                        text = recordingState.message,
                        color = StatusRecording,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MismatchWarningCard(
    warning: com.example.ui.viewmodel.MismatchWarning,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7F1D1D) // Dark Crimson
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Color(0xFFEF4444))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFCA5A5),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CẢNH BÁO: MÃ ĐƠN KHÔNG KHỚP!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Đang đóng đơn [${warning.currentOrder}] nhưng bạn vừa quét phải mã [${warning.scannedDifferentOrder}].",
                color = Color(0xFFFEE2E2),
                fontSize = 12.sp
            )
            Text(
                text = "● Video đóng hàng HIỆN TẠI KHÔNG BỊ DỪNG để đảm bảo toàn vẹn dữ liệu.",
                color = Color(0xFFFEF08A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Đã hiểu")
            }
        }
    }
}

@Composable
fun CompletedSessionDialog(
    session: PackingSession,
    onDismiss: () -> Unit,
    onViewQueue: () -> Unit,
    onPreviewVideo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đóng Hàng Hoàn Tất!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Mã Đơn: ${session.orderCode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thời lượng: ${session.durationMs / 1000}s | Kích thước: ${session.fileSizeBytes / 1024} KB",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mã băm SHA-256 (Toàn vẹn video):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    Text(
                        text = session.videoHash.take(32) + "...",
                        color = PrimaryBlueLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPreviewVideo,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("▶ Xem Lại Video & Watermark", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ Video đã được lưu Room DB và thêm vào Hàng đợi Tự động Upload Google Drive.",
                    fontSize = 11.sp,
                    color = StatusSuccess
                )
            }
        },
        confirmButton = {
            Button(onClick = onViewQueue) {
                Text("Hàng Đợi")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Đóng Đơn Mới")
            }
        }
    )
}

@Composable
fun PermissionFallback(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = PrimaryBlueLight,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Yêu Cầu Quyền Camera & Vị Trí",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ứng dụng cần quyền Camera (quét mã và quay video), Microphone (ghi âm) và GPS (watermark tọa độ đóng hàng).",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueLight)
        ) {
            Text("Cấp Quyền Ngay", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ManualOrderInputDialog(
    initialValue: String,
    isRecording: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRecording) "Quét / Nhập Mã Kết Thúc" else "Nhập Mã Đơn Hàng Thủ Công",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = if (isRecording)
                        "Nhập đúng mã đơn đang quay để kết thúc đóng hàng:"
                    else
                        "Dành cho trường hợp mã vạch bị mờ, rách hoặc không quét được:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.uppercase() },
                    placeholder = { Text("VD: SPX12345678VN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (isRecording) "Xác Nhận Kết Thúc" else "Bắt Đầu Quay")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

