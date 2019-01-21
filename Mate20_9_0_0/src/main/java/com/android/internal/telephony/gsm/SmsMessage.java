package com.android.internal.telephony.gsm;

import android.common.HwFrameworkFactory;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsAddress;
import com.android.internal.telephony.SmsConstants.MessageClass;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsHeader.PortAddrs;
import com.android.internal.telephony.SmsHeader.SpecialSmsMsg;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Iterator;

public class SmsMessage extends SmsMessageBase {
    private static final int INVALID_VALIDITY_PERIOD = -1;
    static final String LOG_TAG = "SmsMessage";
    private static final int MAX_SMS_TPVP = 255;
    private static final int MIN_SMS_TPVP = 0;
    private static final int VALIDITY_PERIOD_FORMAT_ABSOLUTE = 3;
    private static final int VALIDITY_PERIOD_FORMAT_ENHANCED = 1;
    private static final int VALIDITY_PERIOD_FORMAT_NONE = 0;
    private static final int VALIDITY_PERIOD_FORMAT_RELATIVE = 2;
    private static final int VALIDITY_PERIOD_MAX = 635040;
    private static final int VALIDITY_PERIOD_MIN = 5;
    private static final boolean VDBG = false;
    private static boolean hasSmsVp;
    private static final boolean isAddTPVP = SystemProperties.getBoolean("ro.config.hw_SmsAddTP-VP", false);
    private static final boolean isAllowedCsFw = SystemProperties.getBoolean("ro.config.hw_bastet_csfw", false);
    private static int smsValidityPeriod = SystemProperties.getInt("ro.config.sms_vp", -1);
    private int mDataCodingScheme;
    private boolean mIsStatusReportMessage = false;
    private int mMti;
    private int mProtocolIdentifier;
    private GsmSmsAddress mRecipientAddress;
    private boolean mReplyPathPresent = false;
    private int mStatus;
    private int mVoiceMailCount = 0;
    private MessageClass messageClass;

    public static class PduParser {
        public int mCur = 0;
        public byte[] mPdu;
        byte[] mUserData;
        SmsHeader mUserDataHeader;
        int mUserDataSeptetPadding = 0;

        PduParser(byte[] pdu) {
            this.mPdu = pdu;
        }

        String getSCAddress() {
            String ret;
            int len = getByte();
            if (len == 0) {
                ret = null;
            } else {
                try {
                    ret = PhoneNumberUtils.calledPartyBCDToString(this.mPdu, this.mCur, len, 2);
                } catch (RuntimeException tr) {
                    Rlog.d(SmsMessage.LOG_TAG, "invalid SC address: ", tr);
                    ret = null;
                }
            }
            this.mCur += len;
            return ret;
        }

        public int getByte() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            return bArr[i] & 255;
        }

