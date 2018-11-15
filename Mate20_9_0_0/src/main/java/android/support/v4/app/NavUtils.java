package android.support.v4.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public final class NavUtils {
    public static final String PARENT_ACTIVITY = "android.support.PARENT_ACTIVITY";
    private static final String TAG = "NavUtils";

    public static boolean shouldUpRecreateTask(@NonNull Activity sourceActivity, @NonNull Intent targetIntent) {
        if (VERSION.SDK_INT >= 16) {
            return sourceActivity.shouldUpRecreateTask(targetIntent);
        }
        String action = sourceActivity.getIntent().getAction();
        boolean z = (action == null || action.equals("android.intent.action.MAIN")) ? false : true;
        return z;
    }

    public static void navigateUpFromSameTask(@NonNull Activity sourceActivity) {
        Intent upIntent = getParentActivityIntent(sourceActivity);
        if (upIntent != null) {
            navigateUpTo(sourceActivity, upIntent);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Activity ");
        stringBuilder.append(sourceActivity.getClass().getSimpleName());
        stringBuilder.append(" does not have a parent activity name specified.");
        stringBuilder.append(" (Did you forget to add the android.support.PARENT_ACTIVITY <meta-data> ");
        stringBuilder.append(" element in your manifest?)");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static void navigateUpTo(@NonNull Activity sourceActivity, @NonNull Intent upIntent) {
        if (VERSION.SDK_INT >= 16) {
            sourceActivity.navigateUpTo(upIntent);
            return;
        }
        upIntent.addFlags(67108864);
        sourceActivity.startActivity(upIntent);
        sourceActivity.finish();
    }

    @Nullable
    public static Intent getParentActivityIntent(@NonNull Activity sourceActivity) {
        if (VERSION.SDK_INT >= 16) {
            Intent result = sourceActivity.getParentActivityIntent();
            if (result != null) {
                return result;
            }
        }
        String parentName = getParentActivityName(sourceActivity);
        if (parentName == null) {
            return null;
        }
        ComponentName target = new ComponentName(sourceActivity, parentName);
        try {
            Intent makeMainActivity;
            if (getParentActivityName(sourceActivity, target) == null) {
                makeMainActivity = Intent.makeMainActivity(target);
            } else {
                makeMainActivity = new Intent().setComponent(target);
            }
            return makeMainActivity;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getParentActivityIntent: bad parentActivityName '");
            stringBuilder.append(parentName);
            stringBuilder.append("' in manifest");
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    @Nullable
    public static Intent getParentActivityIntent(@NonNull Context context, @NonNull Class<?> sourceActivityClass) throws NameNotFoundException {
        String parentActivity = getParentActivityName(context, new ComponentName(context, sourceActivityClass));
        if (parentActivity == null) {
            return null;
        }
        Intent parentIntent;
        ComponentName target = new ComponentName(context, parentActivity);
        if (getParentActivityName(context, target) == null) {
            parentIntent = Intent.makeMainActivity(target);
        } else {
            parentIntent = new Intent().setComponent(target);
        }
        return parentIntent;
    }

    @Nullable
    public static Intent getParentActivityIntent(@NonNull Context context, @NonNull ComponentName componentName) throws NameNotFoundException {
        String parentActivity = getParentActivityName(context, componentName);
        if (parentActivity == null) {
            return null;
        }
        Intent parentIntent;
        ComponentName target = new ComponentName(componentName.getPackageName(), parentActivity);
        if (getParentActivityName(context, target) == null) {
            parentIntent = Intent.makeMainActivity(target);
        } else {
            parentIntent = new Intent().setComponent(target);
        }
        return parentIntent;
    }

    @Nullable
    public static String getParentActivityName(@NonNull Activity sourceActivity) {
        try {
            return getParentActivityName(sourceActivity, sourceActivity.getComponentName());
        } catch (NameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nullable
    public static String getParentActivityName(@NonNull Context context, @NonNull ComponentName componentName) throws NameNotFoundException {
        String result;
        ActivityInfo info = context.getPackageManager().getActivityInfo(componentName, 128);
        if (VERSION.SDK_INT >= 16) {
            result = info.parentActivityName;
            if (result != null) {
                return result;
            }
        }
        if (info.metaData == null) {
            return null;
        }
        result = info.metaData.getString(PARENT_ACTIVITY);
        if (result == null) {
            return null;
        }
        if (result.charAt(0) == '.') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(context.getPackageName());
            stringBuilder.append(result);
            result = stringBuilder.toString();
        }
        return result;
    }

    private NavUtils() {
    }
}
