package android.support.v4.app;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import java.lang.reflect.Method;

public final class BundleCompat {

    static class BundleCompatBaseImpl {
        private static final String TAG = "BundleCompatBaseImpl";
        private static Method sGetIBinderMethod;
        private static boolean sGetIBinderMethodFetched;
        private static Method sPutIBinderMethod;
        private static boolean sPutIBinderMethodFetched;

        private BundleCompatBaseImpl() {
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x0038 A:{Splitter: B:9:0x002b, ExcHandler: java.lang.reflect.InvocationTargetException (r0_9 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x0038 A:{Splitter: B:9:0x002b, ExcHandler: java.lang.reflect.InvocationTargetException (r0_9 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:12:0x0038, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:13:0x0039, code:
            android.util.Log.i(TAG, "Failed to invoke getIBinder via reflection", r0);
            sGetIBinderMethod = null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static IBinder getBinder(Bundle bundle, String key) {
            if (!sGetIBinderMethodFetched) {
                try {
                    sGetIBinderMethod = Bundle.class.getMethod("getIBinder", new Class[]{String.class});
                    sGetIBinderMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve getIBinder method", e);
                }
                sGetIBinderMethodFetched = true;
            }
            if (sGetIBinderMethod != null) {
                try {
                    return (IBinder) sGetIBinderMethod.invoke(bundle, new Object[]{key});
                } catch (Exception e2) {
                }
            }
            return null;
        }

        /* JADX WARNING: Removed duplicated region for block: B:11:0x003b A:{Splitter: B:9:0x002f, ExcHandler: java.lang.reflect.InvocationTargetException (r0_7 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:11:0x003b A:{Splitter: B:9:0x002f, ExcHandler: java.lang.reflect.InvocationTargetException (r0_7 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:11:0x003b, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:12:0x003c, code:
            android.util.Log.i(TAG, "Failed to invoke putIBinder via reflection", r0);
            sPutIBinderMethod = null;
     */
        /* JADX WARNING: Missing block: B:13:?, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static void putBinder(Bundle bundle, String key, IBinder binder) {
            if (!sPutIBinderMethodFetched) {
                try {
                    sPutIBinderMethod = Bundle.class.getMethod("putIBinder", new Class[]{String.class, IBinder.class});
                    sPutIBinderMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve putIBinder method", e);
                }
                sPutIBinderMethodFetched = true;
            }
            if (sPutIBinderMethod != null) {
                try {
                    sPutIBinderMethod.invoke(bundle, new Object[]{key, binder});
                } catch (Exception e2) {
                }
            }
        }
    }

    private BundleCompat() {
    }

    @Nullable
    public static IBinder getBinder(@NonNull Bundle bundle, @Nullable String key) {
        if (VERSION.SDK_INT >= 18) {
            return bundle.getBinder(key);
        }
        return BundleCompatBaseImpl.getBinder(bundle, key);
    }

    public static void putBinder(@NonNull Bundle bundle, @Nullable String key, @Nullable IBinder binder) {
        if (VERSION.SDK_INT >= 18) {
            bundle.putBinder(key, binder);
        } else {
            BundleCompatBaseImpl.putBinder(bundle, key, binder);
        }
    }
}
