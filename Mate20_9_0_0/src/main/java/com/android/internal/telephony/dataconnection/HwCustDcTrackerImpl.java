package com.android.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.emcom.EmcomManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.PcoData;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Xml;
import com.android.internal.telephony.DctConstants.State;
import com.android.internal.telephony.HwPhoneManager;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwCustDcTrackerImpl extends HwCustDcTracker {
    static final String APNS_OPKEY_CONFIG_FILE = "apns-opkey.xml";
    static final String APN_OPKEY_INFO = "apnOpkeyInfo";
    static final String APN_OPKEY_INFO_APN = "apn";
    static final String APN_OPKEY_INFO_NUMERIC = "numeric";
    static final String APN_OPKEY_INFO_OPKEY = "opkey";
    static final String APN_OPKEY_INFO_USER = "user";
    static final String APN_OPKEY_LAST_IMSI = "apn_opkey_last_imsi";
    static final String APN_OPKEY_LIST_DOCUMENT = "apnOpkeyNodes";
    static final String APN_OPKEY_NODE = "apnOpkeyNode";
    private static final int CAUSE_BY_DATA = 0;
    private static final int CAUSE_BY_ROAM = 1;
    private static final String CUST_ADD_APEC_APN_BUILD = SystemProperties.get("ro.config.add_spec_apn_build", "");
    private static final boolean CUST_ENABLE_OTA_BIP = SystemProperties.getBoolean("ro.config.hw_enable_ota_bip_lgu", false);
    private static final boolean DBG = true;
    private static final String DEFAULT_PCO_DATA = "-2;-2";
    private static final int DEFAULT_PCO_VALUE = -2;
    private static final boolean DISABLE_SIM2_DATA = SystemProperties.getBoolean("ro.config.hw_disable_sim2_data", false);
    private static final String DOCOMO_FOTA_APN = "open-dm2.dcm-dm.ne.jp";
    private static final boolean HW_SIM_ACTIVATION = SystemProperties.getBoolean("ro.config.hw_sim_activation", false);
    private static final int IMS_PCO_TYPE = 1;
    private static final int INTERNET_PCO_TYPE = 3;
    private static final boolean IS_DOCOMO;
    private static final boolean IS_US_CHANNEL;
    private static final boolean IS_VERIZON;
    private static final String KOREA_MCC = "450";
    private static final int OPEN_SERVICE_PDP_CREATE_WAIT_MILLIS = 60000;
    private static final int PCO_CONTENT_LENGTH = 4;
    private static final String PCO_DATA = "pco_data";
    private static final int RADIO_TECH_ALL = 0;
    private static final int RADIO_TECH_GU = 255;
    private static final String SPLIT = ";";
    private static final int SWITCH_ON = 1;
    private static final String TAG = "HwCustDcTrackerImpl";
    private static final String VERIZON_ICCID = "891480";
    private String LGU_PLMN = "45006";
    protected ArrayList<ApnOpkeyInfos> apnOpkeyInfosList = null;
    private boolean hadSentBoardCastToUI = false;
    DcTracker mDcTracker;
    private String mPLMN = "";
    private ContentResolver mResolver;
    private BroadcastReceiver mSimStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && HwCustDcTrackerImpl.this.isValidPhoneParams()) {
                int subId = 0;
                if (intent.getExtra("subscription") != null) {
                    subId = intent.getIntExtra("subscription", -1);
                }
                if (subId != HwCustDcTrackerImpl.this.mSubId.intValue()) {
                    HwCustDcTrackerImpl.this.log("mSimStateChangedReceiver: not current subId, do nothing.");
                } else {
                    HwCustDcTrackerImpl.this.clearApnOpkeyIfNeed(intent.getStringExtra("ss"));
                }
            }
        }
    };
    private Integer mSubId = Integer.valueOf(0);

    private static class ApnOpkeyInfos {
        String apn;
        String opkey;
        String user;

        private ApnOpkeyInfos() {
            this.apn = "";
            this.user = "";
            this.opkey = "";
        }

        /* synthetic */ ApnOpkeyInfos(AnonymousClass1 x0) {
            this();
        }
    }

    static {
        boolean equals = SystemProperties.get("ro.config.hw_opta", "0").equals("341");
        boolean z = DBG;
        equals = (equals && SystemProperties.get("ro.config.hw_optb", "0").equals("392")) ? DBG : false;
        IS_DOCOMO = equals;
        equals = (SystemProperties.get("ro.config.hw_opta", "0").equals("567") && SystemProperties.get("ro.config.hw_optb", "0").equals("840")) ? DBG : false;
        IS_US_CHANNEL = equals;
        if (!(SystemProperties.get("ro.config.hw_opta", "0").equals("389") && SystemProperties.get("ro.config.hw_optb", "0").equals("840"))) {
            z = false;
        }
        IS_VERIZON = z;
    }

    public HwCustDcTrackerImpl(DcTracker dcTracker) {
        super(dcTracker);
        this.mDcTracker = dcTracker;
        if (HW_SIM_ACTIVATION) {
            this.mResolver = this.mDcTracker.mPhone.getContext().getContentResolver();
            Global.putString(this.mResolver, PCO_DATA, DEFAULT_PCO_DATA);
            log("setting default pco values.");
        }
        registerSimStateChangedReceiver();
        if (isValidPhoneParams()) {
            this.mSubId = Integer.valueOf(this.mDcTracker.mPhone.getSubId());
        }
    }

    public boolean apnRoamingAdjust(DcTracker dcTracker, ApnSetting apnSetting, Phone phone) {
        ApnSetting apnSetting2 = apnSetting;
        boolean currentRoaming = phone.getServiceState().getRoaming();
        int radioTech = phone.getServiceState().getRilDataRadioTechnology();
        String operator = dcTracker.getOperatorNumeric();
        String roamingApnStr = System.getString(phone.getContext().getContentResolver(), "hw_customized_roaming_apn");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(phone.getPhoneId());
        stringBuilder.append("]apnRoamingAdjust get hw_customized_roaming_apn: ");
        stringBuilder.append(roamingApnStr);
        Rlog.d(str, stringBuilder.toString());
        if (TextUtils.isEmpty(roamingApnStr)) {
            return DBG;
        }
        String[] roamingApnList = roamingApnStr.split(SPLIT);
        if (roamingApnList.length > 0) {
            String[] roamingApn = null;
            for (int i = 0; i < roamingApnList.length; i++) {
                if (!TextUtils.isEmpty(roamingApnList[i])) {
                    roamingApn = roamingApnList[i].split(":");
                    String mccmnc;
                    if (4 == roamingApn.length) {
                        mccmnc = roamingApn[0];
                        String carrier = roamingApn[1];
                        String apn = roamingApn[2];
                        String roaming = roamingApn[INTERNET_PCO_TYPE];
                        if (operator.equals(mccmnc) && apnSetting2.carrier.equals(carrier) && apnSetting2.apn.equals(apn)) {
                            if (roaming.equals(currentRoaming ? "2" : "1") && (apnSetting2.bearer == 0 || apnSetting2.bearer == radioTech || (14 != radioTech && RADIO_TECH_GU == apnSetting2.bearer))) {
                                return DBG;
                            }
                            return false;
                        }
                    }
                    mccmnc = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[");
                    stringBuilder2.append(phone.getPhoneId());
                    stringBuilder2.append("]apnRoamingAdjust got unsuitable configuration ");
                    stringBuilder2.append(roamingApnList[i]);
                    Rlog.d(mccmnc, stringBuilder2.toString());
                }
            }
        }
        return DBG;
    }

    private String getLguPlmn() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLguPlmn LGU_PLMN is ");
        stringBuilder.append(this.LGU_PLMN);
        log(stringBuilder.toString());
        return this.LGU_PLMN;
    }

    public void checkPLMN(String plmn) {
        if (CUST_ENABLE_OTA_BIP) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkPLMN plmn = ");
            stringBuilder.append(plmn);
            log(stringBuilder.toString());
            String oldPLMN = this.mPLMN;
            this.mPLMN = plmn;
            StringBuilder stringBuilder2;
            if (TextUtils.isEmpty(this.mPLMN) || (this.mPLMN.equals(oldPLMN) && this.hadSentBoardCastToUI)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("checkPLMN newPLMN:");
                stringBuilder2.append(this.mPLMN);
                stringBuilder2.append(" oldPLMN:");
                stringBuilder2.append(oldPLMN);
                stringBuilder2.append(" hadSentBoardCastToUI:");
                stringBuilder2.append(this.hadSentBoardCastToUI);
                log(stringBuilder2.toString());
                return;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("checkPLMN mIsPseudoIMSI = ");
            stringBuilder2.append(getmIsPseudoImsi());
            log(stringBuilder2.toString());
            if (getmIsPseudoImsi()) {
                Intent intent;
                if (this.mPLMN.equals(getLguPlmn())) {
                    intent = new Intent();
                    intent.setAction("com.android.telephony.isopencard");
                    this.mDcTracker.mPhone.getContext().sendBroadcast(intent);
                    log("sendbroadcast OTA_ISOPEN_CARD_ACTION");
                    this.hadSentBoardCastToUI = DBG;
                } else if (!this.mPLMN.startsWith(KOREA_MCC)) {
                    intent = new Intent();
                    intent.setAction("com.android.telephony.roamingpseudo");
                    this.mDcTracker.mPhone.getContext().sendBroadcast(intent);
                    log("sendbroadcast ROAMING_PSEUDO_ACTION");
                    this.hadSentBoardCastToUI = DBG;
                }
            }
        }
    }

    public void onOtaAttachFailed(ApnContext apnContext) {
        if (CUST_ENABLE_OTA_BIP) {
            log("onOtaAttachFailed sendbroadcast OTA_OPEN_SERVICE_ACTION, but Phone.OTARESULT.NETWORKFAIL");
            apnContext.setState(State.FAILED);
            this.mDcTracker.mPhone.notifyDataConnection("apnFailed", apnContext.getApnType());
            apnContext.setDataConnectionAc(null);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.open_service_result");
            intent.putExtra("result_code", 1);
            this.mDcTracker.mPhone.getContext().sendBroadcast(intent);
        }
    }

    public boolean getmIsPseudoImsi() {
        boolean isPseudoIMSI = false;
        if (Integer.parseInt(SystemProperties.get("gsm.sim.card.type", "-1")) == INTERNET_PCO_TYPE) {
            isPseudoIMSI = DBG;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getmIsPSendoIms: mIsPseudoIMSI = ");
        stringBuilder.append(isPseudoIMSI);
        log(stringBuilder.toString());
        return isPseudoIMSI;
    }

    public void sendOTAAttachTimeoutMsg(ApnContext apnContext, boolean retValue) {
        if (CUST_ENABLE_OTA_BIP && "bip0".equals(apnContext.getApnType()) && retValue) {
            log("trySetupData: open service and setupData return true");
            this.mDcTracker.sendMessageDelayed(this.mDcTracker.obtainMessage(270386, apnContext), 60000);
        }
    }

    public void openServiceStart(UiccController uiccController) {
        if (CUST_ENABLE_OTA_BIP) {
            if (uiccController != null && getmIsPseudoImsi()) {
                UiccCard uiccCard = uiccController.getUiccCard(0);
                if (uiccCard != null) {
                    CatService catService = uiccCard.getCatService();
                    if (catService != null) {
                        catService.onOtaCommand(0);
                        log("onDataSetupComplete: Open Service!");
                    } else {
                        log("onDataSetupComplete:catService is null when Open Service!");
                    }
                }
            }
            if (this.mDcTracker.hasMessages(270386)) {
                this.mDcTracker.removeMessages(270386);
            }
        }
    }

    public String getPlmn() {
        if (CUST_ENABLE_OTA_BIP) {
            return this.mPLMN;
        }
        return null;
    }

    private void log(String string) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mSubId);
        stringBuilder.append("]");
        stringBuilder.append(string);
        Rlog.d(str, stringBuilder.toString());
    }

    public boolean isDocomoApn(ApnSetting preferredApn) {
        return (!IS_DOCOMO || (preferredApn != null && "spmode.ne.jp".equals(preferredApn.apn))) ? false : DBG;
    }

    public ApnSetting getDocomoApn(ApnSetting preferredApn) {
        if (!(preferredApn == null || ArrayUtils.contains(preferredApn.types, "dun"))) {
            int len = preferredApn.types.length;
            String[] types = new String[(len + 1)];
            System.arraycopy(preferredApn.types, 0, types, 0, len);
            types[len] = "dun";
            preferredApn.types = types;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDocomoApn end   preferredApn:");
        stringBuilder.append(preferredApn);
        log(stringBuilder.toString());
        return preferredApn;
    }

    public boolean isCanHandleType(ApnSetting apnSetting, String requestedApnType) {
        if (!IS_DOCOMO || apnSetting == null || !"fota".equalsIgnoreCase(requestedApnType) || DOCOMO_FOTA_APN.equals(apnSetting.apn)) {
            return DBG;
        }
        return false;
    }

    public boolean isDocomoTetheringApn(ApnSetting apnSetting, String type) {
        if (IS_DOCOMO && apnSetting != null && "dcmtrg.ne.jp".equals(apnSetting.apn) && ("default".equalsIgnoreCase(type) || "supl".equalsIgnoreCase(type))) {
            return DBG;
        }
        return false;
    }

    public void savePcoData(PcoData pcoData) {
        if (!HW_SIM_ACTIVATION) {
            return;
        }
        if (pcoData.contents == null || pcoData.contents.length != 4) {
            log("pco content illegal,not handle.");
            return;
        }
        String[] pcoValues = getPcoValueFromSetting();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pco setting values is : ");
        stringBuilder.append(Arrays.toString(pcoValues));
        log(stringBuilder.toString());
        String imsPcoValueSetting = pcoValues[null];
        String internetPcoValueSetting = pcoValues[1];
        String imsPcoValue = "";
        String internetPcoValue = "";
        boolean isNeedRefresh = DBG;
        int i = pcoData.cid;
        if (i == 1) {
            imsPcoValue = getPcoValueFromContent(pcoData.contents);
            if (imsPcoValueSetting.equals(imsPcoValue)) {
                isNeedRefresh = false;
            } else {
                imsPcoValueSetting = imsPcoValue;
            }
        } else if (i != INTERNET_PCO_TYPE) {
            isNeedRefresh = false;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("not handle : pco data cid is : ");
            stringBuilder2.append(pcoData.cid);
            log(stringBuilder2.toString());
        } else {
            internetPcoValue = getPcoValueFromContent(pcoData.contents);
            if (internetPcoValueSetting.equals(internetPcoValue)) {
                isNeedRefresh = false;
            } else {
                internetPcoValueSetting = internetPcoValue;
            }
        }
        if (isNeedRefresh) {
            String pcoValue = imsPcoValueSetting.concat(SPLIT).concat(internetPcoValueSetting);
            Global.putString(this.mResolver, PCO_DATA, pcoValue);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("refresh pco setting data is : ");
            stringBuilder3.append(pcoValue);
            log(stringBuilder3.toString());
        } else {
            log("not need to refresh pco data.");
        }
    }

    private String getPcoValueFromContent(byte[] contents) {
        int pcoValue = -2;
        if (contents != null && contents.length == 4) {
            try {
                pcoValue = contents[INTERNET_PCO_TYPE];
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: transform pco value error ");
                stringBuilder.append(e.toString());
                log(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("content pco value is : ");
        stringBuilder2.append(pcoValue);
        log(stringBuilder2.toString());
        return String.valueOf(pcoValue);
    }

    private String[] getPcoValueFromSetting() {
        String[] pcoValues = Global.getString(this.mResolver, PCO_DATA).split(SPLIT);
        if (pcoValues != null && pcoValues.length == 2) {
            return pcoValues;
        }
        return new String[]{String.valueOf(-2), String.valueOf(-2)};
    }

    private boolean isVerizonSim(Context context) {
        if (context == null) {
            return false;
        }
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        String iccid = new StringBuilder();
        iccid.append("");
        iccid.append(tm.getSimSerialNumber());
        return iccid.toString().startsWith(VERIZON_ICCID);
    }

    public boolean isRoamDisallowedByCustomization(ApnContext apnContext) {
        if (apnContext == null || ((!IS_VERIZON && !IS_US_CHANNEL) || !isVerizonSim(this.mDcTracker.mPhone.getContext()) || !"mms".equals(apnContext.getApnType()) || !this.mDcTracker.mPhone.getServiceState().getDataRoaming() || this.mDcTracker.getDataRoamingEnabled())) {
            return false;
        }
        return DBG;
    }

    public void setDataOrRoamOn(int cause) {
        ServiceStateTracker sst = this.mDcTracker.mPhone.getServiceStateTracker();
        if (sst != null && sst.returnObject() != null && sst.returnObject().isDataOffbyRoamAndData()) {
            if (cause == 0) {
                if (SystemProperties.get("gsm.isuser.setdata", "true").equals("true")) {
                    this.mDcTracker.mPhone.mCi.setMobileDataEnable(1, null);
                } else {
                    SystemProperties.set("gsm.isuser.setdata", "true");
                }
            }
            if (cause == 1) {
                this.mDcTracker.mPhone.mCi.setRoamingDataEnable(1, null);
            }
        }
    }

    public boolean isDataDisableBySim2() {
        return DISABLE_SIM2_DATA;
    }

    public boolean addSpecifiedApnSwitch() {
        if (TextUtils.isEmpty(CUST_ADD_APEC_APN_BUILD)) {
            return false;
        }
        return DBG;
    }

    public boolean addSpecifiedApnToWaitingApns(DcTracker dcTracker, ApnSetting preferredApn, ApnSetting apn) {
        if (CUST_ADD_APEC_APN_BUILD.contains(dcTracker.getOperatorNumeric()) && preferredApn != null && preferredApn.carrier.equals(apn.carrier)) {
            return DBG;
        }
        return false;
    }

    public boolean isSmartMpEnable() {
        return EmcomManager.getInstance().isSmartMpEnable();
    }

    private void loge(String s) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mSubId);
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    private void registerSimStateChangedReceiver() {
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        if (isValidPhoneParams()) {
            this.mDcTracker.mPhone.getContext().registerReceiver(this.mSimStateChangedReceiver, filter);
        }
    }

    public void dispose() {
        if (isValidPhoneParams()) {
            if (this.mSimStateChangedReceiver != null) {
                this.mDcTracker.mPhone.getContext().unregisterReceiver(this.mSimStateChangedReceiver);
                this.mSimStateChangedReceiver = null;
            }
            return;
        }
        log("Invalid phone params when dispose, do nothing.");
    }

    private FileInputStream getApnOpkeyCfgFileInputStream() {
        FileInputStream fileIn = null;
        try {
            File confFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/%s", new Object[]{APNS_OPKEY_CONFIG_FILE}), 0);
            if (confFile == null) {
                log("getApnOpkeyCfgFileInputStream: apns-opkey.xml not exists.");
                return null;
            }
            fileIn = new FileInputStream(confFile);
            return fileIn;
        } catch (NoClassDefFoundError e) {
            loge("getApnOpkeyCfgFileInputStream: NoClassDefFoundError occurs.");
        } catch (FileNotFoundException e2) {
            loge("getApnOpkeyCfgFileInputStream: FileNotFoundException occurs.");
        } catch (IOException e3) {
            loge("getApnOpkeyCfgFileInputStream: IOException occurs.");
        } catch (Exception e4) {
            loge("getApnOpkeyCfgFileInputStream: Exception occurs.");
        }
    }

    /* JADX WARNING: Missing block: B:39:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:52:0x012f, code skipped:
            loge("loadMatchedApnOpkeyList: fileInputStream  close error");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadMatchedApnOpkeyList(String numeric) {
        if (this.apnOpkeyInfosList != null) {
            log("loadMatchedApnOpkeyList:load ApnOpkeyList only once for same SIM card, return;");
        } else if (TextUtils.isEmpty(numeric)) {
            loge("loadMatchedApnOpkeyList: numeric is null, return.");
        } else {
            FileInputStream fileIn = getApnOpkeyCfgFileInputStream();
            if (fileIn == null) {
                loge("loadMatchedApnOpkeyList: fileIn is null, retrun.");
                return;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fileIn, null);
                XmlUtils.beginDocument(parser, APN_OPKEY_LIST_DOCUMENT);
                while (true) {
                    int next = parser.next();
                    int eventType = next;
                    if (1 != next) {
                        if (2 == eventType && APN_OPKEY_NODE.equalsIgnoreCase(parser.getName())) {
                            String apnOpkeyInfoNumeric = parser.getAttributeValue(null, APN_OPKEY_INFO_NUMERIC);
                            if (numeric.equals(apnOpkeyInfoNumeric)) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("loadMatchedApnOpkeyList: parser ");
                                stringBuilder.append(parser.getName());
                                stringBuilder.append(" begin, numeric=");
                                stringBuilder.append(numeric);
                                stringBuilder.append(", apnOpkeyInfoNumeric=");
                                stringBuilder.append(apnOpkeyInfoNumeric);
                                log(stringBuilder.toString());
                                this.apnOpkeyInfosList = new ArrayList();
                                while (true) {
                                    int next2 = parser.next();
                                    eventType = next2;
                                    if (1 == next2) {
                                        break;
                                    }
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("loadMatchedApnOpkeyList: parser ");
                                    stringBuilder.append(parser.getName());
                                    stringBuilder.append(", eventType=");
                                    stringBuilder.append(eventType);
                                    log(stringBuilder.toString());
                                    if (2 != eventType || !APN_OPKEY_INFO.equalsIgnoreCase(parser.getName())) {
                                        if (INTERNET_PCO_TYPE == eventType && APN_OPKEY_NODE.equalsIgnoreCase(parser.getName())) {
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("loadMatchedApnOpkeyList: parser ");
                                            stringBuilder2.append(parser.getName());
                                            stringBuilder2.append(" end.");
                                            log(stringBuilder2.toString());
                                            break;
                                        }
                                        log("loadMatchedApnOpkeyList: skip this line node.");
                                    } else {
                                        ApnOpkeyInfos apnOpkeyInfos = new ApnOpkeyInfos();
                                        apnOpkeyInfos.apn = parser.getAttributeValue(null, APN_OPKEY_INFO_APN);
                                        apnOpkeyInfos.user = parser.getAttributeValue(null, APN_OPKEY_INFO_USER);
                                        apnOpkeyInfos.opkey = parser.getAttributeValue(null, APN_OPKEY_INFO_OPKEY);
                                        this.apnOpkeyInfosList.add(apnOpkeyInfos);
                                    }
                                }
                                log("loadMatchedApnOpkeyList: parser config xml end.");
                            }
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                loge("loadMatchedApnOpkeyList: Some errors occur when parsering apns-opkey.xml");
                fileIn.close();
            } catch (IOException e2) {
                loge("loadMatchedApnOpkeyList: Can't find apns-opkey.ml.");
                fileIn.close();
            } catch (Throwable th) {
                try {
                    fileIn.close();
                } catch (IOException e3) {
                    loge("loadMatchedApnOpkeyList: fileInputStream  close error");
                }
                throw th;
            }
        }
    }

    public boolean isValidPhoneParams() {
        return (this.mDcTracker == null || this.mDcTracker.mPhone == null || this.mDcTracker.mPhone.getContext() == null) ? false : DBG;
    }

    public String getOpKeyByActivedApn(String activedNumeric, String activedApn, String activedUser) {
        if (TextUtils.isEmpty(activedNumeric)) {
            loge("getOpKeyByActivedApn: numeric is null, return.");
            return null;
        }
        loadMatchedApnOpkeyList(activedNumeric);
        if (this.apnOpkeyInfosList != null && this.apnOpkeyInfosList.size() > 0) {
            int i = 0;
            int size = this.apnOpkeyInfosList.size();
            while (i < size) {
                ApnOpkeyInfos avInfo = (ApnOpkeyInfos) this.apnOpkeyInfosList.get(i);
                if (avInfo.apn == null || !avInfo.apn.equals(activedApn) || avInfo.user == null || !avInfo.user.equals(activedUser)) {
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getOpKeyByActivedApn: return matched apn opkey = ");
                    stringBuilder.append(avInfo.opkey);
                    log(stringBuilder.toString());
                    return avInfo.opkey;
                }
            }
        }
        return null;
    }

    public void setApnOpkeyToSettingsDB(String activedApnOpkey) {
        if (isValidPhoneParams()) {
            ContentResolver contentResolver = this.mDcTracker.mPhone.getContext().getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sim_apn_opkey");
            stringBuilder.append(this.mSubId);
            System.putString(contentResolver, stringBuilder.toString(), activedApnOpkey);
        }
    }

    private void clearApnOpkeyIfNeed(String simState) {
        boolean needClearApnOpkey = false;
        if ("ABSENT".equals(simState)) {
            needClearApnOpkey = DBG;
        } else if ("IMSI".equals(simState)) {
            if (!isValidPhoneParams() || this.mDcTracker.mPhone.mIccRecords == null || this.mDcTracker.mPhone.mIccRecords.get() == null) {
                needClearApnOpkey = DBG;
            } else {
                String currentImsi = ((IccRecords) this.mDcTracker.mPhone.mIccRecords.get()).getIMSI();
                String oldImsi = HwTelephonyFactory.getHwPhoneManager();
                Context context = this.mDcTracker.mPhone.getContext();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(APN_OPKEY_LAST_IMSI);
                stringBuilder.append(this.mSubId);
                oldImsi = oldImsi.decryptInfo(context, stringBuilder.toString());
                if (currentImsi == null) {
                    log("clearApnOpkeyIfNeed: currentImsi is null, maybe error occurs, clear apn opkey.");
                    needClearApnOpkey = DBG;
                } else if (!currentImsi.equals(oldImsi)) {
                    log("clearApnOpkeyIfNeed: diffrent SIM card, clear apn opkey.");
                    needClearApnOpkey = DBG;
                    HwPhoneManager hwPhoneManager = HwTelephonyFactory.getHwPhoneManager();
                    Context context2 = this.mDcTracker.mPhone.getContext();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(APN_OPKEY_LAST_IMSI);
                    stringBuilder2.append(this.mSubId);
                    hwPhoneManager.encryptInfo(context2, currentImsi, stringBuilder2.toString());
                }
            }
        }
        if (needClearApnOpkey) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("clearApnOpkeyIfNeed: INTENT_VALUE_ICC_");
            stringBuilder3.append(simState);
            stringBuilder3.append(", clear APN opkey.");
            log(stringBuilder3.toString());
            setApnOpkeyToSettingsDB(null);
            this.apnOpkeyInfosList = null;
        }
    }
}
