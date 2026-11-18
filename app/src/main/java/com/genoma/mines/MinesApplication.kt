package com.genoma.mines

import android.app.Application
import com.genoma.mines.core.remoteconfig.RemoteConfigManager

class MinesApplication : Application() {

    lateinit var remoteConfigManager: RemoteConfigManager
        private set

    override fun onCreate() {
        super.onCreate()

        remoteConfigManager = RemoteConfigManager()
        remoteConfigManager.initialize()
    }
}