package defpackage;

import android.content.Context;
import android.util.Log;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallManager;
import com.huawei.android.feature.module.DynamicModule;

/* renamed from: ai  reason: default package */
public final class ai extends af {
    private static ai Y = null;

    private ai(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        this.M = context;
        this.L = featureLocalInstallManager;
    }

    public static synchronized ai b(Context context, FeatureLocalInstallManager featureLocalInstallManager) {
        ai aiVar;
        synchronized (ai.class) {
            if (Y == null) {
                Y = new ai(context, featureLocalInstallManager);
            }
            aiVar = Y;
        }
        return aiVar;
    }

    public final long getVersionCode() {
        try {
            if (this.L.getInstallModules().contains("pushcore")) {
                return new DynamicModule("pushcore").getDynamicModuleInfo().mVersionCode;
            }
            return -1;
        } catch (Exception e) {
            Log.e("PushLogSys", "get local versionCode error");
            return -1;
        }
    }
}
