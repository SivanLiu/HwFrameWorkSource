package com.android.internal.telephony;

import android.telephony.Rlog;

public class TelephonyCapabilities {
    private static final String LOG_TAG = "TelephonyCapabilities";

    private TelephonyCapabilities() {
    }

    public static boolean supportsEcm(Phone phone) {
        String tag = new StringBuilder();
        tag.append("TelephonyCapabilities[SUB");
        tag.append(phone.getPhoneId());
        tag.append("]");
        tag = tag.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("supportsEcm: Phone type = ");
        stringBuilder.append(phone.getPhoneType());
        stringBuilder.append(" Ims Phone = ");
        stringBuilder.append(phone.getImsPhone());
        Rlog.d(tag, stringBuilder.toString());
        return phone.getPhoneType() == 2 || phone.getImsPhone() != null;
    }

    public static boolean supportsOtasp(Phone phone) {
        return phone.getPhoneType() == 2;
    }

    public static boolean supportsVoiceMessageCount(Phone phone) {
        return phone.getVoiceMessageCount() != -1;
    }

    public static boolean supportsNetworkSelection(Phone phone) {
        return phone.getPhoneType() == 1;
    }

    public static int getDeviceIdLabel(Phone phone) {
        if (phone.getPhoneType() == 1) {
            return 17040212;
        }
        if (phone.getPhoneType() == 2) {
            return 17040506;
        }
        String tag = new StringBuilder();
        tag.append("TelephonyCapabilities[SUB");
        tag.append(phone.getPhoneId());
        tag.append("]");
        tag = tag.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDeviceIdLabel: no known label for phone ");
        stringBuilder.append(phone.getPhoneName());
        Rlog.w(tag, stringBuilder.toString());
        return 0;
    }

    public static boolean supportsConferenceCallManagement(Phone phone) {
        if (phone.getPhoneType() == 1 || phone.getPhoneType() == 3) {
            return true;
        }
        return false;
    }

    public static boolean supportsHoldAndUnhold(Phone phone) {
        if (phone.getPhoneType() == 1 || phone.getPhoneType() == 3 || phone.getPhoneType() == 5) {
            return true;
        }
        return false;
    }

    public static boolean supportsAnswerAndHold(Phone phone) {
        if (phone.getPhoneType() == 1 || phone.getPhoneType() == 3) {
            return true;
        }
        return false;
    }

    public static boolean supportsAdn(int phoneType) {
        return phoneType == 1 || phoneType == 2;
    }

    public static boolean canDistinguishDialingAndConnected(int phoneType) {
        return phoneType == 1;
    }
}
