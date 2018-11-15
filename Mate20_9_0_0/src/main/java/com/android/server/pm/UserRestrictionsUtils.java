package com.android.server.pm;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.audio.AudioService;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.wm.WindowManagerService.H;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class UserRestrictionsUtils {
    private static final Set<String> DEFAULT_ENABLED_FOR_DEVICE_OWNERS = Sets.newArraySet(new String[]{"no_add_managed_profile"});
    private static final Set<String> DEFAULT_ENABLED_FOR_MANAGED_PROFILES = Sets.newArraySet(new String[]{"no_bluetooth_sharing"});
    private static final Set<String> DEVICE_OWNER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_user_switch"});
    private static final Set<String> GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_adjust_volume", "no_bluetooth_sharing", "no_config_date_time", "no_system_error_dialogs", "no_run_in_background", "no_unmute_microphone", "disallow_unmute_device"});
    private static final Set<String> IMMUTABLE_BY_OWNERS = Sets.newArraySet(new String[]{"no_record_audio", "no_wallpaper", "no_oem_unlock"});
    private static final Set<String> NON_PERSIST_USER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_record_audio"});
    private static final Set<String> PRIMARY_USER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_bluetooth", "no_usb_file_transfer", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_physical_media", "no_sms", "no_fun", "no_safe_boot", "no_create_windows", "no_data_roaming", "no_airplane_mode"});
    private static final Set<String> PROFILE_GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"ensure_verify_apps", "no_airplane_mode"});
    private static final String TAG = "UserRestrictionsUtils";
    public static final Set<String> USER_RESTRICTIONS = newSetWithUniqueCheck(new String[]{"no_config_wifi", "no_config_locale", "no_modify_accounts", "no_install_apps", "no_uninstall_apps", "no_share_location", "no_install_unknown_sources", "no_config_bluetooth", "no_bluetooth", "no_bluetooth_sharing", "no_usb_file_transfer", "no_config_credentials", "no_remove_user", "no_remove_managed_profile", "no_debugging_features", "no_config_vpn", "no_config_date_time", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_add_managed_profile", "ensure_verify_apps", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_control_apps", "no_physical_media", "no_unmute_microphone", "no_adjust_volume", "no_outgoing_calls", "no_sms", "no_fun", "no_create_windows", "no_system_error_dialogs", "no_cross_profile_copy_paste", "no_outgoing_beam", "no_wallpaper", "no_safe_boot", "allow_parent_profile_app_linking", "no_record_audio", "no_camera", "no_run_in_background", "no_data_roaming", "no_set_user_icon", "no_set_wallpaper", "no_oem_unlock", "disallow_unmute_device", "no_autofill", "no_user_switch", "no_unified_password", "no_config_location", "no_airplane_mode", "no_config_brightness", "no_sharing_into_profile", "no_ambient_display", "no_config_screen_timeout", "no_printing"});

    private UserRestrictionsUtils() {
    }

    private static Set<String> newSetWithUniqueCheck(String[] strings) {
        Set<String> ret = Sets.newArraySet(strings);
        Preconditions.checkState(ret.size() == strings.length);
        return ret;
    }

    public static boolean isValidRestriction(String restriction) {
        if (USER_RESTRICTIONS.contains(restriction)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown restriction: ");
        stringBuilder.append(restriction);
        Slog.e(str, stringBuilder.toString());
        return false;
    }

    public static void writeRestrictions(XmlSerializer serializer, Bundle restrictions, String tag) throws IOException {
        if (restrictions != null) {
            serializer.startTag(null, tag);
            for (String key : restrictions.keySet()) {
                if (!NON_PERSIST_USER_RESTRICTIONS.contains(key)) {
                    if (!USER_RESTRICTIONS.contains(key)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown user restriction detected: ");
                        stringBuilder.append(key);
                        Log.w(str, stringBuilder.toString());
                    } else if (restrictions.getBoolean(key)) {
                        serializer.attribute(null, key, "true");
                    }
                }
            }
            serializer.endTag(null, tag);
        }
    }

    public static void readRestrictions(XmlPullParser parser, Bundle restrictions) {
        restrictions.clear();
        for (String key : USER_RESTRICTIONS) {
            String value = parser.getAttributeValue(null, key);
            if (value != null) {
                restrictions.putBoolean(key, Boolean.parseBoolean(value));
            }
        }
    }

    public static Bundle readRestrictions(XmlPullParser parser) {
        Bundle result = new Bundle();
        readRestrictions(parser, result);
        return result;
    }

    public static Bundle nonNull(Bundle in) {
        return in != null ? in : new Bundle();
    }

    public static boolean isEmpty(Bundle in) {
        return in == null || in.size() == 0;
    }

    public static boolean contains(Bundle in, String restriction) {
        return in != null && in.getBoolean(restriction);
    }

    public static Bundle clone(Bundle in) {
        return in != null ? new Bundle(in) : new Bundle();
    }

    public static void merge(Bundle dest, Bundle in) {
        Preconditions.checkNotNull(dest);
        Preconditions.checkArgument(dest != in);
        if (in != null) {
            for (String key : in.keySet()) {
                if (in.getBoolean(key, false)) {
                    dest.putBoolean(key, true);
                }
            }
        }
    }

    public static Bundle mergeAll(SparseArray<Bundle> restrictions) {
        if (restrictions.size() == 0) {
            return null;
        }
        Bundle result = new Bundle();
        for (int i = 0; i < restrictions.size(); i++) {
            merge(result, (Bundle) restrictions.valueAt(i));
        }
        return result;
    }

    public static boolean canDeviceOwnerChange(String restriction) {
        return IMMUTABLE_BY_OWNERS.contains(restriction) ^ 1;
    }

    public static boolean canProfileOwnerChange(String restriction, int userId) {
        return (IMMUTABLE_BY_OWNERS.contains(restriction) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(restriction) || (userId != 0 && PRIMARY_USER_ONLY_RESTRICTIONS.contains(restriction))) ? false : true;
    }

    public static Set<String> getDefaultEnabledForDeviceOwner() {
        return DEFAULT_ENABLED_FOR_DEVICE_OWNERS;
    }

    public static Set<String> getDefaultEnabledForManagedProfiles() {
        return DEFAULT_ENABLED_FOR_MANAGED_PROFILES;
    }

    public static void sortToGlobalAndLocal(Bundle in, boolean isDeviceOwner, int cameraRestrictionScope, Bundle global, Bundle local) {
        if (cameraRestrictionScope == 2) {
            global.putBoolean("no_camera", true);
        } else if (cameraRestrictionScope == 1) {
            local.putBoolean("no_camera", true);
        }
        if (in != null && in.size() != 0) {
            for (String key : in.keySet()) {
                if (in.getBoolean(key)) {
                    if (isGlobal(isDeviceOwner, key)) {
                        global.putBoolean(key, true);
                    } else {
                        local.putBoolean(key, true);
                    }
                }
            }
        }
    }

    private static boolean isGlobal(boolean isDeviceOwner, String key) {
        return (isDeviceOwner && (PRIMARY_USER_ONLY_RESTRICTIONS.contains(key) || GLOBAL_RESTRICTIONS.contains(key))) || PROFILE_GLOBAL_RESTRICTIONS.contains(key) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(key);
    }

    public static boolean areEqual(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (isEmpty(a)) {
            return isEmpty(b);
        }
        if (isEmpty(b)) {
            return false;
        }
        for (String key : a.keySet()) {
            if (a.getBoolean(key) != b.getBoolean(key)) {
                return false;
            }
        }
        for (String key2 : b.keySet()) {
            if (a.getBoolean(key2) != b.getBoolean(key2)) {
                return false;
            }
        }
        return true;
    }

    public static void applyUserRestrictions(Context context, int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        for (String key : USER_RESTRICTIONS) {
            boolean newValue = newRestrictions.getBoolean(key);
            if (newValue != prevRestrictions.getBoolean(key)) {
                applyUserRestriction(context, userId, key, newValue);
            }
        }
    }

    private static void applyUserRestriction(Context context, int userId, String key, boolean newValue) {
        ContentResolver cr = context.getContentResolver();
        long id = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            boolean z2 = true;
            switch (key.hashCode()) {
                case -1475388515:
                    if (key.equals("no_ambient_display")) {
                        z = true;
                        break;
                    }
                    break;
                case -1315771401:
                    if (key.equals("ensure_verify_apps")) {
                        z = true;
                        break;
                    }
                    break;
                case -1082175374:
                    if (key.equals("no_airplane_mode")) {
                        z = true;
                        break;
                    }
                    break;
                case 387189153:
                    if (key.equals("no_install_unknown_sources")) {
                        z = true;
                        break;
                    }
                    break;
                case 721128150:
                    if (key.equals("no_run_in_background")) {
                        z = true;
                        break;
                    }
                    break;
                case 866097556:
                    if (key.equals("no_config_location")) {
                        z = true;
                        break;
                    }
                    break;
                case 928851522:
                    if (key.equals("no_data_roaming")) {
                        z = false;
                        break;
                    }
                    break;
                case 995816019:
                    if (key.equals("no_share_location")) {
                        z = true;
                        break;
                    }
                    break;
                case 1095593830:
                    if (key.equals("no_safe_boot")) {
                        z = true;
                        break;
                    }
                    break;
                case 1760762284:
                    if (key.equals("no_debugging_features")) {
                        z = true;
                        break;
                    }
                    break;
            }
            switch (z) {
                case false:
                    if (newValue) {
                        List<SubscriptionInfo> subscriptionInfoList = ((SubscriptionManager) context.getSystemService(SubscriptionManager.class)).getActiveSubscriptionInfoList();
                        if (subscriptionInfoList != null) {
                            for (SubscriptionInfo subInfo : subscriptionInfoList) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("data_roaming");
                                stringBuilder.append(subInfo.getSubscriptionId());
                                Global.putStringForUser(cr, stringBuilder.toString(), "0", userId);
                            }
                        }
                        Global.putStringForUser(cr, "data_roaming", "0", userId);
                        break;
                    }
                    break;
                case true:
                    if (newValue) {
                        Secure.putIntForUser(cr, "location_mode", 0, userId);
                        break;
                    }
                    break;
                case true:
                    if (newValue && userId == 0) {
                        Global.putStringForUser(cr, "adb_enabled", "0", userId);
                        break;
                    }
                case true:
                    if (newValue) {
                        Global.putStringForUser(context.getContentResolver(), "package_verifier_enable", "1", userId);
                        Global.putStringForUser(context.getContentResolver(), "verifier_verify_adb_installs", "1", userId);
                        break;
                    }
                    break;
                case true:
                    Secure.putIntForUser(cr, "install_non_market_apps", newValue ^ 1, userId);
                    break;
                case true:
                    if (!(!newValue || ActivityManager.getCurrentUser() == userId || userId == 0)) {
                        ActivityManager.getService().stopUser(userId, false, null);
                        break;
                    }
                case true:
                    Global.putInt(context.getContentResolver(), "safe_boot_disallowed", newValue);
                    break;
                case true:
                    if (newValue) {
                        if (Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 1) {
                            z2 = false;
                        }
                        if (z2) {
                            Global.putInt(context.getContentResolver(), "airplane_mode_on", 0);
                            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
                            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
                            context.sendBroadcastAsUser(intent, UserHandle.ALL);
                            break;
                        }
                    }
                    break;
                case true:
                    if (newValue) {
                        Secure.putIntForUser(context.getContentResolver(), "doze_enabled", 0, userId);
                        Secure.putIntForUser(context.getContentResolver(), "doze_always_on", 0, userId);
                        Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_pick_up", 0, userId);
                        Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_long_press", 0, userId);
                        Secure.putIntForUser(context.getContentResolver(), "doze_pulse_on_double_tap", 0, userId);
                        break;
                    }
                    break;
                case true:
                    if (newValue) {
                        Global.putString(context.getContentResolver(), "location_global_kill_switch", "0");
                        break;
                    }
                    break;
            }
            Binder.restoreCallingIdentity(id);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isSettingRestrictedForUser(Context context, String setting, int userId, String value, int callingUid) {
        boolean z;
        String restriction;
        String restriction2;
        Preconditions.checkNotNull(setting);
        UserManager mUserManager = (UserManager) context.getSystemService(UserManager.class);
        boolean checkAllUser = false;
        switch (setting.hashCode()) {
            case -1796809747:
                if (setting.equals("location_mode")) {
                    z = false;
                    break;
                }
            case -1500478207:
                if (setting.equals("location_providers_allowed")) {
                    z = true;
                    break;
                }
            case -1490222856:
                if (setting.equals("doze_enabled")) {
                    z = true;
                    break;
                }
            case -1115710219:
                if (setting.equals("verifier_verify_adb_installs")) {
                    z = true;
                    break;
                }
            case -970351711:
                if (setting.equals("adb_enabled")) {
                    z = true;
                    break;
                }
            case -693072130:
                if (setting.equals("screen_brightness_mode")) {
                    z = true;
                    break;
                }
            case -623873498:
                if (setting.equals("always_on_vpn_app")) {
                    z = true;
                    break;
                }
            case -416662510:
                if (setting.equals("preferred_network_mode")) {
                    z = true;
                    break;
                }
            case -101820922:
                if (setting.equals("doze_always_on")) {
                    z = true;
                    break;
                }
            case -32505807:
                if (setting.equals("doze_pulse_on_long_press")) {
                    z = true;
                    break;
                }
            case 58027029:
                if (setting.equals("safe_boot_disallowed")) {
                    z = true;
                    break;
                }
            case 258514750:
                if (setting.equals("screen_off_timeout")) {
                    z = true;
                    break;
                }
            case 720635155:
                if (setting.equals("package_verifier_enable")) {
                    z = true;
                    break;
                }
            case 926123534:
                if (setting.equals("airplane_mode_on")) {
                    z = true;
                    break;
                }
            case 1073289638:
                if (setting.equals("doze_pulse_on_double_tap")) {
                    z = true;
                    break;
                }
            case 1275530062:
                if (setting.equals("auto_time_zone")) {
                    z = true;
                    break;
                }
            case 1307734371:
                if (setting.equals("location_global_kill_switch")) {
                    z = true;
                    break;
                }
            case 1602982312:
                if (setting.equals("doze_pulse_on_pick_up")) {
                    z = true;
                    break;
                }
            case 1646894952:
                if (setting.equals("always_on_vpn_lockdown")) {
                    z = true;
                    break;
                }
            case 1661297501:
                if (setting.equals("auto_time")) {
                    z = true;
                    break;
                }
            case 1701140351:
                if (setting.equals("install_non_market_apps")) {
                    z = true;
                    break;
                }
            case 1735689732:
                if (setting.equals("screen_brightness")) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                if (!mUserManager.hasUserRestriction("no_config_location", UserHandle.of(userId)) || callingUid == 1000) {
                    if (!String.valueOf(0).equals(value)) {
                        restriction = "no_share_location";
                        break;
                    }
                    return false;
                }
                return true;
                break;
            case true:
                if (mUserManager.hasUserRestriction("no_config_location", UserHandle.of(userId)) && callingUid != 1000) {
                    return true;
                }
                if (value == null || !value.startsWith("-")) {
                    restriction = "no_share_location";
                    break;
                }
                return false;
                break;
            case true:
                if (!"0".equals(value)) {
                    restriction = "no_install_unknown_sources";
                    break;
                }
                return false;
            case true:
                if (!"0".equals(value)) {
                    restriction = "no_debugging_features";
                    break;
                }
                return false;
            case true:
            case true:
                if (!"1".equals(value)) {
                    restriction = "ensure_verify_apps";
                    break;
                }
                return false;
            case true:
                restriction = "no_config_mobile_networks";
                break;
            case true:
            case true:
                int appId = UserHandle.getAppId(callingUid);
                if (appId != 1000 && appId != 0) {
                    restriction2 = "no_config_vpn";
                    break;
                }
                return false;
                break;
            case true:
                if (!"1".equals(value)) {
                    restriction = "no_safe_boot";
                    break;
                }
                return false;
            case true:
                if (!"0".equals(value)) {
                    restriction = "no_airplane_mode";
                    break;
                }
                return false;
            case true:
            case true:
            case true:
            case true:
            case true:
                if (!"0".equals(value)) {
                    restriction = "no_ambient_display";
                    break;
                }
                return false;
            case true:
                if (!"0".equals(value)) {
                    restriction = "no_config_location";
                    checkAllUser = true;
                    break;
                }
                return false;
            case true:
            case true:
                if (callingUid != 1000) {
                    restriction = "no_config_brightness";
                    break;
                }
                return false;
            case H.REPORT_WINDOWS_CHANGE /*19*/:
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
                if (dpm == null || !dpm.getAutoTimeRequired() || !"0".equals(value)) {
                    if (callingUid != 1000) {
                        restriction2 = "no_config_date_time";
                        break;
                    }
                    return false;
                }
                return true;
                break;
            case true:
                if (callingUid != 1000) {
                    restriction = "no_config_date_time";
                    break;
                }
                return false;
            case BackupHandler.MSG_OP_COMPLETE /*21*/:
                if (callingUid != 1000) {
                    restriction = "no_config_screen_timeout";
                    break;
                }
                return false;
            default:
                if (setting.startsWith("data_roaming") && !"0".equals(value)) {
                    restriction = "no_data_roaming";
                    break;
                }
                return false;
                break;
        }
        restriction = restriction2;
        if (checkAllUser) {
            return mUserManager.hasUserRestrictionOnAnyUser(restriction);
        }
        return mUserManager.hasUserRestriction(restriction, UserHandle.of(userId));
    }

    public static void dumpRestrictions(PrintWriter pw, String prefix, Bundle restrictions) {
        boolean noneSet = true;
        StringBuilder stringBuilder;
        if (restrictions != null) {
            for (String key : restrictions.keySet()) {
                if (restrictions.getBoolean(key, false)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(prefix);
                    stringBuilder2.append(key);
                    pw.println(stringBuilder2.toString());
                    noneSet = false;
                }
            }
            if (noneSet) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("none");
                pw.println(stringBuilder.toString());
                return;
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("null");
        pw.println(stringBuilder.toString());
    }

    public static void moveRestriction(String restrictionKey, SparseArray<Bundle> srcRestrictions, SparseArray<Bundle> destRestrictions) {
        int i = 0;
        while (i < srcRestrictions.size()) {
            int key = srcRestrictions.keyAt(i);
            Bundle from = (Bundle) srcRestrictions.valueAt(i);
            if (contains(from, restrictionKey)) {
                from.remove(restrictionKey);
                Bundle to = (Bundle) destRestrictions.get(key);
                if (to == null) {
                    to = new Bundle();
                    destRestrictions.append(key, to);
                }
                to.putBoolean(restrictionKey, true);
                if (from.isEmpty()) {
                    srcRestrictions.removeAt(i);
                    i--;
                }
            }
            i++;
        }
    }

    public static boolean restrictionsChanged(Bundle oldRestrictions, Bundle newRestrictions, String... restrictions) {
        if (restrictions.length == 0) {
            return areEqual(oldRestrictions, newRestrictions);
        }
        for (String restriction : restrictions) {
            if (oldRestrictions.getBoolean(restriction, false) != newRestrictions.getBoolean(restriction, false)) {
                return true;
            }
        }
        return false;
    }
}
