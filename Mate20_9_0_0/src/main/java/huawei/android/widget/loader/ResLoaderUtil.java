package huawei.android.widget.loader;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ResLoaderUtil {
    public static final String ARRAY = "array";
    public static final String COLOR = "color";
    public static final String DIMEN = "dimen";
    public static final String DRAWABLE = "drawable";
    public static final String ID = "id";
    public static final String LAYOUT = "layout";
    public static final String STAYLEABLE = "styleable";
    public static final String STRING = "string";
    public static final String TAG = "ResLoaderUtil";

    public static ResLoader getInstance() {
        return ResLoader.getInstance();
    }

    public static Resources getResources(Context context) {
        if (context != null) {
            return getInstance().getResources(context);
        }
        Log.w(TAG, "getResources: context can not be null!");
        return null;
    }

    public static int getColor(Context context, String colorResId) {
        if (context == null || colorResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getColor: context or colorResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", colorResId: ");
            stringBuilder.append(colorResId);
            Log.w(str, stringBuilder.toString());
            return 0;
        }
        return getResources(context).getColor(getInstance().getIdentifier(context, COLOR, colorResId));
    }

    public static int getDimensionPixelSize(Context context, String dimenResId) {
        if (context == null || dimenResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDimensionPixelSize: context or dimenResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", dimenResId: ");
            stringBuilder.append(dimenResId);
            Log.w(str, stringBuilder.toString());
            return 0;
        }
        return getResources(context).getDimensionPixelSize(getInstance().getIdentifier(context, DIMEN, dimenResId));
    }

    public static View getLayout(Context context, String layoutResId, ViewGroup root, boolean attachToRoot) {
        if (context == null || layoutResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLayout: context or layoutResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", layoutResId: ");
            stringBuilder.append(layoutResId);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        return LayoutInflater.from(context).inflate(getInstance().getIdentifier(context, LAYOUT, layoutResId), root, attachToRoot);
    }

    public static int getLayoutId(Context context, String layoutResId) {
        if (context != null && layoutResId != null) {
            return getInstance().getIdentifier(context, LAYOUT, layoutResId);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLayoutId: context or layoutResId can not be null! context: ");
        stringBuilder.append(context);
        stringBuilder.append(", layoutResId: ");
        stringBuilder.append(layoutResId);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    public static int getViewId(Context context, String viewResId) {
        if (context != null && viewResId != null) {
            return getInstance().getIdentifier(context, ID, viewResId);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getViewId: context or viewResId can not be null! context: ");
        stringBuilder.append(context);
        stringBuilder.append(", viewResId: ");
        stringBuilder.append(viewResId);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    public static int getDrawableId(Context context, String drawableResId) {
        if (context != null && drawableResId != null) {
            return getInstance().getIdentifier(context, DRAWABLE, drawableResId);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDrawableId: context or drawableResId can not be null! context: ");
        stringBuilder.append(context);
        stringBuilder.append(", drawableResId: ");
        stringBuilder.append(drawableResId);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    public static int getStyleableId(Context context, String styleableResId) {
        if (context != null && styleableResId != null) {
            return ResLoader.getInstance().getIdentifier(context, STAYLEABLE, styleableResId);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getStyleableId: context or styleableResId can not be null! context: ");
        stringBuilder.append(context);
        stringBuilder.append(", styleableResId: ");
        stringBuilder.append(styleableResId);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    public static String getString(Context context, String stringResId) {
        if (context == null || stringResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getString: context or stringResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", stringResId: ");
            stringBuilder.append(stringResId);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        return getResources(context).getString(getInstance().getIdentifier(context, STRING, stringResId));
    }

    public static String[] getStringArray(Context context, String stringArrayResId) {
        if (context == null || stringArrayResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getStringArray: context or stringArrayResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", stringArrayResId: ");
            stringBuilder.append(stringArrayResId);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        return getResources(context).getStringArray(getInstance().getIdentifier(context, ARRAY, stringArrayResId));
    }

    public static int[] getIntArray(Context context, String intArrayResId) {
        if (context == null || intArrayResId == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getIntArray: context or intArrayResId can not be null! context: ");
            stringBuilder.append(context);
            stringBuilder.append(", intArrayResId: ");
            stringBuilder.append(intArrayResId);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        return getResources(context).getIntArray(getInstance().getIdentifier(context, ARRAY, intArrayResId));
    }
}
