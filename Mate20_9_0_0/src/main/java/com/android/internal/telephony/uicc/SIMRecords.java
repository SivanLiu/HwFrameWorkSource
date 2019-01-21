package com.android.internal.telephony.uicc;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.encrypt.PasswordUtil;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccRecords.IccRecordLoaded;
import com.android.internal.telephony.uicc.UsimServiceTable.UsimService;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import huawei.cust.HwGetCfgFileConfig;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class SIMRecords extends IccRecords {
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
    private static final int EVENT_APP_NETWORK_LOCKED = 259;
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
    protected static final int EVENT_GET_SIM_MATCHED_FILE_DONE = 42;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SPDI_DONE = 13;
    private static final int EVENT_GET_SPN_DONE = 12;
    private static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25;
    private static final int EVENT_SET_MBDN_DONE = 20;
    private static final int EVENT_SET_MSISDN_DONE = 30;
    private static final int EVENT_SMS_ON_SIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    protected static final boolean HW_IS_SUPPORT_FAST_FETCH_SIMINFO = SystemProperties.getBoolean("ro.odm.radio.nvcfg_normalization", false);
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
    private String mSecondImsi;
    ArrayList<String> mSpdiNetworks;
    int mSpnDisplayCondition;
    private GetSpnFsmState mSpnState;
    UsimServiceTable mUsimServiceTable;
    VoiceMailConstants mVmConfig;
    private HashMap<String, Integer> sEventIdMap;

    private enum GetSpnFsmState {
        IDLE,
        INIT,
        READ_SPN_3GPP,
        READ_SPN_CPHS,
        READ_SPN_SHORT_CPHS
    }

    private class EfPlLoaded implements IccRecordLoaded {
        private EfPlLoaded() {
        }

        public String getEfName() {
            return "EF_PL";
        }

        public void onRecordLoaded(AsyncResult ar) {
            SIMRecords.this.mEfPl = (byte[]) ar.result;
            SIMRecords sIMRecords = SIMRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_PL=");
            stringBuilder.append(IccUtils.bytesToHexString(SIMRecords.this.mEfPl));
            sIMRecords.log(stringBuilder.toString());
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
            SIMRecords sIMRecords = SIMRecords.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EF_LI=");
            stringBuilder.append(IccUtils.bytesToHexString(SIMRecords.this.mEfLi));
            sIMRecords.log(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SimRecords: ");
        stringBuilder.append(super.toString());
        stringBuilder.append(" mVmConfig");
        stringBuilder.append(this.mVmConfig);
        stringBuilder.append(" callForwardingEnabled=");
        stringBuilder.append(this.mCallForwardingStatus);
        stringBuilder.append(" spnState=");
        stringBuilder.append(this.mSpnState);
        stringBuilder.append(" mCphsInfo=");
        stringBuilder.append(this.mCphsInfo);
        stringBuilder.append(" mCspPlmnEnabled=");
        stringBuilder.append(this.mCspPlmnEnabled);
        stringBuilder.append(" efMWIS=");
        stringBuilder.append(this.mEfMWIS);
        stringBuilder.append(" efCPHS_MWI=");
        stringBuilder.append(this.mEfCPHS_MWI);
        stringBuilder.append(" mEfCff=");
        stringBuilder.append(this.mEfCff);
        stringBuilder.append(" mEfCfis=");
        stringBuilder.append(this.mEfCfis);
        stringBuilder.append(" getOperatorNumeric=");
        stringBuilder.append(getOperatorNumeric());
        return stringBuilder.toString();
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
        this.mHwCustSIMRecords = null;
        this.sEventIdMap = new HashMap();
        this.mAdnCache = HwTelephonyFactory.getHwUiccManager().createHwAdnRecordCache(this.mFh);
        this.mVmConfig = (VoiceMailConstants) HwTelephonyFactory.getHwUiccManager().createHwVoiceMailConstants(c, getSlotId());
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        this.mCi.setOnSmsOnSim(this, 21, null);
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 258, null);
        this.mParentApp.registerForNetworkLocked(this, 259, null);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SIMRecords X ctor this=");
        stringBuilder.append(this);
        log(stringBuilder.toString());
    }

    public void dispose() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disposing SIMRecords this=");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        this.mCi.unSetOnSmsOnSim(this);
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        this.mParentApp.unregisterForNetworkLocked(this);
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setting0 mMncLength");
        stringBuilder.append(this.mMncLength);
        log(stringBuilder.toString());
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
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("update icc_operator_numeric=");
        stringBuilder2.append(null);
        log(stringBuilder2.toString());
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), "");
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
        this.mImsiLoad = false;
    }

    public String getMsisdnNumber() {
        if (!NEED_CHECK_MSISDN_BIT || this.mUsimServiceTable == null || this.mUsimServiceTable.isAvailable(UsimService.MSISDN)) {
            return this.mMsisdn;
        }
        log("EF_MSISDN not available");
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return this.mUsimServiceTable;
    }

    private int getExtFromEf(int ef) {
        if (ef != IccConstants.EF_MSISDN) {
            return IccConstants.EF_EXT1;
        }
        if (this.mParentApp.getType() == AppType.APPTYPE_USIM) {
            return IccConstants.EF_EXT5;
        }
        return IccConstants.EF_EXT1;
    }

    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        this.mNewMsisdn = number;
        this.mNewMsisdnTag = alphaTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Set MSISDN: ");
        stringBuilder.append(this.mNewMsisdnTag);
        stringBuilder.append(" ");
        stringBuilder.append(Rlog.pii(LOG_TAG, this.mNewMsisdn));
        log(stringBuilder.toString());
        new AdnRecordLoader(this.mFh).updateEF(new AdnRecord(this.mNewMsisdnTag, this.mNewMsisdn), IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, null, obtainMessage(30, onComplete));
    }

    public String getMsisdnAlphaTag() {
        if (!NEED_CHECK_MSISDN_BIT || this.mUsimServiceTable == null || this.mUsimServiceTable.isAvailable(UsimService.MSISDN)) {
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
        Message message = onComplete;
        if (this.mIsVoiceMailFixed) {
            AsyncResult.forMessage(onComplete).exception = new IccVmFixedException("Voicemail number is fixed by operator");
            onComplete.sendToTarget();
            return;
        }
        this.mNewVoiceMailNum = voiceNumber;
        this.mNewVoiceMailTag = alphaTag;
        boolean custvmNotToSim = false;
        Boolean editvmnottosim = (Boolean) HwCfgFilePolicy.getValue("vm_edit_not_to_sim_bool", SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mParentApp.getPhoneId()), Boolean.class);
        if (editvmnottosim != null) {
            custvmNotToSim = editvmnottosim.booleanValue();
        }
        if (custvmNotToSim) {
            log("Don't need to edit to SIM");
            AsyncResult.forMessage(onComplete).exception = new IccVmNotSupportedException("Voicemail number can't be edit to SIM");
            onComplete.sendToTarget();
            return;
        }
        AdnRecord adn = new AdnRecord(this.mNewVoiceMailTag, this.mNewVoiceMailNum);
        if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
            new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, null, obtainMessage(20, message));
        } else if (isCphsMailboxEnabled()) {
            new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, message));
        } else {
            AsyncResult.forMessage(onComplete).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
            onComplete.sendToTarget();
        }
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
        if (line == 1) {
            try {
                if (this.mEfMWIS != null) {
                    this.mEfMWIS[0] = (byte) ((this.mEfMWIS[0] & LastCallFailCause.RADIO_LINK_FAILURE) | (countWaiting == 0 ? 0 : 1));
                    if (countWaiting < 0) {
                        this.mEfMWIS[1] = (byte) 0;
                    } else {
                        this.mEfMWIS[1] = (byte) countWaiting;
                    }
                    this.mFh.updateEFLinearFixed(IccConstants.EF_MWIS, 1, this.mEfMWIS, null, obtainMessage(14, IccConstants.EF_MWIS, 0));
                }
                if (this.mEfCPHS_MWI != null) {
                    this.mEfCPHS_MWI[0] = (byte) ((this.mEfCPHS_MWI[0] & 240) | (countWaiting == 0 ? 5 : 10));
                    this.mFh.updateEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, this.mEfCPHS_MWI, obtainMessage(14, Integer.valueOf(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS)));
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                logw("Error saving voice mail state to SIM. Probably malformed SIM record", ex);
            }
        }
    }

    private boolean validEfCfis(byte[] data) {
        if (data != null) {
            if (data[0] < (byte) 1 || data[0] > (byte) 4) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("MSP byte: ");
                stringBuilder.append(data[0]);
                stringBuilder.append(" is not between 1 and 4");
                logw(stringBuilder.toString(), null);
            }
            for (byte b : data) {
                if (b != (byte) -1) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getVoiceMessageCount() {
        int countVoiceMessages = -2;
        boolean z = false;
        if (this.mEfMWIS != null) {
            if ((this.mEfMWIS[0] & 1) != 0) {
                z = true;
            }
            countVoiceMessages = this.mEfMWIS[1] & 255;
            if (z && (countVoiceMessages == 0 || countVoiceMessages == 255)) {
                countVoiceMessages = -1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" VoiceMessageCount from SIM MWIS = ");
            stringBuilder.append(countVoiceMessages);
            log(stringBuilder.toString());
        } else if (this.mEfCPHS_MWI != null) {
            int indicator = this.mEfCPHS_MWI[0] & 15;
            if (indicator == 10) {
                countVoiceMessages = -1;
            } else if (indicator == 5) {
                countVoiceMessages = 0;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" VoiceMessageCount from SIM CPHS = ");
            stringBuilder2.append(countVoiceMessages);
            log(stringBuilder2.toString());
        }
        return countVoiceMessages;
    }

    public int getVoiceCallForwardingFlag() {
        return this.mCallForwardingStatus;
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String dialNumber) {
        if (line == 1) {
            this.mCallForwardingStatus = enable ? 1 : 0;
            this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(1));
            try {
                if (validEfCfis(this.mEfCfis)) {
                    byte[] bArr;
                    if (enable) {
                        bArr = this.mEfCfis;
                        bArr[1] = (byte) (bArr[1] | 1);
                    } else {
                        bArr = this.mEfCfis;
                        bArr[1] = (byte) (bArr[1] & LastCallFailCause.RADIO_LINK_FAILURE);
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setVoiceCallForwardingFlag: enable=");
                    stringBuilder.append(enable);
                    stringBuilder.append(" mEfCfis=");
                    stringBuilder.append(IccUtils.bytesToHexString(this.mEfCfis));
                    log(stringBuilder.toString());
                    if (enable && !TextUtils.isEmpty(dialNumber)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EF_CFIS: updating cf number, ");
                        stringBuilder.append(Rlog.pii(LOG_TAG, dialNumber));
                        logv(stringBuilder.toString());
                        byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(dialNumber, 1);
                        System.arraycopy(bcdNumber, 0, this.mEfCfis, 3, bcdNumber.length);
                        this.mEfCfis[2] = (byte) bcdNumber.length;
                        this.mEfCfis[14] = (byte) -1;
                        this.mEfCfis[15] = (byte) -1;
                    }
                    this.mFh.updateEFLinearFixed(IccConstants.EF_CFIS, 1, this.mEfCfis, null, obtainMessage(14, Integer.valueOf(IccConstants.EF_CFIS)));
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setVoiceCallForwardingFlag: ignoring enable=");
                    stringBuilder2.append(enable);
                    stringBuilder2.append(" invalid mEfCfis=");
                    stringBuilder2.append(IccUtils.bytesToHexString(this.mEfCfis));
                    log(stringBuilder2.toString());
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
            } catch (RuntimeException e) {
                loge("Error saving call forwarding flag to SIM. Probably malformed dialNumber");
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
                            return this.mImsi.substring(0, 3 + this.mMncLength);
                        }
                    }
                }
            }
            return null;
        } else if (imsi.length() >= this.mMncLength + 3) {
            return imsi.substring(0, 3 + this.mMncLength);
        } else {
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:784:0x1392, code skipped:
            if (r3 != false) goto L_0x1394;
     */
    /* JADX WARNING: Missing block: B:785:0x1394, code skipped:
            onRecordLoaded();
     */
    /* JADX WARNING: Missing block: B:790:0x13a0, code skipped:
            if (r3 == false) goto L_0x13a3;
     */
    /* JADX WARNING: Missing block: B:791:0x13a3, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        int length;
        int i;
        String[] strArr;
        String str;
        Message message = msg;
        boolean isRecordLoadResponse = false;
        StringBuilder stringBuilder;
        if (this.mDestroyed.get()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Received message[");
            stringBuilder.append(message.what);
            stringBuilder.append("], Ignoring.");
            loge(stringBuilder.toString());
            return;
        }
        try {
            int i2 = message.what;
            AsyncResult ar;
            if (i2 == 1) {
                onReady();
            } else if (i2 != 30) {
                boolean z = false;
                byte[] data;
                StringBuilder stringBuilder2;
                StringBuilder stringBuilder3;
                AdnRecord adn;
                StringBuilder stringBuilder4;
                StringBuilder stringBuilder5;
                String mccmncCode;
                String str2;
                int slotId;
                StringBuilder stringBuilder6;
                switch (i2) {
                    case 3:
                        isRecordLoadResponse = true;
                        onGetImsiDone(msg);
                        break;
                    case 4:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        data = (byte[]) ar.result;
                        if (ar.exception == null) {
                            this.mIccId = HwTelephonyFactory.getHwUiccManager().bcdIccidToString(data, 0, data.length);
                            this.mFullIccId = IccUtils.bchToString(data, 0, data.length);
                            onIccIdLoadedHw();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("iccid: ");
                            stringBuilder2.append(SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                            log(stringBuilder2.toString());
                            this.mIccIDLoadRegistrants.notifyRegistrants(ar);
                            break;
                        }
                        this.mIccIDLoadRegistrants.notifyRegistrants(ar);
                        break;
                    case 5:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        data = ar.result;
                        boolean isValidMbdn = false;
                        if (ar.exception == null) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("EF_MBI: ");
                            stringBuilder3.append(IccUtils.bytesToHexString(data));
                            log(stringBuilder3.toString());
                            this.mMailboxIndex = data[0] & 255;
                            if (!(this.mMailboxIndex == 0 || this.mMailboxIndex == 255)) {
                                log("Got valid mailbox number for MBDN");
                                isValidMbdn = true;
                            }
                        }
                        this.mRecordsToLoad++;
                        if (!isValidMbdn) {
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            break;
                        } else {
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                            break;
                        }
                    case 6:
                    case 11:
                        this.mVoiceMailNum = null;
                        this.mVoiceMailTag = null;
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        if (ar.exception == null) {
                            adn = ar.result;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("VM: ");
                            stringBuilder2.append(adn);
                            stringBuilder2.append(message.what == 11 ? " EF[MAILBOX]" : " EF[MBDN]");
                            log(stringBuilder2.toString());
                            if (!adn.isEmpty() || message.what != 6) {
                                this.mVoiceMailNum = adn.getNumber();
                                this.mVoiceMailTag = adn.getAlphaTag();
                                this.mVmConfig.setVoicemailOnSIM(this.mVoiceMailNum, this.mVoiceMailTag);
                                break;
                            }
                            this.mRecordsToLoad++;
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            break;
                        }
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Invalid or missing EF");
                        stringBuilder4.append(message.what == 11 ? "[MAILBOX]" : "[MBDN]");
                        log(stringBuilder4.toString());
                        if (message.what == 6) {
                            this.mRecordsToLoad++;
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            break;
                        }
                        break;
                    case 7:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        data = (byte[]) ar.result;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("EF_MWIS : ");
                        stringBuilder5.append(IccUtils.bytesToHexString(data));
                        log(stringBuilder5.toString());
                        if (ar.exception == null) {
                            if ((data[0] & 255) != 255) {
                                if (!SystemProperties.getBoolean("ro.config.hw_eeVoiceMsgCount", false) || (data[0] & 255) != 0) {
                                    this.mEfMWIS = data;
                                    break;
                                }
                                this.mEfMWIS = null;
                                Rlog.d(LOG_TAG, "SIMRecords EE VoiceMessageCount from SIM CPHS");
                                break;
                            }
                            log("SIMRecords: Uninitialized record MWIS");
                            break;
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("EVENT_GET_MWIS_DONE exception = ");
                        stringBuilder2.append(ar.exception);
                        log(stringBuilder2.toString());
                        break;
                        break;
                    case 8:
                        isRecordLoadResponse = true;
                        ar = message.obj;
                        data = ar.result;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("EF_CPHS_MWI: ");
                        stringBuilder2.append(IccUtils.bytesToHexString(data));
                        log(stringBuilder2.toString());
                        if (ar.exception == null) {
                            this.mEfCPHS_MWI = data;
                            break;
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE exception = ");
                        stringBuilder2.append(ar.exception);
                        log(stringBuilder2.toString());
                        break;
                    case 9:
                        String mccmncCode2;
                        String[] strArr2;
                        int mcc;
                        int mcc2;
                        String[] strArr3;
                        isRecordLoadResponse = true;
                        StringBuilder stringBuilder7;
                        try {
                            if (!this.mCarrierTestOverride.isInTestMode() || getIMSI() == null) {
                                int length2;
                                int length3;
                                AsyncResult ar2 = (AsyncResult) message.obj;
                                IccIoResult result = (IccIoResult) ar2.result;
                                IccException iccException = result.getException();
                                if (ar2.exception == null) {
                                    if (iccException == null) {
                                        int length4;
                                        int i3;
                                        if (result.payload == null) {
                                            log("result.payload is null");
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                                                mccmncCode2 = this.mImsi.substring(0, 6);
                                                strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                                length4 = strArr2.length;
                                                i3 = 0;
                                                while (i3 < length4) {
                                                    if (strArr2[i3].equals(mccmncCode2)) {
                                                        this.mMncLength = 3;
                                                        stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("setting6 mMncLength=");
                                                        stringBuilder5.append(this.mMncLength);
                                                        log(stringBuilder5.toString());
                                                    } else {
                                                        i3++;
                                                    }
                                                }
                                            }
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                                                mccmncCode2 = this.mImsi.substring(0, 3);
                                                if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                                    mccmncCode = this.mImsi.substring(0, 5);
                                                    strArr2 = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                                    length2 = strArr2.length;
                                                    length4 = 0;
                                                    while (length4 < length2) {
                                                        if (strArr2[length4].equals(mccmncCode)) {
                                                            this.mMncLength = 2;
                                                        } else {
                                                            length4++;
                                                        }
                                                    }
                                                }
                                                custMncLength(this.mImsi.substring(0, 3));
                                            }
                                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                                if (this.mImsi != null) {
                                                    try {
                                                        mccmncCode2 = this.mImsi.substring(0, 3);
                                                        if (!mccmncCode2.equals("404")) {
                                                            if (!mccmncCode2.equals("405")) {
                                                                mcc = Integer.parseInt(mccmncCode2);
                                                                str2 = LOG_TAG;
                                                                stringBuilder3 = new StringBuilder();
                                                                stringBuilder3.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                                                stringBuilder3.append(mcc);
                                                                Rlog.d(str2, stringBuilder3.toString());
                                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                                            }
                                                        }
                                                        this.mMncLength = 3;
                                                    } catch (NumberFormatException e) {
                                                        this.mMncLength = 0;
                                                        loge("Corrupt IMSI!");
                                                    }
                                                } else {
                                                    this.mMncLength = 0;
                                                    log("MNC length not present in EF_AD");
                                                }
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                                                stringBuilder.append(getOperatorNumeric());
                                                log(stringBuilder.toString());
                                                setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                                                setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("update mccmnc=");
                                                stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                                                log(stringBuilder.toString());
                                                updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                                                z = true;
                                            }
                                            if (z && !this.mImsiLoad) {
                                                this.mImsiLoad = true;
                                                this.mParentApp.notifyGetAdDone(null);
                                                onOperatorNumericLoadedHw();
                                            }
                                            mcc = getSlotId();
                                            initFdnPsStatus(mcc);
                                            break;
                                        }
                                        data = result.payload;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("EF_AD: ");
                                        stringBuilder.append(IccUtils.bytesToHexString(data));
                                        log(stringBuilder.toString());
                                        String[] strArr4;
                                        String str3;
                                        StringBuilder stringBuilder8;
                                        if (data.length < 3) {
                                            log("Corrupt AD data on SIM");
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                                                mccmncCode2 = this.mImsi.substring(0, 6);
                                                strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                                length4 = strArr2.length;
                                                i3 = 0;
                                                while (i3 < length4) {
                                                    if (strArr2[i3].equals(mccmncCode2)) {
                                                        this.mMncLength = 3;
                                                        stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("setting6 mMncLength=");
                                                        stringBuilder5.append(this.mMncLength);
                                                        log(stringBuilder5.toString());
                                                    } else {
                                                        i3++;
                                                    }
                                                }
                                            }
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                                                mccmncCode2 = this.mImsi.substring(0, 3);
                                                if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                                    str2 = this.mImsi.substring(0, 5);
                                                    strArr4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                                    length2 = strArr4.length;
                                                    length4 = 0;
                                                    while (length4 < length2) {
                                                        if (strArr4[length4].equals(str2)) {
                                                            this.mMncLength = 2;
                                                        } else {
                                                            length4++;
                                                        }
                                                    }
                                                }
                                                custMncLength(this.mImsi.substring(0, 3));
                                            }
                                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                                if (this.mImsi != null) {
                                                    try {
                                                        mccmncCode2 = this.mImsi.substring(0, 3);
                                                        if (!mccmncCode2.equals("404")) {
                                                            if (!mccmncCode2.equals("405")) {
                                                                mcc2 = Integer.parseInt(mccmncCode2);
                                                                str3 = LOG_TAG;
                                                                stringBuilder8 = new StringBuilder();
                                                                stringBuilder8.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                                                stringBuilder8.append(mcc2);
                                                                Rlog.d(str3, stringBuilder8.toString());
                                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc2);
                                                            }
                                                        }
                                                        this.mMncLength = 3;
                                                    } catch (NumberFormatException e2) {
                                                        this.mMncLength = 0;
                                                        loge("Corrupt IMSI!");
                                                    }
                                                } else {
                                                    this.mMncLength = 0;
                                                    log("MNC length not present in EF_AD");
                                                }
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                                                stringBuilder.append(getOperatorNumeric());
                                                log(stringBuilder.toString());
                                                setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                                                setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("update mccmnc=");
                                                stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                                                log(stringBuilder.toString());
                                                updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                                                z = true;
                                            }
                                            if (z && !this.mImsiLoad) {
                                                this.mImsiLoad = true;
                                                this.mParentApp.notifyGetAdDone(null);
                                                onOperatorNumericLoadedHw();
                                            }
                                            slotId = getSlotId();
                                        } else if (data.length == 3) {
                                            log("MNC length not present in EF_AD");
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                                                mccmncCode2 = this.mImsi.substring(0, 6);
                                                strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                                length3 = strArr2.length;
                                                length4 = 0;
                                                while (length4 < length3) {
                                                    if (strArr2[length4].equals(mccmncCode2)) {
                                                        this.mMncLength = 3;
                                                        stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("setting6 mMncLength=");
                                                        stringBuilder5.append(this.mMncLength);
                                                        log(stringBuilder5.toString());
                                                    } else {
                                                        length4++;
                                                    }
                                                }
                                            }
                                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                                                mccmncCode2 = this.mImsi.substring(0, 3);
                                                if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                                    str2 = this.mImsi.substring(0, 5);
                                                    strArr4 = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                                    length2 = strArr4.length;
                                                    length4 = 0;
                                                    while (length4 < length2) {
                                                        if (strArr4[length4].equals(str2)) {
                                                            this.mMncLength = 2;
                                                        } else {
                                                            length4++;
                                                        }
                                                    }
                                                }
                                                custMncLength(this.mImsi.substring(0, 3));
                                            }
                                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                                if (this.mImsi != null) {
                                                    try {
                                                        mccmncCode2 = this.mImsi.substring(0, 3);
                                                        if (!mccmncCode2.equals("404")) {
                                                            if (!mccmncCode2.equals("405")) {
                                                                mcc2 = Integer.parseInt(mccmncCode2);
                                                                str3 = LOG_TAG;
                                                                stringBuilder8 = new StringBuilder();
                                                                stringBuilder8.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                                                stringBuilder8.append(mcc2);
                                                                Rlog.d(str3, stringBuilder8.toString());
                                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc2);
                                                            }
                                                        }
                                                        this.mMncLength = 3;
                                                    } catch (NumberFormatException e3) {
                                                        this.mMncLength = 0;
                                                        loge("Corrupt IMSI!");
                                                    }
                                                } else {
                                                    this.mMncLength = 0;
                                                    log("MNC length not present in EF_AD");
                                                }
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                                                stringBuilder.append(getOperatorNumeric());
                                                log(stringBuilder.toString());
                                                setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                                                setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("update mccmnc=");
                                                stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                                                log(stringBuilder.toString());
                                                updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                                            }
                                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                                                z = true;
                                            }
                                            if (z && !this.mImsiLoad) {
                                                this.mImsiLoad = true;
                                                this.mParentApp.notifyGetAdDone(null);
                                                onOperatorNumericLoadedHw();
                                            }
                                            slotId = getSlotId();
                                        } else {
                                            this.mMncLength = data[3] & 15;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("setting4 mMncLength=");
                                            stringBuilder.append(this.mMncLength);
                                            log(stringBuilder.toString());
                                        }
                                        initFdnPsStatus(slotId);
                                    }
                                }
                                log("read EF_AD exception occurs");
                                if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                                    mccmncCode2 = this.mImsi.substring(0, 6);
                                    strArr3 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    mcc2 = strArr3.length;
                                    length3 = 0;
                                    while (length3 < mcc2) {
                                        if (strArr3[length3].equals(mccmncCode2)) {
                                            this.mMncLength = 3;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("setting6 mMncLength=");
                                            stringBuilder4.append(this.mMncLength);
                                            log(stringBuilder4.toString());
                                        } else {
                                            length3++;
                                        }
                                    }
                                }
                                if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                                    mccmncCode2 = this.mImsi.substring(0, 3);
                                    if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                        mccmncCode = this.mImsi.substring(0, 5);
                                        strArr2 = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                        length3 = strArr2.length;
                                        length2 = 0;
                                        while (length2 < length3) {
                                            if (strArr2[length2].equals(mccmncCode)) {
                                                this.mMncLength = 2;
                                            } else {
                                                length2++;
                                            }
                                        }
                                    }
                                    custMncLength(this.mImsi.substring(0, 3));
                                }
                                if (this.mMncLength == 0 || this.mMncLength == -1) {
                                    if (this.mImsi != null) {
                                        try {
                                            mccmncCode2 = this.mImsi.substring(0, 3);
                                            if (!mccmncCode2.equals("404")) {
                                                if (!mccmncCode2.equals("405")) {
                                                    mcc = Integer.parseInt(mccmncCode2);
                                                    str2 = LOG_TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                                    stringBuilder3.append(mcc);
                                                    Rlog.d(str2, stringBuilder3.toString());
                                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                                }
                                            }
                                            this.mMncLength = 3;
                                        } catch (NumberFormatException e4) {
                                            this.mMncLength = 0;
                                            loge("Corrupt IMSI!");
                                        }
                                    } else {
                                        this.mMncLength = 0;
                                        log("MNC length not present in EF_AD");
                                    }
                                }
                                if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                                    stringBuilder.append(getOperatorNumeric());
                                    log(stringBuilder.toString());
                                    setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                                    setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("update mccmnc=");
                                    stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                                    log(stringBuilder.toString());
                                    updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                                }
                                if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                                    z = true;
                                }
                                if (z && !this.mImsiLoad) {
                                    this.mImsiLoad = true;
                                    this.mParentApp.notifyGetAdDone(null);
                                    onOperatorNumericLoadedHw();
                                }
                                mcc = getSlotId();
                                initFdnPsStatus(mcc);
                            } else {
                                this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(getIMSI().substring(0, 3)));
                                stringBuilder7 = new StringBuilder();
                                stringBuilder7.append("[TestMode] mMncLength=");
                                stringBuilder7.append(this.mMncLength);
                                log(stringBuilder7.toString());
                            }
                        } catch (NumberFormatException e5) {
                            this.mMncLength = 0;
                            stringBuilder7 = new StringBuilder();
                            stringBuilder7.append("[TestMode] Corrupt IMSI! mMncLength=");
                            stringBuilder7.append(this.mMncLength);
                            loge(stringBuilder7.toString());
                        } catch (Throwable th) {
                            Throwable th2 = th;
                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                                mccmncCode2 = this.mImsi.substring(0, 6);
                                strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                length = strArr2.length;
                                i = 0;
                                while (i < length) {
                                    if (strArr2[i].equals(mccmncCode2)) {
                                        this.mMncLength = 3;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("setting6 mMncLength=");
                                        stringBuilder5.append(this.mMncLength);
                                        log(stringBuilder5.toString());
                                    } else {
                                        i++;
                                    }
                                }
                            }
                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                                mccmncCode2 = this.mImsi.substring(0, 3);
                                if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                    str2 = this.mImsi.substring(0, 5);
                                    strArr = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                    i = strArr.length;
                                    int i4 = 0;
                                    while (i4 < i) {
                                        if (strArr[i4].equals(str2)) {
                                            this.mMncLength = 2;
                                        } else {
                                            i4++;
                                        }
                                    }
                                }
                                custMncLength(this.mImsi.substring(0, 3));
                            }
                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                if (this.mImsi != null) {
                                    try {
                                        mccmncCode2 = this.mImsi.substring(0, 3);
                                        if (!mccmncCode2.equals("404")) {
                                            if (!mccmncCode2.equals("405")) {
                                                mcc2 = Integer.parseInt(mccmncCode2);
                                                str = LOG_TAG;
                                                stringBuilder7 = new StringBuilder();
                                                stringBuilder7.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                                stringBuilder7.append(mcc2);
                                                Rlog.d(str, stringBuilder7.toString());
                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc2);
                                            }
                                        }
                                        this.mMncLength = 3;
                                    } catch (NumberFormatException e6) {
                                        this.mMncLength = 0;
                                        loge("Corrupt IMSI!");
                                    }
                                } else {
                                    this.mMncLength = 0;
                                    log("MNC length not present in EF_AD");
                                }
                            }
                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                                stringBuilder.append(getOperatorNumeric());
                                log(stringBuilder.toString());
                                setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                                setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("update mccmnc=");
                                stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                                log(stringBuilder.toString());
                                updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                            }
                            if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                                z = true;
                            }
                            if (z && !this.mImsiLoad) {
                                this.mImsiLoad = true;
                                this.mParentApp.notifyGetAdDone(null);
                                onOperatorNumericLoadedHw();
                            }
                            initFdnPsStatus(getSlotId());
                        }
                        if (this.mMncLength == 15) {
                            this.mMncLength = 0;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("setting5 mMncLength=");
                            stringBuilder.append(this.mMncLength);
                            log(stringBuilder.toString());
                        } else if (this.mMncLength > 3) {
                            this.mMncLength = 2;
                        } else if (!(this.mMncLength == 2 || this.mMncLength == 3)) {
                            this.mMncLength = -1;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("setting5 mMncLength=");
                            stringBuilder.append(this.mMncLength);
                            log(stringBuilder.toString());
                        }
                        if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && this.mImsi != null && this.mImsi.length() >= 6) {
                            mccmncCode2 = this.mImsi.substring(0, 6);
                            strArr3 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                            mcc2 = strArr3.length;
                            length = 0;
                            while (length < mcc2) {
                                if (strArr3[length].equals(mccmncCode2)) {
                                    this.mMncLength = 3;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("setting6 mMncLength=");
                                    stringBuilder4.append(this.mMncLength);
                                    log(stringBuilder4.toString());
                                } else {
                                    length++;
                                }
                            }
                        }
                        if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                            mccmncCode2 = this.mImsi.substring(0, 3);
                            if (mccmncCode2.equals("404") || mccmncCode2.equals("405")) {
                                mccmncCode = this.mImsi.substring(0, 5);
                                strArr2 = MCCMNC_CODES_HAVING_2DIGITS_MNC;
                                length = strArr2.length;
                                i = 0;
                                while (i < length) {
                                    if (strArr2[i].equals(mccmncCode)) {
                                        this.mMncLength = 2;
                                    } else {
                                        i++;
                                    }
                                }
                            }
                            custMncLength(this.mImsi.substring(0, 3));
                        }
                        if (this.mMncLength == 0 || this.mMncLength == -1) {
                            if (this.mImsi != null) {
                                try {
                                    mccmncCode2 = this.mImsi.substring(0, 3);
                                    if (!mccmncCode2.equals("404")) {
                                        if (!mccmncCode2.equals("405")) {
                                            mcc = Integer.parseInt(mccmncCode2);
                                            str2 = LOG_TAG;
                                            stringBuilder6 = new StringBuilder();
                                            stringBuilder6.append("SIMRecords: AD err, mcc is determing mnc length in error case::");
                                            stringBuilder6.append(mcc);
                                            Rlog.d(str2, stringBuilder6.toString());
                                            this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                        }
                                    }
                                    this.mMncLength = 3;
                                } catch (NumberFormatException e7) {
                                    this.mMncLength = 0;
                                    loge("Corrupt IMSI!");
                                }
                            } else {
                                this.mMncLength = 0;
                                log("MNC length not present in EF_AD");
                            }
                        }
                        if (!(this.mImsi == null || this.mMncLength == 0 || this.mImsi.length() < this.mMncLength + 3)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("GET_AD_DONE setSystemProperty simOperator=");
                            stringBuilder.append(getOperatorNumeric());
                            log(stringBuilder.toString());
                            setSystemProperty("gsm.sim.operator.numeric", getOperatorNumeric());
                            setSystemProperty(IccRecords.PROPERTY_MCC_MATCHING_FYROM, getOperatorNumeric());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("update mccmnc=");
                            stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
                            log(stringBuilder.toString());
                            updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
                        }
                        if (!(this.mImsi == null || this.mMncLength == 0 || this.mMncLength == -1)) {
                            z = true;
                        }
                        if (z && !this.mImsiLoad) {
                            this.mImsiLoad = true;
                            this.mParentApp.notifyGetAdDone(null);
                            onOperatorNumericLoadedHw();
                        }
                        initFdnPsStatus(getSlotId());
                        break;
                    case 10:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        if (ar.exception == null) {
                            adn = ar.result;
                            this.mMsisdn = adn.getNumber();
                            this.mMsisdnTag = adn.getAlphaTag();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("MSISDN isempty:");
                            stringBuilder2.append(TextUtils.isEmpty(this.mMsisdn));
                            log(stringBuilder2.toString());
                            break;
                        }
                        log("Invalid or missing EF[MSISDN]");
                        break;
                    case 12:
                        isRecordLoadResponse = true;
                        getSpnFsm(false, (AsyncResult) message.obj);
                        if (GetSpnFsmState.IDLE == this.mSpnState) {
                            updateCarrierFile(getSlotId(), 7, getServiceProviderName());
                        }
                        if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 3) && this.mImsi != null && this.mImsi.length() >= 5) {
                            mccmncCode = this.mImsi.substring(0, 3);
                            if (mccmncCode.equals("404") || mccmncCode.equals("405")) {
                                str2 = this.mImsi.substring(0, 5);
                                if (!TextUtils.isEmpty(getServiceProviderName()) && getServiceProviderName().toLowerCase(Locale.US).contains("reliance")) {
                                    strArr = MCCMNC_CODES_HAVING_2DIGITS_MNC_ZERO_PREFIX_RELIANCE;
                                    i = strArr.length;
                                    while (slotId < i) {
                                        if (strArr[slotId].equals(str2)) {
                                            this.mMncLength = 2;
                                        } else {
                                            slotId++;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    case 13:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        data = (byte[]) ar.result;
                        if (ar.exception == null) {
                            parseEfSpdi(data);
                            break;
                        }
                        break;
                    case 14:
                        ar = (AsyncResult) message.obj;
                        if (ar.exception != null) {
                            logw("update failed. ", ar.exception);
                            break;
                        }
                        break;
                    case 15:
                        isRecordLoadResponse = true;
                        ar = (AsyncResult) message.obj;
                        data = ar.result;
                        if (ar.exception == null) {
                            SimTlv tlv = new SimTlv(data, 0, data.length);
                            while (tlv.isValidObject()) {
                                if (tlv.getTag() == 67) {
                                    this.mPnnHomeName = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("PNN: ");
                                    stringBuilder2.append(this.mPnnHomeName);
                                    log(stringBuilder2.toString());
                                    break;
                                }
                                tlv.nextObject();
                            }
                            break;
                        }
                        break;
                    default:
                        switch (i2) {
                            case 17:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) message.obj;
                                byte[] data2 = ar.result;
                                if (ar.exception == null) {
                                    this.mUsimServiceTable = new UsimServiceTable(data2);
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("SST: ");
                                    stringBuilder5.append(this.mUsimServiceTable);
                                    log(stringBuilder5.toString());
                                    if (checkFileInServiceTable(IccConstants.EF_SPN, this.mUsimServiceTable, data2)) {
                                        getSpnFsm(true, null);
                                    } else {
                                        updateCarrierFile(getSlotId(), 7, null);
                                    }
                                    checkFileInServiceTable(IccConstants.EF_PNN, this.mUsimServiceTable, data2);
                                    break;
                                }
                                updateCarrierFile(getSlotId(), 7, null);
                                break;
                            case 18:
                                isRecordLoadResponse = true;
                                ar = (AsyncResult) message.obj;
                                if (ar.exception == null) {
                                    handleSmses((ArrayList) ar.result);
                                    break;
                                }
                                break;
                            case 19:
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("marked read: sms ");
                                stringBuilder4.append(message.arg1);
                                Rlog.i("ENF", stringBuilder4.toString());
                                break;
                            case 20:
                                isRecordLoadResponse = false;
                                ar = (AsyncResult) message.obj;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("EVENT_SET_MBDN_DONE ex:");
                                stringBuilder4.append(ar.exception);
                                log(stringBuilder4.toString());
                                if (ar.exception == null) {
                                    this.mVoiceMailNum = this.mNewVoiceMailNum;
                                    this.mVoiceMailTag = this.mNewVoiceMailTag;
                                }
                                if (!isCphsMailboxEnabled()) {
                                    if (ar.userObj != null) {
                                        CarrierConfigManager configLoader = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                                        if (ar.exception == null || configLoader == null || !configLoader.getConfig().getBoolean("editable_voicemail_number_bool")) {
                                            AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                                        } else {
                                            AsyncResult.forMessage((Message) ar.userObj).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
                                        }
                                        ((Message) ar.userObj).sendToTarget();
                                        break;
                                    }
                                }
                                AdnRecord adn2 = new AdnRecord(this.mVoiceMailTag, this.mVoiceMailNum);
                                Message onCphsCompleted = ar.userObj;
                                if (ar.exception == null && ar.userObj != null) {
                                    AsyncResult.forMessage((Message) ar.userObj).exception = null;
                                    ((Message) ar.userObj).sendToTarget();
                                    log("Callback with MBDN successful.");
                                    onCphsCompleted = null;
                                }
                                new AdnRecordLoader(this.mFh).updateEF(adn2, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, onCphsCompleted));
                                break;
                                break;
                            case 21:
                                isRecordLoadResponse = false;
                                ar = (AsyncResult) message.obj;
                                Integer index = ar.result;
                                if (ar.exception == null) {
                                    if (index != null) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("READ EF_SMS RECORD index=");
                                        stringBuilder2.append(index);
                                        log(stringBuilder2.toString());
                                        this.mFh.loadEFLinearFixed(IccConstants.EF_SMS, index.intValue(), obtainMessage(22));
                                        break;
                                    }
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Error on SMS_ON_SIM with exp ");
                                stringBuilder2.append(ar.exception);
                                stringBuilder2.append(" index ");
                                stringBuilder2.append(index);
                                loge(stringBuilder2.toString());
                                break;
                            case 22:
                                isRecordLoadResponse = false;
                                ar = (AsyncResult) message.obj;
                                if (ar.exception != null) {
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Error on GET_SMS with exp ");
                                    stringBuilder4.append(ar.exception);
                                    loge(stringBuilder4.toString());
                                    break;
                                }
                                handleSms((byte[]) ar.result);
                                break;
                            default:
                                switch (i2) {
                                    case 24:
                                        isRecordLoadResponse = true;
                                        ar = (AsyncResult) message.obj;
                                        data = (byte[]) ar.result;
                                        if (ar.exception == null) {
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("EF_CFF_CPHS: ");
                                            stringBuilder2.append(IccUtils.bytesToHexString(data));
                                            log(stringBuilder2.toString());
                                            this.mEfCff = data;
                                            break;
                                        }
                                        this.mEfCff = null;
                                        break;
                                    case 25:
                                        isRecordLoadResponse = false;
                                        ar = (AsyncResult) message.obj;
                                        if (ar.exception == null) {
                                            this.mVoiceMailNum = this.mNewVoiceMailNum;
                                            this.mVoiceMailTag = this.mNewVoiceMailTag;
                                        } else {
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("Set CPHS MailBox with exception: ");
                                            stringBuilder4.append(ar.exception);
                                            log(stringBuilder4.toString());
                                        }
                                        if (ar.userObj != null) {
                                            log("Callback with CPHS MB successful.");
                                            AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                                            ((Message) ar.userObj).sendToTarget();
                                            break;
                                        }
                                        break;
                                    case 26:
                                        isRecordLoadResponse = true;
                                        ar = (AsyncResult) message.obj;
                                        if (ar.exception == null) {
                                            this.mCphsInfo = (byte[]) ar.result;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("iCPHS: ");
                                            stringBuilder4.append(IccUtils.bytesToHexString(this.mCphsInfo));
                                            log(stringBuilder4.toString());
                                            break;
                                        }
                                        break;
                                    default:
                                        switch (i2) {
                                            case 32:
                                                isRecordLoadResponse = true;
                                                ar = (AsyncResult) message.obj;
                                                byte[] data3 = ar.result;
                                                if (ar.exception == null) {
                                                    stringBuilder6 = new StringBuilder();
                                                    stringBuilder6.append("EF_CFIS: ");
                                                    stringBuilder6.append(IccUtils.bytesToHexString(data3));
                                                    log(stringBuilder6.toString());
                                                    if (!validEfCfis(data3)) {
                                                        stringBuilder4 = new StringBuilder();
                                                        stringBuilder4.append("EF_CFIS: ");
                                                        stringBuilder4.append(IccUtils.bytesToHexString(data3));
                                                        log(stringBuilder4.toString());
                                                        break;
                                                    }
                                                    this.mEfCfis = data3;
                                                    if ((data3[1] & 1) != 0) {
                                                        slotId = 1;
                                                    }
                                                    this.mCallForwardingStatus = slotId;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("EF_CFIS: callForwardingEnabled=");
                                                    stringBuilder2.append(this.mCallForwardingStatus);
                                                    log(stringBuilder2.toString());
                                                    this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(1));
                                                    break;
                                                }
                                                this.mEfCfis = null;
                                                str = getVmSimImsi();
                                                if (this.mOriginVmImsi == null) {
                                                    this.mOriginVmImsi = str;
                                                }
                                                if (str != null && str.equals(this.mImsi)) {
                                                    if (getCallForwardingPreference()) {
                                                        this.mCallForwardingStatus = 1;
                                                        this.mRecordsEventsRegistrants.notifyResult(Integer.valueOf(1));
                                                        break;
                                                    }
                                                } else if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
                                                    setCallForwardingPreference(false);
                                                    break;
                                                } else {
                                                    if (this.mFirstImsi == null) {
                                                        this.mFirstImsi = this.mImsi;
                                                    } else if (!this.mFirstImsi.equals(this.mImsi) && this.mSecondImsi == null) {
                                                        this.mSecondImsi = this.mImsi;
                                                    }
                                                    if (!(this.mOriginVmImsi == null || this.mFirstImsi == null || this.mOriginVmImsi.equals(this.mFirstImsi) || this.mSecondImsi == null || this.mOriginVmImsi.equals(this.mSecondImsi))) {
                                                        setCallForwardingPreference(false);
                                                        break;
                                                    }
                                                }
                                                break;
                                            case 33:
                                                isRecordLoadResponse = true;
                                                ar = (AsyncResult) message.obj;
                                                if (ar.exception == null) {
                                                    data = (byte[]) ar.result;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("EF_CSP: ");
                                                    stringBuilder2.append(IccUtils.bytesToHexString(data));
                                                    log(stringBuilder2.toString());
                                                    z = this.mCspPlmnEnabled;
                                                    handleEfCspData(data);
                                                    checkSendBroadcast(z, this.mCspPlmnEnabled);
                                                    break;
                                                }
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("Exception in fetching EF_CSP data ");
                                                stringBuilder4.append(ar.exception);
                                                loge(stringBuilder4.toString());
                                                break;
                                            case 34:
                                                isRecordLoadResponse = true;
                                                ar = (AsyncResult) message.obj;
                                                data = (byte[]) ar.result;
                                                if (ar.exception == null) {
                                                    this.mGid1 = IccUtils.bytesToHexString(data);
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("GID1: ");
                                                    stringBuilder2.append(this.mGid1);
                                                    log(stringBuilder2.toString());
                                                    updateCarrierFile(getSlotId(), 5, this.mGid1);
                                                    break;
                                                }
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Exception in get GID1 ");
                                                stringBuilder2.append(ar.exception);
                                                loge(stringBuilder2.toString());
                                                this.mGid1 = null;
                                                updateCarrierFile(getSlotId(), 5, null);
                                                break;
                                            default:
                                                switch (i2) {
                                                    case 36:
                                                        isRecordLoadResponse = true;
                                                        ar = (AsyncResult) message.obj;
                                                        data = (byte[]) ar.result;
                                                        if (ar.exception == null) {
                                                            this.mGid2 = IccUtils.bytesToHexString(data);
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("GID2: ");
                                                            stringBuilder2.append(this.mGid2);
                                                            log(stringBuilder2.toString());
                                                            updateCarrierFile(getSlotId(), 6, this.mGid2);
                                                            break;
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Exception in get GID2 ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        this.mGid2 = null;
                                                        updateCarrierFile(getSlotId(), 6, null);
                                                        break;
                                                    case 37:
                                                        isRecordLoadResponse = true;
                                                        ar = (AsyncResult) message.obj;
                                                        data = (byte[]) ar.result;
                                                        if (ar.exception == null) {
                                                            if (data != null) {
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("Received a PlmnActRecord, raw=");
                                                                stringBuilder2.append(IccUtils.bytesToHexString(data));
                                                                log(stringBuilder2.toString());
                                                                this.mPlmnActRecords = PlmnActRecord.getRecords(data);
                                                                break;
                                                            }
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failed getting User PLMN with Access Tech Records: ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        break;
                                                    case 38:
                                                        isRecordLoadResponse = true;
                                                        ar = (AsyncResult) message.obj;
                                                        data = (byte[]) ar.result;
                                                        if (ar.exception == null) {
                                                            if (data != null) {
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("Received a PlmnActRecord, raw=");
                                                                stringBuilder2.append(IccUtils.bytesToHexString(data));
                                                                log(stringBuilder2.toString());
                                                                this.mOplmnActRecords = PlmnActRecord.getRecords(data);
                                                                break;
                                                            }
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failed getting Operator PLMN with Access Tech Records: ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        break;
                                                    case 39:
                                                        isRecordLoadResponse = true;
                                                        ar = (AsyncResult) message.obj;
                                                        data = (byte[]) ar.result;
                                                        if (ar.exception == null) {
                                                            if (data != null) {
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("Received a PlmnActRecord, raw=");
                                                                stringBuilder2.append(IccUtils.bytesToHexString(data));
                                                                log(stringBuilder2.toString());
                                                                this.mHplmnActRecords = PlmnActRecord.getRecords(data);
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("HplmnActRecord[]=");
                                                                stringBuilder2.append(Arrays.toString(this.mHplmnActRecords));
                                                                log(stringBuilder2.toString());
                                                                break;
                                                            }
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failed getting Home PLMN with Access Tech Records: ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        break;
                                                    case 40:
                                                        isRecordLoadResponse = true;
                                                        ar = (AsyncResult) message.obj;
                                                        data = (byte[]) ar.result;
                                                        if (ar.exception == null) {
                                                            if (data != null) {
                                                                this.mEhplmns = parseBcdPlmnList(data, "Equivalent Home");
                                                                break;
                                                            }
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failed getting Equivalent Home PLMNs: ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        break;
                                                    case 41:
                                                        isRecordLoadResponse = true;
                                                        ar = message.obj;
                                                        data = ar.result;
                                                        if (ar.exception == null) {
                                                            if (data != null) {
                                                                this.mFplmns = parseBcdPlmnList(data, "Forbidden");
                                                                if (message.arg1 == 1238273) {
                                                                    isRecordLoadResponse = false;
                                                                    Message response = retrievePendingResponseMessage(Integer.valueOf(message.arg2));
                                                                    if (response == null) {
                                                                        loge("Failed to retrieve a response message for FPLMN");
                                                                        break;
                                                                    }
                                                                    AsyncResult.forMessage(response, Arrays.copyOf(this.mFplmns, this.mFplmns.length), null);
                                                                    response.sendToTarget();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failed getting Forbidden PLMNs: ");
                                                        stringBuilder2.append(ar.exception);
                                                        loge(stringBuilder2.toString());
                                                        break;
                                                    default:
                                                        switch (i2) {
                                                            case 258:
                                                            case 259:
                                                                onLocked(message.what);
                                                                break;
                                                            default:
                                                                super.handleMessage(msg);
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
            } else {
                isRecordLoadResponse = false;
                ar = (AsyncResult) message.obj;
                if (ar.exception == null) {
                    this.mMsisdn = this.mNewMsisdn;
                    this.mMsisdnTag = this.mNewMsisdnTag;
                    log("Success to update EF[MSISDN]");
                }
                if (ar.userObj != null) {
                    AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
            }
        } catch (RuntimeException exc) {
            try {
                logw("Exception parsing SIM record", exc);
            } catch (Throwable th3) {
                if (isRecordLoadResponse) {
                    onRecordLoaded();
                }
            }
        }
    }

    protected void handleFileUpdate(int efid) {
        if (efid != IccConstants.EF_CFF_CPHS) {
            if (efid == IccConstants.EF_CSP_CPHS) {
                this.mRecordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
                return;
            } else if (efid == IccConstants.EF_MAILBOX_CPHS) {
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                return;
            } else if (efid == IccConstants.EF_FDN) {
                log("SIM Refresh called for EF_FDN");
                this.mParentApp.queryFdn();
                this.mAdnCache.reset();
                return;
            } else if (efid == IccConstants.EF_MSISDN) {
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
                return;
            } else if (efid == IccConstants.EF_MBDN) {
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                return;
            } else if (efid != IccConstants.EF_CFIS) {
                this.mAdnCache.reset();
                fetchSimRecords();
                return;
            }
        }
        log("SIM Refresh called for EF_CFIS or EF_CFF_CPHS");
        loadCallForwardingRecords();
    }

    private int dispatchGsmMessage(SmsMessage message) {
        this.mNewSmsRegistrants.notifyResult(message);
        return 0;
    }

    private void handleSms(byte[] ba) {
        if (ba[0] != (byte) 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("status : ");
            stringBuilder.append(ba[0]);
            Rlog.d("ENF", stringBuilder.toString());
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("status ");
                stringBuilder.append(i);
                stringBuilder.append(": ");
                stringBuilder.append(ba[0]);
                Rlog.i("ENF", stringBuilder.toString());
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
        } else if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void setVoiceCallForwardingFlagFromSimRecords() {
        int i = 1;
        StringBuilder stringBuilder;
        if (validEfCfis(this.mEfCfis)) {
            this.mCallForwardingStatus = this.mEfCfis[1] & 1;
            stringBuilder = new StringBuilder();
            stringBuilder.append("EF_CFIS: callForwardingEnabled=");
            stringBuilder.append(this.mCallForwardingStatus);
            log(stringBuilder.toString());
        } else if (this.mEfCff != null) {
            if ((this.mEfCff[0] & 15) != 10) {
                i = 0;
            }
            this.mCallForwardingStatus = i;
            stringBuilder = new StringBuilder();
            stringBuilder.append("EF_CFF: callForwardingEnabled=");
            stringBuilder.append(this.mCallForwardingStatus);
            log(stringBuilder.toString());
        } else {
            this.mCallForwardingStatus = -1;
            stringBuilder = new StringBuilder();
            stringBuilder.append("EF_CFIS and EF_CFF not valid. callForwardingEnabled=");
            stringBuilder.append(this.mCallForwardingStatus);
            log(stringBuilder.toString());
        }
    }

    private void setSimLanguageFromEF() {
        if (Resources.getSystem().getBoolean(17957064)) {
            setSimLanguage(this.mEfLi, this.mEfPl);
        } else {
            log("Not using EF LI/EF PL");
        }
    }

    private void onLockedAllRecordsLoaded() {
        setSimLanguageFromEF();
        if (this.mLockedRecordsReqReason == 1) {
            this.mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else if (this.mLockedRecordsReqReason == 2) {
            this.mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason ");
            stringBuilder.append(this.mLockedRecordsReqReason);
            loge(stringBuilder.toString());
        }
    }

    protected void onAllRecordsLoaded() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("record load complete");
        stringBuilder.append(getSlotId());
        log(stringBuilder.toString());
        setSimLanguageFromEF();
        setVoiceCallForwardingFlagFromSimRecords();
        String operator = getOperatorNumeric();
        if (TextUtils.isEmpty(operator)) {
            log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='");
            stringBuilder2.append(operator);
            stringBuilder2.append("'");
            log(stringBuilder2.toString());
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
        HwGetCfgFileConfig.readCfgFileConfig("xml/telephony-various.xml", getSlotId());
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
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

    private void onLocked(int msg) {
        log("only fetch EF_LI, EF_PL and EF_ICCID in locked state");
        this.mLockedRecordsReqReason = msg == 258 ? 1 : 2;
        loadEfLiAndEfPl();
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
        this.mRecordsToLoad++;
    }

    public void onGetImsiDone(Message msg) {
        if (msg != null) {
            AsyncResult ar = msg.obj;
            StringBuilder stringBuilder;
            if (ar.exception != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception querying IMSI, Exception:");
                stringBuilder.append(ar.exception);
                loge(stringBuilder.toString());
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
            stringBuilder = new StringBuilder();
            stringBuilder.append("IMSI: mMncLength=");
            stringBuilder.append(this.mMncLength);
            stringBuilder.append(", mImsiLoad: ");
            stringBuilder.append(this.mImsiLoad);
            log(stringBuilder.toString());
            if (this.mImsi != null && this.mImsi.length() >= 6) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("IMSI: ");
                stringBuilder.append(this.mImsi.substring(0, 6));
                stringBuilder.append(Rlog.pii(LOG_TAG, this.mImsi.substring(6)));
                log(stringBuilder.toString());
            }
            String imsi = getIMSI();
            onImsiLoadedHw();
            updateSarMnc(imsi);
            if ((this.mMncLength == 0 || this.mMncLength == 2) && imsi != null && imsi.length() >= 6) {
                mccmncCode = imsi.substring(0, 6);
                for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                    if (mccmnc.equals(mccmncCode)) {
                        this.mMncLength = 3;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("IMSI: setting1 mMncLength=");
                        stringBuilder2.append(this.mMncLength);
                        log(stringBuilder2.toString());
                        break;
                    }
                }
            }
            if ((this.mMncLength == 0 || this.mMncLength == 3) && imsi != null && imsi.length() >= 5) {
                mccmncCode = imsi.substring(0, 3);
                if (mccmncCode.equals("404") || mccmncCode.equals("405")) {
                    String mccmncCode2 = imsi.substring(0, 5);
                    for (String mccmnc2 : MCCMNC_CODES_HAVING_2DIGITS_MNC) {
                        if (mccmnc2.equals(mccmncCode2)) {
                            this.mMncLength = 2;
                            break;
                        }
                    }
                }
            }
            if (this.mMncLength == 0 && imsi != null) {
                try {
                    mccmncCode = imsi.substring(0, 3);
                    if (!mccmncCode.equals("404")) {
                        if (!mccmncCode.equals("405")) {
                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(mccmncCode));
                        }
                    }
                    this.mMncLength = 3;
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fetchSimRecords ");
        stringBuilder.append(this.mRecordsToLoad);
        log(stringBuilder.toString());
        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
        this.mRecordsToLoad++;
        if (!getIccidSwitch()) {
            if (IS_MODEM_CAPABILITY_GET_ICCID_AT) {
                this.mCi.getICCID(obtainMessage(4));
            } else {
                this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
            }
            this.mRecordsToLoad++;
        }
        if (HW_IS_SUPPORT_FAST_FETCH_SIMINFO) {
            log("fetchSimRecords: support fast fetch SIM records.");
            loadSimMatchedFileFromRilCache();
        } else {
            CommandsInterface commandsInterface = this.mCi;
            IccFileHandler iccFileHandler = this.mFh;
            commandsInterface.iccIOForApp(176, IccConstants.EF_AD, this.mFh.getEFPath(IccConstants.EF_AD), 0, 0, 4, null, null, this.mParentApp.getAid(), obtainMessage(9));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_SST, obtainMessage(17));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_GID1, obtainMessage(34));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_GID2, obtainMessage(36));
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
        this.mFh.loadEFTransparent(IccConstants.EF_INFO_CPHS, obtainMessage(26));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
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
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fetchSimRecords ");
        stringBuilder2.append(this.mRecordsToLoad);
        stringBuilder2.append(" requested: ");
        stringBuilder2.append(this.mRecordsRequested);
        log(stringBuilder2.toString());
    }

    public int getDisplayRule(ServiceState serviceState) {
        if (this.mParentApp != null && this.mParentApp.getUiccProfile() != null && this.mParentApp.getUiccProfile().getOperatorBrandOverride() != null) {
            return 2;
        }
        if (TextUtils.isEmpty(getServiceProviderName()) || this.mSpnDisplayCondition == -1) {
            return 2;
        }
        if (useRoamingFromServiceState() ? serviceState.getRoaming() : !isOnMatchingPlmn(serviceState.getOperatorNumeric())) {
            if ((this.mSpnDisplayCondition & 2) == 0) {
                return 2 | 1;
            }
            return 2;
        } else if ((this.mSpnDisplayCondition & 1) == 1) {
            return 1 | 2;
        } else {
            return 1;
        }
    }

    private boolean useRoamingFromServiceState() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mParentApp.getPhoneId()));
            if (b != null && b.getBoolean("spn_display_rule_use_roaming_from_service_state_bool")) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnMatchingPlmn(String plmn) {
        if (plmn == null) {
            return false;
        }
        if (plmn.equals(getOperatorNumeric())) {
            return true;
        }
        if (this.mSpdiNetworks != null) {
            Iterator it = this.mSpdiNetworks.iterator();
            while (it.hasNext()) {
                if (plmn.equals((String) it.next())) {
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
        StringBuilder stringBuilder;
        switch (this.mSpnState) {
            case INIT:
                setServiceProviderName(null);
                if (HW_IS_SUPPORT_FAST_FETCH_SIMINFO) {
                    this.mCi.getSimMatchedFileFromRilCache(IccConstants.EF_SPN, obtainMessage(42));
                } else {
                    this.mFh.loadEFTransparent(IccConstants.EF_SPN, obtainMessage(12));
                }
                this.mRecordsToLoad++;
                this.mSpnState = GetSpnFsmState.READ_SPN_3GPP;
                break;
            case READ_SPN_3GPP:
                if (ar == null || ar.exception != null) {
                    this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                } else {
                    data = (byte[]) ar.result;
                    this.mSpnDisplayCondition = 255 & data[0];
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 1, data.length - 1));
                    spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Load EF_SPN: ");
                        stringBuilder.append(spn);
                        stringBuilder.append(" spnDisplayCondition: ");
                        stringBuilder.append(this.mSpnDisplayCondition);
                        log(stringBuilder.toString());
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_CPHS) {
                    if (HW_IS_SUPPORT_FAST_FETCH_SIMINFO) {
                        this.mCi.getSimMatchedFileFromRilCache(IccConstants.EF_SPN_CPHS, obtainMessage(42));
                    } else {
                        this.mFh.loadEFTransparent(IccConstants.EF_SPN_CPHS, obtainMessage(12));
                    }
                    this.mRecordsToLoad++;
                    this.mSpnDisplayCondition = -1;
                    break;
                }
                break;
            case READ_SPN_CPHS:
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
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Load EF_SPN_CPHS: ");
                        stringBuilder.append(spn);
                        log(stringBuilder.toString());
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS) {
                    if (HW_IS_SUPPORT_FAST_FETCH_SIMINFO) {
                        this.mCi.getSimMatchedFileFromRilCache(IccConstants.EF_SPN_SHORT_CPHS, obtainMessage(42));
                    } else {
                        this.mFh.loadEFTransparent(IccConstants.EF_SPN_SHORT_CPHS, obtainMessage(12));
                    }
                    this.mRecordsToLoad++;
                    break;
                }
                break;
            case READ_SPN_SHORT_CPHS:
                if (ar == null || ar.exception != null) {
                    setServiceProviderName(null);
                    log("No SPN loaded in either CHPS or 3GPP");
                } else {
                    data = ar.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 0, data.length));
                    spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        log("No SPN loaded in either CHPS or 3GPP");
                    } else {
                        this.mSpnDisplayCondition = 2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Load EF_SPN_SHORT_CPHS: ");
                        stringBuilder.append(spn);
                        log(stringBuilder.toString());
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
        int i = 0;
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
            while (i + 2 < plmnEntries.length) {
                String plmnCode = IccUtils.bcdPlmnToString(plmnEntries, i);
                if (plmnCode != null && plmnCode.length() >= 5) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EF_SPDI network: ");
                    stringBuilder.append(plmnCode);
                    log(stringBuilder.toString());
                    this.mSpdiNetworks.add(plmnCode);
                }
                i += 3;
            }
        }
    }

    private String[] parseBcdPlmnList(byte[] data, String description) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Received ");
        stringBuilder.append(description);
        stringBuilder.append(" PLMNs, raw=");
        stringBuilder.append(IccUtils.bytesToHexString(data));
        log(stringBuilder.toString());
        if (data.length == 0 || data.length % 3 != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Received invalid ");
            stringBuilder.append(description);
            stringBuilder.append(" PLMN list");
            loge(stringBuilder.toString());
            return null;
        }
        int numPlmns = data.length / 3;
        int numValidPlmns = 0;
        String[] parsed = new String[numPlmns];
        for (int i = 0; i < numPlmns; i++) {
            parsed[numValidPlmns] = IccUtils.bcdPlmnToString(data, i * 3);
            if (!TextUtils.isEmpty(parsed[numValidPlmns])) {
                numValidPlmns++;
            }
        }
        return (String[]) Arrays.copyOf(parsed, numValidPlmns);
    }

    private boolean isCphsMailboxEnabled() {
        boolean z = false;
        if (this.mCphsInfo == null) {
            return false;
        }
        if ((this.mCphsInfo[1] & 48) == 48) {
            z = true;
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
            if (data[2 * i] == (byte) -64) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[CSP] found ValueAddedServicesGroup, value ");
                stringBuilder.append(data[(2 * i) + 1]);
                log(stringBuilder.toString());
                if ((data[(2 * i) + 1] & 128) == 128) {
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SIMRecords: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        pw.println(" extends:");
        super.dump(fd, pw, args);
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mVmConfig=");
        stringBuilder.append(this.mVmConfig);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCallForwardingStatus=");
        stringBuilder.append(this.mCallForwardingStatus);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSpnState=");
        stringBuilder.append(this.mSpnState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCphsInfo=");
        stringBuilder.append(this.mCphsInfo);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCspPlmnEnabled=");
        stringBuilder.append(this.mCspPlmnEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEfMWIS[]=");
        stringBuilder.append(Arrays.toString(this.mEfMWIS));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEfCPHS_MWI[]=");
        stringBuilder.append(Arrays.toString(this.mEfCPHS_MWI));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEfCff[]=");
        stringBuilder.append(Arrays.toString(this.mEfCff));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEfCfis[]=");
        stringBuilder.append(Arrays.toString(this.mEfCfis));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSpnDisplayCondition=");
        stringBuilder.append(this.mSpnDisplayCondition);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSpdiNetworks[]=");
        stringBuilder.append(this.mSpdiNetworks);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mUsimServiceTable=");
        stringBuilder.append(this.mUsimServiceTable);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mGid1=");
        stringBuilder.append(this.mGid1);
        pw.println(stringBuilder.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFakeGid1=");
            stringBuilder.append(this.mCarrierTestOverride.getFakeGid1());
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mGid2=");
        stringBuilder.append(this.mGid2);
        pw.println(stringBuilder.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFakeGid2=");
            stringBuilder.append(this.mCarrierTestOverride.getFakeGid2());
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPnnHomeName=");
        stringBuilder.append(this.mPnnHomeName);
        pw.println(stringBuilder.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFakePnnHomeName=");
            stringBuilder.append(this.mCarrierTestOverride.getFakePnnHomeName());
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPlmnActRecords[]=");
        stringBuilder.append(Arrays.toString(this.mPlmnActRecords));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mOplmnActRecords[]=");
        stringBuilder.append(Arrays.toString(this.mOplmnActRecords));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHplmnActRecords[]=");
        stringBuilder.append(Arrays.toString(this.mHplmnActRecords));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mFplmns[]=");
        stringBuilder.append(Arrays.toString(this.mFplmns));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEhplmns[]=");
        stringBuilder.append(Arrays.toString(this.mEhplmns));
        pw.println(stringBuilder.toString());
        pw.flush();
    }

    private String getVmSimImsi() {
        String imsi;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sim_imsi_key");
        stringBuilder.append(getSlotId());
        if (!sp.contains(stringBuilder.toString())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("vm_sim_imsi_key");
            stringBuilder.append(getSlotId());
            if (sp.contains(stringBuilder.toString())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("vm_sim_imsi_key");
                stringBuilder.append(getSlotId());
                imsi = sp.getString(stringBuilder.toString(), null);
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
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("vm_sim_imsi_key");
                        stringBuilder2.append(getSlotId());
                        editor.remove(stringBuilder2.toString());
                        editor.commit();
                    }
                }
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("sim_imsi_key");
        stringBuilder.append(getSlotId());
        imsi = sp.getString(stringBuilder.toString(), null);
        if (imsi == null) {
            return imsi;
        }
        try {
            return new String(Base64.decode(imsi, 0), "utf-8");
        } catch (IllegalArgumentException e2) {
            Rlog.e(LOG_TAG, "getVmSimImsi IllegalArgumentException");
            return imsi;
        } catch (UnsupportedEncodingException e3) {
            Rlog.e(LOG_TAG, "getVmSimImsi UnsupportedEncodingException");
            return imsi;
        }
    }

    private void setVmSimImsi(String imsi) {
        try {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sim_imsi_key");
            stringBuilder.append(getSlotId());
            editor.putString(stringBuilder.toString(), new String(Base64.encode(imsi.getBytes("utf-8"), 0), "utf-8"));
            editor.apply();
        } catch (UnsupportedEncodingException e) {
            Rlog.d(LOG_TAG, "setVmSimImsi UnsupportedEncodingException");
        }
    }

    private boolean getCallForwardingPreference() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cf_enabled_key");
        stringBuilder.append(getSlotId());
        boolean cf = sp.getBoolean(stringBuilder.toString(), false);
        String str = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Get callforwarding info from perferences getSlotId()=");
        stringBuilder2.append(getSlotId());
        stringBuilder2.append(",cf=");
        stringBuilder2.append(cf);
        Rlog.d(str, stringBuilder2.toString());
        return cf;
    }

    private void setCallForwardingPreference(boolean enabled) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Set callforwarding info to perferences getSlotId()=");
        stringBuilder.append(getSlotId());
        stringBuilder.append(",cf=");
        stringBuilder.append(enabled);
        Rlog.d(str, stringBuilder.toString());
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("cf_enabled_key");
        stringBuilder2.append(getSlotId());
        edit.putBoolean(stringBuilder2.toString(), enabled);
        edit.commit();
        if (this.mImsi != null) {
            setVmSimImsi(this.mImsi);
        }
    }

    private void updateMccMncConfigWithGplmn(String operatorNumeric) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateMccMncConfigWithGplmn: ");
        stringBuilder.append(operatorNumeric);
        log(stringBuilder.toString());
        if (HwTelephonyFactory.getHwUiccManager().isCDMASimCard(this.mParentApp.getPhoneId())) {
            log("cdma card, ignore updateMccMncConfiguration");
        } else if (operatorNumeric != null && operatorNumeric.length() >= 5) {
            MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, false);
        }
    }

    private void adapterForDoubleRilChannelAfterImsiReady() {
        if (this.mImsi != null && this.mMncLength != 0 && this.mMncLength != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_GET_IMSI_DONE, update mccmnc=");
            stringBuilder.append(this.mImsi.substring(0, this.mMncLength + 3));
            log(stringBuilder.toString());
            updateMccMncConfigWithGplmn(this.mImsi.substring(0, 3 + this.mMncLength));
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcast, CSP Plmn Enabled change to ");
            stringBuilder.append(this.mCspPlmnEnabled);
            log(stringBuilder.toString());
        }
    }

    protected void initEventIdMap() {
        this.sEventIdMap.put("EVENT_GET_MBDN_DONE", Integer.valueOf(6));
        this.sEventIdMap.put("EVENT_GET_ICCID_DONE", Integer.valueOf(4));
        this.sEventIdMap.put("EVENT_GET_AD_DONE", Integer.valueOf(9));
        this.sEventIdMap.put("EVENT_GET_SST_DONE", Integer.valueOf(17));
        this.sEventIdMap.put("EVENT_GET_SPN_DONE", Integer.valueOf(12));
        this.sEventIdMap.put("EVENT_GET_GID1_DONE", Integer.valueOf(34));
        this.sEventIdMap.put("EVENT_GET_GID2_DONE", Integer.valueOf(36));
    }

    protected int getEventIdFromMap(String event) {
        if (this.sEventIdMap.containsKey(event)) {
            return ((Integer) this.sEventIdMap.get(event)).intValue();
        }
        log("Event Id not in the map.");
        return -1;
    }
}
