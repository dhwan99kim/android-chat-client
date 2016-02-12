package com.sophism.chatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.File;
import java.util.Date;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class AppUtil {

    private final String PREF_NAME = "PREF";
    private final String PREF_KEY_USER_ID = "PREF_KEY_USER_ID";

    private static AppUtil mSelf = null;
    private static Context mContext = null;
    private SharedPreferences mPreference = null;
    private SharedPreferences.Editor mEditor = null;



    private AppUtil(){
        mPreference = mContext.getSharedPreferences(PREF_NAME, 1);
        mEditor = mPreference.edit();
    }


    static public RestAdapter getRestAdapter(){
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(AppDefine.CHAT_SERVER_URL)
                .setConverter(new GsonConverter(gson))
                .build();
        return restAdapter;
    }
    public static AppUtil getInstance(){
        if (mSelf == null){
            mSelf = new AppUtil();
        }
        return mSelf;
    }

    public static void init(Context context){
        mContext = context;
    }

    public void setUserId(String userId){
        setData(PREF_KEY_USER_ID, userId);
    }
    public String getUserId(){
        return mPreference.getString(PREF_KEY_USER_ID, null);
    }


    private void setData(String key, int aValue){
        mEditor.putInt(key, aValue);
        mEditor.commit();
    }

    private void setData(String key, boolean aBool){
        mEditor.putBoolean(key, aBool);
        mEditor.commit();
    }

    private void setData(String key, String aString){
        mEditor.putString(key, aString);
        mEditor.commit();
    }

    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static int checkPhotoOrientation(String imagePath){
        int rotate = 0;
        try {
            File imageFile = new File(imagePath);
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return rotate;
    }
}
