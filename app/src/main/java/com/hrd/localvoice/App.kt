package com.hrd.localvoice

import android.app.Application
import com.bugfender.sdk.Bugfender

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugfender.init(this, "fbvrkpqQ2DEpEwlhglNMBRB3oSmFwSFr", BuildConfig.DEBUG)
        Bugfender.enableCrashReporting()
        Bugfender.enableUIEventLogging(this)

        if (!BuildConfig.DEBUG)
            Bugfender.enableLogcatLogging() // optional, if you want logs automatically collected from logcat
    }
}