package com.sophism.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sophism.chatapp.dialogs.DialogSignIn;

import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;

/**
 * Created by D.H.KIM on 2016. 2. 5.
 */
public class LoginActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "Login";
    private EditText mEditTextID;
    private EditText mEditTextPW;
    private AppUtil util;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.init(this);
        util = AppUtil.getInstance();
        if (util.getUserId() != null){
            startService(new Intent(LoginActivity.this, SocketService.class));
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }
        setContentView(R.layout.activity_login);
        mEditTextID = (EditText) findViewById(R.id.edittext_id);
        mEditTextPW = (EditText) findViewById(R.id.edittext_pw);

        Button log_in_button = (Button) findViewById(R.id.log_in_button);
        log_in_button.setOnClickListener(this);
        TextView sign_in_btn = (TextView) findViewById(R.id.sign_in_btn);
        sign_in_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.log_in_button:
                loginCheck();
                break;
            case R.id.sign_in_btn:
                DialogSignIn dialog = new DialogSignIn(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.show();
                break;
        }
    }

    public void loginCheck(){
        if (mEditTextID.getText().length() == 0 || mEditTextPW.getText().length() == 0){
            Toast.makeText(this,"Please Check USER ID or Password",Toast.LENGTH_SHORT).show();
            return;
        }
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(AppDefine.CHAT_SERVER_URL)
                .setConverter(new GsonConverter(gson))
                .build();
        try {
            JSONObject object = new JSONObject();
            object.put("user_id",mEditTextID.getText().toString());
            object.put("password", mEditTextPW.getText().toString());
            String json = object.toString();
            TypedInput in = new TypedByteArray("application/json", json.getBytes("UTF-8"));
            restAdapter.create(LoginService.class).login(in, new Callback<String>() {

                @Override
                public void success(String friend, Response response) {
                    util.setUserId(mEditTextID.getText().toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startService(new Intent(LoginActivity.this, SocketService.class));
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });
                }

                @Override
                public void failure(RetrofitError error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e(TAG, error.toString());
                }

            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    Gson gson = AppUtil.getGson();


    public interface LoginService {
        @POST("/login/")
        @Headers({"Content-Type: application/json;charset=UTF-8"})
        void login(
                @Body TypedInput object, Callback<String> callback
        );
    }
}
