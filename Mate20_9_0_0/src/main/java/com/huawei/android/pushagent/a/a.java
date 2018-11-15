package com.huawei.android.pushagent.a;

import android.content.Context;
import android.provider.Settings.Secure;
import com.huawei.android.pushagent.a.a.b;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import java.util.Arrays;

public class a {
    private static Context appCtx;
    private static final int[] bn = new int[]{30, 31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 46, 48, 49, 50, 52, 53, 54, 103, 101, 102, 100, 104, 105};
    private static final int[] bo = new int[]{70, 71, 72, 73, 74, 75, 76};
    private static final int[] bp = new int[]{90, 91, 92};
    private static final int[] bq = new int[]{0, 1, 10, 11, 12, 13};
    private static final int[] br = new int[]{60, 61, 62, 65, 66};
    private static b bs;

    static {
        Arrays.sort(bq);
        Arrays.sort(bp);
        Arrays.sort(bn);
        Arrays.sort(bo);
        Arrays.sort(br);
    }

    public static void ht(Context context) {
        hs(new b(context));
    }

    private static void hs(Runnable runnable) {
        if (runnable == null) {
            c.eq("PushLog3413", "runnable is null, stop execute");
            return;
        }
        try {
            com.huawei.android.pushagent.utils.threadpool.a.cg(runnable);
        } catch (Exception e) {
            c.eq("PushLog3413", "fail to execute runnable!");
        }
    }

    public static void hw(boolean z) {
        hs(new c(z));
    }

    public static void hq(int i, String str) {
        hs(new d(i, str));
    }

    public static void hx(int i) {
        hq(i, "");
    }

    public static void hy() {
        hs(new e());
    }

    private static boolean hu(int i) {
        if (!hv(appCtx)) {
            return false;
        }
        if (Arrays.binarySearch(bq, i) >= 0) {
            if (1 == k.rh(appCtx).ri()) {
                return true;
            }
        } else if (Arrays.binarySearch(bp, i) >= 0) {
            if (1 == k.rh(appCtx).rj()) {
                return true;
            }
        } else if (Arrays.binarySearch(bn, i) >= 0) {
            if (1 == k.rh(appCtx).rk()) {
                return true;
            }
        } else if (Arrays.binarySearch(bo, i) >= 0) {
            if (1 == k.rh(appCtx).rl()) {
                return true;
            }
        } else if (Arrays.binarySearch(br, i) >= 0) {
            if (1 == k.rh(appCtx).rm()) {
                return true;
            }
        } else if (1 == k.rh(appCtx).rn()) {
            return true;
        }
        return false;
    }

    static boolean hv(Context context) {
        int i;
        boolean z;
        if (context != null) {
            try {
                i = Secure.getInt(context.getContentResolver(), "user_experience_involved", 0);
            } catch (Exception e) {
                c.er("PushLog3413", "getOpenUserExperience exception:" + e.toString());
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
        c.er("PushLog3413", "isUserExperienceInvolved: " + z);
        return z;
    }

    public static String hr(String... strArr) {
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
