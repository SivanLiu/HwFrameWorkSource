package com.android.internal.telephony.uicc;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.encrypt.PasswordUtil;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccRecords.IccRecordLoaded;
import com.android.internal.telephony.uicc.UsimServiceTable.UsimService;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SIMRecords extends IccRecords {
    private static final /* synthetic */ int[] -com-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues = null;
    static final int CFF_LINE1_MASK = 15;
    static final int CFF_LINE1_RESET = 240;
    static final int CFF_UNCONDITIONAL_ACTIVE = 10;
    static final int CFF_UNCONDITIONAL_DEACTIVE = 5;
    private static final int CFIS_ADN_CAPABILITY_ID_OFFSET = 14;
    private static final int CFIS_ADN_EXTENSION_ID_OFFSET = 15;
    private static final int CFIS_BCD_NUMBER_LENGTH_OFFSET = 2;
    private static final int CFIS_TON_NPI_OFFSET = 3;
    public static final String CF_ENABLED = "cf_enabled_key";
    private static final int CPHS_SST_MBN_ENABLED = 48;
    private static final int CPHS_SST_MBN_MASK = 48;
    private static final boolean CRASH_RIL = false;
    private static final int EVENT_APP_LOCKED = 258;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 257;
    private static final int EVENT_GET_AD_DONE = 9;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    private static final int EVENT_GET_CFF_DONE = 24;
    private static final int EVENT_GET_CFIS_DONE = 32;
    private static final int EVENT_GET_CPHS_MAILBOX_DONE = 11;
    private static final int EVENT_GET_CSP_CPHS_DONE = 33;
    private static final int EVENT_GET_EHPLMN_DONE = 40;
    private static final int EVENT_GET_FPLMN_DONE = 41;
    private static final int EVENT_GET_GID1_DONE = 34;
    private static final int EVENT_GET_GID2_DONE = 36;
    private static final int EVENT_GET_HPLMN_W_ACT_DONE = 39;
    private static final int EVENT_GET_ICCID_DONE = 4;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_INFO_CPHS_DONE = 26;
    private static final int EVENT_GET_MBDN_DONE = 6;
    private static final int EVENT_GET_MBI_DONE = 5;
    private static final int EVENT_GET_MSISDN_DONE = 10;
    private static final int EVENT_GET_MWIS_DONE = 7;
    private static final int EVENT_GET_OPLMN_W_ACT_DONE = 38;
    private static final int EVENT_GET_PLMN_W_ACT_DONE = 37;
    private static final int EVENT_GET_PNN_DONE = 15;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SPDI_DONE = 13;
    private static final int EVENT_GET_SPN_DONE = 12;
    private static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25;
    private static final int EVENT_SET_MBDN_DONE = 20;
    private static final int EVENT_SET_MSISDN_DONE = 30;
    private static final int EVENT_SIM_REFRESH = 259;
    private static final int EVENT_SMS_ON_SIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    private static final boolean IS_MODEM_CAPABILITY_GET_ICCID_AT = HwModemCapability.isCapabilitySupport(19);
    protected static final String LOG_TAG = "SIMRecords";
    private static final String[] MCCMNC_CODES_HAVING_2DIGITS_MNC = new String[]{"40400", "40401", "40402", "40403", "40404", "40405", "40407", "40409", "40410", "40411", "40412", "40413", "40414", "40415", "40416", "40417", "40418", "40419", "40420", "40421", "40422", "40424", "40425", "40427", "40428", "40429", "40430", "40431", "40433", "40434", "40435", "40436", "40437", "40438", "40440", "40441", "40442", "40443", "40444", "40445", "40446", "40449", "40450", "40451", "40452", "40453", "40454", "40455", "40456", "40457", "40458", "40459", "40460", "40462", "40464", "40466", "40467", "40468", "40469", "40470", "40471", "40472", "40473", "40474", "40475", "40476", "40477", "40478", "40479", "40480", "40481", "40482", "40483", "40484", "40485", "40486", "40487", "40488", "40489", "40490", "40491", "40492", "40493", "40494", "40495", "40496", "40497", "40498", "40501", "40505", "40506", "40507", "40508", "40509", "40510", "40511", "40512", "40513", "40514", "40515", "40517", "40518", "40519", "40520", "40521", "40522", "40523", "40524", "40548", "40551", "40552", "40553", "40554", "40555", "40556", "40566", "40567", "40570", "23210"};
    private static final String[] MCCMNC_CODES_HAVING_2DIGITS_MNC_ZERO_PREFIX_RELIANCE = new String[]{"40503", "40504"};
    private static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = new String[]{"302370", "302720", SimulatedCommands.FAKE_MCC_MNC, "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032", "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040", "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750", "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800", "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808", "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816", "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824", "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832", "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840", "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848", "405849", "405850", "405851", "405852", "405853", "405854", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874", "405875", "405876", "405877", "405878", "405879", "405880", "405881", "405882", "405883", "405884", "405885", "405886", "405908", "405909", "405910", "405911", "405912", "405913", "405914", "405915", "405916", "405917", "405918", "405919", "405920", "405921", "405922", "405923", "405924", "405925", "405926", "405927", "405928", "405929", "405930", "405931", "405932", "502142", "502143", "502145", "502146", "502147", "502148"};
    private static final boolean NEED_CHECK_MSISDN_BIT = SystemProperties.getBoolean("ro.config.hw_check_msisdn", false);
    public static final String SIM_IMSI = "sim_imsi_key";
    private static final int SIM_RECORD_EVENT_BASE = 0;
    private static final int SYSTEM_EVENT_BASE = 256;
    static final int TAG_FULL_NETWORK_NAME = 67;
    static final int TAG_SHORT_NETWORK_NAME = 69;
    static final int TAG_SPDI = 163;
    static final int TAG_SPDI_PLMN_LIST = 128;
    private static final boolean VDBG = false;
    public static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    private static PasswordUtil mPasswordUtil = HwFrameworkFactory.getPasswordUtil();
    private int mCallForwardingStatus;
    private byte[] mCphsInfo;
    boolean mCspPlmnEnabled;
    byte[] mEfCPHS_MWI;
    byte[] mEfCff;
    byte[] mEfCfis;
    byte[] mEfLi;
    byte[] mEfMWIS;
    byte[] mEfPl;
    private String mFirstImsi;
    private HwCustSIMRecords mHwCustSIMRecords;
    private String mOriginVmImsi;
    String mPnnHomeName;
    private final BroadcastReceiver mReceiver;
    private String mSecondImsi;
    ArrayList<String> mSpdiNetworks;
    int mSpnDisplayCondition;
    SpnOverride mSpnOverride;
    private GetSpnFsmState mSpnState;
    UsimServiceTable mUsimServiceTable;
    VoiceMailConstants mVmConfig;
    private HashMap<String, Integer> sEventIdMap;

    private class EfPlLoaded implements IccRecordLoaded {
        private EfPlLoaded() {
        }

        public String getEfName() {
            return "EF_PL";
        }

        public void onRecordLoaded(AsyncResult ar) {
            SIMRecords.this.mEfPl = (byte[]) ar.result;
            SIMRecords.this.log("EF_PL=" + IccUtils.bytesToHexString(SIMRecords.this.mEfPl));
        }
    }

    private class EfUsimLiLoaded implements IccRecordLoaded {
        private EfUsimLiLoaded() {
        }

        public String getEfName() {
            return "EF_LI";
        }

        public void onRecordLoaded(AsyncResult ar) {
            SIMRecords.this.mEfLi = (byte[]) ar.result;
            SIMRecords.this.log("EF_LI=" + IccUtils.bytesToHexString(SIMRecords.this.mEfLi));
        }
    }

    private enum GetSpnFsmState {
        IDLE,
        INIT,
        READ_SPN_3GPP,
        READ_SPN_CPHS,
        READ_SPN_SHORT_CPHS
    }

    private static /* synthetic */ int[] -getcom-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues() {
        if (-com-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues != null) {
            return -com-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues;
        }
        int[] iArr = new int[GetSpnFsmState.values().length];
        try {
            iArr[GetSpnFsmState.IDLE.ordinal()] = 5;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[GetSpnFsmState.INIT.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_3GPP.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_CPHS.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_SHORT_CPHS.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        -com-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues = iArr;
        return iArr;
    }

    public String toString() {
        return "SimRecords: " + super.toString() + " mVmConfig" + this.mVmConfig + " mSpnOverride=" + this.mSpnOverride + " callForwardingEnabled=" + this.mCallForwardingStatus + " spnState=" + this.mSpnState + " mCphsInfo=" + this.mCphsInfo + " mCspPlmnEnabled=" + this.mCspPlmnEnabled + " efMWIS=" + this.mEfMWIS + " efCPHS_MWI=" + this.mEfCPHS_MWI + " mEfCff=" + this.mEfCff + " mEfCfis=" + this.mEfCfis + " getOperatorNumeric=" + getOperatorNumeric();
    }

    public SIMRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mCphsInfo = null;
        this.mCspPlmnEnabled = true;
        this.mEfMWIS = null;
        this.mEfCPHS_MWI = null;
        this.mEfCff = null;
        this.mEfCfis = null;
        this.mEfLi = null;
        this.mEfPl = null;
        this.mSpdiNetworks = null;
        this.mPnnHomeName = null;
        this.mHwCustSIMRecords = null;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    SIMRecords.this.sendMessage(SIMRecords.this.obtainMessage(257));
                }
            }
        };
        this.sEventIdMap = new HashMap();
        this.mAdnCache = HwTelephonyFactory.getHwUiccManager().createHwAdnRecordCache(this.mFh);
        this.mVmConfig = (VoiceMailConstants) HwTelephonyFactory.getHwUiccManager().createHwVoiceMailConstants(c, getSlotId());
        this.mSpnOverride = new SpnOverride();
        this.mRecordsRequested = false;
        this.mRecordsToLoad = 0;
        this.mCi.setOnSmsOnSim(this, 21, null);
        this.mCi.registerForIccRefresh(this, 259, null);
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 258, null);
        log("SIMRecords X ctor this=" + this);
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        c.registerReceiver(this.mReceiver, intentfilter);
    }

    public void dispose() {
        log("Disposing SIMRecords this=" + this);
        this.mCi.unregisterForIccRefresh(this);
        this.mCi.unSetOnSmsOnSim(this);
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        resetRecords();
        super.dispose();
    }

    protected void finalize() {
        log("finalized");
    }

    protected void resetRecords() {
        this.mImsi = null;
        this.mMsisdn = null;
        this.mVoiceMailNum = null;
        this.mMncLength = -1;
        log("setting0 mMncLength" + this.mMncLength);
        this.mIccId = null;
        this.mFullIccId = null;
        this.mSpnDisplayCondition = -1;
        this.mEfMWIS = null;
        this.mEfCPHS_MWI = null;
        this.mSpdiNetworks = null;
        this.mPnnHomeName = null;
        this.mGid1 = null;
        this.mGid2 = null;
        this.mPlmnActRecords = null;
        this.mOplmnActRecords = null;
        this.mHplmnActRecords = null;
        this.mFplmns = null;
        this.mEhplmns = null;
        this.mAdnCache.reset();
        log("SIMRecords: onRadioOffOrNotAvailable set 'gsm.sim.operator.numeric' to operator=null");
        log("update icc_operator_numeric=" + null);
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), "");
        this.mRecordsRequested = false;
        this.mImsiLoad = false;
    }

    public String getMsisdnNumber() {
        if (!NEED_CHECK_MSISDN_BIT || this.mUsimServiceTable == null || (this.mUsimServiceTable.isAvailable(UsimService.MSISDN) ^ 1) == 0) {
            return this.mMsisdn;
        }
        log("EF_MSISDN not available");
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return this.mUsimServiceTable;
    }

    private int getExtFromEf(int ef) {
        switch (ef) {
            case IccConstants.EF_MSISDN /*28480*/:
                if (this.mParentApp.getType() == AppType.APPTYPE_USIM) {
                    return IccConstants.EF_EXT5;
                }
                return IccConstants.EF_EXT1;
            default:
                return IccConstants.EF_EXT1;
        }
    }

    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        this.mNewMsisdn = number;
        this.mNewMsisdnTag = alphaTag;
        log("Set MSISDN: " + this.mNewMsisdnTag + " " + Rlog.pii(LOG_TAG, this.mNewMsisdn));
        new AdnRecordLoader(this.mFh).updateEF(new AdnRecord(this.mNewMsisdnTag, this.mNewMsisdn), IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, null, obtainMessage(30, onComplete));
    }

    public String getMsisdnAlphaTag() {
        if (!NEED_CHECK_MSISDN_BIT || this.mUsimServiceTable == null || (this.mUsimServiceTable.isAvailable(UsimService.MSISDN) ^ 1) == 0) {
            return this.mMsisdnTag;
        }
        log("EF_MSISDN not available");
        return null;
    }

    public String getVoiceMailNumber() {
        if (this.mHwCustSIMRecords == null || !this.mHwCustSIMRecords.isOpenRoamingVoiceMail()) {
            return this.mVoiceMailNum;
        }
        return this.mHwCustSIMRecords.getRoamingVoicemail();
    }

    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
        if (this.mIsVoiceMailFixed) {
            AsyncResult.forMessage(onComplete).exception = new IccVmFixedException("Voicemail number is fixed by operator");
            onComplete.sendToTarget();
            return;
        }
        this.mNewVoiceMailNum = voiceNumber;
        this.mNewVoiceMailTag = alphaTag;
        if (checkVMEditToSim()) {
            AdnRecord adn = new AdnRecord(this.mNewVoiceMailTag, this.mNewVoiceMailNum);
            if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
                new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, null, obtainMessage(20, onComplete));
            } else if (isCphsMailboxEnabled()) {
                new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, onComplete));
            } else {
                AsyncResult.forMessage(onComplete).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
                onComplete.sendToTarget();
            }
            return;
        }
        log("Don't need to edit to SIM");
        AsyncResult.forMessage(onComplete).exception = new IccVmNotSupportedException("Voicemail number can't be edit to SIM");
        onComplete.sendToTarget();
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
        int i = 0;
        if (line == 1) {
            try {
                if (this.mEfMWIS != null) {
                    byte[] bArr = this.mEfMWIS;
                    int i2 = this.mEfMWIS[0] & 254;
                    if (countWaiting != 0) {
                        i = 1;
                    }
                    bArr[0] = (byte) (i | i2);
                    if (countWaiting < 0) {
                        this.mEfMWIS[1] = (byte) 0;
                    } else {
                        this.mEfMWIS[1] = (byte) countWaiting;
                    }
                    this.mFh.updateEFLinearFixed(IccConstants.EF_MWIS, 1, this.mEfMWIS, null, obtainMessage(14, IccConstants.EF_MWIS, 0));
                }
                if (this.mEfCPHS_MWI != null) {
                    this.mEfCPHS_MWI[0] = (byte) ((countWaiting == 0 ? 5 : 10) | (this.mEfCPHS_MWI[0] & 240));
                    this.mFh.updateEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, this.mEfCPHS_MWI, obtainMessage(14, Integer.valueOf(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS)));
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                logw("Error saving voice mail state to SIM. Probably malformed SIM record", ex);
            }
        }
    }

    private boolean validEfCfis(byte[] data) {
        if (data == null) {
            return false;
        }
        if (data[0] < (byte) 1 || data[0] > (byte) 4) {
            logw("MSP byte: " + data[0] + " is not between 1 and 4", null);
        }
        return true;
    }

    public int getVoiceMessageCount() {
        int countVoiceMessages = -2;
        if (this.mEfMWIS != null) {
            countVoiceMessages = this.mEfMWIS[1] & 255;
            if (((this.mEfMWIS[0] & 1) != 0) && (countVoiceMessages == 0 || countVoiceMessages == 255)) {
                countVoiceMessages = -1;
            }
            log(" VoiceMessageCount from SIM MWIS = " + countVoiceMessages);
        } else if (this.mEfCPHS_MWI != null) {
            int indicator = this.mEfCPHS_MWI[0] & 15;
            if (indicator == 10) {
                countVoiceMessages = -1;
            } else if (indicator == 5) {
                countVoiceMessages = 0;
            }
            log(" VoiceMessageCount from SIM CPHS = " + countVoiceMessages);
        }
        return countVoiceMessages;
    }

    public int getVoiceCallForwardingFlag() {
        return this.mCallForwardingStatus;
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String dialNumber) {
        int i = 0;
        if (line == 1) {
            if (enable) {
                i = 1;
            }
            this.mCallForwardingStatus = i;
            this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(1));
            try {
                if (validEfCfis(this.mEfCfis)) {
                    byte[] bArr;
                    if (enable) {
                        bArr = this.mEfCfis;
                        bArr[1] = (byte) (bArr[1] | 1);
                    } else {
                        bArr = this.mEfCfis;
                        bArr[1] = (byte) (bArr[1] & 254);
                    }
                    log("setVoiceCallForwardingFlag: enable=" + enable + " mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
                    if (enable && (TextUtils.isEmpty(dialNumber) ^ 1) != 0) {
                        logv("EF_CFIS: updating cf number, " + Rlog.pii(LOG_TAG, dialNumber));
                        byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(dialNumber);
                        System.arraycopy(bcdNumber, 0, this.mEfCfis, 3, bcdNumber.length);
                        this.mEfCfis[2] = (byte) bcdNumber.length;
                        this.mEfCfis[14] = (byte) -1;
                        this.mEfCfis[15] = (byte) -1;
                    }
                    this.mFh.updateEFLinearFixed(IccConstants.EF_CFIS, 1, this.mEfCfis, null, obtainMessage(14, Integer.valueOf(IccConstants.EF_CFIS)));
                } else {
                    log("setVoiceCallForwardingFlag: ignoring enable=" + enable + " invalid mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
                    setCallForwardingPreference(enable);
                }
                if (this.mEfCff != null) {
                    if (enable) {
                        this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 10);
                    } else {
                        this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 5);
                    }
                    this.mFh.updateEFTransparent(IccConstants.EF_CFF_CPHS, this.mEfCff, obtainMessage(14, Integer.valueOf(IccConstants.EF_CFF_CPHS)));
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                logw("Error saving call forwarding flag to SIM. Probably malformed SIM record", ex);
            }
        }
    }

    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            fetchSimRecords();
        }
    }

    public String getOperatorNumeric() {
        String imsi = getIMSI();
        if (this.mImsi == null) {
            log("IMSI == null");
            return null;
        } else if (this.mImsi.length() < 6 || this.mImsi.length() > 15) {
            Rlog.e(LOG_TAG, "invalid IMSI ");
            return null;
        } else if (this.mMncLength == -1 || this.mMncLength == 0) {
            log("getSIMOperatorNumeric: bad mncLength");
            if (this.mImsi.length() >= 5) {
                String mcc = this.mImsi.substring(0, 3);
                if (mcc.equals("404") || mcc.equals("405") || mcc.equals("232")) {
                    String mccmncCode = this.mImsi.substring(0, 5);
                    for (String mccmnc : MCCMNC_CODES_HAVING_2DIGITS_MNC) {
                        if (mccmnc.equals(mccmncCode)) {
                            this.mMncLength = 2;
                            return this.mImsi.substring(0, this.mMncLength + 3);
                        }
                    }
                }
            }
            return null;
        } else if (imsi.length() >= this.mMncLength + 3) {
            return imsi.substring(0, this.mMncLength + 3);
        } else {
            return null;
        }
    }

    public void handleMessage(android.os.Message r34) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.internal.telephony.uicc.SIMRecords.handleMessage(android.os.Message):void. bs: [B:98:0x03ba, B:240:0x07ba]
	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:86)
	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r33 = this;
        r21 = 0;
        r0 = r33;
        r2 = r0.mDestroyed;
        r2 = r2.get();
        if (r2 == 0) goto L_0x0031;
    L_0x000c:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r4 = "Received message[";
        r2 = r2.append(r4);
        r0 = r34;
        r4 = r0.what;
        r2 = r2.append(r4);
        r4 = "], Ignoring.";
        r2 = r2.append(r4);
        r2 = r2.toString();
        r0 = r33;
        r0.loge(r2);
        return;
    L_0x0031:
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.what;	 Catch:{ RuntimeException -> 0x0045 }
        switch(r2) {
            case 1: goto L_0x0041;
            case 3: goto L_0x005f;
            case 4: goto L_0x0344;
            case 5: goto L_0x0065;
            case 6: goto L_0x00f9;
            case 7: goto L_0x0270;
            case 8: goto L_0x02f4;
            case 9: goto L_0x03a2;
            case 10: goto L_0x01e6;
            case 11: goto L_0x00f9;
            case 12: goto L_0x11f5;
            case 13: goto L_0x12c2;
            case 14: goto L_0x12d9;
            case 15: goto L_0x12ef;
            case 17: goto L_0x1409;
            case 18: goto L_0x132e;
            case 19: goto L_0x1345;
            case 20: goto L_0x1498;
            case 21: goto L_0x1365;
            case 22: goto L_0x13d5;
            case 24: goto L_0x1288;
            case 25: goto L_0x1576;
            case 26: goto L_0x1461;
            case 30: goto L_0x0233;
            case 32: goto L_0x1601;
            case 33: goto L_0x174a;
            case 34: goto L_0x17ac;
            case 36: goto L_0x1805;
            case 37: goto L_0x185e;
            case 38: goto L_0x18b4;
            case 39: goto L_0x190a;
            case 40: goto L_0x1981;
            case 41: goto L_0x19bf;
            case 257: goto L_0x1a38;
            case 258: goto L_0x0054;
            case 259: goto L_0x15cf;
            default: goto L_0x0038;
        };	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0038:
        super.handleMessage(r34);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x003b:
        if (r21 == 0) goto L_0x0040;
    L_0x003d:
        r33.onRecordLoaded();
    L_0x0040:
        return;
    L_0x0041:
        r33.onReady();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;
    L_0x0045:
        r14 = move-exception;
        r2 = "Exception parsing SIM record";	 Catch:{ all -> 0x0058 }
        r0 = r33;	 Catch:{ all -> 0x0058 }
        r0.logw(r2, r14);	 Catch:{ all -> 0x0058 }
        if (r21 == 0) goto L_0x0040;
    L_0x0050:
        r33.onRecordLoaded();
        goto L_0x0040;
    L_0x0054:
        r33.onLocked();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;
    L_0x0058:
        r2 = move-exception;
        if (r21 == 0) goto L_0x005e;
    L_0x005b:
        r33.onRecordLoaded();
    L_0x005e:
        throw r2;
    L_0x005f:
        r21 = 1;
        r33.onGetImsiDone(r34);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0065:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r22 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x00b5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0077:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_MBI: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 & 255;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMailboxIndex = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMailboxIndex;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x00b5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00a3:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMailboxIndex;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x00b5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00ab:
        r2 = "Got valid mailbox number for MBDN";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r22 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00b5:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mRecordsToLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mRecordsToLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r22 == 0) goto L_0x00de;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00c1:
        r2 = new com.android.internal.telephony.uicc.AdnRecordLoader;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMailboxIndex;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.obtainMessage(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 28615; // 0x6fc7 float:4.0098E-41 double:1.41377E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 28616; // 0x6fc8 float:4.01E-41 double:1.4138E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r2.loadFromEF(r6, r7, r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00de:
        r2 = new com.android.internal.telephony.uicc.AdnRecordLoader;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 11;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.obtainMessage(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 28439; // 0x6f17 float:3.9852E-41 double:1.40507E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 28490; // 0x6f4a float:3.9923E-41 double:1.4076E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r2.loadFromEF(r5, r6, r7, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x00f9:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailNum = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0163;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x010f:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Invalid or missing EF";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.what;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 11;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r5) goto L_0x015f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0123:
        r2 = "[MAILBOX]";	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0126:
        r2 = r4.append(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.what;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x013a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mRecordsToLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mRecordsToLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new com.android.internal.telephony.uicc.AdnRecordLoader;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 11;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.obtainMessage(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 28439; // 0x6f17 float:3.9852E-41 double:1.40507E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 28490; // 0x6f4a float:3.9923E-41 double:1.4076E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r2.loadFromEF(r5, r6, r7, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x015f:
        r2 = "[MBDN]";	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0126;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0163:
        r3 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r3 = (com.android.internal.telephony.uicc.AdnRecord) r3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "VM: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r2.append(r3);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.what;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 11;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r5) goto L_0x01c1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x017f:
        r2 = " EF[MAILBOX]";	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0182:
        r2 = r4.append(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r3.isEmpty();	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x01c5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0195:
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.what;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x01c5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x019c:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mRecordsToLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mRecordsToLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new com.android.internal.telephony.uicc.AdnRecordLoader;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 11;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.obtainMessage(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 28439; // 0x6f17 float:3.9852E-41 double:1.40507E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 28490; // 0x6f4a float:3.9923E-41 double:1.4076E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r2.loadFromEF(r5, r6, r7, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x01c1:
        r2 = " EF[MBDN]";	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0182;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x01c5:
        r2 = r3.getNumber();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailNum = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r3.getAlphaTag();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mVmConfig;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mVoiceMailNum;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mVoiceMailTag;	 Catch:{ RuntimeException -> 0x0045 }
        r2.setVoicemailOnSIM(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x01e6:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x01fc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x01f2:
        r2 = "Invalid or missing EF[MSISDN]";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x01fc:
        r3 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r3 = (com.android.internal.telephony.uicc.AdnRecord) r3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r3.getNumber();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMsisdn = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r3.getAlphaTag();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMsisdnTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "MSISDN isempty:";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMsisdn;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = android.text.TextUtils.isEmpty(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0233:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0257;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x023f:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewMsisdn;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMsisdn = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewMsisdnTag;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMsisdnTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Success to update EF[MSISDN]";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0257:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x025b:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.os.AsyncResult.forMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2.exception = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2.sendToTarget();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0270:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_MWIS : ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x02ba;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x029d:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EVENT_GET_MWIS_DONE exception = ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02ba:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 & 255;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 255; // 0xff float:3.57E-43 double:1.26E-321;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x02cd;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02c3:
        r2 = "SIMRecords: Uninitialized record MWIS";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02cd:
        r2 = "ro.config.hw_eeVoiceMsgCount";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r19 = android.os.SystemProperties.getBoolean(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r19 == 0) goto L_0x02ee;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02d7:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 & 255;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x02ee;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02de:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfMWIS = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "SIMRecords";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "SIMRecords EE VoiceMessageCount from SIM CPHS";	 Catch:{ RuntimeException -> 0x0045 }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02ee:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfMWIS = r11;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x02f4:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CPHS_MWI: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x033e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0321:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE exception = ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x033e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfCPHS_MWI = r11;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0344:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x035d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0354:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mIccIDLoadRegistrants;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyRegistrants(r9);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x035d:
        r2 = com.android.internal.telephony.HwTelephonyFactory.getHwUiccManager();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r11.length;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.bcdIccidToString(r11, r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mIccId = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11.length;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = com.android.internal.telephony.uicc.IccUtils.bchToString(r11, r4, r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mFullIccId = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r33.onIccIdLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "iccid: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFullIccId;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = android.telephony.SubscriptionInfo.givePrintableIccid(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mIccIDLoadRegistrants;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyRegistrants(r9);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;
    L_0x03a2:
        r21 = 1;
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r2 = r0.mCarrierTestOverride;	 Catch:{ all -> 0x05f9 }
        r2 = r2.isInTestMode();	 Catch:{ all -> 0x05f9 }
        if (r2 == 0) goto L_0x07ba;	 Catch:{ all -> 0x05f9 }
    L_0x03ae:
        r2 = r33.getIMSI();	 Catch:{ all -> 0x05f9 }
        if (r2 == 0) goto L_0x07ba;	 Catch:{ all -> 0x05f9 }
    L_0x03b4:
        r16 = r33.getIMSI();	 Catch:{ all -> 0x05f9 }
        r2 = 0;
        r4 = 3;
        r0 = r16;	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = r0.substring(r2, r4);	 Catch:{ NumberFormatException -> 0x05d4 }
        r23 = java.lang.Integer.parseInt(r2);	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x05d4 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x05d4 }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x05d4 }
        r2.<init>();	 Catch:{ NumberFormatException -> 0x05d4 }
        r4 = "[TestMode] mMncLength=";	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = r2.append(r4);	 Catch:{ NumberFormatException -> 0x05d4 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x05d4 }
        r4 = r0.mMncLength;	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = r2.append(r4);	 Catch:{ NumberFormatException -> 0x05d4 }
        r2 = r2.toString();	 Catch:{ NumberFormatException -> 0x05d4 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x05d4 }
        r0.log(r2);	 Catch:{ NumberFormatException -> 0x05d4 }
    L_0x03e9:
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r2 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r4 = 15;	 Catch:{ all -> 0x05f9 }
        if (r2 != r4) goto L_0x10e7;	 Catch:{ all -> 0x05f9 }
    L_0x03f1:
        r2 = 0;	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.mMncLength = r2;	 Catch:{ all -> 0x05f9 }
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05f9 }
        r2.<init>();	 Catch:{ all -> 0x05f9 }
        r4 = "setting5 mMncLength=";	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r4 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r2 = r2.toString();	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
    L_0x0413:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0420;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x041a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1127;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0420:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x046b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0426:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x046b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0431:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x043f:
        if (r2 >= r5) goto L_0x046b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0441:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x1130;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0449:
        r2 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x046b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0478;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0472:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1134;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0478:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x04d5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x047e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x04d5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0489:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x04a9;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x049e:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x04c6;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04a9:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04b7:
        if (r2 >= r5) goto L_0x04c6;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04b9:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x113d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04c1:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04c6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04d5:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x04e2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04db:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x050d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x04e2:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x117b;
    L_0x04e8:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x116b }
        r2 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x116b }
        r4 = 0;	 Catch:{ NumberFormatException -> 0x116b }
        r5 = 3;	 Catch:{ NumberFormatException -> 0x116b }
        r25 = r2.substring(r4, r5);	 Catch:{ NumberFormatException -> 0x116b }
        r2 = "404";	 Catch:{ NumberFormatException -> 0x116b }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x116b }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x116b }
        if (r2 != 0) goto L_0x0508;	 Catch:{ NumberFormatException -> 0x116b }
    L_0x04fd:
        r2 = "405";	 Catch:{ NumberFormatException -> 0x116b }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x116b }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x116b }
        if (r2 == 0) goto L_0x1141;	 Catch:{ NumberFormatException -> 0x116b }
    L_0x0508:
        r2 = 3;	 Catch:{ NumberFormatException -> 0x116b }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x116b }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x116b }
    L_0x050d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x059a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0513:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x059a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0519:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x059a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0529:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x059a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x118a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05a0:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x118a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05a6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x118a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05ad:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05af:
        if (r20 == 0) goto L_0x05c9;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05b1:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x05c9;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05b9:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyGetAdDone(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x05c9:
        r2 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;
    L_0x05d4:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.mMncLength = r2;	 Catch:{ all -> 0x05f9 }
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05f9 }
        r2.<init>();	 Catch:{ all -> 0x05f9 }
        r4 = "[TestMode] Corrupt IMSI! mMncLength=";	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r4 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r2 = r2.toString();	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.loge(r2);	 Catch:{ all -> 0x05f9 }
        goto L_0x03e9;
    L_0x05f9:
        r2 = move-exception;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == r5) goto L_0x0607;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0601:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 != 0) goto L_0x118e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0607:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x0652;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x060d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.length();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 < r5) goto L_0x0652;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0618:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r4.substring(r5, r6);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r5.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0626:
        if (r4 >= r6) goto L_0x0652;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0628:
        r26 = r5[r4];	 Catch:{ RuntimeException -> 0x0045 }
        r7 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r7 == 0) goto L_0x1197;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0630:
        r4 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r4.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r4);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0652:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == r5) goto L_0x065f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0659:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 != 0) goto L_0x119b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x065f:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x06bc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0665:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.length();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 < r5) goto L_0x06bc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0670:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r4.substring(r5, r6);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.equals(r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 != 0) goto L_0x0690;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0685:
        r4 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.equals(r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x06ad;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0690:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r4.substring(r5, r6);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r5.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x069e:
        if (r4 >= r6) goto L_0x06ad;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06a0:
        r26 = r5[r4];	 Catch:{ RuntimeException -> 0x0045 }
        r7 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r7 == 0) goto L_0x11a4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06a8:
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06ad:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r5, r6);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r4);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06bc:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x06c9;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06c2:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 != r5) goto L_0x06f4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06c9:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x11e2;
    L_0x06cf:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x11d2 }
        r5 = 0;	 Catch:{ NumberFormatException -> 0x11d2 }
        r6 = 3;	 Catch:{ NumberFormatException -> 0x11d2 }
        r25 = r4.substring(r5, r6);	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = "404";	 Catch:{ NumberFormatException -> 0x11d2 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = r0.equals(r4);	 Catch:{ NumberFormatException -> 0x11d2 }
        if (r4 != 0) goto L_0x06ef;	 Catch:{ NumberFormatException -> 0x11d2 }
    L_0x06e4:
        r4 = "405";	 Catch:{ NumberFormatException -> 0x11d2 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = r0.equals(r4);	 Catch:{ NumberFormatException -> 0x11d2 }
        if (r4 == 0) goto L_0x11a8;	 Catch:{ NumberFormatException -> 0x11d2 }
    L_0x06ef:
        r4 = 3;	 Catch:{ NumberFormatException -> 0x11d2 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x11d2 }
        r0.mMncLength = r4;	 Catch:{ NumberFormatException -> 0x11d2 }
    L_0x06f4:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x0781;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x06fa:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x0781;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0700:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 < r5) goto L_0x0781;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0710:
        r4 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r4.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r4.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r6 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5.substring(r7, r6);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r4);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0781:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x11f1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0787:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x11f1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x078d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == r5) goto L_0x11f1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0794:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0796:
        if (r20 == 0) goto L_0x07b0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0798:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 == 0) goto L_0x07b0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07a0:
        r4 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4.notifyGetAdDone(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07b0:
        r4 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r4);	 Catch:{ RuntimeException -> 0x0045 }
        throw r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07ba:
        r0 = r34;	 Catch:{ all -> 0x05f9 }
        r9 = r0.obj;	 Catch:{ all -> 0x05f9 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ all -> 0x05f9 }
        r0 = r9.result;	 Catch:{ all -> 0x05f9 }
        r31 = r0;	 Catch:{ all -> 0x05f9 }
        r31 = (com.android.internal.telephony.uicc.IccIoResult) r31;	 Catch:{ all -> 0x05f9 }
        r15 = r31.getException();	 Catch:{ all -> 0x05f9 }
        r2 = r9.exception;	 Catch:{ all -> 0x05f9 }
        if (r2 != 0) goto L_0x07d0;	 Catch:{ all -> 0x05f9 }
    L_0x07ce:
        if (r15 == 0) goto L_0x0a00;	 Catch:{ all -> 0x05f9 }
    L_0x07d0:
        r2 = "read EF_AD exception occurs";	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x07e5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07df:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0999;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07e5:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0830;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07eb:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0830;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x07f6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0804:
        if (r2 >= r5) goto L_0x0830;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0806:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x09a2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x080e:
        r2 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0830:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x083d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0837:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x09a6;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x083d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x089a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0843:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x089a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x084e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x086e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0863:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x088b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x086e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x087c:
        if (r2 >= r5) goto L_0x088b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x087e:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x09af;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0886:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x088b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x089a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x08a7;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x08a0:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x08d2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x08a7:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x09ed;
    L_0x08ad:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x09dd }
        r4 = 0;	 Catch:{ NumberFormatException -> 0x09dd }
        r5 = 3;	 Catch:{ NumberFormatException -> 0x09dd }
        r25 = r2.substring(r4, r5);	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = "404";	 Catch:{ NumberFormatException -> 0x09dd }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x09dd }
        if (r2 != 0) goto L_0x08cd;	 Catch:{ NumberFormatException -> 0x09dd }
    L_0x08c2:
        r2 = "405";	 Catch:{ NumberFormatException -> 0x09dd }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x09dd }
        if (r2 == 0) goto L_0x09b3;	 Catch:{ NumberFormatException -> 0x09dd }
    L_0x08cd:
        r2 = 3;	 Catch:{ NumberFormatException -> 0x09dd }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x09dd }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x09dd }
    L_0x08d2:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x095f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x08d8:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x095f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x08de:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x095f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x08ee:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x095f:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x09fc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0965:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x09fc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x096b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x09fc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0972:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0974:
        if (r20 == 0) goto L_0x098e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0976:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x098e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x097e:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyGetAdDone(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x098e:
        r2 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0999:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0830;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x09a0:
        goto L_0x07e5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x09a2:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0804;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x09a6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;
        if (r2 != r4) goto L_0x089a;
    L_0x09ad:
        goto L_0x083d;
    L_0x09af:
        r2 = r2 + 1;
        goto L_0x087c;
    L_0x09b3:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x09dd }
        r4 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x09dd }
        r4.<init>();	 Catch:{ NumberFormatException -> 0x09dd }
        r5 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x09dd }
        r4 = r4.append(r5);	 Catch:{ NumberFormatException -> 0x09dd }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x09dd }
        r4 = r4.append(r0);	 Catch:{ NumberFormatException -> 0x09dd }
        r4 = r4.toString();	 Catch:{ NumberFormatException -> 0x09dd }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ NumberFormatException -> 0x09dd }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x09dd }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x09dd }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x09dd }
        goto L_0x08d2;
    L_0x09dd:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x08d2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x09ed:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x08d2;
    L_0x09fc:
        r20 = 0;
        goto L_0x0974;
    L_0x0a00:
        r0 = r31;	 Catch:{ all -> 0x05f9 }
        r2 = r0.payload;	 Catch:{ all -> 0x05f9 }
        if (r2 != 0) goto L_0x0c36;	 Catch:{ all -> 0x05f9 }
    L_0x0a06:
        r2 = "result.payload is null";	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0a1b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a15:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0bcf;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a1b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0a66;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a21:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0a66;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a2c:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a3a:
        if (r2 >= r5) goto L_0x0a66;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a3c:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x0bd8;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a44:
        r2 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a66:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0a73;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a6d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0bdc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a73:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0ad0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a79:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0ad0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a84:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0aa4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0a99:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0ac1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0aa4:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ab2:
        if (r2 >= r5) goto L_0x0ac1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ab4:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x0be5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0abc:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ac1:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ad0:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0add;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ad6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0b08;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0add:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0c23;
    L_0x0ae3:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x0c13 }
        r4 = 0;	 Catch:{ NumberFormatException -> 0x0c13 }
        r5 = 3;	 Catch:{ NumberFormatException -> 0x0c13 }
        r25 = r2.substring(r4, r5);	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = "404";	 Catch:{ NumberFormatException -> 0x0c13 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x0c13 }
        if (r2 != 0) goto L_0x0b03;	 Catch:{ NumberFormatException -> 0x0c13 }
    L_0x0af8:
        r2 = "405";	 Catch:{ NumberFormatException -> 0x0c13 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x0c13 }
        if (r2 == 0) goto L_0x0be9;	 Catch:{ NumberFormatException -> 0x0c13 }
    L_0x0b03:
        r2 = 3;	 Catch:{ NumberFormatException -> 0x0c13 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0c13 }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x0c13 }
    L_0x0b08:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0b95;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0b0e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0b95;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0b14:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0b95;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0b24:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0b95:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0c32;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0b9b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0c32;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ba1:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0c32;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ba8:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0baa:
        if (r20 == 0) goto L_0x0bc4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bac:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0bc4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bb4:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyGetAdDone(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bc4:
        r2 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bcf:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0a66;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bd6:
        goto L_0x0a1b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bd8:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0a3a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0bdc:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;
        if (r2 != r4) goto L_0x0ad0;
    L_0x0be3:
        goto L_0x0a73;
    L_0x0be5:
        r2 = r2 + 1;
        goto L_0x0ab2;
    L_0x0be9:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x0c13 }
        r4 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x0c13 }
        r4.<init>();	 Catch:{ NumberFormatException -> 0x0c13 }
        r5 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x0c13 }
        r4 = r4.append(r5);	 Catch:{ NumberFormatException -> 0x0c13 }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x0c13 }
        r4 = r4.append(r0);	 Catch:{ NumberFormatException -> 0x0c13 }
        r4 = r4.toString();	 Catch:{ NumberFormatException -> 0x0c13 }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ NumberFormatException -> 0x0c13 }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x0c13 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0c13 }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x0c13 }
        goto L_0x0b08;
    L_0x0c13:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0b08;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c23:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0b08;
    L_0x0c32:
        r20 = 0;
        goto L_0x0baa;
    L_0x0c36:
        r0 = r31;	 Catch:{ all -> 0x05f9 }
        r11 = r0.payload;	 Catch:{ all -> 0x05f9 }
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05f9 }
        r2.<init>();	 Catch:{ all -> 0x05f9 }
        r4 = "EF_AD: ";	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r2 = r2.toString();	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        r2 = r11.length;	 Catch:{ all -> 0x05f9 }
        r4 = 3;	 Catch:{ all -> 0x05f9 }
        if (r2 >= r4) goto L_0x0e8b;	 Catch:{ all -> 0x05f9 }
    L_0x0c5b:
        r2 = "Corrupt AD data on SIM";	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0c70;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c6a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0e24;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c70:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0cbb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c76:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0cbb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c81:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c8f:
        if (r2 >= r5) goto L_0x0cbb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c91:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x0e2d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0c99:
        r2 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cbb:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0cc8;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cc2:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0e31;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cc8:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0d25;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cce:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0d25;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cd9:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0cf9;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cee:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0d16;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0cf9:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d07:
        if (r2 >= r5) goto L_0x0d16;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d09:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x0e3a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d11:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d16:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d25:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0d32;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d2b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0d5d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d32:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0e78;
    L_0x0d38:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x0e68 }
        r4 = 0;	 Catch:{ NumberFormatException -> 0x0e68 }
        r5 = 3;	 Catch:{ NumberFormatException -> 0x0e68 }
        r25 = r2.substring(r4, r5);	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = "404";	 Catch:{ NumberFormatException -> 0x0e68 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x0e68 }
        if (r2 != 0) goto L_0x0d58;	 Catch:{ NumberFormatException -> 0x0e68 }
    L_0x0d4d:
        r2 = "405";	 Catch:{ NumberFormatException -> 0x0e68 }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x0e68 }
        if (r2 == 0) goto L_0x0e3e;	 Catch:{ NumberFormatException -> 0x0e68 }
    L_0x0d58:
        r2 = 3;	 Catch:{ NumberFormatException -> 0x0e68 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0e68 }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x0e68 }
    L_0x0d5d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0dea;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d63:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0dea;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d69:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0dea;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0d79:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0dea:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0e87;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0df0:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0e87;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0df6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0e87;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0dfd:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0dff:
        if (r20 == 0) goto L_0x0e19;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e01:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0e19;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e09:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyGetAdDone(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e19:
        r2 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e24:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0cbb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e2b:
        goto L_0x0c70;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e2d:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0c8f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e31:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;
        if (r2 != r4) goto L_0x0d25;
    L_0x0e38:
        goto L_0x0cc8;
    L_0x0e3a:
        r2 = r2 + 1;
        goto L_0x0d07;
    L_0x0e3e:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x0e68 }
        r4 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x0e68 }
        r4.<init>();	 Catch:{ NumberFormatException -> 0x0e68 }
        r5 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x0e68 }
        r4 = r4.append(r5);	 Catch:{ NumberFormatException -> 0x0e68 }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x0e68 }
        r4 = r4.append(r0);	 Catch:{ NumberFormatException -> 0x0e68 }
        r4 = r4.toString();	 Catch:{ NumberFormatException -> 0x0e68 }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ NumberFormatException -> 0x0e68 }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x0e68 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x0e68 }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x0e68 }
        goto L_0x0d5d;
    L_0x0e68:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0d5d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e78:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0d5d;
    L_0x0e87:
        r20 = 0;
        goto L_0x0dff;
    L_0x0e8b:
        r2 = r11.length;	 Catch:{ all -> 0x05f9 }
        r4 = 3;	 Catch:{ all -> 0x05f9 }
        if (r2 != r4) goto L_0x10bf;	 Catch:{ all -> 0x05f9 }
    L_0x0e8f:
        r2 = "MNC length not present in EF_AD";	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0ea4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0e9e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1058;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ea4:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0eef;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0eaa:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0eef;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0eb5:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 6;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ec3:
        if (r2 >= r5) goto L_0x0eef;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ec5:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x1061;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ecd:
        r2 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "setting6 mMncLength=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0eef:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x0efc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0ef6:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1065;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0efc:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0f59;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f02:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x0f59;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f0d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x0f2d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f22:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0f4a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f2d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f3b:
        if (r2 >= r5) goto L_0x0f4a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f3d:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x106e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f45:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f4a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.custMncLength(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f59:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x0f66;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f5f:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0f91;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f66:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x10ac;
    L_0x0f6c:
        r0 = r33;	 Catch:{ NumberFormatException -> 0x109c }
        r2 = r0.mImsi;	 Catch:{ NumberFormatException -> 0x109c }
        r4 = 0;	 Catch:{ NumberFormatException -> 0x109c }
        r5 = 3;	 Catch:{ NumberFormatException -> 0x109c }
        r25 = r2.substring(r4, r5);	 Catch:{ NumberFormatException -> 0x109c }
        r2 = "404";	 Catch:{ NumberFormatException -> 0x109c }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x109c }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x109c }
        if (r2 != 0) goto L_0x0f8c;	 Catch:{ NumberFormatException -> 0x109c }
    L_0x0f81:
        r2 = "405";	 Catch:{ NumberFormatException -> 0x109c }
        r0 = r25;	 Catch:{ NumberFormatException -> 0x109c }
        r2 = r0.equals(r2);	 Catch:{ NumberFormatException -> 0x109c }
        if (r2 == 0) goto L_0x1072;	 Catch:{ NumberFormatException -> 0x109c }
    L_0x0f8c:
        r2 = 3;	 Catch:{ NumberFormatException -> 0x109c }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x109c }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x109c }
    L_0x0f91:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x101e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f97:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x101e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0f9d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x101e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x0fad:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GET_AD_DONE setSystemProperty simOperator=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "gsm.sim.operator.numeric";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "persist.sys.mcc_match_fyrom";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r33.getOperatorNumeric();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setSystemProperty(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "update mccmnc=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r5 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.substring(r6, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4 + 3;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.substring(r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.updateMccMncConfigWithGplmn(r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x101e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x10bb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1024:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x10bb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x102a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x10bb;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1031:
        r20 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1033:
        if (r20 == 0) goto L_0x104d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1035:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsiLoad;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x104d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x103d:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mImsiLoad = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mParentApp;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyGetAdDone(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r33.onOperatorNumericLoadedHw();	 Catch:{ RuntimeException -> 0x0045 }
    L_0x104d:
        r2 = r33.getSlotId();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.initFdnPsStatus(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1058:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x0eef;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x105f:
        goto L_0x0ea4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1061:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0ec3;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1065:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;
        if (r2 != r4) goto L_0x0f59;
    L_0x106c:
        goto L_0x0efc;
    L_0x106e:
        r2 = r2 + 1;
        goto L_0x0f3b;
    L_0x1072:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x109c }
        r2 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x109c }
        r4 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x109c }
        r4.<init>();	 Catch:{ NumberFormatException -> 0x109c }
        r5 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x109c }
        r4 = r4.append(r5);	 Catch:{ NumberFormatException -> 0x109c }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x109c }
        r4 = r4.append(r0);	 Catch:{ NumberFormatException -> 0x109c }
        r4 = r4.toString();	 Catch:{ NumberFormatException -> 0x109c }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ NumberFormatException -> 0x109c }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x109c }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x109c }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x109c }
        goto L_0x0f91;
    L_0x109c:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0f91;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x10ac:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0f91;
    L_0x10bb:
        r20 = 0;
        goto L_0x1033;
    L_0x10bf:
        r2 = 3;
        r2 = r11[r2];	 Catch:{ all -> 0x05f9 }
        r2 = r2 & 15;	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.mMncLength = r2;	 Catch:{ all -> 0x05f9 }
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05f9 }
        r2.<init>();	 Catch:{ all -> 0x05f9 }
        r4 = "setting4 mMncLength=";	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r4 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r2 = r2.toString();	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        goto L_0x03e9;	 Catch:{ all -> 0x05f9 }
    L_0x10e7:
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r2 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r4 = 3;	 Catch:{ all -> 0x05f9 }
        if (r2 <= r4) goto L_0x10f5;	 Catch:{ all -> 0x05f9 }
    L_0x10ee:
        r2 = 2;	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.mMncLength = r2;	 Catch:{ all -> 0x05f9 }
        goto L_0x0413;	 Catch:{ all -> 0x05f9 }
    L_0x10f5:
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r2 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r4 = 2;	 Catch:{ all -> 0x05f9 }
        if (r2 == r4) goto L_0x0413;	 Catch:{ all -> 0x05f9 }
    L_0x10fc:
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r2 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r4 = 3;	 Catch:{ all -> 0x05f9 }
        if (r2 == r4) goto L_0x0413;	 Catch:{ all -> 0x05f9 }
    L_0x1103:
        r2 = -1;	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.mMncLength = r2;	 Catch:{ all -> 0x05f9 }
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05f9 }
        r2.<init>();	 Catch:{ all -> 0x05f9 }
        r4 = "setting5 mMncLength=";	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r4 = r0.mMncLength;	 Catch:{ all -> 0x05f9 }
        r2 = r2.append(r4);	 Catch:{ all -> 0x05f9 }
        r2 = r2.toString();	 Catch:{ all -> 0x05f9 }
        r0 = r33;	 Catch:{ all -> 0x05f9 }
        r0.log(r2);	 Catch:{ all -> 0x05f9 }
        goto L_0x0413;
    L_0x1127:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x046b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x112e:
        goto L_0x0420;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1130:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x043f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1134:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;
        if (r2 != r4) goto L_0x04d5;
    L_0x113b:
        goto L_0x0478;
    L_0x113d:
        r2 = r2 + 1;
        goto L_0x04b7;
    L_0x1141:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x116b }
        r2 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x116b }
        r4 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x116b }
        r4.<init>();	 Catch:{ NumberFormatException -> 0x116b }
        r5 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x116b }
        r4 = r4.append(r5);	 Catch:{ NumberFormatException -> 0x116b }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x116b }
        r4 = r4.append(r0);	 Catch:{ NumberFormatException -> 0x116b }
        r4 = r4.toString();	 Catch:{ NumberFormatException -> 0x116b }
        android.telephony.Rlog.d(r2, r4);	 Catch:{ NumberFormatException -> 0x116b }
        r2 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x116b }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x116b }
        r0.mMncLength = r2;	 Catch:{ NumberFormatException -> 0x116b }
        goto L_0x050d;
    L_0x116b:
        r12 = move-exception;
        r2 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x050d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x117b:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x050d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x118a:
        r20 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x05af;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x118e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        if (r4 != r5) goto L_0x0652;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1195:
        goto L_0x0607;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1197:
        r4 = r4 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0626;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x119b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;
        if (r4 != r5) goto L_0x06bc;
    L_0x11a2:
        goto L_0x065f;
    L_0x11a4:
        r4 = r4 + 1;
        goto L_0x069e;
    L_0x11a8:
        r23 = java.lang.Integer.parseInt(r25);	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = "SIMRecords";	 Catch:{ NumberFormatException -> 0x11d2 }
        r5 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x11d2 }
        r5.<init>();	 Catch:{ NumberFormatException -> 0x11d2 }
        r6 = "SIMRecords: AD err, mcc is determing mnc length in error case::";	 Catch:{ NumberFormatException -> 0x11d2 }
        r5 = r5.append(r6);	 Catch:{ NumberFormatException -> 0x11d2 }
        r0 = r23;	 Catch:{ NumberFormatException -> 0x11d2 }
        r5 = r5.append(r0);	 Catch:{ NumberFormatException -> 0x11d2 }
        r5 = r5.toString();	 Catch:{ NumberFormatException -> 0x11d2 }
        android.telephony.Rlog.d(r4, r5);	 Catch:{ NumberFormatException -> 0x11d2 }
        r4 = com.android.internal.telephony.MccTable.smallestDigitsMccForMnc(r23);	 Catch:{ NumberFormatException -> 0x11d2 }
        r0 = r33;	 Catch:{ NumberFormatException -> 0x11d2 }
        r0.mMncLength = r4;	 Catch:{ NumberFormatException -> 0x11d2 }
        goto L_0x06f4;
    L_0x11d2:
        r12 = move-exception;
        r4 = 0;
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Corrupt IMSI!";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x06f4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x11e2:
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "MNC length not present in EF_AD";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x06f4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x11f1:
        r20 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x0796;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x11f5:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.getSpnFsm(r2, r9);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = -1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == r4) goto L_0x1210;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x120a:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x127d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1210:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1216:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.length();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 < r4) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1221:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        r24 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "404";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1241;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1236:
        r2 = "405";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r24;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1241:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0045 }
        r27 = r2.substring(r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r33.getServiceProviderName();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.text.TextUtils.isEmpty(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1255:
        r2 = r33.getServiceProviderName();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = java.util.Locale.US;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toLowerCase(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "reliance";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.contains(r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1268:
        r4 = MCCMNC_CODES_HAVING_2DIGITS_MNC_ZERO_PREFIX_RELIANCE;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x126c:
        if (r2 >= r5) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x126e:
        r26 = r4[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r6 = r26.equals(r27);	 Catch:{ RuntimeException -> 0x0045 }
        if (r6 == 0) goto L_0x1285;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1276:
        r2 = 2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mMncLength = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x127d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mMncLength;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 3;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1284:
        goto L_0x1210;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1285:
        r2 = r2 + 1;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x126c;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1288:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x129f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1298:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfCff = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x129f:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CFF_CPHS: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfCff = r11;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12c2:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12d2:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.parseEfSpdi(r11);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12d9:
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12e3:
        r2 = "update failed. ";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.logw(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12ef:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x12ff:
        r32 = new com.android.internal.telephony.gsm.SimTlv;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11.length;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r32;	 Catch:{ RuntimeException -> 0x0045 }
        r0.<init>(r11, r4, r2);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1308:
        r2 = r32.isValidObject();	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x130e:
        r2 = r32.getTag();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 67;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x132a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1316:
        r2 = r32.getData();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r32.getData();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = com.android.internal.telephony.uicc.IccUtils.networkNameToString(r2, r5, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mPnnHomeName = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x132a:
        r32.nextObject();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x1308;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x132e:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x133a:
        r2 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (java.util.ArrayList) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.handleSmses(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1345:
        r2 = "ENF";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r4.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = "marked read: sms ";	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.arg1;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.append(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.toString();	 Catch:{ RuntimeException -> 0x0045 }
        android.telephony.Rlog.i(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1365:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r18 = r0;	 Catch:{ RuntimeException -> 0x0045 }
        r18 = (java.lang.Integer) r18;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1379;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1377:
        if (r18 != 0) goto L_0x13a3;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1379:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Error on SMS_ON_SIM with exp ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = " index ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r18;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r0);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x13a3:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "READ EF_SMS RECORD index=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r18;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r0);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r18.intValue();	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 22;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = r0.obtainMessage(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 28476; // 0x6f3c float:3.9903E-41 double:1.4069E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r2.loadEFLinearFixed(r6, r4, r5);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x13d5:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x13ec;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x13e1:
        r2 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (byte[]) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.handleSms(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x13ec:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Error on GET_SMS with exp ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1409:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1419:
        r2 = new com.android.internal.telephony.uicc.UsimServiceTable;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mUsimServiceTable = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "SST: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mUsimServiceTable;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mUsimServiceTable;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 28486; // 0x6f46 float:3.9917E-41 double:1.4074E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.checkFileInServiceTable(r4, r2, r11);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1454;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x144d:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.getSpnFsm(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1454:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mUsimServiceTable;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 28613; // 0x6fc5 float:4.0095E-41 double:1.41367E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.checkFileInServiceTable(r4, r2, r11);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1461:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x146d:
        r2 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (byte[]) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mCphsInfo = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "iCPHS: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mCphsInfo;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1498:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EVENT_SET_MBDN_DONE ex:";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x14cf;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x14bf:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewVoiceMailNum;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailNum = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewVoiceMailTag;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x14cf:
        r2 = r33.isCphsMailboxEnabled();	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x152a;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x14d5:
        r3 = new com.android.internal.telephony.uicc.AdnRecord;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mVoiceMailTag;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mVoiceMailNum;	 Catch:{ RuntimeException -> 0x0045 }
        r3.<init>(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r29 = r0;	 Catch:{ RuntimeException -> 0x0045 }
        r29 = (android.os.Message) r29;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x150c;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x14ec:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x150c;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x14f0:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.os.AsyncResult.forMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.exception = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2.sendToTarget();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = "Callback with MBDN successful.";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r29 = 0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x150c:
        r2 = new com.android.internal.telephony.uicc.AdnRecordLoader;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFh;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 25;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r1 = r29;	 Catch:{ RuntimeException -> 0x0045 }
        r8 = r0.obtainMessage(r4, r1);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 28439; // 0x6f17 float:3.9852E-41 double:1.40507E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = 28490; // 0x6f4a float:3.9923E-41 double:1.4076E-319;	 Catch:{ RuntimeException -> 0x0045 }
        r6 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r7 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r2.updateEF(r3, r4, r5, r6, r7, r8);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x152a:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x152e:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mContext;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "carrier_config";	 Catch:{ RuntimeException -> 0x0045 }
        r10 = r2.getSystemService(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r10 = (android.telephony.CarrierConfigManager) r10;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1569;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x153f:
        if (r10 == 0) goto L_0x1569;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1541:
        r2 = r10.getConfig();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "editable_voicemail_number_bool";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.getBoolean(r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1569;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x154e:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.os.AsyncResult.forMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = new com.android.internal.telephony.uicc.IccVmNotSupportedException;	 Catch:{ RuntimeException -> 0x0045 }
        r5 = "Update SIM voice mailbox error";	 Catch:{ RuntimeException -> 0x0045 }
        r4.<init>(r5);	 Catch:{ RuntimeException -> 0x0045 }
        r2.exception = r4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1560:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2.sendToTarget();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1569:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.os.AsyncResult.forMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2.exception = r4;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x1560;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1576:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x15b3;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1582:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewVoiceMailNum;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailNum = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mNewVoiceMailTag;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mVoiceMailTag = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1592:
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1596:
        r2 = "Callback with CPHS MB successful.";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = android.os.AsyncResult.forMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2.exception = r4;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.userObj;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (android.os.Message) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2.sendToTarget();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x15b3:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Set CPHS MailBox with exception: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x1592;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x15cf:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Sim REFRESH with exception: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x15f6:
        r2 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = (com.android.internal.telephony.uicc.IccRefreshResponse) r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.handleSimRefresh(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1601:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x16c4;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1611:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfCfis = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r17 = r33.getVmSimImsi();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mOriginVmImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1626;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1620:
        r0 = r17;	 Catch:{ RuntimeException -> 0x0045 }
        r1 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r1.mOriginVmImsi = r0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1626:
        if (r17 == 0) goto L_0x164d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1628:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r17;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.equals(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x164d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1634:
        r2 = r33.getCallForwardingPreference();	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x163a:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mCallForwardingStatus = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mRecordsEventsRegistrants;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyResult(r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x164d:
        r2 = android.telephony.TelephonyManager.getDefault();	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.isMultiSimEnabled();	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x16bc;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1657:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mFirstImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x169f;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x165d:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mFirstImsi = r2;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1665:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mOriginVmImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x166b:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mFirstImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1671:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mOriginVmImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFirstImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.equals(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1681:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mSecondImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1687:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mOriginVmImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mSecondImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.equals(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 ^ 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1697:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setCallForwardingPreference(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x169f:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mFirstImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.equals(r4);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1665;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16ad:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mSecondImsi;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1665;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16b3:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mImsi;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mSecondImsi = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x1665;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16bc:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.setCallForwardingPreference(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16c4:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CFIS: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.validEfCfis(r11);	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x172b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16e9:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEfCfis = r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r11[r2];	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2 & 1;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1727;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16f4:
        r13 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16f5:
        if (r13 == 0) goto L_0x1729;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16f7:
        r2 = 1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x16f8:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mCallForwardingStatus = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CFIS: callForwardingEnabled=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mCallForwardingStatus;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mRecordsEventsRegistrants;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2.notifyResult(r4);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1727:
        r13 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x16f5;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1729:
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x16f8;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x172b:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CFIS: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x174a:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1773;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1756:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Exception in fetching EF_CSP data ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1773:
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "EF_CSP: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r0.mCspPlmnEnabled;	 Catch:{ RuntimeException -> 0x0045 }
        r28 = r0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.handleEfCspData(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mCspPlmnEnabled;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r1 = r28;	 Catch:{ RuntimeException -> 0x0045 }
        r0.checkSendBroadcast(r1, r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x17ac:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x17de;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x17bc:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Exception in get GID1 ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mGid1 = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x17de:
        r2 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mGid1 = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GID1: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mGid1;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1805:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 == 0) goto L_0x1837;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1815:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Exception in get GID2 ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mGid2 = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1837:
        r2 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mGid2 = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "GID2: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mGid2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x185e:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1870;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x186e:
        if (r11 != 0) goto L_0x188d;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1870:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Failed getting User PLMN with Access Tech Records: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x188d:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Received a PlmnActRecord, raw=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = com.android.internal.telephony.uicc.PlmnActRecord.getRecords(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mPlmnActRecords = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x18b4:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x18c6;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x18c4:
        if (r11 != 0) goto L_0x18e3;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x18c6:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Failed getting Operator PLMN with Access Tech Records: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x18e3:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Received a PlmnActRecord, raw=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = com.android.internal.telephony.uicc.PlmnActRecord.getRecords(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mOplmnActRecords = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x190a:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x191c;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x191a:
        if (r11 != 0) goto L_0x1939;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x191c:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Failed getting Home PLMN with Access Tech Records: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1939:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Received a PlmnActRecord, raw=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = com.android.internal.telephony.uicc.IccUtils.bytesToHexString(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = com.android.internal.telephony.uicc.PlmnActRecord.getRecords(r11);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mHplmnActRecords = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "HplmnActRecord[]=";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mHplmnActRecords;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = java.util.Arrays.toString(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.log(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1981:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x1993;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1991:
        if (r11 != 0) goto L_0x19b0;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1993:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Failed getting Equivalent Home PLMNs: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x19b0:
        r2 = "Equivalent Home";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.parseBcdPlmnList(r11, r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mEhplmns = r2;	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x19bf:
        r21 = 1;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = r0.obj;	 Catch:{ RuntimeException -> 0x0045 }
        r9 = (android.os.AsyncResult) r9;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = r9.result;	 Catch:{ RuntimeException -> 0x0045 }
        r11 = (byte[]) r11;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != 0) goto L_0x19d1;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x19cf:
        if (r11 != 0) goto L_0x19ee;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x19d1:
        r2 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0045 }
        r2.<init>();	 Catch:{ RuntimeException -> 0x0045 }
        r4 = "Failed getting Forbidden PLMNs: ";	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r9.exception;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.append(r4);	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r2.toString();	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x19ee:
        r2 = "Forbidden";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.parseBcdPlmnList(r11, r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.mFplmns = r2;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.arg1;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 1238273; // 0x12e501 float:1.73519E-39 double:6.11788E-318;	 Catch:{ RuntimeException -> 0x0045 }
        if (r2 != r4) goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1a04:
        r21 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r34;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.arg2;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = java.lang.Integer.valueOf(r2);	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r30 = r0.retrievePendingResponseMessage(r2);	 Catch:{ RuntimeException -> 0x0045 }
        if (r30 == 0) goto L_0x1a2e;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1a16:
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = r0.mFplmns;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r0.mFplmns;	 Catch:{ RuntimeException -> 0x0045 }
        r4 = r4.length;	 Catch:{ RuntimeException -> 0x0045 }
        r2 = java.util.Arrays.copyOf(r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r4 = 0;	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r30;	 Catch:{ RuntimeException -> 0x0045 }
        android.os.AsyncResult.forMessage(r0, r2, r4);	 Catch:{ RuntimeException -> 0x0045 }
        r30.sendToTarget();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1a2e:
        r2 = "Failed to retrieve a response message for FPLMN";	 Catch:{ RuntimeException -> 0x0045 }
        r0 = r33;	 Catch:{ RuntimeException -> 0x0045 }
        r0.loge(r2);	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;	 Catch:{ RuntimeException -> 0x0045 }
    L_0x1a38:
        r33.handleCarrierNameOverride();	 Catch:{ RuntimeException -> 0x0045 }
        goto L_0x003b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.uicc.SIMRecords.handleMessage(android.os.Message):void");
    }

    protected void handleFileUpdate(int efid) {
        switch (efid) {
            case IccConstants.EF_CFF_CPHS /*28435*/:
            case IccConstants.EF_CFIS /*28619*/:
                log("SIM Refresh called for EF_CFIS or EF_CFF_CPHS");
                loadCallForwardingRecords();
                return;
            case IccConstants.EF_CSP_CPHS /*28437*/:
                this.mRecordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
                return;
            case IccConstants.EF_MAILBOX_CPHS /*28439*/:
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                return;
            case IccConstants.EF_FDN /*28475*/:
                log("SIM Refresh called for EF_FDN");
                this.mParentApp.queryFdn();
                this.mAdnCache.reset();
                return;
            case IccConstants.EF_MSISDN /*28480*/:
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
                return;
            case IccConstants.EF_MBDN /*28615*/:
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                return;
            default:
                this.mAdnCache.reset();
                fetchSimRecords();
                return;
        }
    }

    private void handleSimRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleSimRefresh received without input");
        } else if ((TextUtils.isEmpty(refreshResponse.aid) || (refreshResponse.aid.equals(this.mParentApp.getAid()) ^ 1) == 0) && !beforeHandleSimRefresh(refreshResponse)) {
            switch (refreshResponse.refreshResult) {
                case 0:
                    log("handleSimRefresh with SIM_FILE_UPDATED");
                    handleFileUpdate(refreshResponse.efId);
                    break;
                case 1:
                    log("handleSimRefresh with SIM_REFRESH_INIT");
                    onIccRefreshInit();
                    this.mParentApp.queryFdn();
                    break;
                case 2:
                    log("handleSimRefresh with SIM_REFRESH_RESET");
                    break;
                default:
                    log("handleSimRefresh with unknown operation");
                    break;
            }
            if (!afterHandleSimRefresh(refreshResponse)) {
            }
        }
    }

    private int dispatchGsmMessage(SmsMessage message) {
        this.mNewSmsRegistrants.notifyResult(message);
        return 0;
    }

    private void handleSms(byte[] ba) {
        if (ba[0] != (byte) 0) {
            Rlog.d("ENF", "status : " + ba[0]);
        }
        if (ba[0] == (byte) 3) {
            int n = ba.length;
            byte[] pdu = new byte[(n - 1)];
            System.arraycopy(ba, 1, pdu, 0, n - 1);
            dispatchGsmMessage(SmsMessage.createFromPdu(pdu, "3gpp"));
        }
    }

    private void handleSmses(ArrayList<byte[]> messages) {
        int count = messages.size();
        for (int i = 0; i < count; i++) {
            byte[] ba = (byte[]) messages.get(i);
            if (ba[0] != (byte) 0) {
                Rlog.i("ENF", "status " + i + ": " + ba[0]);
            }
            if (ba[0] == (byte) 3) {
                int n = ba.length;
                byte[] pdu = new byte[(n - 1)];
                System.arraycopy(ba, 1, pdu, 0, n - 1);
                dispatchGsmMessage(SmsMessage.createFromPdu(pdu, "3gpp"));
                ba[0] = (byte) 1;
            }
        }
    }

    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
            onAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void setVoiceCallForwardingFlagFromSimRecords() {
        int i = 1;
        if (validEfCfis(this.mEfCfis)) {
            this.mCallForwardingStatus = this.mEfCfis[1] & 1;
            log("EF_CFIS: callForwardingEnabled=" + this.mCallForwardingStatus);
        } else if (this.mEfCff != null) {
            if ((this.mEfCff[0] & 15) != 10) {
                i = 0;
            }
            this.mCallForwardingStatus = i;
            log("EF_CFF: callForwardingEnabled=" + this.mCallForwardingStatus);
        } else {
            this.mCallForwardingStatus = -1;
            log("EF_CFIS and EF_CFF not valid. callForwardingEnabled=" + this.mCallForwardingStatus);
        }
    }

    protected void onAllRecordsLoaded() {
        log("record load complete");
        if (Resources.getSystem().getBoolean(17957056)) {
            setSimLanguage(this.mEfLi, this.mEfPl);
        } else {
            log("Not using EF LI/EF PL");
        }
        setVoiceCallForwardingFlagFromSimRecords();
        if (this.mParentApp.getState() == AppState.APPSTATE_PIN || this.mParentApp.getState() == AppState.APPSTATE_PUK) {
            this.mRecordsRequested = false;
            return;
        }
        String operator = getOperatorNumeric();
        if (TextUtils.isEmpty(operator)) {
            log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
        } else {
            log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" + operator + "'");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), operator);
        }
        String imsi = getIMSI();
        if (TextUtils.isEmpty(this.mImsi) || imsi.length() < 3) {
            log("onAllRecordsLoaded empty imsi skipping setting mcc");
        } else {
            try {
                this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), MccTable.countryCodeForMcc(Integer.parseInt(this.mImsi.substring(0, 3))));
            } catch (RuntimeException exc) {
                logw("onAllRecordsLoaded: invalid IMSI with the exception ", exc);
            }
        }
        if (!VSimUtilsInner.isVSimSub(getSlotId())) {
            onAllRecordsLoadedHw();
        }
        VSimUtilsInner.setMarkForCardReload(getSlotId(), false);
        setVoiceMailByCountry(operator);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
    }

    private void handleCarrierNameOverride() {
        CarrierConfigManager configLoader = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configLoader == null || !configLoader.getConfig().getBoolean("carrier_name_override_bool")) {
            setSpnFromConfig(getOperatorNumeric());
            return;
        }
        String carrierName = configLoader.getConfig().getString("carrier_name_string");
        setServiceProviderName(carrierName);
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), carrierName);
    }

    private void setSpnFromConfig(String carrier) {
        if (this.mSpnOverride.containsCarrier(carrier)) {
            setServiceProviderName(this.mSpnOverride.getSpn(carrier));
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), getServiceProviderName());
        }
    }

    protected void setVoiceMailByCountry(String spn) {
        if (this.mVmConfig.containsCarrier(spn)) {
            this.mIsVoiceMailFixed = this.mVmConfig.getVoiceMailFixed(spn);
            this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(spn);
            this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(spn);
            this.mHwCustSIMRecords = (HwCustSIMRecords) HwCustUtils.createObj(HwCustSIMRecords.class, new Object[]{this.mContext});
            if (this.mHwCustSIMRecords != null && this.mHwCustSIMRecords.isOpenRoamingVoiceMail()) {
                this.mHwCustSIMRecords.registerRoamingState(this.mVoiceMailNum);
            }
        }
    }

    public void getForbiddenPlmns(Message response) {
        this.mFh.loadEFTransparent(IccConstants.EF_FPLMN, obtainMessage(41, 1238273, storePendingResponseMessage(response)));
    }

    public void onReady() {
        fetchSimRecords();
    }

    private void onLocked() {
        log("only fetch EF_LI and EF_PL in lock state");
        loadEfLiAndEfPl();
    }

    public void onGetImsiDone(Message msg) {
        if (msg != null) {
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                loge("Exception querying IMSI, Exception:" + ar.exception);
                return;
            }
            String mccmncCode;
            this.mImsi = (String) ar.result;
            refreshCardType();
            if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                loge("invalid IMSI ");
                this.mImsi = null;
            }
            if (this.mImsi != null) {
                try {
                    Integer.parseInt(this.mImsi.substring(0, 3));
                } catch (NumberFormatException e) {
                    loge("invalid numberic IMSI ");
                    this.mImsi = null;
                }
            }
            log("IMSI: mMncLength=" + this.mMncLength + ", mImsiLoad: " + this.mImsiLoad);
            if (this.mImsi != null && this.mImsi.length() >= 6) {
                log("IMSI: " + this.mImsi.substring(0, 6) + Rlog.pii(LOG_TAG, this.mImsi.substring(6)));
            }
            String imsi = getIMSI();
            onImsiLoadedHw();
            updateSarMnc(imsi);
            if ((this.mMncLength == 0 || this.mMncLength == 2) && imsi != null && imsi.length() >= 6) {
                mccmncCode = imsi.substring(0, 6);
                for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                    if (mccmnc.equals(mccmncCode)) {
                        this.mMncLength = 3;
                        log("IMSI: setting1 mMncLength=" + this.mMncLength);
                        break;
                    }
                }
            }
            if ((this.mMncLength == 0 || this.mMncLength == 3) && imsi != null && imsi.length() >= 5) {
                String mcc = imsi.substring(0, 3);
                if (mcc.equals("404") || mcc.equals("405")) {
                    mccmncCode = imsi.substring(0, 5);
                    for (String mccmnc2 : MCCMNC_CODES_HAVING_2DIGITS_MNC) {
                        if (mccmnc2.equals(mccmncCode)) {
                            this.mMncLength = 2;
                            break;
                        }
                    }
                }
            }
            if (this.mMncLength == 0 && imsi != null) {
                try {
                    String mccStr = imsi.substring(0, 3);
                    if (mccStr.equals("404") || mccStr.equals("405")) {
                        this.mMncLength = 3;
                    } else {
                        this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(mccStr));
                    }
                } catch (NumberFormatException e2) {
                    this.mMncLength = 0;
                    Rlog.e(LOG_TAG, "SIMRecords: Corrupt IMSI!");
                }
            }
            adapterForDoubleRilChannelAfterImsiReady();
            this.mImsiReadyRegistrants.notifyRegistrants();
        }
    }

    private void loadEfLiAndEfPl() {
        if (this.mParentApp.getType() == AppType.APPTYPE_USIM) {
            this.mRecordsRequested = true;
            this.mFh.loadEFTransparent(IccConstants.EF_LI, obtainMessage(100, new EfUsimLiLoaded()));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_PL, obtainMessage(100, new EfPlLoaded()));
            this.mRecordsToLoad++;
        }
    }

    private void loadCallForwardingRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFLinearFixed(IccConstants.EF_CFIS, 1, obtainMessage(32));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CFF_CPHS, obtainMessage(24));
        this.mRecordsToLoad++;
    }

    protected void fetchSimRecords() {
        this.mRecordsRequested = true;
        log("fetchSimRecords " + this.mRecordsToLoad);
        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
        this.mRecordsToLoad++;
        this.mCi.iccIOForApp(176, IccConstants.EF_AD, this.mFh.getEFPath(IccConstants.EF_AD), 0, 0, 4, null, null, this.mParentApp.getAid(), obtainMessage(9));
        this.mRecordsToLoad++;
        if (!getIccidSwitch()) {
            if (IS_MODEM_CAPABILITY_GET_ICCID_AT) {
                this.mCi.getICCID(obtainMessage(4));
            } else {
                this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
            }
            this.mRecordsToLoad++;
        }
        getPbrRecordSize();
        new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_MBI, 1, obtainMessage(5));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_MWIS, 1, obtainMessage(7));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, obtainMessage(8));
        this.mRecordsToLoad++;
        loadCallForwardingRecords();
        this.mFh.loadEFTransparent(IccConstants.EF_SPDI, obtainMessage(13));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_PNN, 1, obtainMessage(15));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_SST, obtainMessage(17));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_INFO_CPHS, obtainMessage(26));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_GID1, obtainMessage(34));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_GID2, obtainMessage(36));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_PLMN_W_ACT, obtainMessage(37));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_OPLMN_W_ACT, obtainMessage(38));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_HPLMN_W_ACT, obtainMessage(39));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_EHPLMN, obtainMessage(40));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_FPLMN, obtainMessage(41, 1238272, -1));
        this.mRecordsToLoad++;
        loadEons();
        loadGID1();
        loadEfLiAndEfPl();
        loadCardSpecialFile(IccConstants.EF_HPLMN);
        loadCardSpecialFile(IccConstants.EF_OCSGL);
        log("fetchSimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    public int getDisplayRule(String plmn) {
        if (this.mParentApp != null && this.mParentApp.getUiccCard() != null && this.mParentApp.getUiccCard().getOperatorBrandOverride() != null) {
            return 2;
        }
        if (TextUtils.isEmpty(getServiceProviderName()) || this.mSpnDisplayCondition == -1) {
            return 2;
        }
        if (isOnMatchingPlmn(plmn)) {
            if ((this.mSpnDisplayCondition & 1) == 1) {
                return 3;
            }
            return 1;
        } else if ((this.mSpnDisplayCondition & 2) == 0) {
            return 3;
        } else {
            return 2;
        }
    }

    private boolean isOnMatchingPlmn(String plmn) {
        if (plmn == null) {
            return false;
        }
        if (plmn.equals(getOperatorNumeric())) {
            return true;
        }
        if (this.mSpdiNetworks != null) {
            for (String spdiNet : this.mSpdiNetworks) {
                if (plmn.equals(spdiNet)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void getSpnFsm(boolean start, AsyncResult ar) {
        if (start) {
            if (this.mSpnState == GetSpnFsmState.READ_SPN_3GPP || this.mSpnState == GetSpnFsmState.READ_SPN_CPHS || this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS || this.mSpnState == GetSpnFsmState.INIT) {
                this.mSpnState = GetSpnFsmState.INIT;
                return;
            }
            this.mSpnState = GetSpnFsmState.INIT;
        }
        byte[] data;
        String spn;
        switch (-getcom-android-internal-telephony-uicc-SIMRecords$GetSpnFsmStateSwitchesValues()[this.mSpnState.ordinal()]) {
            case 1:
                setServiceProviderName(null);
                this.mFh.loadEFTransparent(IccConstants.EF_SPN, obtainMessage(12));
                this.mRecordsToLoad++;
                this.mSpnState = GetSpnFsmState.READ_SPN_3GPP;
                break;
            case 2:
                if (ar == null || ar.exception != null) {
                    this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                } else {
                    data = ar.result;
                    this.mSpnDisplayCondition = data[0] & 255;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 1, data.length - 1));
                    spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                    } else {
                        log("Load EF_SPN: " + spn + " spnDisplayCondition: " + this.mSpnDisplayCondition);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_CPHS) {
                    this.mFh.loadEFTransparent(IccConstants.EF_SPN_CPHS, obtainMessage(12));
                    this.mRecordsToLoad++;
                    this.mSpnDisplayCondition = -1;
                    break;
                }
                break;
            case 3:
                if (ar == null || ar.exception != null) {
                    this.mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                } else {
                    data = (byte[]) ar.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 0, data.length));
                    spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_CPHS: " + spn);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS) {
                    this.mFh.loadEFTransparent(IccConstants.EF_SPN_SHORT_CPHS, obtainMessage(12));
                    this.mRecordsToLoad++;
                    break;
                }
                break;
            case 4:
                if (ar == null || ar.exception != null) {
                    setServiceProviderName(null);
                    log("No SPN loaded in either CHPS or 3GPP");
                } else {
                    data = (byte[]) ar.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 0, data.length));
                    spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        log("No SPN loaded in either CHPS or 3GPP");
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_SHORT_CPHS: " + spn);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                    }
                }
                this.mSpnState = GetSpnFsmState.IDLE;
                break;
            default:
                this.mSpnState = GetSpnFsmState.IDLE;
                break;
        }
    }

    private void parseEfSpdi(byte[] data) {
        SimTlv tlv = new SimTlv(data, 0, data.length);
        byte[] plmnEntries = null;
        while (tlv.isValidObject()) {
            if (tlv.getTag() == 163) {
                tlv = new SimTlv(tlv.getData(), 0, tlv.getData().length);
            }
            if (tlv.getTag() == 128) {
                plmnEntries = tlv.getData();
                break;
            }
            tlv.nextObject();
        }
        if (plmnEntries != null) {
            this.mSpdiNetworks = new ArrayList(plmnEntries.length / 3);
            for (int i = 0; i + 2 < plmnEntries.length; i += 3) {
                String plmnCode = IccUtils.bcdToString(plmnEntries, i, 3);
                if (plmnCode.length() >= 5) {
                    log("EF_SPDI network: " + plmnCode);
                    this.mSpdiNetworks.add(plmnCode);
                }
            }
        }
    }

    private String[] parseBcdPlmnList(byte[] data, String description) {
        log("Received " + description + " PLMNs, raw=" + IccUtils.bytesToHexString(data));
        if (data.length == 0 || data.length % 3 != 0) {
            loge("Received invalid " + description + " PLMN list");
            return null;
        }
        int numPlmns = data.length / 3;
        String[] ret = new String[numPlmns];
        for (int i = 0; i < numPlmns; i++) {
            ret[i] = IccUtils.bcdPlmnToString(data, i * 3);
        }
        return ret;
    }

    private boolean isCphsMailboxEnabled() {
        boolean z = true;
        if (this.mCphsInfo == null) {
            return false;
        }
        if ((this.mCphsInfo[1] & 48) != 48) {
            z = false;
        }
        return z;
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    protected void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    protected void logw(String s, Throwable tr) {
        Rlog.w(LOG_TAG, s, tr);
    }

    protected void logv(String s) {
        Rlog.v(LOG_TAG, s);
    }

    public boolean isCspPlmnEnabled() {
        return this.mCspPlmnEnabled;
    }

    private void handleEfCspData(byte[] data) {
        int usedCspGroups = data.length / 2;
        this.mCspPlmnEnabled = true;
        for (int i = 0; i < usedCspGroups; i++) {
            if (data[i * 2] == (byte) -64) {
                log("[CSP] found ValueAddedServicesGroup, value " + data[(i * 2) + 1]);
                if ((data[(i * 2) + 1] & 128) == 128) {
                    this.mCspPlmnEnabled = true;
                } else {
                    this.mCspPlmnEnabled = false;
                    log("[CSP] Set Automatic Network Selection");
                    this.mNetworkSelectionModeAutomaticRegistrants.notifyRegistrants();
                }
                return;
            }
        }
        log("[CSP] Value Added Service Group (0xC0), not found!");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SIMRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mVmConfig=" + this.mVmConfig);
        pw.println(" mSpnOverride=" + this.mSpnOverride);
        pw.println(" mCallForwardingStatus=" + this.mCallForwardingStatus);
        pw.println(" mSpnState=" + this.mSpnState);
        pw.println(" mCphsInfo=" + this.mCphsInfo);
        pw.println(" mCspPlmnEnabled=" + this.mCspPlmnEnabled);
        pw.println(" mEfMWIS[]=" + Arrays.toString(this.mEfMWIS));
        pw.println(" mEfCPHS_MWI[]=" + Arrays.toString(this.mEfCPHS_MWI));
        pw.println(" mEfCff[]=" + Arrays.toString(this.mEfCff));
        pw.println(" mEfCfis[]=" + Arrays.toString(this.mEfCfis));
        pw.println(" mSpnDisplayCondition=" + this.mSpnDisplayCondition);
        pw.println(" mSpdiNetworks[]=" + this.mSpdiNetworks);
        pw.println(" mPnnHomeName=" + this.mPnnHomeName);
        pw.println(" mUsimServiceTable=" + this.mUsimServiceTable);
        pw.println(" mGid1=" + this.mGid1);
        if (this.mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeGid1=" + (this.mFakeGid1 != null ? this.mFakeGid1 : "null"));
        }
        pw.println(" mGid2=" + this.mGid2);
        if (this.mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeGid2=" + (this.mFakeGid2 != null ? this.mFakeGid2 : "null"));
        }
        pw.println(" mPlmnActRecords[]=" + Arrays.toString(this.mPlmnActRecords));
        pw.println(" mOplmnActRecords[]=" + Arrays.toString(this.mOplmnActRecords));
        pw.println(" mHplmnActRecords[]=" + Arrays.toString(this.mHplmnActRecords));
        pw.println(" mFplmns[]=" + Arrays.toString(this.mFplmns));
        pw.println(" mEhplmns[]=" + Arrays.toString(this.mEhplmns));
        pw.flush();
    }

    private String getVmSimImsi() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        if (!sp.contains("sim_imsi_key" + getSlotId()) && sp.contains("vm_sim_imsi_key" + getSlotId())) {
            String imsi = sp.getString("vm_sim_imsi_key" + getSlotId(), null);
            if (!(imsi == null || mPasswordUtil == null)) {
                String oldDecodeVmSimImsi = mPasswordUtil.pswd2PlainText(imsi);
                try {
                    imsi = new String(Base64.decode(imsi, 0), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    Rlog.e(LOG_TAG, "getVmSimImsi UnsupportedEncodingException");
                }
                if (imsi.equals(this.mImsi) || oldDecodeVmSimImsi.equals(this.mImsi)) {
                    imsi = this.mImsi;
                    Rlog.d(LOG_TAG, "getVmSimImsi: Old IMSI encryption is not supported, now setVmSimImsi again.");
                    setVmSimImsi(imsi);
                    Editor editor = sp.edit();
                    editor.remove("vm_sim_imsi_key" + getSlotId());
                    editor.commit();
                }
            }
        }
        String vmSimImsi = sp.getString("sim_imsi_key" + getSlotId(), null);
        if (vmSimImsi == null) {
            return vmSimImsi;
        }
        try {
            return new String(Base64.decode(vmSimImsi, 0), "utf-8");
        } catch (IllegalArgumentException e2) {
            Rlog.e(LOG_TAG, "getVmSimImsi IllegalArgumentException");
            return vmSimImsi;
        } catch (UnsupportedEncodingException e3) {
            Rlog.e(LOG_TAG, "getVmSimImsi UnsupportedEncodingException");
            return vmSimImsi;
        }
    }

    private void setVmSimImsi(String imsi) {
        try {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            editor.putString("sim_imsi_key" + getSlotId(), new String(Base64.encode(imsi.getBytes("utf-8"), 0), "utf-8"));
            editor.apply();
        } catch (UnsupportedEncodingException e) {
            Rlog.d(LOG_TAG, "setVmSimImsi UnsupportedEncodingException");
        }
    }

    private boolean getCallForwardingPreference() {
        boolean cf = PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean("cf_enabled_key" + getSlotId(), false);
        Rlog.d(LOG_TAG, "Get callforwarding info from perferences getSlotId()=" + getSlotId() + ",cf=" + cf);
        return cf;
    }

    private void setCallForwardingPreference(boolean enabled) {
        Rlog.d(LOG_TAG, "Set callforwarding info to perferences getSlotId()=" + getSlotId() + ",cf=" + enabled);
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        edit.putBoolean("cf_enabled_key" + getSlotId(), enabled);
        edit.commit();
        if (this.mImsi != null) {
            setVmSimImsi(this.mImsi);
        }
    }

    private void updateMccMncConfigWithGplmn(String operatorNumeric) {
        log("updateMccMncConfigWithGplmn: " + operatorNumeric);
        if (HwTelephonyFactory.getHwUiccManager().isCDMASimCard(this.mParentApp.getPhoneId())) {
            log("cdma card, ignore updateMccMncConfiguration");
        } else if (operatorNumeric != null && operatorNumeric.length() >= 5) {
            MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, false);
        }
    }

    private void adapterForDoubleRilChannelAfterImsiReady() {
        if (this.mImsi != null && this.mMncLength != 0 && this.mMncLength != -1) {
            log("EVENT_GET_IMSI_DONE, update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
            updateMccMncConfigWithGplmn(this.mImsi.substring(0, this.mMncLength + 3));
            if (!this.mImsiLoad) {
                log("EVENT_GET_IMSI_DONE, trigger notifyGetAdDone and onOperatorNumericLoadedHw.");
                setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                this.mImsiLoad = true;
                this.mParentApp.notifyGetAdDone(null);
                onOperatorNumericLoadedHw();
            }
        }
    }

    private void checkSendBroadcast(boolean oldCspPlmnEnabled, boolean CspPlmnEnabled) {
        if (SystemProperties.getBoolean("ro.config.csp_enable", false) && oldCspPlmnEnabled != CspPlmnEnabled) {
            Intent intent = new Intent("android.intent.action.ACTION_HW_CSP_PLMN_CHANGE");
            intent.addFlags(536870912);
            intent.putExtra("state", this.mCspPlmnEnabled);
            this.mContext.sendBroadcast(intent);
            log("Broadcast, CSP Plmn Enabled change to " + this.mCspPlmnEnabled);
        }
    }

    protected void initEventIdMap() {
        this.sEventIdMap.put("EVENT_GET_MBDN_DONE", Integer.valueOf(6));
        this.sEventIdMap.put("EVENT_GET_ICCID_DONE", Integer.valueOf(4));
    }

    protected int getEventIdFromMap(String event) {
        if (this.sEventIdMap.containsKey(event)) {
            return ((Integer) this.sEventIdMap.get(event)).intValue();
        }
        log("Event Id not in the map.");
        return -1;
    }
}
