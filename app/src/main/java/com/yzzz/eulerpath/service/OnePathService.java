package com.yzzz.eulerpath.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import com.orhanobut.logger.Logger;

/**
 * @author yzzz
 * @create 2021/2/13
 */
public class OnePathService extends AccessibilityService {
    private final String TAG = getClass().getName();

    public static OnePathService mService;
    //初始化
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Logger.t(TAG).d("onServiceConnected");
        mService = this;
    }

    //实现辅助功能
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
       // Logger.t(TAG).d("onAccessibilityEvent"+event.toString());
    }

    @Override
    public void onInterrupt() {
        Logger.t(TAG).d("onInterrupt");
        mService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.t(TAG).d("onDestroy");
        mService = null;
    }
    /**
     * 辅助功能是否启动
     */
    public static boolean isStart() {
        return mService != null;
    }
    /**
     * 立即发送移动的手势
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     *
     * @param path  移动路径
     * @param mills 持续总时间
     */
    @RequiresApi(24)
    public void dispatchGestureMove(Path path, long mills) {
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, mills)).build(), null, null);

    }

    /**
     * 点击指定位置
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     */
    @RequiresApi(24)
    public void dispatchGestureClick(int x, int y) {
        Path path = new Path();
        path.moveTo(x - 1, y - 1);
        path.lineTo(x + 1, y + 1);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, 100)).build(), null, null);
    }
}
