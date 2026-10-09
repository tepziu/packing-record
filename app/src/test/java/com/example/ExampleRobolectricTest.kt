package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PackingDatabase
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.util.HashUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: PackingDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PackingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Packing Recorder", appName)
    }

    @Test
    fun `calculate SHA256 checksum correctly`() {
        val tempFile = File(context.cacheDir, "sample_test_video.mp4")
        tempFile.writeText("PACKING_VIDEO_TEST_RAW_DATA_123456789")

        val hash = HashUtils.calculateSHA256(tempFile)
        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 is 64 hex characters
        assertTrue(HashUtils.verifyIntegrity(tempFile, hash))

        tempFile.delete()
    }

    @Test
    fun `insert and retrieve packing session in Room DB`() = runBlocking {
        val dao = db.packingSessionDao()
        val session = PackingSession(
            orderCode = "ORD-TEST-9988",
            employeeId = "EMP-8821",
            warehouseId = "WH-HN-01",
            deviceId = "DEV-01",
            startTimeDevice = 1000L,
            startTimeServer = 1050L,
            endTimeDevice = 5000L,
            endTimeServer = 5050L,
            durationMs = 4000L,
            latitude = 21.0285,
            longitude = 105.8542,
            videoFilePath = "/data/user/0/com.example/files/test.mp4",
            fileSizeBytes = 102400L,
            videoHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            uploadStatus = UploadStatus.PENDING
        )

        val id = dao.insertSession(session)
        assertTrue(id > 0)

        val unsynced = dao.getUnsyncedSessionsList()
        assertEquals(1, unsynced.size)
        assertEquals("ORD-TEST-9988", unsynced[0].orderCode)
        assertEquals(UploadStatus.PENDING, unsynced[0].uploadStatus)

        // Test updating upload state
        dao.updateUploadState(
            id = id,
            status = UploadStatus.COMPLETED,
            progress = 100,
            driveFileId = "1Drive_Test_File_Id_123",
            driveViewUrl = "https://drive.google.com/test",
            error = null
        )

        val afterSync = dao.getUnsyncedSessionsList()
        assertEquals(0, afterSync.size)
    }
}
