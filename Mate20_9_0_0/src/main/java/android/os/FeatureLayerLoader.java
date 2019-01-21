package android.os;

import android.util.Log;

public class FeatureLayerLoader {
    private static final String TAG = "FeatureLayerLoader";
    boolean DEBUG = false;

    private native int nativeLoad(String str, ClassLoader classLoader);

    static {
        try {
            System.loadLibrary("hwfeature_loader");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "featurelayer_loader library not found!", e);
        }
    }

    public void loadFeature(ClassLoader classLoader, String libName) {
        if (libName == null || classLoader == null) {
            Log.w(TAG, "load feature failed!");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            String fullLibName = new StringBuilder();
            fullLibName.append("lib");
            fullLibName.append(libName);
            fullLibName.append(".so");
            fullLibName = fullLibName.toString();
            nativeLoad(fullLibName, classLoader);
            if (this.DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("after nativeLoad ");
                stringBuilder.append(fullLibName);
                Log.i(str, stringBuilder.toString());
            }
        } catch (UnsatisfiedLinkError e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("native load library[");
            stringBuilder.append(libName);
            stringBuilder.append("] not found!");
            Log.e(str, stringBuilder.toString(), e);
        }
    }
}
