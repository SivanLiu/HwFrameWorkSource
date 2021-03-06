package com.android.internal.telephony.cdma;

import android.os.SystemProperties;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBaseUtils;
import com.android.internal.telephony.cdma.SmsMessage.SubmitPdu;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.BearerData.TimeStamp;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.BitwiseInputStream;
import com.android.internal.util.BitwiseInputStream.AccessException;
import com.android.internal.util.HexDump;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class HwSmsMessage {
    private static final byte BEARER_DATA = (byte) 8;
    private static final byte BEARER_REPLY_OPTION = (byte) 6;
    private static final byte CAUSE_CODES = (byte) 7;
    private static final byte[] DELIVERY_ACK_SUCCESS = new byte[]{(byte) 98, (byte) 16, (byte) 82, (byte) -97};
    private static final String DELIVERY_ACK_SUCCESS_EN = "succ";
    private static final byte DESTINATION_ADDRESS = (byte) 4;
    private static final byte DESTINATION_SUB_ADDRESS = (byte) 5;
    private static final String LOGGABLE_TAG = "CDMA:SMS";
    static final String LOG_TAG = "SmsMessage";
    private static final byte ORIGINATING_ADDRESS = (byte) 2;
    private static final byte ORIGINATING_SUB_ADDRESS = (byte) 3;
    private static boolean PLUS_TRANFER_IN_AP = (HwModemCapability.isCapabilitySupport(2) ^ 1);
    private static final byte SERVICE_CATEGORY = (byte) 1;
    private static final byte TELESERVICE_IDENTIFIER = (byte) 0;
    private static final boolean VDBG = false;
    private static SmsMessageUtils smsMessageUtils = ((SmsMessageUtils) EasyInvokeFactory.getInvokeUtils(SmsMessageUtils.class));

    public static SubmitPdu getSubmitDeliverPdu(boolean isSubmitPdu, String mcTimeStamp, String destAddr, String message, SmsHeader smsHeader) {
        if (message == null || destAddr == null) {
            Log.e("CDMA/SmsMessage", "empty message or destAddr");
            return null;
        }
        UserData uData = new UserData();
        uData.payloadStr = message;
        uData.userDataHeader = smsHeader;
        return privateGetSubmitDeliverPdu(isSubmitPdu, destAddr, mcTimeStamp, uData);
    }

    private static SubmitPdu privateGetSubmitDeliverPdu(boolean isSubmitPdu, String origAddrStr, String mcTimestamp, UserData userData) {
        CdmaSmsAddress destAddr;
        IOException ex;
        String str;
        StringBuilder stringBuilder;
        if (PLUS_TRANFER_IN_AP) {
            destAddr = SmsMessage.parseAddrForSMSMO(origAddrStr);
            if (destAddr == null) {
                Rlog.e(LOG_TAG, "privateGetSubmitDeliverPdu, CdmaSmsAddress parse error.");
                return null;
            }
        }
        destAddr = CdmaSmsAddress.parse(PhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(origAddrStr));
        CdmaSmsAddress destAddr2 = destAddr;
        if (destAddr2 == null) {
            return null;
        }
        BearerData bearerData = new BearerData();
        bearerData.messageId = SmsMessage.getNextMessageId();
        int i = 0;
        bearerData.userAckReq = false;
        bearerData.readAckReq = false;
        bearerData.reportReq = false;
        bearerData.deliveryAckReq = false;
        if (isSubmitPdu) {
            bearerData.messageType = 2;
            bearerData.priorityIndicatorSet = true;
            bearerData.priority = 0;
        } else {
            bearerData.messageType = 1;
            TimeStamp msgTimeStamp = new TimeStamp();
            msgTimeStamp.set(Long.parseLong(mcTimestamp));
            bearerData.msgCenterTimeStamp = msgTimeStamp;
        }
        bearerData.userData = userData;
        byte[] encodedBearerData = BearerData.encode(bearerData);
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("MO (encoded) BearerData = ");
            stringBuilder2.append(bearerData);
            Rlog.d(str2, stringBuilder2.toString());
            if (encodedBearerData != null) {
                str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("MO raw BearerData = '");
                stringBuilder2.append(HexDump.toHexString(encodedBearerData));
                stringBuilder2.append("'");
                Rlog.d(str2, stringBuilder2.toString());
            }
        }
        if (encodedBearerData == null) {
            return null;
        }
        CdmaSmsAddress destAddr3;
        BearerData bearerData2;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(363);
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(4098);
            dos.write(0);
            dos.writeInt(0);
            dos.writeInt(destAddr2.digitMode);
            dos.writeInt(destAddr2.numberMode);
            dos.writeInt(destAddr2.ton);
            dos.writeInt(destAddr2.numberPlan);
            dos.write(destAddr2.numberOfDigits);
            dos.write(destAddr2.origBytes, 0, destAddr2.origBytes.length);
            dos.writeInt(0);
            dos.write(0);
            dos.write(0);
            dos.writeInt(encodedBearerData.length);
            dos.write(encodedBearerData, 0, encodedBearerData.length);
            dos.close();
            SubmitPdu pdu = new SubmitPdu();
            pdu.encodedMessage = baos.toByteArray();
            byte[] bArr = pdu.encodedMessage;
            int length = bArr.length;
            while (i < length) {
                StringBuilder stringBuilder3;
                byte content = bArr[i];
                String str3 = LOG_TAG;
                destAddr3 = destAddr2;
                try {
                    stringBuilder3 = new StringBuilder();
                    bearerData2 = bearerData;
                } catch (IOException e) {
                    ex = e;
                    bearerData2 = bearerData;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("creating SubmitPdu failed: ");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                    return null;
                }
                try {
                    stringBuilder3.append("pdu is");
                    stringBuilder3.append(content);
                    Rlog.e(str3, stringBuilder3.toString());
                    i++;
                    destAddr2 = destAddr3;
                    bearerData = bearerData2;
                } catch (IOException e2) {
                    ex = e2;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("creating SubmitPdu failed: ");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                    return null;
                }
            }
            bearerData2 = bearerData;
            pdu.encodedScAddress = null;
            return pdu;
        } catch (IOException e3) {
            ex = e3;
            destAddr3 = destAddr2;
            bearerData2 = bearerData;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("creating SubmitPdu failed: ");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static void parseRUIMPdu(SmsMessage msg, byte[] pdu) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pdu));
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        boolean readSmsEnvOK = false;
        byte[] address = new byte[0];
        byte[] subAddress = new byte[0];
        byte[] causeCodes = new byte[0];
        addr.origBytes = new byte[0];
        int readLenth = 0;
        try {
            env.messageType = dis.readByte();
            while (!readSmsEnvOK) {
                int message_len = 255 & dis.readByte();
                switch (dis.readByte()) {
                    case (byte) 0:
                        env.teleService = dis.readShort();
                        break;
                    case (byte) 1:
                        env.serviceCategory = dis.readShort();
                        break;
                    case (byte) 2:
                    case (byte) 4:
                        address = new byte[message_len];
                        if (dis.read(address, 0, message_len) == message_len) {
                            break;
                        }
                        Log.e(LOG_TAG, "DataInputStream read error");
                        break;
                    case (byte) 3:
                    case (byte) 5:
                        subAddress = new byte[message_len];
                        if (dis.read(subAddress, 0, message_len) == message_len) {
                            break;
                        }
                        Log.e(LOG_TAG, "DataInputStream read error");
                        break;
                    case (byte) 6:
                        env.bearerReply = dis.readByte();
                        break;
                    case (byte) 7:
                        causeCodes = new byte[message_len];
                        if (dis.read(causeCodes, 0, message_len) == message_len) {
                            break;
                        }
                        Log.e(LOG_TAG, "DataInputStream read error");
                        break;
                    case (byte) 8:
                        env.bearerData = new byte[message_len];
                        if (dis.read(env.bearerData, 0, message_len) != message_len) {
                            Log.e(LOG_TAG, "DataInputStream read error");
                        }
                        readSmsEnvOK = true;
                        break;
                    default:
                        break;
                }
            }
            dis.close();
        } catch (Exception ex) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createFromPdu: conversion from byte array to object failed: ");
            stringBuilder.append(ex);
            Log.e(str, stringBuilder.toString());
        }
        parseAddress(address, addr);
        if (PLUS_TRANFER_IN_AP) {
            String number = HwCustPlusAndIddNddConvertUtils.replaceIddNddWithPlusForSms(new String(addr.origBytes, Charset.defaultCharset()));
            if (addr.ton == 1 && number != null && number.length() > 0 && number.charAt(0) != '+') {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("+");
                stringBuilder2.append(number);
                number = stringBuilder2.toString();
                Rlog.d(LOG_TAG, "HwSmsMessage->parseRUIMPdu ton == SmsAddress.TON_INTERNATIONAL");
            }
            if (number != null) {
                addr.origBytes = number.getBytes(Charset.defaultCharset());
            }
            addr.numberOfDigits = addr.origBytes.length;
        }
        parseCauseCodes(causeCodes, env);
        SmsMessageBaseUtils.setOriginatingAddress(msg, addr);
        env.origAddress = addr;
        smsMessageUtils.setEnvelope(msg, env);
        SmsMessageBaseUtils.setPdu(msg, pdu);
        msg.parseSms();
        if (Log.isLoggable(LOGGABLE_TAG, 2)) {
            HexDump.toHexString(subAddress);
        }
    }

    protected static void parseAddress(byte[] address, CdmaSmsAddress addr) {
        if (address != null) {
            try {
                BitwiseInputStream inStream = new BitwiseInputStream(address);
                if (inStream.available() > 0) {
                    int digitsSize;
                    addr.digitMode = inStream.read(1);
                    addr.numberMode = inStream.read(1);
                    int readCount = 2;
                    if (1 == addr.digitMode) {
                        digitsSize = 8;
                        addr.ton = inStream.read(3);
                        readCount = 2 + 3;
                        if (addr.numberMode == 0) {
                            addr.numberPlan = inStream.read(4);
                            readCount += 4;
                        }
                    } else {
                        digitsSize = 4;
                    }
                    addr.numberOfDigits = inStream.read(8);
                    readCount += 8;
                    int numOfDigits = addr.numberOfDigits;
                    int length = numOfDigits;
                    int i = 0;
                    addr.origBytes = new byte[numOfDigits];
                    while (!false) {
                        if (numOfDigits <= 0) {
                            break;
                        }
                        int val;
                        if (4 == digitsSize) {
                            val = inStream.read(4);
                        } else {
                            val = inStream.read(8);
                        }
                        addr.origBytes[i] = (byte) val;
                        numOfDigits--;
                        i++;
                    }
                    if (addr.digitMode == 0) {
                        convertionDTMFDigits(addr, length);
                    }
                }
            } catch (AccessException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parseAddress decode failed: ");
                stringBuilder.append(ex);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    protected static void convertionDTMFDigits(CdmaSmsAddress addr, int length) {
        StringBuffer strBuf = new StringBuffer(length);
        for (int j = 0; j < length; j++) {
            int val = 15 & addr.origBytes[j];
            if (val >= 1 && val <= 9) {
                strBuf.append(Integer.toString(val, 10));
            } else if (10 == val) {
                strBuf.append('0');
            } else if (11 == val) {
                strBuf.append('*');
            } else if (12 == val) {
                strBuf.append('#');
            }
            addr.origBytes[j] = (byte) strBuf.charAt(j);
        }
    }

    protected static void parseCauseCodes(byte[] causeCodes, SmsEnvelope env) {
        String str;
        StringBuilder stringBuilder;
        if (causeCodes != null) {
            try {
                BitwiseInputStream inStream = new BitwiseInputStream(causeCodes);
                if (inStream.available() > 0) {
                    env.replySeqNo = (byte) inStream.read(6);
                    env.errorClass = (byte) inStream.read(2);
                    if (env.errorClass != (byte) 0) {
                        env.causeCode = (byte) inStream.read(8);
                    }
                }
            } catch (AccessException ex) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parseCauseCodes decode failed: ");
                stringBuilder.append(ex);
                Log.e(str, stringBuilder.toString());
            } catch (Exception ex2) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parseCauseCodes: conversion from byte array to object failed: ");
                stringBuilder.append(ex2);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public static void doubleSmsStatusCheck(SmsMessage msg) {
        if (msg != null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DELIVERY_ACK message old status = ");
            stringBuilder.append(smsMessageUtils.getStatus(msg));
            Log.d(str, stringBuilder.toString());
            if ("TELECOM".equals(SystemProperties.get("ro.config.operators")) && smsMessageUtils.getStatus(msg) != 0) {
                str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("network is China Telecom,and status = ");
                stringBuilder2.append(smsMessageUtils.getStatus(msg));
                Log.d(str, stringBuilder2.toString());
                str = "";
                String successStr = "";
                try {
                    byte[] userData = SmsMessageBaseUtils.getUserData(msg);
                    if (userData != null) {
                        str = new String(userData, "utf-16be");
                    }
                    successStr = new String(DELIVERY_ACK_SUCCESS, "utf-16be");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "not support charset utf-16be");
                }
                String str2 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("userDataStr = ");
                stringBuilder3.append(str);
                stringBuilder3.append(";successStr = ");
                stringBuilder3.append(successStr);
                Log.d(str2, stringBuilder3.toString());
                Log.d(LOG_TAG, "mMessageBody");
                if (str.contains(successStr) || (SmsMessageBaseUtils.getMessageBody(msg) != null && SmsMessageBaseUtils.getMessageBody(msg).toLowerCase().contains(DELIVERY_ACK_SUCCESS_EN))) {
                    Log.d(LOG_TAG, "mUserData contains DELIVERY_ACK_SUCCESS, so should set status = 0");
                    smsMessageUtils.setStatus(msg, 0);
                }
            } else if (msg.getStatus() != 0) {
                int status = msg.getStatus() >> 16;
                int errClass = status >> 8;
                int errCause = status & 255;
                String str3 = LOG_TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("CDMA sms status = ");
                stringBuilder4.append(status);
                stringBuilder4.append(", errClass = ");
                stringBuilder4.append(errClass);
                stringBuilder4.append(",  errCause = ");
                stringBuilder4.append(errCause);
                Log.d(str3, stringBuilder4.toString());
                if (errClass == 0 && 3 != errCause) {
                    smsMessageUtils.setStatus(msg, 0);
                }
            }
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("DELIVERY_ACK message new status = ");
            stringBuilder.append(smsMessageUtils.getStatus(msg));
            Log.d(str, stringBuilder.toString());
        }
    }

    public static int getCdmaSub() {
        if (HwTelephonyManagerInner.getDefault().isFullNetworkSupported()) {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            int i = 0;
            while (i < phoneCount) {
                int cdmaSubId = SubscriptionManager.getSubId(i)[0];
                if (2 == TelephonyManager.getDefault().getCurrentPhoneType(cdmaSubId) && 5 == TelephonyManager.getDefault().getSimState(i)) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getCdmaSubId find cdma phone subId = ");
                    stringBuilder.append(cdmaSubId);
                    Rlog.d(str, stringBuilder.toString());
                    return cdmaSubId;
                }
                i++;
            }
        }
        Rlog.d(LOG_TAG, "use default cdma phone sub 0");
        return 0;
    }
}
