package com.android.internal.telephony.gsm;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.ToneGenerator;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.provider.SettingsEx.Systemex;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CsgSearch;
import com.android.internal.telephony.CsgSearchFactory;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HwCustGsmServiceStateTrackerImpl extends HwCustGsmServiceStateTracker {
    public static final String ACTION_LTE_CA_STATE = "com.huawei.intent.action.LTE_CA_STATE";
    private static final int CAUSE_BY_DATA = 0;
    private static final int CAUSE_BY_ROAM = 1;
    private static final int CONGESTTION = 22;
    public static final int CS_ENABLED = 1003;
    public static final int CS_NORMAL_ENABLED = 1005;
    private static final int DATA_DISABLED = 0;
    private static final int DATA_ENABLED = 1;
    protected static final boolean DBG = true;
    private static final String DEFAULT_PCO_DATA = "-2;-2";
    private static final int DIALOG_TIMEOUT = 120000;
    private static final int EPS_SERVICES_AND_NON_EPS_SERVICES_NOT_ALLOWED = 8;
    private static final int ESM_FAILURE = 19;
    protected static final int EVENT_GET_CELL_INFO_LIST_OTDOA = 73;
    private static final int EVENT_GET_LTE_FREQ_WITH_WLAN_COEX = 63;
    protected static final int EVENT_POLL_STATE_REGISTRATION = 4;
    protected static final int EVENT_SET_NETWORK_MODE_DONE = 101;
    private static final int FOCUS_BEEP_VOLUME = 100;
    private static final int GPRS_SERVICES_NOT_ALLOWED = 7;
    private static final int GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN = 14;
    private static final boolean HW_ATT_SHOW_NET_REJ = SystemProperties.getBoolean("ro.config.hw_showNetworkReject", false);
    private static final boolean HW_SIM_ACTIVATION = SystemProperties.getBoolean("ro.config.hw_sim_activation", false);
    private static final int ILLEGAL_ME = 6;
    private static final int ILLEGAL_MS = 3;
    private static final int IMSI_UNKNOWN_IN_HLR = 2;
    private static final boolean IS_DATAONLY_LOCATION_ENABLED = SystemProperties.getBoolean("ro.config.hw_gmap_enabled", false);
    private static final boolean IS_DELAY_UPDATENAME = SystemProperties.getBoolean("ro.config.delay_updatename", false);
    private static final boolean IS_DELAY_UPDATENAME_LAC_NULL = SystemProperties.getBoolean("ro.config.lac_null_delay_update", false);
    private static final boolean IS_EMERGENCY_SHOWS_NOSERVICE = SystemProperties.getBoolean("ro.config.LTE_NO_SERVICE", false);
    public static final boolean IS_KT;
    public static final boolean IS_LGU;
    private static final boolean IS_SIM_POWER_DOWN = SystemProperties.getBoolean("ro.config.SimPowerOperation", false);
    private static final boolean IS_VERIZON;
    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    private static final int LA_NOT_ALLOWED = 12;
    private static final String LOG_TAG = "HwCustGsmServiceStateTrackerImpl";
    private static final String MANUAL_SELECT_FLAG = "manual_select_flag";
    private static final String MANUAL_SET_3G_FLAG = "manual_set_3g_flag";
    private static final int MSC_TEMPORARILY_NOT_REACHABLE = 16;
    private static final int MSG_ID_TIMEOUT = 1;
    private static final int NATIONAL_ROAMING_NOT_ALLOWED = 13;
    private static final int NETWORK_FAILURE = 17;
    private static final int NO_SUITABLE_CELLS_IN_LA = 15;
    private static final String PCO_DATA = "pco_data";
    private static final int PLMN_NOT_ALLOWED = 11;
    private static final int RAT_LTE = 2;
    private static final int RAT_WCDMA = 1;
    private static final int REQUESTED_SERVICE_NOT_AUTHORIZED = 35;
    private static final int ROAM_DISABLED = 0;
    private static final int ROAM_ENABLED = 1;
    private static final boolean SHOW_REJ_NOTIFICATION_KO = SystemProperties.getBoolean("ro.config.show_rej_kt", false);
    private static final int STATE_ENABLED = 1;
    private static final int UNKNOWN_STATE = -1;
    private static final boolean UPDATE_LAC_CID = SystemProperties.getBoolean("ro.config.hw_update_lac_cid", false);
    static final boolean VDBG = false;
    private static boolean mIsSupportCsgSearch = SystemProperties.getBoolean("ro.config.att.csg", false);
    private String ACTION_ENFORCE_LTE_NETWORKTYPE;
    private int ENFORCE_LTE_NETWORKTYPE_PENDING_TIME;
    private int dialogCanceled;
    private PendingIntent enforcePendingLTENetworkTypeIntent;
    private boolean is_ext_plmn_sent;
    private boolean[] lteEmmCauseRecorded;
    protected CommandsInterface mCi;
    private CsgSearch mCsgSrch;
    private ContentObserver mDataEnabledObserver;
    private ContentObserver mDataRoamingObserver;
    private boolean mEnforceLTEPending;
    private BroadcastReceiver mIntentReceiver;
    private boolean mIsCaState;
    private boolean mIsDataChanged;
    private boolean mIsLTEBandWidthChanged;
    private boolean mIsRoamingChanged;
    private List<CellInfo> mLastEnhancedCellInfoList;
    private long mLastEnhancedCellInfoListTime;
    Handler mMyHandler;
    protected int mSetPreferredNetworkType;
    OnCancelListener mShowRejMsgOnCancelListener;
    OnKeyListener mShowRejMsgOnKeyListener;
    private String mSimRecordVoicemail = "";
    Handler mTimeoutHandler;
    private ToneGenerator mToneGenerator = null;
    private String mUlbwDlbwString;
    private AlertDialog networkDialog = null;
    private int oldRejCode;

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        private CellInfoResult() {
            this.lockObj = new Object();
        }

        /* synthetic */ CellInfoResult(HwCustGsmServiceStateTrackerImpl x0, AnonymousClass1 x1) {
            this();
        }
    }

    static {
        boolean equals = SystemProperties.get("ro.config.hw_opta", "0").equals("627");
        boolean z = DBG;
        equals = (equals && SystemProperties.get("ro.config.hw_optb", "0").equals("410")) ? DBG : false;
        IS_LGU = equals;
        equals = (SystemProperties.get("ro.config.hw_opta", "0").equals("710") && SystemProperties.get("ro.config.hw_optb", "0").equals("410")) ? DBG : false;
        IS_KT = equals;
        if (!("389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb")))) {
            z = false;
        }
        IS_VERIZON = z;
    }

    private void onDataRoamingChanged() {
        int dataRoaming = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "data_roaming", 0);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("data roaming change to:");
        stringBuilder.append(dataRoaming);
        Rlog.d(str, stringBuilder.toString());
        if (dataRoaming == 1) {
            this.mIsRoamingChanged = false;
        } else if (!this.mGsmPhone.getServiceState().getDataRoaming()) {
            Rlog.d(LOG_TAG, "device is not roaming , set roam state immediately");
            setDataOrRoamState(1);
        } else if (this.mGsmPhone.mDcTracker.isDisconnected()) {
            Rlog.d(LOG_TAG, "Data disconnected, set roam state immediately");
            setDataOrRoamState(1);
        } else {
            Rlog.d(LOG_TAG, "Data not disconnected, pending");
            enforceLTEPending();
            this.mEnforceLTEPending = DBG;
        }
    }

    private void onDataMobileChanged() {
        int dataEnabled = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mobile data change to:");
        stringBuilder.append(dataEnabled);
        Rlog.d(str, stringBuilder.toString());
        if (dataEnabled == 1) {
            this.mIsDataChanged = false;
        } else if (this.mGsmPhone.mDcTracker.isDisconnected()) {
            Rlog.d(LOG_TAG, "Data disconnected, set mobile stateimmediately");
            setDataOrRoamState(0);
        } else {
            Rlog.d(LOG_TAG, "Data not disconnected, pending");
            enforceLTEPending();
            this.mEnforceLTEPending = DBG;
        }
    }

    private void setDataOrRoamState(int cause) {
        int mobile_data = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
        int data_roaming = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "data_roaming", 0);
        if (cause == 0) {
            if (SystemProperties.get("gsm.isuser.setdata", "true").equals("true")) {
                this.mIsDataChanged = false;
                setMobileDataEnable(mobile_data);
            } else {
                SystemProperties.set("gsm.isuser.setdata", "true");
            }
        }
        if (1 == cause) {
            this.mIsRoamingChanged = false;
            setRoamingDataEnable(data_roaming);
        }
    }

    private void onDataEnableChanged() {
        int dataEnabled = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mobile data change to:");
        stringBuilder.append(dataEnabled);
        Rlog.d(str, stringBuilder.toString());
        if (dataEnabled == 1) {
            Rlog.d(LOG_TAG, "Data on, enforce LTE immediately!");
            enforceLTENetworkType();
        } else if (this.mGsmPhone.mDcTracker.isDisconnected()) {
            Rlog.d(LOG_TAG, "Data disconnected, enforce LTE immediately");
            enforceLTENetworkType();
        } else {
            Rlog.d(LOG_TAG, "Data not disconnected, enforce LTE pending");
            enforceLTEPending();
            this.mEnforceLTEPending = DBG;
        }
    }

    private void enforceLTEPending() {
        if (this.enforcePendingLTENetworkTypeIntent != null) {
            Rlog.d(LOG_TAG, "enforcePendingLTENetworkTypeIntent already exists ,not recreate");
            return;
        }
        AlarmManager alarm = (AlarmManager) this.mGsmPhone.getContext().getSystemService("alarm");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mGsmPhone.getContext(), 0, new Intent(this.ACTION_ENFORCE_LTE_NETWORKTYPE), 134217728);
        alarm.set(2, SystemClock.elapsedRealtime() + ((long) this.ENFORCE_LTE_NETWORKTYPE_PENDING_TIME), alarmIntent);
        this.enforcePendingLTENetworkTypeIntent = alarmIntent;
    }

    public void processEnforceLTENetworkTypePending() {
        if (this.enforcePendingLTENetworkTypeIntent == null && this.mEnforceLTEPending) {
            Rlog.d(LOG_TAG, "No enforce LTE network type pending!");
            return;
        }
        if (DBG == this.mEnforceLTEPending) {
            this.enforcePendingLTENetworkTypeIntent.cancel();
            this.enforcePendingLTENetworkTypeIntent = null;
            int dataEnabled = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Before enforce LTE pending, mobile data is:");
            stringBuilder.append(dataEnabled);
            Rlog.d(str, stringBuilder.toString());
            this.mEnforceLTEPending = false;
            if (isDataOffForbidLTE() && dataEnabled == 0) {
                enforceLTENetworkType();
            }
            if (isDataOffbyRoamAndData()) {
                int roamEnabled = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "data_roaming", 1);
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Before enforce setDataOrRoamState, mobile data is:");
                stringBuilder2.append(dataEnabled);
                stringBuilder2.append(" data roam is ");
                stringBuilder2.append(roamEnabled);
                Rlog.d(str2, stringBuilder2.toString());
                if (roamEnabled == 0 && this.mIsRoamingChanged) {
                    setDataOrRoamState(1);
                }
                if (dataEnabled == 0 && this.mIsDataChanged) {
                    setDataOrRoamState(0);
                }
            }
        }
    }

    private void enforceLTENetworkType() {
        boolean deleteLTE = false;
        int mobile_data = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mobile_data:");
        stringBuilder.append(mobile_data);
        Rlog.d(str, stringBuilder.toString());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mGsmPhone.getContext());
        boolean manual_select_netork_flag = prefs.getBoolean(MANUAL_SELECT_FLAG, false);
        boolean manual_set_3G_flag = prefs.getBoolean(MANUAL_SET_3G_FLAG, false);
        int mNetworkMode = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "preferred_network_mode", LA_NOT_ALLOWED);
        if (mobile_data == 0) {
            deleteLTE = DBG;
            if (manual_select_netork_flag && mNetworkMode == LA_NOT_ALLOWED) {
                deleteLTE = false;
            }
        } else {
            if (mNetworkMode == 2 && (manual_select_netork_flag || manual_set_3G_flag)) {
                deleteLTE = DBG;
            }
            Editor editor = prefs.edit();
            editor.putBoolean(MANUAL_SELECT_FLAG, false);
            editor.commit();
        }
        if (this.mCi instanceof RIL) {
            int networkTypeToSet = getNetworkTypeToSet(deleteLTE);
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("networkTypeSetted==");
            stringBuilder2.append(SystemProperties.getInt("persist.radio.prefered_network", GPRS_SERVICES_NOT_ALLOWED));
            stringBuilder2.append(",networkTypeToSet ==");
            stringBuilder2.append(networkTypeToSet);
            Rlog.d(str2, stringBuilder2.toString());
            if (networkTypeToSet != SystemProperties.getInt("persist.radio.prefered_network", GPRS_SERVICES_NOT_ALLOWED)) {
                this.mCi.setPreferredNetworkType(networkTypeToSet, this.mMyHandler.obtainMessage(EVENT_SET_NETWORK_MODE_DONE, Integer.valueOf(networkTypeToSet)));
            }
        }
    }

    private int getNetworkTypeToSet(boolean deleteLTE) {
        int networkType;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enforceLTENetworkType, deleteLTE: ");
        stringBuilder.append(deleteLTE);
        Rlog.d(str, stringBuilder.toString());
        if (deleteLTE) {
            networkType = deleteLTENetworkType();
        } else {
            networkType = this.mSetPreferredNetworkType;
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mSetPreferredNetworkType:");
        stringBuilder2.append(this.mSetPreferredNetworkType);
        stringBuilder2.append(",networkType:");
        stringBuilder2.append(networkType);
        Rlog.d(str2, stringBuilder2.toString());
        return networkType;
    }

    private int deleteLTENetworkType() {
        int pseudoNetworkType = this.mSetPreferredNetworkType;
        switch (this.mSetPreferredNetworkType) {
            case 8:
                pseudoNetworkType = 4;
                break;
            case CSGNetworkList.RADIO_IF_TDSCDMA /*9*/:
                pseudoNetworkType = 2;
                break;
            case 10:
                pseudoNetworkType = GPRS_SERVICES_NOT_ALLOWED;
                break;
            case PLMN_NOT_ALLOWED /*11*/:
                pseudoNetworkType = 2;
                break;
            case LA_NOT_ALLOWED /*12*/:
                pseudoNetworkType = 2;
                break;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deleteLTENetworkType result=== ");
        stringBuilder.append(pseudoNetworkType);
        Rlog.d(str, stringBuilder.toString());
        return pseudoNetworkType;
    }

    public void initOnce(GsmCdmaPhone phone, CommandsInterface ci) {
        this.mCi = ci;
        phone.getContext().getContentResolver().registerContentObserver(Secure.getUriFor("mobile_data"), DBG, this.mDataEnabledObserver);
        Context context = phone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(this.ACTION_ENFORCE_LTE_NETWORKTYPE);
        context.registerReceiver(this.mIntentReceiver, filter);
        if (isDataOffbyRoamAndData()) {
            phone.getContext().getContentResolver().registerContentObserver(Secure.getUriFor("data_roaming"), DBG, this.mDataRoamingObserver);
            setMobileDataEnable(UNKNOWN_STATE);
            setRoamingDataEnable(UNKNOWN_STATE);
        }
    }

    public void dispose(GsmCdmaPhone phone) {
        phone.getContext().getContentResolver().unregisterContentObserver(this.mDataEnabledObserver);
        if (isDataOffbyRoamAndData()) {
            phone.getContext().getContentResolver().unregisterContentObserver(this.mDataRoamingObserver);
        }
    }

    public boolean isDataOffForbidLTE() {
        return SystemProperties.getBoolean("persist.sys.isDataOffForbidLTE", false);
    }

    public boolean isDataOffbyRoamAndData() {
        return SystemProperties.getBoolean("persist.sys.isDataOffByRAD", false);
    }

    private void setMobileDataEnable(int state) {
        String str;
        StringBuilder stringBuilder;
        if (state == UNKNOWN_STATE) {
            try {
                state = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "mobile_data", 1);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("first set mobile state to ");
                stringBuilder.append(state);
                Rlog.d(str, stringBuilder.toString());
                this.mGsmPhone.mCi.setMobileDataEnable(state, null);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception in setMobileDataEnable", e);
            }
            return;
        }
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("user change mobile state to ");
        stringBuilder.append(state);
        Rlog.d(str, stringBuilder.toString());
        this.mGsmPhone.mCi.setMobileDataEnable(state, null);
    }

    private void setRoamingDataEnable(int state) {
        String str;
        StringBuilder stringBuilder;
        if (state == UNKNOWN_STATE) {
            try {
                state = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "data_roaming", 1);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("first set roam state to ");
                stringBuilder.append(state);
                Rlog.d(str, stringBuilder.toString());
                this.mGsmPhone.mCi.setRoamingDataEnable(state, null);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception in setRoamingDataEnable", e);
            }
            return;
        }
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("user change roam state to ");
        stringBuilder.append(state);
        Rlog.d(str, stringBuilder.toString());
        this.mGsmPhone.mCi.setRoamingDataEnable(state, null);
    }

    public HwCustGsmServiceStateTrackerImpl(GsmCdmaPhone gsmPhone) {
        super(gsmPhone);
        int i = 0;
        this.oldRejCode = 0;
        this.dialogCanceled = 0;
        this.is_ext_plmn_sent = false;
        this.ACTION_ENFORCE_LTE_NETWORKTYPE = "android.intent.action.enforce_lte_networktype";
        this.ENFORCE_LTE_NETWORKTYPE_PENDING_TIME = 10000;
        this.enforcePendingLTENetworkTypeIntent = null;
        this.mSetPreferredNetworkType = SystemProperties.getInt("ro.telephony.default_network", 0);
        this.mIsDataChanged = false;
        this.mIsRoamingChanged = false;
        this.mIsLTEBandWidthChanged = false;
        this.mLastEnhancedCellInfoList = null;
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(HwCustGsmServiceStateTrackerImpl.this.ACTION_ENFORCE_LTE_NETWORKTYPE)) {
                    Rlog.d(HwCustGsmServiceStateTrackerImpl.LOG_TAG, "Enforce LTE pending timer expired!");
                    if (HwCustGsmServiceStateTrackerImpl.DBG == HwCustGsmServiceStateTrackerImpl.this.mEnforceLTEPending) {
                        HwCustGsmServiceStateTrackerImpl.this.processEnforceLTENetworkTypePending();
                    }
                }
            }
        };
        this.mDataEnabledObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Rlog.d(HwCustGsmServiceStateTrackerImpl.LOG_TAG, "Data enabled state changed ");
                if (HwCustGsmServiceStateTrackerImpl.this.isDataOffForbidLTE()) {
                    HwCustGsmServiceStateTrackerImpl.this.onDataEnableChanged();
                }
                if (HwCustGsmServiceStateTrackerImpl.this.isDataOffbyRoamAndData()) {
                    HwCustGsmServiceStateTrackerImpl.this.mIsDataChanged = HwCustGsmServiceStateTrackerImpl.DBG;
                    HwCustGsmServiceStateTrackerImpl.this.onDataMobileChanged();
                }
            }
        };
        this.mDataRoamingObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Rlog.d(HwCustGsmServiceStateTrackerImpl.LOG_TAG, "Data roaming state changed ");
                HwCustGsmServiceStateTrackerImpl.this.mIsRoamingChanged = HwCustGsmServiceStateTrackerImpl.DBG;
                HwCustGsmServiceStateTrackerImpl.this.onDataRoamingChanged();
            }
        };
        this.mMyHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == HwCustGsmServiceStateTrackerImpl.EVENT_SET_NETWORK_MODE_DONE) {
                    AsyncResult ar = msg.obj;
                    if (ar == null || ar.exception != null) {
                        Rlog.e(HwCustGsmServiceStateTrackerImpl.LOG_TAG, "set prefer network mode failed!");
                        return;
                    }
                    int setPrefMode = ((Integer) ar.userObj).intValue();
                    String str = HwCustGsmServiceStateTrackerImpl.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set prefer network mode == ");
                    stringBuilder.append(setPrefMode);
                    Rlog.d(str, stringBuilder.toString());
                    Global.putInt(HwCustGsmServiceStateTrackerImpl.this.mGsmPhone.getContext().getContentResolver(), "preferred_network_mode", setPrefMode);
                }
            }
        };
        this.mTimeoutHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (HwCustGsmServiceStateTrackerImpl.this.dialogCanceled > 0) {
                        HwCustGsmServiceStateTrackerImpl.this.dialogCanceled = HwCustGsmServiceStateTrackerImpl.this.dialogCanceled + HwCustGsmServiceStateTrackerImpl.UNKNOWN_STATE;
                    } else if (HwCustGsmServiceStateTrackerImpl.this.networkDialog != null && HwCustGsmServiceStateTrackerImpl.this.dialogCanceled == 0) {
                        HwCustGsmServiceStateTrackerImpl.this.networkDialog.dismiss();
                        HwCustGsmServiceStateTrackerImpl.this.networkDialog = null;
                        if (HwCustGsmServiceStateTrackerImpl.this.mToneGenerator != null) {
                            HwCustGsmServiceStateTrackerImpl.this.mToneGenerator.release();
                            HwCustGsmServiceStateTrackerImpl.this.mToneGenerator = null;
                        }
                    }
                }
            }
        };
        this.mShowRejMsgOnCancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                HwCustGsmServiceStateTrackerImpl.this.networkDialog = null;
                if (HwCustGsmServiceStateTrackerImpl.this.mToneGenerator != null) {
                    HwCustGsmServiceStateTrackerImpl.this.mToneGenerator.release();
                    HwCustGsmServiceStateTrackerImpl.this.mToneGenerator = null;
                }
                HwCustGsmServiceStateTrackerImpl.this.dialogCanceled = 0;
            }
        };
        this.mShowRejMsgOnKeyListener = new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (82 == keyCode || dialog == null) {
                    return false;
                }
                dialog.dismiss();
                HwCustGsmServiceStateTrackerImpl.this.networkDialog = null;
                if (HwCustGsmServiceStateTrackerImpl.this.mToneGenerator != null) {
                    HwCustGsmServiceStateTrackerImpl.this.mToneGenerator.release();
                    HwCustGsmServiceStateTrackerImpl.this.mToneGenerator = null;
                }
                HwCustGsmServiceStateTrackerImpl.this.dialogCanceled = 0;
                return HwCustGsmServiceStateTrackerImpl.DBG;
            }
        };
        if (CsgSearch.isSupportCsgSearch()) {
            this.mCsgSrch = CsgSearchFactory.createCsgSearch(gsmPhone);
        } else {
            this.mCsgSrch = null;
        }
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        this.lteEmmCauseRecorded = new boolean[phoneCount];
        while (i < phoneCount) {
            this.lteEmmCauseRecorded[i] = DBG;
            i++;
        }
    }

    public void updateRomingVoicemailNumber(ServiceState currentState) {
        String custRoamingVoicemail = null;
        try {
            custRoamingVoicemail = Systemex.getString(this.mContext.getContentResolver(), "hw_cust_roamingvoicemail");
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception get hw_cust_roamingvoicemail value", e);
        }
        if (currentState != null && !TextUtils.isEmpty(custRoamingVoicemail)) {
            IccRecords mIccRecord = (IccRecords) this.mGsmPhone.mIccRecords.get();
            if (mIccRecord != null) {
                String hplmn = mIccRecord.getOperatorNumeric();
                String rplmn = currentState.getOperatorNumeric();
                String mVoicemailNum = mIccRecord.getVoiceMailNumber();
                String[] plmns = custRoamingVoicemail.split(",");
                if (!TextUtils.isEmpty(hplmn) && !TextUtils.isEmpty(rplmn) && plmns.length == ILLEGAL_MS && !TextUtils.isEmpty(plmns[2])) {
                    if (hplmn.equals(plmns[0]) && rplmn.equals(plmns[1])) {
                        if (!plmns[2].equals(mVoicemailNum)) {
                            this.mSimRecordVoicemail = mVoicemailNum;
                            mIccRecord.setVoiceMailNumber(plmns[2]);
                        }
                    } else if (!TextUtils.isEmpty(this.mSimRecordVoicemail) && plmns[2].equals(mVoicemailNum)) {
                        mIccRecord.setVoiceMailNumber(this.mSimRecordVoicemail);
                        this.mSimRecordVoicemail = "";
                    }
                }
            }
        }
    }

    public void setRadioPower(CommandsInterface ci, boolean enabled) {
        if (IS_SIM_POWER_DOWN && ci != null && this.mGsmPhone != null) {
            boolean bAirplaneMode = Global.getInt(this.mGsmPhone.getContext().getContentResolver(), "airplane_mode_on", 0) == 1 ? DBG : false;
            try {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Set radio power: ");
                stringBuilder.append(enabled);
                stringBuilder.append(", is airplane mode: ");
                stringBuilder.append(bAirplaneMode);
                Rlog.d(str, stringBuilder.toString());
                if (enabled) {
                    ci.setSimState(1, 1, null);
                } else if (bAirplaneMode) {
                    ci.setSimState(1, 0, null);
                }
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception in setRadioPower", e);
            }
        }
    }

    public String setEmergencyToNoService(ServiceState mSS, String plmn, boolean mEmergencyOnly) {
        if (IS_EMERGENCY_SHOWS_NOSERVICE && mSS.getRadioTechnology() == GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN && mEmergencyOnly) {
            return Resources.getSystem().getText(17040350).toString();
        }
        if ("true".equals(System.getString(this.mGsmPhone.getContext().getContentResolver(), "emergency_shows_noservice"))) {
            int subId = this.mGsmPhone.getSubId();
            boolean z = false;
            boolean isSimNotReady = TelephonyManager.getDefault().getSimState(subId) != 5 ? DBG : false;
            if (SubscriptionController.getInstance().getSubState(subId) != 1) {
                z = true;
            }
            boolean isSubInActive = z;
            if ((isSimNotReady || isSubInActive) && mEmergencyOnly) {
                return Resources.getSystem().getText(17040350).toString();
            }
        }
        return plmn;
    }

    /* JADX WARNING: Missing block: B:28:0x005a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPsCell(ServiceState mSS, GsmCellLocation mNewCellLoc, String[] states) {
        if (!(mSS == null || mNewCellLoc == null || states == null || !IS_DATAONLY_LOCATION_ENABLED)) {
            int voiceRegState = mSS.getVoiceRegState();
            boolean z = DBG;
            if (voiceRegState != 1) {
                z = false;
            }
            boolean mCsOutOfservice = z;
            int Tac = UNKNOWN_STATE;
            int Ci = UNKNOWN_STATE;
            int Pci = UNKNOWN_STATE;
            try {
                if (states.length >= ILLEGAL_ME) {
                    Ci = Integer.parseInt(states[5]);
                }
                if (states.length >= GPRS_SERVICES_NOT_ALLOWED) {
                    Pci = Integer.parseInt(states[ILLEGAL_ME]);
                }
                if (states.length >= 8) {
                    Tac = Integer.parseInt(states[GPRS_SERVICES_NOT_ALLOWED]);
                }
            } catch (NumberFormatException e) {
                Rlog.d(LOG_TAG, "error parsing GprsRegistrationState: ");
            }
            if (mCsOutOfservice && Tac >= 0 && Ci >= 0 && Pci >= 0) {
                Rlog.d(LOG_TAG, "data only card use ps cellid to location");
                mNewCellLoc.setLacAndCid(Tac, Ci);
                mNewCellLoc.setPsc(Pci);
            }
        }
    }

    public boolean isInServiceState(int combinedregstate) {
        return (HW_ATT_SHOW_NET_REJ && this.is_ext_plmn_sent && combinedregstate == 0) ? DBG : false;
    }

    public boolean isInServiceState(ServiceState ss) {
        return isInServiceState(getCombinedRegState(ss));
    }

    public void setExtPlmnSent(boolean value) {
        if (HW_ATT_SHOW_NET_REJ) {
            this.is_ext_plmn_sent = value;
        }
    }

    public void custHandlePollStateResult(int what, AsyncResult ar, int[] pollingContext) {
        if (HW_ATT_SHOW_NET_REJ && ar.userObj != pollingContext && ar.exception == null && 4 == what) {
            try {
                String[] states = ar.result;
                if (states != null && states.length > NATIONAL_ROAMING_NOT_ALLOWED) {
                    handleNetworkRejection(Integer.parseInt(states[NATIONAL_ROAMING_NOT_ALLOWED]));
                }
            } catch (RuntimeException e) {
            }
        }
    }

    public void handleNetworkRejection(int regState, String[] states) {
        if (!HW_ATT_SHOW_NET_REJ) {
            Rlog.d(LOG_TAG, "HW_ATT_SHOW_NET_REJ is disable.");
        } else if (states == null) {
            Rlog.d(LOG_TAG, "States is null.");
        } else {
            if ((regState == ILLEGAL_MS || !(this.mGsmPhone == null || this.mGsmPhone.getServiceState() == null || !this.mGsmPhone.getServiceState().isEmergencyOnly())) && states.length >= GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN) {
                try {
                    handleNetworkRejection(Integer.parseInt(states[NATIONAL_ROAMING_NOT_ALLOWED]));
                } catch (NumberFormatException ex) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error parsing regCode: ");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    private void showDialog(String msg, int regCode) {
        if (regCode != this.oldRejCode) {
            if (this.networkDialog != null) {
                this.dialogCanceled++;
                this.networkDialog.dismiss();
            }
            this.networkDialog = new Builder(this.mGsmPhone.getContext()).setMessage(msg).setCancelable(DBG).create();
            this.networkDialog.getWindow().setType(2008);
            this.networkDialog.setOnKeyListener(this.mShowRejMsgOnKeyListener);
            this.networkDialog.setOnCancelListener(this.mShowRejMsgOnCancelListener);
            this.networkDialog.show();
            this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(1), 120000);
            try {
                this.mToneGenerator = new ToneGenerator(1, FOCUS_BEEP_VOLUME);
                this.mToneGenerator.startTone(28);
            } catch (RuntimeException e) {
                this.mToneGenerator = null;
            }
        }
    }

    private int getCombinedRegState(ServiceState ss) {
        if (ss == null) {
            return 1;
        }
        int regState = ss.getVoiceRegState();
        int dataRegState = ss.getDataRegState();
        if (regState == 1 && dataRegState == 0) {
            Rlog.e(LOG_TAG, "getCombinedRegState: return STATE_IN_SERVICE as Data is in service");
            regState = dataRegState;
        }
        return regState;
    }

    private void handleNetworkRejection(int rejCode) {
        Resources r = Resources.getSystem();
        String plmn = r.getText(17039987).toString();
        if (rejCode != ILLEGAL_ME) {
            if (!(rejCode == NO_SUITABLE_CELLS_IN_LA || rejCode == NETWORK_FAILURE)) {
                switch (rejCode) {
                    case 2:
                        showDialog(r.getString(33685992), rejCode);
                        handleShowLimitedService(plmn);
                        break;
                    case ILLEGAL_MS /*3*/:
                        showDialog(r.getString(33685993), rejCode);
                        handleShowLimitedService(plmn);
                        break;
                    default:
                        switch (rejCode) {
                            case PLMN_NOT_ALLOWED /*11*/:
                            case LA_NOT_ALLOWED /*12*/:
                            case NATIONAL_ROAMING_NOT_ALLOWED /*13*/:
                                break;
                        }
                        break;
                }
            }
            handleShowLimitedService(" ");
        } else {
            showDialog(r.getString(33685994), rejCode);
            handleShowLimitedService(plmn);
        }
        this.oldRejCode = rejCode;
    }

    private void handleShowLimitedService(String plmn) {
        Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
        intent.putExtra("showSpn", false);
        intent.putExtra("spn", "");
        intent.putExtra("showPlmn", DBG);
        intent.putExtra("plmn", plmn);
        this.mGsmPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        this.is_ext_plmn_sent = DBG;
    }

    public void judgeToLaunchCsgPeriodicSearchTimer() {
        if (this.mCsgSrch != null && mIsSupportCsgSearch) {
            this.mCsgSrch.judgeToLaunchCsgPeriodicSearchTimer();
        }
    }

    public boolean isStopUpdateName(boolean SimCardLoaded) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" isStopUpdateName: SimCardLoaded = ");
        stringBuilder.append(SimCardLoaded);
        stringBuilder.append(", IS_DELAY_UPDATENAME = ");
        stringBuilder.append(IS_DELAY_UPDATENAME);
        Rlog.d(str, stringBuilder.toString());
        if (!IS_DELAY_UPDATENAME && !IS_DELAY_UPDATENAME_LAC_NULL) {
            return false;
        }
        if ((SimCardLoaded || !IS_DELAY_UPDATENAME) && (this.mGsmPhone == null || this.mGsmPhone.mSST == null || ((this.mGsmPhone.mSST.mCellLoc != null && ((GsmCellLocation) this.mGsmPhone.mSST.mCellLoc).getLac() != UNKNOWN_STATE) || !IS_DELAY_UPDATENAME_LAC_NULL))) {
            return false;
        }
        return DBG;
    }

    public boolean isUpdateLacAndCidCust(ServiceStateTracker sst) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isUpdateLacAndCidCust: Update Lac and Cid when cid is 0, ServiceStateTracker = ");
        stringBuilder.append(sst);
        stringBuilder.append(",UPDATE_LAC_CID = ");
        stringBuilder.append(UPDATE_LAC_CID);
        Rlog.d(str, stringBuilder.toString());
        return UPDATE_LAC_CID;
    }

    public void setLTEUsageForRomaing(boolean isRoaming) {
        if (IS_LGU && !isRoaming) {
            HwTelephonyManagerInner inner = HwTelephonyManagerInner.getDefault();
            if (inner.getLteServiceAbility() != 1) {
                Rlog.i(LOG_TAG, " setLTEUsageForRomaing setLteServiceAbility... ");
                inner.setLteServiceAbility(1);
            }
        }
    }

    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i == EVENT_GET_LTE_FREQ_WITH_WLAN_COEX) {
            Rlog.d(LOG_TAG, "EVENT_GET_LTE_FREQ_WITH_WLAN_COEX");
            handleGetLteFreqWithWlanCoex((AsyncResult) msg.obj);
            return DBG;
        } else if (i != EVENT_GET_CELL_INFO_LIST_OTDOA) {
            return false;
        } else {
            AsyncResult ar = msg.obj;
            if (ar.userObj instanceof AsyncResult) {
                Rlog.d(LOG_TAG, "EVENT_GET_CELL_INFO_LIST userObj is AsyncResult!");
                ar = ar.userObj;
            }
            if (ar.userObj instanceof CellInfoResult) {
                CellInfoResult result = ar.userObj;
                synchronized (result.lockObj) {
                    if (ar.exception != null) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("EVENT_GET_CELL_INFO_LIST_OTDOA: error ret null, e=");
                        stringBuilder.append(ar.exception);
                        Rlog.i(str, stringBuilder.toString());
                        result.list = null;
                    } else {
                        result.list = (List) ar.result;
                    }
                    this.mLastEnhancedCellInfoListTime = SystemClock.elapsedRealtime();
                    this.mLastEnhancedCellInfoList = result.list;
                    result.lockObj.notify();
                }
                return DBG;
            }
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("EVENT_GET_CELL_INFO_LIST userObj:");
            stringBuilder2.append(ar.userObj);
            Rlog.d(str2, stringBuilder2.toString());
            return DBG;
        }
    }

    private void handleGetLteFreqWithWlanCoex(AsyncResult ar) {
        if (!(ar == null || ar.exception != null || this.mGsmPhone == null)) {
            int[] result = ar.result;
            if (result == null) {
                Rlog.d(LOG_TAG, "EVENT_GET_LTE_FREQ_WITH_WLAN_COEX  result is null");
                return;
            }
            int ulbw = result[2];
            int dlbw = result[4];
            if (!TextUtils.isEmpty(this.mUlbwDlbwString)) {
                String[] ulbwDlbw = this.mUlbwDlbwString.trim().split(";");
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_GET_LTE_FREQ_WITH_WLAN_COEX  ulbw =");
                stringBuilder.append(ulbw);
                stringBuilder.append(", dlbw =");
                stringBuilder.append(dlbw);
                stringBuilder.append("mUlbwDlbwString =");
                stringBuilder.append(this.mUlbwDlbwString);
                Rlog.d(str, stringBuilder.toString());
                boolean isHasCAState = false;
                if (ulbw >= Integer.parseInt(ulbwDlbw[0]) && dlbw >= Integer.parseInt(ulbwDlbw[1])) {
                    isHasCAState = DBG;
                }
                if (this.mIsCaState != isHasCAState) {
                    this.mIsLTEBandWidthChanged = DBG;
                    this.mIsCaState = isHasCAState;
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("EVENT_GET_LTE_FREQ_WITH_WLAN_COEX  mIsCaState =");
                    stringBuilder2.append(this.mIsCaState);
                    Rlog.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void getLteFreqWithWlanCoex(CommandsInterface ci, ServiceStateTracker sst) {
        if (this.mContext == null || ci == null || sst == null) {
            Rlog.d(LOG_TAG, "getLteFreqWithWlanCoex  error !");
            return;
        }
        try {
            this.mUlbwDlbwString = System.getString(this.mContext.getContentResolver(), "hw_query_lwclash");
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception get hw_query_lwclash value", e);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EVENT_GET_LTE_FREQ_WITH_WLAN_COEX  mUlbwDlbwString =");
        stringBuilder.append(this.mUlbwDlbwString);
        Rlog.d(str, stringBuilder.toString());
        if (!TextUtils.isEmpty(this.mUlbwDlbwString)) {
            ci.getLteFreqWithWlanCoex(sst.obtainMessage(EVENT_GET_LTE_FREQ_WITH_WLAN_COEX));
        }
    }

    private boolean isCustRejCodeKo(int rejcode) {
        boolean result = false;
        if (this.mGsmPhone == null || this.mGsmPhone.mIccRecords == null || this.mGsmPhone.mIccRecords.get() == null) {
            return false;
        }
        String mccmnc = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
        String rejPlmnsConfig = System.getString(this.mContext.getContentResolver(), "hw_rej_info_ko");
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mccmnc = ");
        stringBuilder.append(mccmnc);
        stringBuilder.append(" plmnsConfig = ");
        stringBuilder.append(rejPlmnsConfig);
        Rlog.d(str, stringBuilder.toString());
        if (TextUtils.isEmpty(rejPlmnsConfig) || TextUtils.isEmpty(mccmnc)) {
            return false;
        }
        String[] rejPlmns = rejPlmnsConfig.split(";");
        int length = rejPlmns.length;
        int i = 0;
        while (i < length) {
            String[] rejectCodes = rejPlmns[i].split(":");
            if (rejectCodes.length == 2 && !TextUtils.isEmpty(rejectCodes[0]) && !TextUtils.isEmpty(rejectCodes[1])) {
                if (mccmnc.equals(rejectCodes[0]) && Arrays.asList(rejectCodes[1].split(",")).contains(String.valueOf(rejcode))) {
                    result = DBG;
                    break;
                }
                i++;
            } else {
                return false;
            }
        }
        return result;
    }

    public void handleNetworkRejectionEx(int rejcode, int rejrat) {
        if (HW_ATT_SHOW_NET_REJ) {
            handleNetworkRejection(rejcode);
        }
        if (IS_KT) {
            handleKTNetworkRejectionEx(rejcode, rejrat);
        }
    }

    private void handleKTNetworkRejectionEx(int rejcode, int rejrat) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleNetworkRejectionKo : rejcode :");
        stringBuilder.append(rejcode);
        stringBuilder.append("   rej rat==");
        stringBuilder.append(rejrat);
        Rlog.d(str, stringBuilder.toString());
        if (!IS_KT || rejcode != NO_SUITABLE_CELLS_IN_LA) {
            Resources r = Resources.getSystem();
            String msg = "";
            if (!isCustRejCodeKo(rejcode)) {
                msg = r.getString(33685995);
            } else if (rejcode != ILLEGAL_ME) {
                if (rejcode != 8) {
                    if (rejcode != ESM_FAILURE && rejcode != CONGESTTION) {
                        switch (rejcode) {
                            case 2:
                                break;
                            case ILLEGAL_MS /*3*/:
                                msg = r.getString(33685997);
                                break;
                            default:
                                switch (rejcode) {
                                    case NO_SUITABLE_CELLS_IN_LA /*15*/:
                                        break;
                                    case MSC_TEMPORARILY_NOT_REACHABLE /*16*/:
                                    case NETWORK_FAILURE /*17*/:
                                        if (rejrat != 2 || !IS_KT) {
                                            msg = r.getString(33685999);
                                            break;
                                        } else {
                                            msg = r.getString(33685995);
                                            break;
                                        }
                                        break;
                                    default:
                                        msg = r.getString(33685995);
                                        break;
                                }
                        }
                    }
                    msg = r.getString(33685999);
                }
                msg = r.getString(33685996);
            } else {
                msg = r.getString(33685998);
            }
            showDialog(msg, rejcode);
            this.oldRejCode = rejcode;
        }
    }

    public boolean isUpdateCAByCell(ServiceState newSS) {
        boolean updateCaByCell = DBG;
        if (newSS.getRilDataRadioTechnology() == GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN || newSS.getRilDataRadioTechnology() == ESM_FAILURE) {
            updateCaByCell = SystemProperties.getBoolean("ro.config.hw_updateCA_bycell", DBG);
        }
        if (TextUtils.isEmpty(this.mUlbwDlbwString) || !this.mIsLTEBandWidthChanged) {
            return updateCaByCell;
        }
        this.mIsLTEBandWidthChanged = false;
        return false;
    }

    public void updateLTEBandWidth(ServiceState newSS) {
        if (!(TextUtils.isEmpty(this.mUlbwDlbwString) || newSS.isUsingCarrierAggregation())) {
            newSS.setIsUsingCarrierAggregation(this.mIsCaState);
        }
    }

    public void handleLteEmmCause(int phoneId, int rejrat, int originalrejectcause) {
        if (IS_VERIZON && 2 == rejrat) {
            ContentResolver contentResolver = this.mGsmPhone.getContext().getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LTE_Emm_Cause_");
            stringBuilder.append(phoneId);
            Global.putInt(contentResolver, stringBuilder.toString(), originalrejectcause);
            setLteEmmCauseRecorded(phoneId, DBG);
        }
    }

    public void clearLteEmmCause(int phoneId, ServiceState state) {
        if (IS_VERIZON && isLteEmmCauseRecorded(phoneId)) {
            if ((state.getRilVoiceRadioTechnology() == GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN && state.getVoiceRegState() == 0) || (state.getRilDataRadioTechnology() == GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN && state.getDataRegState() == 0)) {
                ContentResolver contentResolver = this.mGsmPhone.getContext().getContentResolver();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LTE_Emm_Cause_");
                stringBuilder.append(phoneId);
                Global.putInt(contentResolver, stringBuilder.toString(), 0);
                setLteEmmCauseRecorded(phoneId, false);
            }
        }
    }

    private boolean isLteEmmCauseRecorded(int phoneId) {
        if (phoneId < this.lteEmmCauseRecorded.length) {
            return this.lteEmmCauseRecorded[phoneId];
        }
        return false;
    }

    private void setLteEmmCauseRecorded(int phoneId, boolean isRecorded) {
        if (phoneId < this.lteEmmCauseRecorded.length) {
            this.lteEmmCauseRecorded[phoneId] = isRecorded;
        }
    }

    public void clearPcoValue(GsmCdmaPhone phone) {
        if (HW_SIM_ACTIVATION) {
            Global.putString(phone.getContext().getContentResolver(), PCO_DATA, DEFAULT_PCO_DATA);
            Rlog.d(LOG_TAG, "Airplane mode on , clear pco data.");
        }
    }

    public boolean isCsPopShow(int notifyType) {
        if (SystemProperties.getBoolean("ro.config.hw_CSResDialog", false) && (notifyType == CS_ENABLED || notifyType == CS_NORMAL_ENABLED)) {
            return DBG;
        }
        return false;
    }

    private List<CellInfo> getAllEnhancedCellInfoData(WorkSource workSource) {
        CellInfoResult result = new CellInfoResult(this, null);
        if (this.mGsmPhone.mCi.getRilVersion() < 8) {
            Rlog.i(LOG_TAG, "SST.getAllEnhancedCellInfo(): not implemented");
            result.list = null;
        } else if (Thread.currentThread() == this.mGsmPhone.getServiceStateTracker().getLooper().getThread()) {
            Rlog.i(LOG_TAG, "SST.getAllEnhancedCellInfo(): return last, same thread can't block");
            result.list = this.mLastEnhancedCellInfoList;
        } else if (SystemClock.elapsedRealtime() - this.mLastEnhancedCellInfoListTime > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
            Message msg = this.mGsmPhone.getServiceStateTracker().obtainMessage(EVENT_GET_CELL_INFO_LIST_OTDOA, result);
            synchronized (result.lockObj) {
                result.list = null;
                this.mGsmPhone.mCi.getEnhancedCellInfoList(msg, workSource);
                try {
                    result.lockObj.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Rlog.i(LOG_TAG, "SST.getAllEnhancedCellInfo(): return last, back to back calls");
            result.list = this.mLastEnhancedCellInfoList;
        }
        synchronized (result.lockObj) {
            if (result.list != null) {
                List list = result.list;
                return list;
            }
            Rlog.i(LOG_TAG, "SST.getAllEnhancedCellInfo(): X size=0 list=null");
            return null;
        }
    }

    public List<CellInfo> getAllEnhancedCellInfo(WorkSource workSource) {
        List<CellInfo> cellInfoList = getAllEnhancedCellInfoData(workSource);
        if (cellInfoList == null) {
            return null;
        }
        if (Secure.getInt(this.mGsmPhone.getContext().getContentResolver(), "location_mode", 0) == 0) {
            ArrayList<CellInfo> privateCellInfoList = new ArrayList(cellInfoList.size());
            for (CellInfo c : cellInfoList) {
                if (c instanceof CellInfoCdma) {
                    CellInfoCdma cellInfoCdma = (CellInfoCdma) c;
                    CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
                    CellIdentityCdma maskedCellIdentity = new CellIdentityCdma(cellIdentity.getNetworkId(), cellIdentity.getSystemId(), cellIdentity.getBasestationId(), Integer.MAX_VALUE, Integer.MAX_VALUE);
                    CellInfoCdma privateCellInfoCdma = new CellInfoCdma(cellInfoCdma);
                    privateCellInfoCdma.setCellIdentity(maskedCellIdentity);
                    privateCellInfoList.add(privateCellInfoCdma);
                } else {
                    privateCellInfoList.add(c);
                }
            }
            cellInfoList = privateCellInfoList;
        }
        return cellInfoList;
    }
}
