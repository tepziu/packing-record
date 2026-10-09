package com.example.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.PackingDatabase
import com.example.domain.model.UploadStatus

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = PackingDatabase.getDatabase(appContext)
    private val driveService = GoogleDriveService()

    override suspend fun doWork(): Result {
        val unuploadedList = db.packingSessionDao().getUnsyncedSessionsList()
        if (unuploadedList.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false

        for (session in unuploadedList) {
            try {
                // Set status to UPLOADING
                db.packingSessionDao().updateUploadState(
                    id = session.id,
                    status = UploadStatus.UPLOADING,
                    progress = 0,
                    driveFileId = session.driveFileId,
                    driveViewUrl = session.driveViewUrl,
                    error = null
                )

                val result = driveService.uploadVideoResumable(session) { progress ->
                    // Progress callback
                }

                if (result.success) {
                    val updated = session.copy(
                        uploadStatus = UploadStatus.COMPLETED,
                        uploadProgress = 100,
                        driveFileId = result.driveFileId,
                        driveViewUrl = result.driveViewUrl,
                        startTimeServer = result.serverStartTime,
                        endTimeServer = result.serverEndTime,
                        errorMessage = null
                    )
                    db.packingSessionDao().updateSession(updated)
                } else {
                    anyFailed = true
                    val updated = session.copy(
                        uploadStatus = UploadStatus.FAILED,
                        retryCount = session.retryCount + 1,
                        errorMessage = result.errorMessage
                    )
                    db.packingSessionDao().updateSession(updated)
                }
            } catch (e: Exception) {
                anyFailed = true
                val updated = session.copy(
                    uploadStatus = UploadStatus.FAILED,
                    retryCount = session.retryCount + 1,
                    errorMessage = e.localizedMessage
                )
                db.packingSessionDao().updateSession(updated)
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "packing_video_upload_work"

        fun enqueueUploadWork(context: Context, requireWifi: Boolean = false) {
            val networkType = if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
