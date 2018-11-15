package com.android.server.gesture;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.gesture.DeviceStateController.DeviceChangedListener;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.IOverviewProxy.Stub;
import java.io.PrintWriter;

public class OverviewProxyService {
    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final long BACKOFF_MILLIS = 5000;
    private static final long BACKOFF_MILLIS_MAX = 43200000;
    private static final long DEFERRED_CALLBACK_MILLIS = 5000;
    private final BroadcastReceiver mBaseBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(GestureNavConst.TAG_GESTURE_OPS, "User unlocked.");
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    private int mConnectionBackoffAttempts;
    private final Runnable mConnectionRunnable = new -$$Lambda$OverviewProxyService$KOWDBHP6658WCoqHveYatMN2KJc(this);
    private final Context mContext;
    private final Runnable mDeferredConnectionCallback = new -$$Lambda$OverviewProxyService$uA1mapK1w1fvQoRhlNlsAaLeiaM(this);
    private final DeviceChangedListener mDeviceChangedCallback = new DeviceChangedListener() {
        public void onUserSetupChanged(boolean setup) {
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_OPS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUserSetupChanged isCurrentUserSetup=");
                stringBuilder.append(setup);
                Log.d(str, stringBuilder.toString());
            }
            if (OverviewProxyService.this.updateHomeWindow()) {
                OverviewProxyService.this.updateEnabledState();
            }
            if (setup) {
                OverviewProxyService.this.startConnectionToCurrentUser();
            }
        }

