package com.example

import android.app.Application
import com.example.data.local.PackingDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.PackingRepository
import com.example.location.LocationTracker
import com.example.sync.UploadWorker

class PackingApplication : Application() {

    lateinit var packingRepository: PackingRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var locationTracker: LocationTracker
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Database
        PackingDatabase.getDatabase(this)

        // Initialize Repositories
        packingRepository = PackingRepository(this)
        authRepository = AuthRepository(this)
        locationTracker = LocationTracker(this)

        // Auto schedule background uploads on app start
        UploadWorker.enqueueUploadWork(this)
    }

    companion object {
        lateinit var instance: PackingApplication
            private set
    }
}
