package android.net;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

public class HwCustConnectivityManagerImpl extends HwCustConnectivityManager {
    private static final boolean IS_DOCOMO = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");

    public boolean enforceStartUsingNetworkFeaturePermissionFail(Context context, int usedNetworkType) {
        if (!IS_DOCOMO) {
            return false;
        }
        int[] protectedNetworks = context.getResources().getIntArray(17236025);
        List mProtectedNetworks = new ArrayList();
        for (int p : protectedNetworks) {
            if (mProtectedNetworks.contains(Integer.valueOf(p))) {
                log("Ignoring protectedNetwork " + p);
            } else {
                mProtectedNetworks.add(Integer.valueOf(p));
            }
        }
        log("[enter] cs.enforceStartUsingNetworkFeaturePermission usedNetworkType =" + usedNetworkType);
        if (mProtectedNetworks.contains(Integer.valueOf(usedNetworkType))) {
            try {
                log("[enter] enforce permission");
                ConnectivityManager.enforceChangePermission(context);
                context.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
            } catch (SecurityException e) {
                log("Rejected using network " + usedNetworkType);
                return true;
            }
        }
        return false;
    }

    private static void log(String s) {
        Slog.d("HwCustConnectivityManagerImpl", s);
    }
}
