package com.android.server.wifi;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class WifiNotificationController {
    private static final String ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND = "com.huawei.wifipro.action.ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND";
    private static final String ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE = "com.huawei.wifipro.action.ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE";
    private static final boolean HWFLOW;
    private static final int ICON_NETWORKS_AVAILABLE = 17303481;
    private static final boolean NOTIFY_OPEN_NETWORKS_VALUE = SystemProperties.getBoolean("ro.config.notify_open_networks", false);
    private static final int NUM_SCANS_BEFORE_ACTUALLY_SCANNING = 3;
    private static final String TAG = "WifiNotificationController";
    private final long NOTIFICATION_REPEAT_DELAY_MS;
    private final Context mContext;
    private DetailedState mDetailedState = DetailedState.IDLE;
    private FrameworkFacade mFrameworkFacade;
    private boolean mIsAccessAPFound;
    private NetworkInfo mNetworkInfo;
    private Builder mNotificationBuilder;
    private boolean mNotificationEnabled;
    private NotificationEnabledSettingObserver mNotificationEnabledSettingObserver;
    private long mNotificationRepeatTime;
    private boolean mNotificationShown;
    private int mNumScansSinceNetworkStateChange;
    private WifiInjector mWifiInjector;
    private WifiScanner mWifiScanner;
    private volatile int mWifiState = 4;

    /* renamed from: com.android.server.wifi.WifiNotificationController$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CAPTIVE_PORTAL_CHECK.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.IDLE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.SCANNING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.AUTHENTICATING.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.OBTAINING_IPADDR.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.SUSPENDED.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.FAILED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.BLOCKED.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.VERIFYING_POOR_LINK.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    private class NotificationEnabledSettingObserver extends ContentObserver {
        public NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            WifiNotificationController.this.mFrameworkFacade.registerContentObserver(WifiNotificationController.this.mContext, Global.getUriFor("wifi_networks_available_notification_on"), true, this);
            synchronized (WifiNotificationController.this) {
                WifiNotificationController.this.mNotificationEnabled = getValue();
            }
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (WifiNotificationController.this) {
                WifiNotificationController.this.mNotificationEnabled = getValue();
                WifiNotificationController.this.resetNotification();
            }
        }

        private boolean getValue() {
            return WifiNotificationController.this.mFrameworkFacade.getIntegerSetting(WifiNotificationController.this.mContext, "wifi_networks_available_notification_on", 1) == 1;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    WifiNotificationController(Context context, Looper looper, FrameworkFacade framework, Builder builder, WifiInjector wifiInjector) {
        this.mContext = context;
        this.mFrameworkFacade = framework;
        this.mNotificationBuilder = builder;
        this.mWifiInjector = wifiInjector;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        filter.addAction(ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND);
        filter.addAction(ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* JADX WARNING: Missing block: B:17:0x0084, code:
            if (com.android.server.wifi.WifiNotificationController.access$500(r4.this$0) != false) goto L_0x0174;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                String str;
                StringBuilder stringBuilder;
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    WifiNotificationController.this.mWifiState = intent.getIntExtra("wifi_state", 4);
                    WifiNotificationController.this.resetNotification();
                } else if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                    WifiNotificationController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (WifiNotificationController.this.mNetworkInfo != null) {
                        DetailedState detailedState = WifiNotificationController.this.mNetworkInfo.getDetailedState();
                        if (!(detailedState == DetailedState.SCANNING || detailedState == WifiNotificationController.this.mDetailedState)) {
                            WifiNotificationController.this.mDetailedState = detailedState;
                            switch (AnonymousClass2.$SwitchMap$android$net$NetworkInfo$DetailedState[WifiNotificationController.this.mDetailedState.ordinal()]) {
                                case 1:
                                    if (WifiNotificationController.this.mWifiInjector.getWifiStateMachine().isWifiProEnabled()) {
                                        break;
                                    }
                                case 2:
                                case 3:
                                    WifiNotificationController.this.resetNotification();
                                    break;
                            }
                        }
                    }
                } else if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
                    if (WifiNotificationController.this.mWifiScanner == null) {
                        WifiNotificationController.this.mWifiScanner = WifiNotificationController.this.mWifiInjector.getWifiScanner();
                    }
                    if (!WifiNotificationController.this.mWifiInjector.getWifiStateMachine().isWifiProEnabled() || WifiNotificationController.NOTIFY_OPEN_NETWORKS_VALUE) {
                        WifiNotificationController.this.checkAndSetNotification(WifiNotificationController.this.mNetworkInfo, WifiNotificationController.this.mWifiScanner.getSingleScanResults());
                    }
                } else if (intent.getAction().equals(WifiNotificationController.ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND)) {
                    str = WifiNotificationController.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("find access ap, mNotificationShown = ");
                    stringBuilder.append(WifiNotificationController.this.mNotificationShown);
                    Log.d(str, stringBuilder.toString());
                    if (WifiNotificationController.this.mNotificationEnabled && !WifiNotificationController.this.mNotificationShown) {
                        WifiNotificationController.this.mIsAccessAPFound = true;
                        WifiNotificationController.this.setNotificationVisible(false, 1, false, 0);
                    }
                } else if (intent.getAction().equals(WifiNotificationController.ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE)) {
                    str = WifiNotificationController.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("access ap , mNotificationShown = ");
                    stringBuilder.append(WifiNotificationController.this.mNotificationShown);
                    stringBuilder.append(", mIsAccessAPFound = ");
                    stringBuilder.append(WifiNotificationController.this.mIsAccessAPFound);
                    Log.d(str, stringBuilder.toString());
                    if (WifiNotificationController.this.mIsAccessAPFound && WifiNotificationController.this.mNotificationShown) {
                        WifiNotificationController.this.mIsAccessAPFound = false;
                        WifiNotificationController.this.resetNotification();
                    }
                }
            }
        }, filter);
        this.NOTIFICATION_REPEAT_DELAY_MS = ((long) this.mFrameworkFacade.getIntegerSetting(context, "wifi_networks_available_repeat_delay", 900)) * 1000;
        this.mNotificationEnabledSettingObserver = new NotificationEnabledSettingObserver(new Handler(looper));
        this.mNotificationEnabledSettingObserver.register();
    }

    /* JADX WARNING: Missing block: B:43:0x0096, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void checkAndSetNotification(NetworkInfo networkInfo, List<ScanResult> scanResults) {
        if (!this.mNotificationEnabled) {
            return;
        }
        if (this.mWifiState == 3) {
            if (!UserManager.get(this.mContext).hasUserRestriction("no_config_wifi", UserHandle.CURRENT)) {
                State state = State.DISCONNECTED;
                if (networkInfo != null) {
                    state = networkInfo.getState();
                }
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkAndSetNotification, state:");
                    stringBuilder.append(state);
                    Log.i(str, stringBuilder.toString());
                }
                if (state == State.DISCONNECTED || state == State.UNKNOWN) {
                    if (scanResults != null) {
                        int i;
                        int numOpenNetworks = 0;
                        for (i = scanResults.size() - 1; i >= 0; i--) {
                            ScanResult scanResult = (ScanResult) scanResults.get(i);
                            if (scanResult.capabilities != null && scanResult.capabilities.equals("[ESS]")) {
                                numOpenNetworks++;
                            }
                        }
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Open network num:");
                        stringBuilder2.append(numOpenNetworks);
                        Log.d(str2, stringBuilder2.toString());
                        if (numOpenNetworks > 0) {
                            i = this.mNumScansSinceNetworkStateChange + 1;
                            this.mNumScansSinceNetworkStateChange = i;
                            if (i >= 3) {
                                setNotificationVisible(NOTIFY_OPEN_NETWORKS_VALUE, numOpenNetworks, false, 0);
                            }
                        }
                    } else {
                        Log.d(TAG, "scanResults is null");
                    }
                }
                setNotificationVisible(false, 0, false, 0);
            }
        }
    }

    private synchronized void resetNotification() {
        this.mNotificationRepeatTime = 0;
        this.mNumScansSinceNetworkStateChange = 0;
        setNotificationVisible(false, 0, false, 0);
    }

    private void setNotificationVisible(boolean visible, int numNetworks, boolean force, int delay) {
        if (visible || this.mNotificationShown || force) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (!visible) {
                notificationManager.cancelAsUser(null, 13, UserHandle.ALL);
            } else if (System.currentTimeMillis() >= this.mNotificationRepeatTime) {
                if (this.mNotificationBuilder == null) {
                    this.mNotificationBuilder = new Builder(this.mContext, SystemNotificationChannels.NETWORK_AVAILABLE).setWhen(0).setSmallIcon(ICON_NETWORKS_AVAILABLE).setAutoCancel(true).setContentIntent(TaskStackBuilder.create(this.mContext).addNextIntentWithParentStack(new Intent("android.net.wifi.PICK_WIFI_NETWORK")).getPendingIntent(0, 0, null, UserHandle.CURRENT)).setColor(this.mContext.getResources().getColor(17170784));
                    Bitmap bmp = BitmapFactory.decodeResource(this.mContext.getResources(), 33751683);
                    if (bmp != null) {
                        this.mNotificationBuilder.setLargeIcon(bmp);
                    }
                }
                CharSequence title = this.mContext.getResources().getQuantityText(18153501, numNetworks);
                CharSequence details = this.mContext.getResources().getQuantityText(18153502, numNetworks);
                this.mNotificationBuilder.setTicker(title);
                this.mNotificationBuilder.setContentTitle(title);
                this.mNotificationBuilder.setContentText(details);
                this.mNotificationRepeatTime = System.currentTimeMillis() + this.NOTIFICATION_REPEAT_DELAY_MS;
                Notification notification = this.mNotificationBuilder.build();
                notificationManager.notifyAsUser(null, 13, this.mNotificationBuilder.build(), UserHandle.ALL);
            } else {
                return;
            }
            this.mNotificationShown = visible;
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNotificationEnabled ");
        stringBuilder.append(this.mNotificationEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNotificationRepeatTime ");
        stringBuilder.append(this.mNotificationRepeatTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNotificationShown ");
        stringBuilder.append(this.mNotificationShown);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNumScansSinceNetworkStateChange ");
        stringBuilder.append(this.mNumScansSinceNetworkStateChange);
        pw.println(stringBuilder.toString());
    }
}
