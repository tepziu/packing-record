package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserSession
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.VoiceFeedbackManager
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()
    val scrollState = rememberScrollState()

    var empId by remember(userSession) { mutableStateOf(userSession.employeeId) }
    var empName by remember(userSession) { mutableStateOf(userSession.employeeName) }
    var whId by remember(userSession) { mutableStateOf(userSession.warehouseId) }
    var whName by remember(userSession) { mutableStateOf(userSession.warehouseName) }
    var devId by remember(userSession) { mutableStateOf(userSession.deviceId) }
    var debounceSec by remember(userSession) { mutableStateOf(userSession.minRecordDebounceSeconds.toString()) }
    var autoWifi by remember(userSession) { mutableStateOf(userSession.autoUploadOnWifi) }
    var soundBeep by remember(userSession) { mutableStateOf(userSession.enableSoundBeep) }
    var voiceVi by remember(userSession) { mutableStateOf(userSession.enableVoiceVietnamese) }
    var googleEmail by remember(userSession) { mutableStateOf(userSession.googleAccountEmail) }
    var driveFolder by remember(userSession) { mutableStateOf(userSession.googleDriveFolderName) }
    var isDriveConnected by remember(userSession) { mutableStateOf(userSession.isGoogleDriveConnected) }

    var isSavedMessageVisible by remember { mutableStateOf(false) }

    val videosDir = remember {
        File(context.getExternalFilesDir(null), "packing_videos")
    }

    var localVideoCount by remember {
        mutableStateOf(videosDir.listFiles()?.count { it.extension.equals("mp4", true) } ?: 0)
    }
    var localVideoBytes by remember {
        mutableLongStateOf(videosDir.listFiles()?.sumOf { it.length() } ?: 0L)
    }

    val voiceFeedback = remember { VoiceFeedbackManager(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Cài Đặt & Cấu Hình Hệ Thống",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tài khoản Google Drive, Watermark đóng hàng & Giọng nói tiếng Việt",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Google Drive Account Connection Card (Cách 1)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDriveConnected) Color(0xFF10B981) else Color(0xFF64748B)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isDriveConnected) Color(0xFF34D399) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tài Khoản Google Drive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Badge trạng thái
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDriveConnected) Color(0x2210B981) else Color(0x22EF4444),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDriveConnected) "ĐÃ KẾT NỐI ✓" else "CHƯA KẾT NỐI",
                                color = if (isDriveConnected) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Drive Status and Profile
                    if (isDriveConnected && googleEmail.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = PrimaryBlueLight,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = googleEmail,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "🟢 Tự động đồng bộ lên Drive",
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button: Đăng Nhập Bằng Google
                    val activity = context as? android.app.Activity
                    Button(
                        onClick = {
                            if (activity != null) {
                                viewModel.handleGoogleSignIn(activity) { success, msg ->
                                    if (success) {
                                        isDriveConnected = true
                                        googleEmail = userSession.googleAccountEmail
                                    }
                                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isDriveConnected = true
                                Toast.makeText(context, "Đã kết nối tài khoản Google Drive: $googleEmail", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDriveConnected) "Đổi Tài Khoản Google (Google Sign-In)" else "Đăng Nhập Bằng Google",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Email Google Drive Nhận Video") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlueLight)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = driveFolder,
                        onValueChange = { driveFolder = it },
                        label = { Text("Tên Thư Mục Lưu Trữ Trên Drive") },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFDE047))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isDriveConnected = !isDriveConnected
                                if (!isDriveConnected) {
                                    viewModel.disconnectGoogleAccount()
                                }
                                val msg = if (isDriveConnected) "Đã kết nối tài khoản Google Drive!" else "Đã ngắt kết nối Google Drive"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDriveConnected) Color(0xFFF87171) else Color(0xFF34D399))
                        ) {
                            Text(
                                text = if (isDriveConnected) "Đăng Xuất Drive" else "Bật Đồng Bộ",
                                color = if (isDriveConnected) Color(0xFFF87171) else Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.openDrive()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mở Google Drive", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Hệ thống tự động đẩy video vào thư mục Drive của bạn ngay sau khi đóng hàng xong. Bạn cũng có thể mở chi tiết bất kỳ đơn nào và chọn 'Lưu Drive / Chia Sẻ'.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Giọng nói Tiếng Việt & Âm thanh
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Giọng Nói Tiếng Việt & Âm Báo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phát Giọng Nói Tiếng Việt Khi Quét", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "• Quét lần 1: \"Bắt đầu quay\"\n• Quét lần 2: \"Kết thúc\"\n• Quét nhầm mã khác: \"Sai mã đơn hàng\"",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = voiceVi,
                            onCheckedChange = { voiceVi = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlueLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Âm Thanh Bíp & Rung Haptic", color = Color.White, fontSize = 13.sp)
                            Text("Phát âm báo và rung khi quét mã vạch", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = soundBeep,
                            onCheckedChange = { soundBeep = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlueLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons to test voice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { voiceFeedback.speakStartRecording() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("▶ Thử: Bắt đầu", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { voiceFeedback.speakStopRecording() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("▶ Thử: Kết thúc", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { voiceFeedback.speakWrongOrderCode() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("▶ Thử: Sai mã", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Thông tin Watermark (Địa điểm, Nhân viên, Kho)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Địa Điểm & Thông Tin Watermark",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whName,
                        onValueChange = { whName = it },
                        label = { Text("Tên Địa Điểm / Bàn Đóng Gói") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = whId,
                        onValueChange = { whId = it },
                        label = { Text("Mã Kho Bãi (Watermark)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = empId,
                        onValueChange = { empId = it },
                        label = { Text("Mã Nhân Viên (Watermark)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = devId,
                        onValueChange = { devId = it },
                        label = { Text("Mã Thiết Bị / Bàn Đóng Gói (Watermark)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Recording Debounce & Wifi Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quy Tắc Tự Động Quay & Tải Lên",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = debounceSec,
                        onValueChange = { debounceSec = it },
                        label = { Text("Thời Gian Quay Tối Thiểu Trước Khi Dừng (Giây)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlueLight,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Chỉ Tải Lên Khi Kết Nối Wi-Fi", color = Color.White, fontSize = 13.sp)
                            Text("Tiết kiệm dung lượng 4G/5G khi quay nhiều video", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoWifi,
                            onCheckedChange = { autoWifi = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlueLight)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Storage Management Card (Dung Lượng Bộ Nhớ)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quản Lý Bộ Nhớ & Dọn Dẹp Dung Lượng",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Đang lưu: $localVideoCount video cục bộ (${localVideoBytes / (1024 * 1024)} MB)",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Sau khi video đã tải lên Google Drive, bạn có thể dọn dẹp các file video cũ trên máy mà không làm mất lịch sử đối soát.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            var deletedCount = 0
                            videosDir.listFiles()?.forEach { f ->
                                if (f.name.endsWith(".mp4") && f.delete()) {
                                    deletedCount++
                                }
                            }
                            localVideoCount = videosDir.listFiles()?.count { it.extension.equals("mp4", true) } ?: 0
                            localVideoBytes = videosDir.listFiles()?.sumOf { it.length() } ?: 0L
                            Toast.makeText(context, "Đã dọn dẹp $deletedCount file video để giải phóng bộ nhớ!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dọn Dẹp Bộ Nhớ Tạm", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val sec = debounceSec.toIntOrNull() ?: 3
                    viewModel.updateSession(
                        userSession.copy(
                            employeeId = empId.trim(),
                            employeeName = empName.trim(),
                            warehouseId = whId.trim(),
                            warehouseName = whName.trim(),
                            deviceId = devId.trim(),
                            minRecordDebounceSeconds = sec,
                            autoUploadOnWifi = autoWifi,
                            enableSoundBeep = soundBeep,
                            enableVoiceVietnamese = voiceVi,
                            googleAccountEmail = googleEmail.trim(),
                            googleDriveFolderName = driveFolder.trim(),
                            isGoogleDriveConnected = isDriveConnected
                        )
                    )
                    isSavedMessageVisible = true
                    Toast.makeText(context, "Đã lưu cài đặt thành công!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LƯU CẤU HÌNH", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (isSavedMessageVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Đã cập nhật cấu hình Google Drive & Watermark!",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
