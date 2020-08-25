package com.android.server.rms.iaware;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IUserSwitchObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AwareCallback {
    private static final Set<String> ACTIVITY_NOTIFIER_TYPES = new HashSet<String>() {
        /* class com.android.server.rms.iaware.AwareCallback.AnonymousClass1 */

        {
            add(SceneRecogFeature.REASON_INFO);
            add("appSwitch");
        }
    };
    private static final Object LOCK = new Object();
    private static final int NOTIFY_ACTIVITY = 100;
    private static final int NOTIFY_FOREGROUND = 201;
    private static final int NOTIFY_PROC = 202;
    private static final int NOTIFY_USER = 301;
    private static final String TAG = "AwareCb";
    private static AwareCallback sInstance;
    private final Object mActLock = new Object();
    /* access modifiers changed from: private */
    public AwareCbHandler mCbHandler = new AwareCbHandler();
    private ArrayMap<IHwActivityNotifierEx, String> mInActNotifier = new ArrayMap<>();
    /* access modifiers changed from: private */
    public IProcessObserver mInProcObserver;
    private IUserSwitchObserver mInUserObserver;
    private ArrayMap<IHwActivityNotifierEx, String> mOutActNotifier = new ArrayMap<>();
    private ArraySet<IProcessObserver> mOutProcObservers = new ArraySet<>();
    private ArraySet<IUserSwitchObserver> mOutUserObservers = new ArraySet<>();
    /* access modifiers changed from: private */
    public final Object mProcLock = new Object();
    private final Object mUserLock = new Object();

    private AwareCallback() {
    }

    public static AwareCallback getInstance() {
        AwareCallback awareCallback;
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = new AwareCallback();
            }
            awareCallback = sInstance;
        }
        return awareCallback;
    }

    public void registerActivityNotifier(IHwActivityNotifierEx notifier, String reason) {
        synchronized (this.mActLock) {
            if (!this.mOutActNotifier.containsValue(reason)) {
                AwareLog.d(TAG, "registerActivityNotifier=" + reason);
                ActivityNotifierCallBack inNotifier = new ActivityNotifierCallBack();
                this.mInActNotifier.put(inNotifier, reason);
                ActivityManagerEx.registerHwActivityNotifier(inNotifier, reason);
            }
            this.mOutActNotifier.put(notifier, reason);
        }
    }

    public void unregisterActivityNotifier(IHwActivityNotifierEx notifier, String reason) {
        AwareLog.d(TAG, "unregisterActivityNotifier =" + reason);
        synchronized (this.mActLock) {
            this.mOutActNotifier.remove(notifier);
            if (!this.mOutActNotifier.containsValue(reason)) {
                int index = this.mInActNotifier.indexOfValue(reason);
                if (index >= 0) {
                    ActivityManagerEx.unregisterHwActivityNotifier(this.mInActNotifier.keyAt(index));
                    this.mInActNotifier.removeAt(index);
                }
            }
        }
    }

    public void registerProcessObserver(IProcessObserver observer) {
        synchronized (this.mProcLock) {
            if (this.mInProcObserver == null) {
                this.mInProcObserver = new ProcessObserver();
                try {
                    AwareLog.d(TAG, "registerProcessObserver");
                    ActivityManagerNative.getDefault().registerProcessObserver(this.mInProcObserver);
                } catch (RemoteException e) {
                    AwareLog.w(TAG, "register proc observer failed");
                }
            }
            this.mOutProcObservers.add(observer);
        }
    }

    public void unregisterProcessObserver(IProcessObserver observer) {
        AwareLog.d(TAG, "unregisterProcessObserver =" + observer);
        synchronized (this.mProcLock) {
            this.mOutProcObservers.remove(observer);
            if (this.mOutProcObservers.isEmpty()) {
                try {
                    ActivityManagerNative.getDefault().unregisterProcessObserver(this.mInProcObserver);
                    this.mInProcObserver = null;
                } catch (RemoteException e) {
                    AwareLog.w(TAG, "unregister proc observer failed");
                }
            }
        }
    }

    public void registerUserSwitchObserver(IUserSwitchObserver observer) {
        synchronized (this.mUserLock) {
            if (this.mInUserObserver == null) {
                this.mInUserObserver = new UserSwitchObserver();
                try {
                    AwareLog.d(TAG, "registerUserSwitchObserver");
                    ActivityManagerNative.getDefault().registerUserSwitchObserver(this.mInUserObserver, "aware");
                } catch (RemoteException e) {
                    AwareLog.w(TAG, "register userswitch observer failed");
                }
            }
            this.mOutUserObservers.add(observer);
        }
    }

    public void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        AwareLog.d(TAG, "unregisterUserSwitchObserver =" + observer);
        synchronized (this.mUserLock) {
            this.mOutUserObservers.remove(observer);
            if (this.mOutUserObservers.isEmpty()) {
                try {
                    ActivityManagerNative.getDefault().unregisterUserSwitchObserver(this.mInUserObserver);
                    this.mInUserObserver = null;
                } catch (RemoteException e) {
                    AwareLog.w(TAG, "unregister userswitch observer failed");
                }
            }
        }
    }

    private class ActivityNotifierCallBack extends IHwActivityNotifierEx {
        private ActivityNotifierCallBack() {
        }

        public void call(Bundle extras) {
            if (extras != null) {
                String reason = extras.getString("android.intent.extra.REASON");
                AwareLog.d(AwareCallback.TAG, "ActivityNotifierCallBack =" + reason);
                if (!TextUtils.isEmpty(reason)) {
                    Message msg = Message.obtain(AwareCallback.this.mCbHandler, 100);
                    msg.obj = extras;
                    AwareCallback.this.mCbHandler.sendMessage(msg);
                }
            }
        }
    }

    private class ProcessObserver extends IProcessObserver.Stub {
        private ProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            synchronized (AwareCallback.this.mProcLock) {
                if (AwareCallback.this.mInProcObserver != null) {
                    AwareLog.d(AwareCallback.TAG, "onForegroundActivitiesChanged =" + pid);
                    Message msg = Message.obtain(AwareCallback.this.mCbHandler, 201);
                    msg.getData().putBoolean(MemoryConstant.MEM_REPAIR_CONSTANT_FG, foregroundActivities);
                    msg.arg1 = pid;
                    msg.arg2 = uid;
                    AwareCallback.this.mCbHandler.sendMessage(msg);
                }
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            synchronized (AwareCallback.this.mProcLock) {
                if (AwareCallback.this.mInProcObserver != null) {
                    AwareLog.d(AwareCallback.TAG, "onProcessDied =" + pid);
                    Message msg = Message.obtain(AwareCallback.this.mCbHandler, 202);
                    msg.arg1 = pid;
                    msg.arg2 = uid;
                    AwareCallback.this.mCbHandler.sendMessage(msg);
                }
            }
        }
    }

    private class UserSwitchObserver extends IUserSwitchObserver.Stub {
        private UserSwitchObserver() {
        }

        public void onUserSwitching(int newUserId, IRemoteCallback reply) {
            if (reply != null) {
                try {
                    reply.sendResult((Bundle) null);
                } catch (RemoteException e) {
                    AwareLog.e(AwareCallback.TAG, "RemoteException onUserSwitching");
                }
            }
        }

        public void onUserSwitchComplete(int newUserId) throws RemoteException {
            AwareLog.d(AwareCallback.TAG, "onUserSwitchComplete =" + newUserId);
            Message msg = Message.obtain(AwareCallback.this.mCbHandler, 301);
            msg.arg1 = newUserId;
            AwareCallback.this.mCbHandler.sendMessage(msg);
        }

        public void onForegroundProfileSwitch(int newProfileId) {
        }

        public void onLockedBootComplete(int newUserId) {
        }
    }

    /* access modifiers changed from: private */
    public class AwareCbHandler extends Handler {
        private AwareCbHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg != null) {
                int i = msg.what;
                if (i == 100) {
                    AwareCallback.this.notifyActivityMsg(msg);
                } else if (i == 301) {
                    AwareCallback.this.notifyUserMsg(msg);
                } else if (i == 201) {
                    AwareCallback.this.notifyForegroundMsg(msg);
                } else if (i != 202) {
                    AwareLog.w(AwareCallback.TAG, "AwareCb, default msg.what is " + msg.what);
                } else {
                    AwareCallback.this.notifyProcMsg(msg);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyActivityMsg(Message msg) {
        if (msg.obj instanceof Bundle) {
            Bundle bundle = (Bundle) msg.obj;
            String reason = bundle.getString("android.intent.extra.REASON");
            if (!TextUtils.isEmpty(reason)) {
                synchronized (this.mActLock) {
                    if (this.mInActNotifier.containsValue(reason)) {
                        for (Map.Entry<IHwActivityNotifierEx, String> entry : this.mOutActNotifier.entrySet()) {
                            if (reason.equals(entry.getValue())) {
                                entry.getKey().call(bundle);
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyForegroundMsg(Message msg) {
        boolean foreground = msg.getData().getBoolean(MemoryConstant.MEM_REPAIR_CONSTANT_FG);
        int pid = msg.arg1;
        int uid = msg.arg2;
        try {
            synchronized (this.mProcLock) {
                Iterator<IProcessObserver> it = this.mOutProcObservers.iterator();
                while (it.hasNext()) {
                    it.next().onForegroundActivitiesChanged(pid, uid, foreground);
                }
            }
        } catch (RemoteException e) {
            AwareLog.i(TAG, "NOTIFY_FOREGROUND =" + pid + " exception");
        }
    }

    /* access modifiers changed from: private */
    public void notifyProcMsg(Message msg) {
        int pid = msg.arg1;
        int uid = msg.arg2;
        try {
            synchronized (this.mProcLock) {
                Iterator<IProcessObserver> it = this.mOutProcObservers.iterator();
                while (it.hasNext()) {
                    it.next().onProcessDied(pid, uid);
                }
            }
        } catch (RemoteException e) {
            AwareLog.w(TAG, "NOTIFY_PROC =" + pid + " exception");
        }
    }

    /* access modifiers changed from: private */
    public void notifyUserMsg(Message msg) {
        int newUserId = msg.arg1;
        try {
            synchronized (this.mUserLock) {
                Iterator<IUserSwitchObserver> it = this.mOutUserObservers.iterator();
                while (it.hasNext()) {
                    it.next().onUserSwitchComplete(newUserId);
                }
            }
        } catch (RemoteException e) {
            AwareLog.w(TAG, "NOTIFY_USER =" + newUserId + " exception");
        }
    }
}
