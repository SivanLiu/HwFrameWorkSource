package com.android.internal.telephony;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import huawei.cust.HwCfgFilePolicy;
import java.util.HashMap;
import java.util.Map;

public class HwCustRILReferenceImpl extends HwCustRILReference {
    private static final boolean CUST_APN_AUTH_ON = SystemProperties.getBoolean("ro.config.hw_allow_pdp_auth", false);
    private static final String TAG = "HwCustRILReferenceImpl";

    public boolean isCustCorrectApnAuthOn() {
        int slotId = SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId());
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("hw_allow_pdp_auth", slotId, Boolean.class);
        boolean valueFromProp = CUST_APN_AUTH_ON;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCustCorrectApnAuthOn, slotId: ");
        stringBuilder.append(slotId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        Rlog.d(str, stringBuilder.toString());
        if (valueFromCard != null) {
            return valueFromCard.booleanValue() || valueFromProp;
        } else {
            return valueFromProp;
        }
    }

    public Map<String, String> custCorrectApnAuth(String userName, int authType, String password) {
        Map<String, String> map = new HashMap();
        map.put("userName", userName == null ? "" : userName);
        map.put("password", password == null ? "" : password);
        map.put("authType", String.valueOf(authType));
        return map;
    }
}
