package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRecording
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.UploadQueueViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UploadQueueScreen(
    viewModel: UploadQueueViewModel,
    modifier: Modifier = Modifier
) {
    val queueItems by viewModel.queueItems.collectAsState()

    val pendingCount = queueItems.count { it.uploadStatus == UploadStatus.PENDING }
    val uploadingCount = queueItems.count { it.uploadStatus == UploadStatus.UPLOADING }
    val failedCount = queueItems.count { it.uploadStatus == UploadStatus.FAILED }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hàng Đợi Đồng Bộ",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tự động tải lên Google Drive qua WorkManager khi có mạng",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { viewModel.syncNow() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đồng Bộ Ngay", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary metrics bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QueueMetricCard(
                    title = "Chờ Tải",
                    count = pendingCount,
                    color = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
                QueueMetricCard(
                    title = "Đang Tải",
                    count = uploadingCount,
                    color = PrimaryBlueLight,
                    modifier = Modifier.weight(1f)
                )
                QueueMetricCard(
                    title = "Lỗi / Thử Lại",
                    count = failedCount,
                    color = StatusRecording,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queueItems.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tất Cả Video Đã Được Đồng Bộ!",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Không có video nào trong hàng đợi chờ tải lên.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(queueItems, key = { it.id }) { item ->
                        QueueItemCard(
                            session = item,
                            onRetry = { viewModel.retryItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueMetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QueueItemCard(
    session: PackingSession,
    onRetry: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(session.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (session.uploadStatus) {
                            UploadStatus.PENDING -> Icons.Default.CloudQueue
                            UploadStatus.UPLOADING -> Icons.Default.CloudUpload
                            UploadStatus.COMPLETED -> Icons.Default.CloudDone
                            UploadStatus.FAILED -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (session.uploadStatus) {
                            UploadStatus.PENDING -> AccentAmber
                            UploadStatus.UPLOADING -> PrimaryBlueLight
                            UploadStatus.COMPLETED -> StatusSuccess
                            UploadStatus.FAILED -> StatusRecording
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ĐƠN: ${session.orderCode}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Thời gian: $dateStr • ${session.fileSizeBytes / (1024 * 1024)} MB",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (session.uploadStatus == UploadStatus.FAILED) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = PrimaryBlueLight
                        )
                    }
                } else {
                    Surface(
                        color = when (session.uploadStatus) {
                            UploadStatus.PENDING -> AccentAmber.copy(alpha = 0.2f)
                            UploadStatus.UPLOADING -> PrimaryBlueLight.copy(alpha = 0.2f)
                            UploadStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.2f)
                            UploadStatus.FAILED -> StatusRecording.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (session.uploadStatus) {
                                UploadStatus.PENDING -> "Chờ mạng"
                                UploadStatus.UPLOADING -> "Đang tải"
                                UploadStatus.COMPLETED -> "Xong"
                                UploadStatus.FAILED -> "Lỗi (${session.retryCount})"
                            },
                            color = when (session.uploadStatus) {
                                UploadStatus.PENDING -> AccentAmber
                                UploadStatus.UPLOADING -> PrimaryBlueLight
                                UploadStatus.COMPLETED -> StatusSuccess
                                UploadStatus.FAILED -> StatusRecording
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (session.uploadStatus == UploadStatus.UPLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryBlueLight,
                    trackColor = Color(0xFF334155)
                )
            }

            if (!session.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lỗi: ${session.errorMessage}",
                    color = StatusRecording,
                    fontSize = 11.sp
                )
            }
        }
    }
}
