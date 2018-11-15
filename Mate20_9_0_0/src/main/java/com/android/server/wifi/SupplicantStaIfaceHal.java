package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicant.IfaceInfo;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.AnqpData;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.Hs20AnqpData;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatusCode;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiSsid;
import android.os.HidlSupport.Mutable;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.wifi.WifiNative.SupplicantDeathEventHandler;
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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SupplicantStaIfaceHal {
    public static final int HAL_CALL_THRESHOLD_MS = 300;
    private static final String TAG = "SupplicantStaIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    private final Context mContext;
    private HashMap<String, WifiConfiguration> mCurrentNetworkLocalConfigs = new HashMap();
    private HashMap<String, SupplicantStaNetworkHal> mCurrentNetworkRemoteHandles = new HashMap();
    private SupplicantDeathEventHandler mDeathEventHandler;
    private long mHalCallStartTime;
    private String mHalMethod = "";
    private IServiceManager mIServiceManager = null;
    private ISupplicant mISupplicant;
    private HashMap<String, ISupplicantStaIfaceCallback> mISupplicantStaIfaceCallbacks = new HashMap();
    private HashMap<String, ISupplicantStaIface> mISupplicantStaIfaces = new HashMap();
    private final Object mLock = new Object();
    private final DeathRecipient mServiceManagerDeathRecipient = new -$$Lambda$SupplicantStaIfaceHal$HYy_ivRYb5h7sLwkHNoi3DEuZxA(this);
    private final IServiceNotification mServiceNotificationCallback = new Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    String str = SupplicantStaIfaceHal.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IServiceNotification.onRegistration for: ");
                    stringBuilder.append(fqName);
                    stringBuilder.append(", ");
                    stringBuilder.append(name);
                    stringBuilder.append(" preexisting=");
                    stringBuilder.append(preexisting);
                    Log.i(str, stringBuilder.toString());
                }
                if (SupplicantStaIfaceHal.this.initSupplicantService()) {
                    Log.i(SupplicantStaIfaceHal.TAG, "Completed initialization of ISupplicant.");
                } else {
                    Log.e(SupplicantStaIfaceHal.TAG, "initalizing ISupplicant failed.");
                    SupplicantStaIfaceHal.this.supplicantServiceDiedHandler();
                }
            }
        }
    };
    private final DeathRecipient mSupplicantDeathRecipient = new -$$Lambda$SupplicantStaIfaceHal$MsPuzKcT4xAfuigKAAOs1rYm9CU(this);
    private boolean mVerboseLoggingEnabled = false;
    private final WifiMonitor mWifiMonitor;

    private class SupplicantStaIfaceHalCallback extends ISupplicantStaIfaceCallback.Stub {
        private String mIfaceName;
        private boolean mStateIsFourway = false;

        SupplicantStaIfaceHalCallback(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        /* JADX WARNING: Removed duplicated region for block: B:11:0x002c A:{Splitter: B:2:0x0007, Catch:{ IOException -> 0x002c, IOException -> 0x002c }, ExcHandler: java.io.IOException (r1_9 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:11:0x002c, code:
            r1 = move-exception;
     */
        /* JADX WARNING: Missing block: B:12:0x002d, code:
            r2 = com.android.server.wifi.SupplicantStaIfaceHal.TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Failed parsing ANQP element payload: ");
            r3.append(r6);
            android.util.Log.e(r2, r3.toString(), r1);
     */
        /* JADX WARNING: Missing block: B:14:0x0045, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
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
                }
            }
            return parseElement;
        }

        /* JADX WARNING: Missing block: B:10:0x001a, code:
            return;
     */
        /* JADX WARNING: Missing block: B:13:0x001e, code:
            return;
     */
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
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkConnectionEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), bssidStr);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastSupplicantStateChangeEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), wifiSsid, bssidStr, newSupplicantState);
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
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAnqpDoneEvent(this.mIfaceName, new AnqpEvent(NativeUtil.macAddressToLong(bssid).longValue(), elementsMap));
            }
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20IconQueryDone");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastIconDoneEvent(this.mIfaceName, new IconEvent(NativeUtil.macAddressToLong(bssid).longValue(), fileName, data.size(), NativeUtil.byteArrayFromArrayList(data)));
            }
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20SubscriptionRemediation");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWnmEvent(this.mIfaceName, new WnmData(NativeUtil.macAddressToLong(bssid).longValue(), url, osuMethod));
            }
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20DeauthImminentNotice");
                WifiMonitor access$700 = SupplicantStaIfaceHal.this.mWifiMonitor;
                String str = this.mIfaceName;
                long longValue = NativeUtil.macAddressToLong(bssid).longValue();
                boolean z = true;
                if (reasonCode != 1) {
                    z = false;
                }
                access$700.broadcastWnmEvent(str, new WnmData(longValue, url, z, reAuthDelayInSec));
            }
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onDisconnected");
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    String str = SupplicantStaIfaceHal.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onDisconnected 4way=");
                    stringBuilder.append(this.mStateIsFourway);
                    stringBuilder.append(" locallyGenerated=");
                    stringBuilder.append(locallyGenerated);
                    stringBuilder.append(" reasonCode=");
                    stringBuilder.append(reasonCode);
                    Log.e(str, stringBuilder.toString());
                }
                if (this.mStateIsFourway && !(locallyGenerated && reasonCode == 17)) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 2, -1);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkDisconnectionEvent(this.mIfaceName, locallyGenerated, reasonCode, NativeUtil.macAddressFromByteArray(bssid));
            }
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAssociationRejected");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociationRejectionEvent(this.mIfaceName, statusCode, timedOut, NativeUtil.macAddressFromByteArray(bssid));
            }
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAuthenticationTimeout");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 1, -1);
            }
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onBssidChanged");
                if (reason == (byte) 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastTargetBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                } else if (reason == (byte) 1) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociatedBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                }
            }
        }

        public void onEapFailure() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, -1);
            }
        }

        public void onWpsEventSuccess() {
            SupplicantStaIfaceHal.this.logCallback("onWpsEventSuccess");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsSuccessEvent(this.mIfaceName);
            }
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventFail");
                if (configError == (short) 16 && errorInd == (short) 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsTimeoutEvent(this.mIfaceName);
                } else {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsFailEvent(this.mIfaceName, configError, errorInd);
                }
            }
        }

        public void onWpsEventPbcOverlap() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventPbcOverlap");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsOverlapEvent(this.mIfaceName);
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
    }

    private class SupplicantStaIfaceHalCallbackV1_1 extends android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback.Stub {
        private SupplicantStaIfaceHalCallback mCallbackV1_0;
        private String mIfaceName;

        SupplicantStaIfaceHalCallbackV1_1(String ifaceName, SupplicantStaIfaceHalCallback callback) {
            this.mIfaceName = ifaceName;
            this.mCallbackV1_0 = callback;
        }

        public void onNetworkAdded(int id) {
            this.mCallbackV1_0.onNetworkAdded(id);
        }

        public void onNetworkRemoved(int id) {
            this.mCallbackV1_0.onNetworkRemoved(id);
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            this.mCallbackV1_0.onStateChanged(newState, bssid, id, ssid);
        }

        public void onAnqpQueryDone(byte[] bssid, AnqpData data, Hs20AnqpData hs20Data) {
            this.mCallbackV1_0.onAnqpQueryDone(bssid, data, hs20Data);
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            this.mCallbackV1_0.onHs20IconQueryDone(bssid, fileName, data);
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            this.mCallbackV1_0.onHs20SubscriptionRemediation(bssid, osuMethod, url);
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            this.mCallbackV1_0.onHs20DeauthImminentNotice(bssid, reasonCode, reAuthDelayInSec, url);
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            this.mCallbackV1_0.onDisconnected(bssid, locallyGenerated, reasonCode);
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            this.mCallbackV1_0.onAssociationRejected(bssid, statusCode, timedOut);
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            this.mCallbackV1_0.onAuthenticationTimeout(bssid);
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            this.mCallbackV1_0.onBssidChanged(reason, bssid);
        }

        public void onEapFailure() {
            this.mCallbackV1_0.onEapFailure();
        }

        public void onEapFailure_1_1(int code) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure_1_1");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, code);
            }
        }

        public void onWpsEventSuccess() {
            this.mCallbackV1_0.onWpsEventSuccess();
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            this.mCallbackV1_0.onWpsEventFail(bssid, configError, errorInd);
        }

        public void onWpsEventPbcOverlap() {
            this.mCallbackV1_0.onWpsEventPbcOverlap();
        }

        public void onExtRadioWorkStart(int id) {
            this.mCallbackV1_0.onExtRadioWorkStart(id);
        }

        public void onExtRadioWorkTimeout(int id) {
            this.mCallbackV1_0.onExtRadioWorkTimeout(id);
        }
    }

    private class VendorSupplicantStaIfaceHalCallbackV2_0 extends vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIfaceCallback.Stub {
        private SupplicantStaIfaceHalCallbackV1_1 mCallbackV1_1;
        private String mIfaceName;

        VendorSupplicantStaIfaceHalCallbackV2_0(String ifaceName, SupplicantStaIfaceHalCallbackV1_1 callback) {
            this.mIfaceName = ifaceName;
            this.mCallbackV1_1 = callback;
        }

        public void onNetworkAdded(int id) {
            this.mCallbackV1_1.onNetworkAdded(id);
        }

        public void onNetworkRemoved(int id) {
            this.mCallbackV1_1.onNetworkRemoved(id);
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            this.mCallbackV1_1.onStateChanged(newState, bssid, id, ssid);
        }

        public void onAnqpQueryDone(byte[] bssid, AnqpData data, Hs20AnqpData hs20Data) {
            this.mCallbackV1_1.onAnqpQueryDone(bssid, data, hs20Data);
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            this.mCallbackV1_1.onHs20IconQueryDone(bssid, fileName, data);
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            this.mCallbackV1_1.onHs20SubscriptionRemediation(bssid, osuMethod, url);
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            this.mCallbackV1_1.onHs20DeauthImminentNotice(bssid, reasonCode, reAuthDelayInSec, url);
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            this.mCallbackV1_1.onDisconnected(bssid, locallyGenerated, reasonCode);
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            this.mCallbackV1_1.onAssociationRejected(bssid, statusCode, timedOut);
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            this.mCallbackV1_1.onAuthenticationTimeout(bssid);
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            this.mCallbackV1_1.onBssidChanged(reason, bssid);
        }

        public void onEapFailure() {
            this.mCallbackV1_1.onEapFailure();
        }

        public void onEapFailure_1_1(int code) {
            this.mCallbackV1_1.onEapFailure_1_1(code);
        }

        public void onWpsEventSuccess() {
            this.mCallbackV1_1.onWpsEventSuccess();
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            this.mCallbackV1_1.onWpsEventFail(bssid, configError, errorInd);
        }

        public void onWpsEventPbcOverlap() {
            this.mCallbackV1_1.onWpsEventPbcOverlap();
        }

        public void onExtRadioWorkStart(int id) {
            this.mCallbackV1_1.onExtRadioWorkStart(id);
        }

        public void onExtRadioWorkTimeout(int id) {
            this.mCallbackV1_1.onExtRadioWorkTimeout(id);
        }

        public void onWapiCertInitFail() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onWapiCertInitFail received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWapiCertInitFailEvent(this.mIfaceName);
        }

        public void onWapiAuthFail() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onWapiAuthFail received");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWapiAuthFailEvent(this.mIfaceName);
            }
        }

        public void onVoWifiIrqStr() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onVoWifiIrqStr received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastVoWifiIrqStrEvent(this.mIfaceName);
        }

        public void onHilinkStartWps(String arg) {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onHilinkStartWps received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastHilinkStartWpsEvent(this.mIfaceName, arg);
        }

        public void onHilinkStartWps() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onHilinkStartWps received");
        }

        public void onAbsAntCoreRob() {
            Log.d(SupplicantStaIfaceHal.TAG, "ISupplicantStaIfaceCallback. onAbsAntCoreRob received");
            SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAbsAntCoreRobEvent(this.mIfaceName);
        }
    }

    public static /* synthetic */ void lambda$new$0(SupplicantStaIfaceHal supplicantStaIfaceHal, long cookie) {
        synchronized (supplicantStaIfaceHal.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IServiceManager died: cookie=");
            stringBuilder.append(cookie);
            Log.w(str, stringBuilder.toString());
            supplicantStaIfaceHal.supplicantServiceDiedHandler();
            supplicantStaIfaceHal.mIServiceManager = null;
        }
    }

    public static /* synthetic */ void lambda$new$1(SupplicantStaIfaceHal supplicantStaIfaceHal, long cookie) {
        synchronized (supplicantStaIfaceHal.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ISupplicant died: cookie=");
            stringBuilder.append(cookie);
            Log.w(str, stringBuilder.toString());
            supplicantStaIfaceHal.supplicantServiceDiedHandler();
        }
    }

    public SupplicantStaIfaceHal(Context context, WifiMonitor monitor) {
        this.mContext = context;
        this.mWifiMonitor = monitor;
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
            this.mISupplicantStaIfaces.clear();
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while trying to register a listener for ISupplicant service: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    private int getCurrentNetworkId(String ifaceName) {
        synchronized (this.mLock) {
            WifiConfiguration currentConfig = getCurrentNetworkLocalConfig(ifaceName);
            if (currentConfig == null) {
                return -1;
            }
            int i = currentConfig.networkId;
            return i;
        }
    }

    public boolean setupIface(String ifaceName) {
        String methodStr = "setupIface";
        if (checkSupplicantStaIfaceAndLogFailure(ifaceName, "setupIface") != null) {
            return false;
        }
        ISupplicantIface ifaceHwBinder;
        if (isV1_1()) {
            ifaceHwBinder = addIfaceV1_1(ifaceName);
        } else {
            ifaceHwBinder = getIfaceV1_0(ifaceName);
        }
        if (ifaceHwBinder == null) {
            Log.e(TAG, "setupIface got null iface");
            return false;
        }
        SupplicantStaIfaceHalCallback callback = new SupplicantStaIfaceHalCallback(ifaceName);
        if (!isV1_1()) {
            ISupplicantStaIface iface = getStaIfaceMockable(ifaceHwBinder);
            if (!registerCallback(iface, callback)) {
                return false;
            }
            this.mISupplicantStaIfaces.put(ifaceName, iface);
            this.mISupplicantStaIfaceCallbacks.put(ifaceName, callback);
        } else if (trySetupForVendorV2_0(ifaceName, ifaceHwBinder, callback)) {
            return true;
        } else {
            android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iface2 = getStaIfaceMockableV1_1(ifaceHwBinder);
            SupplicantStaIfaceHalCallbackV1_1 callbackV1_1 = new SupplicantStaIfaceHalCallbackV1_1(ifaceName, callback);
            if (!registerCallbackV1_1(iface2, callbackV1_1)) {
                return false;
            }
            this.mISupplicantStaIfaces.put(ifaceName, iface2);
            this.mISupplicantStaIfaceCallbacks.put(ifaceName, callbackV1_1);
        }
        return true;
    }

    private ISupplicantIface getIfaceV1_0(String ifaceName) {
        synchronized (this.mLock) {
            ArrayList<IfaceInfo> supplicantIfaces = new ArrayList();
            try {
                this.mISupplicant.listInterfaces(new -$$Lambda$SupplicantStaIfaceHal$RSD0ugMIGhWHhmPxXCkALD2N5cU(supplicantIfaces));
                if (supplicantIfaces.size() == 0) {
                    Log.e(TAG, "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup.");
                    return null;
                }
                Mutable<ISupplicantIface> supplicantIface = new Mutable();
                Iterator it = supplicantIfaces.iterator();
                while (it.hasNext()) {
                    IfaceInfo ifaceInfo = (IfaceInfo) it.next();
                    if (ifaceInfo.type == 0 && ifaceName.equals(ifaceInfo.name)) {
                        try {
                            this.mISupplicant.getInterface(ifaceInfo, new -$$Lambda$SupplicantStaIfaceHal$RyQnT_v7B4l3vVijvOVBxHlvVoY(supplicantIface));
                            break;
                        } catch (RemoteException e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("ISupplicant.getInterface exception: ");
                            stringBuilder.append(e);
                            Log.e(str, stringBuilder.toString());
                            handleRemoteException(e, "getInterface");
                            return null;
                        }
                    }
                }
                ISupplicantIface iSupplicantIface = (ISupplicantIface) supplicantIface.value;
                return iSupplicantIface;
            } catch (RemoteException e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ISupplicant.listInterfaces exception: ");
                stringBuilder2.append(e2);
                Log.e(str2, stringBuilder2.toString());
                handleRemoteException(e2, "listInterfaces");
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$getIfaceV1_0$2(ArrayList supplicantIfaces, SupplicantStatus status, ArrayList ifaces) {
        if (status.code != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Getting Supplicant Interfaces failed: ");
            stringBuilder.append(status.code);
            Log.e(str, stringBuilder.toString());
            return;
        }
        supplicantIfaces.addAll(ifaces);
    }

    static /* synthetic */ void lambda$getIfaceV1_0$3(Mutable supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get ISupplicantIface ");
            stringBuilder.append(status.code);
            Log.e(str, stringBuilder.toString());
            return;
        }
        supplicantIface.value = iface;
    }

    private ISupplicantIface addIfaceV1_1(String ifaceName) {
        ISupplicantIface iSupplicantIface;
        synchronized (this.mLock) {
            IfaceInfo ifaceInfo = new IfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.type = 0;
            Mutable<ISupplicantIface> supplicantIface = new Mutable();
            try {
                getSupplicantMockableV1_1().addInterface(ifaceInfo, new -$$Lambda$SupplicantStaIfaceHal$jt86rUfXpbjU1MKB5KeL4Iv2b0k(supplicantIface));
                iSupplicantIface = (ISupplicantIface) supplicantIface.value;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.addInterface exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                handleRemoteException(e, "addInterface");
                return null;
            }
        }
        return iSupplicantIface;
    }

    static /* synthetic */ void lambda$addIfaceV1_1$4(Mutable supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code == 0 || status.code == 5) {
            supplicantIface.value = iface;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to create ISupplicantIface ");
        stringBuilder.append(status.code);
        Log.e(str, stringBuilder.toString());
    }

    public boolean teardownIface(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "teardownIface";
            if (checkSupplicantStaIfaceAndLogFailure(ifaceName, "teardownIface") == null) {
                return false;
            } else if (isV1_1() && !removeIfaceV1_1(ifaceName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove iface = ");
                stringBuilder.append(ifaceName);
                Log.e(str, stringBuilder.toString());
                return false;
            } else if (this.mISupplicantStaIfaces.remove(ifaceName) == null) {
                Log.e(TAG, "Trying to teardown unknown inteface");
                return false;
            } else {
                this.mISupplicantStaIfaceCallbacks.remove(ifaceName);
                return true;
            }
        }
    }

    private boolean removeIfaceV1_1(String ifaceName) {
        synchronized (this.mLock) {
            try {
                IfaceInfo ifaceInfo = new IfaceInfo();
                ifaceInfo.name = ifaceName;
                ifaceInfo.type = 0;
                SupplicantStatus status = getSupplicantMockableV1_1().removeInterface(ifaceInfo);
                if (status.code != 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to remove iface ");
                    stringBuilder.append(status.code);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ISupplicant.removeInterface exception: ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
                handleRemoteException(e, "removeInterface");
                return false;
            }
        }
    }

    public boolean registerDeathHandler(SupplicantDeathEventHandler handler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = handler;
        return true;
    }

    public boolean deregisterDeathHandler() {
        if (this.mDeathEventHandler == null) {
            Log.e(TAG, "No Death handler present");
        }
        this.mDeathEventHandler = null;
        return true;
    }

    private void clearState() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantStaIfaces.clear();
            this.mCurrentNetworkLocalConfigs.clear();
            this.mCurrentNetworkRemoteHandles.clear();
        }
    }

    private void supplicantServiceDiedHandler() {
        synchronized (this.mLock) {
            for (String ifaceName : this.mISupplicantStaIfaces.keySet()) {
                this.mWifiMonitor.broadcastSupplicantDisconnectionEvent(ifaceName);
            }
            clearState();
            if (this.mDeathEventHandler != null) {
                this.mDeathEventHandler.onDeath();
            }
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
            z = this.mISupplicant != null;
        }
        return z;
    }

    public void terminate() {
        synchronized (this.mLock) {
            String methodStr = "terminate";
            if (checkSupplicantAndLogFailure("terminate")) {
                try {
                    if (isV1_1()) {
                        getSupplicantMockableV1_1().terminate();
                    }
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                }
            } else {
                return;
            }
        }
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
            try {
                service = ISupplicant.getService();
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get ISupplicant", e);
                return null;
            }
        }
        return service;
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicant getSupplicantMockableV1_1() throws RemoteException {
        android.hardware.wifi.supplicant.V1_1.ISupplicant castFrom;
        synchronized (this.mLock) {
            try {
                castFrom = android.hardware.wifi.supplicant.V1_1.ISupplicant.castFrom(ISupplicant.getService());
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get ISupplicant", e);
                return null;
            }
        }
        return castFrom;
    }

    protected ISupplicantStaIface getStaIfaceMockable(ISupplicantIface iface) {
        ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface getStaIfaceMockableV1_1(ISupplicantIface iface) {
        android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    private boolean isV1_1() {
        boolean z;
        synchronized (this.mLock) {
            z = false;
            try {
                if (getSupplicantMockableV1_1() != null) {
                    z = true;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                handleRemoteException(e, "getSupplicantMockable");
                return false;
            }
        }
        return z;
    }

    private ISupplicantStaIface getStaIface(String ifaceName) {
        return (ISupplicantStaIface) this.mISupplicantStaIfaces.get(ifaceName);
    }

    private SupplicantStaNetworkHal getCurrentNetworkRemoteHandle(String ifaceName) {
        return (SupplicantStaNetworkHal) this.mCurrentNetworkRemoteHandles.get(ifaceName);
    }

    private WifiConfiguration getCurrentNetworkLocalConfig(String ifaceName) {
        return (WifiConfiguration) this.mCurrentNetworkLocalConfigs.get(ifaceName);
    }

    /* JADX WARNING: Missing block: B:25:0x0063, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Pair<SupplicantStaNetworkHal, WifiConfiguration> addNetworkAndSaveConfig(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            logi("addSupplicantStaNetwork via HIDL");
            if (config == null) {
                loge("Cannot add NULL network!");
                return null;
            }
            SupplicantStaNetworkHal network = addNetwork(ifaceName);
            if (network == null) {
                loge("Failed to add a network!");
                return null;
            }
            boolean saveSuccess = false;
            try {
                saveSuccess = network.saveWifiConfiguration(config);
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while saving config params: ");
                stringBuilder.append(config);
                Log.e(str, stringBuilder.toString(), e);
            }
            if (saveSuccess) {
                Pair<SupplicantStaNetworkHal, WifiConfiguration> pair = new Pair(network, new WifiConfiguration(config));
                return pair;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to save variables for: ");
            stringBuilder2.append(config.configKey());
            loge(stringBuilder2.toString());
            if (!removeAllNetworks(ifaceName)) {
                loge("Failed to remove all networks on failure.");
            }
        }
    }

    public boolean connectToNetwork(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("connectToNetwork ");
            stringBuilder2.append(config.configKey());
            logd(stringBuilder2.toString());
            WifiConfiguration currentConfig = getCurrentNetworkLocalConfig(ifaceName);
            if (!WifiConfigurationUtil.isSameNetwork(config, currentConfig)) {
                this.mCurrentNetworkRemoteHandles.remove(ifaceName);
                this.mCurrentNetworkLocalConfigs.remove(ifaceName);
                if (removeAllNetworks(ifaceName)) {
                    Pair<SupplicantStaNetworkHal, WifiConfiguration> pair = addNetworkAndSaveConfig(ifaceName, config);
                    if (pair == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to add/save network configuration: ");
                        stringBuilder.append(config.configKey());
                        loge(stringBuilder.toString());
                        return false;
                    }
                    this.mCurrentNetworkRemoteHandles.put(ifaceName, (SupplicantStaNetworkHal) pair.first);
                    this.mCurrentNetworkLocalConfigs.put(ifaceName, (WifiConfiguration) pair.second);
                } else {
                    loge("Failed to remove existing networks");
                    return false;
                }
            } else if (Objects.equals(config.getNetworkSelectionStatus().getNetworkSelectionBSSID(), currentConfig.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                logd("Network is already saved, will not trigger remove and add operation.");
            } else {
                logd("Network is already saved, but need to update BSSID.");
                if (setCurrentNetworkBssid(ifaceName, config.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                    this.mCurrentNetworkLocalConfigs.put(ifaceName, new WifiConfiguration(config));
                } else {
                    loge("Failed to set current network BSSID.");
                    return false;
                }
            }
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "connectToNetwork");
            if (networkHandle == null || !networkHandle.select()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to select network configuration: ");
                stringBuilder.append(config.configKey());
                loge(stringBuilder.toString());
                return false;
            }
            return true;
        }
    }

    public boolean roamToNetwork(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (getCurrentNetworkId(ifaceName) != config.networkId) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot roam to a different network, initiate new connection. Current network ID: ");
                stringBuilder.append(getCurrentNetworkId(ifaceName));
                Log.w(str, stringBuilder.toString());
                boolean connectToNetwork = connectToNetwork(ifaceName, config);
                return connectToNetwork;
            }
            str = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            stringBuilder = new StringBuilder();
            stringBuilder.append("roamToNetwork");
            stringBuilder.append(config.configKey());
            stringBuilder.append(" (bssid ");
            stringBuilder.append(str);
            stringBuilder.append(")");
            logd(stringBuilder.toString());
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "roamToNetwork");
            if (networkHandle == null || !networkHandle.setBssid(str)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to set new bssid on network: ");
                stringBuilder2.append(config.configKey());
                loge(stringBuilder2.toString());
                return false;
            } else if (reassociate(ifaceName)) {
                return true;
            } else {
                loge("Failed to trigger reassociate");
                return false;
            }
        }
    }

    public boolean loadNetworks(String ifaceName, Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) {
        Throwable th;
        String str = ifaceName;
        SparseArray<Map<String, String>> sparseArray = networkExtras;
        synchronized (this.mLock) {
            WifiConfiguration config;
            String str2;
            StringBuilder stringBuilder;
            Map<String, WifiConfiguration> map;
            try {
                ArrayList<Integer> networkIds = listNetworks(ifaceName);
                boolean z = false;
                if (networkIds == null) {
                    Log.e(TAG, "Failed to list networks");
                    return false;
                }
                for (Integer networkId : networkIds) {
                    SupplicantStaNetworkHal network = getNetwork(str, networkId.intValue());
                    String str3;
                    if (network == null) {
                        str3 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to get network with ID: ");
                        stringBuilder2.append(networkId);
                        Log.e(str3, stringBuilder2.toString());
                        return z;
                    }
                    config = new WifiConfiguration();
                    HashMap networkExtra = new HashMap();
                    boolean loadSuccess = z;
                    loadSuccess = network.loadWifiConfiguration(config, networkExtra);
                    int appId = UserHandle.getAppId(config.creatorUid);
                    if (config.BSSID != null && (appId == 0 || appId == 1000 || appId == 1010)) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("loadNetworks creater: ");
                        stringBuilder.append(config.creatorUid);
                        stringBuilder.append(", ssid: ");
                        stringBuilder.append(config.SSID);
                        stringBuilder.append(", Bssid:");
                        stringBuilder.append(ScanResultUtil.getConfusedBssid(config.BSSID));
                        Log.w(str2, stringBuilder.toString());
                        config.BSSID = null;
                    }
                    if (loadSuccess) {
                        config.setIpAssignment(IpAssignment.DHCP);
                        config.setProxySettings(ProxySettings.NONE);
                        sparseArray.put(networkId.intValue(), networkExtra);
                        str3 = (String) networkExtra.get("configKey");
                        try {
                            WifiConfiguration duplicateConfig = (WifiConfiguration) configs.put(str3, config);
                            if (duplicateConfig != null) {
                                String str4 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Replacing duplicate network: ");
                                stringBuilder3.append(duplicateConfig.networkId);
                                Log.i(str4, stringBuilder3.toString());
                                removeNetwork(str, duplicateConfig.networkId);
                                sparseArray.remove(duplicateConfig.networkId);
                            }
                            z = false;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    str3 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Failed to load wifi configuration for network with ID: ");
                    stringBuilder4.append(networkId);
                    stringBuilder4.append(". Skipping...");
                    Log.e(str3, stringBuilder4.toString());
                }
                map = configs;
                return true;
            } catch (IllegalArgumentException e) {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while loading config params: ");
                stringBuilder.append(config);
                Log.wtf(str2, stringBuilder.toString(), e);
            } catch (Throwable th3) {
                th = th3;
                map = configs;
                throw th;
            }
        }
    }

    public void removeNetworkIfCurrent(String ifaceName, int networkId) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId(ifaceName) == networkId) {
                removeAllNetworks(ifaceName);
            }
        }
    }

    public boolean removeAllNetworks(String ifaceName) {
        synchronized (this.mLock) {
            ArrayList<Integer> networks = listNetworks(ifaceName);
            if (networks == null) {
                Log.e(TAG, "removeAllNetworks failed, got null networks");
                return false;
            }
            Iterator it = networks.iterator();
            while (it.hasNext()) {
                int id = ((Integer) it.next()).intValue();
                if (!removeNetwork(ifaceName, id)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeAllNetworks failed to remove network: ");
                    stringBuilder.append(id);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
            }
            this.mCurrentNetworkRemoteHandles.remove(ifaceName);
            this.mCurrentNetworkLocalConfigs.remove(ifaceName);
            return true;
        }
    }

    public boolean setCurrentNetworkBssid(String ifaceName, String bssidStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "setCurrentNetworkBssid");
            if (networkHandle == null) {
                return false;
            }
            boolean bssid = networkHandle.setBssid(bssidStr);
            return bssid;
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "getCurrentNetworkWpsNfcConfigurationToken");
            if (networkHandle == null) {
                return null;
            }
            String wpsNfcConfigurationToken = networkHandle.getWpsNfcConfigurationToken();
            return wpsNfcConfigurationToken;
        }
    }

    public String getCurrentNetworkEapAnonymousIdentity(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "getCurrentNetworkEapAnonymousIdentity");
            if (networkHandle == null) {
                return null;
            }
            String fetchEapAnonymousIdentity = networkHandle.fetchEapAnonymousIdentity();
            return fetchEapAnonymousIdentity;
        }
    }

    public boolean sendCurrentNetworkEapIdentityResponse(String ifaceName, String identity, String encryptedIdentity) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapIdentityResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapIdentityResponse = networkHandle.sendNetworkEapIdentityResponse(identity, encryptedIdentity);
            return sendNetworkEapIdentityResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimGsmAuthResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthResponse = networkHandle.sendNetworkEapSimGsmAuthResponse(paramsStr);
            return sendNetworkEapSimGsmAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthFailure(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimGsmAuthFailure");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthFailure = networkHandle.sendNetworkEapSimGsmAuthFailure();
            return sendNetworkEapSimGsmAuthFailure;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAuthResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthResponse = networkHandle.sendNetworkEapSimUmtsAuthResponse(paramsStr);
            return sendNetworkEapSimUmtsAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAutsResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAutsResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAutsResponse = networkHandle.sendNetworkEapSimUmtsAutsResponse(paramsStr);
            return sendNetworkEapSimUmtsAutsResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthFailure(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAuthFailure");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthFailure = networkHandle.sendNetworkEapSimUmtsAuthFailure();
            return sendNetworkEapSimUmtsAuthFailure;
        }
    }

    private SupplicantStaNetworkHal addNetwork(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "addNetwork";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "addNetwork");
            if (iface == null) {
                return null;
            }
            Mutable<ISupplicantNetwork> newNetwork = new Mutable();
            try {
                iface.addNetwork(new -$$Lambda$SupplicantStaIfaceHal$Tm7D8fgqduAQdeOyODnUSwEesVo(this, newNetwork));
            } catch (RemoteException e) {
                handleRemoteException(e, "addNetwork");
            }
            if (newNetwork.value != null) {
                SupplicantStaNetworkHal staNetworkMockable = getStaNetworkMockable(ifaceName, ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) newNetwork.value).asBinder()));
                return staNetworkMockable;
            }
            return null;
        }
    }

    public static /* synthetic */ void lambda$addNetwork$5(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable newNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "addNetwork")) {
            newNetwork.value = network;
        }
    }

    private boolean removeNetwork(String ifaceName, int id) {
        synchronized (this.mLock) {
            String methodStr = "removeNetwork";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "removeNetwork");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.removeNetwork(id), "removeNetwork");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "removeNetwork");
                return false;
            }
        }
    }

    protected SupplicantStaNetworkHal getStaNetworkMockable(String ifaceName, ISupplicantStaNetwork iSupplicantStaNetwork) {
        SupplicantStaNetworkHal network;
        synchronized (this.mLock) {
            network = new SupplicantStaNetworkHal(iSupplicantStaNetwork, ifaceName, this.mContext, this.mWifiMonitor);
            network.enableVerboseLogging(this.mVerboseLoggingEnabled);
        }
        return network;
    }

    private SupplicantStaNetworkHal getNetwork(String ifaceName, int id) {
        synchronized (this.mLock) {
            String methodStr = "getNetwork";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "getNetwork");
            if (iface == null) {
                return null;
            }
            Mutable<ISupplicantNetwork> gotNetwork = new Mutable();
            try {
                iface.getNetwork(id, new -$$Lambda$SupplicantStaIfaceHal$iSySXJBDDsRLtEbI_9u6zKZ-gXU(this, gotNetwork));
            } catch (RemoteException e) {
                handleRemoteException(e, "getNetwork");
            }
            if (gotNetwork.value != null) {
                SupplicantStaNetworkHal staNetworkMockable = getStaNetworkMockable(ifaceName, ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) gotNetwork.value).asBinder()));
                return staNetworkMockable;
            }
            return null;
        }
    }

    public static /* synthetic */ void lambda$getNetwork$6(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getNetwork")) {
            gotNetwork.value = network;
        }
    }

    private boolean registerCallback(ISupplicantStaIface iface, ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "registerCallback";
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.registerCallback(callback), "registerCallback");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean registerCallbackV1_1(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "registerCallback_1_1";
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.registerCallback_1_1(callback), methodStr);
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, methodStr);
                return false;
            }
        }
    }

    private ArrayList<Integer> listNetworks(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "listNetworks";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "listNetworks");
            if (iface == null) {
                return null;
            }
            Mutable<ArrayList<Integer>> networkIdList = new Mutable();
            try {
                iface.listNetworks(new -$$Lambda$SupplicantStaIfaceHal$oY40I1ZV1zNoEKNITjSxjIr7WaE(this, networkIdList));
            } catch (RemoteException e) {
                handleRemoteException(e, "listNetworks");
            }
            ArrayList<Integer> arrayList = (ArrayList) networkIdList.value;
            return arrayList;
        }
    }

    public static /* synthetic */ void lambda$listNetworks$7(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable networkIdList, SupplicantStatus status, ArrayList networkIds) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "listNetworks")) {
            networkIdList.value = networkIds;
        }
    }

    public boolean setWpsDeviceName(String ifaceName, String name) {
        synchronized (this.mLock) {
            String methodStr = "setWpsDeviceName";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsDeviceName");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsDeviceName(name), "setWpsDeviceName");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceName");
                return false;
            }
        }
    }

    public boolean setWpsDeviceType(String ifaceName, String typeStr) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
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
                    boolean wpsDeviceType = setWpsDeviceType(ifaceName, bytes);
                    return wpsDeviceType;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Malformed WPS device type ");
                stringBuilder.append(typeStr);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(typeStr);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
    }

    private boolean setWpsDeviceType(String ifaceName, byte[] type) {
        synchronized (this.mLock) {
            String methodStr = "setWpsDeviceType";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsDeviceType");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsDeviceType(type), "setWpsDeviceType");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceType");
                return false;
            }
        }
    }

    public boolean setWpsManufacturer(String ifaceName, String manufacturer) {
        synchronized (this.mLock) {
            String methodStr = "setWpsManufacturer";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsManufacturer");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsManufacturer(manufacturer), "setWpsManufacturer");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsManufacturer");
                return false;
            }
        }
    }

    public boolean setWpsModelName(String ifaceName, String modelName) {
        synchronized (this.mLock) {
            String methodStr = "setWpsModelName";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsModelName");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsModelName(modelName), "setWpsModelName");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelName");
                return false;
            }
        }
    }

    public boolean setWpsModelNumber(String ifaceName, String modelNumber) {
        synchronized (this.mLock) {
            String methodStr = "setWpsModelNumber";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsModelNumber");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsModelNumber(modelNumber), "setWpsModelNumber");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelNumber");
                return false;
            }
        }
    }

    public boolean setWpsSerialNumber(String ifaceName, String serialNumber) {
        synchronized (this.mLock) {
            String methodStr = "setWpsSerialNumber";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsSerialNumber");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsSerialNumber(serialNumber), "setWpsSerialNumber");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsSerialNumber");
                return false;
            }
        }
    }

    public boolean setWpsConfigMethods(String ifaceName, String configMethodsStr) {
        short configMethodsMask;
        synchronized (this.mLock) {
            configMethodsMask = (short) 0;
            for (String stringToWpsConfigMethod : configMethodsStr.split("\\s+")) {
                configMethodsMask = (short) (stringToWpsConfigMethod(stringToWpsConfigMethod) | configMethodsMask);
            }
        }
        return setWpsConfigMethods(ifaceName, configMethodsMask);
    }

    private boolean setWpsConfigMethods(String ifaceName, short configMethods) {
        synchronized (this.mLock) {
            String methodStr = "setWpsConfigMethods";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsConfigMethods");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsConfigMethods(configMethods), "setWpsConfigMethods");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsConfigMethods");
                return false;
            }
        }
    }

    public boolean reassociate(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "reassociate";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "reassociate");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.reassociate(), "reassociate");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "reassociate");
                return false;
            }
        }
    }

    public boolean reconnect(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "reconnect";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "reconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.reconnect(), "reconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "reconnect");
                return false;
            }
        }
    }

    public boolean disconnect(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "disconnect";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "disconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.disconnect(), "disconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "disconnect");
                return false;
            }
        }
    }

    public boolean setPowerSave(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setPowerSave";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setPowerSave");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setPowerSave(enable), "setPowerSave");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setPowerSave");
                return false;
            }
        }
    }

    public boolean initiateTdlsDiscover(String ifaceName, String macAddress) {
        boolean initiateTdlsDiscover;
        synchronized (this.mLock) {
            try {
                initiateTdlsDiscover = initiateTdlsDiscover(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(macAddress);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return initiateTdlsDiscover;
    }

    private boolean initiateTdlsDiscover(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsDiscover";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsDiscover");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsDiscover(macAddress), "initiateTdlsDiscover");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsDiscover");
                return false;
            }
        }
    }

    public boolean initiateTdlsSetup(String ifaceName, String macAddress) {
        boolean initiateTdlsSetup;
        synchronized (this.mLock) {
            try {
                initiateTdlsSetup = initiateTdlsSetup(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(macAddress);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return initiateTdlsSetup;
    }

    private boolean initiateTdlsSetup(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsSetup";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsSetup");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsSetup(macAddress), "initiateTdlsSetup");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsSetup");
                return false;
            }
        }
    }

    public boolean initiateTdlsTeardown(String ifaceName, String macAddress) {
        boolean initiateTdlsTeardown;
        synchronized (this.mLock) {
            try {
                initiateTdlsTeardown = initiateTdlsTeardown(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(macAddress);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return initiateTdlsTeardown;
    }

    private boolean initiateTdlsTeardown(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            String methodStr = "initiateTdlsTeardown";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsTeardown");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsTeardown(macAddress), "initiateTdlsTeardown");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsTeardown");
                return false;
            }
        }
    }

    public boolean initiateAnqpQuery(String ifaceName, String bssid, ArrayList<Short> infoElements, ArrayList<Integer> hs20SubTypes) {
        boolean initiateAnqpQuery;
        synchronized (this.mLock) {
            try {
                initiateAnqpQuery = initiateAnqpQuery(ifaceName, NativeUtil.macAddressToByteArray(bssid), (ArrayList) infoElements, (ArrayList) hs20SubTypes);
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(bssid);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return initiateAnqpQuery;
    }

    private boolean initiateAnqpQuery(String ifaceName, byte[] macAddress, ArrayList<Short> infoElements, ArrayList<Integer> subTypes) {
        synchronized (this.mLock) {
            String methodStr = "initiateAnqpQuery";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateAnqpQuery");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateAnqpQuery(macAddress, infoElements, subTypes), "initiateAnqpQuery");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateAnqpQuery");
                return false;
            }
        }
    }

    public boolean initiateHs20IconQuery(String ifaceName, String bssid, String fileName) {
        boolean initiateHs20IconQuery;
        synchronized (this.mLock) {
            try {
                initiateHs20IconQuery = initiateHs20IconQuery(ifaceName, NativeUtil.macAddressToByteArray(bssid), fileName);
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(bssid);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return initiateHs20IconQuery;
    }

    private boolean initiateHs20IconQuery(String ifaceName, byte[] macAddress, String fileName) {
        synchronized (this.mLock) {
            String methodStr = "initiateHs20IconQuery";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateHs20IconQuery");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateHs20IconQuery(macAddress, fileName), "initiateHs20IconQuery");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateHs20IconQuery");
                return false;
            }
        }
    }

    public String getMacAddress(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "getMacAddress";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "getMacAddress");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotMac = new Mutable();
            try {
                iface.getMacAddress(new -$$Lambda$SupplicantStaIfaceHal$RN5yy1Bc5d6E1Z6k9lqZIMdLATc(this, gotMac));
            } catch (RemoteException e) {
                handleRemoteException(e, "getMacAddress");
            }
            String str = (String) gotMac.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$getMacAddress$8(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotMac, SupplicantStatus status, byte[] macAddr) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getMacAddress")) {
            gotMac.value = NativeUtil.macAddressFromByteArray(macAddr);
        }
    }

    public boolean startRxFilter(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "startRxFilter";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startRxFilter(), "startRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startRxFilter");
                return false;
            }
        }
    }

    public boolean stopRxFilter(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "stopRxFilter";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "stopRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.stopRxFilter(), "stopRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "stopRxFilter");
                return false;
            }
        }
    }

    public boolean addRxFilter(String ifaceName, int type) {
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid Rx Filter type: ");
                    stringBuilder.append(type);
                    Log.e(str, stringBuilder.toString());
                    return false;
            }
            boolean addRxFilter = addRxFilter(ifaceName, halType);
            return addRxFilter;
        }
    }

    private boolean addRxFilter(String ifaceName, byte type) {
        synchronized (this.mLock) {
            String methodStr = "addRxFilter";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "addRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.addRxFilter(type), "addRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "addRxFilter");
                return false;
            }
        }
    }

    public boolean removeRxFilter(String ifaceName, int type) {
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid Rx Filter type: ");
                    stringBuilder.append(type);
                    Log.e(str, stringBuilder.toString());
                    return false;
            }
            boolean removeRxFilter = removeRxFilter(ifaceName, halType);
            return removeRxFilter;
        }
    }

    private boolean removeRxFilter(String ifaceName, byte type) {
        synchronized (this.mLock) {
            String methodStr = "removeRxFilter";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "removeRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.removeRxFilter(type), "removeRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "removeRxFilter");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceMode(String ifaceName, int mode) {
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid Bt Coex mode: ");
                    stringBuilder.append(mode);
                    Log.e(str, stringBuilder.toString());
                    return false;
            }
            boolean btCoexistenceMode = setBtCoexistenceMode(ifaceName, halMode);
            return btCoexistenceMode;
        }
    }

    private boolean setBtCoexistenceMode(String ifaceName, byte mode) {
        synchronized (this.mLock) {
            String methodStr = "setBtCoexistenceMode";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setBtCoexistenceMode");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setBtCoexistenceMode(mode), "setBtCoexistenceMode");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceMode");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceScanModeEnabled(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setBtCoexistenceScanModeEnabled";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setBtCoexistenceScanModeEnabled");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setBtCoexistenceScanModeEnabled(enable), "setBtCoexistenceScanModeEnabled");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceScanModeEnabled");
                return false;
            }
        }
    }

    public boolean setSuspendModeEnabled(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setSuspendModeEnabled";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setSuspendModeEnabled");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setSuspendModeEnabled(enable), "setSuspendModeEnabled");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setSuspendModeEnabled");
                return false;
            }
        }
    }

    public boolean setCountryCode(String ifaceName, String codeStr) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(codeStr)) {
                return false;
            }
            boolean countryCode = setCountryCode(ifaceName, NativeUtil.stringToByteArray(codeStr));
            return countryCode;
        }
    }

    private boolean setCountryCode(String ifaceName, byte[] code) {
        synchronized (this.mLock) {
            String methodStr = "setCountryCode";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setCountryCode");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setCountryCode(code), "setCountryCode");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setCountryCode");
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0036, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startWpsRegistrar(String ifaceName, String bssidStr, String pin) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(bssidStr) || TextUtils.isEmpty(pin)) {
            } else {
                try {
                    boolean startWpsRegistrar = startWpsRegistrar(ifaceName, NativeUtil.macAddressToByteArray(bssidStr), pin);
                    return startWpsRegistrar;
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal argument ");
                    stringBuilder.append(bssidStr);
                    Log.e(str, stringBuilder.toString(), e);
                    return false;
                }
            }
        }
    }

    private boolean startWpsRegistrar(String ifaceName, byte[] bssid, String pin) {
        synchronized (this.mLock) {
            String methodStr = "startWpsRegistrar";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsRegistrar");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsRegistrar(bssid, pin), "startWpsRegistrar");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsRegistrar");
                return false;
            }
        }
    }

    public boolean startWpsPbc(String ifaceName, String bssidStr) {
        boolean startWpsPbc;
        synchronized (this.mLock) {
            try {
                startWpsPbc = startWpsPbc(ifaceName, NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(bssidStr);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return startWpsPbc;
    }

    private boolean startWpsPbc(String ifaceName, byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "startWpsPbc";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPbc");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsPbc(bssid), "startWpsPbc");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPbc");
                return false;
            }
        }
    }

    public boolean startWpsPinKeypad(String ifaceName, String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (this.mLock) {
            String methodStr = "startWpsPinKeypad";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPinKeypad");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsPinKeypad(pin), "startWpsPinKeypad");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinKeypad");
                return false;
            }
        }
    }

    public String startWpsPinDisplay(String ifaceName, String bssidStr) {
        String startWpsPinDisplay;
        synchronized (this.mLock) {
            try {
                startWpsPinDisplay = startWpsPinDisplay(ifaceName, NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(bssidStr);
                Log.e(str, stringBuilder.toString(), e);
                return null;
            }
        }
        return startWpsPinDisplay;
    }

    private String startWpsPinDisplay(String ifaceName, byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "startWpsPinDisplay";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPinDisplay");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotPin = new Mutable();
            try {
                iface.startWpsPinDisplay(bssid, new -$$Lambda$SupplicantStaIfaceHal$obLcU23CRIRzqBy01VuBJv-lyUg(this, gotPin));
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinDisplay");
            }
            String str = (String) gotPin.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$startWpsPinDisplay$9(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotPin, SupplicantStatus status, String pin) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "startWpsPinDisplay")) {
            gotPin.value = pin;
        }
    }

    public boolean cancelWps(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "cancelWps";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "cancelWps");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.cancelWps(), "cancelWps");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "cancelWps");
                return false;
            }
        }
    }

    public boolean setExternalSim(String ifaceName, boolean useExternalSim) {
        synchronized (this.mLock) {
            String methodStr = "setExternalSim";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setExternalSim");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setExternalSim(useExternalSim), "setExternalSim");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setExternalSim");
                return false;
            }
        }
    }

    public boolean enableAutoReconnect(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "enableAutoReconnect";
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "enableAutoReconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.enableAutoReconnect(enable), "enableAutoReconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "enableAutoReconnect");
                return false;
            }
        }
    }

    public boolean setLogLevel(boolean turnOnVerbose) {
        boolean debugParams;
        synchronized (this.mLock) {
            Log.i(TAG, "Force Supplicant log to debug level");
            debugParams = setDebugParams(2, false, false);
        }
        return debugParams;
    }

    private boolean setDebugParams(int level, boolean showTimestamp, boolean showKeys) {
        synchronized (this.mLock) {
            String methodStr = "setDebugParams";
            if (checkSupplicantAndLogFailure("setDebugParams")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicant.setDebugParams(level, showTimestamp, showKeys), "setDebugParams");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setDebugParams");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setConcurrencyPriority(boolean isStaHigherPriority) {
        synchronized (this.mLock) {
            boolean concurrencyPriority;
            if (isStaHigherPriority) {
                concurrencyPriority = setConcurrencyPriority(0);
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call ");
                stringBuilder.append(methodStr);
                stringBuilder.append(", ISupplicant is null");
                Log.e(str, stringBuilder.toString());
                return false;
            }
            return true;
        }
    }

    private ISupplicantStaIface checkSupplicantStaIfaceAndLogFailure(String ifaceName, String methodStr) {
        synchronized (this.mLock) {
            this.mHalMethod = methodStr;
            this.mHalCallStartTime = SystemClock.uptimeMillis();
            ISupplicantStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call ");
                stringBuilder.append(methodStr);
                stringBuilder.append(", ISupplicantStaIface is null");
                Log.e(str, stringBuilder.toString());
                return null;
            }
            return iface;
        }
    }

    private SupplicantStaNetworkHal checkSupplicantStaNetworkAndLogFailure(String ifaceName, String methodStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHal = getCurrentNetworkRemoteHandle(ifaceName);
            if (networkHal == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call ");
                stringBuilder.append(methodStr);
                stringBuilder.append(", SupplicantStaNetwork is null");
                Log.e(str, stringBuilder.toString());
                return null;
            }
            return networkHal;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        checkHalCallThresholdMs(status, methodStr);
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (status.code != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaIface.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" failed: ");
                stringBuilder.append(status);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaIface.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" succeeded");
                Log.d(str, stringBuilder.toString());
            }
            return true;
        }
    }

    private void checkHalCallThresholdMs(SupplicantStatus status, String methodStr) {
        long mHalCallEndTime = SystemClock.uptimeMillis();
        String str;
        StringBuilder stringBuilder;
        if (!this.mHalMethod.equals(methodStr)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error, mHalCallStartTime in:");
            stringBuilder.append(this.mHalMethod);
            stringBuilder.append(", mHalCallEndTime in:");
            stringBuilder.append(methodStr);
            Log.w(str, stringBuilder.toString());
        } else if (mHalCallEndTime - this.mHalCallStartTime > 300) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Hal call took ");
            stringBuilder.append(mHalCallEndTime - this.mHalCallStartTime);
            stringBuilder.append("ms on ");
            stringBuilder.append(methodStr);
            stringBuilder.append(", status.code:");
            stringBuilder.append(SupplicantStatusCode.toString(status.code));
            Log.w(str, stringBuilder.toString(), new Exception());
        }
    }

    private void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaIfaceCallback.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" received");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            clearState();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ISupplicantStaIface.");
            stringBuilder.append(methodStr);
            stringBuilder.append(" failed with exception");
            Log.e(str, stringBuilder.toString(), e);
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static short stringToWpsConfigMethod(String configMethod) {
        short s;
        switch (configMethod.hashCode()) {
            case -1781962557:
                if (configMethod.equals("virtual_push_button")) {
                    s = (short) 9;
                    break;
                }
            case -1419358249:
                if (configMethod.equals("ethernet")) {
                    s = (short) 1;
                    break;
                }
            case -1134657068:
                if (configMethod.equals("keypad")) {
                    s = (short) 8;
                    break;
                }
            case -614489202:
                if (configMethod.equals("virtual_display")) {
                    s = (short) 12;
                    break;
                }
            case -522593958:
                if (configMethod.equals("physical_display")) {
                    s = (short) 13;
                    break;
                }
            case -423872603:
                if (configMethod.equals("nfc_interface")) {
                    s = (short) 6;
                    break;
                }
            case -416734217:
                if (configMethod.equals("push_button")) {
                    s = (short) 7;
                    break;
                }
            case 3388229:
                if (configMethod.equals("p2ps")) {
                    s = (short) 11;
                    break;
                }
            case 3599197:
                if (configMethod.equals("usba")) {
                    s = (short) 0;
                    break;
                }
            case 102727412:
                if (configMethod.equals("label")) {
                    s = (short) 2;
                    break;
                }
            case 179612103:
                if (configMethod.equals("ext_nfc_token")) {
                    s = (short) 5;
                    break;
                }
            case 1146869903:
                if (configMethod.equals("physical_push_button")) {
                    s = (short) 10;
                    break;
                }
            case 1671764162:
                if (configMethod.equals("display")) {
                    s = (short) 3;
                    break;
                }
            case 2010140181:
                if (configMethod.equals("int_nfc_token")) {
                    s = (short) 4;
                    break;
                }
            default:
                s = (short) -1;
                break;
        }
        switch (s) {
            case (short) 0:
                return (short) 1;
            case (short) 1:
                return (short) 2;
            case (short) 2:
                return (short) 4;
            case (short) 3:
                return (short) 8;
            case (short) 4:
                return (short) 32;
            case (short) 5:
                return (short) 16;
            case (short) 6:
                return (short) 64;
            case (short) 7:
                return WpsConfigMethods.PUSHBUTTON;
            case (short) 8:
                return WpsConfigMethods.KEYPAD;
            case (short) 9:
                return WpsConfigMethods.VIRT_PUSHBUTTON;
            case (short) 10:
                return WpsConfigMethods.PHY_PUSHBUTTON;
            case (short) 11:
                return WpsConfigMethods.P2PS;
            case (short) 12:
                return WpsConfigMethods.VIRT_DISPLAY;
            case (short) 13:
                return WpsConfigMethods.PHY_DISPLAY;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid WPS config method: ");
                stringBuilder.append(configMethod);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid state: ");
                stringBuilder.append(state);
                throw new IllegalArgumentException(stringBuilder.toString());
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

    private boolean trySetupForVendorV2_0(String ifaceName, ISupplicantIface ifaceHwBinder, SupplicantStaIfaceHalCallback callback) {
        if (!isVendorV2_0()) {
            return false;
        }
        logi("Start to setup vendor ISupplicantStaIface");
        vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = getVendorStaIfaceV2_0(ifaceHwBinder);
        VendorSupplicantStaIfaceHalCallbackV2_0 callbackV2_0 = new VendorSupplicantStaIfaceHalCallbackV2_0(ifaceName, new SupplicantStaIfaceHalCallbackV1_1(ifaceName, callback));
        if (!hwStaRegisterCallback(iface, callbackV2_0)) {
            return false;
        }
        this.mISupplicantStaIfaces.put(ifaceName, iface);
        this.mISupplicantStaIfaceCallbacks.put(ifaceName, callbackV2_0);
        logi("Successfully setup vendor ISupplicantStaIface");
        return true;
    }

    private boolean isVendorV2_0() {
        boolean z;
        synchronized (this.mLock) {
            z = false;
            try {
                if (getVendorSupplicantV2_0() != null) {
                    z = true;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicant.getService exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                supplicantServiceDiedHandler();
                return false;
            }
        }
        return z;
    }

    private vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicant getVendorSupplicantV2_0() throws RemoteException {
        vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicant castFrom;
        synchronized (this.mLock) {
            try {
                castFrom = vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicant.castFrom(ISupplicant.getService());
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get vendor V2_0 ISupplicant", e);
                return null;
            }
        }
        return castFrom;
    }

    private vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface getVendorStaIfaceV2_0(ISupplicantIface iface) {
        if (iface == null) {
            return null;
        }
        vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface castFrom;
        synchronized (this.mLock) {
            castFrom = vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface.castFrom(iface);
        }
        return castFrom;
    }

    private boolean hwStaRegisterCallback(vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface, vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "hwStaRegisterCallback";
            if (iface == null) {
                Log.e(TAG, "Got null iface when registering callback");
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.hwStaRegisterCallback(callback), "hwStaRegisterCallback");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "hwStaRegisterCallback");
                return false;
            }
        }
    }

    private vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface checkVendorSupplicantStaIfaceAndLogFailure(String ifaceName, String methodStr) {
        return getVendorStaIfaceV2_0(checkSupplicantStaIfaceAndLogFailure(ifaceName, methodStr));
    }

    public String voWifiDetect(String ifaceName, String cmd) {
        synchronized (this.mLock) {
            String methodStr = "VowifiDetect";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "VowifiDetect");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotResult = new Mutable();
            try {
                iface.VowifiDetect(cmd, new -$$Lambda$SupplicantStaIfaceHal$BbveMOGuYvRCxRTr-qxEh9PPedg(this, gotResult));
            } catch (RemoteException e) {
                handleRemoteException(e, "VowifiDetect");
            }
            String str = (String) gotResult.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$voWifiDetect$10(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotResult, SupplicantStatus status, String result) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "VowifiDetect")) {
            gotResult.value = result;
            logd(result);
        }
    }

    public String heartBeat(String ifaceName, String param) {
        synchronized (this.mLock) {
            String methodStr = "heartBeat";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "heartBeat");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotResult = new Mutable();
            try {
                iface.heartBeat(param, new -$$Lambda$SupplicantStaIfaceHal$Ge6N5l53ejJJgXsGvrUwvagqsRs(this, gotResult));
            } catch (RemoteException e) {
                handleRemoteException(e, "heartBeat");
            }
            String str = (String) gotResult.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$heartBeat$11(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotResult, SupplicantStatus status, String result) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "heartBeat")) {
            gotResult.value = result;
            logd(result);
        }
    }

    public boolean enableHiLinkHandshake(String ifaceName, boolean uiEnable, String bssid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableHiLinkHandshake:uiEnable=");
        stringBuilder.append(uiEnable);
        stringBuilder.append(" bssid=");
        stringBuilder.append(bssid);
        Log.d(str, stringBuilder.toString());
        synchronized (this.mLock) {
            String methodStr = "enableHiLinkHandshake";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "enableHiLinkHandshake");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.enableHiLinkHandshake(uiEnable, bssid), "enableHiLinkHandshake");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "enableHiLinkHandshake");
                return false;
            }
        }
    }

    public boolean setTxPower(String ifaceName, int level) {
        synchronized (this.mLock) {
            String methodStr = "setTxPower";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "setTxPower");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setTxPower(level), "setTxPower");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setTxPower");
                return false;
            }
        }
    }

    public boolean setAbsCapability(String ifaceName, int capability) {
        synchronized (this.mLock) {
            String methodStr = "SetAbsCapability";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "SetAbsCapability");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.SetAbsCapability(capability), "SetAbsCapability");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "SetAbsCapability");
                return false;
            }
        }
    }

    public boolean absPowerCtrl(String ifaceName, int type) {
        synchronized (this.mLock) {
            String methodStr = "AbsPowerCtrl";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "AbsPowerCtrl");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.AbsPowerCtrl(type), "AbsPowerCtrl");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "AbsPowerCtrl");
                return false;
            }
        }
    }

    public boolean setAbsBlacklist(String ifaceName, String bssidList) {
        synchronized (this.mLock) {
            String methodStr = "SetAbsBlacklist";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "SetAbsBlacklist");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.SetAbsBlacklist(bssidList), "SetAbsBlacklist");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "SetAbsBlacklist");
                return false;
            }
        }
    }

    public boolean query11vRoamingNetwork(String ifaceName, int reason) {
        synchronized (this.mLock) {
            String methodStr = "wnmBssQurey";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "wnmBssQurey");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.wnmBssQurey(reason), "wnmBssQurey");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "wnmBssQurey");
                return false;
            }
        }
    }

    public String getRsdbCapability(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "getCapabRsdb";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "getCapabRsdb");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotRsdb = new Mutable();
            try {
                iface.getCapabRsdb(new -$$Lambda$SupplicantStaIfaceHal$NLmRQ94LHCEhRjWqZ6MA75YLFqg(this, gotRsdb));
            } catch (RemoteException e) {
                handleRemoteException(e, "getCapabRsdb");
            }
            String str = (String) gotRsdb.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$getRsdbCapability$12(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotRsdb, SupplicantStatus status, String rsdb) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getCapabRsdb")) {
            gotRsdb.value = rsdb;
        }
    }

    public String getWpasConfig(String ifaceName, int psktype) {
        synchronized (this.mLock) {
            String methodStr = "getWpasConfig";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "getWpasConfig");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotPsk = new Mutable();
            try {
                iface.getWpasConfig(psktype, new -$$Lambda$SupplicantStaIfaceHal$C_rmp4-E7MovlV0Isb8I9Q4Ttd4(this, gotPsk));
            } catch (RemoteException e) {
                handleRemoteException(e, "getWpasConfig");
            }
            String str = (String) gotPsk.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$getWpasConfig$13(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotPsk, SupplicantStatus status, String psk) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getWpasConfig")) {
            gotPsk.value = psk;
        }
    }

    public boolean pwrPercentBoostModeset(String ifaceName, int rssi) {
        synchronized (this.mLock) {
            String methodStr = "pwrPercentBoostModeset";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "pwrPercentBoostModeset");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.pwrPercentBoostModeset(rssi), "pwrPercentBoostModeset");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "pwrPercentBoostModeset");
                return false;
            }
        }
    }

    public String getMssState(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "getMssState";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "getMssState");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotMss = new Mutable();
            try {
                iface.getMssState(new -$$Lambda$SupplicantStaIfaceHal$zZnuSnAKUSZZYHzth3z6dzrbGV8(this, gotMss));
            } catch (RemoteException e) {
                handleRemoteException(e, "getMssState");
            }
            String str = (String) gotMss.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$getMssState$14(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotMss, SupplicantStatus status, String mss) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getMssState")) {
            gotMss.value = mss;
        }
    }

    public String getApVendorInfo(String ifaceName) {
        synchronized (this.mLock) {
            String methodStr = "getApVendorInfo";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "getApVendorInfo");
            if (iface == null) {
                return null;
            }
            Mutable<String> gotApVendorInfo = new Mutable();
            try {
                iface.getApVendorInfo(new -$$Lambda$SupplicantStaIfaceHal$_DgO3AASha_6QaWN6lPwzwiPLYM(this, gotApVendorInfo));
            } catch (RemoteException e) {
                handleRemoteException(e, "getApVendorInfo");
            }
            String str = (String) gotApVendorInfo.value;
            return str;
        }
    }

    public static /* synthetic */ void lambda$getApVendorInfo$15(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable gotApVendorInfo, SupplicantStatus status, String apvendorinfo) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(status, "getApVendorInfo")) {
            gotApVendorInfo.value = apvendorinfo;
        }
    }

    public boolean setFilterEnable(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setFilterEnable";
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface iface = checkVendorSupplicantStaIfaceAndLogFailure(ifaceName, "setFilterEnable");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setFilterEnable(enable), "setFilterEnable");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setFilterEnable");
                return false;
            }
        }
    }
}
