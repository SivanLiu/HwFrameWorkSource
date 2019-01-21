package huawei.com.android.internal.policy;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import org.json.JSONObject;

public class HiTouchSensor {
    private static final String CALL_PACKAGE_NAME = "packageName";
    private static final String GRESTURE = "gresture";
    public static final int GRESTURE_DOUBLE_FINGER = 0;
    public static final int GRESTURE_PEN_LONG_TOUCH = 1;
    private static final String HITOUCH_PKG_NAME = "com.huawei.hitouch";
    private static final String HITOUCH_SWITCH = "hitouch_enabled";
    private static final String INCALLUI_PKG_NAME = "com.android.incallui";
    private static final String INTENT_PACKAGE_NAME = "pkgName";
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", "default"));
    private static final String MAIN_TOUCH_POINT_X = "x";
    private static final String MAIN_TOUCH_POINT_Y = "y";
    private static final String METHOD_ISTOUCHEFFECTIVE_HIACTION = "isTouchEffective";
    private static final String MULTI_WINDOW_MODE = "split_screen_mode";
    private static final String SECOND_TOUCH_POINT_X = "x1";
    private static final String SECOND_TOUCH_POINT_Y = "y1";
    private static final int SECUREIME_POPUP = 1;
    private static final String SECUREIME_STATUS = "secure_input_status";
    private static final int SETTINGS_SWITCH_OFF = 0;
    private static final int SETTINGS_SWITCH_ON = 1;
    private static final String TAG = "HiTouch_HiTouchSensor";
    private static final String TALK_BACK_PACKAGE = "com.google.android.marvin.talkback";
    private static final String TALK_BACK_SERVICE = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String URI_HIACTION_MANAGER_PROVIDER = "content://com.huawei.hiaction.provider.HiActionManagerProvider";
    private static final String USER_GUIDE_SETUP_FLAG = "device_provisioned";
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private ContentObserver accessibilitySwitchObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            HiTouchSensor.this.updateAccessibilityStatus();
        }
    };
    private boolean isAccessibilityEnabled = false;
    private boolean isDeviceProvisionedChecked = false;
    private boolean isHiTouchEnabled = true;
    private boolean isInMultiWindowMode = false;
    private boolean isLandscapeOrient = false;
    private boolean isTouchEffective_HiAction = true;
    private Context mContext;
    private Context mContextActivity;
    private ContentObserver settingsHiTouchSwitchObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            HiTouchSensor.this.updateSwitchStatus();
        }
    };

    public HiTouchSensor(Context contextActivity) {
        this.mContextActivity = contextActivity;
        this.mContext = contextActivity;
    }

    public HiTouchSensor(Context context, Context contextActivity) {
        this.mContextActivity = contextActivity;
        this.mContext = context;
    }

    public boolean getStatus() {
        updateSwitchStatus();
        checkMultiWindowModeStatus();
        this.isTouchEffective_HiAction = checkEffectiveStatusFromHiAction(this.mContextActivity.getPackageName());
        return (!this.isHiTouchEnabled || isSecurityIMEPopup() || this.isAccessibilityEnabled || this.isLandscapeOrient || this.isInMultiWindowMode || !this.isTouchEffective_HiAction) ? false : true;
    }

    private boolean isLauncherApp() {
        String pkgName = this.mContext.getPackageName();
        if (pkgName == null) {
            Log.i(TAG, "Can't get package name info, enable textboom by default");
            return false;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        ResolveInfo res = this.mContext.getPackageManager().resolveActivity(intent, 0);
        if (res == null) {
            Log.i(TAG, "ResolveInfo is null.");
            return false;
        } else if (res.activityInfo == null) {
            Log.i(TAG, "ActivityInfo is null.");
            return false;
        } else if (!pkgName.equals(res.activityInfo.packageName)) {
            return false;
        } else {
            Log.i(TAG, "HiTouch restricted: is Launcher App");
            return true;
        }
    }

    private boolean matchPackage(String pkgName) {
        if (!pkgName.equals(this.mContext.getPackageName())) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HiTouch restricted: match package ");
        stringBuilder.append(pkgName);
        Log.i(str, stringBuilder.toString());
        return true;
    }

    private boolean isComputerMode() {
        boolean z = false;
        try {
            Class<?> hwPCUtils = Class.forName("android.util.HwPCUtils");
            if (hwPCUtils != null) {
                Boolean isInPad = (Boolean) hwPCUtils.getMethod("enabledInPad", new Class[0]).invoke(hwPCUtils, new Object[0]);
                Boolean isInPcMode = (Boolean) hwPCUtils.getMethod("isPcCastMode", new Class[0]).invoke(hwPCUtils, new Object[0]);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enabledInPad = ");
                stringBuilder.append(isInPad);
                stringBuilder.append(",isPcCastMode = ");
                stringBuilder.append(isInPcMode);
                Log.i(str, stringBuilder.toString());
                if (isInPad.booleanValue() && isInPcMode.booleanValue()) {
                    z = true;
                }
                return z;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "fail to getIsInPCScreen ClassNotFoundException");
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "fail to getIsInPCScreen NoSuchMethodException");
        } catch (IllegalAccessException e3) {
            Log.e(TAG, "fail to getIsInPCScreen IllegalAccessException");
        } catch (InvocationTargetException e4) {
            Log.e(TAG, "fail to getIsInPCScreen InvocationTargetException");
        } catch (IllegalArgumentException e5) {
            Log.e(TAG, "fail to getIsInPCScreen IllegalArgumentException");
        }
        return false;
    }

    public boolean isUnsupportScence(int windowType) {
        boolean mHiTouchRestricted = false;
        boolean mHiTouchRestricted2 = windowType >= 1000;
        if (mHiTouchRestricted2) {
            Log.i(TAG, "HiTouch restricted: Sub windows restricted.");
            return mHiTouchRestricted2;
        }
        boolean z = isLauncherApp() || matchPackage(INCALLUI_PKG_NAME);
        mHiTouchRestricted2 = z;
        if (mHiTouchRestricted2) {
            return mHiTouchRestricted2;
        }
        z = isComputerMode() && IS_TABLET;
        mHiTouchRestricted2 = z;
        if (true == mHiTouchRestricted2) {
            Log.i(TAG, "HiTouch restricted: tablet in computer mode.");
            return mHiTouchRestricted2;
        }
        if (((KeyguardManager) this.mContext.getSystemService("keyguard")).isKeyguardLocked() && !WECHAT_PACKAGE_NAME.equals(this.mContextActivity.getPackageName())) {
            mHiTouchRestricted = true;
        }
        if (mHiTouchRestricted) {
            Log.i(TAG, "HiTouch restricted: Keyguard locked restricted.");
        }
        return mHiTouchRestricted;
    }

    private boolean isSecurityIMEPopup() {
        boolean z = true;
        if (1 != Global.getInt(this.mContext.getContentResolver(), SECUREIME_STATUS, 0)) {
            z = false;
        }
        boolean isSecrutiyIMEPopup = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSecrutiyIMEPopup:");
        stringBuilder.append(isSecrutiyIMEPopup);
        Log.i(str, stringBuilder.toString());
        return isSecrutiyIMEPopup;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        updateScreenOriatationStatus();
    }

    public boolean checkDeviceProvisioned() {
        if (this.isDeviceProvisionedChecked) {
            return true;
        }
        if (Secure.getInt(this.mContextActivity.getContentResolver(), USER_GUIDE_SETUP_FLAG, 1) == 0) {
            Log.v(TAG, "User guide setup is undergoing...");
            return false;
        }
        Log.v(TAG, "User setup is finished.");
        this.isDeviceProvisionedChecked = true;
        return true;
    }

    public void registerObserver() {
        updateSwitchStatus();
        updateAccessibilityStatus();
        updateScreenOriatationStatus();
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Global.getUriFor(HITOUCH_SWITCH), true, this.settingsHiTouchSwitchObserver);
        resolver.registerContentObserver(Secure.getUriFor("enabled_accessibility_services"), true, this.accessibilitySwitchObserver);
    }

    public void unregisterObserver() {
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.unregisterContentObserver(this.settingsHiTouchSwitchObserver);
        resolver.unregisterContentObserver(this.accessibilitySwitchObserver);
    }

    private void updateSwitchStatus() {
        if (1 == Global.getInt(this.mContextActivity.getContentResolver(), HITOUCH_SWITCH, 1)) {
            this.isHiTouchEnabled = true;
            Log.i(TAG, "HiTouch Setting Switch, ON");
            return;
        }
        this.isHiTouchEnabled = false;
        Log.i(TAG, "HiTouch Setting Switch, OFF");
    }

    private void updateAccessibilityStatus() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Secure.getInt(this.mContextActivity.getContentResolver(), "accessibility_enabled");
        } catch (SettingNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        if (accessibilityEnabled == 1) {
            String services = Secure.getString(this.mContextActivity.getContentResolver(), "enabled_accessibility_services");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("services:");
            stringBuilder.append(services);
            Log.d(str, stringBuilder.toString());
            if (services == null) {
                return;
            }
            if (TALK_BACK_SERVICE.equals(services)) {
                this.isAccessibilityEnabled = true;
                return;
            } else {
                this.isAccessibilityEnabled = false;
                return;
            }
        }
        this.isAccessibilityEnabled = false;
    }

    private boolean isClonedProfile(int userId) {
        boolean isClonedProfile = false;
        if (userId == 0) {
            return false;
        }
        try {
            isClonedProfile = UserManager.get(this.mContextActivity).getUserInfo(userId).isClonedProfile();
        } catch (Exception e) {
            Log.e(TAG, "get Cloned Profile failed.");
        }
        return isClonedProfile;
    }

    private void updateScreenOriatationStatus() {
        if (IS_TABLET) {
            this.isLandscapeOrient = false;
            return;
        }
        if (this.mContextActivity.getResources().getConfiguration().orientation == 2) {
            this.isLandscapeOrient = true;
            Log.i(TAG, "ORIENTATION_LANDSCAPE");
        } else {
            this.isLandscapeOrient = false;
        }
    }

    private void checkMultiWindowModeStatus() {
        if (1 == Secure.getInt(this.mContextActivity.getContentResolver(), MULTI_WINDOW_MODE, 0)) {
            this.isInMultiWindowMode = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Check MultiWindow Mode: ");
            stringBuilder.append(this.isInMultiWindowMode);
            Log.i(str, stringBuilder.toString());
            return;
        }
        this.isInMultiWindowMode = false;
    }

    private boolean checkEffectiveStatusFromHiAction(String pkgName) {
        String str;
        StringBuilder stringBuilder;
        Bundle result = null;
        Uri uri = Uri.parse(URI_HIACTION_MANAGER_PROVIDER);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("packageName", pkgName);
            try {
                if (!(getHitouchStartUpContext() == null || getHitouchStartUpContext().getContentResolver() == null)) {
                    result = getHitouchStartUpContext().getContentResolver().call(uri, METHOD_ISTOUCHEFFECTIVE_HIACTION, jsonObject.toString(), null);
                }
                if (result == null) {
                    return false;
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Checking pkgName: ");
                stringBuilder2.append(pkgName);
                stringBuilder2.append(" Checking result: ");
                stringBuilder2.append(result.getBoolean(METHOD_ISTOUCHEFFECTIVE_HIACTION));
                Log.i(str2, stringBuilder2.toString());
                return result.getBoolean(METHOD_ISTOUCHEFFECTIVE_HIACTION);
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Get error ");
                stringBuilder.append(e.getMessage());
                stringBuilder.append(" when calling isTouchEffective.");
                Log.e(str, stringBuilder.toString());
                return false;
            }
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Get error ");
            stringBuilder.append(e2.getMessage());
            stringBuilder.append(" when building jsonObject.");
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    private Context getHitouchStartUpContext() {
        if (!isClonedProfile(UserHandle.myUserId())) {
            return this.mContextActivity;
        }
        Log.i(TAG, "Cloned profile is true.");
        Context context = null;
        try {
            context = this.mContextActivity.createPackageContextAsUser(HITOUCH_PKG_NAME, 0, new UserHandle(0));
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getHitouchStartUpContext:");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        if (context != null) {
            return context;
        }
        Log.d(TAG, "context is null");
        return this.mContextActivity;
    }

    public void launchHiTouchService(float x1, float y1, int gressture) {
        launchHiTouchService(x1, y1, x1 > 0.0f ? x1 - 1.0f : x1 + 1.0f, y1 > 0.0f ? y1 - 1.0f : 1.0f + y1, gressture);
    }

    public void launchHiTouchService(float x1, float y1, float x2, float y2, int gressture) {
        Intent intent = new Intent();
        intent.setClassName(HITOUCH_PKG_NAME, "com.huawei.hitouch.HiTouchService");
        intent.putExtra(MAIN_TOUCH_POINT_X, x1);
        intent.putExtra(MAIN_TOUCH_POINT_Y, y1);
        intent.putExtra(SECOND_TOUCH_POINT_X, x2);
        intent.putExtra(SECOND_TOUCH_POINT_Y, y2);
        intent.putExtra("pkgName", this.mContextActivity.getPackageName());
        intent.putExtra(GRESTURE, gressture);
        Log.i(TAG, "launch HiTouch Service.");
        Context context = getHitouchStartUpContext();
        if (context != null) {
            try {
                context.startService(intent);
                return;
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Get error ");
                stringBuilder.append(e.getMessage());
                stringBuilder.append(" when starting service");
                Log.e(str, stringBuilder.toString());
                return;
            }
        }
        Log.i(TAG, "get context failed, do not launch Hitouch Service");
    }

    public void processStylusGessture(Context context, int windowType, float x, float y) {
        updateSwitchStatus();
        updateAccessibilityStatus();
        updateScreenOriatationStatus();
        Log.d(TAG, "check HiTouch");
        boolean isUnsupport = isUnsupportScence(windowType);
        boolean status = getStatus();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isUnsupport:");
        stringBuilder.append(isUnsupport);
        stringBuilder.append(", status:");
        stringBuilder.append(status);
        Log.d(str, stringBuilder.toString());
        if (isUnsupport || !status) {
            Log.d(TAG, "cannot start HiTouch!");
            return;
        }
        Log.d(TAG, "can start  HiTouch");
        launchHiTouchService(x, y, 1);
    }
}
