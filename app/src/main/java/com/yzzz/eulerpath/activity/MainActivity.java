package com.yzzz.eulerpath.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.yzzz.eulerpath.R;
import com.yzzz.eulerpath.service.FloatWindow;
import com.yzzz.eulerpath.service.OnePathService;
import com.yzzz.eulerpath.util.DisplayUtils;
import com.yzzz.eulerpath.util.GBData;
import com.yzzz.eulerpath.util.ToastUtil;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 10086;
    private final String TAG = getClass().getName();
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private QMUIGroupListView priorityListView;
    private QMUIRoundButton btnStartFloat;
    private TextView priorityText;
    Context context;
    RadioGroup radioGroupSpeedSelect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        radioGroupSpeedSelect = findViewById(R.id.radio_speed_select);
        priorityListView = findViewById(R.id.list_priority);
        btnStartFloat = findViewById(R.id.btn_start_float);
        priorityText = findViewById(R.id.priority_text);
        btnStartFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //虚拟显示器权限？
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mMediaProjectionManager != null) {
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
                }
                if(!FloatWindow.isStarted){
                    startService(new Intent(MainActivity.this, FloatWindow.class));
                }
                OnePathService.isStart();
            }
        });
        radioGroupSpeedSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rb = findViewById(i);
                String s = rb.getText().toString();
                if(s.equals("慢速")){
                    ToastUtil.show(context,"速度已改为慢速");
                    FloatWindow.speedSelect  = 2;
                }
                if(s.equals("中等")){
                    FloatWindow.speedSelect  = 1;
                    ToastUtil.show(context,"速度已改为中等");
                }
                if(s.equals("较快")){
                    FloatWindow.speedSelect  = 0;
                    ToastUtil.show(context,"速度已改为较快");
                }
            }
        });
        //opcnCV初始化
        if (!OpenCVLoader.initDebug()) {
            Logger.t(TAG).d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Logger.t(TAG).d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //虚拟按键高度
        int navigationBarHeight = DisplayUtils.getNavigationBarHeightIfRoom(context);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int x = dm.widthPixels;
        int y = dm.heightPixels+DisplayUtils.getStatusBarHeight(context);
        Logger.t(TAG).d("屏幕宽高："+x+" "+y+ "虚拟按键："+navigationBarHeight);
        FloatWindow.navigationY = y-navigationBarHeight;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
               // startService(new Intent(MainActivity.this, FloatWindow.class));
            }
        }
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "User cancelled!", Toast.LENGTH_SHORT).show();
                return;
            }
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            setUpVirtualDisplay();
        }
    }
    private void setUpVirtualDisplay() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);

        @SuppressLint("WrongConstant") final ImageReader imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2);
        mMediaProjection.createVirtualDisplay("ScreenCapture",
                dm.widthPixels, dm.heightPixels, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
        GBData.reader = imageReader;
    }
    @Override
    protected void onResume() {
        super.onResume();
        boolean floatWindow = false;
        boolean actionAccess = false;
        priorityListView.removeAllViews();
        if(!Settings.canDrawOverlays(this)){
            QMUICommonListItemView itemWithDetail = priorityListView.createItemView("悬浮窗权限");
            itemWithDetail.setOrientation(QMUICommonListItemView.VERTICAL);
            itemWithDetail.setDetailText("去授予");
            itemWithDetail.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
            QMUIGroupListView.newSection(this)
                    .addItemView(itemWithDetail, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
                        }
                    })
                    .addTo(priorityListView);
        }else {
            floatWindow = true;
        }
        //无障碍权限
        if(!OnePathService.isStart()){
            QMUICommonListItemView itemWithDetail = priorityListView.createItemView("无障碍服务权限");
            itemWithDetail.setOrientation(QMUICommonListItemView.VERTICAL);
            itemWithDetail.setDetailText("去授予");
            itemWithDetail.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
            QMUIGroupListView.newSection(this)
                    .addItemView(itemWithDetail, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));                        }
                    })
                    .addTo(priorityListView);
        }else {
            actionAccess = true;
        }
        if(floatWindow&&actionAccess){
            btnStartFloat.setVisibility(View.VISIBLE);
            priorityText.setText("已获取权限");
        }

    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Logger.t(TAG).d("OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


}
