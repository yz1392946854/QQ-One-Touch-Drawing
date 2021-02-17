package com.yzzz.eulerpath.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;

import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.yzzz.eulerpath.R;
import com.yzzz.eulerpath.util.DisplayUtils;
import com.yzzz.eulerpath.util.GBData;
import com.yzzz.eulerpath.util.PointUtil;
import com.yzzz.eulerpath.util.ToastUtil;
import com.yzzz.eulerpath.util.UnionFind;
import com.yzzz.eulerpath.view.FloatWindowView;
import com.yzzz.eulerpath.view.MyDialog;
import com.yzzz.eulerpath.view.PointMarkView;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yzzz
 * @create 2021/2/13
 */

public class FloatWindow extends Service {
    public static boolean isStarted = false;
    private final String TAG = getClass().getName();
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private List<List<Integer>> edges;
    ExecutorService fixedThreadPool ;
    FloatWindowView floatWindowView;
    Context context;
    Handler handler;
    //虚拟按键栏顶点坐标
    public static int navigationY = 1920;
    protected MyDialog myDialog;
    public static int speedSelect = 0;
    private int[][] delayTime = {
            {100,100,1},
            {150,150,1},
            {200,200,1}
    };
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        fixedThreadPool = Executors.newFixedThreadPool(5);
        layoutParams = new WindowManager.LayoutParams();
        handler = new Handler();

