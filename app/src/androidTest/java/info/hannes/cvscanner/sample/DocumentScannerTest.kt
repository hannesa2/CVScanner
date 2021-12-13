package info.hannes.cvscanner.sample

import android.Manifest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.moka.utils.Screenshot
import info.hannes.cvscanner.DocumentScannerActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DocumentScannerTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(DocumentScannerActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun cameraTest() {
//        Screenshot.takeScreenshot("Start")
//        onView(withId(R.id.graphicOverlay))
//                .check(matches(isDisplayed()))
        Screenshot.takeScreenshot("End")
    }

}