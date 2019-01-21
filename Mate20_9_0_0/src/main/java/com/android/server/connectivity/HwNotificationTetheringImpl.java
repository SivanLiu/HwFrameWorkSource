package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import java.util.List;

public class HwNotificationTetheringImpl implements HwNotificationTethering {
    private static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    private static final String ACTION_WIFI_AP_STA_JOIN = "android.net.wifi.WIFI_AP_STA_JOIN";
    private static final String ACTION_WIFI_AP_STA_LEAVE = "android.net.wifi.WIFI_AP_STA_LEAVE";
    private static final boolean DBG = HWFLOW;
    private static final String EXTRA_BLUETOOTH_PAN_PROFILE_CONNECTEED_SIZE = "bluetooth_pan_profile_connected_size";
    private static final String EXTRA_STA_COUNT = "staCount";
    private static final String EXTRA_WIFI_REPEATER_CLIENTS_SIZE = "wifi_repeater_clients_size";
    protected static final boolean HWFLOW;
    private static final String IDLE_WIFI_TETHER_ALARM_TAG = "HwCustTethering IDLE_WIFI_TETHER";
    private static final int IDLE_WIFI_TETHER_DELAY = 600000;
    private static final int NOTIFICATION_TYPE_BLUETOOTH = 2;
    private static final int NOTIFICATION_TYPE_MULTIPLE = 4;
    private static final int NOTIFICATION_TYPE_NONE = -1;
    private static final int NOTIFICATION_TYPE_P2P = 3;
    private static final int NOTIFICATION_TYPE_STOP_AP = 5;
    private static final int NOTIFICATION_TYPE_USB = 1;
    private static final int NOTIFICATION_TYPE_WIFI = 0;
    private static final int POWER_OFF = 0;
    private static final String START_TETHER_ACTION = "com.huawei.server.connectivity.action.START_TETHER";
    private static final String START_TETHER_PERMISSION = "com.android.server.connectivity.permission.START_TETHERING";
    private static final String TAG = "HwCustTethering";
    private static final String WIFI_REPEATER_CLIENTS_CHANGED_ACTION = "com.huawei.wifi.action.WIFI_REPEATER_CLIENTS_CHANGED";
    private static final Object tetheredLock = new Object();
    private AlarmManager mAlarmManager;
    private int mBluetoothUsbNum;
    private Context mContext;
    OnAlarmListener mIdleApAlarmListener = new OnAlarmListener() {
        public void onAlarm() {
            HwNotificationTetheringImpl.this.mSetIdleAlarm = false;
            if (HwNotificationTetheringImpl.this.mPluggedType == 0) {
                HwNotificationTetheringImpl.this.stopTethering(0);
                Log.d(HwNotificationTetheringImpl.TAG, "WIFI_TETHER was in idle for 10 minutes, stop tethering, show notification");
                HwNotificationTetheringImpl.this.showStopWifiTetherNotification();
            }
        }
    };
    private NotificationManager mNotificationManager;
    private int mP2pConnectNum;
    private boolean mP2pTethered;
    private PendingIntent mPi;
    private int mPluggedType = -1;
    private volatile boolean mSetIdleAlarm = false;
    private OnStartTetheringCallback mStartTetheringCallback;
    private Notification mStopApNotification;
    private boolean mSupportWifiRepeater;
    private Notification mTetheredNotification;
    private int[] mTetheredRecord = new int[]{0, 0, 0, 0};
    private int mTotalNum;
    private int mWifiConnectNum;
    private boolean mWifiTetherd;

    private class BluetoothPanProfileConnectNumReceiver extends BroadcastReceiver {
        private BluetoothPanProfileConnectNumReceiver() {
        }

        /* synthetic */ BluetoothPanProfileConnectNumReceiver(HwNotificationTetheringImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            int bluetoothConnectNum = intent.getIntExtra(HwNotificationTetheringImpl.EXTRA_BLUETOOTH_PAN_PROFILE_CONNECTEED_SIZE, 0);
            if (HwNotificationTetheringImpl.DBG) {
                String str = HwNotificationTetheringImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BluetoothPanProfileConnectNumReceiver bluetoothConnectNum = ");
                stringBuilder.append(bluetoothConnectNum);
                Log.d(str, stringBuilder.toString());
            }
            if (HwNotificationTetheringImpl.this.mTetheredNotification != null && HwNotificationTetheringImpl.this.mBluetoothUsbNum != bluetoothConnectNum) {
                HwNotificationTetheringImpl.this.mBluetoothUsbNum = bluetoothConnectNum;
                HwNotificationTetheringImpl.this.showTetheredNotificationWithNumbers(2);
            }
        }
    }

