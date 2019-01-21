package com.huawei.android.pushagent.b;

import android.content.Context;
import android.provider.Settings.Secure;
import com.huawei.android.pushagent.b.a.c;
import java.util.Arrays;

public class a {
    private static Context appCtx;
    private static final int[] ir = new int[]{30, 31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 46, 48, 49, 50, 52, 53, 54, 103, 101, 102, 100, 104, 105};
    private static final int[] is = new int[]{70, 71, 72, 73, 74, 75, 76};
    private static final int[] it = new int[]{90, 91, 92};
    private static final int[] iu = new int[]{0, 1, 10, 11, 12, 13};
    private static final int[] iv = new int[]{60, 61, 62, 65, 66};
    private static c iw;

    static {
        Arrays.sort(iu);
        Arrays.sort(it);
        Arrays.sort(ir);
        Arrays.sort(is);
        Arrays.sort(iv);
    }

    public static void abg(Context context) {
        abf(new b(context));
    }

    private static void abf(Runnable runnable) {
        if (runnable == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "runnable is null, stop execute");
            return;
        }
        try {
            com.huawei.android.pushagent.utils.threadpool.c.so(runnable);
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to execute runnable!");
        }
    }

    public static void abe(boolean z) {
        abf(new c(z));
    }

    public static void abc(int i, String str) {
        abf(new d(i, str));
    }

    public static void abd(int i) {
        abc(i, "");
    }

    public static void abj() {
        abf(new e());
    }

    private static boolean abh(int i) {
        if (!abi(appCtx)) {
            return false;
        }
        if (Arrays.binarySearch(iu, i) >= 0) {
            if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).gv()) {
                return true;
            }
        } else if (Arrays.binarySearch(it, i) >= 0) {
            if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).gt()) {
                return true;
            }
        } else if (Arrays.binarySearch(ir, i) >= 0) {
            if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).hb()) {
                return true;
            }
        } else if (Arrays.binarySearch(is, i) >= 0) {
            if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).gl()) {
                return true;
            }
        } else if (Arrays.binarySearch(iv, i) >= 0) {
            if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).hx()) {
                return true;
            }
        } else if (1 == com.huawei.android.pushagent.model.prefs.a.ff(appCtx).fx()) {
            return true;
        }
        return false;
    }

    static boolean abi(Context context) {
        int i;
        boolean z;
        if (context != null) {
            try {
                i = Secure.getInt(context.getContentResolver(), "user_experience_involved", 0);
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "getOpenUserExperience exception:" + e.toString());
                i = 0;
            }
        } else {
            i = 0;
        }
        if (i == 1) {
            z = true;
        } else {
            z = false;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "isUserExperienceInvolved: " + z);
        return z;
    }

    public static String abb(String... strArr) {
        if (strArr == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            if (i != 0) {
                stringBuilder.append("^");
            }
            stringBuilder.append(strArr[i]);
        }
        return stringBuilder.toString();
    }
}
