package com.sophism.chatapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;

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
    static public Gson getGson(){
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
    }

    private AppUtil(){
        mPreference = mContext.getSharedPreferences(PREF_NAME, 1);
        mEditor = mPreference.edit();
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
}
