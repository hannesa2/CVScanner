package info.hannes.cvscanner.sample

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import org.opencv.android.OpenCVLoader

class SystemInfoFragment : PreferenceFragmentCompat() {

    @SuppressLint("HardwareIds")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs)
        //		Build.DEVICE
        //		Build.ID
        //		Build.MANUFACTURER
        //		Build.MODEL
        //		Build.PRODUCT
        //		Build.TAGS
        //		Build.TYPE
        //		Build.USER
        findPreference<Preference?>(PREFERENCE_ + "BOARD")?.summary = Build.BOARD
        findPreference<Preference?>(PREFERENCE_ + "BRAND")?.summary = Build.BRAND
        findPreference<Preference?>(PREFERENCE_ + "CPU_ABI")?.summary = Build.SUPPORTED_ABIS[0]
        findPreference<Preference?>(PREFERENCE_ + "DISPLAY")?.summary = Build.DISPLAY
        findPreference<Preference?>(PREFERENCE_ + "USER")?.summary = Build.USER
        findPreference<Preference?>(PREFERENCE_ + "OpenCV_Version")?.summary = OpenCVLoader.OPENCV_VERSION
    }

    companion object {
        private const val PREFERENCE_ = "preference_"
    }

}