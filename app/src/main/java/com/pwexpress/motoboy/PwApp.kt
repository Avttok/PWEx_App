package com.pwexpress.motoboy

import android.app.Application
import com.pwexpress.core.PwCore
import com.pwexpress.feature.tracking.Tracking

class PwApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PwCore.init(this)
        Tracking.init(this, BuildConfig.VERSION_NAME)
    }
}
