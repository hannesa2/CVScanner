package info.hannes.cvscanner.data.live

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import org.opencv.core.Point
import timber.log.Timber

internal class HvacController(
        private val context: Context,
        private val eventListener: HvacEventListener
) {

    companion object {
        const val BIND_TO_HVAC_RETRY_DELAY = 5000L
        const val DELAY_FROM_HARDWARE = 100L

        const val NON_ZONE_AREA_ID = 0
    }


    private var handler = Handler()

    /**
     * Registers callbacks and initializes components upon connection.
     */
    private var carServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("HVAC service connected $service")
            try {
                eventListener.onHvacConnected()
                service.linkToDeath(restart, 0)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.d("HVAC service disconnected")
            destroyHvacManager()
        }

        override fun onBindingDied(name: ComponentName?) {
            Timber.e("Lost binding to the car HVAC service")
        }

        override fun onNullBinding(name: ComponentName?) {
            Timber.e("Failed to bind to the car HVAC service")
        }
    }

    /**
     * Create connection to the Car service. Note: call backs from the Car service
     * ([CarHvacManager]) will happen on the same thread this method was called from.
     */
    fun connectToCarService() {

    }

    fun destroy() {
        destroyHvacManager()
    }

    private fun destroyHvacManager() {
        Timber.d("Unsubscribe from HVAC property updates")
    }

    /**
     * If the connection to car service goes away then restart it.
     */
    private val restart: IBinder.DeathRecipient = IBinder.DeathRecipient {
        destroyHvacManager()

        handler.postDelayed({
            Timber.d("Trying to reconnect to car")

            connectToCarService()
        }, BIND_TO_HVAC_RETRY_DELAY)
    }

    fun cacheProperties(cache: PropertyCache) {
        cache.addZonedProperty(NON_ZONE_AREA_ID)

        cache.let { propertyCache ->
            propertyCache.properties.forEach { property: CarProperty ->
                val value = readProperty(property)
                value?.let {
                    propertyCache.setPropertyValue(property.id, it)
                }
            }
        }
        eventListener.onPropertiesFetched()
    }

    fun setProperty(property: CarProperty, value: Any) {

    }

    private fun simulatePropertyUpdate(property: CarProperty, value: Any) {
        handler.postDelayed({
            Timber.w("simulatePropertyUpdate")
        }, DELAY_FROM_HARDWARE)
    }

    private fun readProperty(property: CarProperty): Array<Point>? {
        return null
    }

    interface HvacEventListener {
        fun onHvacConnected()
        fun onPropertiesFetched()
        fun onPropertyUpdated(property: Array<Point>)
    }
}
