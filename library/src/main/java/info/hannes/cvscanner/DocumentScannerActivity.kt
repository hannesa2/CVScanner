package info.hannes.cvscanner

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import info.hannes.cvscanner.CVScanner.ImageProcessorCallback
import info.hannes.cvscanner.DocumentScannerFragment.Companion.instantiate
import timber.log.Timber

class DocumentScannerActivity : AppCompatActivity(), ImageProcessorCallback {
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        setContentView(R.layout.scanner_activity)
    }

    override fun onResume() {
        super.onResume()
        supportFragmentManager.fragments
        if (supportFragmentManager.fragments.size == 0) {
            checkCameraPermission()
        }
    }

    fun checkCameraPermission() {
        val rc = ActivityCompat.checkSelfPermission(this@DocumentScannerActivity, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            checkPlayServices()
        } else {
            requestCameraPermission()
        }
    }

    fun checkPlayServices() { // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        } else {
            addScannerFragment()
        }
    }

    private fun addScannerFragment() {
        val extras = intent.extras
        val isScanningPassport = extras != null && intent.getBooleanExtra(EXTRA_IS_PASSPORT, false)
        val fragment: DocumentScannerFragment
        fragment = if (extras != null) {
            val borderColor = extras.getInt(EXTRA_DOCUMENT_BORDER_COLOR, -1)
            val bodyColor = extras.getInt(EXTRA_DOCUMENT_BODY_COLOR, -1)
            val torchTintColor = extras.getInt(EXTRA_TORCH_TINT_COLOR, ContextCompat.getColor(this, R.color.dark_gray))
            val torchTintLightColor = extras.getInt(EXTRA_TORCH_TINT_COLOR_LIGHT, ContextCompat.getColor(this, R.color.torch_yellow))
            instantiate(isScanningPassport, borderColor, bodyColor, torchTintColor, torchTintLightColor)
        } else {
            instantiate(isScanningPassport)
        }
        supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment)
                .commitAllowingStateLoss()
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Timber.w("Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
        val thisActivity: Activity = this
        AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("access to device camera is required for scanning")
                .setPositiveButton("OK") { dialog, which -> ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM) }.show()
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Timber.d("Got unexpected permission result: %s", requestCode)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d("Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            checkPlayServices()
            return
        }
        Timber.e("Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)")
        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Access to Camera Denied")
                .setMessage("Cannot scan document without using the camera")
                .setPositiveButton("OK", listener)
                .show()
    }

    fun setResultAndExit(path: String?) {
        val data = intent
        data.putExtra(CVScanner.RESULT_IMAGE_PATH, path)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onImageProcessed(imagePath: String?) {
        setResultAndExit(imagePath)
    }

    override fun onImageProcessingFailed(reason: String?, error: Exception?) {
        Timber.e(error)
        Toast.makeText(this, "Scanner failed: $reason", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        // constants used to pass extra data in the intent
        const val EXTRA_DOCUMENT_BORDER_COLOR = "border_color"
        const val EXTRA_DOCUMENT_BODY_COLOR = "body_color"
        const val EXTRA_TORCH_TINT_COLOR = "torch_tint_color"
        const val EXTRA_TORCH_TINT_COLOR_LIGHT = "torch_tint_color_light"
        const val EXTRA_IS_PASSPORT = "is_passport"
        // intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001
        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
    }
}