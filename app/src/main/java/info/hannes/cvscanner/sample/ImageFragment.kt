package info.hannes.cvscanner.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import java.io.File
import java.net.URI


class ImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.fragment_image, container, false)
        val imagePath = arguments!!.getString(IMAGE_PATH, "")

        val imgFile = File(imagePath)
        if (imgFile.exists()) {
            val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            rootView.findViewById<ImageView>(R.id.scanImage).setImageBitmap(myBitmap)
        }
        return rootView
    }

    companion object {
        const val IMAGE_PATH = "IMAGE_PATH"

        fun newInstance(file: String): ImageFragment {
            val imageFragment = ImageFragment()
            val args = Bundle()
            args.putString(IMAGE_PATH, file)
            imageFragment.arguments = args
            return imageFragment
        }

    }
}