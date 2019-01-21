package org.json;

class JSON {
    JSON() {
    }

    static double checkDouble(double d) throws JSONException {
        if (!Double.isInfinite(d) && !Double.isNaN(d)) {
            return d;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Forbidden numeric value: ");
        stringBuilder.append(d);
        throw new JSONException(stringBuilder.toString());
    }

    static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            if ("true".equalsIgnoreCase(stringValue)) {
                return Boolean.valueOf(true);
            }
            if ("false".equalsIgnoreCase(stringValue)) {
                return Boolean.valueOf(false);
            }
        }
        return null;
    }

    static Double toDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return Double.valueOf((String) value);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    static Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf((int) Double.parseDouble((String) value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    static Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        if (value instanceof String) {
            try {
                return Long.valueOf((long) Double.parseDouble((String) value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    static String toString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            return String.valueOf(value);
        }
        return null;
    }

    public static JSONException typeMismatch(Object indexOrName, Object actual, String requiredType) throws JSONException {
        StringBuilder stringBuilder;
        if (actual == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value at ");
            stringBuilder.append(indexOrName);
            stringBuilder.append(" is null.");
            throw new JSONException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Value ");
        stringBuilder.append(actual);
        stringBuilder.append(" at ");
        stringBuilder.append(indexOrName);
        stringBuilder.append(" of type ");
        stringBuilder.append(actual.getClass().getName());
        stringBuilder.append(" cannot be converted to ");
        stringBuilder.append(requiredType);
        throw new JSONException(stringBuilder.toString());
    }

    public static JSONException typeMismatch(Object actual, String requiredType) throws JSONException {
        if (actual == null) {
            throw new JSONException("Value is null.");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Value ");
        stringBuilder.append(actual);
        stringBuilder.append(" of type ");
        stringBuilder.append(actual.getClass().getName());
        stringBuilder.append(" cannot be converted to ");
        stringBuilder.append(requiredType);
        throw new JSONException(stringBuilder.toString());
    }
}
