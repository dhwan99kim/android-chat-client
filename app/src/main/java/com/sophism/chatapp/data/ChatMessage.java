package com.sophism.chatapp.data;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class ChatMessage {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;
    public static final int TYPE_IMAGE = 3;
    public static final int TYPE_MAP = 4;

    private int mType;
    private String mMessage;
    private String mUsername;
    private int mIndex;
    private int mUnreadCount;

    private ChatMessage() {}

    public int getType() {
        return mType;
    };

    public String getMessage() {
        return mMessage;
    };

    public String getUsername() {
        return mUsername;
    };

    public int getIndex() {
        return mIndex;
    };

    public int getUnreadCount() {
        return mUnreadCount;
    };

    public void reduceUnreadCount(){
        mUnreadCount--;
    }

    public static class Builder {
        private final int mType;
        private String mUsername;
        private String mMessage;
        private int mIndex;
        private int mUnreadCount;

        public Builder(int type) {
            mType = type;
        }

        public Builder username(String username) {
            mUsername = username;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Builder unreadCount(int unreadCount) {
            mUnreadCount = unreadCount;
            return this;
        }

        public Builder index(int index) {
            mIndex = index;
            return this;
        }

        public ChatMessage build() {
            ChatMessage message = new ChatMessage();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mMessage = mMessage;
            message.mUnreadCount = mUnreadCount;
            message.mIndex = mIndex;
            return message;
        }
    }


}
