package info.hannes.cvscanner.util

interface SaveCallback {
    fun onSaveTaskStarted()
    fun onSaved(savedPath: String?)
    fun onSaveFailed(error: Exception?)
}