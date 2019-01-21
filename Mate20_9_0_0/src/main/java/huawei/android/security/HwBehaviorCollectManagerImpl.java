package huawei.android.security;

import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import com.android.server.wifi.HwWifiChrMsgID;
import com.huawei.connectivitylog.ConnectivityLogManager;
import com.huawei.hsm.permission.StubController;
import huawei.android.app.admin.ConstantValue;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.android.security.IInspectAppObserver.Stub;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class HwBehaviorCollectManagerImpl implements IHwBehaviorCollectManager {
    private static final int CONNECT_TIME = 5;
    private static final int EXTRA_PARAM_NUM_ADDWINDOW = 1;
    private static final int EXTRA_PARAM_NUM_BROADCASTINTENT = 1;
    private static final int EXTRA_PARAM_NUM_DELETE = 1;
    private static final int EXTRA_PARAM_NUM_FINISHRECEIVERLOCKED = 1;
    private static final int EXTRA_PARAM_NUM_INSERT = 1;
    private static final int EXTRA_PARAM_NUM_PERFORMRECEIVELOCKED = 1;
    private static final int EXTRA_PARAM_NUM_PROCESSCURBROADCASTLOCKED = 1;
    private static final int EXTRA_PARAM_NUM_QUERY = 1;
    private static final int EXTRA_PARAM_NUM_REGISTERCONTENTOBSERVER = 1;
    private static final int EXTRA_PARAM_NUM_SETCOMPONENTENABLEDSETTING = 1;
    private static final int EXTRA_PARAM_NUM_STARTACTIVITYMAYWAIT = 2;
    private static final int EXTRA_PARAM_NUM_UPDATE = 1;
    private static final String HW_BEHAVIOR_ACTION_INSTALL_SHORTCUT = "android.launcher.action.INSTALL_SHORTCUT";
    private static final String HW_BEHAVIOR_ACTION_SMS_RECEIVED = "com.android.vociemailomtp.sms.sms_received";
    private static final String HW_BEHAVIOR_AUTHORITY_BROWSER = "browser";
    private static final String HW_BEHAVIOR_AUTHORITY_CONTACTS = "contacts";
    private static final String HW_BEHAVIOR_AUTHORITY_SMS = "sms";
    private static final String HW_BEHAVIOR_AUTHORITY_TELEPHONY = "telephony";
    private static final String HW_BEHAVIOR_PACKAGE_BROWSER = "com.android.browser";
    private static final String HW_BEHAVIOR_PACKAGE_CALENDAR = "com.android.calendar";
    private static final String HW_BEHAVIOR_PACKAGE_CALENDAR2 = "com.android.calendar2";
    private static final String HW_BEHAVIOR_PACKAGE_CHROME = "com.android.chrome";
    private static final String HW_BEHAVIOR_PACKAGE_CONTACTS = "com.android.contacts";
    private static final String HW_BEHAVIOR_PACKAGE_DIALER = "com.android.dialer";
    private static final String HW_BEHAVIOR_PACKAGE_GALLERY = "com.android.gallery";
    private static final String HW_BEHAVIOR_PACKAGE_GALLERY3D = "com.android.gallery3d";
    private static final String HW_BEHAVIOR_PACKAGE_HTMLVIEWER = "com.android.htmlviewer";
    private static final String HW_BEHAVIOR_PACKAGE_INSTALLER = "com.android.packageinstaller";
    private static final String HW_BEHAVIOR_PACKAGE_MARKET = "com.android.market";
    private static final String HW_BEHAVIOR_PACKAGE_MMS = "com.android.mms";
    private static final String HW_BEHAVIOR_PACKAGE_MUSIC = "com.android.music";
    private static final String HW_BEHAVIOR_PACKAGE_PHONE = "com.android.phone";
    private static final String HW_BEHAVIOR_PACKAGE_SETTINGS = "com.android.settings";
    private static final String HW_BEHAVIOR_PACKAGE_VENDING = "com.android.vending";
    private static final int PARAM_SEQ_0 = 0;
    private static final int PARAM_SEQ_1 = 1;
    private static final int PARAM_SEQ_10 = 10;
    private static final int PARAM_SEQ_11 = 11;
    private static final int PARAM_SEQ_12 = 12;
    private static final int PARAM_SEQ_13 = 13;
    private static final int PARAM_SEQ_14 = 14;
    private static final int PARAM_SEQ_15 = 15;
    private static final int PARAM_SEQ_16 = 16;
    private static final int PARAM_SEQ_17 = 17;
    private static final int PARAM_SEQ_18 = 18;
    private static final int PARAM_SEQ_19 = 19;
    private static final int PARAM_SEQ_2 = 2;
    private static final int PARAM_SEQ_20 = 20;
    private static final int PARAM_SEQ_21 = 21;
    private static final int PARAM_SEQ_22 = 22;
    private static final int PARAM_SEQ_23 = 23;
    private static final int PARAM_SEQ_24 = 24;
    private static final int PARAM_SEQ_25 = 25;
    private static final int PARAM_SEQ_26 = 26;
    private static final int PARAM_SEQ_27 = 27;
    private static final int PARAM_SEQ_28 = 28;
    private static final int PARAM_SEQ_29 = 29;
    private static final int PARAM_SEQ_3 = 3;
    private static final int PARAM_SEQ_32 = 32;
    private static final int PARAM_SEQ_4 = 4;
    private static final int PARAM_SEQ_5 = 5;
    private static final int PARAM_SEQ_6 = 6;
    private static final int PARAM_SEQ_7 = 7;
    private static final int PARAM_SEQ_8 = 8;
    private static final int PARAM_SEQ_9 = 9;
    private static final int PARAM_SEQ_BASE = 0;
    private static String TAG = "BehaviorCollectManager";
    private static IHwBehaviorCollectManager inst = null;
    private static boolean traceFlag = false;
    private static final String version = "1.0.0";
    private IAppBehaviorDataAnalyzer dataAnalyzerService;
    private IInspectAppObserver inspectAppObserver = new InnerAppObserver();
    private Map<Integer, Integer> inspectUidMap = new HashMap();

    /* renamed from: huawei.android.security.HwBehaviorCollectManagerImpl$3 */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId = new int[BehaviorId.values().length];

        static {
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.WINDOWNMANAGER_ADDWINDOW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.PACKAGEMANAGER_SETCOMPONENTENABLEDSETTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.CONTENT_REGISTERCONTENTOBSERVER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.ACTIVITYSTARTER_STARTACTIVITYMAYWAIT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.ACTIVITYMANAGER_BROADCASTINTENT.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.BROADCASTQUEUE_PROCESSCURBROADCASTLOCKED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.BROADCASTQUEUE_PERFORMRECEIVELOCKED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.BROADCASTQUEUE_FINISHRECEIVERLOCKED.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.CONTENTPROVIDER_UPDATE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.CONTENTPROVIDER_QUERY.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.CONTENTPROVIDER_INSERT.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[BehaviorId.CONTENTPROVIDER_DELETE.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    private class InnerAppObserver extends Stub {
        InnerAppObserver() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InnerAppObserver new");
            stringBuilder.append(Process.myPid());
            HwBehaviorCollectManagerImpl.this.trace("I", stringBuilder.toString());
        }

        public void updateInspectUid(Map uids) throws RemoteException {
            Map<Integer, Integer> loseUidMap = new HashMap();
            try {
                loseUidMap.putAll(HwBehaviorCollectManagerImpl.this.inspectUidMap);
                for (Integer uid : uids.values()) {
                    if (HwBehaviorCollectManagerImpl.this.inspectUidMap.get(uid) != null) {
                        loseUidMap.remove(uid);
                    } else {
                        HwBehaviorCollectManagerImpl.this.inspectUidMap.put(uid, uid);
                    }
                }
                for (Integer value : loseUidMap.values()) {
                    HwBehaviorCollectManagerImpl.this.inspectUidMap.remove(value);
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed updateInspectUid:");
                stringBuilder.append(e);
                HwBehaviorCollectManagerImpl.this.trace("E", stringBuilder.toString());
            }
        }

        public String getVersion() throws RemoteException {
            return HwBehaviorCollectManagerImpl.this.getVersionInfo();
        }
    }

    private HwBehaviorCollectManagerImpl() {
        if (SystemProperties.get("ro.config.aiprotection").equals("true")) {
            timerDiscoverService();
        }
        traceFlag = SystemProperties.get("ro.config.aiprotection.debug").equals("true");
    }

    public static IHwBehaviorCollectManager getDefault() {
        if (inst != null) {
            return inst;
        }
        inst = new HwBehaviorCollectManagerImpl();
        return inst;
    }

    private IAppBehaviorDataAnalyzer getService() {
        return this.dataAnalyzerService;
    }

    private void timerDiscoverService() {
        new Timer().schedule(new TimerTask() {
            private int timerCount = 0;

            public void run() {
                this.timerCount++;
                if (this.timerCount >= 5) {
                    this.timerCount = 0;
                    HwBehaviorCollectManagerImpl.this.bindAnalyzerService();
                }
            }
        }, 1000, 1000);
    }

    private void bindAnalyzerService() {
        if (this.dataAnalyzerService == null) {
            IBinder binder = ServiceManager.getService(IAppBehaviorDataAnalyzer.class.getName());
            if (binder != null) {
                this.dataAnalyzerService = IAppBehaviorDataAnalyzer.Stub.asInterface(binder);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bindAnalyzerService: ");
                stringBuilder.append(this.dataAnalyzerService);
                stringBuilder.append(" pid:");
                stringBuilder.append(Process.myPid());
                Log.i(str, stringBuilder.toString());
                try {
                    binder.linkToDeath(new DeathRecipient() {
                        public void binderDied() {
                            Log.i(HwBehaviorCollectManagerImpl.TAG, "binderDied");
                            HwBehaviorCollectManagerImpl.this.dataAnalyzerService = null;
                            HwBehaviorCollectManagerImpl.this.inspectUidMap.clear();
                        }
                    }, 0);
                } catch (RemoteException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("linkToDeath error:");
                    stringBuilder2.append(e);
                    trace("E", stringBuilder2.toString());
                }
                registerCallBack(this.dataAnalyzerService);
            }
        }
    }

    private void registerCallBack(IAppBehaviorDataAnalyzer service) {
        if (service == null) {
            Log.e(TAG, "registerCallBack with null service");
            return;
        }
        try {
            service.regObservInspectUid(HwBehaviorCollectManagerImpl.class.getSimpleName(), this.inspectAppObserver);
            this.inspectAppObserver.updateInspectUid(service.getInspectAppMap());
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed registerCallBack:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
    }

    private String getVersionInfo() {
        return version;
    }

    private boolean checkActiveUid(int uid) {
        if (uid < 10000) {
            return false;
        }
        try {
            if (this.inspectUidMap.get(Integer.valueOf(uid)) != null) {
                return true;
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed checkActiveUid:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
        return false;
    }

    public void sendBehavior(BehaviorId bid) {
        try {
            if (getService() != null) {
                if (!this.inspectUidMap.isEmpty()) {
                    int finalBid = bid.getValue();
                    int uid = Binder.getCallingUid();
                    if (checkActiveUid(uid)) {
                        sendBehaviorDataToAnalyzer(uid, Binder.getCallingPid(), finalBid);
                    }
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed sendBehavior:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
    }

    public void sendBehavior(BehaviorId bid, Object... params) {
        try {
            if (getService() != null) {
                if (!this.inspectUidMap.isEmpty()) {
                    int uid = Binder.getCallingUid();
                    if (checkActiveUid(uid)) {
                        sendBehaviorParamParse(uid, Binder.getCallingPid(), bid, params);
                    }
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed sendBehavior with params:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
    }

    public void sendBehavior(int uid, int pid, BehaviorId bid) {
        try {
            if (getService() != null) {
                if (!this.inspectUidMap.isEmpty()) {
                    if (checkActiveUid(uid)) {
                        sendBehaviorDataToAnalyzer(uid, pid, bid.getValue());
                    }
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed sendBehavior uid[");
            stringBuilder.append(uid);
            stringBuilder.append("]:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
    }

    public void sendBehavior(int uid, int pid, BehaviorId bid, Object... params) {
        try {
            if (getService() != null) {
                if (!this.inspectUidMap.isEmpty()) {
                    if (checkActiveUid(uid)) {
                        sendBehaviorParamParse(uid, pid, bid, params);
                    }
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed sendBehavior uid[");
            stringBuilder.append(uid);
            stringBuilder.append("] with params:");
            stringBuilder.append(e);
            trace("E", stringBuilder.toString());
        }
    }

    public void sendEvent(int event, int uid, int pid, String packageName, String installer) {
        if (event == 2 || uid >= 10000) {
            try {
                if (this.dataAnalyzerService != null) {
                    this.dataAnalyzerService.onAppEvent(event, uid, pid, packageName, installer);
                }
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendEvent error");
                stringBuilder.append(e);
                trace("E", stringBuilder.toString());
                this.dataAnalyzerService = null;
            }
        }
    }

    private void sendBehaviorParamParse(int uid, int pid, BehaviorId bid, Object... params) {
        Integer paramSeq = null;
        switch (AnonymousClass3.$SwitchMap$huawei$android$security$IHwBehaviorCollectManager$BehaviorId[bid.ordinal()]) {
            case 1:
                paramSeq = getParamSeqWithaddWindow(params);
                break;
            case 2:
                paramSeq = getParamSeqWithsetComponentEnabledSetting(params);
                break;
            case 3:
                paramSeq = getParamSeqWithregisterContentObserver(params);
                break;
            case 4:
                paramSeq = getParamSeqWithstartActivityMayWait(params);
                break;
            case 5:
                paramSeq = getParamSeqWithbroadcastIntent(params);
                break;
            case 6:
                paramSeq = getParamSeqWithprocessCurBroadcastLocked(params);
                break;
            case 7:
                paramSeq = getParamSeqWithperformReceiveLocked(params);
                break;
            case 8:
                paramSeq = getParamSeqWithfinishReceiverLocked(params);
                break;
            case 9:
                paramSeq = getParamSeqWithTransportquery(params);
                break;
            case 10:
                paramSeq = getParamSeqWithTransportupdate(params);
                break;
            case 11:
                paramSeq = getParamSeqWithTransportinsert(params);
                break;
            case 12:
                paramSeq = getParamSeqWithTransportdelete(params);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendBehaviorParamParse Err Bid:");
                stringBuilder.append(bid.getValue());
                stringBuilder.append("with extra param!");
                trace("E", stringBuilder.toString());
                break;
        }
        if (paramSeq != null) {
            sendBehaviorDataToAnalyzer(uid, pid, bid.getValue() + paramSeq.intValue());
        }
    }

    private void sendBehaviorDataToAnalyzer(int uid, int pid, int finalBid) {
        if (this.dataAnalyzerService != null) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onBehaviorEvent: uid(");
                stringBuilder.append(uid);
                stringBuilder.append(") pid(");
                stringBuilder.append(pid);
                stringBuilder.append(") bid(");
                stringBuilder.append(finalBid);
                stringBuilder.append(")");
                trace("I", stringBuilder.toString());
                this.dataAnalyzerService.onBehaviorEvent(uid, pid, finalBid);
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendBehaviorDataToAnalyzer error");
                stringBuilder2.append(e);
                trace("E", stringBuilder2.toString());
                this.dataAnalyzerService = null;
            }
        }
    }

    private Integer getParamSeqWithaddWindow(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithAddWindow length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof LayoutParams) {
            LayoutParams LayoutParams = params[0];
            paramSeq = getAddWindowTypeSeq(LayoutParams.type);
            if (paramSeq == null) {
                return null;
            }
            int width = LayoutParams.width;
            int height = LayoutParams.height;
            if (!(width == -1 && height == -1)) {
                paramSeq = Integer.valueOf(paramSeq.intValue() + 29);
            }
        }
        return paramSeq;
    }

    private Integer getAddWindowTypeSeq(int type) {
        Integer paramSeq = null;
        if (type == 99) {
            paramSeq = Integer.valueOf(4);
        } else if (type == 2030) {
            paramSeq = Integer.valueOf(26);
        } else if (type == 2032) {
            paramSeq = Integer.valueOf(27);
        } else if (type != 2038) {
            switch (type) {
                case 1:
                    paramSeq = Integer.valueOf(0);
                    break;
                case 2:
                    paramSeq = Integer.valueOf(1);
                    break;
                case 3:
                    paramSeq = Integer.valueOf(2);
                    break;
                case 4:
                    paramSeq = Integer.valueOf(3);
                    break;
                default:
                    switch (type) {
                        case 1000:
                            paramSeq = Integer.valueOf(5);
                            break;
                        case 1001:
                            paramSeq = Integer.valueOf(6);
                            break;
                        case 1002:
                            paramSeq = Integer.valueOf(7);
                            break;
                        case 1003:
                            paramSeq = Integer.valueOf(8);
                            break;
                        case 1004:
                            paramSeq = Integer.valueOf(9);
                            break;
                        case 1005:
                            paramSeq = Integer.valueOf(10);
                            break;
                        default:
                            switch (type) {
                                case 2000:
                                    paramSeq = Integer.valueOf(11);
                                    break;
                                case ConstantValue.transaction_hangupCalling /*2001*/:
                                    paramSeq = Integer.valueOf(12);
                                    break;
                                case 2002:
                                    paramSeq = Integer.valueOf(13);
                                    break;
                                case 2003:
                                    paramSeq = Integer.valueOf(14);
                                    break;
                                case 2004:
                                    paramSeq = Integer.valueOf(15);
                                    break;
                                case 2005:
                                    paramSeq = Integer.valueOf(16);
                                    break;
                                case 2006:
                                    paramSeq = Integer.valueOf(17);
                                    break;
                                case 2007:
                                    paramSeq = Integer.valueOf(18);
                                    break;
                                case 2008:
                                    paramSeq = Integer.valueOf(19);
                                    break;
                                case 2009:
                                    paramSeq = Integer.valueOf(20);
                                    break;
                                case 2010:
                                    paramSeq = Integer.valueOf(21);
                                    break;
                                case 2011:
                                    paramSeq = Integer.valueOf(22);
                                    break;
                                case 2012:
                                    paramSeq = Integer.valueOf(23);
                                    break;
                                case HwWifiChrMsgID.EVENT_WIFI_FIRMLOG_TRIGGER /*2013*/:
                                    paramSeq = Integer.valueOf(24);
                                    break;
                                case 2014:
                                    paramSeq = Integer.valueOf(25);
                                    break;
                            }
                            break;
                    }
            }
        } else {
            paramSeq = Integer.valueOf(28);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAddWindowTypeSeq:");
        stringBuilder.append(type);
        trace("I", stringBuilder.toString());
        return paramSeq;
    }

    private Integer getParamSeqWithsetComponentEnabledSetting(Object... params) {
        Integer paramSeq = null;
        StringBuilder stringBuilder;
        if (params.length != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithsetComponentEnabledSetting length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Integer) {
            int state = ((Integer) params[0]).intValue();
            switch (state) {
                case 0:
                    paramSeq = Integer.valueOf(0);
                    break;
                case 1:
                    paramSeq = Integer.valueOf(1);
                    break;
                case 2:
                    paramSeq = Integer.valueOf(2);
                    break;
                case 3:
                    paramSeq = Integer.valueOf(3);
                    break;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithsetComponentEnabledSetting:");
            stringBuilder.append(state);
            trace("I", stringBuilder.toString());
        }
        return paramSeq;
    }

    private Integer getParamSeqWithregisterContentObserver(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithregisterContentObserver length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Uri) {
            paramSeq = getContentObserverUriSeq(params[0]);
        }
        return paramSeq;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Integer getContentObserverUriSeq(Uri uri) {
        int i;
        String authority = uri.getAuthority();
        switch (authority.hashCode()) {
            case -845193793:
                if (authority.equals(HW_BEHAVIOR_PACKAGE_CONTACTS)) {
                    i = 2;
                    break;
                }
            case -456066902:
                if (authority.equals(HW_BEHAVIOR_PACKAGE_CALENDAR)) {
                    i = 3;
                    break;
                }
            case -172298781:
                if (authority.equals("call_log")) {
                    i = 0;
                    break;
                }
            case 114009:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_SMS)) {
                    i = 5;
                    break;
                }
            case 150940456:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_BROWSER)) {
                    i = 1;
                    break;
                }
            case 783201304:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_TELEPHONY)) {
                    i = 4;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return Integer.valueOf(0);
            case 1:
                return Integer.valueOf(1);
            case 2:
                return Integer.valueOf(2);
            case 3:
                return Integer.valueOf(3);
            case 4:
                if (uri.getPath().contains("carriers")) {
                    return Integer.valueOf(4);
                }
                return null;
            case 5:
                return Integer.valueOf(5);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getContentObserverUriSeq:");
                stringBuilder.append(uri.getAuthority());
                trace("I", stringBuilder.toString());
                return null;
        }
    }

    private Integer getParamSeqWithstartActivityMayWait(Object... params) {
        Integer paramSeq = null;
        if (params.length != 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithstartActivityMayWait length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if ((params[0] instanceof Intent) && (params[1] instanceof String)) {
            String callingPackage = params[1];
            paramSeq = getActivityStartActionSeq(params[0].getAction(), callingPackage);
        }
        return paramSeq;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Integer getActivityStartActionSeq(String action, String callingPackage) {
        String str = action;
        String str2 = callingPackage;
        if (str == null || str2 == null) {
            return null;
        }
        int i;
        Integer paramSeq;
        switch (action.hashCode()) {
            case -1405683728:
                if (str.equals("android.app.action.ADD_DEVICE_ADMIN")) {
                    i = 3;
                    break;
                }
            case -1173745501:
                if (str.equals("android.intent.action.CALL")) {
                    i = 5;
                    break;
                }
            case -1173708363:
                if (str.equals("android.intent.action.DIAL")) {
                    i = 12;
                    break;
                }
            case -1173683121:
                if (str.equals("android.intent.action.EDIT")) {
                    i = 2;
                    break;
                }
            case -1173447682:
                if (str.equals("android.intent.action.MAIN")) {
                    i = 13;
                    break;
                }
            case -1173350810:
                if (str.equals("android.intent.action.PICK")) {
                    i = 8;
                    break;
                }
            case -1173264947:
                if (str.equals("android.intent.action.SEND")) {
                    i = 0;
                    break;
                }
            case -1173171990:
                if (str.equals("android.intent.action.VIEW")) {
                    i = 15;
                    break;
                }
            case -570909077:
                if (str.equals("android.intent.action.GET_CONTENT")) {
                    i = 11;
                    break;
                }
            case 239259848:
                if (str.equals("android.intent.action.PICK_ACTIVITY")) {
                    i = 9;
                    break;
                }
            case 1639291568:
                if (str.equals("android.intent.action.DELETE")) {
                    i = 6;
                    break;
                }
            case 1790957502:
                if (str.equals("android.intent.action.INSERT")) {
                    i = 14;
                    break;
                }
            case 1937529752:
                if (str.equals("android.intent.action.WEB_SEARCH")) {
                    i = 1;
                    break;
                }
            case 2038242175:
                if (str.equals("android.intent.action.ATTACH_DATA")) {
                    i = 4;
                    break;
                }
            case 2068413101:
                if (str.equals("android.intent.action.SEARCH")) {
                    i = 7;
                    break;
                }
            case 2068787464:
                if (str.equals("android.intent.action.SENDTO")) {
                    i = 10;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                paramSeq = Integer.valueOf(0);
                break;
            case 1:
                paramSeq = Integer.valueOf(1);
                break;
            case 2:
                paramSeq = Integer.valueOf(2);
                break;
            case 3:
                paramSeq = Integer.valueOf(3);
                break;
            case 4:
                paramSeq = Integer.valueOf(4);
                break;
            case 5:
                paramSeq = Integer.valueOf(5);
                break;
            case 6:
                paramSeq = Integer.valueOf(6);
                break;
            case 7:
                paramSeq = Integer.valueOf(7);
                break;
            case 8:
                paramSeq = Integer.valueOf(8);
                break;
            case 9:
                paramSeq = Integer.valueOf(9);
                break;
            case 10:
                paramSeq = Integer.valueOf(10);
                break;
            case 11:
                paramSeq = Integer.valueOf(11);
                break;
            case 12:
                paramSeq = Integer.valueOf(12);
                break;
            case 13:
                paramSeq = Integer.valueOf(13);
                break;
            case 14:
                paramSeq = Integer.valueOf(14);
                break;
            case 15:
                paramSeq = Integer.valueOf(15 + getActivityStartPackageSeq(str2));
                break;
            default:
                paramSeq = Integer.valueOf(32);
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getActivityStartActionSeq:");
        stringBuilder.append(str);
        trace("I", stringBuilder.toString());
        return paramSeq;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getActivityStartPackageSeq(String callingPackage) {
        Object obj;
        int paramSeq;
        switch (callingPackage.hashCode()) {
            case -1590748058:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_GALLERY)) {
                    obj = 9;
                    break;
                }
            case -1558913047:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_HTMLVIEWER)) {
                    obj = null;
                    break;
                }
            case -1253172024:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_CALENDAR2)) {
                    obj = 13;
                    break;
                }
            case -1243492292:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_BROWSER)) {
                    obj = 6;
                    break;
                }
            case -1046965711:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_VENDING)) {
                    obj = 5;
                    break;
                }
            case -845193793:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_CONTACTS)) {
                    obj = 8;
                    break;
                }
            case -695601689:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_MMS)) {
                    obj = 1;
                    break;
                }
            case -456066902:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_CALENDAR)) {
                    obj = 7;
                    break;
                }
            case 256457446:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_CHROME)) {
                    obj = 14;
                    break;
                }
            case 285500553:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_DIALER)) {
                    obj = 3;
                    break;
                }
            case 299475319:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_GALLERY3D)) {
                    obj = 4;
                    break;
                }
            case 394871662:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_INSTALLER)) {
                    obj = 12;
                    break;
                }
            case 536280232:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_MARKET)) {
                    obj = 10;
                    break;
                }
            case 1156888975:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_SETTINGS)) {
                    obj = 15;
                    break;
                }
            case 1541916729:
                if (callingPackage.equals(HW_BEHAVIOR_PACKAGE_MUSIC)) {
                    obj = 11;
                    break;
                }
            case 1544296322:
                if (callingPackage.equals("com.android.phone")) {
                    obj = 2;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                paramSeq = 0;
                break;
            case 1:
                paramSeq = 1;
                break;
            case 2:
                paramSeq = 2;
                break;
            case 3:
                paramSeq = 3;
                break;
            case 4:
                paramSeq = 4;
                break;
            case 5:
                paramSeq = 5;
                break;
            case 6:
                paramSeq = 6;
                break;
            case 7:
                paramSeq = 7;
                break;
            case 8:
                paramSeq = 8;
                break;
            case 9:
                paramSeq = 9;
                break;
            case 10:
                paramSeq = 10;
                break;
            case 11:
                paramSeq = 11;
                break;
            case 12:
                paramSeq = 12;
                break;
            case 13:
                paramSeq = 13;
                break;
            case 14:
                paramSeq = 14;
                break;
            case 15:
                paramSeq = 15;
                break;
            default:
                paramSeq = 16;
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getActivityStartPackageSeq:");
        stringBuilder.append(callingPackage);
        trace("I", stringBuilder.toString());
        return paramSeq;
    }

    private Integer getParamSeqWithbroadcastIntent(Object... params) {
        Integer paramSeq = null;
        StringBuilder stringBuilder;
        if (params.length != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithbroadcastIntent length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Intent) {
            String action = params[0].getAction();
            if (action != null) {
                int i = -1;
                switch (action.hashCode()) {
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            i = 4;
                            break;
                        }
                        break;
                    case -1513032534:
                        if (action.equals("android.intent.action.TIME_TICK")) {
                            i = 2;
                            break;
                        }
                        break;
                    case -311830893:
                        if (action.equals(HW_BEHAVIOR_ACTION_INSTALL_SHORTCUT)) {
                            i = 6;
                            break;
                        }
                        break;
                    case 172491798:
                        if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 505380757:
                        if (action.equals("android.intent.action.TIME_SET")) {
                            i = 3;
                            break;
                        }
                        break;
                    case 525384130:
                        if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                            i = 1;
                            break;
                        }
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            i = 5;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        paramSeq = Integer.valueOf(0);
                        break;
                    case 1:
                        paramSeq = Integer.valueOf(1);
                        break;
                    case 2:
                        paramSeq = Integer.valueOf(2);
                        break;
                    case 3:
                        paramSeq = Integer.valueOf(3);
                        break;
                    case 4:
                        paramSeq = Integer.valueOf(4);
                        break;
                    case 5:
                        paramSeq = Integer.valueOf(5);
                        break;
                    case 6:
                        paramSeq = Integer.valueOf(6);
                        break;
                    default:
                        paramSeq = Integer.valueOf(7);
                        break;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("getParamSeqWithbroadcastIntent:");
                stringBuilder.append(action);
                trace("I", stringBuilder.toString());
            }
        }
        return paramSeq;
    }

    private Integer getParamSeqWithprocessCurBroadcastLocked(Object... params) {
        Integer paramSeq = null;
        StringBuilder stringBuilder;
        if (params.length != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithprocessCurBroadcastLocked length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Intent) {
            String action = params[0].getAction();
            if (action != null) {
                int i = -1;
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            i = 5;
                            break;
                        }
                        break;
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
                            i = 6;
                            break;
                        }
                        break;
                    case -1173745501:
                        if (action.equals("android.intent.action.CALL")) {
                            i = 1;
                            break;
                        }
                        break;
                    case -1173708363:
                        if (action.equals("android.intent.action.DIAL")) {
                            i = 2;
                            break;
                        }
                        break;
                    case 798292259:
                        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 1280305535:
                        if (action.equals(HW_BEHAVIOR_ACTION_SMS_RECEIVED)) {
                            i = 7;
                            break;
                        }
                        break;
                    case 1901012141:
                        if (action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
                            i = 4;
                            break;
                        }
                        break;
                    case 1948416196:
                        if (action.equals("android.intent.action.CREATE_SHORTCUT")) {
                            i = 3;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        paramSeq = Integer.valueOf(0);
                        break;
                    case 1:
                        paramSeq = Integer.valueOf(1);
                        break;
                    case 2:
                        paramSeq = Integer.valueOf(2);
                        break;
                    case 3:
                        paramSeq = Integer.valueOf(3);
                        break;
                    case 4:
                        paramSeq = Integer.valueOf(4);
                        break;
                    case 5:
                        paramSeq = Integer.valueOf(5);
                        break;
                    case 6:
                        paramSeq = Integer.valueOf(6);
                        break;
                    case 7:
                        paramSeq = Integer.valueOf(7);
                        break;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("getParamSeqWithprocessCurBroadcastLocked:");
                stringBuilder.append(action);
                trace("I", stringBuilder.toString());
            }
        }
        return paramSeq;
    }

    private Integer getParamSeqWithperformReceiveLocked(Object... params) {
        Integer paramSeq = null;
        StringBuilder stringBuilder;
        if (params.length != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithperformReceiveLocked length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Intent) {
            String action = params[0].getAction();
            if (action != null) {
                int i = -1;
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            i = 5;
                            break;
                        }
                        break;
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
                            i = 6;
                            break;
                        }
                        break;
                    case -1173745501:
                        if (action.equals("android.intent.action.CALL")) {
                            i = 1;
                            break;
                        }
                        break;
                    case -1173708363:
                        if (action.equals("android.intent.action.DIAL")) {
                            i = 2;
                            break;
                        }
                        break;
                    case 798292259:
                        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 1280305535:
                        if (action.equals(HW_BEHAVIOR_ACTION_SMS_RECEIVED)) {
                            i = 7;
                            break;
                        }
                        break;
                    case 1901012141:
                        if (action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
                            i = 4;
                            break;
                        }
                        break;
                    case 1948416196:
                        if (action.equals("android.intent.action.CREATE_SHORTCUT")) {
                            i = 3;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        paramSeq = Integer.valueOf(0);
                        break;
                    case 1:
                        paramSeq = Integer.valueOf(1);
                        break;
                    case 2:
                        paramSeq = Integer.valueOf(2);
                        break;
                    case 3:
                        paramSeq = Integer.valueOf(3);
                        break;
                    case 4:
                        paramSeq = Integer.valueOf(4);
                        break;
                    case 5:
                        paramSeq = Integer.valueOf(5);
                        break;
                    case 6:
                        paramSeq = Integer.valueOf(6);
                        break;
                    case 7:
                        paramSeq = Integer.valueOf(7);
                        break;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("getParamSeqWithperformReceiveLocked:");
                stringBuilder.append(action);
                trace("I", stringBuilder.toString());
            }
        }
        return paramSeq;
    }

    private Integer getParamSeqWithfinishReceiverLocked(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithfinishReceiverLocked length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Intent) {
            if (params[0].getFlags() == StubController.RHD_PERMISSION_CODE) {
                paramSeq = Integer.valueOf(0);
            } else {
                paramSeq = Integer.valueOf(1);
            }
        }
        return paramSeq;
    }

    private Integer getParamSeqWithTransportquery(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithTransportquery length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Uri) {
            paramSeq = getContentProviderUriSeq(params[0]);
        }
        return paramSeq;
    }

    private Integer getParamSeqWithTransportupdate(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithTransportupdate length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Uri) {
            paramSeq = getContentProviderUriSeq(params[0]);
        }
        return paramSeq;
    }

    private Integer getParamSeqWithTransportinsert(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithTransportinsert length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Uri) {
            paramSeq = getContentProviderUriSeq(params[0]);
        }
        return paramSeq;
    }

    private Integer getParamSeqWithTransportdelete(Object... params) {
        Integer paramSeq = null;
        if (params.length != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParamSeqWithTransportdelete length:");
            stringBuilder.append(params.length);
            stringBuilder.append("Err!");
            trace("E", stringBuilder.toString());
            return null;
        }
        if (params[0] instanceof Uri) {
            paramSeq = getContentProviderUriSeq(params[0]);
        }
        return paramSeq;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Integer getContentProviderUriSeq(Uri uri) {
        int i;
        String authority = uri.getAuthority();
        switch (authority.hashCode()) {
            case -845193793:
                if (authority.equals(HW_BEHAVIOR_PACKAGE_CONTACTS)) {
                    i = 2;
                    break;
                }
            case -567451565:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_CONTACTS)) {
                    i = 1;
                    break;
                }
            case -456066902:
                if (authority.equals(HW_BEHAVIOR_PACKAGE_CALENDAR)) {
                    i = 3;
                    break;
                }
            case -172298781:
                if (authority.equals("call_log")) {
                    i = 0;
                    break;
                }
            case 114009:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_SMS)) {
                    i = 5;
                    break;
                }
            case 783201304:
                if (authority.equals(HW_BEHAVIOR_AUTHORITY_TELEPHONY)) {
                    i = 4;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return Integer.valueOf(0);
            case 1:
                return Integer.valueOf(1);
            case 2:
                return Integer.valueOf(2);
            case 3:
                return Integer.valueOf(3);
            case 4:
                return Integer.valueOf(4);
            case 5:
                return Integer.valueOf(5);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getContentProviderUriSeq:");
                stringBuilder.append(uri.getAuthority());
                trace("I", stringBuilder.toString());
                return null;
        }
    }

    private void trace(String level, String msg) {
        if (traceFlag) {
            Object obj = -1;
            int hashCode = level.hashCode();
            if (hashCode != 73) {
                switch (hashCode) {
                    case 68:
                        if (level.equals("D")) {
                            obj = 1;
                            break;
                        }
                        break;
                    case ConnectivityLogManager.GPS_POS_TIMEOUT_EVENT_EX /*69*/:
                        if (level.equals("E")) {
                            obj = null;
                            break;
                        }
                        break;
                }
            } else if (level.equals("I")) {
                obj = 2;
            }
            switch (obj) {
                case null:
                    Log.e(TAG, msg);
                    return;
                case 1:
                    Log.d(TAG, msg);
                    return;
                case 2:
                    Log.i(TAG, msg);
                    return;
                default:
                    return;
            }
        }
    }
}
