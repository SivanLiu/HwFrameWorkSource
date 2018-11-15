package com.android.server.rms.iaware.feature;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;

public class StartWindowFeature extends RFeature {
    private static final int FEATURE_MIN_VERSION = 3;
    private static final String TAG = "StartWindowFeature";
    private static final int UNKNOW_CHARGING = 0;
    private static boolean mFeature = false;
    private static final boolean mIgnoreUsbState = SystemProperties.getBoolean("persist.sys.aware.ignore.usb", false);
    private static boolean mIsCharging = false;
    private static final boolean mStartWindowEnabled = SystemProperties.getBoolean("persist.sys.aware.stwin.enable", true);
    private static boolean mUsbConnected = false;
    private BroadcastReceiver mReceiver = new StartWindowBroadcastReceiver();

    private static class StartWindowBroadcastReceiver extends BroadcastReceiver {
        private StartWindowBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                String str;
                StringBuilder stringBuilder;
                String str2;
                StringBuilder stringBuilder2;
                if ("android.hardware.usb.action.USB_STATE".equals(action)) {
                    boolean usbConnected = intent.getBooleanExtra("connected", false);
                    str = StartWindowFeature.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" receiver for  ");
                    stringBuilder.append(action);
                    stringBuilder.append("  usbConnected ");
                    stringBuilder.append(usbConnected);
                    AwareLog.d(str, stringBuilder.toString());
                    if (usbConnected != StartWindowFeature.mUsbConnected) {
                        StartWindowFeature.mUsbConnected = usbConnected;
                    }
                    StartWindowFeature.precessPowerDisconnected();
                } else if ("android.intent.action.ACTION_POWER_CONNECTED".equals(action)) {
                    str2 = StartWindowFeature.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" receiver for  ");
                    stringBuilder2.append(action);
                    AwareLog.d(str2, stringBuilder2.toString());
                    if (!StartWindowFeature.mIsCharging) {
                        StartWindowFeature.mIsCharging = true;
                    }
                } else if ("android.intent.action.ACTION_POWER_DISCONNECTED".equals(action)) {
                    str2 = StartWindowFeature.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" receiver for  ");
                    stringBuilder2.append(action);
                    AwareLog.d(str2, stringBuilder2.toString());
                    StartWindowFeature.precessPowerDisconnected();
                } else if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                    str2 = StartWindowFeature.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" receiver for  ");
                    stringBuilder3.append(action);
                    AwareLog.d(str2, stringBuilder3.toString());
                    int plugedtype = intent.getIntExtra("plugged", 0);
                    if (!(StartWindowFeature.mIsCharging || plugedtype == 0)) {
                        StartWindowFeature.mIsCharging = true;
                        str = StartWindowFeature.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("power connected battery charging plugged type: ");
                        stringBuilder.append(plugedtype);
                        AwareLog.d(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    public StartWindowFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
    }

    public boolean enable() {
        AwareLog.i(TAG, "StartWindowFeature is a iaware3.0 feature, don't allow enable!");
        return false;
    }

    public boolean enableFeatureEx(int realVersion) {
        String str;
        StringBuilder stringBuilder;
        if (realVersion < 3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("the min version of StartWindowFeature is 3, but current version is ");
            stringBuilder.append(realVersion);
            stringBuilder.append(", don't allow enable!");
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("StartWindowFeature enabled, mFeature=");
        stringBuilder.append(mFeature);
        AwareLog.i(str, stringBuilder.toString());
        if (!mFeature) {
            registerReceiver();
        }
        setEnable(true);
        updateUsbState();
        return true;
    }

    public boolean disable() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StartWindowFeature disabled, mFeature=");
        stringBuilder.append(mFeature);
        AwareLog.i(str, stringBuilder.toString());
        if (mFeature) {
            unregisterReceiver();
        }
        setEnable(false);
        updateUsbState();
        return true;
    }

    public boolean reportData(CollectData data) {
        return false;
    }

    private void registerReceiver() {
        if (!(this.mContext == null || mIgnoreUsbState)) {
            IntentFilter usbFilter = new IntentFilter();
            usbFilter.addAction("android.hardware.usb.action.USB_STATE");
            usbFilter.addAction("android.intent.action.BATTERY_CHANGED");
            usbFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            usbFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            this.mContext.registerReceiver(this.mReceiver, usbFilter);
        }
    }

    private void unregisterReceiver() {
        if (this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    private static void precessPowerDisconnected() {
        if (mIsCharging && !mUsbConnected) {
            mIsCharging = false;
        }
    }

    private void updateUsbState() {
        boolean z = false;
        if (mIgnoreUsbState) {
            mUsbConnected = false;
        } else if (this.mContext != null) {
            Intent batteryStatusIntent = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (batteryStatusIntent != null) {
                int plugedtype = batteryStatusIntent.getIntExtra("plugged", 0);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ACTION_BATTERY_CHANGED plugedtype=");
                stringBuilder.append(plugedtype);
                AwareLog.i(str, stringBuilder.toString());
                if (plugedtype != 0) {
                    z = true;
                }
                mIsCharging = z;
            }
        }
    }

    private static void setEnable(boolean enable) {
        mFeature = enable;
    }

    public static boolean isStartWindowEnable() {
        return mFeature && mStartWindowEnabled && !mIsCharging;
    }
}
