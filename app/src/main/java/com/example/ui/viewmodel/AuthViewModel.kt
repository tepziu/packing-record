package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PackingApplication
import com.example.domain.model.UserSession
import com.example.sync.GoogleDriveAuthHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PackingApplication
    private val repo = app.authRepository
    val googleAuthHelper = GoogleDriveAuthHelper(application)

    val userSession: StateFlow<UserSession> = repo.userSessionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSession()
    )

    fun updateSession(session: UserSession) {
        viewModelScope.launch {
            repo.saveUserSession(session)
        }
    }

    fun handleGoogleSignIn(activity: Activity, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = googleAuthHelper.signInWithGoogle(activity)
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    repo.connectGoogleAccount(
                        email = data.email,
                        folderName = userSession.value.googleDriveFolderName
                    )
                    onComplete(true, "Đăng nhập thành công tài khoản Google: ${data.email}")
                } else {
                    onComplete(false, "Không lấy được thông tin tài khoản Google")
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Lỗi đăng nhập Google"
                onComplete(false, err)
            }
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            repo.disconnectGoogleAccount()
        }
    }

    fun openDrive() {
        googleAuthHelper.openGoogleDrive(userSession.value.googleAccountEmail)
    }
}
