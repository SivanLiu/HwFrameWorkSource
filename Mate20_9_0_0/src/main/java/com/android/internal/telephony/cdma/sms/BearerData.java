package com.android.internal.telephony.cdma.sms;

import android.common.HwFrameworkFactory;
import android.content.res.Resources;
import android.telephony.Rlog;
import android.telephony.SmsCbCmasInfo;
import android.telephony.cdma.CdmaSmsCbProgramData;
import android.telephony.cdma.CdmaSmsCbProgramResults;
import android.text.format.Time;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.BitwiseInputStream;
import com.android.internal.util.BitwiseOutputStream;
import com.android.internal.util.BitwiseOutputStream.AccessException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimeZone;

public final class BearerData {
    public static final int ALERT_DEFAULT = 0;
    public static final int ALERT_HIGH_PRIO = 3;
    public static final int ALERT_LOW_PRIO = 1;
    public static final int ALERT_MEDIUM_PRIO = 2;
    public static final int DISPLAY_MODE_DEFAULT = 1;
    public static final int DISPLAY_MODE_IMMEDIATE = 0;
    public static final int DISPLAY_MODE_USER = 2;
    public static final int ERROR_NONE = 0;
    public static final int ERROR_PERMANENT = 3;
    public static final int ERROR_TEMPORARY = 2;
    public static final int ERROR_UNDEFINED = 255;
    public static final int LANGUAGE_CHINESE = 6;
    public static final int LANGUAGE_ENGLISH = 1;
    public static final int LANGUAGE_FRENCH = 2;
    public static final int LANGUAGE_HEBREW = 7;
    public static final int LANGUAGE_JAPANESE = 4;
    public static final int LANGUAGE_KOREAN = 5;
    public static final int LANGUAGE_SPANISH = 3;
    public static final int LANGUAGE_UNKNOWN = 0;
    private static final String LOG_TAG = "BearerData";
    public static final int MESSAGE_TYPE_CANCELLATION = 3;
    public static final int MESSAGE_TYPE_DELIVER = 1;
    public static final int MESSAGE_TYPE_DELIVERY_ACK = 4;
    public static final int MESSAGE_TYPE_DELIVER_REPORT = 7;
    public static final int MESSAGE_TYPE_READ_ACK = 6;
    public static final int MESSAGE_TYPE_SUBMIT = 2;
    public static final int MESSAGE_TYPE_SUBMIT_REPORT = 8;
    public static final int MESSAGE_TYPE_USER_ACK = 5;
    public static final int PRIORITY_EMERGENCY = 3;
    public static final int PRIORITY_INTERACTIVE = 1;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_URGENT = 2;
    public static final int PRIVACY_CONFIDENTIAL = 2;
    public static final int PRIVACY_NOT_RESTRICTED = 0;
    public static final int PRIVACY_RESTRICTED = 1;
    public static final int PRIVACY_SECRET = 3;
    public static final int RELATIVE_TIME_DAYS_LIMIT = 196;
    public static final int RELATIVE_TIME_HOURS_LIMIT = 167;
    public static final int RELATIVE_TIME_INDEFINITE = 245;
    public static final int RELATIVE_TIME_MINS_LIMIT = 143;
    public static final int RELATIVE_TIME_MOBILE_INACTIVE = 247;
    public static final int RELATIVE_TIME_NOW = 246;
    public static final int RELATIVE_TIME_RESERVED = 248;
    public static final int RELATIVE_TIME_WEEKS_LIMIT = 244;
    public static final int STATUS_ACCEPTED = 0;
    public static final int STATUS_BLOCKED_DESTINATION = 7;
    public static final int STATUS_CANCELLED = 3;
    public static final int STATUS_CANCEL_FAILED = 6;
    public static final int STATUS_DELIVERED = 2;
    public static final int STATUS_DEPOSITED_TO_INTERNET = 1;
    public static final int STATUS_DUPLICATE_MESSAGE = 9;
    public static final int STATUS_INVALID_DESTINATION = 10;
    public static final int STATUS_MESSAGE_EXPIRED = 13;
    public static final int STATUS_NETWORK_CONGESTION = 4;
    public static final int STATUS_NETWORK_ERROR = 5;
    public static final int STATUS_TEXT_TOO_LONG = 8;
    public static final int STATUS_UNDEFINED = 255;
    public static final int STATUS_UNKNOWN_ERROR = 31;
    private static final byte SUBPARAM_ALERT_ON_MESSAGE_DELIVERY = (byte) 12;
    private static final byte SUBPARAM_CALLBACK_NUMBER = (byte) 14;
    private static final byte SUBPARAM_DEFERRED_DELIVERY_TIME_ABSOLUTE = (byte) 6;
    private static final byte SUBPARAM_DEFERRED_DELIVERY_TIME_RELATIVE = (byte) 7;
    private static final byte SUBPARAM_ID_LAST_DEFINED = (byte) 23;
    private static final byte SUBPARAM_LANGUAGE_INDICATOR = (byte) 13;
    private static final byte SUBPARAM_MESSAGE_CENTER_TIME_STAMP = (byte) 3;
    private static final byte SUBPARAM_MESSAGE_DEPOSIT_INDEX = (byte) 17;
    private static final byte SUBPARAM_MESSAGE_DISPLAY_MODE = (byte) 15;
    private static final byte SUBPARAM_MESSAGE_IDENTIFIER = (byte) 0;
    private static final byte SUBPARAM_MESSAGE_STATUS = (byte) 20;
    private static final byte SUBPARAM_NUMBER_OF_MESSAGES = (byte) 11;
    private static final byte SUBPARAM_PRIORITY_INDICATOR = (byte) 8;
    private static final byte SUBPARAM_PRIVACY_INDICATOR = (byte) 9;
    private static final byte SUBPARAM_REPLY_OPTION = (byte) 10;
    private static final byte SUBPARAM_SERVICE_CATEGORY_PROGRAM_DATA = (byte) 18;
    private static final byte SUBPARAM_SERVICE_CATEGORY_PROGRAM_RESULTS = (byte) 19;
    private static final byte SUBPARAM_USER_DATA = (byte) 1;
    private static final byte SUBPARAM_USER_RESPONSE_CODE = (byte) 2;
    private static final byte SUBPARAM_VALIDITY_PERIOD_ABSOLUTE = (byte) 4;
    private static final byte SUBPARAM_VALIDITY_PERIOD_RELATIVE = (byte) 5;
    public int alert = 0;
    public boolean alertIndicatorSet = false;
    public CdmaSmsAddress callbackNumber;
    public SmsCbCmasInfo cmasWarningInfo;
    public TimeStamp deferredDeliveryTimeAbsolute;
    public int deferredDeliveryTimeRelative;
    public boolean deferredDeliveryTimeRelativeSet;
    public boolean deliveryAckReq;
    public int depositIndex;
    public int displayMode = 1;
    public boolean displayModeSet = false;
    public int errorClass = 255;
    public boolean hasUserDataHeader;
    public int language = 0;
    public boolean languageIndicatorSet = false;
    public int messageId;
    public int messageStatus = 255;
    public boolean messageStatusSet = false;
    public int messageType;
    public TimeStamp msgCenterTimeStamp;
    public int numberOfMessages;
    public int priority = 0;
    public boolean priorityIndicatorSet = false;
    public int privacy = 0;
    public boolean privacyIndicatorSet = false;
    public boolean readAckReq;
    public boolean reportReq;
    public ArrayList<CdmaSmsCbProgramData> serviceCategoryProgramData;
    public ArrayList<CdmaSmsCbProgramResults> serviceCategoryProgramResults;
    public boolean userAckReq;
    public UserData userData;
    public int userResponseCode;
    public boolean userResponseCodeSet = false;
    public TimeStamp validityPeriodAbsolute;
    public int validityPeriodRelative;
    public boolean validityPeriodRelativeSet;