        //初始化diolog
        myDialog = new MyDialog(this);
        myDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        myDialog.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId()== R.id.dialog_btn_ok){
                    myDialog.dismiss();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = DisplayUtils.dp2px(this, 120);
        layoutParams.height = DisplayUtils.dp2px(this, 100);
        layoutParams.x = 900;
        layoutParams.y = 1400;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            floatWindowView = new FloatWindowView(this);
            floatWindowView.btnFindEdge.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    fixedThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.showLong(context,"开始生成邻接矩阵，请勿触碰屏幕，此过程大约需要几十秒");
                                }
                            });
                            //生成邻接矩阵，单线程，会阻塞屏幕
                            edges = getEdge();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myDialog.setText("邻接矩阵生成完成，请尝试解题\n"+"总共找到 "+edges.size()+" 条边");

                                    myDialog.show();
                                }
                            });
                        }
                    });
                }
            });
            floatWindowView.findCircles.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //先把悬浮窗隐藏一下
                    windowManager.removeView(floatWindowView);
                    fixedThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            //等屏幕刷新
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Mat circles = GBData.getCircles();
                            //返回按钮位置
                            float[] returnPoint = new float[2];

                            List<List<Double>> lst = new ArrayList<>();
                            for (int i = 0; i < circles.cols(); i++)
                            {
                                double[] vCircle = circles.get(0, i);
                                //有可能出现坐标小于1的点，过滤掉
                                if(vCircle[0]<1||vCircle[1]<1){
                                    continue;
                                }
                                //状态栏可能有圆形，过滤一下
                                if (vCircle[1]<DisplayUtils.dp2px(context,20)){
                                    continue;
                                }
                                //虚拟按键可能有圆，过滤
                                if(vCircle[1]>navigationY){
                                    continue;
                                }
                                ArrayList<Double> objects = new ArrayList<>();
                                objects.add(vCircle[0]);
                                objects.add(vCircle[1]);
                                lst.add(objects);
                                if(returnPoint[1]<vCircle[1]){
                                    returnPoint[0] = (float) vCircle[0];
                                    returnPoint[1] = (float) vCircle[1];
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    windowManager.addView(floatWindowView, layoutParams);
                                }
                            });
                            Logger.t(TAG).d(lst);
                            Logger.t(TAG).d(lst.size());
                            //排序，转成二维数组
                            Collections.sort(lst,(o1,o2)->{
                                if(o1.get(0).intValue()==o2.get(0).intValue()){
                                    return o1.get(1).intValue()-o2.get(1).intValue();
                                }
                                return o1.get(0).intValue()-o2.get(0).intValue();
                            });
                            float point[][] = new float[lst.size()-1][2];
                            int p = 0;
                            for(List<Double> i:lst){
                                if(i.get(1)!=returnPoint[1]){
                                    point[p][0] =  i.get(0).floatValue();
                                    point[p++][1] = i.get(1).floatValue();
                                }
                            }
                            PointUtil.point = point;
                            PointUtil.returnPoint = returnPoint;
                            Logger.t(TAG).d(PointUtil.point);
                            Logger.t(TAG).d(PointUtil.returnPoint);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.show(context,"定位圆坐标完成，共找到"+point.length+"个特征点");
                                    List<WindowManager.LayoutParams> lst = getLayOutParams(point);
                                    for(WindowManager.LayoutParams i:lst){
                                      //  windowManager.addView(new PointMarkView(context),i);
                                    }
                                }
                            });
                        }
                    });
                }
            });
            floatWindowView.btnBuildPath.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    buildPath(edges);
                }
            });
            windowManager.addView(floatWindowView, layoutParams);

            floatWindowView.setOnTouchListener(new FloatingOnTouchListener());
        }
    }

    /**
     * 生成标记点
     * @param point
     * @return
     */
    private List<WindowManager.LayoutParams> getLayOutParams(float point[][]){
        List<WindowManager.LayoutParams> rst = new ArrayList<>();
        //状态栏高度
        int result = DisplayUtils.dp2px(context,20);
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = this.getResources().getDimensionPixelSize(resourceId);
        }

        for(float[] i:point){
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            layoutParams.format = PixelFormat.RGBA_8888;
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.x = (int)i[0];
            layoutParams.y = (int)i[1]-result-layoutParams.height/2;
            rst.add(layoutParams);
        }
        return rst;
    }
    /**
     * 计算欧拉路径并模拟点击事件
     * @param edges
     */
    private void buildPath(List<List<Integer>> edges) {
        if(edges.size()==0){
            ToastUtil.show(context,"边信息缺失···");
            Logger.t(TAG).d("边信息缺失");
            return;
        }
        //验证一下是否存在欧拉路径或者欧拉回路
        //并查集判断
        HashSet<Integer> hash = new HashSet<>();
        UnionFind uf = new UnionFind(25);
        int[] edFreq = new int[25];
        for (List<Integer> i:edges){
            hash.add(i.get(0));
            hash.add(i.get(1));
            edFreq[i.get(0)]++;
            edFreq[i.get(1)]++;
            uf.union(i.get(0),i.get(1));
        }
        int res = uf.getCount();
        if (res!=25-hash.size()+1){
            ToastUtil.show(context,"并查集合并异常：不存在欧拉路径，可能是查找边出了问题，请尝试降速");
            Logger.t(TAG).d("并查集合并异常：不存在欧拉路径，可能是查找边出了问题");
            return ;
        }
        //无向图欧拉路径充要条件：
        //回路：所有的顶点度数为偶数。
        //非回路：起点终点度数为奇数
        int oddCount = 0;
        //起点
        int start = edges.get(0).get(0);
        for(int i = 0;i<edFreq.length;i++){
            if(hash.contains(i)&&edFreq[i]%2!=0){
                start=i;
                oddCount++;
            }
        }
        if(oddCount!=0&&oddCount!=2){
            ToastUtil.show(context,"起点终点异常：不存在欧拉路径，可能是查找边出了问题，请尝试降速"+oddCount);
            Logger.t(TAG).d("起点终点异常：不存在欧拉路径，可能是查找边出了问题");
           // return;
        }
        //生成欧拉路径
        List<Integer> path = getEulerCircle(edges,start);
        Logger.t(TAG).d(path);
        Logger.t(TAG).d(path.size()+ "  "+ edges.size());
        for(int i = 1;i<path.size();i++){
            Path p = new Path();
            p.moveTo(PointUtil.point[path.get(i-1)][0],PointUtil.point[path.get(i-1)][1]);
            p.lineTo(PointUtil.point[path.get(i)][0],PointUtil.point[path.get(i)][1]);
            OnePathService.mService.dispatchGestureMove(p,delayTime[speedSelect][2]);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Integer> getEulerCircle(List<List<Integer>> edges,int start) {
        List<Integer> rst = new ArrayList<>();

        Map<Integer,Stack<Integer>> map = new HashMap<>();
        for(List<Integer> edge:edges){
            map.computeIfAbsent(edge.get(0),(o1)->new Stack<>()).push(edge.get(1));
            map.computeIfAbsent(edge.get(1),(o1)->new Stack<>()).push(edge.get(0));
        }
        Set<Integer> vis = new HashSet<>();
        dfs(map,start,rst,vis);
        Collections.reverse(rst);
        return rst;
    }

    private void dfs(Map<Integer,Stack<Integer>> map, int curr,List<Integer> rst,Set<Integer> vis){
        while (map.containsKey(curr)&&!map.get(curr).isEmpty()){
             int tmp = map.get(curr).pop();
             Integer edgsCompute = Math.min(curr,tmp)*100+ Math.max(curr,tmp);
             if(vis.contains(edgsCompute)){
                 continue;
             }
             vis.add(edgsCompute);
             dfs(map,tmp,rst,vis);
        }
        rst.add(curr);
    }
    /**
     * 验证是否存在欧拉回路
     * @param path
     * @return
     */
    public boolean haveEulerPath(List<List<Integer>> path){
        //并查集判断
        HashSet<Integer> hash = new HashSet<>();
        UnionFind uf = new UnionFind(25);
        int[] edFreq = new int[25];
        for (List<Integer> i:path){
            hash.add(i.get(0));
            hash.add(i.get(1));
            edFreq[i.get(0)]++;
            edFreq[i.get(1)]++;
            uf.union(i.get(0),i.get(1));
        }
        int res = uf.getCount();
        if (res!=25-hash.size()+1){
            Logger.t(TAG).d("并查集：区域不连通");
            return false;
        }
        //无向图欧拉路径充要条件：
        //回路：所有的顶点度数为偶数。
        //非回路：起点终点度数为奇数
        int oddCount = 0;
        for(int i = 0;i<edFreq.length;i++){
            if(hash.contains(i)&&edFreq[i]%2!=0){
                oddCount++;
            }
        }
        return oddCount==2||oddCount==0;
    }



    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Logger.t(TAG).d("滑动事件");
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }
public List<List<Integer>> getEdge(){
        List<List<Integer>> lst = new ArrayList<>();
        for(int i = 0;i<PointUtil.point.length;i++){
            for(int j = i;j<PointUtil.point.length;j++){
                Path path = new Path();
                path.moveTo(PointUtil.point[i][0],PointUtil.point[i][1]);
                path.lineTo(PointUtil.point[j][0],PointUtil.point[j][1]);
                        OnePathService.mService.dispatchGestureMove(path,delayTime[speedSelect][2]);
                        try {
                            Thread.sleep(delayTime[speedSelect][0]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int color = GBData.getColor((int)PointUtil.point[j][0],(int)PointUtil.point[j][1]);
                        int g = Color.green(color);
                        int b = Color.blue(color);
                        // Logger.t(TAG).d("red:" + r + ",green:" + g + ",blue:" + b);
                        if(g==65&&b==92){
                            ArrayList<Integer> integers = new ArrayList<>();
                            integers.add(i);
                            integers.add(j);
                            lst.add(integers);
                        }
                        OnePathService.mService.dispatchGestureClick((int)PointUtil.returnPoint[0],(int)PointUtil.returnPoint[1]);
                        try {
                            Thread.sleep(delayTime[speedSelect][1]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
        }
        return lst;
}
}
