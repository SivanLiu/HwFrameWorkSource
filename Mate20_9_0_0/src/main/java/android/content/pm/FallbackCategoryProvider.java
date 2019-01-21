package android.content.pm;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FallbackCategoryProvider {
    private static final String TAG = "FallbackCategoryProvider";
    private static final ArrayMap<String, Integer> sFallbacks = new ArrayMap();

    /* JADX WARNING: Removed duplicated region for block: B:33:0x009e A:{ExcHandler: IOException | NumberFormatException (r1_5 'e' java.lang.Exception), Splitter:B:5:0x0026} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:28:0x0095, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            r3.addSuppressed(r5);
     */
    /* JADX WARNING: Missing block: B:33:0x009e, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:0x009f, code skipped:
            android.util.Log.w(TAG, "Failed to read fallback categories", r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void loadFallbacks() {
        Throwable th;
        sFallbacks.clear();
        if (SystemProperties.getBoolean("fw.ignore_fb_categories", false)) {
            Log.d(TAG, "Ignoring fallback categories");
            return;
        }
        AssetManager assets = new AssetManager();
        assets.addAssetPath("/system/framework/framework-res.apk");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new Resources(assets, null, null).openRawResource(R.raw.fallback_categories)));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                } else if (line.charAt(0) != '#') {
                    String[] split = line.split(",");
                    if (split.length == 2) {
                        sFallbacks.put(split[0], Integer.valueOf(Integer.parseInt(split[1])));
                    }
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found ");
            stringBuilder.append(sFallbacks.size());
            stringBuilder.append(" fallback categories");
            Log.d(str, stringBuilder.toString());
            reader.close();
        } catch (IOException | NumberFormatException e) {
        } catch (Throwable th2) {
            if (th != null) {
                reader.close();
            } else {
                reader.close();
            }
        }
    }

    public static int getFallbackCategory(String packageName) {
        return ((Integer) sFallbacks.getOrDefault(packageName, Integer.valueOf(-1))).intValue();
    }
}
