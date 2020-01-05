package info.hannes.cvscanner.sample.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import info.hannes.cvscanner.CVScanner
import info.hannes.cvscanner.sample.ImageFragment
import info.hannes.cvscanner.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader


abstract class NavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var uri: Uri? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val headerLayout = navigationView.getHeaderView(0)
        (headerLayout.findViewById<View>(R.id.textVersion) as TextView).text = OpenCVLoader.OPENCV_VERSION
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.nav_simple_scan) {
            CVScanner.startScanner(this, false, REQUEST_CODE_SIMPLE_SCAN)
        } else if (id == R.id.nav_passport) {
            CVScanner.startScanner(this, true, REQUEST_CODE_SIMPLE_SCAN)
        } else if (id == R.id.nav_camera) {
            uri = CVScanner.startCameraIntent(this, REQUEST_CODE_CAMERA)
        } else if (id == R.id.nav_image_manipulation) {
            showSnackbar("not implemented")
        } else if (id == R.id.nav_color_blob_detection) {
            showSnackbar("not implemented")
        } else if (id == R.id.nav_camera_calibration) {
            showSnackbar("not implemented")
        } else if (id == R.id.nav_puzzle15) {
            showSnackbar("not implemented")
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    protected fun openActivity(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
        if (clazz.isInstance(NavigationActivity::class.java)) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SIMPLE_SCAN) {
                val scannedDocumentPath = data?.getStringExtra(CVScanner.RESULT_IMAGE_PATH);
                scannedDocumentPath?.let {
                    showSnackbar(it)
                    val imageFragment = ImageFragment.newInstance(it)

                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.contentInfo, imageFragment)
                            .commit()
                }
                Log.d("Scan", scannedDocumentPath)
        } else if (requestCode == REQUEST_CODE_CAMERA) {
                uri?.let {
                    val x = uri.toString() //Uri.parse(it)
                    val imageFragment = ImageFragment.newInstance(x)

                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.contentInfo, imageFragment)
                            .commit()
                }
        }
    }
    }

    fun showSnackbar(text: String) {
        val viewPos = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(viewPos, text, Snackbar.LENGTH_LONG)
        val view = snackbar.view
        val params = view.layoutParams
        if (params is CoordinatorLayout.LayoutParams) {
            val paramsC = view.layoutParams as CoordinatorLayout.LayoutParams
            paramsC.gravity = Gravity.BOTTOM
            view.layoutParams = paramsC
            snackbar.show()
        } else if (params is FrameLayout.LayoutParams) {
            val paramsC = view.layoutParams as FrameLayout.LayoutParams
            paramsC.gravity = Gravity.BOTTOM
            view.layoutParams = paramsC
            snackbar.show()
        } else {
            Toast.makeText(this, text + " " + params.javaClass.simpleName, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val REQUEST_CODE_SIMPLE_SCAN = 1234
        const val REQUEST_CODE_CAMERA = 1235
    }
}