package android.app.timezone;

import android.app.AppProtoEnums;

final class Utils {
    private Utils() {
    }

    static int validateVersion(String type, int version) {
        if (version >= 0 && version <= AppProtoEnums.PROCESS_STATE_UNKNOWN) {
            return version;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid ");
        stringBuilder.append(type);
        stringBuilder.append(" version=");
        stringBuilder.append(version);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static String validateRulesVersion(String type, String rulesVersion) {
        validateNotNull(type, rulesVersion);
        if (!rulesVersion.isEmpty()) {
            return rulesVersion;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(" must not be empty");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static <T> T validateNotNull(String type, T object) {
        if (object != null) {
            return object;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(" == null");
        throw new NullPointerException(stringBuilder.toString());
    }

    static <T> T validateConditionalNull(boolean requireNotNull, String type, T object) {
        if (requireNotNull) {
            return validateNotNull(type, object);
        }
        return validateNull(type, object);
    }

    static <T> T validateNull(String type, T object) {
        if (object == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(" != null");
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
