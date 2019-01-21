package android.util;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;

public class FeatureFlagUtils {
    private static final Map<String, String> DEFAULT_FLAGS = new HashMap();
    public static final String FFLAG_OVERRIDE_PREFIX = "sys.fflag.override.";
    public static final String FFLAG_PREFIX = "sys.fflag.";

    static {
        DEFAULT_FLAGS.put("settings_battery_display_app_list", "false");
        DEFAULT_FLAGS.put("settings_zone_picker_v2", "true");
        DEFAULT_FLAGS.put("settings_about_phone_v2", "true");
        DEFAULT_FLAGS.put("settings_bluetooth_while_driving", "false");
        DEFAULT_FLAGS.put("settings_data_usage_v2", "true");
        DEFAULT_FLAGS.put("settings_audio_switcher", "true");
        DEFAULT_FLAGS.put("settings_systemui_theme", "true");
    }

    public static boolean isEnabled(Context context, String feature) {
        String value;
        if (context != null) {
            value = Global.getString(context.getContentResolver(), feature);
            if (!TextUtils.isEmpty(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FFLAG_OVERRIDE_PREFIX);
        stringBuilder.append(feature);
        value = SystemProperties.get(stringBuilder.toString());
        if (TextUtils.isEmpty(value)) {
            return Boolean.parseBoolean((String) getAllFeatureFlags().get(feature));
        }
        return Boolean.parseBoolean(value);
    }

    public static void setEnabled(Context context, String feature, boolean enabled) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FFLAG_OVERRIDE_PREFIX);
        stringBuilder.append(feature);
        SystemProperties.set(stringBuilder.toString(), enabled ? "true" : "false");
    }

    public static Map<String, String> getAllFeatureFlags() {
        return DEFAULT_FLAGS;
    }
}