    public static class CodingException extends Exception {
        public CodingException(String s) {
            super(s);
        }
    }

    private static class Gsm7bitCodingResult {
        byte[] data;
        int septets;

        private Gsm7bitCodingResult() {
        }
    }

    public static class TimeStamp extends Time {
        public TimeStamp() {
            super(TimeZone.getDefault().getID());
        }

        public static TimeStamp fromByteArray(byte[] data) {
            TimeStamp ts = new TimeStamp();
            int year = IccUtils.cdmaBcdByteToInt(data[0]);
            if (year > 99 || year < 0) {
                return null;
            }
            ts.year = year >= 96 ? year + 1900 : year + 2000;
            int month = IccUtils.cdmaBcdByteToInt(data[1]);
            if (month < 1 || month > 12) {
                return null;
            }
            ts.month = month - 1;
            int day = IccUtils.cdmaBcdByteToInt(data[2]);
            if (day < 1 || day > 31) {
                return null;
            }
            ts.monthDay = day;
            int hour = IccUtils.cdmaBcdByteToInt(data[3]);
            if (hour < 0 || hour > 23) {
                return null;
            }
            ts.hour = hour;
            int minute = IccUtils.cdmaBcdByteToInt(data[4]);
            if (minute < 0 || minute > 59) {
                return null;
            }
            ts.minute = minute;
            int second = IccUtils.cdmaBcdByteToInt(data[5]);
            if (second < 0 || second > 59) {
                return null;
            }
            ts.second = second;
            return ts;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TimeStamp ");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ year=");
            stringBuilder.append(this.year);
            builder.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", month=");
            stringBuilder.append(this.month);
            builder.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", day=");
            stringBuilder.append(this.monthDay);
            builder.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", hour=");
            stringBuilder.append(this.hour);
            builder.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", minute=");
            stringBuilder.append(this.minute);
            builder.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", second=");
            stringBuilder.append(this.second);
            builder.append(stringBuilder.toString());
            builder.append(" }");
            return builder.toString();
        }
    }

    public String getLanguage() {
        return getLanguageCodeForValue(this.language);
    }

    private static String getLanguageCodeForValue(int languageValue) {
        switch (languageValue) {
            case 1:
                return "en";
            case 2:
                return "fr";
            case 3:
                return "es";
            case 4:
                return "ja";
            case 5:
                return "ko";
            case 6:
                return "zh";
            case 7:
                return "he";
            default:
                return null;
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BearerData ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ messageType=");
        stringBuilder.append(this.messageType);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", messageId=");
        stringBuilder.append(this.messageId);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", priority=");
        stringBuilder.append(this.priorityIndicatorSet ? Integer.valueOf(this.priority) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", privacy=");
        stringBuilder.append(this.privacyIndicatorSet ? Integer.valueOf(this.privacy) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", alert=");
        stringBuilder.append(this.alertIndicatorSet ? Integer.valueOf(this.alert) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", displayMode=");
        stringBuilder.append(this.displayModeSet ? Integer.valueOf(this.displayMode) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", language=");
        stringBuilder.append(this.languageIndicatorSet ? Integer.valueOf(this.language) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", errorClass=");
        stringBuilder.append(this.messageStatusSet ? Integer.valueOf(this.errorClass) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", msgStatus=");
        stringBuilder.append(this.messageStatusSet ? Integer.valueOf(this.messageStatus) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", msgCenterTimeStamp=");
        stringBuilder.append(this.msgCenterTimeStamp != null ? this.msgCenterTimeStamp : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", validityPeriodAbsolute=");
        stringBuilder.append(this.validityPeriodAbsolute != null ? this.validityPeriodAbsolute : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", validityPeriodRelative=");
        stringBuilder.append(this.validityPeriodRelativeSet ? Integer.valueOf(this.validityPeriodRelative) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", deferredDeliveryTimeAbsolute=");
        stringBuilder.append(this.deferredDeliveryTimeAbsolute != null ? this.deferredDeliveryTimeAbsolute : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", deferredDeliveryTimeRelative=");
        stringBuilder.append(this.deferredDeliveryTimeRelativeSet ? Integer.valueOf(this.deferredDeliveryTimeRelative) : "unset");
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", userAckReq=");
        stringBuilder.append(this.userAckReq);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", deliveryAckReq=");
        stringBuilder.append(this.deliveryAckReq);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", readAckReq=");
        stringBuilder.append(this.readAckReq);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", reportReq=");
        stringBuilder.append(this.reportReq);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", numberOfMessages=");
        stringBuilder.append(this.numberOfMessages);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", callbackNumber=");
        stringBuilder.append(Rlog.pii(LOG_TAG, this.callbackNumber));
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", depositIndex=");
        stringBuilder.append(this.depositIndex);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", hasUserDataHeader=");
        stringBuilder.append(this.hasUserDataHeader);
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", userData=");
        stringBuilder.append(this.userData);
        builder.append(stringBuilder.toString());
        builder.append(" }");
        return builder.toString();
    }

    private static void encodeMessageId(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 3);
        outStream.write(4, bData.messageType);
        outStream.write(8, bData.messageId >> 8);
        outStream.write(8, bData.messageId);
        outStream.write(1, bData.hasUserDataHeader);
        outStream.skip(3);
    }

    private static int countAsciiSeptets(CharSequence msg, boolean force) {
        int msgLen = msg.length();
        if (force) {
            return msgLen;
        }
        for (int i = 0; i < msgLen; i++) {
            if (UserData.charToAscii.get(msg.charAt(i), -1) == -1) {
                return -1;
            }
        }
        return msgLen;
    }

    public static TextEncodingDetails calcTextEncodingDetails(CharSequence msg, boolean force7BitEncoding, boolean isEntireMsg) {
        TextEncodingDetails ted;
        int septets = countAsciiSeptets(msg, force7BitEncoding);
        if (septets == -1 || septets > 160) {
            ted = SmsMessage.calculateLength(msg, force7BitEncoding);
            if (ted.msgCount == 1 && ted.codeUnitSize == 1 && isEntireMsg) {
                return SmsMessageBase.calcUnicodeEncodingDetails(msg);
            }
        }
        ted = new TextEncodingDetails();
        ted.msgCount = 1;
        ted.codeUnitCount = septets;
        ted.codeUnitsRemaining = 160 - septets;
        ted.codeUnitSize = 1;
        return ted;
    }

    public static byte[] encode7bitAscii(String msg, int septetOffset, boolean force) throws CodingException {
        try {
            int i;
            BitwiseOutputStream outStream = new BitwiseOutputStream(msg.length() + septetOffset);
            int msgLen = msg.length();
            int i2 = 0;
            for (i = 0; i < septetOffset; i++) {
                outStream.write(7, 0);
            }
            while (i2 < msgLen) {
                i = UserData.charToAscii.get(msg.charAt(i2), -1);
                if (i != -1) {
                    outStream.write(7, i);
                } else if (force) {
                    outStream.write(7, 32);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cannot ASCII encode (");
                    stringBuilder.append(msg.charAt(i2));
                    stringBuilder.append(")");
                    throw new CodingException(stringBuilder.toString());
                }
                i2++;
            }
            return outStream.toByteArray();
        } catch (AccessException ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("7bit ASCII encode failed: ");
            stringBuilder2.append(ex);
            throw new CodingException(stringBuilder2.toString());
        }
    }

    private static byte[] encodeUtf16(String msg) throws CodingException {
        try {
            return msg.getBytes("utf-16be");
        } catch (UnsupportedEncodingException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UTF-16 encode failed: ");
            stringBuilder.append(ex);
            throw new CodingException(stringBuilder.toString());
        }
    }

    private static Gsm7bitCodingResult encode7bitGsm(String msg, int septetOffset, boolean force) throws CodingException {
        try {
            byte[] fullData = GsmAlphabet.stringToGsm7BitPacked(msg, septetOffset, force ^ 1, 0, 0);
            Gsm7bitCodingResult result = new Gsm7bitCodingResult();
            result.data = new byte[(fullData.length - 1)];
            System.arraycopy(fullData, 1, result.data, 0, fullData.length - 1);
            result.septets = fullData[0] & 255;
            return result;
        } catch (EncodeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("7bit GSM encode failed: ");
            stringBuilder.append(ex);
            throw new CodingException(stringBuilder.toString());
        }
    }

    private static void encode7bitEms(UserData uData, byte[] udhData, boolean force) throws CodingException {
        Gsm7bitCodingResult gcr = encode7bitGsm(uData.payloadStr, (((udhData.length + 1) * 8) + 6) / 7, force);
        uData.msgEncoding = 9;
        uData.msgEncodingSet = true;
        uData.numFields = gcr.septets;
        uData.payload = gcr.data;
        uData.payload[0] = (byte) udhData.length;
        System.arraycopy(udhData, 0, uData.payload, 1, udhData.length);
    }

    private static void encode16bitEms(UserData uData, byte[] udhData) throws CodingException {
        byte[] payload = encodeUtf16(uData.payloadStr);
        int udhBytes = udhData.length + 1;
        int udhCodeUnits = (udhBytes + 1) / 2;
        int payloadCodeUnits = payload.length / 2;
        uData.msgEncoding = 4;
        uData.msgEncodingSet = true;
        uData.numFields = udhCodeUnits + payloadCodeUnits;
        uData.payload = new byte[(uData.numFields * 2)];
        uData.payload[0] = (byte) udhData.length;
        System.arraycopy(udhData, 0, uData.payload, 1, udhData.length);
        System.arraycopy(payload, 0, uData.payload, udhBytes, payload.length);
    }

    private static void encodeEmsUserDataPayload(UserData uData) throws CodingException {
        byte[] headerData = SmsHeader.toByteArray(uData.userDataHeader);
        if (headerData == null) {
            Rlog.e(LOG_TAG, "user data with null headerData");
            return;
        }
        if (!uData.msgEncodingSet) {
            try {
                if (!HwFrameworkFactory.getHwBaseInnerSmsManager().encode7bitMultiSms(uData, headerData, false)) {
                    throw new CodingException("7bit multi sms encoding failed");
                }
            } catch (CodingException e) {
                encode16bitEms(uData, headerData);
            }
        } else if (uData.msgEncoding == 9) {
            encode7bitEms(uData, headerData, true);
        } else if (uData.msgEncoding == 2) {
            HwFrameworkFactory.getHwBaseInnerSmsManager().encode7bitMultiSms(uData, headerData, true);
        } else if (uData.msgEncoding == 4) {
            encode16bitEms(uData, headerData);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unsupported EMS user data encoding (");
            stringBuilder.append(uData.msgEncoding);
            stringBuilder.append(")");
            throw new CodingException(stringBuilder.toString());
        }
    }

    private static byte[] encodeShiftJis(String msg) throws CodingException {
        try {
            return msg.getBytes("Shift_JIS");
        } catch (UnsupportedEncodingException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Shift-JIS encode failed: ");
            stringBuilder.append(ex);
            throw new CodingException(stringBuilder.toString());
        }
    }

    private static void encodeUserDataPayload(UserData uData) throws CodingException {
        if (uData.payloadStr == null && uData.msgEncoding != 0) {
            Rlog.e(LOG_TAG, "user data with null payloadStr");
            uData.payloadStr = "";
        }
        if (uData.userDataHeader != null) {
            encodeEmsUserDataPayload(uData);
            return;
        }
        if (!uData.msgEncodingSet) {
            try {
                uData.payload = encode7bitAscii(uData.payloadStr, 0, false);
                uData.msgEncoding = 2;
            } catch (CodingException e) {
                uData.payload = encodeUtf16(uData.payloadStr);
                uData.msgEncoding = 4;
            }
            uData.numFields = uData.payloadStr.length();
            uData.msgEncodingSet = true;
        } else if (uData.msgEncoding != 0) {
            if (uData.payloadStr == null) {
                Rlog.e(LOG_TAG, "non-octet user data with null payloadStr");
                uData.payloadStr = "";
            }
            if (uData.msgEncoding == 9) {
                Gsm7bitCodingResult gcr = encode7bitGsm(uData.payloadStr, 0, true);
                uData.payload = gcr.data;
                uData.numFields = gcr.septets;
            } else if (uData.msgEncoding == 2) {
                uData.payload = encode7bitAscii(uData.payloadStr, 0, true);
                uData.numFields = uData.payloadStr.length();
            } else if (uData.msgEncoding == 4) {
                uData.payload = encodeUtf16(uData.payloadStr);
                uData.numFields = uData.payloadStr.length();
            } else if (uData.msgEncoding == 5) {
                uData.payload = encodeShiftJis(uData.payloadStr);
                uData.numFields = uData.payload.length;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unsupported user data encoding (");
                stringBuilder.append(uData.msgEncoding);
                stringBuilder.append(")");
                throw new CodingException(stringBuilder.toString());
            }
        } else if (uData.payload == null) {
            Rlog.e(LOG_TAG, "user data with octet encoding but null payload");
            uData.payload = new byte[0];
            uData.numFields = 0;
        } else {
            uData.numFields = uData.payload.length;
        }
    }

    private static void encodeUserData(BearerData bData, BitwiseOutputStream outStream) throws AccessException, CodingException {
        encodeUserDataPayload(bData.userData);
        bData.hasUserDataHeader = bData.userData.userDataHeader != null;
        if (bData.userData.payload.length <= 140) {
            int dataBits = (bData.userData.payload.length * 8) - bData.userData.paddingBits;
            int paramBits = dataBits + 13;
            if (bData.userData.msgEncoding == 1 || bData.userData.msgEncoding == 10) {
                paramBits += 8;
            }
            int paramBytes = (paramBits / 8) + (paramBits % 8 > 0 ? 1 : 0);
            int paddingBits = (paramBytes * 8) - paramBits;
            outStream.write(8, paramBytes);
            outStream.write(5, bData.userData.msgEncoding);
            if (bData.userData.msgEncoding == 1 || bData.userData.msgEncoding == 10) {
                outStream.write(8, bData.userData.msgType);
            }
            outStream.write(8, bData.userData.numFields);
            outStream.writeByteArray(dataBits, bData.userData.payload);
            if (paddingBits > 0) {
                outStream.write(paddingBits, 0);
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("encoded user data too large (");
        stringBuilder.append(bData.userData.payload.length);
        stringBuilder.append(" > ");
        stringBuilder.append(140);
        stringBuilder.append(" bytes)");
        throw new CodingException(stringBuilder.toString());
    }

    private static void encodeReplyOption(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(1, bData.userAckReq);
        outStream.write(1, bData.deliveryAckReq);
        outStream.write(1, bData.readAckReq);
        outStream.write(1, bData.reportReq);
        outStream.write(4, 0);
    }

    private static byte[] encodeDtmfSmsAddress(String address) {
        int digits = address.length();
        int dataBits = digits * 4;
        int i = 0;
        byte[] rawData = new byte[((dataBits / 8) + (dataBits % 8 > 0 ? 1 : 0))];
        while (i < digits) {
            int val;
            char c = address.charAt(i);
            if (c >= '1' && c <= '9') {
                val = c - 48;
            } else if (c == '0') {
                val = 10;
            } else if (c == '*') {
                val = 11;
            } else if (c != '#') {
                return null;
            } else {
                val = 12;
            }
            int i2 = i / 2;
            rawData[i2] = (byte) (rawData[i2] | (val << (4 - ((i % 2) * 4))));
            i++;
        }
        return rawData;
    }

    private static void encodeCdmaSmsAddress(CdmaSmsAddress addr) throws CodingException {
        if (addr.digitMode == 1) {
            try {
                addr.origBytes = addr.address.getBytes("US-ASCII");
                return;
            } catch (UnsupportedEncodingException e) {
                throw new CodingException("invalid SMS address, cannot convert to ASCII");
            }
        }
        addr.origBytes = encodeDtmfSmsAddress(addr.address);
    }

    private static void encodeCallbackNumber(BearerData bData, BitwiseOutputStream outStream) throws AccessException, CodingException {
        int dataBits;
        CdmaSmsAddress addr = bData.callbackNumber;
        encodeCdmaSmsAddress(addr);
        int paramBits = 9;
        if (addr.digitMode == 1) {
            paramBits = 9 + 7;
            dataBits = addr.numberOfDigits * 8;
        } else {
            dataBits = addr.numberOfDigits * 4;
        }
        paramBits += dataBits;
        int paramBytes = (paramBits / 8) + (paramBits % 8 > 0 ? 1 : 0);
        int paddingBits = (paramBytes * 8) - paramBits;
        outStream.write(8, paramBytes);
        outStream.write(1, addr.digitMode);
        if (addr.digitMode == 1) {
            outStream.write(3, addr.ton);
            outStream.write(4, addr.numberPlan);
        }
        outStream.write(8, addr.numberOfDigits);
        outStream.writeByteArray(dataBits, addr.origBytes);
        if (paddingBits > 0) {
            outStream.write(paddingBits, 0);
        }
    }

    private static void encodeMsgStatus(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(2, bData.errorClass);
        outStream.write(6, bData.messageStatus);
    }

    private static void encodeMsgCount(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(8, bData.numberOfMessages);
    }

    private static void encodeValidityPeriodRel(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(8, bData.validityPeriodRelative);
    }

    private static void encodePrivacyIndicator(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(2, bData.privacy);
        outStream.skip(6);
    }

    private static void encodeLanguageIndicator(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(8, bData.language);
    }

    private static void encodeDisplayMode(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(2, bData.displayMode);
        outStream.skip(6);
    }

    private static void encodePriorityIndicator(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(2, bData.priority);
        outStream.skip(6);
    }

    private static void encodeMsgDeliveryAlert(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        outStream.write(8, 1);
        outStream.write(2, bData.alert);
        outStream.skip(6);
    }

    private static void encodeScpResults(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        ArrayList<CdmaSmsCbProgramResults> results = bData.serviceCategoryProgramResults;
        outStream.write(8, results.size() * 4);
        Iterator it = results.iterator();
        while (it.hasNext()) {
            CdmaSmsCbProgramResults result = (CdmaSmsCbProgramResults) it.next();
            int category = result.getCategory();
            outStream.write(8, category >> 8);
            outStream.write(8, category);
            outStream.write(8, result.getLanguage());
            outStream.write(4, result.getCategoryResult());
            outStream.skip(4);
        }
    }

    public static byte[] encode(BearerData bData) {
        String str;
        StringBuilder stringBuilder;
        boolean z = (bData.userData == null || bData.userData.userDataHeader == null) ? false : true;
        bData.hasUserDataHeader = z;
        try {
            BitwiseOutputStream outStream = new BitwiseOutputStream(200);
            outStream.write(8, 0);
            encodeMessageId(bData, outStream);
            if (bData.userData != null) {
                outStream.write(8, 1);
                encodeUserData(bData, outStream);
            }
            if (bData.callbackNumber != null) {
                outStream.write(8, 14);
                encodeCallbackNumber(bData, outStream);
            }
            if (bData.userAckReq || bData.deliveryAckReq || bData.readAckReq || bData.reportReq) {
                outStream.write(8, 10);
                encodeReplyOption(bData, outStream);
            }
            if (bData.numberOfMessages != 0) {
                outStream.write(8, 11);
                encodeMsgCount(bData, outStream);
            }
            if (bData.validityPeriodRelativeSet) {
                outStream.write(8, 5);
                encodeValidityPeriodRel(bData, outStream);
            }
            if (bData.privacyIndicatorSet) {
                outStream.write(8, 9);
                encodePrivacyIndicator(bData, outStream);
            }
            if (bData.languageIndicatorSet) {
                outStream.write(8, 13);
                encodeLanguageIndicator(bData, outStream);
            }
            if (bData.displayModeSet) {
                outStream.write(8, 15);
                encodeDisplayMode(bData, outStream);
            }
            if (bData.priorityIndicatorSet) {
                outStream.write(8, 8);
                encodePriorityIndicator(bData, outStream);
            }
            if (bData.alertIndicatorSet) {
                outStream.write(8, 12);
                encodeMsgDeliveryAlert(bData, outStream);
            }
            if (bData.messageStatusSet) {
                outStream.write(8, 20);
                encodeMsgStatus(bData, outStream);
            }
            if (bData.serviceCategoryProgramResults != null) {
                outStream.write(8, 19);
                encodeScpResults(bData, outStream);
            }
            HwFrameworkFactory.getHwBaseInnerSmsManager().encodeMsgCenterTimeStampCheck(bData, outStream);
            return outStream.toByteArray();
        } catch (AccessException ex) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BearerData encode failed: ");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return null;
        } catch (CodingException ex2) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BearerData encode failed: ");
            stringBuilder.append(ex2);
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static boolean decodeMessageId(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 24) {
            paramBits -= 24;
            decodeSuccess = true;
            bData.messageType = inStream.read(4);
            bData.messageId = inStream.read(8) << 8;
            bData.messageId = inStream.read(8) | bData.messageId;
            boolean z = true;
            if (inStream.read(1) != 1) {
                z = false;
            }
            bData.hasUserDataHeader = z;
            inStream.skip(3);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MESSAGE_IDENTIFIER decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeReserved(BearerData bData, BitwiseInputStream inStream, int subparamId) throws BitwiseInputStream.AccessException, CodingException {
        boolean decodeSuccess = false;
        int subparamLen = inStream.read(8);
        int paramBits = subparamLen * 8;
        if (paramBits <= inStream.available()) {
            decodeSuccess = true;
            inStream.skip(paramBits);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RESERVED bearer data subparameter ");
        stringBuilder.append(subparamId);
        stringBuilder.append(" decode ");
        stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
        stringBuilder.append(" (param bits = ");
        stringBuilder.append(paramBits);
        stringBuilder.append(")");
        Rlog.d(str, stringBuilder.toString());
        if (decodeSuccess) {
            return decodeSuccess;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("RESERVED bearer data subparameter ");
        stringBuilder.append(subparamId);
        stringBuilder.append(" had invalid SUBPARAM_LEN ");
        stringBuilder.append(subparamLen);
        throw new CodingException(stringBuilder.toString());
    }

    private static boolean decodeUserData(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        int paramBits = inStream.read(8) * 8;
        bData.userData = new UserData();
        bData.userData.msgEncoding = inStream.read(5);
        bData.userData.msgEncodingSet = true;
        bData.userData.msgType = 0;
        int consumedBits = 5;
        if (bData.userData.msgEncoding == 1 || bData.userData.msgEncoding == 10) {
            bData.userData.msgType = inStream.read(8);
            consumedBits = 5 + 8;
        }
        bData.userData.numFields = inStream.read(8);
        int dataBits = paramBits - (consumedBits + 8);
        bData.userData.payload = inStream.readByteArray(dataBits);
        return true;
    }

    private static String decodeUtf8(byte[] data, int offset, int numFields) throws CodingException {
        return decodeCharset(data, offset, numFields, 1, "UTF-8");
    }

    private static String decodeUtf16(byte[] data, int offset, int numFields) throws CodingException {
        return decodeCharset(data, offset, numFields - ((offset + (offset % 2)) / 2), 2, "utf-16be");
    }

    private static String decodeCharset(byte[] data, int offset, int numFields, int width, String charset) throws CodingException {
        if (numFields < 0 || (numFields * width) + offset > data.length) {
            int maxNumFields = ((data.length - offset) - (offset % width)) / width;
            StringBuilder stringBuilder;
            if (maxNumFields >= 0) {
                String str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(charset);
                stringBuilder.append(" decode error: offset = ");
                stringBuilder.append(offset);
                stringBuilder.append(" numFields = ");
                stringBuilder.append(numFields);
                stringBuilder.append(" data.length = ");
                stringBuilder.append(data.length);
                stringBuilder.append(" maxNumFields = ");
                stringBuilder.append(maxNumFields);
                Rlog.e(str, stringBuilder.toString());
                numFields = maxNumFields;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(charset);
                stringBuilder.append(" decode failed: offset out of range");
                throw new CodingException(stringBuilder.toString());
            }
        }
        try {
            return new String(data, offset, numFields * width, charset);
        } catch (UnsupportedEncodingException padding) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(charset);
            stringBuilder2.append(" decode failed: ");
            stringBuilder2.append(padding);
            throw new CodingException(stringBuilder2.toString());
        }
    }

    private static String decode7bitAscii(byte[] data, int offset, int numFields) throws CodingException {
        int offsetBits = offset * 8;
        try {
            int offsetSeptets = (offsetBits + 6) / 7;
            numFields -= offsetSeptets;
            int paddingBits = (offsetSeptets * 7) - offsetBits;
            StringBuffer strBuf = new StringBuffer(numFields);
            BitwiseInputStream inStream = new BitwiseInputStream(data);
            int wantedBits = (offsetSeptets * 7) + (numFields * 7);
            if (inStream.available() >= wantedBits) {
                inStream.skip(offsetBits + paddingBits);
                for (int i = 0; i < numFields; i++) {
                    int charCode = inStream.read(7);
                    if (charCode >= 32 && charCode <= UserData.ASCII_MAP_MAX_INDEX) {
                        strBuf.append(UserData.ASCII_MAP[charCode - 32]);
                    } else if (charCode == 10) {
                        strBuf.append(10);
                    } else if (charCode == 13) {
                        strBuf.append(13);
                    } else {
                        strBuf.append(' ');
                    }
                }
                return strBuf.toString();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insufficient data (wanted ");
            stringBuilder.append(wantedBits);
            stringBuilder.append(" bits, but only have ");
            stringBuilder.append(inStream.available());
            stringBuilder.append(")");
            throw new CodingException(stringBuilder.toString());
        } catch (BitwiseInputStream.AccessException ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("7bit ASCII decode failed: ");
            stringBuilder2.append(ex);
            throw new CodingException(stringBuilder2.toString());
        }
    }

    private static String decode7bitGsm(byte[] data, int offset, int numFields) throws CodingException {
        int offsetBits = offset * 8;
        int offsetSeptets = (offsetBits + 6) / 7;
        String result = GsmAlphabet.gsm7BitPackedToString(data, offset, numFields - offsetSeptets, (offsetSeptets * 7) - offsetBits, 0, 0);
        if (result != null) {
            return result;
        }
        throw new CodingException("7bit GSM decoding failed");
    }

    private static String decodeLatin(byte[] data, int offset, int numFields) throws CodingException {
        return decodeCharset(data, offset, numFields, 1, "ISO-8859-1");
    }

    private static String decodeShiftJis(byte[] data, int offset, int numFields) throws CodingException {
        return decodeCharset(data, offset, numFields, 1, "Shift_JIS");
    }

    private static String decodeGsmDcs(byte[] data, int offset, int numFields, int msgType) throws CodingException {
        StringBuilder stringBuilder;
        if ((msgType & 192) == 0) {
            switch ((msgType >> 2) & 3) {
                case 0:
                    return decode7bitGsm(data, offset, numFields);
                case 1:
                    return decodeUtf8(data, offset, numFields);
                case 2:
                    return decodeUtf16(data, offset, numFields);
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unsupported user msgType encoding (");
                    stringBuilder.append(msgType);
                    stringBuilder.append(")");
                    throw new CodingException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unsupported coding group (");
        stringBuilder.append(msgType);
        stringBuilder.append(")");
        throw new CodingException(stringBuilder.toString());
    }

    private static void decodeUserDataPayload(UserData userData, boolean hasUserDataHeader) throws CodingException {
        byte[] headerData;
        int offset = 0;
        if (hasUserDataHeader) {
            int udhLen = userData.payload[0] & 255;
            offset = 0 + (udhLen + 1);
            headerData = new byte[udhLen];
            System.arraycopy(userData.payload, 1, headerData, 0, udhLen);
            userData.userDataHeader = SmsHeader.fromByteArray(headerData);
        }
        switch (userData.msgEncoding) {
            case 0:
                boolean decodingtypeUTF8 = Resources.getSystem().getBoolean(17957029);
                headerData = new byte[userData.numFields];
                System.arraycopy(userData.payload, 0, headerData, 0, userData.numFields < userData.payload.length ? userData.numFields : userData.payload.length);
                userData.payload = headerData;
                if (decodingtypeUTF8) {
                    userData.payloadStr = decodeUtf8(userData.payload, offset, userData.numFields);
                    return;
                } else {
                    userData.payloadStr = decodeLatin(userData.payload, offset, userData.numFields);
                    return;
                }
            case 2:
            case 3:
                userData.payloadStr = decode7bitAscii(userData.payload, offset, userData.numFields);
                return;
            case 4:
                userData.payloadStr = decodeUtf16(userData.payload, offset, userData.numFields);
                return;
            case 5:
                userData.payloadStr = decodeShiftJis(userData.payload, offset, userData.numFields);
                return;
            case 8:
                userData.payloadStr = decodeLatin(userData.payload, offset, userData.numFields);
                return;
            case 9:
                userData.payloadStr = decode7bitGsm(userData.payload, offset, userData.numFields);
                return;
            case 10:
                userData.payloadStr = decodeGsmDcs(userData.payload, offset, userData.numFields, userData.msgType);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unsupported user data encoding (");
                stringBuilder.append(userData.msgEncoding);
                stringBuilder.append(")");
                throw new CodingException(stringBuilder.toString());
        }
    }

    private static void decodeIs91VoicemailStatus(BearerData bData) throws BitwiseInputStream.AccessException, CodingException {
        StringBuilder stringBuilder;
        BitwiseInputStream inStream = new BitwiseInputStream(bData.userData.payload);
        int dataLen = inStream.available() / 6;
        int numFields = bData.userData.numFields;
        if (dataLen > 14 || dataLen < 3 || dataLen < numFields) {
            throw new CodingException("IS-91 voicemail status decoding failed");
        }
        try {
            StringBuffer strbuf = new StringBuffer(dataLen);
            while (inStream.available() >= 6) {
                strbuf.append(UserData.ASCII_MAP[inStream.read(6)]);
            }
            String data = strbuf.toString();
            bData.numberOfMessages = Integer.parseInt(data.substring(0, 2));
            char prioCode = data.charAt(2);
            if (prioCode == ' ') {
                bData.priority = 0;
            } else if (prioCode == '!') {
                bData.priority = 2;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IS-91 voicemail status decoding failed: illegal priority setting (");
                stringBuilder2.append(prioCode);
                stringBuilder2.append(")");
                throw new CodingException(stringBuilder2.toString());
            }
            bData.priorityIndicatorSet = true;
            bData.userData.payloadStr = data.substring(3, numFields - 3);
        } catch (NumberFormatException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("IS-91 voicemail status decoding failed: ");
            stringBuilder.append(ex);
            throw new CodingException(stringBuilder.toString());
        } catch (IndexOutOfBoundsException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("IS-91 voicemail status decoding failed: ");
            stringBuilder.append(ex2);
            throw new CodingException(stringBuilder.toString());
        }
    }

    private static void decodeIs91ShortMessage(BearerData bData) throws BitwiseInputStream.AccessException, CodingException {
        BitwiseInputStream inStream = new BitwiseInputStream(bData.userData.payload);
        int dataLen = inStream.available() / 6;
        int numFields = bData.userData.numFields;
        if (numFields > 14 || dataLen < numFields) {
            throw new CodingException("IS-91 short message decoding failed");
        }
        StringBuffer strbuf = new StringBuffer(dataLen);
        for (int i = 0; i < numFields; i++) {
            strbuf.append(UserData.ASCII_MAP[inStream.read(6)]);
        }
        bData.userData.payloadStr = strbuf.toString();
    }

    private static void decodeIs91Cli(BearerData bData) throws CodingException {
        int dataLen = new BitwiseInputStream(bData.userData.payload).available() / 4;
        int numFields = bData.userData.numFields;
        if (dataLen > 14 || dataLen < 3 || dataLen < numFields) {
            throw new CodingException("IS-91 voicemail status decoding failed");
        }
        CdmaSmsAddress addr = new CdmaSmsAddress();
        addr.digitMode = 0;
        addr.origBytes = bData.userData.payload;
        addr.numberOfDigits = (byte) numFields;
        decodeSmsAddress(addr);
        bData.callbackNumber = addr;
    }

    private static void decodeIs91(BearerData bData) throws BitwiseInputStream.AccessException, CodingException {
        switch (bData.userData.msgType) {
            case 130:
                decodeIs91VoicemailStatus(bData);
                return;
            case 131:
            case 133:
                decodeIs91ShortMessage(bData);
                return;
            case 132:
                decodeIs91Cli(bData);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unsupported IS-91 message type (");
                stringBuilder.append(bData.userData.msgType);
                stringBuilder.append(")");
                throw new CodingException(stringBuilder.toString());
        }
    }

    private static boolean decodeReplyOption(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            boolean z = true;
            bData.userAckReq = inStream.read(1) == 1;
            bData.deliveryAckReq = inStream.read(1) == 1;
            bData.readAckReq = inStream.read(1) == 1;
            if (inStream.read(1) != 1) {
                z = false;
            }
            bData.reportReq = z;
            inStream.skip(4);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("REPLY_OPTION decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeMsgCount(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.numberOfMessages = IccUtils.cdmaBcdByteToInt((byte) inStream.read(8));
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NUMBER_OF_MESSAGES decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeDepositIndex(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 16) {
            paramBits -= 16;
            decodeSuccess = true;
            bData.depositIndex = inStream.read(8) | (inStream.read(8) << 8);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MESSAGE_DEPOSIT_INDEX decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static String decodeDtmfSmsAddress(byte[] rawData, int numFields) throws CodingException {
        StringBuffer strBuf = new StringBuffer(numFields);
        for (int i = 0; i < numFields; i++) {
            int val = 15 & (rawData[i / 2] >>> (4 - ((i % 2) * 4)));
            if (val >= 1 && val <= 9) {
                strBuf.append(Integer.toString(val, 10));
            } else if (val == 10) {
                strBuf.append('0');
            } else if (val == 11) {
                strBuf.append('*');
            } else if (val == 12) {
                strBuf.append('#');
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid SMS address DTMF code (");
                stringBuilder.append(val);
                stringBuilder.append(")");
                throw new CodingException(stringBuilder.toString());
            }
        }
        return strBuf.toString();
    }

    private static void decodeSmsAddress(CdmaSmsAddress addr) throws CodingException {
        if (addr.digitMode == 1) {
            try {
                addr.address = new String(addr.origBytes, 0, addr.origBytes.length, "US-ASCII");
                return;
            } catch (UnsupportedEncodingException e) {
                throw new CodingException("invalid SMS address ASCII code");
            }
        }
        addr.address = decodeDtmfSmsAddress(addr.origBytes, addr.numberOfDigits);
    }

    private static boolean decodeCallbackNumber(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException, CodingException {
        int paramBits = inStream.read(8) * 8;
        if (paramBits < 8) {
            inStream.skip(paramBits);
            return false;
        }
        CdmaSmsAddress addr = new CdmaSmsAddress();
        addr.digitMode = inStream.read(1);
        byte fieldBits = SUBPARAM_VALIDITY_PERIOD_ABSOLUTE;
        byte consumedBits = SUBPARAM_USER_DATA;
        if (addr.digitMode == 1) {
            addr.ton = inStream.read(3);
            addr.numberPlan = inStream.read(4);
            fieldBits = SUBPARAM_PRIORITY_INDICATOR;
            consumedBits = (byte) (1 + 7);
        }
        addr.numberOfDigits = inStream.read(8);
        int remainingBits = paramBits - ((byte) (consumedBits + 8));
        int dataBits = addr.numberOfDigits * fieldBits;
        int paddingBits = remainingBits - dataBits;
        if (remainingBits >= dataBits) {
            addr.origBytes = inStream.readByteArray(dataBits);
            inStream.skip(paddingBits);
            decodeSmsAddress(addr);
            bData.callbackNumber = addr;
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CALLBACK_NUMBER subparam encoding size error (remainingBits + ");
        stringBuilder.append(remainingBits);
        stringBuilder.append(", dataBits + ");
        stringBuilder.append(dataBits);
        stringBuilder.append(", paddingBits + ");
        stringBuilder.append(paddingBits);
        stringBuilder.append(")");
        throw new CodingException(stringBuilder.toString());
    }

    private static boolean decodeMsgStatus(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.errorClass = inStream.read(2);
            bData.messageStatus = inStream.read(6);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MESSAGE_STATUS decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.messageStatusSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeMsgCenterTimeStamp(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 48) {
            paramBits -= 48;
            decodeSuccess = true;
            bData.msgCenterTimeStamp = TimeStamp.fromByteArray(inStream.readByteArray(48));
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MESSAGE_CENTER_TIME_STAMP decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeValidityAbs(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 48) {
            paramBits -= 48;
            decodeSuccess = true;
            bData.validityPeriodAbsolute = TimeStamp.fromByteArray(inStream.readByteArray(48));
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VALIDITY_PERIOD_ABSOLUTE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeDeferredDeliveryAbs(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 48) {
            paramBits -= 48;
            decodeSuccess = true;
            bData.deferredDeliveryTimeAbsolute = TimeStamp.fromByteArray(inStream.readByteArray(48));
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DEFERRED_DELIVERY_TIME_ABSOLUTE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        return decodeSuccess;
    }

    private static boolean decodeValidityRel(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.deferredDeliveryTimeRelative = inStream.read(8);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VALIDITY_PERIOD_RELATIVE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.deferredDeliveryTimeRelativeSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeDeferredDeliveryRel(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.validityPeriodRelative = inStream.read(8);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DEFERRED_DELIVERY_TIME_RELATIVE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.validityPeriodRelativeSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodePrivacyIndicator(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.privacy = inStream.read(2);
            inStream.skip(6);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PRIVACY_INDICATOR decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.privacyIndicatorSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeLanguageIndicator(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.language = inStream.read(8);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LANGUAGE_INDICATOR decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.languageIndicatorSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeDisplayMode(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.displayMode = inStream.read(2);
            inStream.skip(6);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DISPLAY_MODE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.displayModeSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodePriorityIndicator(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.priority = inStream.read(2);
            inStream.skip(6);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PRIORITY_INDICATOR decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.priorityIndicatorSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeMsgDeliveryAlert(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.alert = inStream.read(2);
            inStream.skip(6);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ALERT_ON_MESSAGE_DELIVERY decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.alertIndicatorSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeUserResponseCode(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException {
        boolean decodeSuccess = false;
        int paramBits = inStream.read(8) * 8;
        if (paramBits >= 8) {
            paramBits -= 8;
            decodeSuccess = true;
            bData.userResponseCode = inStream.read(8);
        }
        if (!decodeSuccess || paramBits > 0) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("USER_RESPONSE_CODE decode ");
            stringBuilder.append(decodeSuccess ? "succeeded" : "failed");
            stringBuilder.append(" (extra bits = ");
            stringBuilder.append(paramBits);
            stringBuilder.append(")");
            Rlog.d(str, stringBuilder.toString());
        }
        inStream.skip(paramBits);
        bData.userResponseCodeSet = decodeSuccess;
        return decodeSuccess;
    }

    private static boolean decodeServiceCategoryProgramData(BearerData bData, BitwiseInputStream inStream) throws BitwiseInputStream.AccessException, CodingException {
        BitwiseInputStream bitwiseInputStream = inStream;
        BearerData bearerData;
        if (inStream.available() >= 13) {
            int i = 8;
            int paramBits = bitwiseInputStream.read(8) * 8;
            int msgEncoding = bitwiseInputStream.read(5);
            paramBits -= 5;
            if (inStream.available() >= paramBits) {
                ArrayList<CdmaSmsCbProgramData> programDataList = new ArrayList();
                int paramBits2 = paramBits;
                paramBits = 0;
                while (paramBits2 >= 48) {
                    int operation = bitwiseInputStream.read(4);
                    int category = (bitwiseInputStream.read(i) << i) | bitwiseInputStream.read(i);
                    int language = bitwiseInputStream.read(i);
                    int maxMessages = bitwiseInputStream.read(i);
                    int alertOption = bitwiseInputStream.read(4);
                    int numFields = bitwiseInputStream.read(i);
                    paramBits2 -= 48;
                    int textBits = getBitsForNumFields(msgEncoding, numFields);
                    if (paramBits2 >= textBits) {
                        UserData userData = new UserData();
                        userData.msgEncoding = msgEncoding;
                        userData.msgEncodingSet = true;
                        userData.numFields = numFields;
                        userData.payload = bitwiseInputStream.readByteArray(textBits);
                        paramBits2 -= textBits;
                        decodeUserDataPayload(userData, false);
                        programDataList.add(new CdmaSmsCbProgramData(operation, category, language, maxMessages, alertOption, userData.payloadStr));
                        paramBits = 1;
                        i = 8;
                    } else {
                        i = textBits;
                        int i2 = numFields;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("category name is ");
                        stringBuilder.append(i);
                        stringBuilder.append(" bits in length, but there are only ");
                        stringBuilder.append(paramBits2);
                        stringBuilder.append(" bits available");
                        throw new CodingException(stringBuilder.toString());
                    }
                }
                if (paramBits == 0 || paramBits2 > 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SERVICE_CATEGORY_PROGRAM_DATA decode ");
                    stringBuilder2.append(paramBits != 0 ? "succeeded" : "failed");
                    stringBuilder2.append(" (extra bits = ");
                    stringBuilder2.append(paramBits2);
                    stringBuilder2.append(')');
                    Rlog.d(str, stringBuilder2.toString());
                }
                bitwiseInputStream.skip(paramBits2);
                bData.serviceCategoryProgramData = programDataList;
                return paramBits;
            }
            bearerData = bData;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("SERVICE_CATEGORY_PROGRAM_DATA decode failed: only ");
            stringBuilder3.append(inStream.available());
            stringBuilder3.append(" bits available (");
            stringBuilder3.append(paramBits);
            stringBuilder3.append(" bits expected)");
            throw new CodingException(stringBuilder3.toString());
        }
        bearerData = bData;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("SERVICE_CATEGORY_PROGRAM_DATA decode failed: only ");
        stringBuilder4.append(inStream.available());
        stringBuilder4.append(" bits available");
        throw new CodingException(stringBuilder4.toString());
    }

    private static int serviceCategoryToCmasMessageClass(int serviceCategory) {
        switch (serviceCategory) {
            case 4096:
                return 0;
            case 4097:
                return 1;
            case 4098:
                return 2;
            case 4099:
                return 3;
            case 4100:
                return 4;
            default:
                return -1;
        }
    }

    private static int getBitsForNumFields(int msgEncoding, int numFields) throws CodingException {
        if (msgEncoding != 0) {
            switch (msgEncoding) {
                case 2:
                case 3:
                case 9:
                    return numFields * 7;
                case 4:
                    return numFields * 16;
                case 5:
                case 6:
                case 7:
                case 8:
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unsupported message encoding (");
                    stringBuilder.append(msgEncoding);
                    stringBuilder.append(')');
                    throw new CodingException(stringBuilder.toString());
            }
        }
        return numFields * 8;
    }

    /* JADX WARNING: Missing block: B:17:0x0098, code skipped:
            r9 = ((r5 * 8) - 5) / 7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void decodeCmasUserData(BearerData bData, int serviceCategory) throws BitwiseInputStream.AccessException, CodingException {
        BearerData bearerData = bData;
        BitwiseInputStream inStream = new BitwiseInputStream(bearerData.userData.payload);
        if (inStream.available() >= 8) {
            int protocolVersion = inStream.read(8);
            if (protocolVersion == 0) {
                int messageClass = serviceCategoryToCmasMessageClass(serviceCategory);
                int category = -1;
                int responseType = -1;
                int severity = -1;
                int urgency = -1;
                int certainty = -1;
                while (inStream.available() >= 16) {
                    int recordType = inStream.read(8);
                    int recordLen = inStream.read(8);
                    switch (recordType) {
                        case 0:
                            UserData alertUserData = new UserData();
                            alertUserData.msgEncoding = inStream.read(5);
                            alertUserData.msgEncodingSet = true;
                            alertUserData.msgType = 0;
                            int i = alertUserData.msgEncoding;
                            if (i != 0) {
                                switch (i) {
                                    case 2:
                                    case 3:
                                        break;
                                    case 4:
                                        i = (recordLen - 1) / 2;
                                        break;
                                    default:
                                        switch (i) {
                                            case 8:
                                                break;
                                            case 9:
                                                break;
                                            default:
                                                i = 0;
                                                break;
                                        }
                                }
                            }
                            i = recordLen - 1;
                            alertUserData.numFields = i;
                            alertUserData.payload = inStream.readByteArray((recordLen * 8) - 5);
                            decodeUserDataPayload(alertUserData, false);
                            bearerData.userData = alertUserData;
                            break;
                        case 1:
                            category = inStream.read(8);
                            responseType = inStream.read(8);
                            severity = inStream.read(4);
                            urgency = inStream.read(4);
                            int certainty2 = inStream.read(4);
                            inStream.skip((recordLen * 8) - 28);
                            certainty = certainty2;
                            break;
                        default:
                            String str = LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("skipping unsupported CMAS record type ");
                            stringBuilder.append(recordType);
                            Rlog.w(str, stringBuilder.toString());
                            inStream.skip(recordLen * 8);
                            break;
                    }
                }
                bearerData.cmasWarningInfo = new SmsCbCmasInfo(messageClass, category, responseType, severity, urgency, certainty);
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unsupported CMAE_protocol_version ");
            stringBuilder2.append(protocolVersion);
            throw new CodingException(stringBuilder2.toString());
        }
        throw new CodingException("emergency CB with no CMAE_protocol_version");
    }

    public static BearerData decode(byte[] smsData) {
        return decode(smsData, 0);
    }

    private static boolean isCmasAlertCategory(int category) {
        return category >= 4096 && category <= SmsEnvelope.SERVICE_CATEGORY_CMAS_LAST_RESERVED_VALUE;
    }

    public static BearerData decode(byte[] smsData, int serviceCategory) {
        String str;
        StringBuilder stringBuilder;
        try {
            BitwiseInputStream inStream = new BitwiseInputStream(smsData);
            BearerData bData = new BearerData();
            int foundSubparamMask = 0;
            while (inStream.available() > 0) {
                boolean decodeSuccess;
                int subparamId = inStream.read(8);
                int subparamIdBit = 1 << subparamId;
                if ((foundSubparamMask & subparamIdBit) != 0 && subparamId >= 0) {
                    if (subparamId <= 23) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("illegal duplicate subparameter (");
                        stringBuilder2.append(subparamId);
                        stringBuilder2.append(")");
                        throw new CodingException(stringBuilder2.toString());
                    }
                }
                switch (subparamId) {
                    case 0:
                        decodeSuccess = decodeMessageId(bData, inStream);
                        break;
                    case 1:
                        decodeSuccess = decodeUserData(bData, inStream);
                        break;
                    case 2:
                        decodeSuccess = decodeUserResponseCode(bData, inStream);
                        break;
                    case 3:
                        decodeSuccess = decodeMsgCenterTimeStamp(bData, inStream);
                        break;
                    case 4:
                        decodeSuccess = decodeValidityAbs(bData, inStream);
                        break;
                    case 5:
                        decodeSuccess = decodeValidityRel(bData, inStream);
                        break;
                    case 6:
                        decodeSuccess = decodeDeferredDeliveryAbs(bData, inStream);
                        break;
                    case 7:
                        decodeSuccess = decodeDeferredDeliveryRel(bData, inStream);
                        break;
                    case 8:
                        decodeSuccess = decodePriorityIndicator(bData, inStream);
                        break;
                    case 9:
                        decodeSuccess = decodePrivacyIndicator(bData, inStream);
                        break;
                    case 10:
                        decodeSuccess = decodeReplyOption(bData, inStream);
                        break;
                    case 11:
                        decodeSuccess = decodeMsgCount(bData, inStream);
                        break;
                    case 12:
                        decodeSuccess = decodeMsgDeliveryAlert(bData, inStream);
                        break;
                    case 13:
                        decodeSuccess = decodeLanguageIndicator(bData, inStream);
                        break;
                    case 14:
                        decodeSuccess = decodeCallbackNumber(bData, inStream);
                        break;
                    case 15:
                        decodeSuccess = decodeDisplayMode(bData, inStream);
                        break;
                    case 17:
                        decodeSuccess = decodeDepositIndex(bData, inStream);
                        break;
                    case 18:
                        decodeSuccess = decodeServiceCategoryProgramData(bData, inStream);
                        break;
                    case 20:
                        decodeSuccess = decodeMsgStatus(bData, inStream);
                        break;
                    default:
                        decodeSuccess = decodeReserved(bData, inStream, subparamId);
                        break;
                }
                if (decodeSuccess && subparamId >= 0 && subparamId <= 23) {
                    foundSubparamMask |= subparamIdBit;
                }
            }
            if ((foundSubparamMask & 1) != 0) {
                if (bData.userData != null) {
                    if (isCmasAlertCategory(serviceCategory)) {
                        decodeCmasUserData(bData, serviceCategory);
                    } else if (bData.userData.msgEncoding == 1) {
                        if (((foundSubparamMask ^ 1) ^ 2) != 0) {
                            String str2 = LOG_TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("IS-91 must occur without extra subparams (");
                            stringBuilder3.append(foundSubparamMask);
                            stringBuilder3.append(")");
                            Rlog.e(str2, stringBuilder3.toString());
                        }
                        decodeIs91(bData);
                    } else {
                        decodeUserDataPayload(bData.userData, bData.hasUserDataHeader);
                    }
                }
                return bData;
            }
            throw new CodingException("missing MESSAGE_IDENTIFIER subparam");
        } catch (BitwiseInputStream.AccessException ex) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BearerData decode failed: ");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return null;
        } catch (CodingException ex2) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BearerData decode failed: ");
            stringBuilder.append(ex2);
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
    }
}
