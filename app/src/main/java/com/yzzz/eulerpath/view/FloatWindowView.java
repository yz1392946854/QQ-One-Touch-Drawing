package com.yzzz.eulerpath.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.yzzz.eulerpath.R;

/**
 * @author yzzz
 * @create 2021/2/13
 */
public class FloatWindowView extends LinearLayout {
    public QMUIRoundButton btnFindEdge;
    public QMUIRoundButton findCircles;
    public QMUIRoundButton btnBuildPath;
    public FloatWindowView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.layout_float_window,this,true);
        btnFindEdge = findViewById(R.id.btn_find_edge);
        findCircles = findViewById(R.id.btn_find_circles);
        btnBuildPath = findViewById(R.id.btn_build_path);
    }
}
