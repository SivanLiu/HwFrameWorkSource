package libcore.util;

import android.icu.impl.PatternTokenizer;
import java.lang.reflect.Field;
import java.util.Arrays;

public final class Objects {
    private Objects() {
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static String toString(Object o) {
        Class<?> c = o.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append(c.getSimpleName());
        sb.append('[');
        int i = 0;
        for (Field f : c.getDeclaredFields()) {
            if ((f.getModifiers() & 136) == 0) {
                f.setAccessible(true);
                int i2;
                try {
                    Object value = f.get(o);
                    i2 = i + 1;
                    if (i > 0) {
                        try {
                            sb.append(',');
                        } catch (IllegalAccessException e) {
                            i = e;
                            throw new AssertionError(i);
                        }
                    }
                    sb.append(f.getName());
                    sb.append('=');
                    if (value.getClass().isArray()) {
                        if (value.getClass() == boolean[].class) {
                            sb.append(Arrays.toString((boolean[]) value));
                        } else if (value.getClass() == byte[].class) {
                            sb.append(Arrays.toString((byte[]) value));
                        } else if (value.getClass() == char[].class) {
                            sb.append(Arrays.toString((char[]) value));
                        } else if (value.getClass() == double[].class) {
                            sb.append(Arrays.toString((double[]) value));
                        } else if (value.getClass() == float[].class) {
                            sb.append(Arrays.toString((float[]) value));
                        } else if (value.getClass() == int[].class) {
                            sb.append(Arrays.toString((int[]) value));
                        } else if (value.getClass() == long[].class) {
                            sb.append(Arrays.toString((long[]) value));
                        } else if (value.getClass() == short[].class) {
                            sb.append(Arrays.toString((short[]) value));
                        } else {
                            sb.append(Arrays.toString((Object[]) value));
                        }
                    } else if (value.getClass() == Character.class) {
                        sb.append(PatternTokenizer.SINGLE_QUOTE);
                        sb.append(value);
                        sb.append(PatternTokenizer.SINGLE_QUOTE);
                    } else if (value.getClass() == String.class) {
                        sb.append('\"');
                        sb.append(value);
                        sb.append('\"');
                    } else {
                        sb.append(value);
                    }
                    i = i2;
                } catch (IllegalAccessException e2) {
                    i2 = i;
                    i = e2;
                    throw new AssertionError(i);
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
