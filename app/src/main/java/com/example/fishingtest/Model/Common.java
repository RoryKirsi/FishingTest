package com.example.fishingtest.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Completed by Kan Bu and Ziqi Zhang on 8/06/2019.
 *
 * It is used for data sharing and passing between different
 * activity, constant values and public static method storing.
 */

public class Common {

    public Common() {
    }

    private final static String TAG = "Common";

    // Use for value passing between Recycler View adapter and the "ViewCompetitionDetails" activity
    public static Competition currentCompItem = null;
    public static Post currentPostItem = null;

    // User Location info
    public static double curLat = 200.0;
    public static double curLon = 200.0;
    public static Location curLoc;

    // Constants used in Competition and User classes
    public static String NA = "NA";
    public static Date NA_Date = new Date(2019,1,1);
    public static int NA_Integer = -1;
    public static int EMPTY_SPINNER = 0;
    public static String EMPTY = "";


    // Constants used in User class
    public static String user_member = "Member";
    public static String user_admin = "Administrator";

    // Constants used in Competition result selection
    public static String COMPID = "compID";
    public static String COMPNAME = "compName";


    // Check the time to the competition date
    // competition date given in the format of  "14/03/2019 20:50 GMT+08:00"
    public static long timeToCompStart(String compTime_str){

        long diff = -1L;
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm z");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date compDate = new Date();
        try {
            // Convert string into Date
            compDate = df.parse(compTime_str);
            System.out.println("Converted by df2 = " + df.format(compDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),Locale.getDefault());
        Date currentLocalTime = calendar.getTime();
        TimeZone uTimeZone= calendar.getTimeZone();

        if(uTimeZone.inDaylightTime(new Date())) {
            diff = compDate.getTime() -(currentLocalTime.getTime()+uTimeZone.getDSTSavings());
        }else {
            diff = compDate.getTime() -currentLocalTime.getTime();
        }

        return diff;


    };

    // Discovery Fragment Sort Order
    public static int DISCOVERY_SORT_ORDER = -1;

    //This function include uploading to storage and database.
    // Before image save its download url in database, the image should be uploaded to the cloud storage
    // and get the download url from the storage,
    // so that we can save the download url to database and retrieve by another usage directly for downloading the image.
    public static void uploadFishingPost(final Context context, final DatabaseReference database, final FirebaseUser currentUser, final Competition currentComp, final Uri oriImageUri, final Uri meaImageUri, final String measuredData, final String fishingName) {
        Long tsLong = System.currentTimeMillis()/1000;
        String timestamp = tsLong.toString();

        final String competitionCategory = "Competitions";
        final String competitionImageCategory = "Post_Images";
        final String originalFishPhotoCategory = "Original";
        final String measuredFishPhotoCategory = "Measured";
        final String postID = timestamp + "_" + currentUser.getUid();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference compRef = storageRef.child(competitionImageCategory);
        StorageReference userRef = compRef.child(currentUser.getUid());
        StorageReference postRef = userRef.child(postID);
        StorageReference originalImagesRef = postRef.child(originalFishPhotoCategory);
        StorageReference measuredImagesRef = postRef.child(measuredFishPhotoCategory);

        String originalFilename = currentUser.getUid() + "_" + timestamp + "_ori";
        String measuredFilename = currentUser.getUid() + "_" + timestamp + "_mea";
        StorageReference originalFileRef = originalImagesRef.child(originalFilename);
        StorageReference measuredFileRef = measuredImagesRef.child(measuredFilename);

        DatabaseReference postDBRef = database.child("Posts").child(competitionCategory).child(currentComp.getCompID()).child(postID);
        DatabaseReference userDBRef = database.child("Users").child(currentUser.getUid());
        //create upload task which is a new thread, then it will upload the image to cloud storage by local image uri
        UploadTask uploadOriTask = originalFileRef.putFile(oriImageUri);
        UploadTask uploadMeaTask = measuredFileRef.putFile(meaImageUri);

        // invoke upload task to upload given images, if success, it will invoke sending post to database with the url of uploaded given images
        uploadOriTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle unsuccessful uploads
                Toast.makeText(context, "Original Image: Upload failed!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri oriDownloadUrl = taskSnapshot.getDownloadUrl();
                Toast.makeText(context, "Original Image: Upload finished!", Toast.LENGTH_SHORT).show();
                uploadMeaTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle unsuccessful uploads+
                        Toast.makeText(context, "Measured Image: Upload failed!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri meaDownloadUrl = taskSnapshot.getDownloadUrl();
                        Toast.makeText(context, "Measured Image: Upload finished!", Toast.LENGTH_SHORT).show();
                        if (uploadOriTask.isComplete() && uploadMeaTask.isComplete()) {
                            Post post = new Post(postID, currentUser.getUid(), currentComp.getCompID(), oriDownloadUrl.toString(), meaDownloadUrl.toString(), measuredData, fishingName, timestamp, curLon, curLat);
                            Toast.makeText(context, "Posting......", Toast.LENGTH_SHORT).show();
                            postToDB(context, postDBRef, post);
                            attendedToComp(context, userDBRef, currentComp.getCompID());
                        }
                    }
                });
            }
        });
    }

    //upload the post with additional information to database,
    // it will upload the image and post as two different category in database
    // and link themselves by post id to match the image and post
    private static void postToDB(final Context context, final DatabaseReference database, Post post) {
        database.setValue(post);
        Toast.makeText(context, "Post Success!", Toast.LENGTH_SHORT).show();
    }

    // update user to attended list of given competition and remove uesr from registerd list if user has been posted
    private static void attendedToComp(final Context context, final DatabaseReference database, String compID) {
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User thisuser = dataSnapshot.getValue(User.class);

                //get both list of attended and registered
                ArrayList<String> user_attend = thisuser.getComps_attended();
                if (user_attend != null) {
                    if (!user_attend.contains(compID)) {
                        //reconstruct the list to add user to attended list and remove from registered list
                        user_attend.add(compID);
                        database.child("comps_attended").setValue(user_attend);

                        List<String> user_registeredComps = thisuser.getComps_registered();
                        user_registeredComps.remove(compID);
                        database.child("comps_registered").setValue(user_registeredComps);
                    }
                } else {
                    //reconstruct the list to add user to attended list and remove from registered list
                    ArrayList<String> temp_user_attend = new ArrayList<>();
                    temp_user_attend.add(compID);
                    database.child("comps_attended").setValue(temp_user_attend);

                    List<String> user_registeredComps = thisuser.getComps_registered();
                    user_registeredComps.remove(compID);
                    database.child("comps_registered").setValue(user_registeredComps);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        database.addListenerForSingleValueEvent(userListener);
        Toast.makeText(context, "Add attended Success!", Toast.LENGTH_SHORT).show();
    }

    //send the comment object to given competition database reference
    public static void commentToDB(final Context context, final DatabaseReference database, Comment comment) {
        database.setValue(comment);
        Toast.makeText(context, "Comment Success!", Toast.LENGTH_SHORT).show();
    }

    // transform given file path of image to uri path
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        // move the cursor to given file path
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);

        //retrieve image uri path in media store by the cursor location in media store
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    //transform timestamp to human readable time
    public static String timeStampToTime(String timeStamp) {
        String result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(String.valueOf(timeStamp)) * 1000));
        return result;
    }

    // transform density independent pixel to pixel
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }


    public static Boolean verifyTime(String timeStr){
        boolean answer = false;

        if(timeStr.contains(":")){
            String[] data = timeStr.trim().split(":");
            if(data.length ==2){
                int hour = Integer.parseInt(data[0]);
                int min = Integer.parseInt(data[1]);
                if (hour <= 24 && hour >= 0) {
                    if(min <= 60 && min >= 0)
                        answer = true;

                }
            }
        }
        return answer;
    }

    public static Boolean verifyGeoInfo(String geoStr){
        boolean answer = false;

        if(geoStr.contains(",")){
            String[] data = geoStr.trim().split(",");
            if(data.length ==3){
                try {
                    double lat = Double.parseDouble(data[0]);
                    double lon = Double.parseDouble(data[1]);
                    double rds = Double.parseDouble(data[2]);
                    answer = true;
                }catch (Exception e){
                    Log.e(TAG, "Geo info formatting incorrect");
                }
            }
        }
        return answer;
    }


    // transform Degrees Minutes Seconds to Decimal Degrees Coordinates for EXIF format
    public static double DMStoDD(String givenDMS, String geoRef) {
        double dimensionality = 0.0;
        if (null==givenDMS){
            return dimensionality;
        }

        String[] split = givenDMS.split(",");
        for (int i = 0; i < split.length; i++) {

            String[] s = split[i].split("/");
            double v = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);
            dimensionality=dimensionality+v/Math.pow(60,i);
        }

        if (geoRef.equals("S") || geoRef.equals("W")) {
            dimensionality = dimensionality * -1;
        }

        return dimensionality;
    }

    //check whether given location is in given location and its radius
    public static boolean ifInCircle(Location center, Location test, float radius) {
        float distanceInMeters = center.distanceTo(test);
        boolean isWithin = distanceInMeters < radius;
        if (isWithin) {
            return true;
        } else {
            return false;
        }
    }
}
