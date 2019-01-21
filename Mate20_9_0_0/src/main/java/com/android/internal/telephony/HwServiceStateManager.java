package com.android.internal.telephony;

import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.rms.HwSysResManager;
import android.rms.iaware.NetLocationStrategy;
import android.telephony.CarrierConfigManager;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.HwLog;
import com.android.internal.telephony.cdma.HwCdmaServiceStateManager;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.gsm.HwGsmServiceStateManager;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatusUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccCardApplicationUtils;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HwServiceStateManager extends Handler {
    protected static final int CT_NUM_MATCH_HOME = 11;
    protected static final int CT_NUM_MATCH_ROAMING = 10;
    protected static final int CT_SID_1st_END = 14335;
    protected static final int CT_SID_1st_START = 13568;
    protected static final int CT_SID_2nd_END = 26111;
    protected static final int CT_SID_2nd_START = 25600;
    protected static final int DEFAULT_SID = 0;
    private static final String DUAL_CARDS_SETTINGS_ACTIVITY = "com.huawei.settings.intent.DUAL_CARD_SETTINGS";
    private static final int DUAL_SIM = 2;
    protected static final int EVENT_DELAY_UPDATE_REGISTER_STATE_DONE = 0;
    protected static final int EVENT_ICC_RECORDS_EONS_UPDATED = 1;
    private static final int EVENT_NETWORK_REJINFO_DONE = 156;
    private static final int EVENT_NETWORK_REJINFO_TIMEOUT = 157;
    protected static final int EVENT_RESUME_DATA = 203;
    protected static final int EVENT_SET_PRE_NETWORKTYPE = 202;
    private static final int EVENT_SIM_HOTPLUG = 158;
    private static final String EXTRA_SHOW_WIFI = "showWifi";
    private static final String EXTRA_WIFI = "wifi";
    private static final int INTERVAL_TIME = 120000;
    protected static final String INVAILD_PLMN = "1023127-123456-1023456-123127-";
    private static final int INVALID = -1;
    protected static final boolean IS_CHINATELECOM = SystemProperties.get("ro.config.hw_opta", "0").equals("92");
    protected static final boolean IS_MULTI_SIM_ENABLED = TelephonyManager.getDefault().isMultiSimEnabled();
    private static final String KEY_WFC_FORMAT_WIFI_STRING = "wfc_format_wifi_string";
    private static final String KEY_WFC_HIDE_WIFI_BOOL = "wfc_hide_wifi_bool";
    private static final String KEY_WFC_IS_SHOW_AIRPLANE = "wfc_is_show_air_plane";
    private static final String KEY_WFC_IS_SHOW_EMERGENCY_ONLY = "wfc_is_show_emergency_only";
    private static final String KEY_WFC_IS_SHOW_NO_SERVICE = "wfc_is_show_no_service";
    private static final String KEY_WFC_SPN_STRING = "wfc_spn_string";
    private static final int MAX_TOP_PACKAGE = 10;
    protected static final long MODE_REQUEST_CELL_LIST_STRATEGY_INVALID = -1;
    protected static final long MODE_REQUEST_CELL_LIST_STRATEGY_VALID = 0;
    private static final int[] NETWORK_REJINFO_CAUSES = new int[]{7, 11, 12, 13, 14, 15, 17};
    private static final int NETWORK_REJINFO_MAX_COUNT = 3;
    private static final int NETWORK_REJINFO_MAX_TIME = 1200000;
    private static final String NETWORK_REJINFO_NOTIFY_CHANNEL = "network_rejinfo_notify_channel";
    private static final int REJINFO_LEN = 4;
    protected static final int RESUME_DATA_TIME = 8000;
    protected static final int SET_PRE_NETWORK_TIME = 5000;
    protected static final int SET_PRE_NETWORK_TIME_DELAY = 2000;
    protected static final int SPN_RULE_SHOW_BOTH = 3;
    protected static final int SPN_RULE_SHOW_PLMN_ONLY = 2;
    protected static final int SPN_RULE_SHOW_PNN_PRIOR = 4;
    protected static final int SPN_RULE_SHOW_SPN_ONLY = 1;
    protected static final int SPN_RULE_SHOW_SPN_PRIOR = 5;
    private static final String TAG = "HwServiceStateManager";
    protected static final long VALUE_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    protected static final int VALUE_SCREEN_OFF_TIME_DEFAULT = 10;
    private static final int WIFI_IDX = 1;
    private static Map<Object, HwCdmaServiceStateManager> cdmaServiceStateManagers = new HashMap();
    private static Map<Object, HwGsmServiceStateManager> gsmServiceStateManagers = new HashMap();
    private static final boolean isScreenOffNotUpdateLocation = SystemProperties.getBoolean("ro.config.updatelocation", false);
    private static Map<Object, HwServiceStateManager> serviceStateManagers = new HashMap();
    protected static final UiccCardApplicationUtils uiccCardApplicationUtils = new UiccCardApplicationUtils();
    private static final boolean voice_reg_state_for_ons = "true".equals(SystemProperties.get("ro.hwpp.voice_reg_state_for_ons", "false"));
    protected int DELAYED_TIME_DEFAULT_VALUE = SystemProperties.getInt("ro.lostnetwork.default_timer", 20);
    protected int DELAYED_TIME_NETWORKSTATUS_CS_2G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_cs2G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    protected int DELAYED_TIME_NETWORKSTATUS_CS_3G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_cs3G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    protected int DELAYED_TIME_NETWORKSTATUS_CS_4G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_cs4G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    protected int DELAYED_TIME_NETWORKSTATUS_PS_2G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_ps2G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    protected int DELAYED_TIME_NETWORKSTATUS_PS_3G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_ps3G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    protected int DELAYED_TIME_NETWORKSTATUS_PS_4G = (SystemProperties.getInt("ro.lostnetwork.delaytimer_ps4G", this.DELAYED_TIME_DEFAULT_VALUE) * 1000);
    private Map<String, Integer> mCellInfoMap = new HashMap();
    private Context mContext;
    protected boolean mCurShowWifi = false;
    protected String mCurWifi = "";
    protected int mMainSlot;
    private boolean mNeedHandleRejInfoFlag = true;
    private NotificationManager mNotificationManager;
    protected int mPendingPreNwType = 0;
    protected Message mPendingsavemessage;
    private Phone mPhoneBase;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
                int slotId = intent.getIntExtra("subscription", -1);
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                if (slotId == HwServiceStateManager.this.mPhoneBase.getPhoneId() && serviceState.getState() == 0) {
                    HwServiceStateManager.this.clearNetworkRejInfo();
                    HwServiceStateManager.this.mNotificationManager.cancel(HwServiceStateManager.TAG, slotId);
                }
            }
        }
    };
    protected boolean mRefreshState = false;
    private ServiceStateTracker mServiceStateTracker;
    protected boolean mSetPreNwTypeRequested = false;
    private long mStartTime = MODE_REQUEST_CELL_LIST_STRATEGY_VALID;
    private ArrayList<String> networkRejInfoList = new ArrayList();

    private enum HotplugState {
        STATE_PLUG_OUT,
        STATE_PLUG_IN
    }

    protected HwServiceStateManager(Phone phoneBase) {
        super(Looper.getMainLooper());
        this.mPhoneBase = phoneBase;
        this.mContext = phoneBase.getContext();
        initNetworkRejInfo();
    }

    public String getPlmn() {
        return "";
    }

    public void sendDualSimUpdateSpnIntent(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            String str;
            StringBuilder stringBuilder;
            Intent intent = null;
            int phoneId = this.mPhoneBase.getPhoneId();
            if (phoneId == 0) {
                intent = new Intent("android.intent.action.ACTION_DSDS_SUB1_OPERATOR_CHANGED");
            } else if (1 == phoneId) {
                intent = new Intent("android.intent.action.ACTION_DSDS_SUB2_OPERATOR_CHANGED");
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unsupport SUB ID :");
                stringBuilder.append(phoneId);
                Rlog.e(str, stringBuilder.toString());
            }
            if (intent != null) {
                intent.addFlags(536870912);
                intent.putExtra("showSpn", showSpn);
                intent.putExtra("spn", spn);
                intent.putExtra("showPlmn", showPlmn);
                intent.putExtra("plmn", plmn);
                intent.putExtra("subscription", phoneId);
                intent.putExtra(EXTRA_SHOW_WIFI, this.mCurShowWifi);
                intent.putExtra(EXTRA_WIFI, this.mCurWifi);
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Send updateSpnIntent for SUB :");
                stringBuilder.append(phoneId);
                Rlog.d(str, stringBuilder.toString());
            }
        }
    }

    public OnsDisplayParams getOnsDisplayParamsHw(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        return new OnsDisplayParams(showSpn, showPlmn, rule, plmn, spn);
    }

    public HwServiceStateManager(ServiceStateTracker serviceStateTracker, Phone phoneBase) {
        super(Looper.getMainLooper());
        this.mPhoneBase = phoneBase;
        this.mServiceStateTracker = serviceStateTracker;
        this.mContext = phoneBase.getContext();
        initNetworkRejInfo();
    }

    public static synchronized HwServiceStateManager getHwServiceStateManager(ServiceStateTracker serviceStateTracker, Phone phoneBase) {
        HwServiceStateManager hwServiceStateManager;
        synchronized (HwServiceStateManager.class) {
            hwServiceStateManager = (HwServiceStateManager) serviceStateManagers.get(serviceStateTracker);
            if (hwServiceStateManager == null) {
                hwServiceStateManager = new HwServiceStateManager(serviceStateTracker, phoneBase);
                serviceStateManagers.put(serviceStateTracker, hwServiceStateManager);
            }
        }
        return hwServiceStateManager;
    }

    public static synchronized HwGsmServiceStateManager getHwGsmServiceStateManager(ServiceStateTracker serviceStateTracker, GsmCdmaPhone phone) {
        HwGsmServiceStateManager hwGsmServiceStateManager;
        synchronized (HwServiceStateManager.class) {
            hwGsmServiceStateManager = (HwGsmServiceStateManager) gsmServiceStateManagers.get(serviceStateTracker);
            if (hwGsmServiceStateManager == null) {
                hwGsmServiceStateManager = new HwGsmServiceStateManager(serviceStateTracker, phone);
                gsmServiceStateManagers.put(serviceStateTracker, hwGsmServiceStateManager);
            }
        }
        return hwGsmServiceStateManager;
    }

    public static synchronized HwCdmaServiceStateManager getHwCdmaServiceStateManager(ServiceStateTracker serviceStateTracker, GsmCdmaPhone phone) {
        HwCdmaServiceStateManager hwCdmaServiceStateManager;
        synchronized (HwServiceStateManager.class) {
            hwCdmaServiceStateManager = (HwCdmaServiceStateManager) cdmaServiceStateManagers.get(serviceStateTracker);
            if (hwCdmaServiceStateManager == null) {
                hwCdmaServiceStateManager = new HwCdmaServiceStateManager(serviceStateTracker, phone);
                cdmaServiceStateManagers.put(serviceStateTracker, hwCdmaServiceStateManager);
            }
        }
        return hwCdmaServiceStateManager;
    }

    public static synchronized void dispose(ServiceStateTracker serviceStateTracker) {
        synchronized (HwServiceStateManager.class) {
            if (serviceStateTracker == null) {
                return;
            }
            HwGsmServiceStateManager hwGsmServiceStateManager = (HwGsmServiceStateManager) gsmServiceStateManagers.get(serviceStateTracker);
            if (hwGsmServiceStateManager != null) {
                hwGsmServiceStateManager.dispose();
            }
            gsmServiceStateManagers.put(serviceStateTracker, null);
            HwCdmaServiceStateManager hwCdmaServiceStateManager = (HwCdmaServiceStateManager) cdmaServiceStateManagers.get(serviceStateTracker);
            if (hwCdmaServiceStateManager != null) {
                hwCdmaServiceStateManager.dispose();
            }
            cdmaServiceStateManagers.put(serviceStateTracker, null);
        }
    }

    public int getCombinedRegState(ServiceState serviceState) {
        if (serviceState == null) {
            return 1;
        }
        int regState = serviceState.getVoiceRegState();
        int dataRegState = serviceState.getDataRegState();
        if (voice_reg_state_for_ons && serviceState.getRilDataRadioTechnology() != 14 && serviceState.getRilDataRadioTechnology() != 19) {
            return regState;
        }
        if (regState == 1 && dataRegState == 0) {
            Rlog.d(TAG, "getCombinedRegState: return STATE_IN_SERVICE as Data is in service");
            regState = dataRegState;
        }
        return regState;
    }

    public void processCTNumMatch(boolean roaming, UiccCardApplication uiccCardApplication) {
    }

    protected void checkMultiSimNumMatch() {
        matchArray = new int[4];
        int i = 2;
        matchArray[2] = SystemProperties.getInt("gsm.hw.matchnum1", -1);
        matchArray[3] = SystemProperties.getInt("gsm.hw.matchnum.short1", -1);
        Arrays.sort(matchArray);
        int numMatch = matchArray[3];
        int numMatchShort = numMatch;
        while (i >= 0) {
            if (matchArray[i] < numMatch && matchArray[i] > 0) {
                numMatchShort = matchArray[i];
            }
            i--;
        }
        SystemProperties.set("gsm.hw.matchnum", Integer.toString(numMatch));
        SystemProperties.set("gsm.hw.matchnum.short", Integer.toString(numMatchShort));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkMultiSimNumMatch: after setprop numMatch = ");
        stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum", 0));
        stringBuilder.append(", numMatchShort = ");
        stringBuilder.append(SystemProperties.getInt("gsm.hw.matchnum.short", 0));
        Rlog.d(str, stringBuilder.toString());
    }

    protected void setCTNumMatchHomeForSlot(int slotId) {
        if (IS_MULTI_SIM_ENABLED) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gsm.hw.matchnum");
            stringBuilder.append(slotId);
            SystemProperties.set(stringBuilder.toString(), Integer.toString(11));
            stringBuilder = new StringBuilder();
            stringBuilder.append("gsm.hw.matchnum.short");
            stringBuilder.append(slotId);
            SystemProperties.set(stringBuilder.toString(), Integer.toString(11));
            checkMultiSimNumMatch();
            return;
        }
        SystemProperties.set("gsm.hw.matchnum", Integer.toString(11));
        SystemProperties.set("gsm.hw.matchnum.short", Integer.toString(11));
    }

    protected void setCTNumMatchRoamingForSlot(int slotId) {
        if (IS_MULTI_SIM_ENABLED) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gsm.hw.matchnum");
            stringBuilder.append(slotId);
            SystemProperties.set(stringBuilder.toString(), Integer.toString(10));
            stringBuilder = new StringBuilder();
            stringBuilder.append("gsm.hw.matchnum.short");
            stringBuilder.append(slotId);
            SystemProperties.set(stringBuilder.toString(), Integer.toString(10));
            checkMultiSimNumMatch();
            return;
        }
        SystemProperties.set("gsm.hw.matchnum", Integer.toString(10));
        SystemProperties.set("gsm.hw.matchnum.short", Integer.toString(10));
    }

    public static boolean isCustScreenOff(GsmCdmaPhone phoneBase) {
        if (!(!isScreenOffNotUpdateLocation || phoneBase == null || phoneBase.getContext() == null)) {
            PowerManager powerManager = (PowerManager) phoneBase.getContext().getSystemService("power");
            if (!(powerManager == null || powerManager.isScreenOn())) {
                Rlog.d(TAG, " ScreenOff do nothing");
                return true;
            }
        }
        return false;
    }

    public void setOOSFlag(boolean flag) {
    }

    private void setPreferredNetworkType(int networkType, int phoneId, Message response) {
        this.mPhoneBase.mCi.setPreferredNetworkType(networkType, response);
    }

    public void setPreferredNetworkTypeSafely(Phone phoneBase, int networkType, Message response) {
        this.mPhoneBase = phoneBase;
        DcTracker dcTracker = this.mPhoneBase.mDcTracker;
        if (this.mServiceStateTracker == null) {
            Rlog.d(TAG, "mServiceStateTracker is null, it is unexpected!");
        }
        if (networkType != 10) {
            if (this.mSetPreNwTypeRequested) {
                removeMessages(202);
                Rlog.d(TAG, "cancel setPreferredNetworkType");
            }
            this.mSetPreNwTypeRequested = false;
            Rlog.d(TAG, "PreNetworkType is not LTE, setPreferredNetworkType now!");
            setPreferredNetworkType(networkType, this.mPhoneBase.getPhoneId(), response);
        } else if (!this.mSetPreNwTypeRequested) {
            if (dcTracker.isDisconnected()) {
                setPreferredNetworkType(networkType, this.mPhoneBase.getPhoneId(), response);
                Rlog.d(TAG, "data is Disconnected, setPreferredNetworkType now!");
                return;
            }
            dcTracker.setInternalDataEnabled(false);
            Rlog.d(TAG, "Data is disabled and wait up to 8s to resume data.");
            sendMessageDelayed(obtainMessage(203), 8000);
            this.mPendingsavemessage = response;
            this.mPendingPreNwType = networkType;
            Message msg = Message.obtain(this);
            msg.what = 202;
            msg.arg1 = networkType;
            msg.obj = response;
            Rlog.d(TAG, "Wait up to 5s for data disconnect to setPreferredNetworkType.");
            sendMessageDelayed(msg, 5000);
            this.mSetPreNwTypeRequested = true;
        }
    }

    public void checkAndSetNetworkType() {
        if (this.mSetPreNwTypeRequested) {
            Rlog.d(TAG, "mSetPreNwTypeRequested is true and wait a few seconds to setPreferredNetworkType");
            removeMessages(202);
            Message msg = Message.obtain(this);
            msg.what = 202;
            msg.arg1 = this.mPendingPreNwType;
            msg.obj = this.mPendingsavemessage;
            sendMessageDelayed(msg, 2000);
            return;
        }
        Rlog.d(TAG, "No need to setPreferredNetworkType");
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        switch (i) {
            case EVENT_NETWORK_REJINFO_DONE /*156*/:
                logd("EVENT_NETWORK_REJINFO_DONE");
                onNetworkRejInfoDone(msg);
                return;
            case EVENT_NETWORK_REJINFO_TIMEOUT /*157*/:
                logd("EVENT_NETWORK_REJINFO_TIMEOUT");
                onNetworkRejInfoTimeout(msg);
                return;
            case EVENT_SIM_HOTPLUG /*158*/:
                logd("EVENT_SIM_HOTPLUG");
                onSimHotPlug(msg);
                return;
            default:
                switch (i) {
                    case 202:
                        if (this.mSetPreNwTypeRequested) {
                            Rlog.d(TAG, "EVENT_SET_PRE_NETWORKTYPE, setPreferredNetworkType now.");
                            setPreferredNetworkType(msg.arg1, this.mPhoneBase.getPhoneId(), (Message) msg.obj);
                            this.mSetPreNwTypeRequested = false;
                            return;
                        }
                        Rlog.d(TAG, "No need to setPreferredNetworkType");
                        return;
                    case 203:
                        this.mPhoneBase.mDcTracker.setInternalDataEnabled(true);
                        Rlog.d(TAG, "EVENT_RESUME_DATA, resume data now.");
                        return;
                    default:
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unhandled message with number: ");
                        stringBuilder.append(msg.what);
                        Rlog.d(str, stringBuilder.toString());
                        return;
                }
        }
    }

    public boolean isCardInvalid(boolean isSubDeactivated, int subId) {
        CardState newState = CardState.CARDSTATE_ABSENT;
        UiccCard newCard = UiccController.getInstance().getUiccCard(subId);
        if (newCard != null) {
            newState = newCard.getCardState();
        }
        boolean isCardPresent = IccCardStatusUtils.isCardPresent(newState);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCardPresent : ");
        stringBuilder.append(isCardPresent);
        stringBuilder.append("  subId : ");
        stringBuilder.append(subId);
        Rlog.d(str, stringBuilder.toString());
        return !isCardPresent || isSubDeactivated;
    }

    protected OnsDisplayParams getOnsDisplayParamsForVoWifi(OnsDisplayParams ons) {
        boolean useGoogleWifiFormat;
        String combineWifi;
        String combineWifi2;
        boolean z;
        String str;
        RuntimeException e;
        String combineWifi3;
        StringBuilder stringBuilder;
        OnsDisplayParams onsDisplayParams = ons;
        int voiceIdx = 0;
        String spnConfiged = "";
        boolean hideWifi = false;
        String wifiConfiged = "";
        boolean isShowNoService = false;
        boolean isShowEmergency = false;
        boolean isShowAirplane = false;
        CarrierConfigManager configLoader = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configLoader != null) {
            try {
                PersistableBundle b = configLoader.getConfigForSubId(this.mPhoneBase.getSubId());
                if (b != null) {
                    voiceIdx = b.getInt("wfc_spn_format_idx_int");
                    spnConfiged = b.getString(KEY_WFC_SPN_STRING);
                    hideWifi = b.getBoolean(KEY_WFC_HIDE_WIFI_BOOL);
                    wifiConfiged = b.getString(KEY_WFC_FORMAT_WIFI_STRING);
                    isShowNoService = b.getBoolean(KEY_WFC_IS_SHOW_NO_SERVICE);
                    isShowEmergency = b.getBoolean(KEY_WFC_IS_SHOW_EMERGENCY_ONLY);
                    isShowAirplane = b.getBoolean(KEY_WFC_IS_SHOW_AIRPLANE);
                }
            } catch (Exception e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getGsmOnsDisplayParams: carrier config error: ");
                stringBuilder2.append(e2);
                Rlog.e(str2, stringBuilder2.toString());
            }
        }
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("updateSpnDisplay, voiceIdx = ");
        stringBuilder3.append(voiceIdx);
        stringBuilder3.append(" spnConfiged = ");
        stringBuilder3.append(spnConfiged);
        stringBuilder3.append(" hideWifi = ");
        stringBuilder3.append(hideWifi);
        stringBuilder3.append(" wifiConfiged = ");
        stringBuilder3.append(wifiConfiged);
        stringBuilder3.append(" isShowNoService = ");
        stringBuilder3.append(isShowNoService);
        stringBuilder3.append(" isShowEmergency = ");
        stringBuilder3.append(isShowEmergency);
        stringBuilder3.append(" isShowAirplane = ");
        stringBuilder3.append(isShowAirplane);
        Rlog.d(str3, stringBuilder3.toString());
        str3 = "%s";
        if (!hideWifi) {
            useGoogleWifiFormat = voiceIdx == 1;
            String[] wfcSpnFormats = this.mContext.getResources().getStringArray(17236092);
            if (!TextUtils.isEmpty(wifiConfiged)) {
                str3 = wifiConfiged;
            } else if (!useGoogleWifiFormat || wfcSpnFormats == null) {
                str3 = this.mContext.getResources().getString(17041233);
            } else {
                str3 = wfcSpnFormats[1];
            }
        }
        String formatWifi = str3;
        str3 = "";
        useGoogleWifiFormat = getCombinedRegState(this.mServiceStateTracker.mSS) == 0;
        boolean noService = false;
        boolean emergencyOnly = false;
        String combineWifi4 = str3;
        boolean airplaneMode = Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        int combinedRegState = getCombinedRegState(this.mServiceStateTracker.mSS);
        if (combinedRegState == 1 || combinedRegState == 2) {
            if (this.mServiceStateTracker.mSS == null || !this.mServiceStateTracker.mSS.isEmergencyOnly()) {
                noService = true;
            } else {
                emergencyOnly = true;
            }
        }
        if (!TextUtils.isEmpty(spnConfiged)) {
            str3 = spnConfiged;
        } else if (!TextUtils.isEmpty(onsDisplayParams.mSpn)) {
            str3 = onsDisplayParams.mSpn;
        } else if (!useGoogleWifiFormat || TextUtils.isEmpty(onsDisplayParams.mPlmn)) {
            str3 = combineWifi4;
        } else {
            str3 = onsDisplayParams.mPlmn;
        }
        if (airplaneMode && isShowAirplane) {
            str3 = Resources.getSystem().getText(17040141).toString();
        } else {
            combineWifi = str3;
            if (noService && isShowNoService) {
                str3 = Resources.getSystem().getText(17040350).toString();
            } else {
                if (emergencyOnly && isShowEmergency) {
                    str3 = Resources.getSystem().getText(17039987).toString();
                }
                combineWifi2 = String.format(formatWifi, new Object[]{combineWifi});
                try {
                    onsDisplayParams.mWifi = combineWifi2.trim();
                    onsDisplayParams.mShowWifi = true;
                    z = airplaneMode;
                    str = spnConfiged;
                } catch (RuntimeException e3) {
                    e = e3;
                    combineWifi3 = combineWifi2;
                    combineWifi2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("combine wifi fail, ");
                    stringBuilder.append(e);
                    Rlog.e(combineWifi2, stringBuilder.toString());
                    combineWifi2 = combineWifi3;
                    return onsDisplayParams;
                }
                return onsDisplayParams;
            }
        }
        combineWifi = str3;
        try {
            combineWifi2 = String.format(formatWifi, new Object[]{combineWifi});
            onsDisplayParams.mWifi = combineWifi2.trim();
            onsDisplayParams.mShowWifi = true;
            z = airplaneMode;
            str = spnConfiged;
        } catch (RuntimeException e4) {
            e = e4;
            combineWifi2 = combineWifi;
            combineWifi3 = combineWifi2;
            combineWifi2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("combine wifi fail, ");
            stringBuilder.append(e);
            Rlog.e(combineWifi2, stringBuilder.toString());
            combineWifi2 = combineWifi3;
            return onsDisplayParams;
        }
        return onsDisplayParams;
    }

    public void countPackageUseCellInfo(String packageName) {
        if (this.mPhoneBase != null) {
            boolean isMainSub = this.mPhoneBase.getSubId() == HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            if (isMainSub || this.mCellInfoMap.size() != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("countPackageUseCellInfo packageName is :");
                stringBuilder.append(packageName);
                Rlog.d(str, stringBuilder.toString());
                if (TextUtils.isEmpty(packageName)) {
                    Rlog.d(TAG, "countPackageUseCellInfo packageName is null");
                    return;
                }
                if (this.mStartTime == MODE_REQUEST_CELL_LIST_STRATEGY_VALID) {
                    this.mStartTime = SystemClock.elapsedRealtime();
                }
                if (Math.abs(SystemClock.elapsedRealtime() - this.mStartTime) >= 120000) {
                    str = "";
                    int topPackageNum = 0;
                    int count = this.mCellInfoMap.size();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("countPackageUseCellInfo size:");
                    stringBuilder2.append(count);
                    Rlog.d(str2, stringBuilder2.toString());
                    for (Entry<String, Integer> entry : this.mCellInfoMap.entrySet()) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(str);
                        stringBuilder3.append(" name=");
                        stringBuilder3.append((String) entry.getKey());
                        stringBuilder3.append(" num=");
                        stringBuilder3.append(entry.getValue());
                        str = stringBuilder3.toString();
                        topPackageNum++;
                        StringBuilder stringBuilder4;
                        String str3;
                        if (10 == topPackageNum) {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("count=10");
                            stringBuilder4.append(str);
                            HwLog.dubaie("DUBAI_TAG_LOCATION_COUNTER", stringBuilder4.toString());
                            str3 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("countPackageUseCellInfo topPackageString:count=10");
                            stringBuilder4.append(str);
                            Rlog.d(str3, stringBuilder4.toString());
                            topPackageNum = 0;
                            str = "";
                            count -= 10;
                        } else if (count / 10 == 0) {
                            count--;
                            if (count == 0) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("count=");
                                stringBuilder4.append(topPackageNum);
                                stringBuilder4.append(str);
                                HwLog.dubaie("DUBAI_TAG_LOCATION_COUNTER", stringBuilder4.toString());
                                str3 = TAG;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("countPackageUseCellInfo topPackageString:count=");
                                stringBuilder4.append(topPackageNum);
                                stringBuilder4.append(str);
                                Rlog.d(str3, stringBuilder4.toString());
                            }
                        }
                    }
                    this.mStartTime = SystemClock.elapsedRealtime();
                    this.mCellInfoMap.clear();
                }
                boolean isCharging = false;
                BatteryManager batteryManager = (BatteryManager) this.mPhoneBase.getContext().getSystemService("batterymanager");
                if (batteryManager != null) {
                    isCharging = batteryManager.isCharging();
                }
                if (isMainSub && isCustScreenOff((GsmCdmaPhone) this.mPhoneBase) && !isCharging) {
                    if (this.mCellInfoMap.containsKey(packageName)) {
                        this.mCellInfoMap.put(packageName, Integer.valueOf(((Integer) this.mCellInfoMap.get(packageName)).intValue() + 1));
                    } else {
                        this.mCellInfoMap.put(packageName, Integer.valueOf(1));
                    }
                }
            }
        }
    }

    private boolean isCellAgeTimePassed(ServiceStateTracker stateTracker, GsmCdmaPhone phoneBase) {
        long curSysTime = SystemClock.elapsedRealtime();
        long lastRequestTime = stateTracker.getLastCellInfoListTime();
        boolean isScreenOff = isCustScreenOff(phoneBase);
        int screenOffTimes = SystemProperties.getInt("ro.config.screen_off_times", 10);
        long cellInfoListMaxAgeTime = 2000;
        if (isScreenOff) {
            cellInfoListMaxAgeTime = 2000 * ((long) screenOffTimes);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCellAgeTimePassed(): isScreenOff=");
        stringBuilder.append(isScreenOff);
        stringBuilder.append(" cellInfoListMaxAgeTime=");
        stringBuilder.append(cellInfoListMaxAgeTime);
        stringBuilder.append("ms.");
        Rlog.d(str, stringBuilder.toString());
        if (curSysTime - lastRequestTime > cellInfoListMaxAgeTime) {
            Rlog.d(TAG, "isCellAgeTimePassed():return true.");
            return true;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("isCellAgeTimePassed():return false,because already requested CellInfoList within ");
        stringBuilder.append(cellInfoListMaxAgeTime);
        stringBuilder.append("ms.");
        Rlog.d(str, stringBuilder.toString());
        return false;
    }

    public boolean isCellRequestStrategyPassed(ServiceStateTracker stateTracker, WorkSource workSource, GsmCdmaPhone phoneBase) {
        ServiceStateTracker serviceStateTracker = stateTracker;
        WorkSource workSource2 = workSource;
        GsmCdmaPhone gsmCdmaPhone = phoneBase;
        if (serviceStateTracker == null || workSource2 == null || gsmCdmaPhone == null || TextUtils.isEmpty(workSource2.getName(0))) {
            Rlog.e(TAG, "isCellRequestStrategyPassed():return false.Because null-pointer params");
            return false;
        }
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm != null && tm.getSimState(this.mPhoneBase.getSubId()) != 5) {
            Rlog.d(TAG, "isCellRequestStrategyPassed():return false.Because sim state not ready.");
            return false;
        } else if (isCellAgeTimePassed(serviceStateTracker, gsmCdmaPhone)) {
            long curSysTime = SystemClock.elapsedRealtime();
            long lastRequestTime = stateTracker.getLastCellInfoListTime();
            String pkgName = workSource2.getName(0);
            int uid = workSource2.get(0);
            long id = Binder.clearCallingIdentity();
            NetLocationStrategy strategy = HwSysResManager.getInstance().getNetLocationStrategy(pkgName, uid, 2);
            Binder.restoreCallingIdentity(id);
            if (strategy != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isCellRequestStrategyPassed():get iAware strategy result = ");
                stringBuilder.append(strategy.toString());
                Rlog.d(str, stringBuilder.toString());
                if (MODE_REQUEST_CELL_LIST_STRATEGY_INVALID == strategy.getCycle()) {
                    Rlog.d(TAG, "isCellRequestStrategyPassed():return false.Because iAware strategy return NOT_ALLOWED");
                    return false;
                } else if (MODE_REQUEST_CELL_LIST_STRATEGY_VALID >= strategy.getCycle() || curSysTime - lastRequestTime >= strategy.getCycle()) {
                    Rlog.d(TAG, "isCellRequestStrategyPassed():return true.");
                    return true;
                } else {
                    Rlog.d(TAG, "isCellRequestStrategyPassed():return false.Because already requested within iAware strategy cycle");
                    return false;
                }
            }
            Rlog.e(TAG, "isCellRequestStrategyPassed():get iAware strategy result = null.");
            return true;
        } else {
            Rlog.d(TAG, "isCellRequestStrategyPassed():return false.Because isCellAgeTime is not passed.");
            return false;
        }
    }

    public int getNetworkType(ServiceState ss) {
        if (ss.getDataNetworkType() != 0) {
            return ss.getDataNetworkType();
        }
        return ss.getVoiceNetworkType();
    }

    private void initNetworkRejInfo() {
        if (TelephonyManager.getDefault().getSimCount() == 2) {
            this.mPhoneBase.mCi.setOnNetReject(this, EVENT_NETWORK_REJINFO_DONE, null);
            this.mPhoneBase.mCi.registerForSimHotPlug(this, EVENT_SIM_HOTPLUG, Integer.valueOf(this.mPhoneBase.getPhoneId()));
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SERVICE_STATE"));
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(NETWORK_REJINFO_NOTIFY_CHANNEL, this.mContext.getString(33686105), 3));
        }
    }

    private void onNetworkRejInfoDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar.exception == null) {
            String[] data = ar.result;
            String rejectplmn = null;
            int rejectdomain = -1;
            int rejectcause = -1;
            int rejectrat = -1;
            if (data.length >= 4) {
                try {
                    if (data[0] != null && data[0].length() > 0) {
                        rejectplmn = data[0];
                    }
                    if (data[1] != null && data[1].length() > 0) {
                        rejectdomain = Integer.parseInt(data[1]);
                    }
                    if (data[2] != null && data[2].length() > 0) {
                        rejectcause = Integer.parseInt(data[2]);
                    }
                    if (data[3] != null && data[3].length() > 0) {
                        rejectrat = Integer.parseInt(data[3]);
                    }
                } catch (Exception ex) {
                    Rlog.e(TAG, "error parsing NetworkReject!", ex);
                }
                handleNetworkRejinfoNotification(rejectplmn, rejectdomain, rejectcause, rejectrat);
            }
        }
    }

    private void onNetworkRejInfoTimeout(Message msg) {
        if (this.networkRejInfoList.size() > 3) {
            showNetworkRejInfoNotification();
            clearNetworkRejInfo();
            return;
        }
        this.networkRejInfoList.clear();
    }

    private void clearNetworkRejInfo() {
        logd("clearNetworkRejInfo");
        this.networkRejInfoList.clear();
        removeMessages(EVENT_NETWORK_REJINFO_TIMEOUT);
        setNeedHandleRejInfoFlag(false);
    }

    /* JADX WARNING: Missing block: B:19:0x007f, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:0x0081, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void handleNetworkRejinfoNotification(String rejectplmn, int rejectdomain, int rejectcause, int rejectrat) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleNetworkRejinfoNotification:PLMN = ");
        stringBuilder.append(rejectplmn);
        stringBuilder.append(" domain = ");
        stringBuilder.append(rejectdomain);
        stringBuilder.append(" cause = ");
        stringBuilder.append(rejectcause);
        stringBuilder.append(" RAT = ");
        stringBuilder.append(rejectrat);
        logd(stringBuilder.toString());
        boolean needHandleRejectCause = Arrays.binarySearch(NETWORK_REJINFO_CAUSES, rejectcause) >= 0;
        if (this.mNeedHandleRejInfoFlag && !TextUtils.isEmpty(rejectplmn)) {
            if (needHandleRejectCause) {
                if (!hasMessages(EVENT_NETWORK_REJINFO_TIMEOUT)) {
                    sendEmptyMessageDelayed(EVENT_NETWORK_REJINFO_TIMEOUT, 1200000);
                }
                if (!this.networkRejInfoList.contains(rejectplmn)) {
                    this.networkRejInfoList.add(rejectplmn);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleNetworkRejinfoNotification: add ");
                    stringBuilder2.append(rejectplmn);
                    stringBuilder2.append(" rejinfoList:");
                    stringBuilder2.append(this.networkRejInfoList);
                    logd(stringBuilder2.toString());
                }
            }
        }
    }

    private void showNetworkRejInfoNotification() {
        if (this.mContext != null) {
            logd("showNetworkRejinfoNotification");
            Intent resultIntent = new Intent(DUAL_CARDS_SETTINGS_ACTIVITY);
            resultIntent.setFlags(335544320);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this.mContext, 0, resultIntent, 134217728);
            int showSubId = this.mPhoneBase.getPhoneId() + 1;
            this.mNotificationManager.notify(TAG, this.mPhoneBase.getPhoneId(), new Builder(this.mContext).setSmallIcon(33751960).setAppName(this.mContext.getString(33686105)).setWhen(System.currentTimeMillis()).setShowWhen(true).setAutoCancel(true).setDefaults(-1).setContentTitle(this.mContext.getString(33686109, new Object[]{Integer.valueOf(showSubId)})).setContentText(this.mContext.getString(33686108, new Object[]{Integer.valueOf(showSubId)})).setContentIntent(resultPendingIntent).setStyle(new BigTextStyle()).setChannelId(NETWORK_REJINFO_NOTIFY_CHANNEL).addAction(new Action.Builder(null, this.mContext.getString(33686104), resultPendingIntent).build()).build());
        }
    }

    private void onSimHotPlug(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null && ar.result != null && ((int[]) ar.result).length > 0) {
            if (HotplugState.STATE_PLUG_IN.ordinal() == ((int[]) ar.result)[0]) {
                setNeedHandleRejInfoFlag(true);
            } else if (HotplugState.STATE_PLUG_OUT.ordinal() == ((int[]) ar.result)[0]) {
                clearNetworkRejInfo();
            }
        }
    }

    private void setNeedHandleRejInfoFlag(boolean needHandleRejInfoFlag) {
        if (this.mNeedHandleRejInfoFlag != needHandleRejInfoFlag) {
            this.mNeedHandleRejInfoFlag = needHandleRejInfoFlag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set mNeedHandleRejInfoFlag =");
            stringBuilder.append(this.mNeedHandleRejInfoFlag);
            logd(stringBuilder.toString());
        }
    }

    private void logd(String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SUB");
        stringBuilder.append(this.mPhoneBase.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }
}
