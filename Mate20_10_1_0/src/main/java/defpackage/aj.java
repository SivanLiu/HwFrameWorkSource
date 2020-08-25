package defpackage;

import android.content.Context;
import android.util.Log;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallManager;

/* renamed from: aj  reason: default package */
public final class aj extends af {
    private static aj Z = null;

    private aj(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        this.M = context;
        this.L = featureLocalInstallManager;
    }

    public static synchronized aj c(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        aj ajVar;
        synchronized (aj.class) {
            if (Z == null) {
                Z = new aj(context, featureLocalInstallManager);
            }
            ajVar = Z;
        }
        return ajVar;
    }

    public final long getVersionCode() {
        long a = a("com.huawei.android.pushagent", "NC");
        if (a != -1) {
            Log.i("PushLogSys", "get nc meta push version");
            return a;
        }
        try {
            return (long) this.M.getPackageManager().getPackageInfo("com.huawei.android.pushagent", 0).versionCode;
        } catch (Exception e) {
            Log.e("PushLogSys", "get nc versionCode error: " + ao.a(e));
            return a;
        }
    }
}
