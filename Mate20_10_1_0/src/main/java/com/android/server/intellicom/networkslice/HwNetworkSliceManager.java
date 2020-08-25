package com.android.server.intellicom.networkslice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.StringNetworkSpecifier;
import android.os.Bundle;
import android.os.Messenger;
import android.os.UserManager;
import android.telephony.HwTelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.intellicom.networkslice.css.BoosterProxy;
import com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver;
import com.android.server.intellicom.networkslice.css.NetworkSliceCallback;
import com.android.server.intellicom.networkslice.css.NetworkSlicesHandler;
import com.android.server.intellicom.networkslice.model.FqdnIps;
import com.android.server.intellicom.networkslice.model.NetworkSliceInfo;
import com.android.server.intellicom.networkslice.model.OsAppId;
import com.android.server.intellicom.networkslice.model.RouteSelectionDescriptor;
import com.android.server.intellicom.networkslice.model.TrafficDescriptors;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.internal.util.IndentingPrintWriterEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwNetworkSliceManager {
    private static final String ACTION_MAKE_DEFAULT_PHONE_DONE = "com.huawei.intent.action.MAKE_DEFAULT_PHONE_DONE";
    private static final String ACTION_NETWORK_SLICE_LOST = "com.huawei.intent.action.NETWORK_SLICE_LOST";
    private static final String ACTION_RIL_CONNECTED = "com.huawei.intent.action.RIL_CONNECTED";
    private static final boolean DBG = true;
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final int INVAILID_UID = -1;
    private static final int IPV4_LEN = 4;
    private static final int IPV6_LEN = 16;
    private static final boolean IS_NR_SLICES_SUPPORTED = HwFrameworkFactory.getHwInnerTelephonyManager().isNrSlicesSupported();
    private static final int MATCH_ALL_UID = -1;
    public static final int MAX_BIND_NUM = 32;
    private static final int MAX_NETWORK_SLICE = 6;
    private static final String NETWORK_ON_LOST_DNN = "dnn";
    private static final String NETWORK_ON_LOST_PDU_SESSION_TYPE = "pduSessionType";
    private static final String NETWORK_ON_LOST_SNSSAI = "sNssai";
    private static final String NETWORK_ON_LOST_SSCMODE = "sscMode";
    public static final String OS_ID = "01020304050607080102030405060708#";
    private static final int REQUEST_NETWORK_TIMEOUT = 10000;
    private static final String SEPARATOR_FOR_NORMAL_DATA = ",";
    private static final String SINGLE_INDENT = "  ";
    private static final String TAG = "HwNetworkSliceManager";
    /* access modifiers changed from: private */
    public BoosterProxy mBoosterProxy;
    private BroadcastReceiver mBroadcastReceiver;
    private ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public Context mContext;
    private NetworkSlicesHandler mHandler;
    private AtomicBoolean mHasMatchAllSlice;
    /* access modifiers changed from: private */
    public HwNetworkSliceSettingsObserver mHwNetworkSliceSettingsObserver;
    private boolean mIsDefaultDataOnMainCard;
    private boolean mIsFirstPhoneMakeDone;
    private AtomicBoolean mIsMatchAllRequsted;
    private boolean mIsMatchRequesting;
    private boolean mIsNr;
    private boolean mIsNrSa;
    private AtomicBoolean mIsReady;
    private boolean mIsScreenOn;
    private boolean mIsUrspAvailable;
    private boolean mIsWifiConnect;
    private AtomicInteger mNetworkRequestCountor;
    private AtomicInteger mNetworkSliceCounter;
    private List<NetworkSliceInfo> mNetworkSliceInfos;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    public static HwNetworkSliceManager getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public void init(Context context) {
        if (context == null) {
            log("context is null, fail to initWhitelist HwNetworkSliceManager.");
            return;
        }
        log("construct HwNetworkSliceManager");
        this.mContext = context;
        this.mHwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.getInstance();
        this.mHwNetworkSliceSettingsObserver.init(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(HW_SYSTEM_SERVER_START);
        filter.addAction(ACTION_MAKE_DEFAULT_PHONE_DONE);
        filter.addAction(ACTION_RIL_CONNECTED);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED);
        filter.addAction(ACTION_NETWORK_SLICE_LOST);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mBoosterProxy = BoosterProxy.getInstance();
        initNetworkSliceInfos();
        this.mHandler.registerForAppStateObserver();
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager == null) {
            loge("can not initialize HwNetworkSliceManager");
            return;
        }
        telephonyManager.listen(this.mPhoneStateListener, 1);
        this.mIsReady.getAndSet(true);
    }

    private void initNetworkSliceInfos() {
        this.mNetworkSliceInfos = new ArrayList();
        for (int nc = 33; nc <= 38; nc++) {
            NetworkRequest request = new NetworkRequest.Builder().addCapability(nc).addTransportType(0).build();
            NetworkSliceInfo networkSliceInfo = new NetworkSliceInfo();
            networkSliceInfo.setNetworkRequest(request);
            networkSliceInfo.setNetworkCapability(nc);
            this.mNetworkSliceInfos.add(networkSliceInfo);
        }
        this.mNetworkSliceCounter = new AtomicInteger(0);
    }

    private HwNetworkSliceManager() {
        this.mIsFirstPhoneMakeDone = true;
        this.mNetworkSliceInfos = new ArrayList();
        this.mNetworkSliceCounter = new AtomicInteger(0);
        this.mIsReady = new AtomicBoolean(false);
        this.mHandler = new NetworkSlicesHandler();
        this.mIsMatchAllRequsted = new AtomicBoolean(false);
        this.mHasMatchAllSlice = new AtomicBoolean(false);
        this.mIsMatchRequesting = false;
        this.mNetworkRequestCountor = new AtomicInteger(1);
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.android.server.intellicom.networkslice.HwNetworkSliceManager.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    HwNetworkSliceManager.logw("intent or intent.getAction is null.");
                    return;
                }
                String action = intent.getAction();
                char c = 65535;
                switch (action.hashCode()) {
                    case -1094345150:
                        if (action.equals(HwNetworkSliceManager.ACTION_NETWORK_SLICE_LOST)) {
                            c = 5;
                            break;
                        }
                        break;
                    case -374985024:
                        if (action.equals(HwNetworkSliceManager.HW_SYSTEM_SERVER_START)) {
                            c = 0;
                            break;
                        }
                        break;
                    case -343630553:
                        if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED)) {
                            c = 4;
                            break;
                        }
                        break;
                    case -25388475:
                        if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                            c = 3;
                            break;
                        }
                        break;
                    case 568726786:
                        if (action.equals(HwNetworkSliceManager.ACTION_MAKE_DEFAULT_PHONE_DONE)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1849863807:
                        if (action.equals(HwNetworkSliceManager.ACTION_RIL_CONNECTED)) {
                            c = 2;
                            break;
                        }
                        break;
                }
                if (c == 0) {
                    HwNetworkSliceManager.this.mBoosterProxy.registerBoosterCallback();
                    HwNetworkSliceManager.this.mHwNetworkSliceSettingsObserver.initWhitelist(HwNetworkSliceManager.this.mContext);
                    HwNetworkSliceManager.log("register booster callback.");
                } else if (c == 1) {
                    HwNetworkSliceManager.log("Receive ACTION_MAKE_DEFAULT_PHONE_DONE");
                    HwNetworkSliceManager.this.handleMakeDefaultPhoneDone();
                } else if (c == 2) {
                    HwNetworkSliceManager.log("Receive ACTION_RIL_CONNECTED");
                } else if (c == 3) {
                    HwNetworkSliceManager.log("Receive ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                    HwNetworkSliceManager.this.handleDefaultDataSubscriptionChanged(intent);
                } else if (c == 4) {
                    HwNetworkSliceManager.log("Receive NETWORK_STATE_CHANGED_ACTION");
                    HwNetworkSliceManager.this.onWifiNetworkStateChanged(intent);
                } else if (c != 5) {
                    HwNetworkSliceManager.log("BroadcastReceiver error: " + action);
                } else {
                    HwNetworkSliceManager.log("Receive ACTION_NETWORK_SLICE_LOST");
                    HwNetworkSliceManager.this.handleNetworkSliceLost(intent);
                }
            }
        };
        this.mPhoneStateListener = new PhoneStateListener() {
            /* class com.android.server.intellicom.networkslice.HwNetworkSliceManager.AnonymousClass2 */

            public void onServiceStateChanged(ServiceState serviceState) {
                HwNetworkSliceManager.logd("onServiceStateChanged");
                HwNetworkSliceManager.this.updateNrState(serviceState);
            }
        };
    }

    private NetworkSliceInfo requestNetworkSlice(TrafficDescriptors td) {
        if (td == null) {
            logw("requestNetworkSlice, the TrafficDescriptor is null");
            return null;
        }
        Bundle result = this.mBoosterProxy.getNetworkSlice(td, this.mContext);
        if (result == null) {
            logw("can't get network slice");
            return null;
        }
        RouteSelectionDescriptor rsd = RouteSelectionDescriptor.makeRouteSelectionDescriptor(result);
        NetworkSliceInfo requestAgain = getNetworkSliceInfoByPara(rsd, NetworkSliceInfo.ParaType.ROUTE_SELECTION_DESCRIPTOR);
        log("requestAgain = " + requestAgain);
        if (requestAgain != null) {
            return handleRsdRequestAgain(requestAgain, td);
        }
        if (this.mNetworkSliceCounter.get() >= 6) {
            log("already has 6 network slices, do not request again. uid = " + td.getUid());
            return null;
        }
        TrafficDescriptors tds = TrafficDescriptors.makeTrafficDescriptors(result);
        NetworkSliceInfo networkSliceInfo = getNetworkSliceInfoByPara(null, NetworkSliceInfo.ParaType.ROUTE_SELECTION_DESCRIPTOR);
        if (networkSliceInfo != null) {
            networkSliceInfo.setRouteSelectionDescriptor(rsd);
            networkSliceInfo.setTrafficDescriptors(tds);
            this.mNetworkSliceCounter.getAndIncrement();
            log("Slice network has binded, networkSliceInfo = " + networkSliceInfo);
        }
        return networkSliceInfo;
    }

    private NetworkSliceInfo handleRsdRequestAgain(NetworkSliceInfo requestAgain, TrafficDescriptors td) {
        if (requestAgain == null || td == null) {
            return null;
        }
        if (requestAgain.isIpTriad() || requestAgain.hasAllreadyBinded(td.getUid(), td.getFqdnIps())) {
            log("networkSlice has allready binded uid:" + td.getUid() + ",networkSliceInfo = " + requestAgain);
            return null;
        } else if (requestAgain.getNetId() == -1) {
            log("networkSlice doesn't finish activity, bind later uid:" + td.getUid() + ",networkSliceInfo = " + requestAgain);
            Set<FqdnIps> waittingFqdnIps = requestAgain.getWaittingFqdnIps();
            if (waittingFqdnIps == null) {
                waittingFqdnIps = new HashSet();
                requestAgain.setWaittingFqdnIps(waittingFqdnIps);
            }
            if (requestAgain.getFqdnIps() != null) {
                waittingFqdnIps.add(requestAgain.getFqdnIps().getNewFqdnIps(td.getFqdnIps()));
            }
            if (!td.isNeedToCreateRequest()) {
                return null;
            }
            TrafficDescriptors tdsAgain = requestAgain.getTrafficDescriptors();
            if (tdsAgain != null) {
                tdsAgain.setRequestAgain(true);
            }
            return requestAgain;
        } else {
            int bindResult = bindNetworkSliceProcessToNetworkForRequestAgain(td.getUid(), requestAgain, td.getFqdnIps());
            if (bindResult == 0 && requestAgain.isNeedCacheUid()) {
                requestAgain.getUids().add(Integer.valueOf(td.getUid()));
                requestAgain.getUsedUids().add(Integer.valueOf(td.getUid()));
            }
            log("no need to request this slice again. uid = " + td.getUid() + " and bind result = " + bindResult);
            if (td.isNeedToCreateRequest()) {
                return requestAgain;
            }
            return null;
        }
    }

    public NetworkSliceInfo getNetworkSliceInfoByPara(Object o, NetworkSliceInfo.ParaType type) {
        for (NetworkSliceInfo sliceInfo : this.mNetworkSliceInfos) {
            if (sliceInfo.isRightNetworkSlice(o, type)) {
                log("getNetworkSliceInfoByPara, sliceInfo = " + sliceInfo);
                return sliceInfo;
            }
        }
        log("getNetworkSliceInfoByPara, return null");
        return null;
    }

    /* access modifiers changed from: private */
    public void updateNrState(ServiceState state) {
        if (state == null) {
            logw("updateNrState failed, invalid state");
            return;
        }
        boolean isNr = false;
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager != null) {
            isNr = telephonyManager.getNetworkType() == 20;
        }
        boolean isNrSa = !HwTelephonyManager.getDefault().isNsaState(this.mHwNetworkSliceSettingsObserver.getMainSlotId());
        log("updateNrState mIsNr=" + this.mIsNr + ", isNr=" + isNr + ", mIsNrSa=" + this.mIsNrSa + ", isNrSa=" + isNrSa + ", MainSlotId=" + this.mHwNetworkSliceSettingsObserver.getMainSlotId());
        if (!(this.mIsNr == isNr && this.mIsNrSa == isNrSa)) {
            this.mIsNr = isNr;
            this.mIsNrSa = isNrSa;
        }
        if (this.mIsNr && this.mIsNrSa) {
            requestMatchAllSlice();
        }
    }

    private int getNextRequestId() {
        return this.mNetworkRequestCountor.getAndIncrement();
    }

    private int bindNetworkSliceProcessToNetworkForRequestAgain(int uid, NetworkSliceInfo nsi, FqdnIps fqdnIps) {
        log("bindNetworkSliceProcessToNetworkForRequestAgain: uid = " + uid + " nsi = " + nsi + " fqdnIps = " + fqdnIps);
        if (nsi == null) {
            return -3;
        }
        if (nsi.isMatchAll()) {
            return 0;
        }
        TrafficDescriptors tds = nsi.getTrafficDescriptors();
        if (tds == null) {
            return -3;
        }
        Bundle bindParas = new Bundle();
        BoosterProxy.fillBindParas(nsi.getNetId(), tds.getUrspPrecedence(), bindParas);
        int i = AnonymousClass3.$SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[nsi.getRouteBindType().ordinal()];
        if (i == 1) {
            fillUidBindParasForRequestAgain(bindParas, uid, nsi);
        } else if (i == 2) {
            fillIpBindParas(bindParas, tds, fqdnIps, nsi);
        } else if (i == 3) {
            fillUidBindParasForRequestAgain(bindParas, uid, nsi);
            fillIpBindParas(bindParas, tds, fqdnIps, nsi);
        } else if (i == 4) {
            logw("Can not bind invalid tds");
        }
        return this.mBoosterProxy.bindProcessToNetwork(bindParas);
    }

    /* renamed from: com.android.server.intellicom.networkslice.HwNetworkSliceManager$3  reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType = new int[TrafficDescriptors.RouteBindType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.UID_TDS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.IP_TDS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.UID_IP_TDS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[TrafficDescriptors.RouteBindType.INVALID_TDS.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private int bindNetworkSliceProcessToNetwork(int uid, NetworkSliceInfo nsi, FqdnIps fqdnIps) {
        TrafficDescriptors tds;
        log("bindNetworkSliceProcessToNetwork: uid = " + uid + " nsi = " + nsi + " fqdnIps = " + fqdnIps);
        if (nsi == null || (tds = nsi.getTrafficDescriptors()) == null) {
            return -3;
        }
        Bundle bindParas = new Bundle();
        BoosterProxy.fillBindParas(nsi.getNetId(), tds.getUrspPrecedence(), bindParas);
        int i = AnonymousClass3.$SwitchMap$com$android$server$intellicom$networkslice$model$TrafficDescriptors$RouteBindType[nsi.getRouteBindType().ordinal()];
        if (i == 1) {
            fillUidBindParas(bindParas, tds, uid, nsi);
        } else if (i == 2) {
            fillIpBindParas(bindParas, tds, fqdnIps, nsi);
        } else if (i == 3) {
            fillUidBindParas(bindParas, tds, uid, nsi);
            fillIpBindParas(bindParas, tds, fqdnIps, nsi);
        } else if (i == 4) {
            logw("Can not bind invalid tds");
        }
        return this.mBoosterProxy.bindProcessToNetwork(bindParas);
    }

    private void fillUidBindParasForRequestAgain(Bundle bindParas, int uid, NetworkSliceInfo nsi) {
        if (nsi.getUids().size() > 32) {
            log("Over uid bind specification limit");
            return;
        }
        BoosterProxy.fillBindParas(String.valueOf(uid), bindParas);
        nsi.getUids().add(Integer.valueOf(uid));
        nsi.getUsedUids().add(Integer.valueOf(uid));
    }

    private void fillUidBindParas(Bundle bindParas, TrafficDescriptors tds, int uid, NetworkSliceInfo nsi) {
        String[] uidStrs;
        if (nsi.getUids().size() > 32) {
            log("Over uid bind specification limit");
            return;
        }
        if (tds.isMatchDnn()) {
            BoosterProxy.fillBindParas(String.valueOf(uid), bindParas);
            nsi.getUids().add(Integer.valueOf(uid));
        } else {
            String uids = getUidsFromAppIds(tds.getAppIds());
            if (uids != null && (uidStrs = uids.split(",")) != null) {
                Set<Integer> tempUids = new HashSet<>();
                for (String uidStr : uidStrs) {
                    if (uidStr != null) {
                        try {
                            tempUids.add(Integer.valueOf(uidStr));
                        } catch (NumberFormatException e) {
                            log("wrong uid string");
                        }
                    }
                }
                log("bindNetworkSliceProcessToNetwork getUidsFromAppIds=" + uids);
                BoosterProxy.fillBindParas(uids, bindParas);
                nsi.setUids(tempUids);
            } else {
                return;
            }
        }
        nsi.getUsedUids().add(Integer.valueOf(uid));
    }

    private void fillIpBindParas(Bundle bindParas, TrafficDescriptors tds, FqdnIps fqdnIps, NetworkSliceInfo nsi) {
        if (tds.isMatchFqdn()) {
            FqdnIps newFqdnIps = fqdnIps;
            if (nsi.getFqdnIps() == null) {
                nsi.setFqdnIps(fqdnIps);
            } else {
                newFqdnIps = nsi.getFqdnIps().getNewFqdnIps(fqdnIps);
            }
            BoosterProxy.fillIpBindParasForFqdn(bindParas, newFqdnIps);
            nsi.mergeFqdnIps(fqdnIps);
            return;
        }
        BoosterProxy.fillIpBindParasForIpTriad(bindParas, tds);
    }

    private String getUidsFromAppIds(String originAppIds) {
        String result;
        Set<String> appIds = getAppIdsWithoutOsId(originAppIds);
        if (appIds == null) {
            result = null;
        } else {
            result = (String) appIds.stream().map(new Function() {
                /* class com.android.server.intellicom.networkslice.$$Lambda$HwNetworkSliceManager$LUne0BElZASNasNlQ5XLhRBt_Q */

                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return HwNetworkSliceManager.this.lambda$getUidsFromAppIds$0$HwNetworkSliceManager((String) obj);
                }
            }).flatMap($$Lambda$AMboGYEhuVfighAr4ZxypJn5wlE.INSTANCE).filter($$Lambda$HwNetworkSliceManager$J8PQoEjhjgm45kvrbzUJf1heKA.INSTANCE).map($$Lambda$HwNetworkSliceManager$pMG1Z11RzEEdIpBcuuWB1PujQfQ.INSTANCE).collect(Collectors.joining(","));
        }
        log("getUidsFromAppIds, uids = " + result);
        return result;
    }

    static /* synthetic */ boolean lambda$getUidsFromAppIds$1(Integer uids) {
        return uids != null;
    }

    private Set<String> getAppIdsWithoutOsId(String originAppIds) {
        String[] orgins = originAppIds.split(",");
        if (orgins == null || orgins.length == 0) {
            loge("getAppIdsWithoutOsId orgins == null, should not run here.");
            return null;
        }
        Set<String> appIds = new HashSet<>();
        int length = orgins.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            OsAppId osAppId = OsAppId.create(orgins[i]);
            if (osAppId != null) {
                if (appIds.size() > 32) {
                    log("slice tds appid is more than MAX_BIND_NUM=32");
                    break;
                }
                appIds.add(osAppId.getAppId());
            }
            i++;
        }
        return appIds;
    }

    public void requestNetworkSliceForPackageName(int uid) {
        if (!isCanMatchNetworkSlices()) {
            log("requestNetworkSlice, current environment cannot match slices");
            return;
        }
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        log("requestNetworkSliceForPackageName, uid = " + uid);
        if (!this.mHwNetworkSliceSettingsObserver.isNeedToRequestSliceForAppId(packageName)) {
            log("No need to requestNetwork for uid = " + uid);
            return;
        }
        requestNetwork(uid, requestNetworkSlice(new TrafficDescriptors.Builder().setUid(uid).build()));
    }

    public void requestNetworkSliceForFqdn(int uid, String fqdn, List<String> ipAddresses, int ipAddressesCount) {
        if (!isCanMatchNetworkSlices()) {
            log("requestNetworkSliceForFqdn, current environment cannot match slices");
        } else if (ipAddresses == null) {
            logd("requestNetworkSliceForFqdn ipAddresses is null.");
        } else {
            logd("requestNetworkSliceForFqdn for uid = " + uid);
            if (!this.mHwNetworkSliceSettingsObserver.isNeedToRequestSliceForFqdn(fqdn)) {
                log("No need to requestNetwork for uid = " + uid);
                return;
            }
            Set<InetAddress> ipv4 = new HashSet<>();
            Set<InetAddress> ipv6 = new HashSet<>();
            for (String s : ipAddresses) {
                try {
                    InetAddress ip = InetAddress.getByName(s);
                    if (ip != null) {
                        int length = ip.getAddress().length;
                        if (length != 4) {
                            if (length != 16) {
                                logw("ip length wrong, len = " + ip.getAddress().length);
                            } else if (ipv6.size() <= 32) {
                                ipv6.add(ip);
                            }
                        } else if (ipv4.size() <= 32) {
                            ipv4.add(ip);
                        }
                    }
                } catch (UnknownHostException e) {
                    logw("RequestNetworkSliceForFqdn has UnknownHostException");
                }
            }
            requestNetwork(uid, requestNetworkSlice(new TrafficDescriptors.Builder().setUid(uid).setFqdn(fqdn).setFqdnIps(new FqdnIps.Builder().setIpv4Addr(ipv4).setIpv6Addr(ipv6).build()).build()));
        }
    }

    public void handleUidGone(int uid) {
        if (IS_NR_SLICES_SUPPORTED) {
            if (HwNetworkSliceSettingsObserver.getInstance().isNeedToRequestSliceForAppId(this.mContext.getPackageManager().getNameForUid(uid))) {
                for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
                    if (nsi.isNeedCacheUid() && nsi.getUsedUids().contains(Integer.valueOf(uid))) {
                        Set<Integer> usedUids = nsi.getUsedUids();
                        log("handleUidGone - usedUids  = " + usedUids + " uid = " + uid);
                        usedUids.remove(Integer.valueOf(uid));
                        if (usedUids.size() == 0) {
                            releaseNetworkSlice(nsi);
                        }
                    }
                }
            }
        }
    }

    private void unbindProcessToNetworkForSingleUid(NetworkSliceInfo nsi, int uid) {
        Set<Integer> usedUids = nsi.getUsedUids();
        usedUids.remove(Integer.valueOf(uid));
        if (usedUids.size() == 0) {
            releaseNetworkSlice(nsi);
        } else {
            this.mBoosterProxy.unbindProcessToNetwork(String.valueOf(uid), nsi.getNetId());
        }
    }

    public void handleUidRemoved(String packageName) {
        Set<Integer> uids;
        if (IS_NR_SLICES_SUPPORTED && (uids = lambda$getUidsFromAppIds$0$HwNetworkSliceManager(packageName)) != null) {
            for (Integer num : uids) {
                int uid = num.intValue();
                for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
                    if (nsi.isNeedCacheUid() && nsi.getUsedUids().contains(Integer.valueOf(uid))) {
                        unbindProcessToNetworkForSingleUid(nsi, uid);
                        nsi.getUids().remove(Integer.valueOf(uid));
                    }
                }
            }
        }
    }

    public void handleUrspChanged(Bundle data) {
        if (!IS_NR_SLICES_SUPPORTED) {
            logd("requestNetworkSlice, current environment cannot match slices");
            return;
        }
        this.mIsReady.getAndSet(false);
        this.mIsUrspAvailable = true;
        TrafficDescriptors tds = TrafficDescriptors.makeMatchAllTrafficDescriptors(data);
        cleanEnvironment();
        this.mIsReady.getAndSet(true);
        if (!tds.isMatchAll()) {
            this.mHasMatchAllSlice.getAndSet(false);
            return;
        }
        this.mHasMatchAllSlice.getAndSet(true);
        RouteSelectionDescriptor rsd = RouteSelectionDescriptor.makeRouteSelectionDescriptor(data);
        NetworkSliceInfo networkSliceInfo = getNetworkSliceInfoByPara(null, NetworkSliceInfo.ParaType.ROUTE_SELECTION_DESCRIPTOR);
        if (networkSliceInfo != null) {
            log("networkSliceInfo != null == true, setRouteSelectionDescriptor = " + rsd + ", tds = " + tds);
            networkSliceInfo.setRouteSelectionDescriptor(rsd);
            networkSliceInfo.setTrafficDescriptors(tds);
            this.mNetworkSliceCounter.getAndIncrement();
        }
        log("match all slice start to active when ursp changed, networkSliceInfo= " + networkSliceInfo);
        if (!isCanRequestNetwork()) {
            log("handleUrspChanged can not request network");
            return;
        }
        this.mIsMatchRequesting = true;
        requestNetwork(-1, networkSliceInfo);
    }

    public void requestMatchAllSlice() {
        if (this.mHasMatchAllSlice.get() && !this.mIsMatchAllRequsted.get() && !this.mIsMatchRequesting && isCanRequestNetwork()) {
            this.mIsMatchRequesting = true;
            for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
                if (nsi.isMatchAll()) {
                    requestNetwork(-1, nsi);
                    return;
                }
            }
        }
    }

    private void cleanEnvironment() {
        int result = this.mBoosterProxy.unbindAllRoute();
        StringBuilder sb = new StringBuilder();
        sb.append("unbind all route, result = ");
        sb.append(result == 0);
        log(sb.toString());
        for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
            unregisterNetworkCallback(nsi);
        }
        initNetworkSliceInfos();
        log("Clean enveronment done");
    }

    private void unregisterNetworkCallbackRemoteInitiated(RouteSelectionDescriptor rsd) {
        NetworkSliceInfo nsi = getNetworkSliceInfoByPara(rsd, NetworkSliceInfo.ParaType.ROUTE_SELECTION_DESCRIPTOR);
        if (nsi != null) {
            unregisterNetworkCallback(nsi);
            cleanRouteSelectionDescriptor(nsi);
        }
    }

    private void releaseNetworkSlice(NetworkSliceInfo nsi) {
        this.mBoosterProxy.unbindProcessToNetwork(nsi.getUidsStr(), nsi.getNetId());
        unregisterNetworkCallback(nsi);
        cleanRouteSelectionDescriptor(nsi);
    }

    private void unregisterNetworkCallback(NetworkSliceInfo nsi) {
        ConnectivityManager cm = ConnectivityManager.from(this.mContext);
        if (cm == null) {
            log("Can not get ConnectivityManager in onNetworkLost.");
            return;
        }
        NetworkSliceCallback networkCallback = nsi.getNetworkCallback();
        if (networkCallback != null) {
            log("unregisterNetworkCallback nsi = " + nsi);
            cm.unregisterNetworkCallback(networkCallback);
            if (networkCallback.getNetwork() != null) {
                networkCallback.onLostForNetworkSlice(nsi.getNetworkCallback().getNetwork(), false);
            }
        }
    }

    public NetworkRequest requestNetworkSliceForDnn(NetworkCapabilities nc, Messenger messenger, int uid) {
        String apnName = getApnName(nc);
        log("requestNetworkSliceForDnn apnNames = " + apnName);
        if (!this.mHwNetworkSliceSettingsObserver.isNeedToRequestSliceForDnn(apnName)) {
            logd("No need to requestNetwork for dnn");
            return null;
        }
        TrafficDescriptors td = new TrafficDescriptors.Builder().setDnn(apnName).setUid(uid).setMessenger(messenger).setNeedToCreateRequest(true).build();
        NetworkSliceInfo networkSliceInfo = requestNetworkSlice(td);
        if (networkSliceInfo == null) {
            return null;
        }
        NetworkCapabilities newNc = new NetworkCapabilities(nc);
        newNc.addCapability(networkSliceInfo.getNetworkCapability());
        newNc.addTransportType(0);
        NetworkRequest request = new NetworkRequest(newNc, -1, getNextRequestId(), NetworkRequest.Type.REQUEST);
        networkSliceInfo.putCreatedNetworkRequest(request);
        networkSliceInfo.addMessenger(request.requestId, td.getMessenger());
        TrafficDescriptors tds = networkSliceInfo.getTrafficDescriptors();
        if (tds != null) {
            if (!tds.isRequestAgain()) {
                requestNetwork(uid, networkSliceInfo, request.requestId);
            } else {
                networkSliceInfo.getNetworkCallback().onAvailable(request.requestId);
            }
        }
        return request;
    }

    public void requestNetworkSliceForIp(int uid, byte[] ip, String protocolId, String remotePort) {
        if (!isCanMatchNetworkSlices()) {
            logd("requestNetworkSliceForIp, current environment cannot match slices");
            return;
        }
        try {
            requestNetwork(uid, requestNetworkSlice(new TrafficDescriptors.Builder().setUid(uid).setIp(InetAddress.getByAddress(ip)).setProtocolId(protocolId).setRemotePort(remotePort).build()));
        } catch (UnknownHostException e) {
            logw("addr is of illegal length");
        }
    }

    private void requestNetwork(int uid, NetworkSliceInfo networkSliceInfo) {
        if (networkSliceInfo == null || networkSliceInfo.getNetworkRequest() == null) {
            logw("networkSliceInfo is null with no request id");
        } else {
            requestNetwork(uid, networkSliceInfo, networkSliceInfo.getNetworkRequest().requestId);
        }
    }

    private void requestNetwork(int uid, NetworkSliceInfo networkSliceInfo, int requestId) {
        if (networkSliceInfo == null) {
            logw("requestNetwork networkSliceInfo is null");
            return;
        }
        RouteSelectionDescriptor rsd = networkSliceInfo.getRouteSelectionDescriptor();
        TrafficDescriptors tds = networkSliceInfo.getTrafficDescriptors();
        if (rsd == null || tds == null) {
            logw("requestNetwork rsd is null or tds is null");
            return;
        }
        NetworkRequest request = networkSliceInfo.getNetworkRequest();
        if (request == null) {
            logw("Can not get request by capability:" + networkSliceInfo.getNetworkCapability());
            cleanRouteSelectionDescriptor(networkSliceInfo);
            return;
        }
        fillRsdIntoNetworkRquest(request, rsd, tds);
        ConnectivityManager cm = ConnectivityManager.from(this.mContext);
        if (cm == null) {
            log("Can not get ConnectivityManager");
            cleanRouteSelectionDescriptor(networkSliceInfo);
            return;
        }
        log("For " + uid + " start to request:" + request);
        NetworkSliceCallback networkSliceCallback = new NetworkSliceCallback(uid, requestId, this.mHandler);
        cm.requestNetwork(request, networkSliceCallback, 10000);
        networkSliceInfo.setNetworkCallback(networkSliceCallback);
    }

    private void fillRsdIntoNetworkRquest(NetworkRequest request, RouteSelectionDescriptor rsd, TrafficDescriptors tds) {
        request.networkCapabilities.setDnn(rsd.getDnn());
        request.networkCapabilities.setSnssai(rsd.getSnssai());
        request.networkCapabilities.setSscMode(rsd.getSscMode());
        request.networkCapabilities.setPduSessionType(rsd.getPduSessionType());
        request.networkCapabilities.setRouteBitmap(tds.getRouteBitmap());
    }

    private void cleanRouteSelectionDescriptor(NetworkSliceInfo networkSliceInfo) {
        if (networkSliceInfo != null) {
            log("cleanRouteSelectionDescriptor:" + networkSliceInfo);
            cleanRequest(networkSliceInfo.getNetworkRequest());
            networkSliceInfo.setRouteSelectionDescriptor(null);
            this.mNetworkSliceCounter.getAndDecrement();
        }
    }

    public void onNetworkAvailable(int uid, int netId, NetworkRequest request) {
        log("onNetworkAvailable request = " + request + " netId = " + netId + ", uid = " + uid);
        NetworkSliceInfo networkSliceInfo = getNetworkSliceInfoByPara(request, NetworkSliceInfo.ParaType.NETWORK_REQUEST);
        if (networkSliceInfo == null) {
            log("onNetworkAvailable - networkSliceInfo is null");
            return;
        }
        networkSliceInfo.setNetId(netId);
        if (networkSliceInfo.isMatchAll()) {
            log("match_all do not need to bind route");
            this.mIsMatchAllRequsted.getAndSet(true);
            this.mIsMatchRequesting = false;
            return;
        }
        int result = bindNetworkSliceProcessToNetwork(uid, networkSliceInfo, networkSliceInfo.getFqdnIps());
        if (result == 0) {
            for (FqdnIps fqdnIps : networkSliceInfo.getWaittingFqdnIps()) {
                bindNetworkSliceProcessToNetwork(uid, networkSliceInfo, fqdnIps);
            }
            networkSliceInfo.cleanWaittingFqdnIps();
            log("bind success networkSliceInfo = " + networkSliceInfo);
            return;
        }
        recoveryNetworkSlice(networkSliceInfo);
        log("can not bind slice, uid = " + uid + " result = " + result);
    }

    private Set<Integer> getUidsSetFromAppIds(String originAppIds) {
        Set<String> appIds = getAppIdsWithoutOsId(originAppIds);
        Set<Integer> uids = null;
        if (appIds != null) {
            uids = new HashSet<>();
            for (String id : appIds) {
                Set<Integer> uidsForAppId = lambda$getUidsFromAppIds$0$HwNetworkSliceManager(id);
                if (uidsForAppId != null) {
                    uids.addAll(uidsForAppId);
                }
            }
        }
        log("getUidsFromAppIds, originAppIds=" + originAppIds + ",uids=" + uids);
        return uids;
    }

    public void onNetworkLost(NetworkRequest request) {
        log("onNetworkLost request = " + request);
        if (request != null) {
            NetworkSliceInfo networkSliceInfo = getNetworkSliceInfoByPara(request, NetworkSliceInfo.ParaType.NETWORK_REQUEST);
            if (networkSliceInfo == null) {
                log("onNetworkLost - networkSliceInfo = null");
            } else if (networkSliceInfo.isMatchAll()) {
                this.mIsMatchAllRequsted.getAndSet(false);
                this.mIsMatchRequesting = false;
            } else {
                networkSliceInfo.cleanUsedUids();
                int result = this.mBoosterProxy.unbindProcessToNetwork(networkSliceInfo.getUidsStr(), networkSliceInfo.getNetId());
                log("unbind uid to network slice result = " + result);
            }
        }
    }

    public void onNetworkUnAvailable(int uid, NetworkRequest request) {
        NetworkSliceInfo nsi = getNetworkSliceInfoByPara(request, NetworkSliceInfo.ParaType.NETWORK_REQUEST);
        log("onNetworkUnAvailable request= " + request + " ,nsi= " + nsi);
        if (nsi != null) {
            if (nsi.isMatchAll()) {
                this.mIsMatchRequesting = false;
                this.mIsMatchAllRequsted.getAndSet(false);
            }
            recoveryNetworkSlice(nsi);
        }
    }

    private void recoveryNetworkSlice(NetworkSliceInfo networkSliceInfo) {
        if (networkSliceInfo != null) {
            networkSliceInfo.setNetId(-1);
            networkSliceInfo.cleanUids();
            networkSliceInfo.cleanUsedUids();
            networkSliceInfo.setRouteSelectionDescriptor(null);
            networkSliceInfo.setFqdnIps(null);
            ConnectivityManager cm = ConnectivityManager.from(this.mContext);
            NetworkSliceCallback networkCallback = networkSliceInfo.getNetworkCallback();
            if (!(cm == null || networkCallback == null)) {
                cm.unregisterNetworkCallback(networkCallback);
            }
            networkSliceInfo.setNetworkCallback(null);
            cleanRequest(networkSliceInfo.getNetworkRequest());
            this.mNetworkSliceCounter.getAndDecrement();
        }
    }

    /* access modifiers changed from: private */
    public static void log(String msg) {
        Log.i(TAG, msg);
    }

    /* access modifiers changed from: private */
    public static void logd(String msg) {
        Log.d(TAG, msg);
    }

    /* access modifiers changed from: private */
    public static void logw(String msg) {
        Log.w(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }

    private boolean isValidCapability(int capability) {
        return 33 <= capability && capability <= 38;
    }

    private void cleanRequest(NetworkRequest request) {
        if (request != null) {
            log("cleanRequest:" + request);
            request.networkCapabilities.setDnn("");
            request.networkCapabilities.setSscMode((byte) 0);
            request.networkCapabilities.setSnssai("");
            request.networkCapabilities.setPduSessionType(0);
            request.networkCapabilities.setRouteBitmap((byte) 0);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriterEx pw = new IndentingPrintWriterEx(writer, SINGLE_INDENT);
        pw.println("Active Network Slice:");
        pw.increaseIndent();
        for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
            if (nsi.getRouteSelectionDescriptor() != null) {
                pw.println(nsi.toString());
            }
        }
        pw.println("isNrSlicesSupported: " + IS_NR_SLICES_SUPPORTED);
        pw.println("isInNr: " + isInNr());
        pw.println("isSaState: " + isSaState());
        pw.println("isAirPlaneModeOn: " + this.mHwNetworkSliceSettingsObserver.isAirplaneModeOn());
        pw.println("isMobileDataEnabled: " + this.mHwNetworkSliceSettingsObserver.isMobileDataEnabled());
        pw.println("isWifiConnected: " + isWifiConnected());
        pw.println("isScreenOn: " + isScreenOn());
        pw.println("isVpnOn: " + isVpnOn());
        pw.println("isDefaultDataOnMainCard: " + isDefaultDataOnMainCard());
        pw.println("isUrspAvailable: " + isUrspAvailable());
        pw.decreaseIndent();
    }

    private boolean isCanRequestNetwork() {
        if (!isMobileDataClose() && !isAirplaneModeOn() && !isWifiConnected() && isInNr() && isSaState() && isDefaultDataOnMainCard() && !isVpnOn()) {
            return true;
        }
        return false;
    }

    private boolean isCanMatchNetworkSlices() {
        if (IS_NR_SLICES_SUPPORTED && this.mIsReady.get() && isUrspAvailable() && isCanRequestNetwork()) {
            return true;
        }
        return false;
    }

    private boolean isMobileDataClose() {
        return !this.mHwNetworkSliceSettingsObserver.isMobileDataEnabled();
    }

    private boolean isSaState() {
        return this.mIsNrSa;
    }

    private boolean isWifiConnected() {
        return this.mIsWifiConnect;
    }

    private boolean isScreenOn() {
        return this.mIsScreenOn;
    }

    private boolean isInNr() {
        return this.mIsNr;
    }

    private boolean isAirplaneModeOn() {
        return this.mHwNetworkSliceSettingsObserver.isAirplaneModeOn();
    }

    private boolean isVpnOn() {
        return this.mHwNetworkSliceSettingsObserver.isVpnOn();
    }

    private boolean isDefaultDataOnMainCard() {
        return this.mIsDefaultDataOnMainCard;
    }

    private boolean isUrspAvailable() {
        return this.mIsUrspAvailable;
    }

    private void setUrspAvailable(boolean urspAvailable) {
        this.mIsUrspAvailable = urspAvailable;
    }

    private static class SingletonInstance {
        /* access modifiers changed from: private */
        public static final HwNetworkSliceManager INSTANCE = new HwNetworkSliceManager();

        private SingletonInstance() {
        }
    }

    private static int getApnTypeFromNetworkCapabilities(NetworkCapabilities nc) {
        if (nc.getTransportTypes().length <= 0 || nc.hasTransport(0)) {
            int apnType = 0;
            if (nc.hasCapability(12)) {
                apnType = 17;
            }
            if (nc.hasCapability(0)) {
                apnType = 2;
            }
            if (nc.hasCapability(1)) {
                apnType = 4;
            }
            if (nc.hasCapability(2)) {
                apnType = 8;
            }
            if (nc.hasCapability(3)) {
                apnType = 32;
            }
            if (nc.hasCapability(4)) {
                apnType = 64;
            }
            if (nc.hasCapability(5)) {
                apnType = 128;
            }
            if (nc.hasCapability(7)) {
                apnType = 256;
            }
            if (nc.hasCapability(8)) {
                Log.e(TAG, "RCS APN type not yet supported");
            }
            if (nc.hasCapability(9)) {
                apnType = 4194304;
            }
            if (nc.hasCapability(10)) {
                apnType = 512;
            }
            if (nc.hasCapability(23)) {
                apnType = 1024;
            }
            if (nc.hasCapability(25)) {
                apnType = AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION;
            }
            if (nc.hasCapability(26)) {
                apnType = 65536;
            }
            if (nc.hasCapability(27)) {
                apnType = 131072;
            }
            if (nc.hasCapability(28)) {
                apnType = 262144;
            }
            if (nc.hasCapability(29)) {
                apnType = AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE;
            }
            if (nc.hasCapability(30)) {
                apnType = HighBitsCompModeID.MODE_COLOR_ENHANCE;
            }
            if (nc.hasCapability(31)) {
                apnType = HighBitsCompModeID.MODE_EYE_PROTECT;
            }
            if (nc.hasCapability(32)) {
                apnType = 8388608;
            }
            if (NetworkCapabilities.hasSNSSAICapability(nc)) {
                apnType = 33554432;
            }
            if (apnType == 0) {
                Log.e(TAG, "Unsupported NetworkRequest in Telephony: nc = " + nc);
            }
            return apnType;
        }
        log("nc.getTransportTypes().length = " + nc.getTransportTypes().length + ",nc = " + nc);
        return 0;
    }

    private boolean hasApnType(int apnTypeBitmask, int type) {
        return (apnTypeBitmask & type) == type;
    }

    private int getPhoneIdFromNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        int subId = -1;
        int phoneId = -1;
        NetworkSpecifier networkSpecifier = networkCapabilities.getNetworkSpecifier();
        if (networkSpecifier != null) {
            try {
                if (networkSpecifier instanceof StringNetworkSpecifier) {
                    subId = Integer.parseInt(networkSpecifier.toString());
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "getPhoneIdFromNetworkCapabilities exceptio");
            }
        }
        if (subId != -1) {
            phoneId = SubscriptionManager.getPhoneId(subId);
        }
        log("getPhoneIdFromNetworkCapabilities, subId = " + subId + " phoneId:" + phoneId);
        return phoneId;
    }

    public boolean isNeedToMatchNetworkSlice(NetworkCapabilities nc) {
        if (nc != null && IS_NR_SLICES_SUPPORTED && !inApnTypeWhiteList(nc)) {
            return true;
        }
        return false;
    }

    private boolean inApnTypeWhiteList(NetworkCapabilities nc) {
        switch (getApnTypeFromNetworkCapabilities(nc)) {
            case 0:
            case 17:
            case 64:
            case AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION /*{ENCODED_INT: 32768}*/:
            case 65536:
            case 131072:
            case 262144:
            case AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE /*{ENCODED_INT: 524288}*/:
            case HighBitsCompModeID.MODE_COLOR_ENHANCE /*{ENCODED_INT: 1048576}*/:
            case HighBitsCompModeID.MODE_EYE_PROTECT /*{ENCODED_INT: 2097152}*/:
            case 4194304:
            case 8388608:
            case 16777216:
            case 33554432:
                return true;
            default:
                return false;
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: getUidsFromAppId */
    public Set<Integer> lambda$getUidsFromAppIds$0$HwNetworkSliceManager(String appid) {
        int userCount = UserManager.get(this.mContext).getUserCount();
        List<UserInfo> users = UserManager.get(this.mContext).getUsers();
        PackageManager pm = this.mContext.getPackageManager();
        Set<Integer> uids = new HashSet<>();
        for (int n = 0; n < userCount; n++) {
            try {
                int uid = pm.getPackageUidAsUser(appid, users.get(n).id);
                log("getUidsFromAppIds uid " + uid);
                if (uid != -1) {
                    uids.add(Integer.valueOf(uid));
                }
            } catch (PackageManager.NameNotFoundException e) {
                loge("getUidsFromAppId can not find the packageName");
            }
        }
        return uids;
    }

    private String getApnName(NetworkCapabilities nc) {
        int apnType = getApnTypeFromNetworkCapabilities(nc);
        for (HwNetworkSliceSettingsObserver.ApnObject apnObject : this.mHwNetworkSliceSettingsObserver.getApnObjects()) {
            if (apnType != 0 && hasApnType(apnObject.getApnTypesBitmask(), apnType)) {
                log("match dnn for nc = " + nc + ", apnType=" + apnType + ", apnNmae = " + apnObject.getApnName());
                return apnObject.getApnName();
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void onWifiNetworkStateChanged(Intent intent) {
        if (intent == null) {
            logw("intent from wifi broadcast is null");
            return;
        }
        boolean isWifiConnected = false;
        boolean isChanged = false;
        NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        if (netInfo != null) {
            if (netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                isWifiConnected = false;
            }
            if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
                isWifiConnected = true;
            }
            if (this.mIsWifiConnect != isWifiConnected) {
                logd("onWifiNetworkStateChanged wifi is from " + this.mIsWifiConnect + " to " + isWifiConnected);
                isChanged = true;
                this.mIsWifiConnect = isWifiConnected;
            }
            if (isChanged) {
                if (this.mIsWifiConnect) {
                    unbindAllProccessToNetwork();
                } else {
                    requestMatchAllSlice();
                    bindAllProccessToNetwork();
                }
            }
        }
        log("onWifiNetworkStateChanged mIsWifiConnect = " + this.mIsWifiConnect);
    }

    private void unbindAllProccessToNetwork() {
        for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
            String uids = nsi.getUidsStr();
            if (!nsi.isMatchAll() && nsi.getNetId() != -1) {
                nsi.cleanUsedUids();
                int result = this.mBoosterProxy.unbindProcessToNetwork(uids, nsi.getNetId());
                log("unbindAllProccessToNetwork, unbind to network slice result = " + result + " nsi = " + nsi);
            }
        }
    }

    private void bindAllProccessToNetwork() {
        for (NetworkSliceInfo nsi : this.mNetworkSliceInfos) {
            nsi.cleanUsedUids();
            nsi.getUidsStr();
            if (!nsi.isMatchAll()) {
                TrafficDescriptors tds = nsi.getTrafficDescriptors();
                NetworkSliceCallback nsc = nsi.getNetworkCallback();
                int result = -3;
                if (!(tds == null || nsc == null || nsi.getNetId() == -1)) {
                    result = bindNetworkSliceProcessToNetwork(nsc.getUid(), nsi, nsi.getFqdnIps());
                }
                log("bindAllProccessToNetwork, bind uid to network slice result = " + result + " nsi = " + nsi);
                if (result != 0) {
                    recoveryNetworkSlice(nsi);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNetworkSliceLost(Intent intent) {
        if (intent != null) {
            byte sscMode = intent.getByteExtra(NETWORK_ON_LOST_SSCMODE, (byte) 0);
            int pduSessionType = intent.getIntExtra(NETWORK_ON_LOST_PDU_SESSION_TYPE, -1);
            String snssai = "";
            String dnn = intent.getStringExtra(NETWORK_ON_LOST_DNN) != null ? intent.getStringExtra(NETWORK_ON_LOST_DNN) : snssai;
            if (intent.getStringExtra(NETWORK_ON_LOST_SNSSAI) != null) {
                snssai = intent.getStringExtra(NETWORK_ON_LOST_SNSSAI);
            }
            RouteSelectionDescriptor rsd = new RouteSelectionDescriptor.Builder().setDnn(dnn).setPduSessionType(pduSessionType).setSnssai(snssai).setSscMode(sscMode).build();
            log("ACTION_NETWORK_SLICE_LOST : " + rsd);
            unregisterNetworkCallbackRemoteInitiated(rsd);
        }
    }

    /* access modifiers changed from: private */
    public void handleDefaultDataSubscriptionChanged(Intent intent) {
        if (intent != null) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(this.mContext);
            if (subscriptionManager != null) {
                log("handleDefaultDataSubscriptionChanged MainSlotId=" + this.mHwNetworkSliceSettingsObserver.getMainSlotId() + ", DefaultDataPhoneId=" + subscriptionManager.getDefaultDataPhoneId());
                this.mIsDefaultDataOnMainCard = this.mHwNetworkSliceSettingsObserver.getMainSlotId() == subscriptionManager.getDefaultDataPhoneId();
            }
            if (this.mIsDefaultDataOnMainCard) {
                requestMatchAllSlice();
                bindAllProccessToNetwork();
                return;
            }
            unbindAllProccessToNetwork();
        }
    }

    /* access modifiers changed from: private */
    public void handleMakeDefaultPhoneDone() {
        if (!this.mIsFirstPhoneMakeDone) {
            cleanEnvironment();
        } else {
            this.mIsFirstPhoneMakeDone = false;
        }
    }
}
