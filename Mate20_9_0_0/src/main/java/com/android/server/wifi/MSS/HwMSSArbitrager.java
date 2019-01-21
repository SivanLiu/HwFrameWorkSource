package com.android.server.wifi.MSS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import com.huawei.pgmng.plug.PGSdk;
import java.util.ArrayList;
import java.util.List;

public class HwMSSArbitrager {
    private static final String ACTION_PKG_PREFIX = "package:";
    private static final String BRCM_CHIP_4359 = "bcm4359";
    private static final int MAX_FREQ_24G = 2484;
    private static final int MIN_FREQ_24G = 2412;
    private static final long MIN_MSS_TIME_SPAN = 60000;
    private static final int MSS_SWITCH_24G = 1;
    private static final int MSS_SWITCH_50G = 2;
    private static final String PRODUCT_VTR_PREFIX = "VTR";
    public static final String SMART_MODE_STATUS = "SmartModeStatus";
    private static final String TAG = "MSSArbitrager";
    private static HwMSSArbitrager mInstance;
    private MSSState mABSCurrentState = MSSState.ABSMIMO;
    private String[] mAllowMSSApkList = null;
    private BroadcastReceiver mBcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = HwMSSArbitrager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("action:");
            stringBuilder.append(action);
            HwMSSUtils.log(3, str, stringBuilder.toString());
            StringBuilder stringBuilder2;
            NetworkInfo networkInfo;
            String str2;
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                HwMSSArbitrager.this.mPluggedType = intent.getIntExtra("plugged", 0);
                str = HwMSSArbitrager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mPluggedType:");
                stringBuilder2.append(HwMSSArbitrager.this.mPluggedType);
                HwMSSUtils.log(3, str, stringBuilder2.toString());
                if (SystemProperties.getInt("runtime.hwmss.debug", 0) == 0 && HwMSSArbitrager.this.isChargePluggedin() && !HwMSSArbitrager.this.isP2PConnected() && !HwMSSArbitrager.this.mWiFiApMode) {
                    for (IHwMSSObserver observer : HwMSSArbitrager.this.mMSSObservers) {
                        observer.onMSSSwitchRequest(2);
                    }
                }
            } else if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(action)) {
                networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (networkInfo != null) {
                    str2 = HwMSSArbitrager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=");
                    stringBuilder2.append(networkInfo);
                    HwMSSUtils.log(3, str2, stringBuilder2.toString());
                    HwMSSArbitrager.this.mP2pConnectState = networkInfo.isConnected();
                    str2 = HwMSSArbitrager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mP2pConnectState: ");
                    stringBuilder2.append(HwMSSArbitrager.this.mP2pConnectState);
                    HwMSSUtils.log(3, str2, stringBuilder2.toString());
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (networkInfo != null) {
                    switch (AnonymousClass3.$SwitchMap$android$net$NetworkInfo$DetailedState[networkInfo.getDetailedState().ordinal()]) {
                        case 1:
                            HwMSSArbitrager.this.mMssSwitchTimestamp = 0;
                            HwMSSUtils.log(3, HwMSSArbitrager.TAG, "connect enter");
                            HwMSSArbitrager.this.mWiFiConnectState = true;
                            synchronized (HwMSSArbitrager.this.mSyncLock) {
                                if (HwMSSArbitrager.this.mWifiManager == null) {
                                    HwMSSArbitrager.this.mWifiManager = (WifiManager) HwMSSArbitrager.this.mContext.getSystemService("wifi");
                                }
                                HwMSSArbitrager.this.mConnectWifiInfo = HwMSSArbitrager.this.mWifiManager.getConnectionInfo();
                            }
                            if (HwMSSArbitrager.this.mConnectWifiInfo != null) {
                                str2 = HwMSSArbitrager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("ssid:");
                                stringBuilder2.append(HwMSSArbitrager.this.mConnectWifiInfo.getSSID());
                                stringBuilder2.append(",bssid:");
                                stringBuilder2.append(HwMSSArbitrager.this.mConnectWifiInfo.getBSSID());
                                stringBuilder2.append(",freq:");
                                stringBuilder2.append(HwMSSArbitrager.this.mConnectWifiInfo.getFrequency());
                                HwMSSUtils.log(3, str2, stringBuilder2.toString());
                                HwMSSArbitrager.this.mCurrentBssid = HwMSSArbitrager.this.mConnectWifiInfo.getBSSID();
                                break;
                            }
                            break;
                        case 2:
                            HwMSSArbitrager.this.mMssSwitchTimestamp = 0;
                            HwMSSArbitrager.this.mWiFiConnectState = false;
                            HwMSSArbitrager.this.mCurrentBssid = null;
                            break;
                    }
                }
            } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                int apState = intent.getIntExtra("wifi_state", 11);
                StringBuilder stringBuilder3;
                if (apState == 11) {
                    HwMSSArbitrager.this.mMssSwitchTimestamp = 0;
                    HwMSSArbitrager.this.mWiFiApMode = false;
                    str2 = HwMSSArbitrager.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("ap close status ");
                    stringBuilder3.append(HwMSSArbitrager.this.mWiFiApMode);
                    Log.d(str2, stringBuilder3.toString());
                } else if (apState == 13) {
                    HwMSSArbitrager.this.mMssSwitchTimestamp = 0;
                    HwMSSArbitrager.this.mWiFiApMode = true;
                    str2 = HwMSSArbitrager.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("ap enable status ");
                    stringBuilder3.append(HwMSSArbitrager.this.mWiFiApMode);
                    Log.d(str2, stringBuilder3.toString());
                }
            }
        }
    };
    private WifiInfo mConnectWifiInfo = null;
    private Context mContext;
    private String mCurrentBssid = null;
    private boolean mGameForeground = false;
    private String[] mHT40PkgList = null;
    private HwWifiCHRService mHwWifiCHRService;
    private String[] mIncompatibleBssidList = null;
    private List<String> mInstalledPkgNameList = new ArrayList();
    private IHwMSSBlacklistMgr mMSSBlackMgrt = null;
    private MSSState mMSSCurrentState = MSSState.MSSMIMO;
    private String[] mMSSLimitList = null;
    private String[] mMSSLimitSwitchAPKList = null;
    private List<IHwMSSObserver> mMSSObservers = new ArrayList();
    private int mMSSSwitchCapa = 0;
    private long mMssSwitchTimestamp = 0;
    private boolean mP2pConnectState = false;
    private PGSdk mPGSdk = null;
    private BroadcastReceiver mPkgChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                String str = HwMSSArbitrager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("action:");
                stringBuilder.append(action);
                HwMSSUtils.log(3, str, stringBuilder.toString());
                str = intent.getDataString();
                if (str != null && str.startsWith(HwMSSArbitrager.ACTION_PKG_PREFIX)) {
                    str = str.substring(HwMSSArbitrager.ACTION_PKG_PREFIX.length());
                }
                if (!TextUtils.isEmpty(str)) {
                    String str2;
                    StringBuilder stringBuilder2;
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        str2 = HwMSSArbitrager.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("packageName:");
                        stringBuilder2.append(str);
                        HwMSSUtils.log(3, str2, stringBuilder2.toString());
                        for (String pkgName : HwMSSArbitrager.this.mInstalledPkgNameList) {
                            if (pkgName.equals(str)) {
                                return;
                            }
                        }
                        HwMSSArbitrager.this.mInstalledPkgNameList.add(str);
                        HwMSSArbitrager.this.checkMssLimitPackage(str);
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        str2 = HwMSSArbitrager.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("packageName:");
                        stringBuilder2.append(str);
                        HwMSSUtils.log(3, str2, stringBuilder2.toString());
                        for (int idx = HwMSSArbitrager.this.mInstalledPkgNameList.size() - 1; idx >= 0; idx--) {
                            if (((String) HwMSSArbitrager.this.mInstalledPkgNameList.get(idx)).equals(str)) {
                                String str3 = HwMSSArbitrager.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("match:packageName:");
                                stringBuilder3.append(str);
                                HwMSSUtils.log(3, str3, stringBuilder3.toString());
                                HwMSSArbitrager.this.mInstalledPkgNameList.remove(idx);
                                break;
                            }
                        }
                    }
                }
            }
        }
    };
    private int mPluggedType = 0;
    private Object mSyncLock = new Object();
    private boolean mWiFiApMode = false;
    private boolean mWiFiConnectState = false;
    private WifiManager mWifiManager = null;
    private boolean sisoFixFlag = false;

    /* renamed from: com.android.server.wifi.MSS.HwMSSArbitrager$3 */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public interface IHwMSSObserver {
        void onHT40Request();

        void onMSSSwitchRequest(int i);
    }

    public enum MSSState {
        MSSUNKNOWN,
        MSSMIMO,
        MSSSISO,
        ABSMIMO,
        ABSMRC,
        ABSSWITCHING,
        MSSSWITCHING
    }

    public enum MSS_TRIG_TYPE {
        CLONE_TRIG,
        COMMON_TRIG
    }

    public static synchronized HwMSSArbitrager getInstance(Context cxt) {
        HwMSSArbitrager hwMSSArbitrager;
        synchronized (HwMSSArbitrager.class) {
            if (mInstance == null) {
                mInstance = new HwMSSArbitrager(cxt);
            }
            hwMSSArbitrager = mInstance;
        }
        return hwMSSArbitrager;
    }

    public boolean setABSCurrentState(MSSState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setABSCurrentState:");
        stringBuilder.append(state);
        HwMSSUtils.log(1, str, stringBuilder.toString());
        if (state != MSSState.ABSMIMO && state != MSSState.ABSMRC && state != MSSState.ABSSWITCHING) {
            return false;
        }
        this.mABSCurrentState = state;
        HwMSSUtils.switchToast(this.mContext, this.mABSCurrentState);
        return true;
    }

    public boolean setMSSCurrentState(MSSState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMSSCurrentState:");
        stringBuilder.append(state);
        HwMSSUtils.log(1, str, stringBuilder.toString());
        if (state != MSSState.MSSMIMO && state != MSSState.MSSSISO && state != MSSState.MSSUNKNOWN && state != MSSState.MSSSWITCHING) {
            return false;
        }
        this.mMSSCurrentState = state;
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        if (this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.updateMSSState(this.mMSSCurrentState.toString());
        }
        HwMSSUtils.switchToast(this.mContext, this.mMSSCurrentState);
        return true;
    }

    public void setGameForeground(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setGameForeground:");
        stringBuilder.append(enable);
        HwMSSUtils.log(1, str, stringBuilder.toString());
        this.mGameForeground = enable;
    }

    public boolean isGameForeground() {
        return this.mGameForeground;
    }

    public MSSState getMSSCurrentState() {
        return this.mMSSCurrentState;
    }

    public MSSState getABSCurrentState() {
        return this.mABSCurrentState;
    }

    public void registerMSSObserver(IHwMSSObserver observer) {
        for (IHwMSSObserver item : this.mMSSObservers) {
            if (item == observer) {
                return;
            }
        }
        this.mMSSObservers.add(observer);
    }

    public void unregisterMSSObserver(IHwMSSObserver observer) {
        for (int i = this.mMSSObservers.size() - 1; i >= 0; i--) {
            if (this.mMSSObservers.get(i) == observer) {
                this.mMSSObservers.remove(i);
            }
        }
    }

    public boolean isSupportHT40() {
        return this.mMSSSwitchCapa > 0;
    }

    private HwMSSArbitrager(Context cxt) {
        this.mContext = cxt;
        if (HwMSSUtils.is1103()) {
            this.mMSSBlackMgrt = HisiMSSBlackListManager.getInstance(this.mContext);
        } else {
            this.mMSSBlackMgrt = HwMSSBlackListManager.getInstance(this.mContext);
        }
        getMSSSwitchCapa();
        getWhiteList();
        getHT40WhiteList();
        getAllInstalledPkg();
        getAllLimitSwitchAPK();
        getAllowMSSApkList();
        registerBcastReceiver();
        registerPkgChangedReceiver();
        getIncompatibleBssidList();
    }

    public boolean isMSSAllowed(int direction, int freq, MSS_TRIG_TYPE reason) {
        long now = SystemClock.elapsedRealtime();
        if (direction == 2 && true == this.sisoFixFlag) {
            HwMSSUtils.log(1, TAG, "isMSSAllowed false: Mobile Clone");
            return false;
        } else if (SystemProperties.getInt("runtime.hwmss.blktest", 0) == 0 && direction == 1 && now - this.mMssSwitchTimestamp < 60000) {
            HwMSSUtils.log(1, TAG, "isMSSAllowed false: time limit");
            return false;
        } else if (1 == SystemProperties.getInt("persist.hwmss.switch", 0)) {
            HwMSSUtils.log(1, TAG, "doMssSwitch persist.hwmss.switch:false");
            return false;
        } else if (SystemProperties.getInt("runtime.hwmss.bssid", 0) == 0 && !this.mWiFiApMode && isIncompatibleBssid(this.mCurrentBssid)) {
            HwMSSUtils.log(1, TAG, "isMSSAllowed isIncompatibleBssid true");
            return false;
        } else if (isGameForeground()) {
            HwMSSUtils.log(1, TAG, "isMSSAllowed isGameForeground true");
            return false;
        } else if (this.mABSCurrentState != MSSState.ABSMIMO && (this.mABSCurrentState != MSSState.ABSMRC || !ScanResult.is5GHz(freq))) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isMSSAllowed false: ABSState ");
            stringBuilder.append(this.mABSCurrentState);
            HwMSSUtils.log(1, str, stringBuilder.toString());
            return false;
        } else if (direction == 1 && isChargePluggedin() && reason != MSS_TRIG_TYPE.CLONE_TRIG && SystemProperties.getInt("runtime.hwmss.debug", 0) == 0) {
            HwMSSUtils.log(1, TAG, "isMSSAllowed isChargePluggedin:false");
            return false;
        } else if (isChipSwitchSupport(freq)) {
            if (!this.mWiFiApMode) {
                if (!isWiFiConnected()) {
                    HwMSSUtils.log(1, TAG, "isMSSAllowed isWiFiConnected: false");
                    return false;
                } else if (SystemProperties.getInt("runtime.hwmss.blktest", 0) == 0 && direction == 1 && isInMSSBlacklist()) {
                    HwMSSUtils.log(1, TAG, "isMSSAllowed isInMSSBlacklist true");
                    return false;
                }
            }
            if (isP2PConnected()) {
                HwMSSUtils.log(1, TAG, "isMSSAllowed isP2PConnected true");
                return false;
            } else if (direction == 1 && isLimitAPKonFront()) {
                HwMSSUtils.log(1, TAG, "isLimitAPKonFront true");
                return false;
            } else if (direction == 1 && matchMssLimitList() && reason != MSS_TRIG_TYPE.CLONE_TRIG) {
                HwMSSUtils.log(1, TAG, "isMSSAllowed: matchMssLimitList true");
                return false;
            } else {
                this.mMssSwitchTimestamp = now;
                return true;
            }
        } else {
            HwMSSUtils.log(1, TAG, "isMSSAllowed isChipSwitchSupport:false");
            return false;
        }
    }

    public boolean isMSSSwitchBandSupport() {
        if ((this.mMSSSwitchCapa & 1) > 0) {
            return HwMSSUtils.isAllowSwitch();
        }
        return false;
    }

    public boolean matchHT40List() {
        String pkgname = "";
        if (this.mHT40PkgList == null || this.mInstalledPkgNameList.size() == 0) {
            return false;
        }
        pkgname = null;
        while (pkgname < this.mHT40PkgList.length) {
            for (int j = this.mInstalledPkgNameList.size() - 1; j >= 0; j--) {
                String pkgname2 = (String) this.mInstalledPkgNameList.get(j);
                if (pkgname2 != null && pkgname2.contains(this.mHT40PkgList[pkgname])) {
                    return true;
                }
            }
            pkgname++;
        }
        return false;
    }

    public int readSaveMode() {
        return System.getIntForUser(this.mContext.getContentResolver(), SMART_MODE_STATUS, 1, 0);
    }

    private void registerBcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addCategory("android.net.wifi.STATE_CHANGE@hwBrExpand@WifiNetStatus=WIFICON|WifiNetStatus=WIFIDSCON");
        filter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBcastReceiver, filter);
    }

    private void registerPkgChangedReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        this.mContext.registerReceiver(this.mPkgChangedReceiver, filter);
    }

    private void getAllInstalledPkg() {
        List<PackageInfo> pkgInfoList = this.mContext.getPackageManager().getInstalledPackages(null);
        this.mInstalledPkgNameList.clear();
        HwMSSUtils.log(3, TAG, "getAllInstalledPkg:");
        for (PackageInfo info : pkgInfoList) {
            this.mInstalledPkgNameList.add(info.packageName);
            HwMSSUtils.log(3, TAG, info.packageName);
        }
    }

    private void getWhiteList() {
        this.mMSSLimitList = new String[]{"com.magicandroidapps.iperf", "net.he.networktools", "org.zwanoo.android.speedtest", "com.example.wptp.testapp", "com.veriwave.waveagent"};
    }

    private void getHT40WhiteList() {
        this.mHT40PkgList = new String[]{"com.example.wptp.testapp", "com.veriwave.waveagent"};
    }

    public void getAllowMSSApkList() {
        this.mAllowMSSApkList = new String[]{"com.ixia.ixchariot"};
    }

    private void getAllLimitSwitchAPK() {
        this.mMSSLimitSwitchAPKList = new String[]{"com.dewmobile.kuaiya"};
    }

    private void getIncompatibleBssidList() {
        this.mIncompatibleBssidList = new String[]{"b0:d5:9d", "c8:d5:fe", "70:b0:35"};
    }

    private boolean matchMssLimitList() {
        String pkgname = "";
        if (this.mMSSLimitList == null || this.mInstalledPkgNameList.size() == 0) {
            return false;
        }
        pkgname = null;
        while (pkgname < this.mMSSLimitList.length) {
            for (int j = this.mInstalledPkgNameList.size() - 1; j >= 0; j--) {
                String pkgname2 = (String) this.mInstalledPkgNameList.get(j);
                if (pkgname2 != null && pkgname2.contains(this.mMSSLimitList[pkgname])) {
                    return true;
                }
            }
            pkgname++;
        }
        return false;
    }

    public boolean matchAllowMSSApkList() {
        String apkname = "";
        if (this.mAllowMSSApkList == null || this.mInstalledPkgNameList.size() == 0) {
            return false;
        }
        apkname = null;
        while (apkname < this.mAllowMSSApkList.length) {
            for (int j = this.mInstalledPkgNameList.size() - 1; j >= 0; j--) {
                String apkname2 = (String) this.mInstalledPkgNameList.get(j);
                if (apkname2 != null && apkname2.contains(this.mAllowMSSApkList[apkname])) {
                    return true;
                }
            }
            apkname++;
        }
        return false;
    }

    private boolean isChargePluggedin() {
        if (this.mPluggedType == 2 || this.mPluggedType == 5) {
            return true;
        }
        return false;
    }

    public boolean isP2PConnected() {
        return this.mP2pConnectState;
    }

    public boolean isWiFiConnected() {
        return this.mWiFiConnectState;
    }

    private boolean isLimitAPKonFront() {
        if (this.mPGSdk == null) {
            this.mPGSdk = PGSdk.getInstance();
        }
        if (this.mPGSdk != null) {
            try {
                String pktName = this.mPGSdk.getTopFrontApp(this.mContext);
                for (String equals : this.mMSSLimitSwitchAPKList) {
                    if (equals.equals(pktName)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("found limit APK: ");
                        stringBuilder.append(pktName);
                        HwMSSUtils.log(1, str, stringBuilder.toString());
                        return true;
                    }
                }
            } catch (RemoteException e) {
                HwMSSUtils.log(1, TAG, "get top front app fail");
                return false;
            }
        }
        return false;
    }

    public boolean isInMSSBlacklist() {
        String ssid = "";
        String bssid = "";
        synchronized (this.mSyncLock) {
            if (this.mConnectWifiInfo != null) {
                ssid = this.mConnectWifiInfo.getSSID();
                bssid = this.mConnectWifiInfo.getBSSID();
            }
        }
        if ((HwMSSUtils.is1103() && !TextUtils.isEmpty(bssid) && this.mMSSBlackMgrt.isInBlacklistByBssid(bssid)) || TextUtils.isEmpty(ssid)) {
            return true;
        }
        return this.mMSSBlackMgrt.isInBlacklist(ssid);
    }

    private void getMSSSwitchCapa() {
        this.mMSSSwitchCapa = 0;
        String chipName = SystemProperties.get("ro.connectivity.sub_chiptype", "");
        if (BRCM_CHIP_4359.equals(chipName)) {
            if (SystemProperties.get("ro.product.name", "").startsWith(PRODUCT_VTR_PREFIX)) {
                this.mMSSSwitchCapa = 1;
            } else {
                this.mMSSSwitchCapa = 3;
            }
        } else if (HwMSSUtils.HISI_CHIP_1103.equals(chipName)) {
            this.mMSSSwitchCapa = 3;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x001a, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isChipSwitchSupport(int freq) {
        if (this.mMSSSwitchCapa == 3) {
            return true;
        }
        if (this.mMSSSwitchCapa != 1) {
            return false;
        }
        synchronized (this.mSyncLock) {
            if (freq < MIN_FREQ_24G || freq > MAX_FREQ_24G) {
            } else {
                return true;
            }
        }
    }

    private void checkMssLimitPackage(String packageName) {
        if (this.mMSSObservers.size() != 0) {
            int i;
            int i2 = 0;
            if (this.mABSCurrentState == MSSState.ABSMIMO && this.mMSSCurrentState == MSSState.MSSSISO) {
                i = 0;
                while (i < this.mMSSLimitList.length) {
                    if (this.mMSSLimitList[i].equals(packageName)) {
                        for (IHwMSSObserver observer : this.mMSSObservers) {
                            observer.onMSSSwitchRequest(2);
                        }
                    } else {
                        i++;
                    }
                }
            }
            while (true) {
                i = i2;
                if (i >= this.mHT40PkgList.length) {
                    break;
                } else if (this.mHT40PkgList[i].equals(packageName)) {
                    for (IHwMSSObserver observer2 : this.mMSSObservers) {
                        observer2.onHT40Request();
                    }
                } else {
                    i2 = i + 1;
                }
            }
        }
    }

    private boolean isIncompatibleBssid(String bssid) {
        if (bssid == null || bssid.length() < 8) {
            HwMSSUtils.log(1, TAG, "found incompatible vendor");
            return true;
        }
        for (String equals : this.mIncompatibleBssidList) {
            String equals2;
            if (equals2.equals(bssid.substring(0, 8))) {
                equals2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("found incompatible vendor: ");
                stringBuilder.append(bssid.substring(0, 8));
                HwMSSUtils.log(1, equals2, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    public void setSisoFixFlag(boolean value) {
        this.sisoFixFlag = value;
    }

    public boolean getSisoFixFlag() {
        return this.sisoFixFlag;
    }
}