    static final class OnStartTetheringCallback extends android.net.ConnectivityManager.OnStartTetheringCallback {
        OnStartTetheringCallback() {
        }

        public void onTetheringStarted() {
        }

        public void onTetheringFailed() {
            Log.e(HwNotificationTetheringImpl.TAG, "WIFI tethering FAILED!");
        }
    }

    private class P2pConnectNumReceiver extends BroadcastReceiver {
        private P2pConnectNumReceiver() {
        }

        /* synthetic */ P2pConnectNumReceiver(HwNotificationTetheringImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String str = HwNotificationTetheringImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive: ");
            stringBuilder.append(intent.getAction());
            Log.d(str, stringBuilder.toString());
            int p2pConnectNum = intent.getIntExtra(HwNotificationTetheringImpl.EXTRA_WIFI_REPEATER_CLIENTS_SIZE, 0);
            if (HwNotificationTetheringImpl.this.mTetheredNotification != null && HwNotificationTetheringImpl.this.mP2pTethered && HwNotificationTetheringImpl.this.mP2pConnectNum != p2pConnectNum) {
                HwNotificationTetheringImpl.this.mP2pConnectNum = p2pConnectNum;
                HwNotificationTetheringImpl.this.showTetheredNotificationWithNumbers(3);
            }
        }
    }

    private class WifiConnectNumReceiver extends BroadcastReceiver {
        private WifiConnectNumReceiver() {
        }

        /* synthetic */ WifiConnectNumReceiver(HwNotificationTetheringImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String str = HwNotificationTetheringImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive: ");
            stringBuilder.append(intent.getAction());
            Log.d(str, stringBuilder.toString());
            int wifiConnectNum = intent.getIntExtra(HwNotificationTetheringImpl.EXTRA_STA_COUNT, 0);
            if (HwNotificationTetheringImpl.this.mTetheredNotification != null && HwNotificationTetheringImpl.this.mWifiTetherd && HwNotificationTetheringImpl.this.mWifiConnectNum != wifiConnectNum) {
                HwNotificationTetheringImpl.this.mWifiConnectNum = wifiConnectNum;
                HwNotificationTetheringImpl.this.showTetheredNotificationWithNumbers(0);
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public HwNotificationTetheringImpl(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        initConfigs();
        registerConnectNumReceiver();
        registerStartTetherReceiver();
        if (!isSoftApCust()) {
            registerPluggedTypeReceiver();
        }
    }

    private void initConfigs() {
        this.mSupportWifiRepeater = SystemProperties.getBoolean("ro.config.hw_wifibridge", false);
    }

    private void registerConnectNumReceiver() {
        if (this.mSupportWifiRepeater) {
            this.mContext.registerReceiver(new P2pConnectNumReceiver(this, null), new IntentFilter(WIFI_REPEATER_CLIENTS_CHANGED_ACTION));
        }
        BroadcastReceiver receiver = new WifiConnectNumReceiver(this, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_WIFI_AP_STA_JOIN);
        filter.addAction(ACTION_WIFI_AP_STA_LEAVE);
        this.mContext.registerReceiver(receiver, filter);
        this.mContext.registerReceiver(new BluetoothPanProfileConnectNumReceiver(this, null), new IntentFilter(ACTION_CONNECTION_STATE_CHANGED));
    }

    private void registerStartTetherReceiver() {
        this.mStartTetheringCallback = new OnStartTetheringCallback();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (HwNotificationTetheringImpl.START_TETHER_ACTION.equals(intent.getAction())) {
                    Log.d(HwNotificationTetheringImpl.TAG, "receive start tether action");
                    ((ConnectivityManager) HwNotificationTetheringImpl.this.mContext.getSystemService("connectivity")).startTethering(0, false, HwNotificationTetheringImpl.this.mStartTetheringCallback);
                    HwNotificationTetheringImpl.this.clearStopApNotification();
                }
            }
        }, new IntentFilter(START_TETHER_ACTION), START_TETHER_PERMISSION, null);
    }

