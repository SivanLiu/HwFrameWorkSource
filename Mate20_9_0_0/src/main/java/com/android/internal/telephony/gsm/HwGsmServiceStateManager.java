package com.android.internal.telephony.gsm;

import android.common.HwCfgKey;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.HwTelephony.NumMatchs;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.CellLocation;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.CustPlmnMember;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwAddonTelephonyFactory;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwPlmnActConcat;
import com.android.internal.telephony.HwReportManagerImpl;
import com.android.internal.telephony.HwServiceStateManager;
import com.android.internal.telephony.HwSignalStrength;
import com.android.internal.telephony.HwSignalStrength.SignalThreshold;
import com.android.internal.telephony.HwSignalStrength.SignalType;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.OnsDisplayParams;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PlmnConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.ServiceStateTrackerUtils;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.VirtualNet;
import com.android.internal.telephony.VirtualNetOnsExtend;
import com.android.internal.telephony.dataconnection.ApnReminder;
import com.android.internal.telephony.dataconnection.InCallDataStateMachine;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimUtils;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import huawei.cust.HwGetCfgFileConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public class HwGsmServiceStateManager extends HwServiceStateManager {
    private static final int AIRPLANE_MODE_ON = 1;
    private static final int CDMA_LEVEL = 8;
    private static final String CHINAMOBILE_MCCMNC = "46000;46002;46007;46008;46004";
    private static final String CHINA_OPERATOR_MCC = "460";
    private static final String CHINA_TELECOM_SPN = "%E4%B8%AD%E5%9B%BD%E7%94%B5%E4%BF%A1";
    public static final String CLOUD_OTA_DPLMN_UPDATE = "cloud.ota.dplmn.UPDATE";
    public static final String CLOUD_OTA_MCC_UPDATE = "cloud.ota.mcc.UPDATE";
    public static final String CLOUD_OTA_PERMISSION = "huawei.permission.RECEIVE_CLOUD_OTA_UPDATA";
    private static final String EMERGENCY_PLMN = Resources.getSystem().getText(17039987).toString();
    private static final int EVDO_LEVEL = 16;
    private static final int EVENT_4R_MIMO_ENABLE = 106;
    private static final int EVENT_CA_STATE_CHANGED = 104;
    private static final int EVENT_CRR_CONN = 152;
    private static final int EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH = 103;
    private static final int EVENT_DELAY_UPDATE_REGISTER_STATE_DONE = 102;
    private static final int EVENT_DSDS_MODE = 153;
    private static final int EVENT_MCC_CHANGED = 154;
    private static final int EVENT_NETWORK_REJECTED_CASE = 105;
    private static final int EVENT_PLMN_SELINFO = 151;
    private static final int EVENT_POLL_LOCATION_INFO = 64;
    private static final int EVENT_RPLMNS_STATE_CHANGED = 101;
    private static final int GSM_LEVEL = 1;
    private static final int GSM_STRENGTH_POOR_STD = -109;
    private static final boolean IS_VERIZON;
    private static final boolean KEEP_3GPLUS_HPLUS = SystemProperties.getBoolean("ro.config.keep_3gplus_hplus", false);
    private static final String LOG_TAG = "HwGsmServiceStateManager";
    private static final int LTE_LEVEL = 4;
    private static final int LTE_RSSNR_POOR_STD = SystemProperties.getInt("ro.lte.rssnrpoorstd", L_RSSNR_POOR_STD);
    private static final int LTE_RSSNR_UNKOUWN_STD = 99;
    private static final int LTE_STRENGTH_POOR_STD = SystemProperties.getInt("ro.lte.poorstd", L_STRENGTH_POOR_STD);
    private static final int LTE_STRENGTH_UNKOUWN_STD = -44;
    private static final int L_RSSNR_POOR_STD = -5;
    private static final int L_STRENGTH_POOR_STD = -125;
    private static final int MCC_LENGTH = 3;
    private static final boolean MIMO_4R_REPORT = SystemProperties.getBoolean("ro.config.hw_4.5gplus", false);
    private static final int NETWORK_MODE_GSM_UMTS = 3;
    private static final String NO_SERVICE_PLMN = Resources.getSystem().getText(17040350).toString();
    private static final Uri PREFERAPN_NO_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn_no_update");
    private static final String PROPERTY_GLOBAL_FORCE_TO_SET_ECC = "ril.force_to_set_ecc";
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final boolean PS_CLEARCODE = SystemProperties.getBoolean("ro.config.hw_clearcode_pdp", false);
    private static final int RAT_LTE = 2;
    private static final String REGEX = "((\\d{5,14},\\d{5,14},[^:,;]{1,20};)){1,}$";
    private static final int REJ_TIMES = 3;
    private static final boolean SHOW_4G_PLUS_ICON = SystemProperties.getBoolean("ro.config.hw_show_4G_Plus_icon", false);
    private static final boolean SHOW_REJ_INFO_KT = SystemProperties.getBoolean("ro.config.show_rej_info", false);
    private static final int TIME_NOT_SET = 0;
    private static final int UMTS_LEVEL = 2;
    private static final int VALUE_DELAY_DURING_TIME = 6000;
    private static final int WCDMA_ECIO_NONE = 255;
    private static final int WCDMA_ECIO_POOR_STD = SystemProperties.getInt("ro.wcdma.eciopoorstd", W_ECIO_POOR_STD);
    private static final int WCDMA_STRENGTH_POOR_STD = SystemProperties.getInt("ro.wcdma.poorstd", W_STRENGTH_POOR_STD);
    private static final int W_ECIO_POOR_STD = -17;
    private static final int W_STRENGTH_POOR_STD = -112;
    private static final boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private static final int mDelayDuringTime = SystemProperties.getInt("ro.signalsmooth.delaytimer", VALUE_DELAY_DURING_TIME);
    private static HwSignalStrength mHwSigStr = HwSignalStrength.getInstance();
    private final boolean FEATURE_SIGNAL_DUALPARAM = SystemProperties.getBoolean("signal.dualparam", false);
    private int lastCid = -1;
    private int lastLac = -1;
    private int lastType = -1;
    CommandsInterface mCi;
    private CloudOtaBroadcastReceiver mCloudOtaBroadcastReceiver = new CloudOtaBroadcastReceiver(this, null);
    private Context mContext;
    private ContentResolver mCr;
    private DoubleSignalStrength mDoubleSignalStrength;
    private GsmCdmaPhone mGsmPhone;
    private ServiceStateTracker mGsst;
    private HwCustGsmServiceStateManager mHwCustGsmServiceStateManager;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = "";
            if (intent != null) {
                action = intent.getAction();
                int slotId;
                String str;
                StringBuilder stringBuilder;
                if ("android.intent.action.refreshapn".equals(action)) {
                    Rlog.i(HwGsmServiceStateManager.LOG_TAG, "refresh apn worked,updateSpnDisplay.");
                    HwGsmServiceStateManager.this.mGsst.updateSpnDisplay();
                } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                    String simState = (String) intent.getExtra("ss");
                    slotId = intent.getIntExtra("slot", -1000);
                    str = HwGsmServiceStateManager.LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("simState = ");
                    stringBuilder.append(simState);
                    stringBuilder.append(" slotId = ");
                    stringBuilder.append(slotId);
                    Rlog.i(str, stringBuilder.toString());
                    if ("ABSENT".equals(simState)) {
                        VirtualNet.removeVirtualNet(slotId);
                        Rlog.i(HwGsmServiceStateManager.LOG_TAG, "sim absent, reset");
                        if (-1 != SystemProperties.getInt("gsm.sim.updatenitz", -1)) {
                            SystemProperties.set("gsm.sim.updatenitz", String.valueOf(-1));
                        }
                    } else if ("LOADED".equals(simState) && slotId == HwGsmServiceStateManager.this.mPhoneId) {
                        Rlog.i(HwGsmServiceStateManager.LOG_TAG, "after simrecords loaded,updateSpnDisplay for virtualnet ons.");
                        HwGsmServiceStateManager.this.mGsst.updateSpnDisplay();
                    }
                } else {
                    boolean z = false;
                    if ("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(action)) {
                        int slotId2 = intent.getIntExtra("subscription", -1);
                        slotId = intent.getIntExtra("intContent", 0);
                        str = intent.getStringExtra("columnName");
                        String str2 = HwGsmServiceStateManager.LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Received ACTION_SUBINFO_CONTENT_CHANGE on slotId: ");
                        stringBuilder2.append(slotId2);
                        stringBuilder2.append(" for ");
                        stringBuilder2.append(str);
                        stringBuilder2.append(", intValue: ");
                        stringBuilder2.append(slotId);
                        Rlog.i(str2, stringBuilder2.toString());
                        str2 = HwGsmServiceStateManager.LOG_TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("PROPERTY_GSM_SIM_UPDATE_NITZ=");
                        stringBuilder2.append(SystemProperties.getInt("gsm.sim.updatenitz", -1));
                        Rlog.i(str2, stringBuilder2.toString());
                        if ("sub_state".equals(str) && -1 != slotId2 && slotId == 0 && slotId2 == SystemProperties.getInt("gsm.sim.updatenitz", -1)) {
                            Rlog.i(HwGsmServiceStateManager.LOG_TAG, "reset PROPERTY_GSM_SIM_UPDATE_NITZ when the sim is inactive and the time zone get from the sim' NITZ.");
                            SystemProperties.set("gsm.sim.updatenitz", String.valueOf(-1));
                        }
                    } else if ("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT".equals(action)) {
                        String str3 = HwGsmServiceStateManager.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[SLOT");
                        stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
                        stringBuilder.append("]CardState: ");
                        stringBuilder.append(intent.getIntExtra("newSubState", -1));
                        stringBuilder.append("IsMphone: ");
                        stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId == intent.getIntExtra("phone", 0));
                        Rlog.d(str3, stringBuilder.toString());
                        if (intent.getIntExtra("operationResult", 1) == 0 && HwGsmServiceStateManager.this.mPhoneId == intent.getIntExtra("phone", 0) && HwGsmServiceStateManager.this.hasMessages(HwGsmServiceStateManager.EVENT_DELAY_UPDATE_REGISTER_STATE_DONE) && intent.getIntExtra("newSubState", -1) == 0) {
                            HwGsmServiceStateManager.this.cancelDeregisterStateDelayTimer();
                            HwGsmServiceStateManager.this.mGsst.pollState();
                        }
                    } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                        if (Global.getInt(HwGsmServiceStateManager.this.mCr, "airplane_mode_on", 0) == 1) {
                            z = true;
                        }
                        boolean airplaneMode = z;
                        String str4 = HwGsmServiceStateManager.LOG_TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("airplaneMode: ");
                        stringBuilder3.append(airplaneMode);
                        Rlog.d(str4, stringBuilder3.toString());
                        if (airplaneMode) {
                            SystemProperties.set("gsm.sim.updatenitz", String.valueOf(-1));
                        }
                    } else if (HwGsmServiceStateManager.this.mHwCustGsmServiceStateManager != null) {
                        HwGsmServiceStateManager.this.mRac = HwGsmServiceStateManager.this.mHwCustGsmServiceStateManager.handleBroadcastReceived(context, intent, HwGsmServiceStateManager.this.mRac);
                    }
                }
            }
        }
    };
    private SignalStrength mModemSignalStrength;
    private int mOldCAstate = 0;
    private DoubleSignalStrength mOldDoubleSignalStrength;
    private int mPhoneId = 0;
    private int mRac = -1;
    private boolean mis4RMimoEnable = false;
    private String oldRplmn = "";
    private int rejNum = 0;
    private String rplmn = "";

    private class CloudOtaBroadcastReceiver extends BroadcastReceiver {
        private CloudOtaBroadcastReceiver() {
        }

        /* synthetic */ CloudOtaBroadcastReceiver(HwGsmServiceStateManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (HwGsmServiceStateManager.this.mGsmPhone != null && HwGsmServiceStateManager.this.mCi != null) {
                String action = intent.getAction();
                if ("cloud.ota.mcc.UPDATE".equals(action)) {
                    Rlog.e(HwGsmServiceStateManager.LOG_TAG, "HwCloudOTAService CLOUD_OTA_MCC_UPDATE");
                    HwGsmServiceStateManager.this.mCi.sendCloudMessageToModem(1);
                } else if ("cloud.ota.dplmn.UPDATE".equals(action)) {
                    Rlog.e(HwGsmServiceStateManager.LOG_TAG, "HwCloudOTAService CLOUD_OTA_DPLMN_UPDATE");
                    HwGsmServiceStateManager.this.mCi.sendCloudMessageToModem(2);
                }
            }
        }
    }

    private class DoubleSignalStrength {
        private static final double VALUE_NEW_COEF_QUA_DES_SS = 0.15d;
        private static final int VALUE_NEW_COEF_STR_DES_SS = 5;
        private static final double VALUE_OLD_COEF_QUA_DES_SS = 0.85d;
        private static final int VALUE_OLD_COEF_STR_DES_SS = 7;
        private int mDelayTime;
        private double mDoubleGsmSS;
        private double mDoubleLteRsrp;
        private double mDoubleLteRssnr;
        private double mDoubleWcdmaEcio;
        private double mDoubleWcdmaRscp;
        private double mOldDoubleGsmSS;
        private double mOldDoubleLteRsrp;
        private double mOldDoubleLteRssnr;
        private double mOldDoubleWcdmaEcio;
        private double mOldDoubleWcdmaRscp;
        private int mTechState;

        public DoubleSignalStrength(SignalStrength ss) {
            this.mDoubleLteRsrp = (double) ss.getLteRsrp();
            this.mDoubleLteRssnr = (double) ss.getLteRssnr();
            this.mDoubleWcdmaRscp = (double) ss.getWcdmaRscp();
            this.mDoubleWcdmaEcio = (double) ss.getWcdmaEcio();
            this.mDoubleGsmSS = (double) ss.getGsmSignalStrength();
            this.mTechState = 0;
            if (ss.getGsmSignalStrength() < -1) {
                this.mTechState |= 1;
            }
            if (ss.getWcdmaRscp() < -1) {
                this.mTechState |= 2;
            }
            if (ss.getLteRsrp() < -1) {
                this.mTechState |= 4;
            }
            this.mOldDoubleLteRsrp = this.mDoubleLteRsrp;
            this.mOldDoubleLteRssnr = this.mDoubleLteRssnr;
            this.mOldDoubleWcdmaRscp = this.mDoubleWcdmaRscp;
            this.mOldDoubleWcdmaEcio = this.mDoubleWcdmaEcio;
            this.mOldDoubleGsmSS = this.mDoubleGsmSS;
            this.mDelayTime = 0;
        }

        public DoubleSignalStrength(DoubleSignalStrength doubleSS) {
            this.mDoubleLteRsrp = doubleSS.mDoubleLteRsrp;
            this.mDoubleLteRssnr = doubleSS.mDoubleLteRssnr;
            this.mDoubleWcdmaRscp = doubleSS.mDoubleWcdmaRscp;
            this.mDoubleWcdmaEcio = doubleSS.mDoubleWcdmaEcio;
            this.mDoubleGsmSS = doubleSS.mDoubleGsmSS;
            this.mTechState = doubleSS.mTechState;
            this.mOldDoubleLteRsrp = doubleSS.mDoubleLteRsrp;
            this.mOldDoubleLteRssnr = doubleSS.mDoubleLteRssnr;
            this.mOldDoubleWcdmaRscp = doubleSS.mDoubleWcdmaRscp;
            this.mOldDoubleWcdmaEcio = doubleSS.mDoubleWcdmaEcio;
            this.mOldDoubleGsmSS = doubleSS.mDoubleGsmSS;
            this.mDelayTime = doubleSS.mDelayTime;
        }

        public double getDoubleLteRsrp() {
            return this.mDoubleLteRsrp;
        }

        public double getDoubleLteRssnr() {
            return this.mDoubleLteRssnr;
        }

        public double getDoubleWcdmaRscp() {
            return this.mDoubleWcdmaRscp;
        }

        public double getDoubleWcdmaEcio() {
            return this.mDoubleWcdmaEcio;
        }

        public double getDoubleGsmSignalStrength() {
            return this.mDoubleGsmSS;
        }

        public double getOldDoubleLteRsrp() {
            return this.mOldDoubleLteRsrp;
        }

        public double getOldDoubleLteRssnr() {
            return this.mOldDoubleLteRssnr;
        }

        public double getOldDoubleWcdmaRscp() {
            return this.mOldDoubleWcdmaRscp;
        }

        public double getOldDoubleWcdmaEcio() {
            return this.mOldDoubleWcdmaEcio;
        }

        public double getOldDoubleGsmSignalStrength() {
            return this.mOldDoubleGsmSS;
        }

        public boolean processLteRsrpAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            double oldRsrp = oldDoubleSS.getDoubleLteRsrp();
            double modemLteRsrp = (double) modemSS.getLteRsrp();
            this.mOldDoubleLteRsrp = oldRsrp;
            String str = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]--old : ");
            stringBuilder.append(oldRsrp);
            stringBuilder.append("; instant new : ");
            stringBuilder.append(modemLteRsrp);
            Rlog.d(str, stringBuilder.toString());
            if (modemLteRsrp >= -1.0d) {
                modemLteRsrp = (double) HwGsmServiceStateManager.LTE_STRENGTH_POOR_STD;
            }
            if (oldRsrp <= modemLteRsrp) {
                this.mDoubleLteRsrp = modemLteRsrp;
            } else if (needProcessDescend) {
                this.mDoubleLteRsrp = ((7.0d * oldRsrp) + (5.0d * modemLteRsrp)) / 12.0d;
            } else {
                this.mDoubleLteRsrp = oldRsrp;
            }
            String str2 = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SUB[");
            stringBuilder2.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder2.append("]modem : ");
            stringBuilder2.append(modemLteRsrp);
            stringBuilder2.append("; old : ");
            stringBuilder2.append(oldRsrp);
            stringBuilder2.append("; new : ");
            stringBuilder2.append(this.mDoubleLteRsrp);
            Rlog.d(str2, stringBuilder2.toString());
            if (this.mDoubleLteRsrp - modemLteRsrp <= -1.0d || this.mDoubleLteRsrp - modemLteRsrp >= 1.0d) {
                newSS.setLteRsrp((int) this.mDoubleLteRsrp);
                return true;
            }
            this.mDoubleLteRsrp = modemLteRsrp;
            newSS.setLteRsrp((int) this.mDoubleLteRsrp);
            return false;
        }

        public boolean processLteRssnrAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            double oldRssnr = oldDoubleSS.getDoubleLteRssnr();
            double modemLteRssnr = (double) modemSS.getLteRssnr();
            this.mOldDoubleLteRssnr = oldRssnr;
            String str = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]Before processLteRssnrAlaphFilter -- old : ");
            stringBuilder.append(oldRssnr);
            stringBuilder.append("; instant new : ");
            stringBuilder.append(modemLteRssnr);
            Rlog.d(str, stringBuilder.toString());
            if (modemLteRssnr == 99.0d) {
                modemLteRssnr = (double) HwGsmServiceStateManager.LTE_RSSNR_POOR_STD;
            }
            if (oldRssnr <= modemLteRssnr) {
                this.mDoubleLteRssnr = modemLteRssnr;
            } else if (needProcessDescend) {
                this.mDoubleLteRssnr = (VALUE_OLD_COEF_QUA_DES_SS * oldRssnr) + (VALUE_NEW_COEF_QUA_DES_SS * modemLteRssnr);
            } else {
                this.mDoubleLteRssnr = oldRssnr;
            }
            str = HwGsmServiceStateManager.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]LteRssnrAlaphFilter modem : ");
            stringBuilder.append(modemLteRssnr);
            stringBuilder.append("; old : ");
            stringBuilder.append(oldRssnr);
            stringBuilder.append("; new : ");
            stringBuilder.append(this.mDoubleLteRssnr);
            Rlog.d(str, stringBuilder.toString());
            if (this.mDoubleLteRssnr - modemLteRssnr <= -1.0d || this.mDoubleLteRssnr - modemLteRssnr >= 1.0d) {
                newSS.setLteRssnr((int) this.mDoubleLteRssnr);
                return true;
            }
            this.mDoubleLteRssnr = modemLteRssnr;
            newSS.setLteRssnr((int) this.mDoubleLteRssnr);
            return false;
        }

        public boolean processWcdmaRscpAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            double oldWcdmaRscp = oldDoubleSS.getDoubleWcdmaRscp();
            double modemWcdmaRscp = (double) modemSS.getWcdmaRscp();
            this.mOldDoubleWcdmaRscp = oldWcdmaRscp;
            String str = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]Before processWcdmaRscpAlaphFilter -- old : ");
            stringBuilder.append(oldWcdmaRscp);
            stringBuilder.append("; instant new : ");
            stringBuilder.append(modemWcdmaRscp);
            Rlog.d(str, stringBuilder.toString());
            if (modemWcdmaRscp >= -1.0d) {
                modemWcdmaRscp = (double) HwGsmServiceStateManager.WCDMA_STRENGTH_POOR_STD;
            }
            if (oldWcdmaRscp <= modemWcdmaRscp) {
                this.mDoubleWcdmaRscp = modemWcdmaRscp;
            } else if (needProcessDescend) {
                this.mDoubleWcdmaRscp = ((7.0d * oldWcdmaRscp) + (5.0d * modemWcdmaRscp)) / 12.0d;
            } else {
                this.mDoubleWcdmaRscp = oldWcdmaRscp;
            }
            String str2 = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SUB[");
            stringBuilder2.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder2.append("]WcdmaRscpAlaphFilter modem : ");
            stringBuilder2.append(modemWcdmaRscp);
            stringBuilder2.append("; old : ");
            stringBuilder2.append(oldWcdmaRscp);
            stringBuilder2.append("; new : ");
            stringBuilder2.append(this.mDoubleWcdmaRscp);
            Rlog.d(str2, stringBuilder2.toString());
            if (this.mDoubleWcdmaRscp - modemWcdmaRscp <= -1.0d || this.mDoubleWcdmaRscp - modemWcdmaRscp >= 1.0d) {
                newSS.setWcdmaRscp((int) this.mDoubleWcdmaRscp);
                return true;
            }
            this.mDoubleWcdmaRscp = modemWcdmaRscp;
            newSS.setWcdmaRscp((int) this.mDoubleWcdmaRscp);
            return false;
        }

        public boolean processWcdmaEcioAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            double oldWcdmaEcio = oldDoubleSS.getDoubleWcdmaEcio();
            double modemWcdmaEcio = (double) modemSS.getWcdmaEcio();
            this.mOldDoubleWcdmaEcio = oldWcdmaEcio;
            String str = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]Before processWcdmaEcioAlaphFilter -- old : ");
            stringBuilder.append(oldWcdmaEcio);
            stringBuilder.append("; instant new : ");
            stringBuilder.append(modemWcdmaEcio);
            Rlog.d(str, stringBuilder.toString());
            if (oldWcdmaEcio <= modemWcdmaEcio) {
                this.mDoubleWcdmaEcio = modemWcdmaEcio;
            } else if (needProcessDescend) {
                this.mDoubleWcdmaEcio = (VALUE_OLD_COEF_QUA_DES_SS * oldWcdmaEcio) + (VALUE_NEW_COEF_QUA_DES_SS * modemWcdmaEcio);
            } else {
                this.mDoubleWcdmaEcio = oldWcdmaEcio;
            }
            str = HwGsmServiceStateManager.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]WcdmaEcioAlaphFilter modem : ");
            stringBuilder.append(modemWcdmaEcio);
            stringBuilder.append("; old : ");
            stringBuilder.append(oldWcdmaEcio);
            stringBuilder.append("; new : ");
            stringBuilder.append(this.mDoubleWcdmaEcio);
            Rlog.d(str, stringBuilder.toString());
            if (this.mDoubleWcdmaEcio - modemWcdmaEcio <= -1.0d || this.mDoubleWcdmaEcio - modemWcdmaEcio >= 1.0d) {
                newSS.setWcdmaEcio((int) this.mDoubleWcdmaEcio);
                return true;
            }
            this.mDoubleWcdmaEcio = modemWcdmaEcio;
            newSS.setWcdmaEcio((int) this.mDoubleWcdmaEcio);
            return false;
        }

        public boolean processGsmSignalStrengthAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            double oldGsmSS = oldDoubleSS.getDoubleGsmSignalStrength();
            double modemGsmSS = (double) modemSS.getGsmSignalStrength();
            this.mOldDoubleGsmSS = oldGsmSS;
            String str = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder.append("]Before>>old : ");
            stringBuilder.append(oldGsmSS);
            stringBuilder.append("; instant new : ");
            stringBuilder.append(modemGsmSS);
            Rlog.d(str, stringBuilder.toString());
            if (modemGsmSS >= -1.0d) {
                modemGsmSS = -109.0d;
            }
            if (oldGsmSS <= modemGsmSS) {
                this.mDoubleGsmSS = modemGsmSS;
            } else if (needProcessDescend) {
                this.mDoubleGsmSS = ((7.0d * oldGsmSS) + (5.0d * modemGsmSS)) / 12.0d;
            } else {
                this.mDoubleGsmSS = oldGsmSS;
            }
            String str2 = HwGsmServiceStateManager.LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SUB[");
            stringBuilder2.append(HwGsmServiceStateManager.this.mPhoneId);
            stringBuilder2.append("]GsmSS AlaphFilter modem : ");
            stringBuilder2.append(modemGsmSS);
            stringBuilder2.append("; old : ");
            stringBuilder2.append(oldGsmSS);
            stringBuilder2.append("; new : ");
            stringBuilder2.append(this.mDoubleGsmSS);
            Rlog.d(str2, stringBuilder2.toString());
            if (this.mDoubleGsmSS - modemGsmSS <= -1.0d || this.mDoubleGsmSS - modemGsmSS >= 1.0d) {
                newSS.setGsmSignalStrength((int) this.mDoubleGsmSS);
                return true;
            }
            this.mDoubleGsmSS = modemGsmSS;
            newSS.setGsmSignalStrength((int) this.mDoubleGsmSS);
            return false;
        }

        public void proccessAlaphFilter(SignalStrength newSS, SignalStrength modemSS) {
            proccessAlaphFilter(this, newSS, modemSS, true);
        }

        public void proccessAlaphFilter(DoubleSignalStrength oldDoubleSS, SignalStrength newSS, SignalStrength modemSS, boolean needProcessDescend) {
            boolean needUpdate = false;
            if (oldDoubleSS == null) {
                String str = HwGsmServiceStateManager.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SUB[");
                stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
                stringBuilder.append("]proccess oldDoubleSS is null");
                Rlog.d(str, stringBuilder.toString());
                return;
            }
            if ((this.mTechState & 4) != 0) {
                needUpdate = false | processLteRsrpAlaphFilter(oldDoubleSS, newSS, modemSS, needProcessDescend);
                if (HwGsmServiceStateManager.this.FEATURE_SIGNAL_DUALPARAM) {
                    needUpdate |= processLteRssnrAlaphFilter(oldDoubleSS, newSS, modemSS, needProcessDescend);
                }
            }
            if ((this.mTechState & 2) != 0) {
                needUpdate |= processWcdmaRscpAlaphFilter(oldDoubleSS, newSS, modemSS, needProcessDescend);
                if (HwGsmServiceStateManager.this.FEATURE_SIGNAL_DUALPARAM) {
                    needUpdate |= processWcdmaEcioAlaphFilter(oldDoubleSS, newSS, modemSS, needProcessDescend);
                }
            }
            boolean isGsmPhone = true;
            if ((this.mTechState & 1) != 0) {
                needUpdate |= processGsmSignalStrengthAlaphFilter(oldDoubleSS, newSS, modemSS, needProcessDescend);
            }
            if (TelephonyManager.getDefault().getCurrentPhoneType(HwGsmServiceStateManager.this.mPhoneId) != 1) {
                isGsmPhone = false;
            }
            if (isGsmPhone || !HwGsmServiceStateManager.this.hasMessages(HwGsmServiceStateManager.EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH)) {
                setFakeSignalStrengthForSlowDescend(this, newSS);
                HwGsmServiceStateManager.this.mGsst.setSignalStrength(newSS);
                if (needUpdate) {
                    HwGsmServiceStateManager.this.sendMessageDelayUpdateSingalStrength(this.mDelayTime);
                }
                return;
            }
            HwGsmServiceStateManager.this.removeMessages(HwGsmServiceStateManager.EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH);
        }

        private void setFakeSignalStrengthForSlowDescend(DoubleSignalStrength oldDoubleSS, SignalStrength newSS) {
            int oldLevel;
            int newLevel;
            int diffLevel;
            String str;
            StringBuilder stringBuilder;
            SignalThreshold signalThreshold;
            int lowerLevel;
            int lteRsrp;
            String str2;
            StringBuilder stringBuilder2;
            int lteRssnr;
            this.mDelayTime = 0;
            if ((this.mTechState & 4) != 0) {
                oldLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.LTE, (int) oldDoubleSS.getOldDoubleLteRsrp(), (int) oldDoubleSS.getOldDoubleLteRssnr());
                newLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.LTE, newSS.getLteRsrp(), newSS.getLteRssnr());
                diffLevel = oldLevel - newLevel;
                str = HwGsmServiceStateManager.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SUB[");
                stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
                stringBuilder.append("]LTE oldLevel: ");
                stringBuilder.append(oldLevel);
                stringBuilder.append(", newLevel: ");
                stringBuilder.append(newLevel);
                Rlog.d(str, stringBuilder.toString());
                if (diffLevel > 1) {
                    signalThreshold = HwGsmServiceStateManager.mHwSigStr.getSignalThreshold(SignalType.LTE);
                    if (signalThreshold != null) {
                        lowerLevel = oldLevel - 1;
                        lteRsrp = signalThreshold.getHighThresholdBySignalLevel(lowerLevel, false);
                        if (-1 != lteRsrp) {
                            this.mDoubleLteRsrp = (double) lteRsrp;
                            newSS.setLteRsrp(lteRsrp);
                            this.mDelayTime = HwGsmServiceStateManager.VALUE_DELAY_DURING_TIME / diffLevel;
                        }
                        str2 = HwGsmServiceStateManager.LOG_TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SUB[");
                        stringBuilder2.append(HwGsmServiceStateManager.this.mPhoneId);
                        stringBuilder2.append("]LTE lowerLevel: ");
                        stringBuilder2.append(lowerLevel);
                        stringBuilder2.append(", lteRsrp: ");
                        stringBuilder2.append(lteRsrp);
                        Rlog.d(str2, stringBuilder2.toString());
                        if (HwGsmServiceStateManager.this.FEATURE_SIGNAL_DUALPARAM) {
                            lteRssnr = signalThreshold.getHighThresholdBySignalLevel(lowerLevel, true);
                            if (-1 != lteRssnr) {
                                this.mDoubleLteRssnr = (double) lteRssnr;
                                newSS.setLteRssnr(lteRssnr);
                                this.mDelayTime = HwGsmServiceStateManager.VALUE_DELAY_DURING_TIME / diffLevel;
                            }
                        }
                    }
                }
            }
            if ((this.mTechState & 2) != 0) {
                oldLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.UMTS, (int) oldDoubleSS.getOldDoubleWcdmaRscp(), (int) oldDoubleSS.getOldDoubleWcdmaEcio());
                newLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.UMTS, newSS.getWcdmaRscp(), newSS.getWcdmaEcio());
                diffLevel = oldLevel - newLevel;
                str = HwGsmServiceStateManager.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SUB[");
                stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
                stringBuilder.append("]UMTS oldLevel: ");
                stringBuilder.append(oldLevel);
                stringBuilder.append(", newLevel: ");
                stringBuilder.append(newLevel);
                Rlog.d(str, stringBuilder.toString());
                if (diffLevel > 1) {
                    signalThreshold = HwGsmServiceStateManager.mHwSigStr.getSignalThreshold(SignalType.UMTS);
                    if (signalThreshold != null) {
                        lowerLevel = oldLevel - 1;
                        lteRsrp = signalThreshold.getHighThresholdBySignalLevel(lowerLevel, false);
                        if (-1 != lteRsrp) {
                            this.mDoubleWcdmaRscp = (double) lteRsrp;
                            newSS.setWcdmaRscp(lteRsrp);
                            this.mDelayTime = HwGsmServiceStateManager.VALUE_DELAY_DURING_TIME / diffLevel;
                        }
                        str2 = HwGsmServiceStateManager.LOG_TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SUB[");
                        stringBuilder2.append(HwGsmServiceStateManager.this.mPhoneId);
                        stringBuilder2.append("]UMTS lowerLevel: ");
                        stringBuilder2.append(lowerLevel);
                        stringBuilder2.append(", wcdmaRscp: ");
                        stringBuilder2.append(lteRsrp);
                        Rlog.d(str2, stringBuilder2.toString());
                        if (HwGsmServiceStateManager.this.FEATURE_SIGNAL_DUALPARAM) {
                            lteRssnr = signalThreshold.getHighThresholdBySignalLevel(lowerLevel, true);
                            if (-1 != lteRssnr) {
                                this.mDoubleWcdmaEcio = (double) lteRssnr;
                                newSS.setWcdmaEcio(lteRssnr);
                                this.mDelayTime = HwGsmServiceStateManager.VALUE_DELAY_DURING_TIME / diffLevel;
                            }
                        }
                    }
                }
            }
            if ((this.mTechState & 1) != 0) {
                oldLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.GSM, (int) oldDoubleSS.getOldDoubleGsmSignalStrength(), 255);
                newLevel = HwGsmServiceStateManager.mHwSigStr.getLevel(SignalType.GSM, newSS.getGsmSignalStrength(), 255);
                diffLevel = oldLevel - newLevel;
                str = HwGsmServiceStateManager.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SUB[");
                stringBuilder.append(HwGsmServiceStateManager.this.mPhoneId);
                stringBuilder.append("]GSM oldLevel: ");
                stringBuilder.append(oldLevel);
                stringBuilder.append(", newLevel: ");
                stringBuilder.append(newLevel);
                Rlog.d(str, stringBuilder.toString());
                if (diffLevel > 1) {
                    SignalThreshold signalThreshold2 = HwGsmServiceStateManager.mHwSigStr.getSignalThreshold(SignalType.GSM);
                    if (signalThreshold2 != null) {
                        int lowerLevel2 = oldLevel - 1;
                        int gsmSS = signalThreshold2.getHighThresholdBySignalLevel(lowerLevel2, false);
                        if (-1 != gsmSS) {
                            this.mDoubleGsmSS = (double) gsmSS;
                            newSS.setGsmSignalStrength(gsmSS);
                            this.mDelayTime = HwGsmServiceStateManager.VALUE_DELAY_DURING_TIME / diffLevel;
                        }
                        String str3 = HwGsmServiceStateManager.LOG_TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("SUB[");
                        stringBuilder3.append(HwGsmServiceStateManager.this.mPhoneId);
                        stringBuilder3.append("]GSM lowerLevel: ");
                        stringBuilder3.append(lowerLevel2);
                        stringBuilder3.append(", gsmSS: ");
                        stringBuilder3.append(gsmSS);
                        Rlog.d(str3, stringBuilder3.toString());
                    }
                }
            }
        }
    }

    static {
        boolean z = false;
        if ("389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"))) {
            z = true;
        }
        IS_VERIZON = z;
    }

    public HwGsmServiceStateManager(ServiceStateTracker sst, GsmCdmaPhone gsmPhone) {
        super(sst, gsmPhone);
        this.mGsst = sst;
        this.mGsmPhone = gsmPhone;
        this.mContext = gsmPhone.getContext();
        this.mCr = this.mContext.getContentResolver();
        this.mPhoneId = gsmPhone.getPhoneId();
        this.mCi = gsmPhone.mCi;
        this.mCi.registerForRplmnsStateChanged(this, EVENT_RPLMNS_STATE_CHANGED, null);
        sendMessage(obtainMessage(EVENT_RPLMNS_STATE_CHANGED));
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]constructor init");
        Rlog.d(str, stringBuilder.toString());
        this.mHwCustGsmServiceStateManager = (HwCustGsmServiceStateManager) HwCustUtils.createObj(HwCustGsmServiceStateManager.class, new Object[]{sst, gsmPhone});
        addBroadCastReceiver();
        this.mMainSlot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        this.mCi.registerForCaStateChanged(this, EVENT_CA_STATE_CHANGED, null);
        this.mCi.registerForCrrConn(this, EVENT_CRR_CONN, null);
        this.mCi.setOnRegPLMNSelInfo(this, EVENT_PLMN_SELINFO, null);
        this.mGsmPhone.registerForMccChanged(this, EVENT_MCC_CHANGED, null);
        this.mCi.registerForDSDSMode(this, EVENT_DSDS_MODE, null);
        this.mCi.registerForUnsol4RMimoStatus(this, EVENT_4R_MIMO_ENABLE, null);
        registerCloudOtaBroadcastReceiver();
        if (PS_CLEARCODE || SHOW_REJ_INFO_KT || IS_VERIZON) {
            this.mCi.setOnNetReject(this, EVENT_NETWORK_REJECTED_CASE, null);
        }
    }

    private ServiceState getNewSS() {
        return ServiceStateTrackerUtils.getNewSS(this.mGsst);
    }

    private ServiceState getSS() {
        return this.mGsst.mSS;
    }

    public String getRplmn() {
        return this.rplmn;
    }

    public boolean getRoamingStateHw(boolean roaming) {
        StringBuilder stringBuilder;
        if (this.mHwCustGsmServiceStateManager != null) {
            this.mHwCustGsmServiceStateManager.storeModemRoamingStatus(roaming);
        }
        boolean isCTCard_Reg_GSM = true;
        if (roaming) {
            String hplmn = null;
            if (this.mGsmPhone.mIccRecords.get() != null) {
                hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            }
            String regplmn = getNewSS().getOperatorNumeric();
            String regplmnCustomString = null;
            if (getNoRoamingByMcc(getNewSS())) {
                roaming = false;
            }
            try {
                regplmnCustomString = System.getString(this.mContext.getContentResolver(), "reg_plmn_custom");
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handlePollStateResult plmnCustomString = ");
                stringBuilder2.append(regplmnCustomString);
                Rlog.d("HwGsmServiceStateTracker", stringBuilder2.toString());
            } catch (Exception e) {
                Rlog.e("HwGsmServiceStateTracker", "Exception when got name value", e);
            }
            if (regplmnCustomString != null) {
                String[] regplmnCustomArray = regplmnCustomString.split(";");
                if (TextUtils.isEmpty(hplmn) || TextUtils.isEmpty(regplmn)) {
                    roaming = false;
                } else {
                    String[] regplmnCustomArrEleBuf = null;
                    for (String split : regplmnCustomArray) {
                        regplmnCustomArrEleBuf = split.split(",");
                        boolean isContainplmn = containsPlmn(hplmn, regplmnCustomArrEleBuf) && containsPlmn(regplmn, regplmnCustomArrEleBuf);
                        if (isContainplmn) {
                            roaming = false;
                            break;
                        }
                    }
                }
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("roaming = ");
        stringBuilder3.append(roaming);
        Rlog.d("GsmServiceStateTracker", stringBuilder3.toString());
        if (!(HwTelephonyManagerInner.getDefault().isCTSimCard(this.mGsmPhone.getPhoneId()) && getNewSS().getState() == 0 && ServiceState.isGsm(getNewSS().getRilVoiceRadioTechnology()) && !ServiceState.isLte(getNewSS().getRilVoiceRadioTechnology()))) {
            isCTCard_Reg_GSM = false;
        }
        if (isCTCard_Reg_GSM) {
            roaming = true;
            stringBuilder = new StringBuilder();
            stringBuilder.append("When CT card register in GSM/UMTS, it always should be roaming");
            stringBuilder.append(true);
            Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
        }
        if (this.mHwCustGsmServiceStateManager != null) {
            roaming = this.mHwCustGsmServiceStateManager.setRoamingStateForOperatorCustomization(getNewSS(), roaming);
            stringBuilder = new StringBuilder();
            stringBuilder.append("roaming customization for MCC 302 roaming=");
            stringBuilder.append(roaming);
            Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
        }
        roaming = getGsmRoamingSpecialCustByNetType(getGsmRoamingCustByIMSIStart(roaming));
        if (this.mHwCustGsmServiceStateManager != null) {
            return this.mHwCustGsmServiceStateManager.checkIsInternationalRoaming(roaming, getNewSS());
        }
        return roaming;
    }

    private boolean getGsmRoamingSpecialCustByNetType(boolean roaming) {
        if (this.mGsmPhone.mIccRecords.get() != null) {
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            String regplmn = getNewSS().getOperatorNumeric();
            int netType = getNewSS().getVoiceNetworkType();
            TelephonyManager.getDefault();
            int netClass = TelephonyManager.getNetworkClass(netType);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getGsmRoamingSpecialCustByNetType: hplmn=");
            stringBuilder.append(hplmn);
            stringBuilder.append(" regplmn=");
            stringBuilder.append(regplmn);
            stringBuilder.append(" netType=");
            stringBuilder.append(netType);
            stringBuilder.append(" netClass=");
            stringBuilder.append(netClass);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
            if ("50218".equals(hplmn) && "50212".equals(regplmn)) {
                if (1 == netClass || 2 == netClass) {
                    roaming = false;
                }
                if (3 == netClass) {
                    roaming = true;
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getGsmRoamingSpecialCustByNetType: roaming = ");
        stringBuilder2.append(roaming);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder2.toString());
        return roaming;
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x017b  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0174  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x01a0  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x0205  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getPlmn() {
        if (getCombinedRegState(getSS()) != 0) {
            return null;
        }
        String data;
        String str;
        int slotId;
        StringBuilder stringBuilder;
        String operatorNumeric = getSS().getOperatorNumeric();
        try {
            data = System.getString(this.mCr, "plmn");
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception when got data value", e);
            data = null;
        }
        PlmnConstants plmnConstants = new PlmnConstants(data);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(Locale.getDefault().getLanguage());
        stringBuilder2.append("_");
        stringBuilder2.append(Locale.getDefault().getCountry());
        String languageCode = stringBuilder2.toString();
        String plmnValue = plmnConstants.getPlmnValue(operatorNumeric, languageCode);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getPlmn languageCode:");
        stringBuilder3.append(languageCode);
        stringBuilder3.append("  plmnValue:");
        stringBuilder3.append(plmnValue);
        Rlog.d(str2, stringBuilder3.toString());
        if (plmnValue == null) {
            str2 = "en_us";
            plmnValue = plmnConstants.getPlmnValue(operatorNumeric, "en_us");
            str = LOG_TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("get default en_us plmn name:");
            stringBuilder4.append(plmnValue);
            Rlog.d(str, stringBuilder4.toString());
        }
        int slotId2 = this.mGsmPhone.getPhoneId();
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("slotId = ");
        stringBuilder3.append(slotId2);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder3.toString());
        str2 = null;
        str = null;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            str2 = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            str = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getIMSI();
        }
        String hplmn = str2;
        String imsi = str;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("hplmn = ");
        stringBuilder3.append(hplmn);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder3.toString());
        boolean z = false;
        boolean z2 = this.mHwCustGsmServiceStateManager != null && this.mHwCustGsmServiceStateManager.notUseVirtualName(imsi);
        boolean isUseVirtualName = z2;
        boolean roaming = getSS().getRoaming();
        z2 = this.mHwCustGsmServiceStateManager != null && this.mHwCustGsmServiceStateManager.iscustRoamingRuleAffect(roaming);
        boolean isMatchRoamingRule = z2;
        String imsi2;
        String hplmn2;
        if (isUseVirtualName) {
            imsi2 = imsi;
            hplmn2 = hplmn;
            slotId = slotId2;
        } else if (isMatchRoamingRule) {
            boolean z3 = roaming;
            imsi2 = imsi;
            hplmn2 = hplmn;
            slotId = slotId2;
        } else {
            ApnReminder apnReminder;
            slotId = slotId2;
            imsi2 = imsi;
            hplmn2 = hplmn;
            plmnValue = getVirCarrierOperatorName(HwTelephonyFactory.getHwPhoneManager().getVirtualNetOperatorName(plmnValue, roaming, hasNitzOperatorName(slotId2), slotId, hplmn), roaming, hasNitzOperatorName(slotId2), slotId2, hplmn2);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("VirtualNetName = ");
            stringBuilder3.append(plmnValue);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder3.toString());
            if (isMultiSimEnabled) {
                apnReminder = ApnReminder.getInstance(this.mContext);
            } else {
                apnReminder = ApnReminder.getInstance(this.mContext, slotId);
            }
            if (!(apnReminder.isPopupApnSettingsEmpty() || getSS().getRoaming() || hasNitzOperatorName(slotId) || hplmn2 == null)) {
                z = true;
            }
            if (z) {
                int apnId = getPreferedApnId();
                if (-1 != apnId) {
                    plmnValue = apnReminder.getOnsNameByPreferedApn(apnId, plmnValue);
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("apnReminder plmnValue = ");
                    stringBuilder5.append(plmnValue);
                    Rlog.d("HwGsmServiceStateTracker", stringBuilder5.toString());
                } else {
                    plmnValue = null;
                }
            }
            plmnValue = getGsmOnsDisplayPlmnByAbbrevPriority(getGsmOnsDisplayPlmnByPriority(plmnValue, slotId), slotId);
            if (TextUtils.isEmpty(plmnValue)) {
                getSS().setOperatorAlphaLong(plmnValue);
            } else {
                plmnValue = getVirtualNetPlmnValue(operatorNumeric, hplmn2, imsi2, getEons(getSS().getOperatorAlphaLong()));
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("plmnValue = ");
            stringBuilder.append(plmnValue);
            Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
            if (HwPlmnActConcat.needPlmnActConcat()) {
                plmnValue = HwPlmnActConcat.getPlmnActConcat(plmnValue, getSS());
            }
            return plmnValue;
        }
        Rlog.d("HwGsmServiceStateTracker", "passed the Virtualnet cust");
        if (isMultiSimEnabled) {
        }
        z = true;
        if (z) {
        }
        plmnValue = getGsmOnsDisplayPlmnByAbbrevPriority(getGsmOnsDisplayPlmnByPriority(plmnValue, slotId), slotId);
        if (TextUtils.isEmpty(plmnValue)) {
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("plmnValue = ");
        stringBuilder.append(plmnValue);
        Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
        if (HwPlmnActConcat.needPlmnActConcat()) {
        }
        return plmnValue;
    }

    public String getVirCarrierOperatorName(String plmnValue, boolean roaming, boolean hasNitzOperatorName, int slotId, String hplmn) {
        if (roaming || hplmn == null || hasNitzOperatorName) {
            return plmnValue;
        }
        String custplmn = (String) HwCfgFilePolicy.getValue("virtualnet_operatorname", slotId, String.class);
        if (TextUtils.isEmpty(custplmn)) {
            return plmnValue;
        }
        plmnValue = custplmn;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getVirCarrierOperatorName: plmnValue = ");
        stringBuilder.append(plmnValue);
        stringBuilder.append(" custplmn = ");
        stringBuilder.append(custplmn);
        Rlog.d(str, stringBuilder.toString());
        return plmnValue;
    }

    public String getGsmOnsDisplayPlmnByAbbrevPriority(String custPlmnValue, int slotId) {
        String result = custPlmnValue;
        String plmnAbbrev = null;
        String custPlmn = System.getString(this.mCr, "hw_plmn_abbrev");
        if (this.mGsmPhone.mIccRecords.get() != null) {
            String str;
            StringBuilder stringBuilder;
            CustPlmnMember cpm = CustPlmnMember.getInstance();
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            if (cpm.acquireFromCust(hplmn, getSS(), custPlmn)) {
                plmnAbbrev = cpm.plmn;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" plmn2 =");
                stringBuilder.append(plmnAbbrev);
                Rlog.d(str, stringBuilder.toString());
            }
            if (cpm.getCfgCustDisplayParams(hplmn, getSS(), "plmn_abbrev", slotId)) {
                plmnAbbrev = cpm.plmn;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwCfgFile: plmn =");
                stringBuilder.append(plmnAbbrev);
                Rlog.d(str, stringBuilder.toString());
            }
        }
        if (TextUtils.isEmpty(plmnAbbrev)) {
            return custPlmnValue;
        }
        if (hasNitzOperatorName(slotId)) {
            result = getEons(getSS().getOperatorAlphaLong());
        } else {
            result = getEons(plmnAbbrev);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("plmnValueByAbbrevPriority = ");
        stringBuilder2.append(result);
        stringBuilder2.append(" slotId = ");
        stringBuilder2.append(slotId);
        stringBuilder2.append(" PlmnValue = ");
        stringBuilder2.append(custPlmnValue);
        Rlog.d("getGsmOnsDisplayPlmnByAbbrevPriority, ", stringBuilder2.toString());
        return result;
    }

    public boolean getCarrierConfigPri(int slotId) {
        boolean custPriority = false;
        try {
            Boolean carrerPriority = (Boolean) HwGetCfgFileConfig.getCfgFileData(new HwCfgKey("net_sim_ue_pri", "network_mccmnc", null, null, "network_highest", getSS().getOperatorNumeric(), null, null, slotId), Boolean.class);
            if (carrerPriority != null) {
                return carrerPriority.booleanValue();
            }
            return custPriority;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: read net_sim_ue_pri error ");
            stringBuilder.append(e.toString());
            log(stringBuilder.toString());
            return custPriority;
        }
    }

    public String getGsmOnsDisplayPlmnByPriority(String custPlmnValue, int slotId) {
        boolean HW_NETWORK_SIM_UE_PRIORITY = SystemProperties.getBoolean("ro.config.net_sim_ue_pri", false);
        boolean CONFIG_NETWORK_SIM_UE_PRIORITY = getCarrierConfigPri(slotId);
        boolean CUST_HPLMN_REGPLMN = enablePlmnByNetSimUePriority();
        if (!HW_NETWORK_SIM_UE_PRIORITY && !CUST_HPLMN_REGPLMN && !CONFIG_NETWORK_SIM_UE_PRIORITY) {
            return custPlmnValue;
        }
        String result = custPlmnValue;
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        String spnSim = null;
        if (r != null) {
            spnSim = r.getServiceProviderName();
        }
        if (hasNitzOperatorName(slotId)) {
            result = getEons(result);
            result = getSS().getOperatorAlphaLong();
        } else {
            result = getSS().getOperatorAlphaLong();
            if (custPlmnValue != null) {
                result = custPlmnValue;
            }
            result = getEons(result);
            if ((CUST_HPLMN_REGPLMN || CONFIG_NETWORK_SIM_UE_PRIORITY) && !TextUtils.isEmpty(spnSim)) {
                result = spnSim;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plmnValue = ");
        stringBuilder.append(result);
        stringBuilder.append(" slotId = ");
        stringBuilder.append(slotId);
        stringBuilder.append(" custPlmnValue = ");
        stringBuilder.append(custPlmnValue);
        Rlog.d("getGsmOnsDisplayPlmnByPriority, ", stringBuilder.toString());
        return result;
    }

    public boolean enablePlmnByNetSimUePriority() {
        String hplmn = null;
        String regplmn = null;
        boolean cust_hplmn_regplmn = false;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            regplmn = getSS().getOperatorNumeric();
        }
        String custNetSimUePriority = System.getString(this.mCr, "hw_net_sim_ue_pri");
        if (!TextUtils.isEmpty(hplmn) && !TextUtils.isEmpty(regplmn) && !TextUtils.isEmpty(custNetSimUePriority)) {
            for (String mccmnc_item : custNetSimUePriority.split(";")) {
                String[] mccmncs = mccmnc_item.split(",");
                if (hplmn.equals(mccmncs[0]) && regplmn.equals(mccmncs[1])) {
                    cust_hplmn_regplmn = true;
                    break;
                }
            }
        } else {
            cust_hplmn_regplmn = false;
            Rlog.d(LOG_TAG, "enablePlmnByNetSimUePriority() failed, custNetSimUePriority or hplmm or regplmn is null or empty string");
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" cust_hplmn_equal_regplmn = ");
        stringBuilder.append(cust_hplmn_regplmn);
        Rlog.d(str, stringBuilder.toString());
        return cust_hplmn_regplmn;
    }

    private String getVirtualNetPlmnValue(String operatorNumeric, String hplmn, String imsi, String plmnValue) {
        if (hasNitzOperatorName(this.mGsmPhone.getPhoneId())) {
            return plmnValue;
        }
        if ("22299".equals(operatorNumeric)) {
            if (hplmn == null) {
                return " ";
            }
            if (imsi != null && imsi.startsWith("222998")) {
                return " ";
            }
        }
        if ("22201".equals(operatorNumeric)) {
            if (hplmn == null) {
                return " ";
            }
            IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
            String spnSim = null;
            if (r != null) {
                spnSim = r.getServiceProviderName();
            }
            if ("Coop Mobile".equals(spnSim) && imsi != null && imsi.startsWith("22201")) {
                return " ";
            }
        }
        return plmnValue;
    }

    private int getPreferedApnId() {
        Cursor cursor;
        int apnId = -1;
        if (isMultiSimEnabled) {
            cursor = this.mContext.getContentResolver().query(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) this.mGsmPhone.getPhoneId()), new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        } else {
            cursor = this.mContext.getContentResolver().query(PREFERAPN_NO_UPDATE_URI, new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        }
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apnId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String apnName = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
            String carrierName = cursor.getString(cursor.getColumnIndexOrThrow(NumMatchs.NAME));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPreferedApnId: ");
            stringBuilder.append(apnId);
            stringBuilder.append(", apn: ");
            stringBuilder.append(apnName);
            stringBuilder.append(", name: ");
            stringBuilder.append(carrierName);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
        }
        if (cursor != null) {
            cursor.close();
        }
        return apnId;
    }

    private boolean isChinaMobileMccMnc() {
        String hplmn = null;
        String regplmn = null;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            regplmn = getSS().getOperatorNumeric();
        }
        String[] mccMncList = CHINAMOBILE_MCCMNC.split(";");
        boolean z = false;
        if (TextUtils.isEmpty(regplmn) || TextUtils.isEmpty(hplmn)) {
            return false;
        }
        boolean isHplmnCMCC = false;
        boolean isHplmnCMCC2 = false;
        for (boolean isRegplmnCMCC = false; isRegplmnCMCC < mccMncList.length; isRegplmnCMCC++) {
            if (mccMncList[isRegplmnCMCC].equals(regplmn)) {
                isHplmnCMCC2 = true;
            }
            if (mccMncList[isRegplmnCMCC].equals(hplmn)) {
                isHplmnCMCC = true;
            }
        }
        if (isHplmnCMCC2 && isHplmnCMCC) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:126:0x03ab  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0295  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0295  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x03ab  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public OnsDisplayParams getOnsDisplayParamsHw(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        String str;
        boolean rule2;
        String str2;
        StringBuilder stringBuilder;
        int rule3;
        String str3;
        boolean showSpn2;
        int rule4;
        boolean z;
        OnsDisplayParams odp;
        String tempPlmn = plmn;
        OnsDisplayParams odp2 = getOnsDisplayParamsBySpnOnly(showSpn, showPlmn, rule, plmn, spn);
        boolean showSpn3 = odp2.mShowSpn;
        boolean showPlmn2 = odp2.mShowPlmn;
        int rule5 = odp2.mRule;
        String plmn2 = odp2.mPlmn;
        String spn2 = odp2.mSpn;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            StringBuilder stringBuilder2;
            boolean showPlmn3;
            String spn3;
            CustPlmnMember cpm = CustPlmnMember.getInstance();
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumericEx(this.mCr, "hw_ons_hplmn_ex");
            int slotId = SubscriptionManager.getSlotIndex(this.mGsmPhone.getSubId());
            String regplmn = getSS().getOperatorNumeric();
            String custSpn = System.getString(this.mCr, "hw_plmn_spn");
            if (custSpn != null) {
                str = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                showPlmn3 = showPlmn2;
                stringBuilder2.append("custSpn length =");
                stringBuilder2.append(custSpn.length());
                Rlog.d(str, stringBuilder2.toString());
            } else {
                showPlmn3 = showPlmn2;
            }
            if (cpm.acquireFromCust(hplmn, getSS(), custSpn)) {
                boolean showSpn4;
                boolean showPlmn4;
                String plmn3;
                showSpn3 = cpm.judgeShowSpn(showSpn3);
                showPlmn2 = cpm.rule == 0 ? showPlmn3 : cpm.showPlmn;
                rule2 = cpm.rule == 0 ? rule5 : cpm.rule;
                plmn2 = cpm.judgePlmn(plmn2);
                spn2 = cpm.judgeSpn(spn2);
                if (1 == cpm.rule && TextUtils.isEmpty(spn2)) {
                    str2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    showSpn4 = showSpn3;
                    stringBuilder.append(" want to show spn while spn is null,use plmn instead ");
                    stringBuilder.append(tempPlmn);
                    Rlog.d(str2, stringBuilder.toString());
                    showSpn3 = tempPlmn;
                    if (TextUtils.isEmpty(((IccRecords) this.mGsmPhone.mIccRecords.get()).getServiceProviderName())) {
                        showSpn3 = getDefaultSpn(showSpn3, hplmn, regplmn);
                    }
                    str2 = showSpn3;
                } else {
                    showSpn4 = showSpn3;
                    str2 = spn2;
                }
                if (4 == cpm.rule) {
                    spn2 = getEonsWithoutCphs();
                    str = LOG_TAG;
                    boolean pnnEmpty = false;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("temPnn = ");
                    stringBuilder3.append(spn2);
                    Rlog.d(str, stringBuilder3.toString());
                    if (TextUtils.isEmpty(spn2)) {
                        showSpn3 = true;
                    } else {
                        showSpn3 = pnnEmpty;
                    }
                    if (!showSpn3 || TextUtils.isEmpty(str2)) {
                        showPlmn2 = true;
                        if (!showSpn3) {
                            plmn2 = spn2;
                        }
                    } else {
                        Rlog.d(LOG_TAG, "want to show PNN while PNN is null, show SPN instead ");
                        showPlmn2 = true;
                    }
                    showPlmn4 = showPlmn2 & 2;
                    showPlmn3 = showPlmn2;
                    showSpn4 = (showPlmn2 & 1) == 1;
                    plmn3 = plmn2;
                } else {
                    showPlmn4 = showPlmn2;
                    showPlmn3 = rule2;
                    plmn3 = plmn2;
                }
                if (5 == cpm.rule) {
                    spn3 = getGsmOnsDisplayParamsSpnPrior(showSpn4, showPlmn4, showPlmn3, plmn3, str2);
                    showPlmn2 = spn3.mShowSpn;
                    rule2 = spn3.mShowPlmn;
                    rule3 = spn3.mRule;
                    String str4 = spn3.mPlmn;
                    spn2 = spn3.mSpn;
                    showSpn3 = showPlmn2;
                    showPlmn2 = rule2;
                    rule5 = rule3;
                    plmn2 = str4;
                } else {
                    spn2 = str2;
                    rule5 = showPlmn3;
                    plmn2 = plmn3;
                    showSpn3 = showSpn4;
                    showPlmn2 = showPlmn4;
                }
                str = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("showSpn2 =");
                stringBuilder2.append(showSpn3);
                stringBuilder2.append(" showPlmn2 =");
                stringBuilder2.append(showPlmn2);
                stringBuilder2.append(" spn2 =");
                stringBuilder2.append(spn2);
                stringBuilder2.append(" plmn2 =");
                stringBuilder2.append(plmn2);
                Rlog.d(str, stringBuilder2.toString());
            } else {
                showPlmn2 = showPlmn3;
            }
            if (VirtualNetOnsExtend.isVirtualNetOnsExtend() && this.mGsmPhone.mIccRecords.get() != null) {
                VirtualNetOnsExtend.createVirtualNetByHplmn(hplmn, (IccRecords) this.mGsmPhone.mIccRecords.get());
                if (VirtualNetOnsExtend.getCurrentVirtualNet() != null) {
                    str = VirtualNetOnsExtend.getCurrentVirtualNet().getOperatorName();
                    str3 = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    showSpn2 = showSpn3;
                    stringBuilder2.append("hplmn=");
                    stringBuilder2.append(hplmn);
                    stringBuilder2.append(" regplmn=");
                    stringBuilder2.append(regplmn);
                    stringBuilder2.append(" VirtualNetOnsExtend.custOns=");
                    stringBuilder2.append(str);
                    Rlog.d(str3, stringBuilder2.toString());
                    if (!TextUtils.isEmpty(str) && cpm.acquireFromCust(hplmn, getSS(), str)) {
                        showSpn3 = cpm.showSpn;
                        showPlmn2 = cpm.showPlmn;
                        rule5 = cpm.rule;
                        plmn2 = cpm.judgePlmn(plmn2);
                        spn2 = cpm.judgeSpn(spn2);
                        if (1 == cpm.rule && TextUtils.isEmpty(spn2)) {
                            str3 = LOG_TAG;
                            stringBuilder2 = new StringBuilder();
                            rule4 = rule5;
                            stringBuilder2.append("want to show spn while spn is null,use plmn instead ");
                            stringBuilder2.append(tempPlmn);
                            Rlog.d(str3, stringBuilder2.toString());
                            rule5 = tempPlmn;
                            if (TextUtils.isEmpty(((IccRecords) this.mGsmPhone.mIccRecords.get()).getServiceProviderName())) {
                                rule5 = getDefaultSpn(rule5, hplmn, regplmn);
                            }
                            spn2 = rule5;
                        } else {
                            rule4 = rule5;
                        }
                        String str5 = LOG_TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("showSpn2=");
                        stringBuilder4.append(showSpn3);
                        stringBuilder4.append(" showPlmn2=");
                        stringBuilder4.append(showPlmn2);
                        stringBuilder4.append(" spn2=");
                        stringBuilder4.append(spn2);
                        stringBuilder4.append(" plmn2=");
                        stringBuilder4.append(plmn2);
                        Rlog.d(str5, stringBuilder4.toString());
                        showSpn2 = showSpn3;
                        if (cpm.getCfgCustDisplayParams(hplmn, getSS(), "plmn_spn", slotId)) {
                            showSpn3 = cpm.rule == 0 ? rule4 : cpm.rule;
                            rule5 = "####".equals(cpm.plmn) ? plmn2 : cpm.plmn;
                            plmn2 = "####".equals(cpm.spn) ? spn2 : cpm.spn;
                            if (1 == cpm.rule && TextUtils.isEmpty(plmn2)) {
                                spn2 = LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(" want to show spn while spn is null,use plmn instead ");
                                stringBuilder.append(tempPlmn);
                                Rlog.d(spn2, stringBuilder.toString());
                                plmn2 = tempPlmn;
                                if (TextUtils.isEmpty(((IccRecords) this.mGsmPhone.mIccRecords.get()).getServiceProviderName())) {
                                    plmn2 = getDefaultSpn(plmn2, hplmn, regplmn);
                                }
                            }
                            boolean rule6;
                            if (4 == cpm.rule) {
                                boolean pnnEmpty2 = false;
                                str = null;
                                IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
                                if (r == null || r.isEonsDisabled()) {
                                    rule6 = showSpn3;
                                } else {
                                    rule6 = showSpn3;
                                    Rlog.d(LOG_TAG, "getEons():get plmn from SIM card! ");
                                    if (updateEons(r)) {
                                        str = r.getEons();
                                    }
                                }
                                spn3 = LOG_TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("temPnn = ");
                                stringBuilder2.append(str);
                                Rlog.d(spn3, stringBuilder2.toString());
                                if (TextUtils.isEmpty(str)) {
                                    pnnEmpty2 = true;
                                }
                                if (!pnnEmpty2 || TextUtils.isEmpty(plmn2)) {
                                    showSpn3 = true;
                                    if (!pnnEmpty2) {
                                        rule5 = str;
                                    }
                                } else {
                                    Rlog.d(LOG_TAG, "want to show PNN while PNN is null, show SPN instead ");
                                    showSpn3 = true;
                                }
                            } else {
                                rule6 = showSpn3;
                                z = showPlmn2;
                            }
                            str2 = true;
                            showPlmn2 = showSpn3 & 1;
                            if ((showSpn3 & 2) != 2) {
                                str2 = null;
                            }
                            spn2 = str2;
                            str = LOG_TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("getCfgCustDisplayParams showSpn=");
                            stringBuilder4.append(showPlmn2);
                            stringBuilder4.append(" showPlmn=");
                            stringBuilder4.append(spn2);
                            stringBuilder4.append(" spn=");
                            stringBuilder4.append(plmn2);
                            stringBuilder4.append(" plmn=");
                            stringBuilder4.append(rule5);
                            stringBuilder4.append(" rule=");
                            stringBuilder4.append(showSpn3);
                            Rlog.d(str, stringBuilder4.toString());
                            rule4 = showSpn3;
                            showSpn2 = showPlmn2;
                            str3 = rule5;
                            str2 = plmn2;
                            z = spn2;
                        } else {
                            z = showPlmn2;
                            str3 = plmn2;
                            str2 = spn2;
                        }
                    }
                    rule4 = rule5;
                    if (cpm.getCfgCustDisplayParams(hplmn, getSS(), "plmn_spn", slotId)) {
                    }
                }
            }
            showSpn2 = showSpn3;
            rule4 = rule5;
            if (cpm.getCfgCustDisplayParams(hplmn, getSS(), "plmn_spn", slotId)) {
            }
        } else {
            OnsDisplayParams onsDisplayParams = odp2;
            showSpn2 = showSpn3;
            rule4 = rule5;
            str3 = plmn2;
            str2 = spn2;
            z = showPlmn2;
        }
        showPlmn2 = showSpn2;
        rule2 = z;
        rule3 = rule4;
        spn2 = str3;
        str = str2;
        OnsDisplayParams odpCust = getGsmOnsDisplayParamsBySpecialCust(showPlmn2, rule2, rule3, spn2, str);
        OnsDisplayParams odpForChinaOperator = getGsmOnsDisplayParamsForChinaOperator(showPlmn2, rule2, rule3, spn2, str);
        OnsDisplayParams odpForGeneralOperator = null;
        OnsDisplayParams odpCustbyVirtualNetType = null;
        OnsDisplayParams thisR = getGsmOnsDisplayParamsBySpnCust(showPlmn2, rule2, rule3, spn2, str);
        OnsDisplayParams odpCustForVideotron = null;
        if (this.mHwCustGsmServiceStateManager != null) {
            boolean z2 = showSpn2;
            boolean z3 = z;
            int i = rule4;
            String str6 = str3;
            String str7 = str2;
            odpForGeneralOperator = this.mHwCustGsmServiceStateManager.getGsmOnsDisplayParamsForGlobalOperator(z2, z3, i, str6, str7);
            odpCustbyVirtualNetType = this.mHwCustGsmServiceStateManager.getVirtualNetOnsDisplayParams();
            odpCustForVideotron = this.mHwCustGsmServiceStateManager.getGsmOnsDisplayParamsForVideotron(z2, z3, i, str6, str7);
        }
        if (odpCust != null) {
            odp = odpCust;
        } else if (thisR != null) {
            odp = thisR;
        } else if (odpForChinaOperator != null) {
            odp = odpForChinaOperator;
        } else if (odpCustbyVirtualNetType != null) {
            odp = odpCustbyVirtualNetType;
        } else if (odpForGeneralOperator != null) {
            odp = odpForGeneralOperator;
        } else if (odpCustForVideotron != null) {
            odp = odpCustForVideotron;
        } else {
            OnsDisplayParams onsDisplayParams2 = new OnsDisplayParams(showSpn2, z, rule4, str3, str2);
        }
        if (this.mHwCustGsmServiceStateManager != null) {
            odp = this.mHwCustGsmServiceStateManager.setOnsDisplayCustomization(odp, getSS());
        }
        if (this.mGsmPhone.getImsPhone() != null && this.mGsmPhone.getImsPhone().isWifiCallingEnabled()) {
            odp = getOnsDisplayParamsForVoWifi(odp);
        }
        this.mCurShowWifi = odp.mShowWifi;
        this.mCurWifi = odp.mWifi;
        if (TextUtils.isEmpty(odp.mSpn) && 3 == odp.mRule) {
            Rlog.d(LOG_TAG, "Show plmn and spn while spn is null, show plmn only !");
            odp.mShowSpn = false;
            odp.mRule = 2;
        }
        plmn2 = null;
        if (odp.mShowPlmn) {
            plmn2 = odp.mPlmn;
        } else if (odp.mShowSpn) {
            plmn2 = odp.mSpn;
        }
        if (!(TextUtils.isEmpty(plmn2) || getSS() == null || ((getSS().getDataRegState() != 0 && getSS().getVoiceRegState() != 0) || this.mHwCustGsmServiceStateManager == null || this.mHwCustGsmServiceStateManager.skipPlmnUpdateFromCust()))) {
            spn2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("before setprop:");
            stringBuilder.append(getSS().getOperatorAlphaLong());
            Rlog.d(spn2, stringBuilder.toString());
            getSS().setOperatorName(plmn2, getSS().getOperatorAlphaShort(), getSS().getOperatorNumeric());
            spn2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("after setprop:");
            stringBuilder.append(getSS().getOperatorAlphaLong());
            Rlog.d(spn2, stringBuilder.toString());
        }
        return odp;
    }

    private OnsDisplayParams getGsmOnsDisplayParamsBySpnCust(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        boolean showSpnRslt;
        String plmnRslt = plmn;
        String spnRslt = spn;
        boolean showPlmnRslt = showPlmn;
        boolean showSpnRslt2 = showSpn;
        int ruleRslt = rule;
        boolean matched = true;
        String regPlmn = getSS().getOperatorNumeric();
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        if (r != null) {
            boolean matched2 = r.getOperatorNumeric();
            showSpnRslt = r.getServiceProviderName();
            if ("732130".equals(matched2) && !TextUtils.isEmpty(showSpnRslt) && ("732103".equals(regPlmn) || "732111".equals(regPlmn) || "732123".equals(regPlmn) || "732101".equals(regPlmn))) {
                showSpnRslt2 = true;
                showPlmnRslt = false;
                ruleRslt = 1;
                spnRslt = showSpnRslt;
            } else {
                matched = false;
            }
        } else {
            matched = false;
        }
        String spnRslt2 = spnRslt;
        boolean showPlmnRslt2 = showPlmnRslt;
        showSpnRslt = showSpnRslt2;
        int ruleRslt2 = ruleRslt;
        if (matched) {
            return new OnsDisplayParams(showSpnRslt, showPlmnRslt2, ruleRslt2, plmnRslt, spnRslt2);
        }
        return null;
    }

    private OnsDisplayParams getGsmOnsDisplayParamsBySpecialCust(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        boolean showPlmnRslt;
        OnsDisplayParams odpRslt;
        String str = plmn;
        String plmnRslt = str;
        String spnRslt = spn;
        boolean showPlmnRslt2 = showPlmn;
        boolean showSpnRslt = showSpn;
        int ruleRslt = rule;
        boolean matched = true;
        String regPlmn = getSS().getOperatorNumeric();
        int netType = getSS().getVoiceNetworkType();
        TelephonyManager.getDefault();
        boolean netClass = TelephonyManager.getNetworkClass(netType);
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        boolean showSpnRslt2;
        if (r != null) {
            String hplmn = r.getOperatorNumeric();
            String spnSim = r.getServiceProviderName();
            int ruleSim = r.getDisplayRule(getSS());
            showPlmnRslt = showPlmnRslt2;
            showPlmnRslt2 = LOG_TAG;
            showSpnRslt2 = showSpnRslt;
            StringBuilder stringBuilder = new StringBuilder();
            odpRslt = null;
            stringBuilder.append("regPlmn = ");
            stringBuilder.append(regPlmn);
            stringBuilder.append(",hplmn = ");
            stringBuilder.append(hplmn);
            stringBuilder.append(",spnSim = ");
            stringBuilder.append(spnSim);
            stringBuilder.append(",ruleSim = ");
            stringBuilder.append(ruleSim);
            stringBuilder.append(",netType = ");
            stringBuilder.append(netType);
            stringBuilder.append(",netClass = ");
            stringBuilder.append(netClass);
            Rlog.d(showPlmnRslt2, stringBuilder.toString());
            if ("21405".equals(hplmn) && "21407".equals(regPlmn) && "tuenti".equalsIgnoreCase(spnSim)) {
                showPlmnRslt2 = getEons(str);
                if (!TextUtils.isEmpty(spnSim)) {
                    spnRslt = spnSim;
                    showSpnRslt2 = (ruleSim & 1) == 1;
                }
                if (!TextUtils.isEmpty(showPlmnRslt2)) {
                    plmnRslt = showPlmnRslt2;
                    showPlmnRslt = (ruleSim & 2) == 2;
                }
            } else if ("21407".equals(hplmn) && "21407".equals(regPlmn)) {
                showPlmnRslt2 = getEons(str);
                if (!TextUtils.isEmpty(spnSim)) {
                    spnRslt = spnSim;
                    showSpnRslt2 = (ruleRslt & 1) == 1;
                }
                if (!TextUtils.isEmpty(showPlmnRslt2)) {
                    plmnRslt = showPlmnRslt2;
                    showPlmnRslt = (ruleRslt & 2) == 2;
                }
            } else if ("23420".equals(hplmn) && !getCombinedRegState(getSS())) {
                showPlmnRslt2 = getEons(str);
                if (!TextUtils.isEmpty(showPlmnRslt2)) {
                    spnRslt = spnSim;
                    plmnRslt = showPlmnRslt2;
                    ruleRslt = 2;
                    showSpnRslt2 = false;
                    showPlmnRslt = true;
                }
            } else if (("74000".equals(hplmn) && "74000".equals(regPlmn) && plmn.equals(spn)) || ("45006".equals(hplmn) && "45006".equals(regPlmn) && "LG U+".equals(str))) {
                showSpnRslt = false;
                ruleRslt = 2;
                showPlmnRslt = true;
            } else if ("732187".equals(hplmn) && ("732103".equals(regPlmn) || "732111".equals(regPlmn))) {
                if (true == netClass || true == netClass) {
                    plmnRslt = "ETB";
                } else if (true == netClass) {
                    plmnRslt = "ETB 4G";
                }
            } else if ("50218".equals(hplmn) && "50212".equals(regPlmn)) {
                if (true == netClass || true == netClass) {
                    showPlmnRslt2 = true;
                    showSpnRslt = false;
                    ruleRslt = 1;
                    plmnRslt = "U Mobile";
                    spnRslt = "U Mobile";
                } else if (true == netClass) {
                    showPlmnRslt2 = true;
                    showSpnRslt = false;
                    ruleRslt = 1;
                    plmnRslt = "MY MAXIS";
                    spnRslt = "MY MAXIS";
                }
                showPlmnRslt = showSpnRslt;
                showSpnRslt = showPlmnRslt2;
            } else if ("334050".equals(hplmn) || "334090".equals(hplmn) || "33405".equals(hplmn)) {
                if (TextUtils.isEmpty(spnSim) && (("334050".equals(regPlmn) || "334090".equals(regPlmn)) && !TextUtils.isEmpty(plmnRslt) && (plmnRslt.startsWith("Iusacell") || plmnRslt.startsWith("Nextel")))) {
                    Rlog.d(LOG_TAG, "AT&T a part of card has no opl/PNN and spn, then want it to be treated as AT&T");
                    plmnRslt = "AT&T";
                }
                if (!TextUtils.isEmpty(plmnRslt) && plmnRslt.startsWith("AT&T")) {
                    String plmnRslt2;
                    if (true == netClass) {
                        plmnRslt2 = "AT&T EDGE";
                    } else if (true == netClass) {
                        plmnRslt2 = "AT&T";
                    } else if (true == netClass) {
                        plmnRslt2 = "AT&T 4G";
                    }
                    plmnRslt = plmnRslt2;
                }
            } else if (ServiceStateTrackerUtils.isDocomoTablet()) {
                if (TextUtils.isEmpty(spnRslt)) {
                    spnRslt = spnSim;
                }
                boolean z = (getCombinedRegState(getSS()) || TextUtils.isEmpty(spnRslt) || (rule & 1) != 1) ? false : true;
                showSpnRslt = z;
                OnsDisplayParams odpRslt2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getGsmOnsDisplayParamsBySpecialCust: spnRslt = ");
                stringBuilder2.append(spnRslt);
                stringBuilder2.append(", showSpnRslt = ");
                stringBuilder2.append(showSpnRslt);
                Rlog.d(odpRslt2, stringBuilder2.toString());
            } else {
                matched = false;
            }
            showSpnRslt = showSpnRslt2;
        } else {
            showPlmnRslt = showPlmnRslt2;
            showSpnRslt2 = showSpnRslt;
            odpRslt = null;
            matched = false;
        }
        boolean showPlmnRslt3 = showPlmnRslt;
        str = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("matched = ");
        stringBuilder3.append(matched);
        stringBuilder3.append(",showPlmnRslt = ");
        stringBuilder3.append(showPlmnRslt3);
        stringBuilder3.append(",showSpnRslt = ");
        stringBuilder3.append(showSpnRslt);
        stringBuilder3.append(",ruleRslt = ");
        stringBuilder3.append(ruleRslt);
        stringBuilder3.append(",plmnRslt = ");
        stringBuilder3.append(plmnRslt);
        stringBuilder3.append(",spnRslt = ");
        stringBuilder3.append(spnRslt);
        Rlog.d(str, stringBuilder3.toString());
        if (matched) {
            return new OnsDisplayParams(showSpnRslt, showPlmnRslt3, ruleRslt, plmnRslt, spnRslt);
        }
        return odpRslt;
    }

    private OnsDisplayParams getGsmOnsDisplayParamsForChinaOperator(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        String str = plmn;
        String spn2 = spn;
        String hplmn = null;
        String regplmn = null;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            regplmn = getSS().getOperatorNumeric();
        }
        String hplmn2 = hplmn;
        String regplmn2 = regplmn;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showSpn:");
        boolean z = showSpn;
        stringBuilder.append(z);
        stringBuilder.append(",showPlmn:");
        boolean z2 = showPlmn;
        stringBuilder.append(z2);
        stringBuilder.append(",rule:");
        int i = rule;
        stringBuilder.append(i);
        stringBuilder.append(",plmn:");
        stringBuilder.append(str);
        stringBuilder.append(",spn:");
        stringBuilder.append(spn2);
        stringBuilder.append(",hplmn:");
        stringBuilder.append(hplmn2);
        stringBuilder.append(",regplmn:");
        stringBuilder.append(regplmn2);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
        if (!TextUtils.isEmpty(regplmn2)) {
            if (!isChinaMobileMccMnc()) {
                if (HwTelephonyFactory.getHwUiccManager().isCDMASimCard(this.mPhoneId)) {
                    if (!getSS().getRoaming()) {
                        Rlog.d("HwGsmServiceStateTracker", "In not roaming condition just show plmn without spn.");
                        return new OnsDisplayParams(false, true, i, str, spn2);
                    } else if (HwTelephonyManagerInner.getDefault().isCTSimCard(this.mPhoneId)) {
                        if (EMERGENCY_PLMN.equals(str) || NO_SERVICE_PLMN.equals(str)) {
                            Rlog.d("HwGsmServiceStateTracker", "out of service or emergency.");
                            return new OnsDisplayParams(z, z2, i, str, spn2);
                        }
                        if (TextUtils.isEmpty(spn)) {
                            Rlog.d("HwGsmServiceStateTracker", "spn is null.");
                            try {
                                spn2 = URLDecoder.decode(CHINA_TELECOM_SPN, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                Rlog.d("HwGsmServiceStateTracker", "UnsupportedEncodingException.");
                            }
                        }
                        return new OnsDisplayParams(true, true, i, str, spn2);
                    }
                }
                if (HuaweiTelephonyConfigs.isChinaTelecom() || (getSS().getRoaming() && !TextUtils.isEmpty(hplmn2) && "20404".equals(hplmn2) && "20404".equals(regplmn2) && 3 == (HwFullNetworkManager.getInstance().getSpecCardType(this.mGsmPhone.getPhoneId()) & 15))) {
                    Rlog.d("HwGsmServiceStateTracker", "In China Telecom, just show plmn without spn.");
                    return new OnsDisplayParams(false, true, i, str, spn2);
                }
            } else if (spn2 == null || "".equals(spn2) || "CMCC".equals(spn2) || "China Mobile".equals(spn2)) {
                Rlog.d("HwGsmServiceStateTracker", "chinamobile just show plmn without spn.");
                return new OnsDisplayParams(false, true, i, str, spn2);
            } else {
                Rlog.d("HwGsmServiceStateTracker", "third party provider sim cust just show original rule.");
                return new OnsDisplayParams(z, z2, i, str, spn2);
            }
        }
        return null;
    }

    private void log(String string) {
    }

    private boolean containsPlmn(String plmn, String[] plmnArray) {
        if (plmn == null || plmnArray == null) {
            return false;
        }
        for (String h : plmnArray) {
            if (plmn.equals(h)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNitzOperatorName(int slotId) {
        String result = SystemProperties.get("persist.radio.nitz_hw_name");
        String result1 = SystemProperties.get("persist.radio.nitz_hw_name1");
        boolean z = false;
        if (!isMultiSimEnabled) {
            if (result != null && result.length() > 0) {
                z = true;
            }
            return z;
        } else if (slotId == 0) {
            if (result != null && result.length() > 0) {
                z = true;
            }
            return z;
        } else if (1 == slotId) {
            if (result1 != null && result1.length() > 0) {
                z = true;
            }
            return z;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasNitzOperatorName invalid sub id");
            stringBuilder.append(slotId);
            Rlog.e("HwGsmServiceStateTracker", stringBuilder.toString());
            return false;
        }
    }

    private boolean getGsmRoamingCustByIMSIStart(boolean roaming) {
        String regplmnRoamCustomString = null;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            String regplmnMcc;
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumericEx(this.mCr, "hw_roam_hplmn_ex");
            String regplmn = getNewSS().getOperatorNumeric();
            int netType = getNewSS().getVoiceNetworkType();
            TelephonyManager.getDefault();
            int netClass = TelephonyManager.getNetworkClass(netType);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hplmn=");
            stringBuilder.append(hplmn);
            stringBuilder.append("  regplmn=");
            stringBuilder.append(regplmn);
            stringBuilder.append("  netType=");
            stringBuilder.append(netType);
            stringBuilder.append("  netClass=");
            stringBuilder.append(netClass);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
            try {
                regplmnRoamCustomString = System.getString(this.mCr, "reg_plmn_roam_custom");
            } catch (Exception e) {
                Rlog.e("HwGsmServiceStateTracker", "Exception when got reg_plmn_roam_custom value", e);
            }
            String str;
            if (regplmnRoamCustomString == null || hplmn == null || regplmn == null) {
                str = null;
                regplmnMcc = roaming;
            } else {
                String[] rules = regplmnRoamCustomString.split(";");
                int length = rules.length;
                String hplmnMcc = null;
                str = null;
                int hplmnMcc2 = 0;
                regplmnMcc = roaming;
                while (hplmnMcc2 < length) {
                    String[] rules2;
                    String regplmnRoamCustomString2;
                    String[] rule_plmn_roam = rules[hplmnMcc2].split(":");
                    if (rule_plmn_roam.length == 2) {
                        int rule_roam = Integer.parseInt(rule_plmn_roam[0]);
                        rules2 = rules;
                        rules = rule_plmn_roam[1].split(",");
                        regplmnRoamCustomString2 = regplmnRoamCustomString;
                        if (4 == rule_roam && 3 == rules.length && containsPlmn(hplmn, rules[0].split("\\|"))) {
                            return getGsmRoamingCustBySpecialRule(rules[0], rules[1], rules[2], regplmnMcc);
                        }
                        if (2 == rules.length) {
                            int i;
                            if (rules[0].equals(hplmn) && rules[1].equals(regplmn)) {
                                Rlog.d("HwGsmServiceStateTracker", "roaming customization by hplmn and regplmn success!");
                                if (1 == rule_roam) {
                                    return true;
                                }
                                i = 2;
                                if (2 == rule_roam) {
                                    return false;
                                }
                            }
                            i = 2;
                            if (3 == rule_roam && hplmn.length() > i && regplmn.length() > i) {
                                hplmnMcc = hplmn.substring(0, 3);
                                str = regplmn.substring(0, 3);
                                if (rules[0].equals(hplmnMcc) && rules[1].equals(str)) {
                                    regplmnMcc = false;
                                }
                            }
                        } else if (3 == rules.length) {
                            Rlog.d("HwGsmServiceStateTracker", "roaming customization by RAT");
                            if (rules[0].equals(hplmn) && rules[1].equals(regplmn) && rules[2].contains(String.valueOf(netClass + 1))) {
                                Rlog.d("HwGsmServiceStateTracker", "roaming customization by RAT success!");
                                if (1 == rule_roam) {
                                    return true;
                                }
                                if (2 == rule_roam) {
                                    return null;
                                }
                            }
                        } else {
                            continue;
                        }
                    } else {
                        rules2 = rules;
                        regplmnRoamCustomString2 = regplmnRoamCustomString;
                    }
                    hplmnMcc2++;
                    rules = rules2;
                    regplmnRoamCustomString = regplmnRoamCustomString2;
                }
            }
            return regplmnMcc;
        }
        Rlog.e("HwGsmServiceStateTracker", "mIccRecords null while getGsmRoamingCustByIMSIStart was called.");
        return roaming;
    }

    private boolean getGsmRoamingCustBySpecialRule(String hplmnlist, String regmcclist, String regplmnlist, boolean roaming) {
        boolean match_hplmn = false;
        boolean match_regmcc = false;
        boolean match_regplmn = false;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
            String regplmn = getNewSS().getOperatorNumeric();
            if (!(hplmn == null || regplmn == null || hplmnlist == null || regmcclist == null || regplmnlist == null)) {
                String[] hplmnString = hplmnlist.split("\\|");
                String[] regmccString = regmcclist.split("\\|");
                String[] regplmnString = regplmnlist.split("\\|");
                if (containsPlmn(hplmn, hplmnString)) {
                    match_hplmn = true;
                }
                if (containsPlmn(regplmn.substring(0, 3), regmccString)) {
                    match_regmcc = true;
                }
                if (containsPlmn(regplmn, regplmnString)) {
                    match_regplmn = true;
                }
                if (match_hplmn && match_regmcc && match_regplmn) {
                    Rlog.d(LOG_TAG, "match regmcc and regplmn, roaming");
                    return true;
                } else if (match_hplmn && match_regmcc) {
                    Rlog.d(LOG_TAG, "only match regmcc, no roaming");
                    return false;
                }
            }
        }
        return roaming;
    }

    private String getDefaultSpn(String spn, String hplmn, String regplmn) {
        if (TextUtils.isEmpty(hplmn) || TextUtils.isEmpty(regplmn)) {
            return spn;
        }
        String defaultSpnString = System.getString(this.mCr, "hw_spnnull_defaultspn");
        if (TextUtils.isEmpty(defaultSpnString) || !Pattern.matches(REGEX, defaultSpnString)) {
            return spn;
        }
        for (String defaultSpnItem : defaultSpnString.split(";")) {
            String[] defaultSpn = defaultSpnItem.split(",");
            if (hplmn.equals(defaultSpn[0]) && regplmn.equals(defaultSpn[1])) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("defaultspn is not null,use defaultspn instead ");
                stringBuilder.append(defaultSpn[2]);
                Rlog.d(str, stringBuilder.toString());
                return defaultSpn[2];
            }
        }
        return spn;
    }

    public void dispose() {
        if (this.mGsmPhone != null) {
            this.mCi.unregisterForCrrConn(this);
            this.mCi.unSetOnRegPLMNSelInfo(this);
            this.mGsmPhone.unregisterForMccChanged(this);
            this.mCi.unregisterForDSDSMode(this);
            this.mCi.unregisterForUnsol4RMimoStatus(this);
            this.mGsmPhone.getContext().unregisterReceiver(this.mIntentReceiver);
            this.mGsmPhone.getContext().unregisterReceiver(this.mCloudOtaBroadcastReceiver);
            if (PS_CLEARCODE || SHOW_REJ_INFO_KT) {
                this.mCi.unSetOnNetReject(this);
            }
        }
    }

    private void sendBroadcastCrrConnInd(int modem0, int modem1, int modem2) {
        Rlog.i(LOG_TAG, "GSM sendBroadcastCrrConnInd");
        String MODEM0 = "modem0";
        String MODEM1 = "modem1";
        String MODEM2 = "modem2";
        Intent intent = new Intent("com.huawei.action.ACTION_HW_CRR_CONN_IND");
        intent.putExtra("modem0", modem0);
        intent.putExtra("modem1", modem1);
        intent.putExtra("modem2", modem2);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("modem0: ");
        stringBuilder.append(modem0);
        stringBuilder.append(" modem1: ");
        stringBuilder.append(modem1);
        stringBuilder.append(" modem2: ");
        stringBuilder.append(modem2);
        Rlog.i(str, stringBuilder.toString());
        this.mGsmPhone.getContext().sendBroadcast(intent, "com.huawei.permission.CRRCONN_PERMISSION");
    }

    private void sendBroadcastRegPLMNSelInfo(int flag, int result) {
        String SUB_ID = HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID;
        String FLAG = "flag";
        String RES = "res";
        Intent intent = new Intent("com.huawei.action.SIM_PLMN_SELINFO");
        int subId = this.mGsmPhone.getPhoneId();
        intent.putExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, subId);
        intent.putExtra("flag", flag);
        intent.putExtra("res", result);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(" flag: ");
        stringBuilder.append(flag);
        stringBuilder.append(" result: ");
        stringBuilder.append(result);
        Rlog.i(str, stringBuilder.toString());
        this.mGsmPhone.getContext().sendBroadcast(intent, "com.huawei.permission.HUAWEI_BUSSINESS_PERMISSION");
    }

    private void sendBroadcastDsdsMode(int dsdsMode) {
        String DSDSMODE = "dsdsmode";
        Intent intent = new Intent(InCallDataStateMachine.ACTION_HW_DSDS_MODE_STATE);
        intent.putExtra("dsdsmode", dsdsMode);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GSM dsdsMode: ");
        stringBuilder.append(dsdsMode);
        Rlog.i(str, stringBuilder.toString());
        this.mGsmPhone.getContext().sendBroadcast(intent, "com.huawei.permission.DSDSMODE_PERMISSION");
    }

    private void addBroadCastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.refreshapn");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        if (this.mHwCustGsmServiceStateManager != null) {
            filter = this.mHwCustGsmServiceStateManager.getCustIntentFilter(filter);
        }
        this.mGsmPhone.getContext().registerReceiver(this.mIntentReceiver, filter);
    }

    public boolean needUpdateNITZTime() {
        int otherCardType;
        int otherCardState;
        int otherPhoneType;
        int phoneId = this.mGsmPhone.getPhoneId();
        int ownCardType = HwTelephonyManagerInner.getDefault().getCardType(phoneId);
        int ownPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(phoneId);
        if (phoneId == 0) {
            otherCardType = HwTelephonyManagerInner.getDefault().getCardType(1);
            otherCardState = HwTelephonyManagerInner.getDefault().getSubState(1);
            otherPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(1);
        } else if (phoneId == 1) {
            otherCardType = HwTelephonyManagerInner.getDefault().getCardType(0);
            otherCardState = HwTelephonyManagerInner.getDefault().getSubState(0);
            otherPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(0);
        } else {
            otherCardType = -1;
            otherCardState = 0;
            otherPhoneType = 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ownCardType = ");
        stringBuilder.append(ownCardType);
        stringBuilder.append(", otherCardType = ");
        stringBuilder.append(otherCardType);
        stringBuilder.append(", otherCardState = ");
        stringBuilder.append(otherCardState);
        stringBuilder.append(" ownPhoneType = ");
        stringBuilder.append(ownPhoneType);
        stringBuilder.append(", otherPhoneType = ");
        stringBuilder.append(otherPhoneType);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
        StringBuilder stringBuilder2;
        if ((ownCardType == 41 || ownCardType == 43) && ownPhoneType == 2) {
            Rlog.d("HwGsmServiceStateTracker", "Cdma card, uppdate NITZ time!");
            return true;
        } else if ((otherCardType == 30 || otherCardType == 43 || otherCardType == 41) && 1 == otherCardState && otherPhoneType == 2) {
            HwReportManagerImpl.getDefault().reportNitzIgnore(phoneId, "CG_IGNORE");
            Rlog.d("HwGsmServiceStateTracker", "Other cdma card, ignore updating NITZ time!");
            return false;
        } else if (HwVSimUtils.isVSimOn() && HwVSimUtils.isVSimSub(phoneId)) {
            Rlog.d("HwGsmServiceStateTracker", "vsim phone, update NITZ time!");
            return true;
        } else if (phoneId == SystemProperties.getInt("gsm.sim.updatenitz", phoneId) || -1 == SystemProperties.getInt("gsm.sim.updatenitz", -1) || otherCardState == 0) {
            SystemProperties.set("gsm.sim.updatenitz", String.valueOf(phoneId));
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Update NITZ time, set update card : ");
            stringBuilder2.append(phoneId);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder2.toString());
            return true;
        } else {
            HwReportManagerImpl.getDefault().reportNitzIgnore(phoneId, "GG_IGNORE");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Ignore updating NITZ time, phoneid : ");
            stringBuilder2.append(phoneId);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder2.toString());
            return false;
        }
    }

    public void processCTNumMatch(boolean roaming, UiccCardApplication uiccCardApplication) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processCTNumMatch, roaming: ");
        stringBuilder.append(roaming);
        Rlog.d("HwGsmServiceStateTracker", stringBuilder.toString());
        if (IS_CHINATELECOM && uiccCardApplication != null && AppState.APPSTATE_READY == uiccCardApplication.getState()) {
            int slotId = HwAddonTelephonyFactory.getTelephony().getDefault4GSlotId();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processCTNumMatch->getDefault4GSlotId, slotId: ");
            stringBuilder2.append(slotId);
            Rlog.d("HwGsmServiceStateTracker", stringBuilder2.toString());
            if (HwTelephonyManagerInner.getDefault().isCTCdmaCardInGsmMode() && uiccCardApplicationUtils.getUiccCard(uiccCardApplication) == UiccController.getInstance().getUiccCard(slotId)) {
                Rlog.d("HwGsmServiceStateTracker", "processCTNumMatch, isCTCdmaCardInGsmMode..");
                setCTNumMatchRoamingForSlot(slotId);
            }
        }
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        boolean z = true;
        AsyncResult ar;
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        String mcc;
        if (i == 1) {
            ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_ICC_RECORDS_EONS_UPDATED exception ");
                stringBuilder.append(ar.exception);
                Rlog.e(str, stringBuilder.toString());
                return;
            }
            processIccEonsRecordsUpdated(((Integer) ar.result).intValue());
        } else if (i != 64) {
            String str2;
            switch (i) {
                case EVENT_RPLMNS_STATE_CHANGED /*101*/:
                    setNetworkSelectionModeAutomaticHw(this.oldRplmn, this.rplmn);
                    str2 = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[SLOT");
                    stringBuilder2.append(this.mPhoneId);
                    stringBuilder2.append("]EVENT_RPLMNS_STATE_CHANGED");
                    Rlog.d(str2, stringBuilder2.toString());
                    this.oldRplmn = this.rplmn;
                    ar = (AsyncResult) msg.obj;
                    if (ar == null || ar.result == null || !(ar.result instanceof String)) {
                        this.rplmn = SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, "");
                    } else {
                        this.rplmn = (String) ar.result;
                    }
                    mcc = "";
                    String oldMcc = "";
                    if (this.rplmn != null && this.rplmn.length() > 3) {
                        mcc = this.rplmn.substring(0, 3);
                    }
                    if (this.oldRplmn != null && this.oldRplmn.length() > 3) {
                        oldMcc = this.oldRplmn.substring(0, 3);
                    }
                    if (!("".equals(mcc) || mcc.equals(oldMcc))) {
                        this.mGsmPhone.notifyMccChanged(mcc);
                        Rlog.d(LOG_TAG, "rplmn mcc changed.");
                    }
                    String str3 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("rplmn");
                    stringBuilder3.append(this.rplmn);
                    Rlog.d(str3, stringBuilder3.toString());
                    if (SystemProperties.getBoolean("ro.config.hw_enable_ota_bip_lgu", false) && this.mGsmPhone.mDcTracker != null) {
                        this.mGsmPhone.mDcTracker.checkPLMN(this.rplmn);
                    }
                    if (SystemProperties.getBoolean("ro.config.hw_globalEcc", true)) {
                        Rlog.d(LOG_TAG, "the global emergency numbers custom-make does enable!!!!");
                        toGetRplmnsThenSendEccNum();
                        return;
                    }
                    return;
                case EVENT_DELAY_UPDATE_REGISTER_STATE_DONE /*102*/:
                    str2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[Phone");
                    stringBuilder.append(this.mPhoneId);
                    stringBuilder.append("]Delay Timer expired, begin get register state");
                    Rlog.d(str2, stringBuilder.toString());
                    this.mRefreshState = true;
                    this.mGsst.pollState();
                    return;
                case EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH /*103*/:
                    Rlog.d(LOG_TAG, "event update gsm signal strength");
                    this.mDoubleSignalStrength.proccessAlaphFilter(this.mGsst.getSignalStrength(), this.mModemSignalStrength);
                    this.mGsmPhone.notifySignalStrength();
                    return;
                case EVENT_CA_STATE_CHANGED /*104*/:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        if (((int[]) ar.result)[0] != 1) {
                            z = false;
                        }
                        onCaStateChanged(z);
                        return;
                    }
                    log("EVENT_CA_STATE_CHANGED: exception;");
                    return;
                case EVENT_NETWORK_REJECTED_CASE /*105*/:
                    Rlog.d(LOG_TAG, "EVENT_NETWORK_REJECTED_CASE");
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EVENT_NETWORK_REJECTED_CASE exception ");
                        stringBuilder.append(ar.exception);
                        Rlog.e(str, stringBuilder.toString());
                        return;
                    }
                    onNetworkReject(ar);
                    return;
                case EVENT_4R_MIMO_ENABLE /*106*/:
                    if (MIMO_4R_REPORT) {
                        Rlog.d(LOG_TAG, "EVENT_4R_MIMO_ENABLE");
                        ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("EVENT_MIMO_ENABLE exception ");
                            stringBuilder.append(ar.exception);
                            Rlog.e(str, stringBuilder.toString());
                            return;
                        }
                        on4RMimoChange(ar);
                        return;
                    }
                    return;
                default:
                    switch (i) {
                        case EVENT_PLMN_SELINFO /*151*/:
                            Rlog.d(LOG_TAG, "EVENT_PLMN_SELINFO");
                            ar = (AsyncResult) msg.obj;
                            if (ar.exception == null) {
                                int[] response = ar.result;
                                if (response.length != 0) {
                                    sendBroadcastRegPLMNSelInfo(response[0], response[1]);
                                    return;
                                }
                                return;
                            }
                            return;
                        case EVENT_CRR_CONN /*152*/:
                            Rlog.d(LOG_TAG, "GSM EVENT_CRR_CONN");
                            ar = (AsyncResult) msg.obj;
                            if (ar.exception == null) {
                                int[] response2 = ar.result;
                                if (response2.length > 2) {
                                    sendBroadcastCrrConnInd(response2[0], response2[1], response2[2]);
                                    return;
                                }
                                return;
                            }
                            Rlog.d(LOG_TAG, "GSM EVENT_CRR_CONN: exception;");
                            return;
                        case EVENT_DSDS_MODE /*153*/:
                            Rlog.d(LOG_TAG, "GSM EVENT_DSDS_MODE");
                            ar = msg.obj;
                            if (ar.exception == null) {
                                int[] response3 = ar.result;
                                if (response3.length != 0) {
                                    sendBroadcastDsdsMode(response3[0]);
                                    return;
                                }
                                return;
                            }
                            Rlog.d(LOG_TAG, "GSM EVENT_DSDS_MODE: exception;");
                            return;
                        case EVENT_MCC_CHANGED /*154*/:
                            Rlog.d(LOG_TAG, "EVENT_MCC_CHANGED");
                            SystemProperties.set("gsm.sim.updatenitz", String.valueOf(-1));
                            return;
                        default:
                            super.handleMessage(msg);
                            return;
                    }
            }
        } else {
            ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_POLL_LOCATION_INFO exception ");
                stringBuilder.append(ar.exception);
                Rlog.e(str, stringBuilder.toString());
                return;
            }
            String[] states = ar.result;
            Rlog.i(LOG_TAG, "CLEARCODE EVENT_POLL_LOCATION_INFO");
            if (states.length == 4) {
                try {
                    if (states[2] != null && states[2].length() > 0) {
                        this.mRac = Integer.parseInt(states[2], 16);
                        String str4 = LOG_TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("CLEARCODE mRac = ");
                        stringBuilder2.append(this.mRac);
                        Rlog.d(str4, stringBuilder2.toString());
                    }
                } catch (NumberFormatException ex) {
                    mcc = LOG_TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("error parsing LocationInfoState: ");
                    stringBuilder4.append(ex);
                    Rlog.e(mcc, stringBuilder4.toString());
                }
            }
        }
    }

    private void setNetworkSelectionModeAutomaticHw(String oldRplmn, String rplmn) {
        String autoSelectMccs = System.getString(this.mContext.getContentResolver(), "hw_auto_select_network_mcc");
        if (!TextUtils.isEmpty(autoSelectMccs) && !TextUtils.isEmpty(oldRplmn) && !TextUtils.isEmpty(rplmn) && oldRplmn.length() >= 3 && rplmn.length() >= 3) {
            if (this.mGsmPhone == null || !this.mGsmPhone.getServiceState().getIsManualSelection()) {
                Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomaticHw - already auto, ignoring.");
                return;
            }
            String[] mccs = autoSelectMccs.split(",");
            int i = 0;
            String oldMcc = oldRplmn.substring(0, 3);
            String newMcc = rplmn.substring(0, 3);
            boolean isNeedSelectAuto = false;
            while (i < mccs.length) {
                if (!oldMcc.equals(mccs[i]) && newMcc.equals(mccs[i])) {
                    isNeedSelectAuto = true;
                    break;
                }
                i++;
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetworkSelectionModeAutomaticHw isNeedSelectAuto:");
            stringBuilder.append(isNeedSelectAuto);
            Rlog.d(str, stringBuilder.toString());
            if (isNeedSelectAuto) {
                this.mGsmPhone.setNetworkSelectionModeAutomatic(null);
            }
        }
    }

    private void onNetworkReject(AsyncResult ar) {
        if (ar.exception == null) {
            String[] data = ar.result;
            String rejectplmn = null;
            int rejectdomain = -1;
            int rejectcause = -1;
            int rejectrat = -1;
            int orignalrejectcause = -1;
            if (data.length > 0) {
                try {
                    if (data[0] != null && data[0].length() > 0) {
                        rejectplmn = data[0];
                    }
                    if (data.length > 1 && data[1] != null && data[1].length() > 0) {
                        rejectdomain = Integer.parseInt(data[1]);
                    }
                    if (data.length > 2 && data[2] != null && data[2].length() > 0) {
                        rejectcause = Integer.parseInt(data[2]);
                    }
                    if (data.length > 3 && data[3] != null && data[3].length() > 0) {
                        rejectrat = Integer.parseInt(data[3]);
                    }
                    if (IS_VERIZON && data.length > 4 && data[4] != null && data[4].length() > 0) {
                        orignalrejectcause = Integer.parseInt(data[4]);
                    }
                } catch (Exception ex) {
                    Rlog.e(LOG_TAG, "error parsing NetworkReject!", ex);
                }
                if (SHOW_REJ_INFO_KT && this.mGsst.returnObject() != null) {
                    this.mGsst.returnObject().handleNetworkRejectionEx(rejectcause, rejectrat);
                }
                if (PS_CLEARCODE) {
                    if (2 == rejectrat) {
                        this.rejNum++;
                    }
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("NetworkReject:PLMN = ");
                    stringBuilder.append(rejectplmn);
                    stringBuilder.append(" domain = ");
                    stringBuilder.append(rejectdomain);
                    stringBuilder.append(" cause = ");
                    stringBuilder.append(rejectcause);
                    stringBuilder.append(" RAT = ");
                    stringBuilder.append(rejectrat);
                    stringBuilder.append(" rejNum = ");
                    stringBuilder.append(this.rejNum);
                    Rlog.d(str, stringBuilder.toString());
                    if (this.rejNum >= 3) {
                        this.mGsmPhone.setPreferredNetworkType(3, null);
                        Global.putInt(this.mGsmPhone.getContext().getContentResolver(), "preferred_network_mode", 3);
                        this.rejNum = 0;
                        this.mRac = -1;
                    }
                }
                if (IS_VERIZON && this.mGsst.returnObject() != null) {
                    this.mGsst.returnObject().handleLteEmmCause(this.mGsmPhone.getPhoneId(), rejectrat, orignalrejectcause);
                }
            }
        }
    }

    public boolean isNetworkTypeChanged(SignalStrength oldSS, SignalStrength newSS) {
        int newState = 0;
        int oldState = 0;
        if (newSS.getGsmSignalStrength() < -1) {
            newState = 0 | 1;
        }
        if (oldSS.getGsmSignalStrength() < -1) {
            oldState = 0 | 1;
        }
        if (newSS.getWcdmaRscp() < -1) {
            newState |= 2;
        }
        if (oldSS.getWcdmaRscp() < -1) {
            oldState |= 2;
        }
        if (newSS.getLteRsrp() < -1) {
            newState |= 4;
        }
        if (oldSS.getLteRsrp() < -1) {
            oldState |= 4;
        }
        if (newState == 0 || newState == oldState) {
            return false;
        }
        return true;
    }

    public void sendMessageDelayUpdateSingalStrength(int time) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SUB[");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]sendMessageDelayUpdateSingalStrength, time: ");
        stringBuilder.append(time);
        Rlog.d(str, stringBuilder.toString());
        Message msg = obtainMessage();
        msg.what = EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH;
        if (time == 0) {
            sendMessageDelayed(msg, (long) mDelayDuringTime);
        } else {
            sendMessageDelayed(msg, (long) time);
        }
    }

    public boolean notifySignalStrength(SignalStrength oldSS, SignalStrength newSS) {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        boolean notified;
        this.mModemSignalStrength = new SignalStrength(newSS);
        if (hasMessages(EVENT_DELAY_UPDATE_REGISTER_STATE_DONE)) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append("] no notify signal");
            Rlog.d(str, stringBuilder.toString());
        }
        if (isNetworkTypeChanged(oldSS, newSS)) {
            str = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SUB[");
            stringBuilder2.append(this.mPhoneId);
            stringBuilder2.append("]Network is changed immediately!");
            Rlog.d(str, stringBuilder2.toString());
            if (hasMessages(EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH)) {
                removeMessages(EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH);
            }
            this.mDoubleSignalStrength = new DoubleSignalStrength(newSS);
            this.mGsst.setSignalStrength(newSS);
            notified = true;
        } else if (hasMessages(EVENT_DELAY_UPDATE_GSM_SIGNAL_STRENGTH)) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SUB[");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append("]has delay update msg");
            Rlog.d(str, stringBuilder.toString());
            notified = false;
        } else {
            this.mOldDoubleSignalStrength = this.mDoubleSignalStrength;
            this.mDoubleSignalStrength = new DoubleSignalStrength(newSS);
            this.mDoubleSignalStrength.proccessAlaphFilter(this.mOldDoubleSignalStrength, newSS, this.mModemSignalStrength, false);
            notified = true;
        }
        if (notified) {
            try {
                this.mGsmPhone.notifySignalStrength();
            } catch (NullPointerException ex) {
                String str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onSignalStrengthResult() Phone already destroyed: ");
                stringBuilder2.append(ex);
                stringBuilder2.append("SignalStrength not notified");
                Rlog.e(str2, stringBuilder2.toString());
            }
        }
        return notified;
    }

    public int getCARilRadioType(int type) {
        int radioType = type;
        if (SHOW_4G_PLUS_ICON && 19 == type) {
            radioType = 14;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[CA]radioType=");
        stringBuilder.append(radioType);
        stringBuilder.append(" type=");
        stringBuilder.append(type);
        Rlog.d(str, stringBuilder.toString());
        return radioType;
    }

    public int updateCAStatus(int currentType) {
        int newType = currentType;
        if (SHOW_4G_PLUS_ICON) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[CA] currentType=");
            stringBuilder.append(currentType);
            stringBuilder.append(" oldCAstate=");
            stringBuilder.append(this.mOldCAstate);
            Rlog.d(str, stringBuilder.toString());
            boolean HasCAactivated = 19 == currentType && 19 != this.mOldCAstate;
            boolean HasCAdeActivated = 19 != currentType && 19 == this.mOldCAstate;
            this.mOldCAstate = currentType;
            if (HasCAactivated || HasCAdeActivated) {
                Intent intentLteCAState = new Intent("com.huawei.intent.action.LTE_CA_STATE");
                intentLteCAState.putExtra("subscription", this.mGsmPhone.getSubId());
                if (HasCAactivated) {
                    intentLteCAState.putExtra("LteCAstate", true);
                    Rlog.d(LOG_TAG, "[CA] CA activated !");
                } else if (HasCAdeActivated) {
                    intentLteCAState.putExtra("LteCAstate", false);
                    Rlog.d(LOG_TAG, "[CA] CA deactivated !");
                }
                this.mGsmPhone.getContext().sendBroadcast(intentLteCAState);
            }
        }
        return newType;
    }

    private void onCaStateChanged(boolean caActive) {
        boolean oldCaActive = 19 == this.mOldCAstate;
        if (SHOW_4G_PLUS_ICON && oldCaActive != caActive) {
            if (caActive) {
                this.mOldCAstate = 19;
                Rlog.d(LOG_TAG, "[CA] CA activated !");
            } else {
                this.mOldCAstate = 0;
                Rlog.d(LOG_TAG, "[CA] CA deactivated !");
            }
            Intent intentLteCAState = new Intent("com.huawei.intent.action.LTE_CA_STATE");
            intentLteCAState.putExtra("subscription", this.mGsmPhone.getSubId());
            intentLteCAState.putExtra("LteCAstate", caActive);
            this.mGsmPhone.getContext().sendBroadcast(intentLteCAState);
        }
    }

    private void processIccEonsRecordsUpdated(int eventCode) {
        if (eventCode == 2) {
            this.mGsst.updateSpnDisplay();
        } else if (eventCode == 100) {
            this.mGsst.updateSpnDisplay();
        }
    }

    public void unregisterForRecordsEvents(IccRecords r) {
        if (r != null) {
            r.unregisterForRecordsEvents(this);
        }
    }

    public void registerForRecordsEvents(IccRecords r) {
        if (r != null) {
            r.registerForRecordsEvents(this, 1, null);
        }
    }

    public String getEons(String defaultValue) {
        if (HwModemCapability.isCapabilitySupport(5)) {
            return defaultValue;
        }
        String hplmn;
        String result = null;
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        if (r != null && !r.isEonsDisabled()) {
            Rlog.d(LOG_TAG, "getEons():get plmn from SIM card! ");
            if (updateEons(r)) {
                result = r.getEons();
            }
        } else if (r != null && r.isEonsDisabled()) {
            hplmn = r.getOperatorNumeric();
            String regplmn = getSS().getOperatorNumeric();
            if (!(hplmn == null || !hplmn.equals(regplmn) || r.getEons() == null)) {
                Rlog.d(LOG_TAG, "getEons():get plmn from Cphs when register to hplmn ");
                result = r.getEons();
            }
        }
        hplmn = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("result = ");
        stringBuilder.append(result);
        Rlog.d(hplmn, stringBuilder.toString());
        if (TextUtils.isEmpty(result)) {
            result = defaultValue;
        }
        return result;
    }

    public boolean updateEons(IccRecords r) {
        int lac = -1;
        if (this.mGsst.mCellLoc != null) {
            lac = ((GsmCellLocation) this.mGsst.mCellLoc).getLac();
        }
        if (r != null) {
            return r.updateEons(getSS().getOperatorNumeric(), lac);
        }
        return false;
    }

    private void cancelDeregisterStateDelayTimer() {
        if (hasMessages(EVENT_DELAY_UPDATE_REGISTER_STATE_DONE)) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[SUB");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append("]cancelDeregisterStateDelayTimer");
            Rlog.d(str, stringBuilder.toString());
            removeMessages(EVENT_DELAY_UPDATE_REGISTER_STATE_DONE);
        }
    }

    private void delaySendDeregisterStateChange(int delayedTime) {
        if (!hasMessages(EVENT_DELAY_UPDATE_REGISTER_STATE_DONE)) {
            Message msg = obtainMessage();
            msg.what = EVENT_DELAY_UPDATE_REGISTER_STATE_DONE;
            sendMessageDelayed(msg, (long) delayedTime);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[SUB");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append("]RegisterStateChange timer is running,do nothing");
            Rlog.d(str, stringBuilder.toString());
        }
    }

    public boolean proccessGsmDelayUpdateRegisterStateDone(ServiceState oldSS, ServiceState newSS) {
        if (HwModemCapability.isCapabilitySupport(6)) {
            return false;
        }
        boolean lostNework = ((oldSS.getVoiceRegState() != 0 && oldSS.getDataRegState() != 0) || newSS.getVoiceRegState() == 0 || newSS.getDataRegState() == 0) ? false : true;
        boolean isSubDeactivated = SubscriptionController.getInstance().getSubState(this.mGsmPhone.getSubId()) == 0;
        int newMainSlot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        State mExternalState = State.UNKNOWN;
        IccCard iccCard = this.mGsmPhone.getIccCard();
        if (iccCard != null) {
            mExternalState = iccCard.getState();
        }
        PhoneConstants.State callState = PhoneFactory.getPhone(this.mGsmPhone.getPhoneId()).getState();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]lostNework : ");
        stringBuilder.append(lostNework);
        stringBuilder.append(", desiredPowerState : ");
        stringBuilder.append(this.mGsst.getDesiredPowerState());
        stringBuilder.append(", radiostate : ");
        stringBuilder.append(this.mCi.getRadioState());
        stringBuilder.append(", mRadioOffByDoRecovery : ");
        stringBuilder.append(this.mGsst.getDoRecoveryTriggerState());
        stringBuilder.append(", isSubDeactivated : ");
        stringBuilder.append(isSubDeactivated);
        stringBuilder.append(", newMainSlot : ");
        stringBuilder.append(newMainSlot);
        stringBuilder.append(", phoneOOS : ");
        stringBuilder.append(this.mGsmPhone.getOOSFlag());
        stringBuilder.append(", isUserPref4GSlot : ");
        stringBuilder.append(HwFullNetworkManager.getInstance().isUserPref4GSlot(this.mMainSlot));
        stringBuilder.append(", mExternalState : ");
        stringBuilder.append(mExternalState);
        stringBuilder.append(", callState : ");
        stringBuilder.append(callState);
        Rlog.d(str, stringBuilder.toString());
        if (newSS.getDataRegState() == 0 || newSS.getVoiceRegState() == 0 || !this.mGsst.getDesiredPowerState() || this.mCi.getRadioState() == RadioState.RADIO_OFF || this.mGsst.getDoRecoveryTriggerState() || isCardInvalid(isSubDeactivated, this.mGsmPhone.getSubId()) || !HwFullNetworkManager.getInstance().isUserPref4GSlot(this.mMainSlot) || this.mGsmPhone.getOOSFlag() || newSS.getDataRegState() == 3 || mExternalState == State.PUK_REQUIRED || callState != PhoneConstants.State.IDLE) {
            this.mMainSlot = newMainSlot;
            cancelDeregisterStateDelayTimer();
        } else if (hasMessages(EVENT_DELAY_UPDATE_REGISTER_STATE_DONE)) {
            return true;
        } else {
            if (lostNework && !this.mRefreshState) {
                int delayedTime = getSendDeregisterStateDelayedTime(oldSS, newSS);
                if (delayedTime > 0) {
                    delaySendDeregisterStateChange(delayedTime);
                    newSS.setStateOutOfService();
                    return true;
                }
            }
        }
        this.mRefreshState = false;
        return false;
    }

    private int getSendDeregisterStateDelayedTime(ServiceState oldSS, ServiceState newSS) {
        int slotId;
        boolean isPsLostNetwork = false;
        boolean isCsLostNetwork = (oldSS.getVoiceRegState() != 0 || newSS.getVoiceRegState() == 0 || newSS.getDataRegState() == 0) ? false : true;
        if (!(oldSS.getDataRegState() != 0 || newSS.getVoiceRegState() == 0 || newSS.getDataRegState() == 0)) {
            isPsLostNetwork = true;
        }
        try {
            slotId = SubscriptionManager.getSlotIndex(this.mGsmPhone.getPhoneId());
            Integer default_time = (Integer) HwCfgFilePolicy.getValue("lostnetwork.default_timer", slotId, Integer.class);
            Integer delaytimer_cs2G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_cs2G", slotId, Integer.class);
            Integer delaytimer_cs3G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_cs3G", slotId, Integer.class);
            Integer delaytimer_cs4G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_cs4G", slotId, Integer.class);
            Integer delaytimer_ps2G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_ps2G", slotId, Integer.class);
            Integer delaytimer_ps3G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_ps3G", slotId, Integer.class);
            Integer delaytimer_ps4G = (Integer) HwCfgFilePolicy.getValue("lostnetwork.delaytimer_ps4G", slotId, Integer.class);
            if (default_time != null) {
                this.DELAYED_TIME_DEFAULT_VALUE = default_time.intValue();
            }
            if (delaytimer_cs2G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_CS_2G = delaytimer_cs2G.intValue() * 1000;
            }
            if (delaytimer_cs3G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_CS_3G = delaytimer_cs3G.intValue() * 1000;
            }
            if (delaytimer_cs4G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_CS_4G = delaytimer_cs4G.intValue() * 1000;
            }
            if (delaytimer_ps2G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_PS_2G = delaytimer_ps2G.intValue() * 1000;
            }
            if (delaytimer_ps3G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_PS_3G = delaytimer_ps3G.intValue() * 1000;
            }
            if (delaytimer_ps4G != null) {
                this.DELAYED_TIME_NETWORKSTATUS_PS_4G = delaytimer_ps4G.intValue() * 1000;
            }
        } catch (Exception ex) {
            Rlog.e(LOG_TAG, "lostnetwork error!", ex);
        }
        int delayedTime = 0;
        TelephonyManager.getDefault();
        slotId = TelephonyManager.getNetworkClass(getNetworkType(oldSS));
        if (isCsLostNetwork) {
            if (slotId == 1) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_CS_2G;
            } else if (slotId == 2) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_CS_3G;
            } else if (slotId == 3) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_CS_4G;
            } else {
                delayedTime = this.DELAYED_TIME_DEFAULT_VALUE * 1000;
            }
        } else if (isPsLostNetwork) {
            if (slotId == 1) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_PS_2G;
            } else if (slotId == 2) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_PS_3G;
            } else if (slotId == 3) {
                delayedTime = this.DELAYED_TIME_NETWORKSTATUS_PS_4G;
            } else {
                delayedTime = this.DELAYED_TIME_DEFAULT_VALUE * 1000;
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("] delay time = ");
        stringBuilder.append(delayedTime);
        Rlog.d(str, stringBuilder.toString());
        return delayedTime;
    }

    public boolean isUpdateLacAndCid(int cid) {
        if (!HwModemCapability.isCapabilitySupport(12) || cid != 0) {
            return true;
        }
        Rlog.d(LOG_TAG, "do not set the Lac and Cid when cid is 0");
        return false;
    }

    public void toGetRplmnsThenSendEccNum() {
        String rplmns = "";
        String hplmn = TelephonyManager.getDefault().getSimOperator(this.mPhoneId);
        String forceEccState = SystemProperties.get(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "invalid");
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]GECC-toGetRplmnsThenSendEccNum: hplmn = ");
        stringBuilder.append(hplmn);
        stringBuilder.append("; forceEccState = ");
        stringBuilder.append(forceEccState);
        Rlog.d(str, stringBuilder.toString());
        UiccProfile profile = UiccController.getInstance().getUiccProfileForPhone(PhoneFactory.getDefaultPhone().getPhoneId());
        if ((profile != null && profile.getIccCardStateHW()) || hplmn.equals("") || forceEccState.equals("usim_absent")) {
            rplmns = SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, "");
            if (!rplmns.equals("")) {
                this.mGsmPhone.getContext().sendBroadcast(new Intent("com.android.net.wifi.countryCode"));
                this.mGsmPhone.globalEccCustom(rplmns);
            }
        }
    }

    public void sendGsmRoamingIntentIfDenied(int regState, int rejectCode) {
        this.mGsst.returnObject();
        if ((regState == 3 || getNewSS().isEmergencyOnly()) && rejectCode == 10) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Posting Managed roaming intent sub = ");
            stringBuilder.append(this.mGsmPhone.getSubId());
            Rlog.d(str, stringBuilder.toString());
            Intent intent = new Intent("codeaurora.intent.action.ACTION_MANAGED_ROAMING_IND");
            intent.putExtra("subscription", this.mGsmPhone.getSubId());
            this.mGsmPhone.getContext().sendBroadcast(intent);
        }
    }

    private OnsDisplayParams getOnsDisplayParamsBySpnOnly(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        String plmnRes = plmn;
        String spnRes = spn;
        int mRule = rule;
        boolean mShowSpn = showSpn;
        boolean mShowPlmn = showPlmn;
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        if (r == null) {
            return new OnsDisplayParams(mShowSpn, mShowPlmn, mRule, plmnRes, spnRes);
        }
        String hPlmn = r.getOperatorNumeric();
        String regPlmn = getSS().getOperatorNumeric();
        String spnSim = r.getServiceProviderName();
        int ruleSim = r.getDisplayRule(getSS());
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SpnOnly spn:");
        stringBuilder.append(spnSim);
        stringBuilder.append(",hplmn:");
        stringBuilder.append(hPlmn);
        stringBuilder.append(",regPlmn:");
        stringBuilder.append(regPlmn);
        Rlog.d(str, stringBuilder.toString());
        if (!TextUtils.isEmpty(spnSim) && isMccForSpn(r.getOperatorNumeric()) && !TextUtils.isEmpty(hPlmn) && hPlmn.length() > 3) {
            String currentMcc = hPlmn.substring(0, 3);
            if (!TextUtils.isEmpty(regPlmn) && regPlmn.length() > 3 && currentMcc.equals(regPlmn.substring(0, 3))) {
                mShowSpn = true;
                mShowPlmn = false;
                spnRes = spnSim;
                mRule = ruleSim;
                plmnRes = "";
            }
        }
        return new OnsDisplayParams(mShowSpn, mShowPlmn, mRule, plmnRes, spnRes);
    }

    private boolean isMccForSpn(String currentMccmnc) {
        String strMcc = System.getString(this.mContext.getContentResolver(), "hw_mcc_showspn_only");
        HashSet<String> mShowspnOnlyMcc = new HashSet();
        String currentMcc = "";
        int i = 0;
        if (currentMccmnc == null || currentMccmnc.length() < 3) {
            return false;
        }
        currentMcc = currentMccmnc.substring(0, 3);
        if (strMcc == null || mShowspnOnlyMcc.size() != 0) {
            return false;
        }
        String[] mcc = strMcc.split(",");
        while (i < mcc.length) {
            mShowspnOnlyMcc.add(mcc[i].trim());
            i++;
        }
        return mShowspnOnlyMcc.contains(currentMcc);
    }

    private boolean getNoRoamingByMcc(ServiceState mSS) {
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        if (!(r == null || mSS == null)) {
            String hplmn = r.getOperatorNumeric();
            String regplmn = mSS.getOperatorNumeric();
            if (isMccForNoRoaming(hplmn)) {
                String currentMcc = hplmn.substring(0, 3);
                if (regplmn != null && regplmn.length() > 3 && currentMcc.equals(regplmn.substring(0, 3))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMccForNoRoaming(String currentMccmnc) {
        String strMcc = System.getString(this.mContext.getContentResolver(), "hw_mcc_show_no_roaming");
        HashSet<String> mShowNoRoamingMcc = new HashSet();
        String currentMcc = "";
        int i = 0;
        if (currentMccmnc == null || currentMccmnc.length() < 3) {
            return false;
        }
        currentMcc = currentMccmnc.substring(0, 3);
        if (strMcc == null || mShowNoRoamingMcc.size() != 0) {
            return false;
        }
        String[] mcc = strMcc.split(",");
        while (i < mcc.length) {
            mShowNoRoamingMcc.add(mcc[i].trim());
            i++;
        }
        return mShowNoRoamingMcc.contains(currentMcc);
    }

    public void setRac(int rac) {
        this.mRac = rac;
    }

    public int getRac() {
        return this.mRac;
    }

    public void getLocationInfo() {
        if (PS_CLEARCODE) {
            this.mCi.getLocationInfo(obtainMessage(64));
        }
    }

    private void registerCloudOtaBroadcastReceiver() {
        Rlog.e(LOG_TAG, "HwCloudOTAService registerCloudOtaBroadcastReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction("cloud.ota.mcc.UPDATE");
        filter.addAction("cloud.ota.dplmn.UPDATE");
        this.mGsmPhone.getContext().registerReceiver(this.mCloudOtaBroadcastReceiver, filter, "huawei.permission.RECEIVE_CLOUD_OTA_UPDATA", null);
    }

    public int updateHSPAStatus(int type, GsmCdmaPhone phone) {
        if (KEEP_3GPLUS_HPLUS) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateHSPAStatus dataRadioTechnology: ");
            stringBuilder.append(type);
            Rlog.d(str, stringBuilder.toString());
            int lac = -1;
            int cid = -1;
            if (phone != null) {
                CellLocation cl = phone.getCellLocation();
                if (cl instanceof GsmCellLocation) {
                    GsmCellLocation cellLocation = (GsmCellLocation) cl;
                    lac = cellLocation.getLac();
                    cid = cellLocation.getCid();
                }
            }
            if (this.lastLac == lac && this.lastCid == cid && this.lastType == 15 && (3 == type || 9 == type || 10 == type || 11 == type)) {
                type = this.lastType;
            }
            if (15 == type) {
                this.lastLac = lac;
                this.lastCid = cid;
                this.lastType = type;
            }
        }
        return type;
    }

    public boolean is4RMimoEnabled() {
        if (!MIMO_4R_REPORT) {
            return false;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("is4RMimoEnabled = ");
        stringBuilder.append(this.mis4RMimoEnable);
        Rlog.d(str, stringBuilder.toString());
        return this.mis4RMimoEnable;
    }

    private void on4RMimoChange(AsyncResult ar) {
        int[] responseArray = ar.result;
        int mimoResult = 0;
        if (responseArray.length != 0) {
            mimoResult = responseArray[0];
        }
        if (mimoResult == 1 && !this.mis4RMimoEnable) {
            this.mis4RMimoEnable = true;
        } else if (mimoResult == 0 && this.mis4RMimoEnable) {
            this.mis4RMimoEnable = false;
        } else {
            return;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("4R_MIMO_ENABLE = ");
        stringBuilder.append(mimoResult);
        Rlog.d(str, stringBuilder.toString());
        Intent intent = new Intent("com.huawei.intent.action.4R_MIMO_CHANGE");
        intent.addFlags(536870912);
        intent.putExtra("4RMimoStatus", this.mis4RMimoEnable);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mGsmPhone.getPhoneId());
        this.mGsmPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public String getEonsWithoutCphs() {
        IccRecords r = (IccRecords) this.mGsmPhone.mIccRecords.get();
        if (r == null || r.isEonsDisabled()) {
            return null;
        }
        Rlog.d(LOG_TAG, "getEonsWithoutCphs():get plmn from SIM card! ");
        if (updateEons(r)) {
            return r.getEons();
        }
        return null;
    }

    private OnsDisplayParams getGsmOnsDisplayParamsSpnPrior(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        boolean showSpnRes = showSpn;
        boolean showPlmnRes = showPlmn;
        String plmnRes = plmn;
        String spnRes = spn;
        String temPnn = null;
        int Rule = 1;
        String cardspn = this.mGsmPhone.mIccRecords.get() != null ? ((IccRecords) this.mGsmPhone.mIccRecords.get()).getServiceProviderName() : null;
        if (TextUtils.isEmpty(cardspn)) {
            temPnn = getEonsWithoutCphs();
            if (!TextUtils.isEmpty(temPnn)) {
                Rule = 2;
                plmnRes = temPnn;
            }
        } else {
            Rule = 1;
            spnRes = cardspn;
        }
        boolean showPlmnRes2 = false;
        boolean showSpnRes2 = (Rule & 1) == 1;
        if ((Rule & 2) == 2) {
            showPlmnRes2 = true;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getGsmOnsDisplayParamsSpnPrior: cardspn= ");
        stringBuilder.append(cardspn);
        stringBuilder.append(" temPnn= ");
        stringBuilder.append(temPnn);
        stringBuilder.append(" Rule= ");
        stringBuilder.append(Rule);
        Rlog.d(str, stringBuilder.toString());
        return new OnsDisplayParams(showSpnRes2, showPlmnRes2, Rule, plmnRes, spnRes);
    }
}
