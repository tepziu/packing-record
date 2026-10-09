package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.ui.components.OrderDetailDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.StatusRecording
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PackingHistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    val filter by viewModel.filter.collectAsState()

    var selectedSessionForDetail by remember { mutableStateOf<PackingSession?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Lịch Sử Đơn Đã Đóng",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Danh sách video đã quay có gắn mã băm SHA-256 & Watermark",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Tìm theo mã đơn, nhân viên, hash...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryBlueLight
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlueLight,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (All, Completed, Pending, Failed)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = filter.statusFilter == null,
                        onClick = { viewModel.updateStatusFilter(null) },
                        label = { Text("Tất cả (${sessions.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlueLight,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filter.statusFilter == UploadStatus.COMPLETED,
                        onClick = { viewModel.updateStatusFilter(UploadStatus.COMPLETED) },
                        label = { Text("Đã Tải Lên", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusSuccess,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filter.statusFilter == UploadStatus.PENDING,
                        onClick = { viewModel.updateStatusFilter(UploadStatus.PENDING) },
                        label = { Text("Chờ Tải", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filter.statusFilter == UploadStatus.FAILED,
                        onClick = { viewModel.updateStatusFilter(UploadStatus.FAILED) },
                        label = { Text("Lỗi", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusRecording,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Không tìm thấy phiên đóng hàng nào",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
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
                    items(sessions, key = { it.id }) { item ->
                        HistoryItemCard(
                            session = item,
                            onClick = { selectedSessionForDetail = item }
                        )
                    }
                }
            }
        }

        selectedSessionForDetail?.let { session ->
            OrderDetailDialog(
                session = session,
                onDismiss = { selectedSessionForDetail = null }
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    session: PackingSession,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(session.startTimeDevice))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.orderCode,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (session.uploadStatus) {
                            UploadStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.2f)
                            UploadStatus.UPLOADING -> PrimaryBlueLight.copy(alpha = 0.2f)
                            UploadStatus.PENDING -> AccentAmber.copy(alpha = 0.2f)
                            UploadStatus.FAILED -> StatusRecording.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (session.uploadStatus) {
                                UploadStatus.COMPLETED -> "Drive Đã Lưu"
                                UploadStatus.UPLOADING -> "Đang Tải"
                                UploadStatus.PENDING -> "Chờ Tải"
                                UploadStatus.FAILED -> "Lỗi Tải"
                            },
                            color = when (session.uploadStatus) {
                                UploadStatus.COMPLETED -> StatusSuccess
                                UploadStatus.UPLOADING -> PrimaryBlueLight
                                UploadStatus.PENDING -> AccentAmber
                                UploadStatus.FAILED -> StatusRecording
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Giờ: $dateStr • ${session.durationMs / 1000}s",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Text(
                    text = "NV: ${session.employeeId} | Kho: ${session.warehouseId}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
                Text(
                    text = "SHA: ${session.videoHash.take(16)}...",
                    color = PrimaryBlueLight.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play Video",
                tint = PrimaryBlueLight,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