        public GsmSmsAddress getAddress() {
            int lengthBytes = 2 + (((this.mPdu[this.mCur] & 255) + 1) / 2);
            try {
                GsmSmsAddress ret = new GsmSmsAddress(this.mPdu, this.mCur, lengthBytes);
                this.mCur += lengthBytes;
                return ret;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        long getSCTimestampMillis() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            int year = IccUtils.gsmBcdByteToInt(bArr[i]);
            byte[] bArr2 = this.mPdu;
            int i2 = this.mCur;
            this.mCur = i2 + 1;
            i = IccUtils.gsmBcdByteToInt(bArr2[i2]);
            byte[] bArr3 = this.mPdu;
            int i3 = this.mCur;
            this.mCur = i3 + 1;
            i2 = IccUtils.gsmBcdByteToInt(bArr3[i3]);
            byte[] bArr4 = this.mPdu;
            int i4 = this.mCur;
            this.mCur = i4 + 1;
            i3 = IccUtils.gsmBcdByteToInt(bArr4[i4]);
            byte[] bArr5 = this.mPdu;
            int i5 = this.mCur;
            this.mCur = i5 + 1;
            i4 = IccUtils.gsmBcdByteToInt(bArr5[i5]);
            byte[] bArr6 = this.mPdu;
            int i6 = this.mCur;
            this.mCur = i6 + 1;
            i5 = IccUtils.gsmBcdByteToInt(bArr6[i6]);
            byte tzByte = this.mPdu;
            int i7 = this.mCur;
            this.mCur = i7 + 1;
            tzByte = tzByte[i7];
            i7 = IccUtils.gsmBcdByteToInt((byte) (tzByte & -9));
            i7 = (tzByte & 8) == 0 ? i7 : -i7;
            Time time = new Time("UTC");
            time.year = year >= 90 ? year + 1900 : year + 2000;
            time.month = i - 1;
            time.monthDay = i2;
            time.hour = i3;
            time.minute = i4;
            time.second = i5;
            return time.toMillis(true) - ((long) (((i7 * 15) * 60) * 1000));
        }

        int constructUserData(boolean hasUserDataHeader, boolean dataInSeptets) {
            int offset;
            int offset2;
            int offset3 = this.mCur;
            int offset4 = offset3 + 1;
            offset3 = this.mPdu[offset3] & 255;
            int headerSeptets = 0;
            int userDataHeaderLength = 0;
            int i = 0;
            if (hasUserDataHeader) {
                offset = offset4 + 1;
                userDataHeaderLength = this.mPdu[offset4] & 255;
                byte[] udh = new byte[userDataHeaderLength];
                System.arraycopy(this.mPdu, offset, udh, 0, userDataHeaderLength);
                this.mUserDataHeader = SmsHeader.fromByteArray(udh);
                offset2 = offset + userDataHeaderLength;
                offset = (userDataHeaderLength + 1) * 8;
                headerSeptets = (offset / 7) + (offset % 7 > 0 ? 1 : 0);
                this.mUserDataSeptetPadding = (headerSeptets * 7) - offset;
                offset4 = offset2;
            }
            if (dataInSeptets) {
                offset2 = this.mPdu.length - offset4;
            } else {
                offset2 = offset3 - (hasUserDataHeader ? userDataHeaderLength + 1 : 0);
                if (offset2 < 0) {
                    offset2 = 0;
                }
            }
            this.mUserData = new byte[offset2];
            System.arraycopy(this.mPdu, offset4, this.mUserData, 0, this.mUserData.length);
            this.mCur = offset4;
            if (!dataInSeptets) {
                return this.mUserData.length;
            }
            offset = offset3 - headerSeptets;
            if (offset >= 0) {
                i = offset;
            }
            return i;
        }

        byte[] getUserData() {
            return this.mUserData;
        }

        SmsHeader getUserDataHeader() {
            return this.mUserDataHeader;
        }

        String getUserDataGSM7Bit(int septetCount, int languageTable, int languageShiftTable) {
            String ret = GsmAlphabet.gsm7BitPackedToString(this.mPdu, this.mCur, septetCount, this.mUserDataSeptetPadding, languageTable, languageShiftTable);
            this.mCur += (septetCount * 7) / 8;
            return ret;
        }

        String getUserDataGSM8bit(int byteCount) {
            String ret = GsmAlphabet.gsm8BitUnpackedToString(this.mPdu, this.mCur, byteCount);
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataUCS2(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, "utf-16");
            } catch (UnsupportedEncodingException ex) {
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", ex);
                ret = "";
            }
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataKSC5601(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, "KSC5601");
            } catch (UnsupportedEncodingException ex) {
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", ex);
                ret = "";
            }
            this.mCur += byteCount;
            return ret;
        }

        boolean moreDataPresent() {
            return this.mPdu.length > this.mCur;
        }
    }

    public static class SubmitPdu extends SubmitPduBase {
    }

    static {
        boolean z = false;
        if (smsValidityPeriod >= 0 && smsValidityPeriod <= 255) {
            z = true;
        }
        hasSmsVp = z;
    }

    public static SmsMessage createFromPdu(byte[] pdu) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        } catch (OutOfMemoryError e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed with out of memory: ", e);
            return null;
        }
    }

    public boolean isTypeZero() {
        return this.mProtocolIdentifier == 64;
    }

    public static SmsMessage newFromCMT(byte[] pdu) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static SmsMessage newFromCDS(byte[] pdu) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "CDS SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static SmsMessage createFromEfRecord(int index, byte[] data) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.mIndexOnIcc = index;
            if ((data[0] & 1) == 0) {
                Rlog.w(LOG_TAG, "SMS parsing failed: Trying to parse a free record");
                return null;
            }
            msg.mStatusOnIcc = data[0] & 7;
            int size = data.length - 1;
            byte[] pdu = new byte[size];
            System.arraycopy(data, 1, pdu, 0, size);
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static int getTPLayerLengthForPDU(String pdu) {
        return ((pdu.length() / 2) - Integer.parseInt(pdu.substring(0, 2), 16)) - 1;
    }

    public static int getRelativeValidityPeriod(int validityPeriod) {
        int relValidityPeriod = -1;
        if (validityPeriod < 5 || validityPeriod > VALIDITY_PERIOD_MAX) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid Validity Period");
            stringBuilder.append(validityPeriod);
            Rlog.e(str, stringBuilder.toString());
            return -1;
        }
        if (validityPeriod <= MetricsEvent.ACTION_PERMISSION_DENIED_RECEIVE_WAP_PUSH) {
            relValidityPeriod = (validityPeriod / 5) - 1;
        } else if (validityPeriod <= MetricsEvent.ACTION_HUSH_GESTURE) {
            relValidityPeriod = ((validityPeriod - 720) / 30) + 143;
        } else if (validityPeriod <= 43200) {
            relValidityPeriod = (validityPeriod / MetricsEvent.ACTION_HUSH_GESTURE) + 166;
        } else if (validityPeriod <= VALIDITY_PERIOD_MAX) {
            relValidityPeriod = (validityPeriod / 10080) + 192;
        }
        return relValidityPeriod;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, header, 0, 0, 0);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header, int encoding, int languageTable, int languageShiftTable) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, header, encoding, languageTable, languageShiftTable, -1);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header, int encoding, int languageTable, int languageShiftTable, int validityPeriod) {
        String str = destinationAddress;
        String str2 = message;
        if (str2 == null || str == null) {
            String str3 = scAddress;
            boolean z = statusReportRequested;
            Rlog.e(LOG_TAG, "message or destinationAddress null");
            return null;
        }
        int languageTable2;
        int languageShiftTable2;
        byte[] header2;
        int encoding2;
        byte mtiByte;
        if (encoding == 0) {
            TextEncodingDetails ted = calculateLength(str2, false);
            int encoding3 = ted.codeUnitSize;
            languageTable2 = ted.languageTable;
            languageShiftTable2 = ted.languageShiftTable;
            SmsHeader smsHeader;
            if (encoding3 != 1 || (languageTable2 == 0 && languageShiftTable2 == 0)) {
                header2 = header;
            } else if (header != null) {
                smsHeader = SmsHeader.fromByteArray(header);
                if (smsHeader.languageTable == languageTable2 && smsHeader.languageShiftTable == languageShiftTable2) {
                    header2 = header;
                } else {
                    String str4 = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Updating language table in SMS header: ");
                    stringBuilder.append(smsHeader.languageTable);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(languageTable2);
                    stringBuilder.append(", ");
                    stringBuilder.append(smsHeader.languageShiftTable);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(languageShiftTable2);
                    Rlog.w(str4, stringBuilder.toString());
                    smsHeader.languageTable = languageTable2;
                    smsHeader.languageShiftTable = languageShiftTable2;
                    header2 = SmsHeader.toByteArray(smsHeader);
                }
            } else {
                smsHeader = new SmsHeader();
                smsHeader.languageTable = languageTable2;
                smsHeader.languageShiftTable = languageShiftTable2;
                header2 = SmsHeader.toByteArray(smsHeader);
            }
            encoding2 = encoding3;
        } else {
            header2 = header;
            encoding2 = encoding;
            languageTable2 = languageTable;
            languageShiftTable2 = languageShiftTable;
        }
        SubmitPdu ret = new SubmitPdu();
        int i = 64;
        if (isAddTPVP || hasSmsVp) {
            if (header2 == null) {
                i = 0;
            }
            mtiByte = (byte) ((1 | i) | 16);
        } else {
            if (header2 == null) {
                i = 0;
            }
            mtiByte = (byte) (1 | i);
        }
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, str, mtiByte, statusReportRequested, ret);
        if (bo == null) {
            return ret;
        }
        byte[] userData;
        String str5;
        if (encoding2 == 1) {
            try {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(str2, header2, languageTable2, languageShiftTable2);
            } catch (EncodeException uex) {
                Exception ex = uex;
                try {
                    userData = encodeUCS2(str2, header2);
                    encoding2 = 3;
                } catch (UnsupportedEncodingException uex2) {
                    UnsupportedEncodingException unsupportedEncodingException = uex2;
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex2);
                    return null;
                }
            }
        }
        try {
            userData = encodeUCS2(str2, header2);
        } catch (UnsupportedEncodingException uex22) {
            UnsupportedEncodingException unsupportedEncodingException2 = uex22;
            Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex22);
            return null;
        }
        StringBuilder stringBuilder2;
        if (encoding2 == 1) {
            if ((userData[0] & 255) > 160) {
                str5 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Message too long (");
                stringBuilder2.append(userData[0] & 255);
                stringBuilder2.append(" septets)");
                Rlog.e(str5, stringBuilder2.toString());
                return null;
            }
            bo.write(0);
        } else if ((userData[0] & 255) > 140) {
            str5 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Message too long (");
            stringBuilder2.append(userData[0] & 255);
            stringBuilder2.append(" bytes)");
            Rlog.e(str5, stringBuilder2.toString());
            return null;
        } else {
            bo.write(8);
        }
        if (hasSmsVp) {
            bo.write(smsValidityPeriod);
        } else if (isAddTPVP) {
            bo.write(255);
        }
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        str5 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("pdu size");
        stringBuilder3.append(ret.encodedMessage.length);
        Rlog.d(str5, stringBuilder3.toString());
        return ret;
    }

    public static byte[] encodeUCS2(String message, byte[] header) throws UnsupportedEncodingException {
        byte[] userData;
        byte[] textPart = message.getBytes("utf-16be");
        if (header != null) {
            userData = new byte[((header.length + textPart.length) + 1)];
            userData[0] = (byte) header.length;
            System.arraycopy(header, 0, userData, 1, header.length);
            System.arraycopy(textPart, 0, userData, header.length + 1, textPart.length);
        } else {
            userData = textPart;
        }
        byte[] ret = new byte[(userData.length + 1)];
        ret[0] = (byte) (userData.length & 255);
        System.arraycopy(userData, 0, ret, 1, userData.length);
        return ret;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, null);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, int validityPeriod) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, null, 0, 0, 0, validityPeriod);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, int destinationPort, byte[] data, boolean statusReportRequested) {
        PortAddrs portAddrs = new PortAddrs();
        portAddrs.destPort = destinationPort;
        portAddrs.origPort = 0;
        portAddrs.areEightBits = false;
        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;
        if ((destinationAddress.equals("1065840409") || destinationAddress.equals("10654040")) && destinationPort == 16998) {
            portAddrs.origPort = 16998;
        }
        byte[] smsHeaderData = SmsHeader.toByteArray(smsHeader);
        if ((data.length + smsHeaderData.length) + 1 > 140) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SMS data message may only contain ");
            stringBuilder.append((140 - smsHeaderData.length) - 1);
            stringBuilder.append(" bytes");
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
        SubmitPdu ret = new SubmitPdu();
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, (byte) 65, statusReportRequested, ret);
        if (bo == null) {
            return ret;
        }
        bo.write(4);
        bo.write((data.length + smsHeaderData.length) + 1);
        bo.write(smsHeaderData.length);
        bo.write(smsHeaderData, 0, smsHeaderData.length);
        bo.write(data, 0, data.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    private static ByteArrayOutputStream getSubmitPduHead(String scAddress, String destinationAddress, byte mtiByte, boolean statusReportRequested, SubmitPdu ret) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(180);
        if (scAddress == null) {
            Rlog.e(LOG_TAG, "scAddress is null");
            ret.encodedScAddress = null;
        } else {
            ret.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(scAddress);
        }
        if (statusReportRequested) {
            mtiByte = (byte) (mtiByte | 32);
        }
        bo.write(mtiByte);
        bo.write(0);
        byte[] daBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(destinationAddress);
        if (daBytes == null) {
            return null;
        }
        int i = 1;
        int length = (daBytes.length - 1) * 2;
        if ((daBytes[daBytes.length - 1] & MetricsEvent.FINGERPRINT_ENROLLING) != MetricsEvent.FINGERPRINT_ENROLLING) {
            i = 0;
        }
        bo.write(length - i);
        bo.write(daBytes, 0, daBytes.length);
        bo.write(0);
        return bo;
    }

    public static TextEncodingDetails calculateLength(CharSequence msgBody, boolean use7bitOnly) {
        CharSequence newMsgBody = null;
        if (Resources.getSystem().getBoolean(17957028)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(msgBody);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = msgBody;
        }
        TextEncodingDetails ted = GsmAlphabet.countGsmSeptets(newMsgBody, use7bitOnly);
        if (ted == null) {
            return SmsMessageBase.calcUnicodeEncodingDetails(newMsgBody);
        }
        return ted;
    }

    public int getProtocolIdentifier() {
        return this.mProtocolIdentifier;
    }

    int getDataCodingScheme() {
        return this.mDataCodingScheme;
    }

    public boolean isReplace() {
        return (this.mProtocolIdentifier & 192) == 64 && (this.mProtocolIdentifier & 63) > 0 && (this.mProtocolIdentifier & 63) < 8;
    }

    public boolean isCphsMwiMessage() {
        boolean z = false;
        if (this.mOriginatingAddress == null) {
            return false;
        }
        if (((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear() || ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet()) {
            z = true;
        }
        return z;
    }

    public boolean isMWIClearMessage() {
        boolean z = true;
        if (this.mIsMwi && !this.mMwiSense) {
            return true;
        }
        if (this.mOriginatingAddress == null || !((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear()) {
            z = false;
        }
        return z;
    }

    public boolean isMWISetMessage() {
        boolean z = true;
        if (this.mIsMwi && this.mMwiSense) {
            return true;
        }
        if (this.mOriginatingAddress == null || !((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet()) {
            z = false;
        }
        return z;
    }

    public boolean isMwiDontStore() {
        if (this.mIsMwi && this.mMwiDontStore) {
            return true;
        }
        if (isCphsMwiMessage() && " ".equals(getMessageBody())) {
            return true;
        }
        return false;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public boolean isStatusReportMessage() {
        return this.mIsStatusReportMessage;
    }

    public boolean isReplyPathPresent() {
        return this.mReplyPathPresent;
    }

    private void parsePdu(byte[] pdu) {
        this.mPdu = pdu;
        PduParser p = new PduParser(pdu);
        if (isAllowedCsFw) {
            if (255 == (p.mPdu[p.mCur] & 255)) {
                p.mCur++;
                this.blacklistFlag = true;
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parsePdu blacklistFlag: ");
                stringBuilder.append(this.blacklistFlag);
                stringBuilder.append("p.mCur: ");
                stringBuilder.append(p.mCur);
                Rlog.d(str, stringBuilder.toString());
            } else {
                this.blacklistFlag = false;
            }
        }
        this.mScAddress = p.getSCAddress();
        String str2 = this.mScAddress;
        int firstByte = p.getByte();
        this.mMti = firstByte & 3;
        if (!HwFrameworkFactory.getHwBaseInnerSmsManager().parseGsmSmsSubmit(this, this.mMti, p, firstByte)) {
            switch (this.mMti) {
                case 0:
                case 3:
                    parseSmsDeliver(p, firstByte);
                    break;
                case 1:
                    parseSmsSubmit(p, firstByte);
                    break;
                case 2:
                    parseSmsStatusReport(p, firstByte);
                    break;
                default:
                    throw new RuntimeException("Unsupported message type");
            }
        }
    }

    private void parseSmsStatusReport(PduParser p, int firstByte) {
        boolean hasUserDataHeader = true;
        this.mIsStatusReportMessage = true;
        this.mMessageRef = p.getByte();
        this.mRecipientAddress = p.getAddress();
        this.mScTimeMillis = p.getSCTimestampMillis();
        p.getSCTimestampMillis();
        this.mStatus = p.getByte();
        if (p.moreDataPresent()) {
            int extraParams = p.getByte();
            int moreExtraParams = extraParams;
            while ((moreExtraParams & 128) != 0) {
                moreExtraParams = p.getByte();
            }
            if ((extraParams & 120) == 0) {
                if ((extraParams & 1) != 0) {
                    this.mProtocolIdentifier = p.getByte();
                }
                if ((extraParams & 2) != 0) {
                    this.mDataCodingScheme = p.getByte();
                }
                if ((extraParams & 4) != 0) {
                    if ((firstByte & 64) != 64) {
                        hasUserDataHeader = false;
                    }
                    parseUserData(p, hasUserDataHeader);
                }
            }
        }
    }

    private void parseSmsDeliver(PduParser p, int firstByte) {
        boolean z = false;
        this.mReplyPathPresent = (firstByte & 128) == 128;
        this.mOriginatingAddress = p.getAddress();
        SmsAddress smsAddress = this.mOriginatingAddress;
        this.mProtocolIdentifier = p.getByte();
        this.mDataCodingScheme = p.getByte();
        this.mScTimeMillis = p.getSCTimestampMillis();
        if ((firstByte & 64) == 64) {
            z = true;
        }
        parseUserData(p, z);
    }

    private void parseSmsSubmit(PduParser p, int firstByte) {
        int validityPeriodLength;
        boolean z = false;
        this.mReplyPathPresent = (firstByte & 128) == 128;
        this.mMessageRef = p.getByte();
        this.mRecipientAddress = p.getAddress();
        GsmSmsAddress gsmSmsAddress = this.mRecipientAddress;
        this.mProtocolIdentifier = p.getByte();
        this.mDataCodingScheme = p.getByte();
        int validityPeriodFormat = (firstByte >> 3) & 3;
        if (validityPeriodFormat == 0) {
            validityPeriodLength = 0;
        } else if (2 == validityPeriodFormat) {
            validityPeriodLength = 1;
        } else {
            validityPeriodLength = 7;
        }
        while (true) {
            int validityPeriodLength2 = validityPeriodLength - 1;
            if (validityPeriodLength <= 0) {
                break;
            }
            p.getByte();
            validityPeriodLength = validityPeriodLength2;
        }
        if ((firstByte & 64) == 64) {
            z = true;
        }
        parseUserData(p, z);
    }

    /* JADX WARNING: Missing block: B:83:0x01e1, code skipped:
            if ((r0.mDataCodingScheme & com.android.internal.logging.nano.MetricsProto.MetricsEvent.FINGERPRINT_ENROLLING) == 224) goto L_0x01e6;
     */
    /* JADX WARNING: Missing block: B:86:0x01ea, code skipped:
            if ((r0.mDataCodingScheme & 3) != 0) goto L_0x01ec;
     */
    /* JADX WARNING: Missing block: B:87:0x01ec, code skipped:
            r0.mMwiDontStore = true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseUserData(PduParser p, boolean hasUserDataHeader) {
        PduParser pduParser = p;
        boolean z = hasUserDataHeader;
        boolean hasMessageClass = false;
        int encodingType = 0;
        int i = 128;
        int i2 = 0;
        String str;
        StringBuilder stringBuilder;
        if ((this.mDataCodingScheme & 128) == 0) {
            boolean userDataCompressed = (this.mDataCodingScheme & 32) != 0;
            hasMessageClass = (this.mDataCodingScheme & 16) != 0;
            if (!userDataCompressed) {
                switch ((this.mDataCodingScheme >> 2) & 3) {
                    case 0:
                        encodingType = 1;
                        break;
                    case 1:
                        if (Resources.getSystem().getBoolean(17957027)) {
                            encodingType = 2;
                            break;
                        }
                    case 2:
                        encodingType = 3;
                        break;
                    case 3:
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("1 - Unsupported SMS data coding scheme ");
                        stringBuilder.append(this.mDataCodingScheme & 255);
                        Rlog.w(str, stringBuilder.toString());
                        encodingType = 2;
                        break;
                }
            }
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("4 - Unsupported SMS data coding scheme (compression) ");
            stringBuilder.append(this.mDataCodingScheme & 255);
            Rlog.w(str, stringBuilder.toString());
        } else if ((this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == MetricsEvent.FINGERPRINT_ENROLLING) {
            hasMessageClass = true;
            encodingType = (this.mDataCodingScheme & 4) == 0 ? 1 : 2;
        } else if ((this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == 192 || (this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == 208 || (this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == 224) {
            if ((this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == 224) {
                encodingType = 3;
            } else {
                encodingType = 1;
            }
            boolean active = (this.mDataCodingScheme & 8) == 8;
            if ((this.mDataCodingScheme & 3) == 0) {
                this.mIsMwi = true;
                this.mMwiSense = active;
                this.mMwiDontStore = (this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) == 192;
                if (active) {
                    this.mVoiceMailCount = -1;
                } else {
                    this.mVoiceMailCount = 0;
                }
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("MWI in DCS for Vmail. DCS = ");
                stringBuilder2.append(this.mDataCodingScheme & 255);
                stringBuilder2.append(" Dont store = ");
                stringBuilder2.append(this.mMwiDontStore);
                stringBuilder2.append(" vmail count = ");
                stringBuilder2.append(this.mVoiceMailCount);
                Rlog.w(str2, stringBuilder2.toString());
            } else {
                this.mIsMwi = false;
                String str3 = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("MWI in DCS for fax/email/other: ");
                stringBuilder.append(this.mDataCodingScheme & 255);
                Rlog.w(str3, stringBuilder.toString());
            }
        } else if ((this.mDataCodingScheme & 192) != 128) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("3 - Unsupported SMS data coding scheme ");
            stringBuilder.append(this.mDataCodingScheme & 255);
            Rlog.w(str, stringBuilder.toString());
        } else if (this.mDataCodingScheme == 132) {
            encodingType = 4;
        } else {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("5 - Unsupported SMS data coding scheme ");
            stringBuilder.append(this.mDataCodingScheme & 255);
            Rlog.w(str, stringBuilder.toString());
        }
        int count = pduParser.constructUserData(z, encodingType == 1 ? 1 : 0);
        this.mUserData = p.getUserData();
        this.mUserDataHeader = p.getUserDataHeader();
        if (z && this.mUserDataHeader.specialSmsMsgList.size() != 0) {
            Iterator it = this.mUserDataHeader.specialSmsMsgList.iterator();
            while (it.hasNext()) {
                Object obj;
                SpecialSmsMsg msg = (SpecialSmsMsg) it.next();
                int msgInd = msg.msgIndType & 255;
                if (msgInd == 0 || msgInd == i) {
                    this.mIsMwi = true;
                    if (msgInd == i) {
                        this.mMwiDontStore = false;
                    } else if (!this.mMwiDontStore) {
                        obj = 208;
                        if ((this.mDataCodingScheme & MetricsEvent.FINGERPRINT_ENROLLING) != 208) {
                        }
                    }
                    obj = 208;
                    this.mVoiceMailCount = msg.msgCount & 255;
                    if (this.mVoiceMailCount > 0) {
                        this.mMwiSense = true;
                    } else {
                        this.mMwiSense = false;
                    }
                    String str4 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("MWI in TP-UDH for Vmail. Msg Ind = ");
                    stringBuilder3.append(msgInd);
                    stringBuilder3.append(" Dont store = ");
                    stringBuilder3.append(this.mMwiDontStore);
                    stringBuilder3.append(" Vmail count = ");
                    stringBuilder3.append(this.mVoiceMailCount);
                    Rlog.w(str4, stringBuilder3.toString());
                } else {
                    String str5 = LOG_TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("TP_UDH fax/email/extended msg/multisubscriber profile. Msg Ind = ");
                    stringBuilder4.append(msgInd);
                    Rlog.w(str5, stringBuilder4.toString());
                    obj = 208;
                }
                Object obj2 = obj;
                i = 128;
            }
        }
        switch (encodingType) {
            case 0:
                this.mMessageBody = null;
                break;
            case 1:
                if (z) {
                    i = this.mUserDataHeader.languageTable;
                } else {
                    i = 0;
                }
                if (z) {
                    i2 = this.mUserDataHeader.languageShiftTable;
                }
                this.mMessageBody = pduParser.getUserDataGSM7Bit(count, i, i2);
                break;
            case 2:
                if (!Resources.getSystem().getBoolean(17957027)) {
                    this.mMessageBody = null;
                    break;
                } else {
                    this.mMessageBody = pduParser.getUserDataGSM8bit(count);
                    break;
                }
            case 3:
                this.mMessageBody = pduParser.getUserDataUCS2(count);
                break;
            case 4:
                this.mMessageBody = pduParser.getUserDataKSC5601(count);
                break;
        }
        if (this.mMessageBody != null) {
            parseMessageBody();
        }
        if (hasMessageClass) {
            switch (this.mDataCodingScheme & 3) {
                case 0:
                    this.messageClass = MessageClass.CLASS_0;
                    return;
                case 1:
                    this.messageClass = MessageClass.CLASS_1;
                    return;
                case 2:
                    this.messageClass = MessageClass.CLASS_2;
                    return;
                case 3:
                    this.messageClass = MessageClass.CLASS_3;
                    return;
                default:
                    return;
            }
        }
        this.messageClass = MessageClass.UNKNOWN;
    }

    public MessageClass getMessageClass() {
        return this.messageClass;
    }

    boolean isUsimDataDownload() {
        return this.messageClass == MessageClass.CLASS_2 && (this.mProtocolIdentifier == 127 || this.mProtocolIdentifier == 124);
    }

    public int getNumOfVoicemails() {
        if (!this.mIsMwi && isCphsMwiMessage()) {
            if (this.mOriginatingAddress == null || !((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet()) {
                this.mVoiceMailCount = 0;
            } else {
                this.mVoiceMailCount = 255;
            }
            Rlog.v(LOG_TAG, "CPHS voice mail message");
        }
        return this.mVoiceMailCount;
    }

    public void setProtocolIdentifierHw(int value) {
        this.mProtocolIdentifier = value;
    }

    public void setDataCodingSchemeHw(int value) {
        this.mDataCodingScheme = value;
    }

    public void parseUserDataHw(PduParser p, boolean hasUserDataHeader) {
        parseUserData(p, hasUserDataHeader);
    }
}
