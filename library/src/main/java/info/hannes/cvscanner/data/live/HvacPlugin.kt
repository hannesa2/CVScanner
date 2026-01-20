package info.hannes.cvscanner.data.live

import android.content.Context
import androidx.lifecycle.LiveData
import org.opencv.core.Point
import timber.log.Timber

class HvacPlugin(
        context: Context,
        private val dataLifecycleCallback: IPluginDataLifecycle
) :
        HvacController.HvacEventListener {

    companion object {
        val PROPERTY_IDS = arrayOf(1, 2)
    }

    private var hvacController: HvacController = HvacController(context, this)

    private var propertyCache = PropertyCache()

    fun setTemperature(zoneId: Int, value: Float) {
        propertyCache.getProperty(PROPERTY_IDS[0])?.let {
            hvacController.setProperty(it, value)
        }
    }

    fun getTemperature(zoneId: Int): LiveData<Float> =
            propertyCache.getProperty(PROPERTY_IDS[0])?.liveData as LiveData<Float>

    fun startService() {
        hvacController.connectToCarService()
    }

    fun stopService() {
        hvacController.destroy()
    }

    override fun onHvacConnected() {
        hvacController.cacheProperties(propertyCache)
    }

    override fun onPropertiesFetched() {
        // this is where all the parties interested in the initial property values
        // should be able to get those
        dataLifecycleCallback.onPluginDataValid()
    }

    override fun onPropertyUpdated(property: Array<Point>) {
        propertyCache.getProperty(PROPERTY_IDS[0])?.let {
            propertyCache.setPropertyValue(PROPERTY_IDS[0], property)
        } ?: run {
            Timber.w("missing %s", property.toString())
        }
    }
}