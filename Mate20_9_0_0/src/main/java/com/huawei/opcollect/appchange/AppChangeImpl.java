package com.huawei.opcollect.appchange;

import android.content.ComponentName;
import android.content.Context;
import android.contentsensor.IActivityObserver.Stub;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.ArrayList;
import java.util.List;

public class AppChangeImpl extends Stub {
    private static final String TAG = "AppChangeImpl";
    private static AppChangeImpl sInstance = null;
    private Context mContext;
    private final List<AppChangeListener> mListeners = new ArrayList();
    private boolean registered = false;

    public static synchronized AppChangeImpl getInstance(Context context) {
        AppChangeImpl appChangeImpl;
        synchronized (AppChangeImpl.class) {
            if (sInstance == null) {
                sInstance = new AppChangeImpl(context);
            }
            appChangeImpl = sInstance;
        }
        return appChangeImpl;
    }

    private AppChangeImpl(Context context) {
        this.mContext = context;
    }

    private void enable() {
        OPCollectLog.i(TAG, "enable.");
        ActivityManagerEx.registerActivityObserver(this);
    }

    private void disable() {
        OPCollectLog.i(TAG, "disable.");
        ActivityManagerEx.unregisterActivityObserver(this);
    }

    public void addListener(AppChangeListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            }
            if (!this.registered) {
                enable();
                this.registered = true;
            }
        }
    }

    public void removeListener(AppChangeListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
            }
            if (this.mListeners.size() == 0 && this.registered) {
                disable();
                this.registered = false;
            }
        }
    }

    public void activityResumed(int pid, int uid, ComponentName componentName) {
        synchronized (this.mListeners) {
            if (componentName != null) {
                OPCollectLog.i(TAG, "pid: " + pid + " uid: " + uid + " pkg: " + componentName.getPackageName() + " class: " + componentName.getClassName());
            } else {
                OPCollectLog.i(TAG, "pid: " + pid + " uid: " + uid);
            }
            for (AppChangeListener listener : this.mListeners) {
                if (listener != null) {
                    listener.onAppChange(pid, uid, componentName);
                }
            }
        }
    }

    public void activityPaused(int pid, int uid, ComponentName componentName) {
    }
}
