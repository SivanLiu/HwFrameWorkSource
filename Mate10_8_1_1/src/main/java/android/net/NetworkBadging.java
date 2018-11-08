package android.net;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;

@Deprecated
public class NetworkBadging {
    public static final int BADGING_4K = 30;
    public static final int BADGING_HD = 20;
    public static final int BADGING_NONE = 0;
    public static final int BADGING_SD = 10;

    private NetworkBadging() {
    }

    public static Drawable getWifiIcon(int signalLevel, int badging, Theme theme) {
        return Resources.getSystem().getDrawable(getWifiSignalResource(signalLevel), theme);
    }

    private static int getWifiSignalResource(int signalLevel) {
        switch (signalLevel) {
            case 0:
                return 17302774;
            case 1:
                return 17302775;
            case 2:
                return 17302776;
            case 3:
                return 17302777;
            case 4:
                return 17302778;
            default:
                throw new IllegalArgumentException("Invalid signal level: " + signalLevel);
        }
    }

    private static int getBadgedWifiSignalResource(int signalLevel) {
        switch (signalLevel) {
            case 0:
                return 17302737;
            case 1:
                return 17302738;
            case 2:
                return 17302739;
            case 3:
                return 17302740;
            case 4:
                return 17302741;
            default:
                throw new IllegalArgumentException("Invalid signal level: " + signalLevel);
        }
    }
}
