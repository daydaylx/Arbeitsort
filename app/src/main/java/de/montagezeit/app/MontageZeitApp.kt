package de.montagezeit.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MontageZeitApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // DI Container wird automatisch durch Hilt initialisiert
        // TODO: Initialize database
        // TODO: Schedule reminders on first launch
    }
}
