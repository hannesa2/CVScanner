package info.hannes.cvscanner

import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Tracker
import info.hannes.visionpipeline.GraphicOverlay

class DocumentTracker(
        private val graphicOverlay: GraphicOverlay<DocumentGraphic>,
        private val documentGraphic: DocumentGraphic,
        private val documentDetectionListener: DocumentDetectionListener) : Tracker<Document>() {

    override fun onNewItem(i: Int, document: Document) {
        documentGraphic.id = i
        documentDetectionListener.onDocumentDetected(document)
    }

    override fun onUpdate(detections: Detections<Document>, document: Document?) {
        graphicOverlay.add(documentGraphic)
        documentGraphic.update(document)
    }

    override fun onMissing(detections: Detections<Document>) {
        graphicOverlay.remove(documentGraphic)
    }

    override fun onDone() {
        graphicOverlay.remove(documentGraphic)
    }

    interface DocumentDetectionListener {
        fun onDocumentDetected(document: Document)
    }

}