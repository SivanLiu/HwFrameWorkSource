package com.android.internal.telephony.cdma;

import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.CdmaSmsSubaddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import java.nio.charset.Charset;

public class SmsMessageConverter {
    private static final String LOGGABLE_TAG = "CDMA:SMS";
    static final String LOG_TAG = "SmsMessageConverter";
    private static boolean PLUS_TRANFER_IN_AP = (HwModemCapability.isCapabilitySupport(2) ^ 1);
    private static final boolean VDBG = false;

    public static SmsMessage newCdmaSmsMessageFromRil(CdmaSmsMessage cdmaSmsMessage) {
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        CdmaSmsSubaddress subaddr = new CdmaSmsSubaddress();
        env.teleService = cdmaSmsMessage.teleserviceId;
        int index = 0;
        if (cdmaSmsMessage.isServicePresent) {
            env.messageType = 1;
        } else if (env.teleService == 0) {
            env.messageType = 2;
        } else {
            env.messageType = 0;
        }
        env.serviceCategory = cdmaSmsMessage.serviceCategory;
        int addressDigitMode = cdmaSmsMessage.address.digitMode;
        addr.digitMode = (byte) (255 & addressDigitMode);
        addr.numberMode = (byte) (cdmaSmsMessage.address.numberMode & 255);
        addr.ton = cdmaSmsMessage.address.numberType;
        addr.numberPlan = (byte) (255 & cdmaSmsMessage.address.numberPlan);
        byte count = (byte) cdmaSmsMessage.address.digits.size();
        addr.numberOfDigits = count;
        byte[] data = new byte[count];
        for (byte index2 = (byte) 0; index2 < count; index2++) {
            data[index2] = ((Byte) cdmaSmsMessage.address.digits.get(index2)).byteValue();
            if (addressDigitMode == 0) {
                data[index2] = SmsMessage.convertDtmfToAscii(data[index2]);
            }
        }
        addr.origBytes = data;
        if (PLUS_TRANFER_IN_AP) {
            String number = HwCustPlusAndIddNddConvertUtils.replaceIddNddWithPlusForSms(new String(addr.origBytes, Charset.defaultCharset()));
            if (addr.ton == 1 && number != null && number.length() > 0 && number.charAt(0) != '+') {
                Rlog.d(LOG_TAG, "newFromParcel ton == SmsAddress.TON_INTERNATIONAL");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("+");
                stringBuilder.append(number);
                number = stringBuilder.toString();
            }
            if (number != null) {
                addr.origBytes = number.getBytes(Charset.defaultCharset());
            } else {
                addr.origBytes = data;
            }
            addr.numberOfDigits = addr.origBytes.length;
        }
        subaddr.type = cdmaSmsMessage.subAddress.subaddressType;
        subaddr.odd = (byte) cdmaSmsMessage.subAddress.odd;
        byte count2 = (byte) cdmaSmsMessage.subAddress.digits.size();
        if (count2 < (byte) 0) {
            count2 = (byte) 0;
        }
        byte[] data2 = new byte[count2];
        for (byte index3 = (byte) 0; index3 < count2; index3++) {
            data2[index3] = ((Byte) cdmaSmsMessage.subAddress.digits.get(index3)).byteValue();
        }
        subaddr.origBytes = data2;
        int countInt = cdmaSmsMessage.bearerData.size();
        if (countInt < 0) {
            countInt = 0;
        }
        data2 = new byte[countInt];
        while (index < countInt) {
            data2[index] = ((Byte) cdmaSmsMessage.bearerData.get(index)).byteValue();
            index++;
        }
        env.bearerData = data2;
        env.origAddress = addr;
        env.origSubaddress = subaddr;
        return new SmsMessage(addr, env);
    }

    public static SmsMessage newSmsMessageFromCdmaSmsMessage(CdmaSmsMessage msg) {
        return new SmsMessage(newCdmaSmsMessageFromRil(msg));
    }
}
