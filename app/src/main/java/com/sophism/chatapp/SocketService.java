package com.sophism.chatapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class SocketService extends Service {
    static public Socket mSocket;
    private Context mContext;
    private Handler mHandler;
    private String mUsername = AppUtil.getInstance().getUserId();
    boolean isDisconnectFirstEvent = true;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mHandler = new Handler();
        Toast.makeText(this, "Create Socket Service", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Toast.makeText(this,"Socket Service Started",Toast.LENGTH_SHORT).show();
        try {
            mSocket = IO.socket(AppDefine.CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        mSocket.on("new message", onNewMessage);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.connect();
        Log.d("Donghwan", "add user from Socket Service");
        mSocket.emit("add user", mUsername);
        return START_STICKY;

    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    generateChatNotification(mContext,data);

                    String username;
                    String message;
                    String type;
                    int roomId;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                        type = data.getString("type");
                        roomId = Integer.parseInt(data.getString("roomId"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    insertDB(username, username, type, roomId, message);
                }
            };
            mHandler.post(runnable);

        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (isDisconnectFirstEvent) {
                isDisconnectFirstEvent = false;
                Log.d("Donghwan", "onDisconnect");
                try {
                    mSocket = IO.socket(AppDefine.CHAT_SERVER_URL);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                mSocket.connect();

            }
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("Donghwan", "onConnectError");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        mSocket = IO.socket(AppDefine.CHAT_SERVER_URL);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    mSocket.connect();
                }
            };
            mHandler.postDelayed(runnable, 5000);


        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            isDisconnectFirstEvent = true;
            Log.d("Donghwan","onConnect");
            Log.d("Donghwan","add user from Socket Service add user");
            mSocket.emit("add user", mUsername);

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void generateChatNotification(Context context, JSONObject object) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("isFromChatNoti",true);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationManager mNotifM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(object.getString("username")).setContentText(object.getString("message")).setAutoCancel(true);

            mBuilder.setContentIntent(contentIntent);
            mNotifM.notify(1, mBuilder.build());
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private void insertDB(String id, String name, String type, int roomId, String message){
        ChatDatabaseHelper helper = new ChatDatabaseHelper(mContext,ChatDatabaseHelper.DATABASE_NAME, null, ChatDatabaseHelper.DATABASE_VERSION);
        helper.open();
        helper.insert(id, name, type, roomId, message);
        helper.close();
    }
}
