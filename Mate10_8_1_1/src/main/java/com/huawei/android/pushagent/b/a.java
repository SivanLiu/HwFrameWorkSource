package com.huawei.android.pushagent.b;

import android.content.Context;
import android.provider.Settings.Secure;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import java.util.Arrays;

public class a {
    private static Context appCtx;
    private static final int[] im = new int[]{30, 31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 46, 48, 49, 50, 52, 53, 54, 103, 101, 102, 100, 104, 105};
    private static final int[] in = new int[]{70, 71, 72, 73, 74, 75};
    private static final int[] io = new int[]{90, 91, 92};
    private static final int[] ip = new int[]{0, 1, 10, 11, 12, 13};
    private static final int[] iq = new int[]{60, 61, 62, 65, 66};
    private static com.huawei.android.pushagent.b.a.a ir;

    static {
        Arrays.sort(ip);
        Arrays.sort(io);
        Arrays.sort(im);
        Arrays.sort(in);
        Arrays.sort(iq);
    }

    public static void aam(Context context) {
        aao(new b(context));
    }

    private static void aao(Runnable runnable) {
        if (runnable == null) {
            b.y("PushLog2976", "runnable is null, stop execute");
            return;
        }
        try {
            com.huawei.android.pushagent.utils.threadpool.b.e(runnable);
        } catch (Exception e) {
            b.y("PushLog2976", "fail to execute runnable!");
        }
    }

    public static void aar(boolean z) {
        aao(new c(z));
    }

    public static void aaj(int i, String str) {
        aao(new d(i, str));
    }

    public static void aak(int i) {
        aaj(i, "");
    }

    public static void aan() {
        aao(new e());
    }

    private static boolean aap(int i) {
        if (!aaq(appCtx)) {
            return false;
        }
        if (Arrays.binarySearch(ip, i) >= 0) {
            if (1 == i.mj(appCtx).of()) {
                return true;
            }
        } else if (Arrays.binarySearch(io, i) >= 0) {
            if (1 == i.mj(appCtx).og()) {
                return true;
            }
        } else if (Arrays.binarySearch(im, i) >= 0) {
            if (1 == i.mj(appCtx).oh()) {
                return true;
            }
        } else if (Arrays.binarySearch(in, i) >= 0) {
            if (1 == i.mj(appCtx).oi()) {
                return true;
            }
        } else if (Arrays.binarySearch(iq, i) >= 0) {
            if (1 == i.mj(appCtx).oj()) {
                return true;
            }
        } else if (1 == i.mj(appCtx).ok()) {
            return true;
        }
        return false;
    }

    static boolean aaq(Context context) {
        int i;
        boolean z;
        if (context != null) {
            try {
                i = Secure.getInt(context.getContentResolver(), "user_experience_involved", 0);
            } catch (Exception e) {
                b.x("PushLog2976", "getOpenUserExperience exception:" + e.toString());
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
        b.x("PushLog2976", "isUserExperienceInvolved: " + z);
        return z;
    }

    public static String aal(String... strArr) {
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
