package com.yzzz.eulerpath;

import android.app.Application;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

/**
 * @author yzzz
 * @create 2021/2/13
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //控制台日志打印策略
        FormatStrategy logCatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)
                .methodCount(2)
                .methodOffset(5)
                .tag("Logger")
                .build();
        //控制日志打印适配器
        Logger.addLogAdapter(new AndroidLogAdapter(logCatStrategy));
    }
}
