package com.example.detector

import android.app.Application
import com.example.detector.data.ServiceLocator

class DetectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
