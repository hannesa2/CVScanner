package info.hannes.cvscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES10
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import org.opencv.android.Utils
import org.opencv.core.Mat
import timber.log.Timber
import java.io.*

object Util {
    private const val SIZE_DEFAULT = 2048
    private const val SIZE_LIMIT = 4096
    private fun closeSilently(closeable: Closeable?) {
        if (closeable == null) return
        try {
            closeable.close()
        } catch (ignore: Throwable) {
        }
    }

    fun getUriFromPath(path: String): Uri {
        return Uri.fromFile(File(path))
    }

    /**
     * @param imageName without extension
     */
    @Throws(IOException::class)
    fun saveImage(context: Context, imageName: String, img: Mat, useExternalStorage: Boolean): String {
        val imagePath: String
        val dir: File? = if (useExternalStorage) {
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            File(context.cacheDir, "/CVScanner/")
        }
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        val imageFile = File.createTempFile(imageName, ".jpg", dir)
        val bitmap = Bitmap.createBitmap(img.size().width.toInt(), img.size().height.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(img, bitmap)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            imagePath = imageFile.absolutePath
        } finally {
            closeSilently(fileOutputStream)
        }
        return imagePath
    }

    @JvmStatic
    @Throws(IOException::class)
    fun calculateBitmapSampleSize(context: Context, bitmapUri: Uri): Int {
        val options = decodeImageForSize(context, bitmapUri)
        val maxSize = maxImageSize
        var sampleSize = 1
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize shl 1
        }
        return sampleSize
    }

    @Throws(FileNotFoundException::class)
    private fun decodeImageForSize(context: Context, imageUri: Uri): BitmapFactory.Options {
        var `is`: InputStream? = null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            `is` = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(`is`, null, options) // Just get image size
        } finally {
            closeSilently(`is`)
        }
        return options
    }

    @Throws(FileNotFoundException::class)
    fun loadBitmapFromUri(context: Context, sampleSize: Int, uri: Uri?): Bitmap? {
        var `is`: InputStream? = null
        val out: Bitmap?
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        try {
            `is` = context.contentResolver.openInputStream(uri!!)
            out = BitmapFactory.decodeStream(`is`, null, options)
        } finally {
            closeSilently(`is`)
        }
        return out
    }

    @Throws(FileNotFoundException::class)
    fun calculateInSampleSize(
        context: Context, imageUri: Uri, reqWidth: Int,
        reqHeightIn: Int, keepAspectRatio: Boolean
    ): Int {
        var reqHeight = reqHeightIn
        val options = decodeImageForSize(context, imageUri)
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        val aspectRatio = height.toFloat() / width
        var inSampleSize = 1
        if (reqWidth > 0 && (keepAspectRatio || reqHeight > 0)) {
            if (keepAspectRatio) {
                reqHeight = Math.round(reqWidth * aspectRatio)
            }
            // Calculate ratios of height and width to requested height and width
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    /**
     * Tries to preserve aspect ratio
     */
    fun loadBitmapFromUri(context: Context, imageUri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        var imageStream: InputStream? = null
        var image: Bitmap? = null
        try { // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(context, imageUri, reqWidth, reqHeight, true)
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            imageStream = context.contentResolver.openInputStream(imageUri)
            image = BitmapFactory.decodeStream(imageStream, null, options)
        } catch (e: FileNotFoundException) {
            Timber.e(e)
        } finally {
            closeSilently(imageStream)
        }
        return image
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getExifRotation(context: Context, imageUri: Uri?): Int {
        if (imageUri == null) return 0
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(imageUri)
            val exifInterface = ExifInterface(inputStream!!)
            when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> ExifInterface.ORIENTATION_UNDEFINED
            }
        } finally {
            closeSilently(inputStream)
        }
    }

    @Throws(IOException::class)
    fun setExifRotation(context: Context, imageUri: Uri?, rotation: Int): Boolean {
        if (imageUri == null) return false
        var destStream: InputStream? = null
        try {
            destStream = context.contentResolver.openInputStream(imageUri)
            val exif: ExifInterface
            if (destStream != null) {
                exif = ExifInterface(destStream)
                exif.setAttribute("UserComment", "Generated using CVScanner")
                var orientation = ExifInterface.ORIENTATION_NORMAL
                when (rotation) {
                    1 -> orientation = ExifInterface.ORIENTATION_ROTATE_90
                    2 -> orientation = ExifInterface.ORIENTATION_ROTATE_180
                    3 -> orientation = ExifInterface.ORIENTATION_ROTATE_270
                }
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                exif.saveAttributes()
            }
        } finally {
            closeSilently(destStream)
        }
        return true
    }

    private val maxImageSize: Int
        get() {
            val textureLimit = maxTextureSize
            return if (textureLimit == 0) {
                SIZE_DEFAULT
            } else {
                Math.min(textureLimit, SIZE_LIMIT)
            }
        }

    // The OpenGL texture size is the maximum size that can be drawn in an ImageView
    private val maxTextureSize: Int
        get() { // The OpenGL texture size is the maximum size that can be drawn in an ImageView
            val maxSize = IntArray(1)
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0)
            return maxSize[0]
        }
}