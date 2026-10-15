package com.example

import android.app.Application
import com.example.actions.DeviceActionRegistry
import com.example.data.api.GeminiApiClient
import com.example.data.local.NovaDatabase
import com.example.voice.VoiceSubsystem

class NovaApplication : Application() {

    val database by lazy { NovaDatabase.getInstance(this) }
    val actionRegistry by lazy { DeviceActionRegistry(this) }
    val apiClient by lazy { GeminiApiClient(this) }
    val voiceSubsystem by lazy { VoiceSubsystem(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceSubsystem.shutdown()
    }

    companion object {
        lateinit var instance: NovaApplication
            private set
    }
}
