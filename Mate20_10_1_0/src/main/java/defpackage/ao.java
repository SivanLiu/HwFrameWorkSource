package defpackage;

import com.huawei.android.feature.BuildConfig;

/* renamed from: ao  reason: default package */
public final class ao {
    public static String a(Throwable th) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(th.getClass().getName()).append('\n');
        StackTraceElement[] stackTrace = th.getStackTrace();
        if (stackTrace == null) {
            return BuildConfig.FLAVOR;
        }
        for (StackTraceElement stackTraceElement : stackTrace) {
            sb.append(stackTraceElement.toString()).append('\n');
        }
        return sb.toString();
    }
}
