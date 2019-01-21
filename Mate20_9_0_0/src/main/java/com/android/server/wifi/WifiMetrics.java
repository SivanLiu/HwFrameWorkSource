package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.aware.WifiAwareMetrics;
import com.android.server.wifi.hotspot2.ANQPNetworkKey;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.NetworkDetail.HSRelease;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.PasspointMatch;
import com.android.server.wifi.hotspot2.PasspointProvider;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.nano.WifiMetricsProto.AlertReasonCount;
import com.android.server.wifi.nano.WifiMetricsProto.ConnectToNetworkNotificationAndActionCount;
import com.android.server.wifi.nano.WifiMetricsProto.NumConnectableNetworksBucket;
import com.android.server.wifi.nano.WifiMetricsProto.PnoScanMetrics;
import com.android.server.wifi.nano.WifiMetricsProto.RssiPollCount;
import com.android.server.wifi.nano.WifiMetricsProto.SoftApConnectedClientsEvent;
import com.android.server.wifi.nano.WifiMetricsProto.SoftApReturnCodeCount;
import com.android.server.wifi.nano.WifiMetricsProto.StaEvent;
import com.android.server.wifi.nano.WifiMetricsProto.StaEvent.ConfigInfo;
import com.android.server.wifi.nano.WifiMetricsProto.WifiLog;
import com.android.server.wifi.nano.WifiMetricsProto.WifiLog.ScanReturnEntry;
import com.android.server.wifi.nano.WifiMetricsProto.WifiLog.WifiSystemStateEntry;
import com.android.server.wifi.nano.WifiMetricsProto.WifiScoreCount;
import com.android.server.wifi.nano.WifiMetricsProto.WpsMetrics;
import com.android.server.wifi.rtt.RttMetrics;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiMetrics {
    public static final String CLEAN_DUMP_ARG = "clean";
    private static final int CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER = 1000;
    private static final boolean DBG = false;
    @VisibleForTesting
    static final int LOW_WIFI_SCORE = 50;
    public static final int MAX_CONNECTABLE_BSSID_NETWORK_BUCKET = 50;
    public static final int MAX_CONNECTABLE_SSID_NETWORK_BUCKET = 20;
    private static final int MAX_CONNECTION_EVENTS = 256;
    private static final int MAX_NUM_SOFT_AP_EVENTS = 256;
    public static final int MAX_PASSPOINT_APS_PER_UNIQUE_ESS_BUCKET = 50;
    public static final int MAX_RSSI_DELTA = 127;
    private static final int MAX_RSSI_POLL = 0;
    public static final int MAX_STA_EVENTS = 768;
    public static final int MAX_TOTAL_80211MC_APS_BUCKET = 20;
    public static final int MAX_TOTAL_PASSPOINT_APS_BUCKET = 50;
    public static final int MAX_TOTAL_PASSPOINT_UNIQUE_ESS_BUCKET = 20;
    public static final int MAX_TOTAL_SCAN_RESULTS_BUCKET = 250;
    public static final int MAX_TOTAL_SCAN_RESULT_SSIDS_BUCKET = 100;
    private static final int MAX_WIFI_SCORE = 60;
    public static final int MIN_RSSI_DELTA = -127;
    private static final int MIN_RSSI_POLL = -127;
    private static final int MIN_WIFI_SCORE = 0;
    public static final String PROTO_DUMP_ARG = "wifiMetricsProto";
    private static final int SCREEN_OFF = 0;
    private static final int SCREEN_ON = 1;
    private static final String TAG = "WifiMetrics";
    public static final long TIMEOUT_RSSI_DELTA_MILLIS = 3000;
    private final SparseIntArray mAvailableOpenBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenOrSavedBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenOrSavedSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedPasspointProviderBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedPasspointProviderProfilesInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedSsidsInScanHistogram = new SparseIntArray();
    private Clock mClock;
    private final SparseIntArray mConnectToNetworkNotificationActionCount = new SparseIntArray();
    private final SparseIntArray mConnectToNetworkNotificationCount = new SparseIntArray();
    private final List<ConnectionEvent> mConnectionEventList = new ArrayList();
    private ConnectionEvent mCurrentConnectionEvent;
    private Handler mHandler;
    private boolean mIsMacRandomizationOn = false;
    private boolean mIsWifiNetworksAvailableNotificationOn = false;
    private int mLastPollFreq = -1;
    private int mLastPollLinkSpeed = -1;
    private int mLastPollRssi = -127;
    private int mLastScore = -1;
    private final Object mLock = new Object();
    private int mNumOpenNetworkConnectMessageFailedToSend = 0;
    private int mNumOpenNetworkRecommendationUpdates = 0;
    private final SparseIntArray mObserved80211mcApInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR1ApInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR1ApsPerEssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR1EssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2ApInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2ApsPerEssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2EssInScanHistogram = new SparseIntArray();
    private int mOpenNetworkRecommenderBlacklistSize = 0;
    private PasspointManager mPasspointManager;
    private final PnoScanMetrics mPnoScanMetrics = new PnoScanMetrics();
    private long mRecordStartTimeSec;
    private final SparseIntArray mRssiDeltaCounts = new SparseIntArray();
    private final Map<Integer, SparseIntArray> mRssiPollCountsMap = new HashMap();
    private RttMetrics mRttMetrics;
    private int mScanResultRssi = 0;
    private long mScanResultRssiTimestampMillis = -1;
    private final SparseIntArray mScanReturnEntries = new SparseIntArray();
    private ScoringParams mScoringParams;
    private boolean mScreenOn;
    private final List<SoftApConnectedClientsEvent> mSoftApEventListLocalOnly = new ArrayList();
    private final List<SoftApConnectedClientsEvent> mSoftApEventListTethered = new ArrayList();
    private final SparseIntArray mSoftApManagerReturnCodeCounts = new SparseIntArray();
    private LinkedList<StaEventWithTime> mStaEventList = new LinkedList();
    private int mSupplicantStateChangeBitmask = 0;
    private final SparseIntArray mTotalBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mTotalSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mWifiAlertReasonCounts = new SparseIntArray();
    private WifiAwareMetrics mWifiAwareMetrics;
    private WifiConfigManager mWifiConfigManager;
    private final WifiLog mWifiLogProto = new WifiLog();
    private WifiNetworkSelector mWifiNetworkSelector;
    private WifiPowerMetrics mWifiPowerMetrics = new WifiPowerMetrics();
    private final SparseIntArray mWifiScoreCounts = new SparseIntArray();
    private int mWifiState;
    private final SparseIntArray mWifiSystemStateEntries = new SparseIntArray();
    private final WifiWakeMetrics mWifiWakeMetrics = new WifiWakeMetrics();
    private boolean mWifiWins = false;
    private final WpsMetrics mWpsMetrics = new WpsMetrics();

    /* renamed from: com.android.server.wifi.WifiMetrics$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$net$wifi$SupplicantState = new int[SupplicantState.values().length];

        static {
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INTERFACE_DISABLED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INACTIVE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.SCANNING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.AUTHENTICATING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.FOUR_WAY_HANDSHAKE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.GROUP_HANDSHAKE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.COMPLETED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DORMANT.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.UNINITIALIZED.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INVALID.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    class ConnectionEvent {
        public static final int FAILURE_ASSOCIATION_REJECTION = 2;
        public static final int FAILURE_ASSOCIATION_TIMED_OUT = 11;
        public static final int FAILURE_AUTHENTICATION_FAILURE = 3;
        public static final int FAILURE_CONNECT_NETWORK_FAILED = 5;
        public static final int FAILURE_DHCP = 10;
        public static final int FAILURE_NETWORK_DISCONNECTION = 6;
        public static final int FAILURE_NEW_CONNECTION_ATTEMPT = 7;
        public static final int FAILURE_NONE = 1;
        public static final int FAILURE_REDUNDANT_CONNECTION_ATTEMPT = 8;
        public static final int FAILURE_ROAM_TIMEOUT = 9;
        public static final int FAILURE_SSID_TEMP_DISABLED = 4;
        public static final int FAILURE_UNKNOWN = 0;
        private String mConfigBssid;
        private String mConfigSsid;
        com.android.server.wifi.nano.WifiMetricsProto.ConnectionEvent mConnectionEvent;
        private long mRealEndTime;
        private long mRealStartTime;
        RouterFingerPrint mRouterFingerPrint;
        private boolean mScreenOn;
        private int mWifiState;

        /* synthetic */ ConnectionEvent(WifiMetrics x0, AnonymousClass1 x1) {
            this();
        }

        private ConnectionEvent() {
            this.mConnectionEvent = new com.android.server.wifi.nano.WifiMetricsProto.ConnectionEvent();
            this.mRealEndTime = 0;
            this.mRealStartTime = 0;
            this.mRouterFingerPrint = new RouterFingerPrint();
            this.mConnectionEvent.routerFingerprint = this.mRouterFingerPrint.mRouterFingerPrintProto;
            this.mConfigSsid = "<NULL>";
            this.mConfigBssid = "<NULL>";
            this.mWifiState = 0;
            this.mScreenOn = false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("startTime=");
            Calendar c = Calendar.getInstance();
            synchronized (WifiMetrics.this.mLock) {
                String str;
                c.setTimeInMillis(this.mConnectionEvent.startTimeMillis);
                if (this.mConnectionEvent.startTimeMillis == 0) {
                    str = "            <null>";
                } else {
                    str = String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c});
                }
                sb.append(str);
                sb.append(", SSID=");
                sb.append(this.mConfigSsid);
                sb.append(", BSSID=");
                sb.append(this.mConfigBssid);
                sb.append(", durationMillis=");
                sb.append(this.mConnectionEvent.durationTakenToConnectMillis);
                sb.append(", roamType=");
                switch (this.mConnectionEvent.roamType) {
                    case 1:
                        sb.append("ROAM_NONE");
                        break;
                    case 2:
                        sb.append("ROAM_DBDC");
                        break;
                    case 3:
                        sb.append("ROAM_ENTERPRISE");
                        break;
                    case 4:
                        sb.append("ROAM_USER_SELECTED");
                        break;
                    case 5:
                        sb.append("ROAM_UNRELATED");
                        break;
                    default:
                        sb.append("ROAM_UNKNOWN");
                        break;
                }
                sb.append(", connectionResult=");
                sb.append(this.mConnectionEvent.connectionResult);
                sb.append(", level2FailureCode=");
                switch (this.mConnectionEvent.level2FailureCode) {
                    case 1:
                        sb.append("NONE");
                        break;
                    case 2:
                        sb.append("ASSOCIATION_REJECTION");
                        break;
                    case 3:
                        sb.append("AUTHENTICATION_FAILURE");
                        break;
                    case 4:
                        sb.append("SSID_TEMP_DISABLED");
                        break;
                    case 5:
                        sb.append("CONNECT_NETWORK_FAILED");
                        break;
                    case 6:
                        sb.append("NETWORK_DISCONNECTION");
                        break;
                    case 7:
                        sb.append("NEW_CONNECTION_ATTEMPT");
                        break;
                    case 8:
                        sb.append("REDUNDANT_CONNECTION_ATTEMPT");
                        break;
                    case 9:
                        sb.append("ROAM_TIMEOUT");
                        break;
                    case 10:
                        sb.append("DHCP");
                        break;
                    case 11:
                        sb.append("ASSOCIATION_TIMED_OUT");
                        break;
                    default:
                        sb.append("UNKNOWN");
                        break;
                }
                sb.append(", connectivityLevelFailureCode=");
                switch (this.mConnectionEvent.connectivityLevelFailureCode) {
                    case 1:
                        sb.append("NONE");
                        break;
                    case 2:
                        sb.append("DHCP");
                        break;
                    case 3:
                        sb.append("NO_INTERNET");
                        break;
                    case 4:
                        sb.append("UNWANTED");
                        break;
                    default:
                        sb.append("UNKNOWN");
                        break;
                }
                sb.append(", signalStrength=");
                sb.append(this.mConnectionEvent.signalStrength);
                sb.append(", wifiState=");
                switch (this.mWifiState) {
                    case 1:
                        sb.append("WIFI_DISABLED");
                        break;
                    case 2:
                        sb.append("WIFI_DISCONNECTED");
                        break;
                    case 3:
                        sb.append("WIFI_ASSOCIATED");
                        break;
                    default:
                        sb.append("WIFI_UNKNOWN");
                        break;
                }
                sb.append(", screenOn=");
                sb.append(this.mScreenOn);
                sb.append(". mRouterFingerprint: ");
                sb.append(this.mRouterFingerPrint.toString());
            }
            return sb.toString();
        }
    }

    class RouterFingerPrint {
        private com.android.server.wifi.nano.WifiMetricsProto.RouterFingerPrint mRouterFingerPrintProto = new com.android.server.wifi.nano.WifiMetricsProto.RouterFingerPrint();

        RouterFingerPrint() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            synchronized (WifiMetrics.this.mLock) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mConnectionEvent.roamType=");
                stringBuilder.append(this.mRouterFingerPrintProto.roamType);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mChannelInfo=");
                stringBuilder.append(this.mRouterFingerPrintProto.channelInfo);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mDtim=");
                stringBuilder.append(this.mRouterFingerPrintProto.dtim);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mAuthentication=");
                stringBuilder.append(this.mRouterFingerPrintProto.authentication);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mHidden=");
                stringBuilder.append(this.mRouterFingerPrintProto.hidden);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mRouterTechnology=");
                stringBuilder.append(this.mRouterFingerPrintProto.routerTechnology);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", mSupportsIpv6=");
                stringBuilder.append(this.mRouterFingerPrintProto.supportsIpv6);
                sb.append(stringBuilder.toString());
            }
            return sb.toString();
        }

        /* JADX WARNING: Removed duplicated region for block: B:18:0x007a A:{Catch:{ all -> 0x0080 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void updateFromWifiConfiguration(WifiConfiguration config) {
            synchronized (WifiMetrics.this.mLock) {
                if (config != null) {
                    try {
                        this.mRouterFingerPrintProto.hidden = config.hiddenSSID;
                        if (config.dtimInterval > 0) {
                            this.mRouterFingerPrintProto.dtim = config.dtimInterval;
                        }
                        WifiMetrics.this.mCurrentConnectionEvent.mConfigSsid = config.SSID;
                        ScanResult candidate;
                        if (config.allowedKeyManagement == null || !config.allowedKeyManagement.get(0)) {
                            if (config.isEnterprise()) {
                                WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 3;
                            } else {
                                WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 2;
                            }
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.passpoint = config.isPasspoint();
                            candidate = config.getNetworkSelectionStatus().getCandidate();
                            if (candidate != null) {
                                WifiMetrics.this.updateMetricsFromScanResult(candidate);
                            }
                        } else {
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 1;
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.passpoint = config.isPasspoint();
                            candidate = config.getNetworkSelectionStatus().getCandidate();
                            if (candidate != null) {
                            }
                        }
                    } finally {
                    }
                }
            }
        }
    }

    private static class StaEventWithTime {
        public StaEvent staEvent;
        public long wallClockMillis;

        StaEventWithTime(StaEvent event, long wallClockMillis) {
            this.staEvent = event;
            this.wallClockMillis = wallClockMillis;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Calendar.getInstance().setTimeInMillis(this.wallClockMillis);
            if (this.wallClockMillis != 0) {
                sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c}));
            } else {
                sb.append("                  ");
            }
            sb.append(" ");
            sb.append(WifiMetrics.staEventToString(this.staEvent));
            return sb.toString();
        }
    }

    public WifiMetrics(Clock clock, Looper looper, WifiAwareMetrics awareMetrics, RttMetrics rttMetrics) {
        this.mClock = clock;
        this.mCurrentConnectionEvent = null;
        this.mScreenOn = true;
        this.mWifiState = 1;
        this.mRecordStartTimeSec = this.mClock.getElapsedSinceBootMillis() / 1000;
        this.mWifiAwareMetrics = awareMetrics;
        this.mRttMetrics = rttMetrics;
        this.mHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                synchronized (WifiMetrics.this.mLock) {
                    WifiMetrics.this.processMessage(msg);
                }
            }
        };
    }

    public void setScoringParams(ScoringParams scoringParams) {
        this.mScoringParams = scoringParams;
    }

    public void setWifiConfigManager(WifiConfigManager wifiConfigManager) {
        this.mWifiConfigManager = wifiConfigManager;
    }

    public void setWifiNetworkSelector(WifiNetworkSelector wifiNetworkSelector) {
        this.mWifiNetworkSelector = wifiNetworkSelector;
    }

    public void setPasspointManager(PasspointManager passpointManager) {
        this.mPasspointManager = passpointManager;
    }

    public void incrementPnoScanStartAttempCount() {
        synchronized (this.mLock) {
            PnoScanMetrics pnoScanMetrics = this.mPnoScanMetrics;
            pnoScanMetrics.numPnoScanAttempts++;
        }
    }

    public void incrementPnoScanFailedCount() {
        synchronized (this.mLock) {
            PnoScanMetrics pnoScanMetrics = this.mPnoScanMetrics;
            pnoScanMetrics.numPnoScanFailed++;
        }
    }

    public void incrementPnoScanStartedOverOffloadCount() {
        synchronized (this.mLock) {
            PnoScanMetrics pnoScanMetrics = this.mPnoScanMetrics;
            pnoScanMetrics.numPnoScanStartedOverOffload++;
        }
    }

    public void incrementPnoScanFailedOverOffloadCount() {
        synchronized (this.mLock) {
            PnoScanMetrics pnoScanMetrics = this.mPnoScanMetrics;
            pnoScanMetrics.numPnoScanFailedOverOffload++;
        }
    }

    public void incrementPnoFoundNetworkEventCount() {
        synchronized (this.mLock) {
            PnoScanMetrics pnoScanMetrics = this.mPnoScanMetrics;
            pnoScanMetrics.numPnoFoundNetworkEvents++;
        }
    }

    public void incrementWpsAttemptCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsAttempts++;
        }
    }

    public void incrementWpsSuccessCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsSuccess++;
        }
    }

    public void incrementWpsStartFailureCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsStartFailure++;
        }
    }

    public void incrementWpsOverlapFailureCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsOverlapFailure++;
        }
    }

    public void incrementWpsTimeoutFailureCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsTimeoutFailure++;
        }
    }

    public void incrementWpsOtherConnectionFailureCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsOtherConnectionFailure++;
        }
    }

    public void incrementWpsSupplicantFailureCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsSupplicantFailure++;
        }
    }

    public void incrementWpsCancellationCount() {
        synchronized (this.mLock) {
            WpsMetrics wpsMetrics = this.mWpsMetrics;
            wpsMetrics.numWpsCancellation++;
        }
    }

    public void startConnectionEvent(WifiConfiguration config, String targetBSSID, int roamType) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                if (this.mCurrentConnectionEvent.mConfigSsid == null || this.mCurrentConnectionEvent.mConfigBssid == null || config == null || !this.mCurrentConnectionEvent.mConfigSsid.equals(config.SSID) || !(this.mCurrentConnectionEvent.mConfigBssid.equals("any") || this.mCurrentConnectionEvent.mConfigBssid.equals(targetBSSID))) {
                    endConnectionEvent(7, 1);
                } else {
                    this.mCurrentConnectionEvent.mConfigBssid = targetBSSID;
                    endConnectionEvent(8, 1);
                }
            }
            while (this.mConnectionEventList.size() >= 256) {
                this.mConnectionEventList.remove(0);
            }
            this.mCurrentConnectionEvent = new ConnectionEvent(this, null);
            this.mCurrentConnectionEvent.mConnectionEvent.startTimeMillis = this.mClock.getWallClockMillis();
            this.mCurrentConnectionEvent.mConfigBssid = targetBSSID;
            this.mCurrentConnectionEvent.mConnectionEvent.roamType = roamType;
            this.mCurrentConnectionEvent.mRouterFingerPrint.updateFromWifiConfiguration(config);
            this.mCurrentConnectionEvent.mConfigBssid = "any";
            this.mCurrentConnectionEvent.mRealStartTime = this.mClock.getElapsedSinceBootMillis();
            this.mCurrentConnectionEvent.mWifiState = this.mWifiState;
            this.mCurrentConnectionEvent.mScreenOn = this.mScreenOn;
            this.mConnectionEventList.add(this.mCurrentConnectionEvent);
            this.mScanResultRssiTimestampMillis = -1;
            if (config != null) {
                ScanResult candidate = config.getNetworkSelectionStatus().getCandidate();
                if (candidate != null) {
                    this.mScanResultRssi = candidate.level;
                    this.mScanResultRssiTimestampMillis = this.mClock.getElapsedSinceBootMillis();
                }
            }
        }
    }

    public void setConnectionEventRoamType(int roamType) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                this.mCurrentConnectionEvent.mConnectionEvent.roamType = roamType;
            }
        }
    }

    public void setConnectionScanDetail(ScanDetail scanDetail) {
        synchronized (this.mLock) {
            if (!(this.mCurrentConnectionEvent == null || scanDetail == null)) {
                NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                ScanResult scanResult = scanDetail.getScanResult();
                if (!(networkDetail == null || scanResult == null || this.mCurrentConnectionEvent.mConfigSsid == null)) {
                    String access$200 = this.mCurrentConnectionEvent.mConfigSsid;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(networkDetail.getSSID());
                    stringBuilder.append("\"");
                    if (access$200.equals(stringBuilder.toString())) {
                        updateMetricsFromNetworkDetail(networkDetail);
                        updateMetricsFromScanResult(scanResult);
                    }
                }
            }
        }
    }

    public void endConnectionEvent(int level2FailureCode, int connectivityFailureCode) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                int i = 0;
                boolean result = level2FailureCode == 1 && connectivityFailureCode == 1;
                com.android.server.wifi.nano.WifiMetricsProto.ConnectionEvent connectionEvent = this.mCurrentConnectionEvent.mConnectionEvent;
                if (result) {
                    i = 1;
                }
                connectionEvent.connectionResult = i;
                this.mCurrentConnectionEvent.mRealEndTime = this.mClock.getElapsedSinceBootMillis();
                this.mCurrentConnectionEvent.mConnectionEvent.durationTakenToConnectMillis = (int) (this.mCurrentConnectionEvent.mRealEndTime - this.mCurrentConnectionEvent.mRealStartTime);
                this.mCurrentConnectionEvent.mConnectionEvent.level2FailureCode = level2FailureCode;
                this.mCurrentConnectionEvent.mConnectionEvent.connectivityLevelFailureCode = connectivityFailureCode;
                this.mCurrentConnectionEvent = null;
                if (!result) {
                    this.mScanResultRssiTimestampMillis = -1;
                }
            }
        }
    }

    private void updateMetricsFromNetworkDetail(NetworkDetail networkDetail) {
        int connectionWifiMode;
        int dtimInterval = networkDetail.getDtimInterval();
        if (dtimInterval > 0) {
            this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.dtim = dtimInterval;
        }
        switch (networkDetail.getWifiMode()) {
            case 0:
                connectionWifiMode = 0;
                break;
            case 1:
                connectionWifiMode = 1;
                break;
            case 2:
                connectionWifiMode = 2;
                break;
            case 3:
                connectionWifiMode = 3;
                break;
            case 4:
                connectionWifiMode = 4;
                break;
            case 5:
                connectionWifiMode = 5;
                break;
            default:
                connectionWifiMode = 6;
                break;
        }
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.routerTechnology = connectionWifiMode;
    }

    private void updateMetricsFromScanResult(ScanResult scanResult) {
        this.mCurrentConnectionEvent.mConnectionEvent.signalStrength = scanResult.level;
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 1;
        this.mCurrentConnectionEvent.mConfigBssid = scanResult.BSSID;
        if (scanResult.capabilities != null) {
            if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
                this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 2;
            } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
                this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 2;
            } else if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 3;
            }
        }
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.channelInfo = scanResult.frequency;
    }

    void setIsLocationEnabled(boolean enabled) {
        synchronized (this.mLock) {
            this.mWifiLogProto.isLocationEnabled = enabled;
        }
    }

    void setIsScanningAlwaysEnabled(boolean enabled) {
        synchronized (this.mLock) {
            this.mWifiLogProto.isScanningAlwaysEnabled = enabled;
        }
    }

    public void incrementNonEmptyScanResultCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numNonEmptyScanResults++;
        }
    }

    public void incrementEmptyScanResultCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numEmptyScanResults++;
        }
    }

    public void incrementBackgroundScanCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numBackgroundScans++;
        }
    }

    public int getBackgroundScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numBackgroundScans;
        }
        return i;
    }

    public void incrementOneshotScanCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numOneshotScans++;
        }
        incrementWifiSystemScanStateCount(this.mWifiState, this.mScreenOn);
    }

    public void incrementConnectivityOneshotScanCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numConnectivityOneshotScans++;
        }
    }

    public int getOneshotScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numOneshotScans;
        }
        return i;
    }

    public int getConnectivityOneshotScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numConnectivityOneshotScans;
        }
        return i;
    }

    public void incrementExternalAppOneshotScanRequestsCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numExternalAppOneshotScanRequests++;
        }
    }

    public void incrementExternalForegroundAppOneshotScanRequestsThrottledCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numExternalForegroundAppOneshotScanRequestsThrottled++;
        }
    }

    public void incrementExternalBackgroundAppOneshotScanRequestsThrottledCount() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numExternalBackgroundAppOneshotScanRequestsThrottled++;
        }
    }

    private String returnCodeToString(int scanReturnCode) {
        switch (scanReturnCode) {
            case 0:
                return "SCAN_UNKNOWN";
            case 1:
                return "SCAN_SUCCESS";
            case 2:
                return "SCAN_FAILURE_INTERRUPTED";
            case 3:
                return "SCAN_FAILURE_INVALID_CONFIGURATION";
            case 4:
                return "FAILURE_WIFI_DISABLED";
            default:
                return "<UNKNOWN>";
        }
    }

    public void incrementScanReturnEntry(int scanReturnCode, int countToAdd) {
        synchronized (this.mLock) {
            this.mScanReturnEntries.put(scanReturnCode, this.mScanReturnEntries.get(scanReturnCode) + countToAdd);
        }
    }

    public int getScanReturnEntry(int scanReturnCode) {
        int i;
        synchronized (this.mLock) {
            i = this.mScanReturnEntries.get(scanReturnCode);
        }
        return i;
    }

    private String wifiSystemStateToString(int state) {
        switch (state) {
            case 0:
                return "WIFI_UNKNOWN";
            case 1:
                return "WIFI_DISABLED";
            case 2:
                return "WIFI_DISCONNECTED";
            case 3:
                return "WIFI_ASSOCIATED";
            default:
                return HalDeviceManager.HAL_INSTANCE_NAME;
        }
    }

    public void incrementWifiSystemScanStateCount(int state, boolean screenOn) {
        synchronized (this.mLock) {
            int index = (state * 2) + screenOn;
            this.mWifiSystemStateEntries.put(index, this.mWifiSystemStateEntries.get(index) + 1);
        }
    }

    public int getSystemStateCount(int state, boolean screenOn) {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiSystemStateEntries.get((state * 2) + screenOn);
        }
        return i;
    }

    public void incrementNumLastResortWatchdogTriggers() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogTriggers++;
        }
    }

    public void addCountToNumLastResortWatchdogBadAssociationNetworksTotal(int count) {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogBadAssociationNetworksTotal += count;
        }
    }

    public void addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(int count) {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogBadAuthenticationNetworksTotal += count;
        }
    }

    public void addCountToNumLastResortWatchdogBadDhcpNetworksTotal(int count) {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogBadDhcpNetworksTotal += count;
        }
    }

    public void addCountToNumLastResortWatchdogBadOtherNetworksTotal(int count) {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogBadOtherNetworksTotal += count;
        }
    }

    public void addCountToNumLastResortWatchdogAvailableNetworksTotal(int count) {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogAvailableNetworksTotal += count;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadAssociation() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogTriggersWithBadAssociation++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadAuthentication() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogTriggersWithBadAuthentication++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadDhcp() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogTriggersWithBadDhcp++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadOther() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogTriggersWithBadOther++;
        }
    }

    public void incrementNumConnectivityWatchdogPnoGood() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numConnectivityWatchdogPnoGood++;
        }
    }

    public void incrementNumConnectivityWatchdogPnoBad() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numConnectivityWatchdogPnoBad++;
        }
    }

    public void incrementNumConnectivityWatchdogBackgroundGood() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numConnectivityWatchdogBackgroundGood++;
        }
    }

    public void incrementNumConnectivityWatchdogBackgroundBad() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numConnectivityWatchdogBackgroundBad++;
        }
    }

    public void handlePollResult(WifiInfo wifiInfo) {
        this.mLastPollRssi = wifiInfo.getRssi();
        this.mLastPollLinkSpeed = wifiInfo.getLinkSpeed();
        this.mLastPollFreq = wifiInfo.getFrequency();
        incrementRssiPollRssiCount(this.mLastPollFreq, this.mLastPollRssi);
    }

    @VisibleForTesting
    public void incrementRssiPollRssiCount(int frequency, int rssi) {
        if (rssi >= -127 && rssi <= 0) {
            synchronized (this.mLock) {
                if (!this.mRssiPollCountsMap.containsKey(Integer.valueOf(frequency))) {
                    this.mRssiPollCountsMap.put(Integer.valueOf(frequency), new SparseIntArray());
                }
                SparseIntArray sparseIntArray = (SparseIntArray) this.mRssiPollCountsMap.get(Integer.valueOf(frequency));
                sparseIntArray.put(rssi, sparseIntArray.get(rssi) + 1);
                maybeIncrementRssiDeltaCount(rssi - this.mScanResultRssi);
            }
        }
    }

    private void maybeIncrementRssiDeltaCount(int rssi) {
        if (this.mScanResultRssiTimestampMillis >= 0) {
            if (this.mClock.getElapsedSinceBootMillis() - this.mScanResultRssiTimestampMillis <= TIMEOUT_RSSI_DELTA_MILLIS && rssi >= -127 && rssi <= 127) {
                this.mRssiDeltaCounts.put(rssi, this.mRssiDeltaCounts.get(rssi) + 1);
            }
            this.mScanResultRssiTimestampMillis = -1;
        }
    }

    public void incrementNumLastResortWatchdogSuccesses() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numLastResortWatchdogSuccesses++;
        }
    }

    public void incrementWatchdogTotalConnectionFailureCountAfterTrigger() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.watchdogTotalConnectionFailureCountAfterTrigger++;
        }
    }

    public void setWatchdogSuccessTimeDurationMs(long ms) {
        synchronized (this.mLock) {
            this.mWifiLogProto.watchdogTriggerToConnectionSuccessDurationMs = ms;
        }
    }

    public void incrementAlertReasonCount(int reason) {
        if (reason > 64 || reason < 0) {
            reason = 0;
        }
        synchronized (this.mLock) {
            this.mWifiAlertReasonCounts.put(reason, this.mWifiAlertReasonCounts.get(reason) + 1);
        }
    }

    public void countScanResults(List<ScanDetail> scanDetails) {
        if (scanDetails != null) {
            int totalResults = 0;
            int openNetworks = 0;
            int personalNetworks = 0;
            int enterpriseNetworks = 0;
            int hiddenNetworks = 0;
            int hotspot2r1Networks = 0;
            int hotspot2r2Networks = 0;
            for (ScanDetail scanDetail : scanDetails) {
                NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                ScanResult scanResult = scanDetail.getScanResult();
                totalResults++;
                if (networkDetail != null) {
                    if (networkDetail.isHiddenBeaconFrame()) {
                        hiddenNetworks++;
                    }
                    if (networkDetail.getHSRelease() != null) {
                        if (networkDetail.getHSRelease() == HSRelease.R1) {
                            hotspot2r1Networks++;
                        } else if (networkDetail.getHSRelease() == HSRelease.R2) {
                            hotspot2r2Networks++;
                        }
                    }
                }
                if (!(scanResult == null || scanResult.capabilities == null)) {
                    if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                        enterpriseNetworks++;
                    } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult) || ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
                        personalNetworks++;
                    } else {
                        openNetworks++;
                    }
                }
            }
            synchronized (this.mLock) {
                WifiLog wifiLog = this.mWifiLogProto;
                wifiLog.numTotalScanResults += totalResults;
                wifiLog = this.mWifiLogProto;
                wifiLog.numOpenNetworkScanResults += openNetworks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numPersonalNetworkScanResults += personalNetworks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numEnterpriseNetworkScanResults += enterpriseNetworks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numHiddenNetworkScanResults += hiddenNetworks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numHotspot2R1NetworkScanResults += hotspot2r1Networks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numHotspot2R2NetworkScanResults += hotspot2r2Networks;
                wifiLog = this.mWifiLogProto;
                wifiLog.numScans++;
            }
        }
    }

    public void incrementWifiScoreCount(int score) {
        if (score >= 0 && score <= 60) {
            synchronized (this.mLock) {
                this.mWifiScoreCounts.put(score, this.mWifiScoreCounts.get(score) + 1);
                boolean wifiWins = this.mWifiWins;
                if (this.mWifiWins && score < 50) {
                    wifiWins = false;
                } else if (!this.mWifiWins && score > 50) {
                    wifiWins = true;
                }
                this.mLastScore = score;
                if (wifiWins != this.mWifiWins) {
                    this.mWifiWins = wifiWins;
                    StaEvent event = new StaEvent();
                    event.type = 16;
                    addStaEvent(event);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0037, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void incrementSoftApStartResult(boolean result, int failureCode) {
        synchronized (this.mLock) {
            if (result) {
                try {
                    this.mSoftApManagerReturnCodeCounts.put(1, this.mSoftApManagerReturnCodeCounts.get(1) + 1);
                } catch (Throwable th) {
                }
            } else if (failureCode == 1) {
                this.mSoftApManagerReturnCodeCounts.put(3, this.mSoftApManagerReturnCodeCounts.get(3) + 1);
            } else {
                this.mSoftApManagerReturnCodeCounts.put(2, this.mSoftApManagerReturnCodeCounts.get(2) + 1);
            }
        }
    }

    public void addSoftApUpChangedEvent(boolean isUp, int mode) {
        SoftApConnectedClientsEvent event = new SoftApConnectedClientsEvent();
        event.eventType = isUp ? 0 : 1;
        event.numConnectedClients = 0;
        addSoftApConnectedClientsEvent(event, mode);
    }

    public void addSoftApNumAssociatedStationsChangedEvent(int numStations, int mode) {
        SoftApConnectedClientsEvent event = new SoftApConnectedClientsEvent();
        event.eventType = 2;
        event.numConnectedClients = numStations;
        addSoftApConnectedClientsEvent(event, mode);
    }

    private void addSoftApConnectedClientsEvent(SoftApConnectedClientsEvent event, int mode) {
        synchronized (this.mLock) {
            List<SoftApConnectedClientsEvent> softApEventList;
            switch (mode) {
                case 1:
                    softApEventList = this.mSoftApEventListTethered;
                    break;
                case 2:
                    softApEventList = this.mSoftApEventListLocalOnly;
                    break;
                default:
                    return;
            }
            if (softApEventList.size() > 256) {
                return;
            }
            event.timeStampMillis = this.mClock.getElapsedSinceBootMillis();
            softApEventList.add(event);
        }
    }

    /* JADX WARNING: Missing block: B:17:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addSoftApChannelSwitchedEvent(int frequency, int bandwidth, int mode) {
        synchronized (this.mLock) {
            List<SoftApConnectedClientsEvent> softApEventList;
            switch (mode) {
                case 1:
                    softApEventList = this.mSoftApEventListTethered;
                    break;
                case 2:
                    softApEventList = this.mSoftApEventListLocalOnly;
                    break;
                default:
                    return;
            }
            int index = softApEventList.size() - 1;
            while (index >= 0) {
                SoftApConnectedClientsEvent event = (SoftApConnectedClientsEvent) softApEventList.get(index);
                if (event == null || event.eventType != 0) {
                    index--;
                } else {
                    event.channelFrequency = frequency;
                    event.channelBandwidth = bandwidth;
                }
            }
        }
    }

    public void incrementNumHalCrashes() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numHalCrashes++;
        }
    }

    public void incrementNumWificondCrashes() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numWificondCrashes++;
        }
    }

    public void incrementNumSupplicantCrashes() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSupplicantCrashes++;
        }
    }

    public void incrementNumHostapdCrashes() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numHostapdCrashes++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToHal() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupClientInterfaceFailureDueToHal++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToWificond() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupClientInterfaceFailureDueToWificond++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToSupplicant() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupClientInterfaceFailureDueToSupplicant++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToHal() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupSoftApInterfaceFailureDueToHal++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToWificond() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupSoftApInterfaceFailureDueToWificond++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToHostapd() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSetupSoftApInterfaceFailureDueToHostapd++;
        }
    }

    public void incrementNumClientInterfaceDown() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numClientInterfaceDown++;
        }
    }

    public void incrementNumSoftApInterfaceDown() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSoftApInterfaceDown++;
        }
    }

    public void incrementNumPasspointProviderInstallation() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numPasspointProviderInstallation++;
        }
    }

    public void incrementNumPasspointProviderInstallSuccess() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numPasspointProviderInstallSuccess++;
        }
    }

    public void incrementNumPasspointProviderUninstallation() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numPasspointProviderUninstallation++;
        }
    }

    public void incrementNumPasspointProviderUninstallSuccess() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numPasspointProviderUninstallSuccess++;
        }
    }

    public void incrementNumRadioModeChangeToMcc() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numRadioModeChangeToMcc++;
        }
    }

    public void incrementNumRadioModeChangeToScc() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numRadioModeChangeToScc++;
        }
    }

    public void incrementNumRadioModeChangeToSbs() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numRadioModeChangeToSbs++;
        }
    }

    public void incrementNumRadioModeChangeToDbs() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numRadioModeChangeToDbs++;
        }
    }

    public void incrementNumSoftApUserBandPreferenceUnsatisfied() {
        synchronized (this.mLock) {
            WifiLog wifiLog = this.mWifiLogProto;
            wifiLog.numSoftApUserBandPreferenceUnsatisfied++;
        }
    }

    /* JADX WARNING: Missing block: B:102:0x02c3, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void incrementAvailableNetworksHistograms(List<ScanDetail> scanDetails, boolean isFullBand) {
        boolean validBssid;
        synchronized (this.mLock) {
            if (!(this.mWifiConfigManager == null || this.mWifiNetworkSelector == null)) {
                if (this.mPasspointManager != null) {
                    if (isFullBand) {
                        int openOrSavedBssids;
                        int savedBssids;
                        int openBssids;
                        Set<ScanResultMatchInfo> savedSsids;
                        Set<ScanResultMatchInfo> ssids = new HashSet();
                        Set<ScanResultMatchInfo> openSsids = new HashSet();
                        Set<ScanResultMatchInfo> savedSsids2 = new HashSet();
                        Set<PasspointProvider> savedPasspointProviderProfiles = new HashSet();
                        int passpointR2Aps = 0;
                        Map<ANQPNetworkKey, Integer> passpointR1UniqueEss = new HashMap();
                        Map<ANQPNetworkKey, Integer> passpointR2UniqueEss = new HashMap();
                        Iterator scanDetail = scanDetails.iterator();
                        int savedPasspointProviderBssids = 0;
                        int openOrSavedBssids2 = 0;
                        int savedBssids2 = 0;
                        int openBssids2 = 0;
                        int bssids = 0;
                        int passpointR1Aps = 0;
                        int supporting80211mcAps = 0;
                        while (scanDetail.hasNext()) {
                            int passpointR1Aps2;
                            Set<ScanResultMatchInfo> savedSsids3;
                            PasspointProvider passpointProvider;
                            Iterator it = scanDetail;
                            ScanDetail scanDetail2 = (ScanDetail) scanDetail.next();
                            NetworkDetail networkDetail = scanDetail2.getNetworkDetail();
                            ScanResult scanResult = scanDetail2.getScanResult();
                            Set<PasspointProvider> savedPasspointProviderProfiles2 = savedPasspointProviderProfiles;
                            NetworkDetail networkDetail2 = networkDetail;
                            if (networkDetail2.is80211McResponderSupport()) {
                                supporting80211mcAps++;
                            }
                            int supporting80211mcAps2 = supporting80211mcAps;
                            ScanResult scanResult2 = scanResult;
                            ScanResultMatchInfo matchInfo = ScanResultMatchInfo.fromScanResult(scanResult2);
                            boolean z = false;
                            if (networkDetail2.isInterworking()) {
                                PasspointProvider passpointProvider2;
                                openOrSavedBssids = openOrSavedBssids2;
                                Pair<PasspointProvider, PasspointMatch> providerMatch = this.mPasspointManager.matchProvider(scanResult2);
                                if (providerMatch != null) {
                                    savedBssids = savedBssids2;
                                    passpointProvider2 = (PasspointProvider) providerMatch.first;
                                } else {
                                    savedBssids = savedBssids2;
                                    passpointProvider2 = null;
                                }
                                PasspointProvider passpointProvider3 = passpointProvider2;
                                if (networkDetail2.getHSRelease() == HSRelease.R1) {
                                    passpointR1Aps++;
                                } else if (networkDetail2.getHSRelease() == HSRelease.R2) {
                                    passpointR2Aps++;
                                }
                                savedBssids2 = passpointR1Aps;
                                long bssid = 0;
                                validBssid = false;
                                try {
                                    bssid = Utils.parseMac(scanResult2.BSSID);
                                    openOrSavedBssids2 = 1;
                                    passpointR1Aps2 = savedBssids2;
                                } catch (IllegalArgumentException e) {
                                    String str = TAG;
                                    passpointR1Aps2 = savedBssids2;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    boolean validBssid2 = validBssid;
                                    stringBuilder.append("Invalid BSSID provided in the scan result: ");
                                    stringBuilder.append(scanResult2.BSSID);
                                    Log.e(str, stringBuilder.toString());
                                    openOrSavedBssids2 = validBssid2;
                                }
                                if (openOrSavedBssids2 != 0) {
                                    savedSsids3 = savedSsids2;
                                    openBssids = openBssids2;
                                    ANQPNetworkKey uniqueEss = ANQPNetworkKey.buildKey(scanResult2.SSID, bssid, scanResult2.hessid, networkDetail2.getAnqpDomainID());
                                    Integer savedSsids4;
                                    if (networkDetail2.getHSRelease() == HSRelease.R1) {
                                        savedSsids4 = (Integer) passpointR1UniqueEss.get(uniqueEss);
                                        passpointR1UniqueEss.put(uniqueEss, Integer.valueOf((savedSsids4 == null ? 0 : savedSsids4.intValue()) + 1));
                                    } else if (networkDetail2.getHSRelease() == HSRelease.R2) {
                                        savedSsids4 = (Integer) passpointR2UniqueEss.get(uniqueEss);
                                        passpointR2UniqueEss.put(uniqueEss, Integer.valueOf((savedSsids4 == null ? 0 : savedSsids4.intValue()) + 1));
                                    }
                                } else {
                                    savedSsids3 = savedSsids2;
                                    openBssids = openBssids2;
                                }
                                passpointProvider = passpointProvider3;
                            } else {
                                savedSsids3 = savedSsids2;
                                openBssids = openBssids2;
                                savedBssids = savedBssids2;
                                openOrSavedBssids = openOrSavedBssids2;
                                passpointR1Aps2 = passpointR1Aps;
                                Pair<PasspointProvider, PasspointMatch> pair = null;
                                passpointProvider = null;
                            }
                            if (this.mWifiNetworkSelector.isSignalTooWeak(scanResult2) != null) {
                                scanDetail = it;
                                savedPasspointProviderProfiles = savedPasspointProviderProfiles2;
                                supporting80211mcAps = supporting80211mcAps2;
                                openOrSavedBssids2 = openOrSavedBssids;
                                savedBssids2 = savedBssids;
                                passpointR1Aps = passpointR1Aps2;
                                savedSsids2 = savedSsids3;
                                openBssids2 = openBssids;
                            } else {
                                ScanResultMatchInfo savedSsids5 = matchInfo;
                                ssids.add(savedSsids5);
                                bssids++;
                                boolean isOpen = savedSsids5.networkType == 0;
                                WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetworkForScanDetail(scanDetail2);
                                validBssid = (config == null || config.isEphemeral() || config.isPasspoint()) ? false : true;
                                if (passpointProvider != null) {
                                    z = true;
                                }
                                boolean isSavedPasspoint = z;
                                if (isOpen) {
                                    openSsids.add(savedSsids5);
                                    openBssids++;
                                }
                                if (validBssid) {
                                    savedSsids = savedSsids3;
                                    savedSsids.add(savedSsids5);
                                    savedBssids++;
                                } else {
                                    savedSsids = savedSsids3;
                                }
                                if (isOpen || validBssid) {
                                    openOrSavedBssids++;
                                }
                                if (isSavedPasspoint) {
                                    ScanResultMatchInfo matchInfo2 = savedSsids5;
                                    savedSsids2 = savedPasspointProviderProfiles2;
                                    savedSsids2.add(passpointProvider);
                                    savedPasspointProviderBssids++;
                                } else {
                                    savedSsids2 = savedPasspointProviderProfiles2;
                                }
                                savedPasspointProviderProfiles = savedSsids2;
                                supporting80211mcAps = supporting80211mcAps2;
                                openOrSavedBssids2 = openOrSavedBssids;
                                savedBssids2 = savedBssids;
                                passpointR1Aps = passpointR1Aps2;
                                openBssids2 = openBssids;
                                savedSsids2 = savedSsids;
                                scanDetail = it;
                            }
                        }
                        savedSsids = savedSsids2;
                        openBssids = openBssids2;
                        savedBssids = savedBssids2;
                        Set<PasspointProvider> savedPasspointProviderProfiles3 = savedPasspointProviderProfiles;
                        openOrSavedBssids = openOrSavedBssids2;
                        WifiLog wifiLog = this.mWifiLogProto;
                        wifiLog.fullBandAllSingleScanListenerResults++;
                        incrementTotalScanSsids(this.mTotalSsidsInScanHistogram, ssids.size());
                        incrementTotalScanResults(this.mTotalBssidsInScanHistogram, bssids);
                        incrementSsid(this.mAvailableOpenSsidsInScanHistogram, openSsids.size());
                        incrementBssid(this.mAvailableOpenBssidsInScanHistogram, openBssids);
                        incrementSsid(this.mAvailableSavedSsidsInScanHistogram, savedSsids.size());
                        incrementBssid(this.mAvailableSavedBssidsInScanHistogram, savedBssids);
                        openSsids.addAll(savedSsids);
                        incrementSsid(this.mAvailableOpenOrSavedSsidsInScanHistogram, openSsids.size());
                        incrementBssid(this.mAvailableOpenOrSavedBssidsInScanHistogram, openOrSavedBssids);
                        incrementSsid(this.mAvailableSavedPasspointProviderProfilesInScanHistogram, savedPasspointProviderProfiles3.size());
                        incrementBssid(this.mAvailableSavedPasspointProviderBssidsInScanHistogram, savedPasspointProviderBssids);
                        incrementTotalPasspointAps(this.mObservedHotspotR1ApInScanHistogram, passpointR1Aps);
                        incrementTotalPasspointAps(this.mObservedHotspotR2ApInScanHistogram, passpointR2Aps);
                        incrementTotalUniquePasspointEss(this.mObservedHotspotR1EssInScanHistogram, passpointR1UniqueEss.size());
                        incrementTotalUniquePasspointEss(this.mObservedHotspotR2EssInScanHistogram, passpointR2UniqueEss.size());
                        Iterator it2 = passpointR1UniqueEss.values().iterator();
                        while (it2.hasNext()) {
                            Iterator it3 = it2;
                            Set<ScanResultMatchInfo> ssids2 = ssids;
                            incrementPasspointPerUniqueEss(this.mObservedHotspotR1ApsPerEssInScanHistogram, ((Integer) it2.next()).intValue());
                            it2 = it3;
                            ssids = ssids2;
                        }
                        it2 = passpointR2UniqueEss.values().iterator();
                        while (it2.hasNext()) {
                            Iterator it4 = it2;
                            incrementPasspointPerUniqueEss(this.mObservedHotspotR2ApsPerEssInScanHistogram, ((Integer) it2.next()).intValue());
                            it2 = it4;
                        }
                        increment80211mcAps(this.mObserved80211mcApInScanHistogram, supporting80211mcAps);
                        return;
                    }
                    WifiLog wifiLog2 = this.mWifiLogProto;
                    wifiLog2.partialAllSingleScanListenerResults++;
                }
            }
        }
    }

    public void incrementConnectToNetworkNotification(String notifierTag, int notificationType) {
        synchronized (this.mLock) {
            this.mConnectToNetworkNotificationCount.put(notificationType, this.mConnectToNetworkNotificationCount.get(notificationType) + 1);
        }
    }

    public void incrementConnectToNetworkNotificationAction(String notifierTag, int notificationType, int actionType) {
        synchronized (this.mLock) {
            int key = (notificationType * CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER) + actionType;
            this.mConnectToNetworkNotificationActionCount.put(key, this.mConnectToNetworkNotificationActionCount.get(key) + 1);
        }
    }

    public void setNetworkRecommenderBlacklistSize(String notifierTag, int size) {
        synchronized (this.mLock) {
            this.mOpenNetworkRecommenderBlacklistSize = size;
        }
    }

    public void setIsWifiNetworksAvailableNotificationEnabled(String notifierTag, boolean enabled) {
        synchronized (this.mLock) {
            this.mIsWifiNetworksAvailableNotificationOn = enabled;
        }
    }

    public void incrementNumNetworkRecommendationUpdates(String notifierTag) {
        synchronized (this.mLock) {
            this.mNumOpenNetworkRecommendationUpdates++;
        }
    }

    public void incrementNumNetworkConnectMessageFailedToSend(String notifierTag) {
        synchronized (this.mLock) {
            this.mNumOpenNetworkConnectMessageFailedToSend++;
        }
    }

    public void setIsMacRandomizationOn(boolean enabled) {
        synchronized (this.mLock) {
            this.mIsMacRandomizationOn = enabled;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        FileDescriptor fileDescriptor = fd;
        PrintWriter printWriter = pw;
        String[] strArr = args;
        synchronized (this.mLock) {
            consolidateScoringParams();
            int i = 0;
            if (strArr == null || strArr.length <= 0 || !PROTO_DUMP_ARG.equals(strArr[0])) {
                StringBuilder stringBuilder;
                int i2;
                StringBuilder stringBuilder2;
                StringBuilder stringBuilder3;
                printWriter.println("WifiMetrics:");
                printWriter.println("mConnectionEvents:");
                for (ConnectionEvent event : this.mConnectionEventList) {
                    String eventLine = event.toString();
                    if (event == this.mCurrentConnectionEvent) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(eventLine);
                        stringBuilder4.append("CURRENTLY OPEN EVENT");
                        eventLine = stringBuilder4.toString();
                    }
                    printWriter.println(eventLine);
                }
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numSavedNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numSavedNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numOpenNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numOpenNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numPersonalNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numPersonalNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numEnterpriseNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numEnterpriseNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numHiddenNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numHiddenNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numPasspointNetworks=");
                stringBuilder5.append(this.mWifiLogProto.numPasspointNetworks);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.isLocationEnabled=");
                stringBuilder5.append(this.mWifiLogProto.isLocationEnabled);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.isScanningAlwaysEnabled=");
                stringBuilder5.append(this.mWifiLogProto.isScanningAlwaysEnabled);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numNetworksAddedByUser=");
                stringBuilder5.append(this.mWifiLogProto.numNetworksAddedByUser);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numNetworksAddedByApps=");
                stringBuilder5.append(this.mWifiLogProto.numNetworksAddedByApps);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numNonEmptyScanResults=");
                stringBuilder5.append(this.mWifiLogProto.numNonEmptyScanResults);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numEmptyScanResults=");
                stringBuilder5.append(this.mWifiLogProto.numEmptyScanResults);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numConnecitvityOneshotScans=");
                stringBuilder5.append(this.mWifiLogProto.numConnectivityOneshotScans);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numOneshotScans=");
                stringBuilder5.append(this.mWifiLogProto.numOneshotScans);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numBackgroundScans=");
                stringBuilder5.append(this.mWifiLogProto.numBackgroundScans);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numExternalAppOneshotScanRequests=");
                stringBuilder5.append(this.mWifiLogProto.numExternalAppOneshotScanRequests);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numExternalForegroundAppOneshotScanRequestsThrottled=");
                stringBuilder5.append(this.mWifiLogProto.numExternalForegroundAppOneshotScanRequestsThrottled);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numExternalBackgroundAppOneshotScanRequestsThrottled=");
                stringBuilder5.append(this.mWifiLogProto.numExternalBackgroundAppOneshotScanRequestsThrottled);
                printWriter.println(stringBuilder5.toString());
                printWriter.println("mScanReturnEntries:");
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  SCAN_UNKNOWN: ");
                stringBuilder5.append(getScanReturnEntry(0));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  SCAN_SUCCESS: ");
                stringBuilder5.append(getScanReturnEntry(1));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  SCAN_FAILURE_INTERRUPTED: ");
                stringBuilder5.append(getScanReturnEntry(2));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  SCAN_FAILURE_INVALID_CONFIGURATION: ");
                stringBuilder5.append(getScanReturnEntry(3));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  FAILURE_WIFI_DISABLED: ");
                stringBuilder5.append(getScanReturnEntry(4));
                printWriter.println(stringBuilder5.toString());
                printWriter.println("mSystemStateEntries: <state><screenOn> : <scansInitiated>");
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_UNKNOWN       ON: ");
                stringBuilder5.append(getSystemStateCount(0, true));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_DISABLED      ON: ");
                stringBuilder5.append(getSystemStateCount(1, true));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_DISCONNECTED  ON: ");
                stringBuilder5.append(getSystemStateCount(2, true));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_ASSOCIATED    ON: ");
                stringBuilder5.append(getSystemStateCount(3, true));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_UNKNOWN      OFF: ");
                stringBuilder5.append(getSystemStateCount(0, false));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_DISABLED     OFF: ");
                stringBuilder5.append(getSystemStateCount(1, false));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_DISCONNECTED OFF: ");
                stringBuilder5.append(getSystemStateCount(2, false));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  WIFI_ASSOCIATED   OFF: ");
                stringBuilder5.append(getSystemStateCount(3, false));
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numConnectivityWatchdogPnoGood=");
                stringBuilder5.append(this.mWifiLogProto.numConnectivityWatchdogPnoGood);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numConnectivityWatchdogPnoBad=");
                stringBuilder5.append(this.mWifiLogProto.numConnectivityWatchdogPnoBad);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numConnectivityWatchdogBackgroundGood=");
                stringBuilder5.append(this.mWifiLogProto.numConnectivityWatchdogBackgroundGood);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numConnectivityWatchdogBackgroundBad=");
                stringBuilder5.append(this.mWifiLogProto.numConnectivityWatchdogBackgroundBad);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogTriggers=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogTriggers);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogBadAssociationNetworksTotal=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogBadAssociationNetworksTotal);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogBadAuthenticationNetworksTotal=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogBadAuthenticationNetworksTotal);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogBadDhcpNetworksTotal=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogBadDhcpNetworksTotal);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogBadOtherNetworksTotal=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogBadOtherNetworksTotal);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogAvailableNetworksTotal=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogAvailableNetworksTotal);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogTriggersWithBadAssociation=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAssociation);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogTriggersWithBadAuthentication=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAuthentication);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogTriggersWithBadDhcp=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogTriggersWithBadDhcp);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogTriggersWithBadOther=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogTriggersWithBadOther);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.numLastResortWatchdogSuccesses=");
                stringBuilder5.append(this.mWifiLogProto.numLastResortWatchdogSuccesses);
                printWriter.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("mWifiLogProto.recordDurationSec=");
                stringBuilder5.append((this.mClock.getElapsedSinceBootMillis() / 1000) - this.mRecordStartTimeSec);
                printWriter.println(stringBuilder5.toString());
                try {
                    JSONObject rssiMap = new JSONObject();
                    for (Entry<Integer, SparseIntArray> entry : this.mRssiPollCountsMap.entrySet()) {
                        int frequency = ((Integer) entry.getKey()).intValue();
                        SparseIntArray histogram = (SparseIntArray) entry.getValue();
                        JSONArray histogramElements = new JSONArray();
                        int i3 = -127;
                        while (true) {
                            int i4 = i3;
                            if (i4 > 0) {
                                break;
                            }
                            int count = histogram.get(i4);
                            if (count != 0) {
                                JSONObject histogramElement = new JSONObject();
                                histogramElement.put(Integer.toString(i4), count);
                                histogramElements.put(histogramElement);
                            }
                            i3 = i4 + 1;
                        }
                        rssiMap.put(Integer.toString(frequency), histogramElements);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mWifiLogProto.rssiPollCount: ");
                    stringBuilder.append(rssiMap.toString());
                    printWriter.println(stringBuilder.toString());
                } catch (JSONException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("JSONException occurred: ");
                    stringBuilder.append(e.getMessage());
                    printWriter.println(stringBuilder.toString());
                }
                printWriter.println("mWifiLogProto.rssiPollDeltaCount: Printing counts for [-127, 127]");
                stringBuilder5 = new StringBuilder();
                int i5 = -127;
                while (true) {
                    i2 = i5;
                    if (i2 > 127) {
                        break;
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.mRssiDeltaCounts.get(i2));
                    stringBuilder2.append(" ");
                    stringBuilder5.append(stringBuilder2.toString());
                    i5 = i2 + 1;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(stringBuilder5.toString());
                printWriter.println(stringBuilder.toString());
                printWriter.print("mWifiLogProto.alertReasonCounts=");
                stringBuilder5.setLength(0);
                for (i2 = 0; i2 <= 64; i2++) {
                    int count2 = this.mWifiAlertReasonCounts.get(i2);
                    if (count2 > 0) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("(");
                        stringBuilder3.append(i2);
                        stringBuilder3.append(",");
                        stringBuilder3.append(count2);
                        stringBuilder3.append("),");
                        stringBuilder5.append(stringBuilder3.toString());
                    }
                }
                if (stringBuilder5.length() > 1) {
                    stringBuilder5.setLength(stringBuilder5.length() - 1);
                    printWriter.println(stringBuilder5.toString());
                } else {
                    printWriter.println("()");
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numTotalScanResults=");
                stringBuilder.append(this.mWifiLogProto.numTotalScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numOpenNetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numOpenNetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numPersonalNetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numPersonalNetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numEnterpriseNetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numEnterpriseNetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numHiddenNetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numHiddenNetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numHotspot2R1NetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numHotspot2R1NetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numHotspot2R2NetworkScanResults=");
                stringBuilder.append(this.mWifiLogProto.numHotspot2R2NetworkScanResults);
                printWriter.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("mWifiLogProto.numScans=");
                stringBuilder.append(this.mWifiLogProto.numScans);
                printWriter.println(stringBuilder.toString());
                printWriter.println("mWifiLogProto.WifiScoreCount: [0, 60]");
                while (i <= 60) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mWifiScoreCounts.get(i));
                    stringBuilder.append(" ");
                    printWriter.print(stringBuilder.toString());
                    i++;
                }
                pw.println();
                printWriter.println("mWifiLogProto.SoftApManagerReturnCodeCounts:");
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("  SUCCESS: ");
                stringBuilder6.append(this.mSoftApManagerReturnCodeCounts.get(1));
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("  FAILED_GENERAL_ERROR: ");
                stringBuilder6.append(this.mSoftApManagerReturnCodeCounts.get(2));
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("  FAILED_NO_CHANNEL: ");
                stringBuilder6.append(this.mSoftApManagerReturnCodeCounts.get(3));
                printWriter.println(stringBuilder6.toString());
                printWriter.print("\n");
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numHalCrashes=");
                stringBuilder6.append(this.mWifiLogProto.numHalCrashes);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numWificondCrashes=");
                stringBuilder6.append(this.mWifiLogProto.numWificondCrashes);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSupplicantCrashes=");
                stringBuilder6.append(this.mWifiLogProto.numSupplicantCrashes);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numHostapdCrashes=");
                stringBuilder6.append(this.mWifiLogProto.numHostapdCrashes);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupClientInterfaceFailureDueToHal=");
                stringBuilder6.append(this.mWifiLogProto.numSetupClientInterfaceFailureDueToHal);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupClientInterfaceFailureDueToWificond=");
                stringBuilder6.append(this.mWifiLogProto.numSetupClientInterfaceFailureDueToWificond);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupClientInterfaceFailureDueToSupplicant=");
                stringBuilder6.append(this.mWifiLogProto.numSetupClientInterfaceFailureDueToSupplicant);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupSoftApInterfaceFailureDueToHal=");
                stringBuilder6.append(this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHal);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupSoftApInterfaceFailureDueToWificond=");
                stringBuilder6.append(this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToWificond);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSetupSoftApInterfaceFailureDueToHostapd=");
                stringBuilder6.append(this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHostapd);
                printWriter.println(stringBuilder6.toString());
                printWriter.println("StaEventList:");
                Iterator it = this.mStaEventList.iterator();
                while (it.hasNext()) {
                    printWriter.println((StaEventWithTime) it.next());
                }
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProviders=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProviders);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProviderInstallation=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProviderInstallation);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProviderInstallSuccess=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProviderInstallSuccess);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProviderUninstallation=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProviderUninstallation);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProviderUninstallSuccess=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProviderUninstallSuccess);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numPasspointProvidersSuccessfullyConnected=");
                stringBuilder6.append(this.mWifiLogProto.numPasspointProvidersSuccessfullyConnected);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numRadioModeChangeToMcc=");
                stringBuilder6.append(this.mWifiLogProto.numRadioModeChangeToMcc);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numRadioModeChangeToScc=");
                stringBuilder6.append(this.mWifiLogProto.numRadioModeChangeToScc);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numRadioModeChangeToSbs=");
                stringBuilder6.append(this.mWifiLogProto.numRadioModeChangeToSbs);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numRadioModeChangeToDbs=");
                stringBuilder6.append(this.mWifiLogProto.numRadioModeChangeToDbs);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numSoftApUserBandPreferenceUnsatisfied=");
                stringBuilder6.append(this.mWifiLogProto.numSoftApUserBandPreferenceUnsatisfied);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mTotalSsidsInScanHistogram:");
                stringBuilder6.append(this.mTotalSsidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mTotalBssidsInScanHistogram:");
                stringBuilder6.append(this.mTotalBssidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableOpenSsidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableOpenSsidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableOpenBssidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableOpenBssidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableSavedSsidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableSavedSsidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableSavedBssidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableSavedBssidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableOpenOrSavedSsidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableOpenOrSavedSsidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableOpenOrSavedBssidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableOpenOrSavedBssidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableSavedPasspointProviderProfilesInScanHistogram:");
                stringBuilder6.append(this.mAvailableSavedPasspointProviderProfilesInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAvailableSavedPasspointProviderBssidsInScanHistogram:");
                stringBuilder6.append(this.mAvailableSavedPasspointProviderBssidsInScanHistogram.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.partialAllSingleScanListenerResults=");
                stringBuilder6.append(this.mWifiLogProto.partialAllSingleScanListenerResults);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.fullBandAllSingleScanListenerResults=");
                stringBuilder6.append(this.mWifiLogProto.fullBandAllSingleScanListenerResults);
                printWriter.println(stringBuilder6.toString());
                printWriter.println("mWifiAwareMetrics:");
                this.mWifiAwareMetrics.dump(fileDescriptor, printWriter, strArr);
                printWriter.println("mRttMetrics:");
                this.mRttMetrics.dump(fileDescriptor, printWriter, strArr);
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mPnoScanMetrics.numPnoScanAttempts=");
                stringBuilder6.append(this.mPnoScanMetrics.numPnoScanAttempts);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mPnoScanMetrics.numPnoScanFailed=");
                stringBuilder6.append(this.mPnoScanMetrics.numPnoScanFailed);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mPnoScanMetrics.numPnoScanStartedOverOffload=");
                stringBuilder6.append(this.mPnoScanMetrics.numPnoScanStartedOverOffload);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mPnoScanMetrics.numPnoScanFailedOverOffload=");
                stringBuilder6.append(this.mPnoScanMetrics.numPnoScanFailedOverOffload);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mPnoScanMetrics.numPnoFoundNetworkEvents=");
                stringBuilder6.append(this.mPnoScanMetrics.numPnoFoundNetworkEvents);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.connectToNetworkNotificationCount=");
                stringBuilder6.append(this.mConnectToNetworkNotificationCount.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.connectToNetworkNotificationActionCount=");
                stringBuilder6.append(this.mConnectToNetworkNotificationActionCount.toString());
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.openNetworkRecommenderBlacklistSize=");
                stringBuilder6.append(this.mOpenNetworkRecommenderBlacklistSize);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.isWifiNetworksAvailableNotificationOn=");
                stringBuilder6.append(this.mIsWifiNetworksAvailableNotificationOn);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numOpenNetworkRecommendationUpdates=");
                stringBuilder6.append(this.mNumOpenNetworkRecommendationUpdates);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.numOpenNetworkConnectMessageFailedToSend=");
                stringBuilder6.append(this.mNumOpenNetworkConnectMessageFailedToSend);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR1ApInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR1ApInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR2ApInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR2ApInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR1EssInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR1EssInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR2EssInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR2EssInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR1ApsPerEssInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR1ApsPerEssInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observedHotspotR2ApsPerEssInScanHistogram=");
                stringBuilder6.append(this.mObservedHotspotR2ApsPerEssInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.observed80211mcSupportingApsInScanHistogram");
                stringBuilder6.append(this.mObserved80211mcApInScanHistogram);
                printWriter.println(stringBuilder6.toString());
                printWriter.println("mSoftApTetheredEvents:");
                for (SoftApConnectedClientsEvent event2 : this.mSoftApEventListTethered) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("event_type=");
                    stringBuilder3.append(event2.eventType);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",time_stamp_millis=");
                    stringBuilder3.append(event2.timeStampMillis);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",num_connected_clients=");
                    stringBuilder3.append(event2.numConnectedClients);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",channel_frequency=");
                    stringBuilder3.append(event2.channelFrequency);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",channel_bandwidth=");
                    stringBuilder3.append(event2.channelBandwidth);
                    stringBuilder2.append(stringBuilder3.toString());
                    printWriter.println(stringBuilder2.toString());
                }
                printWriter.println("mSoftApLocalOnlyEvents:");
                for (SoftApConnectedClientsEvent event22 : this.mSoftApEventListLocalOnly) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("event_type=");
                    stringBuilder3.append(event22.eventType);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",time_stamp_millis=");
                    stringBuilder3.append(event22.timeStampMillis);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",num_connected_clients=");
                    stringBuilder3.append(event22.numConnectedClients);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",channel_frequency=");
                    stringBuilder3.append(event22.channelFrequency);
                    stringBuilder2.append(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(",channel_bandwidth=");
                    stringBuilder3.append(event22.channelBandwidth);
                    stringBuilder2.append(stringBuilder3.toString());
                    printWriter.println(stringBuilder2.toString());
                }
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsAttempts=");
                stringBuilder6.append(this.mWpsMetrics.numWpsAttempts);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsSuccess=");
                stringBuilder6.append(this.mWpsMetrics.numWpsSuccess);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsStartFailure=");
                stringBuilder6.append(this.mWpsMetrics.numWpsStartFailure);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsOverlapFailure=");
                stringBuilder6.append(this.mWpsMetrics.numWpsOverlapFailure);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsTimeoutFailure=");
                stringBuilder6.append(this.mWpsMetrics.numWpsTimeoutFailure);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsOtherConnectionFailure=");
                stringBuilder6.append(this.mWpsMetrics.numWpsOtherConnectionFailure);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsSupplicantFailure=");
                stringBuilder6.append(this.mWpsMetrics.numWpsSupplicantFailure);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWpsMetrics.numWpsCancellation=");
                stringBuilder6.append(this.mWpsMetrics.numWpsCancellation);
                printWriter.println(stringBuilder6.toString());
                this.mWifiPowerMetrics.dump(printWriter);
                this.mWifiWakeMetrics.dump(printWriter);
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.isMacRandomizationOn=");
                stringBuilder6.append(this.mIsMacRandomizationOn);
                printWriter.println(stringBuilder6.toString());
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mWifiLogProto.scoreExperimentId=");
                stringBuilder6.append(this.mWifiLogProto.scoreExperimentId);
                printWriter.println(stringBuilder6.toString());
            } else {
                consolidateProto(true);
                for (ConnectionEvent event3 : this.mConnectionEventList) {
                    if (this.mCurrentConnectionEvent != event3) {
                        event3.mConnectionEvent.automaticBugReportTaken = true;
                    }
                }
                i = Base64.encodeToString(WifiLog.toByteArray(this.mWifiLogProto), 0);
                if (strArr.length <= 1 || !CLEAN_DUMP_ARG.equals(strArr[1])) {
                    printWriter.println("WifiMetrics:");
                    printWriter.println(i);
                    printWriter.println("EndWifiMetrics");
                } else {
                    printWriter.print(i);
                }
                clear();
            }
        }
    }

    public void updateSavedNetworks(List<WifiConfiguration> networks) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSavedNetworks = networks.size();
            this.mWifiLogProto.numOpenNetworks = 0;
            this.mWifiLogProto.numPersonalNetworks = 0;
            this.mWifiLogProto.numEnterpriseNetworks = 0;
            this.mWifiLogProto.numNetworksAddedByUser = 0;
            this.mWifiLogProto.numNetworksAddedByApps = 0;
            this.mWifiLogProto.numHiddenNetworks = 0;
            this.mWifiLogProto.numPasspointNetworks = 0;
            for (WifiConfiguration config : networks) {
                WifiLog wifiLog;
                if (config.allowedKeyManagement.get(0)) {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numOpenNetworks++;
                } else if (config.isEnterprise()) {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numEnterpriseNetworks++;
                } else {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numPersonalNetworks++;
                }
                if (config.selfAdded) {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numNetworksAddedByUser++;
                } else {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numNetworksAddedByApps++;
                }
                if (config.hiddenSSID) {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numHiddenNetworks++;
                }
                if (config.isPasspoint()) {
                    wifiLog = this.mWifiLogProto;
                    wifiLog.numPasspointNetworks++;
                }
            }
        }
    }

    public void updateSavedPasspointProfiles(int numSavedProfiles, int numConnectedProfiles) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviders = numSavedProfiles;
            this.mWifiLogProto.numPasspointProvidersSuccessfullyConnected = numConnectedProfiles;
        }
    }

    private void consolidateProto(boolean incremental) {
        ArrayList events = new ArrayList();
        ArrayList rssis = new ArrayList();
        ArrayList rssiDeltas = new ArrayList();
        ArrayList alertReasons = new ArrayList();
        ArrayList scores = new ArrayList();
        synchronized (this.mLock) {
            int i;
            int frequency;
            int i2;
            int i3;
            for (ConnectionEvent event : this.mConnectionEventList) {
                if (!(incremental && (this.mCurrentConnectionEvent == event || event.mConnectionEvent.automaticBugReportTaken))) {
                    events.add(event.mConnectionEvent);
                    if (incremental) {
                        event.mConnectionEvent.automaticBugReportTaken = true;
                    }
                }
            }
            if (events.size() > 0) {
                this.mWifiLogProto.connectionEvent = (com.android.server.wifi.nano.WifiMetricsProto.ConnectionEvent[]) events.toArray(this.mWifiLogProto.connectionEvent);
            }
            this.mWifiLogProto.scanReturnEntries = new ScanReturnEntry[this.mScanReturnEntries.size()];
            for (i = 0; i < this.mScanReturnEntries.size(); i++) {
                this.mWifiLogProto.scanReturnEntries[i] = new ScanReturnEntry();
                this.mWifiLogProto.scanReturnEntries[i].scanReturnCode = this.mScanReturnEntries.keyAt(i);
                this.mWifiLogProto.scanReturnEntries[i].scanResultsCount = this.mScanReturnEntries.valueAt(i);
            }
            this.mWifiLogProto.wifiSystemStateEntries = new WifiSystemStateEntry[this.mWifiSystemStateEntries.size()];
            for (i = 0; i < this.mWifiSystemStateEntries.size(); i++) {
                this.mWifiLogProto.wifiSystemStateEntries[i] = new WifiSystemStateEntry();
                this.mWifiLogProto.wifiSystemStateEntries[i].wifiState = this.mWifiSystemStateEntries.keyAt(i) / 2;
                this.mWifiLogProto.wifiSystemStateEntries[i].wifiStateCount = this.mWifiSystemStateEntries.valueAt(i);
                this.mWifiLogProto.wifiSystemStateEntries[i].isScreenOn = this.mWifiSystemStateEntries.keyAt(i) % 2 > 0;
            }
            this.mWifiLogProto.recordDurationSec = (int) ((this.mClock.getElapsedSinceBootMillis() / 1000) - this.mRecordStartTimeSec);
            for (Entry<Integer, SparseIntArray> entry : this.mRssiPollCountsMap.entrySet()) {
                frequency = ((Integer) entry.getKey()).intValue();
                SparseIntArray histogram = (SparseIntArray) entry.getValue();
                for (i2 = 0; i2 < histogram.size(); i2++) {
                    RssiPollCount keyVal = new RssiPollCount();
                    keyVal.rssi = histogram.keyAt(i2);
                    keyVal.count = histogram.valueAt(i2);
                    keyVal.frequency = frequency;
                    rssis.add(keyVal);
                }
            }
            this.mWifiLogProto.rssiPollRssiCount = (RssiPollCount[]) rssis.toArray(this.mWifiLogProto.rssiPollRssiCount);
            for (i3 = 0; i3 < this.mRssiDeltaCounts.size(); i3++) {
                RssiPollCount keyVal2 = new RssiPollCount();
                keyVal2.rssi = this.mRssiDeltaCounts.keyAt(i3);
                keyVal2.count = this.mRssiDeltaCounts.valueAt(i3);
                rssiDeltas.add(keyVal2);
            }
            this.mWifiLogProto.rssiPollDeltaCount = (RssiPollCount[]) rssiDeltas.toArray(this.mWifiLogProto.rssiPollDeltaCount);
            for (i3 = 0; i3 < this.mWifiAlertReasonCounts.size(); i3++) {
                AlertReasonCount keyVal3 = new AlertReasonCount();
                keyVal3.reason = this.mWifiAlertReasonCounts.keyAt(i3);
                keyVal3.count = this.mWifiAlertReasonCounts.valueAt(i3);
                alertReasons.add(keyVal3);
            }
            this.mWifiLogProto.alertReasonCount = (AlertReasonCount[]) alertReasons.toArray(this.mWifiLogProto.alertReasonCount);
            for (i3 = 0; i3 < this.mWifiScoreCounts.size(); i3++) {
                WifiScoreCount keyVal4 = new WifiScoreCount();
                keyVal4.score = this.mWifiScoreCounts.keyAt(i3);
                keyVal4.count = this.mWifiScoreCounts.valueAt(i3);
                scores.add(keyVal4);
            }
            this.mWifiLogProto.wifiScoreCount = (WifiScoreCount[]) scores.toArray(this.mWifiLogProto.wifiScoreCount);
            i3 = this.mSoftApManagerReturnCodeCounts.size();
            this.mWifiLogProto.softApReturnCode = new SoftApReturnCodeCount[i3];
            for (i = 0; i < i3; i++) {
                this.mWifiLogProto.softApReturnCode[i] = new SoftApReturnCodeCount();
                this.mWifiLogProto.softApReturnCode[i].startResult = this.mSoftApManagerReturnCodeCounts.keyAt(i);
                this.mWifiLogProto.softApReturnCode[i].count = this.mSoftApManagerReturnCodeCounts.valueAt(i);
            }
            this.mWifiLogProto.staEventList = new StaEvent[this.mStaEventList.size()];
            for (i = 0; i < this.mStaEventList.size(); i++) {
                this.mWifiLogProto.staEventList[i] = ((StaEventWithTime) this.mStaEventList.get(i)).staEvent;
            }
            this.mWifiLogProto.totalSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mTotalSsidsInScanHistogram);
            this.mWifiLogProto.totalBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mTotalBssidsInScanHistogram);
            this.mWifiLogProto.availableOpenSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenSsidsInScanHistogram);
            this.mWifiLogProto.availableOpenBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenBssidsInScanHistogram);
            this.mWifiLogProto.availableSavedSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedSsidsInScanHistogram);
            this.mWifiLogProto.availableSavedBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedBssidsInScanHistogram);
            this.mWifiLogProto.availableOpenOrSavedSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenOrSavedSsidsInScanHistogram);
            this.mWifiLogProto.availableOpenOrSavedBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenOrSavedBssidsInScanHistogram);
            this.mWifiLogProto.availableSavedPasspointProviderProfilesInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedPasspointProviderProfilesInScanHistogram);
            this.mWifiLogProto.availableSavedPasspointProviderBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedPasspointProviderBssidsInScanHistogram);
            this.mWifiLogProto.wifiAwareLog = this.mWifiAwareMetrics.consolidateProto();
            this.mWifiLogProto.wifiRttLog = this.mRttMetrics.consolidateProto();
            this.mWifiLogProto.pnoScanMetrics = this.mPnoScanMetrics;
            ConnectToNetworkNotificationAndActionCount[] notificationCountArray = new ConnectToNetworkNotificationAndActionCount[this.mConnectToNetworkNotificationCount.size()];
            for (int i4 = 0; i4 < this.mConnectToNetworkNotificationCount.size(); i4++) {
                ConnectToNetworkNotificationAndActionCount keyVal5 = new ConnectToNetworkNotificationAndActionCount();
                keyVal5.notification = this.mConnectToNetworkNotificationCount.keyAt(i4);
                keyVal5.recommender = 1;
                keyVal5.count = this.mConnectToNetworkNotificationCount.valueAt(i4);
                notificationCountArray[i4] = keyVal5;
            }
            this.mWifiLogProto.connectToNetworkNotificationCount = notificationCountArray;
            ConnectToNetworkNotificationAndActionCount[] notificationActionCountArray = new ConnectToNetworkNotificationAndActionCount[this.mConnectToNetworkNotificationActionCount.size()];
            int i5 = 0;
            while (true) {
                frequency = i5;
                if (frequency >= this.mConnectToNetworkNotificationActionCount.size()) {
                    break;
                }
                ConnectToNetworkNotificationAndActionCount keyVal6 = new ConnectToNetworkNotificationAndActionCount();
                i2 = this.mConnectToNetworkNotificationActionCount.keyAt(frequency);
                keyVal6.notification = i2 / CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER;
                keyVal6.action = i2 % CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER;
                keyVal6.recommender = 1;
                keyVal6.count = this.mConnectToNetworkNotificationActionCount.valueAt(frequency);
                notificationActionCountArray[frequency] = keyVal6;
                i5 = frequency + 1;
            }
            this.mWifiLogProto.connectToNetworkNotificationActionCount = notificationActionCountArray;
            this.mWifiLogProto.openNetworkRecommenderBlacklistSize = this.mOpenNetworkRecommenderBlacklistSize;
            this.mWifiLogProto.isWifiNetworksAvailableNotificationOn = this.mIsWifiNetworksAvailableNotificationOn;
            this.mWifiLogProto.numOpenNetworkRecommendationUpdates = this.mNumOpenNetworkRecommendationUpdates;
            this.mWifiLogProto.numOpenNetworkConnectMessageFailedToSend = this.mNumOpenNetworkConnectMessageFailedToSend;
            this.mWifiLogProto.observedHotspotR1ApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1ApInScanHistogram);
            this.mWifiLogProto.observedHotspotR2ApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2ApInScanHistogram);
            this.mWifiLogProto.observedHotspotR1EssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1EssInScanHistogram);
            this.mWifiLogProto.observedHotspotR2EssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2EssInScanHistogram);
            this.mWifiLogProto.observedHotspotR1ApsPerEssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1ApsPerEssInScanHistogram);
            this.mWifiLogProto.observedHotspotR2ApsPerEssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2ApsPerEssInScanHistogram);
            this.mWifiLogProto.observed80211McSupportingApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObserved80211mcApInScanHistogram);
            if (this.mSoftApEventListTethered.size() > 0) {
                this.mWifiLogProto.softApConnectedClientsEventsTethered = (SoftApConnectedClientsEvent[]) this.mSoftApEventListTethered.toArray(this.mWifiLogProto.softApConnectedClientsEventsTethered);
            }
            if (this.mSoftApEventListLocalOnly.size() > 0) {
                this.mWifiLogProto.softApConnectedClientsEventsLocalOnly = (SoftApConnectedClientsEvent[]) this.mSoftApEventListLocalOnly.toArray(this.mWifiLogProto.softApConnectedClientsEventsLocalOnly);
            }
            this.mWifiLogProto.wpsMetrics = this.mWpsMetrics;
            this.mWifiLogProto.wifiPowerStats = this.mWifiPowerMetrics.buildProto();
            this.mWifiLogProto.wifiWakeStats = this.mWifiWakeMetrics.buildProto();
            this.mWifiLogProto.isMacRandomizationOn = this.mIsMacRandomizationOn;
        }
    }

    private void consolidateScoringParams() {
        synchronized (this.mLock) {
            if (this.mScoringParams != null) {
                int experimentIdentifier = this.mScoringParams.getExperimentIdentifier();
                if (experimentIdentifier == 0) {
                    this.mWifiLogProto.scoreExperimentId = "";
                } else {
                    WifiLog wifiLog = this.mWifiLogProto;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("x");
                    stringBuilder.append(experimentIdentifier);
                    wifiLog.scoreExperimentId = stringBuilder.toString();
                }
            }
        }
    }

    private NumConnectableNetworksBucket[] makeNumConnectableNetworksBucketArray(SparseIntArray sia) {
        NumConnectableNetworksBucket[] array = new NumConnectableNetworksBucket[sia.size()];
        for (int i = 0; i < sia.size(); i++) {
            NumConnectableNetworksBucket keyVal = new NumConnectableNetworksBucket();
            keyVal.numConnectableNetworks = sia.keyAt(i);
            keyVal.count = sia.valueAt(i);
            array[i] = keyVal;
        }
        return array;
    }

    private void clear() {
        synchronized (this.mLock) {
            this.mConnectionEventList.clear();
            if (this.mCurrentConnectionEvent != null) {
                this.mConnectionEventList.add(this.mCurrentConnectionEvent);
            }
            this.mScanReturnEntries.clear();
            this.mWifiSystemStateEntries.clear();
            this.mRecordStartTimeSec = this.mClock.getElapsedSinceBootMillis() / 1000;
            this.mRssiPollCountsMap.clear();
            this.mRssiDeltaCounts.clear();
            this.mWifiAlertReasonCounts.clear();
            this.mWifiScoreCounts.clear();
            this.mWifiLogProto.clear();
            this.mScanResultRssiTimestampMillis = -1;
            this.mSoftApManagerReturnCodeCounts.clear();
            this.mStaEventList.clear();
            this.mWifiAwareMetrics.clear();
            this.mRttMetrics.clear();
            this.mTotalSsidsInScanHistogram.clear();
            this.mTotalBssidsInScanHistogram.clear();
            this.mAvailableOpenSsidsInScanHistogram.clear();
            this.mAvailableOpenBssidsInScanHistogram.clear();
            this.mAvailableSavedSsidsInScanHistogram.clear();
            this.mAvailableSavedBssidsInScanHistogram.clear();
            this.mAvailableOpenOrSavedSsidsInScanHistogram.clear();
            this.mAvailableOpenOrSavedBssidsInScanHistogram.clear();
            this.mAvailableSavedPasspointProviderProfilesInScanHistogram.clear();
            this.mAvailableSavedPasspointProviderBssidsInScanHistogram.clear();
            this.mPnoScanMetrics.clear();
            this.mConnectToNetworkNotificationCount.clear();
            this.mConnectToNetworkNotificationActionCount.clear();
            this.mNumOpenNetworkRecommendationUpdates = 0;
            this.mNumOpenNetworkConnectMessageFailedToSend = 0;
            this.mObservedHotspotR1ApInScanHistogram.clear();
            this.mObservedHotspotR2ApInScanHistogram.clear();
            this.mObservedHotspotR1EssInScanHistogram.clear();
            this.mObservedHotspotR2EssInScanHistogram.clear();
            this.mObservedHotspotR1ApsPerEssInScanHistogram.clear();
            this.mObservedHotspotR2ApsPerEssInScanHistogram.clear();
            this.mSoftApEventListTethered.clear();
            this.mSoftApEventListLocalOnly.clear();
            this.mWpsMetrics.clear();
            this.mWifiWakeMetrics.clear();
            this.mObserved80211mcApInScanHistogram.clear();
        }
    }

    public void setScreenState(boolean screenOn) {
        synchronized (this.mLock) {
            this.mScreenOn = screenOn;
        }
    }

    public void setWifiState(int wifiState) {
        synchronized (this.mLock) {
            this.mWifiState = wifiState;
            this.mWifiWins = wifiState == 3;
        }
    }

    private void processMessage(Message msg) {
        StaEvent event = new StaEvent();
        boolean logEvent = true;
        boolean z = false;
        switch (msg.what) {
            case 131213:
                event.type = 10;
                break;
            case 131219:
                event.type = 6;
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                event.type = 3;
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                event.type = 4;
                event.reason = msg.arg2;
                if (msg.arg1 != 0) {
                    z = true;
                }
                event.localGen = z;
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                logEvent = false;
                this.mSupplicantStateChangeBitmask |= supplicantStateToBit(msg.obj.state);
                break;
            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT /*147463*/:
                event.type = 2;
                switch (msg.arg1) {
                    case 0:
                        event.authFailureReason = 1;
                        break;
                    case 1:
                        event.authFailureReason = 2;
                        break;
                    case 2:
                        event.authFailureReason = 3;
                        break;
                    case 3:
                        event.authFailureReason = 4;
                        break;
                }
                break;
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                event.type = 1;
                if (msg.arg1 > 0) {
                    z = true;
                }
                event.associationTimedOut = z;
                event.status = msg.arg2;
                break;
            default:
                return;
        }
        if (logEvent) {
            addStaEvent(event);
        }
    }

    public void logStaEvent(int type) {
        logStaEvent(type, 0, null);
    }

    public void logStaEvent(int type, WifiConfiguration config) {
        logStaEvent(type, 0, config);
    }

    public void logStaEvent(int type, int frameworkDisconnectReason) {
        logStaEvent(type, frameworkDisconnectReason, null);
    }

    public void logStaEvent(int type, int frameworkDisconnectReason, WifiConfiguration config) {
        switch (type) {
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                StaEvent event = new StaEvent();
                event.type = type;
                if (frameworkDisconnectReason != 0) {
                    event.frameworkDisconnectReason = frameworkDisconnectReason;
                }
                event.configInfo = createConfigInfo(config);
                addStaEvent(event);
                return;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown StaEvent:");
                stringBuilder.append(type);
                Log.e(str, stringBuilder.toString());
                return;
        }
    }

    private void addStaEvent(StaEvent staEvent) {
        staEvent.startTimeMillis = this.mClock.getElapsedSinceBootMillis();
        staEvent.lastRssi = this.mLastPollRssi;
        staEvent.lastFreq = this.mLastPollFreq;
        staEvent.lastLinkSpeed = this.mLastPollLinkSpeed;
        staEvent.supplicantStateChangesBitmask = this.mSupplicantStateChangeBitmask;
        staEvent.lastScore = this.mLastScore;
        this.mSupplicantStateChangeBitmask = 0;
        this.mLastPollRssi = -127;
        this.mLastPollFreq = -1;
        this.mLastPollLinkSpeed = -1;
        this.mLastScore = -1;
        this.mStaEventList.add(new StaEventWithTime(staEvent, this.mClock.getWallClockMillis()));
        if (this.mStaEventList.size() > MAX_STA_EVENTS) {
            this.mStaEventList.remove();
        }
    }

    private ConfigInfo createConfigInfo(WifiConfiguration config) {
        if (config == null) {
            return null;
        }
        ConfigInfo info = new ConfigInfo();
        info.allowedKeyManagement = bitSetToInt(config.allowedKeyManagement);
        info.allowedProtocols = bitSetToInt(config.allowedProtocols);
        info.allowedAuthAlgorithms = bitSetToInt(config.allowedAuthAlgorithms);
        info.allowedPairwiseCiphers = bitSetToInt(config.allowedPairwiseCiphers);
        info.allowedGroupCiphers = bitSetToInt(config.allowedGroupCiphers);
        info.hiddenSsid = config.hiddenSSID;
        info.isPasspoint = config.isPasspoint();
        info.isEphemeral = config.isEphemeral();
        info.hasEverConnected = config.getNetworkSelectionStatus().getHasEverConnected();
        ScanResult candidate = config.getNetworkSelectionStatus().getCandidate();
        if (candidate != null) {
            info.scanRssi = candidate.level;
            info.scanFreq = candidate.frequency;
        }
        return info;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public WifiAwareMetrics getWifiAwareMetrics() {
        return this.mWifiAwareMetrics;
    }

    public WifiWakeMetrics getWakeupMetrics() {
        return this.mWifiWakeMetrics;
    }

    public RttMetrics getRttMetrics() {
        return this.mRttMetrics;
    }

    public static int supplicantStateToBit(SupplicantState state) {
        switch (AnonymousClass2.$SwitchMap$android$net$wifi$SupplicantState[state.ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 8;
            case 5:
                return 16;
            case 6:
                return 32;
            case 7:
                return 64;
            case 8:
                return 128;
            case 9:
                return 256;
            case 10:
                return 512;
            case 11:
                return 1024;
            case 12:
                return 2048;
            case 13:
                return 4096;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got unknown supplicant state: ");
                stringBuilder.append(state.ordinal());
                Log.wtf(str, stringBuilder.toString());
                return 0;
        }
    }

    private static String supplicantStateChangesBitmaskToString(int mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("supplicantStateChangeEvents: {");
        if ((mask & 1) > 0) {
            sb.append(" DISCONNECTED");
        }
        if ((mask & 2) > 0) {
            sb.append(" INTERFACE_DISABLED");
        }
        if ((mask & 4) > 0) {
            sb.append(" INACTIVE");
        }
        if ((mask & 8) > 0) {
            sb.append(" SCANNING");
        }
        if ((mask & 16) > 0) {
            sb.append(" AUTHENTICATING");
        }
        if ((mask & 32) > 0) {
            sb.append(" ASSOCIATING");
        }
        if ((mask & 64) > 0) {
            sb.append(" ASSOCIATED");
        }
        if ((mask & 128) > 0) {
            sb.append(" FOUR_WAY_HANDSHAKE");
        }
        if ((mask & 256) > 0) {
            sb.append(" GROUP_HANDSHAKE");
        }
        if ((mask & 512) > 0) {
            sb.append(" COMPLETED");
        }
        if ((mask & 1024) > 0) {
            sb.append(" DORMANT");
        }
        if ((mask & 2048) > 0) {
            sb.append(" UNINITIALIZED");
        }
        if ((mask & 4096) > 0) {
            sb.append(" INVALID");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String staEventToString(StaEvent event) {
        if (event == null) {
            return "<NULL>";
        }
        StringBuilder sb = new StringBuilder();
        switch (event.type) {
            case 1:
                sb.append("ASSOCIATION_REJECTION_EVENT");
                sb.append(" timedOut=");
                sb.append(event.associationTimedOut);
                sb.append(" status=");
                sb.append(event.status);
                sb.append(":");
                sb.append(StatusCode.toString(event.status));
                break;
            case 2:
                sb.append("AUTHENTICATION_FAILURE_EVENT reason=");
                sb.append(event.authFailureReason);
                sb.append(":");
                sb.append(authFailureReasonToString(event.authFailureReason));
                break;
            case 3:
                sb.append("NETWORK_CONNECTION_EVENT");
                break;
            case 4:
                sb.append("NETWORK_DISCONNECTION_EVENT");
                sb.append(" local_gen=");
                sb.append(event.localGen);
                sb.append(" reason=");
                sb.append(event.reason);
                sb.append(":");
                sb.append(ReasonCode.toString(event.reason >= 0 ? event.reason : event.reason * -1));
                break;
            case 6:
                sb.append("CMD_ASSOCIATED_BSSID");
                break;
            case 7:
                sb.append("CMD_IP_CONFIGURATION_SUCCESSFUL");
                break;
            case 8:
                sb.append("CMD_IP_CONFIGURATION_LOST");
                break;
            case 9:
                sb.append("CMD_IP_REACHABILITY_LOST");
                break;
            case 10:
                sb.append("CMD_TARGET_BSSID");
                break;
            case 11:
                sb.append("CMD_START_CONNECT");
                break;
            case 12:
                sb.append("CMD_START_ROAM");
                break;
            case 13:
                sb.append("CONNECT_NETWORK");
                break;
            case 14:
                sb.append("NETWORK_AGENT_VALID_NETWORK");
                break;
            case 15:
                sb.append("FRAMEWORK_DISCONNECT");
                sb.append(" reason=");
                sb.append(frameworkDisconnectReasonToString(event.frameworkDisconnectReason));
                break;
            case 16:
                sb.append("SCORE_BREACH");
                break;
            case 17:
                sb.append("MAC_CHANGE");
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UNKNOWN ");
                stringBuilder.append(event.type);
                stringBuilder.append(":");
                sb.append(stringBuilder.toString());
                break;
        }
        if (event.lastRssi != -127) {
            sb.append(" lastRssi=");
            sb.append(event.lastRssi);
        }
        if (event.lastFreq != -1) {
            sb.append(" lastFreq=");
            sb.append(event.lastFreq);
        }
        if (event.lastLinkSpeed != -1) {
            sb.append(" lastLinkSpeed=");
            sb.append(event.lastLinkSpeed);
        }
        if (event.lastScore != -1) {
            sb.append(" lastScore=");
            sb.append(event.lastScore);
        }
        if (event.supplicantStateChangesBitmask != 0) {
            sb.append(", ");
            sb.append(supplicantStateChangesBitmaskToString(event.supplicantStateChangesBitmask));
        }
        if (event.configInfo != null) {
            sb.append(", ");
            sb.append(configInfoToString(event.configInfo));
        }
        return sb.toString();
    }

    private static String authFailureReasonToString(int authFailureReason) {
        switch (authFailureReason) {
            case 1:
                return "ERROR_AUTH_FAILURE_NONE";
            case 2:
                return "ERROR_AUTH_FAILURE_TIMEOUT";
            case 3:
                return "ERROR_AUTH_FAILURE_WRONG_PSWD";
            case 4:
                return "ERROR_AUTH_FAILURE_EAP_FAILURE";
            default:
                return "";
        }
    }

    private static String frameworkDisconnectReasonToString(int frameworkDisconnectReason) {
        switch (frameworkDisconnectReason) {
            case 1:
                return "DISCONNECT_API";
            case 2:
                return "DISCONNECT_GENERIC";
            case 3:
                return "DISCONNECT_UNWANTED";
            case 4:
                return "DISCONNECT_ROAM_WATCHDOG_TIMER";
            case 5:
                return "DISCONNECT_P2P_DISCONNECT_WIFI_REQUEST";
            case 6:
                return "DISCONNECT_RESET_SIM_NETWORKS";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DISCONNECT_UNKNOWN=");
                stringBuilder.append(frameworkDisconnectReason);
                return stringBuilder.toString();
        }
    }

    private static String configInfoToString(ConfigInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigInfo:");
        sb.append(" allowed_key_management=");
        sb.append(info.allowedKeyManagement);
        sb.append(" allowed_protocols=");
        sb.append(info.allowedProtocols);
        sb.append(" allowed_auth_algorithms=");
        sb.append(info.allowedAuthAlgorithms);
        sb.append(" allowed_pairwise_ciphers=");
        sb.append(info.allowedPairwiseCiphers);
        sb.append(" allowed_group_ciphers=");
        sb.append(info.allowedGroupCiphers);
        sb.append(" hidden_ssid=");
        sb.append(info.hiddenSsid);
        sb.append(" is_passpoint=");
        sb.append(info.isPasspoint);
        sb.append(" is_ephemeral=");
        sb.append(info.isEphemeral);
        sb.append(" has_ever_connected=");
        sb.append(info.hasEverConnected);
        sb.append(" scan_rssi=");
        sb.append(info.scanRssi);
        sb.append(" scan_freq=");
        sb.append(info.scanFreq);
        return sb.toString();
    }

    private static int bitSetToInt(BitSet bits) {
        int i = 31;
        if (bits.length() < 31) {
            i = bits.length();
        }
        int value = 0;
        int i2 = 0;
        while (i2 < i) {
            value += bits.get(i2) ? 1 << i2 : 0;
            i2++;
        }
        return value;
    }

    private void incrementSsid(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 20));
    }

    private void incrementBssid(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 50));
    }

    private void incrementTotalScanResults(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, MAX_TOTAL_SCAN_RESULTS_BUCKET));
    }

    private void incrementTotalScanSsids(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 100));
    }

    private void incrementTotalPasspointAps(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 50));
    }

    private void incrementTotalUniquePasspointEss(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 20));
    }

    private void incrementPasspointPerUniqueEss(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 50));
    }

    private void increment80211mcAps(SparseIntArray sia, int element) {
        increment(sia, Math.min(element, 20));
    }

    private void increment(SparseIntArray sia, int element) {
        sia.put(element, sia.get(element) + 1);
    }
}
