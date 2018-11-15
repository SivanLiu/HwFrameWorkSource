package android_maps_conflict_avoidance.com.google.googlenav;

import java.util.Vector;

public class StartupHelper {
    private static Vector startupCallbacksForBgThread = new Vector();
    private static Vector startupCallbacksForUiThread = new Vector();

    public static void addPostStartupBgCallback(Runnable runnable) {
        addPostStartupCallback(runnable, false);
    }

    /* JADX WARNING: Missing block: B:12:0x001a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void addPostStartupCallback(Runnable runnable, boolean needsUiThread) {
        synchronized (StartupHelper.class) {
            if (startupCallbacksForUiThread == null) {
                runnable.run();
            } else if (needsUiThread) {
                startupCallbacksForUiThread.addElement(runnable);
            } else {
                startupCallbacksForBgThread.addElement(runnable);
            }
        }
    }
}
