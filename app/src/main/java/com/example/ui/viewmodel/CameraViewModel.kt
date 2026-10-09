package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PackingApplication
import com.example.domain.model.PackingSession
import com.example.domain.model.UserSession
import com.example.domain.model.WatermarkMetadata
import com.example.location.GpsCoordinates
import com.example.recording.RecordingState
import com.example.recording.VideoRecorderManager
import com.example.util.VoiceFeedbackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MismatchWarning(
    val currentOrder: String,
    val scannedDifferentOrder: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CameraUiState(
    val isTorchOn: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val zoomRatio: Float = 1.0f,
    val lastScannedCode: String? = null,
    val mismatchWarning: MismatchWarning? = null,
    val latestCompletedSession: PackingSession? = null,
    val showSuccessDialog: Boolean = false,
    val liveClockString: String = ""
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PackingApplication
    private val packingRepo = app.packingRepository
    private val authRepo = app.authRepository
    val locationTracker = app.locationTracker

    val userSession: StateFlow<UserSession> = authRepo.userSessionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSession()
    )

    val locationCoordinates: StateFlow<GpsCoordinates> = locationTracker.locationFlow

    val videoRecorderManager = VideoRecorderManager(app, viewModelScope)
    val recordingState: StateFlow<RecordingState> = videoRecorderManager.recordingState

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null
    private val voiceFeedback = VoiceFeedbackManager(app)

    init {
        startLiveClock()
        locationTracker.startLocationUpdates()

        // Listen for completed recording to save to Room
        viewModelScope.launch {
            recordingState.collect { state ->
                if (state is RecordingState.Completed) {
                    val savedId = packingRepo.saveCompletedSession(state.session)
                    _uiState.value = _uiState.value.copy(
                        latestCompletedSession = state.session.copy(id = savedId),
                        showSuccessDialog = true
                    )
                }
            }
        }
    }

    private fun startLiveClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            while (true) {
                _uiState.value = _uiState.value.copy(
                    liveClockString = sdf.format(Date())
                )
                delay(1000)
            }
        }
    }

    /**
     * Realtime barcode detection handler with business logic:
     * 1. If IDLE and order code scanned -> Speak "Bắt đầu quay" and auto start recording!
     * 2. If RECORDING and SAME order code scanned after debounce -> Speak "Kết thúc" and auto stop!
     * 3. If RECORDING and DIFFERENT order code scanned -> DO NOT stop, speak "Sai mã đơn hàng" & show warning!
     */
    fun onBarcodeScanned(scannedCode: String) {
        val cleanCode = scannedCode.trim()
        if (cleanCode.isBlank()) return

        _uiState.value = _uiState.value.copy(lastScannedCode = cleanCode)
        val currentState = recordingState.value

        when (currentState) {
            is RecordingState.Idle -> {
                // First scan: Auto start recording & speak Vietnamese "Bắt đầu quay"
                if (userSession.value.enableVoiceVietnamese) {
                    voiceFeedback.speakStartRecording()
                }
                videoRecorderManager.startRecording(
                    orderCode = cleanCode,
                    userSession = userSession.value,
                    coordinates = locationCoordinates.value
                )
            }

            is RecordingState.RecordingInProgress -> {
                if (cleanCode.equals(currentState.orderCode, ignoreCase = true)) {
                    // Same order code scanned again
                    if (currentState.canStop) {
                        // Debounce passed! Second scan: Auto stop recording & speak Vietnamese "Kết thúc"
                        if (userSession.value.enableVoiceVietnamese) {
                            voiceFeedback.speakStopRecording()
                        }
                        videoRecorderManager.stopRecording()
                    } else {
                        // Within minimum debounce window (e.g. within 3s), ignore duplicate scan
                    }
                } else {
                    // DIFFERENT order code detected while recording active!
                    // Speak "Sai mã đơn hàng", show warning banner, do NOT stop recording!
                    if (userSession.value.enableVoiceVietnamese) {
                        voiceFeedback.speakWrongOrderCode()
                    }
                    _uiState.value = _uiState.value.copy(
                        mismatchWarning = MismatchWarning(
                            currentOrder = currentState.orderCode,
                            scannedDifferentOrder = cleanCode
                        )
                    )
                }
            }

            else -> {
                // Processing or Error, ignore barcode
            }
        }
    }

    fun manualStopRecording() {
        if (userSession.value.enableVoiceVietnamese) {
            voiceFeedback.speakStopRecording()
        }
        videoRecorderManager.stopRecording()
    }

    fun startManualRecording(orderCode: String) {
        val clean = orderCode.trim()
        if (clean.isBlank()) return
        onBarcodeScanned(clean)
    }

    fun setZoom(ratio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = ratio)
    }

    fun dismissMismatchWarning() {
        _uiState.value = _uiState.value.copy(mismatchWarning = null)
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
        videoRecorderManager.resetState()
    }

    fun toggleTorch(enable: Boolean) {
        _uiState.value = _uiState.value.copy(isTorchOn = enable)
    }

    fun toggleAudio() {
        val newAudio = !_uiState.value.isAudioEnabled
        _uiState.value = _uiState.value.copy(isAudioEnabled = newAudio)
        videoRecorderManager.setAudioEnabled(newAudio)
    }

    fun getLiveWatermarkMetadata(): WatermarkMetadata {
        val currentOrder = when (val state = recordingState.value) {
            is RecordingState.RecordingInProgress -> state.orderCode
            is RecordingState.Processing -> state.orderCode
            else -> _uiState.value.lastScannedCode ?: "CHỜ QUÉT MÃ ĐƠN HÀNG"
        }
        return WatermarkMetadata(
            orderCode = currentOrder,
            timestampString = _uiState.value.liveClockString,
            employeeId = userSession.value.employeeId,
            warehouseId = userSession.value.warehouseId,
            deviceId = userSession.value.deviceId,
            gpsString = locationCoordinates.value.toFormattedString(),
            locationName = userSession.value.warehouseName
        )
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        locationTracker.stopLocationUpdates()
        voiceFeedback.release()
    }
}
