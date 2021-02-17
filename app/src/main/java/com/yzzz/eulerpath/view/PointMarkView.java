package com.yzzz.eulerpath.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.yzzz.eulerpath.R;

/**
 * @author yzzz
 * @create 2021/2/16
 */
public class PointMarkView extends LinearLayout {
    public PointMarkView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.layout_point_mark,this,true);
    }
}
