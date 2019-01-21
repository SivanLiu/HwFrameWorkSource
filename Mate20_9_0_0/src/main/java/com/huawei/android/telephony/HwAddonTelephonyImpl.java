package com.huawei.android.telephony;

import android.util.Log;
import com.android.internal.telephony.HwAddonTelephonyFactory.HwAddonTelephonyInterface;

public class HwAddonTelephonyImpl implements HwAddonTelephonyInterface {
    private static final String TAG = "HwAddonTelephonyFactoryImpl";

    public int getDefault4GSlotId() {
        try {
            return TelephonyManagerEx.getDefault4GSlotId();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDefault4GSlotId exception is ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return 0;
        }
    }
}
