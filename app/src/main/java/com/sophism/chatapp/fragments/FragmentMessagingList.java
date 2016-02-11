package com.sophism.chatapp.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.sophism.chatapp.AppDefine;
import com.sophism.chatapp.AppUtil;
import com.sophism.chatapp.MainActivity;
import com.sophism.chatapp.MessagingActivity;
import com.sophism.chatapp.R;
import com.sophism.chatapp.SocketService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class FragmentMessagingList extends Fragment {

    private final String TAG = "ChatMessagingList";
    private AppUtil util;
    private ArrayList<MessagingRoomInfo> mRoomList;
    private RoomListAdapter mAdapter;
    private Socket mSocket = SocketService.mSocket;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        util = AppUtil.getInstance();
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_messaging_room_list, container, false);
        mRoomList = new ArrayList<>();
        ListView listview_friend = (ListView) rootView.findViewById(R.id.listview_messaging_room);
        mAdapter = new RoomListAdapter(getActivity());
        listview_friend.setAdapter(mAdapter);
        getRoomList();
        mSocket.on("open room", onOpenRoom);
        mSocket.on("new message", onNewMessage);
        return rootView;
    }

    private Emitter.Listener onOpenRoom = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity()!= null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int roomId = Integer.valueOf(args[0].toString());
                        Intent intent = new Intent(getActivity(), MessagingActivity.class);
                        intent.putExtra("isFromChatNoti",true);
                        intent.putExtra("room",roomId);
                        startActivity(intent);

                    }
                });
            }
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity()!= null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        JSONObject data = (JSONObject) args[0];
                        String message;
                        String roomId;
                        try {
                            message = data.getString("message");
                            roomId =  data.getString("roomId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        for (int i=0;i<mRoomList.size();i++){
                            MessagingRoomInfo info = mRoomList.get(i);
                            if(info.room_id.equals(roomId)) {
                                info.updateMessage(message);
                                mRoomList.set(i, info);
                            }
                            mAdapter.notifyDataSetChanged();

                        }
                    }
                });
            }
        }
    };

    Gson gson = AppUtil.getGson();

    private void getRoomList(){
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(AppDefine.CHAT_SERVER_URL)
                .setConverter(new GsonConverter(gson))
                .build();

        try {
            restAdapter.create(GetRoomListService.class).friendsItems(util.getUserId(),new Callback<List<MessagingRoomInfo>>() {

                @Override
                public void success(List<MessagingRoomInfo> rooms, Response response) {
                    mRoomList = new ArrayList<>();
                    for (MessagingRoomInfo item:rooms){
                        mRoomList.add(item);
                    }
                    mAdapter.notifyDataSetChanged();
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

    public interface GetRoomListService {
        @GET("/messaging_rooms/{id}")
        void friendsItems(
                @Path("id") String id, Callback <List<MessagingRoomInfo>> callback
        );
    }

    public class RoomListAdapter extends BaseAdapter

    {
        private LayoutInflater mInflater;

        public RoomListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mRoomList.size();
        }

        @Override
        public Object getItem(int position) {
            return mRoomList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_item_messaging_room, parent, false);
                holder.chat_room_name = (TextView) convertView.findViewById(R.id.chat_room_name);
                holder.chat_last_message = (TextView) convertView.findViewById(R.id.chat_last_message);
                holder.chat_room_holder = (LinearLayout) convertView.findViewById(R.id.chat_room_holder);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.chat_room_name.setText(mRoomList.get(position).member);
            holder.chat_last_message.setText(mRoomList.get(position).message);
            holder.chat_room_holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSocket.emit("invite",mRoomList.get(position).member);

                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView chat_room_name;
            TextView chat_last_message;
            LinearLayout chat_room_holder;
        }
    }

    public class MessagingRoomInfo{
        String member;
        String room_id;
        String message;

        public void updateMessage(String message){
            this.message = message;
        }
    }
}
