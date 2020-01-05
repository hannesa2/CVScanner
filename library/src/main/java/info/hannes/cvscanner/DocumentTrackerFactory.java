package info.hannes.cvscanner;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;

import online.devliving.mobilevisionpipeline.GraphicOverlay;


public class DocumentTrackerFactory implements MultiProcessor.Factory<Document> {
    GraphicOverlay<DocumentGraphic> mOverlay;
    DocumentTracker.DocumentDetectionListener mListener;

    public DocumentTrackerFactory(GraphicOverlay<DocumentGraphic> mOverlay, DocumentTracker.DocumentDetectionListener mListener) {
        this.mOverlay = mOverlay;
        this.mListener = mListener;
    }

    @Override
    public Tracker<Document> create(Document document) {
        DocumentGraphic graphic = new DocumentGraphic(mOverlay, document);
        return new DocumentTracker(mOverlay, graphic, mListener);
    }
}
