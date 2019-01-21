package com.android.server.wifi.p2p;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ip.IpClient;
import android.net.ip.IpClient.Callback;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pGroupList.GroupDeleteListener;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IDeviceIdleController;
import android.os.IDeviceIdleController.Stub;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HwWifiBigDataConstant;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiServiceFactory;
import com.android.server.wifi.SupplicantStaIfaceHal;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiP2pServiceHisiExt;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.util.WifiAsyncChannel;
import com.android.server.wifi.util.WifiCommonUtils;
import com.android.server.wifi.util.WifiHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiP2pServiceImpl extends AbsWifiP2pService {
    private static final int APKACTION_P2PSCAN = 13;
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static final boolean DBG = true;
    private static final int DELAY_TIME = 2000;
    public static final int DISABLED = 0;
    public static final int DISABLE_P2P = 143377;
    public static final int DISABLE_P2P_TIMED_OUT = 143366;
    private static final int DISABLE_P2P_WAIT_TIME_MS = 5000;
    public static final int DISCONNECT_WIFI_REQUEST = 143372;
    public static final int DISCONNECT_WIFI_RESPONSE = 143373;
    private static final int DISCOVER_TIMEOUT_S = 120;
    private static final int DROP_WIFI_USER_ACCEPT = 143364;
    private static final int DROP_WIFI_USER_REJECT = 143365;
    private static final String EMPTY_DEVICE_ADDRESS = "00:00:00:00:00:00";
    public static final int ENABLED = 1;
    public static final int ENABLE_P2P = 143376;
    private static final Boolean FORM_GROUP = Boolean.valueOf(false);
    public static final int GROUP_CREATING_TIMED_OUT = 143361;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 120000;
    private static final int GROUP_IDLE_TIME_S = 10;
    private static final boolean HWDBG;
    private static final int IPC_DHCP_RESULTS = 143392;
    private static final int IPC_POST_DHCP_ACTION = 143391;
    private static final int IPC_PRE_DHCP_ACTION = 143390;
    private static final int IPC_PROVISIONING_FAILURE = 143394;
    private static final int IPC_PROVISIONING_SUCCESS = 143393;
    private static final boolean IS_ATT;
    private static final boolean IS_VERIZON;
    private static final Boolean JOIN_GROUP = Boolean.valueOf(true);
    private static final String NETWORKTYPE = "WIFI_P2P";
    private static final Boolean NO_RELOAD = Boolean.valueOf(false);
    static final int P2P_BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    static final int P2P_BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    public static final int P2P_CONNECTION_CHANGED = 143371;
    private static final int P2P_DEVICE_OF_MIRACAST = 7;
    private static final int P2P_DEVICE_RETRY_MAX = 5;
    private static final int P2P_DISCONNECT = -1;
    private static final String PACKAGE_NAME = "com.huawei.android.wfdft";
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    private static final Boolean RELOAD = Boolean.valueOf(true);
    private static final String SERVER_ADDRESS = "192.168.49.1";
    private static final String SERVER_ADDRESS_WIFI_BRIDGE = "192.168.43.1";
    public static final int SET_MIRACAST_MODE = 143374;
    public static final int SHOW_USER_CONFIRM_DIALOG = 143410;
    private static final String TAG = "WifiP2pService";
    private static int sDisableP2pTimeoutIndex = 0;
    private static int sGroupCreatingTimeoutIndex = 0;
    private boolean mAutonomousGroup;
    private ClientHandler mClientHandler;
    private HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap();
    private Context mContext;
    private boolean mCreateWifiBridge = false;
    private final Map<IBinder, DeathHandlerData> mDeathDataByBinder = new HashMap();
    private DhcpResults mDhcpResults;
    private boolean mDiscoveryBlocked;
    private boolean mDiscoveryPostponed = false;
    private boolean mDiscoveryStarted;
    private HwWifiCHRService mHwWifiCHRService;
    private IpClient mIpClient;
    private boolean mIsInvite = false;
    private boolean mJoinExistingGroup;
    private Object mLock = new Object();
    private NetworkInfo mNetworkInfo;
    INetworkManagementService mNwService;
    private String mP2pServerAddress;
    protected P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    private AsyncChannel mReplyChannel = new WifiAsyncChannel(TAG);
    private String mServiceDiscReqId;
    private byte mServiceTransactionId = (byte) 0;
    private boolean mTemporarilyDisconnectedWifi = false;
    private WifiP2pDevice mThisDevice = new WifiP2pDevice();
    private AsyncChannel mWifiChannel;
    private WifiInjector mWifiInjector;
    private int mWifiP2pDevCreateRetry = 0;
    WifiP2pServiceHisiExt mWifiP2pServiceHisiExt = null;

    private class ClientInfo {
        private Messenger mMessenger;
        private SparseArray<WifiP2pServiceRequest> mReqList;
        private List<WifiP2pServiceInfo> mServList;

        /* synthetic */ ClientInfo(WifiP2pServiceImpl x0, Messenger x1, AnonymousClass1 x2) {
            this(x1);
        }

        private ClientInfo(Messenger m) {
            this.mMessenger = m;
            this.mReqList = new SparseArray();
            this.mServList = new ArrayList();
        }
    }

    private class DeathHandlerData {
        DeathRecipient mDeathRecipient;
        Messenger mMessenger;

        DeathHandlerData(DeathRecipient dr, Messenger m) {
            this.mDeathRecipient = dr;
            this.mMessenger = m;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deathRecipient=");
            stringBuilder.append(this.mDeathRecipient);
            stringBuilder.append(", messenger=");
            stringBuilder.append(this.mMessenger);
            return stringBuilder.toString();
        }
    }

    protected class P2pStateMachine extends StateMachine {
        private AfterUserAuthorizingJoinState mAfterUserAuthorizingJoinState = new AfterUserAuthorizingJoinState();
        private DefaultState mDefaultState = new DefaultState();
        private FrequencyConflictState mFrequencyConflictState = new FrequencyConflictState();
        private WifiP2pGroup mGroup;
        private GroupCreatedState mGroupCreatedState = new GroupCreatedState();
        private GroupCreatingState mGroupCreatingState = new GroupCreatingState();
        protected GroupNegotiationState mGroupNegotiationState = new GroupNegotiationState();
        protected final WifiP2pGroupList mGroups = new WifiP2pGroupList(null, new GroupDeleteListener() {
            public void onDeleteGroup(int netId) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("called onDeleteGroup() netId=");
                stringBuilder.append(netId);
                p2pStateMachine.logd(stringBuilder.toString());
                P2pStateMachine.this.mWifiNative.removeP2pNetwork(netId);
                P2pStateMachine.this.mWifiNative.saveConfig();
                P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
            }
        });
        IDeviceIdleController mIDeviceIdleController;
        private InactiveState mInactiveState = new InactiveState();
        private String mInterfaceName;
        private boolean mIsBTCoexDisabled = false;
        private boolean mIsInterfaceAvailable = false;
        private boolean mIsWifiEnabled = false;
        private OngoingGroupRemovalState mOngoingGroupRemovalState = new OngoingGroupRemovalState();
        private P2pDisabledState mP2pDisabledState = new P2pDisabledState();
        private P2pDisablingState mP2pDisablingState = new P2pDisablingState();
        private P2pEnabledState mP2pEnabledState = new P2pEnabledState();
        private P2pNotSupportedState mP2pNotSupportedState = new P2pNotSupportedState();
        protected final WifiP2pDeviceList mPeers = new WifiP2pDeviceList();
        private final WifiP2pDeviceList mPeersLostDuringConnection = new WifiP2pDeviceList();
        private boolean mPendingReformGroupIndication = false;
        private ProvisionDiscoveryState mProvisionDiscoveryState = new ProvisionDiscoveryState();
        protected WifiP2pConfig mSavedPeerConfig = new WifiP2pConfig();
        private UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState = new UserAuthorizingInviteRequestState();
        private UserAuthorizingJoinState mUserAuthorizingJoinState = new UserAuthorizingJoinState();
        private UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState = new UserAuthorizingNegotiationRequestState();
        private WifiInjector mWifiInjector;
        private WifiP2pMonitor mWifiMonitor = WifiInjector.getInstance().getWifiP2pMonitor();
        protected WifiP2pNative mWifiNative = WifiInjector.getInstance().getWifiP2pNative();
        private final WifiP2pInfo mWifiP2pInfo = new WifiP2pInfo();

        class AfterUserAuthorizingJoinState extends State {
            AfterUserAuthorizingJoinState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                return false;
            }

            public void exit() {
            }
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                if (WifiP2pServiceImpl.this.processMessageForP2pCollision(message, this)) {
                    return true;
                }
                Object obj = null;
                StringBuilder stringBuilder2;
                switch (message.what) {
                    case -1:
                        P2pStateMachine.this.removePowerSaveWhitelist();
                        break;
                    case 69632:
                        if (message.arg1 != 0) {
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Full connection failure, error = ");
                            stringBuilder2.append(message.arg1);
                            p2pStateMachine.loge(stringBuilder2.toString());
                            WifiP2pServiceImpl.this.mWifiChannel = null;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            break;
                        }
                        P2pStateMachine.this.logd("Full connection with WifiStateMachine established");
                        WifiP2pServiceImpl.this.mWifiChannel = (AsyncChannel) message.obj;
                        break;
                    case 69633:
                        new WifiAsyncChannel(WifiP2pServiceImpl.TAG).connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        break;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Client connection lost with reason: ");
                            stringBuilder2.append(message.arg1);
                            p2pStateMachine.loge(stringBuilder2.toString());
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = null;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 2);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 2);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 2);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 2);
                        if (WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                            WifiP2pServiceImpl.this.stopWifiRepeater(P2pStateMachine.this.mGroup);
                            break;
                        }
                        break;
                    case 139283:
                        P2pStateMachine.this.replyToMessage(message, 139284, (Object) P2pStateMachine.this.getPeers((Bundle) message.obj, message.sendingUid));
                        break;
                    case 139285:
                        P2pStateMachine.this.replyToMessage(message, 139286, (Object) new WifiP2pInfo(P2pStateMachine.this.mWifiP2pInfo));
                        break;
                    case 139287:
                        p2pStateMachine = P2pStateMachine.this;
                        if (P2pStateMachine.this.mGroup != null) {
                            obj = new WifiP2pGroup(P2pStateMachine.this.mGroup);
                        }
                        p2pStateMachine.replyToMessage(message, 139288, obj);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 2);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 2);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 2);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 2);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 2);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 2);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 2);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 2);
                        break;
                    case 139321:
                        P2pStateMachine.this.replyToMessage(message, 139322, (Object) new WifiP2pGroupList(P2pStateMachine.this.mGroups, null));
                        break;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 2);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
                        break;
                    case 139329:
                    case 139332:
                    case 139335:
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                    case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION /*143390*/:
                    case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION /*143391*/:
                    case WifiP2pServiceImpl.IPC_DHCP_RESULTS /*143392*/:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS /*143393*/:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE /*143394*/:
                    case 147457:
                    case 147458:
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT /*147477*/:
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT /*147488*/:
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT /*147493*/:
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT /*147494*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT /*147495*/:
                        break;
                    case 139339:
                    case 139340:
                        P2pStateMachine.this.replyToMessage(message, 139341, null);
                        break;
                    case 139342:
                    case 139343:
                        P2pStateMachine.this.replyToMessage(message, 139345, 2);
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                        WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            if (message.obj != null) {
                                try {
                                    message.obj.sendMessage(message.arg2);
                                    break;
                                } catch (Exception e) {
                                    P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("unable to send BLOCK_DISCOVERY response: ");
                                    stringBuilder3.append(e);
                                    p2pStateMachine2.loge(stringBuilder3.toString());
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                        if (WifiP2pServiceImpl.this.mWifiChannel == null) {
                            P2pStateMachine.this.loge("Unexpected disable request when WifiChannel is null");
                            break;
                        }
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                        break;
                    case WifiP2pServiceImpl.SHOW_USER_CONFIRM_DIALOG /*143410*/:
                        WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.showP2pEanbleDialog();
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Unexpected group creation, remove ");
                            stringBuilder2.append(P2pStateMachine.this.mGroup);
                            p2pStateMachine.loge(stringBuilder2.toString());
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                        break;
                    default:
                        return WifiP2pServiceImpl.this.handleDefaultStateMessage(message);
                }
                return true;
            }
        }

        class FrequencyConflictState extends State {
            private AlertDialog mFrequencyConflictDialog;

            FrequencyConflictState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                notifyFrequencyConflict();
            }

            private void notifyFrequencyConflict() {
                P2pStateMachine.this.logd("Notify frequency conflict");
                Resources r = Resources.getSystem();
                AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext).setMessage(r.getString(17041404, new Object[]{P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)})).setPositiveButton(r.getString(17039952), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(r.getString(17039912), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.getWindow().setType(2003);
                LayoutParams attrs = dialog.getWindow().getAttributes();
                attrs.privateFlags = 16;
                dialog.getWindow().setAttributes(attrs);
                dialog.show();
                this.mFrequencyConflictDialog = dialog;
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                int i = message.what;
                if (i != WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE) {
                    switch (i) {
                        case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                            if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                            } else {
                                P2pStateMachine.this.loge("DROP_WIFI_USER_ACCEPT message received when WifiChannel is null");
                            }
                            WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                            break;
                        case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            break;
                        default:
                            switch (i) {
                                case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                                case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT /*147483*/:
                                    p2pStateMachine = P2pStateMachine.this;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(getName());
                                    stringBuilder.append("group sucess during freq conflict!");
                                    p2pStateMachine.loge(stringBuilder.toString());
                                    break;
                                case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT /*147482*/:
                                case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                                case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                                    break;
                                case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                                    p2pStateMachine = P2pStateMachine.this;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(getName());
                                    stringBuilder.append("group started after freq conflict, handle anyway");
                                    p2pStateMachine.loge(stringBuilder.toString());
                                    P2pStateMachine.this.deferMessage(message);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    break;
                                default:
                                    return false;
                            }
                    }
                }
                p2pStateMachine = P2pStateMachine.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append("Wifi disconnected, retry p2p");
                p2pStateMachine.logd(stringBuilder.toString());
                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                return true;
            }

            public void exit() {
                if (this.mFrequencyConflictDialog != null) {
                    this.mFrequencyConflictDialog.dismiss();
                }
            }
        }

        class GroupCreatedState extends State {
            GroupCreatedState() {
            }

            private boolean handlP2pGroupRestart() {
                boolean remove = true;
                if (P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                    Slog.d(WifiP2pServiceImpl.TAG, "Removed P2P group successfully");
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mOngoingGroupRemovalState);
                } else {
                    Slog.d(WifiP2pServiceImpl.TAG, "Failed to remove the P2P group");
                    P2pStateMachine.this.handleGroupRemoved();
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                    remove = false;
                }
                if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                    Slog.d(WifiP2pServiceImpl.TAG, "AutonomousGroup is set, reform P2P Group");
                    P2pStateMachine.this.sendMessage(139277);
                } else {
                    Slog.d(WifiP2pServiceImpl.TAG, "AutonomousGroup is not set, will not reform P2P Group");
                }
                return remove;
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append("mPendingReformGroupIndication=");
                stringBuilder.append(P2pStateMachine.this.mPendingReformGroupIndication);
                p2pStateMachine.logd(stringBuilder.toString());
                if (P2pStateMachine.this.mPendingReformGroupIndication) {
                    P2pStateMachine.this.mPendingReformGroupIndication = false;
                    handlP2pGroupRestart();
                } else {
                    P2pStateMachine.this.mSavedPeerConfig.invalidate();
                    WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                    if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                        WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.sendP2pNetworkChangedBroadcast();
                    }
                    P2pStateMachine.this.updateThisDevice(0);
                    WifiP2pServiceImpl.this.sendP2pConnectedStateBroadcast();
                    if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                        P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.this.mP2pServerAddress));
                    }
                    if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                    }
                }
                if (WifiP2pServiceImpl.this.getWifiRepeaterEnabled() && !WifiP2pServiceImpl.this.startWifiRepeater(P2pStateMachine.this.mGroup)) {
                    P2pStateMachine.this.sendMessage(139280);
                }
                WifiP2pServiceImpl.this.notifyP2pChannelNumber(WifiCommonUtils.convertFrequencyToChannelNumber(P2pStateMachine.this.mGroup.getFrequence()));
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append("when=");
                stringBuilder.append(message.getWhen());
                stringBuilder.append(" what=");
                stringBuilder.append(message.what);
                stringBuilder.append(" arg1=");
                stringBuilder.append(message.arg1);
                stringBuilder.append(" arg2=");
                stringBuilder.append(message.arg2);
                p2pStateMachine.logd(stringBuilder.toString());
                if (WifiP2pServiceImpl.this.processMessageForP2pCollision(message, this)) {
                    return true;
                }
                P2pStateMachine p2pStateMachine2;
                P2pStateMachine p2pStateMachine3;
                StringBuilder stringBuilder2;
                P2pStateMachine p2pStateMachine4;
                StringBuilder stringBuilder3;
                WifiP2pDevice device;
                StringBuilder stringBuilder4;
                String deviceAddress;
                switch (message.what) {
                    case 139271:
                        WifiP2pConfig config = message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            p2pStateMachine2 = P2pStateMachine.this;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Inviting device : ");
                            stringBuilder5.append(config.deviceAddress);
                            p2pStateMachine2.logd(stringBuilder5.toString());
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            if (!P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config.deviceAddress)) {
                                P2pStateMachine.this.replyToMessage(message, 139272, 0);
                                break;
                            }
                            P2pStateMachine.this.mPeers.updateStatus(config.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                            break;
                        }
                        p2pStateMachine3 = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Dropping connect request ");
                        stringBuilder2.append(config);
                        p2pStateMachine3.loge(stringBuilder2.toString());
                        P2pStateMachine.this.replyToMessage(message, 139272);
                        break;
                    case 139280:
                        p2pStateMachine4 = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" remove group");
                        p2pStateMachine4.logd(stringBuilder2.toString());
                        if (WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                            WifiP2pServiceImpl.this.stopWifiRepeater(P2pStateMachine.this.mGroup);
                        }
                        P2pStateMachine.this.enableBTCoex();
                        if (!P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                            P2pStateMachine.this.handleGroupRemoved();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            P2pStateMachine.this.replyToMessage(message, 139281, 0);
                            break;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mOngoingGroupRemovalState);
                        P2pStateMachine.this.replyToMessage(message, 139282);
                        break;
                    case 139326:
                        WpsInfo wps = message.obj;
                        int i = 139327;
                        if (wps != null) {
                            boolean ret = true;
                            if (wps.setup == 0) {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                            } else if (wps.pin == null) {
                                String pin = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface(), null);
                                try {
                                    Integer.parseInt(pin);
                                    P2pStateMachine.this.notifyInvitationSent(pin, "any");
                                } catch (NumberFormatException e) {
                                    ret = false;
                                }
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wps.pin);
                            }
                            P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                            if (ret) {
                                i = 139328;
                            }
                            p2pStateMachine5.replyToMessage(message, i);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139327);
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                        P2pStateMachine.this.sendMessage(139280);
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION /*143390*/:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), false);
                        WifiP2pServiceImpl.this.mIpClient.completedPreDhcpAction();
                        break;
                    case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION /*143391*/:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                        break;
                    case WifiP2pServiceImpl.IPC_DHCP_RESULTS /*143392*/:
                        WifiP2pServiceImpl.this.mDhcpResults = (DhcpResults) message.obj;
                        break;
                    case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS /*143393*/:
                        p2pStateMachine4 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mDhcpResults: ");
                        stringBuilder3.append(WifiP2pServiceImpl.this.mDhcpResults);
                        p2pStateMachine4.logd(stringBuilder3.toString());
                        P2pStateMachine.this.enableBTCoex();
                        if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(WifiP2pServiceImpl.this.mDhcpResults.serverAddress);
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        try {
                            String ifname = P2pStateMachine.this.mGroup.getInterface();
                            if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                                WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(ifname, WifiP2pServiceImpl.this.mDhcpResults.getRoutes(ifname));
                                break;
                            }
                        } catch (RemoteException e2) {
                            p2pStateMachine3 = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to add iface to local network ");
                            stringBuilder2.append(e2);
                            p2pStateMachine3.loge(stringBuilder2.toString());
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE /*143394*/:
                        P2pStateMachine.this.loge("IP provisioning failed");
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            return false;
                        }
                        device = message.obj;
                        if (!P2pStateMachine.this.mGroup.contains(device)) {
                            return false;
                        }
                        p2pStateMachine4 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Add device to lost list ");
                        stringBuilder3.append(device);
                        p2pStateMachine4.logd(stringBuilder3.toString());
                        P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                        break;
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                        if (WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                            WifiP2pServiceImpl.this.stopWifiRepeater(P2pStateMachine.this.mGroup);
                        }
                        p2pStateMachine4 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(getName());
                        stringBuilder3.append(" group removed");
                        p2pStateMachine4.logd(stringBuilder3.toString());
                        P2pStateMachine.this.enableBTCoex();
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT /*147488*/:
                        P2pStatus status = message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            p2pStateMachine2 = P2pStateMachine.this;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Invitation result ");
                            stringBuilder4.append(status);
                            p2pStateMachine2.loge(stringBuilder4.toString());
                            if (status == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int netId = P2pStateMachine.this.mGroup.getNetworkId();
                                if (netId >= 0) {
                                    P2pStateMachine.this.logd("Remove unknown client from the list");
                                    P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, false);
                                    P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                                    break;
                                }
                            }
                        }
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        WifiP2pProvDiscEvent provDisc = message.obj;
                        P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                        if (!(provDisc == null || provDisc.device == null)) {
                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = provDisc.device.deviceAddress;
                        }
                        if (message.what == WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                        } else if (message.what == WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                        } else {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                        }
                        p2pStateMachine3 = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mGroup.isGroupOwner()");
                        stringBuilder2.append(P2pStateMachine.this.mGroup.isGroupOwner());
                        p2pStateMachine3.logd(stringBuilder2.toString());
                        if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                            P2pStateMachine.this.logd("Device is GO, going to mUserAuthorizingJoinState");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingJoinState);
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_REMOVE_AND_REFORM_GROUP_EVENT /*147496*/:
                        Slog.d(WifiP2pServiceImpl.TAG, "Received event P2P_REMOVE_AND_REFORM_GROUP, remove P2P group");
                        handlP2pGroupRestart();
                        break;
                    case WifiP2pMonitor.AP_STA_DISCONNECTED_EVENT /*147497*/:
                        if (message.obj != null) {
                            device = message.obj;
                            deviceAddress = device.deviceAddress;
                            if (deviceAddress != null) {
                                P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 3);
                                if (P2pStateMachine.this.mGroup.removeClient(deviceAddress)) {
                                    p2pStateMachine4 = P2pStateMachine.this;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Removed client ");
                                    stringBuilder3.append(deviceAddress);
                                    p2pStateMachine4.logd(stringBuilder3.toString());
                                    if (WifiP2pServiceImpl.this.mAutonomousGroup || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                    } else {
                                        P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                    }
                                } else {
                                    p2pStateMachine4 = P2pStateMachine.this;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Failed to remove client ");
                                    stringBuilder3.append(deviceAddress);
                                    p2pStateMachine4.logd(stringBuilder3.toString());
                                    for (WifiP2pDevice c : P2pStateMachine.this.mGroup.getClientList()) {
                                        p2pStateMachine2 = P2pStateMachine.this;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("client ");
                                        stringBuilder4.append(c.deviceAddress);
                                        p2pStateMachine2.logd(stringBuilder4.toString());
                                    }
                                }
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                p2pStateMachine4 = P2pStateMachine.this;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(getName());
                                stringBuilder3.append(" ap sta disconnected");
                                p2pStateMachine4.logd(stringBuilder3.toString());
                            } else {
                                p2pStateMachine4 = P2pStateMachine.this;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Disconnect on unknown device: ");
                                stringBuilder3.append(device);
                                p2pStateMachine4.loge(stringBuilder3.toString());
                            }
                            WifiP2pServiceImpl.this.handleClientDisconnect(P2pStateMachine.this.mGroup);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case WifiP2pMonitor.AP_STA_CONNECTED_EVENT /*147498*/:
                        if (message.obj != null) {
                            deviceAddress = message.obj.deviceAddress;
                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                            if (deviceAddress != null) {
                                if (P2pStateMachine.this.mPeers.get(deviceAddress) != null) {
                                    P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(deviceAddress));
                                } else {
                                    P2pStateMachine.this.mGroup.addClient(deviceAddress);
                                }
                                P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 0);
                                p2pStateMachine4 = P2pStateMachine.this;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(getName());
                                stringBuilder3.append(" ap sta connected");
                                p2pStateMachine4.logd(stringBuilder3.toString());
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                            } else {
                                P2pStateMachine.this.loge("Connect on null device address, ignore");
                            }
                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            WifiP2pServiceImpl.this.handleClientConnect(P2pStateMachine.this.mGroup);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    case 196612:
                        DhcpResults dhcpResults = message.obj;
                        P2pStateMachine.this.enableBTCoex();
                        if (message.arg1 == 1 && dhcpResults != null) {
                            p2pStateMachine3 = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DhcpResults: ");
                            stringBuilder2.append(dhcpResults);
                            p2pStateMachine3.logd(stringBuilder2.toString());
                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(dhcpResults.serverAddress);
                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                            try {
                                if (!WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                                    String iface = P2pStateMachine.this.mGroup.getInterface();
                                    WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(iface, dhcpResults.getRoutes(iface));
                                    break;
                                }
                                WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork("wlan0", dhcpResults.getRoutes("wlan0"));
                                break;
                            } catch (RemoteException e3) {
                                p2pStateMachine2 = P2pStateMachine.this;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Failed to add iface to local network ");
                                stringBuilder4.append(e3);
                                p2pStateMachine2.loge(stringBuilder4.toString());
                                break;
                            } catch (IllegalStateException e4) {
                                p2pStateMachine2 = P2pStateMachine.this;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Failed to add iface to local network ");
                                stringBuilder4.append(e4);
                                p2pStateMachine2.loge(stringBuilder4.toString());
                                break;
                            } catch (IllegalArgumentException e5) {
                                p2pStateMachine2 = P2pStateMachine.this;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Failed to add iface to local network: ");
                                stringBuilder4.append(e5);
                                p2pStateMachine2.loge(stringBuilder4.toString());
                                break;
                            }
                        }
                        P2pStateMachine.this.loge("DHCP failed");
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        break;
                        break;
                    default:
                        return WifiP2pServiceImpl.this.handleGroupCreatedStateExMessage(message);
                }
                return true;
            }

            public void exit() {
                P2pStateMachine.this.updateThisDevice(3);
                P2pStateMachine.this.resetWifiP2pInfo();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
                if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                    WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.sendP2pNetworkChangedBroadcast();
                }
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                WifiP2pServiceImpl.this.notifyP2pState(WifiCommonUtils.STATE_DISCONNECTED);
            }
        }

        class GroupCreatingState extends State {
            GroupCreatingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.access$5404(), 0), 120000);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                if (WifiP2pServiceImpl.this.processMessageForP2pCollision(message, this)) {
                    return true;
                }
                boolean ret = true;
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139274:
                        P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.replyToMessage(message, 139276);
                        break;
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                        if (WifiP2pServiceImpl.sGroupCreatingTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.logd("Group negotiation timed out");
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj != null) {
                            WifiP2pDevice device = message.obj;
                            P2pStateMachine p2pStateMachine2;
                            StringBuilder stringBuilder2;
                            if (!P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                                p2pStateMachine2 = P2pStateMachine.this;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("mSavedPeerConfig ");
                                stringBuilder2.append(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                stringBuilder2.append("device ");
                                stringBuilder2.append(device.deviceAddress);
                                p2pStateMachine2.logd(stringBuilder2.toString());
                                ret = false;
                                break;
                            }
                            p2pStateMachine2 = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Add device to lost list ");
                            stringBuilder2.append(device);
                            p2pStateMachine2.logd(stringBuilder2.toString());
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    default:
                        ret = false;
                        break;
                }
                return ret;
            }
        }

        class GroupNegotiationState extends State {
            GroupNegotiationState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                WifiP2pServiceImpl.this.sendP2pConnectingStateBroadcast();
                P2pStateMachine.this.mPendingReformGroupIndication = false;
            }

            /* Code decompiled incorrectly, please refer to instructions dump. */
            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                int i = message.what;
                if (i != 139280) {
                    if (i == WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT) {
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(getName());
                        stringBuilder.append(" deal with P2P_GO_NEGOTIATION_REQUEST_EVENT");
                        p2pStateMachine.logd(stringBuilder.toString());
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                    } else if (i == WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT) {
                        P2pStatus status = (P2pStatus) message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invitation result ");
                            stringBuilder2.append(status);
                            p2pStateMachine2.loge(stringBuilder2.toString());
                            if (status == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int netId = P2pStateMachine.this.mSavedPeerConfig.netId;
                                if (netId >= 0) {
                                    P2pStateMachine.this.logd("Remove unknown client from the list");
                                    P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, true);
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (status == P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE) {
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (status == P2pStatus.NO_COMMON_CHANNEL) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            } else {
                                P2pStateMachine.this.handleGroupCreationFailure();
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            }
                        }
                    } else if (i != WifiP2pMonitor.P2P_REMOVE_AND_REFORM_GROUP_EVENT) {
                        switch (i) {
                            case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT /*147481*/:
                            case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT /*147483*/:
                                p2pStateMachine = P2pStateMachine.this;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(getName());
                                stringBuilder.append(" go success");
                                p2pStateMachine.logd(stringBuilder.toString());
                                break;
                            case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT /*147482*/:
                                if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                                    break;
                                }
                            case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT /*147484*/:
                                if (message.obj == P2pStatus.NO_COMMON_CHANNEL) {
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                                    break;
                                }
                                break;
                            case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                                if (message.obj != null) {
                                    P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                                    p2pStateMachine = P2pStateMachine.this;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(getName());
                                    stringBuilder.append(" group started");
                                    p2pStateMachine.logd(stringBuilder.toString());
                                    if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                        P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                                    }
                                    if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                                        P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                                        P2pStateMachine.this.mGroup.setNetworkId(P2pStateMachine.this.mGroups.getNetworkId(P2pStateMachine.this.mGroup.getOwner().deviceAddress, P2pStateMachine.this.mGroup.getNetworkName()));
                                    }
                                    if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                        WifiP2pServiceImpl.this.sendGroupConfigInfo(P2pStateMachine.this.mGroup);
                                        if (!WifiP2pServiceImpl.this.mAutonomousGroup) {
                                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                        }
                                        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                                            P2pStateMachine.this.startDhcpServer("wlan0");
                                        } else {
                                            P2pStateMachine.this.startDhcpServer(P2pStateMachine.this.mGroup.getInterface());
                                        }
                                    } else {
                                        if (!WifiP2pServiceImpl.this.getMagicLinkDeviceFlag()) {
                                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                            if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                                                WifiP2pServiceImpl.this.startIpClient("wlan0");
                                            } else {
                                                WifiP2pServiceImpl.this.startIpClient(P2pStateMachine.this.mGroup.getInterface());
                                            }
                                            if (P2pStateMachine.this.mWifiInjector == null) {
                                                P2pStateMachine.this.mWifiInjector = WifiInjector.getInstance();
                                            }
                                            P2pStateMachine.this.mWifiInjector.getWifiNative().setBluetoothCoexistenceMode(P2pStateMachine.this.mInterfaceName, 1);
                                            P2pStateMachine.this.mIsBTCoexDisabled = true;
                                        }
                                        WifiP2pDevice groupOwner = P2pStateMachine.this.mGroup.getOwner();
                                        if (groupOwner != null) {
                                            WifiP2pDevice peer = P2pStateMachine.this.mPeers.get(groupOwner.deviceAddress);
                                            if (peer != null) {
                                                groupOwner.updateSupplicantDetails(peer);
                                                P2pStateMachine.this.mPeers.updateStatus(groupOwner.deviceAddress, 0);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                            } else {
                                                if (!(groupOwner == null || WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(groupOwner.deviceAddress))) {
                                                    Matcher match = Pattern.compile("([0-9a-f]{2}:){5}[0-9a-f]{2}").matcher(groupOwner.deviceAddress);
                                                    Log.e(WifiP2pServiceImpl.TAG, "try to judge groupOwner is valid or not");
                                                    if (match.find()) {
                                                        groupOwner.primaryDeviceType = "10-0050F204-5";
                                                        P2pStateMachine.this.mPeers.updateSupplicantDetails(groupOwner);
                                                        P2pStateMachine.this.mPeers.updateStatus(groupOwner.deviceAddress, 0);
                                                        P2pStateMachine.this.sendPeersChangedBroadcast();
                                                    }
                                                }
                                                P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("Unknown group owner ");
                                                stringBuilder3.append(groupOwner);
                                                p2pStateMachine3.logw(stringBuilder3.toString());
                                            }
                                        } else {
                                            P2pStateMachine.this.loge("Group owner is null.");
                                        }
                                    }
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                                    break;
                                }
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                break;
                            case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT /*147486*/:
                                p2pStateMachine = P2pStateMachine.this;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(getName());
                                stringBuilder.append(" go failure");
                                p2pStateMachine.logd(stringBuilder.toString());
                                P2pStateMachine.this.handleGroupCreationFailure();
                                WifiP2pServiceImpl.this.sendP2pFailStateBroadcast();
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                                break;
                            default:
                                return WifiP2pServiceImpl.this.handleGroupNegotiationStateExMessage(message);
                        }
                    } else {
                        P2pStateMachine.this.logd("P2P_REMOVE_AND_REFORM_GROUP_EVENT event received in GroupNegotiationState state");
                        P2pStateMachine.this.mPendingReformGroupIndication = true;
                    }
                } else if (!WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                    return false;
                } else {
                    P2pStateMachine.this.deferMessage(message);
                }
                return true;
            }
        }

        class InactiveState extends State {
            InactiveState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                WifiP2pServiceImpl.this.mIsInvite = false;
                WifiP2pServiceImpl.this.setmMagicLinkDeviceFlag(false);
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.what);
                p2pStateMachine.logd(stringBuilder.toString());
                if (WifiP2pServiceImpl.this.processMessageForP2pCollision(message, this)) {
                    return true;
                }
                StringBuilder stringBuilder2;
                WifiP2pConfig config;
                P2pStateMachine p2pStateMachine2;
                StringBuilder stringBuilder3;
                int oc;
                P2pStateMachine p2pStateMachine3;
                StringBuilder stringBuilder4;
                String handoverSelect;
                WifiP2pDevice owner;
                switch (message.what) {
                    case 139268:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.mWifiNative.p2pFlush();
                            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                            P2pStateMachine.this.replyToMessage(message, 139270);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        }
                        WifiP2pServiceImpl.this.handleP2pStopFind(message.sendingUid);
                        break;
                    case 139271:
                        WifiP2pServiceImpl.this.setmMagicLinkDeviceFlag(false);
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" sending connect");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        config = message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                            if (P2pStateMachine.this.reinvokePersistentGroup(config)) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mProvisionDiscoveryState);
                            }
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                            break;
                        }
                        p2pStateMachine2 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Dropping connect requeset ");
                        stringBuilder3.append(config);
                        p2pStateMachine2.loge(stringBuilder3.toString());
                        P2pStateMachine.this.replyToMessage(message, 139272);
                        break;
                    case 139277:
                        boolean ret;
                        WifiP2pServiceImpl.this.mAutonomousGroup = true;
                        if (message.arg1 == -2) {
                            int netId = P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                            if (netId != -1) {
                                ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(netId);
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(true);
                            }
                        } else {
                            ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(false);
                        }
                        if (!ret) {
                            P2pStateMachine.this.replyToMessage(message, 139278, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139279);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case 139329:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" start listen mode");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (!P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139331);
                        break;
                    case 139332:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" stop listen mode");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        break;
                    case 139335:
                        if (message.obj != null) {
                            Bundle p2pChannels = message.obj;
                            int lc = p2pChannels.getInt("lc", 0);
                            oc = p2pChannels.getInt("oc", 0);
                            p2pStateMachine3 = P2pStateMachine.this;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(getName());
                            stringBuilder4.append(" set listen and operating channel");
                            p2pStateMachine3.logd(stringBuilder4.toString());
                            if (!P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                                P2pStateMachine.this.replyToMessage(message, 139336);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139337);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments(s)");
                        break;
                    case 139342:
                        handoverSelect = null;
                        if (message.obj != null) {
                            handoverSelect = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverSelect != null && P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(handoverSelect)) {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139345);
                        break;
                        break;
                    case 139343:
                        handoverSelect = null;
                        if (message.obj != null) {
                            handoverSelect = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverSelect != null && P2pStateMachine.this.mWifiNative.responderReportNfcHandover(handoverSelect)) {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139345);
                        break;
                    case 141268:
                        WifiP2pServiceImpl.this.mCreateWifiBridge = true;
                        return WifiP2pServiceImpl.this.handleInactiveStateMessage(message);
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT /*147479*/:
                        config = message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                            break;
                        }
                        p2pStateMachine2 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Dropping GO neg request ");
                        stringBuilder3.append(config);
                        p2pStateMachine2.loge(stringBuilder3.toString());
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT /*147485*/:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(getName());
                            stringBuilder2.append(" group started");
                            p2pStateMachine.logd(stringBuilder2.toString());
                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                            }
                            if (P2pStateMachine.this.mGroup.getNetworkId() != -2) {
                                p2pStateMachine = P2pStateMachine.this;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unexpected group creation, remove ");
                                stringBuilder2.append(P2pStateMachine.this.mGroup);
                                p2pStateMachine.loge(stringBuilder2.toString());
                                P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                break;
                            }
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            P2pStateMachine.this.deferMessage(message);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT /*147487*/:
                        if (message.obj != null) {
                            WifiP2pGroup group = message.obj;
                            owner = group.getOwner();
                            if (owner == null) {
                                oc = group.getNetworkId();
                                if (oc >= 0) {
                                    String addr = P2pStateMachine.this.mGroups.getOwnerAddr(oc);
                                    if (addr == null) {
                                        P2pStateMachine.this.loge("Ignored invitation from null owner");
                                        break;
                                    }
                                    group.setOwner(new WifiP2pDevice(addr));
                                    owner = group.getOwner();
                                } else {
                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                    break;
                                }
                            }
                            WifiP2pConfig config2 = new WifiP2pConfig();
                            config2.deviceAddress = group.getOwner().deviceAddress;
                            if (!P2pStateMachine.this.isConfigInvalid(config2)) {
                                P2pStateMachine.this.mSavedPeerConfig = config2;
                                if (owner != null) {
                                    WifiP2pDevice wifiP2pDevice = P2pStateMachine.this.mPeers.get(owner.deviceAddress);
                                    owner = wifiP2pDevice;
                                    if (wifiP2pDevice != null) {
                                        if (owner.wpsPbcSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                        } else if (owner.wpsKeypadSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                        } else if (owner.wpsDisplaySupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                        }
                                    }
                                }
                                WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                WifiP2pServiceImpl.this.mIsInvite = true;
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingInviteRequestState);
                                break;
                            }
                            p2pStateMachine3 = P2pStateMachine.this;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Dropping invitation request ");
                            stringBuilder4.append(config2);
                            p2pStateMachine3.loge(stringBuilder4.toString());
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        if (message.obj != null) {
                            WifiP2pProvDiscEvent provDisc = message.obj;
                            owner = provDisc.device;
                            if (owner != null) {
                                P2pStateMachine.this.notifyP2pProvDiscShowPinRequest(provDisc.pin, owner.deviceAddress);
                                P2pStateMachine.this.mPeers.updateStatus(owner.deviceAddress, 1);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                break;
                            }
                            P2pStateMachine.this.loge("Device entry is null");
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    default:
                        return WifiP2pServiceImpl.this.handleInactiveStateMessage(message);
                }
                return true;
            }
        }

        class OngoingGroupRemovalState extends State {
            OngoingGroupRemovalState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                int i = message.what;
                if (i == 139280) {
                    P2pStateMachine.this.replyToMessage(message, 139282);
                } else if (i != 141268) {
                    return WifiP2pServiceImpl.this.handleOngoingGroupRemovalStateExMessage(message);
                } else {
                    if (!WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                        P2pStateMachine.this.deferMessage(message);
                    }
                }
                return true;
            }
        }

        class P2pDisabledState extends State {
            P2pDisabledState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine;
                StringBuilder stringBuilder;
                P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(getName());
                stringBuilder2.append(message.toString());
                p2pStateMachine2.logd(stringBuilder2.toString());
                if (message.what != WifiP2pServiceImpl.ENABLE_P2P) {
                    return false;
                }
                if (P2pStateMachine.this.mIsWifiEnabled) {
                    P2pStateMachine.this.mInterfaceName = P2pStateMachine.this.mWifiNative.setupInterface(new -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM(this), P2pStateMachine.this.getHandler());
                    if (P2pStateMachine.this.mInterfaceName == null) {
                        Log.e(WifiP2pServiceImpl.TAG, "Failed to setup interface for P2P");
                    } else {
                        try {
                            WifiP2pServiceImpl.this.mNwService.setInterfaceUp(P2pStateMachine.this.mInterfaceName);
                        } catch (RemoteException re) {
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unable to change interface settings: ");
                            stringBuilder.append(re);
                            p2pStateMachine.loge(stringBuilder.toString());
                        } catch (IllegalStateException ie) {
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unable to change interface settings: ");
                            stringBuilder.append(ie);
                            p2pStateMachine.loge(stringBuilder.toString());
                        }
                        P2pStateMachine.this.registerForWifiMonitorEvents();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                    }
                } else {
                    Log.e(WifiP2pServiceImpl.TAG, "Ignore P2P enable since wifi is disabled");
                }
                return true;
            }
        }

        class P2pDisablingState extends State {
            P2pDisablingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.access$1804(), 0), 5000);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.what);
                p2pStateMachine.logd(stringBuilder.toString());
                switch (message.what) {
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                        if (WifiP2pServiceImpl.sDisableP2pTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case 147458:
                        P2pStateMachine.this.logd("p2p socket connection lost");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                } else {
                    P2pStateMachine.this.loge("P2pDisablingState exit(): WifiChannel is null");
                }
            }
        }

        class P2pEnabledState extends State {
            P2pEnabledState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                if (!WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                } else if (WifiP2pServiceImpl.this.isWifiP2pEnabled()) {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                }
                P2pStateMachine.this.initializeP2pSettings();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" when=");
                stringBuilder.append(message.getWhen());
                stringBuilder.append(" what=");
                stringBuilder.append(message.what);
                stringBuilder.append(" arg1=");
                stringBuilder.append(message.arg1);
                stringBuilder.append(" arg2=");
                stringBuilder.append(message.arg2);
                p2pStateMachine.logd(stringBuilder.toString());
                if (WifiP2pServiceImpl.this.processMessageForP2pCollision(message, this)) {
                    return true;
                }
                StringBuilder stringBuilder2;
                WifiP2pDevice d;
                Bundle p2pChannels;
                P2pStateMachine p2pStateMachine2;
                StringBuilder stringBuilder3;
                switch (message.what) {
                    case 139265:
                        if (!WifiP2pServiceImpl.this.mDiscoveryBlocked && WifiP2pServiceImpl.this.allowP2pFind(message.sendingUid)) {
                            P2pStateMachine.this.clearSupplicantServiceRequest();
                            if (!P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.this.addScanChannelInTimeout(message.arg1, WifiP2pServiceImpl.DISCOVER_TIMEOUT_S))) {
                                P2pStateMachine.this.replyToMessage(message, 139266, 0);
                                WifiP2pServiceImpl.this.handleP2pStopFind(message.sendingUid);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139267);
                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                            if (WifiP2pServiceImpl.this.mHwWifiCHRService != null) {
                                WifiP2pServiceImpl.this.mHwWifiCHRService.updateApkChangewWifiStatus(13, WifiP2pServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid));
                                break;
                            }
                        }
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                        break;
                    case 139268:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139270);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        }
                        WifiP2pServiceImpl.this.handleP2pStopFind(message.sendingUid);
                        break;
                    case 139292:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" add service");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        if (!P2pStateMachine.this.addLocalService(message.replyTo, (WifiP2pServiceInfo) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139293);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139294);
                        break;
                    case 139295:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" remove service");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.removeLocalService(message.replyTo, message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139297);
                        break;
                    case 139298:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" clear service");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.clearLocalServices(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139300);
                        break;
                    case 139301:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" add service request");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        if (!P2pStateMachine.this.addServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139302);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139303);
                        break;
                    case 139304:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" remove service request");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.removeServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139306);
                        break;
                    case 139307:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" clear service request");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.clearServiceRequests(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139309);
                        break;
                    case 139310:
                        if (!WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            p2pStateMachine = P2pStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(getName());
                            stringBuilder2.append(" discover services");
                            p2pStateMachine.logd(stringBuilder2.toString());
                            if (P2pStateMachine.this.updateSupplicantServiceRequest()) {
                                if (!P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S)) {
                                    P2pStateMachine.this.replyToMessage(message, 139311, 0);
                                    break;
                                }
                                P2pStateMachine.this.replyToMessage(message, 139312);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139311, 3);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        d = message.obj;
                        if (d != null && P2pStateMachine.this.setAndPersistDeviceName(d.deviceName)) {
                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("set device name ");
                            stringBuilder4.append(d.deviceName);
                            p2pStateMachine3.logd(stringBuilder4.toString());
                            P2pStateMachine.this.replyToMessage(message, 139317);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139316, 0);
                        break;
                    case 139318:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" delete persistent group");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.mGroups.remove(message.arg1);
                        P2pStateMachine.this.replyToMessage(message, 139320);
                        break;
                    case 139323:
                        WifiP2pWfdInfo d2 = message.obj;
                        if (WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            if (d2 != null && P2pStateMachine.this.setWfdInfo(d2)) {
                                P2pStateMachine.this.replyToMessage(message, 139325);
                                break;
                            }
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        break;
                        break;
                    case 139329:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" start listen mode");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (!P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139331);
                        break;
                    case 139332:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" stop listen mode");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        break;
                    case 139335:
                        p2pChannels = message.obj;
                        int lc = p2pChannels.getInt("lc", 0);
                        int oc = p2pChannels.getInt("oc", 0);
                        p2pStateMachine2 = P2pStateMachine.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(getName());
                        stringBuilder3.append(" set listen and operating channel");
                        p2pStateMachine2.logd(stringBuilder3.toString());
                        if (!P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139336);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139337);
                        break;
                    case 139339:
                        p2pChannels = new Bundle();
                        p2pChannels.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverRequest());
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) p2pChannels);
                        break;
                    case 139340:
                        p2pChannels = new Bundle();
                        p2pChannels.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverSelect());
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) p2pChannels);
                        break;
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                        P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                        boolean blocked = message.arg1 == 1;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked != blocked) {
                            WifiP2pServiceImpl.this.mDiscoveryBlocked = blocked;
                            if (blocked && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                            }
                            if (!blocked && WifiP2pServiceImpl.this.mDiscoveryPostponed) {
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                            }
                            if (blocked) {
                                if (message.obj != null) {
                                    try {
                                        message.obj.sendMessage(message.arg2);
                                        break;
                                    } catch (Exception e) {
                                        p2pStateMachine2 = P2pStateMachine.this;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("unable to send BLOCK_DISCOVERY response: ");
                                        stringBuilder3.append(e);
                                        p2pStateMachine2.loge(stringBuilder3.toString());
                                        break;
                                    }
                                }
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                break;
                            }
                        }
                        break;
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                        if (P2pStateMachine.this.mPeers.clear()) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        if (P2pStateMachine.this.mGroups.clear()) {
                            P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                        }
                        P2pStateMachine.this.mWifiMonitor.stopMonitoring(P2pStateMachine.this.mInterfaceName);
                        P2pStateMachine.this.mWifiNative.teardownInterface();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisablingState);
                        break;
                    case 147458:
                        P2pStateMachine.this.loge("Unexpected loss of p2p socket connection");
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(147458);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT /*147477*/:
                        if (message.obj != null) {
                            d = (WifiP2pDevice) message.obj;
                            if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(d.deviceAddress)) {
                                P2pStateMachine.this.mPeers.updateSupplicantDetails(d);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                if (WifiP2pServiceHisiExt.hisiWifiEnabled() && WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.P2pFindDeviceUpdate) {
                                    WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.P2pFindDeviceUpdate = false;
                                    P2pStateMachine.this.updatePersistentNetworks(true);
                                    break;
                                }
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT /*147478*/:
                        if (message.obj != null) {
                            if (P2pStateMachine.this.mPeers.remove(message.obj.deviceAddress) != null) {
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                break;
                            }
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                        break;
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT /*147493*/:
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                        WifiP2pServiceImpl.this.handleP2pStopFind(message.sendingUid);
                        break;
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT /*147494*/:
                        p2pStateMachine = P2pStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getName());
                        stringBuilder2.append(" receive service response");
                        p2pStateMachine.logd(stringBuilder2.toString());
                        if (message.obj != null) {
                            for (WifiP2pServiceResponse resp : message.obj) {
                                resp.setSrcDevice(P2pStateMachine.this.mPeers.get(resp.getSrcDevice().deviceAddress));
                                P2pStateMachine.this.sendServiceResponse(resp);
                            }
                            break;
                        }
                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        break;
                    default:
                        return WifiP2pServiceImpl.this.handleP2pEnabledStateExMessage(message);
                }
                return true;
            }

            public void exit() {
                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(false);
            }
        }

        class P2pNotSupportedState extends State {
            P2pNotSupportedState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 1);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 1);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 1);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 1);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 1);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 1);
                        if (WifiP2pServiceImpl.this.getWifiRepeaterEnabled()) {
                            WifiP2pServiceImpl.this.stopWifiRepeater(P2pStateMachine.this.mGroup);
                            break;
                        }
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 1);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 1);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 1);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 1);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 1);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 1);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 1);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 1);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 1);
                        break;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139324, 1);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 1);
                        break;
                    case 139329:
                        P2pStateMachine.this.replyToMessage(message, 139330, 1);
                        break;
                    case 139332:
                        P2pStateMachine.this.replyToMessage(message, 139333, 1);
                        break;
                    default:
                        return WifiP2pServiceImpl.this.handleP2pNotSupportedStateMessage(message);
                }
                return true;
            }
        }

        class ProvisionDiscoveryState extends State {
            ProvisionDiscoveryState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                int i = message.what;
                P2pStateMachine p2pStateMachine2;
                StringBuilder stringBuilder2;
                if (i == WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT) {
                    p2pStateMachine2 = P2pStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(getName());
                    stringBuilder2.append(" deal with P2P_GO_NEGOTIATION_REQUEST_EVENT");
                    p2pStateMachine2.logd(stringBuilder2.toString());
                    P2pStateMachine.this.deferMessage(message);
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                } else if (i != WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT) {
                    WifiP2pDevice device;
                    switch (i) {
                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT /*147490*/:
                            if (message.obj != null) {
                                device = message.obj.device;
                                if ((device == null || device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                    p2pStateMachine2 = P2pStateMachine.this;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Found a match ");
                                    stringBuilder2.append(P2pStateMachine.this.mSavedPeerConfig);
                                    p2pStateMachine2.logd(stringBuilder2.toString());
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                            break;
                        case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                            if (message.obj != null) {
                                device = message.obj.device;
                                if ((device == null || device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                    p2pStateMachine2 = P2pStateMachine.this;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Found a match ");
                                    stringBuilder3.append(P2pStateMachine.this.mSavedPeerConfig);
                                    p2pStateMachine2.logd(stringBuilder3.toString());
                                    if (!TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                        break;
                                    }
                                    WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                                    break;
                                }
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            break;
                        case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                            if (message.obj != null) {
                                WifiP2pProvDiscEvent provDisc = message.obj;
                                device = provDisc.device;
                                if (device != null) {
                                    if (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                                        p2pStateMachine2 = P2pStateMachine.this;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Found a match ");
                                        stringBuilder2.append(P2pStateMachine.this.mSavedPeerConfig);
                                        p2pStateMachine2.logd(stringBuilder2.toString());
                                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.notifyInvitationSent(provDisc.pin, device.deviceAddress);
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                        break;
                                    }
                                }
                                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                                break;
                            }
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            break;
                        default:
                            return false;
                    }
                } else {
                    P2pStateMachine.this.loge("provision discovery failed");
                    P2pStateMachine.this.handleGroupCreationFailure();
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                }
                return true;
            }
        }

        class UserAuthorizingInviteRequestState extends State {
            UserAuthorizingInviteRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("User rejected invitation ");
                        stringBuilder2.append(P2pStateMachine.this.mSavedPeerConfig);
                        p2pStateMachine2.logd(stringBuilder2.toString());
                        P2pStateMachine.this.mWifiNative.p2pReject(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        class UserAuthorizingJoinState extends State {
            UserAuthorizingJoinState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                        } else {
                            P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mAfterUserAuthorizingJoinState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        P2pStateMachine.this.logd("User rejected incoming request");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mAfterUserAuthorizingJoinState);
                        break;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT /*147489*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT /*147491*/:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT /*147492*/:
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        class UserAuthorizingNegotiationRequestState extends State {
            UserAuthorizingNegotiationRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(message.toString());
                p2pStateMachine.logd(stringBuilder.toString());
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("User rejected negotiation ");
                        stringBuilder2.append(P2pStateMachine.this.mSavedPeerConfig);
                        p2pStateMachine2.logd(stringBuilder2.toString());
                        P2pStateMachine.this.mWifiNative.p2pReject(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 3);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.logd("p2p_reject delay 1500ms to start find ");
                        P2pStateMachine.this.sendMessageDelayed(139265, 1500);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
            }
        }

        P2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(name, looper);
            if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.mWifiP2pInfo = this.mWifiP2pInfo;
                WifiP2pServiceImpl.this.mWifiP2pServiceHisiExt.mGroup = this.mGroup;
            }
            addState(this.mDefaultState);
            addState(this.mP2pNotSupportedState, this.mDefaultState);
            addState(this.mP2pDisablingState, this.mDefaultState);
            addState(this.mP2pDisabledState, this.mDefaultState);
            addState(this.mP2pEnabledState, this.mDefaultState);
            addState(this.mInactiveState, this.mP2pEnabledState);
            addState(this.mGroupCreatingState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingInviteRequestState, this.mGroupCreatingState);
            addState(this.mUserAuthorizingNegotiationRequestState, this.mGroupCreatingState);
            addState(this.mProvisionDiscoveryState, this.mGroupCreatingState);
            addState(this.mGroupNegotiationState, this.mGroupCreatingState);
            addState(this.mFrequencyConflictState, this.mGroupCreatingState);
            addState(this.mGroupCreatedState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingJoinState, this.mGroupCreatedState);
            addState(this.mAfterUserAuthorizingJoinState, this.mGroupCreatedState);
            addState(this.mOngoingGroupRemovalState, this.mGroupCreatedState);
            if (p2pSupported) {
                setInitialState(this.mP2pDisabledState);
            } else {
                setInitialState(this.mP2pNotSupportedState);
            }
            setLogRecSize(50);
            setLogOnlyTransitions(true);
            if (p2pSupported) {
                WifiP2pServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver(WifiP2pServiceImpl.this) {
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getIntExtra("wifi_state", 4) == 3) {
                            P2pStateMachine.this.mIsWifiEnabled = true;
                            WifiP2pServiceImpl.this.mWifiP2pDevCreateRetry = 0;
                            P2pStateMachine.this.checkAndReEnableP2p();
                        } else {
                            P2pStateMachine.this.mIsWifiEnabled = false;
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                        }
                        P2pStateMachine.this.checkAndSendP2pStateChangedBroadcast();
                    }
                }, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
                this.mWifiNative.registerInterfaceAvailableListener(new -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$zMDJmVHxNOQccRUsy4cDbijFDbc(this), getHandler());
            }
        }

        public static /* synthetic */ void lambda$new$0(P2pStateMachine p2pStateMachine, boolean isAvailable) {
            p2pStateMachine.mIsInterfaceAvailable = isAvailable;
            if (p2pStateMachine.mIsWifiEnabled && isAvailable && WifiP2pServiceImpl.this.mWifiP2pDevCreateRetry > 5) {
                Log.d(WifiP2pServiceImpl.TAG, "Endless loop, try to stop create P2P device!");
                p2pStateMachine.mIsInterfaceAvailable = false;
                WifiP2pServiceImpl.this.mWifiP2pDevCreateRetry = 0;
            }
            if (isAvailable) {
                p2pStateMachine.checkAndReEnableP2p();
                if (p2pStateMachine.mIsWifiEnabled) {
                    WifiP2pServiceImpl.this.mWifiP2pDevCreateRetry = WifiP2pServiceImpl.this.mWifiP2pDevCreateRetry + 1;
                }
            }
            p2pStateMachine.checkAndSendP2pStateChangedBroadcast();
            p2pStateMachine.mIsInterfaceAvailable = isAvailable;
        }

        public void registerForWifiMonitorEvents() {
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.AP_STA_CONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.AP_STA_DISCONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_LOST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_FIND_STOPPED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_STARTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147457, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147458, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_REMOVE_AND_REFORM_GROUP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147556, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147557, getHandler());
            this.mWifiMonitor.startMonitoring(this.mInterfaceName);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            super.dump(fd, pw, args);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mWifiP2pInfo ");
            stringBuilder.append(this.mWifiP2pInfo);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mGroup ");
            stringBuilder.append(this.mGroup);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSavedPeerConfig ");
            stringBuilder.append(this.mSavedPeerConfig);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mGroups");
            stringBuilder.append(this.mGroups);
            pw.println(stringBuilder.toString());
            pw.println();
        }

        private void checkAndReEnableP2p() {
            String str = WifiP2pServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wifi enabled=");
            stringBuilder.append(this.mIsWifiEnabled);
            stringBuilder.append(", P2P Interface availability=");
            stringBuilder.append(this.mIsInterfaceAvailable);
            stringBuilder.append(", Number of clients=");
            stringBuilder.append(WifiP2pServiceImpl.this.mDeathDataByBinder.size());
            Log.d(str, stringBuilder.toString());
            if (this.mIsWifiEnabled && this.mIsInterfaceAvailable && !WifiP2pServiceImpl.this.mDeathDataByBinder.isEmpty()) {
                sendMessage(WifiP2pServiceImpl.ENABLE_P2P);
            }
        }

        private void checkAndSendP2pStateChangedBroadcast() {
            String str = WifiP2pServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wifi enabled=");
            stringBuilder.append(this.mIsWifiEnabled);
            stringBuilder.append(", P2P Interface availability=");
            stringBuilder.append(this.mIsInterfaceAvailable);
            Log.d(str, stringBuilder.toString());
            boolean z = this.mIsWifiEnabled && this.mIsInterfaceAvailable;
            sendP2pStateChangedBroadcast(z);
        }

        private void sendP2pStateChangedBroadcast(boolean enabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("p2pState change broadcast ");
            stringBuilder.append(enabled);
            logd(stringBuilder.toString());
            Intent intent = new Intent("android.net.wifi.p2p.STATE_CHANGED");
            intent.addFlags(67108864);
            if (enabled) {
                intent.putExtra("wifi_p2p_state", 2);
            } else {
                intent.putExtra("wifi_p2p_state", 1);
            }
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pDiscoveryChangedBroadcast(boolean started) {
            if (WifiP2pServiceImpl.this.mDiscoveryStarted != started) {
                int i;
                WifiP2pServiceImpl.this.mDiscoveryStarted = started;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("discovery change broadcast ");
                stringBuilder.append(started);
                logd(stringBuilder.toString());
                Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
                intent.addFlags(67108864);
                String str = "discoveryState";
                if (started) {
                    i = 2;
                } else {
                    i = 1;
                }
                intent.putExtra(str, i);
                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
        }

        private void sendThisDeviceChangedBroadcast() {
            logd("sending this device change broadcast ");
            Intent intent = new Intent("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("wifiP2pDevice", new WifiP2pDevice(WifiP2pServiceImpl.this.mThisDevice));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendPeersChangedBroadcast() {
            logd("sending p2pPeers change broadcast");
            Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
            intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pConnectionChangedBroadcast() {
            logd("sending p2p connection changed broadcast");
            NetworkInfo networkInfo = new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo);
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", networkInfo);
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            initPowerSaveWhitelist(networkInfo);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            } else {
                loge("sendP2pConnectionChangedBroadcast(): WifiChannel is null");
            }
        }

        private void initPowerSaveWhitelist(NetworkInfo networkInfo) {
            if (this.mIDeviceIdleController == null) {
                this.mIDeviceIdleController = Stub.asInterface(ServiceManager.getService("deviceidle"));
            }
            logd("initPowerSaveWhitelist");
            removeMessages(-1);
            if (networkInfo.isConnected()) {
                addPowerSaveWhitelist();
            } else {
                sendMessageDelayed(-1, 2000);
            }
        }

        private void addPowerSaveWhitelist() {
            try {
                this.mIDeviceIdleController.addPowerSaveWhitelistApp(WifiP2pServiceImpl.PACKAGE_NAME);
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addPowerSaveWhitelistApp RemoteException : ");
                stringBuilder.append(e.toString());
                loge(stringBuilder.toString());
            }
        }

        private void removePowerSaveWhitelist() {
            try {
                this.mIDeviceIdleController.removePowerSaveWhitelistApp(WifiP2pServiceImpl.PACKAGE_NAME);
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePowerSaveWhitelistApp RemoteException : ");
                stringBuilder.append(e.toString());
                loge(stringBuilder.toString());
            }
        }

        private void sendP2pPersistentGroupsChangedBroadcast() {
            logd("sending p2p persistent groups changed broadcast");
            Intent intent = new Intent("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void startDhcpServer(String intf) {
            StringBuilder stringBuilder;
            try {
                InterfaceConfiguration ifcg = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(intf);
                if (WifiP2pServiceImpl.this.mCreateWifiBridge) {
                    WifiP2pServiceImpl.this.mP2pServerAddress = WifiP2pServiceImpl.this.getWifiRepeaterServerAddress();
                    if (WifiP2pServiceImpl.this.mP2pServerAddress == null) {
                        WifiP2pServiceImpl.this.mP2pServerAddress = WifiP2pServiceImpl.SERVER_ADDRESS_WIFI_BRIDGE;
                    }
                } else {
                    WifiP2pServiceImpl.this.mP2pServerAddress = WifiP2pServiceImpl.SERVER_ADDRESS;
                }
                if (this.mGroup != null) {
                    this.mGroup.setP2pServerAddress(WifiP2pServiceImpl.this.mP2pServerAddress);
                }
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.this.mP2pServerAddress), 24));
                WifiP2pServiceImpl.this.mCreateWifiBridge = false;
                ifcg.setInterfaceUp();
                WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(intf, ifcg);
                String[] tetheringDhcpRanges = ((ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity")).getTetheredDhcpRanges();
                WifiP2pServiceImpl.this.handleTetheringDhcpRange(tetheringDhcpRanges);
                if (WifiP2pServiceImpl.this.mNwService.isTetheringStarted()) {
                    logd("Stop existing tethering and restart it");
                    WifiP2pServiceImpl.this.mNwService.stopTethering();
                }
                WifiP2pServiceImpl.this.mNwService.tetherInterface(intf);
                WifiP2pServiceImpl.this.mNwService.startTethering(tetheringDhcpRanges);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Started Dhcp server on ");
                stringBuilder.append(intf);
                logd(stringBuilder.toString());
            } catch (Exception e) {
                WifiP2pServiceImpl.this.mCreateWifiBridge = false;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error configuring interface ");
                stringBuilder.append(intf);
                stringBuilder.append(", :");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
        }

        private void stopDhcpServer(String intf) {
            try {
                WifiP2pServiceImpl.this.mNwService.untetherInterface(intf);
                for (String temp : WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("List all interfaces ");
                    stringBuilder.append(temp);
                    logd(stringBuilder.toString());
                    if (temp.compareTo(intf) != 0) {
                        logd("Found other tethering interfaces, so keep tethering alive");
                        return;
                    }
                }
                WifiP2pServiceImpl.this.mNwService.stopTethering();
                logd("Stopped Dhcp server");
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error stopping Dhcp server");
                stringBuilder2.append(e);
                loge(stringBuilder2.toString());
            } finally {
                logd("Stopped Dhcp server");
            }
        }

        private void notifyP2pEnableFailure() {
            Resources r = Resources.getSystem();
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(17041399)).setMessage(r.getString(17041403)).setPositiveButton(r.getString(17039370), null).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void addRowToDialog(ViewGroup group, int stringId, String value) {
            Resources r = Resources.getSystem();
            View row = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367337, group, false);
            ((TextView) row.findViewById(16909108)).setText(r.getString(stringId));
            ((TextView) row.findViewById(16909508)).setText(value);
            group.addView(row);
        }

        protected void notifyInvitationSent(String pin, String peerAddress) {
            Resources r = Resources.getSystem();
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367335, null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(16908993);
            addRowToDialog(group, 17041409, getDeviceName(peerAddress));
            addRowToDialog(group, 17041408, pin);
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(17041406)).setView(textEntryView).setPositiveButton(r.getString(17039370), null).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void notifyP2pProvDiscShowPinRequest(String pin, String peerAddress) {
            Resources r = Resources.getSystem();
            final String tempDevAddress = peerAddress;
            final String tempPin = pin;
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367335, null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(16908993);
            addRowToDialog(group, 17041409, getDeviceName(peerAddress));
            addRowToDialog(group, 17041408, pin);
            AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(17041406)).setView(textEntryView).setPositiveButton(r.getString(17039525), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                    P2pStateMachine.this.mSavedPeerConfig.deviceAddress = tempDevAddress;
                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                    P2pStateMachine.this.mSavedPeerConfig.wps.pin = tempPin;
                    P2pStateMachine.this.mWifiNative.p2pConnect(P2pStateMachine.this.mSavedPeerConfig, WifiP2pServiceImpl.FORM_GROUP.booleanValue());
                }
            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setType(2003);
            LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void processStatistics(Context mContext, int eventID, int choice) {
            JSONObject eventMsg = new JSONObject();
            try {
                eventMsg.put(HwWifiBigDataConstant.KEY_CHOICE, choice);
            } catch (JSONException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processStatistics put error.");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
            Flog.bdReport(mContext, eventID, eventMsg);
        }

        private void notifyInvitationReceived() {
            if (!WifiP2pServiceImpl.this.autoAcceptConnection()) {
                Resources r = Resources.getSystem();
                final WpsInfo wps = this.mSavedPeerConfig.wps;
                View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367335, null);
                ViewGroup group = (ViewGroup) textEntryView.findViewById(16908993);
                View row = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367336, group, false);
                ((TextView) row.findViewById(16909108)).setText(String.format(r.getString(33685699), new Object[]{getDeviceName(this.mSavedPeerConfig.deviceAddress)}));
                ((TextView) row.findViewById(16909108)).setTextColor(-16777216);
                group.addView(row);
                final EditText pin = (EditText) textEntryView.findViewById(16909529);
                AlertDialog dialog = new Builder(WifiP2pServiceImpl.this.mContext, 33947691).setTitle(r.getString(33685701)).setView(textEntryView).setPositiveButton(r.getString(33685700), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (wps.setup == 2) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = pin.getText().toString();
                        }
                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(P2pStateMachine.this.getName());
                        stringBuilder.append(" accept invitation ");
                        stringBuilder.append(P2pStateMachine.this.mSavedPeerConfig);
                        p2pStateMachine.logd(stringBuilder.toString());
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                        P2pStateMachine.this.processStatistics(WifiP2pServiceImpl.this.mContext, SupplicantStaIfaceHal.HAL_CALL_THRESHOLD_MS, 0);
                    }
                }).setNegativeButton(r.getString(17039360), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(P2pStateMachine.this.getName());
                        stringBuilder.append(" ignore connect");
                        p2pStateMachine.logd(stringBuilder.toString());
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                        P2pStateMachine.this.processStatistics(WifiP2pServiceImpl.this.mContext, SupplicantStaIfaceHal.HAL_CALL_THRESHOLD_MS, 1);
                    }
                }).setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(P2pStateMachine.this.getName());
                        stringBuilder.append(" ignore connect");
                        p2pStateMachine.logd(stringBuilder.toString());
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                        P2pStateMachine.this.processStatistics(WifiP2pServiceImpl.this.mContext, SupplicantStaIfaceHal.HAL_CALL_THRESHOLD_MS, 1);
                    }
                }).create();
                dialog.setCanceledOnTouchOutside(false);
                switch (wps.setup) {
                    case 1:
                        logd("Shown pin section visible");
                        addRowToDialog(group, 17041408, wps.pin);
                        break;
                    case 2:
                        logd("Enter pin section visible");
                        textEntryView.findViewById(16908872).setVisibility(0);
                        break;
                }
                if ((r.getConfiguration().uiMode & 5) == 5) {
                    dialog.setOnKeyListener(new OnKeyListener() {
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode != 164) {
                                return false;
                            }
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                            dialog.dismiss();
                            return true;
                        }
                    });
                }
                dialog.getWindow().setType(2003);
                LayoutParams attrs = dialog.getWindow().getAttributes();
                attrs.privateFlags = 16;
                dialog.getWindow().setAttributes(attrs);
                dialog.show();
            }
        }

        protected void updatePersistentNetworks(boolean reload) {
            if (reload) {
                this.mGroups.clear();
            }
            if (this.mWifiNative.p2pListNetworks(this.mGroups) || reload) {
                for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                    if (group.getOwner() != null && WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(group.getOwner().deviceAddress)) {
                        group.setOwner(WifiP2pServiceImpl.this.mThisDevice);
                    }
                }
                this.mWifiNative.saveConfig();
                sendP2pPersistentGroupsChangedBroadcast();
            }
        }

        private boolean isConfigInvalid(WifiP2pConfig config) {
            if (config == null || TextUtils.isEmpty(config.deviceAddress) || this.mPeers.get(config.deviceAddress) == null) {
                return true;
            }
            return false;
        }

        private WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig config) {
            if (config == null) {
                return null;
            }
            this.mPeers.updateGroupCapability(config.deviceAddress, this.mWifiNative.getGroupCapability(config.deviceAddress));
            return this.mPeers.get(config.deviceAddress);
        }

        private boolean isMiracastDevice(String deviceType) {
            if (deviceType == null) {
                return false;
            }
            String[] tokens = deviceType.split("-");
            try {
                if (tokens.length > 0 && Integer.parseInt(tokens[0]) == 7) {
                    logd("As connecting miracast device ,set go_intent = 14 to let it works as GO ");
                    return true;
                }
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isMiracastDevice: ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
            return false;
        }

        private boolean wifiIsConnected() {
            if (WifiP2pServiceImpl.this.mContext == null) {
                return false;
            }
            WifiManager wifiMgr = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
            if (wifiMgr != null && wifiMgr.getWifiState() == 3) {
                NetworkInfo wifiInfo = ((ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity")).getNetworkInfo(1);
                if (wifiInfo != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("wifiIsConnected: ");
                    stringBuilder.append(wifiInfo.isConnected());
                    logd(stringBuilder.toString());
                    return wifiInfo.isConnected();
                }
            }
            return false;
        }

        private void p2pConnectWithPinDisplay(WifiP2pConfig config) {
            if (config == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            if (dev == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("target device is not found ");
                stringBuilder.append(config.deviceAddress);
                loge(stringBuilder.toString());
                return;
            }
            if ((dev.primaryDeviceType != null && isMiracastDevice(dev.primaryDeviceType)) || wifiIsConnected()) {
                logd("set groupOwnerIntent is 14");
                config.groupOwnerIntent = 14;
            }
            String pin = this.mWifiNative.p2pConnect(config, dev.isGroupOwner());
            try {
                Integer.parseInt(pin);
                notifyInvitationSent(pin, config.deviceAddress);
            } catch (NumberFormatException e) {
            }
            WifiP2pServiceImpl.this.mIsInvite = false;
        }

        private boolean reinvokePersistentGroup(WifiP2pConfig config) {
            if (config == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            if (dev == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                return false;
            }
            int netId;
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("target ssid is ");
            stringBuilder.append(ssid);
            stringBuilder.append(" join:");
            stringBuilder.append(join);
            logd(stringBuilder.toString());
            if (join && dev.isGroupLimit()) {
                logd("target device reaches group limit.");
                join = false;
            } else if (join) {
                netId = this.mGroups.getNetworkId(dev.deviceAddress, ssid);
                if (netId >= 0) {
                    if (!this.mWifiNative.p2pGroupAdd(netId)) {
                        return false;
                    }
                    sendReinvokePGBroadcast(netId);
                    return true;
                }
            }
            if (join || !dev.isDeviceLimit()) {
                if (!join && dev.isInvitationCapable()) {
                    netId = -2;
                    if (config.netId < 0) {
                        netId = this.mGroups.getNetworkId(dev.deviceAddress);
                    } else if (config.deviceAddress.equals(this.mGroups.getOwnerAddr(config.netId))) {
                        netId = config.netId;
                    }
                    if (netId < 0) {
                        netId = getNetworkIdFromClientList(dev.deviceAddress);
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("netId related with ");
                    stringBuilder2.append(dev.deviceAddress);
                    stringBuilder2.append(" = ");
                    stringBuilder2.append(netId);
                    logd(stringBuilder2.toString());
                    if (netId >= 0) {
                        if (this.mWifiNative.p2pReinvoke(netId, dev.deviceAddress)) {
                            config.netId = netId;
                            sendReinvokePGBroadcast(netId);
                            return true;
                        }
                        loge("p2pReinvoke() failed, update networks");
                        updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                        return false;
                    }
                }
                return false;
            }
            loge("target device reaches the device limit.");
            return false;
        }

        private void sendReinvokePGBroadcast(int netId) {
            Intent intent = new Intent("com.huawei.net.wifi.p2p.REINVOKE_PERSISTENT_GROUP_ACTION");
            intent.putExtra("com.huawei.net.wifi.p2p.EXTRA_REINVOKE_NETID", netId);
            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.permission.REINVOKE_PERSISTENT");
        }

        protected int getNetworkIdFromClientList(String deviceAddress) {
            if (deviceAddress == null) {
                return -1;
            }
            for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                int netId = group.getNetworkId();
                String[] p2pClientList = getClientList(netId);
                if (p2pClientList != null) {
                    for (String client : p2pClientList) {
                        if (deviceAddress.equalsIgnoreCase(client)) {
                            return netId;
                        }
                    }
                }
            }
            return -1;
        }

        private String[] getClientList(int netId) {
            String p2pClients = this.mWifiNative.getP2pClientList(netId);
            if (p2pClients == null) {
                return null;
            }
            return p2pClients.split(" ");
        }

        private boolean removeClientFromList(int netId, String addr, boolean isRemovable) {
            StringBuilder modifiedClientList = new StringBuilder();
            String[] currentClientList = getClientList(netId);
            boolean isClientRemoved = false;
            if (currentClientList != null) {
                boolean isClientRemoved2 = false;
                for (String client : currentClientList) {
                    if (client.equalsIgnoreCase(addr)) {
                        isClientRemoved2 = true;
                    } else {
                        modifiedClientList.append(" ");
                        modifiedClientList.append(client);
                    }
                }
                isClientRemoved = isClientRemoved2;
            }
            if (modifiedClientList.length() == 0 && isRemovable) {
                logd("Remove unknown network");
                this.mGroups.remove(netId);
                return true;
            } else if (!isClientRemoved) {
                return false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Modified client list: ");
                stringBuilder.append(modifiedClientList);
                logd(stringBuilder.toString());
                if (modifiedClientList.length() == 0) {
                    modifiedClientList.append("\"\"");
                }
                this.mWifiNative.setP2pClientList(netId, modifiedClientList.toString());
                this.mWifiNative.saveConfig();
                return true;
            }
        }

        private void setWifiP2pInfoOnGroupFormation(InetAddress serverInetAddress) {
            this.mWifiP2pInfo.groupFormed = true;
            this.mWifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
            this.mWifiP2pInfo.groupOwnerAddress = serverInetAddress;
        }

        private void resetWifiP2pInfo() {
            this.mWifiP2pInfo.groupFormed = false;
            this.mWifiP2pInfo.isGroupOwner = false;
            this.mWifiP2pInfo.groupOwnerAddress = null;
        }

        private String getDeviceName(String deviceAddress) {
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d != null) {
                return d.deviceName;
            }
            return deviceAddress;
        }

        private String getCustomDeviceName(String deviceName) {
            if (!SystemProperties.getBoolean("ro.config.hw_wifi_bt_name", false) || !TextUtils.isEmpty(deviceName)) {
                return deviceName;
            }
            StringBuilder sb = new StringBuilder();
            String uuidStr = UUID.randomUUID().toString();
            String marketing_name = SystemProperties.get("ro.config.marketing_name");
            if (TextUtils.isEmpty(marketing_name)) {
                sb.append("HUAWEI ");
                sb.append(Build.PRODUCT);
                sb.append("_");
                sb.append(uuidStr.substring(24, 28).toUpperCase(Locale.US));
                deviceName = sb.toString();
            } else {
                sb.append(marketing_name);
                sb.append("_");
                sb.append(uuidStr.substring(24, 28).toUpperCase(Locale.US));
                deviceName = sb.toString();
            }
            Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", deviceName);
            return deviceName;
        }

        private String getPersistedDeviceName() {
            String deviceName = getCustomDeviceName(Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name"));
            if (deviceName != null) {
                return deviceName;
            }
            deviceName = SystemProperties.get("ro.config.marketing_name");
            if (!TextUtils.isEmpty(deviceName)) {
                return deviceName;
            }
            String id = Secure.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "android_id");
            if (id == null || id.length() <= 4 || WifiP2pServiceImpl.IS_ATT || WifiP2pServiceImpl.IS_VERIZON) {
                return Build.MODEL;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Build.MODEL);
            stringBuilder.append("_");
            stringBuilder.append(id.substring(0, 4));
            return stringBuilder.toString();
        }

        private boolean setAndPersistDeviceName(String devName) {
            if (devName == null) {
                return false;
            }
            StringBuilder stringBuilder;
            if (this.mWifiNative.setDeviceName(devName)) {
                WifiP2pServiceImpl.this.mThisDevice.deviceName = devName;
                WifiP2pNative wifiP2pNative = this.mWifiNative;
                stringBuilder = new StringBuilder();
                stringBuilder.append("-");
                stringBuilder.append(getSsidPostFix(WifiP2pServiceImpl.this.mThisDevice.deviceName));
                wifiP2pNative.setP2pSsidPostfix(stringBuilder.toString());
                Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", devName);
                sendThisDeviceChangedBroadcast();
                return true;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set device name ");
            stringBuilder.append(devName);
            loge(stringBuilder.toString());
            return false;
        }

        private boolean setWfdInfo(WifiP2pWfdInfo wfdInfo) {
            boolean success = !wfdInfo.isWfdEnabled() ? this.mWifiNative.setWfdEnable(false) : this.mWifiNative.setWfdEnable(true) && this.mWifiNative.setWfdDeviceInfo(wfdInfo.getDeviceInfoHex());
            if (success) {
                WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wfdInfo;
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set wfd properties");
            return false;
        }

        private void initializeP2pSettings() {
            WifiP2pServiceImpl.this.mThisDevice.deviceName = getPersistedDeviceName();
            this.mWifiNative.setP2pDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName);
            WifiP2pNative wifiP2pNative = this.mWifiNative;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("-");
            stringBuilder.append(getSsidPostFix(WifiP2pServiceImpl.this.mThisDevice.deviceName));
            wifiP2pNative.setP2pSsidPostfix(stringBuilder.toString());
            this.mWifiNative.setP2pDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
            WifiP2pServiceImpl.this.mThisDevice.deviceAddress = this.mWifiNative.p2pGetDeviceAddress();
            updateThisDevice(3);
            WifiP2pServiceImpl.this.mClientInfoList.clear();
            this.mWifiNative.p2pFlush();
            this.mWifiNative.p2pServiceFlush();
            WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            WifiP2pServiceImpl.this.clearValidDeivceList();
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
        }

        private void updateThisDevice(int status) {
            WifiP2pServiceImpl.this.mThisDevice.status = status;
            sendThisDeviceChangedBroadcast();
        }

        private void handleGroupCreationFailure() {
            resetWifiP2pInfo();
            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(DetailedState.FAILED, null, null);
            sendP2pConnectionChangedBroadcast();
            boolean peersChanged = this.mPeers.remove(this.mPeersLostDuringConnection);
            if (!(TextUtils.isEmpty(this.mSavedPeerConfig.deviceAddress) || this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) == null)) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            sendMessage(139265);
        }

        private void handleGroupRemoved() {
            StringBuilder stringBuilder;
            if (this.mGroup.isGroupOwner()) {
                if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                    stopDhcpServer("wlan0");
                } else {
                    stopDhcpServer(this.mGroup.getInterface());
                }
            } else if (WifiP2pServiceImpl.this.getMagicLinkDeviceFlag()) {
                WifiP2pServiceImpl.this.setmMagicLinkDeviceFlag(false);
            } else {
                logd("stop IpManager");
                WifiP2pServiceImpl.this.stopIpClient();
                try {
                    WifiP2pServiceImpl.this.mNwService.removeInterfaceFromLocalNetwork(this.mGroup.getInterface());
                } catch (RemoteException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to remove iface from local network ");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                }
            }
            try {
                if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
                    WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses("wlan0");
                } else {
                    WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses(this.mGroup.getInterface());
                }
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to clear addresses ");
                stringBuilder.append(e2);
                loge(stringBuilder.toString());
            }
            this.mWifiNative.setP2pGroupIdle(this.mGroup.getInterface(), 0);
            boolean peersChanged = false;
            for (WifiP2pDevice d : this.mGroup.getClientList()) {
                if (this.mPeers.remove(d)) {
                    peersChanged = true;
                }
            }
            if (this.mPeers.remove(this.mGroup.getOwner())) {
                peersChanged = true;
            }
            if (this.mPeers.remove(this.mPeersLostDuringConnection)) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mGroup = null;
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                } else {
                    loge("handleGroupRemoved(): WifiChannel is null");
                }
                WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
            }
            WifiP2pServiceImpl.this.notifyRptGroupRemoved();
        }

        protected void replyToMessage(Message msg, int what) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, int arg1) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.arg1 = arg1;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, Object obj) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.obj = obj;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private Message obtainMessage(Message srcMsg) {
            Message msg = Message.obtain();
            msg.arg2 = srcMsg.arg2;
            return msg;
        }

        protected void logd(String s) {
            Slog.d(WifiP2pServiceImpl.TAG, s);
        }

        protected void loge(String s) {
            Slog.e(WifiP2pServiceImpl.TAG, s);
        }

        private boolean updateSupplicantServiceRequest() {
            clearSupplicantServiceRequest();
            StringBuffer sb = new StringBuffer();
            Iterator it = WifiP2pServiceImpl.this.mClientInfoList.values().iterator();
            while (true) {
                int i = 0;
                if (!it.hasNext()) {
                    break;
                }
                ClientInfo c = (ClientInfo) it.next();
                while (i < c.mReqList.size()) {
                    WifiP2pServiceRequest req = (WifiP2pServiceRequest) c.mReqList.valueAt(i);
                    if (req != null) {
                        sb.append(req.getSupplicantQuery());
                    }
                    i++;
                }
            }
            if (sb.length() == 0) {
                return false;
            }
            WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq(WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS, sb.toString());
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return false;
            }
            return true;
        }

        private void clearSupplicantServiceRequest() {
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
                WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            }
        }

        private boolean addServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            if (m == null || req == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null) {
                return false;
            }
            WifiP2pServiceImpl.access$8704(WifiP2pServiceImpl.this);
            if (WifiP2pServiceImpl.this.mServiceTransactionId == (byte) 0) {
                WifiP2pServiceImpl.access$8704(WifiP2pServiceImpl.this);
            }
            req.setTransactionId(WifiP2pServiceImpl.this.mServiceTransactionId);
            clientInfo.mReqList.put(WifiP2pServiceImpl.this.mServiceTransactionId, req);
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return true;
            }
            return updateSupplicantServiceRequest();
        }

        private void removeServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            if (m == null || req == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
            }
            int i = 0;
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo != null) {
                boolean removed = false;
                while (i < clientInfo.mReqList.size()) {
                    if (req.equals(clientInfo.mReqList.valueAt(i))) {
                        removed = true;
                        clientInfo.mReqList.removeAt(i);
                        break;
                    }
                    i++;
                }
                if (removed) {
                    if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                        logd("remove client information from framework");
                        WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                    }
                    if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                        updateSupplicantServiceRequest();
                    }
                }
            }
        }

        private void clearServiceRequests(Messenger m) {
            if (m == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, null);
            if (clientInfo != null && clientInfo.mReqList.size() != 0) {
                clientInfo.mReqList.clear();
                if (clientInfo.mServList.size() == 0) {
                    logd("remove channel information from framework");
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
                if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                    updateSupplicantServiceRequest();
                }
            }
        }

        private boolean addLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            if (m == null || servInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null || !clientInfo.mServList.add(servInfo)) {
                return false;
            }
            if (this.mWifiNative.p2pServiceAdd(servInfo)) {
                return true;
            }
            clientInfo.mServList.remove(servInfo);
            return false;
        }

        private void removeLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            if (m == null || servInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, null);
            if (clientInfo != null) {
                this.mWifiNative.p2pServiceDel(servInfo);
                clientInfo.mServList.remove(servInfo);
                if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                    logd("remove client information from framework");
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
            }
        }

        private void clearLocalServices(Messenger m) {
            if (m == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(m, null);
            if (clientInfo != null) {
                for (WifiP2pServiceInfo servInfo : clientInfo.mServList) {
                    this.mWifiNative.p2pServiceDel(servInfo);
                }
                clientInfo.mServList.clear();
                if (clientInfo.mReqList.size() == 0) {
                    logd("remove client information from framework");
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
            }
        }

        private void clearClientInfo(Messenger m) {
            clearLocalServices(m);
            clearServiceRequests(m);
        }

        private void sendServiceResponse(WifiP2pServiceResponse resp) {
            if (resp == null) {
                Log.e(WifiP2pServiceImpl.TAG, "sendServiceResponse with null response");
                return;
            }
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                if (((WifiP2pServiceRequest) c.mReqList.get(resp.getTransactionId())) != null) {
                    Message msg = Message.obtain();
                    msg.what = 139314;
                    msg.arg1 = 0;
                    msg.arg2 = 0;
                    msg.obj = resp;
                    if (c.mMessenger != null) {
                        try {
                            c.mMessenger.send(msg);
                        } catch (RemoteException e) {
                            logd("detect dead channel");
                            clearClientInfo(c.mMessenger);
                            return;
                        }
                    }
                }
            }
        }

        private void clearClientDeadChannels() {
            ArrayList<Messenger> deadClients = new ArrayList();
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                Message msg = Message.obtain();
                msg.what = 139313;
                msg.arg1 = 0;
                msg.arg2 = 0;
                msg.obj = null;
                if (c.mMessenger != null) {
                    try {
                        c.mMessenger.send(msg);
                    } catch (RemoteException e) {
                        logd("detect dead channel");
                        deadClients.add(c.mMessenger);
                    }
                }
            }
            Iterator it = deadClients.iterator();
            while (it.hasNext()) {
                clearClientInfo((Messenger) it.next());
            }
        }

        private ClientInfo getClientInfo(Messenger m, boolean createIfNotExist) {
            ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(m);
            if (clientInfo != null || !createIfNotExist) {
                return clientInfo;
            }
            logd("add a new client");
            clientInfo = new ClientInfo(WifiP2pServiceImpl.this, m, null);
            WifiP2pServiceImpl.this.mClientInfoList.put(m, clientInfo);
            return clientInfo;
        }

        private void enableBTCoex() {
            if (this.mIsBTCoexDisabled) {
                this.mWifiInjector.getWifiNative().setBluetoothCoexistenceMode(this.mInterfaceName, 2);
                this.mIsBTCoexDisabled = false;
            }
        }

        private String getSsidPostFix(String deviceName) {
            String ssidPostFix = deviceName;
            if (ssidPostFix == null) {
                return ssidPostFix;
            }
            byte[] ssidPostFixBytes = ssidPostFix.getBytes();
            while (ssidPostFixBytes.length > 22) {
                ssidPostFix = ssidPostFix.substring(0, ssidPostFix.length() - 1);
                ssidPostFixBytes = ssidPostFix.getBytes();
            }
            if (ssidPostFixBytes.length != 14) {
                return ssidPostFix;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ssidPostFix);
            stringBuilder.append(" ");
            return stringBuilder.toString();
        }

        private WifiP2pDeviceList getPeers(Bundle pkg, int uid) {
            String pkgName = pkg.getString("android.net.wifi.p2p.CALLING_PACKAGE");
            if (this.mWifiInjector == null) {
                this.mWifiInjector = WifiInjector.getInstance();
            }
            try {
                this.mWifiInjector.getWifiPermissionsUtil().enforceCanAccessScanResults(pkgName, uid);
                return new WifiP2pDeviceList(this.mPeers);
            } catch (SecurityException e) {
                Log.v(WifiP2pServiceImpl.TAG, "Security Exception, cannot access peer list");
                return new WifiP2pDeviceList();
            }
        }
    }

    public enum P2pStatus {
        SUCCESS,
        INFORMATION_IS_CURRENTLY_UNAVAILABLE,
        INCOMPATIBLE_PARAMETERS,
        LIMIT_REACHED,
        INVALID_PARAMETER,
        UNABLE_TO_ACCOMMODATE_REQUEST,
        PREVIOUS_PROTOCOL_ERROR,
        NO_COMMON_CHANNEL,
        UNKNOWN_P2P_GROUP,
        BOTH_GO_INTENT_15,
        INCOMPATIBLE_PROVISIONING_METHOD,
        REJECTED_BY_USER,
        UNKNOWN;

        public static P2pStatus valueOf(int error) {
            switch (error) {
                case 0:
                    return SUCCESS;
                case 1:
                    return INFORMATION_IS_CURRENTLY_UNAVAILABLE;
                case 2:
                    return INCOMPATIBLE_PARAMETERS;
                case 3:
                    return LIMIT_REACHED;
                case 4:
                    return INVALID_PARAMETER;
                case 5:
                    return UNABLE_TO_ACCOMMODATE_REQUEST;
                case 6:
                    return PREVIOUS_PROTOCOL_ERROR;
                case 7:
                    return NO_COMMON_CHANNEL;
                case 8:
                    return UNKNOWN_P2P_GROUP;
                case 9:
                    return BOTH_GO_INTENT_15;
                case 10:
                    return INCOMPATIBLE_PROVISIONING_METHOD;
                case 11:
                    return REJECTED_BY_USER;
                default:
                    return UNKNOWN;
            }
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 139265:
                case 139268:
                case 139271:
                case 139274:
                case 139277:
                case 139280:
                case 139283:
                case 139285:
                case 139287:
                case 139292:
                case 139295:
                case 139298:
                case 139301:
                case 139304:
                case 139307:
                case 139310:
                case 139315:
                case 139318:
                case 139321:
                case 139323:
                case 139326:
                case 139329:
                case 139332:
                case 139335:
                    boolean isRequestPeers = true;
                    boolean isConnect = msg.what == 139271;
                    boolean isDiscoverPeers = msg.what == 139265;
                    if (msg.what != 139283) {
                        isRequestPeers = false;
                    }
                    if (HwDeviceManager.disallowOp(45) && (isConnect || isDiscoverPeers || isRequestPeers)) {
                        String str = WifiP2pServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("wifiP2P function is forbidden,msg.what = ");
                        stringBuilder.append(msg.what);
                        Slog.d(str, stringBuilder.toString());
                        Toast.makeText(WifiP2pServiceImpl.this.mContext, WifiP2pServiceImpl.this.mContext.getResources().getString(33686008), 0).show();
                        return;
                    }
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(Message.obtain(msg));
                    return;
                default:
                    if (HwDeviceManager.disallowOp(45)) {
                        String str2 = WifiP2pServiceImpl.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("wifiP2P function is forbidden,msg.what = ");
                        stringBuilder2.append(msg.what);
                        Slog.d(str2, stringBuilder2.toString());
                        Toast.makeText(WifiP2pServiceImpl.this.mContext, WifiP2pServiceImpl.this.mContext.getResources().getString(33686008), 0).show();
                        return;
                    }
                    WifiP2pServiceImpl.this.handleClientHwMessage(Message.obtain(msg));
                    return;
            }
        }
    }

    static /* synthetic */ int access$1804() {
        int i = sDisableP2pTimeoutIndex + 1;
        sDisableP2pTimeoutIndex = i;
        return i;
    }

    static /* synthetic */ int access$5404() {
        int i = sGroupCreatingTimeoutIndex + 1;
        sGroupCreatingTimeoutIndex = i;
        return i;
    }

    static /* synthetic */ byte access$8704(WifiP2pServiceImpl x0) {
        byte b = (byte) (x0.mServiceTransactionId + 1);
        x0.mServiceTransactionId = b;
        return b;
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDBG = z2;
        z2 = "389".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "840".equals(SystemProperties.get("ro.config.hw_optb", "0"));
        IS_VERIZON = z2;
        if (!("07".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "840".equals(SystemProperties.get("ro.config.hw_optb", "0")))) {
            z = false;
        }
        IS_ATT = z;
    }

    public WifiP2pServiceImpl(Context context) {
        this.mContext = context;
        this.mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(17039849);
        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiP2pServiceHisiExt = new WifiP2pServiceHisiExt(this.mContext, this.mThisDevice, this.mWifiChannel, this.mNetworkInfo);
        }
        HandlerThread wifiP2pThread = new HandlerThread(TAG);
        wifiP2pThread.start();
        this.mClientHandler = new ClientHandler(TAG, wifiP2pThread.getLooper());
        this.mP2pStateMachine = (P2pStateMachine) getHwP2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        if (this.mP2pStateMachine == null) {
            Slog.d(TAG, "use android origin P2pStateMachine");
            this.mP2pStateMachine = new P2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        }
        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiP2pServiceHisiExt.mP2pStateMachine = this.mP2pStateMachine;
        }
        this.mP2pStateMachine.start();
        if (SystemProperties.getBoolean("ro.config.hw_wifibridge", false)) {
            initWifiRepeaterConfig();
        }
    }

    public boolean setWifiP2pEnabled(int p2pFlag) {
        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
            return this.mWifiP2pServiceHisiExt.setWifiP2pEnabled(p2pFlag);
        }
        return false;
    }

    public boolean isWifiP2pEnabled() {
        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
            return this.mWifiP2pServiceHisiExt.isWifiP2pEnabled();
        }
        return false;
    }

    public void setRecoveryWifiFlag(boolean flag) {
        enforceChangePermission();
        if (WifiP2pServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiP2pServiceHisiExt.setRecoveryWifiFlag(flag);
        }
    }

    public void connectivityServiceReady() {
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    private int checkConnectivityInternalPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL");
    }

    private int checkLocationHardwarePermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE");
    }

    private void enforceConnectivityInternalOrLocationHardwarePermission() {
        if (checkConnectivityInternalPermission() != 0 && checkLocationHardwarePermission() != 0) {
            enforceConnectivityInternalPermission();
        }
    }

    private void stopIpClient() {
        if (this.mIpClient != null) {
            this.mIpClient.shutdown();
            this.mIpClient = null;
        }
        this.mDhcpResults = null;
    }

    private void startIpClient(String ifname) {
        stopIpClient();
        this.mIpClient = new IpClient(this.mContext, ifname, new Callback() {
            public void onPreDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION);
            }

            public void onPostDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_POST_DHCP_ACTION);
            }

            public void onNewDhcpResults(DhcpResults dhcpResults) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_DHCP_RESULTS, dhcpResults);
            }

            public void onProvisioningSuccess(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS);
            }

            public void onProvisioningFailure(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE);
            }
        }, this.mNwService);
        IpClient ipClient = this.mIpClient;
        this.mIpClient.startProvisioning(IpClient.buildProvisioningConfiguration().withoutIPv6().withoutIpReachabilityMonitor().withPreDhcpAction(WifiStateMachine.LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS).withProvisioningTimeoutMs(36000).build());
    }

    public Messenger getMessenger(IBinder binder) {
        Messenger messenger;
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            messenger = new Messenger(this.mClientHandler);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMessenger: uid=");
            stringBuilder.append(getCallingUid());
            stringBuilder.append(", binder=");
            stringBuilder.append(binder);
            stringBuilder.append(", messenger=");
            stringBuilder.append(messenger);
            Log.d(str, stringBuilder.toString());
            DeathRecipient dr = new -$$Lambda$WifiP2pServiceImpl$LwceCrSRIRY_Lp9TjCEZZ62j-ls(this, binder);
            try {
                binder.linkToDeath(dr, 0);
                this.mDeathDataByBinder.put(binder, new DeathHandlerData(dr, messenger));
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error on linkToDeath: e=");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
            this.mWifiP2pDevCreateRetry = 0;
            this.mP2pStateMachine.sendMessage(ENABLE_P2P);
        }
        return messenger;
    }

    public static /* synthetic */ void lambda$getMessenger$0(WifiP2pServiceImpl wifiP2pServiceImpl, IBinder binder) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("binderDied: binder=");
        stringBuilder.append(binder);
        Log.d(str, stringBuilder.toString());
        wifiP2pServiceImpl.close(binder);
    }

    public Messenger getP2pStateMachineMessenger() {
        enforceConnectivityInternalOrLocationHardwarePermission();
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mP2pStateMachine.getHandler());
    }

    /* JADX WARNING: Missing block: B:19:0x0070, code skipped:
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:20:0x0071, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close(IBinder binder) {
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            DeathHandlerData dhd = (DeathHandlerData) this.mDeathDataByBinder.get(binder);
            if (dhd == null) {
                Log.w(TAG, "close(): no death recipient for binder");
                return;
            }
            binder.unlinkToDeath(dhd.mDeathRecipient, 0);
            this.mDeathDataByBinder.remove(binder);
            if (dhd.mMessenger != null && this.mDeathDataByBinder.isEmpty()) {
                try {
                    dhd.mMessenger.send(this.mClientHandler.obtainMessage(139268));
                    dhd.mMessenger.send(this.mClientHandler.obtainMessage(139280));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("close: Failed sending clean-up commands: e=");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                this.mP2pStateMachine.sendMessage(DISABLE_P2P);
            }
        }
    }

    public void setMiracastMode(int mode) {
        enforceConnectivityInternalPermission();
        checkConfigureWifiDisplayPermission();
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, mode);
    }

    public void checkConfigureWifiDisplayPermission() {
        if (!getWfdPermission(Binder.getCallingUid())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wifi Display Permission denied for uid = ");
            stringBuilder.append(Binder.getCallingUid());
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private boolean getWfdPermission(int uid) {
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        return this.mWifiInjector.getWifiPermissionsWrapper().getUidPermission("android.permission.CONFIGURE_WIFI_DISPLAY", uid) != -1;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: can't dump WifiP2pService from from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            pw.println(stringBuilder.toString());
            return;
        }
        this.mP2pStateMachine.dump(fd, pw, args);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAutonomousGroup ");
        stringBuilder.append(this.mAutonomousGroup);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mJoinExistingGroup ");
        stringBuilder.append(this.mJoinExistingGroup);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDiscoveryStarted ");
        stringBuilder.append(this.mDiscoveryStarted);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkInfo ");
        stringBuilder.append(this.mNetworkInfo);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mTemporarilyDisconnectedWifi ");
        stringBuilder.append(this.mTemporarilyDisconnectedWifi);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mServiceDiscReqId ");
        stringBuilder.append(this.mServiceDiscReqId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDeathDataByBinder ");
        stringBuilder.append(this.mDeathDataByBinder);
        pw.println(stringBuilder.toString());
        pw.println();
        IpClient ipClient = this.mIpClient;
        if (ipClient != null) {
            pw.println("mIpClient:");
            ipClient.dump(fd, pw, args);
        }
    }

    public void handleClientConnect(WifiP2pGroup group) {
    }

    public void handleClientDisconnect(WifiP2pGroup group) {
    }

    public void notifyRptGroupRemoved() {
    }

    public void setWifiRepeaterState(int state) {
    }

    protected boolean allowP2pFind(int uid) {
        return true;
    }

    protected void handleP2pStopFind(int uid) {
    }
}
