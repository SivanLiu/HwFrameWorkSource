package com.android.server.wifi.p2p;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.server.wifi.HwQoE.HwQoEUtils;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import com.android.server.wifi.HwWifiStateMachine;
import com.android.server.wifi.WifiNativeUtils;
import com.android.server.wifi.WifiRepeater;
import com.android.server.wifi.WifiRepeaterConfigStore;
import com.android.server.wifi.WifiRepeaterController;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.DefaultState;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.GroupCreatedState;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.GroupCreatingState;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.InactiveState;
import com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pEnabledState;
import com.android.server.wifi.util.WifiCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class HwWifiP2pService extends WifiP2pServiceImpl {
    private static final String ACTION_DEVICE_DELAY_IDLE = "com.android.server.wifi.p2p.action.DEVICE_DELAY_IDLE";
    private static final int BAND_ERROR = -1;
    private static final int BASE = 143360;
    private static final String[] BLACKLIST_P2P_FIND = new String[]{"com.hp.android.printservice"};
    private static final int CHANNEL_ERROR = -1;
    public static final int CMD_BATTERY_CHANGED = 143469;
    public static final int CMD_DEVICE_DELAY_IDLE = 143465;
    public static final int CMD_LINKSPEED_POLL = 143470;
    public static final int CMD_REQUEST_REPEATER_CONFIG = 143463;
    public static final int CMD_RESPONSE_REPEATER_CONFIG = 143464;
    public static final int CMD_SCREEN_OFF = 143467;
    public static final int CMD_SCREEN_ON = 143466;
    public static final int CMD_SET_REPEATER_CONFIG = 143461;
    public static final int CMD_SET_REPEATER_CONFIG_COMPLETED = 143462;
    public static final int CMD_USER_PRESENT = 143468;
    private static final int CODE_GET_GROUP_CONFIG_INFO = 1005;
    private static final int CODE_GET_WIFI_REPEATER_CONFIG = 1001;
    private static final int CODE_SET_WIFI_REPEATER_CONFIG = 1002;
    private static final int CODE_WIFI_MAGICLINK_CONFIG_IP = 1003;
    private static final int CODE_WIFI_MAGICLINK_RELEASE_IP = 1004;
    private static final int CONNECT_FAILURE = -1;
    private static final int CONNECT_SUCCESS = 0;
    private static final boolean DBG = true;
    private static final long DEFAULT_IDLE_MS = 1800000;
    private static final long DEFAULT_LOW_DATA_TRAFFIC_LINE = 102400;
    private static final long DELAY_IDLE_MS = 60000;
    private static final String DESCRIPTOR = "android.net.wifi.p2p.IWifiP2pManager";
    private static final String HUAWEI_WIFI_DEVICE_DELAY_IDLE = "huawei.android.permission.WIFI_DEVICE_DELAY_IDLE";
    private static final boolean HWDBG;
    private static final boolean HWLOGW_E = true;
    private static long INTERVAL_DISALLOW_P2P_FIND = 130000;
    private static final int LINKSPEED_ESTIMATE_TIMES = 4;
    private static final int LINKSPEED_POLL_INTERVAL = 1000;
    private static final Boolean NO_REINVOCATION = Boolean.valueOf(false);
    private static final int P2P_BAND_2G = 0;
    private static final int P2P_BAND_5G = 1;
    private static final int P2P_CHOOSE_CHANNEL_RANDOM = 0;
    private static final Boolean RELOAD = Boolean.valueOf(true);
    private static final String SERVER_ADDRESS_WIFI_BRIDGE = "192.168.43.1";
    private static final String SERVER_ADDRESS_WIFI_BRIDGE_OTHER = "192.168.50.1";
    private static final String TAG = "HwWifiP2pService";
    private static final Boolean TRY_REINVOCATION = Boolean.valueOf(true);
    private static final int WHITELIST_DURATION_MS = 15000;
    private static WifiNativeUtils wifiNativeUtils = ((WifiNativeUtils) EasyInvokeFactory.getInvokeUtils(WifiNativeUtils.class));
    private static WifiP2pServiceUtils wifiP2pServiceUtils = ((WifiP2pServiceUtils) EasyInvokeFactory.getInvokeUtils(WifiP2pServiceUtils.class));
    private AlarmManager mAlarmManager;
    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                String str = HwWifiP2pService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive, action:");
                stringBuilder.append(action);
                Slog.d(str, stringBuilder.toString());
                if (action.equals(HwWifiP2pService.ACTION_DEVICE_DELAY_IDLE)) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage(HwWifiP2pService.CMD_DEVICE_DELAY_IDLE);
                }
            }
        }
    };
    private String mConfigInfo;
    private Context mContext;
    private PendingIntent mDefaultIdleIntent;
    private PendingIntent mDelayIdleIntent;
    private HwP2pStateMachine mHwP2pStateMachine = null;
    HwWifiCHRService mHwWifiCHRService;
    private String mInterface = "";
    private boolean mIsWifiRepeaterTetherStarted = false;
    private volatile int mLastLinkSpeed = 0;
    private long mLastRxBytes = 0;
    private long mLastTxBytes = 0;
    private boolean mLegacyGO = false;
    private int mLinkSpeedCounter = 0;
    private int mLinkSpeedPollToken = 0;
    private int[] mLinkSpeedWeights;
    private int[] mLinkSpeeds = new int[4];
    private boolean mMagicLinkDeviceFlag = false;
    NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
    private List<P2pFindProcessInfo> mP2pFindProcessInfoList = null;
    NetworkInfo mP2pNetworkInfo = new NetworkInfo(13, 0, "WIFI_P2P", "");
    private PowerManager mPowerManager = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = HwWifiP2pService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive, action:");
            stringBuilder.append(action);
            Slog.d(str, stringBuilder.toString());
            if (action != null) {
                NetworkInfo networkInfo;
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage(HwWifiP2pService.CMD_SCREEN_ON);
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage(HwWifiP2pService.CMD_USER_PRESENT);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage(HwWifiP2pService.CMD_SCREEN_OFF);
                } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        HwWifiP2pService.this.mNetworkInfo = networkInfo;
                    }
                } else if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                    networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        HwWifiP2pService.this.mP2pNetworkInfo = networkInfo;
                    }
                }
            }
        }
    };
    private String mTetherInterfaceName;
    private List<Pair<String, Long>> mValidDeivceList = new ArrayList();
    private Handler mWifiP2pDataTrafficHandler;
    private WifiRepeater mWifiRepeater;
    private long mWifiRepeaterBeginWorkTime = 0;
    private AsyncChannel mWifiRepeaterConfigChannel;
    private WifiRepeaterConfigStore mWifiRepeaterConfigStore;
    private boolean mWifiRepeaterEnabled = false;
    private long mWifiRepeaterEndWorkTime = 0;
    private int mWifiRepeaterFreq = 0;
    HandlerThread wifip2pThread = new HandlerThread("WifiP2pService");

    class HwP2pStateMachine extends P2pStateMachine {
        private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
        private Message mCreatPskGroupMsg;

        HwP2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(HwWifiP2pService.this, name, looper, p2pSupported);
        }

        public boolean handleDefaultStateMessage(Message message) {
            String addDeviceAddress;
            HwWifiP2pService hwWifiP2pService;
            StringBuilder stringBuilder;
            switch (message.what) {
                case HwWifiStateMachine.CMD_STOP_WIFI_REPEATER /*131577*/:
                    if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                        sendMessage(139280);
                        break;
                    }
                    break;
                case 141264:
                    addDeviceAddress = message.getData().getString("avlidDevice");
                    hwWifiP2pService = HwWifiP2pService.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("add p2p deivce valid addDeviceAddress = ");
                    stringBuilder.append(addDeviceAddress);
                    hwWifiP2pService.logd(stringBuilder.toString());
                    addP2PValidDevice(addDeviceAddress);
                    break;
                case 141265:
                    addDeviceAddress = message.getData().getString("avlidDevice");
                    hwWifiP2pService = HwWifiP2pService.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("remove p2p valid deivce removeDeviceAddress = ");
                    stringBuilder.append(addDeviceAddress);
                    hwWifiP2pService.logd(stringBuilder.toString());
                    removeP2PValidDevice(addDeviceAddress);
                    break;
                case 141266:
                    HwWifiP2pService.this.logd("clear p2p valid deivce");
                    clearP2PValidDevice();
                    break;
                case 141268:
                case 141270:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 2);
                    break;
                case 141269:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139272, 2);
                    break;
                case 141271:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 2);
                    if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                        HwWifiP2pService.this.stopWifiRepeater(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine));
                        break;
                    }
                    break;
                case HwWifiP2pService.CMD_DEVICE_DELAY_IDLE /*143465*/:
                    HwWifiP2pService.this.mWifiP2pDataTrafficHandler.sendMessage(Message.obtain(HwWifiP2pService.this.mWifiP2pDataTrafficHandler, 0));
                    break;
                case HwWifiP2pService.CMD_SCREEN_ON /*143466*/:
                    Slog.d(HwWifiP2pService.TAG, "cancel alarm.");
                    HwWifiP2pService.this.mAlarmManager.cancel(HwWifiP2pService.this.mDefaultIdleIntent);
                    HwWifiP2pService.this.mAlarmManager.cancel(HwWifiP2pService.this.mDelayIdleIntent);
                    break;
                case HwWifiP2pService.CMD_SCREEN_OFF /*143467*/:
                    HwWifiP2pService.this.mLastTxBytes = 0;
                    HwWifiP2pService.this.mLastRxBytes = 0;
                    if (HwWifiP2pService.this.shouldDisconnectWifiP2p()) {
                        if (HwWifiP2pService.this.mNetworkInfo.getDetailedState() != DetailedState.CONNECTED || !HwWifiP2pService.this.mP2pNetworkInfo.isConnected()) {
                            if (HwWifiP2pService.this.mP2pNetworkInfo.isConnected()) {
                                Slog.d(HwWifiP2pService.TAG, "start to removeP2PGroup.");
                                HwWifiP2pService.this.handleUpdataDateTraffic();
                                break;
                            }
                        }
                        Slog.d(HwWifiP2pService.TAG, "set default idle timer: 1800000 ms");
                        HwWifiP2pService.this.mAlarmManager.set(0, System.currentTimeMillis() + HwWifiP2pService.DEFAULT_IDLE_MS, HwWifiP2pService.this.mDefaultIdleIntent);
                        break;
                    }
                    break;
                case 147459:
                    HwWifiP2pService.this.sendNetworkConnectedBroadcast(message.obj);
                    break;
                case 147460:
                    HwWifiP2pService.this.sendNetworkDisconnectedBroadcast(message.obj);
                    break;
                default:
                    HwWifiP2pService hwWifiP2pService2 = HwWifiP2pService.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unhandled message ");
                    stringBuilder2.append(message);
                    hwWifiP2pService2.loge(stringBuilder2.toString());
                    return false;
            }
            return true;
        }

        public boolean handleP2pEnabledStateExMessage(Message message) {
            return false;
        }

        public boolean handleOngoingGroupRemovalStateExMessage(Message message) {
            if (message.what != 141271) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unhandled message ");
                stringBuilder.append(message);
                hwWifiP2pService.loge(stringBuilder.toString());
                return false;
            }
            replyToMessage(message, 139282);
            return true;
        }

        public boolean handleGroupNegotiationStateExMessage(Message message) {
            if (message.what != 141271) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unhandled message ");
                stringBuilder.append(message);
                hwWifiP2pService.loge(stringBuilder.toString());
                return false;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(getName());
            stringBuilder2.append(" MAGICLINK_REMOVE_GC_GROUP");
            logd(stringBuilder2.toString());
            String p2pInterface = message.obj.getString("iface");
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(getName());
            stringBuilder3.append("p2pInterface :");
            stringBuilder3.append(p2pInterface);
            logd(stringBuilder3.toString());
            if (p2pInterface == null || p2pInterface.equals("")) {
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
                transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
            } else {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(getName());
                stringBuilder4.append(" MAGICLINK_REMOVE_GC_GROUP,p2pInterface !=null,now remove it");
                logd(stringBuilder4.toString());
                if (this.mWifiNative.p2pGroupRemove(p2pInterface)) {
                    replyToMessage(message, 139282);
                    HwWifiP2pService.wifiP2pServiceUtils.sendP2pConnectionChangedBroadcast(HwWifiP2pService.this.mP2pStateMachine);
                } else {
                    HwWifiP2pService.wifiP2pServiceUtils.handleGroupRemoved(HwWifiP2pService.this.mP2pStateMachine);
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
                }
                transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
            }
            return true;
        }

        public boolean handleGroupCreatedStateExMessage(Message message) {
            int i = message.what;
            StringBuilder stringBuilder;
            if (i == 141271) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" MAGICLINK_REMOVE_GC_GROUP");
                logd(stringBuilder.toString());
                HwWifiP2pService.wifiP2pServiceUtils.enableBTCoex(HwWifiP2pService.this.mP2pStateMachine);
                if (this.mWifiNative.p2pGroupRemove(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine).getInterface())) {
                    transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmOngoingGroupRemovalState(HwWifiP2pService.this.mP2pStateMachine));
                    replyToMessage(message, 139282);
                } else {
                    HwWifiP2pService.wifiP2pServiceUtils.handleGroupRemoved(HwWifiP2pService.this.mP2pStateMachine);
                    transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
                }
                if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                    HwWifiP2pService.this.stopWifiRepeater(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine));
                }
            } else if (i == 143374) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" SET_MIRACAST_MODE: ");
                stringBuilder.append(message.arg1);
                logd(stringBuilder.toString());
                if (1 == message.arg1) {
                    HwWifiP2pService.this.mLastLinkSpeed = -1;
                    HwWifiP2pService.this.mLinkSpeedCounter = 0;
                    HwWifiP2pService.this.mLinkSpeedPollToken = 0;
                    sendMessage(HwWifiP2pService.CMD_LINKSPEED_POLL, HwWifiP2pService.this.mLinkSpeedPollToken);
                }
                return false;
            } else if (i != HwWifiP2pService.CMD_LINKSPEED_POLL) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unhandled message when=");
                stringBuilder2.append(message.getWhen());
                stringBuilder2.append(" what=");
                stringBuilder2.append(message.what);
                stringBuilder2.append(" arg1=");
                stringBuilder2.append(message.arg1);
                stringBuilder2.append(" arg2=");
                stringBuilder2.append(message.arg2);
                hwWifiP2pService.loge(stringBuilder2.toString());
                return false;
            } else if (HwWifiP2pService.this.mLinkSpeedPollToken == message.arg1) {
                String ifname = HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine).getInterface();
                int linkSpeed = SystemProperties.getInt("wfd.config.linkspeed", 0);
                if (linkSpeed == 0) {
                    linkSpeed = this.mWifiNative.getLinkSpeed(ifname);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("ifname: ");
                stringBuilder3.append(ifname);
                stringBuilder3.append(", get linkspeed from wpa: ");
                stringBuilder3.append(linkSpeed);
                stringBuilder3.append(", mLinkSpeed ");
                stringBuilder3.append(linkSpeed);
                logd(stringBuilder3.toString());
                if (HwWifiP2pService.this.mLinkSpeedCounter < 4) {
                    HwWifiP2pService.this.mLinkSpeeds[HwWifiP2pService.this.mLinkSpeedCounter = HwWifiP2pService.this.mLinkSpeedCounter + 1] = linkSpeed;
                }
                if (HwWifiP2pService.this.mLinkSpeedCounter >= 4) {
                    int avarageLinkSpeed = 0;
                    for (int i2 = 0; i2 < 4; i2++) {
                        avarageLinkSpeed += HwWifiP2pService.this.mLinkSpeeds[i2] * HwWifiP2pService.this.mLinkSpeedWeights[i2];
                    }
                    avarageLinkSpeed /= 100;
                    if (HwWifiP2pService.this.mLastLinkSpeed != avarageLinkSpeed) {
                        HwWifiP2pService.this.mLastLinkSpeed = avarageLinkSpeed;
                        HwWifiP2pService.this.sendLinkSpeedChangedBroadcast();
                    }
                    HwWifiP2pService.this.mLinkSpeedCounter = 0;
                }
                sendMessageDelayed(HwWifiP2pService.CMD_LINKSPEED_POLL, HwWifiP2pService.access$1504(HwWifiP2pService.this), 1000);
            }
            return true;
        }

        public boolean handleP2pNotSupportedStateMessage(Message message) {
            if (message.what != 141268) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unhandled message ");
                stringBuilder.append(message);
                hwWifiP2pService.loge(stringBuilder.toString());
                return false;
            }
            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 1);
            return true;
        }

        public boolean handleInactiveStateMessage(Message message) {
            int i = message.what;
            WifiConfiguration config;
            if (i == HwWifiP2pService.CMD_RESPONSE_REPEATER_CONFIG) {
                config = message.obj;
                if (config != null) {
                    creatGroupForRepeater(config);
                } else {
                    HwWifiP2pService.this.loge("wifi repeater config is null!");
                }
            } else if (i != 147557) {
                switch (i) {
                    case 141267:
                        WifiP2pConfig beam_config = message.obj;
                        HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, false);
                        HwWifiP2pService.this.updateGroupCapability(this.mPeers, beam_config.deviceAddress, this.mWifiNative.getGroupCapability(beam_config.deviceAddress));
                        if (beam_connect(beam_config, HwWifiP2pService.TRY_REINVOCATION.booleanValue()) != -1) {
                            HwWifiP2pService.this.updateStatus(this.mPeers, this.mSavedPeerConfig.deviceAddress, 1);
                            sendPeersChangedBroadcast();
                            replyToMessage(message, 139273);
                            transitionTo(this.mGroupNegotiationState);
                            break;
                        }
                        replyToMessage(message, 139272);
                        break;
                    case 141268:
                        if (!HwWifiP2pService.this.mWifiRepeater.isEncryptionTypeTetheringAllowed()) {
                            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
                            HwWifiP2pService.this.setWifiRepeaterState(5);
                            break;
                        }
                        HwWifiP2pService.this.setWifiRepeaterState(3);
                        HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, true);
                        if (HwWifiP2pService.this.mWifiRepeaterConfigChannel != null) {
                            this.mCreatPskGroupMsg = message;
                            config = message.obj;
                            if (config != null) {
                                HwWifiP2pService.this.mWifiRepeaterConfigChannel.sendMessage(HwWifiP2pService.CMD_SET_REPEATER_CONFIG, config);
                                creatGroupForRepeater(config);
                                break;
                            }
                            HwWifiP2pService.this.mWifiRepeaterConfigChannel.sendMessage(HwWifiP2pService.CMD_REQUEST_REPEATER_CONFIG);
                            break;
                        }
                        break;
                    case 141269:
                        String info = ((Bundle) message.obj).getString("cfg");
                        if (!TextUtils.isEmpty(info)) {
                            String[] tokens = info.split("\n");
                            if (tokens.length >= 4) {
                                StringBuffer buf = new StringBuffer();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("P\"");
                                stringBuilder.append(tokens[0]);
                                stringBuilder.append("\"\n");
                                stringBuilder.append(tokens[1]);
                                stringBuilder.append("\n\"");
                                stringBuilder.append(tokens[2]);
                                stringBuilder.append("\"\n");
                                stringBuilder.append(tokens[3]);
                                buf.append(stringBuilder.toString());
                                for (int i2 = 4; i2 < tokens.length; i2++) {
                                    if (4 == i2) {
                                        try {
                                            HwWifiP2pService.this.mLegacyGO = 1 == Integer.parseInt(tokens[4]);
                                        } catch (Exception e) {
                                            String str = HwWifiP2pService.TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("mLegacyGO = ");
                                            stringBuilder2.append(HwWifiP2pService.this.mLegacyGO);
                                            Slog.e(str, stringBuilder2.toString());
                                            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139272, 0);
                                            return true;
                                        }
                                    }
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("\n");
                                    stringBuilder.append(tokens[i2]);
                                    buf.append(stringBuilder.toString());
                                }
                                this.mWifiNative.magiclinkConnect(buf.toString());
                                break;
                            }
                        }
                        break;
                    case 141270:
                        boolean mret;
                        HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, true);
                        i = message.arg1;
                        String freq = message.obj.getString("freq");
                        if (i == -2) {
                            i = this.mGroups.getNetworkId(HwWifiP2pService.wifiP2pServiceUtils.getmThisDevice(HwWifiP2pService.this).deviceAddress);
                            if (i != -1) {
                                mret = this.mWifiNative.magiclinkGroupAdd(i, freq);
                            } else {
                                mret = this.mWifiNative.magiclinkGroupAdd(true, freq);
                            }
                        } else {
                            mret = this.mWifiNative.magiclinkGroupAdd(false, freq);
                        }
                        if (!mret) {
                            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 0);
                            break;
                        }
                        replyToMessage(message, 139279);
                        transitionTo(this.mGroupNegotiationState);
                        break;
                    default:
                        HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Unhandled message when=");
                        stringBuilder3.append(message.getWhen());
                        stringBuilder3.append(" what=");
                        stringBuilder3.append(message.what);
                        stringBuilder3.append(" arg1=");
                        stringBuilder3.append(message.arg1);
                        stringBuilder3.append(" arg2=");
                        stringBuilder3.append(message.arg2);
                        hwWifiP2pService.loge(stringBuilder3.toString());
                        return false;
                }
            } else {
                HwWifiP2pService.this.sendInterfaceCreatedBroadcast(message.obj);
                HwWifiP2pService.this.mMagicLinkDeviceFlag = HwWifiP2pService.this.mLegacyGO ^ 1;
                transitionTo(this.mGroupNegotiationState);
            }
            return true;
        }

        private void creatGroupForRepeater(WifiConfiguration config) {
            HwWifiP2pService.this.mWifiRepeaterEnabled = true;
            config.apChannel = HwWifiP2pService.this.mWifiRepeater.retrieveDownstreamChannel();
            config.apBand = HwWifiP2pService.this.mWifiRepeater.retrieveDownstreamBand();
            if (config.apChannel == -1 || config.apBand == -1) {
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
                HwWifiP2pService.this.setWifiRepeaterState(5);
                HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                return;
            }
            if (HwWifiP2pService.this.isWifiConnected()) {
                StringBuilder repeater_conf = new StringBuilder("WifiRepeater=y");
                repeater_conf.append("\nssid=");
                repeater_conf.append(config.SSID);
                repeater_conf.append("\npsk=");
                repeater_conf.append(new SensitiveArg(config.preSharedKey));
                repeater_conf.append("\nchannel=");
                repeater_conf.append(config.apChannel);
                repeater_conf.append("\nband=");
                if (!this.mWifiNative.addP2pRptGroup(repeater_conf.append(config.apBand).toString())) {
                    HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                }
            } else {
                HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                HwWifiP2pService.this.loge("wifirpt: isWifiConnected = false");
            }
            if (HwWifiP2pService.this.mWifiRepeaterEnabled) {
                Global.putInt(HwWifiP2pService.this.mContext.getContentResolver(), "wifi_repeater_on", 6);
                replyToMessage(this.mCreatPskGroupMsg, 139279);
                transitionTo(this.mGroupNegotiationState);
                if (HwWifiP2pService.HWDBG) {
                    HwWifiP2pService.this.logd("wifirpt: CREATE_GROUP_PSK SUCCEEDED, now transitionTo GroupNegotiationState");
                }
            } else {
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
                HwWifiP2pService.this.setWifiRepeaterState(5);
                HwWifiP2pService.this.loge("wifirpt: CREATE_GROUP_PSK FAILED, remain at this state.");
            }
        }

        private synchronized void addP2PValidDevice(String deviceAddress) {
            if (deviceAddress != null) {
                Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
                while (iter.hasNext()) {
                    if (((String) ((Pair) iter.next()).first).equals(deviceAddress)) {
                        iter.remove();
                    }
                }
                HwWifiP2pService.this.mValidDeivceList.add(new Pair(deviceAddress, Long.valueOf(SystemClock.elapsedRealtime())));
            }
        }

        private synchronized void removeP2PValidDevice(String deviceAddress) {
            if (HwWifiP2pService.this.mValidDeivceList != null) {
                Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
                while (iter.hasNext()) {
                    if (((String) ((Pair) iter.next()).first).equals(deviceAddress)) {
                        iter.remove();
                    }
                }
            }
        }

        private void cleanupValidDevicelist() {
            long curTime = SystemClock.elapsedRealtime();
            Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
            while (iter.hasNext()) {
                if (curTime - ((Long) ((Pair) iter.next()).second).longValue() > 15000) {
                    iter.remove();
                }
            }
        }

        private synchronized boolean isP2PValidDevice(String deviceAddress) {
            cleanupValidDevicelist();
            for (Pair<String, Long> entry : HwWifiP2pService.this.mValidDeivceList) {
                if (((String) entry.first).equals(deviceAddress)) {
                    return true;
                }
            }
            return false;
        }

        private synchronized void clearP2PValidDevice() {
            HwWifiP2pService.this.mValidDeivceList.clear();
        }

        private int beam_connect(WifiP2pConfig config, boolean tryInvocation) {
            if (config == null) {
                HwWifiP2pService.this.loge("config is null");
                return -1;
            }
            this.mSavedPeerConfig = config;
            WifiP2pDevice dev = this.mPeers.get(config.deviceAddress);
            if (dev == null) {
                HwWifiP2pService.this.loge("target device not found ");
                return -1;
            }
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("target ssid is ");
            stringBuilder.append(ssid);
            stringBuilder.append(" join:");
            stringBuilder.append(join);
            hwWifiP2pService.logd(stringBuilder.toString());
            if (join && dev.isGroupLimit()) {
                HwWifiP2pService.this.logd("target device reaches group limit.");
                join = false;
            } else if (join) {
                int netId = HwWifiP2pService.this.getNetworkId(this.mGroups, dev.deviceAddress, ssid);
                if (netId >= 0) {
                    if (this.mWifiNative.p2pGroupAdd(netId)) {
                        return 0;
                    }
                    return -1;
                }
            }
            if (join || !dev.isDeviceLimit()) {
                if (!join && tryInvocation && dev.isInvitationCapable()) {
                    int netId2 = -2;
                    if (config.netId < 0) {
                        netId2 = HwWifiP2pService.this.getNetworkId(this.mGroups, dev.deviceAddress);
                    } else if (config.deviceAddress.equals(HwWifiP2pService.this.getOwnerAddr(this.mGroups, config.netId))) {
                        netId2 = config.netId;
                    }
                    if (netId2 < 0) {
                        netId2 = getNetworkIdFromClientList(dev.deviceAddress);
                    }
                    HwWifiP2pService hwWifiP2pService2 = HwWifiP2pService.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("netId related with ");
                    stringBuilder2.append(dev.deviceAddress);
                    stringBuilder2.append(" = ");
                    stringBuilder2.append(netId2);
                    hwWifiP2pService2.logd(stringBuilder2.toString());
                    if (netId2 >= 0) {
                        if (this.mWifiNative.p2pReinvoke(netId2, dev.deviceAddress)) {
                            this.mSavedPeerConfig.netId = netId2;
                            return 0;
                        }
                        HwWifiP2pService.this.loge("p2pReinvoke() failed, update networks");
                        updatePersistentNetworks(HwWifiP2pService.RELOAD.booleanValue());
                    }
                }
                this.mWifiNative.p2pStopFind();
                p2pBeamConnectWithPinDisplay(config);
                return 0;
            }
            HwWifiP2pService.this.loge("target device reaches the device limit.");
            return -1;
        }

        private void p2pBeamConnectWithPinDisplay(WifiP2pConfig config) {
            WifiP2pDevice dev = this.mPeers.get(config.deviceAddress);
            if (dev == null) {
                HwWifiP2pService.this.loge("target device is not found ");
                return;
            }
            String pin = this.mWifiNative.p2pConnect(config, dev.isGroupOwner());
            try {
                Integer.parseInt(pin);
                notifyInvitationSent(pin, config.deviceAddress);
            } catch (NumberFormatException e) {
            }
        }

        private void sendPeersChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
            intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
            intent.addFlags(67108864);
            HwWifiP2pService.this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
        }

        private void sendP2pConnectionStateBroadcast(int state) {
            HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sending p2p connection state broadcast and state = ");
            stringBuilder.append(state);
            hwWifiP2pService.logd(stringBuilder.toString());
            Intent intent = new Intent("android.net.wifi.p2p.CONNECT_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("extraState", state);
            if (this.mSavedPeerConfig == null || state != 2) {
                HwWifiP2pService.this.loge("GroupCreatedState:mSavedConnectConfig is null");
            } else {
                String opposeInterfaceAddressString = this.mSavedPeerConfig.deviceAddress;
                String conDeviceName = null;
                intent.putExtra("interfaceAddress", opposeInterfaceAddressString);
                for (WifiP2pDevice d : this.mPeers.getDeviceList()) {
                    if (d.deviceAddress != null && d.deviceAddress.equals(this.mSavedPeerConfig.deviceAddress)) {
                        conDeviceName = d.deviceName;
                        break;
                    }
                }
                intent.putExtra("oppDeviceName", conDeviceName);
                HwWifiP2pService hwWifiP2pService2 = HwWifiP2pService.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("oppDeviceName = ");
                stringBuilder2.append(conDeviceName);
                hwWifiP2pService2.logd(stringBuilder2.toString());
                hwWifiP2pService2 = HwWifiP2pService.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("opposeInterfaceAddressString = ");
                stringBuilder2.append(opposeInterfaceAddressString);
                hwWifiP2pService2.logd(stringBuilder2.toString());
            }
            HwWifiP2pService.this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
        }

        public boolean autoAcceptConnection() {
            if (!isP2PValidDevice(this.mSavedPeerConfig.deviceAddress) && !isP2PValidDevice(getDeviceName(this.mSavedPeerConfig.deviceAddress))) {
                return false;
            }
            HwWifiP2pService.this.logd("notifyInvitationReceived is a valid device");
            removeP2PValidDevice(this.mSavedPeerConfig.deviceAddress);
            sendMessage(HwWifiP2pService.wifiP2pServiceUtils.getPeerConnectionUserAccept(HwWifiP2pService.this));
            return true;
        }

        private String getDeviceName(String deviceAddress) {
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d != null) {
                return d.deviceName;
            }
            return deviceAddress;
        }

        public String p2pBeamConnect(WifiP2pConfig config, boolean joinExistingGroup) {
            if (config == null) {
                return null;
            }
            List<String> args = new ArrayList();
            WpsInfo wps = config.wps;
            args.add(config.deviceAddress);
            switch (wps.setup) {
                case 0:
                    args.add("pbc");
                    break;
                case 1:
                    if (TextUtils.isEmpty(wps.pin)) {
                        args.add("pin");
                    } else {
                        args.add(wps.pin);
                    }
                    args.add("display");
                    break;
                case 2:
                    args.add(wps.pin);
                    args.add("keypad");
                    break;
                case 3:
                    args.add(wps.pin);
                    args.add("label");
                    break;
            }
            if (config.netId == -2) {
                args.add("persistent");
            }
            if (joinExistingGroup) {
                args.add("join");
            } else {
                int groupOwnerIntent = config.groupOwnerIntent;
                if (groupOwnerIntent < 0 || groupOwnerIntent > 15) {
                    groupOwnerIntent = 6;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("go_intent=");
                stringBuilder.append(groupOwnerIntent);
                args.add(stringBuilder.toString());
            }
            args.add("beam");
            StringBuffer command = new StringBuffer("P2P_CONNECT ");
            for (String s : args) {
                command.append(s);
                command.append(" ");
            }
            return "success";
        }
    }

    private class P2pFindProcessInfo {
        public long mLastP2pFindTimestamp;
        public int mUid;

        public P2pFindProcessInfo(int uid, long p2pFindTimestamp) {
            this.mUid = uid;
            this.mLastP2pFindTimestamp = p2pFindTimestamp;
        }
    }

    public static class SensitiveArg {
        private final Object mArg;

        public SensitiveArg(Object arg) {
            this.mArg = arg;
        }

        public String toString() {
            return String.valueOf(this.mArg);
        }
    }

    private class WifiP2pDataTrafficHandler extends Handler {
        private static final int MSG_UPDATA_DATA_TAFFIC = 0;

        WifiP2pDataTrafficHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                HwWifiP2pService.this.handleUpdataDateTraffic();
            }
        }
    }

    static /* synthetic */ int access$1504(HwWifiP2pService x0) {
        int i = x0.mLinkSpeedPollToken + 1;
        x0.mLinkSpeedPollToken = i;
        return i;
    }

    static {
        boolean z = true;
        if (!(Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)))) {
            z = false;
        }
        HWDBG = z;
    }

    public HwWifiP2pService(Context context) {
        super(context);
        this.mContext = context;
        if (this.mP2pStateMachine instanceof HwP2pStateMachine) {
            this.mHwP2pStateMachine = (HwP2pStateMachine) this.mP2pStateMachine;
        }
        this.wifip2pThread.start();
        this.mWifiP2pDataTrafficHandler = new WifiP2pDataTrafficHandler(this.wifip2pThread.getLooper());
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mDefaultIdleIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_DEVICE_DELAY_IDLE, null), 0);
        this.mDelayIdleIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_DEVICE_DELAY_IDLE, null), 0);
        registerForBroadcasts();
        this.mWifiRepeater = new WifiRepeaterController(this.mContext, getP2pStateMachineMessenger());
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        this.mLinkSpeedWeights = new int[]{15, 20, 30, 35};
        this.mP2pFindProcessInfoList = new ArrayList();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        this.mContext.registerReceiver(this.mAlarmReceiver, new IntentFilter(ACTION_DEVICE_DELAY_IDLE), HUAWEI_WIFI_DEVICE_DELAY_IDLE, null);
    }

    public boolean isWifiRepeaterStarted() {
        return 1 == Global.getInt(this.mContext.getContentResolver(), "wifi_repeater_on", 0);
    }

    private boolean shouldDisconnectWifiP2p() {
        if (!this.mWifiRepeaterEnabled) {
            return true;
        }
        Slog.i(TAG, "WifiRepeater is open.");
        return false;
    }

    private boolean checkP2pDataTrafficLine() {
        WifiP2pGroup mWifiP2pGroup = wifiP2pServiceUtils.getmGroup(this.mP2pStateMachine);
        if (mWifiP2pGroup == null) {
            Slog.d(TAG, "WifiP2pGroup is null.");
            return true;
        }
        long txBytes;
        this.mInterface = mWifiP2pGroup.getInterface();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mInterface: ");
        stringBuilder.append(this.mInterface);
        Slog.d(str, stringBuilder.toString());
        long txBytes2 = TrafficStats.getTxBytes(this.mInterface);
        long rxBytes = TrafficStats.getRxBytes(this.mInterface);
        long txSpeed = txBytes2 - this.mLastTxBytes;
        long rxSpeed = rxBytes - this.mLastRxBytes;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" txBytes:");
        stringBuilder2.append(txBytes2);
        stringBuilder2.append(" rxBytes:");
        stringBuilder2.append(rxBytes);
        stringBuilder2.append(" txSpeed:");
        stringBuilder2.append(txSpeed);
        stringBuilder2.append(" rxSpeed:");
        stringBuilder2.append(rxSpeed);
        stringBuilder2.append(" mLowDataTrafficLine:");
        stringBuilder2.append(102400);
        stringBuilder2.append(" DELAY_IDLE_MS:");
        stringBuilder2.append(60000);
        Slog.d(str2, stringBuilder2.toString());
        if (this.mLastTxBytes == 0) {
            long txBytes3 = txBytes2;
            if (this.mLastRxBytes == 0) {
                this.mLastTxBytes = txBytes3;
                this.mLastRxBytes = rxBytes;
                return false;
            }
            txBytes = txBytes3;
        } else {
            txBytes = txBytes2;
        }
        this.mLastTxBytes = txBytes;
        this.mLastRxBytes = rxBytes;
        if (txSpeed + rxSpeed < 102400) {
            return true;
        }
        return false;
    }

    private void handleUpdataDateTraffic() {
        Slog.d(TAG, "handleUpdataDateTraffic");
        if (!this.mP2pNetworkInfo.isConnected()) {
            Slog.d(TAG, "p2p is disconnected.");
        } else if (checkP2pDataTrafficLine()) {
            Slog.w(TAG, "remove group, disconnect wifi p2p");
            this.mP2pStateMachine.sendMessage(139280);
        } else {
            this.mAlarmManager.setExact(0, System.currentTimeMillis() + 60000, this.mDelayIdleIntent);
        }
    }

    protected Object getHwP2pStateMachine(String name, Looper looper, boolean p2pSupported) {
        return new HwP2pStateMachine(name, looper, p2pSupported);
    }

    protected boolean handleDefaultStateMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleDefaultStateMessage(message);
        }
        return false;
    }

    protected boolean handleP2pNotSupportedStateMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleP2pNotSupportedStateMessage(message);
        }
        return false;
    }

    protected boolean handleInactiveStateMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleInactiveStateMessage(message);
        }
        return false;
    }

    protected boolean handleP2pEnabledStateExMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleP2pEnabledStateExMessage(message);
        }
        return false;
    }

    protected boolean handleGroupNegotiationStateExMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleGroupNegotiationStateExMessage(message);
        }
        return false;
    }

    protected boolean handleGroupCreatedStateExMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleGroupCreatedStateExMessage(message);
        }
        return false;
    }

    protected boolean handleOngoingGroupRemovalStateExMessage(Message message) {
        if (this.mHwP2pStateMachine != null) {
            return this.mHwP2pStateMachine.handleOngoingGroupRemovalStateExMessage(message);
        }
        return false;
    }

    protected void sendGroupConfigInfo(WifiP2pGroup mGroup) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mGroup.getNetworkName());
        stringBuilder.append("\n");
        stringBuilder.append(mGroup.getOwner().deviceAddress);
        stringBuilder.append("\n");
        stringBuilder.append(mGroup.getPassphrase());
        stringBuilder.append("\n");
        stringBuilder.append(mGroup.getFrequence());
        this.mConfigInfo = stringBuilder.toString();
        this.mContext.sendBroadcastAsUser(new Intent("android.net.wifi.p2p.CONFIG_INFO"), UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    private void sendInterfaceCreatedBroadcast(String ifName) {
        logd("sending interface created broadcast");
        Intent intent = new Intent("android.net.wifi.p2p.INTERFACE_CREATED");
        intent.putExtra("p2pInterfaceName", ifName);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    private void sendNetworkConnectedBroadcast(String bssid) {
        logd("sending network connected broadcast");
        Intent intent = new Intent("android.net.wifi.p2p.NETWORK_CONNECTED_ACTION");
        intent.putExtra("bssid", bssid);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    private void sendNetworkDisconnectedBroadcast(String bssid) {
        logd("sending network disconnected broadcast");
        Intent intent = new Intent("android.net.wifi.p2p.NETWORK_DISCONNECTED_ACTION");
        intent.putExtra("bssid", bssid);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    private void sendLinkSpeedChangedBroadcast() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sending linkspeed changed broadcast ");
        stringBuilder.append(this.mLastLinkSpeed);
        logd(stringBuilder.toString());
        Intent intent = new Intent("com.huawei.net.wifi.p2p.LINK_SPEED");
        intent.putExtra("linkSpeed", this.mLastLinkSpeed);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.wfd.permission.ACCESS_P2P_LINKSPEED");
    }

    protected void handleTetheringDhcpRange(String[] tetheringDhcpRanges) {
        for (int i = tetheringDhcpRanges.length - 1; i >= 0; i--) {
            if ("192.168.49.2".equals(tetheringDhcpRanges[i])) {
                tetheringDhcpRanges[i] = "192.168.49.101";
                return;
            }
        }
    }

    protected boolean handleClientHwMessage(Message message) {
        switch (message.what) {
            case 141264:
            case 141265:
            case 141266:
            case 141267:
            case 141268:
            case 141269:
            case 141270:
            case 141271:
                this.mP2pStateMachine.sendMessage(message);
                return true;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ClientHandler.handleMessage ignoring msg=");
                stringBuilder.append(message);
                Slog.d(str, stringBuilder.toString());
                return false;
        }
    }

    protected void sendP2pConnectingStateBroadcast() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mHwP2pStateMachine = ");
        stringBuilder.append(this.mHwP2pStateMachine);
        stringBuilder.append(" this = ");
        stringBuilder.append(this);
        logd(stringBuilder.toString());
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(1);
    }

    protected void sendP2pFailStateBroadcast() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mHwP2pStateMachine = ");
        stringBuilder.append(this.mHwP2pStateMachine);
        stringBuilder.append(" this = ");
        stringBuilder.append(this);
        logd(stringBuilder.toString());
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(3);
        if (this.mIsWifiRepeaterTetherStarted && this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.addRepeaterConnFailedCount(1);
        }
    }

    protected void sendP2pConnectedStateBroadcast() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mHwP2pStateMachine = ");
        stringBuilder.append(this.mHwP2pStateMachine);
        stringBuilder.append(" this = ");
        stringBuilder.append(this);
        logd(stringBuilder.toString());
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(2);
    }

    protected void clearValidDeivceList() {
        this.mValidDeivceList.clear();
    }

    protected boolean autoAcceptConnection() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mHwP2pStateMachine = ");
        stringBuilder.append(this.mHwP2pStateMachine);
        stringBuilder.append(" this = ");
        stringBuilder.append(this);
        logd(stringBuilder.toString());
        return this.mHwP2pStateMachine.autoAcceptConnection();
    }

    private void loge(String string) {
        Slog.e(TAG, string);
    }

    private void logd(String string) {
        Slog.d(TAG, string);
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    private boolean isWifiConnected() {
        return this.mNetworkInfo != null ? this.mNetworkInfo.isConnected() : false;
    }

    private int convertFrequencyToChannelNumber(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency < 5170 || frequency > 5825) {
            return 0;
        }
        return ((frequency - 5170) / 5) + 34;
    }

    protected boolean startWifiRepeater(WifiP2pGroup group) {
        this.mTetherInterfaceName = group.getInterface();
        if (HWDBG) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start wifi repeater, ifaceName=");
            stringBuilder.append(this.mTetherInterfaceName);
            stringBuilder.append(", mWifiRepeaterEnabled=");
            stringBuilder.append(this.mWifiRepeaterEnabled);
            stringBuilder.append(", isWifiConnected=");
            stringBuilder.append(isWifiConnected());
            logd(stringBuilder.toString());
        }
        this.mWifiRepeaterFreq = group.getFrequence();
        if (isWifiConnected()) {
            int resultCode = getConnectivityManager().tether(this.mTetherInterfaceName);
            if (HWDBG) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ConnectivityManager.tether resultCode = ");
                stringBuilder2.append(resultCode);
                logd(stringBuilder2.toString());
            }
            if (resultCode == 0) {
                this.mWifiRepeater.handleP2pTethered(group);
                this.mIsWifiRepeaterTetherStarted = true;
                setWifiRepeaterState(1);
                if (this.mHwWifiCHRService != null) {
                    this.mHwWifiCHRService.addWifiRepeaterOpenedCount(1);
                    this.mHwWifiCHRService.setWifiRepeaterStatus(true);
                }
                return true;
            }
        }
        setWifiRepeaterState(5);
        if (this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.updateRepeaterOpenOrCloseError(HwQoEUtils.QOE_MSG_SCREEN_OFF, 1, "REPEATER_OPEN_OR_CLOSE_FAILED_UNKNOWN");
        }
        return false;
    }

    protected String getWifiRepeaterServerAddress() {
        WifiManager mWM = (WifiManager) this.mContext.getSystemService("wifi");
        int defaultAddress = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(SERVER_ADDRESS_WIFI_BRIDGE));
        if (mWM != null) {
            DhcpInfo dhcpInfo = mWM.getDhcpInfo();
            if (dhcpInfo != null) {
                int gateway = dhcpInfo.gateway;
                if (gateway > 0 && (16777215 & gateway) == (16777215 & defaultAddress)) {
                    if (HWDBG) {
                        logd("getWifiRepeaterServerAddress use SERVER_ADDRESS_WIFI_BRIDGE_OTHER");
                    }
                    return SERVER_ADDRESS_WIFI_BRIDGE_OTHER;
                }
            }
        }
        if (HWDBG) {
            logd("getWifiRepeaterServerAddress use SERVER_ADDRESS_WIFI_BRIDGE");
        }
        return SERVER_ADDRESS_WIFI_BRIDGE;
    }

    public WifiRepeater getWifiRepeater() {
        return this.mWifiRepeater;
    }

    public void notifyRptGroupRemoved() {
        this.mWifiRepeater.handleP2pUntethered();
    }

    public int getWifiRepeaterFreq() {
        return this.mWifiRepeaterFreq;
    }

    public int getWifiRepeaterChannel() {
        return convertFrequencyToChannelNumber(this.mWifiRepeaterFreq);
    }

    public boolean getWifiRepeaterTetherStarted() {
        return this.mIsWifiRepeaterTetherStarted;
    }

    public void handleClientConnect(WifiP2pGroup group) {
        if (this.mIsWifiRepeaterTetherStarted && group != null) {
            if (group.getClientList().size() >= 1) {
                DecisionUtil.bindService(this.mContext);
                Log.d(TAG, "bindService");
            }
            this.mWifiRepeater.handleClientListChanged(group);
            if (0 == this.mWifiRepeaterBeginWorkTime && group.getClientList().size() == 1) {
                this.mWifiRepeaterBeginWorkTime = SystemClock.elapsedRealtime();
            }
            this.mHwWifiCHRService.setRepeaterMaxClientCount(group.getClientList().size() > 0 ? group.getClientList().size() : 0);
        }
    }

    public void handleClientDisconnect(WifiP2pGroup group) {
        if (this.mIsWifiRepeaterTetherStarted) {
            this.mWifiRepeater.handleClientListChanged(group);
            if (group.getClientList().size() == 0 && this.mHwWifiCHRService != null) {
                this.mWifiRepeaterEndWorkTime = SystemClock.elapsedRealtime();
                this.mHwWifiCHRService.setWifiRepeaterWorkingTime((this.mWifiRepeaterEndWorkTime - this.mWifiRepeaterBeginWorkTime) / 1000);
                this.mWifiRepeaterEndWorkTime = 0;
                this.mWifiRepeaterBeginWorkTime = 0;
            }
        }
    }

    protected void stopWifiRepeater(WifiP2pGroup group) {
        setWifiRepeaterState(2);
        this.mWifiRepeaterEnabled = false;
        this.mWifiRepeaterEndWorkTime = SystemClock.elapsedRealtime();
        if (!(group == null || group.getClientList().size() <= 0 || this.mHwWifiCHRService == null)) {
            this.mHwWifiCHRService.setWifiRepeaterWorkingTime((this.mWifiRepeaterEndWorkTime - this.mWifiRepeaterBeginWorkTime) / 1000);
        }
        this.mWifiRepeaterBeginWorkTime = 0;
        this.mWifiRepeaterEndWorkTime = 0;
        this.mWifiRepeaterFreq = 0;
        if (this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.setWifiRepeaterFreq(this.mWifiRepeaterFreq);
        }
        if (this.mIsWifiRepeaterTetherStarted) {
            int resultCode = getConnectivityManager().untether(this.mTetherInterfaceName);
            if (HWDBG) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ConnectivityManager.untether resultCode = ");
                stringBuilder.append(resultCode);
                logd(stringBuilder.toString());
            }
            if (resultCode == 0) {
                this.mIsWifiRepeaterTetherStarted = false;
                setWifiRepeaterState(0);
                if (this.mHwWifiCHRService != null) {
                    this.mHwWifiCHRService.setWifiRepeaterStatus(false);
                    return;
                }
                return;
            }
            loge("Untether initiate failed!");
            setWifiRepeaterState(4);
            if (this.mHwWifiCHRService != null) {
                this.mHwWifiCHRService.updateRepeaterOpenOrCloseError(HwQoEUtils.QOE_MSG_SCREEN_OFF, 0, "REPEATER_OPEN_OR_CLOSE_FAILED_UNKNOWN");
                return;
            }
            return;
        }
        setWifiRepeaterState(0);
    }

    public void setWifiRepeaterState(int state) {
        if (this.mContext != null) {
            Global.putInt(this.mContext.getContentResolver(), "wifi_repeater_on", state);
            Intent intent = new Intent("com.huawei.android.net.wifi.p2p.action.WIFI_RPT_STATE_CHANGED");
            intent.putExtra("wifi_rpt_state", state);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    protected boolean getWifiRepeaterEnabled() {
        return this.mWifiRepeaterEnabled;
    }

    protected void initWifiRepeaterConfig() {
        if (this.mWifiRepeaterConfigChannel == null) {
            this.mWifiRepeaterConfigChannel = new AsyncChannel();
            this.mWifiRepeaterConfigStore = WifiRepeaterConfigStore.makeWifiRepeaterConfigStore(this.mP2pStateMachine.getHandler());
            this.mWifiRepeaterConfigStore.loadRepeaterConfiguration();
            this.mWifiRepeaterConfigChannel.connectSync(this.mContext, this.mP2pStateMachine.getHandler(), this.mWifiRepeaterConfigStore.getMessenger());
        }
    }

    public void setWifiRepeaterConfiguration(WifiConfiguration config) {
        if (this.mWifiRepeaterConfigChannel != null && config != null) {
            this.mWifiRepeaterConfigChannel.sendMessage(CMD_SET_REPEATER_CONFIG, config);
        }
    }

    public WifiConfiguration syncGetWifiRepeaterConfiguration() {
        if (this.mWifiRepeaterConfigChannel == null) {
            return null;
        }
        Message resultMsg = this.mWifiRepeaterConfigChannel.sendMessageSynchronously(CMD_REQUEST_REPEATER_CONFIG);
        WifiConfiguration ret = resultMsg.obj;
        resultMsg.recycle();
        return ret;
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "WifiService");
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        WifiConfiguration _arg2 = null;
        String _arg0;
        switch (code) {
            case CODE_GET_WIFI_REPEATER_CONFIG /*1001*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                Slog.d(TAG, "GetWifiRepeaterConfiguration ");
                _arg2 = syncGetWifiRepeaterConfiguration();
                reply.writeNoException();
                if (_arg2 != null) {
                    reply.writeInt(1);
                    _arg2.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case CODE_SET_WIFI_REPEATER_CONFIG /*1002*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                Slog.d(TAG, "setWifiRepeaterConfiguration ");
                if (data.readInt() != 0) {
                    _arg2 = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                }
                setWifiRepeaterConfiguration(_arg2);
                reply.writeNoException();
                return true;
            case CODE_WIFI_MAGICLINK_CONFIG_IP /*1003*/:
                String _arg02;
                String _arg1;
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                Slog.d(TAG, "configIPAddr ");
                if (data.readInt() != 0) {
                    _arg0 = data.readString();
                    String readString = data.readString();
                    _arg02 = _arg0;
                    _arg0 = data.readString();
                    _arg1 = readString;
                } else {
                    _arg02 = null;
                    _arg1 = null;
                }
                configIPAddr(_arg02, _arg1, _arg0);
                reply.writeNoException();
                return true;
            case CODE_WIFI_MAGICLINK_RELEASE_IP /*1004*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                Slog.d(TAG, "setWifiRepeaterConfiguration ");
                if (data.readInt() != 0) {
                    _arg0 = data.readString();
                }
                releaseIPAddr(_arg0);
                reply.writeNoException();
                return true;
            case CODE_GET_GROUP_CONFIG_INFO /*1005*/:
                if (wifiP2pServiceUtils.checkSignMatchOrIsSystemApp(this.mContext)) {
                    _arg0 = this.mConfigInfo;
                    this.mConfigInfo = "";
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    reply.writeNoException();
                    if (_arg0 == null) {
                        reply.writeInt(0);
                        return true;
                    }
                    reply.writeInt(1);
                    reply.writeString(_arg0);
                    return true;
                }
                Log.e(TAG, "WifiP2pService  CODE_GET_GROUP_CONFIG_INFO  SIGNATURE_NO_MATCH or not systemApp");
                reply.writeInt(0);
                reply.writeNoException();
                return false;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    protected boolean processMessageForP2pCollision(Message msg, State state) {
        boolean mIsP2pCollision = false;
        if (state instanceof DefaultState) {
            switch (msg.what) {
                case 139265:
                case 139268:
                case 139271:
                case 139274:
                case 139277:
                case 139315:
                case 139318:
                case 139321:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if (state instanceof P2pEnabledState) {
            switch (msg.what) {
                case 139265:
                case 139315:
                case 139318:
                case 139329:
                case 139332:
                case 139335:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if (state instanceof InactiveState) {
            switch (msg.what) {
                case 139265:
                case 139274:
                case 139277:
                case 139329:
                case 139332:
                case 139335:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if (state instanceof GroupCreatingState) {
            int i = msg.what;
            if ((i == 139265 || i == 139274) && this.mWifiRepeaterEnabled) {
                showUserToastIfP2pCollision();
                mIsP2pCollision = true;
            }
        }
        if (!(state instanceof GroupCreatedState) || msg.what != 139271 || !this.mWifiRepeaterEnabled) {
            return mIsP2pCollision;
        }
        showUserToastIfP2pCollision();
        return true;
    }

    private void showUserToastIfP2pCollision() {
        Toast.makeText(this.mContext, 33685839, 0).show();
    }

    protected boolean getMagicLinkDeviceFlag() {
        return this.mMagicLinkDeviceFlag;
    }

    protected void setmMagicLinkDeviceFlag(boolean magicLinkDeviceFlag) {
        this.mMagicLinkDeviceFlag = magicLinkDeviceFlag;
        if (!this.mMagicLinkDeviceFlag) {
            this.mLegacyGO = false;
        }
    }

    protected void notifyP2pChannelNumber(int channel) {
        if (channel > 13) {
            channel = 0;
        }
        WifiCommonUtils.notifyDeviceState("WLAN-P2P", String.valueOf(channel), "");
    }

    protected void notifyP2pState(String state) {
        WifiCommonUtils.notifyDeviceState("WLAN-P2P", state, "");
    }

    private String buildPrintableIpAddress(String originIpAddr) {
        if (originIpAddr == null) {
            return null;
        }
        byte[] ipAddrArray = NetworkUtils.numericToInetAddress(originIpAddr).getAddress();
        if (ipAddrArray.length != 4) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < 3; index++) {
            sb.append(ipAddrArray[index]);
            sb.append(".");
        }
        sb.append("***");
        return sb.toString();
    }

    private boolean configIPAddr(String ifName, String ipAddr, String gateway) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("configIPAddr: ");
        stringBuilder.append(ifName);
        stringBuilder.append(" ");
        stringBuilder.append(buildPrintableIpAddress(ipAddr));
        Slog.d(str, stringBuilder.toString());
        InterfaceConfiguration ifcg = null;
        try {
            this.mNwService.enableIpv6(ifName);
            ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(ipAddr), 24));
            ifcg.setInterfaceUp();
            this.mNwService.setInterfaceConfig(ifName, ifcg);
            RouteInfo connectedRoute = new RouteInfo(new LinkAddress((Inet4Address) NetworkUtils.numericToInetAddress(ipAddr), 24), null, ifName);
            List<RouteInfo> routes = new ArrayList(3);
            routes.add(connectedRoute);
            routes.add(new RouteInfo((IpPrefix) null, NetworkUtils.numericToInetAddress(gateway), ifName));
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("add new RouteInfo() gateway:");
            stringBuilder2.append(buildPrintableIpAddress(gateway));
            stringBuilder2.append(" iface:");
            stringBuilder2.append(ifName);
            Log.e(str, stringBuilder2.toString());
            this.mNwService.addInterfaceToLocalNetwork(ifName, routes);
        } catch (Exception e) {
            Log.i(TAG, "", e);
        }
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("configIPAddr: ");
        stringBuilder3.append(ifName);
        stringBuilder3.append(" ");
        stringBuilder3.append(buildPrintableIpAddress(ipAddr));
        stringBuilder3.append("* ok");
        Slog.d(str, stringBuilder3.toString());
        return true;
    }

    private boolean releaseIPAddr(String ifName) {
        if (ifName == null) {
            return false;
        }
        try {
            this.mNwService.disableIpv6(ifName);
            this.mNwService.clearInterfaceAddresses(ifName);
            return true;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to clear addresses or disable IPv6");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    protected boolean allowP2pFind(int uid) {
        if (Process.isCoreUid(uid)) {
            return true;
        }
        boolean allow;
        boolean isBlackApp = isInBlacklistForP2pFind(uid);
        if (isScreenOn()) {
            if (isBlackApp) {
                allow = allowP2pFindByTime(uid);
            } else {
                allow = true;
            }
        } else if (isBlackApp) {
            allow = false;
        } else {
            allow = allowP2pFindByTime(uid);
        }
        if (!allow) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("p2p find disallowed, uid:");
            stringBuilder.append(uid);
            Log.d(str, stringBuilder.toString());
        }
        return allow;
    }

    /* JADX WARNING: Missing block: B:17:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected synchronized void handleP2pStopFind(int uid) {
        if (this.mP2pFindProcessInfoList != null) {
            if (uid < 0) {
                this.mP2pFindProcessInfoList.clear();
            }
            for (P2pFindProcessInfo p2pInfo : this.mP2pFindProcessInfoList) {
                if (uid == p2pInfo.mUid) {
                    this.mP2pFindProcessInfoList.remove(p2pInfo);
                    break;
                }
            }
        }
    }

    private boolean isScreenOn() {
        if (this.mPowerManager == null || this.mPowerManager.isScreenOn()) {
            return true;
        }
        return false;
    }

    private boolean isInBlacklistForP2pFind(int uid) {
        if (this.mContext == null) {
            return false;
        }
        PackageManager pkgMgr = this.mContext.getPackageManager();
        if (pkgMgr == null) {
            return false;
        }
        String pkgName = pkgMgr.getNameForUid(uid);
        for (String black : BLACKLIST_P2P_FIND) {
            if (black.equals(pkgName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("p2p-find blacklist: ");
                stringBuilder.append(pkgName);
                Log.d(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    private synchronized boolean allowP2pFindByTime(int uid) {
        if (this.mP2pFindProcessInfoList == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        List<P2pFindProcessInfo> disallowedList = (List) this.mP2pFindProcessInfoList.stream().filter(new -$$Lambda$HwWifiP2pService$mTGzbNymvkAXSXqBrM8ot-RFbQ0(now)).collect(Collectors.toList());
        this.mP2pFindProcessInfoList.clear();
        this.mP2pFindProcessInfoList.addAll(disallowedList);
        for (P2pFindProcessInfo p2pInfo : this.mP2pFindProcessInfoList) {
            if (uid == p2pInfo.mUid) {
                return false;
            }
        }
        this.mP2pFindProcessInfoList.add(new P2pFindProcessInfo(uid, now));
        return true;
    }

    static /* synthetic */ boolean lambda$allowP2pFindByTime$0(long now, P2pFindProcessInfo P2pFindProcessInfo) {
        return now - P2pFindProcessInfo.mLastP2pFindTimestamp <= INTERVAL_DISALLOW_P2P_FIND;
    }

    protected int addScanChannelInTimeout(int channelID, int timeout) {
        int ret = (channelID << 16) + (timeout & 255);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("discover time ");
        stringBuilder.append(ret);
        logd(stringBuilder.toString());
        return ret;
    }
}
