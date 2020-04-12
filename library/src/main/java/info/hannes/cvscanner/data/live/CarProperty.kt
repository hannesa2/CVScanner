package info.hannes.cvscanner.data.live

import androidx.lifecycle.MutableLiveData
import org.opencv.core.Point

data class CarProperty(
        val id: Int,
        val liveData: MutableLiveData<Array<Point>>
)
