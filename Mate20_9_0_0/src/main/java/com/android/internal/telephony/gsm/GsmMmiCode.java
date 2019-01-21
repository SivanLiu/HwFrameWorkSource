package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwPhoneManager;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.MmiCode.State;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.SsData.RequestType;
import com.android.internal.telephony.gsm.SsData.ServiceType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.util.ArrayUtils;
import huawei.cust.HwCfgFilePolicy;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GsmMmiCode extends Handler implements MmiCode {
    static final String ACTION_ACTIVATE = "*";
    static final String ACTION_DEACTIVATE = "#";
    static final String ACTION_ERASURE = "##";
    static final String ACTION_INTERROGATE = "*#";
    static final String ACTION_REGISTER = "**";
    static final char END_OF_USSD_COMMAND = '#';
    static final int EVENT_GET_CLIR_COMPLETE = 2;
    static final int EVENT_QUERY_CF_COMPLETE = 3;
    static final int EVENT_QUERY_COMPLETE = 5;
    static final int EVENT_SET_CFF_COMPLETE = 6;
    static final int EVENT_SET_COMPLETE = 1;
    static final int EVENT_USSD_CANCEL_COMPLETE = 7;
    static final int EVENT_USSD_COMPLETE = 4;
    static final String LOG_TAG_STATIC = "GsmMmiCode";
    static final int MATCH_GROUP_ACTION = 2;
    static final int MATCH_GROUP_DIALING_NUMBER = 16;
    static final int MATCH_GROUP_POUND_STRING = 1;
    static final int MATCH_GROUP_PWD_CONFIRM = 15;
    static final int MATCH_GROUP_SERVICE_CODE = 3;
    static final int MATCH_GROUP_SIA = 6;
    static final int MATCH_GROUP_SIB = 9;
    static final int MATCH_GROUP_SIC = 12;
    static final int MAX_LENGTH_SHORT_CODE = 2;
    static final int NOT_HAS_VALUES = -1;
    static final String SC_107 = "107";
    static final String SC_108 = "108";
    static final String SC_BAIC = "35";
    static final String SC_BAICr = "351";
    static final String SC_BAOC = "33";
    static final String SC_BAOIC = "331";
    static final String SC_BAOICxH = "332";
    static final String SC_BA_ALL = "330";
    static final String SC_BA_MO = "333";
    static final String SC_BA_MT = "353";
    static final String SC_CFB = "67";
    static final String SC_CFNR = "62";
    static final String SC_CFNRy = "61";
    static final String SC_CFU = "21";
    static final String SC_CF_All = "002";
    static final String SC_CF_All_Conditional = "004";
    static final String SC_CLIP = "30";
    static final String SC_CLIR = "31";
    static final String SC_PIN = "04";
    static final String SC_PIN2 = "042";
    static final String SC_PUK = "05";
    static final String SC_PUK2 = "052";
    static final String SC_PWD = "03";
    static final String SC_WAIT = "43";
    public static final boolean USSD_REMOVE_ERROR_MSG = SystemProperties.getBoolean("ro.config.hw_remove_mmi", false);
    static final int VALUES_FALSE = 0;
    static final int VALUES_TRUE = 1;
    private static final boolean isDocomo = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    private static final boolean mToastSwitch = SystemProperties.getBoolean("ro.config.hw_ss_toast", false);
    static Pattern sPatternSuppService;
    private static String[] sTwoDigitNumberPattern;
    String LOG_TAG = LOG_TAG_STATIC;
    String mAction;
    private ResultReceiver mCallbackReceiver;
    Context mContext;
    public String mDialingNumber;
    IccRecords mIccRecords;
    Phone mImsPhone = null;
    private boolean mIncomingUSSD = false;
    private boolean mIsCallFwdReg;
    private boolean mIsPendingUSSD;
    private boolean mIsSsInfo = false;
    private boolean mIsUssdRequest;
    CharSequence mMessage;
    GsmCdmaPhone mPhone;
    String mPoundString;
    String mPwd;
    String mSc;
    String mSia;
    String mSib;
    String mSic;
    State mState = State.PENDING;
    UiccCardApplication mUiccApplication;

    static {
        sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
        sPatternSuppService = HwPhoneManager.sPatternSuppService;
    }

    public static GsmMmiCode newFromDialString(String dialString, GsmCdmaPhone phone, UiccCardApplication app) {
        return newFromDialString(dialString, phone, app, null);
    }

    public static GsmMmiCode newFromDialString(String dialString, GsmCdmaPhone phone, UiccCardApplication app, ResultReceiver wrappedCallback) {
        GsmMmiCode ret = null;
        if (dialString == null) {
            Rlog.d(LOG_TAG_STATIC, "newFromDialString: dialString cannot be null");
            return null;
        }
        if (phone.getServiceState().getVoiceRoaming() && phone.supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming()) {
            dialString = convertCdmaMmiCodesTo3gppMmiCodes(dialString);
        }
        if (HwTelephonyFactory.getHwPhoneManager().isStringHuaweiIgnoreCode(phone, dialString)) {
            String tag = LOG_TAG_STATIC;
            if (phone != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(tag);
                stringBuilder.append("[SUB");
                stringBuilder.append(phone.getPhoneId());
                stringBuilder.append("]");
                tag = stringBuilder.toString();
            }
            Rlog.d(tag, "newFromDialString, a huawei ignore code found, return null.");
            return null;
        }
        Matcher m = sPatternSuppService.matcher(dialString);
        if (m.matches()) {
            ret = new GsmMmiCode(phone, app);
            ret.mPoundString = makeEmptyNull(m.group(1));
            ret.mAction = makeEmptyNull(m.group(2));
            ret.mSc = makeEmptyNull(m.group(3));
            ret.mSia = makeEmptyNull(m.group(6));
            ret.mSib = makeEmptyNull(m.group(9));
            ret.mSic = makeEmptyNull(m.group(12));
            ret.mPwd = makeEmptyNull(m.group(15));
            ret.mDialingNumber = makeEmptyNull(m.group(16));
            if (ret.mDialingNumber != null && ret.mDialingNumber.endsWith(ACTION_DEACTIVATE) && dialString.endsWith(ACTION_DEACTIVATE)) {
                ret = new GsmMmiCode(phone, app);
                ret.mPoundString = dialString;
            } else if (ret.isFacToDial()) {
                ret = null;
            }
        } else if (dialString.endsWith(ACTION_DEACTIVATE)) {
            ret = new GsmMmiCode(phone, app);
            ret.mPoundString = dialString;
        } else if (isTwoDigitShortCode(phone.getContext(), dialString)) {
            ret = null;
        } else if (isShortCode(dialString, phone)) {
            ret = new GsmMmiCode(phone, app);
            ret.mDialingNumber = dialString;
        }
        if (ret != null) {
            ret.mCallbackReceiver = wrappedCallback;
        }
        return ret;
    }

    private static String convertCdmaMmiCodesTo3gppMmiCodes(String dialString) {
        Matcher m = sPatternCdmaMmiCodeWhileRoaming.matcher(dialString);
        if (!m.matches()) {
            return dialString;
        }
        String serviceCode = makeEmptyNull(m.group(1));
        String prefix = m.group(2);
        String number = makeEmptyNull(m.group(3));
        StringBuilder stringBuilder;
        if (serviceCode.equals(SC_CFB) && number != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("#31#");
            stringBuilder.append(prefix);
            stringBuilder.append(number);
            return stringBuilder.toString();
        } else if (!serviceCode.equals("82") || number == null) {
            return dialString;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("*31#");
            stringBuilder.append(prefix);
            stringBuilder.append(number);
            return stringBuilder.toString();
        }
    }

    public String getmSC() {
        return this.mSc;
    }

    public static GsmMmiCode newNetworkInitiatedUssd(String ussdMessage, boolean isUssdRequest, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);
        ret.mMessage = ussdMessage;
        ret.mIsUssdRequest = isUssdRequest;
        if (isUssdRequest) {
            ret.mIsPendingUSSD = true;
            ret.mState = State.PENDING;
        } else {
            ret.mState = State.COMPLETE;
        }
        return ret;
    }

    public static GsmMmiCode newFromUssdUserInput(String ussdMessge, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);
        ret.mMessage = ussdMessge;
        ret.mState = State.PENDING;
        ret.mIsPendingUSSD = true;
        return ret;
    }

    public void processSsData(AsyncResult data) {
        String str;
        StringBuilder stringBuilder;
        Rlog.d(this.LOG_TAG, "In processSsData");
        this.mIsSsInfo = true;
        try {
            parseSsData(data.result);
        } catch (ClassCastException ex) {
            str = this.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Class Cast Exception in parsing SS Data : ");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
        } catch (NullPointerException ex2) {
            str = this.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Null Pointer Exception in parsing SS Data : ");
            stringBuilder.append(ex2);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    void parseSsData(SsData ssData) {
        CommandException ex = CommandException.fromRilErrno(ssData.result);
        this.mSc = getScStringFromScType(ssData.serviceType);
        this.mAction = getActionStringFromReqType(ssData.requestType);
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parseSsData msc = ");
        stringBuilder.append(this.mSc);
        stringBuilder.append(", action = ");
        stringBuilder.append(this.mAction);
        stringBuilder.append(", ex = ");
        stringBuilder.append(ex);
        Rlog.d(str, stringBuilder.toString());
        switch (ssData.requestType) {
            case SS_ACTIVATION:
            case SS_DEACTIVATION:
            case SS_REGISTRATION:
            case SS_ERASURE:
                if (ssData.result == 0 && ssData.serviceType.isTypeUnConditional()) {
                    boolean cffEnabled = (ssData.requestType == RequestType.SS_ACTIVATION || ssData.requestType == RequestType.SS_REGISTRATION) && isServiceClassVoiceorNone(ssData.serviceClass);
                    String str2 = this.LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setVoiceCallForwardingFlag cffEnabled: ");
                    stringBuilder2.append(cffEnabled);
                    Rlog.d(str2, stringBuilder2.toString());
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, cffEnabled, null);
                        Rlog.d(this.LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                    } else {
                        Rlog.e(this.LOG_TAG, "setVoiceCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult(null, ssData.cfInfo, ex));
                return;
            case SS_INTERROGATION:
                if (ssData.serviceType.isTypeClir()) {
                    Rlog.d(this.LOG_TAG, "CLIR INTERROGATION");
                    onGetClirComplete(new AsyncResult(null, ssData.ssInfo, ex));
                    return;
                } else if (ssData.serviceType.isTypeCF()) {
                    Rlog.d(this.LOG_TAG, "CALL FORWARD INTERROGATION");
                    onQueryCfComplete(new AsyncResult(null, ssData.cfInfo, ex));
                    return;
                } else {
                    onQueryComplete(new AsyncResult(null, ssData.ssInfo, ex));
                    return;
                }
            default:
                str = this.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invaid requestType in SSData : ");
                stringBuilder.append(ssData.requestType);
                Rlog.e(str, stringBuilder.toString());
                return;
        }
    }

    private String getScStringFromScType(ServiceType sType) {
        switch (sType) {
            case SS_CFU:
                return SC_CFU;
            case SS_CF_BUSY:
                return SC_CFB;
            case SS_CF_NO_REPLY:
                return SC_CFNRy;
            case SS_CF_NOT_REACHABLE:
                return SC_CFNR;
            case SS_CF_ALL:
                return SC_CF_All;
            case SS_CF_ALL_CONDITIONAL:
                return SC_CF_All_Conditional;
            case SS_CLIP:
                return SC_CLIP;
            case SS_CLIR:
                return SC_CLIR;
            case SS_WAIT:
                return SC_WAIT;
            case SS_BAOC:
                return SC_BAOC;
            case SS_BAOIC:
                return SC_BAOIC;
            case SS_BAOIC_EXC_HOME:
                return SC_BAOICxH;
            case SS_BAIC:
                return SC_BAIC;
            case SS_BAIC_ROAMING:
                return SC_BAICr;
            case SS_ALL_BARRING:
                return SC_BA_ALL;
            case SS_OUTGOING_BARRING:
                return SC_BA_MO;
            case SS_INCOMING_BARRING:
                return SC_BA_MT;
            default:
                return "";
        }
    }

    private String getActionStringFromReqType(RequestType rType) {
        switch (rType) {
            case SS_ACTIVATION:
                return "*";
            case SS_DEACTIVATION:
                return ACTION_DEACTIVATE;
            case SS_REGISTRATION:
                return ACTION_REGISTER;
            case SS_ERASURE:
                return ACTION_ERASURE;
            case SS_INTERROGATION:
                return ACTION_INTERROGATE;
            default:
                return "";
        }
    }

    private boolean isServiceClassVoiceorNone(int serviceClass) {
        return (serviceClass & 1) != 0 || serviceClass == 0;
    }

    private static String makeEmptyNull(String s) {
        if (s == null || s.length() != 0) {
            return s;
        }
        return null;
    }

    private static boolean isEmptyOrNull(CharSequence s) {
        return s == null || s.length() == 0;
    }

    private static int scToCallForwardReason(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call forward sc");
        } else if (sc.equals(SC_CF_All)) {
            return 4;
        } else {
            if (sc.equals(SC_CFU)) {
                return 0;
            }
            if (sc.equals(SC_CFB)) {
                return 1;
            }
            if (sc.equals(SC_CFNR)) {
                return 3;
            }
            if (sc.equals(SC_CFNRy)) {
                return 2;
            }
            if (sc.equals(SC_CF_All_Conditional)) {
                return 5;
            }
            throw new RuntimeException("invalid call forward sc");
        }
    }

    private static int siToServiceClass(String si) {
        if (si == null || si.length() == 0) {
            return 0;
        }
        int serviceCode = Integer.parseInt(si, 10);
        if (serviceCode == 16) {
            return 8;
        }
        if (serviceCode == 99) {
            return 64;
        }
        switch (serviceCode) {
            case 10:
                return 13;
            case 11:
                return 1;
            case 12:
                return 12;
            case 13:
                return 4;
            default:
                switch (serviceCode) {
                    case 19:
                        return 5;
                    case 20:
                        return 48;
                    case 21:
                        return 160;
                    case 22:
                        return 80;
                    default:
                        switch (serviceCode) {
                            case 24:
                                return 16;
                            case 25:
                                return 32;
                            case 26:
                                return 17;
                            default:
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("unsupported MMI service code ");
                                stringBuilder.append(si);
                                throw new RuntimeException(stringBuilder.toString());
                        }
                }
        }
    }

    private static int siToTime(String si) {
        if (si == null || si.length() == 0) {
            return 0;
        }
        return Integer.parseInt(si, 10);
    }

    public boolean isServiceCodeCallForwarding() {
        if (TextUtils.isEmpty(this.mSc)) {
            return false;
        }
        return isServiceCodeCallForwarding(this.mSc);
    }

    public static boolean isServiceCodeCallForwarding(String sc) {
        return sc != null && (sc.equals(SC_CFU) || sc.equals(SC_CFB) || sc.equals(SC_CFNRy) || sc.equals(SC_CFNR) || sc.equals(SC_CF_All) || sc.equals(SC_CF_All_Conditional));
    }

    static boolean isServiceCodeCallBarring(String sc) {
        Resources resource = Resources.getSystem();
        if (sc != null) {
            String[] barringMMI = resource.getStringArray(17235991);
            if (barringMMI != null) {
                for (String match : barringMMI) {
                    if (sc.equals(match)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static String scToBarringFacility(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call barring sc");
        } else if (sc.equals(SC_BAOC)) {
            return CommandsInterface.CB_FACILITY_BAOC;
        } else {
            if (sc.equals(SC_BAOIC)) {
                return CommandsInterface.CB_FACILITY_BAOIC;
            }
            if (sc.equals(SC_BAOICxH)) {
                return CommandsInterface.CB_FACILITY_BAOICxH;
            }
            if (sc.equals(SC_BAIC)) {
                return CommandsInterface.CB_FACILITY_BAIC;
            }
            if (sc.equals(SC_BAICr)) {
                return CommandsInterface.CB_FACILITY_BAICr;
            }
            if (sc.equals(SC_BA_ALL)) {
                return CommandsInterface.CB_FACILITY_BA_ALL;
            }
            if (sc.equals(SC_BA_MO)) {
                return CommandsInterface.CB_FACILITY_BA_MO;
            }
            if (sc.equals(SC_BA_MT)) {
                return CommandsInterface.CB_FACILITY_BA_MT;
            }
            throw new RuntimeException("invalid call barring sc");
        }
    }

    public GsmMmiCode(GsmCdmaPhone phone, UiccCardApplication app) {
        super(phone.getHandler().getLooper());
        this.mPhone = phone;
        this.mContext = phone.getContext();
        this.mUiccApplication = app;
        if (app != null) {
            this.mIccRecords = app.getIccRecords();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.LOG_TAG);
        stringBuilder.append("[SUB");
        stringBuilder.append(phone.getPhoneId());
        stringBuilder.append("]");
        this.LOG_TAG = stringBuilder.toString();
    }

    public State getState() {
        return this.mState;
    }

    public CharSequence getMessage() {
        return this.mMessage;
    }

    public Phone getPhone() {
        return this.mPhone;
    }

    public void cancel() {
        if (this.mState != State.COMPLETE && this.mState != State.FAILED) {
            this.mState = State.CANCELLED;
            if (this.mIsPendingUSSD) {
                this.mPhone.mCi.cancelPendingUssd(obtainMessage(7, this));
            } else {
                this.mPhone.onMMIDone(this);
            }
        }
    }

    public boolean isCancelable() {
        return this.mIsPendingUSSD;
    }

    boolean isMMI() {
        return this.mPoundString != null;
    }

    boolean isShortCode() {
        return this.mPoundString == null && this.mDialingNumber != null && this.mDialingNumber.length() <= 2;
    }

    public String getDialString() {
        return this.mPoundString;
    }

    private static boolean isTwoDigitShortCode(Context context, String dialString) {
        Rlog.d(LOG_TAG_STATIC, "isTwoDigitShortCode");
        if (dialString == null || dialString.length() > 2) {
            return false;
        }
        if (sTwoDigitNumberPattern == null) {
            sTwoDigitNumberPattern = context.getResources().getStringArray(17236049);
        }
        for (String dialnumber : sTwoDigitNumberPattern) {
            String str = LOG_TAG_STATIC;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Two Digit Number Pattern ");
            stringBuilder.append(dialnumber);
            Rlog.d(str, stringBuilder.toString());
            if (dialString.equals(dialnumber)) {
                Rlog.d(LOG_TAG_STATIC, "Two Digit Number Pattern -true");
                return true;
            }
        }
        Rlog.d(LOG_TAG_STATIC, "Two Digit Number Pattern -false");
        return false;
    }

    private static boolean isShortCode(String dialString, GsmCdmaPhone phone) {
        if (dialString == null || dialString.length() == 0) {
            return false;
        }
        if (dialString != null && 2 >= dialString.length()) {
            String hwMmiCodeStr = SystemProperties.get("ro.config.hw_mmi_code", "-1");
            try {
                String cfgMmiCode = (String) HwCfgFilePolicy.getValue("mmi_code", SubscriptionManager.getSlotIndex(phone.getSubId()), String.class);
                if (!isEmptyOrNull(cfgMmiCode)) {
                    hwMmiCodeStr = cfgMmiCode;
                    String str = LOG_TAG_STATIC;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HwCfgFile: hwMmiCodeStr=");
                    stringBuilder.append(hwMmiCodeStr);
                    Rlog.d(str, stringBuilder.toString());
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG_STATIC, "read mmi_code error! ");
            }
            if (hwMmiCodeStr != null) {
                String[] hwMmiCodes = hwMmiCodeStr.split(",");
                if (hwMmiCodes != null && Arrays.asList(hwMmiCodes).contains(dialString)) {
                    return false;
                }
            }
        }
        if (PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), dialString)) {
            return false;
        }
        return isShortCodeUSSD(dialString, phone);
    }

    private static boolean isShortCodeUSSD(String dialString, GsmCdmaPhone phone) {
        if (HwTelephonyFactory.getHwPhoneManager().isShortCodeCustomization() || dialString == null || dialString.length() > 2 || (!phone.isInCall() && dialString.length() == 2 && dialString.charAt(0) == '1')) {
            return false;
        }
        return true;
    }

    public boolean isPinPukCommand() {
        return this.mSc != null && (this.mSc.equals(SC_PIN) || this.mSc.equals(SC_PIN2) || this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2));
    }

    public boolean isTemporaryModeCLIR() {
        return this.mSc != null && this.mSc.equals(SC_CLIR) && this.mDialingNumber != null && (isActivate() || isDeactivate());
    }

    public int getCLIRMode() {
        if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
            if (isActivate()) {
                return 2;
            }
            if (isDeactivate()) {
                return 1;
            }
        }
        return 0;
    }

    private boolean isFacToDial() {
        PersistableBundle b = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
        if (b != null) {
            String[] dialFacList = b.getStringArray("feature_access_codes_string_array");
            if (!ArrayUtils.isEmpty(dialFacList)) {
                for (String fac : dialFacList) {
                    if (fac.equals(this.mSc)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    boolean isActivate() {
        return this.mAction != null && this.mAction.equals("*");
    }

    boolean isDeactivate() {
        return this.mAction != null && this.mAction.equals(ACTION_DEACTIVATE);
    }

    boolean isInterrogate() {
        return this.mAction != null && this.mAction.equals(ACTION_INTERROGATE);
    }

    boolean isRegister() {
        return this.mAction != null && this.mAction.equals(ACTION_REGISTER);
    }

    boolean isErasure() {
        return this.mAction != null && this.mAction.equals(ACTION_ERASURE);
    }

    public boolean isPendingUSSD() {
        return this.mIsPendingUSSD;
    }

    public boolean isUssdRequest() {
        return this.mIsUssdRequest;
    }

    public boolean isSsInfo() {
        return this.mIsSsInfo;
    }

    public static boolean isVoiceUnconditionalForwarding(int reason, int serviceClass) {
        return (reason == 0 || reason == 4) && ((serviceClass & 1) != 0 || serviceClass == 0);
    }

    public void processCode() throws CallStateException {
        String newPwd;
        try {
            if (HwTelephonyFactory.getHwPhoneManager().isStringHuaweiCustCode(this.mPoundString)) {
                Rlog.d(this.LOG_TAG, "Huawei custimized MMI codes, send out directly. ");
                sendUssd(this.mPoundString);
            } else if (HwTelephonyFactory.getHwPhoneManager().changeMMItoUSSD(this.mPhone, this.mPoundString)) {
                Rlog.d(this.LOG_TAG, "changeMMItoUSSD");
                sendUssd(this.mPoundString);
            } else if (HwTelephonyFactory.getHwPhoneManager().processImsPhoneMmiCode(this, this.mImsPhone)) {
                Rlog.d(this.LOG_TAG, "Process IMS Phone MMI codes.");
            } else {
                if (isShortCode()) {
                    Rlog.d(this.LOG_TAG, "processCode: isShortCode");
                    sendUssd(this.mDialingNumber);
                } else if (this.mDialingNumber != null) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                } else if (this.mSc == null || !this.mSc.equals(SC_CLIP)) {
                    int isEnableDesired = 1;
                    String dialingNumber;
                    int cfAction;
                    if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
                        Rlog.d(this.LOG_TAG, "processCode: is CLIR");
                        if (isActivate()) {
                            this.mPhone.mCi.setCLIR(1, obtainMessage(1, this));
                        } else if (isDeactivate()) {
                            this.mPhone.mCi.setCLIR(2, obtainMessage(1, this));
                        } else if (isInterrogate()) {
                            this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                        } else {
                            throw new RuntimeException("Invalid or Unsupported MMI Code");
                        }
                    } else if (isServiceCodeCallForwarding(this.mSc)) {
                        Rlog.d(this.LOG_TAG, "processCode: is CF");
                        dialingNumber = this.mSia;
                        int serviceClass = siToServiceClass(this.mSib);
                        int reason = scToCallForwardReason(this.mSc);
                        int time = siToTime(this.mSic);
                        if (isInterrogate()) {
                            this.mPhone.mCi.queryCallForwardStatus(reason, serviceClass, dialingNumber, obtainMessage(3, this));
                        } else {
                            if (isActivate()) {
                                if (isEmptyOrNull(dialingNumber)) {
                                    cfAction = 1;
                                    this.mIsCallFwdReg = false;
                                } else {
                                    cfAction = 3;
                                    this.mIsCallFwdReg = true;
                                }
                            } else if (isDeactivate()) {
                                cfAction = 0;
                            } else if (isRegister()) {
                                cfAction = 3;
                            } else if (isErasure()) {
                                cfAction = 4;
                            } else {
                                throw new RuntimeException("invalid action");
                            }
                            int cfAction2 = cfAction;
                            if (cfAction2 != 1) {
                                if (cfAction2 != 3) {
                                    isEnableDesired = 0;
                                }
                            }
                            Rlog.d(this.LOG_TAG, "processCode: is CF setCallForward");
                            this.mPhone.mCi.setCallForward(cfAction2, reason, serviceClass, dialingNumber, time, obtainMessage(6, isVoiceUnconditionalForwarding(reason, serviceClass), isEnableDesired, this));
                        }
                    } else if (isServiceCodeCallBarring(this.mSc)) {
                        dialingNumber = this.mSia;
                        int serviceClass2 = siToServiceClass(this.mSib);
                        String facility = scToBarringFacility(this.mSc);
                        if (isInterrogate()) {
                            this.mPhone.mCi.queryFacilityLock(facility, dialingNumber, serviceClass2, obtainMessage(5, this));
                        } else {
                            if (!isActivate()) {
                                if (!isDeactivate()) {
                                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                                }
                            }
                            this.mPhone.mCi.setFacilityLock(facility, isActivate(), dialingNumber, serviceClass2, obtainMessage(1, this));
                        }
                    } else if (this.mSc != null && this.mSc.equals(SC_PWD)) {
                        String facility2;
                        dialingNumber = this.mSib;
                        newPwd = this.mSic;
                        if (!isActivate()) {
                            if (!isRegister()) {
                                throw new RuntimeException("Invalid or Unsupported MMI Code");
                            }
                        }
                        this.mAction = ACTION_REGISTER;
                        if (this.mSia == null) {
                            facility2 = CommandsInterface.CB_FACILITY_BA_ALL;
                        } else {
                            facility2 = scToBarringFacility(this.mSia);
                        }
                        if (newPwd.equals(this.mPwd)) {
                            this.mPhone.mCi.changeBarringPassword(facility2, dialingNumber, newPwd, obtainMessage(1, this));
                        } else {
                            handlePasswordError(17040631);
                        }
                    } else if (this.mSc != null && this.mSc.equals(SC_WAIT)) {
                        int serviceClass3 = siToServiceClass(this.mSia);
                        if (!isActivate()) {
                            if (!isDeactivate()) {
                                if (isInterrogate()) {
                                    this.mPhone.mCi.queryCallWaiting(serviceClass3, obtainMessage(5, this));
                                } else {
                                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                                }
                            }
                        }
                        this.mPhone.mCi.setCallWaiting(isActivate(), serviceClass3, obtainMessage(1, this));
                    } else if (isPinPukCommand()) {
                        dialingNumber = this.mSia;
                        newPwd = this.mSib;
                        cfAction = newPwd.length();
                        StringBuilder stringBuilder;
                        if (!isRegister()) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Ivalid register/action=");
                            stringBuilder.append(this.mAction);
                            throw new RuntimeException(stringBuilder.toString());
                        } else if (newPwd.equals(this.mSic)) {
                            if (cfAction >= 4) {
                                if (cfAction <= 8) {
                                    if (this.mSc.equals(SC_PIN) && this.mUiccApplication != null && this.mUiccApplication.getState() == AppState.APPSTATE_PUK) {
                                        handlePasswordError(17040543);
                                    } else if (this.mUiccApplication != null) {
                                        String str = this.LOG_TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("processCode: process mmi service code using UiccApp sc=");
                                        stringBuilder2.append(this.mSc);
                                        Rlog.d(str, stringBuilder2.toString());
                                        if (this.mSc.equals(SC_PIN)) {
                                            this.mUiccApplication.changeIccLockPassword(dialingNumber, newPwd, obtainMessage(1, this));
                                        } else if (this.mSc.equals(SC_PIN2)) {
                                            this.mUiccApplication.changeIccFdnPassword(dialingNumber, newPwd, obtainMessage(1, this));
                                        } else if (this.mSc.equals(SC_PUK)) {
                                            this.mUiccApplication.supplyPuk(dialingNumber, newPwd, obtainMessage(1, this));
                                        } else if (this.mSc.equals(SC_PUK2)) {
                                            this.mUiccApplication.supplyPuk2(dialingNumber, newPwd, obtainMessage(1, this));
                                        } else {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("uicc unsupported service code=");
                                            stringBuilder.append(this.mSc);
                                            throw new RuntimeException(stringBuilder.toString());
                                        }
                                    } else {
                                        throw new RuntimeException("No application mUiccApplicaiton is null");
                                    }
                                }
                            }
                            handlePasswordError(17040227);
                        } else {
                            handlePasswordError(HwTelephonyFactory.getHwPhoneManager().handlePasswordError(this.mSc));
                        }
                    } else if (this.mPoundString != null) {
                        sendUssd(this.mPoundString);
                    } else {
                        Rlog.d(this.LOG_TAG, "processCode: Invalid or Unsupported MMI Code");
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                } else {
                    Rlog.d(this.LOG_TAG, "processCode: is CLIP");
                    if (isInterrogate()) {
                        this.mPhone.mCi.queryCLIP(obtainMessage(5, this));
                    } else {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                }
            }
        } catch (RuntimeException exc) {
            this.mState = State.FAILED;
            this.mMessage = this.mContext.getText(17040531);
            newPwd = this.LOG_TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("processCode: RuntimeException=");
            stringBuilder3.append(exc);
            Rlog.d(newPwd, stringBuilder3.toString());
            this.mPhone.onMMIDone(this);
        }
    }

    private void handlePasswordError(int res) {
        this.mState = State.FAILED;
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        sb.append(this.mContext.getText(res));
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    public void onUssdFinished(String ussdMessage, boolean isUssdRequest) {
        if (this.mState == State.PENDING) {
            if (TextUtils.isEmpty(ussdMessage)) {
                this.mMessage = this.mContext.getText(HwTelephonyFactory.getHwPhoneManager().showMmiError(17040530, SubscriptionManager.getSlotIndex(this.mPhone.getSubId())));
                Rlog.d(this.LOG_TAG, "onUssdFinished: no network provided message; using default.");
            } else {
                this.mMessage = ussdMessage;
            }
            this.mIsUssdRequest = isUssdRequest;
            if (!isUssdRequest) {
                this.mState = State.COMPLETE;
                int custUssdState = removeUssdCust(this.mPhone);
                if (custUssdState != 0 && ((USSD_REMOVE_ERROR_MSG || custUssdState == 1) && this.mIncomingUSSD && ussdMessage == null)) {
                    this.mMessage = "";
                }
            }
            Rlog.d(this.LOG_TAG, "onUssdFinished");
            this.mPhone.onMMIDone(this);
        }
    }

    private static int removeUssdCust(GsmCdmaPhone mPhone) {
        if (mPhone == null) {
            return -1;
        }
        boolean removeUssdState = false;
        boolean hasHwCfgConfig = false;
        try {
            Boolean removeUssd = (Boolean) HwCfgFilePolicy.getValue("remove_mmi", SubscriptionManager.getSlotIndex(mPhone.getSubId()), Boolean.class);
            if (removeUssd != null) {
                removeUssdState = removeUssd.booleanValue();
                hasHwCfgConfig = true;
            }
            if (hasHwCfgConfig && !removeUssdState) {
                return 0;
            }
            if (removeUssdState) {
                return 1;
            }
            return -1;
        } catch (Exception e) {
            Rlog.d(LOG_TAG_STATIC, "read remove_mmi error! ");
        }
    }

    public void onUssdFinishedError() {
        if (this.mState == State.PENDING) {
            this.mState = State.FAILED;
            int custremoveUssdState = removeUssdCust(this.mPhone);
            if (custremoveUssdState == 0 || !((USSD_REMOVE_ERROR_MSG || custremoveUssdState == 1) && this.mIncomingUSSD)) {
                this.mMessage = this.mContext.getText(17040531);
            } else {
                this.mMessage = "";
            }
            Rlog.d(this.LOG_TAG, "onUssdFinishedError");
            this.mPhone.onMMIDone(this);
        }
    }

    public void onUssdRelease() {
        if (this.mState == State.PENDING) {
            this.mState = State.COMPLETE;
            this.mMessage = null;
            Rlog.d(this.LOG_TAG, "onUssdRelease");
            this.mPhone.onMMIDone(this);
        }
    }

    public void sendUssd(String ussdMessage) {
        if (HwTelephonyFactory.getHwPhoneManager().processSendUssdInImsCall(this, this.mImsPhone)) {
            Rlog.i(this.LOG_TAG, "forbid sending ussd when is in ims call.");
            return;
        }
        Rlog.i(this.LOG_TAG, "executing sendUssd.");
        this.mIsPendingUSSD = true;
        this.mPhone.mCi.sendUSSD(HwTelephonyFactory.getHwPhoneManager().convertUssdMessage(ussdMessage), obtainMessage(4, this));
    }

    public void handleMessage(Message msg) {
        AsyncResult ar;
        switch (msg.what) {
            case 1:
                onSetComplete(msg, (AsyncResult) msg.obj);
                return;
            case 2:
                onGetClirComplete((AsyncResult) msg.obj);
                return;
            case 3:
                onQueryCfComplete((AsyncResult) msg.obj);
                return;
            case 4:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    this.mState = State.FAILED;
                    this.mMessage = getErrorMessage(ar);
                    if (this.mSc != null && ((this.mSc.equals(SC_107) || this.mSc.equals(SC_108)) && (ar.exception instanceof CommandException) && Error.FDN_CHECK_FAILURE == ((CommandException) ar.exception).getCommandError())) {
                        String str = this.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SC is ");
                        stringBuilder.append(this.mSc);
                        stringBuilder.append(" and exception is FDN_CHECK_FAILURE");
                        Rlog.d(str, stringBuilder.toString());
                        this.mMessage = "";
                    }
                    this.mPhone.onMMIDone(this);
                    return;
                }
                return;
            case 5:
                onQueryComplete((AsyncResult) msg.obj);
                return;
            case 6:
                ar = msg.obj;
                if (ar.exception == null && msg.arg1 == 1) {
                    boolean cffEnabled = msg.arg2 == 1;
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, cffEnabled, this.mDialingNumber);
                    }
                }
                onSetComplete(msg, ar);
                return;
            case 7:
                this.mPhone.onMMIDone(this);
                return;
            default:
                HwTelephonyFactory.getHwPhoneManager().handleMessageGsmMmiCode(this, msg);
                return;
        }
    }

    private CharSequence getErrorMessage(AsyncResult ar) {
        if (ar.exception instanceof CommandException) {
            Error err = ((CommandException) ar.exception).getCommandError();
            if (err == Error.FDN_CHECK_FAILURE) {
                Rlog.i(this.LOG_TAG, "FDN_CHECK_FAILURE");
                return this.mContext.getText(17040533);
            } else if (err == Error.USSD_MODIFIED_TO_DIAL) {
                Rlog.i(this.LOG_TAG, "USSD_MODIFIED_TO_DIAL");
                return this.mContext.getText(17041209);
            } else if (err == Error.USSD_MODIFIED_TO_SS) {
                Rlog.i(this.LOG_TAG, "USSD_MODIFIED_TO_SS");
                return this.mContext.getText(17041211);
            } else if (err == Error.USSD_MODIFIED_TO_USSD) {
                Rlog.i(this.LOG_TAG, "USSD_MODIFIED_TO_USSD");
                return this.mContext.getText(17041212);
            } else if (err == Error.SS_MODIFIED_TO_DIAL) {
                Rlog.i(this.LOG_TAG, "SS_MODIFIED_TO_DIAL");
                return this.mContext.getText(17041205);
            } else if (err == Error.SS_MODIFIED_TO_USSD) {
                Rlog.i(this.LOG_TAG, "SS_MODIFIED_TO_USSD");
                return this.mContext.getText(17041208);
            } else if (err == Error.SS_MODIFIED_TO_SS) {
                Rlog.i(this.LOG_TAG, "SS_MODIFIED_TO_SS");
                return this.mContext.getText(17041207);
            } else if (err == Error.OEM_ERROR_1) {
                Rlog.i(this.LOG_TAG, "OEM_ERROR_1 USSD_MODIFIED_TO_DIAL_VIDEO");
                return this.mContext.getText(17041210);
            }
        }
        return this.mContext.getText(17040531);
    }

    private CharSequence getScString() {
        if (this.mSc != null) {
            if (isServiceCodeCallBarring(this.mSc)) {
                if (isDocomo && this.mSc.equals(SC_BAICr)) {
                    return this.mContext.getText(33685524);
                }
                return this.mContext.getText(17039387);
            } else if (isServiceCodeCallForwarding(this.mSc)) {
                return HwTelephonyFactory.getHwPhoneManager().getCallForwardingString(this.mContext, this.mSc);
            } else {
                if (this.mSc.equals(SC_CLIP)) {
                    return this.mContext.getText(17039398);
                }
                if (this.mSc.equals(SC_CLIR)) {
                    return this.mContext.getText(17039399);
                }
                if (this.mSc.equals(SC_PWD)) {
                    return this.mContext.getText(17039458);
                }
                if (this.mSc.equals(SC_WAIT)) {
                    return this.mContext.getText(17039405);
                }
                if (isPinPukCommand()) {
                    return HwTelephonyFactory.getHwPhoneManager().processgoodPinString(this.mContext, this.mSc);
                }
            }
        }
        return "";
    }

    private void onSetComplete(Message msg, AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = State.FAILED;
            if (ar.exception instanceof CommandException) {
                Error err = ((CommandException) ar.exception).getCommandError();
                if (err == Error.PASSWORD_INCORRECT) {
                    if (isPinPukCommand()) {
                        if (!this.mSc.equals(SC_PUK) && !this.mSc.equals(SC_PUK2)) {
                            sb.append(HwTelephonyFactory.getHwPhoneManager().processBadPinString(this.mContext, this.mSc));
                        } else if (this.mSc.equals(SC_PUK)) {
                            sb.append(this.mContext.getText(17039689));
                        } else {
                            sb.append(this.mContext.getText(33685781));
                        }
                        int attemptsRemaining = msg.arg1;
                        if (attemptsRemaining <= 0) {
                            Rlog.d(this.LOG_TAG, "onSetComplete: PUK locked, cancel as lock screen will handle this");
                            this.mState = State.CANCELLED;
                        } else if (attemptsRemaining > 0) {
                            String str = this.LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onSetComplete: attemptsRemaining=");
                            stringBuilder.append(attemptsRemaining);
                            Rlog.d(str, stringBuilder.toString());
                            sb.append(this.mContext.getResources().getQuantityString(18153497, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)}));
                        }
                    } else {
                        sb.append(this.mContext.getText(17040631));
                    }
                } else if (err == Error.SIM_PUK2) {
                    sb.append(HwTelephonyFactory.getHwPhoneManager().processBadPinString(this.mContext, this.mSc));
                    sb.append("\n");
                    sb.append(this.mContext.getText(17040544));
                } else if (err == Error.REQUEST_NOT_SUPPORTED) {
                    if (this.mSc.equals(SC_PIN)) {
                        sb.append(this.mContext.getText(17039988));
                    }
                } else if (err == Error.FDN_CHECK_FAILURE) {
                    Rlog.i(this.LOG_TAG, "FDN_CHECK_FAILURE");
                    sb.append(this.mContext.getText(17040533));
                } else if (err == Error.MODEM_ERR) {
                    if (isServiceCodeCallForwarding(this.mSc) && this.mPhone.getServiceState().getVoiceRoaming() && !this.mPhone.supports3gppCallForwardingWhileRoaming()) {
                        sb.append(this.mContext.getText(17040532));
                    } else {
                        sb.append(getErrorMessage(ar));
                    }
                } else if (SC_PIN.equals(this.mSc)) {
                    sb.append(this.mContext.getText(17041082));
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(this.mContext.getText(17040531));
            }
        } else if (isActivate()) {
            this.mState = State.COMPLETE;
            if (this.mIsCallFwdReg) {
                sb.append(this.mContext.getText(17041081));
            } else if (mToastSwitch) {
                sb.append(this.mContext.getText(17041164));
            } else {
                sb.append(this.mContext.getText(17041077));
            }
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(1);
            }
        } else if (isDeactivate()) {
            this.mState = State.COMPLETE;
            if (mToastSwitch) {
                sb.append(this.mContext.getText(17041163));
            } else {
                sb.append(this.mContext.getText(17041076));
            }
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(2);
            }
        } else if (isRegister()) {
            this.mState = State.COMPLETE;
            sb.append(this.mContext.getText(17041081));
        } else if (isErasure()) {
            this.mState = State.COMPLETE;
            sb.append(this.mContext.getText(17041079));
        } else {
            this.mState = State.FAILED;
            sb.append(this.mContext.getText(17040531));
        }
        this.mMessage = sb;
        String str2 = this.LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("onSetComplete mmi=");
        stringBuilder2.append(this);
        Rlog.d(str2, stringBuilder2.toString());
        this.mPhone.onMMIDone(this);
    }

    private void onGetClirComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception == null) {
            int[] clirArgs = ar.result;
            switch (clirArgs[1]) {
                case 0:
                    sb.append(this.mContext.getText(17041080));
                    this.mState = State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(17039392));
                    this.mState = State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(17040531));
                    this.mState = State.FAILED;
                    break;
                case 3:
                    switch (clirArgs[0]) {
                        case 1:
                            sb.append(this.mContext.getText(17039391));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(17039390));
                            break;
                        default:
                            sb.append(this.mContext.getText(17039391));
                            break;
                    }
                    this.mState = State.COMPLETE;
                    break;
                case 4:
                    switch (clirArgs[0]) {
                        case 1:
                            sb.append(this.mContext.getText(17039389));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(17039388));
                            break;
                        default:
                            sb.append(this.mContext.getText(17039388));
                            break;
                    }
                    this.mState = State.COMPLETE;
                    break;
            }
        }
        this.mState = State.FAILED;
        sb.append(getErrorMessage(ar));
        this.mMessage = sb;
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetClirComplete: mmi=");
        stringBuilder.append(this);
        Rlog.d(str, stringBuilder.toString());
        this.mPhone.onMMIDone(this);
    }

    private CharSequence serviceClassToCFString(int serviceClass) {
        if (serviceClass == 4) {
            return this.mContext.getText(17041071);
        }
        if (serviceClass == 8) {
            return this.mContext.getText(17041074);
        }
        if (serviceClass == 16) {
            return this.mContext.getText(17041070);
        }
        if (serviceClass == 32) {
            return this.mContext.getText(17041069);
        }
        if (serviceClass == 64) {
            return this.mContext.getText(17041073);
        }
        if (serviceClass == 128) {
            return this.mContext.getText(17041072);
        }
        switch (serviceClass) {
            case 1:
                return this.mContext.getText(17041075);
            case 2:
                return this.mContext.getText(17041068);
            default:
                return null;
        }
    }

    private CharSequence makeCFQueryResultMessage(CallForwardInfo info, int serviceClassMask) {
        CharSequence template;
        String[] sources = new String[]{"{0}", "{1}", "{2}"};
        CharSequence[] destinations = new CharSequence[3];
        boolean z = false;
        boolean needTimeTemplate = info.reason == 2;
        if (info.status == 1) {
            if (needTimeTemplate) {
                template = this.mContext.getText(17039735);
            } else {
                template = this.mContext.getText(17039734);
            }
        } else if (info.status == 0 && isEmptyOrNull(info.number)) {
            template = this.mContext.getText(17039736);
        } else if (needTimeTemplate) {
            template = this.mContext.getText(17039738);
        } else {
            template = this.mContext.getText(17039737);
        }
        destinations[0] = serviceClassToCFString(info.serviceClass & serviceClassMask);
        destinations[1] = formatLtr(PhoneNumberUtils.stringFromStringAndTOA(info.number, info.toa));
        destinations[2] = Integer.toString(info.timeSeconds);
        if (info.reason == 0 && (info.serviceClass & serviceClassMask) == 1) {
            if (info.status == 1) {
                z = true;
            }
            boolean cffEnabled = z;
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, cffEnabled, info.number);
            }
        }
        return TextUtils.replace(template, sources, destinations);
    }

    private String formatLtr(String str) {
        return str == null ? str : BidiFormatter.getInstance().unicodeWrap(str, TextDirectionHeuristics.LTR, true);
    }

    private void onQueryCfComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = State.FAILED;
            sb.append(getErrorMessage(ar));
        } else {
            CallForwardInfo[] infos = ar.result;
            int serviceClassMask = 1;
            if (infos == null || infos.length == 0) {
                if (mToastSwitch) {
                    sb.append(this.mContext.getText(17041163));
                } else {
                    sb.append(this.mContext.getText(17041076));
                }
                if (this.mIccRecords != null) {
                    this.mPhone.setVoiceCallForwardingFlag(1, false, null);
                }
            } else {
                SpannableStringBuilder tb = new SpannableStringBuilder();
                while (serviceClassMask <= 128) {
                    int s = infos.length;
                    for (int i = 0; i < s; i++) {
                        if ((infos[i].serviceClass & serviceClassMask) != 0) {
                            tb.append(makeCFQueryResultMessage(infos[i], serviceClassMask));
                            tb.append("\n");
                        }
                    }
                    serviceClassMask <<= 1;
                }
                sb.append(tb);
            }
            this.mState = State.COMPLETE;
        }
        this.mMessage = sb;
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onQueryCfComplete: mmi=");
        stringBuilder.append(this);
        Rlog.d(str, stringBuilder.toString());
        this.mPhone.onMMIDone(this);
    }

    private void onQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = State.FAILED;
            sb.append(getErrorMessage(ar));
        } else {
            int[] ints = ar.result;
            if (ints.length == 0) {
                sb.append(this.mContext.getText(17040531));
            } else if (ints[0] == 0) {
                if (mToastSwitch) {
                    sb.append(this.mContext.getText(17040962));
                } else {
                    sb.append(this.mContext.getText(17041076));
                }
            } else if (this.mSc.equals(SC_WAIT)) {
                sb.append(createQueryCallWaitingResultMessage(ints[1]));
            } else if (isServiceCodeCallBarring(this.mSc)) {
                sb.append(createQueryCallBarringResultMessage(ints[0]));
            } else if (ints[0] != 1) {
                sb.append(this.mContext.getText(17040531));
            } else if (mToastSwitch) {
                sb.append(this.mContext.getText(17040963));
            } else {
                sb.append(this.mContext.getText(17041077));
            }
            this.mState = State.COMPLETE;
        }
        this.mMessage = sb;
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onQueryComplete: mmi=");
        stringBuilder.append(this);
        Rlog.d(str, stringBuilder.toString());
        this.mPhone.onMMIDone(this);
    }

    private CharSequence createQueryCallWaitingResultMessage(int serviceClass) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(17041078));
        for (int classMask = 1; classMask <= 128; classMask <<= 1) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }

    private CharSequence createQueryCallBarringResultMessage(int serviceClass) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(17041078));
        for (int classMask = 1; classMask <= 128; classMask <<= 1) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }

    public ResultReceiver getUssdCallbackReceiver() {
        return this.mCallbackReceiver;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GsmMmiCode {");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("State=");
        stringBuilder.append(getState());
        sb.append(stringBuilder.toString());
        if (this.mAction != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" action=");
            stringBuilder.append(this.mAction);
            sb.append(stringBuilder.toString());
        }
        if (this.mSc != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" sc=");
            stringBuilder.append(this.mSc);
            sb.append(stringBuilder.toString());
        }
        if (this.mSia != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" sia=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mSia));
            sb.append(stringBuilder.toString());
        }
        if (this.mSib != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" sib=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mSib));
            sb.append(stringBuilder.toString());
        }
        if (this.mSic != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" sic=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mSic));
            sb.append(stringBuilder.toString());
        }
        if (this.mPoundString != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" poundString=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mPoundString));
            sb.append(stringBuilder.toString());
        }
        if (this.mDialingNumber != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" dialingNumber=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mDialingNumber));
            sb.append(stringBuilder.toString());
        }
        if (this.mPwd != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" pwd=");
            stringBuilder.append(Rlog.pii(this.LOG_TAG, this.mPwd));
            sb.append(stringBuilder.toString());
        }
        if (this.mCallbackReceiver != null) {
            sb.append(" hasReceiver");
        }
        sb.append("}");
        return sb.toString();
    }

    public void setIncomingUSSD(boolean incomingUSSD) {
        this.mIncomingUSSD = incomingUSSD;
    }

    public void setImsPhone(Phone imsPhone) {
        this.mImsPhone = imsPhone;
    }

    public void setHwCallFwgReg(boolean isCallFwdReg) {
        this.mIsCallFwdReg = isCallFwdReg;
    }

    public boolean getHwCallFwdReg() {
        return this.mIsCallFwdReg;
    }

    public CharSequence createQueryCallWaitingResultMessageEx(int serviceClass) {
        return createQueryCallWaitingResultMessage(serviceClass);
    }

    public CharSequence makeCFQueryResultMessageEx(CallForwardInfo info, int serviceClassMask) {
        return makeCFQueryResultMessage(info, serviceClassMask);
    }

    public static int scToCallForwardReasonEx(String sc) {
        return scToCallForwardReason(sc);
    }

    public static int siToTimeEx(String si) {
        return siToTime(si);
    }
}