    private void registerPluggedTypeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                    int temp = intent.getIntExtra("plugged", 0);
                    String str = HwNotificationTetheringImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("new plugged:");
                    stringBuilder.append(temp);
                    stringBuilder.append(", current plugged:");
                    stringBuilder.append(HwNotificationTetheringImpl.this.mPluggedType);
                    Log.d(str, stringBuilder.toString());
                    if (HwNotificationTetheringImpl.this.mPluggedType != temp) {
                        HwNotificationTetheringImpl.this.mPluggedType = temp;
                        if (HwNotificationTetheringImpl.this.mPluggedType != 0) {
                            HwNotificationTetheringImpl.this.mSetIdleAlarm = false;
                            HwNotificationTetheringImpl.this.mAlarmManager.cancel(HwNotificationTetheringImpl.this.mIdleApAlarmListener);
                        } else if (HwNotificationTetheringImpl.this.mWifiTetherd && HwNotificationTetheringImpl.this.mWifiConnectNum <= 0) {
                            Log.d(HwNotificationTetheringImpl.TAG, "reset IDLE_WIFI_TETHER alarm");
                            HwNotificationTetheringImpl.this.mAlarmManager.cancel(HwNotificationTetheringImpl.this.mIdleApAlarmListener);
                            HwNotificationTetheringImpl.this.mSetIdleAlarm = true;
                            HwNotificationTetheringImpl.this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME, HwNotificationTetheringImpl.IDLE_WIFI_TETHER_ALARM_TAG, HwNotificationTetheringImpl.this.mIdleApAlarmListener, null);
                        }
                    }
                }
            }
        }, filter);
    }

    private void sendNotification(boolean shouldForceUpdate) {
        int totalNum = this.mBluetoothUsbNum + this.mWifiConnectNum;
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendNumberChangeNotification:");
            stringBuilder.append(totalNum);
            Log.d(str, stringBuilder.toString());
        }
        if (shouldForceUpdate || this.mTotalNum != totalNum) {
            this.mTotalNum = totalNum;
            Notification tempTetheredNotification = this.mTetheredNotification;
            if (tempTetheredNotification != null) {
                this.mNotificationManager.cancelAsUser(null, tempTetheredNotification.icon, UserHandle.ALL);
                CharSequence title = null;
                CharSequence message = null;
                if (this.mWifiConnectNum > 0 && this.mBluetoothUsbNum == 0) {
                    Resources r = Resources.getSystem();
                    title = this.mContext.getString(33685840);
                    message = r.getQuantityString(34406400, this.mWifiConnectNum, new Object[]{Integer.valueOf(this.mWifiConnectNum)});
                }
                tempTetheredNotification.tickerText = title;
                tempTetheredNotification.setLatestEventInfo(this.mContext, title, message, this.mPi);
                this.mNotificationManager.notifyAsUser(null, tempTetheredNotification.icon, tempTetheredNotification, UserHandle.ALL);
            }
        }
    }

    @Deprecated
    public int getTetheredIcon(boolean usbTethered, boolean wifiTethered, boolean bluetoothTethered, boolean p2pTethered) {
        return 0;
    }

    @Deprecated
    public void setTetheringNumber(boolean wifiTethered, boolean usbTethered, boolean bluetoothTethered) {
    }

    public void setTetheringNumber(List<String> tetheringNumbers) {
        boolean p2pTethered = tetheringNumbers.contains("p2p");
        if (this.mSupportWifiRepeater) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setTetheringNumber: p2pTethered = ");
            stringBuilder.append(p2pTethered);
            Log.d(str, stringBuilder.toString());
            this.mP2pTethered = p2pTethered;
            if (!this.mP2pTethered) {
                this.mP2pConnectNum = 0;
            }
        }
        boolean wifiTethered = tetheringNumbers.contains("wifi");
        boolean usbTethered = tetheringNumbers.contains("usb");
        boolean bluetoothTethered = tetheringNumbers.contains("bluetooth");
        if (DBG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("wifiTethered:");
            stringBuilder2.append(wifiTethered);
            stringBuilder2.append(" usbTethered:");
            stringBuilder2.append(usbTethered);
            stringBuilder2.append(" bluetoothTethered:");
            stringBuilder2.append(bluetoothTethered);
            Log.d(str2, stringBuilder2.toString());
        }
        this.mBluetoothUsbNum = 0;
        if (usbTethered) {
            this.mBluetoothUsbNum = 1;
        }
        if (bluetoothTethered) {
            this.mBluetoothUsbNum++;
        }
        this.mWifiTetherd = wifiTethered;
        if (!this.mWifiTetherd) {
            this.mWifiConnectNum = 0;
        }
    }

    @Deprecated
    public boolean sendTetherNotification(Notification tetheredNotification, CharSequence title, CharSequence message, PendingIntent pi) {
        return false;
    }

    public void sendTetherNotification() {
        sendNotification(false);
    }

    public void clearTetheredNotification() {
        synchronized (tetheredLock) {
            if (DBG) {
                Log.d(TAG, "clearTetheredNotification");
            }
            this.mTetheredNotification = null;
            this.mSetIdleAlarm = false;
            this.mAlarmManager.cancel(this.mIdleApAlarmListener);
        }
    }

    private void resetTetheredRecord() {
        for (int type = 0; type < 4; type++) {
            this.mTetheredRecord[type] = 0;
        }
    }

    public void stopTethering() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        for (int type = 0; type < 4; type++) {
            if (this.mTetheredRecord[type] == 1) {
                cm.stopTethering(type);
            }
        }
    }

    private void stopTethering(int type) {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (this.mTetheredRecord[type] == 1) {
            cm.stopTethering(type);
        }
    }

    public int getNotificationType(List<String> tetheringNumbers) {
        int type = -1;
        int tetheredTypes = 0;
        resetTetheredRecord();
        boolean wifiTethered = tetheringNumbers.contains("wifi");
        boolean usbTethered = tetheringNumbers.contains("usb");
        boolean bluetoothTethered = tetheringNumbers.contains("bluetooth");
        boolean p2pTethered = tetheringNumbers.contains("p2p");
        if (wifiTethered) {
            type = 0;
            tetheredTypes = 0 + 1;
            this.mTetheredRecord[0] = 1;
        }
        if (usbTethered) {
            type = 1;
            tetheredTypes++;
            this.mTetheredRecord[1] = 1;
        }
        if (bluetoothTethered) {
            type = 2;
            tetheredTypes++;
            this.mTetheredRecord[2] = 1;
        }
        if (p2pTethered) {
            type = 3;
            tetheredTypes++;
            this.mTetheredRecord[3] = 1;
        }
        if (tetheredTypes > 1) {
            return 4;
        }
        return type;
    }

    public int getNotificationIcon(int notificationType) {
        switch (notificationType) {
            case 0:
                return 17303548;
            case 1:
                return 33751226;
            case 2:
                return 33751224;
            case 3:
                return 17303548;
            case 4:
                return 33751225;
            case 5:
                return 33751942;
            default:
                return 0;
        }
    }

    public CharSequence getNotificationTitle(int notificationType) {
        Resources r = Resources.getSystem();
        if (3 == notificationType) {
            return r.getText(33685850);
        }
        if (5 == notificationType) {
            return r.getText(33686179);
        }
        return r.getText(33685847);
    }

    private CharSequence getNotificationMessageWithNumbers(int notificationType) {
        Resources r = Resources.getSystem();
        switch (notificationType) {
            case 0:
                return r.getQuantityString(34406401, this.mWifiConnectNum, new Object[]{Integer.valueOf(this.mWifiConnectNum)});
            case 1:
            case 2:
                return r.getQuantityString(34406401, this.mBluetoothUsbNum, new Object[]{Integer.valueOf(this.mBluetoothUsbNum)});
            case 3:
                return r.getQuantityString(34406401, this.mP2pConnectNum, new Object[]{Integer.valueOf(this.mP2pConnectNum)});
            default:
                return r.getText(33685848);
        }
    }

    public CharSequence getNotificationActionText(int notificationType) {
        Resources r = Resources.getSystem();
        if (notificationType == 0) {
            return r.getText(33685852);
        }
        if (notificationType == 3) {
            return r.getText(33685851);
        }
        if (notificationType != 5) {
            return r.getText(33685849);
        }
        return r.getText(33686177);
    }

    public Intent getNotificationIntent(int notificationType) {
        Intent intent = new Intent();
        if (notificationType == 0) {
            intent.setAction("android.settings.WIFI_AP_SETTINGS");
        } else if (notificationType != 3) {
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        } else {
            intent.setAction("android.settings.WIFI_BRIDGE_SETTINGS");
        }
        return intent;
    }

    public void showTetheredNotification(int notificationType, Notification notification, PendingIntent pi) {
        this.mTetheredNotification = notification;
        this.mPi = pi;
        showTetheredNotificationWithNumbers(notificationType);
    }

    /* JADX WARNING: Missing block: B:37:0x00c7, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void showTetheredNotificationWithNumbers(int notificationType) {
        synchronized (tetheredLock) {
            if (this.mTetheredNotification == null) {
                return;
            }
            this.mNotificationManager.cancelAsUser(null, this.mTetheredNotification.icon, UserHandle.ALL);
            if (this.mWifiTetherd) {
                clearStopApNotification();
                if (this.mWifiConnectNum > 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set mSetIdleAlarm false, mWifiConnectNum is ");
                    stringBuilder.append(this.mWifiConnectNum);
                    Log.d(str, stringBuilder.toString());
                    this.mSetIdleAlarm = false;
                    this.mAlarmManager.cancel(this.mIdleApAlarmListener);
                } else if (!(isSoftApCust() || this.mSetIdleAlarm)) {
                    Log.d(TAG, "set IDLE_WIFI_TETHER alarm, set mSetIdleAlarm:true");
                    this.mSetIdleAlarm = true;
                    this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME, IDLE_WIFI_TETHER_ALARM_TAG, this.mIdleApAlarmListener, null);
                }
            }
            Log.d(TAG, "showTetheredNotificationWithNumbers");
            if (isConfigR1()) {
                CharSequence message;
                this.mTetheredNotification.flags = 32;
                if (this.mWifiConnectNum <= 0) {
                    message = Resources.getSystem().getText(33686180);
                } else {
                    message = getNotificationMessageWithNumbers(notificationType);
                }
                notifyNotification(notificationType, message);
            } else if (this.mWifiConnectNum > 0) {
                notifyNotification(notificationType, getNotificationMessageWithNumbers(notificationType));
            }
            if (this.mP2pConnectNum <= 0) {
                if (this.mBluetoothUsbNum <= 0) {
                    if (notificationType == 3) {
                        notifyNotification(notificationType, Resources.getSystem().getText(33685848));
                    }
                }
            }
            notifyNotification(notificationType, getNotificationMessageWithNumbers(notificationType));
        }
    }

    private void notifyNotification(int notificationType, CharSequence message) {
        int selectType = notificationType;
        this.mTetheredNotification.setLatestEventInfo(this.mContext, getNotificationTitle(selectType), message, this.mPi);
        this.mTetheredNotification.actions = getNotificationAction(selectType);
        this.mNotificationManager.notifyAsUser(null, this.mTetheredNotification.icon, this.mTetheredNotification, UserHandle.ALL);
    }

    private boolean isConfigR1() {
        return SystemProperties.get("ro.config.hw_opta", "0").equals("389") && SystemProperties.get("ro.config.hw_optb", "0").equals("840");
    }

    private synchronized void showStopWifiTetherNotification() {
        PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, getNotificationIntent(0), 0, null, UserHandle.CURRENT);
        PendingIntent pIntentCancel = PendingIntent.getBroadcast(this.mContext, 0, new Intent(START_TETHER_ACTION), 134217728);
        Resources resource = Resources.getSystem();
        CharSequence title = getNotificationTitle(5);
        CharSequence message = resource.getText(33686178);
        CharSequence action_text = getNotificationActionText(5);
        int icon = getNotificationIcon(5);
        Builder stopApNotificationBuilder = new Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
        stopApNotificationBuilder.setWhen(0).setOngoing(false).setVisibility(1).setCategory("status").addAction(new Action(0, action_text, pIntentCancel)).setSmallIcon(icon).setContentTitle(title).setContentText(message).setStyle(new BigTextStyle().bigText(message)).setContentIntent(pi).setAutoCancel(true);
        this.mStopApNotification = stopApNotificationBuilder.build();
        this.mNotificationManager.notifyAsUser(null, this.mStopApNotification.icon, this.mStopApNotification, UserHandle.ALL);
    }

    private synchronized void clearStopApNotification() {
        if (this.mStopApNotification != null) {
            this.mNotificationManager.cancelAsUser(null, this.mStopApNotification.icon, UserHandle.ALL);
            this.mStopApNotification = null;
        }
    }

    private Action[] getNotificationAction(int notificationType) {
        Action[] actions = new Action[1];
        actions[0] = new Action(0, getNotificationActionText(notificationType), PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.server.connectivity.action.STOP_TETHERING"), 134217728));
        return actions;
    }

    private boolean isSoftApCust() {
        if (SystemProperties.getBoolean("ro.config.check_hotspot_status", false) || "true".equals(Global.getString(this.mContext.getContentResolver(), "hotspot_power_mode_on"))) {
            return true;
        }
        return false;
    }
}
