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
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.SsidSetStoreData.DataSource;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

public class AvailableNetworkNotifier {
    @VisibleForTesting
    static final int DEFAULT_REPEAT_DELAY_SEC = 900;
    private static final int STATE_CONNECTED_NOTIFICATION = 3;
    private static final int STATE_CONNECTING_IN_NOTIFICATION = 2;
    private static final int STATE_CONNECT_FAILED_NOTIFICATION = 4;
    private static final int STATE_NO_NOTIFICATION = 0;
    private static final int STATE_SHOWING_RECOMMENDATION_NOTIFICATION = 1;
    private static final int TIME_TO_SHOW_CONNECTED_MILLIS = 5000;
    private static final int TIME_TO_SHOW_CONNECTING_MILLIS = 10000;
    private static final int TIME_TO_SHOW_FAILED_MILLIS = 5000;
    private final Set<String> mBlacklistedSsids;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (AvailableNetworkNotifier.this.mTag.equals(intent.getExtra(ConnectToNetworkNotificationBuilder.AVAILABLE_NETWORK_NOTIFIER_TAG))) {
                String action = intent.getAction();
                Object obj = -1;
                int hashCode = action.hashCode();
                if (hashCode != -1692061185) {
                    if (hashCode != -1140661470) {
                        if (hashCode != 303648504) {
                            if (hashCode == 1260970165 && action.equals(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION)) {
                                obj = null;
                            }
                        } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE)) {
                            obj = 3;
                        }
                    } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK)) {
                        obj = 2;
                    }
                } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK)) {
                    obj = 1;
                }
                switch (obj) {
                    case null:
                        AvailableNetworkNotifier.this.handleUserDismissedAction();
                        break;
                    case 1:
                        AvailableNetworkNotifier.this.handleConnectToNetworkAction();
                        break;
                    case 2:
                        AvailableNetworkNotifier.this.handleSeeAllNetworksAction();
                        break;
                    case 3:
                        AvailableNetworkNotifier.this.handlePickWifiNetworkAfterConnectFailure();
                        break;
                    default:
                        action = AvailableNetworkNotifier.this.mTag;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown action ");
                        stringBuilder.append(intent.getAction());
                        Log.e(action, stringBuilder.toString());
                        break;
                }
            }
        }
    };
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final Callback mConnectionStateCallback = new -$$Lambda$AvailableNetworkNotifier$uFi1H-bLBjC8591OGivQMgKmiaU(this);
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private final ConnectToNetworkNotificationBuilder mNotificationBuilder;
    private final long mNotificationRepeatDelay;
    private long mNotificationRepeatTime;
    private ScanResult mRecommendedNetwork;
    private boolean mScreenOn;
    private boolean mSettingEnabled;
    private final Messenger mSrcMessenger;
    private int mState = 0;
    private final String mStoreDataIdentifier;
    private final int mSystemMessageNotificationId;
    private final String mTag;
    private final String mToggleSettingsName;
    private final WifiMetrics mWifiMetrics;
    private final WifiStateMachine mWifiStateMachine;

    private class NotificationEnabledSettingObserver extends ContentObserver {
        NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            AvailableNetworkNotifier.this.mFrameworkFacade.registerContentObserver(AvailableNetworkNotifier.this.mContext, Global.getUriFor(AvailableNetworkNotifier.this.mToggleSettingsName), true, this);
            AvailableNetworkNotifier.this.mSettingEnabled = getValue();
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            AvailableNetworkNotifier.this.mSettingEnabled = getValue();
            AvailableNetworkNotifier.this.clearPendingNotification(true);
        }

        private boolean getValue() {
            boolean z = true;
            if (AvailableNetworkNotifier.this.mFrameworkFacade.getIntegerSetting(AvailableNetworkNotifier.this.mContext, AvailableNetworkNotifier.this.mToggleSettingsName, 1) != 1) {
                z = false;
            }
            boolean enabled = z;
            AvailableNetworkNotifier.this.mWifiMetrics.setIsWifiNetworksAvailableNotificationEnabled(AvailableNetworkNotifier.this.mTag, enabled);
            String access$100 = AvailableNetworkNotifier.this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Settings toggle enabled=");
            stringBuilder.append(enabled);
            Log.d(access$100, stringBuilder.toString());
            return enabled;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    private class AvailableNetworkNotifierStoreData implements DataSource {
        private AvailableNetworkNotifierStoreData() {
        }

        /* synthetic */ AvailableNetworkNotifierStoreData(AvailableNetworkNotifier x0, AnonymousClass1 x1) {
            this();
        }

        public Set<String> getSsids() {
            return new ArraySet(AvailableNetworkNotifier.this.mBlacklistedSsids);
        }

        public void setSsids(Set<String> ssidList) {
            AvailableNetworkNotifier.this.mBlacklistedSsids.addAll(ssidList);
            AvailableNetworkNotifier.this.mWifiMetrics.setNetworkRecommenderBlacklistSize(AvailableNetworkNotifier.this.mTag, AvailableNetworkNotifier.this.mBlacklistedSsids.size());
        }
    }

    public AvailableNetworkNotifier(String tag, String storeDataIdentifier, String toggleSettingsName, int notificationIdentifier, Context context, Looper looper, FrameworkFacade framework, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiStateMachine wifiStateMachine, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        Context context2 = context;
        Looper looper2 = looper;
        this.mTag = tag;
        this.mStoreDataIdentifier = storeDataIdentifier;
        this.mToggleSettingsName = toggleSettingsName;
        this.mSystemMessageNotificationId = notificationIdentifier;
        this.mContext = context2;
        this.mHandler = new Handler(looper2);
        this.mFrameworkFacade = framework;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
        this.mConfigManager = wifiConfigManager;
        this.mWifiStateMachine = wifiStateMachine;
        this.mNotificationBuilder = connectToNetworkNotificationBuilder;
        this.mScreenOn = false;
        this.mSrcMessenger = new Messenger(new Handler(looper2, this.mConnectionStateCallback));
        this.mBlacklistedSsids = new ArraySet();
        wifiConfigStore.registerStoreData(new SsidSetStoreData(this.mStoreDataIdentifier, new AvailableNetworkNotifierStoreData(this, null)));
        this.mNotificationRepeatDelay = ((long) this.mFrameworkFacade.getIntegerSetting(context2, "wifi_networks_available_repeat_delay", DEFAULT_REPEAT_DELAY_SEC)) * 1000;
        NotificationEnabledSettingObserver settingObserver = new NotificationEnabledSettingObserver(this.mHandler);
        settingObserver.register();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK);
        filter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, this.mHandler);
    }

    public static /* synthetic */ boolean lambda$new$0(AvailableNetworkNotifier availableNetworkNotifier, Message msg) {
        switch (msg.what) {
            case 151554:
                availableNetworkNotifier.handleConnectionAttemptFailedToSend();
                break;
            case 151555:
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown message ");
                stringBuilder.append(msg.what);
                Log.e("AvailableNetworkNotifier", stringBuilder.toString());
                break;
        }
        return true;
    }

    public void clearPendingNotification(boolean resetRepeatTime) {
        if (resetRepeatTime) {
            this.mNotificationRepeatTime = 0;
        }
        if (this.mState != 0) {
            getNotificationManager().cancel(this.mSystemMessageNotificationId);
            if (this.mRecommendedNetwork != null) {
                String str = this.mTag;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Notification with state=");
                stringBuilder.append(this.mState);
                stringBuilder.append(" was cleared for recommended network: ");
                stringBuilder.append(this.mRecommendedNetwork.SSID);
                Log.d(str, stringBuilder.toString());
            }
            this.mState = 0;
            this.mRecommendedNetwork = null;
        }
    }

    private boolean isControllerEnabled() {
        return this.mSettingEnabled && !UserManager.get(this.mContext).hasUserRestriction("no_config_wifi", UserHandle.CURRENT);
    }

    public void handleScanResults(List<ScanDetail> availableNetworks) {
        if (!isControllerEnabled()) {
            clearPendingNotification(true);
        } else if (availableNetworks.isEmpty() && this.mState == 1) {
            clearPendingNotification(false);
        } else if (this.mState == 0 && this.mClock.getWallClockMillis() < this.mNotificationRepeatTime) {
        } else {
            if (this.mState != 0 || this.mScreenOn) {
                if (this.mState == 0 || this.mState == 1) {
                    ScanResult recommendation = recommendNetwork(availableNetworks, new ArraySet(this.mBlacklistedSsids));
                    if (recommendation != null) {
                        postInitialNotification(recommendation);
                    } else {
                        clearPendingNotification(false);
                    }
                }
            }
        }
    }

    public ScanResult recommendNetwork(List<ScanDetail> networks, Set<String> blacklistedSsids) {
        ScanResult result = null;
        int highestRssi = Integer.MIN_VALUE;
        for (ScanDetail scanDetail : networks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (scanResult.level > highestRssi) {
                result = scanResult;
                highestRssi = scanResult.level;
            }
        }
        if (result == null || !blacklistedSsids.contains(result.SSID)) {
            return result;
        }
        return null;
    }

    public void handleScreenStateChanged(boolean screenOn) {
        this.mScreenOn = screenOn;
    }

    public void handleWifiConnected() {
        if (this.mState != 2) {
            clearPendingNotification(true);
            return;
        }
        postNotification(this.mNotificationBuilder.createNetworkConnectedNotification(this.mTag, this.mRecommendedNetwork));
        String str = this.mTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User connected to recommended network: ");
        stringBuilder.append(this.mRecommendedNetwork.SSID);
        Log.d(str, stringBuilder.toString());
        this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 3);
        this.mState = 3;
        this.mHandler.postDelayed(new -$$Lambda$AvailableNetworkNotifier$fIUenOK3XCnkKxNOfiG4FqrVP9c(this), 5000);
    }

    public static /* synthetic */ void lambda$handleWifiConnected$1(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 3) {
            availableNetworkNotifier.clearPendingNotification(true);
        }
    }

    public void handleConnectionFailure() {
        if (this.mState == 2) {
            postNotification(this.mNotificationBuilder.createNetworkFailedNotification(this.mTag));
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User failed to connect to recommended network: ");
            stringBuilder.append(this.mRecommendedNetwork.SSID);
            Log.d(str, stringBuilder.toString());
            this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 4);
            this.mState = 4;
            this.mHandler.postDelayed(new -$$Lambda$AvailableNetworkNotifier$td7RNeapolv8UxSAWhbB9B3dpLc(this), 5000);
        }
    }

    public static /* synthetic */ void lambda$handleConnectionFailure$2(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 4) {
            availableNetworkNotifier.clearPendingNotification(false);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) this.mContext.getSystemService("notification");
    }

    private void postInitialNotification(ScanResult recommendedNetwork) {
        if (this.mRecommendedNetwork == null || !TextUtils.equals(this.mRecommendedNetwork.SSID, recommendedNetwork.SSID)) {
            postNotification(this.mNotificationBuilder.createConnectToAvailableNetworkNotification(this.mTag, recommendedNetwork));
            if (this.mState == 0) {
                this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 1);
            } else {
                this.mWifiMetrics.incrementNumNetworkRecommendationUpdates(this.mTag);
            }
            this.mState = 1;
            this.mRecommendedNetwork = recommendedNetwork;
            this.mNotificationRepeatTime = this.mClock.getWallClockMillis() + this.mNotificationRepeatDelay;
        }
    }

    private void postNotification(Notification notification) {
        getNotificationManager().notify(this.mSystemMessageNotificationId, notification);
    }

    private void handleConnectToNetworkAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 2);
        if (this.mState == 1) {
            postNotification(this.mNotificationBuilder.createNetworkConnectingNotification(this.mTag, this.mRecommendedNetwork));
            this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 2);
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User initiated connection to recommended network: ");
            stringBuilder.append(this.mRecommendedNetwork.SSID);
            Log.d(str, stringBuilder.toString());
            WifiConfiguration network = createRecommendedNetworkConfig(this.mRecommendedNetwork);
            Message msg = Message.obtain();
            msg.what = 151553;
            msg.arg1 = -1;
            msg.obj = network;
            msg.replyTo = this.mSrcMessenger;
            this.mWifiStateMachine.sendMessage(msg);
            this.mState = 2;
            this.mHandler.postDelayed(new -$$Lambda$AvailableNetworkNotifier$oTSUi0JYUdNLPt_-Lt-_qgMDQE8(this), 10000);
        }
    }

    public static /* synthetic */ void lambda$handleConnectToNetworkAction$3(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 2) {
            availableNetworkNotifier.handleConnectionFailure();
        }
    }

    WifiConfiguration createRecommendedNetworkConfig(ScanResult recommendedNetwork) {
        return ScanResultUtil.createNetworkFromScanResult(recommendedNetwork);
    }

    private void handleSeeAllNetworksAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 3);
        startWifiSettings();
    }

    private void startWifiSettings() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        this.mContext.startActivity(new Intent("android.settings.WIFI_SETTINGS").addFlags(268435456));
        clearPendingNotification(false);
    }

    private void handleConnectionAttemptFailedToSend() {
        handleConnectionFailure();
        this.mWifiMetrics.incrementNumNetworkConnectMessageFailedToSend(this.mTag);
    }

    private void handlePickWifiNetworkAfterConnectFailure() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 4);
        startWifiSettings();
    }

    private void handleUserDismissedAction() {
        String str = this.mTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User dismissed notification with state=");
        stringBuilder.append(this.mState);
        Log.d(str, stringBuilder.toString());
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 1);
        if (this.mState == 1) {
            this.mBlacklistedSsids.add(this.mRecommendedNetwork.SSID);
            this.mWifiMetrics.setNetworkRecommenderBlacklistSize(this.mTag, this.mBlacklistedSsids.size());
            this.mConfigManager.saveToStore(false);
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Network is added to the network notification blacklist: ");
            stringBuilder.append(this.mRecommendedNetwork.SSID);
            Log.d(str, stringBuilder.toString());
        }
        resetStateAndDelayNotification();
    }

    private void resetStateAndDelayNotification() {
        this.mState = 0;
        this.mNotificationRepeatTime = System.currentTimeMillis() + this.mNotificationRepeatDelay;
        this.mRecommendedNetwork = null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mTag);
        stringBuilder.append(": ");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSettingEnabled ");
        stringBuilder.append(this.mSettingEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("currentTime: ");
        stringBuilder.append(this.mClock.getWallClockMillis());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNotificationRepeatTime: ");
        stringBuilder.append(this.mNotificationRepeatTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mState: ");
        stringBuilder.append(this.mState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mBlacklistedSsids: ");
        stringBuilder.append(this.mBlacklistedSsids.toString());
        pw.println(stringBuilder.toString());
    }
}
