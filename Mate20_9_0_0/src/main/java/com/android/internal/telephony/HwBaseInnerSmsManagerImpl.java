package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ISms.Stub;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.HwBearerData;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.telephony.gsm.HwSmsMessage;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.gsm.SmsMessage.PduParser;
import com.android.internal.telephony.gsm.SmsMessageUtils;
import com.android.internal.util.BitwiseOutputStream;
import com.android.internal.util.BitwiseOutputStream.AccessException;
import java.util.ArrayList;

public class HwBaseInnerSmsManagerImpl implements HwBaseInnerSmsManager {
    private static final int CB_RANGE_START_END_STEP = 2;
    private static final String CONTACT_PACKAGE_NAME = "com.android.contacts";
    private static final String LOG_TAG = "HwBaseInnerSmsManagerImpl";
    private static final String MMS_ACTIVITY_NAME = "com.android.mms.ui.ComposeMessageActivity";
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    public static final int SMS_GW_VP_ABSOLUTE_FORMAT = 24;
    public static final int SMS_GW_VP_ENHANCED_FORMAT = 8;
    public static final int SMS_GW_VP_RELATIVE_FORMAT = 16;
    private static SmsMessageUtils gsmSmsMessageUtils = new SmsMessageUtils();
    private static HwBaseInnerSmsManager mInstance = new HwBaseInnerSmsManagerImpl();

    public static HwBaseInnerSmsManager getDefault() {
        return mInstance;
    }

    public boolean shouldSetDefaultApplicationForPackage(String packageName, Context context) {
        String hwMmsPackageName = getHwMmsPackageName(context);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current packageName: ");
        stringBuilder.append(packageName);
        stringBuilder.append(", hwMmsPackageName: ");
        stringBuilder.append(hwMmsPackageName);
        Rlog.d(str, stringBuilder.toString());
        return !SystemProperties.getBoolean("ro.config.hw_privacymode", false) || 1 != Secure.getInt(context.getContentResolver(), "privacy_mode_on", 0) || 1 != Secure.getInt(context.getContentResolver(), "privacy_mode_state", 0) || packageName == null || hwMmsPackageName == null || packageName.equals(hwMmsPackageName);
    }

    private static String getHwMmsPackageName(Context context) {
        Intent intent = new Intent();
        intent.setClassName(CONTACT_PACKAGE_NAME, "com.android.mms.ui.ComposeMessageActivity");
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            return MMS_PACKAGE_NAME;
        }
        return CONTACT_PACKAGE_NAME;
    }

    public boolean allowToSetSmsWritePermission(String packageName) {
        if (CONTACT_PACKAGE_NAME.equalsIgnoreCase(packageName) || MMS_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            return true;
        }
        return false;
    }

    public boolean parseGsmSmsSubmit(SmsMessage smsMessage, int mti, Object parcel, int firstByte) {
        PduParser p = (PduParser) parcel;
        boolean hasUserDataHeader = false;
        if (1 != mti) {
            return false;
        }
        p.getByte();
        SmsMessageBaseUtils.setOriginatingAddress(smsMessage, p.getAddress());
        gsmSmsMessageUtils.setProtocolIdentifier(smsMessage, p.getByte());
        gsmSmsMessageUtils.setDataCodingScheme(smsMessage, p.getByte());
        p.mCur += getValidityPeriod(firstByte);
        if ((firstByte & 64) == 64) {
            hasUserDataHeader = true;
        }
        gsmSmsMessageUtils.parseUserData(smsMessage, p, hasUserDataHeader);
        return true;
    }

    private int getValidityPeriod(int firstByte) {
        int ValidityPeriod = firstByte & 24;
        if (ValidityPeriod != 8) {
            if (ValidityPeriod == 16) {
                return 1;
            }
            if (ValidityPeriod != 24) {
                Rlog.e("PduParser", "unsupported validity format.");
                return 0;
            }
        }
        return 7;
    }

    public String getUserDataGSM8Bit(PduParser p, int septetCount) {
        return HwSmsMessage.getUserDataGSM8Bit(p, septetCount);
    }

    public void parseRUIMPdu(com.android.internal.telephony.cdma.SmsMessage msg, byte[] pdu) {
        com.android.internal.telephony.cdma.HwSmsMessage.parseRUIMPdu(msg, pdu);
    }

    public boolean encode7bitMultiSms(UserData uData, byte[] udhData, boolean force) {
        return HwBearerData.encode7bitMultiSms(uData, udhData, force);
    }

    public void encodeMsgCenterTimeStampCheck(BearerData bData, BitwiseOutputStream outStream) throws AccessException {
        HwBearerData.encodeMsgCenterTimeStampCheck(bData, outStream);
    }

    public void doubleSmsStatusCheck(com.android.internal.telephony.cdma.SmsMessage msg) {
        com.android.internal.telephony.cdma.HwSmsMessage.doubleSmsStatusCheck(msg);
    }

    public int getCdmaSub() {
        return com.android.internal.telephony.cdma.HwSmsMessage.getCdmaSub();
    }

    public ArrayList<String> fragmentForEmptyText() {
        ArrayList<String> result = new ArrayList(1);
        result.add("");
        return result;
    }

    public byte[] getNewbyte() {
        if (2 == TelephonyManager.getDefault().getPhoneType()) {
            return new byte[254];
        }
        return new byte[PduHeaders.START];
    }

    public String getSmscAddr() {
        try {
            ISms simISms = Stub.asInterface(ServiceManager.getService("isms"));
            if (simISms != null) {
                return simISms.getSmscAddr();
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean setSmscAddr(String smscAddr) {
        try {
            ISms simISms = Stub.asInterface(ServiceManager.getService("isms"));
            if (simISms != null) {
                return simISms.setSmscAddr(smscAddr);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getSmscAddr(long subId) {
        try {
            ISms simISms = Stub.asInterface(ServiceManager.getService("isms"));
            if (simISms != null) {
                return simISms.getSmscAddrForSubscriber(subId);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean setSmscAddr(long subId, String smscAddr) {
        try {
            ISms simISms = Stub.asInterface(ServiceManager.getService("isms"));
            if (simISms != null) {
                return simISms.setSmscAddrForSubscriber(subId, smscAddr);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setCellBroadcastRangeList(int subId, int[] messageIds, int ranType) {
        if (isValidCellBroadcastRange(messageIds)) {
            try {
                ISms iccISms = getISmsService();
                if (iccISms != null) {
                    return iccISms.setCellBroadcastRangeListForSubscriber(subId, messageIds, ranType);
                }
                return false;
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "setCellBroadcastRangeList RemoteException");
                return false;
            }
        }
        throw new IllegalArgumentException("endMessageId < startMessageId");
    }

    private boolean isValidCellBroadcastRange(int[] messageIds) {
        if (messageIds == null || messageIds.length % 2 != 0) {
            return false;
        }
        int len = messageIds.length;
        for (int i = 0; i < len; i += 2) {
            if (messageIds[i + 1] < messageIds[i]) {
                return false;
            }
        }
        return true;
    }

    private static ISms getISmsService() {
        return Stub.asInterface(ServiceManager.getService("isms"));
    }
}
