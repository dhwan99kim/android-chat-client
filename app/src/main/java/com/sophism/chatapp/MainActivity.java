package com.sophism.chatapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.sophism.chatapp.fragments.FragmentFriendList;
import com.sophism.chatapp.fragments.FragmentMessagingList;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tab_friend = (TextView) findViewById(R.id.tab_friend);
        tab_friend.setOnClickListener(this);
        TextView tab_chat = (TextView) findViewById(R.id.tab_chat);
        tab_chat.setOnClickListener(this);
        changeFragment(new FragmentFriendList());

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.tab_chat:
                changeFragment(new FragmentMessagingList());
                break;
            case R.id.tab_friend:
                changeFragment(new FragmentFriendList());
                break;
        }
    }

    public void changeFragment(Fragment fragment){
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_holder, fragment);
        ft.commit();
    }
}
