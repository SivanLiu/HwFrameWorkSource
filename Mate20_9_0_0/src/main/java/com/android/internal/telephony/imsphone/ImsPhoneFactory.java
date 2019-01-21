package com.android.internal.telephony.imsphone;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneNotifier;

public class ImsPhoneFactory {
    private static final String IMS_SERVICE_CLASS_NAME;
    private static final String IMS_SERVICE_PKG_NAME = SystemProperties.get("ro.config.hw_ims_pkg", "com.huawei.ims").trim();
    private static final boolean isImsAsNormal = HuaweiTelephonyConfigs.isHisiPlatform();
    private static final boolean volte = SystemProperties.getBoolean("ro.config.hw_volte_on", false);

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(IMS_SERVICE_PKG_NAME);
        stringBuilder.append(".ImsService");
        IMS_SERVICE_CLASS_NAME = stringBuilder.toString();
    }

    public static ImsPhone makePhone(Context context, PhoneNotifier phoneNotifier, Phone defaultPhone) {
        try {
            return new ImsPhone(context, phoneNotifier, defaultPhone);
        } catch (Exception e) {
            Rlog.e("VoltePhoneFactory", "makePhone", e);
            return null;
        }
    }

    public static void startImsService(Context context) {
        if (volte) {
            Rlog.d("ImsPhoneFactory", "startImsService");
            try {
                Intent intent = new Intent();
                intent.setClassName(IMS_SERVICE_PKG_NAME, IMS_SERVICE_CLASS_NAME);
                context.startService(intent);
            } catch (SecurityException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startImsService: exception = ");
                stringBuilder.append(ex);
                Rlog.w("ImsPhoneFactory", stringBuilder.toString());
            }
        }
    }

    public static boolean isimsAsNormalCon() {
        return isImsAsNormal;
    }
}
