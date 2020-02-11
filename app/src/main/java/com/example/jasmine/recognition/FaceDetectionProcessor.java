package com.example.jasmine.recognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.example.jasmine.utility.BitmapUtils;
import com.example.jasmine.utility.CameraImageGraphic;
import com.example.jasmine.utility.FrameMetadata;
import com.example.jasmine.utility.GraphicOverlay;
import com.example.jasmine.MainActivity;
import com.example.jasmine.R;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;


/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceDetectionProcessor";

    private final FirebaseVisionFaceDetector detector;

    private final Bitmap overlayBitmap;
    private Bitmap imageBitmap;
    public static Bitmap croppedBitmap;


    public  static int prevId = -1;
    private float prevfaceArea = 0;

    public static final String collectionId = "employees";
    public static final String bucket = "face-dtr2";


    private SearchFacesByImageRequest searchFacesByImageRequest;
    private SearchFacesByImageResult searchFacesByImageResult;
    private BitmapUtils bitUtil = new BitmapUtils();
    private CameraFragment fragment;

    long ageDetectorStarted = 0;


    // use this to start and trigger a service
    Intent awsServiceIntent;



    public FaceDetectionProcessor(Resources resources, CameraFragment fragment) {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                        .setMinFaceSize(0.3f)
                        .enableTracking()
                        .build();

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        overlayBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background);

        awsServiceIntent = new Intent(fragment.getActivity(), AwsServiceIntent.class);
// potentially add data to the intent
     //   awsIntentService.
     //   i.putExtra("KEY1", "Value to be used by the service");
      //  fragment.getActivity().startService(awsServiceIntent);



        this.fragment =   fragment;
        ageDetectorStarted = System.currentTimeMillis();
    }



    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }


        Log.d(TAG,"Face Detected!" + faces.size());

        FirebaseVisionFace biggestFace=null;

        prevfaceArea = 0;
        for (FirebaseVisionFace face :faces) {

            //the bounding box area should be 50% of the entire screen


            if (prevfaceArea < face.getBoundingBox().height() * face.getBoundingBox().width()) {
                biggestFace = face;
                Log.d(TAG,"We got teh biggest face");
                prevfaceArea = face.getBoundingBox().height() * face.getBoundingBox().width();
            }

        }



          if (biggestFace != null){
              FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, biggestFace, overlayBitmap);
              graphicOverlay.add(faceGraphic);

              //crop only of unique

              if (prevId != biggestFace.getTrackingId()) {
                  prevId = biggestFace.getTrackingId();
                  croppedBitmap = cropBitmap(originalCameraImage, biggestFace.getBoundingBox());

                  //call aws to verify


                  awsServiceIntent.setAction("com.service.help.upload.photo");
                  awsServiceIntent.putExtra("bitmapPhoto",croppedBitmap);
             //     awsServiceIntent.putExtra("logType", activity.getTogInOUt().getText());
                  fragment.getActivity().startService(awsServiceIntent);

                  //update name later

              }

          }



        graphicOverlay.postInvalidate();
    }



    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }


    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        Bitmap ret = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        return ret;
    }








}