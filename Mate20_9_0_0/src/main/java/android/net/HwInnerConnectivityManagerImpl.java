package android.net;

import android.os.SystemProperties;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.HuaweiTelephonyConfigs;

public class HwInnerConnectivityManagerImpl implements HwInnerConnectivityManager {
    private static final String TAG = "HwInnerConnectivityManagerImpl";
    private static HwInnerConnectivityManagerImpl mInstance = new HwInnerConnectivityManagerImpl();

    public static HwInnerConnectivityManagerImpl getDefault() {
        return mInstance;
    }

    public boolean isHwFeature(String feature) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isHwFeature: for feature = ");
        stringBuilder.append(feature);
        Log.d(str, stringBuilder.toString());
        if (getFeature(feature)[1] != null) {
            return true;
        }
        return false;
    }

    public String[] getFeature(String str) {
        if (str != null) {
            String[] result = new String[2];
            String reqSub = null;
            if (str.equals("enableMMS_sub1")) {
                str = "enableMMS";
                reqSub = String.valueOf(0);
            } else if (str.equals("enableMMS_sub2")) {
                str = "enableMMS";
                reqSub = String.valueOf(1);
            } else if (HuaweiTelephonyConfigs.isChinaTelecom() && str.equals("enableSUPL")) {
                reqSub = String.valueOf(HwTelephonyManagerInner.getDefault().getDefault4GSlotId());
            } else if ("enableHIPRI_sub1".equals(str)) {
                str = "enableHIPRI";
                reqSub = String.valueOf(0);
            } else if ("enableHIPRI_sub2".equals(str)) {
                str = "enableHIPRI";
                reqSub = String.valueOf(1);
            }
            result[0] = str;
            result[1] = reqSub;
            return result;
        }
        throw new IllegalArgumentException("getFeature() received null string");
    }

    public boolean checkHwFeature(String feature, NetworkCapabilities networkCapabilities, int networkType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startUsingNetworkFeature: for feature = ");
        stringBuilder.append(feature);
        Log.d(str, stringBuilder.toString());
        String[] result = getFeature(feature);
        feature = result[0];
        String reqSubId = result[1];
        if (reqSubId == null) {
            return false;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("networkCapabilities setNetworkSpecifier reqSubId = ");
        stringBuilder2.append(reqSubId);
        Log.d(str2, stringBuilder2.toString());
        networkCapabilities.setNetworkSpecifier(new StringNetworkSpecifier(reqSubId));
        if (isDualCellDataForHipri(networkType, feature, reqSubId)) {
            networkCapabilities.setDualCellData("true");
        }
        return true;
    }

    private boolean isDualCellDataForHipri(int networkType, String feature, String subId) {
        if (SystemProperties.getBoolean("ro.hwpp.dual_cell_data", false) && "enableHIPRI".equals(feature) && networkType == 0 && subId != null && ((subId.equals(String.valueOf(0)) || subId.equals(String.valueOf(1))) && !subId.equals(String.valueOf(SubscriptionManager.getDefaultDataSubscriptionId())))) {
            return true;
        }
        return false;
    }
}
