package info.hannes.cvscanner.sample

import android.os.Bundle
import info.hannes.cvscanner.sample.base.NavigationActivity

class MainActivity : NavigationActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.contentInfo, SystemInfoFragment())
                .commit()
    }
}