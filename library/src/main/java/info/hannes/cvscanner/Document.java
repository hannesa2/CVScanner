package info.hannes.cvscanner;

import android.os.Vibrator;

import com.google.android.gms.vision.Frame;

import org.opencv.core.Point;

import info.hannes.cvscanner.util.CVProcessor;

/**
 * Holds the actual image data. Quad point are also scaled with respect to actual image.
 */
public class Document {

    private Frame image;
    CVProcessor.Quadrilateral detectedQuad;
    private static final int HAPTIC_FEEDBACK_LENGTH = 30;

    public Document(Frame image, CVProcessor.Quadrilateral detectedQuad, Vibrator hapticFeedback) {
        this.image = image;
        this.detectedQuad = detectedQuad;

        if (hapticFeedback != null) {
            hapticFeedback.vibrate(HAPTIC_FEEDBACK_LENGTH);
        }
    }

    public Frame getImage() {
        return image;
    }

    public void setImage(Frame image) {
        this.image = image;
    }

    public CVProcessor.Quadrilateral getDetectedQuad() {
        return detectedQuad;
    }

    public void setDetectedQuad(CVProcessor.Quadrilateral detectedQuad) {
        this.detectedQuad = detectedQuad;
    }

    public int getMaxArea() {
        Point tl = detectedQuad.points[0];
        Point tr = detectedQuad.points[1];
        Point br = detectedQuad.points[2];
        Point bl = detectedQuad.points[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);

        return Double.valueOf(dw).intValue() * Double.valueOf(dh).intValue();
    }
}
