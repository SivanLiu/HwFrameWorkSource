package android.support.v4.util;

import android.os.Build.VERSION;
import android.support.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public class ObjectsCompat {
    private ObjectsCompat() {
    }

    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        if (VERSION.SDK_INT >= 19) {
            return Objects.equals(a, b);
        }
        boolean z = a == b || (a != null && a.equals(b));
        return z;
    }

    public static int hashCode(@Nullable Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static int hash(@Nullable Object... values) {
        if (VERSION.SDK_INT >= 19) {
            return Objects.hash(values);
        }
        return Arrays.hashCode(values);
    }
}
