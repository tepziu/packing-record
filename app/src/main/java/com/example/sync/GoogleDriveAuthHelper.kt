package com.example.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Initiates Google Sign-In bottom sheet via Jetpack CredentialManager.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInResult> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                // Use a default client id if available, or fall back to standard account picker
                .setServerClientId("651621855460-aistudio.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(
                    GoogleSignInResult(
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id,
                        idToken = googleIdTokenCredential.idToken,
                        profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                )
            } else {
                Result.failure(Exception("Định dạng xác thực Google không được hỗ trợ"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Người dùng đã hủy đăng nhập"))
        } catch (e: GetCredentialException) {
            Log.e("GoogleDriveAuth", "Credential error: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Lỗi xác thực Google Credential Manager"))
        } catch (e: Exception) {
            Log.e("GoogleDriveAuth", "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Opens Google Drive on device or browser.
     */
    fun openGoogleDrive(email: String? = null) {
        try {
            val url = if (!email.isNullOrBlank()) {
                "https://drive.google.com/drive/u/0/my-drive"
            } else {
                "https://drive.google.com/drive/my-drive"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GoogleDriveAuth", "Cannot open Drive: ${e.message}")
        }
    }
}

data class GoogleSignInResult(
    val email: String,
    val displayName: String,
    val idToken: String?,
    val profilePictureUri: String?
)
