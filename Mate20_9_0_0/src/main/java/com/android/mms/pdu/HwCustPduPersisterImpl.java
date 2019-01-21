package com.android.mms.pdu;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.google.android.mms.pdu.EncodedStringValue;
import java.util.ArrayList;

public class HwCustPduPersisterImpl extends HwCustPduPersister {
    private static final String TAG = "HwCustPduPersisterImpl";
    private static boolean excludeShortCode = SystemProperties.getBoolean("ro.config.exclude_short_code", false);

    public boolean isShortCodeFeatureEnabled() {
        return excludeShortCode;
    }

    private boolean hasShortCode(EncodedStringValue[] enNumbers) {
        int i = 0;
        if (!excludeShortCode || enNumbers == null || enNumbers.length < 2) {
            return false;
        }
        String[] numbers = new String[enNumbers.length];
        for (int i2 = 0; i2 < enNumbers.length; i2++) {
            if (enNumbers[i2] != null) {
                numbers[i2] = enNumbers[i2].getString();
            }
        }
        boolean hasShortCode = false;
        while (i < numbers.length) {
            String number = numbers[i];
            if (TextUtils.isEmpty(number) || !number.contains("@") || !number.contains(".")) {
                number = extractDigit(number);
                if (!TextUtils.isEmpty(number)) {
                    if (number.startsWith("+") || number.startsWith("011")) {
                        if (number.length() == 12 && number.startsWith("+") && number.substring(1).startsWith("1") && (number.substring(2).startsWith("0") || number.substring(2).startsWith("1"))) {
                            hasShortCode = true;
                        }
                        if (number.length() == 14 && number.startsWith("011") && number.substring(3).startsWith("1") && (number.substring(4).startsWith("0") || number.substring(4).startsWith("1"))) {
                            hasShortCode = true;
                        }
                    } else if (number.length() < 10) {
                        hasShortCode = true;
                    } else if (number.length() == 10 && (number.startsWith("0") || number.startsWith("1"))) {
                        hasShortCode = true;
                    } else if (number.length() == 11 && number.startsWith("1") && (number.charAt(1) == '0' || number.charAt(1) == '1')) {
                        hasShortCode = true;
                    }
                }
            }
            i++;
        }
        return hasShortCode;
    }

    public boolean hasShortCode(boolean isMMSEnable, String[] list, Context context, String toastString) {
        if (!isMMSEnable || list == null) {
            return false;
        }
        boolean bShortCodeStatus = hasShortCode(list);
        if (bShortCodeStatus) {
            Log.i(TAG, "One of the recepients has short code");
            Toast.makeText(context, toastString, 0).show();
        }
        return bShortCodeStatus;
    }

    private boolean hasShortCode(String[] list) {
        return hasShortCode(EncodedStringValue.encodeStrings(list));
    }

    private String extractDigit(String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        int numLength = number.length();
        for (int i = 0; i < numLength; i++) {
            if (i == 0 && number.charAt(0) == '+') {
                sb.append(number.charAt(0));
            } else if (Character.isDigit(number.charAt(i))) {
                sb.append(number.charAt(i));
            }
        }
        return sb.toString();
    }

    public boolean hasShortCode(EncodedStringValue[] toNumbers, EncodedStringValue[] ccNumbers) {
        return hasShortCode(concatEncodedStringValue(toNumbers, ccNumbers));
    }

    private EncodedStringValue[] concatEncodedStringValue(EncodedStringValue[] toNumbers, EncodedStringValue[] ccNumbers) {
        int length;
        ArrayList<EncodedStringValue> list = new ArrayList();
        int i = 0;
        if (toNumbers != null) {
            for (EncodedStringValue v : toNumbers) {
                if (v != null) {
                    list.add(v);
                }
            }
        }
        if (ccNumbers != null) {
            length = ccNumbers.length;
            while (i < length) {
                EncodedStringValue v2 = ccNumbers[i];
                if (v2 != null) {
                    list.add(v2);
                }
                i++;
            }
        }
        return (EncodedStringValue[]) list.toArray(new EncodedStringValue[list.size()]);
    }
}
