/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber
import java.util.*

abstract class MonitoredActivity : AppCompatActivity() {
    private val mListeners = ArrayList<LifeCycleListener>()

    @Synchronized
    fun addLifeCycleListener(listener: LifeCycleListener) {
        if (mListeners.contains(listener)) return
        mListeners.add(listener)
    }

    @Synchronized
    fun removeLifeCycleListener(listener: LifeCycleListener?) {
        mListeners.remove(listener)
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        val lifeCycleListeners = copyListeners()
        for (listener in lifeCycleListeners) {
            listener.onActivityPaused(this)
        }
    }

    private fun copyListeners(): ArrayList<LifeCycleListener> {
        val lifeCycleListeners = ArrayList<LifeCycleListener>(mListeners.size)
        lifeCycleListeners.addAll(mListeners)
        return lifeCycleListeners
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        for (listener in mListeners) {
            listener.onActivityResumed(this)
        }
    }

    @Synchronized
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lifeCycleListeners = copyListeners()
        for (listener in lifeCycleListeners) {
            listener.onActivityCreated(this)
        }
    }

    @Synchronized
    override fun onDestroy() {
        super.onDestroy()
        val lifeCycleListeners = copyListeners()
        for (listener in lifeCycleListeners) {
            listener.onActivityDestroyed(this)
        }
    }

    @Synchronized
    override fun onStart() {
        super.onStart()
        val lifeCycleListeners = copyListeners()
        for (listener in lifeCycleListeners) {
            listener.onActivityStarted(this)
        }
    }

    @Synchronized
    override fun onStop() {
        super.onStop()
        val lifeCycleListeners = copyListeners()
        for (listener in lifeCycleListeners) {
            listener.onActivityStopped(this)
        }
        Timber.i("onStop")
    }

    interface LifeCycleListener {
        fun onActivityCreated(activity: MonitoredActivity?)
        fun onActivityDestroyed(activity: MonitoredActivity?)
        fun onActivityPaused(activity: MonitoredActivity?)
        fun onActivityResumed(activity: MonitoredActivity?)
        fun onActivityStarted(activity: MonitoredActivity?)
        fun onActivityStopped(activity: MonitoredActivity?)
    }

}