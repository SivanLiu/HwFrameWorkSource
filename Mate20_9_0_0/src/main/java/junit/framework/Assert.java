package junit.framework;

@Deprecated
public class Assert {
    protected Assert() {
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    public static void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    public static void assertFalse(String message, boolean condition) {
        assertTrue(message, condition ^ 1);
    }

    public static void assertFalse(boolean condition) {
        assertFalse(null, condition);
    }

    public static void fail(String message) {
        if (message == null) {
            throw new AssertionFailedError();
        }
        throw new AssertionFailedError(message);
    }

    public static void fail() {
        fail(null);
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        if (expected != null || actual != null) {
            if (expected == null || !expected.equals(actual)) {
                failNotEquals(message, expected, actual);
            }
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, String expected, String actual) {
        if (expected != null || actual != null) {
            if (expected == null || !expected.equals(actual)) {
                throw new ComparisonFailure(message == null ? "" : message, expected, actual);
            }
        }
    }

    public static void assertEquals(String expected, String actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, double expected, double actual, double delta) {
        if (Double.compare(expected, actual) != 0 && Math.abs(expected - actual) > delta) {
            failNotEquals(message, new Double(expected), new Double(actual));
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(null, expected, actual, delta);
    }

    public static void assertEquals(String message, float expected, float actual, float delta) {
        if (Float.compare(expected, actual) != 0 && Math.abs(expected - actual) > delta) {
            failNotEquals(message, new Float(expected), new Float(actual));
        }
    }

    public static void assertEquals(float expected, float actual, float delta) {
        assertEquals(null, expected, actual, delta);
    }

    public static void assertEquals(String message, long expected, long actual) {
        assertEquals(message, Long.valueOf(expected), Long.valueOf(actual));
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, boolean expected, boolean actual) {
        assertEquals(message, Boolean.valueOf(expected), Boolean.valueOf(actual));
    }

    public static void assertEquals(boolean expected, boolean actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, byte expected, byte actual) {
        assertEquals(message, Byte.valueOf(expected), Byte.valueOf(actual));
    }

    public static void assertEquals(byte expected, byte actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, char expected, char actual) {
        assertEquals(message, Character.valueOf(expected), Character.valueOf(actual));
    }

    public static void assertEquals(char expected, char actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, short expected, short actual) {
        assertEquals(message, Short.valueOf(expected), Short.valueOf(actual));
    }

    public static void assertEquals(short expected, short actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, int expected, int actual) {
        assertEquals(message, Integer.valueOf(expected), Integer.valueOf(actual));
    }

    public static void assertEquals(int expected, int actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertNotNull(Object object) {
        assertNotNull(null, object);
    }

    public static void assertNotNull(String message, Object object) {
        assertTrue(message, object != null);
    }

    public static void assertNull(Object object) {
        if (object != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected: <null> but was: ");
            stringBuilder.append(object.toString());
            assertNull(stringBuilder.toString(), object);
        }
    }

    public static void assertNull(String message, Object object) {
        assertTrue(message, object == null);
    }

    public static void assertSame(String message, Object expected, Object actual) {
        if (expected != actual) {
            failNotSame(message, expected, actual);
        }
    }

    public static void assertSame(Object expected, Object actual) {
        assertSame(null, expected, actual);
    }

    public static void assertNotSame(String message, Object expected, Object actual) {
        if (expected == actual) {
            failSame(message);
        }
    }

    public static void assertNotSame(Object expected, Object actual) {
        assertNotSame(null, expected, actual);
    }

    public static void failSame(String message) {
        String formatted;
        if (message != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(" ");
            formatted = stringBuilder.toString();
        } else {
            formatted = "";
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(formatted);
        stringBuilder2.append("expected not same");
        fail(stringBuilder2.toString());
    }

    public static void failNotSame(String message, Object expected, Object actual) {
        String formatted;
        if (message != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(" ");
            formatted = stringBuilder.toString();
        } else {
            formatted = "";
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(formatted);
        stringBuilder2.append("expected same:<");
        stringBuilder2.append(expected);
        stringBuilder2.append("> was not:<");
        stringBuilder2.append(actual);
        stringBuilder2.append(">");
        fail(stringBuilder2.toString());
    }

    public static void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual));
    }

    public static String format(String message, Object expected, Object actual) {
        StringBuilder stringBuilder;
        String formatted = "";
        if (message != null && message.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(" ");
            formatted = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(formatted);
        stringBuilder.append("expected:<");
        stringBuilder.append(expected);
        stringBuilder.append("> but was:<");
        stringBuilder.append(actual);
        stringBuilder.append(">");
        return stringBuilder.toString();
    }
}
