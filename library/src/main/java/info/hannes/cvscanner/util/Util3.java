package info.hannes.cvscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import info.hannes.cvscanner.CVScanner;
import timber.log.Timber;


public final class Util3 {

    public static Uri createTempFile(Context context, String fileName, String fileExtension, boolean useExternalStorage) throws IOException {
        File storageDir;

        if (useExternalStorage) {
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else {
            storageDir = new File(context.getCacheDir(), "/CVScanner/");

            if (!storageDir.exists())
                storageDir.mkdirs();
        }

        File image = File.createTempFile(fileName, fileExtension, storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        Uri currentPhotoUri = getUriForFile(context, image);
        Timber.d("photo-uri: %s", currentPhotoUri);

        return currentPhotoUri;
    }

    /**
     * Shareable FileProvider uri. The image must be in either 'context.getCacheDir() + "/CVScanner/"'
     * or 'context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)'
     */
    private static Uri getUriForFile(Context context, File file) {
        return CVFileProvider.getUriForFile(context, CVScanner.getFileProviderName(context), file);
    }

}
