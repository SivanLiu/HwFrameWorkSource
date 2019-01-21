package com.android.internal.telephony.gsm;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.HwTelephony.NumMatchs;
import android.provider.HwTelephony.VirtualNets;
import android.provider.Settings.System;
import android.provider.Telephony.GlobalMatchs;
import android.telephony.CarrierConfigManager;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.HwImsManagerInner;
import com.android.ims.ImsException;
import com.android.internal.telephony.AbstractGsmCdmaPhone.GSMPhoneReference;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwPhoneReferenceBase;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.PlmnConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.vsim.HwVSimConstants;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;

public class HwGSMPhoneReference extends HwPhoneReferenceBase implements GSMPhoneReference {
    private static final int APNNAME_RULE = 3;
    private static final int APN_RULE = 2;
    private static final int ECC_NOCARD_INDEX = 4;
    private static final int ECC_WITHCARD_INDEX = 3;
    private static final int EVENT_GET_AVALIABLE_NETWORKS_DONE = 1;
    private static final int EVENT_RADIO_ON = 5;
    private static final int GID1_RULE = 1;
    private static final boolean HW_VM_NOT_FROM_SIM = SystemProperties.getBoolean("ro.config.hw_voicemail_sim", false);
    protected static final String LOG_TAG = "HwGSMPhoneReference";
    private static final int MEID_LENGTH = 14;
    private static final int NAME_INDEX = 1;
    private static final int NUMERIC_INDEX = 2;
    private static final Uri PREFERAPN_NO_UPDATE_URI = Uri.parse("content://telephony/carriers/preferapn_no_update");
    private static final String PROPERTY_GLOBAL_FORCE_TO_SET_ECC = "ril.force_to_set_ecc";
    protected static final String PROP_REDUCE_SAR_SPECIAL_TYPE1 = "ro.config.reduce_sar_type1";
    protected static final String PROP_REDUCE_SAR_SPECIAL_TYPE2 = "ro.config.reduce_sar_type2";
    protected static final String PROP_REDUCE_SAR_SPECIAL_lEVEL = "ro.config.reduce_sar_level";
    public static final int REDUCE_SAR_NORMAL_HOTSPOT_ON = 8192;
    public static final int REDUCE_SAR_NORMAL_WIFI_OFF = 0;
    public static final int REDUCE_SAR_NORMAL_WIFI_ON = 4096;
    public static final int REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON = 20480;
    public static final int REDUCE_SAR_SPECIAL_TYPE1_WIFI_OFF = 12288;
    public static final int REDUCE_SAR_SPECIAL_TYPE1_WIFI_ON = 16384;
    private static final int REDUCE_SAR_SPECIAL_TYPE2_HOTSPOT_ON = 12287;
    private static final int REDUCE_SAR_SPECIAL_TYPE2_WIFI_OFF = 4095;
    private static final int REDUCE_SAR_SPECIAL_TYPE2_WIFI_ON = 8191;
    private static final int SEARCH_LENGTH = 2;
    private static final int SPN_RULE = 0;
    public static final int SUB_NONE = -1;
    private static final String USSD_IS_OK_FOR_RELEASE = "is_ussd_ok_for_nw_release";
    private static final boolean isMultiEnable = TelephonyManager.getDefault().isMultiSimEnabled();
    private static boolean mIsQCRilGoDormant = true;
    private static Object mQcRilHook = null;
    private static final boolean mWcdmaVpEnabled = SystemProperties.get("ro.hwpp.wcdma_voice_preference", "false").equals("true");
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                super.handleMessage(msg);
                return;
            }
            ArrayList<OperatorInfo> searchResults = null;
            AsyncResult ar = msg.obj;
            if (ar.exception == null && ar.result != null) {
                searchResults = HwGSMPhoneReference.this.custAvaliableNetworks((ArrayList) ar.result);
            }
            if (searchResults != null) {
                HwGSMPhoneReference.this.logd("[EVENT_GET_NETWORKS_DONE] Populated global version cust names for available networks.");
            } else {
                searchResults = ar.result;
            }
            Message onComplete = ar.userObj;
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, searchResults, ar.exception);
                if (onComplete.getTarget() != null) {
                    onComplete.sendToTarget();
                    return;
                }
                return;
            }
            HwGSMPhoneReference.this.loge("[EVENT_GET_NETWORKS_DONE] In EVENT_GET_NETWORKS_DONE, onComplete is null!");
        }
    };
    private HwCustHwGSMPhoneReference mHwCustHwGSMPhoneReference;
    private HwVpApStatusHandler mHwVpApStatusHandler;
    private int mLteReleaseVersion;
    private String mMeid;
    private GsmCdmaPhone mPhone;
    private int mPhoneId = 0;
    private final PhoneStateListener mPhoneStateListener;
    private int mPrevPowerGrade = -1;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE".equals(intent.getAction()) && HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT) {
                CommandsInterface commandsInterface = HwGSMPhoneReference.this.mPhone.mCi;
                GsmCdmaPhone access$000 = HwGSMPhoneReference.this.mPhone;
                HwGSMPhoneReference.this.mPhone;
                commandsInterface.getDeviceIdentity(access$000.obtainMessage(21));
                return;
            }
            if (-1 != SystemProperties.getInt("persist.radio.stack_id_0", -1) && "com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT".equals(intent.getAction())) {
                int subId = intent.getIntExtra("subscription", -1000);
                int phoneId = intent.getIntExtra("phone", 0);
                int status = intent.getIntExtra("operationResult", 1);
                int state = intent.getIntExtra("newSubState", 0);
                String str = HwGSMPhoneReference.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received ACTION_SUBSCRIPTION_SET_UICC_RESULT on subId: ");
                stringBuilder.append(subId);
                stringBuilder.append("phoneId ");
                stringBuilder.append(phoneId);
                stringBuilder.append(" status: ");
                stringBuilder.append(status);
                stringBuilder.append("state: ");
                stringBuilder.append(state);
                Rlog.d(str, stringBuilder.toString());
                if (status == 0 && state == 1) {
                    CommandsInterface commandsInterface2 = HwGSMPhoneReference.this.mPhone.mCi;
                    GsmCdmaPhone access$0002 = HwGSMPhoneReference.this.mPhone;
                    HwGSMPhoneReference.this.mPhone;
                    commandsInterface2.getDeviceIdentity(access$0002.obtainMessage(21));
                }
            }
        }
    };
    private Boolean mSarControlServiceExist = null;
    private IccRecords mSimRecords;
    private int mSlotId = 0;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private String mncForSAR = null;
    private String preOperatorNumeric = "";
    private String redcueSARSpeacialType1 = null;
    private String redcueSARSpeacialType2 = null;
    private String subTag;

    public HwGSMPhoneReference(GsmCdmaPhone phone) {
        super(phone);
        this.mPhone = phone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwGSMPhoneReference[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        this.subTag = stringBuilder.toString();
        this.mPhoneId = this.mPhone.getPhoneId();
        Context mContext = this.mPhone.getContext();
        IntentFilter filter = new IntentFilter("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        mContext.registerReceiver(this.mReceiver, filter);
        this.mSlotId = SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
        this.mWifiManager = (WifiManager) this.mPhone.getContext().getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (mWcdmaVpEnabled) {
            this.mHwVpApStatusHandler = new HwVpApStatusHandler(this.mPhone);
        }
        this.mHwCustHwGSMPhoneReference = (HwCustHwGSMPhoneReference) HwCustUtils.createObj(HwCustHwGSMPhoneReference.class, new Object[]{this.mPhone});
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(this.mSlotId)) {
            public void onServiceStateChanged(ServiceState serviceState) {
                String hplmn = null;
                if (HwGSMPhoneReference.this.mPhone.mIccRecords.get() != null) {
                    hplmn = ((IccRecords) HwGSMPhoneReference.this.mPhone.mIccRecords.get()).getOperatorNumeric();
                }
                if (TelephonyManager.getDefault().getCurrentPhoneType(HwGSMPhoneReference.this.mSlotId) != 1 || !SystemProperties.getBoolean("ro.config.hw_eccNumUseRplmn", false)) {
                    return;
                }
                if (TelephonyManager.getDefault().isNetworkRoaming(HwGSMPhoneReference.this.mSlotId)) {
                    HwGSMPhoneReference.this.globalEccCustom(serviceState.getOperatorNumeric());
                } else if (hplmn != null) {
                    HwGSMPhoneReference.this.globalEccCustom(hplmn);
                }
            }
        };
        startListen();
    }

    public void startListen() {
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
    }

    public void setLTEReleaseVersion(int state, Message response) {
        this.mPhone.mCi.setLTEReleaseVersion(state, response);
    }

    public int getLteReleaseVersion() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLteReleaseVersion: ");
        stringBuilder.append(this.mLteReleaseVersion);
        logd(stringBuilder.toString());
        return this.mLteReleaseVersion;
    }

    public void getCallbarringOption(String facility, String serviceClass, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCallbarringOption, facility=");
        stringBuilder.append(facility);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        logd(stringBuilder.toString());
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isUtEnable()) {
            this.mPhone.mCi.queryFacilityLock(facility, "", HwGsmMmiCode.siToServiceClass(serviceClass), response);
            return;
        }
        logd("getCallbarringOption via Ut");
        imsPhone.getCallBarring(facility, response);
    }

    public void setCallbarringOption(String facility, String password, boolean isActivate, String serviceClass, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCallbarringOption, facility=");
        stringBuilder.append(facility);
        stringBuilder.append(", isActivate=");
        stringBuilder.append(isActivate);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        logd(stringBuilder.toString());
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isUtEnable()) {
            this.mPhone.mCi.setFacilityLock(facility, isActivate, password, HwGsmMmiCode.siToServiceClass(serviceClass), response);
            return;
        }
        logd("setCallbarringOption via Ut");
        imsPhone.setCallBarring(facility, isActivate, password, response);
    }

    public void getCallbarringOption(String facility, int serviceClass, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCallbarringOption, facility=");
        stringBuilder.append(facility);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        logd(stringBuilder.toString());
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isUtEnable()) {
            this.mPhone.mCi.queryFacilityLock(facility, "", serviceClass, response);
            return;
        }
        logd("getCallbarringOption via Ut");
        imsPhone.getCallBarring(facility, response);
    }

    public void setCallbarringOption(String facility, String password, boolean isActivate, int serviceClass, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCallbarringOption, facility=");
        stringBuilder.append(facility);
        stringBuilder.append(", isActivate=");
        stringBuilder.append(isActivate);
        stringBuilder.append(", serviceClass=");
        stringBuilder.append(serviceClass);
        logd(stringBuilder.toString());
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isUtEnable()) {
            this.mPhone.mCi.setFacilityLock(facility, isActivate, password, serviceClass, response);
            return;
        }
        logd("setCallbarringOption via Ut");
        imsPhone.setCallBarring(facility, isActivate, password, response);
    }

    public void changeBarringPassword(String oldPassword, String newPassword, Message response) {
        this.mPhone.mCi.changeBarringPassword("AB", oldPassword, newPassword, response);
    }

    public Message getCustAvailableNetworksMessage(Message response) {
        return this.mHandler.obtainMessage(1, response);
    }

    private String getSelectedApn() {
        String apn = null;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        if (mContext == null) {
            return null;
        }
        Cursor cursor;
        if (isMultiEnable) {
            cursor = mContext.getContentResolver().query(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) this.mPhone.getSubId()), new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        } else {
            cursor = mContext.getContentResolver().query(PREFERAPN_NO_UPDATE_URI, new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        }
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apn = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
        }
        if (cursor != null) {
            cursor.close();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("apn:");
        stringBuilder.append(apn);
        logd(stringBuilder.toString());
        return apn;
    }

    public String getSelectedApnName() {
        String apnName = null;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        if (mContext == null) {
            return null;
        }
        Cursor cursor;
        if (isMultiEnable) {
            cursor = mContext.getContentResolver().query(ContentUris.withAppendedId(PREFERAPN_NO_UPDATE_URI, (long) this.mPhone.getSubId()), new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        } else {
            cursor = mContext.getContentResolver().query(PREFERAPN_NO_UPDATE_URI, new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        }
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apnName = cursor.getString(cursor.getColumnIndexOrThrow(NumMatchs.NAME));
        }
        if (cursor != null) {
            cursor.close();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("apnname");
        stringBuilder.append(apnName);
        logd(stringBuilder.toString());
        return apnName;
    }

    /* JADX WARNING: Missing block: B:64:0x0154, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getCustOperatorName(String rplmn) {
        String str = rplmn;
        if (this.mPhone != null) {
            this.mSimRecords = (IccRecords) this.mPhone.mIccRecords.get();
        }
        String mImsi = this.mSimRecords != null ? this.mSimRecords.getIMSI() : null;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        String mCustOperatorString = mContext != null ? System.getString(mContext.getContentResolver(), "hw_cust_operator") : null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCustOperatorString");
        stringBuilder.append(mCustOperatorString);
        logd(stringBuilder.toString());
        if (TextUtils.isEmpty(mCustOperatorString) || TextUtils.isEmpty(mImsi) || TextUtils.isEmpty(rplmn)) {
            return null;
        }
        int maxLength = 0;
        String operatorName = null;
        for (String item : mCustOperatorString.split(";")) {
            String[] plmns = item.split(",");
            if (plmns.length != 3) {
                if (plmns.length == 5) {
                    if (str.equals(plmns[3]) && mImsi.startsWith(plmns[1])) {
                        if (!"0".equals(plmns[0]) || !isSpnGid1ApnnameMatched(plmns[2], 0)) {
                            if (!"1".equals(plmns[0]) || !isSpnGid1ApnnameMatched(plmns[2], 1)) {
                                if (!"2".equals(plmns[0]) || !isSpnGid1ApnnameMatched(plmns[2], 2)) {
                                    if ("3".equals(plmns[0]) && isSpnGid1ApnnameMatched(plmns[2], 3)) {
                                        operatorName = plmns[4];
                                        logd("operatorName changed by Apnname confirg");
                                        break;
                                    }
                                }
                                operatorName = plmns[4];
                                logd("operatorName changed by Apn confirg");
                                break;
                            }
                            operatorName = plmns[4];
                            logd("operatorName changed by Gid1 confirg");
                            break;
                        }
                        operatorName = plmns[4];
                        logd("operatorName changed by spn confirg");
                        break;
                    }
                }
                logd("Wrong length");
            } else if (str.equals(plmns[1])) {
                if ((mImsi.startsWith(plmns[0]) || "0".equals(plmns[0])) && plmns[0].length() > maxLength) {
                    String operatorName2 = plmns[2];
                    maxLength = plmns[0].length();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("operatorName changed");
                    stringBuilder2.append(operatorName2);
                    logd(stringBuilder2.toString());
                    operatorName = operatorName2;
                }
            }
        }
        return operatorName;
    }

    public boolean isSpnGid1ApnnameMatched(String spnorgid1orapnname, int rule) {
        if (this.mSimRecords == null || spnorgid1orapnname == null) {
            return false;
        }
        String spn;
        StringBuilder stringBuilder;
        if (rule == 0) {
            spn = this.mSimRecords.getServiceProviderName();
            stringBuilder = new StringBuilder();
            stringBuilder.append("[EONS] spn = ");
            stringBuilder.append(spn);
            stringBuilder.append(", spnorgid1orapnname(gid1 start with 0x) = ");
            stringBuilder.append(spnorgid1orapnname);
            logd(stringBuilder.toString());
            if (!spnorgid1orapnname.equals(spn)) {
                return false;
            }
            logd("[EONS] ShowPLMN: use the spn rule");
            return true;
        } else if (rule == 1) {
            byte[] gid1 = this.mSimRecords.getGID1();
            if (gid1 == null || gid1.length <= 0 || spnorgid1orapnname.length() <= 2 || !spnorgid1orapnname.substring(0, 2).equalsIgnoreCase("0x")) {
                return false;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[EONS] gid1 = ");
            stringBuilder2.append(gid1[0]);
            logd(stringBuilder2.toString());
            byte[] gid1valueBytes = IccUtils.hexStringToBytes(spnorgid1orapnname.substring(2));
            int i = 0;
            while (i < gid1.length && i < gid1valueBytes.length) {
                if (gid1[i] != gid1valueBytes[i]) {
                    return false;
                }
                i++;
            }
            logd("[EONS] ShowPLMN: use the Gid1 rule");
            return true;
        } else if (rule == 2) {
            spn = getSelectedApn();
            stringBuilder = new StringBuilder();
            stringBuilder.append("[EONS] apn = ");
            stringBuilder.append(spn);
            logd(stringBuilder.toString());
            if (!spnorgid1orapnname.equals(spn)) {
                return false;
            }
            logd("[EONS] ShowPLMN: use the apn rule");
            return true;
        } else if (rule != 3) {
            return false;
        } else {
            spn = getSelectedApnName();
            stringBuilder = new StringBuilder();
            stringBuilder.append("[EONS] apnname = ");
            stringBuilder.append(spn);
            logd(stringBuilder.toString());
            if (!spnorgid1orapnname.equals(spn)) {
                return false;
            }
            logd("[EONS] ShowPLMN: use the apnname rule");
            return true;
        }
    }

    private ArrayList<OperatorInfo> getEonsForAvailableNetworks(ArrayList<OperatorInfo> searchResult) {
        if (HwModemCapability.isCapabilitySupport(5)) {
            return searchResult;
        }
        IccRecords rrecords = (IccRecords) this.mPhone.mIccRecords.get();
        if (rrecords != null) {
            ArrayList<OperatorInfo> eonsNetworkNames = rrecords.getEonsForAvailableNetworks(searchResult);
            if (eonsNetworkNames != null) {
                searchResult = eonsNetworkNames;
            }
        }
        return searchResult;
    }

    private boolean enableCustOperatorNameBySpn(String hplmn, String plmn) {
        if (this.mPhone == null) {
            return false;
        }
        Context mContext = this.mPhone.getContext();
        String custOperatorName = null;
        if (TextUtils.isEmpty(plmn) || TextUtils.isEmpty(hplmn)) {
            return false;
        }
        if (mContext != null) {
            custOperatorName = System.getString(mContext.getContentResolver(), "hw_enable_srchListNameBySpn");
        }
        if (TextUtils.isEmpty(custOperatorName)) {
            return false;
        }
        for (String item : custOperatorName.split(";")) {
            String[] plmns = item.split(",");
            if (2 == plmns.length && hplmn.equals(plmns[0]) && plmn.equals(plmns[1])) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<OperatorInfo> custAvaliableNetworks(ArrayList<OperatorInfo> searchResult) {
        if (searchResult == null || searchResult.size() == 0) {
            return searchResult;
        }
        ArrayList<OperatorInfo> searchResult2;
        int i = 0;
        boolean HW_SEARCH_NET_SIM_UE_PRIORITY = SystemProperties.getBoolean("ro.config.srch_net_sim_ue_pri", false);
        if (HW_SEARCH_NET_SIM_UE_PRIORITY) {
            searchResult2 = searchResult;
        } else {
            searchResult2 = getEonsForAvailableNetworks(searchResult);
        }
        ArrayList<OperatorInfo> custResults = new ArrayList();
        String radioTechStr = "";
        int i2 = 0;
        int list_size = searchResult2.size();
        while (i2 < list_size) {
            String hplmn;
            boolean HW_SEARCH_NET_SIM_UE_PRIORITY2;
            String substring;
            OperatorInfo operatorInfo = (OperatorInfo) searchResult2.get(i2);
            String plmn = operatorInfo.getOperatorNumericWithoutAct();
            if (plmn != null) {
                int plmnEnd = plmn.indexOf(",");
                if (plmnEnd > 0) {
                    plmn = plmn.substring(i, plmnEnd);
                }
            }
            String longName = null;
            String plmnRadioTechStr = "";
            String longNameWithoutAct = null;
            IccRecords iccRecords = (IccRecords) this.mPhone.mIccRecords.get();
            if (iccRecords != null) {
                hplmn = iccRecords.getOperatorNumeric();
                int lastSpaceIndexInLongName = operatorInfo.getOperatorAlphaLong().lastIndexOf(32);
                if (-1 != lastSpaceIndexInLongName) {
                    radioTechStr = operatorInfo.getOperatorAlphaLong().substring(lastSpaceIndexInLongName);
                    if (!(" 2G".equals(radioTechStr) || " 3G".equals(radioTechStr) || " 4G".equals(radioTechStr))) {
                        radioTechStr = "";
                    }
                    HW_SEARCH_NET_SIM_UE_PRIORITY2 = HW_SEARCH_NET_SIM_UE_PRIORITY;
                    longNameWithoutAct = operatorInfo.getOperatorAlphaLong().substring(false, lastSpaceIndexInLongName);
                } else {
                    HW_SEARCH_NET_SIM_UE_PRIORITY2 = HW_SEARCH_NET_SIM_UE_PRIORITY;
                    longNameWithoutAct = operatorInfo.getOperatorAlphaLong();
                }
                HW_SEARCH_NET_SIM_UE_PRIORITY = operatorInfo.getOperatorNumeric().lastIndexOf(32);
                if (true != HW_SEARCH_NET_SIM_UE_PRIORITY) {
                    substring = operatorInfo.getOperatorNumeric().substring(HW_SEARCH_NET_SIM_UE_PRIORITY);
                    if ((" 2G".equals(substring) || " 3G".equals(substring) || " 4G".equals(substring)) && plmn != null) {
                        plmnRadioTechStr = null;
                        plmn = plmn.substring(0, HW_SEARCH_NET_SIM_UE_PRIORITY);
                    } else {
                        plmnRadioTechStr = null;
                    }
                    String str = plmnRadioTechStr;
                    plmnRadioTechStr = substring;
                    substring = str;
                }
                boolean lastSpaceIndexInPlmn;
                if (plmn != null) {
                    substring = getCustOperatorName(plmn);
                    lastSpaceIndexInPlmn = HW_SEARCH_NET_SIM_UE_PRIORITY;
                    if (this.mHwCustHwGSMPhoneReference) {
                        substring = this.mHwCustHwGSMPhoneReference.getCustOperatorNameBySpn(plmn, substring);
                    }
                } else {
                    lastSpaceIndexInPlmn = HW_SEARCH_NET_SIM_UE_PRIORITY;
                    substring = null;
                }
                if (substring != null) {
                    longName = substring.concat(radioTechStr);
                    if (this.mHwCustHwGSMPhoneReference) {
                        longName = this.mHwCustHwGSMPhoneReference.modifyTheFormatName(plmn, substring, radioTechStr);
                    }
                }
                if (("50503".equals(plmn) && "50503".equals(hplmn)) || enableCustOperatorNameBySpn(hplmn, plmn)) {
                    if (iccRecords.getDisplayRule(this.mPhone.getServiceState()) & true) {
                        HW_SEARCH_NET_SIM_UE_PRIORITY = iccRecords.getServiceProviderName();
                        if (HW_SEARCH_NET_SIM_UE_PRIORITY && HW_SEARCH_NET_SIM_UE_PRIORITY.trim().length() > 0) {
                            longName = HW_SEARCH_NET_SIM_UE_PRIORITY.concat(radioTechStr);
                        }
                    }
                }
                HW_SEARCH_NET_SIM_UE_PRIORITY = radioTechStr;
            } else {
                HW_SEARCH_NET_SIM_UE_PRIORITY2 = HW_SEARCH_NET_SIM_UE_PRIORITY;
                HW_SEARCH_NET_SIM_UE_PRIORITY = radioTechStr;
            }
            if (!TextUtils.isEmpty(longNameWithoutAct)) {
                substring = null;
                try {
                    substring = System.getString(this.mPhone.getContext().getContentResolver(), "plmn");
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("plmn config = ");
                    stringBuilder.append(substring);
                    logd(stringBuilder.toString());
                } catch (Exception e) {
                    loge("Exception when got data value", e);
                }
                if (!TextUtils.isEmpty(substring)) {
                    PlmnConstants plmnConstants = new PlmnConstants(substring);
                    hplmn = new StringBuilder();
                    hplmn.append(Locale.getDefault().getLanguage());
                    hplmn.append("_");
                    hplmn.append(Locale.getDefault().getCountry());
                    String longNameCust = plmnConstants.getPlmnValue(plmn, hplmn.toString());
                    if (longNameCust == null) {
                        String DEFAULT_LOCALE = "en_US";
                        longNameCust = plmnConstants.getPlmnValue(plmn, "en_US");
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("longName = ");
                    stringBuilder2.append(longName);
                    stringBuilder2.append(", longNameCust = ");
                    stringBuilder2.append(longNameCust);
                    logd(stringBuilder2.toString());
                    if (longName == null && longNameCust != null) {
                        longName = longNameCust.concat(HW_SEARCH_NET_SIM_UE_PRIORITY);
                    }
                }
            }
            if (longName != null) {
                custResults.add(new OperatorInfo(longName, operatorInfo.getOperatorAlphaShort(), operatorInfo.getOperatorNumeric(), operatorInfo.getState()));
            } else {
                custResults.add(operatorInfo);
            }
            i2++;
            radioTechStr = HW_SEARCH_NET_SIM_UE_PRIORITY;
            HW_SEARCH_NET_SIM_UE_PRIORITY = HW_SEARCH_NET_SIM_UE_PRIORITY2;
            i = 0;
        }
        if (this.mHwCustHwGSMPhoneReference != null) {
            custResults = this.mHwCustHwGSMPhoneReference.filterActAndRepeatedItems(custResults);
        }
        return custResults;
    }

    private boolean getHwVmNotFromSimValue() {
        boolean valueFromProp = HW_VM_NOT_FROM_SIM;
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("hw_voicemail_sim", this.mPhoneId, Boolean.class);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwVmNotFromSimValue, phoneId");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop: ");
        stringBuilder.append(valueFromProp);
        logd(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    public String getDefaultVoiceMailAlphaTagText(Context mContext, String ret) {
        if (getHwVmNotFromSimValue() || ret == null || ret.length() == 0 || isCustVoicemailTag(mContext)) {
            return mContext.getText(17039364).toString();
        }
        return ret;
    }

    private boolean isCustVoicemailTag(Context mContext) {
        String strVMTagNotFromConf = System.getString(mContext.getContentResolver(), "hw_vmtag_follow_language");
        String carrier = "";
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            carrier = TelephonyManager.getDefault().getSimOperator(this.mPhoneId);
        } else {
            carrier = TelephonyManager.getDefault().getSimOperator();
        }
        if (!(TextUtils.isEmpty(strVMTagNotFromConf) || TextUtils.isEmpty(carrier))) {
            for (String area : strVMTagNotFromConf.split(",")) {
                if (area.equals(carrier)) {
                    logd("voicemail Tag need follow language");
                    return true;
                }
            }
        }
        logd("voicemail Tag not need follow language");
        return false;
    }

    public String getMeid() {
        logd("[HwGSMPhoneReference]getMeid()");
        return this.mMeid;
    }

    public String getPesn() {
        logd("[HwGSMPhoneReference]getPesn()");
        return this.mPhone.getEsn();
    }

    public boolean beforeHandleMessage(Message msg) {
        boolean result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("beforeHandleMessage what = ");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
        int i = msg.what;
        if (i != 105) {
            if (i == 108) {
                logd("onGetLteReleaseVersionDone:");
                AsyncResult ar = msg.obj;
                result = true;
                if (ar.exception == null) {
                    int[] resultint = ar.result;
                    if (resultint != null) {
                        if (resultint.length != 0) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("onGetLteReleaseVersionDone: result=");
                            stringBuilder2.append(resultint[0]);
                            logd(stringBuilder2.toString());
                            switch (resultint[0]) {
                                case 0:
                                    this.mLteReleaseVersion = 0;
                                    break;
                                case 1:
                                    this.mLteReleaseVersion = 1;
                                    break;
                                case 2:
                                    this.mLteReleaseVersion = 2;
                                    break;
                                case 3:
                                    this.mLteReleaseVersion = 3;
                                    break;
                                default:
                                    this.mLteReleaseVersion = 0;
                                    break;
                            }
                        }
                    }
                    logd("Error in get lte release version: null resultint");
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Error in get lte release version:");
                    stringBuilder3.append(ar.exception);
                    logd(stringBuilder3.toString());
                }
            } else if (i == 111) {
                logd("beforeHandleMessage handled->EVENT_SET_MODE_TO_AUTO ");
                result = true;
                this.mPhone.setNetworkSelectionModeAutomatic(null);
            } else if (i != 1000) {
                return super.beforeHandleMessage(msg);
            } else {
                if (msg.arg2 == 2) {
                    logd("start retry get DEVICE_ID_MASK_ALL");
                    this.mPhone.mCi.getDeviceIdentity(this.mPhone.obtainMessage(21, msg.arg1, 0, null));
                } else {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("EVENT_RETRY_GET_DEVICE_ID msg.arg2:");
                    stringBuilder4.append(msg.arg2);
                    stringBuilder4.append(", error!!");
                    logd(stringBuilder4.toString());
                }
                result = true;
            }
        } else {
            updateReduceSARState();
            result = true;
        }
        return result;
    }

    public void afterHandleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("afterHandleMessage what = ");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
        int i = msg.what;
        if (i == 1) {
            this.mPhone.mCi.getDeviceIdentity(this.mPhone.obtainMessage(21));
            logd("[HwGSMPhoneReference]PhoneBaseUtils.EVENT_RADIO_AVAILABLE");
            logd("Radio available, get lte release version");
            this.mPhone.mCi.getLteReleaseVersion(this.mPhone.obtainMessage(108));
        } else if (i != 5) {
            logd("unhandle event");
        } else {
            logd("Radio on, get lte release version");
            this.mPhone.mCi.getLteReleaseVersion(this.mPhone.obtainMessage(108));
        }
    }

    public void closeRrc() {
        if (mIsQCRilGoDormant) {
            qcRilGoDormant();
            if (!mIsQCRilGoDormant) {
                closeRrcInner();
                return;
            }
            return;
        }
        closeRrcInner();
    }

    private void closeRrcInner() {
        try {
            logd("closeRrcInner in GSMPhone");
            this.mPhone.mCi.getClass().getMethod("closeRrc", new Class[0]).invoke(this.mPhone.mCi, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void qcRilGoDormant() {
        try {
            if (mQcRilHook == null) {
                Object[] params = new Object[]{this.mPhone.getContext()};
                mQcRilHook = Class.forName("com.qualcomm.qcrilhook.QcRilHook").getConstructor(new Class[]{Context.class}).newInstance(params);
            }
            if (mQcRilHook != null) {
                mQcRilHook.getClass().getMethod("qcRilGoDormant", new Class[]{String.class}).invoke(mQcRilHook, new Object[]{""});
                return;
            }
            logd("mQcRilHook is null");
            mIsQCRilGoDormant = false;
        } catch (ClassNotFoundException e) {
            mIsQCRilGoDormant = false;
            loge("the class QcRilHook not exist");
        } catch (NoSuchMethodException e2) {
            mIsQCRilGoDormant = false;
            loge("the class QcRilHook NoSuchMethod: qcRilGoDormant [String]");
        } catch (LinkageError e3) {
            mIsQCRilGoDormant = false;
            loge("class QcRilHook LinkageError", e3);
        } catch (InstantiationException e4) {
            mIsQCRilGoDormant = false;
            loge("class QcRilHook InstantiationException", e4);
        } catch (IllegalAccessException e5) {
            mIsQCRilGoDormant = false;
            loge("class QcRilHook IllegalAccessException", e5);
        } catch (IllegalArgumentException e6) {
            mIsQCRilGoDormant = false;
            loge("class QcRilHook IllegalArgumentException", e6);
        } catch (InvocationTargetException e7) {
            mIsQCRilGoDormant = false;
            loge("class QcRilHook InvocationTargetException", e7);
        }
    }

    public void setMeid(String meid) {
        this.mMeid = meid;
    }

    private int getCardType() {
        this.redcueSARSpeacialType1 = SystemProperties.get(PROP_REDUCE_SAR_SPECIAL_TYPE1, " ");
        this.redcueSARSpeacialType2 = SystemProperties.get(PROP_REDUCE_SAR_SPECIAL_TYPE2, " ");
        this.mncForSAR = SystemProperties.get("reduce.sar.imsi.mnc", "FFFFF");
        if (this.mncForSAR == null || this.mncForSAR.equals("FFFFF") || this.mncForSAR.length() < 3) {
            return 0;
        }
        this.mncForSAR = this.mncForSAR.substring(0, 3);
        if (this.redcueSARSpeacialType1.contains(this.mncForSAR)) {
            return 1;
        }
        if (this.redcueSARSpeacialType2.contains(this.mncForSAR)) {
            return 2;
        }
        return 0;
    }

    private int getWifiType(int wifiHotState, int wifiState) {
        if (wifiHotState == 13) {
            return 2;
        }
        if (wifiState == 3) {
            return 1;
        }
        return wifiState == 1 ? 0 : 0;
    }

    private int[][] getGradeData(boolean isReceiveOFF) {
        if (isReceiveOFF) {
            return new int[][]{new int[]{REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON, REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON, REDUCE_SAR_NORMAL_HOTSPOT_ON}, new int[]{REDUCE_SAR_SPECIAL_TYPE1_WIFI_OFF, REDUCE_SAR_SPECIAL_TYPE1_WIFI_ON, REDUCE_SAR_NORMAL_HOTSPOT_ON}, new int[]{REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON, REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON, REDUCE_SAR_SPECIAL_TYPE1_HOTSPOT_ON}};
        }
        return new int[][]{new int[]{0, REDUCE_SAR_NORMAL_WIFI_ON, REDUCE_SAR_NORMAL_HOTSPOT_ON}, new int[]{REDUCE_SAR_SPECIAL_TYPE1_WIFI_OFF, REDUCE_SAR_SPECIAL_TYPE1_WIFI_ON, REDUCE_SAR_NORMAL_HOTSPOT_ON}, new int[]{REDUCE_SAR_SPECIAL_TYPE2_WIFI_OFF, REDUCE_SAR_SPECIAL_TYPE2_WIFI_ON, REDUCE_SAR_SPECIAL_TYPE2_HOTSPOT_ON}};
    }

    private void setPowerGrade(int powerGrade) {
        if (this.mPrevPowerGrade != powerGrade) {
            this.mPrevPowerGrade = powerGrade;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateReduceSARState()    setPowerGrade() : ");
            stringBuilder.append(powerGrade);
            logd(stringBuilder.toString());
            this.mPhone.mCi.setPowerGrade(powerGrade, null);
        }
    }

    private boolean reduceOldSar() {
        if (!SystemProperties.getBoolean("ro.config.old_reduce_sar", false)) {
            return false;
        }
        int wifiHotState = this.mWifiManager.getWifiApState();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Radio available, set wifiHotState  ");
        stringBuilder.append(wifiHotState);
        logd(stringBuilder.toString());
        if (wifiHotState == 13) {
            setPowerGrade(1);
        } else if (wifiHotState == 11) {
            setPowerGrade(0);
        }
        return true;
    }

    public void resetReduceSARPowerGrade() {
        this.mPrevPowerGrade = -1;
    }

    public void updateReduceSARState() {
        boolean isReceiveOFF = true;
        if (this.mSarControlServiceExist == null) {
            try {
                this.mSarControlServiceExist = this.mPhone.getContext().getPackageManager().getApplicationInfo("com.huawei.sarcontrolservice", REDUCE_SAR_NORMAL_HOTSPOT_ON) == null ? Boolean.valueOf(false) : Boolean.valueOf(true);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateReduceSARState mSarControlServiceExist=");
                stringBuilder.append(this.mSarControlServiceExist);
                logd(stringBuilder.toString());
                if (this.mSarControlServiceExist.booleanValue() && !SystemProperties.getBoolean("ro.config.hw_ReduceSAR", false)) {
                    logd("updateReduceSARState mSarControlServiceExist=true");
                    return;
                }
            } catch (NameNotFoundException e) {
                this.mSarControlServiceExist = Boolean.valueOf(false);
                logd("updateReduceSARState mSarControlServiceExist=false");
            }
        } else if (this.mSarControlServiceExist.booleanValue() && !SystemProperties.getBoolean("ro.config.hw_ReduceSAR", false)) {
            return;
        }
        logd("updateReduceSARState()");
        if (!reduceOldSar() && SystemProperties.getBoolean("ro.config.hw_ReduceSAR", false)) {
            AudioManager localAudioManager = (AudioManager) this.mPhone.getContext().getSystemService("audio");
            int cardType = getCardType();
            int wifiType = getWifiType(this.mWifiManager.getWifiApState(), this.mWifiManager.getWifiState());
            int state = TelephonyManager.getDefault().getCallState();
            boolean isWiredHeadsetOn = localAudioManager.isWiredHeadsetOn();
            boolean isSpeakerphoneOn = localAudioManager.isSpeakerphoneOn();
            if (!(isWiredHeadsetOn || isSpeakerphoneOn || state != 2)) {
                isReceiveOFF = false;
            }
            if (SystemProperties.getBoolean("persist.gsm.ReceiveTestMode", false)) {
                isReceiveOFF = SystemProperties.getBoolean("persist.gsm.ReceiveTestValue", false);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("hw_ReceiveTestMode = true isReceiveOFF = ");
                stringBuilder2.append(isReceiveOFF);
                logd(stringBuilder2.toString());
            }
            int reduceData = getGradeData(isReceiveOFF)[cardType][wifiType];
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("updateReduceSARState(); isWiredHeadsetOn=");
            stringBuilder3.append(isWiredHeadsetOn);
            stringBuilder3.append(", isSpeakerphoneOn=");
            stringBuilder3.append(isSpeakerphoneOn);
            stringBuilder3.append(", state=");
            stringBuilder3.append(state);
            stringBuilder3.append(", reduceData=");
            stringBuilder3.append(reduceData);
            logd(stringBuilder3.toString());
            setPowerGrade(reduceData);
        }
    }

    public void dispose() {
        if (mWcdmaVpEnabled && this.mHwVpApStatusHandler != null) {
            this.mHwVpApStatusHandler.dispose();
        }
        this.mPhone.getContext().unregisterReceiver(this.mReceiver);
    }

    public void switchVoiceCallBackgroundState(int state) {
        this.mPhone.mCT.switchVoiceCallBackgroundState(state);
    }

    public boolean isMmiCode(String dialString, UiccCardApplication app) {
        if (GsmMmiCode.newFromDialString(dialString, this.mPhone, app) != null) {
            return true;
        }
        boolean result = false;
        switch (dialString.charAt(0)) {
            case HwVSimConstants.EVENT_GET_PREFERRED_NETWORK_TYPE_DONE /*48*/:
                if (dialString.length() == 1) {
                    result = true;
                    break;
                }
                break;
            case HwVSimConstants.EVENT_SET_PREFERRED_NETWORK_TYPE_DONE /*49*/:
                if (dialString.length() <= 2) {
                    result = true;
                    break;
                }
                break;
            case '2':
                if (dialString.length() <= 2) {
                    result = true;
                    break;
                }
                break;
            case HwVSimConstants.EVENT_ENABLE_VSIM_DONE /*51*/:
                if (dialString.length() == 1) {
                    result = true;
                    break;
                }
                break;
            case HwVSimConstants.CMD_DISABLE_VSIM /*52*/:
                if (dialString.length() == 1) {
                    result = true;
                    break;
                }
                break;
            case HwVSimConstants.EVENT_DISABLE_VSIM_DONE /*53*/:
                if (dialString.length() == 1) {
                    result = true;
                    break;
                }
                break;
        }
        return result;
    }

    public void getPOLCapabilty(Message response) {
        this.mPhone.mCi.getPOLCapabilty(response);
    }

    public void getPreferedOperatorList(Message response) {
        this.mPhone.mCi.getCurrentPOLList(response);
    }

    public void setPOLEntry(int index, String numeric, int nAct, Message response) {
        this.mPhone.mCi.setPOLEntry(index, numeric, nAct, response);
    }

    public boolean isCTSimCard(int slotId) {
        return HwTelephonyManagerInner.getDefault().isCTSimCard(slotId);
    }

    public String processPlusSymbol(String dialNumber, String imsi) {
        if (TextUtils.isEmpty(dialNumber) || !dialNumber.startsWith("+") || TextUtils.isEmpty(imsi) || imsi.length() > 15) {
            return dialNumber;
        }
        return PhoneNumberUtils.convertPlusByMcc(dialNumber, Integer.parseInt(imsi.substring(null, 3)));
    }

    public boolean isUtEnable() {
        return ((ImsPhone) this.mPhone.getImsPhone()).mHwImsPhone.isUtEnable();
    }

    public void getOutgoingCallerIdDisplay(Message onComplete) {
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone != null && imsPhone.mHwImsPhone.isUtEnable()) {
            logd("getOutgoingCallerIdDisplay via Ut");
            imsPhone.mHwImsPhone.getOutgoingCallerIdDisplay(onComplete);
        }
    }

    public boolean isSupportCFT() {
        boolean isSupportCFT = false;
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone != null) {
            logd("imsPhone is exist, isSupportCFT");
            isSupportCFT = imsPhone.mHwImsPhone.isSupportCFT();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSupportCFT=");
        stringBuilder.append(isSupportCFT);
        logd(stringBuilder.toString());
        return isSupportCFT;
    }

    public void setCallForwardingUncondTimerOption(int startHour, int startMinute, int endHour, int endMinute, int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, Message onComplete) {
        Message message = onComplete;
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isUtEnable()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCallForwardingUncondTimerOption can not go Ut interface, imsPhone=");
            stringBuilder.append(imsPhone);
            logd(stringBuilder.toString());
            if (message != null) {
                AsyncResult.forMessage(message, null, new CommandException(Error.GENERIC_FAILURE));
                onComplete.sendToTarget();
                return;
            }
            return;
        }
        logd("setCallForwardingUncondTimerOption via Ut");
        imsPhone.mHwImsPhone.setCallForwardingUncondTimerOption(startHour, startMinute, endHour, endMinute, commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, message);
    }

    public void setImsSwitch(boolean on) {
        this.mPhone.mCi.setImsSwitch(on);
    }

    public boolean getImsSwitch() {
        return this.mPhone.mCi.getImsSwitch();
    }

    public void processEccNumber(ServiceStateTracker gSST) {
        if (SystemProperties.getBoolean("ro.config.hw_globalEcc", false)) {
            logd("EVENT_SIM_RECORDS_LOADED!!!!");
            SystemProperties.set(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "usim_present");
            String hplmn = TelephonyManager.getDefault().getSimOperator(this.mPhoneId);
            boolean isRoaming = TelephonyManager.getDefault().isNetworkRoaming(this.mPhoneId);
            String rplmn = gSST.mSS.getOperatorNumeric();
            if (TextUtils.isEmpty(hplmn)) {
                logd("received EVENT_SIM_RECORDS_LOADED but not hplmn !!!!");
            } else if (isRoaming && SystemProperties.getBoolean("ro.config.hw_eccNumUseRplmn", false)) {
                globalEccCustom(rplmn);
            } else {
                globalEccCustom(hplmn);
            }
        }
    }

    public void globalEccCustom(String operatorNumeric) {
        String ecclist_withcard = null;
        String ecclist_nocard = null;
        String forceEccState = SystemProperties.get(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "invalid");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(this.mPhoneId);
        stringBuilder.append("]GECC-globalEccCustom: operator numeric = ");
        stringBuilder.append(operatorNumeric);
        stringBuilder.append("; preOperatorNumeric = ");
        stringBuilder.append(this.preOperatorNumeric);
        stringBuilder.append(";forceEccState  = ");
        stringBuilder.append(forceEccState);
        logd(stringBuilder.toString());
        if (!(TextUtils.isEmpty(operatorNumeric) || (operatorNumeric.equals(this.preOperatorNumeric) && forceEccState.equals("invalid")))) {
            StringBuilder stringBuilder2;
            this.preOperatorNumeric = operatorNumeric;
            SystemProperties.set(PROPERTY_GLOBAL_FORCE_TO_SET_ECC, "invalid");
            if (HwTelephonyFactory.getHwPhoneManager().isSupportEccFormVirtualNet()) {
                ecclist_withcard = HwTelephonyFactory.getHwPhoneManager().getVirtualNetEccWihCard(this.mPhoneId);
                ecclist_nocard = HwTelephonyFactory.getHwPhoneManager().getVirtualNetEccNoCard(this.mPhoneId);
                stringBuilder = new StringBuilder();
                stringBuilder.append("try to get Ecc form virtualNet ecclist_withcard=");
                stringBuilder.append(ecclist_withcard);
                stringBuilder.append(" ecclist_nocard=");
                stringBuilder.append(ecclist_nocard);
                logd(stringBuilder.toString());
            }
            if (virtualNetEccFormCarrier(this.mPhoneId)) {
                int slotId = SubscriptionManager.getSlotIndex(this.mPhoneId);
                try {
                    ecclist_withcard = (String) HwCfgFilePolicy.getValue("virtual_ecclist_withcard", slotId, String.class);
                    ecclist_nocard = (String) HwCfgFilePolicy.getValue("virtual_ecclist_nocard", slotId, String.class);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("try to get Ecc form virtualNet virtual_ecclist from carrier =");
                    stringBuilder2.append(ecclist_withcard);
                    stringBuilder2.append(" ecclist_nocard=");
                    stringBuilder2.append(ecclist_nocard);
                    logd(stringBuilder2.toString());
                } catch (Exception e) {
                    loge("Failed to get ecclist in carrier", e);
                }
            }
            String custEcc = getCustEccList(operatorNumeric);
            if (!TextUtils.isEmpty(custEcc)) {
                String[] custEccArray = custEcc.split(":");
                if (custEccArray.length == 3 && custEccArray[0].equals(operatorNumeric) && !TextUtils.isEmpty(custEccArray[1]) && !TextUtils.isEmpty(custEccArray[2])) {
                    ecclist_withcard = custEccArray[1];
                    ecclist_nocard = custEccArray[2];
                }
            }
            if (ecclist_withcard == null) {
                String where = new StringBuilder();
                where.append("numeric=\"");
                where.append(operatorNumeric);
                where.append("\"");
                Cursor cursor = this.mPhone.getContext().getContentResolver().query(GlobalMatchs.CONTENT_URI, new String[]{"_id", NumMatchs.NAME, "numeric", VirtualNets.ECC_WITH_CARD, VirtualNets.ECC_NO_CARD}, where.toString(), null, NumMatchs.DEFAULT_SORT_ORDER);
                if (cursor == null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[SLOT");
                    stringBuilder3.append(this.mPhoneId);
                    stringBuilder3.append("]GECC-globalEccCustom: No matched emergency numbers in db.");
                    logd(stringBuilder3.toString());
                    this.mPhone.mCi.requestSetEmergencyNumbers("", "");
                    return;
                }
                try {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        ecclist_withcard = cursor.getString(3);
                        ecclist_nocard = cursor.getString(4);
                        cursor.moveToNext();
                    }
                } catch (Exception ex) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("[SLOT");
                    stringBuilder4.append(this.mPhoneId);
                    stringBuilder4.append("]globalEccCustom: global version cause exception!");
                    stringBuilder4.append(ex.toString());
                    logd(stringBuilder4.toString());
                } catch (Throwable th) {
                    cursor.close();
                }
                cursor.close();
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[SLOT");
            stringBuilder2.append(this.mPhoneId);
            stringBuilder2.append("]GECC-globalEccCustom: ecc_withcard = ");
            stringBuilder2.append(ecclist_withcard);
            stringBuilder2.append(", ecc_nocard = ");
            stringBuilder2.append(ecclist_nocard);
            logd(stringBuilder2.toString());
            ecclist_withcard = ecclist_withcard != null ? ecclist_withcard : "";
            ecclist_nocard = ecclist_nocard != null ? ecclist_nocard : "";
            if ((ecclist_withcard == null || ecclist_withcard.equals("")) && (ecclist_nocard == null || ecclist_nocard.equals(""))) {
                this.mPhone.mCi.requestSetEmergencyNumbers("", "");
            } else {
                this.mPhone.mCi.requestSetEmergencyNumbers(ecclist_withcard, ecclist_nocard);
            }
        }
    }

    public String getHwCdmaPrlVersion() {
        int subId = this.mPhone.getSubId();
        String prlVersion = "0";
        int simCardState = TelephonyManager.getDefault().getSimState(subId);
        if (5 == simCardState && HwTelephonyManagerInner.getDefault().isCDMASimCard(subId)) {
            prlVersion = this.mPhone.mCi.getHwPrlVersion();
        } else {
            prlVersion = "0";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwCdmaPrlVersion: prlVersion=");
        stringBuilder.append(prlVersion);
        stringBuilder.append(", subid=");
        stringBuilder.append(subId);
        stringBuilder.append(", simState=");
        stringBuilder.append(simCardState);
        logd(stringBuilder.toString());
        return prlVersion;
    }

    public String getHwCdmaEsn() {
        int subId = this.mPhone.getSubId();
        String esn = "0";
        int simCardState = TelephonyManager.getDefault().getSimState(subId);
        if (5 == simCardState && HwTelephonyManagerInner.getDefault().isCDMASimCard(subId)) {
            esn = this.mPhone.mCi.getHwUimid();
        } else {
            esn = "0";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwCdmaEsn: esn=");
        stringBuilder.append(esn);
        stringBuilder.append(", subid=");
        stringBuilder.append(subId);
        stringBuilder.append(", simState=");
        stringBuilder.append(simCardState);
        logd(stringBuilder.toString());
        return esn;
    }

    public String getVMNumberWhenIMSIChange() {
        if (this.mPhone == null) {
            return null;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        String number = null;
        String mIccId = null;
        if (this.mPhone != null) {
            mIccId = this.mPhone.getIccSerialNumber();
        }
        if (!TextUtils.isEmpty(mIccId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mIccId);
            stringBuilder.append(this.mPhone.getPhoneId());
            String temp = sp.getString(stringBuilder.toString(), null);
            if (temp != null) {
                number = temp;
            }
            logd("getVMNumberWhenIMSIChange number= xxxxxx");
        }
        return number;
    }

    public boolean setISMCOEX(String setISMCoex) {
        this.mPhone.mCi.setISMCOEX(setISMCoex, null);
        return true;
    }

    private String getCustEccList(String operatorNumeric) {
        String custEccList = null;
        String matchEccList = "";
        try {
            custEccList = System.getString(this.mPhone.getContext().getContentResolver(), "hw_cust_emergency_nums");
        } catch (Exception e) {
            loge("Failed to load vmNum from SettingsEx", e);
        }
        if (TextUtils.isEmpty(custEccList) || TextUtils.isEmpty(operatorNumeric)) {
            return matchEccList;
        }
        String[] custEccListItems = custEccList.split(";");
        for (int i = 0; i < custEccListItems.length; i++) {
            String[] custItem = custEccListItems[i].split(":");
            if (custItem.length == 3 && custItem[0].equals(operatorNumeric)) {
                matchEccList = custEccListItems[i];
                break;
            }
        }
        return matchEccList;
    }

    public void setImsDomainConfig(int domainType) {
        this.mPhone.mCi.setImsDomainConfig(domainType, null);
    }

    public void getImsDomain(Message response) {
        this.mPhone.mCi.getImsDomain(response);
    }

    public void handleUiccAuth(int auth_type, byte[] rand, byte[] auth, Message response) {
        this.mPhone.mCi.handleUiccAuth(auth_type, rand, auth, response);
    }

    public void handleMapconImsaReq(byte[] Msg) {
        this.mPhone.mCi.handleMapconImsaReq(Msg, null);
    }

    private void logd(String msg) {
        Rlog.d(this.subTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(this.subTag, msg);
    }

    private void loge(String msg, Throwable tr) {
        Rlog.e(this.subTag, msg, tr);
    }

    public void selectCsgNetworkManually(Message response) {
        if (this.mHwCustHwGSMPhoneReference != null) {
            this.mHwCustHwGSMPhoneReference.selectCsgNetworkManually(response);
        }
    }

    public void judgeToLaunchCsgPeriodicSearchTimer() {
        if (this.mHwCustHwGSMPhoneReference != null) {
            this.mHwCustHwGSMPhoneReference.judgeToLaunchCsgPeriodicSearchTimer();
        }
    }

    public void registerForCsgRecordsLoadedEvent() {
        if (this.mHwCustHwGSMPhoneReference != null) {
            this.mHwCustHwGSMPhoneReference.registerForCsgRecordsLoadedEvent();
        }
    }

    public void unregisterForCsgRecordsLoadedEvent() {
        if (this.mHwCustHwGSMPhoneReference != null) {
            this.mHwCustHwGSMPhoneReference.unregisterForCsgRecordsLoadedEvent();
        }
    }

    public void notifyCellularCommParaReady(int paratype, int pathtype, Message response) {
        this.mPhone.mCi.notifyCellularCommParaReady(paratype, pathtype, response);
    }

    public void updateWfcMode(Context context, boolean roaming, int subId) throws ImsException {
        HwImsManagerInner.updateWfcMode(context, roaming, subId);
    }

    public boolean isDualImsAvailable() {
        return HwImsManagerInner.isDualImsAvailable();
    }

    public boolean isUssdOkForRelease() {
        boolean ussdIsOkForRelease = false;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        if (mContext == null) {
            return false;
        }
        CarrierConfigManager configLoader = (CarrierConfigManager) mContext.getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configLoader != null) {
            b = configLoader.getConfigForSubId(this.mPhone.getSubId());
        }
        if (b != null) {
            ussdIsOkForRelease = b.getBoolean(USSD_IS_OK_FOR_RELEASE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isUssdOkForRelease: ussdIsOkForRelease ");
        stringBuilder.append(ussdIsOkForRelease);
        logd(stringBuilder.toString());
        return ussdIsOkForRelease;
    }
}
