package com.sophism.chatapp.view;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sophism.chatapp.R;

/**
 * Created by D.H.KIM on 2016. 2. 11.
 */
public class DialogInputText extends Dialog {
    private Context mContext;
    private String mInputValue;
    private String mTitle;
    private EditText mEditText;
    private TextView mTitleText;
    private Resources res;
    public DialogInputText(Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mContext = context;
    }

    public DialogInputText(Context context, String title) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mTitle = title;
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //backgrond dim
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.5f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.dialog_input_text);
        res = mContext.getResources();
        getWindow().setLayout(res.getDimensionPixelOffset(R.dimen.input_text_dialog_width), res.getDimensionPixelOffset(R.dimen.input_text_dialog_height));

        mEditText = (EditText) findViewById(R.id.edittext);
        mTitleText = (TextView) findViewById(R.id.title);

        Button submit_btn = (Button) findViewById(R.id.submit_btn);
        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInputValue = mEditText.getText().toString();
                dismiss();
            }
        });

        if (mTitle != null){
            mTitleText.setText(mTitle);
        }
    }

    public void setTitle(String title){
        mTitleText.setText(title);
    }

    public String getValue(){
        return mInputValue;
    }
}
