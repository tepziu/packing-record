package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}

@Entity(tableName = "packing_sessions")
data class PackingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderCode: String,
    val employeeId: String,
    val warehouseId: String,
    val deviceId: String,
    val startTimeDevice: Long,
    val startTimeServer: Long = startTimeDevice,
    val endTimeDevice: Long = 0,
    val endTimeServer: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val videoFilePath: String = "",
    val videoHash: String = "", // SHA-256
    val fileSizeBytes: Long = 0,
    val durationMs: Long = 0,
    val driveFileId: String? = null,
    val driveViewUrl: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val uploadProgress: Int = 0,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserSession(
    val employeeId: String = "EMP-8821",
    val employeeName: String = "Nguyễn Văn Nam",
    val warehouseId: String = "KHO-HANOI-01",
    val warehouseName: String = "Kho Tổng Hà Nội - Bàn Đóng Gói 01",
    val deviceId: String = "DEV-PACK-STATION-01",
    val shift: String = "Ca Sáng (08:00 - 16:00)",
    val minRecordDebounceSeconds: Int = 3,
    val autoUploadOnWifi: Boolean = true,
    val enableSoundBeep: Boolean = true,
    val enableVoiceVietnamese: Boolean = true,
    val googleAccountEmail: String = "donamdh@gmail.com",
    val isGoogleDriveConnected: Boolean = true,
    val googleDriveFolderId: String = "1Video_Dong_Hang_Kho_2026",
    val googleDriveFolderName: String = "Video Đóng Hàng Kho Tổng"
)

data class WatermarkMetadata(
    val orderCode: String,
    val timestampString: String,
    val employeeId: String,
    val warehouseId: String,
    val deviceId: String,
    val gpsString: String,
    val locationName: String = "Kho Tổng Hà Nội"
)
