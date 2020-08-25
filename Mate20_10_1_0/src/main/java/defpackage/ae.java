package defpackage;

import android.content.Context;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallManager;

/* renamed from: ae  reason: default package */
public final class ae extends af {
    private static ae S = null;
    private int time = 0;

    private ae(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        this.M = context;
        this.L = featureLocalInstallManager;
    }

    public static synchronized ae a(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        ae aeVar;
        synchronized (ae.class) {
            if (S == null) {
                S = new ae(context, featureLocalInstallManager);
            }
            aeVar = S;
        }
        return aeVar;
    }

    public final long getVersionCode() {
        return a("com.huawei.hwid", "HMS");
    }
}
