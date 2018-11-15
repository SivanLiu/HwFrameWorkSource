package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicant.IfaceInfo;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.AnqpData;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.Hs20AnqpData;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiSsid;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass10;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass11;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass12;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass2;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass3;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass4;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass5;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass6;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass7;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass8;
import com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass9;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.ANQPParser;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;

@ThreadSafe
public class SupplicantStaIfaceHal {
    public static final int HAL_CALL_THRESHOLD_MS = 300;
    private static final String TAG = "SupplicantStaIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    private final Context mContext;
    private WifiConfiguration mCurrentNetworkLocalConfig;
    private SupplicantStaNetworkHal mCurrentNetworkRemoteHandle;
    private long mHalCallStartTime;
    private String mHalMethod = "";
    private IServiceManager mIServiceManager = null;
    private ISupplicant mISupplicant;
    private ISupplicantStaIface mISupplicantStaIface;
    private ISupplicantStaIfaceCallback mISupplicantStaIfaceCallback;
    private String mIfaceName;
    private final Object mLock = new Object();
    private final DeathRecipient mServiceManagerDeathRecipient = new -$Lambda$lKwQPnSIRFCOpiR3PrChpeYqVrQ((byte) 2, this);
    private final IServiceNotification mServiceNotificationCallback = new Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.i(SupplicantStaIfaceHal.TAG, "IServiceNotification.onRegistration for: " + fqName + ", " + name + " preexisting=" + preexisting);
                }
                if (SupplicantStaIfaceHal.this.initSupplicantService() && (SupplicantStaIfaceHal.this.initSupplicantStaIface() ^ 1) == 0) {
                    Log.i(SupplicantStaIfaceHal.TAG, "Completed initialization of ISupplicant interfaces.");
                } else {
                    Log.e(SupplicantStaIfaceHal.TAG, "initalizing ISupplicantIfaces failed.");
                    SupplicantStaIfaceHal.this.supplicantServiceDiedHandler();
                }
            }
        }
    };
    private final DeathRecipient mSupplicantDeathRecipient = new -$Lambda$lKwQPnSIRFCOpiR3PrChpeYqVrQ((byte) 3, this);
    private boolean mVerboseLoggingEnabled = false;
    private final WifiMonitor mWifiMonitor;

    private static class Mutable<E> {
        public E value;

        Mutable() {
            this.value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

    private class SupplicantStaIfaceHalCallback extends ISupplicantStaIfaceCallback.Stub {
        private static final int WLAN_REASON_IE_IN_4WAY_DIFFERS = 17;
        private boolean mStateIsFourway;

        private SupplicantStaIfaceHalCallback() {
            this.mStateIsFourway = false;
        }

        private ANQPElement parseAnqpElement(ANQPElementType infoID, ArrayList<Byte> payload) {
            ANQPElement parseElement;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                try {
                    if (Constants.getANQPElementID(infoID) != null) {
                        parseElement = ANQPParser.parseElement(infoID, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(payload)));
                    } else {
                        parseElement = ANQPParser.parseHS20Element(infoID, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(payload)));
                    }
                } catch (Exception e) {
                    Log.e(SupplicantStaIfaceHal.TAG, "Failed parsing ANQP element payload: " + infoID, e);
                    return null;
                }
            }
            return parseElement;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void addAnqpElementToMap(Map<ANQPElementType, ANQPElement> elementsMap, ANQPElementType infoID, ArrayList<Byte> payload) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (payload != null) {
                    if (!payload.isEmpty()) {
                        ANQPElement element = parseAnqpElement(infoID, payload);
                        if (element != null) {
                            elementsMap.put(infoID, element);
                        }
                    }
                }
            }
        }

        public void onNetworkAdded(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkAdded");
            }
        }

        public void onNetworkRemoved(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkRemoved");
            }
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onStateChanged");
                SupplicantState newSupplicantState = SupplicantStaIfaceHal.supplicantHidlStateToFrameworkState(newState);
                WifiSsid wifiSsid = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(ssid));
                String bssidStr = NativeUtil.macAddressFromByteArray(bssid);
                this.mStateIsFourway = newState == 7;
                if (newSupplicantState == SupplicantState.COMPLETED) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkConnectionEvent(SupplicantStaIfaceHal.this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(), bssidStr);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastSupplicantStateChangeEvent(SupplicantStaIfaceHal.this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(), wifiSsid, bssidStr, newSupplicantState);
            }
        }

        public void onAnqpQueryDone(byte[] bssid, AnqpData data, Hs20AnqpData hs20Data) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAnqpQueryDone");
                Map<ANQPElementType, ANQPElement> elementsMap = new HashMap();
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQPVenueName, data.venueName);
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQPRoamingConsortium, data.roamingConsortium);
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQPIPAddrAvailability, data.ipAddrTypeAvailability);
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQPNAIRealm, data.naiRealm);
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQP3GPPNetwork, data.anqp3gppCellularNetwork);
                addAnqpElementToMap(elementsMap, ANQPElementType.ANQPDomName, data.domainName);
                addAnqpElementToMap(elementsMap, ANQPElementType.HSFriendlyName, hs20Data.operatorFriendlyName);
                addAnqpElementToMap(elementsMap, ANQPElementType.HSWANMetrics, hs20Data.wanMetrics);
                addAnqpElementToMap(elementsMap, ANQPElementType.HSConnCapability, hs20Data.connectionCapability);
                addAnqpElementToMap(elementsMap, ANQPElementType.HSOSUProviders, hs20Data.osuProvidersList);
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAnqpDoneEvent(SupplicantStaIfaceHal.this.mIfaceName, new AnqpEvent(NativeUtil.macAddressToLong(bssid).longValue(), elementsMap));
            }
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20IconQueryDone");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastIconDoneEvent(SupplicantStaIfaceHal.this.mIfaceName, new IconEvent(NativeUtil.macAddressToLong(bssid).longValue(), fileName, data.size(), NativeUtil.byteArrayFromArrayList(data)));
            }
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20SubscriptionRemediation");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWnmEvent(SupplicantStaIfaceHal.this.mIfaceName, new WnmData(NativeUtil.macAddressToLong(bssid).longValue(), url, osuMethod));
            }
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            boolean z = true;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20DeauthImminentNotice");
                WifiMonitor -get3 = SupplicantStaIfaceHal.this.mWifiMonitor;
                String -get0 = SupplicantStaIfaceHal.this.mIfaceName;
                long longValue = NativeUtil.macAddressToLong(bssid).longValue();
                if (reasonCode != 1) {
                    z = false;
                }
                -get3.broadcastWnmEvent(-get0, new WnmData(longValue, url, z, reAuthDelayInSec));
            }
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onDisconnected");
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.e(SupplicantStaIfaceHal.TAG, "onDisconnected 4way=" + this.mStateIsFourway + " locallyGenerated=" + locallyGenerated + " reasonCode=" + reasonCode);
                }
                if (this.mStateIsFourway && !(locallyGenerated && reasonCode == 17)) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(SupplicantStaIfaceHal.this.mIfaceName, 2);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkDisconnectionEvent(SupplicantStaIfaceHal.this.mIfaceName, locallyGenerated ? 1 : 0, reasonCode, NativeUtil.macAddressFromByteArray(bssid));
            }
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAssociationRejected");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociationRejectionEvent(SupplicantStaIfaceHal.this.mIfaceName, statusCode, timedOut, NativeUtil.macAddressFromByteArray(bssid));
            }
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAuthenticationTimeout");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(SupplicantStaIfaceHal.this.mIfaceName, 1);
            }
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onBssidChanged");
                if (reason == (byte) 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastTargetBssidEvent(SupplicantStaIfaceHal.this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                } else if (reason == (byte) 1) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociatedBssidEvent(SupplicantStaIfaceHal.this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                }
            }
        }

        public void onEapFailure() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(SupplicantStaIfaceHal.this.mIfaceName, 3);
            }
        }

        public void onWpsEventSuccess() {
            SupplicantStaIfaceHal.this.logCallback("onWpsEventSuccess");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsSuccessEvent(SupplicantStaIfaceHal.this.mIfaceName);
            }
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventFail");
                if (configError == (short) 16 && errorInd == (short) 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsTimeoutEvent(SupplicantStaIfaceHal.this.mIfaceName);
                } else {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsFailEvent(SupplicantStaIfaceHal.this.mIfaceName, configError, errorInd);
                }
            }
        }

        public void onWpsEventPbcOverlap() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventPbcOverlap");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsOverlapEvent(SupplicantStaIfaceHal.this.mIfaceName);
            }
        }

        public void onExtRadioWorkStart(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkStart");
            }
        }

        public void onExtRadioWorkTimeout(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkTimeout");
            }
        }

        public void onWapiCertInitFail() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onWapiCertInitFail received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWapiCertInitFailEvent(SupplicantStaIfaceHal.this.mIfaceName);
        }

        public void onWapiAuthFail() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onWapiAuthFail received");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWapiAuthFailEvent(SupplicantStaIfaceHal.this.mIfaceName);
            }
        }

        public void onVoWifiIrqStr() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onVoWifiIrqStr received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastVoWifiIrqStrEvent(SupplicantStaIfaceHal.this.mIfaceName);
        }

        public void onHilinkStartWps(String arg) {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onHilinkStartWps received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastHilinkStartWpsEvent(SupplicantStaIfaceHal.this.mIfaceName, arg);
        }

        public void onHilinkStartWps() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onHilinkStartWps received");
        }

        public void onAbsAntCoreRob() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onAbsAntCoreRob received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAbsAntCoreRobEvent(SupplicantStaIfaceHal.this.mIfaceName);
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_5828(long cookie) {
        synchronized (this.mLock) {
            Log.w(TAG, "IServiceManager died: cookie=" + cookie);
            supplicantServiceDiedHandler();
            this.mIServiceManager = null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_6222(long cookie) {
        synchronized (this.mLock) {
            Log.w(TAG, "ISupplicant/ISupplicantStaIface died: cookie=" + cookie);
            supplicantServiceDiedHandler();
        }
    }

    public SupplicantStaIfaceHal(Context context, WifiMonitor monitor) {
        this.mContext = context;
        this.mWifiMonitor = monitor;
        this.mISupplicantStaIfaceCallback = new SupplicantStaIfaceHalCallback();
    }

    void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                supplicantServiceDiedHandler();
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering ISupplicant service ready callback.");
            }
            this.mISupplicant = null;
            this.mISupplicantStaIface = null;
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                } else if (!linkToServiceManagerDeath()) {
                    return false;
                } else if (!this.mIServiceManager.registerForNotifications(ISupplicant.kInterfaceName, "", this.mServiceNotificationCallback)) {
                    Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.supplicant@1.0::ISupplicant");
                    this.mIServiceManager = null;
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for ISupplicant service: " + e);
                supplicantServiceDiedHandler();
            }
        }
        return true;
    }

    private boolean linkToSupplicantDeath() {
        synchronized (this.mLock) {
            if (this.mISupplicant == null) {
                return false;
            }
            try {
                if (this.mISupplicant.linkToDeath(this.mSupplicantDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on ISupplicant");
                supplicantServiceDiedHandler();
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initSupplicantService() {
        synchronized (this.mLock) {
            try {
                this.mISupplicant = getSupplicantMockable();
                if (this.mISupplicant == null) {
                    Log.e(TAG, "Got null ISupplicant service. Stopping supplicant HIDL startup");
                    return false;
                } else if (linkToSupplicantDeath()) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.getService exception: " + e);
                return false;
            }
        }
    }

    private boolean linkToSupplicantStaIfaceDeath() {
        synchronized (this.mLock) {
            if (this.mISupplicantStaIface == null) {
                return false;
            }
            try {
                if (this.mISupplicantStaIface.linkToDeath(this.mSupplicantDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on ISupplicantStaIface");
                supplicantServiceDiedHandler();
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantStaIface.linkToDeath exception", e);
                return false;
            }
        }
    }

    private int getCurrentNetworkId() {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkLocalConfig == null) {
                return -1;
            }
            int i = this.mCurrentNetworkLocalConfig.networkId;
            return i;
        }
    }

    private boolean initSupplicantStaIface() {
        synchronized (this.mLock) {
            ArrayList<IfaceInfo> supplicantIfaces = new ArrayList();
            try {
                this.mISupplicant.listInterfaces(new com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8.AnonymousClass1(supplicantIfaces));
                if (supplicantIfaces.size() == 0) {
                    Log.e(TAG, "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup.");
                    return false;
                }
                Mutable<ISupplicantIface> supplicantIface = new Mutable();
                Mutable<String> ifaceName = new Mutable();
                for (IfaceInfo ifaceInfo : supplicantIfaces) {
                    if (ifaceInfo.type == 0) {
                        try {
                            this.mISupplicant.getInterface(ifaceInfo, new -$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8(supplicantIface));
                            ifaceName.value = ifaceInfo.name;
                            break;
                        } catch (RemoteException e) {
                            Log.e(TAG, "ISupplicant.getInterface exception: " + e);
                            return false;
                        }
                    }
                }
                if (supplicantIface.value == null) {
                    Log.e(TAG, "initSupplicantStaIface got null iface");
                    return false;
                }
                this.mISupplicantStaIface = getStaIfaceMockable((ISupplicantIface) supplicantIface.value);
                this.mIfaceName = (String) ifaceName.value;
                if (!linkToSupplicantStaIfaceDeath()) {
                    return false;
                } else if (hwStaRegisterCallback(this.mISupplicantStaIfaceCallback)) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e2) {
                Log.e(TAG, "ISupplicant.listInterfaces exception: " + e2);
                return false;
            }
        }
    }

    static /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_12261(ArrayList supplicantIfaces, SupplicantStatus status, ArrayList ifaces) {
        if (status.code != 0) {
            Log.e(TAG, "Getting Supplicant Interfaces failed: " + status.code);
        } else {
            supplicantIfaces.addAll(ifaces);
        }
    }

    static /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_13372(Mutable supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code != 0) {
            Log.e(TAG, "Failed to get ISupplicantIface " + status.code);
        } else {
            supplicantIface.value = iface;
        }
    }

    private void supplicantServiceDiedHandler() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantStaIface = null;
            this.mWifiMonitor.broadcastSupplicantDisconnectionEvent(this.mIfaceName);
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mISupplicantStaIface != null;
        }
        return z;
    }

    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    protected ISupplicant getSupplicantMockable() throws RemoteException {
        ISupplicant service;
        synchronized (this.mLock) {
            service = ISupplicant.getService();
        }
        return service;
    }

    protected ISupplicantStaIface getStaIfaceMockable(ISupplicantIface iface) {
        ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Pair<SupplicantStaNetworkHal, WifiConfiguration> addNetworkAndSaveConfig(WifiConfiguration config) {
        synchronized (this.mLock) {
            logi("addSupplicantStaNetwork via HIDL");
            if (config == null) {
                loge("Cannot add NULL network!");
                return null;
            }
            SupplicantStaNetworkHal network = addNetwork();
            if (network == null) {
                loge("Failed to add a network!");
                return null;
            }
            boolean saveSuccess = false;
            try {
                saveSuccess = network.saveWifiConfiguration(config);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Exception while saving config params: " + config, e);
            }
            if (saveSuccess) {
                Pair<SupplicantStaNetworkHal, WifiConfiguration> pair = new Pair(network, new WifiConfiguration(config));
                return pair;
            }
            loge("Failed to save variables for: " + config.configKey());
            if (!removeAllNetworks()) {
                loge("Failed to remove all networks on failure.");
            }
        }
    }

    public boolean connectToNetwork(WifiConfiguration config) {
        synchronized (this.mLock) {
            logd("connectToNetwork " + config.configKey());
            if (WifiConfigurationUtil.isSameNetwork(config, this.mCurrentNetworkLocalConfig)) {
                logd("Network is already saved, will not trigger remove and add operation.");
            } else {
                this.mCurrentNetworkRemoteHandle = null;
                this.mCurrentNetworkLocalConfig = null;
                if (removeAllNetworks()) {
                    Pair<SupplicantStaNetworkHal, WifiConfiguration> pair = addNetworkAndSaveConfig(config);
                    if (pair == null) {
                        loge("Failed to add/save network configuration: " + config.configKey());
                        return false;
                    }
                    this.mCurrentNetworkRemoteHandle = (SupplicantStaNetworkHal) pair.first;
                    this.mCurrentNetworkLocalConfig = (WifiConfiguration) pair.second;
                } else {
                    loge("Failed to remove existing networks");
                    return false;
                }
            }
            if (this.mCurrentNetworkRemoteHandle == null) {
                loge("mCurrentNetworkRemoteHandle is null when connectToNetwork.");
                return false;
            } else if (this.mCurrentNetworkRemoteHandle.select()) {
                return true;
            } else {
                loge("Failed to select network configuration: " + config.configKey());
                return false;
            }
        }
    }

    public boolean roamToNetwork(WifiConfiguration config) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId() != config.networkId) {
                Log.w(TAG, "Cannot roam to a different network, initiate new connection. Current network ID: " + getCurrentNetworkId());
                boolean connectToNetwork = connectToNetwork(config);
                return connectToNetwork;
            }
            String bssid = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            logd("roamToNetwork" + config.configKey() + " (bssid " + bssid + ")");
            if (this.mCurrentNetworkRemoteHandle == null) {
                loge("mCurrentNetworkRemoteHandle is null when roamToNetwork.");
                return false;
            } else if (!this.mCurrentNetworkRemoteHandle.setBssid(bssid)) {
                loge("Failed to set new bssid on network: " + config.configKey());
                return false;
            } else if (reassociate()) {
                return true;
            } else {
                loge("Failed to trigger reassociate");
                return false;
            }
        }
    }

    public boolean loadNetworks(Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) {
        synchronized (this.mLock) {
            List<Integer> networkIds = listNetworks();
            if (networkIds == null) {
                Log.e(TAG, "Failed to list networks");
                return false;
            }
            for (Integer networkId : networkIds) {
                SupplicantStaNetworkHal network = getNetwork(networkId.intValue());
                if (network == null) {
                    Log.e(TAG, "Failed to get network with ID: " + networkId);
                    return false;
                }
                WifiConfiguration config = new WifiConfiguration();
                Map<String, String> networkExtra = new HashMap();
                boolean loadSuccess = false;
                try {
                    loadSuccess = network.loadWifiConfiguration(config, networkExtra);
                    int appId = UserHandle.getAppId(config.creatorUid);
                    if (config.BSSID != null) {
                        if (!(appId == 0 || appId == 1000)) {
                            if (appId == 1010) {
                            }
                        }
                        Log.w(TAG, "loadNetworks creater: " + config.creatorUid + ", ssid: " + config.SSID + ", Bssid:" + ScanResultUtil.getConfusedBssid(config.BSSID));
                        config.BSSID = null;
                    }
                } catch (IllegalArgumentException e) {
                    Log.wtf(TAG, "Exception while loading config params: " + config, e);
                }
                if (loadSuccess) {
                    config.setIpAssignment(IpAssignment.DHCP);
                    config.setProxySettings(ProxySettings.NONE);
                    networkExtras.put(networkId.intValue(), networkExtra);
                    WifiConfiguration duplicateConfig = (WifiConfiguration) configs.put((String) networkExtra.get("configKey"), config);
                    if (duplicateConfig != null) {
                        Log.i(TAG, "Replacing duplicate network: " + duplicateConfig.networkId);
                        removeNetwork(duplicateConfig.networkId);
                        networkExtras.remove(duplicateConfig.networkId);
                    }
                } else {
                    Log.e(TAG, "Failed to load wifi configuration for network with ID: " + networkId + ". Skipping...");
                }
            }
            return true;
        }
    }

    public void removeNetworkIfCurrent(int networkId) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId() == networkId) {
                removeAllNetworks();
            }
        }
    }

    public boolean removeAllNetworks() {
        synchronized (this.mLock) {
            ArrayList<Integer> networks = listNetworks();
            if (networks == null) {
                Log.e(TAG, "removeAllNetworks failed, got null networks");
                return false;
            }
            for (Integer intValue : networks) {
                int id = intValue.intValue();
                if (!removeNetwork(id)) {
                    Log.e(TAG, "removeAllNetworks failed to remove network: " + id);
                    return false;
                }
            }
            this.mCurrentNetworkLocalConfig = null;
            this.mCurrentNetworkRemoteHandle = null;
            return true;
        }
    }

    public boolean setCurrentNetworkBssid(String bssidStr) {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean bssid = this.mCurrentNetworkRemoteHandle.setBssid(bssidStr);
            return bssid;
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken() {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return null;
            }
            String wpsNfcConfigurationToken = this.mCurrentNetworkRemoteHandle.getWpsNfcConfigurationToken();
            return wpsNfcConfigurationToken;
        }
    }

    public String getCurrentNetworkEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return null;
            }
            String fetchEapAnonymousIdentity = this.mCurrentNetworkRemoteHandle.fetchEapAnonymousIdentity();
            return fetchEapAnonymousIdentity;
        }
    }

    public boolean sendCurrentNetworkEapIdentityResponse(String identityStr) {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapIdentityResponse = this.mCurrentNetworkRemoteHandle.sendNetworkEapIdentityResponse(identityStr);
            return sendNetworkEapIdentityResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthResponse = this.mCurrentNetworkRemoteHandle.sendNetworkEapSimGsmAuthResponse(paramsStr);
            return sendNetworkEapSimGsmAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthFailure() {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthFailure = this.mCurrentNetworkRemoteHandle.sendNetworkEapSimGsmAuthFailure();
            return sendNetworkEapSimGsmAuthFailure;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthResponse = this.mCurrentNetworkRemoteHandle.sendNetworkEapSimUmtsAuthResponse(paramsStr);
            return sendNetworkEapSimUmtsAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAutsResponse(String paramsStr) {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAutsResponse = this.mCurrentNetworkRemoteHandle.sendNetworkEapSimUmtsAutsResponse(paramsStr);
            return sendNetworkEapSimUmtsAutsResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthFailure() {
        synchronized (this.mLock) {
            if (this.mCurrentNetworkRemoteHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthFailure = this.mCurrentNetworkRemoteHandle.sendNetworkEapSimUmtsAuthFailure();
            return sendNetworkEapSimUmtsAuthFailure;
        }
    }

    private SupplicantStaNetworkHal addNetwork() {
        synchronized (this.mLock) {
            String methodStr = "addNetwork";
            if (checkSupplicantStaIfaceAndLogFailure("addNetwork")) {
                Mutable<ISupplicantNetwork> newNetwork = new Mutable();
                try {
                    this.mISupplicantStaIface.addNetwork(new AnonymousClass2(this, newNetwork));
                } catch (RemoteException e) {
                    handleRemoteException(e, "addNetwork");
                }
                if (newNetwork.value != null) {
                    SupplicantStaNetworkHal staNetworkMockable = getStaNetworkMockable(ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) newNetwork.value).asBinder()));
                    return staNetworkMockable;
                }
                return null;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_30285(Mutable newNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (checkStatusAndLogFailure(status, "addNetwork")) {
            newNetwork.value = network;
        }
    }

    private boolean removeNetwork(int id) {
        synchronized (this.mLock) {
            String methodStr = "removeNetwork";
            if (checkSupplicantStaIfaceAndLogFailure("removeNetwork")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.removeNetwork(id), "removeNetwork");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "removeNetwork");
                    return false;
                }
            }
            return false;
        }
    }

    protected SupplicantStaNetworkHal getStaNetworkMockable(ISupplicantStaNetwork iSupplicantStaNetwork) {
        SupplicantStaNetworkHal network;
        synchronized (this.mLock) {
            network = new SupplicantStaNetworkHal(iSupplicantStaNetwork, this.mIfaceName, this.mContext, this.mWifiMonitor);
            if (network != null) {
                network.enableVerboseLogging(this.mVerboseLoggingEnabled);
            }
        }
        return network;
    }

    private SupplicantStaNetworkHal getNetwork(int id) {
        synchronized (this.mLock) {
            String methodStr = "getNetwork";
            Mutable<ISupplicantNetwork> gotNetwork = new Mutable();
            if (checkSupplicantStaIfaceAndLogFailure("getNetwork")) {
                try {
                    this.mISupplicantStaIface.getNetwork(id, new AnonymousClass3(this, gotNetwork));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getNetwork");
                }
                if (gotNetwork.value != null) {
                    SupplicantStaNetworkHal staNetworkMockable = getStaNetworkMockable(ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) gotNetwork.value).asBinder()));
                    return staNetworkMockable;
                }
                return null;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_32882(Mutable gotNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (checkStatusAndLogFailure(status, "getNetwork")) {
            gotNetwork.value = network;
        }
    }

    private boolean hwStaRegisterCallback(ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "hwStaRegisterCallback";
            if (checkSupplicantStaIfaceAndLogFailure("hwStaRegisterCallback")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.hwStaRegisterCallback(callback), "hwStaRegisterCallback");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "hwStaRegisterCallback");
                    return false;
                }
            }
            return false;
        }
    }

    private ArrayList<Integer> listNetworks() {
        synchronized (this.mLock) {
            String methodStr = "listNetworks";
            Mutable<ArrayList<Integer>> networkIdList = new Mutable();
            if (checkSupplicantStaIfaceAndLogFailure("listNetworks")) {
                try {
                    this.mISupplicantStaIface.listNetworks(new AnonymousClass4(this, networkIdList));
                } catch (RemoteException e) {
                    handleRemoteException(e, "listNetworks");
                }
                ArrayList<Integer> arrayList = (ArrayList) networkIdList.value;
                return arrayList;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_34661(Mutable networkIdList, SupplicantStatus status, ArrayList networkIds) {
        if (checkStatusAndLogFailure(status, "listNetworks")) {
            networkIdList.value = networkIds;
        }
    }

    public boolean setWpsDeviceName(String name) {
        synchronized (this.mLock) {
            String methodStr = "setWpsDeviceName";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsDeviceName")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsDeviceName(name), "setWpsDeviceName");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsDeviceName");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsDeviceType(String typeStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = WPS_DEVICE_TYPE_PATTERN.matcher(typeStr);
                if (match.find() && match.groupCount() == 3) {
                    short categ = Short.parseShort(match.group(1));
                    byte[] oui = NativeUtil.hexStringToByteArray(match.group(2));
                    short subCateg = Short.parseShort(match.group(3));
                    byte[] bytes = new byte[8];
                    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                    byteBuffer.putShort(categ);
                    byteBuffer.put(oui);
                    byteBuffer.putShort(subCateg);
                    boolean wpsDeviceType = setWpsDeviceType(bytes);
                    return wpsDeviceType;
                }
                Log.e(TAG, "Malformed WPS device type " + typeStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + typeStr, e);
                return false;
            }
        }
    }

    private boolean setWpsDeviceType(byte[] type) {
        synchronized (this.mLock) {
            String methodStr = "setWpsDeviceType";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsDeviceType")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsDeviceType(type), "setWpsDeviceType");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsDeviceType");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsManufacturer(String manufacturer) {
        synchronized (this.mLock) {
            String methodStr = "setWpsManufacturer";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsManufacturer")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsManufacturer(manufacturer), "setWpsManufacturer");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsManufacturer");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsModelName(String modelName) {
        synchronized (this.mLock) {
            String methodStr = "setWpsModelName";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsModelName")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsModelName(modelName), "setWpsModelName");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsModelName");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsModelNumber(String modelNumber) {
        synchronized (this.mLock) {
            String methodStr = "setWpsModelNumber";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsModelNumber")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsModelNumber(modelNumber), "setWpsModelNumber");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsModelNumber");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsSerialNumber(String serialNumber) {
        synchronized (this.mLock) {
            String methodStr = "setWpsSerialNumber";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsSerialNumber")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsSerialNumber(serialNumber), "setWpsSerialNumber");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsSerialNumber");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setWpsConfigMethods(String configMethodsStr) {
        short configMethodsMask;
        synchronized (this.mLock) {
            configMethodsMask = (short) 0;
            for (String stringToWpsConfigMethod : configMethodsStr.split("\\s+")) {
                configMethodsMask = (short) (stringToWpsConfigMethod(stringToWpsConfigMethod) | configMethodsMask);
            }
        }
        return setWpsConfigMethods(configMethodsMask);
    }

    private boolean setWpsConfigMethods(short configMethods) {
        synchronized (this.mLock) {
            String methodStr = "setWpsConfigMethods";
            if (checkSupplicantStaIfaceAndLogFailure("setWpsConfigMethods")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setWpsConfigMethods(configMethods), "setWpsConfigMethods");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWpsConfigMethods");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean reassociate() {
        synchronized (this.mLock) {
            String methodStr = "reassociate";
            if (checkSupplicantStaIfaceAndLogFailure("reassociate")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.reassociate(), "reassociate");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "reassociate");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean reconnect() {
        synchronized (this.mLock) {
            String methodStr = "reconnect";
            if (checkSupplicantStaIfaceAndLogFailure("reconnect")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.reconnect(), "reconnect");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "reconnect");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean disconnect() {
        synchronized (this.mLock) {
            String methodStr = "disconnect";
            if (checkSupplicantStaIfaceAndLogFailure("disconnect")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.disconnect(), "disconnect");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "disconnect");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setPowerSave(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setPowerSave";
            if (checkSupplicantStaIfaceAndLogFailure("setPowerSave")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setPowerSave(enable), "setPowerSave");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPowerSave");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean initiateTdlsDiscover(String macAddress) {
        boolean initiateTdlsDiscover;
        synchronized (this.mLock) {
            try {
                initiateTdlsDiscover = initiateTdlsDiscover(NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            }
        }
        return initiateTdlsDiscover;
    }

    private boolean initiateTdlsDiscover(byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsDiscover";
            if (checkSupplicantStaIfaceAndLogFailure("initiateTdlsDiscover")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.initiateTdlsDiscover(macAddress), "initiateTdlsDiscover");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "initiateTdlsDiscover");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean initiateTdlsSetup(String macAddress) {
        boolean initiateTdlsSetup;
        synchronized (this.mLock) {
            try {
                initiateTdlsSetup = initiateTdlsSetup(NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            }
        }
        return initiateTdlsSetup;
    }

    private boolean initiateTdlsSetup(byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsSetup";
            if (checkSupplicantStaIfaceAndLogFailure("initiateTdlsSetup")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.initiateTdlsSetup(macAddress), "initiateTdlsSetup");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "initiateTdlsSetup");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean initiateTdlsTeardown(String macAddress) {
        boolean initiateTdlsTeardown;
        synchronized (this.mLock) {
            try {
                initiateTdlsTeardown = initiateTdlsTeardown(NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            }
        }
        return initiateTdlsTeardown;
    }

    private boolean initiateTdlsTeardown(byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsTeardown";
            if (checkSupplicantStaIfaceAndLogFailure("initiateTdlsTeardown")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.initiateTdlsTeardown(macAddress), "initiateTdlsTeardown");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "initiateTdlsTeardown");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean initiateAnqpQuery(String bssid, ArrayList<Short> infoElements, ArrayList<Integer> hs20SubTypes) {
        boolean initiateAnqpQuery;
        synchronized (this.mLock) {
            try {
                initiateAnqpQuery = initiateAnqpQuery(NativeUtil.macAddressToByteArray(bssid), (ArrayList) infoElements, (ArrayList) hs20SubTypes);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssid, e);
                return false;
            }
        }
        return initiateAnqpQuery;
    }

    private boolean initiateAnqpQuery(byte[] macAddress, ArrayList<Short> infoElements, ArrayList<Integer> subTypes) {
        synchronized (this.mLock) {
            String methodStr = "initiateAnqpQuery";
            if (checkSupplicantStaIfaceAndLogFailure("initiateAnqpQuery")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.initiateAnqpQuery(macAddress, infoElements, subTypes), "initiateAnqpQuery");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "initiateAnqpQuery");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean initiateHs20IconQuery(String bssid, String fileName) {
        boolean initiateHs20IconQuery;
        synchronized (this.mLock) {
            try {
                initiateHs20IconQuery = initiateHs20IconQuery(NativeUtil.macAddressToByteArray(bssid), fileName);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssid, e);
                return false;
            }
        }
        return initiateHs20IconQuery;
    }

    private boolean initiateHs20IconQuery(byte[] macAddress, String fileName) {
        synchronized (this.mLock) {
            String methodStr = "initiateHs20IconQuery";
            if (checkSupplicantStaIfaceAndLogFailure("initiateHs20IconQuery")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.initiateHs20IconQuery(macAddress, fileName), "initiateHs20IconQuery");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "initiateHs20IconQuery");
                    return false;
                }
            }
            return false;
        }
    }

    public String getMacAddress() {
        synchronized (this.mLock) {
            String methodStr = "getMacAddress";
            if (checkSupplicantStaIfaceAndLogFailure("getMacAddress")) {
                Mutable<String> gotMac = new Mutable();
                try {
                    this.mISupplicantStaIface.getMacAddress(new AnonymousClass5(this, gotMac));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getMacAddress");
                }
                String str = (String) gotMac.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_51626(Mutable gotMac, SupplicantStatus status, byte[] macAddr) {
        if (checkStatusAndLogFailure(status, "getMacAddress")) {
            gotMac.value = NativeUtil.macAddressFromByteArray(macAddr);
        }
    }

    public boolean startRxFilter() {
        synchronized (this.mLock) {
            String methodStr = "startRxFilter";
            if (checkSupplicantStaIfaceAndLogFailure("startRxFilter")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.startRxFilter(), "startRxFilter");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startRxFilter");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean stopRxFilter() {
        synchronized (this.mLock) {
            String methodStr = "stopRxFilter";
            if (checkSupplicantStaIfaceAndLogFailure("stopRxFilter")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.stopRxFilter(), "stopRxFilter");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "stopRxFilter");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean addRxFilter(int type) {
        synchronized (this.mLock) {
            byte halType;
            switch (type) {
                case 0:
                    halType = (byte) 0;
                    break;
                case 1:
                    halType = (byte) 1;
                    break;
                default:
                    Log.e(TAG, "Invalid Rx Filter type: " + type);
                    return false;
            }
            boolean addRxFilter = addRxFilter(halType);
            return addRxFilter;
        }
    }

    public boolean addRxFilter(byte type) {
        synchronized (this.mLock) {
            String methodStr = "addRxFilter";
            if (checkSupplicantStaIfaceAndLogFailure("addRxFilter")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.addRxFilter(type), "addRxFilter");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "addRxFilter");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean removeRxFilter(int type) {
        synchronized (this.mLock) {
            byte halType;
            switch (type) {
                case 0:
                    halType = (byte) 0;
                    break;
                case 1:
                    halType = (byte) 1;
                    break;
                default:
                    Log.e(TAG, "Invalid Rx Filter type: " + type);
                    return false;
            }
            boolean removeRxFilter = removeRxFilter(halType);
            return removeRxFilter;
        }
    }

    public boolean removeRxFilter(byte type) {
        synchronized (this.mLock) {
            String methodStr = "removeRxFilter";
            if (checkSupplicantStaIfaceAndLogFailure("removeRxFilter")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.removeRxFilter(type), "removeRxFilter");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "removeRxFilter");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setBtCoexistenceMode(int mode) {
        synchronized (this.mLock) {
            byte halMode;
            switch (mode) {
                case 0:
                    halMode = (byte) 0;
                    break;
                case 1:
                    halMode = (byte) 1;
                    break;
                case 2:
                    halMode = (byte) 2;
                    break;
                default:
                    Log.e(TAG, "Invalid Bt Coex mode: " + mode);
                    return false;
            }
            boolean btCoexistenceMode = setBtCoexistenceMode(halMode);
            return btCoexistenceMode;
        }
    }

    private boolean setBtCoexistenceMode(byte mode) {
        synchronized (this.mLock) {
            String methodStr = "setBtCoexistenceMode";
            if (checkSupplicantStaIfaceAndLogFailure("setBtCoexistenceMode")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setBtCoexistenceMode(mode), "setBtCoexistenceMode");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setBtCoexistenceMode");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setBtCoexistenceScanModeEnabled(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setBtCoexistenceScanModeEnabled";
            if (checkSupplicantStaIfaceAndLogFailure("setBtCoexistenceScanModeEnabled")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setBtCoexistenceScanModeEnabled(enable), "setBtCoexistenceScanModeEnabled");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setBtCoexistenceScanModeEnabled");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setSuspendModeEnabled(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setSuspendModeEnabled";
            if (checkSupplicantStaIfaceAndLogFailure("setSuspendModeEnabled")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setSuspendModeEnabled(enable), "setSuspendModeEnabled");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setSuspendModeEnabled");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setFilterEnable(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setFilterEnable";
            if (checkSupplicantStaIfaceAndLogFailure("setFilterEnable")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setFilterEnable(enable), "setFilterEnable");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setFilterEnable");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setCountryCode(String codeStr) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(codeStr)) {
                return false;
            }
            boolean countryCode = setCountryCode(NativeUtil.stringToByteArray(codeStr));
            return countryCode;
        }
    }

    private boolean setCountryCode(byte[] code) {
        synchronized (this.mLock) {
            String methodStr = "setCountryCode";
            if (checkSupplicantStaIfaceAndLogFailure("setCountryCode")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setCountryCode(code), "setCountryCode");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setCountryCode");
                    return false;
                }
            }
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startWpsRegistrar(String bssidStr, String pin) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(bssidStr) || TextUtils.isEmpty(pin)) {
            } else {
                try {
                    boolean startWpsRegistrar = startWpsRegistrar(NativeUtil.macAddressToByteArray(bssidStr), pin);
                    return startWpsRegistrar;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + bssidStr, e);
                    return false;
                }
            }
        }
    }

    private boolean startWpsRegistrar(byte[] bssid, String pin) {
        synchronized (this.mLock) {
            String methodStr = "startWpsRegistrar";
            if (checkSupplicantStaIfaceAndLogFailure("startWpsRegistrar")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.startWpsRegistrar(bssid, pin), "startWpsRegistrar");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsRegistrar");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean startWpsPbc(String bssidStr) {
        boolean startWpsPbc;
        synchronized (this.mLock) {
            try {
                startWpsPbc = startWpsPbc(NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return false;
            }
        }
        return startWpsPbc;
    }

    private boolean startWpsPbc(byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "startWpsPbc";
            if (checkSupplicantStaIfaceAndLogFailure("startWpsPbc")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.startWpsPbc(bssid), "startWpsPbc");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsPbc");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean startWpsPinKeypad(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "startWpsPinKeypad";
            if (checkSupplicantStaIfaceAndLogFailure("startWpsPinKeypad")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.startWpsPinKeypad(pin), "startWpsPinKeypad");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsPinKeypad");
                    return false;
                }
            }
            return false;
        }
    }

    public String startWpsPinDisplay(String bssidStr) {
        String startWpsPinDisplay;
        synchronized (this.mLock) {
            try {
                startWpsPinDisplay = startWpsPinDisplay(NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return null;
            }
        }
        return startWpsPinDisplay;
    }

    private String startWpsPinDisplay(byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "startWpsPinDisplay";
            if (checkSupplicantStaIfaceAndLogFailure("startWpsPinDisplay")) {
                Mutable<String> gotPin = new Mutable();
                try {
                    this.mISupplicantStaIface.startWpsPinDisplay(bssid, new AnonymousClass6(this, gotPin));
                } catch (RemoteException e) {
                    handleRemoteException(e, "startWpsPinDisplay");
                }
                String str = (String) gotPin.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_65764(Mutable gotPin, SupplicantStatus status, String pin) {
        if (checkStatusAndLogFailure(status, "startWpsPinDisplay")) {
            gotPin.value = pin;
        }
    }

    public boolean cancelWps() {
        synchronized (this.mLock) {
            String methodStr = "cancelWps";
            if (checkSupplicantStaIfaceAndLogFailure("cancelWps")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.cancelWps(), "cancelWps");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "cancelWps");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setExternalSim(boolean useExternalSim) {
        synchronized (this.mLock) {
            String methodStr = "setExternalSim";
            if (checkSupplicantStaIfaceAndLogFailure("setExternalSim")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setExternalSim(useExternalSim), "setExternalSim");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setExternalSim");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean enableAutoReconnect(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "enableAutoReconnect";
            if (checkSupplicantAndLogFailure("enableAutoReconnect")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.enableAutoReconnect(enable), "enableAutoReconnect");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "enableAutoReconnect");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setLogLevel(boolean turnOnVerbose) {
        boolean debugParams;
        synchronized (this.mLock) {
            int logLevel;
            if (turnOnVerbose) {
                logLevel = 2;
            } else {
                logLevel = 3;
            }
            debugParams = setDebugParams(logLevel, false, false);
        }
        return debugParams;
    }

    private boolean setDebugParams(int level, boolean showTimestamp, boolean showKeys) {
        synchronized (this.mLock) {
            String methodStr = "setDebugParams";
            if (checkSupplicantAndLogFailure("setDebugParams")) {
                return true;
            }
            return false;
        }
    }

    public boolean setConcurrencyPriority(boolean isStaHigherPriority) {
        synchronized (this.mLock) {
            if (isStaHigherPriority) {
                boolean concurrencyPriority = setConcurrencyPriority(0);
                return concurrencyPriority;
            }
            concurrencyPriority = setConcurrencyPriority(1);
            return concurrencyPriority;
        }
    }

    private boolean setConcurrencyPriority(int type) {
        synchronized (this.mLock) {
            String methodStr = "setConcurrencyPriority";
            if (checkSupplicantAndLogFailure("setConcurrencyPriority")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicant.setConcurrencyPriority(type), "setConcurrencyPriority");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setConcurrencyPriority");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean checkSupplicantAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            this.mHalMethod = methodStr;
            this.mHalCallStartTime = SystemClock.uptimeMillis();
            if (this.mISupplicant == null) {
                Log.e(TAG, "Can't call " + methodStr + ", ISupplicant is null");
                return false;
            }
            return true;
        }
    }

    private boolean checkSupplicantStaIfaceAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            this.mHalMethod = methodStr;
            this.mHalCallStartTime = SystemClock.uptimeMillis();
            if (this.mISupplicantStaIface == null) {
                Log.e(TAG, "Can't call " + methodStr + ", ISupplicantStaIface is null");
                return false;
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        checkHalCallThresholdMs(status, methodStr);
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed: " + supplicantStatusCodeToString(status.code) + ", " + status.debugMessage);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIface." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private void checkHalCallThresholdMs(SupplicantStatus status, String methodStr) {
        long mHalCallEndTime = SystemClock.uptimeMillis();
        if (!this.mHalMethod.equals(methodStr)) {
            Log.w(TAG, "error, mHalCallStartTime in:" + this.mHalMethod + ", mHalCallEndTime in:" + methodStr);
        } else if (mHalCallEndTime - this.mHalCallStartTime > 300) {
            Log.w(TAG, "Hal call took " + (mHalCallEndTime - this.mHalCallStartTime) + "ms on " + methodStr + ", status.code:" + supplicantStatusCodeToString(status.code), new Exception());
        }
    }

    private void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIfaceCallback." + methodStr + " received");
            }
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            supplicantServiceDiedHandler();
            Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed with exception", e);
        }
    }

    public static String supplicantStatusCodeToString(int code) {
        switch (code) {
            case 0:
                return "SUCCESS";
            case 1:
                return "FAILURE_UNKNOWN";
            case 2:
                return "FAILURE_ARGS_INVALID";
            case 3:
                return "FAILURE_IFACE_INVALID";
            case 4:
                return "FAILURE_IFACE_UNKNOWN";
            case 5:
                return "FAILURE_IFACE_EXISTS";
            case 6:
                return "FAILURE_IFACE_DISABLED";
            case 7:
                return "FAILURE_IFACE_NOT_DISCONNECTED";
            case 8:
                return "FAILURE_NETWORK_INVALID";
            case 9:
                return "FAILURE_NETWORK_UNKNOWN";
            default:
                return "??? UNKNOWN_CODE";
        }
    }

    private static short stringToWpsConfigMethod(String configMethod) {
        if (configMethod.equals("usba")) {
            return (short) 1;
        }
        if (configMethod.equals("ethernet")) {
            return (short) 2;
        }
        if (configMethod.equals("label")) {
            return (short) 4;
        }
        if (configMethod.equals("display")) {
            return (short) 8;
        }
        if (configMethod.equals("int_nfc_token")) {
            return (short) 32;
        }
        if (configMethod.equals("ext_nfc_token")) {
            return (short) 16;
        }
        if (configMethod.equals("nfc_interface")) {
            return (short) 64;
        }
        if (configMethod.equals("push_button")) {
            return WpsConfigMethods.PUSHBUTTON;
        }
        if (configMethod.equals("keypad")) {
            return WpsConfigMethods.KEYPAD;
        }
        if (configMethod.equals("virtual_push_button")) {
            return WpsConfigMethods.VIRT_PUSHBUTTON;
        }
        if (configMethod.equals("physical_push_button")) {
            return WpsConfigMethods.PHY_PUSHBUTTON;
        }
        if (configMethod.equals("p2ps")) {
            return WpsConfigMethods.P2PS;
        }
        if (configMethod.equals("virtual_display")) {
            return WpsConfigMethods.VIRT_DISPLAY;
        }
        if (configMethod.equals("physical_display")) {
            return WpsConfigMethods.PHY_DISPLAY;
        }
        throw new IllegalArgumentException("Invalid WPS config method: " + configMethod);
    }

    private static SupplicantState supplicantHidlStateToFrameworkState(int state) {
        switch (state) {
            case 0:
                return SupplicantState.DISCONNECTED;
            case 1:
                return SupplicantState.INTERFACE_DISABLED;
            case 2:
                return SupplicantState.INACTIVE;
            case 3:
                return SupplicantState.SCANNING;
            case 4:
                return SupplicantState.AUTHENTICATING;
            case 5:
                return SupplicantState.ASSOCIATING;
            case 6:
                return SupplicantState.ASSOCIATED;
            case 7:
                return SupplicantState.FOUR_WAY_HANDSHAKE;
            case 8:
                return SupplicantState.GROUP_HANDSHAKE;
            case 9:
                return SupplicantState.COMPLETED;
            default:
                throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    private static void logi(String s) {
        Log.i(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }

    public String voWifiDetect(String cmd) {
        synchronized (this.mLock) {
            String methodStr = "VowifiDetect";
            if (checkSupplicantStaIfaceAndLogFailure("VowifiDetect")) {
                Mutable<String> gotResult = new Mutable();
                try {
                    this.mISupplicantStaIface.VowifiDetect(cmd, new AnonymousClass7(this, gotResult));
                } catch (RemoteException e) {
                    handleRemoteException(e, "VowifiDetect");
                }
                String str = (String) gotResult.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_91463(Mutable gotResult, SupplicantStatus status, String result) {
        if (checkStatusAndLogFailure(status, "VowifiDetect")) {
            gotResult.value = result;
            logd(result);
        }
    }

    public String heartBeat(String param) {
        synchronized (this.mLock) {
            String methodStr = "heartBeat";
            if (checkSupplicantStaIfaceAndLogFailure("heartBeat")) {
                Mutable<String> gotResult = new Mutable();
                try {
                    this.mISupplicantStaIface.heartBeat(param, new AnonymousClass12(this, gotResult));
                } catch (RemoteException e) {
                    handleRemoteException(e, "heartBeat");
                }
                String str = (String) gotResult.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_92403(Mutable gotResult, SupplicantStatus status, String result) {
        if (checkStatusAndLogFailure(status, "heartBeat")) {
            gotResult.value = result;
            logd(result);
        }
    }

    public boolean enableHiLinkHandshake(boolean uiEnable, String bssid) {
        Log.d(TAG, "enableHiLinkHandshake:uiEnable=" + uiEnable + " bssid=" + bssid);
        synchronized (this.mLock) {
            String methodStr = "enableHiLinkHandshake";
            if (checkSupplicantStaIfaceAndLogFailure("enableHiLinkHandshake")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.enableHiLinkHandshake(uiEnable, bssid), "enableHiLinkHandshake");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "enableHiLinkHandshake");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setTxPower(int level) {
        synchronized (this.mLock) {
            String methodStr = "setTxPower";
            if (checkSupplicantStaIfaceAndLogFailure("setTxPower")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.setTxPower(level), "setTxPower");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setTxPower");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setAbsCapability(int capability) {
        synchronized (this.mLock) {
            String methodStr = "SetAbsCapability";
            if (checkSupplicantStaIfaceAndLogFailure("SetAbsCapability")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.SetAbsCapability(capability), "SetAbsCapability");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "SetAbsCapability");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean absPowerCtrl(int type) {
        synchronized (this.mLock) {
            String methodStr = "AbsPowerCtrl";
            if (checkSupplicantStaIfaceAndLogFailure("AbsPowerCtrl")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.AbsPowerCtrl(type), "AbsPowerCtrl");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "AbsPowerCtrl");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setAbsBlacklist(String bssidList) {
        synchronized (this.mLock) {
            String methodStr = "SetAbsBlacklist";
            if (checkSupplicantStaIfaceAndLogFailure("SetAbsBlacklist")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.SetAbsBlacklist(bssidList), "SetAbsBlacklist");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "SetAbsBlacklist");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean query11vRoamingNetwork(int reason) {
        synchronized (this.mLock) {
            String methodStr = "wnmBssQurey";
            if (checkSupplicantStaIfaceAndLogFailure("wnmBssQurey")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.wnmBssQurey(reason), "wnmBssQurey");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "wnmBssQurey");
                    return false;
                }
            }
            return false;
        }
    }

    public String getRsdbCapability() {
        synchronized (this.mLock) {
            String methodStr = "getCapabRsdb";
            if (checkSupplicantStaIfaceAndLogFailure("getCapabRsdb")) {
                Mutable<String> gotRsdb = new Mutable();
                try {
                    this.mISupplicantStaIface.getCapabRsdb(new AnonymousClass9(this, gotRsdb));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getCapabRsdb");
                }
                String str = (String) gotRsdb.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_97278(Mutable gotRsdb, SupplicantStatus status, String rsdb) {
        if (checkStatusAndLogFailure(status, "getCapabRsdb")) {
            gotRsdb.value = rsdb;
        }
    }

    public String getWpasConfig(int psktype) {
        synchronized (this.mLock) {
            String methodStr = "getWpasConfig";
            if (checkSupplicantStaIfaceAndLogFailure("getWpasConfig")) {
                Mutable<String> gotPsk = new Mutable();
                try {
                    this.mISupplicantStaIface.getWpasConfig(psktype, new AnonymousClass11(this, gotPsk));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWpasConfig");
                }
                String str = (String) gotPsk.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_98168(Mutable gotPsk, SupplicantStatus status, String psk) {
        if (checkStatusAndLogFailure(status, "getWpasConfig")) {
            gotPsk.value = psk;
        }
    }

    public boolean pwrPercentBoostModeset(int rssi) {
        synchronized (this.mLock) {
            String methodStr = "pwrPercentBoostModeset";
            if (checkSupplicantStaIfaceAndLogFailure("pwrPercentBoostModeset")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaIface.pwrPercentBoostModeset(rssi), "pwrPercentBoostModeset");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "pwrPercentBoostModeset");
                    return false;
                }
            }
            return false;
        }
    }

    public String getMssState() {
        synchronized (this.mLock) {
            String methodStr = "getMssState";
            if (checkSupplicantStaIfaceAndLogFailure("getMssState")) {
                Mutable<String> gotMss = new Mutable();
                try {
                    this.mISupplicantStaIface.getMssState(new AnonymousClass10(this, gotMss));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getMssState");
                }
                String str = (String) gotMss.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_99742(Mutable gotMss, SupplicantStatus status, String mss) {
        if (checkStatusAndLogFailure(status, "getMssState")) {
            gotMss.value = mss;
        }
    }

    public String getApVendorInfo() {
        synchronized (this.mLock) {
            String methodStr = "getApVendorInfo";
            if (checkSupplicantStaIfaceAndLogFailure("getApVendorInfo")) {
                Mutable<String> gotApVendorInfo = new Mutable();
                try {
                    this.mISupplicantStaIface.getApVendorInfo(new AnonymousClass8(this, gotApVendorInfo));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getApVendorInfo");
                }
                String str = (String) gotApVendorInfo.value;
                return str;
            }
            return null;
        }
    }

    /* synthetic */ void lambda$-com_android_server_wifi_SupplicantStaIfaceHal_100627(Mutable gotApVendorInfo, SupplicantStatus status, String apvendorinfo) {
        if (checkStatusAndLogFailure(status, "getApVendorInfo")) {
            gotApVendorInfo.value = apvendorinfo;
        }
    }
}
