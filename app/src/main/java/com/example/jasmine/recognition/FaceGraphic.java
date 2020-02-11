package com.example.jasmine.recognition;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.jasmine.utility.GraphicOverlay;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 4.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private int facing;

    private final Paint facePositionPaint;
    private final Paint idPaint;
    private final Paint boxPaint;


    private volatile FirebaseVisionFace firebaseVisionFace;

    private final Bitmap overlayBitmap;

    public FaceGraphic(GraphicOverlay overlay, FirebaseVisionFace face, Bitmap overlayBitmap) {
        super(overlay);

        firebaseVisionFace = face;
        this.facing = facing;
        this.overlayBitmap = overlayBitmap;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        idPaint = new Paint();
        idPaint.setColor(selectedColor);
        idPaint.setTextSize(ID_TEXT_SIZE);

        boxPaint = new Paint();
        boxPaint.setColor(selectedColor);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        boxPaint.setTextSize(30);


    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        FirebaseVisionFace face = firebaseVisionFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        // An offset is used on the Y axis in order to draw the circle, face id and happiness level in the top area
        // of the face's bounding box
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());


        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
        float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, boxPaint);
       canvas.drawText(
                face.getTrackingId() + " : unknown :" ,
                left,
                bottom + 50,
                boxPaint
        );



    }

  /*  private void drawLandmarkPosition(Canvas canvas, FirebaseVisionFace face, int landmarkID) {
        FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkID);
        if (landmark != null) {
            FirebaseVisionPoint point = landmark.getPosition();
            canvas.drawCircle(
                    translateX(point.getX()),
                    translateY(point.getY()),
                    10f, idPaint);
        }
    }
*/
    private void drawBitmap(Canvas canvas, FirebaseVisionFace face, int landmarkID) {
        FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkID);
        if (landmark == null) {
            return;
        }

        FirebaseVisionPoint point = landmark.getPosition();

        if (overlayBitmap != null) {
            float imageEdgeSizeBasedOnFaceSize = (face.getBoundingBox().width() / 4.0f);

            int left = (int) (translateX(point.getX()) - imageEdgeSizeBasedOnFaceSize);
            int top = (int) (translateY(point.getY()) - imageEdgeSizeBasedOnFaceSize);
            int right = (int) (translateX(point.getX()) + imageEdgeSizeBasedOnFaceSize);
            int bottom = (int) (translateY(point.getY()) + imageEdgeSizeBasedOnFaceSize);

            canvas.drawBitmap(overlayBitmap,
                    null,
                    new Rect(left, top, right, bottom),
                    null);
        }

    }
}