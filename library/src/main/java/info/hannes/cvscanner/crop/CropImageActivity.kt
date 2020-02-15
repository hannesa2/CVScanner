/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.hannes.cvscanner.crop

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import info.hannes.cvscanner.CVScanner
import info.hannes.cvscanner.CVScanner.ImageProcessorCallback
import info.hannes.cvscanner.R
import timber.log.Timber

/**
 * The activity can crop specific region of interest from an image.
 */
class CropImageActivity : AppCompatActivity(), ImageProcessorCallback {
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        supportActionBar?.hide()
        setContentView(R.layout.scanner_activity)
    }

    override fun onResume() {
        super.onResume()
        if (supportFragmentManager.fragments.size == 0) {
            addCropperFragment()
        }
    }

    private fun addCropperFragment() {
        var imageUri: Uri? = null
        val extras = intent.extras
        if (extras != null) {
            imageUri = Uri.parse(extras.getString(EXTRA_IMAGE_URI))
        }
        if (imageUri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            val rtlImageResId = extras!!.getInt(EXTRA_ROTATE_LEFT_IMAGE_RES, R.drawable.ic_rotate_left)
            val rtrImageResId = extras.getInt(EXTRA_ROTATE_RIGHT_IMAGE_RES, R.drawable.ic_rotate_right)
            val saveImageResId = extras.getInt(EXTRA_SAVE_IMAGE_RES, R.drawable.ic_check_circle)
            val rtColorResId = extras.getInt(EXTRA_ROTATE_BTN_COLOR_RES, R.color.colorPrimary)
            val saveColorResId = extras.getInt(EXTRA_SAVE_BTN_COLOR_RES, R.color.colorAccent)
            val fragment: Fragment = ImageCropperFragment.instantiate(imageUri, saveColorResId, rtColorResId, rtlImageResId,
                    rtrImageResId, saveImageResId)
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragment)
                    .commitAllowingStateLoss()
        }
    }

    override fun onImageProcessingFailed(reason: String?, error: Exception?) {
        Timber.e("CROP-ACTIVITY image processing failed: %s", reason)
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onImageProcessed(imagePath: String?) {
        Timber.e("CROP-ACTIVITY image processed")
        setResultAndExit(this, imagePath)
    }

    companion object {
        const val EXTRA_IMAGE_URI = "input_image_uri"
        const val EXTRA_ROTATE_LEFT_IMAGE_RES = "rotateLeft_imageRes"
        const val EXTRA_SAVE_IMAGE_RES = "save_imageRes"
        const val EXTRA_ROTATE_RIGHT_IMAGE_RES = "rotateRight_imageRes"
        const val EXTRA_SAVE_BTN_COLOR_RES = "save_imageColorRes"
        const val EXTRA_ROTATE_BTN_COLOR_RES = "rotate_imageColorRes"
        private fun setResultAndExit(cropImageActivity: CropImageActivity, imagePath: String?) {
            val data = cropImageActivity.intent
            data.putExtra(CVScanner.RESULT_IMAGE_PATH, imagePath)
            cropImageActivity.setResult(Activity.RESULT_OK, data)
            cropImageActivity.finish()
        }
    }
}