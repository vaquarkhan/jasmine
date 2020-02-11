package com.example.jasmine.recognition;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.example.jasmine.MainActivity;
import com.example.jasmine.utility.BitmapUtils;

import java.nio.ByteBuffer;
import java.util.List;

public class AwsServiceIntent extends IntentService {

    public static final String collectionId = "staff";
    public static final String bucket = "allanlao-faces";

    private final AmazonRekognition rekognitionClient;
    private SearchFacesByImageRequest searchFacesByImageRequest;
    private SearchFacesByImageResult searchFacesByImageResult;
    private BitmapUtils bitUtil = new BitmapUtils();



    // Action Name you define for the Intent received in the service for intent //filter
    private static final String ACTION_UPLOAD_PHOTO = "com.service.help.upload.photo";
    private static final String ACTION_SEND_MESSAGE = "com.service.help.send.message";

    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";


    //This is String class object but you need to send the Bitmap class object so //that it can be
    // uploaded on the server using this KEY
    private static final String UPLOAD_PHOTO = "bitmapPhoto";
    private static final String UPLOAD_LOGTYPR = "logType";
    private Handler mHandler;

    public AwsServiceIntent() {
        super("Aws Service Intent");
        // You don’t want your service to redeliver its process if in any case phone //shutdown and application get started
        // If you require such action you can set it to true
        setIntentRedelivery(false);


        AWSCredentials credentials = new BasicAWSCredentials("AKIAJDT3X7XPDJNCQIDQ","I+KVT2EQCt4l1YJ93GG53yf7NmGMD7hTUF7fMh34");
        rekognitionClient = new AmazonRekognitionClient(credentials);
        rekognitionClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));



    }


    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();



    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

Log.d("AWS INTENT","aws intent called with " + intent.toString());

// Getting the Action of the Intent
        String intentAction = intent.getAction();
        // Comparing the action that you received in the onHandleIntent class
        if (ACTION_UPLOAD_PHOTO.equals(intentAction)) {
            Bitmap photo = intent.getParcelableExtra(UPLOAD_PHOTO);
         final   String logType = intent.getStringExtra("logType");
       //     String logType = intent.get
         //   Toast.makeText(this, "Upload Photo", Toast.LENGTH_SHORT).show();
           final String result = uploadPhotoToServer(photo);





           if(result != "error") {

          //parse the result as name-id-random

               String[] arrSplit = result.split("-");

               String fullname = arrSplit[0];
               String id =  arrSplit[1];

              final String  name = fullname.replace("_"," ");

              name.toUpperCase();


               mHandler.post(new Runnable() {
                   @Override
                   public void run() {
             //          Toast.makeText(AwsServiceIntent.this, logType + ":" + name, Toast.LENGTH_SHORT).show();
                   }
               });

               //showLogTypeDialog

               System.out.println("intent Received");
               String s = "ASK";
               Intent RTReturn = new Intent(MainActivity.RECEIVE_JSON);
               RTReturn.putExtra("json", name);

               LocalBroadcastManager.getInstance(this).sendBroadcast(RTReturn);


               //

          //     MediaPlayer mediaPlayer  = MediaPlayer.create(this, R.raw.sample_media)
          //     mediaPlayer.start();


           }else{
               FaceDetectionProcessor.prevId = -1;
           }




        }else if (ACTION_SEND_MESSAGE.equals(intentAction)) {
            //Toast.makeText(this, "Send Message", Toast.LENGTH_SHORT).show();

        }


    }


    private String uploadPhotoToServer(Bitmap photo) {
// You can start network call to upload photo to server here
        String result = "error";


        ByteBuffer imageBytes = ByteBuffer.wrap(Base64.decode(bitUtil.bitmapToBase64(photo), Base64.DEFAULT));


        Image awsimage = new Image()
                .withBytes(imageBytes);


        // Search collection for faces similar to the largest face in the image.
        searchFacesByImageRequest = new SearchFacesByImageRequest()
                .withCollectionId(collectionId)
                .withImage(awsimage)
                .withFaceMatchThreshold(90F)
                .withMaxFaces(1);

        try {
            searchFacesByImageResult =
                    rekognitionClient.searchFacesByImage(searchFacesByImageRequest);
            List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();


           float confidence = 0;
           if (!faceImageMatches.isEmpty()) {


               FaceMatch bestMatch= faceImageMatches.get(0);
               float similarity =0;

               for (FaceMatch match : faceImageMatches) {
                   if (similarity < match.getSimilarity()){
                       bestMatch = match ;
                       similarity =  match.getSimilarity();
                   }

                           Log.d("Amazon MATCH", match.toString());
                   result = bestMatch.getFace().getExternalImageId();
               }
           }

        } catch (Exception e) {
            Log.d("Amazon Error", e.toString());

        }

     return result;

    }
}
