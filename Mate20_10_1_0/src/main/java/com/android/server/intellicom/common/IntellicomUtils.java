package com.android.server.intellicom.common;

import android.common.HwFrameworkFactory;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.HwTelephonyFactory;

public class IntellicomUtils {
    private static final String TAG = "IntellicomUtils";

    public static boolean isMultiSimEnabled() {
        return TelephonyManager.getDefault().isMultiSimEnabled();
    }

    public static String getOperator(int slotId) {
        String operator = HwFrameworkFactory.getHwInnerTelephonyManager().getCTOperator(slotId, TelephonyManager.getDefault().getSimOperatorNumericForPhone(slotId));
        if (isMultiSimEnabled()) {
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(slotId))) {
                return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(slotId));
            }
            return operator;
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            return HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
        } else {
            return operator;
        }
    }

    public static int getSubId(int slotId) {
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds != null && subIds.length > 0) {
            return subIds[0];
        }
        logw("can not get subId for slotId =" + slotId);
        return -1;
    }

    private IntellicomUtils() {
    }

    private static void logw(String msg) {
        Log.w(TAG, msg);
    }
}
