package android.telephony;

import android.os.SystemProperties;

public class HwCustUtil {
    public static final boolean isVZW;
    public static final boolean isVoLteOn = SystemProperties.getBoolean("ro.config.hw_volte_on", false);
    public static final boolean isVoWiFi = SystemProperties.getBoolean("ro.config.hw_vowifi", false);

    static {
        boolean equals;
        if ("389".equals(SystemProperties.get("ro.config.hw_opta"))) {
            equals = "840".equals(SystemProperties.get("ro.config.hw_optb"));
        } else {
            equals = false;
        }
        isVZW = equals;
    }
}
