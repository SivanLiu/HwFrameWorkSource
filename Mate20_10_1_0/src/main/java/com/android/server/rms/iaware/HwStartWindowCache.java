package com.android.server.rms.iaware;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import android.view.View;
import android.view.WindowManager;
import com.android.server.rms.algorithm.AwareUserHabit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HwStartWindowCache {
    private static final long DELAY_UPDATE_TOPN = 2000;
    private static final int MSG_CLEAR_ALL_CACHE = 2;
    private static final int MSG_UPDATE_TOPN = 1;
    private static final String TAG = "HwStartWindowCache";
    private static volatile HwStartWindowCache sInstance = null;
    private boolean mFeatureSwitch = false;
    private Handler mHandler;
    private int mLastNightMode = 0;
    private final HashMap<String, StartWindowInfo> mStartWindowCache = new HashMap<>();
    private int mTopN = 0;
    private final ArraySet<String> mTopNApk = new ArraySet<>();

    private HwStartWindowCache() {
    }

    private static class StartWindowInfo {
        protected WindowManager.LayoutParams mParams;
        protected View mView;

        public StartWindowInfo(View startView, WindowManager.LayoutParams params) {
            this.mView = startView;
            this.mParams = params;
        }
    }

    private class StartWindowCacheHandler extends Handler {
        public StartWindowCacheHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                HwStartWindowCache.this.updateTopN();
            } else if (i == 2) {
                HwStartWindowCache.this.clearAllCache();
            }
        }
    }

    public static HwStartWindowCache getInstance() {
        if (sInstance == null) {
            synchronized (HwStartWindowCache.class) {
                if (sInstance == null) {
                    sInstance = new HwStartWindowCache();
                }
            }
        }
        return sInstance;
    }

    private boolean isTopNApk(String pkg) {
        boolean isTopN;
        synchronized (this.mTopNApk) {
            isTopN = this.mTopNApk.contains(pkg);
        }
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessageDelayed(1, DELAY_UPDATE_TOPN);
        }
        return isTopN;
    }

    /* access modifiers changed from: private */
    public void updateTopN() {
        List<String> topNPkg;
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit != null && (topNPkg = habit.getMostFrequentUsedApp(this.mTopN, 0)) != null) {
            synchronized (this.mTopNApk) {
                this.mTopNApk.clear();
                this.mTopNApk.addAll(topNPkg);
            }
            removeColdCache();
        }
    }

    private void removeColdCache() {
        synchronized (this.mStartWindowCache) {
            Iterator<Map.Entry<String, StartWindowInfo>> iter = this.mStartWindowCache.entrySet().iterator();
            while (iter.hasNext()) {
                if (!this.mTopNApk.contains(iter.next().getKey())) {
                    iter.remove();
                }
            }
            AwareLog.d(TAG, "cache updated = " + this.mStartWindowCache);
        }
    }

    public View tryAddViewFromCache(String packageName, IBinder appToken, Configuration config) {
        StartWindowInfo info;
        if (!this.mFeatureSwitch || config == null) {
            return null;
        }
        if (this.mLastNightMode != config.uiMode) {
            clearAllCache();
            this.mLastNightMode = config.uiMode;
            return null;
        }
        synchronized (this.mStartWindowCache) {
            info = this.mStartWindowCache.get(packageName);
        }
        if (info == null) {
            return null;
        }
        Context context = info.mView.getContext();
        if (context == null) {
            AwareLog.d(TAG, "context == null, do not addView for " + packageName);
            return null;
        }
        WindowManager wm = (WindowManager) context.getSystemService(WindowManager.class);
        if (wm == null) {
            AwareLog.d(TAG, "window manager == null, do not addView for " + packageName);
            return null;
        }
        info.mParams.token = appToken;
        wm.addView(info.mView, info.mParams);
        return info.mView;
    }

    public void putViewToCache(String packageName, View startView, WindowManager.LayoutParams params) {
        if (this.mFeatureSwitch && startView != null && params != null && isTopNApk(packageName)) {
            synchronized (this.mStartWindowCache) {
                this.mStartWindowCache.put(packageName, new StartWindowInfo(startView, params));
            }
        }
    }

    public void clearCacheWhenUninstall(String packageName) {
        if (this.mFeatureSwitch && packageName != null) {
            synchronized (this.mStartWindowCache) {
                this.mStartWindowCache.remove(packageName);
            }
            AwareLog.d(TAG, "remove pkg from cache = " + packageName);
        }
    }

    public void init(int topN) {
        if (topN > 0) {
            this.mTopN = topN;
            this.mFeatureSwitch = true;
            AwareLog.d(TAG, "Feature enabled ! topN = " + topN);
        }
    }

    public void deinit() {
        this.mTopN = 0;
        this.mFeatureSwitch = false;
        AwareLog.d(TAG, "Feature disabled !");
    }

    public void setHandler(Handler handler) {
        if (handler == null) {
            AwareLog.w(TAG, "handler from rms is null , cache not working!");
        } else {
            this.mHandler = new StartWindowCacheHandler(handler.getLooper());
        }
    }

    public void notifyMemCritical() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessage(2);
            AwareLog.d(TAG, "mem critical abandon all cache !");
        }
    }

    public void notifyResolutionChange() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessage(2);
            AwareLog.d(TAG, "resolution change abandon all cache !");
        }
    }

    /* access modifiers changed from: private */
    public void clearAllCache() {
        synchronized (this.mStartWindowCache) {
            this.mStartWindowCache.clear();
        }
    }
}
