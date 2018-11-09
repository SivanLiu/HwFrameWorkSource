package com.android.internal.app.procstats;

import android.os.Build;
import android.os.Parcel;
import android.util.EventLog;
import android.util.Slog;
import java.util.ArrayList;

public class SparseMappingTable {
    private static final int ARRAY_MASK = 255;
    private static final int ARRAY_SHIFT = 8;
    public static final int ARRAY_SIZE = 4096;
    private static final int ID_MASK = 255;
    private static final int ID_SHIFT = 0;
    private static final int INDEX_MASK = 65535;
    private static final int INDEX_SHIFT = 16;
    public static final int INVALID_KEY = -1;
    private static final String TAG = "SparseMappingTable";
    private final ArrayList<long[]> mLongs = new ArrayList();
    private int mNextIndex;
    private int mSequence;

    public static class Table {
        private SparseMappingTable mParent;
        private int mSequence = 1;
        private int mSize;
        private int[] mTable;

        private int binarySearch(byte r1) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.internal.app.procstats.SparseMappingTable.Table.binarySearch(byte):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 8 more
*/
            /*
            // Can't load method instructions.
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.app.procstats.SparseMappingTable.Table.binarySearch(byte):int");
        }

        public int getOrAddKey(byte r1, int r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.internal.app.procstats.SparseMappingTable.Table.getOrAddKey(byte, int):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 8 more
*/
            /*
            // Can't load method instructions.
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.app.procstats.SparseMappingTable.Table.getOrAddKey(byte, int):int");
        }

        public Table(SparseMappingTable parent) {
            this.mParent = parent;
            this.mSequence = parent.mSequence;
        }

        public void copyFrom(Table copyFrom, int valueCount) {
            this.mTable = null;
            this.mSize = 0;
            int N = copyFrom.getKeyCount();
            for (int i = 0; i < N; i++) {
                int theirKey = copyFrom.getKeyAt(i);
                long[] theirLongs = (long[]) copyFrom.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(theirKey));
                int myKey = getOrAddKey(SparseMappingTable.getIdFromKey(theirKey), valueCount);
                System.arraycopy(theirLongs, SparseMappingTable.getIndexFromKey(theirKey), (long[]) this.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(myKey)), SparseMappingTable.getIndexFromKey(myKey), valueCount);
            }
        }

        public int getKey(byte id) {
            assertConsistency();
            int idx = binarySearch(id);
            if (idx >= 0) {
                return this.mTable[idx];
            }
            return -1;
        }

        public long getValue(int key) {
            return getValue(key, 0);
        }

        public long getValue(int key, int index) {
            assertConsistency();
            try {
                return ((long[]) this.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(key)))[SparseMappingTable.getIndexFromKey(key) + index];
            } catch (IndexOutOfBoundsException ex) {
                SparseMappingTable.logOrThrow("key=0x" + Integer.toHexString(key) + " index=" + index + " -- " + dumpInternalState(), ex);
                return 0;
            }
        }

        public long getValueForId(byte id) {
            return getValueForId(id, 0);
        }

        public long getValueForId(byte id, int index) {
            assertConsistency();
            int idx = binarySearch(id);
            if (idx < 0) {
                return 0;
            }
            int key = this.mTable[idx];
            try {
                return ((long[]) this.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(key)))[SparseMappingTable.getIndexFromKey(key) + index];
            } catch (IndexOutOfBoundsException ex) {
                SparseMappingTable.logOrThrow("id=0x" + Integer.toHexString(id) + " idx=" + idx + " key=0x" + Integer.toHexString(key) + " index=" + index + " -- " + dumpInternalState(), ex);
                return 0;
            }
        }

        public long[] getArrayForKey(int key) {
            assertConsistency();
            return (long[]) this.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(key));
        }

        public void setValue(int key, long value) {
            setValue(key, 0, value);
        }

        public void setValue(int key, int index, long value) {
            assertConsistency();
            if (value < 0) {
                SparseMappingTable.logOrThrow("can't store negative values key=0x" + Integer.toHexString(key) + " index=" + index + " value=" + value + " -- " + dumpInternalState());
                return;
            }
            try {
                ((long[]) this.mParent.mLongs.get(SparseMappingTable.getArrayFromKey(key)))[SparseMappingTable.getIndexFromKey(key) + index] = value;
            } catch (IndexOutOfBoundsException ex) {
                SparseMappingTable.logOrThrow("key=0x" + Integer.toHexString(key) + " index=" + index + " value=" + value + " -- " + dumpInternalState(), ex);
            }
        }

        public void resetTable() {
            this.mTable = null;
            this.mSize = 0;
            this.mSequence = this.mParent.mSequence;
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.mSequence);
            out.writeInt(this.mSize);
            for (int i = 0; i < this.mSize; i++) {
                out.writeInt(this.mTable[i]);
            }
        }

        public boolean readFromParcel(Parcel in) {
            this.mSequence = in.readInt();
            this.mSize = in.readInt();
            if (this.mSize != 0) {
                this.mTable = new int[this.mSize];
                for (int i = 0; i < this.mSize; i++) {
                    this.mTable[i] = in.readInt();
                }
            } else {
                this.mTable = null;
            }
            if (validateKeys(true)) {
                return true;
            }
            this.mSize = 0;
            this.mTable = null;
            return false;
        }

        public int getKeyCount() {
            return this.mSize;
        }

        public int getKeyAt(int i) {
            return this.mTable[i];
        }

        private void assertConsistency() {
        }

        private boolean validateKeys(boolean log) {
            ArrayList<long[]> longs = this.mParent.mLongs;
            int longsSize = longs.size();
            int N = this.mSize;
            for (int i = 0; i < N; i++) {
                int key = this.mTable[i];
                int arrayIndex = SparseMappingTable.getArrayFromKey(key);
                int index = SparseMappingTable.getIndexFromKey(key);
                if (arrayIndex >= longsSize || index >= ((long[]) longs.get(arrayIndex)).length) {
                    if (log) {
                        Slog.w(SparseMappingTable.TAG, "Invalid stats at index " + i + " -- " + dumpInternalState());
                    }
                    return false;
                }
            }
            return true;
        }

        public String dumpInternalState() {
            StringBuilder sb = new StringBuilder();
            sb.append("SparseMappingTable.Table{mSequence=");
            sb.append(this.mSequence);
            sb.append(" mParent.mSequence=");
            sb.append(this.mParent.mSequence);
            sb.append(" mParent.mLongs.size()=");
            sb.append(this.mParent.mLongs.size());
            sb.append(" mSize=");
            sb.append(this.mSize);
            sb.append(" mTable=");
            if (this.mTable == null) {
                sb.append("null");
            } else {
                int N = this.mTable.length;
                sb.append('[');
                for (int i = 0; i < N; i++) {
                    int key = this.mTable[i];
                    sb.append("0x");
                    sb.append(Integer.toHexString((key >> 0) & 255));
                    sb.append("/0x");
                    sb.append(Integer.toHexString((key >> 8) & 255));
                    sb.append("/0x");
                    sb.append(Integer.toHexString((key >> 16) & 65535));
                    if (i != N - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(']');
            }
            sb.append(" clazz=");
            sb.append(getClass().getName());
            sb.append('}');
            return sb.toString();
        }
    }

    private static void readCompactedLongArray(android.os.Parcel r1, long[] r2, int r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.internal.app.procstats.SparseMappingTable.readCompactedLongArray(android.os.Parcel, long[], int):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.app.procstats.SparseMappingTable.readCompactedLongArray(android.os.Parcel, long[], int):void");
    }

    private static void writeCompactedLongArray(android.os.Parcel r1, long[] r2, int r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.internal.app.procstats.SparseMappingTable.writeCompactedLongArray(android.os.Parcel, long[], int):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.app.procstats.SparseMappingTable.writeCompactedLongArray(android.os.Parcel, long[], int):void");
    }

    public SparseMappingTable() {
        this.mLongs.add(new long[4096]);
    }

    public void reset() {
        this.mLongs.clear();
        this.mLongs.add(new long[4096]);
        this.mNextIndex = 0;
        this.mSequence++;
    }

    public void writeToParcel(Parcel out) {
        out.writeInt(this.mSequence);
        out.writeInt(this.mNextIndex);
        int N = this.mLongs.size();
        out.writeInt(N);
        for (int i = 0; i < N - 1; i++) {
            long[] array = (long[]) this.mLongs.get(i);
            out.writeInt(array.length);
            writeCompactedLongArray(out, array, array.length);
        }
        long[] lastLongs = (long[]) this.mLongs.get(N - 1);
        out.writeInt(this.mNextIndex);
        writeCompactedLongArray(out, lastLongs, this.mNextIndex);
    }

    public void readFromParcel(Parcel in) {
        this.mSequence = in.readInt();
        this.mNextIndex = in.readInt();
        this.mLongs.clear();
        int N = in.readInt();
        for (int i = 0; i < N; i++) {
            int size = in.readInt();
            long[] array = new long[size];
            readCompactedLongArray(in, array, size);
            this.mLongs.add(array);
        }
        if (N > 0 && ((long[]) this.mLongs.get(N - 1)).length != this.mNextIndex) {
            EventLog.writeEvent(1397638484, "73252178", Integer.valueOf(-1), "");
            throw new IllegalStateException("Expected array of length " + this.mNextIndex + " but was " + ((long[]) this.mLongs.get(N - 1)).length);
        }
    }

    public String dumpInternalState(boolean includeData) {
        StringBuilder sb = new StringBuilder();
        sb.append("SparseMappingTable{");
        sb.append("mSequence=");
        sb.append(this.mSequence);
        sb.append(" mNextIndex=");
        sb.append(this.mNextIndex);
        sb.append(" mLongs.size=");
        int N = this.mLongs.size();
        sb.append(N);
        sb.append("\n");
        if (includeData) {
            int i = 0;
            while (i < N) {
                long[] array = (long[]) this.mLongs.get(i);
                int j = 0;
                while (j < array.length && (i != N - 1 || j != this.mNextIndex)) {
                    sb.append(String.format(" %4d %d 0x%016x %-19d\n", new Object[]{Integer.valueOf(i), Integer.valueOf(j), Long.valueOf(array[j]), Long.valueOf(array[j])}));
                    j++;
                }
                i++;
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static byte getIdFromKey(int key) {
        return (byte) ((key >> 0) & 255);
    }

    public static int getArrayFromKey(int key) {
        return (key >> 8) & 255;
    }

    public static int getIndexFromKey(int key) {
        return (key >> 16) & 65535;
    }

    private static void logOrThrow(String message) {
        logOrThrow(message, new RuntimeException("Stack trace"));
    }

    private static void logOrThrow(String message, Throwable th) {
        Slog.e(TAG, message, th);
        if (Build.IS_ENG) {
            throw new RuntimeException(message, th);
        }
    }
}
