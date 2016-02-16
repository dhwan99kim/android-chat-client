package com.sophism.chatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Created by D.H.KIM on 2016. 2. 12.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    Context context;
    boolean isProfile;
    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
        isProfile = false;
    }
    public DownloadImageTask(Context context, ImageView bmImage, boolean isProfile) {
        this.context = context;
        this.bmImage = bmImage;
        this.isProfile = isProfile;
    }
    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon1 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon1 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (urldisplay != null & mIcon1 != null) {
            if (isProfile){
                mIcon1 = AppUtil.getRoundedCroppedBitmap(mIcon1);
            }
            AppUtil.sImageCashe.put(urldisplay, mIcon1);
        }
        return mIcon1;
    }

    protected void onPostExecute(Bitmap result) {
        if (result == null && isProfile){
            bmImage.setImageBitmap(AppUtil.getRoundedCroppedBitmap(BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.noavatar)));
        }else{
            bmImage.setImageBitmap(result);
        }


    }
}
