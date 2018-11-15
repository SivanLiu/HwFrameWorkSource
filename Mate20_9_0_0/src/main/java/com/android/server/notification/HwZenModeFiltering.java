package com.android.server.notification;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.notification.ZenModeConfig;
import android.util.Slog;

public class HwZenModeFiltering {
    private static final String EXTRA_NUMBER_IS_SMS = "com.huawei.hsm.number_type_sms";
    private static final String KEY_ZEN_CALL_WHITE_LIST_ENABLED = "zen_call_white_list_enabled";
    private static final String TAG = "HwZenModeFiltering";

    public static boolean matchesCallFilter(Context context, int zen, ZenModeConfig config, UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        boolean whiteListMode = false;
        boolean isSms = extras.getBoolean(EXTRA_NUMBER_IS_SMS, false);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("matchesCallFilter, isSms:");
        stringBuilder.append(isSms);
        stringBuilder.append(",allow message:");
        stringBuilder.append(config.allowMessages);
        stringBuilder.append(",allow from:");
        stringBuilder.append(config.allowMessagesFrom);
        Slog.w(str, stringBuilder.toString());
        if (!isSms) {
            if (Secure.getInt(context.getContentResolver(), KEY_ZEN_CALL_WHITE_LIST_ENABLED, 0) != 0) {
                whiteListMode = true;
            }
            if (zen == 1 && whiteListMode) {
                return ZenModeFiltering.isRepeatCall(context, zen, config, extras);
            }
            return ZenModeFiltering.matchesCallFilter(context, zen, config, userHandle, extras, validator, contactsTimeoutMs, timeoutAffinity);
        } else if (zen == 2 || zen == 3) {
            return false;
        } else {
            if (zen == 1) {
                if (!config.allowMessages) {
                    return false;
                }
                if (validator != null) {
                    float contactAffinity = validator.getContactAffinity(userHandle, extras, contactsTimeoutMs, timeoutAffinity);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("matchesCallFilter , affinit:");
                    stringBuilder.append(contactAffinity);
                    Slog.w(str, stringBuilder.toString());
                    return audienceMatches(config.allowMessagesFrom, contactAffinity);
                }
            }
            return true;
        }
    }

    private static boolean audienceMatches(int source, float contactAffinity) {
        boolean z = false;
        switch (source) {
            case 0:
                return true;
            case 1:
                if (contactAffinity >= 0.5f) {
                    z = true;
                }
                return z;
            case 2:
                if (contactAffinity >= 1.0f) {
                    z = true;
                }
                return z;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Encountered unknown source: ");
                stringBuilder.append(source);
                Slog.w(str, stringBuilder.toString());
                return true;
        }
    }
}
