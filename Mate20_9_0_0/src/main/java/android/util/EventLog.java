package android.util;

import android.annotation.SystemApi;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventLog {
    private static final String COMMENT_PATTERN = "^\\s*(#.*)?$";
    private static final String TAG = "EventLog";
    private static final String TAGS_FILE = "/system/etc/event-log-tags";
    private static final String TAG_PATTERN = "^\\s*(\\d+)\\s+(\\w+)\\s*(\\(.*\\))?\\s*$";
    private static HashMap<String, Integer> sTagCodes = null;
    private static HashMap<Integer, String> sTagNames = null;

    public static final class Event {
        private static final int DATA_OFFSET = 4;
        private static final byte FLOAT_TYPE = (byte) 4;
        private static final int HEADER_SIZE_OFFSET = 2;
        private static final byte INT_TYPE = (byte) 0;
        private static final int LENGTH_OFFSET = 0;
        private static final byte LIST_TYPE = (byte) 3;
        private static final byte LONG_TYPE = (byte) 1;
        private static final int NANOSECONDS_OFFSET = 16;
        private static final int PROCESS_OFFSET = 4;
        private static final int SECONDS_OFFSET = 12;
        private static final byte STRING_TYPE = (byte) 2;
        private static final int THREAD_OFFSET = 8;
        private static final int UID_OFFSET = 24;
        private static final int V1_PAYLOAD_START = 20;
        private final ByteBuffer mBuffer;
        private Exception mLastWtf;

        Event(byte[] data) {
            this.mBuffer = ByteBuffer.wrap(data);
            this.mBuffer.order(ByteOrder.nativeOrder());
        }

        public int getProcessId() {
            return this.mBuffer.getInt(4);
        }

        @SystemApi
        public int getUid() {
            try {
                return this.mBuffer.getInt(24);
            } catch (IndexOutOfBoundsException e) {
                return -1;
            }
        }

        public int getThreadId() {
            return this.mBuffer.getInt(8);
        }

        public long getTimeNanos() {
            return (((long) this.mBuffer.getInt(12)) * 1000000000) + ((long) this.mBuffer.getInt(16));
        }

        public int getTag() {
            int offset = this.mBuffer.getShort(2);
            if (offset == 0) {
                offset = 20;
            }
            return this.mBuffer.getInt(offset);
        }

        public synchronized Object getData() {
            String str;
            StringBuilder stringBuilder;
            try {
                int offset = this.mBuffer.getShort(2);
                if (offset == 0) {
                    offset = 20;
                }
                this.mBuffer.limit(this.mBuffer.getShort(0) + offset);
                if (offset + 4 >= this.mBuffer.limit()) {
                    return null;
                }
                this.mBuffer.position(offset + 4);
                return decodeObject();
            } catch (IllegalArgumentException e) {
                str = EventLog.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal entry payload: tag=");
                stringBuilder.append(getTag());
                Log.wtf(str, stringBuilder.toString(), e);
                this.mLastWtf = e;
                return null;
            } catch (BufferUnderflowException e2) {
                str = EventLog.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Truncated entry payload: tag=");
                stringBuilder.append(getTag());
                Log.wtf(str, stringBuilder.toString(), e2);
                this.mLastWtf = e2;
                return null;
            }
        }

        private Object decodeObject() {
            byte type = this.mBuffer.get();
            int length;
            switch (type) {
                case (byte) 0:
                    return Integer.valueOf(this.mBuffer.getInt());
                case (byte) 1:
                    return Long.valueOf(this.mBuffer.getLong());
                case (byte) 2:
                    try {
                        length = this.mBuffer.getInt();
                        int start = this.mBuffer.position();
                        this.mBuffer.position(start + length);
                        return new String(this.mBuffer.array(), start, length, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.wtf(EventLog.TAG, "UTF-8 is not supported", e);
                        this.mLastWtf = e;
                        return null;
                    }
                case (byte) 3:
                    length = this.mBuffer.get();
                    if (length < 0) {
                        length += 256;
                    }
                    Object[] array = new Object[length];
                    for (int i = 0; i < length; i++) {
                        array[i] = decodeObject();
                    }
                    return array;
                case (byte) 4:
                    return Float.valueOf(this.mBuffer.getFloat());
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown entry type: ");
                    stringBuilder.append(type);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public static Event fromBytes(byte[] data) {
            return new Event(data);
        }

        public byte[] getBytes() {
            byte[] bytes = this.mBuffer.array();
            return Arrays.copyOf(bytes, bytes.length);
        }

        public Exception getLastError() {
            return this.mLastWtf;
        }

        public void clearError() {
            this.mLastWtf = null;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Arrays.equals(this.mBuffer.array(), ((Event) o).mBuffer.array());
        }

        public int hashCode() {
            return Arrays.hashCode(this.mBuffer.array());
        }
    }

    public static native void readEvents(int[] iArr, Collection<Event> collection) throws IOException;

    @SystemApi
    public static native void readEventsOnWrapping(int[] iArr, long j, Collection<Event> collection) throws IOException;

    public static native int writeEvent(int i, float f);

    public static native int writeEvent(int i, int i2);

    public static native int writeEvent(int i, long j);

    public static native int writeEvent(int i, String str);

    public static native int writeEvent(int i, Object... objArr);

    public static String getTagName(int tag) {
        readTagsFile();
        return (String) sTagNames.get(Integer.valueOf(tag));
    }

    public static int getTagCode(String name) {
        readTagsFile();
        Integer code = (Integer) sTagCodes.get(name);
        return code != null ? code.intValue() : -1;
    }

    private static synchronized void readTagsFile() {
        synchronized (EventLog.class) {
            if (sTagCodes == null || sTagNames == null) {
                sTagCodes = new HashMap();
                sTagNames = new HashMap();
                Pattern comment = Pattern.compile(COMMENT_PATTERN);
                Pattern tag = Pattern.compile(TAG_PATTERN);
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(TAGS_FILE), 256);
                    while (true) {
                        String readLine = reader.readLine();
                        String line = readLine;
                        if (readLine == null) {
                            try {
                                break;
                            } catch (IOException e) {
                            }
                        } else if (!comment.matcher(line).matches()) {
                            Matcher m = tag.matcher(line);
                            if (m.matches()) {
                                String name;
                                try {
                                    int num = Integer.parseInt(m.group(1));
                                    name = m.group(2);
                                    sTagCodes.put(name, Integer.valueOf(num));
                                    sTagNames.put(Integer.valueOf(num), name);
                                } catch (NumberFormatException e2) {
                                    name = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error in /system/etc/event-log-tags: ");
                                    stringBuilder.append(line);
                                    Log.wtf(name, stringBuilder.toString(), e2);
                                }
                            } else {
                                String str = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Bad entry in /system/etc/event-log-tags: ");
                                stringBuilder2.append(line);
                                Log.wtf(str, stringBuilder2.toString());
                            }
                        }
                    }
                    reader.close();
                } catch (IOException e3) {
                    try {
                        Log.wtf(TAG, "Error reading /system/etc/event-log-tags", e3);
                        if (reader != null) {
                            reader.close();
                        }
                        return;
                    } catch (Throwable th) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e4) {
                            }
                        }
                    }
                }
            } else {
                return;
            }
        }
    }
}
