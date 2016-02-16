package com.sophism.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class ChatDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "chat.db";
    public static final int DATABASE_VERSION = 1;
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
                "message text);";
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

    public void insert(String id, String name, String type, int room_id, String message){
        if (mDB != null) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            values.put("type", type);
            values.put("room_id", room_id);
            values.put("message", message);
            mDB.insert(TABLE_NAME, null, values);
        }
    }

    public Cursor getMessages(int roomId){
        String sql = "select type, id, message from " + TABLE_NAME + " where room_id ='"+roomId+ "';";
        Cursor results = mDB.rawQuery(sql, null);
        return results;
    }

}