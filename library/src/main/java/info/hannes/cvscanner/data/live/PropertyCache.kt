package info.hannes.cvscanner.data.live

import androidx.lifecycle.MutableLiveData
import org.opencv.core.Point

internal class PropertyCache {

    val properties: MutableList<CarProperty> = mutableListOf()

    fun setPropertyValue(propertyId: Int, value: Array<Point>) =
            properties.first { it.id == propertyId }.liveData.postValue(value)

    fun getProperty(propertyId: Int) = properties.firstOrNull {
        it.id == propertyId
    }

    fun getGlobalProperty(propertyId: Int) = properties.firstOrNull {
        it.id == propertyId
    }

    fun getPropertyCount(propertyId: Int) = properties.count {
        it.id == propertyId
    }

    fun addZonedProperty(zoneId: Int) {
        if (getProperty(1) == null) {
            properties.add(
                    CarProperty(
                            1,
                            MutableLiveData()
                    )
            )
        }
    }
}