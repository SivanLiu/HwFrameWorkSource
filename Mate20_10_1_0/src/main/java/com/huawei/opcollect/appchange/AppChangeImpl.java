package com.huawei.opcollect.appchange;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.ArrayList;
import java.util.List;

public class AppChangeImpl {
    private static final String COMPONENT_NAME = "comp";
    private static final int INIT_LIST_SIZE = 10;
    private static final Object LOCK = new Object();
    private static final String ONRESUME_METHOD_NAME = "onResume";
    private static final String PID_NAME = "pid";
    private static final String STATE_NAME = "state";
    private static final String TAG = "AppChangeImpl";
    private static final String UID_NAME = "uid";
    private static AppChangeImpl instance = null;
    private boolean isRegistered = false;
    private IHwActivityNotifierEx mActivityNotifierEx = new IHwActivityNotifierEx() {
        /* class com.huawei.opcollect.appchange.AppChangeImpl.AnonymousClass1 */

        public void call(Bundle extras) {
            if (extras == null) {
                OPCollectLog.i(AppChangeImpl.TAG, "call extras is null.");
                return;
            }
            int pid = extras.getInt(AppChangeImpl.PID_NAME);
            int uid = extras.getInt(AppChangeImpl.UID_NAME);
            String flag = extras.getString(AppChangeImpl.STATE_NAME);
            ComponentName componentName = (ComponentName) extras.getParcelable(AppChangeImpl.COMPONENT_NAME);
            if (AppChangeImpl.ONRESUME_METHOD_NAME.equals(flag) && componentName != null) {
                synchronized (AppChangeImpl.this.mListeners) {
                    OPCollectLog.i(AppChangeImpl.TAG, "call onAppChange.");
                    for (AppChangeListener listener : AppChangeImpl.this.mListeners) {
                        if (listener != null) {
                            listener.onAppChange(pid, uid, componentName);
                        }
                    }
                }
            }
        }
    };
    private Context mContext;
    /* access modifiers changed from: private */
    public final List<AppChangeListener> mListeners = new ArrayList(10);

    private AppChangeImpl(Context context) {
        this.mContext = context;
    }

    public static AppChangeImpl getInstance(Context context) {
        AppChangeImpl appChangeImpl;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new AppChangeImpl(context);
            }
            appChangeImpl = instance;
        }
        return appChangeImpl;
    }

    private void enable() {
        OPCollectLog.i(TAG, "enable.");
        try {
            ActivityManagerEx.registerHwActivityNotifier(this.mActivityNotifierEx, "activityLifeState");
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, " " + e.getMessage());
        }
    }

    private void disable() {
        OPCollectLog.i(TAG, "disable.");
        try {
            ActivityManagerEx.unregisterHwActivityNotifier(this.mActivityNotifierEx);
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, " " + e.getMessage());
        }
    }

    public void addListener(AppChangeListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            }
            if (!this.isRegistered) {
                enable();
                this.isRegistered = true;
            }
        }
    }

    public void removeListener(AppChangeListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
            }
            if (this.mListeners.size() == 0 && this.isRegistered) {
                disable();
                this.isRegistered = false;
            }
        }
    }
}
