package com.android.internal.util;

import android.text.TextUtils;
import java.util.Collection;

public class Preconditions {
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkArgument(boolean expression, String messageTemplate, Object... messageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(messageTemplate, messageArgs));
        }
    }

    public static <T extends CharSequence> T checkStringNotEmpty(T string) {
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        throw new IllegalArgumentException();
    }

    public static <T extends CharSequence> T checkStringNotEmpty(T string, Object errorMessage) {
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        throw new IllegalArgumentException(String.valueOf(errorMessage));
    }

    public static <T> T checkNotNull(T reference) {
        if (reference != null) {
            return reference;
        }
        throw new NullPointerException();
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference != null) {
            return reference;
        }
        throw new NullPointerException(String.valueOf(errorMessage));
    }

    public static <T> T checkNotNull(T reference, String messageTemplate, Object... messageArgs) {
        if (reference != null) {
            return reference;
        }
        throw new NullPointerException(String.format(messageTemplate, messageArgs));
    }

    public static void checkState(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static void checkState(boolean expression) {
        checkState(expression, null);
    }

    public static int checkFlagsArgument(int requestedFlags, int allowedFlags) {
        if ((requestedFlags & allowedFlags) == requestedFlags) {
            return requestedFlags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Requested flags 0x");
        stringBuilder.append(Integer.toHexString(requestedFlags));
        stringBuilder.append(", but only 0x");
        stringBuilder.append(Integer.toHexString(allowedFlags));
        stringBuilder.append(" are allowed");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static int checkArgumentNonnegative(int value, String errorMessage) {
        if (value >= 0) {
            return value;
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public static int checkArgumentNonnegative(int value) {
        if (value >= 0) {
            return value;
        }
        throw new IllegalArgumentException();
    }

    public static long checkArgumentNonnegative(long value) {
        if (value >= 0) {
            return value;
        }
        throw new IllegalArgumentException();
    }

    public static long checkArgumentNonnegative(long value, String errorMessage) {
        if (value >= 0) {
            return value;
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public static int checkArgumentPositive(int value, String errorMessage) {
        if (value > 0) {
            return value;
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public static float checkArgumentFinite(float value, String valueName) {
        StringBuilder stringBuilder;
        if (Float.isNaN(value)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(valueName);
            stringBuilder.append(" must not be NaN");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!Float.isInfinite(value)) {
            return value;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(valueName);
            stringBuilder.append(" must not be infinite");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static float checkArgumentInRange(float value, float lower, float upper, String valueName) {
        if (Float.isNaN(value)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(valueName);
            stringBuilder.append(" must not be NaN");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value < lower) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%f, %f] (too low)", new Object[]{valueName, Float.valueOf(lower), Float.valueOf(upper)}));
        } else if (value <= upper) {
            return value;
        } else {
            throw new IllegalArgumentException(String.format("%s is out of range of [%f, %f] (too high)", new Object[]{valueName, Float.valueOf(lower), Float.valueOf(upper)}));
        }
    }

    public static int checkArgumentInRange(int value, int lower, int upper, String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too low)", new Object[]{valueName, Integer.valueOf(lower), Integer.valueOf(upper)}));
        } else if (value <= upper) {
            return value;
        } else {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too high)", new Object[]{valueName, Integer.valueOf(lower), Integer.valueOf(upper)}));
        }
    }

    public static long checkArgumentInRange(long value, long lower, long upper, String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too low)", new Object[]{valueName, Long.valueOf(lower), Long.valueOf(upper)}));
        } else if (value <= upper) {
            return value;
        } else {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too high)", new Object[]{valueName, Long.valueOf(lower), Long.valueOf(upper)}));
        }
    }

    public static <T> T[] checkArrayElementsNotNull(T[] value, String valueName) {
        if (value != null) {
            int i = 0;
            while (i < value.length) {
                if (value[i] != null) {
                    i++;
                } else {
                    throw new NullPointerException(String.format("%s[%d] must not be null", new Object[]{valueName, Integer.valueOf(i)}));
                }
            }
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(valueName);
        stringBuilder.append(" must not be null");
        throw new NullPointerException(stringBuilder.toString());
    }

    public static <C extends Collection<T>, T> C checkCollectionElementsNotNull(C value, String valueName) {
        if (value != null) {
            long ctr = 0;
            for (T elem : value) {
                if (elem != null) {
                    ctr++;
                } else {
                    throw new NullPointerException(String.format("%s[%d] must not be null", new Object[]{valueName, Long.valueOf(ctr)}));
                }
            }
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(valueName);
        stringBuilder.append(" must not be null");
        throw new NullPointerException(stringBuilder.toString());
    }

    public static <T> Collection<T> checkCollectionNotEmpty(Collection<T> value, String valueName) {
        StringBuilder stringBuilder;
        if (value == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(valueName);
            stringBuilder.append(" must not be null");
            throw new NullPointerException(stringBuilder.toString());
        } else if (!value.isEmpty()) {
            return value;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(valueName);
            stringBuilder.append(" is empty");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static float[] checkArrayElementsInRange(float[] value, float lower, float upper, String valueName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(valueName);
        stringBuilder.append(" must not be null");
        checkNotNull(value, stringBuilder.toString());
        int i = 0;
        while (i < value.length) {
            float v = value[i];
            if (Float.isNaN(v)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(valueName);
                stringBuilder2.append("[");
                stringBuilder2.append(i);
                stringBuilder2.append("] must not be NaN");
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else if (v < lower) {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%f, %f] (too low)", new Object[]{valueName, Integer.valueOf(i), Float.valueOf(lower), Float.valueOf(upper)}));
            } else if (v <= upper) {
                i++;
            } else {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%f, %f] (too high)", new Object[]{valueName, Integer.valueOf(i), Float.valueOf(lower), Float.valueOf(upper)}));
            }
        }
        return value;
    }

    public static int[] checkArrayElementsInRange(int[] value, int lower, int upper, String valueName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(valueName);
        stringBuilder.append(" must not be null");
        checkNotNull(value, stringBuilder.toString());
        int i = 0;
        while (i < value.length) {
            int v = value[i];
            if (v < lower) {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%d, %d] (too low)", new Object[]{valueName, Integer.valueOf(i), Integer.valueOf(lower), Integer.valueOf(upper)}));
            } else if (v <= upper) {
                i++;
            } else {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%d, %d] (too high)", new Object[]{valueName, Integer.valueOf(i), Integer.valueOf(lower), Integer.valueOf(upper)}));
            }
        }
        return value;
    }
}
