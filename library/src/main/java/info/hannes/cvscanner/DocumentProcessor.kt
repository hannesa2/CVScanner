package info.hannes.cvscanner

import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.FocusingProcessor
import com.google.android.gms.vision.Tracker

class DocumentProcessor(detector: Detector<Document>?, tracker: Tracker<Document>?) : FocusingProcessor<Document>(detector, tracker) {

    override fun selectFocus(detections: Detections<Document>): Int {
        var detectedItems: SparseArray<Document>
        return if (detections.detectedItems.also { detectedItems = it }.size() == 0) {
            throw IllegalArgumentException("No documents for selectFocus.")
        } else {
            var itemKey = detectedItems.keyAt(0)
            var itemArea = detectedItems.valueAt(0).maxArea
            for (index in 1 until detectedItems.size()) {
                val itemKey2 = detectedItems.keyAt(index)
                var itemArea2: Int
                if (detectedItems.valueAt(index).maxArea.also { itemArea2 = it } > itemArea) {
                    itemKey = itemKey2
                    itemArea = itemArea2
                }
            }
            itemKey
        }
    }
}