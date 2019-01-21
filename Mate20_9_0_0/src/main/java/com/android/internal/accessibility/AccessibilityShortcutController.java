package com.android.internal.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import java.util.Collections;
import java.util.Map;

public class AccessibilityShortcutController {
    public static final ComponentName COLOR_INVERSION_COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "ColorInversion");
    public static final ComponentName DALTONIZER_COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "Daltonizer");
    private static final String TAG = "AccessibilityShortcutController";
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(11).build();
    private static Map<ComponentName, ToggleableFrameworkFeatureInfo> sFrameworkShortcutFeaturesMap;
    private AlertDialog mAlertDialog;
    private final Context mContext;
    private boolean mEnabledOnLockScreen;
    public FrameworkObjectProvider mFrameworkObjectProvider = new FrameworkObjectProvider();
    private boolean mIsShortcutEnabled;
    private int mUserId;

    public static class FrameworkObjectProvider {
        public AccessibilityManager getAccessibilityManagerInstance(Context context) {
            return AccessibilityManager.getInstance(context);
        }

        public AlertDialog.Builder getAlertDialogBuilder(Context context) {
            return new AlertDialog.Builder(context);
        }

        public Toast makeToastFromText(Context context, CharSequence charSequence, int duration) {
            return Toast.makeText(context, charSequence, duration);
        }

        public Context getSystemUiContext() {
            return ActivityThread.currentActivityThread().getSystemUiContext();
        }
    }

    public static class ToggleableFrameworkFeatureInfo {
        private int mIconDrawableId;
        private final int mLabelStringResourceId;
        private final String mSettingKey;
        private final String mSettingOffValue;
        private final String mSettingOnValue;

        ToggleableFrameworkFeatureInfo(String settingKey, String settingOnValue, String settingOffValue, int labelStringResourceId) {
            this.mSettingKey = settingKey;
            this.mSettingOnValue = settingOnValue;
            this.mSettingOffValue = settingOffValue;
            this.mLabelStringResourceId = labelStringResourceId;
        }

        public String getSettingKey() {
            return this.mSettingKey;
        }

        public String getSettingOnValue() {
            return this.mSettingOnValue;
        }

        public String getSettingOffValue() {
            return this.mSettingOffValue;
        }

        public String getLabel(Context context) {
            return context.getString(this.mLabelStringResourceId);
        }
    }

    public static String getTargetServiceComponentNameString(Context context, int userId) {
        String currentShortcutServiceId = Secure.getStringForUser(context.getContentResolver(), "accessibility_shortcut_target_service", userId);
        if (currentShortcutServiceId != null) {
            return currentShortcutServiceId;
        }
        return context.getString(R.string.config_defaultAccessibilityService);
    }

    public static Map<ComponentName, ToggleableFrameworkFeatureInfo> getFrameworkShortcutFeaturesMap() {
        if (sFrameworkShortcutFeaturesMap == null) {
            Map<ComponentName, ToggleableFrameworkFeatureInfo> featuresMap = new ArrayMap(2);
            featuresMap.put(COLOR_INVERSION_COMPONENT_NAME, new ToggleableFrameworkFeatureInfo("accessibility_display_inversion_enabled", "1", "0", R.string.color_inversion_feature_name));
            featuresMap.put(DALTONIZER_COMPONENT_NAME, new ToggleableFrameworkFeatureInfo("accessibility_display_daltonizer_enabled", "1", "0", R.string.color_correction_feature_name));
            sFrameworkShortcutFeaturesMap = Collections.unmodifiableMap(featuresMap);
        }
        return sFrameworkShortcutFeaturesMap;
    }

    public AccessibilityShortcutController(Context context, Handler handler, int initialUserId) {
        this.mContext = context;
        this.mUserId = initialUserId;
        ContentObserver co = new ContentObserver(handler) {
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if (userId == AccessibilityShortcutController.this.mUserId) {
                    AccessibilityShortcutController.this.onSettingsChanged();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_shortcut_target_service"), false, co, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_shortcut_enabled"), false, co, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_shortcut_on_lock_screen"), false, co, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_shortcut_dialog_shown"), false, co, -1);
        setCurrentUser(this.mUserId);
    }

    public void setCurrentUser(int currentUserId) {
        this.mUserId = currentUserId;
        onSettingsChanged();
    }

    public boolean isAccessibilityShortcutAvailable(boolean phoneLocked) {
        return this.mIsShortcutEnabled && (!phoneLocked || this.mEnabledOnLockScreen);
    }

    public void onSettingsChanged() {
        boolean z = true;
        boolean haveValidService = TextUtils.isEmpty(getTargetServiceComponentNameString(this.mContext, this.mUserId)) ^ true;
        ContentResolver cr = this.mContext.getContentResolver();
        boolean enabled = Secure.getIntForUser(cr, "accessibility_shortcut_enabled", 1, this.mUserId) == 1;
        this.mEnabledOnLockScreen = Secure.getIntForUser(cr, "accessibility_shortcut_on_lock_screen", Secure.getIntForUser(cr, "accessibility_shortcut_dialog_shown", 0, this.mUserId), this.mUserId) == 1;
        if (!(enabled && haveValidService)) {
            z = false;
        }
        this.mIsShortcutEnabled = z;
    }

    public void performAccessibilityShortcut() {
        int audioAttributesUsage;
        Slog.d(TAG, "Accessibility shortcut activated");
        ContentResolver cr = this.mContext.getContentResolver();
        int userId = ActivityManager.getCurrentUser();
        int dialogAlreadyShown = Secure.getIntForUser(cr, "accessibility_shortcut_dialog_shown", 0, userId);
        if (hasFeatureLeanback()) {
            audioAttributesUsage = 11;
        } else {
            audioAttributesUsage = 10;
        }
        Ringtone tone = RingtoneManager.getRingtone(this.mContext, System.DEFAULT_NOTIFICATION_URI);
        if (tone != null) {
            tone.setAudioAttributes(new Builder().setUsage(audioAttributesUsage).build());
            tone.play();
        }
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ArrayUtils.convertToLongArray(this.mContext.getResources().getIntArray(R.array.config_longPressVibePattern)), -1, VIBRATION_ATTRIBUTES);
        }
        if (dialogAlreadyShown == 0) {
            this.mAlertDialog = createShortcutWarningDialog(userId);
            if (this.mAlertDialog != null) {
                Window w = this.mAlertDialog.getWindow();
                LayoutParams attr = w.getAttributes();
                attr.type = 2009;
                w.setAttributes(attr);
                this.mAlertDialog.show();
                Secure.putIntForUser(cr, "accessibility_shortcut_dialog_shown", 1, userId);
            } else {
                return;
            }
        }
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
        if (getShortcutFeatureDescription(false) == null) {
            Slog.e(TAG, "Accessibility shortcut set to invalid service");
            return;
        }
        AccessibilityServiceInfo serviceInfo = getInfoForTargetService();
        if (serviceInfo != null) {
            int i;
            String toastMessageFormatString = this.mContext;
            if (isServiceEnabled(serviceInfo)) {
                i = R.string.accessibility_shortcut_disabling_service;
            } else {
                i = R.string.accessibility_shortcut_enabling_service;
            }
            Toast warningToast = this.mFrameworkObjectProvider.makeToastFromText(this.mContext, String.format(toastMessageFormatString.getString(i), new Object[]{serviceName}), 1);
            LayoutParams windowParams = warningToast.getWindowParams();
            windowParams.privateFlags |= 16;
            warningToast.show();
        }
        this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).performAccessibilityShortcut();
    }

    private AlertDialog createShortcutWarningDialog(int userId) {
        if (getShortcutFeatureDescription(true) == null) {
            return null;
        }
        return new AlertDialog.Builder(this.mFrameworkObjectProvider.getSystemUiContext(), this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null)).setTitle((int) R.string.accessibility_shortcut_warning_dialog_title).setMessage(String.format(this.mContext.getString(33685528), new Object[]{Integer.valueOf(3), serviceDescription})).setCancelable(false).setPositiveButton((int) R.string.leave_accessibility_shortcut_on, null).setNegativeButton((int) R.string.disable_accessibility_shortcut, new -$$Lambda$AccessibilityShortcutController$2NcDVJHkpsPbwr45v1_NfIM8row(this, userId)).setOnCancelListener(new -$$Lambda$AccessibilityShortcutController$T96D356-n5VObNOonEIYV8s83Fc(this, userId)).create();
    }

    private AccessibilityServiceInfo getInfoForTargetService() {
        String currentShortcutServiceString = getTargetServiceComponentNameString(this.mContext, -2);
        if (currentShortcutServiceString == null) {
            return null;
        }
        return this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getInstalledServiceInfoWithComponentName(ComponentName.unflattenFromString(currentShortcutServiceString));
    }

    private String getShortcutFeatureDescription(boolean includeSummary) {
        String currentShortcutServiceString = getTargetServiceComponentNameString(this.mContext, -2);
        if (currentShortcutServiceString == null) {
            return null;
        }
        ComponentName targetComponentName = ComponentName.unflattenFromString(currentShortcutServiceString);
        ToggleableFrameworkFeatureInfo frameworkFeatureInfo = (ToggleableFrameworkFeatureInfo) getFrameworkShortcutFeaturesMap().get(targetComponentName);
        if (frameworkFeatureInfo != null) {
            return frameworkFeatureInfo.getLabel(this.mContext);
        }
        AccessibilityServiceInfo serviceInfo = this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getInstalledServiceInfoWithComponentName(targetComponentName);
        if (serviceInfo == null) {
            return null;
        }
        PackageManager pm = this.mContext.getPackageManager();
        String label = serviceInfo.getResolveInfo().loadLabel(pm).toString();
        CharSequence summary = serviceInfo.loadSummary(pm);
        if (!includeSummary || TextUtils.isEmpty(summary)) {
            return label;
        }
        return String.format("%s\n%s", new Object[]{label, summary});
    }

    private boolean isServiceEnabled(AccessibilityServiceInfo serviceInfo) {
        return this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getEnabledAccessibilityServiceList(-1).contains(serviceInfo);
    }

    private boolean hasFeatureLeanback() {
        return this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
