package android_maps_conflict_avoidance.com.google.common;

import android_maps_conflict_avoidance.com.google.common.io.PersistentStore;
import android_maps_conflict_avoidance.com.google.common.util.text.TextUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class Log {
    private static final long START_TIME = System.currentTimeMillis();
    private static StringBuffer entryBuffer = new StringBuffer(256);
    private static boolean isEventLoggingEnabledForTest = false;
    private static boolean isExplicitClearForTest = false;
    private static long lastEventTimeMillis = 0;
    private static final Object lastThrowableLock = new Object();
    private static String lastThrowableString = null;
    private static final Vector logEntries = new Vector(150);
    private static boolean logMemory = false;
    private static LogSaver logSaver;
    private static boolean logThread = false;
    private static boolean logTime = true;
    private static OnScreenPrinter onScreenPrinter;
    private static Printer printer = new StandardErrorPrinter();
    private static int throwableCount = 0;
    private static ThrowableListener throwableListener;
    private static final Hashtable timers = new Hashtable();

    public interface LogSaver {
        Object uploadEventLog(boolean z, Object obj, byte[] bArr);
    }

    public interface OnScreenPrinter {
        void printToScreen(String str);
    }

    public interface Printer {
    }

    public interface ThrowableListener {
        void onThrowable(String str, Throwable th, boolean z);
    }

    public static class StandardErrorPrinter implements Printer {
    }

    public static void logThrowable(String source, Throwable t) {
        t.printStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(source);
        stringBuilder.append(": ");
        stringBuilder.append(t.toString());
        addThrowableString(stringBuilder.toString());
        sendThrowable(source, t, false);
    }

    public static void logQuietThrowable(String source, Throwable t) {
        t.printStackTrace();
        sendThrowable(source, t, true);
    }

    public static void addThrowableString(String message) {
        if (message != null) {
            synchronized (lastThrowableLock) {
                if (lastThrowableString == null) {
                    lastThrowableString = message;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(lastThrowableString);
                    stringBuilder.append("\n");
                    stringBuilder.append(message);
                    lastThrowableString = stringBuilder.toString();
                }
                if (lastThrowableString.length() > 300) {
                    lastThrowableString = lastThrowableString.substring(0, 300);
                }
            }
        }
    }

    public static boolean addEvent(short type, String status, String data) {
        String str;
        String str2;
        long timestamp = System.currentTimeMillis();
        PersistentStore store = getPersistentStore();
        byte[] oldEvents = store.readPreference("EVENT_LOG");
        if (oldEvents == null || oldEvents.length > 600 || timestamp - lastEventTimeMillis > 6553500) {
            if (oldEvents == null) {
                resetPersistentEventLog(timestamp);
            } else if (logSaver != null) {
                uploadEventLog(false, null, timestamp);
            }
            oldEvents = store.readPreference("EVENT_LOG");
        }
        byte[] oldEvents2 = oldEvents;
        short numEvents = (short) 0;
        if (oldEvents2.length > 2) {
            numEvents = (short) (((oldEvents2[0] & 255) << 8) | (oldEvents2[1] & 255));
        }
        short numEvents2 = (short) (numEvents + 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeShort(numEvents2);
            dos.write(oldEvents2, 2, oldEvents2.length - 2);
            try {
                dos.writeShort(type);
                dos.writeShort((int) (Math.min(timestamp - lastEventTimeMillis, 6553500) / 100));
            } catch (IOException e) {
                str = status;
                str2 = data;
                return false;
            }
            try {
                dos.writeUTF(status);
                try {
                    dos.writeUTF(data);
                    getPersistentStore().setPreference("EVENT_LOG", baos.toByteArray());
                    lastEventTimeMillis = timestamp;
                    return true;
                } catch (IOException e2) {
                    return false;
                }
            } catch (IOException e3) {
                str2 = data;
                return false;
            }
        } catch (IOException e4) {
            short s = type;
            str = status;
            str2 = data;
            return false;
        }
    }

    public static String createEventTuple(String[] elements) {
        if (elements.length == 0) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("|");
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                StringBuffer element = new StringBuffer(elements[i]);
                TextUtil.replace("|", "", element);
                buffer.append(element);
                buffer.append("|");
            }
        }
        return buffer.toString();
    }

    public static void setLogSaver(LogSaver logSaver) {
        logSaver = logSaver;
    }

    private static Object uploadEventLog(boolean immediate, Object waitObject, long timestamp) {
        Object uploadTracker = logSaver.uploadEventLog(immediate, waitObject, getPersistentStore().readPreference("EVENT_LOG"));
        resetPersistentEventLog(timestamp);
        return uploadTracker;
    }

    private static void resetPersistentEventLog(long timestamp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeShort(0);
            dos.writeLong(timestamp);
            lastEventTimeMillis = timestamp;
        } catch (IOException e) {
        } catch (Throwable th) {
            getPersistentStore().setPreference("EVENT_LOG", baos.toByteArray());
        }
        getPersistentStore().setPreference("EVENT_LOG", baos.toByteArray());
    }

    private static PersistentStore getPersistentStore() {
        return Config.getInstance().getPersistentStore();
    }

    private static void sendThrowable(String source, Throwable throwable, boolean isQuiet) {
        if (throwableListener != null) {
            throwableListener.onThrowable(source, throwable, isQuiet);
        }
    }

    public static void logToScreen(String logString) {
        if (onScreenPrinter != null) {
            onScreenPrinter.printToScreen(logString);
        }
    }
}
