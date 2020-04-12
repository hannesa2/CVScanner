package info.hannes.cvscanner

import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import info.hannes.cvscanner.DocumentTracker.DocumentDetectionListener
import info.hannes.visionpipeline.GraphicOverlay

class DocumentTrackerFactory(private val graphicOverlay: GraphicOverlay<DocumentGraphic>, private val documentDetectionListener: DocumentDetectionListener) : MultiProcessor.Factory<Document> {

    override fun create(document: Document): Tracker<Document> {
        val graphic = DocumentGraphic(graphicOverlay, document)
        return DocumentTracker(graphicOverlay, graphic, documentDetectionListener)
    }

}