package com.sophism.chatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.sophism.chatapp.data.ChatMessage;
import com.sophism.chatapp.R;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class MessagingActivity extends Activity{

    private static final String TAG = "Messaging";
    private final int RESULT_LOAD_IMG = 1;

    private static final int TYPING_TIMER_LENGTH = 600;
    private static final int INVALID = -1;
    private AppUtil util;
    private Context mContext;
    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<ChatMessage> mMessages = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername = AppUtil.getInstance().getUserId();
    private int mRoomId = INVALID;
    private Socket mSocket = SocketService.mSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        util = AppUtil.getInstance();
        setContentView(R.layout.activity_messaging);
        mAdapter = new MessageAdapter(this, mMessages);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewMessage);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.on("invite", onInvite);
        mSocket.connect();

        int roomID = getIntent().getIntExtra("room",INVALID);
        if (roomID != INVALID){
            mRoomId = roomID;
        }
        mMessagesView = (RecyclerView) findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(this));
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("typing", mRoomId);
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageView sendButton = (ImageView) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
            }
        });

        ImageView file_upload_btn = (ImageView) findViewById(R.id.file_upload_btn);
        file_upload_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
            }
        });
        getMessages(mRoomId);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                if (cursor != null) {
                    cursor.moveToFirst();
                }

                int columnIndex;
                String imgDecodableString;
                if (cursor != null) {
                    columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();
                }else
                    return;
                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setLogLevel(RestAdapter.LogLevel.FULL)
                        .setClient(new OkClient(new OkHttpClient()))
                        .setEndpoint(AppDefine.CHAT_SERVER_URL)
                        .build();
                try {
                    TypedFile typedFile = new TypedFile("multipart/form-data", new File(imgDecodableString));
                    restAdapter.create(FileUploadService.class).upload(typedFile,  new Callback<Result>() {

                        @Override
                        public void success(Result s, Response response) {
                            try {
                                String path = AppDefine.CHAT_SERVER_URL+"/files/"+s.url;
                                mSocket.emit("new message", "image", path, mRoomId);
                                addMessage("image", mUsername, path);
                                insertDB(mUsername, mUsername, "image", mRoomId, path);
                                Log.d(TAG, "Success");
                            }catch(Exception e){
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void failure(RetrofitError error) {

                            Log.d(TAG, error.toString());
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    private void uploadProfileImage(Intent data){
        Uri selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        // Get the cursor
        Cursor cursor = getContentResolver().query(selectedImage,
                filePathColumn, null, null, null);
        // Move to first row
        if (cursor != null) {
            cursor.moveToFirst();
        }

        int columnIndex;
        String imgDecodableString;
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            imgDecodableString = cursor.getString(columnIndex);
            cursor.close();
        }else
            return;
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setClient(new OkClient(new OkHttpClient()))
                .setEndpoint(AppDefine.CHAT_SERVER_URL)
                .build();
        try {
            TypedFile typedFile = new TypedFile("multipart/form-data", new File(imgDecodableString));
            String description = "hello, this is description speaking";
            restAdapter.create(AvatarUploadService.class).upload(util.getUserId(),typedFile, description,  new Callback<String>() {

                @Override
                public void success(String s, Response response) {

                    Log.d(TAG, "Success");
                }

                @Override
                public void failure(RetrofitError error) {

                    Log.d(TAG, error.toString());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public interface FileUploadService {
        @Multipart
        @POST("/files/")
        void upload(@Part("myfile") TypedFile file,
                    Callback<Result> callback);
    }

    public class Result{
        public String url;
    }
    public interface AvatarUploadService {
        @Multipart
        @POST("/users/{id}/avatar")
        void upload(@Path("id")String id,
                    @Part("myfile") TypedFile file,
                    @Part("description") String description,
                    Callback<String> callback);
    }

    private void addLog(String message) {
        mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addParticipantsLog(int numUsers) {
        addLog(numUsers + "명의 참가자가 있습니다");
    }

    private void addMessage( String username, String message) {
        addMessage("text", username, message);
    }
    private void addMessage(String type, String username, String message) {
        if (type.equals("text")) {
            mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_MESSAGE)
                    .username(username).message(message).build());
        }else if (type.equals("image")){
            mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_IMAGE)
                    .username(username).message(message).build());
        }
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = mMessages.get(i);
            if (message.getType() == ChatMessage.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() {
        if (null == mUsername) return;
        if (!mSocket.connected()) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", "text", message, mRoomId);
        insertDB(mUsername,mUsername,"text",mRoomId,message);
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    String username, message, type;
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
                    removeTyping(username);
                    if (roomId == mRoomId) {
                        addMessage(type, username, message);
                    }
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(username + "님이 입장하였습니다");
                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(username + "님이 퇴장하였습니다");
                    addParticipantsLog(numUsers);
                    removeTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(username);
                }
            });

        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                }
            });

        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("stop typing",mRoomId);
        }
    };

    private Emitter.Listener onInvite = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            int room = Integer.valueOf(args[0].toString());
            Log.d("Donghwan", "invited to" + room);
            mRoomId = room;
            mSocket.emit("join",room);
        }
    };

    public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

        private List<ChatMessage> mMessages;
        private int[] mUsernameColors;

        public MessageAdapter(Context context, List<ChatMessage> messages) {
            mMessages = messages;
            mUsernameColors = context.getResources().getIntArray(R.array.username_colors);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int layout = -1;
            switch (viewType) {
                case ChatMessage.TYPE_MESSAGE:
                    layout = R.layout.list_item_chat_message;
                    break;
                case ChatMessage.TYPE_LOG:
                    layout = R.layout.list_item_chat_log;
                    break;
                case ChatMessage.TYPE_ACTION:
                    layout = R.layout.list_item_chat_action;
                    break;
                case ChatMessage.TYPE_IMAGE:
                    layout = R.layout.list_item_chat_message_image;
                    break;
            }
            View v = LayoutInflater
                    .from(parent.getContext())
                    .inflate(layout, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            ChatMessage message = mMessages.get(position);
            viewHolder.setMessage(message.getMessage());
            viewHolder.setUsername(message.getUsername());
            viewHolder.setImage(message.getMessage());
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mMessages.get(position).getType();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mUsernameView;
            private TextView mMessageView;
            private ImageView mImageView;
            public ViewHolder(View itemView) {
                super(itemView);

                mUsernameView = (TextView) itemView.findViewById(R.id.username);
                mMessageView = (TextView) itemView.findViewById(R.id.message);
                mImageView = (ImageView) itemView.findViewById(R.id.image);
            }

            public void setUsername(String username) {
                if (null == mUsernameView) return;
                mUsernameView.setText(username);
                mUsernameView.setTextColor(getUsernameColor(username));
            }

            public void setMessage(String message) {
                if (null == mMessageView) return;
                mMessageView.setText(message);
            }

            public void setImage(String url) {
                if (null == mImageView) return;
                new DownloadImageTask(MessagingActivity.this, mImageView, true).execute(url);
            }

            private int getUsernameColor(String username) {
                int hash = 7;
                for (int i = 0, len = username.length(); i < len; i++) {
                    hash = username.codePointAt(i) + (hash << 5) - hash;
                }
                int index = Math.abs(hash % mUsernameColors.length);
                return mUsernameColors[index];
            }
        }
    }


    private void insertDB(String id, String name, String type, int roomId, String message){
        ChatDatabaseHelper helper = new ChatDatabaseHelper(mContext,ChatDatabaseHelper.DATABASE_NAME, null, ChatDatabaseHelper.DATABASE_VERSION);
        helper.open();
        helper.insert(id, name, type, roomId, message);
        helper.close();
    }

    private void getMessages(int roomId){
        Log.d(TAG,"getMessage;");
        ChatDatabaseHelper helper = new ChatDatabaseHelper(mContext,ChatDatabaseHelper.DATABASE_NAME, null, ChatDatabaseHelper.DATABASE_VERSION);
        helper.open();
        Cursor cursor = helper.getMessages(roomId);
        cursor.moveToFirst();

        while(!cursor.isAfterLast()){
            String type = cursor.getString(0);
            String id = cursor.getString(1);
            String message = cursor.getString(2);

            addMessage(type,id,message);
            cursor.moveToNext();
        }
        if (cursor != null)
            cursor.close();
        helper.close();
    }
}
