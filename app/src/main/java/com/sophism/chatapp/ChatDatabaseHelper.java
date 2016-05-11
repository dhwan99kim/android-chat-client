package com.sophism.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class ChatDatabaseHelper extends SQLiteOpenHelper {

    public static final String TAG = "ChatDatabaseHelper";
    public static final String DATABASE_NAME = "chat.db";
    public static final int DATABASE_VERSION = 7;
    private static final String TABLE_NAME = "chat_log";
    private static SQLiteDatabase mDB;
    private Context mContext;


    public ChatDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory cursorFactory, int version){
        super(context, name, cursorFactory, version);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table " + TABLE_NAME +" (" +
                "id text, " +
                "name text, " +
                "type text, " +
                "room_id text, " +
                "message text, "+
                "idx int, "+
                "unread_count int, "+
                "read int);";
        db.execSQL(sql);
        Toast.makeText(mContext, "DB onCreate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Toast.makeText(mContext, "DB onUpgrade", Toast.LENGTH_SHORT).show();
        db.execSQL("DROP TABLE IF EXISTS chat_log");
        onCreate(db);
    }

    public void open(){
        mDB = getWritableDatabase();
    }
    public void close(){
        mDB.close();
    }

    public void insert(String id, String name, String type, int room_id, String message, int read, int idx, int unread_count){
        if (mDB != null) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            values.put("type", type);
            values.put("room_id", room_id);
            values.put("message", message);
            values.put("read",read);
            values.put("idx",idx);
            values.put("unread_count",unread_count);
            mDB.insert(TABLE_NAME, null, values);
        }
    }

    public Cursor getMessages(int roomId){
        String sql = "select type, id, message, read, idx, unread_count from " + TABLE_NAME + " where room_id ='"+roomId+ "';";
        return mDB.rawQuery(sql, null);
    }

    public void reduceUnreadCount(int idx){
        String sql = "select unread_count from " + TABLE_NAME + " where idx ='"+idx+ "';";
        Cursor cursor = mDB.rawQuery(sql, null);
        cursor.moveToFirst();

        while(!cursor.isAfterLast()){
            int unread_cnt = cursor.getInt(0);
            if (unread_cnt >0) {
                ContentValues values = new ContentValues();
                values.put("unread_count", unread_cnt - 1);
                mDB.update(TABLE_NAME, values, "idx =?", new String[]{Integer.toString(idx)});
            }
            cursor.moveToNext();
        }
        if (cursor != null)
            cursor.close();
    }

    public void setReadCheck(int idx){
        Log.d(TAG,"setReadCheck "+idx );
        ContentValues values = new ContentValues();
        values.put("read", 1);
        mDB.update(TABLE_NAME,values,"idx =?",new String[]{Integer.toString(idx)});
    }
}