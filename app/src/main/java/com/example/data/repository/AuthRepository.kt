package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "packing_auth_prefs")

class AuthRepository(private val context: Context) {

    private val KEY_EMP_ID = stringPreferencesKey("emp_id")
    private val KEY_EMP_NAME = stringPreferencesKey("emp_name")
    private val KEY_WH_ID = stringPreferencesKey("wh_id")
    private val KEY_WH_NAME = stringPreferencesKey("wh_name")
    private val KEY_DEV_ID = stringPreferencesKey("dev_id")
    private val KEY_SHIFT = stringPreferencesKey("shift")
    private val KEY_DEBOUNCE_SEC = intPreferencesKey("debounce_sec")
    private val KEY_AUTO_UPLOAD_WIFI = booleanPreferencesKey("auto_upload_wifi")
    private val KEY_SOUND_BEEP = booleanPreferencesKey("sound_beep")
    private val KEY_VOICE_VI = booleanPreferencesKey("voice_vi")
    private val KEY_GOOGLE_EMAIL = stringPreferencesKey("google_email")
    private val KEY_GOOGLE_CONNECTED = booleanPreferencesKey("google_connected")
    private val KEY_GOOGLE_FOLDER_NAME = stringPreferencesKey("google_folder_name")
    private val KEY_GOOGLE_FOLDER_ID = stringPreferencesKey("google_folder_id")
    private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")

    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            employeeId = prefs[KEY_EMP_ID] ?: "EMP-8821",
            employeeName = prefs[KEY_EMP_NAME] ?: "Nguyễn Văn Nam",
            warehouseId = prefs[KEY_WH_ID] ?: "KHO-HANOI-01",
            warehouseName = prefs[KEY_WH_NAME] ?: "Kho Tổng Hà Nội - Bàn Đóng Gói 01",
            deviceId = prefs[KEY_DEV_ID] ?: "DEV-PACK-STATION-01",
            shift = prefs[KEY_SHIFT] ?: "Ca Sáng (08:00 - 16:00)",
            minRecordDebounceSeconds = prefs[KEY_DEBOUNCE_SEC] ?: 3,
            autoUploadOnWifi = prefs[KEY_AUTO_UPLOAD_WIFI] ?: true,
            enableSoundBeep = prefs[KEY_SOUND_BEEP] ?: true,
            enableVoiceVietnamese = prefs[KEY_VOICE_VI] ?: true,
            googleAccountEmail = prefs[KEY_GOOGLE_EMAIL] ?: "donamdh@gmail.com",
            isGoogleDriveConnected = prefs[KEY_GOOGLE_CONNECTED] ?: true,
            googleDriveFolderName = prefs[KEY_GOOGLE_FOLDER_NAME] ?: "Video Đóng Hàng Kho Tổng",
            googleDriveFolderId = prefs[KEY_GOOGLE_FOLDER_ID] ?: "1Packing_Drive_Folder_2026"
        )
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: true // Default true for instant access
    }

    suspend fun saveUserSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMP_ID] = session.employeeId
            prefs[KEY_EMP_NAME] = session.employeeName
            prefs[KEY_WH_ID] = session.warehouseId
            prefs[KEY_WH_NAME] = session.warehouseName
            prefs[KEY_DEV_ID] = session.deviceId
            prefs[KEY_SHIFT] = session.shift
            prefs[KEY_DEBOUNCE_SEC] = session.minRecordDebounceSeconds
            prefs[KEY_AUTO_UPLOAD_WIFI] = session.autoUploadOnWifi
            prefs[KEY_SOUND_BEEP] = session.enableSoundBeep
            prefs[KEY_VOICE_VI] = session.enableVoiceVietnamese
            prefs[KEY_GOOGLE_EMAIL] = session.googleAccountEmail
            prefs[KEY_GOOGLE_CONNECTED] = session.isGoogleDriveConnected
            prefs[KEY_GOOGLE_FOLDER_NAME] = session.googleDriveFolderName
            prefs[KEY_GOOGLE_FOLDER_ID] = session.googleDriveFolderId
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun connectGoogleAccount(email: String, folderName: String = "Video Đóng Hàng Kho Tổng") {
        context.dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_EMAIL] = email
            prefs[KEY_GOOGLE_CONNECTED] = true
            prefs[KEY_GOOGLE_FOLDER_NAME] = folderName
        }
    }

    suspend fun disconnectGoogleAccount() {
        context.dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_CONNECTED] = false
        }
    }

    suspend fun updateDebounceTime(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEBOUNCE_SEC] = seconds
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }
}
