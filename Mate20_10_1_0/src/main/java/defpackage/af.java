package defpackage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.feature.install.InstallRequest;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallManager;
import com.huawei.android.feature.install.localinstall.FeatureLocalInstallRequest;
import com.huawei.android.feature.module.DynamicModule;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.dynamicload.IPushManager;

/* renamed from: af  reason: default package */
public abstract class af {
    protected FeatureLocalInstallManager L;
    protected Context M;
    protected int T = 0;

    public static void a(int i) {
        if (-10 == i) {
            Log.e("PushLogSys", "pushcore not exist");
        } else if (-11 == i) {
            Log.e("PushLogSys", "pushcore parse error");
        } else if (-12 == i) {
            Log.e("PushLogSys", "pushcore signature is error");
        } else if (-15 == i) {
            Log.e("PushLogSys", "pushcore version is invalid");
        } else if (-17 == i) {
            Log.e("PushLogSys", "copy pushcore to directory error");
        } else if (-18 == i) {
            Log.e("PushLogSys", "pushcore is installing, can not install now");
        } else if (-19 == i) {
            Log.e("PushLogSys", "pushcore path parse error");
        } else if (-21 == i || -22 == i) {
            Log.e("PushLogSys", "FeatureLocalInstallManager class loader error");
        } else if (-26 == i) {
            Log.e("PushLogSys", "feature directory do not exist");
        } else if (-100 == i) {
            Log.e("PushLogSys", "other internal error");
        }
    }

    /* access modifiers changed from: private */
    public void c(String str, String str2) {
        if (this.T >= 3 || this.T < 0) {
            Log.e("PushLogSys", "install pushcore error, try to load local pushcore");
            e();
            return;
        }
        Handler handler = new Handler();
        this.L.startInstall(InstallRequest.newBuilder().addModule(new FeatureLocalInstallRequest("pushcore", str, "1E3EEE2A88A6DF75FB4AF56ADC8373BB818F3CB90A4935C7821582B8CEBB694C")).build(), new ag(this, new am(this.M), str2, str), handler);
    }

    private boolean f() {
        try {
            if (this.L.getInstallModules().contains("pushcore")) {
                DynamicModule dynamicModule = new DynamicModule("pushcore");
                ah ahVar = new ah(this);
                if (!PushService.b().N) {
                    IPushManager iPushManager = (IPushManager) dynamicModule.getClassInstance("com.huawei.android.pushagent.PushManagerImpl", ahVar);
                    if (iPushManager != null) {
                        Log.i("PushLogSys", "start pushcore service");
                        iPushManager.startPushService(this.M);
                        PushService.b().N = true;
                        PushService.b().K = iPushManager;
                        PushService b = PushService.b();
                        if (b.O != null) {
                            b.O.countDown();
                        }
                        return true;
                    }
                    Log.e("PushLogSys", "start pushcore service error");
                    return false;
                }
                Log.i("PushLogSys", "pushcore service is running");
                return true;
            }
            Log.e("PushLogSys", "pushcore apk not exist");
            return false;
        } catch (Exception e) {
            Log.e("PushLogSys", "load pushcore exception: " + ao.a(e));
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public final long a(String str, String str2) {
        Bundle bundle;
        try {
            ApplicationInfo applicationInfo = this.M.getPackageManager().getApplicationInfo(str, 128);
            if (applicationInfo == null || (bundle = applicationInfo.metaData) == null) {
                return -1;
            }
            String string = bundle.getString("pushcore_version");
            if (TextUtils.isEmpty(string) || string.length() <= str2.length()) {
                return -1;
            }
            return Long.parseLong(string.substring(str2.length()));
        } catch (Exception e) {
            Log.e("PushLogSys", "get meta push version code error: " + ao.a(e));
            return -1;
        }
    }

    public final void b(String str, String str2) {
        this.T = 0;
        c(str, str2);
    }

    public final void e() {
        for (int i = 0; i < 3; i++) {
            Log.i("PushLogSys", "try run push time is " + i);
            if (f()) {
                Log.i("PushLogSys", "run push once result is true");
                return;
            }
        }
    }
}
