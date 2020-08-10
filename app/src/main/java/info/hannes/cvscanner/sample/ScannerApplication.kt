package info.hannes.cvscanner.sample

import android.app.Application
import info.hannes.timber.DebugTree
import timber.log.Timber

class ScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }
}