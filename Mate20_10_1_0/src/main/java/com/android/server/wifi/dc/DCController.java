package com.android.server.wifi.dc;

import android.content.Context;
import android.net.wifi.IWifiActionListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManagerUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.HwWifiServiceManager;
import com.android.server.wifi.HwWifiServiceManagerImpl;
import com.android.server.wifi.WifiConfigurationUtil;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.p2p.WifiP2pNative;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.util.NativeUtil;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.Locale;

public class DCController extends StateMachine {
    private static final int DC_CMD_SET_STATE = 116;
    private static final int DC_CONNECTED = 2;
    private static final int DC_CONNECTING = 1;
    private static final int DC_DISCONNECTED = 4;
    private static final int DC_DISCONNECTING = 3;
    private static final int DC_STATE_DISABLED = 0;
    private static final int DC_STATE_ENABLED = 1;
    private static final int MAGICLINK_CONNECT_RETRY_DELAY_TIME_MSEC = 3000;
    private static final int MAGICLINK_CONNECT_RETRY_LIMIT_TIMES = 3;
    private static final int MAGICLINK_GROUP_CREATING_WAIT_TIME_MS = 20000;
    private static final String ONEHOP_TV_MIRACAST_PACKAGE_NAME = "com.huawei.pcassistant";
    private static final String P2P_TETHER_IFAC_110X = "p2p-p2p0-";
    private static final String TAG = "DCController";
    private static DCController mDCController = null;
    private static WifiP2pManagerUtils mWifiP2pManagerUtils = EasyInvokeFactory.getInvokeUtils(WifiP2pManagerUtils.class);
    /* access modifiers changed from: private */
    public WifiP2pManager.Channel mChannel;
    private WifiP2pManager.ChannelListener mChannelListener = new WifiP2pManager.ChannelListener() {
        /* class com.android.server.wifi.dc.DCController.AnonymousClass3 */

        public void onChannelDisconnected() {
            WifiP2pManager.Channel unused = DCController.this.mChannel = null;
            DCController.this.initWifiP2pChannel();
            HwHiLog.i(DCController.TAG, false, "Wifi p2p channel is disconnected.", new Object[0]);
        }
    };
    private Context mContext;
    /* access modifiers changed from: private */
    public DCArbitra mDCArbitra;
    /* access modifiers changed from: private */
    public DCConnectedState mDCConnectedState = new DCConnectedState();
    /* access modifiers changed from: private */
    public DCConnectingState mDCConnectingState = new DCConnectingState();
    /* access modifiers changed from: private */
    public DCDisconnectingState mDCDisconnectingState = new DCDisconnectingState();
    /* access modifiers changed from: private */
    public DCInActiveState mDCInActiveState = new DCInActiveState();
    private DCJniAdapter mDCJniAdapter;
    /* access modifiers changed from: private */
    public DcChr mDcChr;
    private DefaultState mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public String mGcIf = "";
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsDcConfigGot = false;
    /* access modifiers changed from: private */
    public boolean mIsDcConnectByApp = false;
    /* access modifiers changed from: private */
    public boolean mIsDcConnecting = false;
    /* access modifiers changed from: private */
    public boolean mIsMagiclinkConnected = false;
    /* access modifiers changed from: private */
    public IWifiActionListener mListener = null;
    private WifiP2pManager.ActionListener mMagiclinkConnectListener = new WifiP2pManager.ActionListener() {
        /* class com.android.server.wifi.dc.DCController.AnonymousClass1 */

        public void onSuccess() {
            HwHiLog.d(DCController.TAG, false, "Start magiclinkConnect success.", new Object[0]);
            DCController dCController = DCController.this;
            dCController.notifyListenerOnSuccess(dCController.mListener);
        }

        public void onFailure(int reason) {
            boolean unused = DCController.this.mIsMagiclinkConnected = false;
            HwHiLog.i(DCController.TAG, false, "Start magiclinkConnect failed, error code = %{public}d", new Object[]{Integer.valueOf(reason)});
            DCController dCController = DCController.this;
            dCController.notifyListenerOnFailure(dCController.mListener, reason);
            if (reason == 2) {
                HwHiLog.i(DCController.TAG, false, "have other p2p service, conflict ", new Object[0]);
                DCController.this.sendMessage(11);
                DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(11);
                return;
            }
            DCController.this.sendMessageDelayed(10, 3000);
        }
    };
    /* access modifiers changed from: private */
    public int mMagiclinkConnectRetryTimes = 0;
    private INetworkManagementService mNmService;
    private WifiP2pServiceImpl.IP2pNotDhcpCallback mP2pNotDhcpCallback = new WifiP2pServiceImpl.IP2pNotDhcpCallback() {
        /* class com.android.server.wifi.dc.DCController.AnonymousClass4 */

        public void onP2pConnected(String interfaceName) {
            HwHiLog.d(DCController.TAG, false, "notifyDCP2pConnected", new Object[0]);
            if (TextUtils.isEmpty(interfaceName)) {
                HwHiLog.e(DCController.TAG, false, "p2p interface is empty", new Object[0]);
                return;
            }
            if (!interfaceName.equals(DCController.this.mGcIf)) {
                String unused = DCController.this.mGcIf = interfaceName;
            }
            DCHilinkController.getInstance().handleP2pConnected(DCController.this.mGcIf);
            DCController.this.sendMessage(32);
        }

        public boolean isP2pNotDhcpRunning() {
            return DCController.this.mIsDcConnecting;
        }
    };
    private WifiP2pManager.ActionListener mRemoveGcGroupListener = new WifiP2pManager.ActionListener() {
        /* class com.android.server.wifi.dc.DCController.AnonymousClass2 */

        public void onSuccess() {
            if (DCController.this.mIsMagiclinkConnected) {
                DCController.this.startOrStopHiP2p(false);
                DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(27);
            }
            boolean unused = DCController.this.mIsMagiclinkConnected = false;
            String unused2 = DCController.this.mGcIf = "";
            HwHiLog.i(DCController.TAG, false, "Start MagiclinkRemoveGcGroup success.", new Object[0]);
            DCController.this.sendMessage(12);
        }

        public void onFailure(int reason) {
            DCController.this.removeGcGroup();
            String unused = DCController.this.mGcIf = "";
            HwHiLog.i(DCController.TAG, false, "Start MagiclinkRemoveGcGroup failed, error code=%{public}d", new Object[]{Integer.valueOf(reason)});
        }
    };
    /* access modifiers changed from: private */
    public SCConnectedState mSCConnectedState = new SCConnectedState();
    private WifiInjector mWifiInjector;
    private WifiManager mWifiManager;

