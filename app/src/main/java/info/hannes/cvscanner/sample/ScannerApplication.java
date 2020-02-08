package info.hannes.cvscanner.sample;

import android.app.Application;

import info.hannes.timber.DebugTree;
import timber.log.Timber;

public class ScannerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new DebugTree());
    }
}
