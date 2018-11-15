package com.android.server.hidata.wavemapping.statehandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.os.Bundle;
import android.os.Handler;
import com.android.server.hidata.arbitration.HwArbitrationFunction;
import com.android.server.hidata.arbitration.HwArbitrationHistoryQoeManager;
import com.android.server.hidata.wavemapping.HwWaveMappingManager;
import com.android.server.hidata.wavemapping.IWaveMappingCallback;
import com.android.server.hidata.wavemapping.chr.QueryHistAppQoeService;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ContextManager;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.FastBack2LteChrDAO;
import com.android.server.hidata.wavemapping.dao.HisQoEChrDAO;
import com.android.server.hidata.wavemapping.dao.LocationDAO;
import com.android.server.hidata.wavemapping.dao.SpaceUserDAO;
import com.android.server.hidata.wavemapping.entity.HwWmpAppInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RecognizeResult;
import com.android.server.hidata.wavemapping.entity.SpaceExpInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.NetUtil;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.security.trustcircle.utils.LogHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class CollectUserFingersHandler {
    private static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";
    private static final int EVENT_UPDATE_SOURCE_NETWORK = 0;
    private static final int RESTARTMAX = 10;
    private static final String TAG;
    private static Bundle lastCellState = new Bundle();
    private static Bundle lastWifiState = new Bundle();
    private static CollectUserFingersHandler mCollectUserFingersHandler = null;
    private boolean back4g_begin = true;
    private int back4g_restartCnt = 0;
    private boolean connectedMobile = false;
    private boolean connectedWifi = false;
    private String freqLoc = Constant.NAME_FREQLOCATION_OTHER;
    private Runnable getRegStateAfterPeriodHandler = new Runnable() {
        public void run() {
            String nwName = "UNKNOWN";
            Bundle cellInfo = NetUtil.getMobileDataState(CollectUserFingersHandler.this.mContext);
            LogUtil.i("getRegStateAfterPeriodHandler");
            nwName = cellInfo.getString("cellRAT");
            if (nwName != null && nwName.equals("4G")) {
                CollectUserFingersHandler.this.mFastBack2LteChrDAO.addsuccessBack();
            }
            CollectUserFingersHandler.this.mFastBack2LteChrDAO.insertRecordByLoc();
        }
    };
    private Handler handler = new Handler();
    private boolean isDestNetworkCellular = false;
    private int locBatch = 0;
    private boolean mCHRUserPrefAutoSwitch = false;
    private long mCHRUserPrefAutoSwitchTime = 0;
    private boolean mCHRUserPrefDetermineSwitch = false;
    private boolean mCHRUserPrefManualSwitch = false;
    private long mCHRUserPrefManualSwitchTime = 0;
    private String mCHRUserPrefOriginalNWName = "UNKNOWN";
    private int mCHRUserPrefOriginalNWType = 8;
    private String mCHRUserPrefSwitchNWName = "UNKNOWN";
    private int mCHRUserPrefSwitchNWType = 8;
    private long mCHRUserPrefSwitchTime = 0;
    private Context mContext;
    private String mDestNetworkFreq = "UNKNOWN";
    private String mDestNetworkId = "UNKNOWN";
    private String mDestNetworkName = "UNKNOWN";
    private int mDestNetworkType = 8;
    private FastBack2LteChrDAO mFastBack2LteChrDAO;
    private HwWmpFastBackLteManager mFastBackLteMgr = null;
    private HisQoEChrDAO mHisQoEChrDAO;
    private QueryHistAppQoeService mHisQoEChrService;
    private HwArbitrationHistoryQoeManager mHistoryQoeMgr = null;
    private LocationDAO mLocationDAO;
    private Handler mMachineHandler;
    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                LogUtil.w(" action is null");
                return;
            }
            Object obj = -1;
            int hashCode = action.hashCode();
            if (hashCode != -1172645946) {
                if (hashCode != -229777127) {
                    if (hashCode != 233521600) {
                        if (hashCode == 1067187475 && action.equals("com.android.server.hidata.arbitration.HwArbitrationStateMachine")) {
                            obj = 2;
                        }
                    } else if (action.equals("android.net.wifi.supplicant.STATE_CHANGE")) {
                        obj = 1;
                    }
                } else if (action.equals(CollectUserFingersHandler.ACTION_SIM_STATE_CHANGED)) {
                    obj = 3;
                }
            } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                obj = null;
            }
            switch (obj) {
                case null:
                    CollectUserFingersHandler.this.mMachineHandler.sendEmptyMessage(91);
                    LogUtil.i("Connectivity state changes");
                    break;
                case 1:
                    if (intent.getParcelableExtra("newState") != null && (intent.getParcelableExtra("newState") instanceof SupplicantState)) {
                        SupplicantState mCurrentsupplicantState = (SupplicantState) intent.getParcelableExtra("newState");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SUPPLICANT_STATE_CHANGED_ACTION mCurrentsupplicantState = ");
                        stringBuilder.append(mCurrentsupplicantState);
                        LogUtil.i(stringBuilder.toString());
                        if (mCurrentsupplicantState == SupplicantState.COMPLETED) {
                            CollectUserFingersHandler.this.mMachineHandler.sendEmptyMessage(92);
                            break;
                        }
                    }
                    break;
                case 2:
                    CollectUserFingersHandler.this.mMachineHandler.sendEmptyMessage(91);
                    LogUtil.i("MP-LINK state changes");
                    break;
                case 3:
                    CollectUserFingersHandler.this.setCurrScrbId();
                    LogUtil.i("SIM state changes");
                    break;
            }
        }
    };
    private String mPrefNetworkName = "UNKNOWN";
    private int mPrefNetworkType = 8;
    private String mSourceNetworkFreq = "UNKNOWN";
    private String mSourceNetworkId = "UNKNOWN";
    private String mSourceNetworkName = "UNKNOWN";
    private int mSourceNetworkType = 8;
    private HashMap<String, SpaceExpInfo> mSpaceExperience = new HashMap();
    private SpaceUserDAO mSpaceUserDAO;
    private StringBuilder mSpaceid = new StringBuilder("0");
    private StringBuilder mSpaceid_mainAp = new StringBuilder("0");
    private boolean mUserHasPref = false;
    private boolean mUserOperation = false;
    private Map<String, String> mapCollectFileName = new HashMap();
    private String oldBssid = "UNKNOWN";
    private String oldCellId = "UNKNOWN";
    private String oldRat = "UNKNOWN";
    private String oldSsid = "UNKNOWN";
    private long outof4G_begin = 0;
    private ParameterInfo param;
    private Runnable periodicOutof4GHandler = new Runnable() {
        public void run() {
            LogUtil.i("periodically check no 4G coverage");
            CollectUserFingersHandler.this.checkOutOf4GCoverage(false);
        }
    };
    private HashMap<String, HwWmpAppInfo> saveAppInfo = new HashMap();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(CollectUserFingersHandler.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public CollectUserFingersHandler(Handler handler) {
        LogUtil.i(" ,new CollectUserFingersHandler ");
        try {
            this.mContext = ContextManager.getInstance().getContext();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.getRawDataPath());
            stringBuilder.append("network");
            stringBuilder.append(Constant.RAW_FILE_WIFIPRO_EXTENSION);
            this.mapCollectFileName.put("wifipro", stringBuilder.toString());
            this.param = ParamManager.getInstance().getParameterInfo();
            this.mFastBackLteMgr = HwWmpFastBackLteManager.getInstance();
            this.mSpaceUserDAO = new SpaceUserDAO();
            this.mLocationDAO = new LocationDAO();
            this.mHisQoEChrDAO = new HisQoEChrDAO();
            this.mHisQoEChrService = QueryHistAppQoeService.getInstance();
            this.mFastBack2LteChrDAO = new FastBack2LteChrDAO();
            this.mMachineHandler = handler;
            this.mHistoryQoeMgr = HwArbitrationHistoryQoeManager.getInstance(handler);
            startCollect();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(TAG);
            stringBuilder2.append(" CollectUserFingersHandler ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public static synchronized CollectUserFingersHandler getInstance(Handler handler) {
        CollectUserFingersHandler collectUserFingersHandler;
        synchronized (CollectUserFingersHandler.class) {
            if (mCollectUserFingersHandler == null) {
                mCollectUserFingersHandler = new CollectUserFingersHandler(handler);
            }
            collectUserFingersHandler = mCollectUserFingersHandler;
        }
        return collectUserFingersHandler;
    }

    public static synchronized CollectUserFingersHandler getInstance() {
        CollectUserFingersHandler collectUserFingersHandler;
        synchronized (CollectUserFingersHandler.class) {
            collectUserFingersHandler = mCollectUserFingersHandler;
        }
        return collectUserFingersHandler;
    }

    public void setFreqLocation(String location) {
        clearUserPrefCHR();
        if (!this.mFastBack2LteChrDAO.getLocation().equals(location)) {
            this.mFastBack2LteChrDAO.insertRecordByLoc();
            this.mFastBack2LteChrDAO.setLocation(location);
            if (!this.mFastBack2LteChrDAO.getCountersByLocation()) {
                this.mFastBack2LteChrDAO.insertRecordByLoc();
            }
        }
        if (!this.mHisQoEChrDAO.getLocation().equals(location)) {
            this.mHisQoEChrDAO.insertRecordByLoc();
            this.mHisQoEChrDAO.setLocation(location);
            this.mHisQoEChrDAO.getCountersByLocation();
        }
        this.freqLoc = location;
        this.mSpaceUserDAO.setFreqLocation(location);
    }

    public void setModelVer(String model_allAp, String model_mainAp) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" set Model version: ");
        stringBuilder.append(model_allAp);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(model_mainAp);
        LogUtil.i(stringBuilder.toString());
        this.mSpaceUserDAO.setModelVer(model_allAp, model_mainAp);
    }

    public void setCurrScrbId() {
        this.mSpaceUserDAO.setScrbId(NetUtil.getMobileDataScrbId(this.mContext));
    }

    public void setBatch(int batch) {
        this.locBatch = batch;
    }

    public final void startCollect() {
        try {
            LogUtil.i(" startCollect ");
            setCurrScrbId();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            filter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
            filter.addAction(ACTION_SIM_STATE_CHANGED);
            filter.addAction("com.android.server.hidata.arbitration.HwArbitrationStateMachine");
            this.mContext.registerReceiver(this.mNetworkReceiver, filter);
            int networkType = NetUtil.getNetworkTypeInfo(this.mContext);
            if (1 == networkType) {
                setNewStartTime(new HwWmpAppInfo(1));
                LogUtil.d(" Wifi active ");
                backupWifiInfo();
            } else {
                resetTime(Constant.USERDB_APP_NAME_WIFI);
            }
            if (networkType == 0) {
                setNewStartTime(new HwWmpAppInfo(0));
                LogUtil.d(" Mobile active ");
                backupCellInfo();
            } else {
                resetTime(Constant.USERDB_APP_NAME_MOBILE);
            }
            checkOutOf4GCoverage(false);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" startCollect ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public void updateSourceNetwork() {
        try {
            Bundle connectivity = NetUtil.getConnectedNetworkState(this.mContext);
            String networkId = "UNKNOWN";
            String networkName = "UNKNOWN";
            String networkFreq = "UNKNOWN";
            int defaultType = connectivity.getInt("defaultType");
            if (this.isDestNetworkCellular && defaultType == 0) {
                LogUtil.i("Disable WIFI without connection ");
                return;
            }
            if (1 == defaultType || defaultType == 0) {
                networkId = connectivity.getString("defaultNwId", "UNKNOWN");
                networkName = connectivity.getString("defaultNwName", "UNKNOWN");
                networkFreq = connectivity.getString("defaultNwFreq", "UNKNOWN");
            }
            this.mSourceNetworkId = networkId;
            this.mSourceNetworkName = networkName;
            this.mSourceNetworkFreq = networkFreq;
            this.mSourceNetworkType = defaultType;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" updateSourceNetwork: mSourceNetworkId: ");
            stringBuilder.append(this.mSourceNetworkId);
            stringBuilder.append(" mSourceNetworkName");
            stringBuilder.append(this.mSourceNetworkName);
            LogUtil.v(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" updateSourceNetwork ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public void updateUserAction(String action, String apkname) {
        try {
            this.isDestNetworkCellular = action.equals("ACTION_ENABLE_WIFI_FALSE");
            if (!this.mUserOperation) {
                this.mUserOperation = true;
                this.mMachineHandler.sendEmptyMessage(115);
            }
            this.mDestNetworkId = "UNKNOWN";
            this.mDestNetworkName = "UNKNOWN";
            this.mDestNetworkFreq = "UNKNOWN";
            this.mDestNetworkType = 8;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" updateUserAction: action: ");
            stringBuilder.append(action);
            stringBuilder.append(" apkname:");
            stringBuilder.append(apkname);
            stringBuilder.append(" isDestNetworkCellular:");
            stringBuilder.append(this.isDestNetworkCellular);
            LogUtil.d(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" updateUserAction ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    private void updateUserPreference() {
        SpaceExpInfo sourceNetworkSpaceInfo = null;
        SpaceExpInfo destNetworkSpaceInfo = null;
        try {
            StringBuilder stringBuilder;
            if (this.mSourceNetworkName.equals(this.mDestNetworkName)) {
                this.mUserOperation = false;
                LogUtil.i("mSourceNetworkName = mDestNetworkName, mUserOperation = false");
            }
            if (this.mUserOperation) {
                if (!(this.mSourceNetworkId.equals("UNKNOWN") && this.mDestNetworkId.equals("UNKNOWN"))) {
                    if (this.mSpaceExperience.containsKey(this.mSourceNetworkId)) {
                        sourceNetworkSpaceInfo = (SpaceExpInfo) this.mSpaceExperience.get(this.mSourceNetworkId);
                    }
                    if (sourceNetworkSpaceInfo == null) {
                        sourceNetworkSpaceInfo = new SpaceExpInfo(this.mSpaceid, this.mSpaceid_mainAp, this.mSourceNetworkId, this.mSourceNetworkName, this.mSourceNetworkFreq, this.mSourceNetworkType);
                    }
                    sourceNetworkSpaceInfo.accUserPrefOptOut();
                    this.mSpaceExperience.put(this.mSourceNetworkId, sourceNetworkSpaceInfo);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sourceNetworkSpaceInfo: ");
                    stringBuilder.append(sourceNetworkSpaceInfo.toString());
                    LogUtil.i(stringBuilder.toString());
                    if (this.mSpaceExperience.containsKey(this.mDestNetworkId)) {
                        destNetworkSpaceInfo = (SpaceExpInfo) this.mSpaceExperience.get(this.mDestNetworkId);
                    }
                    if (destNetworkSpaceInfo == null) {
                        destNetworkSpaceInfo = new SpaceExpInfo(this.mSpaceid, this.mSpaceid_mainAp, this.mDestNetworkId, this.mDestNetworkName, this.mDestNetworkFreq, this.mDestNetworkType);
                    }
                    destNetworkSpaceInfo.accUserPrefOptIn();
                    this.mSpaceExperience.put(this.mDestNetworkId, destNetworkSpaceInfo);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("destNetworkSpaceInfo: ");
                    stringBuilder.append(destNetworkSpaceInfo.toString());
                    LogUtil.i(stringBuilder.toString());
                }
            } else if (this.mSpaceExperience.size() != 0) {
                SpaceExpInfo maxEntry = null;
                for (Entry<String, SpaceExpInfo> entry : this.mSpaceExperience.entrySet()) {
                    SpaceExpInfo value = (SpaceExpInfo) entry.getValue();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" mSpaceExperience:");
                    stringBuilder2.append(value.toString());
                    LogUtil.i(stringBuilder2.toString());
                    if (maxEntry == null || maxEntry.getDuration() < value.getDuration()) {
                        maxEntry = value;
                    }
                }
                if (!(maxEntry == null || maxEntry.getDuration() <= 0 || maxEntry.getNetworkId().equals("UNKNOWN"))) {
                    maxEntry.accUserPrefStay();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("No manual operation, USER_PREF_STAY Network: ");
                    stringBuilder3.append(maxEntry.toString());
                    LogUtil.i(stringBuilder3.toString());
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateUserPreference record size: ");
            stringBuilder.append(this.mSpaceExperience.size());
            LogUtil.i(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(" updateUserPreference ");
            stringBuilder4.append(e.getMessage());
            LogUtil.e(stringBuilder4.toString());
        }
    }

    private void clearUserPreference() {
        this.mUserOperation = false;
        this.mSourceNetworkId = "UNKNOWN";
        this.mSourceNetworkName = "UNKNOWN";
        this.mSourceNetworkFreq = "UNKNOWN";
        this.mSourceNetworkType = 8;
        this.mDestNetworkId = "UNKNOWN";
        this.mDestNetworkName = "UNKNOWN";
        this.mDestNetworkFreq = "UNKNOWN";
        this.mDestNetworkType = 8;
        this.isDestNetworkCellular = false;
        this.mUserHasPref = false;
        this.mPrefNetworkName = "UNKNOWN";
        this.mPrefNetworkType = 8;
    }

    public void startAppCollect(HwWmpAppInfo appInfo) {
        LogUtil.i(" startAppCollect ");
        StringBuilder stringBuilder;
        try {
            String app = appInfo.getAppName();
            if (app == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" not handle this APP:");
                stringBuilder.append(appInfo.getScenceId());
                LogUtil.i(stringBuilder.toString());
                return;
            }
            setNewStartTime(appInfo);
            stringBuilder = new StringBuilder();
            stringBuilder.append(" startAppCollect, scenes:");
            stringBuilder.append(appInfo.getScenceId());
            stringBuilder.append(", app:");
            stringBuilder.append(app);
            stringBuilder.append(", appNwType:");
            stringBuilder.append(appInfo.getConMgrNetworkType());
            LogUtil.d(stringBuilder.toString());
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" startAppCollect ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public void endAppCollect(HwWmpAppInfo appInfo) {
        LogUtil.i(" endAppCollect ");
        StringBuilder stringBuilder;
        try {
            String app = appInfo.getAppName();
            if (app == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" APP is null: scenesId=");
                stringBuilder.append(appInfo.getScenceId());
                LogUtil.i(stringBuilder.toString());
            } else if (this.saveAppInfo.isEmpty()) {
                LogUtil.w(" no saved APP");
            } else if (this.saveAppInfo.containsKey(app)) {
                int network = appInfo.getConMgrNetworkType();
                Bundle state = getSpecifiedDataState(network);
                if (state.getBoolean("VALID")) {
                    String nwName = state.getString("NAME");
                    String nwId = state.getString("ID");
                    String nwFreq = state.getString("FREQ");
                    String signal = state.getString("SIGNAL");
                    int signalVal = 0;
                    if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                        signalVal = Integer.parseInt(signal.trim());
                    }
                    int signalVal2 = signalVal;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" endAppCollect, scenes:");
                    stringBuilder.append(appInfo.getScenceId());
                    stringBuilder.append(", app:");
                    stringBuilder.append(app);
                    LogUtil.d(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("              , appNwType:");
                    stringBuilder.append(network);
                    stringBuilder.append(", nwId:");
                    stringBuilder.append(nwId);
                    stringBuilder.append(", signal:");
                    stringBuilder.append(signalVal2);
                    LogUtil.v(stringBuilder.toString());
                    updateDurationbyNwId(app, nwId, nwName, nwFreq, network, signalVal2);
                    resetTime(appInfo.getAppName());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" endAppCollect, app:");
                    stringBuilder.append(app);
                    stringBuilder.append(", nwType:");
                    stringBuilder.append(appInfo.getConMgrNetworkType());
                    LogUtil.i(stringBuilder.toString());
                    return;
                }
                LogUtil.w(" network info is null");
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" no this APP: scenesId=");
                stringBuilder.append(appInfo.getScenceId());
                LogUtil.d(stringBuilder.toString());
            }
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" endAppCollect ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public void updateAppNetwork(String app, int newAppNw) {
        StringBuilder stringBuilder;
        Exception e;
        LogUtil.i(" updateAppNetwork ");
        if (app == null) {
            try {
                LogUtil.i(" APP is null");
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" updateAppNetwork ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        } else if (this.saveAppInfo.isEmpty()) {
            LogUtil.w(" no saved APP");
        } else if (this.saveAppInfo.containsKey(app)) {
            e2 = ((HwWmpAppInfo) this.saveAppInfo.get(app)).getConMgrNetworkType();
            if (newAppNw == e2) {
                LogUtil.d(" new == old, not to update");
                return;
            }
            Bundle state = getSpecifiedDataState(e2);
            if (state.getBoolean("VALID")) {
                String nwName = state.getString("NAME");
                String nwId = state.getString("ID");
                String nwFreq = state.getString("FREQ");
                String signal = state.getString("SIGNAL");
                int signalVal = 0;
                if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                    signalVal = Integer.parseInt(signal.trim());
                }
                int signalVal2 = signalVal;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" updateAppNetwork, scenes:");
                stringBuilder.append(((HwWmpAppInfo) this.saveAppInfo.get(app)).getScenceId());
                stringBuilder.append(", app:");
                stringBuilder.append(app);
                LogUtil.d(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("                 , app:");
                stringBuilder.append(app);
                stringBuilder.append(", old appNwType:");
                stringBuilder.append(e2);
                stringBuilder.append(", nwId:");
                stringBuilder.append(nwId);
                stringBuilder.append(", new appNwType:");
                stringBuilder.append(newAppNw);
                stringBuilder.append(", signal:");
                stringBuilder.append(signalVal2);
                LogUtil.v(stringBuilder.toString());
                updateDurationbyNwId(app, nwId, nwName, nwFreq, e2, signalVal2);
                updateStartTime(app, newAppNw);
                return;
            }
            LogUtil.w(" old network info is null");
            updateStartTime(app, newAppNw);
        } else {
            LogUtil.d(" no this APP in containsKey");
        }
    }

    public void updateAppQoE(String app, int levelQoE) {
        String str = app;
        int i = levelQoE;
        LogUtil.i(" updateAppQoE ");
        Exception e = null;
        if (str == null) {
            try {
                LogUtil.i(" APP is null");
            } catch (Exception e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" updateAppQoE ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        } else if (this.saveAppInfo.isEmpty()) {
            LogUtil.w(" no saved APP");
        } else if (this.saveAppInfo.containsKey(str)) {
            int network = ((HwWmpAppInfo) this.saveAppInfo.get(str)).getConMgrNetworkType();
            Bundle state = getSpecifiedDataState(network);
            if (state.getBoolean("VALID")) {
                int signalVal;
                String nwName = state.getString("NAME");
                String nwId = state.getString("ID");
                String nwFreq = state.getString("FREQ");
                String signal = state.getString("SIGNAL");
                if (this.mSpaceExperience.containsKey(nwId)) {
                    e2 = (SpaceExpInfo) this.mSpaceExperience.get(nwId);
                }
                if (e2 == null) {
                    e2 = new SpaceExpInfo(this.mSpaceid, this.mSpaceid_mainAp, nwId, nwName, nwFreq, network);
                }
                if (i == 1) {
                    e2.accAppPoor(str);
                }
                if (i == 2) {
                    e2.accAppGood(str);
                }
                if (signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN")) {
                    signalVal = 0;
                } else {
                    signalVal = Integer.parseInt(signal.trim());
                    e2.accSignalValue(signalVal);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" updateAppQoE, app:");
                stringBuilder2.append(str);
                stringBuilder2.append(", level:");
                stringBuilder2.append(i);
                stringBuilder2.append(", poor count=");
                stringBuilder2.append(e2.getAppQoePoor(str));
                stringBuilder2.append(", good count=");
                stringBuilder2.append(e2.getAppQoeGood(str));
                stringBuilder2.append(", signal:");
                stringBuilder2.append(signalVal);
                LogUtil.d(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("              nwId:");
                stringBuilder2.append(nwId);
                LogUtil.v(stringBuilder2.toString());
                this.mSpaceExperience.put(nwId, e2);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" updateAppQoE record size: ");
                stringBuilder2.append(this.mSpaceExperience.size());
                stringBuilder2.append("spaceInfo:");
                stringBuilder2.append(e2.toString());
                LogUtil.i(stringBuilder2.toString());
                return;
            }
            LogUtil.w(" network info is null");
        } else {
            LogUtil.d(" no this APP");
        }
    }

    public void updateWifiDurationForAp(boolean checkChanges) {
        LogUtil.i(" updateWifiDurationForAp ");
        StringBuilder stringBuilder;
        try {
            Bundle wifiInfo = NetUtil.getWifiStateString(this.mContext);
            String newBssid = wifiInfo.getString("wifiMAC", "UNKNOWN");
            String newSsid = wifiInfo.getString("wifiAp", "UNKNOWN");
            String newState = wifiInfo.getString("wifiState", "UNKNOWN");
            if (newBssid == null) {
                newBssid = "UNKNOWN";
            }
            String newBssid2 = newBssid;
            newBssid = lastWifiState.getString("wifiMAC", "UNKNOWN");
            String oldSsid = lastWifiState.getString("wifiAp", "UNKNOWN");
            String oldFreq = lastWifiState.getString("wifiCh", "UNKNOWN");
            String signal = lastWifiState.getString("wifiRssi", "0");
            if (newBssid == null) {
                newBssid = "UNKNOWN";
            }
            String oldBssid = newBssid;
            if (newSsid.equals(oldSsid) && newBssid2.equals(oldBssid)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" wifi id NOT changed:");
                stringBuilder.append(newSsid);
                LogUtil.i(stringBuilder.toString());
                if (checkChanges) {
                    return;
                }
            }
            LogUtil.d(" wifi id changed");
            stringBuilder = new StringBuilder();
            stringBuilder.append("                , new=");
            stringBuilder.append(newSsid);
            stringBuilder.append(LogHelper.SEPARATOR);
            stringBuilder.append(newBssid2);
            stringBuilder.append(" , old=");
            stringBuilder.append(oldSsid);
            stringBuilder.append(LogHelper.SEPARATOR);
            stringBuilder.append(oldBssid);
            LogUtil.i(stringBuilder.toString());
            backupWifiInfo();
            int signalVal = 0;
            if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                signalVal = Integer.parseInt(signal.trim());
            }
            int signalVal2 = signalVal;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateWifiDurationForAp, save to :");
            stringBuilder.append(oldSsid);
            LogUtil.i(stringBuilder.toString());
            if (!this.saveAppInfo.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("                   , saveAppInfo.size=");
                stringBuilder.append(this.saveAppInfo.size());
                LogUtil.i(stringBuilder.toString());
                Iterator it = this.saveAppInfo.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, HwWmpAppInfo> entry = (Entry) it.next();
                    String app = (String) entry.getKey();
                    HwWmpAppInfo info = (HwWmpAppInfo) entry.getValue();
                    int nwType = info.getConMgrNetworkType();
                    stringBuilder = new StringBuilder();
                    Bundle wifiInfo2 = wifiInfo;
                    stringBuilder.append(" saveAppInfo, app: ");
                    stringBuilder.append(info.getAppName());
                    stringBuilder.append(", network=");
                    stringBuilder.append(nwType);
                    stringBuilder.append(", startTime=");
                    Iterator it2 = it;
                    stringBuilder.append(info.getStartTime());
                    LogUtil.d(stringBuilder.toString());
                    if (nwType != 0) {
                        updateDurationbyNwId(app, oldBssid, oldSsid, oldFreq, nwType, signalVal2);
                    }
                    wifiInfo = wifiInfo2;
                    it = it2;
                }
            }
            if (!newState.equals("ENABLED") || newBssid2.contains("UNKNOWN")) {
                LogUtil.d(" current wifi == null");
                resetTime(Constant.USERDB_APP_NAME_WIFI);
            }
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateWifiDurationByAp ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private void updateWifiDurationEnd() {
        StringBuilder stringBuilder;
        LogUtil.i(" updateWifiDurationEnd ");
        try {
            String oldBssid = lastWifiState.getString("wifiMAC", "UNKNOWN");
            String oldSsid = lastWifiState.getString("wifiAp", "UNKNOWN");
            String oldFreq = lastWifiState.getString("wifiCh", "UNKNOWN");
            String signal = lastWifiState.getString("wifiRssi", "0");
            if (oldBssid == null) {
                oldBssid = "UNKNOWN";
            }
            int signalVal = 0;
            if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                signalVal = Integer.parseInt(signal.trim());
            }
            int signalVal2 = signalVal;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" updateWifiDurationEnd, save to : ");
            stringBuilder2.append(oldSsid);
            LogUtil.i(stringBuilder2.toString());
            if (!this.saveAppInfo.isEmpty()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("                   , saveAppInfo.size=");
                stringBuilder2.append(this.saveAppInfo.size());
                LogUtil.i(stringBuilder2.toString());
                for (Entry<String, HwWmpAppInfo> entry : this.saveAppInfo.entrySet()) {
                    String app = (String) entry.getKey();
                    HwWmpAppInfo info = (HwWmpAppInfo) entry.getValue();
                    int nwType = info.getConMgrNetworkType();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" saveAppInfo, app: ");
                    stringBuilder2.append(info.getAppName());
                    stringBuilder2.append(", network=");
                    stringBuilder2.append(nwType);
                    stringBuilder2.append(", startTime=");
                    stringBuilder2.append(info.getStartTime());
                    LogUtil.d(stringBuilder2.toString());
                    if (nwType != 0) {
                        updateDurationbyNwId(app, oldBssid, oldSsid, oldFreq, nwType, signalVal2);
                    }
                }
            }
            resetTime(Constant.USERDB_APP_NAME_WIFI);
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateWifiDurationEnd, RuntimeException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateWifiDurationEnd ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private void backupWifiInfo() {
        StringBuilder stringBuilder;
        LogUtil.i(" backupWifiInfo ");
        try {
            Bundle wifiInfo = NetUtil.getWifiStateString(this.mContext);
            String newState = wifiInfo.getString("wifiState", "UNKNOWN");
            String newBssid = wifiInfo.getString("wifiMAC", "UNKNOWN");
            if (newState.equalsIgnoreCase("ENABLED") && newBssid != null && !newBssid.contains("UNKNOWN")) {
                lastWifiState = wifiInfo.deepCopy();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" wifi ENABLED, backup info: ssid=");
                stringBuilder2.append(lastWifiState.getString("wifiAp"));
                stringBuilder2.append(", ");
                stringBuilder2.append(lastWifiState.getString("wifiState"));
                LogUtil.i(stringBuilder2.toString());
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" backupWifiInfo, RuntimeException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" backupWifiInfo, e: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public void updateMobileDurationForCell(boolean checkChanges) {
        LogUtil.i(" updateMobileDurationForCell ");
        StringBuilder stringBuilder;
        try {
            Bundle cellInfo = NetUtil.getMobileDataState(this.mContext);
            String newRat = cellInfo.getString("cellRAT", "UNKNOWN");
            String newId = cellInfo.getString("cellId", "UNKNOWN");
            if (newId == null) {
                newId = "UNKNOWN";
            }
            String newId2 = newId;
            String oldRat = lastCellState.getString("cellRAT", "UNKNOWN");
            newId = lastCellState.getString("cellId", "UNKNOWN");
            String oldFreq = lastCellState.getString("cellFreq", "UNKNOWN");
            String signal = cellInfo.getString("cellRssi", "0");
            if (newId == null) {
                newId = "UNKNOWN";
            }
            String oldId = newId;
            if (checkChanges) {
                LogUtil.i(" check cell ID change ");
                if (newId2.equals(oldId) && newRat.equals(oldRat)) {
                    LogUtil.d(" cell id not changed ");
                    return;
                }
            }
            backupCellInfo();
            int signalVal = 0;
            if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                signalVal = Integer.parseInt(signal.trim());
            }
            int signalVal2 = signalVal;
            LogUtil.i("updateMobileDurationForCell, save to cellId: ");
            if (!this.saveAppInfo.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("                   , saveAppInfo.size=");
                stringBuilder.append(this.saveAppInfo.size());
                LogUtil.i(stringBuilder.toString());
                Iterator it = this.saveAppInfo.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, HwWmpAppInfo> entry = (Entry) it.next();
                    String app = (String) entry.getKey();
                    HwWmpAppInfo info = (HwWmpAppInfo) entry.getValue();
                    int nwType = info.getConMgrNetworkType();
                    stringBuilder = new StringBuilder();
                    Bundle cellInfo2 = cellInfo;
                    stringBuilder.append(" saveAppInfo, app: ");
                    stringBuilder.append(info.getAppName());
                    stringBuilder.append(", network=");
                    stringBuilder.append(nwType);
                    stringBuilder.append(", startTime=");
                    Iterator it2 = it;
                    stringBuilder.append(info.getStartTime());
                    LogUtil.d(stringBuilder.toString());
                    if (1 != nwType) {
                        updateDurationbyNwId(app, oldId, oldRat, oldFreq, nwType, signalVal2);
                    }
                    cellInfo = cellInfo2;
                    it = it2;
                }
            }
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateMobileDurationForCell ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private void updateMobileDurationEnd() {
        LogUtil.i(" updateMobileDurationEnd ");
        try {
            Bundle cellInfo = NetUtil.getMobileDataState(this.mContext);
            String oldRat = lastCellState.getString("cellRAT", "UNKNOWN");
            String oldId = lastCellState.getString("cellId", "UNKNOWN");
            String oldFreq = lastCellState.getString("cellFreq", "UNKNOWN");
            String signal = cellInfo.getString("cellRssi", "0");
            if (oldId == null) {
                oldId = "UNKNOWN";
            }
            int signalVal = 0;
            if (!(signal == null || signal.equalsIgnoreCase("") || signal.equalsIgnoreCase("UNKNOWN"))) {
                signalVal = Integer.parseInt(signal.trim());
            }
            LogUtil.i("updateMobileDurationEnd, save to cellId: ");
            if (!this.saveAppInfo.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("                   , saveAppInfo.size=");
                stringBuilder.append(this.saveAppInfo.size());
                LogUtil.i(stringBuilder.toString());
                for (Entry<String, HwWmpAppInfo> entry : this.saveAppInfo.entrySet()) {
                    String app = (String) entry.getKey();
                    HwWmpAppInfo info = (HwWmpAppInfo) entry.getValue();
                    int nwType = info.getConMgrNetworkType();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" saveAppInfo, app: ");
                    stringBuilder.append(info.getAppName());
                    stringBuilder.append(", network=");
                    stringBuilder.append(nwType);
                    stringBuilder.append(", startTime=");
                    stringBuilder.append(info.getStartTime());
                    LogUtil.d(stringBuilder.toString());
                    if (1 != nwType) {
                        updateDurationbyNwId(app, oldId, oldRat, oldFreq, nwType, signalVal);
                    }
                }
            }
            resetTime(Constant.USERDB_APP_NAME_MOBILE);
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" updateMobileDurationEnd ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    private void backupCellInfo() {
        StringBuilder stringBuilder;
        LogUtil.i(" backupCellInfo ");
        try {
            Bundle cellInfo = NetUtil.getMobileDataState(this.mContext);
            String cellState = cellInfo.getString("cellState", "UNKNOWN");
            String cellId = cellInfo.getString("cellId", "UNKNOWN");
            if (cellState.equalsIgnoreCase("ENABLED") && cellId != null && !cellId.contains("UNKNOWN")) {
                lastCellState = cellInfo.deepCopy();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" cell ENABLED, backup info: cellRat=");
                stringBuilder2.append(lastCellState.getString("cellRAT"));
                stringBuilder2.append(", ");
                stringBuilder2.append(lastCellState.getString("cellState"));
                LogUtil.i(stringBuilder2.toString());
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" backupCellInfo, RuntimeException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" backupCellInfo, e: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private void updateDurationbyNwId(String app, String newId, String newNwName, String newNwFreq, int nw_type, int signal) {
        Exception e;
        int i;
        StringBuilder stringBuilder;
        String str = app;
        String str2 = newNwName;
        LogUtil.i(" updateDurationbyNwId ");
        try {
            if (this.saveAppInfo.isEmpty()) {
                try {
                    LogUtil.w(" no saved APP");
                } catch (Exception e2) {
                    e = e2;
                    i = signal;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" updateDurationbyNwId ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
            } else if (this.saveAppInfo.get(str) == null) {
                LogUtil.d(" updateDurationbyNwId, no saved app ");
            } else if (newId == null || str2 == null || newNwFreq == null) {
                i = signal;
                try {
                    LogUtil.d(" updateDurationbyNwId, network==null ");
                } catch (Exception e3) {
                    e = e3;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" updateDurationbyNwId ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
            } else {
                String newId2 = this.freqLoc.equals(Constant.NAME_FREQLOCATION_OTHER) ? newNwFreq : newId;
                SpaceExpInfo spaceInfo = null;
                try {
                    if (this.mSpaceExperience.containsKey(newId2)) {
                        spaceInfo = (SpaceExpInfo) this.mSpaceExperience.get(newId2);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(" get old records:");
                        stringBuilder.append(spaceInfo.toString());
                        LogUtil.i(stringBuilder.toString());
                    }
                    if (spaceInfo == null) {
                        spaceInfo = new SpaceExpInfo(this.mSpaceid, this.mSpaceid_mainAp, newId2, str2, newNwFreq, nw_type);
                    }
                    long duration = System.currentTimeMillis() - ((HwWmpAppInfo) this.saveAppInfo.get(str)).getStartTime();
                    if (duration > 0) {
                        spaceInfo.accDuration(str, duration);
                    }
                    if (Constant.USERDB_APP_NAME_WIFI.equals(str) || Constant.USERDB_APP_NAME_MOBILE.equals(str)) {
                        long[] dataTraffic = NetUtil.getTraffic(((HwWmpAppInfo) this.saveAppInfo.get(str)).getStartTime(), System.currentTimeMillis(), nw_type, this.mContext);
                        spaceInfo.accDataTraffic(dataTraffic[0], dataTraffic[1]);
                    }
                    try {
                        spaceInfo.accSignalValue(signal);
                        this.mSpaceExperience.put(newId2, spaceInfo);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" updateDurationbyNwId record size: ");
                        stringBuilder2.append(this.mSpaceExperience.size());
                        stringBuilder2.append(", NW: ");
                        stringBuilder2.append(str2);
                        stringBuilder2.append(", spaceInfo:");
                        stringBuilder2.append(spaceInfo.toString());
                        LogUtil.i(stringBuilder2.toString());
                        ((HwWmpAppInfo) this.saveAppInfo.get(str)).setStartTime(System.currentTimeMillis());
                    } catch (Exception e4) {
                        e = e4;
                    }
                } catch (Exception e5) {
                    e = e5;
                    i = signal;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" updateDurationbyNwId ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
            }
        } catch (Exception e6) {
            e = e6;
            i = signal;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateDurationbyNwId ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private void saveSpaceExptoDatabaseNew(RecognizeResult results) {
        try {
            if (this.mSpaceExperience.size() != 0) {
                LogUtil.i(" loadSpaceExpfromDatabase");
                HashMap<String, SpaceExpInfo> mSavedSpaceExp = this.mSpaceUserDAO.findAllByTwoSpaces(results.getRgResult(), results.getMainApRgResult());
                mCombineSpaceExp = new HashMap[2];
                int num = 0;
                mCombineSpaceExp[0] = this.mSpaceExperience;
                mCombineSpaceExp[1] = mSavedSpaceExp;
                for (Entry entry : mergeSumOfMaps(mCombineSpaceExp).entrySet()) {
                    SpaceExpInfo val = (SpaceExpInfo) entry.getValue();
                    if (val != null) {
                        LogUtil.d(" saveSpaceExptoDatabaseNew:");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("                            Records");
                        stringBuilder.append(num);
                        stringBuilder.append(": ");
                        stringBuilder.append(val.toString());
                        LogUtil.i(stringBuilder.toString());
                        this.mSpaceUserDAO.insertBase(val);
                        this.mSpaceUserDAO.insertApp(val);
                        num++;
                    }
                }
                return;
            }
            LogUtil.d(" mSpaceExperience size=0");
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" saveSpaceExptoDatabaseNew ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    private HashMap<String, SpaceExpInfo> mergeSumOfMaps(HashMap<String, SpaceExpInfo>... maps) {
        HashMap<String, SpaceExpInfo> resultMap = new HashMap();
        for (HashMap<String, SpaceExpInfo> map : maps) {
            for (Entry<String, SpaceExpInfo> entry : map.entrySet()) {
                SpaceExpInfo value;
                String key = (String) entry.getKey();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" mergeSumOfMaps: nwId=");
                stringBuilder.append(key);
                LogUtil.v(stringBuilder.toString());
                if (resultMap.containsKey(key)) {
                    value = (SpaceExpInfo) entry.getValue();
                    value.mergeAllRecords((SpaceExpInfo) resultMap.get(key));
                } else {
                    value = (SpaceExpInfo) entry.getValue();
                }
                resultMap.put(key, value);
            }
        }
        return resultMap;
    }

    public void assignSpaceExp2Space(RecognizeResult result) {
        StringBuilder stringBuilder;
        Exception e;
        LogUtil.i(" assignSpaceExp2Space ");
        if (result == null) {
            try {
                LogUtil.i(" no result");
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        } else {
            String spaceId_mainAp;
            StringBuilder stringBuilder2;
            if (result.getRgResult() == null || result.getRgResult().contains(Constant.RESULT_UNKNOWN)) {
                e2 = "0";
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space: All AP result=");
                stringBuilder.append(result.getRgResult());
                LogUtil.i(stringBuilder.toString());
            } else {
                e2 = result.getRgResult();
            }
            if (result.getMainApRgResult() == null || result.getMainApRgResult().contains(Constant.RESULT_UNKNOWN)) {
                spaceId_mainAp = "0";
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" assignSpaceExp2Space: Main AP result=");
                stringBuilder2.append(result.getMainApRgResult());
                LogUtil.i(stringBuilder2.toString());
            } else {
                spaceId_mainAp = result.getMainApRgResult();
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" assignSpaceExp2Space: spaceId_allAp=");
            stringBuilder2.append(e2);
            stringBuilder2.append(", spaceId_mainAp=");
            stringBuilder2.append(spaceId_mainAp);
            stringBuilder2.append(", model ver=");
            stringBuilder2.append(result.getAllApModelName());
            stringBuilder2.append(Constant.RESULT_SEPERATE);
            stringBuilder2.append(result.getMainApModelName());
            LogUtil.d(stringBuilder2.toString());
            updateWifiDurationForAp(false);
            updateMobileDurationForCell(false);
            updateUserPreference();
            this.mSpaceid.setLength(0);
            this.mSpaceid.trimToSize();
            this.mSpaceid.append(e2);
            this.mSpaceid_mainAp.setLength(0);
            this.mSpaceid_mainAp.trimToSize();
            this.mSpaceid_mainAp.append(spaceId_mainAp);
            saveSpaceExptoDatabaseNew(result);
            this.mSpaceExperience.clear();
            clearUserPreference();
        }
    }

    public Bundle getUserPrefNetwork(String spaceId_allAp, String spaceId_mainAp) {
        String str = spaceId_allAp;
        String str2 = spaceId_mainAp;
        LogUtil.i(" getUserPrefNetwork ");
        Bundle output = new Bundle();
        output.putBoolean("isUserHasPref", false);
        output.putInt("prefNetworkType", 8);
        output.putString("prefNetworkName", "UNKNOWN");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" getUserPrefNetwork: spaceId_allAp=");
        stringBuilder.append(str);
        stringBuilder.append(", spaceId_mainAp=");
        stringBuilder.append(str2);
        LogUtil.d(stringBuilder.toString());
        try {
            if (str.equals("0") && str2.equals("0")) {
                LogUtil.d(" spaceId_allAp & spaceId_mainAP are all unknown");
                return output;
            } else if (!this.mUserOperation) {
                return calculateUserPreference(this.mSpaceUserDAO.findUserPrefByTwoSpaces(str, str2), false);
            } else {
                LogUtil.d(" mUserOperation = true");
                return output;
            }
        } catch (RuntimeException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getUserPrefNetwork RuntimeException,e:");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            LogUtil.d(" User has no preference");
            return output;
        } catch (Exception e2) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" getUserPrefNetwork, e:");
            stringBuilder3.append(e2.getMessage());
            LogUtil.e(stringBuilder3.toString());
            LogUtil.d(" User has no preference");
            return output;
        }
    }

    public Bundle getUserPrefNetwork(String spaceId_allAp) {
        StringBuilder stringBuilder;
        LogUtil.i(" getUserPrefNetwork, allAp Space ");
        Bundle output = new Bundle();
        output.putBoolean("isUserHasPref", false);
        output.putInt("prefNetworkType", 8);
        output.putString("prefNetworkName", "UNKNOWN");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" getUserPrefNetwork: spaceId_allAp=");
        stringBuilder2.append(spaceId_allAp);
        LogUtil.d(stringBuilder2.toString());
        try {
            if (spaceId_allAp.equals("0")) {
                LogUtil.d(" spaceId_allAp & spaceId_mainAP are all unknown");
                this.mLocationDAO.accCHRUserPrefUnknownSpacebyFreqLoc(this.freqLoc);
                return output;
            } else if (!this.mUserOperation) {
                return calculateUserPreference(this.mSpaceUserDAO.findUserPrefByAllApSpaces(spaceId_allAp), true);
            } else {
                LogUtil.d(" mUserOperation = true");
                return output;
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getUserPrefNetwork RuntimeException,e:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            LogUtil.d(" User has no preference");
            return output;
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" getUserPrefNetwork, e:");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            LogUtil.d(" User has no preference");
            return output;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x02e0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Bundle calculateUserPreference(HashMap<String, Bundle> currentSpaceExp, boolean needRecord) {
        RuntimeException e;
        Exception e2;
        StringBuilder stringBuilder;
        int inoutCount;
        int stayCount;
        Bundle result = new Bundle();
        Bundle maxEntry = null;
        long totalDuration = 0;
        int inoutCount2 = 0;
        int stayCount2 = 0;
        long cellDuration = 0;
        int cellUserPrefOptOut = 0;
        int cellUserPrefOptIn = 0;
        int cellUserPrefStay = 0;
        try {
            result.putBoolean("isUserHasPref", false);
            result.putInt("prefNetworkType", 8);
            result.putString("prefNetworkName", "UNKNOWN");
            if (currentSpaceExp.size() != 0) {
                Iterator it = currentSpaceExp.entrySet().iterator();
                while (it.hasNext()) {
                    Iterator it2;
                    StringBuilder stringBuilder2;
                    long totalDuration2;
                    Bundle value = (Bundle) ((Entry) it.next()).getValue();
                    totalDuration += value.getLong("duration_connected");
                    try {
                        it2 = it;
                        inoutCount2 = (value.getInt("user_pref_opt_in") + inoutCount2) + value.getInt("user_pref_opt_out");
                        stayCount2 += value.getInt("user_pref_stay");
                        if (value.getInt("networktype") == 0) {
                            cellDuration += value.getLong("duration_connected");
                            cellUserPrefOptOut += value.getInt("user_pref_opt_out");
                            cellUserPrefOptIn += value.getInt("user_pref_opt_in");
                            cellUserPrefStay += value.getInt("user_pref_stay");
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" Entry: Networkname:");
                        stringBuilder2.append(value.getString("networkname"));
                        stringBuilder2.append(" Type:");
                        stringBuilder2.append(value.getInt("networktype"));
                        stringBuilder2.append(" IN:");
                        stringBuilder2.append(value.getInt("user_pref_opt_in"));
                        stringBuilder2.append(" OUT:");
                        stringBuilder2.append(value.getInt("user_pref_opt_out"));
                        stringBuilder2.append(" STAY:");
                        stringBuilder2.append(value.getInt("user_pref_stay"));
                        stringBuilder2.append(" duration_connected:");
                        totalDuration2 = totalDuration;
                    } catch (RuntimeException e3) {
                        e = e3;
                        totalDuration2 = totalDuration;
                    } catch (Exception e4) {
                        e2 = e4;
                        totalDuration2 = totalDuration;
                    }
                    try {
                        stringBuilder2.append(value.getLong("duration_connected"));
                        LogUtil.d(stringBuilder2.toString());
                        if (maxEntry == null || maxEntry.getLong("duration_connected") < value.getLong("duration_connected")) {
                            maxEntry = value;
                        }
                        it = it2;
                        totalDuration = totalDuration2;
                    } catch (RuntimeException e5) {
                        e = e5;
                        totalDuration = totalDuration2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("calculateUserPreference RuntimeException,e:");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (!result.getBoolean("isUserHasPref", false)) {
                        }
                        return result;
                    } catch (Exception e6) {
                        e2 = e6;
                        totalDuration = totalDuration2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(" calculateUserPreference, e:");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (result.getBoolean("isUserHasPref", false)) {
                        }
                        return result;
                    }
                }
            } else if (needRecord) {
                this.mLocationDAO.accCHRUserPrefUnknownDBbyFreqLoc(this.freqLoc);
            }
            try {
                StringBuilder stringBuilder3;
                int totalCount = Math.round(((float) inoutCount2) / 2.0f) + stayCount2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("totalDuration: ");
                stringBuilder.append(totalDuration);
                stringBuilder.append(" totalCount:");
                stringBuilder.append(totalCount);
                LogUtil.d(stringBuilder.toString());
                if (maxEntry == null || totalDuration <= this.param.getUserPrefStartDuration() || maxEntry.getString("networkname", "UNKNOWN").equalsIgnoreCase("UNKNOWN")) {
                    inoutCount = inoutCount2;
                    stayCount = stayCount2;
                } else {
                    inoutCount = inoutCount2;
                    stayCount = stayCount2;
                    try {
                        if (((double) maxEntry.getLong("duration_connected")) / ((double) totalDuration) >= this.param.getUserPrefDurationRatio()) {
                            inoutCount2 = (totalCount - maxEntry.getInt("user_pref_opt_out")) - maxEntry.getInt("user_pref_stay");
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("otherNetworkCount: ");
                            stringBuilder3.append(inoutCount2);
                            stringBuilder3.append(" maxEntry: ");
                            stringBuilder3.append(maxEntry.toString());
                            LogUtil.d(stringBuilder3.toString());
                            if (totalCount > this.param.getUserPrefStartTimes() && inoutCount2 > 0 && ((float) maxEntry.getInt("user_pref_opt_in")) / ((float) inoutCount2) >= this.param.getUserPrefFreqRatio()) {
                                result.putBoolean("isUserHasPref", true);
                                result.putInt("prefNetworkType", maxEntry.getInt("networktype"));
                                result.putString("prefNetworkName", maxEntry.getString("networkname"));
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(" isUserHasPref: true prefNetworkType:");
                                stringBuilder3.append(maxEntry.getInt("networktype"));
                                stringBuilder3.append(" prefNetworkName:");
                                stringBuilder3.append(maxEntry.getString("networkname"));
                                LogUtil.d(stringBuilder3.toString());
                                return result;
                            }
                        }
                    } catch (RuntimeException e7) {
                        e = e7;
                        stayCount2 = stayCount;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("calculateUserPreference RuntimeException,e:");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (result.getBoolean("isUserHasPref", false)) {
                        }
                        return result;
                    } catch (Exception e8) {
                        e2 = e8;
                        inoutCount2 = inoutCount;
                        stayCount2 = stayCount;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(" calculateUserPreference, e:");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (result.getBoolean("isUserHasPref", false)) {
                        }
                        return result;
                    }
                }
                if (totalDuration > this.param.getUserPrefStartDuration() && ((double) cellDuration) / ((double) totalDuration) >= this.param.getUserPrefDurationRatio()) {
                    inoutCount2 = (totalCount - cellUserPrefOptOut) - cellUserPrefStay;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("otherNetworkCount: ");
                    stringBuilder3.append(inoutCount2);
                    stringBuilder3.append(" cellDuration");
                    stringBuilder3.append(cellDuration);
                    LogUtil.d(stringBuilder3.toString());
                    if (totalCount > this.param.getUserPrefStartTimes() && inoutCount2 > 0 && ((float) cellUserPrefOptIn) / ((float) inoutCount2) >= this.param.getUserPrefFreqRatio()) {
                        result.putBoolean("isUserHasPref", true);
                        result.putInt("prefNetworkType", 0);
                        result.putString("prefNetworkName", "UNKNOWN");
                        LogUtil.d(" User Prefer Cellular");
                        return result;
                    }
                }
                inoutCount2 = inoutCount;
                stayCount2 = stayCount;
            } catch (RuntimeException e9) {
                e = e9;
                inoutCount = inoutCount2;
                stayCount = stayCount2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("calculateUserPreference RuntimeException,e:");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (result.getBoolean("isUserHasPref", false)) {
                }
                return result;
            } catch (Exception e10) {
                e2 = e10;
                inoutCount = inoutCount2;
                stayCount = stayCount2;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" calculateUserPreference, e:");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (result.getBoolean("isUserHasPref", false)) {
                }
                return result;
            }
        } catch (RuntimeException e11) {
            e = e11;
        } catch (Exception e12) {
            e2 = e12;
        }
        if (result.getBoolean("isUserHasPref", false)) {
            LogUtil.i(" NO user prefer");
        }
        return result;
    }

    public void recognizeActions(RecognizeResult result) {
        StringBuilder stringBuilder;
        Exception e;
        LogUtil.i(" recognizeActions ");
        if (result == null) {
            try {
                LogUtil.i(" no result");
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        } else {
            String spaceId_mainAp;
            StringBuilder stringBuilder2;
            if (result.getRgResult() == null || result.getRgResult().contains(Constant.RESULT_UNKNOWN)) {
                e2 = "0";
                stringBuilder = new StringBuilder();
                stringBuilder.append(" recognizeActions: All AP result=");
                stringBuilder.append(result.getRgResult());
                LogUtil.i(stringBuilder.toString());
            } else {
                e2 = result.getRgResult();
            }
            if (result.getMainApRgResult() == null || result.getMainApRgResult().contains(Constant.RESULT_UNKNOWN)) {
                spaceId_mainAp = "0";
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" recognizeActions: Main AP result=");
                stringBuilder2.append(result.getMainApRgResult());
                LogUtil.i(stringBuilder2.toString());
            } else {
                spaceId_mainAp = result.getMainApRgResult();
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" recognizeActions: spaceId_allAp=");
            stringBuilder2.append(e2);
            stringBuilder2.append(", spaceId_mainAp=");
            stringBuilder2.append(spaceId_mainAp);
            LogUtil.d(stringBuilder2.toString());
            determineUserPreference(e2, spaceId_mainAp);
        }
    }

    private void determineUserPreference(String spaceId_allAp, String spaceId_mainAp) {
        LogUtil.i(" determineUserPreference ");
        try {
            HwWaveMappingManager hwWaveMappingManager = HwWaveMappingManager.getInstance();
            if (hwWaveMappingManager != null) {
                IWaveMappingCallback brainCallback = hwWaveMappingManager.getWaveMappingCallback();
                if (brainCallback != null) {
                    if (spaceId_allAp.equals("0")) {
                        LogUtil.i("No preference due to unknown space");
                        brainCallback.onWaveMappingReportCallback(0, this.mPrefNetworkName, this.mPrefNetworkType);
                        this.mLocationDAO.accCHRUserPrefUnknownSpacebyFreqLoc(this.freqLoc);
                        return;
                    }
                    updateUserPrefCHR();
                    clearUserPrefCHR();
                    Bundle userPrefResult = getUserPrefNetwork(spaceId_allAp);
                    this.mUserHasPref = userPrefResult.getBoolean("isUserHasPref");
                    this.mPrefNetworkName = userPrefResult.getString("prefNetworkName");
                    this.mPrefNetworkType = userPrefResult.getInt("prefNetworkType");
                    Bundle connectivity = NetUtil.getConnectedNetworkState(this.mContext);
                    int defaultType = connectivity.getInt("defaultType");
                    String currNwName = connectivity.getString("defaultNwName", "UNKNOWN");
                    if (this.mUserHasPref && this.mPrefNetworkType == 1 && defaultType == 1) {
                        String printStr1 = new StringBuilder();
                        printStr1.append("W2W: perfered WiFi name=");
                        printStr1.append(this.mPrefNetworkName);
                        printStr1.append(", current WiFi name=");
                        printStr1.append(currNwName);
                        LogUtil.i(printStr1.toString());
                        if (!currNwName.equals(this.mPrefNetworkName)) {
                            brainCallback.onWaveMappingReportCallback(1, this.mPrefNetworkName, this.mPrefNetworkType);
                            this.mCHRUserPrefSwitchTime = System.currentTimeMillis();
                            this.mCHRUserPrefOriginalNWName = currNwName;
                            this.mCHRUserPrefOriginalNWType = defaultType;
                            this.mCHRUserPrefSwitchNWName = this.mPrefNetworkName;
                            this.mCHRUserPrefSwitchNWType = this.mPrefNetworkType;
                            this.mCHRUserPrefDetermineSwitch = true;
                        }
                    } else if (this.mUserHasPref && this.mPrefNetworkType == 0 && defaultType == 1) {
                        String cellName = NetUtil.getMobileDataState(this.mContext).getString("cellRAT", "UNKNOWN");
                        String printStr2 = new StringBuilder();
                        printStr2.append("W2C: perfered Cell name=");
                        printStr2.append(this.mPrefNetworkName);
                        printStr2.append(", current Cell name=");
                        printStr2.append(cellName);
                        LogUtil.i(printStr2.toString());
                        if (cellName.equals(this.mPrefNetworkName)) {
                            brainCallback.onWaveMappingReportCallback(1, this.mPrefNetworkName, this.mPrefNetworkType);
                            this.mCHRUserPrefSwitchTime = System.currentTimeMillis();
                            this.mCHRUserPrefOriginalNWName = currNwName;
                            this.mCHRUserPrefOriginalNWType = defaultType;
                            this.mCHRUserPrefSwitchNWName = this.mPrefNetworkName;
                            this.mCHRUserPrefSwitchNWType = this.mPrefNetworkType;
                            this.mCHRUserPrefDetermineSwitch = true;
                        }
                    } else {
                        LogUtil.i("No preference");
                        brainCallback.onWaveMappingReportCallback(0, this.mPrefNetworkName, this.mPrefNetworkType);
                    }
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" determineUserPreference ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:72:0x01de  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0104 A:{Catch:{ Exception -> 0x01f2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0104 A:{Catch:{ Exception -> 0x01f2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x01de  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean determine4gCoverage(RecognizeResult result) {
        Exception e;
        HwWmpFastBackLte hwWmpFastBackLte;
        double d;
        StringBuilder stringBuilder;
        Bundle record4g;
        boolean foundRecord;
        boolean foundRecord2 = false;
        HwWmpFastBackLte mBack = new HwWmpFastBackLte();
        Bundle record4g2 = new Bundle();
        double duration4gRatio = 0.0d;
        LogUtil.i(" determine4gCoverage ");
        if (!this.param.getBack4GEnabled()) {
            LogUtil.d("Fast Back 4G Feature - Disabled");
        }
        if (result == null) {
            try {
                LogUtil.i(" no result");
                return false;
            } catch (Exception e2) {
                e = e2;
                hwWmpFastBackLte = mBack;
                d = duration4gRatio;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" determine4gCoverage ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return foundRecord2;
            }
        }
        String spaceId_allAp;
        String spaceId_mainAp;
        StringBuilder stringBuilder2;
        try {
            if (result.getRgResult() != null) {
                try {
                    if (result.getRgResult().contains("0")) {
                        record4g = record4g2;
                    } else {
                        spaceId_allAp = result.getRgResult();
                        if (result.getMainApRgResult() == null) {
                        } else if (result.getMainApRgResult().contains("0")) {
                            record4g = record4g2;
                        } else {
                            spaceId_mainAp = result.getMainApRgResult();
                            StringBuilder stringBuilder3 = new StringBuilder();
                            record4g = record4g2;
                            try {
                                stringBuilder3.append(" determine4gCoverage, space ID: allAp=");
                                stringBuilder3.append(spaceId_allAp);
                                stringBuilder3.append(" ,mainAp=");
                                stringBuilder3.append(spaceId_mainAp);
                                LogUtil.d(stringBuilder3.toString());
                                record4g2 = this.mSpaceUserDAO.find4gCoverageByBothSpace(spaceId_allAp, spaceId_mainAp);
                                if (record4g2.containsKey("cell_num")) {
                                    long duration = record4g2.getLong("total_duration");
                                    int cellNum = record4g2.getInt("cell_num");
                                    int avgSignal = record4g2.getInt("avg_signal");
                                    d = 0.0d;
                                    duration4gRatio = (((double) duration) * 1.0d) / ((double) (duration + record4g2.getLong("duration_out4g")));
                                    try {
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append(" found 4g record: duration=");
                                        stringBuilder4.append(duration);
                                        stringBuilder4.append(" ,cell num=");
                                        stringBuilder4.append(cellNum);
                                        stringBuilder4.append(" ,avg signal=");
                                        stringBuilder4.append(avgSignal);
                                        stringBuilder4.append(" ,duration4gRatio=");
                                        stringBuilder4.append(duration4gRatio);
                                        LogUtil.d(stringBuilder4.toString());
                                        foundRecord2 = true;
                                        try {
                                            this.mFastBack2LteChrDAO.setcells4G(cellNum);
                                            if (((long) this.param.getBack4GTH_duration_min()) >= duration || this.param.getBack4GTH_signal_min() >= avgSignal || this.param.getBack4GTH_duration_4gRatio() >= duration4gRatio) {
                                                foundRecord = true;
                                                hwWmpFastBackLte = mBack;
                                                LogUtil.i(" 4g cell NOT found ");
                                                this.mFastBack2LteChrDAO.addoutLteCnt();
                                                this.mFastBack2LteChrDAO.insertRecordByLoc();
                                            } else {
                                                LogUtil.d(" in 4G coverage, send to booster");
                                                mBack.mSubId = 0;
                                                mBack.setRat("4G");
                                                if (this.mFastBackLteMgr != null) {
                                                    this.mFastBackLteMgr.SendDataToBooster(mBack);
                                                    this.mFastBack2LteChrDAO.addfastBack();
                                                }
                                                this.mFastBack2LteChrDAO.addinLteCnt();
                                                foundRecord = true;
                                                try {
                                                    this.handler.postDelayed(this.getRegStateAfterPeriodHandler, (long) this.param.getReGetPsRegStatus());
                                                } catch (Exception e3) {
                                                    e = e3;
                                                    d = duration4gRatio;
                                                    foundRecord2 = foundRecord;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append(" determine4gCoverage ");
                                                    stringBuilder.append(e.getMessage());
                                                    LogUtil.e(stringBuilder.toString());
                                                    return foundRecord2;
                                                }
                                            }
                                            d = duration4gRatio;
                                            foundRecord2 = foundRecord;
                                        } catch (Exception e4) {
                                            e = e4;
                                            foundRecord = true;
                                            hwWmpFastBackLte = mBack;
                                            d = duration4gRatio;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append(" determine4gCoverage ");
                                            stringBuilder.append(e.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            return foundRecord2;
                                        }
                                    } catch (Exception e5) {
                                        e = e5;
                                        hwWmpFastBackLte = mBack;
                                        d = duration4gRatio;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append(" determine4gCoverage ");
                                        stringBuilder.append(e.getMessage());
                                        LogUtil.e(stringBuilder.toString());
                                        return foundRecord2;
                                    }
                                    return foundRecord2;
                                }
                                d = 0.0d;
                                String str = spaceId_allAp;
                                try {
                                    this.mFastBack2LteChrDAO.setcells4G(0);
                                    this.mFastBack2LteChrDAO.insertRecordByLoc();
                                } catch (Exception e6) {
                                    e = e6;
                                }
                                return foundRecord2;
                            } catch (Exception e7) {
                                e = e7;
                                hwWmpFastBackLte = mBack;
                                d = 0.0d;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(" determine4gCoverage ");
                                stringBuilder.append(e.getMessage());
                                LogUtil.e(stringBuilder.toString());
                                return foundRecord2;
                            }
                        }
                        spaceId_mainAp = "0";
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" determine4gCoverage: space ID: allAp=");
                        stringBuilder2.append(spaceId_allAp);
                        LogUtil.d(stringBuilder2.toString());
                        record4g2 = this.mSpaceUserDAO.find4gCoverageByAllApSpace(spaceId_allAp);
                        try {
                            if (record4g2.containsKey("cell_num")) {
                            }
                        } catch (Exception e8) {
                            e = e8;
                            hwWmpFastBackLte = mBack;
                            d = 0.0d;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" determine4gCoverage ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            return foundRecord2;
                        }
                    }
                } catch (Exception e9) {
                    e = e9;
                    record4g = record4g2;
                    hwWmpFastBackLte = mBack;
                    d = 0.0d;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" determine4gCoverage ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return foundRecord2;
                }
            }
            record4g = record4g2;
        } catch (Exception e10) {
            e = e10;
            hwWmpFastBackLte = mBack;
            record4g = record4g2;
            d = 0.0d;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" determine4gCoverage ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return foundRecord2;
        }
        try {
            spaceId_allAp = "0";
            if (result.getMainApRgResult() == null) {
                hwWmpFastBackLte = mBack;
                d = 0.0d;
            } else if (result.getMainApRgResult().contains("0")) {
                hwWmpFastBackLte = mBack;
                d = 0.0d;
            } else {
                spaceId_mainAp = result.getMainApRgResult();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" determine4gCoverage, space ID: mainAp=");
                stringBuilder2.append(spaceId_mainAp);
                LogUtil.d(stringBuilder2.toString());
                record4g2 = this.mSpaceUserDAO.find4gCoverageByMainApSpace(spaceId_mainAp);
                if (record4g2.containsKey("cell_num")) {
                }
            }
            try {
                spaceId_mainAp = "0";
                LogUtil.i(" determine4gCoverage: no space ID");
                this.mFastBack2LteChrDAO.addunknownSpace();
                return false;
            } catch (Exception e11) {
                e = e11;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" determine4gCoverage ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return foundRecord2;
            }
        } catch (Exception e12) {
            e = e12;
            hwWmpFastBackLte = mBack;
            d = 0.0d;
            record4g2 = record4g;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" determine4gCoverage ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return foundRecord2;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x012c A:{SYNTHETIC, Splitter: B:44:0x012c} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0172 A:{SYNTHETIC, Splitter: B:53:0x0172} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x018d  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0189 A:{SYNTHETIC, Splitter: B:59:0x0189} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x03b8 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0194 A:{Catch:{ Exception -> 0x0404 }} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x03f2 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x03e4 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00d7 A:{SYNTHETIC, Splitter: B:36:0x00d7} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x012c A:{SYNTHETIC, Splitter: B:44:0x012c} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0172 A:{SYNTHETIC, Splitter: B:53:0x0172} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0189 A:{SYNTHETIC, Splitter: B:59:0x0189} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x018d  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0194 A:{Catch:{ Exception -> 0x0404 }} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x03b8 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x03e4 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x03f2 A:{Catch:{ Exception -> 0x0402 }} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0421  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0421  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0421  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void queryAppQoebyTargetNw(RecognizeResult result, int fullId, int UID, int Network, IWaveMappingCallback callback) {
        Exception e;
        StringBuilder stringBuilder;
        int i = fullId;
        int i2 = Network;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" queryAppQoebyTargetNw: appName=");
        stringBuilder2.append(i);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(Constant.USERDB_APP_NAME_PREFIX);
        stringBuilder2.append(i);
        String appName = stringBuilder2.toString();
        float sourceNwBadRatio = 2.0f;
        float targetNwBadRatio = 2.0f;
        int mArbitrationNet = 802;
        int mArbitrationNet2;
        if (callback == null) {
            try {
                LogUtil.d(" no callback");
                return;
            } catch (Exception e2) {
                e = e2;
                mArbitrationNet2 = mArbitrationNet;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (callback != null) {
                }
                return;
            }
        }
        if (1 == i2) {
            mArbitrationNet = 800;
        } else if (i2 == 0) {
            mArbitrationNet = 801;
        }
        mArbitrationNet2 = mArbitrationNet;
        if (result == null) {
            try {
                LogUtil.i(" no result");
                callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, true, false);
                return;
            } catch (Exception e3) {
                e = e3;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (callback != null) {
                }
                return;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" queryAppQoebyTargetNw: space results=");
        stringBuilder.append(result.toString());
        LogUtil.i(stringBuilder.toString());
        if (Constant.getSavedQoeAppList().containsKey(Integer.valueOf(fullId))) {
            String spaceId_allAp;
            String spaceId_allAp2;
            String spaceId_mainAp;
            HashMap<String, List> historyQoe;
            Bundle wifiInfo;
            boolean isGood;
            String currWifiName;
            Bundle cellInfo;
            boolean isFound;
            Bundle wifiInfo2;
            String currCellName;
            Bundle cellInfo2;
            String str;
            StringBuilder stringBuilder3;
            Bundle appName2;
            this.mHisQoEChrDAO.accQueryCnt();
            if (result.getRgResult() != null) {
                if (!result.getRgResult().contains(Constant.RESULT_UNKNOWN)) {
                    spaceId_allAp = result.getRgResult();
                    spaceId_allAp2 = spaceId_allAp;
                    if (result.getMainApRgResult() != null) {
                        if (!result.getMainApRgResult().contains(Constant.RESULT_UNKNOWN)) {
                            spaceId_allAp = result.getMainApRgResult();
                            spaceId_mainAp = spaceId_allAp;
                            this.mHisQoEChrService.resetRecordByApp(i);
                            this.mHisQoEChrService.setSpaceInfo(result.getRgResultInt(), result.getAllApModelNameInt(), result.getMainApRgResultInt(), result.getMainApModelNameInt(), 0, 0);
                            if (spaceId_allAp2.equals("0")) {
                                if (spaceId_mainAp.equals("0")) {
                                    LogUtil.i(" no space found");
                                    callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, true, null);
                                    this.mHisQoEChrDAO.accUnknownSpace();
                                    this.mHisQoEChrService.saveRecordByApp(i);
                                    return;
                                }
                            }
                            spaceId_mainAp = spaceId_mainAp;
                            historyQoe = calculateAppQoeFromDatabase(appName, spaceId_allAp2, spaceId_mainAp);
                            wifiInfo = NetUtil.getWifiStateString(this.mContext);
                            isGood = false;
                            currWifiName = wifiInfo.getString("wifiAp", "UNKNOWN");
                            if (currWifiName == null) {
                                currWifiName = "UNKNOWN";
                            }
                            cellInfo = NetUtil.getMobileDataState(this.mContext);
                            isFound = false;
                            wifiInfo2 = wifiInfo;
                            currCellName = cellInfo.getString("cellRAT", "UNKNOWN");
                            String currCellName2;
                            if (currCellName == null) {
                                currCellName2 = "UNKNOWN";
                            } else {
                                currCellName2 = currCellName;
                            }
                            HashMap<String, List> historyQoe2;
                            String spaceId_mainAp2;
                            String spaceId_allAp3;
                            if (historyQoe.isEmpty()) {
                                cellInfo2 = cellInfo;
                                historyQoe2 = historyQoe;
                                spaceId_mainAp2 = spaceId_mainAp;
                                spaceId_allAp3 = spaceId_allAp2;
                                str = appName;
                                LogUtil.i("  historyQoe is empty");
                            } else {
                                stringBuilder3 = new StringBuilder();
                                cellInfo2 = cellInfo;
                                stringBuilder3.append("              ,historyQoe size=");
                                stringBuilder3.append(historyQoe.size());
                                LogUtil.i(stringBuilder3.toString());
                                Iterator it = historyQoe.entrySet().iterator();
                                while (it.hasNext()) {
                                    String currWifiName2;
                                    Entry<String, List> entry = (Entry) it.next();
                                    String nwName = (String) entry.getKey();
                                    Iterator it2 = it;
                                    List value = (List) entry.getValue();
                                    mArbitrationNet = ((Integer) value.get(0)).intValue();
                                    historyQoe2 = historyQoe;
                                    int nwId = ((Integer) value.get(1)).intValue();
                                    spaceId_mainAp2 = spaceId_mainAp;
                                    int nwFreq = ((Integer) value.get(2)).intValue();
                                    spaceId_allAp3 = spaceId_allAp2;
                                    int dur = ((Integer) value.get(3)).intValue();
                                    int good = ((Integer) value.get(4)).intValue();
                                    int poor = ((Integer) value.get(5)).intValue();
                                    int rx = ((Integer) value.get(6)).intValue();
                                    int tx = ((Integer) value.get(7)).intValue();
                                    int days = ((Integer) value.get(8)).intValue();
                                    float badRatio = ((Float) value.get(9)).floatValue();
                                    if (1 == mArbitrationNet) {
                                        currCellName = nwName;
                                        if (currCellName.equals(currWifiName)) {
                                            str = appName;
                                            try {
                                                if (currWifiName.equals("UNKNOWN")) {
                                                    currWifiName2 = currWifiName;
                                                } else {
                                                    StringBuilder stringBuilder4 = new StringBuilder();
                                                    currWifiName2 = currWifiName;
                                                    stringBuilder4.append("  found Wifi network:");
                                                    stringBuilder4.append(currCellName);
                                                    LogUtil.v(stringBuilder4.toString());
                                                    if (i2 == mArbitrationNet) {
                                                        targetNwBadRatio = badRatio;
                                                        currWifiName = new StringBuilder();
                                                        currWifiName.append("  found target network, bad ratio=");
                                                        currWifiName.append(badRatio);
                                                        LogUtil.i(currWifiName.toString());
                                                        this.mHisQoEChrService.setNetInfo(nwId, currCellName, nwFreq, mArbitrationNet);
                                                        this.mHisQoEChrService.setRecords(days, dur, good, poor, rx, tx);
                                                    } else {
                                                        sourceNwBadRatio = badRatio;
                                                        currWifiName = new StringBuilder();
                                                        currWifiName.append("  found source network, bad ratio=");
                                                        currWifiName.append(badRatio);
                                                        LogUtil.i(currWifiName.toString());
                                                    }
                                                }
                                            } catch (Exception e4) {
                                                e = e4;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append(" assignSpaceExp2Space ");
                                                stringBuilder.append(e.getMessage());
                                                LogUtil.e(stringBuilder.toString());
                                                if (callback != null) {
                                                    callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, true, false);
                                                }
                                                return;
                                            }
                                        }
                                        currWifiName2 = currWifiName;
                                        str = appName;
                                    } else {
                                        currWifiName2 = currWifiName;
                                        str = appName;
                                        currCellName = nwName;
                                    }
                                    if (mArbitrationNet == 0 && currCellName.equals(currCellName2) != null && currCellName2.equals("UNKNOWN") == null) {
                                        currWifiName = new StringBuilder();
                                        currWifiName.append("  found CELL network:");
                                        currWifiName.append(currCellName);
                                        LogUtil.v(currWifiName.toString());
                                        if (i2 == mArbitrationNet) {
                                            targetNwBadRatio = badRatio;
                                            currWifiName = new StringBuilder();
                                            currWifiName.append("  found target network, bad ratio=");
                                            currWifiName.append(badRatio);
                                            LogUtil.i(currWifiName.toString());
                                            this.mHisQoEChrService.setNetInfo(nwId, currCellName, nwFreq, mArbitrationNet);
                                            this.mHisQoEChrService.setRecords(days, dur, good, poor, rx, tx);
                                        } else {
                                            sourceNwBadRatio = badRatio;
                                            currWifiName = new StringBuilder();
                                            currWifiName.append("  found source network, bad ratio=");
                                            currWifiName.append(badRatio);
                                            LogUtil.i(currWifiName.toString());
                                        }
                                    }
                                    it = it2;
                                    historyQoe = historyQoe2;
                                    spaceId_mainAp = spaceId_mainAp2;
                                    spaceId_allAp2 = spaceId_allAp3;
                                    appName = str;
                                    currWifiName = currWifiName2;
                                }
                                historyQoe2 = historyQoe;
                                spaceId_mainAp2 = spaceId_mainAp;
                                spaceId_allAp3 = spaceId_allAp2;
                                str = appName;
                                if (targetNwBadRatio != 2.0f) {
                                    float threshold = ((Float) Constant.getSavedQoeAppList().get(Integer.valueOf(fullId))).floatValue();
                                    float margin = this.param.getAppTH_Target_Ration_margin();
                                    StringBuilder stringBuilder5;
                                    if (targetNwBadRatio < threshold) {
                                        currWifiName = true;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("  target network is GOOD enough, targetNwBadRatio:");
                                        stringBuilder5.append(targetNwBadRatio);
                                        LogUtil.d(stringBuilder5.toString());
                                    } else if (sourceNwBadRatio == 2.0f || targetNwBadRatio + margin >= sourceNwBadRatio) {
                                        LogUtil.d("  target network is Bad");
                                        currWifiName = isGood;
                                    } else {
                                        currWifiName = true;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("  target network(");
                                        stringBuilder5.append(targetNwBadRatio);
                                        stringBuilder5.append(") is BETTER than source network(");
                                        stringBuilder5.append(sourceNwBadRatio);
                                        stringBuilder5.append(")");
                                        LogUtil.d(stringBuilder5.toString());
                                    }
                                    isGood = currWifiName;
                                    isFound = true;
                                }
                            }
                            appName2 = cellInfo2;
                            callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, isGood, isFound);
                            if (!isFound) {
                                this.mHisQoEChrDAO.accUnknownDB();
                            } else if (isGood) {
                                this.mHisQoEChrDAO.accGoodCnt();
                            } else {
                                this.mHisQoEChrDAO.accPoorCnt();
                            }
                            this.mHisQoEChrDAO.insertRecordByLoc();
                            this.mHisQoEChrService.saveRecordByApp(i);
                            return;
                        }
                    }
                    spaceId_allAp = "0";
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" queryAppQoebyTargetNw: Main AP result=");
                    stringBuilder3.append(result.getMainApRgResult());
                    LogUtil.i(stringBuilder3.toString());
                    spaceId_mainAp = spaceId_allAp;
                    this.mHisQoEChrService.resetRecordByApp(i);
                    this.mHisQoEChrService.setSpaceInfo(result.getRgResultInt(), result.getAllApModelNameInt(), result.getMainApRgResultInt(), result.getMainApModelNameInt(), 0, 0);
                    if (spaceId_allAp2.equals("0")) {
                    }
                    spaceId_mainAp = spaceId_mainAp;
                    historyQoe = calculateAppQoeFromDatabase(appName, spaceId_allAp2, spaceId_mainAp);
                    wifiInfo = NetUtil.getWifiStateString(this.mContext);
                    isGood = false;
                    currWifiName = wifiInfo.getString("wifiAp", "UNKNOWN");
                    if (currWifiName == null) {
                    }
                    cellInfo = NetUtil.getMobileDataState(this.mContext);
                    isFound = false;
                    wifiInfo2 = wifiInfo;
                    currCellName = cellInfo.getString("cellRAT", "UNKNOWN");
                    if (currCellName == null) {
                    }
                    if (historyQoe.isEmpty()) {
                    }
                    appName2 = cellInfo2;
                    callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, isGood, isFound);
                    if (!isFound) {
                    }
                    this.mHisQoEChrDAO.insertRecordByLoc();
                    this.mHisQoEChrService.saveRecordByApp(i);
                    return;
                }
            }
            try {
                spaceId_allAp = "0";
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" queryAppQoebyTargetNw: All AP result=");
                stringBuilder3.append(result.getRgResult());
                LogUtil.i(stringBuilder3.toString());
                spaceId_allAp2 = spaceId_allAp;
                if (result.getMainApRgResult() != null) {
                }
                spaceId_allAp = "0";
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" queryAppQoebyTargetNw: Main AP result=");
                stringBuilder3.append(result.getMainApRgResult());
                LogUtil.i(stringBuilder3.toString());
                spaceId_mainAp = spaceId_allAp;
                this.mHisQoEChrService.resetRecordByApp(i);
                this.mHisQoEChrService.setSpaceInfo(result.getRgResultInt(), result.getAllApModelNameInt(), result.getMainApRgResultInt(), result.getMainApModelNameInt(), 0, 0);
                if (spaceId_allAp2.equals("0")) {
                }
                spaceId_mainAp = spaceId_mainAp;
                historyQoe = calculateAppQoeFromDatabase(appName, spaceId_allAp2, spaceId_mainAp);
                wifiInfo = NetUtil.getWifiStateString(this.mContext);
                isGood = false;
                currWifiName = wifiInfo.getString("wifiAp", "UNKNOWN");
                if (currWifiName == null) {
                }
                cellInfo = NetUtil.getMobileDataState(this.mContext);
                isFound = false;
                wifiInfo2 = wifiInfo;
                currCellName = cellInfo.getString("cellRAT", "UNKNOWN");
                if (currCellName == null) {
                }
                if (historyQoe.isEmpty()) {
                }
                appName2 = cellInfo2;
                callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, isGood, isFound);
                if (!isFound) {
                }
                this.mHisQoEChrDAO.insertRecordByLoc();
                this.mHisQoEChrService.saveRecordByApp(i);
            } catch (Exception e5) {
                e = e5;
                str = appName;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" assignSpaceExp2Space ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (callback != null) {
                }
                return;
            }
            return;
        }
        LogUtil.i(" NOT monitor app");
        callback.onWaveMappingRespondCallback(UID, 0, mArbitrationNet2, true, false);
    }

    private HashMap<String, List> calculateAppQoeFromDatabase(String app, String spaceId_allAp, String spaceId_mainAp) {
        Exception e;
        HashMap<String, List> resultsQoe = new HashMap();
        StringBuilder stringBuilder;
        try {
            try {
                StringBuilder stringBuilder2;
                HashMap<String, List> valueQoe = this.mSpaceUserDAO.findAppQoEgroupBySpace(app, spaceId_allAp, spaceId_mainAp);
                for (Entry<String, List> entry : valueQoe.entrySet()) {
                    String nwName = (String) entry.getKey();
                    List qoeRaw = (List) entry.getValue();
                    if (qoeRaw == null) {
                        LogUtil.w(" qoe record invalid, qoeRaw=null");
                        return resultsQoe;
                    } else if (qoeRaw.size() < 4) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(" qoe record invalid, qoeRaw size=");
                        stringBuilder.append(qoeRaw.size());
                        LogUtil.w(stringBuilder.toString());
                        return resultsQoe;
                    } else {
                        HashMap<String, List> valueQoe2;
                        int type = ((Integer) qoeRaw.get(0)).intValue();
                        int duration = ((Integer) qoeRaw.get(3)).intValue();
                        int good = ((Integer) qoeRaw.get(4)).intValue();
                        int poor = ((Integer) qoeRaw.get(5)).intValue();
                        if (duration > this.param.getAppTH_duration_min()) {
                            if (poor > this.param.getAppTH_poorCnt_min()) {
                                float ratio;
                                if (good > this.param.getAppTH_goodCnt_min()) {
                                    valueQoe2 = valueQoe;
                                    ratio = (((float) poor) * 1.0f) / (((float) good) * 1065353216);
                                } else {
                                    valueQoe2 = valueQoe;
                                    ratio = (((float) poor) * 5000.0f) / (((float) duration) * 1.0f);
                                }
                                valueQoe = ratio;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(" have poor, network=");
                                stringBuilder3.append(nwName);
                                stringBuilder3.append(", poor ratio=");
                                stringBuilder3.append(valueQoe);
                                LogUtil.i(stringBuilder3.toString());
                            } else {
                                valueQoe2 = valueQoe;
                                valueQoe = new StringBuilder();
                                valueQoe.append(" too less poor=");
                                valueQoe.append(poor);
                                valueQoe.append(", network=");
                                valueQoe.append(nwName);
                                LogUtil.i(valueQoe.toString());
                                valueQoe = null;
                            }
                            qoeRaw.add(Float.valueOf(valueQoe));
                            resultsQoe.put(nwName, qoeRaw);
                            LogUtil.d(" add history Qoe");
                        } else {
                            valueQoe2 = valueQoe;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" too less duration = ");
                            stringBuilder2.append(duration);
                            stringBuilder2.append(", network=");
                            stringBuilder2.append(nwName);
                            LogUtil.i(stringBuilder2.toString());
                        }
                        valueQoe = valueQoe2;
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" record size:");
                stringBuilder2.append(resultsQoe.size());
                LogUtil.d(stringBuilder2.toString());
            } catch (Exception e2) {
                e = e2;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" saveSpaceExptoDatabase ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return resultsQoe;
            }
        } catch (Exception e3) {
            e = e3;
            String str = app;
            String str2 = spaceId_allAp;
            String str3 = spaceId_mainAp;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" saveSpaceExptoDatabase ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return resultsQoe;
        }
        return resultsQoe;
    }

    private String getTime(long time) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date(time));
    }

    private void setNewStartTime(HwWmpAppInfo info) {
        if (this.saveAppInfo.isEmpty() || !this.saveAppInfo.containsKey(info.getAppName())) {
            HwWmpAppInfo newInfo = new HwWmpAppInfo(8);
            newInfo.copyObjectValue(info);
            newInfo.setStartTime(System.currentTimeMillis());
            if (newInfo.getAppName() != null) {
                this.saveAppInfo.put(newInfo.getAppName(), newInfo);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" setNewStartTime - new, ");
                stringBuilder.append(newInfo.toString());
                LogUtil.d(stringBuilder.toString());
                return;
            }
            LogUtil.w(" getAppName == null");
            return;
        }
        ((HwWmpAppInfo) this.saveAppInfo.get(info.getAppName())).setStartTime(System.currentTimeMillis());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" setNewStartTime - update, ");
        stringBuilder2.append(info.toString());
        LogUtil.i(stringBuilder2.toString());
    }

    private void updateStartTime(String app, int net) {
        StringBuilder stringBuilder;
        if (this.saveAppInfo.isEmpty() || !this.saveAppInfo.containsKey(app)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" updateStartTime, no this app:");
            stringBuilder.append(app);
            LogUtil.d(stringBuilder.toString());
            return;
        }
        ((HwWmpAppInfo) this.saveAppInfo.get(app)).setStartTime(System.currentTimeMillis());
        ((HwWmpAppInfo) this.saveAppInfo.get(app)).setConMgrNetworkType(net);
        stringBuilder = new StringBuilder();
        stringBuilder.append(" updateStartTime, update app:");
        stringBuilder.append(app);
        stringBuilder.append(", appNwType:");
        stringBuilder.append(net);
        LogUtil.i(stringBuilder.toString());
    }

    private void resetTime(String appName) {
        if (this.saveAppInfo.isEmpty()) {
            LogUtil.i(" no saved APP");
            return;
        }
        if (this.saveAppInfo.containsKey(appName)) {
            this.saveAppInfo.remove(appName);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" resetTime, app:");
        stringBuilder.append(appName);
        LogUtil.i(stringBuilder.toString());
    }

    public void checkConnectivityState() {
        Bundle connectivity = NetUtil.getConnectedNetworkState(this.mContext);
        boolean wifiState = connectivity.getBoolean("wifi");
        boolean mobileState = connectivity.getBoolean("mobile");
        int defaultType = connectivity.getInt("defaultType");
        if (this.mCHRUserPrefDetermineSwitch) {
            if (!this.mCHRUserPrefAutoSwitch && !this.mUserOperation && this.mCHRUserPrefSwitchNWName.equals(connectivity.getString("defaultNwName")) && defaultType == this.mCHRUserPrefSwitchNWType) {
                this.mCHRUserPrefAutoSwitch = true;
                this.mCHRUserPrefAutoSwitchTime = System.currentTimeMillis();
            }
            if (!this.mCHRUserPrefManualSwitch && this.mUserOperation && this.mDestNetworkId.equals("UNKNOWN") && !this.mCHRUserPrefAutoSwitch && this.mCHRUserPrefSwitchNWName.equals(connectivity.getString("defaultNwName")) && defaultType == this.mCHRUserPrefSwitchNWType) {
                this.mCHRUserPrefManualSwitch = true;
                this.mCHRUserPrefManualSwitchTime = System.currentTimeMillis();
            }
            if (!this.mCHRUserPrefManualSwitch && this.mUserOperation && this.mDestNetworkId.equals("UNKNOWN") && this.mCHRUserPrefAutoSwitch && this.mCHRUserPrefOriginalNWName.equals(connectivity.getString("defaultNwName")) && defaultType == this.mCHRUserPrefOriginalNWType) {
                this.mCHRUserPrefManualSwitch = true;
                this.mCHRUserPrefManualSwitchTime = System.currentTimeMillis();
            }
        }
        if (wifiState && !this.connectedWifi) {
            LogUtil.i("checkConnectivityState, wifi start");
            setNewStartTime(new HwWmpAppInfo(1));
            backupWifiInfo();
            this.connectedWifi = true;
        } else if (!wifiState && this.connectedWifi) {
            LogUtil.i("checkConnectivityState, wifi end");
            updateWifiDurationEnd();
            this.connectedWifi = false;
        }
        if (mobileState && !this.connectedMobile) {
            LogUtil.i("checkConnectivityState, mobile start");
            setNewStartTime(new HwWmpAppInfo(0));
            this.connectedMobile = true;
        } else if (!mobileState && this.connectedMobile) {
            LogUtil.i("checkConnectivityState, mobile end");
            updateMobileDurationEnd();
            this.connectedMobile = false;
        }
        StringBuilder stringBuilder;
        if (defaultType == 8 || !this.mDestNetworkId.equals("UNKNOWN")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not setup mDestNetworkId, defaultType=");
            stringBuilder.append(defaultType);
            stringBuilder.append(", DestNetworkId=");
            stringBuilder.append(this.mDestNetworkId);
            LogUtil.i(stringBuilder.toString());
        } else if (!this.isDestNetworkCellular && 1 == defaultType) {
            this.mDestNetworkId = connectivity.getString("defaultNwId");
            this.mDestNetworkName = connectivity.getString("defaultNwName");
            this.mDestNetworkFreq = connectivity.getString("defaultNwFreq");
            this.mDestNetworkType = 1;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CONNECTIVITY_ACTION WIFI connected: mDestNetworkName:");
            stringBuilder.append(this.mDestNetworkName);
            LogUtil.d(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("                                    mDestNetworkId:");
            stringBuilder.append(this.mDestNetworkId);
            LogUtil.v(stringBuilder.toString());
        } else if (this.isDestNetworkCellular && defaultType == 0) {
            this.mDestNetworkId = connectivity.getString("defaultNwId");
            this.mDestNetworkName = connectivity.getString("defaultNwName");
            this.mDestNetworkFreq = connectivity.getString("defaultNwFreq");
            this.mDestNetworkType = 0;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CONNECTIVITY_ACTION MOBILE connected: mDestNetworkName:");
            stringBuilder.append(this.mDestNetworkName);
            LogUtil.d(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("                                      mDestNetworkId:");
            stringBuilder.append(this.mDestNetworkId);
            LogUtil.v(stringBuilder.toString());
        }
        checkAppCurrentNetwork();
    }

    private void checkAppCurrentNetwork() {
        LogUtil.i("checkAppCurrentNetwork ");
        if (!this.saveAppInfo.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("              ,saveAppInfo size=");
            stringBuilder.append(this.saveAppInfo.size());
            LogUtil.i(stringBuilder.toString());
            for (Entry<String, HwWmpAppInfo> entry : this.saveAppInfo.entrySet()) {
                String app = (String) entry.getKey();
                HwWmpAppInfo info = (HwWmpAppInfo) entry.getValue();
                int nwType = info.getConMgrNetworkType();
                int uid = info.getAppUid();
                if (info.isNormalApp()) {
                    int arbitrationNwType = HwArbitrationFunction.getCurrentNetwork(this.mContext, uid);
                    int currConMgrNwType = 8;
                    if (800 == arbitrationNwType) {
                        currConMgrNwType = 1;
                    } else if (801 == arbitrationNwType) {
                        currConMgrNwType = 0;
                    }
                    if (currConMgrNwType != nwType) {
                        updateAppNetwork(app, currConMgrNwType);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  network change, app=");
                        stringBuilder2.append(app);
                        stringBuilder2.append(", from ");
                        stringBuilder2.append(nwType);
                        stringBuilder2.append(" to ");
                        stringBuilder2.append(currConMgrNwType);
                        LogUtil.d(stringBuilder2.toString());
                    }
                }
            }
        }
    }

    public boolean checkOutOf4GCoverage(boolean restart) {
        Bundle cellInfo = NetUtil.getMobileDataState(this.mContext);
        boolean callIdle = NetUtil.isMobileCallStateIdle(this.mContext);
        if (!this.param.getBack4GEnabled()) {
            LogUtil.d("Fast Back 4G Feature - Disabled");
            return false;
        } else if (!callIdle) {
            LogUtil.d("checkOutOf4GCoverage: call exists");
            resetOut4GBeginTime();
            this.handler.removeCallbacks(this.periodicOutof4GHandler);
            return false;
        } else if (cellInfo == null || !cellInfo.containsKey("cellRAT")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkOutOf4GCoverage: null cell RAT, last outof4Gflag=");
            stringBuilder.append(false);
            LogUtil.d(stringBuilder.toString());
            resetOut4GBeginTime();
            this.handler.removeCallbacks(this.periodicOutof4GHandler);
            return false;
        } else {
            boolean outof4G;
            StringBuilder stringBuilder2;
            String RAT = cellInfo.getString("cellRAT", "UNKNOWN");
            String mState = cellInfo.getString("cellState", "DISABLED");
            int mService = cellInfo.getInt("cellService");
            if (mState.equals("DISABLED") || mService != 0) {
                outof4G = false;
                this.handler.removeCallbacks(this.periodicOutof4GHandler);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("checkOutOf4GCoverage: Data Disabled(");
                stringBuilder2.append(mState);
                stringBuilder2.append(") or OOS(");
                stringBuilder2.append(mService);
                stringBuilder2.append("), then stop searching 4G process");
                LogUtil.d(stringBuilder2.toString());
            } else if (RAT.equals("4G")) {
                outof4G = false;
                resetOut4GBeginTime();
                this.handler.removeCallbacks(this.periodicOutof4GHandler);
                if (!this.back4g_begin) {
                    LogUtil.i(" once back to 4G");
                    this.back4g_begin = true;
                    this.back4g_restartCnt = 0;
                    this.mMachineHandler.sendEmptyMessage(141);
                    this.mFastBack2LteChrDAO.insertRecordByLoc();
                }
            } else {
                outof4G = true;
                this.back4g_begin = false;
                int cell4gNum = 0;
                Bundle record4g = this.mSpaceUserDAO.find4gCoverageByCurrLoc();
                if (record4g.containsKey("cell_num")) {
                    cell4gNum = record4g.getInt("cell_num");
                }
                if (cell4gNum == 0) {
                    LogUtil.d("checkOutOf4GCoverage: no 4G cell in this freq location, not trigger searching 4G process");
                } else if (0 == this.outof4G_begin) {
                    this.outof4G_begin = System.currentTimeMillis();
                    this.back4g_restartCnt = 0;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("checkOutOf4GCoverage: 1st out to 4G, begin time=");
                    stringBuilder3.append(this.outof4G_begin);
                    LogUtil.i(stringBuilder3.toString());
                    this.mFastBack2LteChrDAO.addlowRatCnt();
                    this.handler.postDelayed(this.periodicOutof4GHandler, (long) this.param.getBack4GTH_out4G_interval());
                } else if (restart) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("checkOutOf4GCoverage: restart(");
                    stringBuilder4.append(this.back4g_restartCnt);
                    stringBuilder4.append(")");
                    LogUtil.i(stringBuilder4.toString());
                    if (this.back4g_restartCnt < 10) {
                        this.handler.postDelayed(this.periodicOutof4GHandler, (long) (this.param.getBack4GTH_out4G_interval() * 2));
                        this.back4g_restartCnt++;
                    }
                } else {
                    long duration_outof4G = System.currentTimeMillis() - this.outof4G_begin;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("checkOutOf4GCoverage: already out to 4G, duration=");
                    stringBuilder5.append(duration_outof4G);
                    stringBuilder5.append(", TH=");
                    stringBuilder5.append(this.param.getBack4GTH_out4G_interval());
                    LogUtil.i(stringBuilder5.toString());
                    if (((long) this.param.getBack4GTH_out4G_interval()) < duration_outof4G) {
                        this.handler.removeCallbacks(this.periodicOutof4GHandler);
                        this.mMachineHandler.sendEmptyMessage(140);
                    } else {
                        this.handler.postDelayed(this.periodicOutof4GHandler, (long) this.param.getBack4GTH_out4G_interval());
                    }
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("checkOutOf4GCoverage: out of 4G=");
            stringBuilder2.append(outof4G);
            stringBuilder2.append(", RAT=");
            stringBuilder2.append(RAT);
            LogUtil.d(stringBuilder2.toString());
            return outof4G;
        }
    }

    public void resetOut4GBeginTime() {
        this.outof4G_begin = 0;
    }

    private Bundle getSpecifiedDataState(int networkType) {
        Bundle wifiInfo;
        Bundle output = new Bundle();
        boolean valid = false;
        String nwName = "UNKNOWN";
        String nwId = "UNKNOWN";
        String nwFreq = "UNKNOWN";
        String signal = "UNKNOWN";
        if (1 == networkType) {
            wifiInfo = NetUtil.getWifiStateString(this.mContext);
            nwName = wifiInfo.getString("wifiAp", "UNKNOWN");
            nwId = wifiInfo.getString("wifiMAC", "UNKNOWN");
            nwFreq = wifiInfo.getString("wifiCh", "UNKNOWN");
            signal = wifiInfo.getString("wifiRssi", "0");
            if (!(nwName == null || nwId == null || nwFreq == null)) {
                valid = true;
                backupWifiInfo();
            }
        }
        if (networkType == 0) {
            wifiInfo = NetUtil.getMobileDataState(this.mContext);
            nwName = wifiInfo.getString("cellRAT", "UNKNOWN");
            nwId = wifiInfo.getString("cellId", "UNKNOWN");
            nwFreq = wifiInfo.getString("cellFreq", "UNKNOWN");
            signal = wifiInfo.getString("cellRssi", "UNKNOWN");
            if (!(nwName == null || nwId == null || nwFreq == null)) {
                valid = true;
                backupCellInfo();
            }
        }
        output.putBoolean("VALID", valid);
        output.putString("ID", nwId);
        output.putString("NAME", nwName);
        output.putString("FREQ", nwFreq);
        output.putString("SIGNAL", signal);
        return output;
    }

    public FastBack2LteChrDAO getBack2LteChrDAO() {
        return this.mFastBack2LteChrDAO;
    }

    private void updateUserPrefCHR() {
        if (this.mCHRUserPrefDetermineSwitch) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateUserPrefCHR: DetermineSwitch, ");
            stringBuilder.append(this.mCHRUserPrefOriginalNWName);
            stringBuilder.append(" ->");
            stringBuilder.append(this.mCHRUserPrefSwitchNWName);
            stringBuilder.append(" Time:");
            stringBuilder.append(this.mCHRUserPrefSwitchTime);
            LogUtil.i(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateUserPrefCHR: Auto Switch Time ");
            stringBuilder.append(this.mCHRUserPrefAutoSwitchTime);
            stringBuilder.append(" Manual Switch Time:");
            stringBuilder.append(this.mCHRUserPrefManualSwitchTime);
            LogUtil.i(stringBuilder.toString());
            this.mLocationDAO.accCHRUserPrefTotalSwitchbyFreqLoc(this.freqLoc);
            if (this.mCHRUserPrefAutoSwitch) {
                if (this.mCHRUserPrefManualSwitch && this.mCHRUserPrefManualSwitchTime > this.mCHRUserPrefAutoSwitchTime && this.mCHRUserPrefManualSwitchTime - this.mCHRUserPrefAutoSwitchTime < AppHibernateCst.DELAY_ONE_MINS) {
                    LogUtil.i("updateUserPrefCHR: AutoFail + 1");
                    this.mLocationDAO.accCHRUserPrefAutoFailbyFreqLoc(this.freqLoc);
                }
                if (!this.mCHRUserPrefManualSwitch) {
                    LogUtil.i("updateUserPrefCHR: AutoSucc + 1");
                    this.mLocationDAO.accCHRUserPrefAutoSuccbyFreqLoc(this.freqLoc);
                }
            } else if (this.mCHRUserPrefManualSwitch) {
                LogUtil.i("updateUserPrefCHR: ManualSucc + 1");
                this.mLocationDAO.accCHRUserPrefManualSuccbyFreqLoc(this.freqLoc);
            } else {
                LogUtil.i("updateUserPrefCHR: NoSwitchFail + 1");
                this.mLocationDAO.accCHRUserPrefNoSwitchFailbyFreqLoc(this.freqLoc);
            }
        }
    }

    private void clearUserPrefCHR() {
        this.mCHRUserPrefDetermineSwitch = false;
        this.mCHRUserPrefOriginalNWName = "UNKNOWN";
        this.mCHRUserPrefOriginalNWType = 8;
        this.mCHRUserPrefSwitchNWName = "UNKNOWN";
        this.mCHRUserPrefSwitchNWType = 8;
        this.mCHRUserPrefSwitchTime = 0;
        this.mCHRUserPrefAutoSwitch = false;
        this.mCHRUserPrefAutoSwitchTime = 0;
        this.mCHRUserPrefManualSwitch = false;
        this.mCHRUserPrefManualSwitchTime = 0;
    }
}
