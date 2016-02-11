package com.sophism.chatapp;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
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

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.sophism.chatapp.data.ChatMessage;
import com.sophism.chatapp.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class MessagingActivity extends Activity{

    private static final String TAG = "Messaging";
    private static final int TYPING_TIMER_LENGTH = 600;
    private static final int INVALID = -1;
    private Context mContext;
    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<ChatMessage> mMessages = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername = "sophism";
    private int mRoomId = INVALID;
    private Socket mSocket = SocketService.mSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
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
                    mSocket.emit("typing",mRoomId);
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

        getMessages(mRoomId);

    }
    private void addLog(String message) {
        mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addParticipantsLog(int numUsers) {
        addLog(numUsers+"명의 참가자가 있습니다");
    }

    private void addMessage(String username, String message) {
        mMessages.add(new ChatMessage.Builder(ChatMessage.TYPE_MESSAGE)
                .username(username).message(message).build());
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
        mSocket.emit("new message", message, mRoomId);
        insertDB(mUsername,mUsername,mRoomId,message);
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
                    String username;
                    String message;
                    int roomId;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                        roomId =  Integer.parseInt(data.getString("roomId"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    removeTyping(username);
                    if (roomId == mRoomId)
                        addMessage(username, message);
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
            /*String roomId;
            try {
                roomId = data.getString("roomId");
            } catch (JSONException e) {
                return;
            }*/
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

            public ViewHolder(View itemView) {
                super(itemView);

                mUsernameView = (TextView) itemView.findViewById(R.id.username);
                mMessageView = (TextView) itemView.findViewById(R.id.message);
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


    private void insertDB(String id, String name, int roomId, String message){
        ChatDatabaseHelper helper = new ChatDatabaseHelper(mContext,ChatDatabaseHelper.DATABASE_NAME, null, ChatDatabaseHelper.DATABASE_VERSION);
        helper.open();
        helper.insert(id, name, roomId, message);
        helper.close();
    }

    private void getMessages(int roomId){
        Log.d(TAG,"getMessage;");
        ChatDatabaseHelper helper = new ChatDatabaseHelper(mContext,ChatDatabaseHelper.DATABASE_NAME, null, ChatDatabaseHelper.DATABASE_VERSION);
        helper.open();
        Cursor cursor = helper.getMessages(roomId);
        cursor.moveToFirst();

        while(!cursor.isAfterLast()){
            String id = cursor.getString(0);
            String message = cursor.getString(1);
            addMessage(id,message);
            cursor.moveToNext();
        }
        if (cursor != null)
            cursor.close();
        helper.close();
    }
}
