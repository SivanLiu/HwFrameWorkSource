package com.android.server.gesture;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import com.android.server.LocalServices;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.HwGameDockGesture;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.systemui.shared.system.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class DeviceStateController {
    private static final String TAG = "DeviceStateController";
    private static DeviceStateController sInstance;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentUserId;
    /* access modifiers changed from: private */
    public final Uri mDeviceProvisionedUri;
    private final ArrayList<DeviceChangedListener> mListeners = new ArrayList<>();
    private Looper mLooper;
    private WindowManagerPolicy mPolicy;
    private final BroadcastReceiver mPreferChangedReceiver = new BroadcastReceiver() {
        /* class com.android.server.gesture.DeviceStateController.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            boolean isPrefer;
            if (context != null && intent != null) {
                if (!PackageManagerWrapper.ACTION_PREFERRED_ACTIVITY_CHANGED.equals(intent.getAction())) {
                    isPrefer = false;
                } else if (intent.getIntExtra("android.intent.extra.user_handle", -10000) != -10000) {
                    isPrefer = true;
                } else {
                    return;
                }
                DeviceStateController.this.notifyPreferredActivityChanged(isPrefer);
            }
        }
    };
    private SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public final Uri mUserSetupUri;
    private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
        /* class com.android.server.gesture.DeviceStateController.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (context != null && intent != null) {
                DeviceStateController deviceStateController = DeviceStateController.this;
                int unused = deviceStateController.mCurrentUserId = deviceStateController.getCurrentUser();
                Log.i(DeviceStateController.TAG, "User switched receiver, userId=" + DeviceStateController.this.mCurrentUserId);
                DeviceStateController deviceStateController2 = DeviceStateController.this;
                deviceStateController2.onUserSwitched(deviceStateController2.mCurrentUserId);
            }
        }
    };

    private DeviceStateController(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
        this.mUserSetupUri = Settings.Secure.getUriFor("user_setup_complete");
        this.mCurrentUserId = getCurrentUser();
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    public static DeviceStateController getInstance(Context context) {
        DeviceStateController deviceStateController;
        synchronized (DeviceStateController.class) {
            if (sInstance == null) {
                sInstance = new DeviceStateController(context);
            }
            deviceStateController = sInstance;
        }
        return deviceStateController;
    }

    public boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContentResolver, "device_provisioned", 0) != 0;
    }

    public boolean isCurrentUserSetup() {
        return Settings.Secure.getIntForUser(this.mContentResolver, "user_setup_complete", 0, -2) != 0;
    }

    public boolean isOOBEActivityEnabled() {
        if (this.mContext == null) {
            Log.d(TAG, "mContext is null.");
            return false;
        }
        List<ResolveInfo> resolveInfo = this.mContext.getPackageManager().queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT").setPackage(GestureNavConst.OOBE_MAIN_PACKAGE), 0);
        if (resolveInfo == null || resolveInfo.size() <= 0) {
            return false;
        }
        return true;
    }

    public boolean isSetupWizardEnabled() {
        if (this.mContext == null) {
            Log.d(TAG, "mContext is null.");
            return false;
        }
        List<ResolveInfo> resolveInfo = this.mContext.getPackageManager().queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT").setPackage(GestureNavConst.SETUP_WIZARD_PACKAGE), 0);
        if (resolveInfo == null || resolveInfo.size() <= 0) {
            return false;
        }
        return true;
    }

    public int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public boolean isKeyguardOccluded() {
        return this.mPolicy.isKeyguardOccluded();
    }

    public boolean isKeyguardShowingOrOccluded() {
        return this.mPolicy.isKeyguardShowingOrOccluded();
    }

    public boolean isKeyguardLocked() {
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public WindowManagerPolicy.WindowState getNavigationBar() {
        HwPhoneWindowManager policy = this.mPolicy;
        if (policy instanceof HwPhoneWindowManager) {
            return policy.getNavigationBar();
        }
        return null;
    }

    public WindowManagerPolicy.WindowState getFocusWindow() {
        HwPhoneWindowManager policy = this.mPolicy;
        if (policy instanceof HwPhoneWindowManager) {
            return policy.getFocusedWindow();
        }
        return null;
    }

    public WindowManagerPolicy.WindowState getInputMethodWindow() {
        HwPhoneWindowManager policy = this.mPolicy;
        if (policy instanceof HwPhoneWindowManager) {
            return policy.getInputMethodWindow();
        }
        return null;
    }

    public String getFocusWindowName() {
        WindowManagerPolicy.WindowState focusWindowState = getFocusWindow();
        if (focusWindowState == null || focusWindowState.getAttrs() == null) {
            return null;
        }
        return focusWindowState.getAttrs().getTitle().toString();
    }

    public String getFocusPackageName() {
        WindowManagerPolicy.WindowState focusWindowState = getFocusWindow();
        if (focusWindowState == null || focusWindowState.getAttrs() == null) {
            return null;
        }
        return focusWindowState.getAttrs().packageName;
    }

    public int getCurrentRotation() {
        int rotation = GestureNavConst.DEFAULT_ROTATION;
        IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        if (windowManager == null) {
            return rotation;
        }
        try {
            return windowManager.getDefaultDisplayRotation();
        } catch (RemoteException e) {
            Log.w(TAG, "Error occured when get rotation", e);
            return rotation;
        }
    }

    public int getShrinkIdByDockPosition() {
        HwGameDockGesture gameDock = (HwGameDockGesture) LocalServices.getService(HwGameDockGesture.class);
        if (gameDock != null) {
            return gameDock.getShrinkIdByDockPosition();
        }
        return 0;
    }

    private int getSystemUiFlag() {
        HwPhoneWindowManager policy = this.mPolicy;
        if (policy instanceof HwPhoneWindowManager) {
            return policy.getLastSystemUiFlags();
        }
        return 0;
    }

    public boolean isWindowBackDisabled() {
        return (getSystemUiFlag() & 4194304) != 0;
    }

    public boolean isWindowHomeDisabled() {
        return (getSystemUiFlag() & HighBitsCompModeID.MODE_EYE_PROTECT) != 0;
    }

    public boolean isWindowRecentDisabled() {
        return (getSystemUiFlag() & 16777216) != 0;
    }

    public String getCurrentHomeActivity() {
        return getCurrentHomeActivity(this.mCurrentUserId);
    }

    public String getCurrentHomeActivity(int userId) {
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveActivityAsUser(getHomeIntent(), 786432, userId);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name;
    }

    public void onConfigurationChanged() {
        notifyConfigurationChanged();
    }

    public void addCallback(DeviceChangedListener listener) {
        this.mListeners.add(listener);
        if (this.mListeners.size() == 1) {
            startListening();
        }
        listener.onDeviceProvisionedChanged(isDeviceProvisioned());
        listener.onUserSetupChanged(isCurrentUserSetup());
    }

    public void removeCallback(DeviceChangedListener listener) {
        this.mListeners.remove(listener);
        if (this.mListeners.size() == 0) {
            stopListening();
        }
    }

    private void startListening() {
        this.mCurrentUserId = getCurrentUser();
        this.mLooper = Looper.myLooper();
        Log.i(TAG, "start listening, userId:" + this.mCurrentUserId);
        Handler handler = new Handler(this.mLooper);
        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
        this.mContext.registerReceiverAsUser(this.mUserSwitchedReceiver, UserHandle.ALL, filter, null, handler);
        IntentFilter filter2 = new IntentFilter(PackageManagerWrapper.ACTION_PREFERRED_ACTIVITY_CHANGED);
        filter2.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mPreferChangedReceiver, UserHandle.ALL, filter2, null, handler);
        IntentFilter packageFilter = new IntentFilter("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPreferChangedReceiver, UserHandle.ALL, packageFilter, null, handler);
        registerObserver(this.mCurrentUserId);
    }

    private void stopListening() {
        this.mContext.unregisterReceiver(this.mUserSwitchedReceiver);
        this.mContext.unregisterReceiver(this.mPreferChangedReceiver);
        unregisterObserver();
        Log.i(TAG, "stop listening.");
    }

    private void registerObserver(int userId) {
        if (this.mSettingsObserver == null) {
            Looper looper = this.mLooper;
            if (looper == null) {
                looper = Looper.myLooper();
            }
            this.mSettingsObserver = new SettingsObserver(new Handler(looper));
            this.mContentResolver.registerContentObserver(this.mDeviceProvisionedUri, true, this.mSettingsObserver, 0);
            this.mContentResolver.registerContentObserver(this.mUserSetupUri, true, this.mSettingsObserver, userId);
        }
    }

    private void unregisterObserver() {
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver != null) {
            this.mContentResolver.unregisterContentObserver(settingsObserver);
            this.mSettingsObserver = null;
        }
    }

    /* access modifiers changed from: private */
    public void onUserSwitched(int newUserId) {
        unregisterObserver();
        registerObserver(newUserId);
        notifyUserChanged(newUserId);
    }

    private void notifyUserChanged(int newUserId) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onUserSwitched(newUserId);
        }
    }

    /* access modifiers changed from: private */
    public void notifyProvisionedChanged(boolean isProvisioned) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onDeviceProvisionedChanged(isProvisioned);
        }
    }

    /* access modifiers changed from: private */
    public void notifySetupChanged(boolean isSetup) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onUserSetupChanged(isSetup);
        }
    }

    private void notifyConfigurationChanged() {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onConfigurationChanged();
        }
    }

    /* access modifiers changed from: private */
    public void notifyPreferredActivityChanged(boolean isPrefer) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onPreferredActivityChanged(isPrefer);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri, int userId) {
            if (DeviceStateController.this.mDeviceProvisionedUri.equals(uri)) {
                DeviceStateController deviceStateController = DeviceStateController.this;
                deviceStateController.notifyProvisionedChanged(deviceStateController.isDeviceProvisioned());
            } else if (DeviceStateController.this.mUserSetupUri.equals(uri)) {
                DeviceStateController deviceStateController2 = DeviceStateController.this;
                deviceStateController2.notifySetupChanged(deviceStateController2.isCurrentUserSetup());
            }
        }
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    public static abstract class DeviceChangedListener {
        /* access modifiers changed from: package-private */
        public void onDeviceProvisionedChanged(boolean isProvisioned) {
        }

        /* access modifiers changed from: package-private */
        public void onUserSwitched(int newUserId) {
        }

        /* access modifiers changed from: package-private */
        public void onUserSetupChanged(boolean isSetup) {
        }

        /* access modifiers changed from: package-private */
        public void onConfigurationChanged() {
        }

        /* access modifiers changed from: package-private */
        public void onPreferredActivityChanged(boolean isPrefer) {
        }
    }
}
