package com.sophism.chatapp.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sophism.chatapp.AppUtil;
import com.sophism.chatapp.R;

import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class DialogSignIn  extends Dialog {

    final static private String TAG = "SignIn";
    private Context mContext;
    private EditText mEditTextID, mEditTextNick, mEditTextPW, mEditTextPWConfirm;
    public DialogSignIn(Context context){
        super(context,android.R.style.Theme_Translucent_NoTitleBar);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.height = WindowManager.LayoutParams.MATCH_PARENT;
        lpWindow.width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.dialog_signin);

        mEditTextID = (EditText)findViewById(R.id.edittext_id);
        mEditTextNick = (EditText)findViewById(R.id.edittext_nick);
        mEditTextPW = (EditText)findViewById(R.id.edittext_pw);
        mEditTextPWConfirm = (EditText)findViewById(R.id.edittext_pw_confirm);

        TextView sign_in_btn = (TextView) findViewById(R.id.sign_in_btn);
        sign_in_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditTextPW.getText().toString().equals(mEditTextPWConfirm.getText().toString())) {
                    signIn();
                }else{
                    Toast.makeText(mContext,"비밀번호가 일치하지 않습니다",Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
    private void signIn(){
        RestAdapter restAdapter = AppUtil.getRestAdapter();
        try {
            JSONObject object = new JSONObject();
            object.put("user_id",mEditTextID.getText().toString());
            object.put("nickname",mEditTextNick.getText().toString());
            object.put("password", mEditTextPW.getText().toString());
            String json = object.toString();
            TypedInput in = new TypedByteArray("application/json", json.getBytes("UTF-8"));
            restAdapter.create(SignService.class).signin(in, new Callback<String>() {

                @Override
                public void success(String friend, Response response) {
                    Activity activity = (Activity) mContext;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "SignIn Success", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    });

                }

                @Override
                public void failure(RetrofitError error) {
                    Activity activity = (Activity) mContext;
                    if (error.getResponse() == null){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Server Down", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else if (error.getResponse().getStatus() == 409) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Please Use other ID", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    Log.e(TAG, error.toString());
                }

            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public interface SignService {
        @POST("/users/")
        @Headers({"Content-Type: application/json;charset=UTF-8"})
        void signin(
                @Body TypedInput object, Callback<String> callback
        );
    }
}
