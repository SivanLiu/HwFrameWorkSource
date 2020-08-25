package huawei.com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IDnsResolver;
import android.net.INetd;
import android.net.INetworkPolicyListener;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;
import android.net.util.NetdService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.HwTelephonyManagerInner;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.server.ConnectivityService;
import com.android.server.HwBluetoothManagerServiceEx;
import com.android.server.connectivity.IHwConnectivityServiceInner;
import com.android.server.connectivity.NetworkAgentInfo;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import com.huawei.server.connectivity.IHwConnectivityServiceEx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HwConnectivityServiceEx implements IHwConnectivityServiceEx {
    private static final int BLOCK = 1;
    private static final String CTS_PACKAGE = "android.jobscheduler.cts";
    private static final boolean DBG = true;
    private static final String DNS_RESOLVER_SERVICE = "dnsresolver";
    private static final int EVENT_DELETE_NETWORK_CONNECT_CACHE = 3;
    private static final int EVENT_FRESH_NETWORK_CONNECT_CACHE = 1;
    private static final int EVENT_NETWORK_TESTED = 41;
    private static final int EVENT_UID_RULES_CHANGED = 2;
    private static final int INVAILD_RESULT = -1;
    private static final int INVALID_NETWORK_ID = -1;
    private static final int NETWORK_STATE_CACHE_FRESH_TIME = 10000;
    private static final int NETWORK_STATE_CACHE_LIFE_TIME = 2000;
    private static final int NETWORK_TESTED_RESULT_INVALID = 0;
    private static final String REFLECTION_CONNECTIVITY_SERVICE = "com.android.server.ConnectivityService";
    private static final String REFLECTION_TRACKER_HANDLER = "mTrackerHandler";
    private static final String TAG = HwConnectivityServiceEx.class.getSimpleName();
    private static final int UNBLOCK = 0;
    private Context context;
    private IHwConnectivityServiceInner csi;
    /* access modifiers changed from: private */
    public final Map<Integer, CacheNetworkState> mCacheNetworkStateMapByUid = new ConcurrentHashMap();
    InternalHandler mHandler;
    protected final HandlerThread mHandlerThread;
    private final INetworkPolicyListener mPolicyListener = new NetworkPolicyManager.Listener() {
        /* class huawei.com.android.server.connectivity.HwConnectivityServiceEx.AnonymousClass1 */

        public void onUidRulesChanged(int uid, int uidRules) {
            HwConnectivityServiceEx.this.mHandler.sendMessage(HwConnectivityServiceEx.this.mHandler.obtainMessage(2, uid, uidRules));
        }

        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            HwConnectivityServiceEx.this.mHandler.sendMessage(HwConnectivityServiceEx.this.mHandler.obtainMessage(3));
        }
    };

    public HwConnectivityServiceEx(IHwConnectivityServiceInner csi2, Context context2) {
        this.csi = csi2;
        this.context = context2;
        this.mHandlerThread = new HandlerThread("HwConnectivityServiceExThread");
        this.mHandlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        ((NetworkPolicyManager) context2.getSystemService("netpolicy")).registerListener(this.mPolicyListener);
    }

    private static void log(String s) {
        Slog.d(TAG, s);
    }

    private static void loge(String s) {
        Slog.e(TAG, s);
    }

    public void maybeHandleNetworkAgentMessageEx(Message msg, NetworkAgentInfo nai, Handler handler) {
        HashMap<Messenger, NetworkAgentInfo> mNetworkAgentInfos = this.csi.getNetworkAgentInfos();
        int i = msg.what;
        switch (i) {
            case 528486:
                nai.networkMisc.wifiApType = msg.arg1;
                log("CMD_UPDATE_WIFI_AP_TYPE :" + nai.networkMisc.wifiApType);
                return;
            case 528487:
                nai.networkMisc.connectToCellularAndWLAN = msg.arg1;
                nai.networkMisc.acceptUnvalidated = ((Boolean) msg.obj).booleanValue();
                log("update acceptUnvalidated is: " + nai.networkMisc.acceptUnvalidated + ", connectToCellularAndWLAN: " + nai.networkMisc.connectToCellularAndWLAN);
                return;
            default:
                switch (i) {
                    case 528585:
                        setExplicitlyUnselected(mNetworkAgentInfos.get(msg.replyTo));
                        return;
                    case 528586:
                        updateNetworkConcurrently(mNetworkAgentInfos.get(msg.replyTo), (NetworkInfo) msg.obj);
                        return;
                    case 528587:
                        triggerRoamingNetworkMonitor(mNetworkAgentInfos.get(msg.replyTo));
                        return;
                    case 528588:
                        triggerInvalidlinkNetworkMonitor(mNetworkAgentInfos.get(msg.replyTo), handler);
                        return;
                    default:
                        return;
                }
        }
    }

    public void removeLegacyRouteToHost(int netId, RouteInfo bestRoute, int uid) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("network_management");
        log("removeLegacyRouteToHost");
        if (b != null) {
            try {
                _data.writeInterfaceToken(this.csi.getDescriptor());
                _data.writeInt(netId);
                bestRoute.writeToParcel(_data, 0);
                _data.writeInt(uid);
                b.transact(this.csi.getCodeRemoveLegacyrouteToHost(), _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException e) {
                loge("Exception trying to remove a route : RemoteException");
            } catch (Exception e2) {
                loge("Exception trying to remove a route");
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
                throw th;
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public NetworkAgentInfo getIdenticalActiveNetworkAgentInfo(NetworkAgentInfo na) {
        if (HuaweiTelephonyConfigs.isHisiPlatform() || na == null || na.networkInfo.getState() != NetworkInfo.State.CONNECTED) {
            return null;
        }
        for (NetworkAgentInfo network : this.csi.getNetworkAgentInfos().values()) {
            log("checking existed " + network.name());
            if (network != this.csi.getHwNetworkForType(network.networkInfo.getType())) {
                log("not recorded, ignore");
            } else {
                LinkProperties curNetworkLp = network.linkProperties;
                LinkProperties newNetworkLp = na.linkProperties;
                if (network.networkInfo.getState() != NetworkInfo.State.CONNECTED || curNetworkLp == null || TextUtils.isEmpty(curNetworkLp.getInterfaceName())) {
                    log("some key parameter is null, ignore");
                } else {
                    boolean isLpIdentical = curNetworkLp.keyEquals(newNetworkLp);
                    log("LinkProperties Identical are " + isLpIdentical);
                    NetworkSpecifier ns = network.networkCapabilities.getNetworkSpecifier();
                    NetworkSpecifier ns2 = na.networkCapabilities.getNetworkSpecifier();
                    if (ns != null && ns.satisfiedBy(ns2) && isLpIdentical) {
                        log("apparently satisfied");
                        return network;
                    }
                }
            }
        }
        return null;
    }

    public void setupUniqueDeviceName() {
        String id;
        String hostname = SystemProperties.get("net.hostname");
        if ((TextUtils.isEmpty(hostname) || hostname.length() < 8) && (id = Settings.Secure.getString(this.context.getContentResolver(), "android_id")) != null && id.length() > 0) {
            if (TextUtils.isEmpty(hostname)) {
                String hostname2 = SystemProperties.get("ro.config.marketing_name", "");
                if (TextUtils.isEmpty(hostname2)) {
                    hostname = Build.MODEL.replace(" ", "_");
                    if (hostname != null && hostname.length() > 18) {
                        hostname = hostname.substring(0, 18);
                    }
                } else {
                    hostname = hostname2.replace(" ", "_");
                }
            }
            String hostname3 = hostname + AwarenessInnerConstants.DASH_KEY + id;
            if (hostname3 != null && hostname3.length() > 25) {
                hostname3 = hostname3.substring(0, 25);
            }
            SystemProperties.set("net.hostname", hostname3);
        }
    }

    private void setExplicitlyUnselected(NetworkAgentInfo nai) {
        if (nai != null) {
            nai.networkMisc.explicitlySelected = false;
            nai.networkMisc.acceptUnvalidated = false;
            if (nai.networkInfo != null && ConnectivityManager.getNetworkTypeName(1).equals(nai.networkInfo.getTypeName())) {
                log("WiFi+ switch from WiFi to Cellular, enableDefaultTypeAPN explicitly.");
                enableDefaultTypeAPN(true);
            }
        }
    }

    private IDnsResolver getDnsResolver() {
        return IDnsResolver.Stub.asInterface(ServiceManager.getService(DNS_RESOLVER_SERVICE));
    }

    private void updateNetworkConcurrently(NetworkAgentInfo networkAgent, NetworkInfo newInfo) {
        NetworkInfo oldInfo;
        NetworkInfo.State state = newInfo.getState();
        INetd netd = NetdService.getInstance();
        synchronized (networkAgent) {
            oldInfo = networkAgent.networkInfo;
            networkAgent.networkInfo = newInfo;
        }
        if (oldInfo != null && oldInfo.getState() == state) {
            log("updateNetworkConcurrently, ignoring duplicate network state non-change");
        } else if (netd == null) {
            loge("updateNetworkConcurrently, invalid member, netd = null");
        } else {
            networkAgent.setCurrentScore(0);
            try {
                netd.networkCreatePhysical(networkAgent.network.netId, networkAgent.networkCapabilities.hasCapability(13) ? 0 : 2);
                getDnsResolver().createNetworkCache(networkAgent.network.netId);
                networkAgent.created = true;
                this.csi.hwUpdateLinkProperties(networkAgent, (LinkProperties) null);
                log("updateNetworkConcurrently, nai.networkInfo = " + networkAgent.networkInfo);
                Bundle redirectUrlBundle = new Bundle();
                redirectUrlBundle.putString(NetworkAgent.REDIRECT_URL_KEY, "");
                networkAgent.asyncChannel.sendMessage(528391, 4, 0, redirectUrlBundle);
            } catch (Exception e) {
                loge("updateNetworkConcurrently, Error creating network");
            }
        }
    }

    private void triggerRoamingNetworkMonitor(NetworkAgentInfo networkAgent) {
    }

    private int getCurrentNetworkId() {
        int networkId = -1;
        ConnectivityService cs = null;
        ConnectivityService cs2 = this.csi;
        if (cs2 instanceof ConnectivityService) {
            cs = cs2;
        }
        if (cs == null) {
            return -1;
        }
        Network network = cs.getNetworkForTypeWifi();
        if (network != null) {
            networkId = network.netId;
        }
        log("networkId= " + networkId);
        return networkId;
    }

    private void triggerInvalidlinkNetworkMonitor(NetworkAgentInfo networkAgent, Handler handler) {
        int netId = getCurrentNetworkId();
        if (netId == -1) {
            loge("netId is invalid");
        } else if (handler != null) {
            handler.sendMessage(handler.obtainMessage(41, 0, netId, ""));
        }
    }

    private void enableDefaultTypeAPN(boolean enabled) {
        log("enableDefaultTypeAPN= " + enabled);
        String str = "true";
        String defaultMobileEnable = SystemProperties.get("sys.defaultapn.enabled", str);
        log("DEFAULT_MOBILE_ENABLE before state is " + defaultMobileEnable);
        if (!enabled) {
            str = "false";
        }
        SystemProperties.set("sys.defaultapn.enabled", str);
        HwTelephonyManagerInner hwTm = HwTelephonyManagerInner.getDefault();
        if (hwTm != null) {
            hwTm.setDefaultMobileEnable(enabled);
        }
    }

    private class InterfaceBlockInfo {
        public int block;
        public long timeStamp;

        public InterfaceBlockInfo(int block2, long timeStamp2) {
            this.timeStamp = timeStamp2;
            this.block = block2;
        }
    }

    /* access modifiers changed from: private */
    public class CacheNetworkState {
        public Map<String, InterfaceBlockInfo> cacheNetworkStateMapByIntf = new ConcurrentHashMap();
        private long timeStamp = SystemClock.elapsedRealtime();

        public CacheNetworkState() {
        }

        public InterfaceBlockInfo getBlockByIntf(String intf) {
            return this.cacheNetworkStateMapByIntf.get(intf);
        }

        public void setBlockByIntf(String intf, int blockValue, long timeStamp2) {
            this.cacheNetworkStateMapByIntf.put(intf, new InterfaceBlockInfo(blockValue, timeStamp2));
        }

        public boolean isNeedDelete(long currentTime) {
            if (currentTime - this.timeStamp > 2000) {
                return true;
            }
            return false;
        }
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                HwConnectivityServiceEx.this.handleFreshCache();
            } else if (i == 2) {
                synchronized (HwConnectivityServiceEx.this.mCacheNetworkStateMapByUid) {
                    int uid = msg.arg1;
                    if (HwConnectivityServiceEx.this.mCacheNetworkStateMapByUid.get(Integer.valueOf(uid)) != null) {
                        HwConnectivityServiceEx.this.mCacheNetworkStateMapByUid.remove(Integer.valueOf(uid));
                    }
                }
            } else if (i == 3) {
                synchronized (HwConnectivityServiceEx.this.mCacheNetworkStateMapByUid) {
                    HwConnectivityServiceEx.this.mCacheNetworkStateMapByUid.clear();
                }
            }
        }
    }

    public int getCacheNetworkState(int uid, String interfaceName) {
        InterfaceBlockInfo interfaceBlockInfo;
        if (interfaceName == null) {
            interfaceName = HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME;
        }
        CacheNetworkState cacheNetworkState = this.mCacheNetworkStateMapByUid.get(Integer.valueOf(uid));
        if (cacheNetworkState == null || (interfaceBlockInfo = cacheNetworkState.getBlockByIntf(interfaceName)) == null || SystemClock.elapsedRealtime() - interfaceBlockInfo.timeStamp >= 2000) {
            return -1;
        }
        return interfaceBlockInfo.block;
    }

    private boolean isSystem(int uid) {
        return uid < 10000;
    }

    public void setCacheNetworkState(int uid, String interfaceName, boolean isBlock) {
        if (interfaceName == null) {
            interfaceName = HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME;
        }
        if (!CTS_PACKAGE.equals(this.context.getPackageManager().getNameForUid(uid))) {
            if (!isSystem(Process.myUid())) {
                log("setCacheNetworkState fail , Process.myUid" + Process.myUid());
                return;
            }
            log("set " + uid + " " + interfaceName + " value " + isBlock);
            synchronized (this.mCacheNetworkStateMapByUid) {
                CacheNetworkState cacheNetworkState = this.mCacheNetworkStateMapByUid.get(Integer.valueOf(uid));
                if (cacheNetworkState == null) {
                    cacheNetworkState = new CacheNetworkState();
                    this.mCacheNetworkStateMapByUid.put(Integer.valueOf(uid), cacheNetworkState);
                }
                cacheNetworkState.setBlockByIntf(interfaceName, isBlock ? 1 : 0, SystemClock.elapsedRealtime());
                if (!this.mHandler.hasMessages(1)) {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 10000);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFreshCache() {
        long currentTime = SystemClock.elapsedRealtime();
        synchronized (this.mCacheNetworkStateMapByUid) {
            Iterator<Map.Entry<Integer, CacheNetworkState>> iter = this.mCacheNetworkStateMapByUid.entrySet().iterator();
            while (iter.hasNext()) {
                if (iter.next().getValue().isNeedDelete(currentTime)) {
                    iter.remove();
                }
            }
        }
        if (this.mCacheNetworkStateMapByUid.size() != 0 && !this.mHandler.hasMessages(1)) {
            InternalHandler internalHandler = this.mHandler;
            internalHandler.sendMessageDelayed(internalHandler.obtainMessage(1), 10000);
        }
    }
}
