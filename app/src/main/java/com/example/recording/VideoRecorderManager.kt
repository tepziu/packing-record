package com.example.recording

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.domain.model.UserSession
import com.example.domain.model.WatermarkMetadata
import com.example.location.GpsCoordinates
import com.example.util.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RecordingState {
    object Idle : RecordingState()
    data class RecordingInProgress(
        val orderCode: String,
        val startTimeMillis: Long,
        val durationMillis: Long = 0L,
        val canStop: Boolean = false // True once min debounce threshold passed
    ) : RecordingState()
    data class Processing(val orderCode: String) : RecordingState()
    data class Completed(val session: PackingSession) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class VideoRecorderManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var activeRecording: Recording? = null
    private var currentOutputFile: File? = null
    private var currentSessionBuilder: PackingSession? = null
    private var timerJob: Job? = null
    private var isAudioEnabled: Boolean = true

    val videoCapture: VideoCapture<Recorder> by lazy {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        VideoCapture.withOutput(recorder)
    }

    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled = enabled
    }

    /**
     * Starts video recording for a given orderCode.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        orderCode: String,
        userSession: UserSession,
        coordinates: GpsCoordinates,
        onStarted: (() -> Unit)? = null
    ) {
        if (_recordingState.value is RecordingState.RecordingInProgress ||
            _recordingState.value is RecordingState.Processing) {
            return
        }

        try {
            val videosDir = File(context.getExternalFilesDir(null), "packing_videos").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val sanitizedOrder = orderCode.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val videoFile = File(videosDir, "PACK_${sanitizedOrder}_$timestamp.mp4")
            currentOutputFile = videoFile

            val startTime = System.currentTimeMillis()
            currentSessionBuilder = PackingSession(
                orderCode = orderCode,
                employeeId = userSession.employeeId,
                warehouseId = userSession.warehouseId,
                deviceId = userSession.deviceId,
                startTimeDevice = startTime,
                startTimeServer = startTime,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                videoFilePath = videoFile.absolutePath,
                uploadStatus = UploadStatus.PENDING
            )

            val outputOptions = FileOutputOptions.Builder(videoFile).build()
            val pendingRecording = videoCapture.output.prepareRecording(context, outputOptions)

            if (isAudioEnabled) {
                try {
                    pendingRecording.withAudioEnabled()
                } catch (_: SecurityException) {
                    // Audio permission not granted, proceed video only
                }
            }

            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _recordingState.value = RecordingState.RecordingInProgress(
                            orderCode = orderCode,
                            startTimeMillis = startTime,
                            durationMillis = 0L,
                            canStop = false
                        )
                        startTimer(orderCode, startTime, userSession.minRecordDebounceSeconds)
                        onStarted?.invoke()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            processFinishedRecording(videoFile, userSession, coordinates)
                        } else {
                            timerJob?.cancel()
                            _recordingState.value = RecordingState.Error(
                                "Lỗi quay video: ${event.error} (Code: ${event.cause?.message ?: "Unknown"})"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Không thể bắt đầu quay: ${e.localizedMessage}")
        }
    }

    private fun startTimer(orderCode: String, startTime: Long, minDebounceSec: Int) {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (true) {
                delay(500)
                val duration = System.currentTimeMillis() - startTime
                val canStop = (duration >= minDebounceSec * 1000L)
                val current = _recordingState.value
                if (current is RecordingState.RecordingInProgress) {
                    _recordingState.value = current.copy(
                        durationMillis = duration,
                        canStop = canStop
                    )
                } else {
                    break
                }
            }
        }
    }

    /**
     * Stops video recording and triggers watermark embedding and SHA-256 calculation.
     */
    fun stopRecording(onComplete: ((PackingSession) -> Unit)? = null) {
        val currentState = _recordingState.value
        if (currentState !is RecordingState.RecordingInProgress) return

        timerJob?.cancel()
        _recordingState.value = RecordingState.Processing(currentState.orderCode)

        try {
            activeRecording?.stop()
            activeRecording = null
        } catch (e: Exception) {
            // Force process if activeRecording already stopped
            currentOutputFile?.let { file ->
                val dummyUser = UserSession()
                processFinishedRecording(file, dummyUser, GpsCoordinates(), onComplete)
            }
        }
    }

    private fun processFinishedRecording(
        file: File,
        userSession: UserSession,
        coordinates: GpsCoordinates,
        onComplete: ((PackingSession) -> Unit)? = null
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val initialSession = currentSessionBuilder ?: return@launch
                val endTime = System.currentTimeMillis()
                val duration = endTime - initialSession.startTimeDevice

                val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(initialSession.startTimeDevice))
                val gpsFormatted = String.format("%.5f, %.5f", initialSession.latitude, initialSession.longitude)

                val watermarkMeta = WatermarkMetadata(
                    orderCode = initialSession.orderCode,
                    timestampString = dateFormatted,
                    employeeId = initialSession.employeeId,
                    warehouseId = initialSession.warehouseId,
                    deviceId = initialSession.deviceId,
                    gpsString = gpsFormatted
                )

                // Embed forensic watermark atom into MP4 container
                VideoWatermarker.embedForensicAtom(file, watermarkMeta)

                // Calculate SHA-256 hash of completed file
                val fileHash = HashUtils.calculateSHA256(file)
                val fileSize = file.length()

                val completedSession = initialSession.copy(
                    endTimeDevice = endTime,
                    endTimeServer = endTime,
                    durationMs = duration,
                    fileSizeBytes = fileSize,
                    videoHash = fileHash,
                    uploadStatus = UploadStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )

                _recordingState.value = RecordingState.Completed(completedSession)
                onComplete?.invoke(completedSession)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error("Lỗi xử lý file video: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _recordingState.value = RecordingState.Idle
        currentOutputFile = null
        currentSessionBuilder = null
    }
}