    private DCController(Context context) {
        super(TAG);
        this.mContext = context;
        this.mWifiInjector = WifiInjector.getInstance();
        this.mNmService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mHandler = getHandler();
        this.mDCArbitra = DCArbitra.createDCArbitra(context);
        this.mDCJniAdapter = DCJniAdapter.getInstance();
        this.mDcChr = DcChr.getInstance();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        addState(this.mDefaultState);
        addState(this.mDCInActiveState, this.mDefaultState);
        addState(this.mSCConnectedState, this.mDCInActiveState);
        addState(this.mDCConnectingState, this.mSCConnectedState);
        addState(this.mDCConnectedState, this.mSCConnectedState);
        addState(this.mDCDisconnectingState, this.mSCConnectedState);
        setInitialState(this.mDCInActiveState);
        start();
    }

    public static DCController createDCController(Context context) {
        if (mDCController == null) {
            mDCController = new DCController(context);
        }
        return mDCController;
    }

    public static DCController getInstance() {
        return mDCController;
    }

    public Handler getDCControllerHandler() {
        return this.mHandler;
    }

    public void handleUpdateScanResults() {
        this.mDCArbitra.updateScanResults();
        sendMessage(30);
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(State state, Message message) {
        HwHiLog.d(TAG, false, "%{public}s : handle message: %{public}s", new Object[]{state.getClass().getSimpleName(), DCUtils.getStateAndMessageString(state, message)});
    }

    private void magiclinkConnect(WifiP2pManager.Channel wifiP2pChannel, String config) {
        HwHiLog.d(TAG, false, "DC magiclinkConnect enter", new Object[0]);
        Bundle bd = new Bundle();
        bd.putString("cfg", config);
        wifiP2pChannel.getAsyncChannel().sendMessage(141269, 0, mWifiP2pManagerUtils.putListener(wifiP2pChannel, this.mMagiclinkConnectListener), bd);
    }

    private void magiclinkRemoveGcGroup(WifiP2pManager.Channel wifiP2pChannel, String iface) {
        HwHiLog.d(TAG, false, "magiclinkRemoveGcGroup enter", new Object[0]);
        Bundle bundle = new Bundle();
        bundle.putString("iface", iface);
        wifiP2pChannel.getAsyncChannel().sendMessage(141271, 0, mWifiP2pManagerUtils.putListener(wifiP2pChannel, this.mRemoveGcGroupListener), bundle);
    }

    /* access modifiers changed from: private */
    public void initWifiP2pChannel() {
        WifiP2pManager wifiP2pManager = (WifiP2pManager) this.mContext.getSystemService("wifip2p");
        if (this.mChannel == null && wifiP2pManager != null) {
            Context context = this.mContext;
            this.mChannel = wifiP2pManager.initialize(context, context.getMainLooper(), this.mChannelListener);
            if (this.mChannel == null) {
                HwHiLog.w(TAG, false, "mWifiP2pManager initialize failed, channel is null", new Object[0]);
            }
        }
    }

    public void startMagiclinkConnect(DCConfiguration selectedDcConfig) {
        WifiManager wifiManager;
        if (this.mIsMagiclinkConnected) {
            HwHiLog.i(TAG, false, "magiclinkConnected, abort", new Object[0]);
            return;
        }
        initWifiP2pChannel();
        if (this.mChannel == null || (wifiManager = this.mWifiManager) == null) {
            HwHiLog.e(TAG, false, "mChannel or mWifiManager is null", new Object[0]);
            return;
        }
        String wifiMacAddr = wifiManager.getConnectionInfo().getMacAddress();
        if (TextUtils.isEmpty(wifiMacAddr)) {
            HwHiLog.e(TAG, false, "get_wifi_mac_address is empty", new Object[0]);
            return;
        }
        String p2pMacAddr = DCUtils.wifiAddr2p2pAddr(wifiMacAddr);
        if (TextUtils.isEmpty(p2pMacAddr)) {
            HwHiLog.e(TAG, false, "get_p2p_mac_address is empty", new Object[0]);
            return;
        }
        this.mMagiclinkConnectRetryTimes++;
        String connectInfo = selectedDcConfig.getSSID() + "\n" + selectedDcConfig.getBSSID() + "\n" + selectedDcConfig.getPreSharedKey() + "\n" + selectedDcConfig.getFrequency() + "\n1\n1\n" + p2pMacAddr;
        HwHiLog.d(TAG, false, "startMagiclinkConnect %{private}s", new Object[]{connectInfo});
        magiclinkConnect(this.mChannel, connectInfo);
    }

    /* access modifiers changed from: private */
    public void notifyListenerOnSuccess(IWifiActionListener listener) {
        if (listener != null) {
            try {
                listener.onSuccess();
            } catch (RemoteException e) {
                HwHiLog.e(TAG, false, "Exceptions happen at MagiclinkConnectListener onSuccess", new Object[0]);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyListenerOnFailure(IWifiActionListener listener, int reason) {
        if (listener != null) {
            try {
                listener.onFailure(reason);
            } catch (RemoteException e) {
                HwHiLog.e(TAG, false, "Exceptions happen when notufy listener OnFailure", new Object[0]);
            }
        }
    }

    public void dcConnect(WifiConfiguration configuration, IWifiActionListener listener) {
        if (this.mIsDcConnectByApp) {
            HwHiLog.i(TAG, false, "dcConnect is calling by other app", new Object[0]);
            notifyListenerOnFailure(listener, 2);
            return;
        }
        DCHilinkController dcHilinkController = DCHilinkController.getInstance();
        DCArbitra dcArbitra = DCArbitra.getInstance();
        if (this.mWifiManager == null || dcArbitra == null || dcHilinkController == null || configuration == null || configuration.BSSID == null || !WifiConfigurationUtil.validate(configuration, true) || !dcHilinkController.isWifiAndP2pStateAllowDc()) {
            HwHiLog.w(TAG, false, "dcConnect: do not allow APP to connect DC", new Object[0]);
            notifyListenerOnFailure(listener, 0);
            return;
        }
        int dcFrequency = dcArbitra.getFrequencyForBssid(configuration.BSSID);
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        if (wifiInfo == null || ((ScanResult.is24GHz(wifiInfo.getFrequency()) && ScanResult.is24GHz(dcFrequency)) || (ScanResult.is5GHz(wifiInfo.getFrequency()) && ScanResult.is5GHz(dcFrequency)))) {
            HwHiLog.w(TAG, false, "dcConnect: dcFrequency is the same with wifi, return", new Object[0]);
            notifyListenerOnFailure(listener, 1);
            return;
        }
        String securityType = configuration.getSsidAndSecurityTypeString().substring(configuration.SSID.length());
        if (WifiConfiguration.KeyMgmt.strings[0].equals(securityType) || WifiConfiguration.KeyMgmt.strings[1].equals(securityType) || WifiConfiguration.KeyMgmt.strings[4].equals(securityType)) {
            initWifiP2pChannel();
            if (this.mChannel == null) {
                HwHiLog.e(TAG, false, "mChannel is null", new Object[0]);
                notifyListenerOnFailure(listener, 0);
                return;
            }
            String preSharedKey = "";
            if (!WifiConfiguration.KeyMgmt.strings[0].equals(securityType)) {
                preSharedKey = NativeUtil.removeEnclosingQuotes(configuration.preSharedKey);
            }
            String connectInfo = NativeUtil.removeEnclosingQuotes(configuration.SSID) + "\n" + configuration.BSSID.toLowerCase(Locale.ROOT) + "\n" + preSharedKey + "\n" + dcFrequency + "\n1";
            HwHiLog.i(TAG, false, "dcConnect %{private}s", new Object[]{connectInfo});
            magiclinkConnect(this.mChannel, connectInfo);
            this.mIsDcConnectByApp = true;
            this.mListener = listener;
            sendMessage(35);
            return;
        }
        HwHiLog.e(TAG, false, "dcConnect: AuthType is not allowed", new Object[0]);
        notifyListenerOnFailure(listener, 0);
    }

    public boolean isWifiDcActive() {
        return getCurrentState() == this.mDCConnectedState;
    }

    public boolean dcDisconnect() {
        WifiP2pManager.Channel channel;
        this.mListener = null;
        String dcIface = getDcInterface();
        if (TextUtils.isEmpty(dcIface) || (channel = this.mChannel) == null) {
            HwHiLog.i(TAG, false, "dcDisconnect abort", new Object[0]);
            return false;
        }
        magiclinkRemoveGcGroup(channel, dcIface);
        return true;
    }

    /* access modifiers changed from: private */
    public void removeGcGroup() {
        if (this.mIsMagiclinkConnected || !TextUtils.isEmpty(this.mGcIf)) {
            WifiP2pManager.Channel channel = this.mChannel;
            if (channel == null) {
                HwHiLog.e(TAG, false, "mChannel is null", new Object[0]);
            } else {
                magiclinkRemoveGcGroup(channel, this.mGcIf);
            }
        } else {
            HwHiLog.i(TAG, false, "magiclinkDisconnected, abort", new Object[0]);
            DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(12);
        }
    }

    /* access modifiers changed from: private */
    public boolean isDcP2pConnectStart(DCConfiguration dcSelectedNetwork) {
        if (dcSelectedNetwork == null) {
            HwHiLog.d(TAG, false, "no network to start DC", new Object[0]);
            this.mIsDcConfigGot = true;
            if (!this.mDCArbitra.isValidDcConfigSaved()) {
                DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(31);
            }
            return false;
        }
        this.mDcChr.uploadDcConnectTotalCount();
        this.mDcChr.setDcConnectStartTime(SystemClock.elapsedRealtime());
        HwWifiServiceManager hwWifiServiceManager = HwWifiServiceManagerImpl.getDefault();
        if (hwWifiServiceManager instanceof HwWifiServiceManagerImpl) {
            this.mIsDcConnecting = true;
            ((HwWifiServiceManagerImpl) hwWifiServiceManager).getHwWifiP2pService().registerP2pNotDhcpCallback(this.mP2pNotDhcpCallback);
        }
        this.mDcChr.setDcP2pConnectStartTime(SystemClock.elapsedRealtime());
        startMagiclinkConnect(dcSelectedNetwork);
        return true;
    }

    /* access modifiers changed from: private */
    public void startOrStopHiP2p(boolean enable) {
        String wifiInterface = SystemProperties.get("wifi.interface", "wlan0");
        HwHiLog.i(TAG, false, "wifiInterface=%{public}s p2pInterface=%{public}s enable=%{public}s", new Object[]{wifiInterface, this.mGcIf, Boolean.valueOf(enable)});
        if (!TextUtils.isEmpty(wifiInterface) && !TextUtils.isEmpty(this.mGcIf)) {
            this.mDCJniAdapter.startOrStopHiP2p(wifiInterface, this.mGcIf, enable);
        }
    }

    /* access modifiers changed from: private */
    public void setDcState(int state) {
        HwHiLog.d(TAG, false, "dcState = %{public}d", new Object[]{Integer.valueOf(state)});
        String wifiInterface = SystemProperties.get("wifi.interface", "wlan0");
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        WifiNative wifiNative = this.mWifiInjector.getWifiNative();
        if (wifiNative == null || wifiNative.mHwWifiNativeEx == null) {
            HwHiLog.e(TAG, false, "wifiNative or mHwWifiNativeEx is null", new Object[0]);
            return;
        }
        if (wifiNative.mHwWifiNativeEx.sendCmdToDriver(wifiInterface, 116, new byte[]{(byte) state}) < 0) {
            HwHiLog.e(TAG, false, "set dc enable state command error", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void setWifiApType(int type) {
        WifiInfo wifiInfo;
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        ClientModeImpl clientModeImpl = this.mWifiInjector.getClientModeImpl();
        if (clientModeImpl != null && (wifiInfo = clientModeImpl.getWifiInfo()) != null) {
            wifiInfo.setWifiApType(type);
        }
    }

    /* access modifiers changed from: private */
    public String getDcInterface() {
        INetworkManagementService iNetworkManagementService = this.mNmService;
        if (iNetworkManagementService == null) {
            HwHiLog.e(TAG, false, "mNmService is null", new Object[0]);
            return "";
        }
        try {
            String[] ifaces = iNetworkManagementService.listInterfaces();
            if (ifaces != null) {
                for (String iface : ifaces) {
                    if (iface != null && iface.startsWith(P2P_TETHER_IFAC_110X)) {
                        return iface;
                    }
                }
            }
            return "";
        } catch (RemoteException | IllegalStateException e) {
            HwHiLog.e(TAG, false, "Error listing Interfaces", new Object[0]);
            return "";
        }
    }

    /* access modifiers changed from: private */
    public void removeGroupAfterCreationFail() {
        String chosenIface = getDcInterface();
        if (TextUtils.isEmpty(chosenIface)) {
            HwHiLog.e(TAG, false, "could not find iface", new Object[0]);
            return;
        }
        HwHiLog.d(TAG, false, "chosenIface is " + chosenIface, new Object[0]);
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        WifiP2pNative wifiNative = this.mWifiInjector.getWifiP2pNative();
        if (wifiNative == null) {
            HwHiLog.e(TAG, false, "wifiNative is null", new Object[0]);
        } else if (!wifiNative.p2pGroupRemove(chosenIface)) {
            HwHiLog.e(TAG, false, "Failed to remove the P2P group", new Object[0]);
        }
    }

    public boolean isDcDisconnectSuccess(String pkgName) {
        if (TextUtils.isEmpty(pkgName) || !ONEHOP_TV_MIRACAST_PACKAGE_NAME.equals(pkgName)) {
            HwHiLog.e(TAG, false, "pkgName Error", new Object[0]);
            return false;
        }
        HwHiLog.d(TAG, false, "pkgName = %{public}s", new Object[]{pkgName});
        if (getCurrentState() == this.mDCConnectedState) {
            removeGcGroup();
            return true;
        } else if (getCurrentState() != this.mDCConnectingState) {
            return false;
        } else {
            if (!TextUtils.isEmpty(this.mGcIf)) {
                removeGcGroup();
            } else {
                removeGroupAfterCreationFail();
            }
            return true;
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
        }

        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class DCInActiveState extends State {
        DCInActiveState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            boolean unused = DCController.this.mIsDcConfigGot = false;
            String unused2 = DCController.this.mGcIf = "";
            boolean unused3 = DCController.this.mIsMagiclinkConnected = false;
            boolean unused4 = DCController.this.mIsDcConnectByApp = false;
            IWifiActionListener unused5 = DCController.this.mListener = null;
        }

        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            if (message.what != 0) {
                return true;
            }
            DCController dCController = DCController.this;
            dCController.transitionTo(dCController.mSCConnectedState);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class SCConnectedState extends State {
        SCConnectedState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            boolean unused = DCController.this.mIsMagiclinkConnected = false;
            boolean unused2 = DCController.this.mIsDcConnecting = false;
            boolean unused3 = DCController.this.mIsDcConfigGot = false;
            boolean unused4 = DCController.this.mIsDcConnectByApp = false;
            IWifiActionListener unused5 = DCController.this.mListener = null;
        }

        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            DCHilinkController dcHilinkController = DCHilinkController.getInstance();
            int i = message.what;
            if (i != 1) {
                if (!(i == 7 || i == 18)) {
                    if (i != 24) {
                        if (i != 32) {
                            if (i == 35) {
                                DCController dCController = DCController.this;
                                dCController.transitionTo(dCController.mDCConnectingState);
                            } else if (i != 3) {
                                if (i != 4) {
                                    if (i != 29) {
                                        if (i != 30) {
                                            return true;
                                        }
                                        if ((dcHilinkController == null || dcHilinkController.isDcAllowed()) && DCController.this.mIsDcConfigGot && DCController.this.mDCArbitra.isValidDcConfigSaved()) {
                                            if (DCController.this.isDcP2pConnectStart(DCController.this.mDCArbitra.selectDcNetwork())) {
                                                DCController dCController2 = DCController.this;
                                                dCController2.transitionTo(dCController2.mDCConnectingState);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                            DCController.this.removeGcGroup();
                        }
                    } else {
                        if (DCController.this.mHandler.hasMessages(24)) {
                            DCController.this.mHandler.removeMessages(24);
                        }
                        if (dcHilinkController == null || dcHilinkController.isDcAllowed()) {
                            HwHiLog.d(DCController.TAG, false, "MSG_GET_DC_CONFIG_SUCC", new Object[0]);
                            if (DCController.this.isDcP2pConnectStart(DCController.this.mDCArbitra.selectDCNetworkFromPayload((String) message.obj))) {
                                DCController dCController3 = DCController.this;
                                dCController3.transitionTo(dCController3.mDCConnectingState);
                            }
                        }
                    }
                    return true;
                }
                boolean unused = DCController.this.mIsDcConfigGot = false;
                return true;
            }
            DCController dCController4 = DCController.this;
            dCController4.transitionTo(dCController4.mDCInActiveState);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class DCConnectingState extends State {
        DCConnectingState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            DCController.this.mDcChr.uploadDcState(1);
            boolean unused = DCController.this.mIsDcConfigGot = false;
            DCController.this.sendMessageDelayed(143361, 20000);
        }

        /* JADX WARNING: Removed duplicated region for block: B:59:0x01a4  */
        /* JADX WARNING: Removed duplicated region for block: B:62:0x01b5  */
        /* JADX WARNING: Removed duplicated region for block: B:65:0x01ca  */
        /* JADX WARNING: Removed duplicated region for block: B:66:0x01d4  */
        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            DCHilinkController dcHilinkController = DCHilinkController.getInstance();
            DCMonitor dcMonitor = DCMonitor.getInstance();
            if (dcMonitor == null || dcHilinkController == null) {
                HwHiLog.e(DCController.TAG, false, "dcMonitor or dcHilinkController is null", new Object[0]);
                return true;
            }
            int i = message.what;
            if (i != 1) {
                if (i != 7) {
                    if (i != 32) {
                        if (i == 143361) {
                            DCController.this.removeGroupAfterCreationFail();
                            if (dcMonitor.isWifiConnected()) {
                                DCController dCController = DCController.this;
                                dCController.transitionTo(dCController.mSCConnectedState);
                            } else {
                                DCController dCController2 = DCController.this;
                                dCController2.transitionTo(dCController2.mDCInActiveState);
                            }
                            dcHilinkController.getDCHilinkHandler().sendEmptyMessage(10);
                            if (DCController.this.mIsDcConnectByApp) {
                                DCController dCController3 = DCController.this;
                                dCController3.notifyListenerOnFailure(dCController3.mListener, 0);
                            }
                        } else if (i != 3) {
                            if (i != 4) {
                                if (i != 5) {
                                    if (i == 10) {
                                        DCController.this.mDcChr.uploadDcP2pConnectFailCount();
                                        HwHiLog.d(DCController.TAG, false, "magiclinkconnect fail, retry most 3 times, RetryTimes=%{public}d", new Object[]{Integer.valueOf(DCController.this.mMagiclinkConnectRetryTimes)});
                                        if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                                            DCController.this.removeGcGroup();
                                        }
                                        boolean unused = DCController.this.mIsDcConnecting = false;
                                        if (DCController.this.mMagiclinkConnectRetryTimes <= 3) {
                                            DCConfiguration dcSelectedNetwork = DCController.this.mDCArbitra.getSelectedDCConfig();
                                            if (dcSelectedNetwork != null) {
                                                boolean unused2 = DCController.this.mIsDcConnecting = true;
                                                DCController.this.startMagiclinkConnect(dcSelectedNetwork);
                                            }
                                        } else {
                                            DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(10);
                                            DCController dCController4 = DCController.this;
                                            dCController4.transitionTo(dCController4.mSCConnectedState);
                                        }
                                    } else if (i != 11) {
                                        if (i == 14) {
                                            DCController.this.startOrStopHiP2p(true);
                                            DCController dCController5 = DCController.this;
                                            dCController5.transitionTo(dCController5.mDCConnectedState);
                                        } else if (i != 15) {
                                            return true;
                                        } else {
                                            if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                                                DCController.this.removeGcGroup();
                                            }
                                            DCController dCController6 = DCController.this;
                                            dCController6.transitionTo(dCController6.mSCConnectedState);
                                        }
                                    }
                                }
                                if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                                    DCController.this.removeGcGroup();
                                }
                                IWifiActionListener unused3 = DCController.this.mListener = null;
                                if (!DCMonitor.getInstance().isWifiConnected()) {
                                    DCController dCController7 = DCController.this;
                                    dCController7.transitionTo(dCController7.mDCInActiveState);
                                } else {
                                    DCController dCController8 = DCController.this;
                                    dCController8.transitionTo(dCController8.mSCConnectedState);
                                }
                            } else if (DCController.this.mIsDcConnectByApp) {
                                DCController dCController9 = DCController.this;
                                String unused4 = dCController9.mGcIf = dCController9.getDcInterface();
                                DCController dCController10 = DCController.this;
                                dCController10.notifyListenerOnSuccess(dCController10.mListener);
                                DCController dCController11 = DCController.this;
                                dCController11.transitionTo(dCController11.mDCConnectedState);
                            }
                        }
                    } else if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                        HwHiLog.d(DCController.TAG, false, "MagiclinkConnected", new Object[0]);
                        boolean unused5 = DCController.this.mIsMagiclinkConnected = true;
                        DCController.this.mDcChr.uploadDcP2pConnectDura(SystemClock.elapsedRealtime());
                        DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(25);
                    } else {
                        DCHilinkController.getInstance().getDCHilinkHandler().sendEmptyMessage(11);
                        DCController dCController12 = DCController.this;
                        dCController12.transitionTo(dCController12.mSCConnectedState);
                    }
                    return true;
                }
                if (message.what == 7 || !DCController.this.mIsDcConnectByApp) {
                    if (TextUtils.isEmpty(DCController.this.mGcIf)) {
                        DCController.this.removeGroupAfterCreationFail();
                    }
                    if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
                    }
                    IWifiActionListener unused6 = DCController.this.mListener = null;
                    if (!DCMonitor.getInstance().isWifiConnected()) {
                    }
                    return true;
                }
                HwHiLog.i(DCController.TAG, false, "Dc Connect By App, do not deal with MSG_GAME_STOP", new Object[0]);
                return true;
            }
            DCController.this.mDcChr.uploadDcConnectWifiDisconnectCount();
            if (message.what == 7) {
            }
            if (TextUtils.isEmpty(DCController.this.mGcIf)) {
            }
            if (!TextUtils.isEmpty(DCController.this.mGcIf)) {
            }
            IWifiActionListener unused7 = DCController.this.mListener = null;
            if (!DCMonitor.getInstance().isWifiConnected()) {
            }
            return true;
        }

        public void exit() {
            boolean unused = DCController.this.mIsDcConnecting = false;
            if (DCController.this.mHandler.hasMessages(143361)) {
                DCController.this.mHandler.removeMessages(143361);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class DCConnectedState extends State {
        DCConnectedState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            boolean unused = DCController.this.mIsDcConnecting = false;
            DCController.this.mDcChr.uploadDcConnectSuccCount();
            DCController.this.mDcChr.uploadDcConnectDura(SystemClock.elapsedRealtime());
            DCController.this.mDcChr.uploadDcState(2);
            DCController.this.setDcState(1);
            DCController.this.setWifiApType(100);
        }

        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            int i = message.what;
            if (i != 1) {
                if (i == 3) {
                    boolean unused = DCController.this.mIsMagiclinkConnected = false;
                    DCController.this.mDcChr.uploadDcState(4);
                    DCController dCController = DCController.this;
                    dCController.transitionTo(dCController.mDCInActiveState);
                } else if (i == 5) {
                    boolean unused2 = DCController.this.mIsMagiclinkConnected = false;
                    if (DCMonitor.getInstance().isWifiConnected()) {
                        DCController.this.mDcChr.uploadDcAbnormalDisconnectCount();
                    }
                    DCController.this.deferMessage(message);
                    DCController dCController2 = DCController.this;
                    dCController2.transitionTo(dCController2.mDCDisconnectingState);
                } else if (i != 7 && i != 29 && i != 34) {
                    return true;
                } else {
                    if (DCController.this.mIsDcConnectByApp) {
                        HwHiLog.i(DCController.TAG, false, "Dc Connect By App, do not deal with MSG_WIFI_SIGNAL_BAD", new Object[0]);
                    }
                }
                return true;
            }
            DCController.this.removeGcGroup();
            DCController dCController3 = DCController.this;
            dCController3.transitionTo(dCController3.mDCDisconnectingState);
            return true;
        }

        public void exit() {
            DCController.this.setDcState(0);
            DCController.this.startOrStopHiP2p(false);
            String unused = DCController.this.mGcIf = "";
            IWifiActionListener unused2 = DCController.this.mListener = null;
            DCController.this.setWifiApType(0);
        }
    }

    /* access modifiers changed from: package-private */
    public class DCDisconnectingState extends State {
        DCDisconnectingState() {
        }

        public void enter() {
            HwHiLog.d(DCController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            DCController.this.mDcChr.uploadDcState(3);
        }

        public boolean processMessage(Message message) {
            DCController.this.logStateAndMessage(this, message);
            DCHilinkController dcHilinkController = DCHilinkController.getInstance();
            DCMonitor dcMonitor = DCMonitor.getInstance();
            if (dcMonitor == null || dcHilinkController == null) {
                HwHiLog.e(DCController.TAG, false, "DCDisconnectingState dcMonitor or dcHilinkController is null", new Object[0]);
                return true;
            }
            int i = message.what;
            if (i != 0) {
                if (i == 1 || i == 3) {
                    DCController dCController = DCController.this;
                    dCController.transitionTo(dCController.mDCInActiveState);
                    return true;
                } else if (i == 5 || i == 7) {
                    if (!DCController.this.mIsMagiclinkConnected) {
                        dcHilinkController.getDCHilinkHandler().sendEmptyMessage(12);
                        if (dcMonitor.isWifiConnected()) {
                            DCController dCController2 = DCController.this;
                            dCController2.transitionTo(dCController2.mSCConnectedState);
                        } else {
                            DCController dCController3 = DCController.this;
                            dCController3.transitionTo(dCController3.mDCInActiveState);
                        }
                    } else {
                        boolean unused = DCController.this.mIsMagiclinkConnected = false;
                        if (dcMonitor.isWifiConnected()) {
                            dcHilinkController.getDCHilinkHandler().sendEmptyMessage(27);
                        } else {
                            DCController dCController4 = DCController.this;
                            dCController4.transitionTo(dCController4.mDCInActiveState);
                        }
                    }
                    return true;
                } else if (i == 12 || i == 17) {
                    if (dcMonitor.isWifiConnected()) {
                        DCController dCController5 = DCController.this;
                        dCController5.transitionTo(dCController5.mSCConnectedState);
                    } else {
                        DCController dCController6 = DCController.this;
                        dCController6.transitionTo(dCController6.mDCInActiveState);
                    }
                    return true;
                } else if (i != 29) {
                    return true;
                }
            }
            DCController dCController7 = DCController.this;
            dCController7.transitionTo(dCController7.mSCConnectedState);
            return true;
        }

        public void exit() {
            DCController.this.mDcChr.uploadDcState(4);
        }
    }
}
