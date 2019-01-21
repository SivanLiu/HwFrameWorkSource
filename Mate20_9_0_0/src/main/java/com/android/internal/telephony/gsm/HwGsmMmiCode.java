package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsSsInfo;
import android.text.SpannableStringBuilder;
import com.android.ims.ImsException;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.HwGsmAlphabet;
import com.android.internal.telephony.MmiCode.State;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.HwImsPhoneMmiCode;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.vsim.HwVSimConstants;
import huawei.cust.HwCfgFilePolicy;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HwGsmMmiCode {
    protected static final String CARRIER_MMICODE_UNESCAPEHTML = "carrier_mmicode_unescapehtml_bool";
    private static final int CLIR_DEFAULT = 0;
    private static final int CLIR_INVOCATION = 1;
    private static final int CLIR_NOT_PROVISIONED = 0;
    private static final int CLIR_PRESENTATION_ALLOWED_TEMPORARY = 4;
    private static final int CLIR_PRESENTATION_RESTRICTED_TEMPORARY = 3;
    private static final int CLIR_PROVISIONED_PERMANENT = 1;
    private static final int CLIR_SUPPRESSION = 2;
    static final String CONNECT_MESSAGE_ERRORCODE = "ut409perfix";
    static final int EVENT_HWIMS_BASE = 1000;
    static final int EVENT_HWIMS_GET_CLIR_COMPLETE = 1004;
    static final int EVENT_HWIMS_QUERY_CF_COMPLETE = 1006;
    static final int EVENT_HWIMS_QUERY_COMPLETE = 1002;
    static final int EVENT_HWIMS_SET_CFF_COMPLETE = 1005;
    static final int EVENT_HWIMS_SET_COMPLETE = 1001;
    static final int EVENT_HWIMS_SUPP_SVC_QUERY_COMPLETE = 1003;
    static final String HW_OPTA = SystemProperties.get("ro.config.hw_opta", "0");
    static final String HW_OPTB = SystemProperties.get("ro.config.hw_optb", "0");
    static final String LOG_TAG = "HwGsmMmiCode";
    public static final int MATCH_GROUP_DIALING_NUMBER = 16;
    public static final int MATCH_GROUP_PWD_CONFIRM = 15;
    public static final int MATCH_GROUP_SIA = 6;
    public static final int MATCH_GROUP_SIB = 9;
    public static final int MATCH_GROUP_SIC = 12;
    private static final boolean MMI_CODE_CUSTOM = SystemProperties.getBoolean("ro.config.hw_mmicode_custom", false);
    private static final int NUM_PRESENTATION_ALLOWED = 0;
    private static final int NUM_PRESENTATION_RESTRICTED = 1;
    static final String SC_CFB = "67";
    static final String SC_CFNR = "62";
    static final String SC_CFNRy = "61";
    static final String SC_CFU = "21";
    private static final String SC_CLIP = "30";
    private static final String SC_CLIR = "31";
    private static final String SC_COLP = "76";
    private static final String SC_COLR = "77";
    static final String SC_PIN = "04";
    static final String SC_PIN2 = "042";
    static final String SC_PUK = "05";
    static final String SC_PUK2 = "052";
    private static final String SC_PWD = "03";
    private static final String SC_WAIT = "43";
    static String huaweiMmiIgnoreList = SystemProperties.get("ro.config.hw_mmi_ignore_list", "null");
    static String huaweiMmiMatchList = SystemProperties.get("ro.config.hw_mmi_match_list", "null");
    private static final boolean isDocomo = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    static boolean mPromptUserBadPin2 = SystemProperties.getBoolean("ro.config.prompt_bad_pin2", true);
    private static final boolean mToastSwitch = SystemProperties.getBoolean("ro.config.hw_ss_toast", false);
    static Pattern sPatternHuaweiMMICode = Pattern.compile("(([\\*\\#\\d]{1,10},)*([\\*\\#\\d]{1,10})?)");
    public static final Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})((\\*#|\\*|#)([^*#]*)((\\*|#)([^*#]*)((\\*|#)([^*#]*)((\\*|#)([^*#]*))?)?)?)?#)(.*)");

    public static boolean isShortCodeCustomization() {
        return true == SystemProperties.getBoolean("gsm.hw.matchnum.vmn_shortcode", false);
    }

    public static String convertUssdMessage(String ussdMessage) {
        if (!HW_OPTA.equals("27") || !HW_OPTB.equals("604")) {
            return ussdMessage;
        }
        char[] ussdChr = ussdMessage.toCharArray();
        for (int i = 0; i < ussdChr.length; i++) {
            ussdChr[i] = HwGsmAlphabet.ussd_7bit_ucs2_to_gsm_char_default(ussdChr[i]);
            ussdChr[i] = HwGsmAlphabet.util_UnicodeToGsm7DefaultExtended(ussdChr[i]);
        }
        return new String(ussdChr);
    }

    public static int siToServiceClass(String si) {
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
                            case HwVSimConstants.EVENT_NETWORK_SCAN_COMPLETED /*24*/:
                                return 16;
                            case HwVSimConstants.CMD_GET_DEVSUBMODE /*25*/:
                                return 32;
                            case HwVSimConstants.EVENT_GET_DEVSUBMODE_DONE /*26*/:
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

    public static boolean isStringHuaweiIgnoreCode(String dialString) {
        Rlog.d(LOG_TAG, "isHuaweiIgnoreCode");
        if (dialString == null) {
            Rlog.e(LOG_TAG, "isStringHuaweiIgnoreCode null dial string");
            return false;
        } else if (isStringHuaweiCustCode(dialString)) {
            return false;
        } else {
            if (huaweiMmiIgnoreList.equals("null")) {
                Rlog.e(LOG_TAG, "isStringHuaweiIgnoreCode, null ignore code list");
            } else if (huaweiMmiIgnoreList.equals("ALL")) {
                Rlog.e(LOG_TAG, "isStringHuaweiIgnoreCode, block all codes");
                return true;
            } else {
                Matcher m = sPatternHuaweiMMICode.matcher(huaweiMmiIgnoreList);
                if (m.matches()) {
                    String[] notHwMmiCodes = m.group(1).split(",");
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isHuaweiIgnoreCode,group(1)=");
                    stringBuilder.append(m.group(1));
                    stringBuilder.append(", size()=");
                    stringBuilder.append(notHwMmiCodes.length);
                    Rlog.d(str, stringBuilder.toString());
                    if (Arrays.asList(notHwMmiCodes).contains(dialString)) {
                        return true;
                    }
                }
                Rlog.d(LOG_TAG, "isHuaweiIgnoreCode, not a ignore mmi code");
            }
            return false;
        }
    }

    public static boolean isStringHuaweiCustCode(String dialString) {
        Rlog.d(LOG_TAG, "isStringHuaweiCustCode");
        if (dialString == null) {
            Rlog.e(LOG_TAG, "isStringHuaweiCustCode null dial string");
            return false;
        } else if (huaweiMmiMatchList.equals("null")) {
            Rlog.e(LOG_TAG, "isStringHuaweiCustCode, null match code list");
            return false;
        } else {
            Matcher m = sPatternHuaweiMMICode.matcher(huaweiMmiMatchList);
            if (m.matches()) {
                String[] hwMmiCodes = m.group(1).split(",");
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isStringHuaweiCustCode,group(1)=");
                stringBuilder.append(m.group(1));
                stringBuilder.append(", size()=");
                stringBuilder.append(hwMmiCodes.length);
                Rlog.d(str, stringBuilder.toString());
                if (Arrays.asList(hwMmiCodes).contains(dialString)) {
                    return true;
                }
            }
            Rlog.d(LOG_TAG, "isStringHuaweiCustCode, not a customized mmi code");
            return false;
        }
    }

    public static CharSequence processgoodPinString(Context context, String sc) {
        Rlog.d(LOG_TAG, "processgoodPinString enter");
        CharSequence result = context.getText(17039457);
        if (mPromptUserBadPin2 && (SC_PIN2.equals(sc) || SC_PUK2.equals(sc))) {
            return context.getText(33685779);
        }
        return context.getText(17039457);
    }

    public static CharSequence processBadPinString(Context context, String sc) {
        Rlog.d(LOG_TAG, "processBadPinString enter");
        CharSequence result = context.getText(17039688);
        if (mPromptUserBadPin2 && (SC_PIN2.equals(sc) || SC_PUK2.equals(sc))) {
            return context.getText(33685780);
        }
        return context.getText(17039688);
    }

    public static int handlePasswordError(String sc) {
        Rlog.d(LOG_TAG, "handlePasswordError enter");
        if (!mPromptUserBadPin2) {
            return 17040521;
        }
        if (SC_PIN2.equals(sc) || SC_PUK2.equals(sc)) {
            return 33685782;
        }
        return 17040521;
    }

    public static int showMmiError(int sc, int slotId) {
        boolean custMmiError = false;
        boolean hasHwCfgConfig = false;
        try {
            Boolean MmiError = (Boolean) HwCfgFilePolicy.getValue("show_mmiError", slotId, Boolean.class);
            if (MmiError != null) {
                hasHwCfgConfig = true;
                custMmiError = MmiError.booleanValue();
            }
            if (hasHwCfgConfig && !custMmiError) {
                return sc;
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwCfgFile:custMmiError =");
            stringBuilder.append(custMmiError);
            Rlog.d(str, stringBuilder.toString());
            if (SystemProperties.getBoolean("ro.config.hw_show_mmiError", false) || custMmiError) {
                sc = 17040531;
            }
            return sc;
        } catch (Exception e) {
            Rlog.d(LOG_TAG, "read show_mmiError is error!");
        }
    }

    private static boolean isProcessGsmPhoneMmiCode(GsmMmiCode gsmMmiCode, Phone imsPhone) {
        if (imsPhone == null || gsmMmiCode == null) {
            Rlog.d(LOG_TAG, "imsPhone is null, don't process IMSPhone Mmi Code.");
            return true;
        } else if (!isDocomo) {
            return false;
        } else {
            Rlog.d(LOG_TAG, "Docomo need to process Mmi Code in GsmMmiCode");
            return true;
        }
    }

    public static boolean processImsPhoneMmiCode(GsmMmiCode gsmMmiCode, Phone imsPhone) {
        String str;
        StringBuilder stringBuilder;
        GsmMmiCode gsmMmiCode2 = gsmMmiCode;
        Phone phone = imsPhone;
        boolean z = false;
        if (isProcessGsmPhoneMmiCode(gsmMmiCode, imsPhone)) {
            return false;
        }
        String dialingNumber;
        if (gsmMmiCode2.mSc != null && gsmMmiCode2.mSc.equals(SC_CLIP)) {
            Rlog.d(LOG_TAG, "is CLIP");
            HwImsPhoneMmiCode.isUnSupportMMICode(gsmMmiCode2.mSc, imsPhone.getPhoneId());
            if (gsmMmiCode.isInterrogate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().queryCLIP(gsmMmiCode2.obtainMessage(1003, gsmMmiCode2));
                } catch (ImsException ex) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception in getUtInterface : ");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                }
            } else if (gsmMmiCode.isActivate() || gsmMmiCode.isDeactivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCLIP(gsmMmiCode.isActivate(), gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException e) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIP.");
                }
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        } else if (gsmMmiCode2.mSc != null && gsmMmiCode2.mSc.equals(SC_CLIR)) {
            Rlog.d(LOG_TAG, "is CLIR");
            if (gsmMmiCode.isActivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCLIR(1, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException ex2) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception in getUtInterface : ");
                    stringBuilder.append(ex2);
                    Rlog.e(str, stringBuilder.toString());
                }
            } else if (gsmMmiCode.isDeactivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCLIR(2, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException ex22) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception in getUtInterface : ");
                    stringBuilder.append(ex22);
                    Rlog.e(str, stringBuilder.toString());
                }
            } else if (gsmMmiCode.isInterrogate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().queryCLIR(gsmMmiCode2.obtainMessage(1004, gsmMmiCode2));
                } catch (ImsException ex222) {
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception in getUtInterface : ");
                    stringBuilder.append(ex222);
                    Rlog.e(str, stringBuilder.toString());
                }
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        } else if (gsmMmiCode2.mSc != null && gsmMmiCode2.mSc.equals(SC_COLP)) {
            Rlog.d(LOG_TAG, "is COLP");
            HwImsPhoneMmiCode.isUnSupportMMICode(gsmMmiCode2.mSc, imsPhone.getPhoneId());
            if (gsmMmiCode.isActivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCOLP(true, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException e2) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLP.");
                }
            } else if (gsmMmiCode.isDeactivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCOLP(false, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException e3) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLP.");
                }
            } else if (gsmMmiCode.isInterrogate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().queryCOLP(gsmMmiCode2.obtainMessage(1003, gsmMmiCode2));
                } catch (ImsException e4) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLP.");
                }
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        } else if (gsmMmiCode2.mSc != null && gsmMmiCode2.mSc.equals(SC_COLR)) {
            Rlog.d(LOG_TAG, "is COLR");
            HwImsPhoneMmiCode.isUnSupportMMICode(gsmMmiCode2.mSc, imsPhone.getPhoneId());
            if (gsmMmiCode.isActivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCOLR(0, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException e5) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
                }
            } else if (gsmMmiCode.isDeactivate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().updateCOLR(1, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
                } catch (ImsException e6) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
                }
            } else if (gsmMmiCode.isInterrogate()) {
                try {
                    ((ImsPhoneCallTracker) imsPhone.getCallTracker()).getUtInterfaceEx().queryCOLR(gsmMmiCode2.obtainMessage(1003, gsmMmiCode2));
                } catch (ImsException e7) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLR.");
                }
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        } else if (GsmMmiCode.isServiceCodeCallForwarding(gsmMmiCode2.mSc)) {
            Rlog.d(LOG_TAG, "is CF");
            dialingNumber = gsmMmiCode2.mSia;
            int serviceClass = siToServiceClass(gsmMmiCode2.mSib);
            int reason = GsmMmiCode.scToCallForwardReasonEx(gsmMmiCode2.mSc);
            int time = GsmMmiCode.siToTimeEx(gsmMmiCode2.mSic);
            if (gsmMmiCode.isInterrogate()) {
                ((ImsPhone) phone).getCallForwardForServiceClass(reason, serviceClass, gsmMmiCode2.obtainMessage(1006, gsmMmiCode2));
            } else {
                int cfAction;
                if (gsmMmiCode.isActivate()) {
                    if (isEmptyOrNull(dialingNumber)) {
                        cfAction = 1;
                        gsmMmiCode2.setHwCallFwgReg(false);
                    } else {
                        cfAction = 3;
                        gsmMmiCode2.setHwCallFwgReg(true);
                    }
                } else if (gsmMmiCode.isDeactivate()) {
                    cfAction = 0;
                } else if (gsmMmiCode.isRegister()) {
                    cfAction = 3;
                } else if (gsmMmiCode.isErasure()) {
                    cfAction = 4;
                } else {
                    throw new RuntimeException("invalid action");
                }
                int cfAction2 = cfAction;
                boolean z2 = (reason == 0 || reason == 4) && ((serviceClass & 1) != 0 || serviceClass == 0);
                boolean isSettingUnconditionalVoice = z2;
                if (cfAction2 == 1 || cfAction2 == 3) {
                    z = true;
                }
                boolean isEnableDesired = z;
                Rlog.d(LOG_TAG, "is CF setCallForward");
                phone.setCallForwardingOption(cfAction2, reason, dialingNumber, serviceClass, time, gsmMmiCode2.obtainMessage(1005, isSettingUnconditionalVoice, isEnableDesired, gsmMmiCode2));
            }
            return true;
        } else if (GsmMmiCode.isServiceCodeCallBarring(gsmMmiCode2.mSc)) {
            dialingNumber = gsmMmiCode2.mSia;
            int serviceClass2 = siToServiceClass(gsmMmiCode2.mSib);
            String facility = GsmMmiCode.scToBarringFacility(gsmMmiCode2.mSc);
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("is CB setCallBarring, with serviceClass:");
            stringBuilder2.append(serviceClass2);
            Rlog.d(str2, stringBuilder2.toString());
            if (gsmMmiCode.isInterrogate()) {
                ((ImsPhone) phone).getCallBarring(facility, gsmMmiCode2.obtainMessage(1003, gsmMmiCode2), serviceClass2);
            } else if (gsmMmiCode.isActivate() || gsmMmiCode.isDeactivate()) {
                ((ImsPhone) phone).setCallBarring(facility, gsmMmiCode.isActivate(), dialingNumber, gsmMmiCode2.obtainMessage(1001, gsmMmiCode2), serviceClass2);
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        } else if (gsmMmiCode2.mSc == null || !gsmMmiCode2.mSc.equals(SC_WAIT)) {
            return false;
        } else {
            if (gsmMmiCode.isActivate() || gsmMmiCode.isDeactivate()) {
                phone.setCallWaiting(gsmMmiCode.isActivate(), gsmMmiCode2.obtainMessage(1001, gsmMmiCode2));
            } else if (gsmMmiCode.isInterrogate()) {
                phone.getCallWaiting(gsmMmiCode2.obtainMessage(1002, gsmMmiCode2));
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
            return true;
        }
    }

    public static void handleMessageGsmMmiCode(GsmMmiCode gsmMmiCode, Message msg) {
        switch (msg.what) {
            case 1001:
                onHwImsSetComplete(gsmMmiCode, msg, (AsyncResult) msg.obj);
                return;
            case 1002:
                onHwImsQueryComplete(gsmMmiCode, (AsyncResult) msg.obj);
                return;
            case 1003:
                onHwImsSuppSvcQueryComplete(gsmMmiCode, (AsyncResult) msg.obj);
                return;
            case 1004:
                onHwImsQueryClirComplete(gsmMmiCode, (AsyncResult) msg.obj);
                return;
            case 1005:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null && msg.arg1 == 1) {
                    boolean cffEnabled = msg.arg2 == 1;
                    if (gsmMmiCode.mIccRecords != null) {
                        gsmMmiCode.mIccRecords.setVoiceCallForwardingFlag(1, cffEnabled, gsmMmiCode.mDialingNumber);
                    }
                }
                onHwImsSetComplete(gsmMmiCode, msg, ar);
                return;
            case 1006:
                onHwImsQueryCfComplete(gsmMmiCode, msg.obj);
                return;
            default:
                return;
        }
    }

    private static CharSequence getScString(GsmMmiCode gsmMmiCode) {
        if (!(gsmMmiCode == null || gsmMmiCode.mSc == null)) {
            if (GsmMmiCode.isServiceCodeCallBarring(gsmMmiCode.mSc)) {
                return gsmMmiCode.mContext.getText(17039387);
            }
            if (GsmMmiCode.isServiceCodeCallForwarding(gsmMmiCode.mSc)) {
                return getCallForwardingString(gsmMmiCode.mContext, gsmMmiCode.mSc);
            }
            if (gsmMmiCode.mSc.equals(SC_CLIP)) {
                return gsmMmiCode.mContext.getText(17039398);
            }
            if (gsmMmiCode.mSc.equals(SC_CLIR)) {
                return gsmMmiCode.mContext.getText(17039399);
            }
            if (gsmMmiCode.mSc.equals(SC_PWD)) {
                return gsmMmiCode.mContext.getText(17039458);
            }
            if (gsmMmiCode.mSc.equals(SC_WAIT)) {
                return gsmMmiCode.mContext.getText(17039405);
            }
            if (gsmMmiCode.mSc.equals(SC_COLP)) {
                return gsmMmiCode.mContext.getText(17039403);
            }
            if (gsmMmiCode.mSc.equals(SC_COLR)) {
                return gsmMmiCode.mContext.getText(17039404);
            }
        }
        return "";
    }

    private static CharSequence getErrorMessage(GsmMmiCode gsmMmiCode, AsyncResult ar) {
        return gsmMmiCode.mContext.getText(17040531);
    }

    private static CharSequence getImsExceptionMessage(GsmMmiCode gsmMmiCode, ImsException error) {
        if (error.getCode() == 0) {
            return getErrorMessage(gsmMmiCode, null);
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return getErrorMessage(gsmMmiCode, null);
    }

    private static boolean isEmptyOrNull(CharSequence s) {
        return s == null || s.length() == 0;
    }

    private static boolean isUtNoConnectionException(Exception e) {
        if (e instanceof ImsException) {
            if (((ImsException) e).getCode() == 831) {
                return true;
            }
        } else if ((e instanceof CommandException) && ((CommandException) e).getCommandError() == Error.UT_NO_CONNECTION) {
            return true;
        }
        return false;
    }

    private static void onHwImsSuppSvcQueryComplete(GsmMmiCode gsmMmiCode, AsyncResult ar) {
        if (isUtNoConnectionException((Exception) ar.exception)) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, new CommandException(Error.UT_NO_CONNECTION));
            return;
        }
        StringBuilder sb = new StringBuilder(getScString(gsmMmiCode));
        sb.append("\n");
        if (ar.exception != null) {
            gsmMmiCode.mState = State.FAILED;
            if (ar.exception instanceof ImsException) {
                sb.append(getImsExceptionMessage(gsmMmiCode, ar.exception));
            } else {
                sb.append(getErrorMessage(gsmMmiCode, ar));
            }
        } else {
            gsmMmiCode.mState = State.FAILED;
            if (ar.result instanceof Bundle) {
                Rlog.d(LOG_TAG, "onHwImsSuppSvcQueryComplete : Received CLIP/COLP/COLR Response.");
                ImsSsInfo ssInfo = (ImsSsInfo) ar.result.getParcelable("imsSsInfo");
                if (ssInfo != null) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onHwImsSuppSvcQueryComplete : ImsSsInfo mStatus = ");
                    stringBuilder.append(ssInfo.mStatus);
                    Rlog.d(str, stringBuilder.toString());
                    if (ssInfo.mStatus == 0) {
                        if (mToastSwitch) {
                            sb.append(gsmMmiCode.mContext.getText(17040962));
                        } else {
                            sb.append(gsmMmiCode.mContext.getText(17041076));
                        }
                        gsmMmiCode.mState = State.COMPLETE;
                    } else if (ssInfo.mStatus == 1) {
                        if (mToastSwitch) {
                            sb.append(gsmMmiCode.mContext.getText(17040963));
                        } else {
                            sb.append(gsmMmiCode.mContext.getText(17041077));
                        }
                        gsmMmiCode.mState = State.COMPLETE;
                    } else {
                        sb.append(gsmMmiCode.mContext.getText(17040531));
                    }
                } else {
                    sb.append(gsmMmiCode.mContext.getText(17040531));
                }
            } else {
                Rlog.d(LOG_TAG, "onHwImsSuppSvcQueryComplete : Received Call Barring Response.");
                if (ar.result[0] != 0) {
                    if (mToastSwitch) {
                        sb.append(gsmMmiCode.mContext.getText(17040963));
                    } else {
                        sb.append(gsmMmiCode.mContext.getText(17041077));
                    }
                    gsmMmiCode.mState = State.COMPLETE;
                } else {
                    if (mToastSwitch) {
                        sb.append(gsmMmiCode.mContext.getText(17040962));
                    } else {
                        sb.append(gsmMmiCode.mContext.getText(17041076));
                    }
                    gsmMmiCode.mState = State.COMPLETE;
                }
            }
        }
        gsmMmiCode.mMessage = sb;
        gsmMmiCode.mPhone.onMMIDone(gsmMmiCode);
    }

    private static void onHwImsSetComplete(GsmMmiCode gsmMmiCode, Message msg, AsyncResult ar) {
        if (isUtNoConnectionException((Exception) ar.exception)) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, new CommandException(Error.UT_NO_CONNECTION));
            return;
        }
        StringBuilder sb = new StringBuilder(getScString(gsmMmiCode));
        sb.append("\n");
        if (ar.exception != null) {
            gsmMmiCode.mState = State.FAILED;
            if (ar.exception instanceof CommandException) {
                Error err = ((CommandException) ar.exception).getCommandError();
                String message = ((CommandException) ar.exception).getMessage();
                if (err == Error.PASSWORD_INCORRECT) {
                    sb.append(gsmMmiCode.mContext.getText(17040631));
                } else if (message == null || message.isEmpty() || !message.contains(CONNECT_MESSAGE_ERRORCODE)) {
                    sb.append(gsmMmiCode.mContext.getText(17040531));
                } else {
                    sb.append(message.substring(null, message.indexOf(CONNECT_MESSAGE_ERRORCODE)));
                    message = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onHwImsSetComplete : errorMessage = ");
                    stringBuilder.append(sb);
                    Rlog.d(message, stringBuilder.toString());
                }
            } else {
                sb.append(getImsExceptionMessage(gsmMmiCode, ar.exception));
            }
        } else if (gsmMmiCode.isActivate()) {
            gsmMmiCode.mState = State.COMPLETE;
            if (gsmMmiCode.getHwCallFwdReg()) {
                sb.append(gsmMmiCode.mContext.getText(17041081));
            } else if (mToastSwitch) {
                sb.append(gsmMmiCode.mContext.getText(17041164));
            } else {
                sb.append(gsmMmiCode.mContext.getText(17041077));
            }
            if (SC_CLIR.equals(gsmMmiCode.mSc)) {
                gsmMmiCode.mPhone.saveClirSetting(1);
            }
        } else if (gsmMmiCode.isDeactivate()) {
            gsmMmiCode.mState = State.COMPLETE;
            if (mToastSwitch) {
                sb.append(gsmMmiCode.mContext.getText(17041163));
            } else {
                sb.append(gsmMmiCode.mContext.getText(17041076));
            }
            if (SC_CLIR.equals(gsmMmiCode.mSc)) {
                gsmMmiCode.mPhone.saveClirSetting(2);
            }
        } else if (gsmMmiCode.isRegister()) {
            gsmMmiCode.mState = State.COMPLETE;
            sb.append(gsmMmiCode.mContext.getText(17041081));
        } else if (gsmMmiCode.isErasure()) {
            gsmMmiCode.mState = State.COMPLETE;
            sb.append(gsmMmiCode.mContext.getText(17041079));
        } else {
            gsmMmiCode.mState = State.FAILED;
            sb.append(gsmMmiCode.mContext.getText(17040531));
        }
        gsmMmiCode.mMessage = sb;
        if (ar.exception == null) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode);
        } else if (ar.exception instanceof CommandException) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, (CommandException) ar.exception);
        }
    }

    private static void onHwImsQueryClirComplete(GsmMmiCode gsmMmiCode, AsyncResult ar) {
        if (isUtNoConnectionException((Exception) ar.exception)) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, new CommandException(Error.UT_NO_CONNECTION));
            return;
        }
        StringBuilder sb = new StringBuilder(getScString(gsmMmiCode));
        sb.append("\n");
        gsmMmiCode.mState = State.FAILED;
        if (ar.exception == null) {
            int[] clirInfo;
            if (ar.result instanceof Bundle) {
                clirInfo = ar.result.getIntArray("queryClir");
            } else {
                clirInfo = (int[]) ar.result;
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CLIR param n=");
            stringBuilder.append(clirInfo[0]);
            stringBuilder.append(" m=");
            stringBuilder.append(clirInfo[1]);
            Rlog.d(str, stringBuilder.toString());
            switch (clirInfo[1]) {
                case 0:
                    sb.append(gsmMmiCode.mContext.getText(17041080));
                    gsmMmiCode.mState = State.COMPLETE;
                    break;
                case 1:
                    sb.append(gsmMmiCode.mContext.getText(17039392));
                    gsmMmiCode.mState = State.COMPLETE;
                    break;
                case 3:
                    switch (clirInfo[0]) {
                        case 0:
                            sb.append(gsmMmiCode.mContext.getText(17039391));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        case 1:
                            sb.append(gsmMmiCode.mContext.getText(17039391));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        case 2:
                            sb.append(gsmMmiCode.mContext.getText(17039390));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        default:
                            sb.append(gsmMmiCode.mContext.getText(17040531));
                            gsmMmiCode.mState = State.FAILED;
                            break;
                    }
                case 4:
                    switch (clirInfo[0]) {
                        case 0:
                            sb.append(gsmMmiCode.mContext.getText(17039388));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        case 1:
                            sb.append(gsmMmiCode.mContext.getText(17039389));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        case 2:
                            sb.append(gsmMmiCode.mContext.getText(17039388));
                            gsmMmiCode.mState = State.COMPLETE;
                            break;
                        default:
                            sb.append(gsmMmiCode.mContext.getText(17040531));
                            gsmMmiCode.mState = State.FAILED;
                            break;
                    }
                default:
                    sb.append(gsmMmiCode.mContext.getText(17040531));
                    gsmMmiCode.mState = State.FAILED;
                    break;
            }
        } else if (ar.exception instanceof ImsException) {
            sb.append(getImsExceptionMessage(gsmMmiCode, ar.exception));
        }
        gsmMmiCode.mMessage = sb;
        gsmMmiCode.mPhone.onMMIDone(gsmMmiCode);
    }

    private static void onHwImsQueryCfComplete(GsmMmiCode gsmMmiCode, AsyncResult ar) {
        if (isUtNoConnectionException((Exception) ar.exception)) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, new CommandException(Error.UT_NO_CONNECTION));
            return;
        }
        StringBuilder sb = new StringBuilder(getScString(gsmMmiCode));
        sb.append("\n");
        if (ar.exception != null) {
            gsmMmiCode.mState = State.FAILED;
            if (ar.exception instanceof ImsException) {
                sb.append(getImsExceptionMessage(gsmMmiCode, ar.exception));
            } else {
                sb.append(getErrorMessage(gsmMmiCode, ar));
            }
        } else {
            CallForwardInfo[] infos = ar.result;
            int serviceClassMask = 1;
            if (infos.length == 0) {
                if (mToastSwitch) {
                    sb.append(gsmMmiCode.mContext.getText(17040962));
                } else {
                    sb.append(gsmMmiCode.mContext.getText(17041076));
                }
                if (gsmMmiCode.mIccRecords != null) {
                    gsmMmiCode.mIccRecords.setVoiceCallForwardingFlag(1, false, null);
                }
            } else {
                SpannableStringBuilder tb = new SpannableStringBuilder();
                while (serviceClassMask <= 128) {
                    int s = infos.length;
                    for (int i = 0; i < s; i++) {
                        if ((infos[i].serviceClass & serviceClassMask) != 0) {
                            tb.append(gsmMmiCode.makeCFQueryResultMessageEx(infos[i], serviceClassMask));
                            tb.append("\n");
                        }
                    }
                    serviceClassMask <<= 1;
                }
                sb.append(tb);
            }
            gsmMmiCode.mState = State.COMPLETE;
        }
        gsmMmiCode.mMessage = sb;
        gsmMmiCode.mPhone.onMMIDone(gsmMmiCode);
    }

    private static void onHwImsQueryComplete(GsmMmiCode gsmMmiCode, AsyncResult ar) {
        if (isUtNoConnectionException((Exception) ar.exception)) {
            gsmMmiCode.mPhone.onMMIDone(gsmMmiCode, new CommandException(Error.UT_NO_CONNECTION));
            return;
        }
        StringBuilder sb = new StringBuilder(getScString(gsmMmiCode));
        sb.append("\n");
        if (ar.exception != null) {
            gsmMmiCode.mState = State.FAILED;
            if (ar.exception instanceof ImsException) {
                sb.append(getImsExceptionMessage(gsmMmiCode, ar.exception));
            } else {
                sb.append(getErrorMessage(gsmMmiCode, ar));
            }
        } else {
            int[] ints = ar.result;
            if (ints.length == 0) {
                sb.append(gsmMmiCode.mContext.getText(17040531));
            } else if (ints[0] == 0) {
                if (mToastSwitch) {
                    sb.append(gsmMmiCode.mContext.getText(17040962));
                } else {
                    sb.append(gsmMmiCode.mContext.getText(17041076));
                }
            } else if (gsmMmiCode.mSc.equals(SC_WAIT)) {
                sb.append(gsmMmiCode.createQueryCallWaitingResultMessageEx(ints[1]));
            } else if (ints[0] != 1) {
                sb.append(gsmMmiCode.mContext.getText(17040531));
            } else if (mToastSwitch) {
                sb.append(gsmMmiCode.mContext.getText(17040963));
            } else {
                sb.append(gsmMmiCode.mContext.getText(17041077));
            }
            gsmMmiCode.mState = State.COMPLETE;
        }
        gsmMmiCode.mMessage = sb;
        gsmMmiCode.mPhone.onMMIDone(gsmMmiCode);
    }

    private static boolean getHwMmicodeCustom() {
        boolean valueFromProp = MMI_CODE_CUSTOM;
        boolean result = valueFromProp;
        Boolean valueFromCard1 = (Boolean) HwCfgFilePolicy.getValue("hw_mmicode_custom", 0, Boolean.class);
        boolean z = true;
        if (valueFromCard1 != null) {
            boolean z2 = result || valueFromCard1.booleanValue();
            result = z2;
        }
        Boolean valueFromCard2 = (Boolean) HwCfgFilePolicy.getValue("hw_mmicode_custom", 1, Boolean.class);
        if (valueFromCard2 != null) {
            if (!(result || valueFromCard2.booleanValue())) {
                z = false;
            }
            result = z;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHwMmicodeCustom, card1:");
        stringBuilder.append(valueFromCard1);
        stringBuilder.append(", card2:");
        stringBuilder.append(valueFromCard2);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        Rlog.d(str, stringBuilder.toString());
        return result;
    }

    public static CharSequence getCallForwardingString(Context context, String sc) {
        if (!getHwMmicodeCustom()) {
            return context.getText(17039393);
        }
        if (sc.equals(SC_CFU)) {
            return context.getText(17039397);
        }
        if (sc.equals(SC_CFB)) {
            return context.getText(17039396);
        }
        if (sc.equals(SC_CFNRy)) {
            return context.getText(17039395);
        }
        if (sc.equals(SC_CFNR)) {
            return context.getText(17039394);
        }
        return context.getText(17039393);
    }

    public static boolean processSendUssdInImsCall(GsmMmiCode gsmMmiCode, Phone imsPhone) {
        CarrierConfigManager cfgManager = (CarrierConfigManager) gsmMmiCode.mContext.getSystemService("carrier_config");
        if (cfgManager == null) {
            Rlog.w(LOG_TAG, "Carrier config service is not available.");
            return false;
        }
        int subId = gsmMmiCode.mPhone.getSubId();
        PersistableBundle b = cfgManager.getConfigForSubId(subId);
        if (b == null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't get the config. subId = ");
            stringBuilder.append(subId);
            Rlog.w(str, stringBuilder.toString());
            return false;
        } else if (!b.getBoolean("carrier_forbid_ussd_when_ims_calling_bool") || !ImsPhone.class.isInstance(imsPhone) || !((ImsPhone) imsPhone).isInCall()) {
            return false;
        } else {
            Message onCompleted = gsmMmiCode.obtainMessage(4, gsmMmiCode);
            AsyncResult.forMessage(onCompleted).exception = new CommandException(Error.GENERIC_FAILURE);
            onCompleted.sendToTarget();
            return true;
        }
    }

    public static boolean needUnEscapeHtmlforUssdMsg(Phone phone) {
        if (phone == null || phone.getContext() == null) {
            Rlog.d(LOG_TAG, "needUnEscapeHtmlforUssdMsg: phone or phone Context is null, return false;");
            return false;
        }
        int subId = phone.getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            CarrierConfigManager configManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
            if (configManager == null) {
                Rlog.e(LOG_TAG, "needUnEscapeHtmlforUssdMsg: configManager is null, return false;");
                return false;
            }
            PersistableBundle bundle = configManager.getConfigForSubId(subId);
            if (bundle == null || bundle.get(CARRIER_MMICODE_UNESCAPEHTML) == null) {
                return false;
            }
            return bundle.getBoolean(CARRIER_MMICODE_UNESCAPEHTML);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("needUnEscapeHtmlforUssdMsg: subId=");
        stringBuilder.append(subId);
        stringBuilder.append(" is Invalid, return false;");
        Rlog.e(str, stringBuilder.toString());
        return false;
    }
}
