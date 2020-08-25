package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.wifi.HwHiSLog;
import com.android.server.hidata.wavemapping.statehandler.CollectUserFingersHandler;
import com.android.server.wifi.ABS.HwABSDetectorService;
import com.android.server.wifi.ABS.HwABSUtils;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.HwWiTas.HwWiTasMonitor;
import com.android.server.wifi.HwWiTas.HwWiTasStateMachine;
import com.android.server.wifi.HwWiTas.HwWiTasUtils;
import com.android.server.wifi.LAA.HwLaaController;
import com.android.server.wifi.LAA.HwLaaUtils;
import com.android.server.wifi.dc.DCMonitor;
import com.android.server.wifi.dc.DCUtils;
import com.android.server.wifi.fastsleep.FsArbitration;
import com.android.server.wifi.hwcoex.HiCoexManagerImpl;
import com.android.server.wifi.rxlisten.RxListenArbitration;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.wifipro.HwWifiProServiceManager;
import com.huawei.hwwifiproservice.MessageUtil;

public class HwWifiController extends WifiController {
    private static final int BASE = 155648;
    private static final int CMD_AUTO_CONNECTION_MODE_CHANGED = 155748;
    private Context mContext;
    private HwWifiDataTrafficTracking mHwWifiDataTrafficTracking;
    private HwWifiProServiceManager mHwWifiProServiceManager;
    HwWifiSettingsStoreEx mSettingsStoreEx;
    ClientModeImpl mWifiStateMachine;

    public /* bridge */ /* synthetic */ void start() {
        HwWifiController.super.start();
    }

    public HwWifiController(Context context, ClientModeImpl wsm, Looper wifiStateMachineLooper, WifiSettingsStore wss, Looper wifiServiceLooper, FrameworkFacade f, ActiveModeWarden wsmp, WifiPermissionsUtil wifiPermissionsUtil) {
        super(context, wsm, wifiStateMachineLooper, wss, wifiServiceLooper, f, wsmp, wifiPermissionsUtil);
        this.mWifiStateMachine = wsm;
        this.mContext = context;
        this.mSettingsStoreEx = new HwWifiSettingsStoreEx(context);
        registerForConnectModeChange();
        this.mHwWifiDataTrafficTracking = new HwWifiDataTrafficTracking(this.mContext, wifiServiceLooper);
        this.mHwWifiProServiceManager = HwWifiProServiceManager.createHwWifiProServiceManager(context);
    }

    private void registerForConnectModeChange() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MessageUtil.WIFI_CONNECT_TYPE), false, new ContentObserver(null) {
            /* class com.android.server.wifi.HwWifiController.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                HwWifiController.this.mSettingsStoreEx.handleWifiAutoConnectChanged();
                HwWifiController hwWifiController = HwWifiController.this;
                hwWifiController.sendMessage(HwWifiController.CMD_AUTO_CONNECTION_MODE_CHANGED, hwWifiController.mSettingsStoreEx.isAutoConnectionEnabled() ? 1 : 0);
            }
        });
    }

    /* access modifiers changed from: protected */
    public boolean processDefaultState(Message message) {
        if (message.what != CMD_AUTO_CONNECTION_MODE_CHANGED) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean processStaEnabled(Message message) {
        if (message.what != CMD_AUTO_CONNECTION_MODE_CHANGED) {
            return false;
        }
        int i = message.arg1;
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean setOperationalModeByMode() {
        HwWifiSettingsStoreEx hwWifiSettingsStoreEx = this.mSettingsStoreEx;
        if (hwWifiSettingsStoreEx == null) {
            HwHiSLog.d("HwWifiController", false, "setOperationalModeByMode mSettingsStoreEx = null", new Object[0]);
            return false;
        } else if (hwWifiSettingsStoreEx == null || hwWifiSettingsStoreEx.isAutoConnectionEnabled()) {
            return false;
        } else {
            return true;
        }
    }

    public void createWifiProStateMachine(Context context, Messenger messenger) {
        if (messenger != null) {
            this.mHwWifiProServiceManager.createHwWifiProService(context);
        }
    }

    public void startWifiDataTrafficTrack() {
        this.mHwWifiDataTrafficTracking.startTrack();
    }

    public void stopWifiDataTrafficTrack() {
        this.mHwWifiDataTrafficTracking.stopTrack();
    }

    public boolean isWifiRepeaterStarted() {
        return 1 == Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_repeater_on", 0);
    }

    public void createABSService(Context context, ClientModeImpl wifiStateMachine) {
        HwHiSLog.d("HwWifiController", false, "createABSService", new Object[0]);
        if (HwABSUtils.getABSEnable()) {
            HwABSDetectorService.createHwABSDetectorService(this.mContext, wifiStateMachine);
        }
    }

    public void createQoEEngineService(Context context, ClientModeImpl wifiStateMachine) {
        HwHiSLog.d("HwQoEService", false, "createQoEService", new Object[0]);
        HwQoEService.createHwQoEService(context, wifiStateMachine);
        if (HwLaaUtils.isLaaPlusEnable()) {
            HwLaaController.createHwLaaController(context);
        }
    }

    public void updateWMUserAction(Context context, String action, String apkname) {
        CollectUserFingersHandler collectUserFingersHandler;
        HwHiSLog.d("WMapping.CollectUserFingersHandler.", false, "updateUserAction", new Object[0]);
        if (("android.uid.system:1000".equals(apkname) || "com.android.settings".equals(apkname) || "com.android.systemui".equals(apkname)) && (collectUserFingersHandler = CollectUserFingersHandler.getInstance()) != null) {
            collectUserFingersHandler.updateUserAction(action, apkname);
        }
    }

    public void createWiTasService(Context context, WifiNative wifiNative) {
        if (HwWiTasUtils.getWiTasEnable()) {
            HwHiSLog.d(HwWiTasUtils.TAG, false, "createWiTasService", new Object[0]);
            HwWiTasStateMachine.createWiTasStateMachine(context, wifiNative);
        }
    }

    public void reportWiTasAntRssi(int index, int rssi) {
        HwWiTasMonitor witasMonitor = HwWiTasMonitor.getInstance();
        if (witasMonitor != null) {
            witasMonitor.reportAntRssi(index, rssi);
        } else {
            HwHiSLog.w(HwWiTasUtils.TAG, false, "witasMonitor is null", new Object[0]);
        }
    }

    public void createHiCoexService(Context context, WifiNative wifiNative) {
        HiCoexManagerImpl.createHiCoexManager(context, wifiNative);
    }

    public void createDCService(Context context) {
        if (DCUtils.isDcSupported()) {
            HwHiSLog.d("DCMonitor", false, "createDCService", new Object[0]);
            DCMonitor.createDCMonitor(context);
        }
    }

    public void createFastSleepService(Context context, WifiNative wifiNative) {
        FsArbitration.createFsArbitration(context, wifiNative);
        RxListenArbitration.createRxListenArbitration(context, wifiNative);
    }
}
