package android.content.pm;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FallbackCategoryProvider {
    private static final String TAG = "FallbackCategoryProvider";
    private static final ArrayMap<String, Integer> sFallbacks = new ArrayMap();

    public static void loadFallbacks() {
        Throwable th;
        Exception e;
        Throwable th2 = null;
        sFallbacks.clear();
        if (SystemProperties.getBoolean("fw.ignore_fb_categories", false)) {
            Log.d(TAG, "Ignoring fallback categories");
            return;
        }
        AssetManager assets = new AssetManager();
        assets.addAssetPath("/system/framework/framework-res.apk");
        BufferedReader bufferedReader = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new Resources(assets, null, null).openRawResource(17825796)));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    } else if (line.charAt(0) != '#') {
                        String[] split = line.split(",");
                        if (split.length == 2) {
                            sFallbacks.put(split[0], Integer.valueOf(Integer.parseInt(split[1])));
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bufferedReader = reader;
                }
            }
            Log.d(TAG, "Found " + sFallbacks.size() + " fallback categories");
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th2 = th4;
                }
            }
            if (th2 != null) {
                try {
                    throw th2;
                } catch (IOException e2) {
                    e = e2;
                }
            }
        } catch (Throwable th5) {
            th = th5;
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable th6) {
                    if (th2 == null) {
                        th2 = th6;
                    } else if (th2 != th6) {
                        th2.addSuppressed(th6);
                    }
                }
            }
            if (th2 != null) {
                try {
                    throw th2;
                } catch (IOException e3) {
                    e = e3;
                }
            } else {
                throw th;
            }
        }
        Log.w(TAG, "Failed to read fallback categories", e);
    }

    public static int getFallbackCategory(String packageName) {
        return ((Integer) sFallbacks.getOrDefault(packageName, Integer.valueOf(-1))).intValue();
    }
}
