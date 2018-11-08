package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.wifi.SsidSetStoreData.DataSource;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

public class OpenNetworkNotifier {
    static final int DEFAULT_REPEAT_DELAY_SEC = 900;
    private static final int STATE_CONNECTED_NOTIFICATION = 3;
    private static final int STATE_CONNECTING_IN_NOTIFICATION = 2;
    private static final int STATE_CONNECT_FAILED_NOTIFICATION = 4;
    private static final int STATE_NO_NOTIFICATION = 0;
    private static final int STATE_SHOWING_RECOMMENDATION_NOTIFICATION = 1;
    private static final String STORE_DATA_IDENTIFIER = "OpenNetworkNotifierBlacklist";
    private static final String TAG = "OpenNetworkNotifier";
    private static final int TIME_TO_SHOW_CONNECTED_MILLIS = 5000;
    private static final int TIME_TO_SHOW_CONNECTING_MILLIS = 10000;
    private static final int TIME_TO_SHOW_FAILED_MILLIS = 5000;
    private final Set<String> mBlacklistedSsids;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION)) {
                OpenNetworkNotifier.this.handleUserDismissedAction();
            } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK)) {
                OpenNetworkNotifier.this.handleConnectToNetworkAction();
            } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK)) {
                OpenNetworkNotifier.this.handleSeeAllNetworksAction();
            } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE)) {
                OpenNetworkNotifier.this.handlePickWifiNetworkAfterConnectFailure();
            } else {
                Log.e(OpenNetworkNotifier.TAG, "Unknown action " + intent.getAction());
            }
        }
    };
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final Callback mConnectionStateCallback = new -$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM(this);
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private final ConnectToNetworkNotificationBuilder mNotificationBuilder;
    private final long mNotificationRepeatDelay;
    private long mNotificationRepeatTime;
    private final OpenNetworkRecommender mOpenNetworkRecommender;
    private ScanResult mRecommendedNetwork;
    private boolean mScreenOn;
    private boolean mSettingEnabled;
    private final Messenger mSrcMessenger;
    private int mState = 0;
    private final WifiMetrics mWifiMetrics;
    private final WifiStateMachine mWifiStateMachine;

    private class NotificationEnabledSettingObserver extends ContentObserver {
        NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            OpenNetworkNotifier.this.mFrameworkFacade.registerContentObserver(OpenNetworkNotifier.this.mContext, Global.getUriFor("wifi_networks_available_notification_on"), true, this);
            OpenNetworkNotifier.this.mSettingEnabled = getValue();
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            OpenNetworkNotifier.this.mSettingEnabled = getValue();
            OpenNetworkNotifier.this.clearPendingNotification(true);
        }

        private boolean getValue() {
            OpenNetworkNotifier.this.mWifiMetrics.setIsWifiNetworksAvailableNotificationEnabled(false);
            return false;
        }
    }

    private class OpenNetworkNotifierStoreData implements DataSource {
        private OpenNetworkNotifierStoreData() {
        }

        public Set<String> getSsids() {
            return new ArraySet(OpenNetworkNotifier.this.mBlacklistedSsids);
        }

        public void setSsids(Set<String> ssidList) {
            OpenNetworkNotifier.this.mBlacklistedSsids.addAll(ssidList);
            OpenNetworkNotifier.this.mWifiMetrics.setOpenNetworkRecommenderBlacklistSize(OpenNetworkNotifier.this.mBlacklistedSsids.size());
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    OpenNetworkNotifier(Context context, Looper looper, FrameworkFacade framework, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiStateMachine wifiStateMachine, OpenNetworkRecommender openNetworkRecommender, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mFrameworkFacade = framework;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
        this.mConfigManager = wifiConfigManager;
        this.mWifiStateMachine = wifiStateMachine;
        this.mOpenNetworkRecommender = openNetworkRecommender;
        this.mNotificationBuilder = connectToNetworkNotificationBuilder;
        this.mScreenOn = false;
        this.mSrcMessenger = new Messenger(new Handler(looper, this.mConnectionStateCallback));
        this.mBlacklistedSsids = new ArraySet();
        wifiConfigStore.registerStoreData(new SsidSetStoreData(STORE_DATA_IDENTIFIER, new OpenNetworkNotifierStoreData()));
        this.mNotificationRepeatDelay = ((long) this.mFrameworkFacade.getIntegerSetting(context, "wifi_networks_available_repeat_delay", DEFAULT_REPEAT_DELAY_SEC)) * 1000;
        new NotificationEnabledSettingObserver(this.mHandler).register();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, this.mHandler);
    }

    /* synthetic */ boolean lambda$-com_android_server_wifi_OpenNetworkNotifier_8777(Message msg) {
        switch (msg.what) {
            case 151554:
                handleConnectionAttemptFailedToSend();
                break;
            case 151555:
                break;
            default:
                Log.e(TAG, "Unknown message " + msg.what);
                break;
        }
        return true;
    }

    public void clearPendingNotification(boolean resetRepeatTime) {
        if (resetRepeatTime) {
            this.mNotificationRepeatTime = 0;
        }
        if (this.mState != 0) {
            getNotificationManager().cancel(17303299);
            if (this.mRecommendedNetwork != null) {
                Log.d(TAG, "Notification with state=" + this.mState + " was cleared for recommended network: " + this.mRecommendedNetwork.SSID);
            }
            this.mState = 0;
            this.mRecommendedNetwork = null;
        }
    }

    private boolean isControllerEnabled() {
        return this.mSettingEnabled ? UserManager.get(this.mContext).hasUserRestriction("no_config_wifi", UserHandle.CURRENT) ^ 1 : false;
    }

    public void handleScanResults(List<ScanDetail> availableNetworks) {
        if (!isControllerEnabled()) {
            clearPendingNotification(true);
        } else if (availableNetworks.isEmpty()) {
            clearPendingNotification(false);
        } else if (this.mState == 0 && this.mClock.getWallClockMillis() < this.mNotificationRepeatTime) {
        } else {
            if (this.mState != 0 || (this.mScreenOn ^ 1) == 0) {
                if (this.mState == 0 || this.mState == 1) {
                    ScanResult recommendation = this.mOpenNetworkRecommender.recommendNetwork(availableNetworks, new ArraySet(this.mBlacklistedSsids));
                    if (recommendation != null) {
                        postInitialNotification(recommendation);
                    } else {
                        clearPendingNotification(false);
                    }
                }
            }
        }
    }

    public void handleScreenStateChanged(boolean screenOn) {
        this.mScreenOn = screenOn;
    }

    public void handleWifiConnected() {
        if (this.mState != 2) {
            clearPendingNotification(true);
            return;
        }
        postNotification(this.mNotificationBuilder.createNetworkConnectedNotification(this.mRecommendedNetwork));
        Log.d(TAG, "User connected to recommended network: " + this.mRecommendedNetwork.SSID);
        this.mWifiMetrics.incrementConnectToNetworkNotification(3);
        this.mState = 3;
        this.mHandler.postDelayed(new com.android.server.wifi.-$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM.AnonymousClass1((byte) 2, this), 5000);
    }

    /* synthetic */ void lambda$-com_android_server_wifi_OpenNetworkNotifier_13081() {
        if (this.mState == 3) {
            clearPendingNotification(true);
        }
    }

    public void handleConnectionFailure() {
        if (this.mState == 2) {
            postNotification(this.mNotificationBuilder.createNetworkFailedNotification());
            Log.d(TAG, "User failed to connect to recommended network: " + this.mRecommendedNetwork.SSID);
            this.mWifiMetrics.incrementConnectToNetworkNotification(4);
            this.mState = 4;
            this.mHandler.postDelayed(new com.android.server.wifi.-$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM.AnonymousClass1((byte) 1, this), 5000);
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_OpenNetworkNotifier_13963() {
        if (this.mState == 4) {
            clearPendingNotification(false);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) this.mContext.getSystemService("notification");
    }

    private void postInitialNotification(ScanResult recommendedNetwork) {
        if (this.mRecommendedNetwork == null || !TextUtils.equals(this.mRecommendedNetwork.SSID, recommendedNetwork.SSID)) {
            postNotification(this.mNotificationBuilder.createConnectToNetworkNotification(recommendedNetwork));
            if (this.mState == 0) {
                this.mWifiMetrics.incrementConnectToNetworkNotification(1);
            } else {
                this.mWifiMetrics.incrementNumOpenNetworkRecommendationUpdates();
            }
            this.mState = 1;
            this.mRecommendedNetwork = recommendedNetwork;
            this.mNotificationRepeatTime = this.mClock.getWallClockMillis() + this.mNotificationRepeatDelay;
        }
    }

    private void postNotification(Notification notification) {
        getNotificationManager().notify(17303299, notification);
    }

    private void handleConnectToNetworkAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mState, 2);
        if (this.mState == 1) {
            postNotification(this.mNotificationBuilder.createNetworkConnectingNotification(this.mRecommendedNetwork));
            this.mWifiMetrics.incrementConnectToNetworkNotification(2);
            Log.d(TAG, "User initiated connection to recommended network: " + this.mRecommendedNetwork.SSID);
            WifiConfiguration network = ScanResultUtil.createNetworkFromScanResult(this.mRecommendedNetwork);
            Message msg = Message.obtain();
            msg.what = 151553;
            msg.arg1 = -1;
            msg.obj = network;
            msg.replyTo = this.mSrcMessenger;
            this.mWifiStateMachine.sendMessage(msg);
            this.mState = 2;
            this.mHandler.postDelayed(new com.android.server.wifi.-$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM.AnonymousClass1((byte) 0, this), 10000);
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_OpenNetworkNotifier_16544() {
        if (this.mState == 2) {
            handleConnectionFailure();
        }
    }

    private void handleSeeAllNetworksAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mState, 3);
        startWifiSettings();
    }

    private void startWifiSettings() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        this.mContext.startActivity(new Intent("android.settings.WIFI_SETTINGS").addFlags(268435456));
        clearPendingNotification(false);
    }

    private void handleConnectionAttemptFailedToSend() {
        handleConnectionFailure();
        this.mWifiMetrics.incrementNumOpenNetworkConnectMessageFailedToSend();
    }

    private void handlePickWifiNetworkAfterConnectFailure() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mState, 4);
        startWifiSettings();
    }

    private void handleUserDismissedAction() {
        Log.d(TAG, "User dismissed notification with state=" + this.mState);
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mState, 1);
        if (this.mState == 1) {
            this.mBlacklistedSsids.add(this.mRecommendedNetwork.SSID);
            this.mWifiMetrics.setOpenNetworkRecommenderBlacklistSize(this.mBlacklistedSsids.size());
            this.mConfigManager.saveToStore(false);
            Log.d(TAG, "Network is added to the open network notification blacklist: " + this.mRecommendedNetwork.SSID);
        }
        resetStateAndDelayNotification();
    }

    private void resetStateAndDelayNotification() {
        this.mState = 0;
        this.mNotificationRepeatTime = System.currentTimeMillis() + this.mNotificationRepeatDelay;
        this.mRecommendedNetwork = null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("OpenNetworkNotifier: ");
        pw.println("mSettingEnabled " + this.mSettingEnabled);
        pw.println("currentTime: " + this.mClock.getWallClockMillis());
        pw.println("mNotificationRepeatTime: " + this.mNotificationRepeatTime);
        pw.println("mState: " + this.mState);
        pw.println("mBlacklistedSsids: " + this.mBlacklistedSsids.toString());
    }
}
