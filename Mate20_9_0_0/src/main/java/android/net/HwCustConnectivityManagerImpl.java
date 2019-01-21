package android.net;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

public class HwCustConnectivityManagerImpl extends HwCustConnectivityManager {
    private static final boolean IS_DOCOMO = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    protected static final boolean NEED_TO_HANDLE_EMERGENCY_APN = SystemProperties.getBoolean("ro.config.emergency_apn_handle", false);

    public boolean enforceStartUsingNetworkFeaturePermissionFail(Context context, int usedNetworkType) {
        if (!IS_DOCOMO) {
            return false;
        }
        int[] protectedNetworks = context.getResources().getIntArray(17236028);
        List mProtectedNetworks = new ArrayList();
        for (int p : protectedNetworks) {
            if (mProtectedNetworks.contains(Integer.valueOf(p))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring protectedNetwork ");
                stringBuilder.append(p);
                log(stringBuilder.toString());
            } else {
                mProtectedNetworks.add(Integer.valueOf(p));
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[enter] cs.enforceStartUsingNetworkFeaturePermission usedNetworkType =");
        stringBuilder2.append(usedNetworkType);
        log(stringBuilder2.toString());
        if (mProtectedNetworks.contains(Integer.valueOf(usedNetworkType))) {
            try {
                log("[enter] enforce permission");
                ConnectivityManager.enforceChangePermission(context);
                context.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
            } catch (SecurityException e) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Rejected using network ");
                stringBuilder2.append(usedNetworkType);
                log(stringBuilder2.toString());
                return true;
            }
        }
        return false;
    }

    private static void log(String s) {
        Slog.d("HwCustConnectivityManagerImpl", s);
    }

    public boolean canHandleEimsNetworkCapabilities(NetworkCapabilities nc) {
        boolean z = false;
        if (nc == null) {
            return false;
        }
        if (NEED_TO_HANDLE_EMERGENCY_APN && nc.hasCapability(10)) {
            z = true;
        }
        return z;
    }

    public NetworkCapabilities networkCapabilitiesForEimsType(int type) {
        if (NEED_TO_HANDLE_EMERGENCY_APN) {
            return ConnectivityManager.networkCapabilitiesForType(type);
        }
        return null;
    }
}
