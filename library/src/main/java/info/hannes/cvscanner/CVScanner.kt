package info.hannes.cvscanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import info.hannes.cvscanner.crop.CropImageActivity
import info.hannes.cvscanner.util.Util3
import java.io.IOException

object CVScanner {

    var RESULT_IMAGE_PATH = "result_image_path"

    @JvmStatic
    fun getFileProviderName(context: Context) = context.packageName + ".cvscanner.fileprovider"

    @Throws(IOException::class)
    fun startCameraIntent(context: Activity, reqCode: Int): Uri? {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.packageManager) != null) { // Create the File where the photo should go
            val photoUri = Util3.createTempFile(
                context,
                "SCAN_" + System.currentTimeMillis(), ".jpg",
                true
            )
            // Continue only if the File was successfully created
            if (photoUri != null) {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                context.startActivityForResult(takePictureIntent, reqCode)
                return photoUri
            }
        }
        return null
    }

    fun startScanner(activity: Activity, isPassport: Boolean, reqCode: Int) {
        val intent = Intent(activity, DocumentScannerActivity::class.java)
        intent.putExtra(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport)
        activity.startActivityForResult(intent, reqCode)
    }

    fun startScanner(
        activity: Activity,
        isPassport: Boolean, reqCode: Int,
        @ColorRes docBorderColorRes: Int,
        @ColorRes docBodyColorRes: Int,
        @ColorRes torchColor: Int,
        @ColorRes torchColorLight: Int
    ) {
        val intent = Intent(activity, DocumentScannerActivity::class.java)
        intent.putExtra(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport)
        intent.putExtra(DocumentScannerActivity.EXTRA_DOCUMENT_BODY_COLOR, docBodyColorRes)
        intent.putExtra(DocumentScannerActivity.EXTRA_DOCUMENT_BORDER_COLOR, docBorderColorRes)
        intent.putExtra(DocumentScannerActivity.EXTRA_TORCH_TINT_COLOR, torchColor)
        intent.putExtra(DocumentScannerActivity.EXTRA_TORCH_TINT_COLOR_LIGHT, torchColorLight)
        activity.startActivityForResult(intent, reqCode)
    }

    fun startManualCropper(activity: Activity, inputImageUri: Uri, reqCode: Int) {
        val intent = Intent(activity, CropImageActivity::class.java)
        intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, inputImageUri.toString())
        activity.startActivityForResult(intent, reqCode)
    }

    fun startManualCropper(
        activity: Activity, imageUri: Uri, reqCode: Int, @ColorRes buttonTint: Int,
        @ColorRes buttonTintSecondary: Int, @DrawableRes rotateLeftIconRes: Int,
        @DrawableRes rotateRightIconRes: Int, @DrawableRes saveButtonIconRes: Int
    ) {
        val intent = Intent(activity, CropImageActivity::class.java)
        intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, imageUri.toString())
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_BTN_COLOR_RES, buttonTintSecondary)
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_LEFT_IMAGE_RES, rotateLeftIconRes)
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_RIGHT_IMAGE_RES, rotateRightIconRes)
        intent.putExtra(CropImageActivity.EXTRA_SAVE_BTN_COLOR_RES, buttonTint)
        intent.putExtra(CropImageActivity.EXTRA_SAVE_IMAGE_RES, saveButtonIconRes)
        activity.startActivityForResult(intent, reqCode)
    }

    interface ImageProcessorCallback {
        fun onImageProcessingFailed(reason: String?, error: Exception?)
        fun onImageProcessed(imagePath: String?)
    }
}