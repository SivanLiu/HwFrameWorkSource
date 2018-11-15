package com.android.server.pm;

import android.os.SystemProperties;
import dalvik.system.DexFile;

public class PackageManagerServiceCompilerMapping {
    static final int REASON_SHARED_INDEX = 6;
    public static final String[] REASON_STRINGS = new String[]{"first-boot", "boot", "install", "bg-dexopt", "ab-ota", "inactive", "shared", "bg-speed-dexopt"};

    static {
        if (8 != REASON_STRINGS.length) {
            throw new IllegalStateException("REASON_STRINGS not correct");
        } else if (!"shared".equals(REASON_STRINGS[6])) {
            throw new IllegalStateException("REASON_STRINGS not correct because of shared index");
        }
    }

    private static String getSystemPropertyName(int reason) {
        if (reason < 0 || reason >= REASON_STRINGS.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reason ");
            stringBuilder.append(reason);
            stringBuilder.append(" invalid");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("pm.dexopt.");
        stringBuilder2.append(REASON_STRINGS[reason]);
        return stringBuilder2.toString();
    }

    private static String getAndCheckValidity(int reason) {
        String sysPropValue = SystemProperties.get(getSystemPropertyName(reason));
        if (7 == reason && (sysPropValue == null || sysPropValue.isEmpty())) {
            sysPropValue = "speed";
        }
        StringBuilder stringBuilder;
        if (sysPropValue == null || sysPropValue.isEmpty() || !DexFile.isValidCompilerFilter(sysPropValue)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value \"");
            stringBuilder.append(sysPropValue);
            stringBuilder.append("\" not valid (reason ");
            stringBuilder.append(REASON_STRINGS[reason]);
            stringBuilder.append(")");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (isFilterAllowedForReason(reason, sysPropValue)) {
            return sysPropValue;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value \"");
            stringBuilder.append(sysPropValue);
            stringBuilder.append("\" not allowed (reason ");
            stringBuilder.append(REASON_STRINGS[reason]);
            stringBuilder.append(")");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private static boolean isFilterAllowedForReason(int reason, String filter) {
        return (reason == 6 && DexFile.isProfileGuidedCompilerFilter(filter)) ? false : true;
    }

    static void checkProperties() {
        RuntimeException toThrow = null;
        int reason = 0;
        while (reason <= 7) {
            try {
                String sysPropName = getSystemPropertyName(reason);
                if (sysPropName == null || sysPropName.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Reason system property name \"");
                    stringBuilder.append(sysPropName);
                    stringBuilder.append("\" for reason ");
                    stringBuilder.append(REASON_STRINGS[reason]);
                    throw new IllegalStateException(stringBuilder.toString());
                }
                getAndCheckValidity(reason);
                reason++;
            } catch (Exception exc) {
                if (toThrow == null) {
                    toThrow = new IllegalStateException("PMS compiler filter settings are bad.");
                }
                toThrow.addSuppressed(exc);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }

    public static String getCompilerFilterForReason(int reason) {
        return getAndCheckValidity(reason);
    }

    public static String getDefaultCompilerFilter() {
        String value = SystemProperties.get("dalvik.vm.dex2oat-filter");
        if (value == null || value.isEmpty()) {
            return "speed";
        }
        if (!DexFile.isValidCompilerFilter(value) || DexFile.isProfileGuidedCompilerFilter(value)) {
            return "speed";
        }
        return value;
    }

    public static String getReasonName(int reason) {
        if (reason == -1) {
            return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        if (reason >= 0 && reason < REASON_STRINGS.length) {
            return REASON_STRINGS[reason];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reason ");
        stringBuilder.append(reason);
        stringBuilder.append(" invalid");
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