        public void onUserSwitched(int newUserId) {
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_OPS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUserSwitched newUserId=");
                stringBuilder.append(newUserId);
                Log.d(str, stringBuilder.toString());
            }
            if (OverviewProxyService.this.updateHomeWindow()) {
                OverviewProxyService.this.updateEnabledState();
            }
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.startConnectionToCurrentUser();
        }

        public void onPreferredActivityChanged(boolean isPrefer) {
            if (OverviewProxyService.this.updateHomeWindow()) {
                OverviewProxyService.this.updateEnabledState();
                OverviewProxyService.this.startConnectionToCurrentUser();
            }
        }
    };
    private final DeviceStateController mDeviceStateController;
    private final Runnable mDisConnectionRunnable = new -$$Lambda$OverviewProxyService$Oun8DXyLo1XpuLvrpUdaj8mbO7Y(this);
    private final Handler mHandler;
    private String mHomeWindow;
    private boolean mIsEnabled;
    private final BroadcastReceiver mLauncherStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Launcher state changed, intent=");
            stringBuilder.append(intent);
            Log.i(str, stringBuilder.toString());
            OverviewProxyService.this.updateEnabledState();
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    private IOverviewProxy mOverviewProxy;
    private final ServiceConnection mOverviewServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            OverviewProxyService.this.mHandler.removeCallbacks(OverviewProxyService.this.mDeferredConnectionCallback);
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.mOverviewProxy = Stub.asInterface(service);
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Launcher service connected, mOverviewProxy=");
            stringBuilder.append(OverviewProxyService.this.mOverviewProxy);
            Log.i(str, stringBuilder.toString());
            try {
                service.linkToDeath(OverviewProxyService.this.mOverviewServiceDeathRcpt, 0);
            } catch (RemoteException e) {
                Log.e(GestureNavConst.TAG_GESTURE_OPS, "Lost connection to launcher service", e);
            }
        }

        public void onNullBinding(ComponentName name) {
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Null binding of '");
            stringBuilder.append(name);
            stringBuilder.append("', try reconnecting");
            Log.w(str, stringBuilder.toString());
            OverviewProxyService.this.internalConnectToCurrentUser();
        }

        public void onBindingDied(ComponentName name) {
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Binding died of '");
            stringBuilder.append(name);
            stringBuilder.append("', try reconnecting");
            Log.w(str, stringBuilder.toString());
            OverviewProxyService.this.internalConnectToCurrentUser();
        }

        public void onServiceDisconnected(ComponentName name) {
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Launcher service disconnected, name=");
            stringBuilder.append(name);
            Log.i(str, stringBuilder.toString());
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
        }
    };
    private final DeathRecipient mOverviewServiceDeathRcpt = new -$$Lambda$fa_E-AinJ_OblsY1v92TO_iW2GQ(this);
    private final Intent mQuickStepIntent;
    private final ComponentName mRecentsComponentName;

    public static /* synthetic */ void lambda$new$0(OverviewProxyService overviewProxyService) {
        Log.w(GestureNavConst.TAG_GESTURE_OPS, "Binder supposed established connection but actual connection to service timed out, trying again");
        overviewProxyService.internalConnectToCurrentUser();
    }

    public OverviewProxyService(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mRecentsComponentName = new ComponentName(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, GestureNavConst.DEFAULT_QUICKSTEP_CLASS);
        this.mQuickStepIntent = new Intent(ACTION_QUICKSTEP).setPackage(this.mRecentsComponentName.getPackageName());
        this.mDeviceStateController = DeviceStateController.getInstance(this.mContext);
    }

    public void notifyStart() {
        this.mConnectionBackoffAttempts = 0;
        this.mHomeWindow = this.mDeviceStateController.getCurrentHomeActivity();
        this.mDeviceStateController.addCallback(this.mDeviceChangedCallback);
        String str = GestureNavConst.TAG_GESTURE_OPS;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mQuickStepIntent=");
        stringBuilder.append(this.mQuickStepIntent);
        Log.i(str, stringBuilder.toString());
        updateEnabledState();
        startConnectionToCurrentUser();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart(this.mRecentsComponentName.getPackageName(), 0);
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, filter, null, this.mHandler);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, filter2, null, this.mHandler);
    }

    public void notifyStop() {
        this.mContext.unregisterReceiver(this.mLauncherStateChangedReceiver);
        this.mContext.unregisterReceiver(this.mBaseBroadcastReceiver);
        this.mDeviceStateController.removeCallback(this.mDeviceChangedCallback);
        stopConnectionToCurrentUser();
    }

    public void startConnectionToCurrentUser() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mConnectionRunnable);
        } else {
            internalConnectToCurrentUser();
        }
    }

    public void stopConnectionToCurrentUser() {
        this.mHandler.removeCallbacks(this.mConnectionRunnable);
        this.mHandler.removeCallbacks(this.mDeferredConnectionCallback);
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mDisConnectionRunnable);
        } else {
            disconnectFromLauncherService();
        }
    }

    private void internalConnectToCurrentUser() {
        disconnectFromLauncherService();
        if (this.mDeviceStateController.isCurrentUserSetup() && isEnabled()) {
            this.mHandler.removeCallbacks(this.mConnectionRunnable);
            Intent launcherServiceIntent = new Intent(ACTION_QUICKSTEP).setPackage(this.mRecentsComponentName.getPackageName());
            boolean bound = false;
            try {
                bound = this.mContext.bindServiceAsUser(launcherServiceIntent, this.mOverviewServiceConnection, 1, UserHandle.of(this.mDeviceStateController.getCurrentUser()));
            } catch (SecurityException e) {
                Log.e(GestureNavConst.TAG_GESTURE_OPS, "Unable to bind because of security error", e);
            } catch (Exception e2) {
                Log.e(GestureNavConst.TAG_GESTURE_OPS, "bind service fail", e2);
            }
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("internalConnectToCurrentUser, bound=");
            stringBuilder.append(bound);
            stringBuilder.append(", launcherServiceIntent=");
            stringBuilder.append(launcherServiceIntent);
            Log.i(str, stringBuilder.toString());
            if (bound) {
                this.mHandler.postDelayed(this.mDeferredConnectionCallback, 5000);
            } else {
                long timeoutMs = (long) Math.scalb(5000.0f, this.mConnectionBackoffAttempts);
                if (timeoutMs > BACKOFF_MILLIS_MAX) {
                    timeoutMs = BACKOFF_MILLIS_MAX;
                }
                this.mHandler.postDelayed(this.mConnectionRunnable, timeoutMs);
                this.mConnectionBackoffAttempts++;
                String str2 = GestureNavConst.TAG_GESTURE_OPS;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to connect on attempt ");
                stringBuilder2.append(this.mConnectionBackoffAttempts);
                stringBuilder2.append(" will try again in ");
                stringBuilder2.append(timeoutMs);
                stringBuilder2.append("ms");
                Log.w(str2, stringBuilder2.toString());
            }
            return;
        }
        String str3 = GestureNavConst.TAG_GESTURE_OPS;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Not setup or not enable, isEnabled=");
        stringBuilder3.append(isEnabled());
        Log.i(str3, stringBuilder3.toString());
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public IOverviewProxy getProxy() {
        return this.mOverviewProxy;
    }

    private void disconnectFromLauncherService() {
        String str;
        StringBuilder stringBuilder;
        if (this.mOverviewProxy != null) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_OPS, "disconnectFromLauncherService start");
            }
            try {
                this.mOverviewProxy.asBinder().unlinkToDeath(this.mOverviewServiceDeathRcpt, 0);
            } catch (Exception e) {
                str = GestureNavConst.TAG_GESTURE_OPS;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unlinkToDeath fail, mIsEnabled=");
                stringBuilder.append(this.mIsEnabled);
                Log.e(str, stringBuilder.toString(), e);
            }
            boolean unbind = true;
            try {
                this.mContext.unbindService(this.mOverviewServiceConnection);
            } catch (Exception e2) {
                unbind = false;
                Log.e(GestureNavConst.TAG_GESTURE_OPS, "unbind service fail", e2);
            }
            this.mOverviewProxy = null;
            str = GestureNavConst.TAG_GESTURE_OPS;
            stringBuilder = new StringBuilder();
            stringBuilder.append("unbind service:");
            stringBuilder.append(unbind);
            Log.i(str, stringBuilder.toString());
        }
    }

    private void updateEnabledState() {
        this.mIsEnabled = this.mContext.getPackageManager().resolveServiceAsUser(this.mQuickStepIntent, 786432, ActivityManager.getCurrentUser()) != null;
        String str = GestureNavConst.TAG_GESTURE_OPS;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsEnabled=");
        stringBuilder.append(this.mIsEnabled);
        Log.i(str, stringBuilder.toString());
    }

    private boolean updateHomeWindow() {
        String homeWindow = this.mDeviceStateController.getCurrentHomeActivity();
        if (homeWindow == null || homeWindow.equals(this.mHomeWindow)) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_OPS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Home changed, newHome=");
            stringBuilder.append(homeWindow);
            stringBuilder.append(", oldHome=");
            stringBuilder.append(this.mHomeWindow);
            Log.i(str, stringBuilder.toString());
        }
        this.mHomeWindow = homeWindow;
        return true;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mQuickStepIntent=");
        stringBuilder.append(this.mQuickStepIntent);
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mConnectionBackoffAttempts=");
        stringBuilder.append(this.mConnectionBackoffAttempts);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsEnabled=");
        stringBuilder.append(this.mIsEnabled);
        pw.print(stringBuilder.toString());
        pw.println();
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mHomeWindow=");
        stringBuilder.append(this.mHomeWindow);
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mOverviewProxy=");
        stringBuilder.append(this.mOverviewProxy);
        pw.println(stringBuilder.toString());
    }
}
