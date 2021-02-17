package com.yzzz.eulerpath.view;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;


import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.yzzz.eulerpath.R;

/**
 * @author yzzz
 * @create 2020/9/11
 */
public class MyDialog extends Dialog {
    public QMUIRoundButton button;
    public MyDialog(Context context) {
        super(context, R.style.inputDialog);
        setContentView(R.layout.dialog_text);
        button=findViewById(R.id.dialog_btn_ok);

        //setCanceledOnTouchOutside(false);
    }
    public void setText(String string){
        TextView textView = findViewById(R.id.dialog_text_content);
        textView.setText(string);
    }
}

