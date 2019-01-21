package android.net;

import android.app.AlarmClockInfoProto;
import android.content.ClipDescriptionProto;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.util.MathUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.Random;
import libcore.util.EmptyArray;

public class NetworkStatsHistory implements Parcelable {
    public static final Creator<NetworkStatsHistory> CREATOR = new Creator<NetworkStatsHistory>() {
        public NetworkStatsHistory createFromParcel(Parcel in) {
            return new NetworkStatsHistory(in);
        }

        public NetworkStatsHistory[] newArray(int size) {
            return new NetworkStatsHistory[size];
        }
    };
    public static final int FIELD_ACTIVE_TIME = 1;
    public static final int FIELD_ALL = -1;
    public static final int FIELD_OPERATIONS = 32;
    public static final int FIELD_RX_BYTES = 2;
    public static final int FIELD_RX_PACKETS = 4;
    public static final int FIELD_TX_BYTES = 8;
    public static final int FIELD_TX_PACKETS = 16;
    private static final int VERSION_ADD_ACTIVE = 3;
    private static final int VERSION_ADD_PACKETS = 2;
    private static final int VERSION_INIT = 1;
    private long[] activeTime;
    private int bucketCount;
    private long bucketDuration;
    private long[] bucketStart;
    private long[] operations;
    private long[] rxBytes;
    private long[] rxPackets;
    private long totalBytes;
    private long totalRxBytes;
    private long totalTxBytes;
    private long[] txBytes;
    private long[] txPackets;

    public static class DataStreamUtils {
        @Deprecated
        public static long[] readFullLongArray(DataInputStream in) throws IOException {
            int size = in.readInt();
            if (size >= 0) {
                long[] values = new long[size];
                for (int i = 0; i < values.length; i++) {
                    values[i] = in.readLong();
                }
                return values;
            }
            throw new ProtocolException("negative array size");
        }

        public static long readVarLong(DataInputStream in) throws IOException {
            long result = 0;
            for (int shift = 0; shift < 64; shift += 7) {
                byte b = in.readByte();
                result |= ((long) (b & 127)) << shift;
                if ((b & 128) == 0) {
                    return result;
                }
            }
            throw new ProtocolException("malformed long");
        }

        public static void writeVarLong(DataOutputStream out, long value) throws IOException {
            while ((-128 & value) != 0) {
                out.writeByte((((int) value) & 127) | 128);
                value >>>= 7;
            }
            out.writeByte((int) value);
        }

        public static long[] readVarLongArray(DataInputStream in) throws IOException {
            int size = in.readInt();
            if (size == -1) {
                return null;
            }
            if (size >= 0) {
                long[] values = new long[size];
                for (int i = 0; i < values.length; i++) {
                    values[i] = readVarLong(in);
                }
                return values;
            }
            throw new ProtocolException("negative array size");
        }

        public static void writeVarLongArray(DataOutputStream out, long[] values, int size) throws IOException {
            if (values == null) {
                out.writeInt(-1);
            } else if (size <= values.length) {
                out.writeInt(size);
                for (int i = 0; i < size; i++) {
                    writeVarLong(out, values[i]);
                }
            } else {
                throw new IllegalArgumentException("size larger than length");
            }
        }
    }

    public static class Entry {
        public static final long UNKNOWN = -1;
        public long activeTime;
        public long bucketDuration;
        public long bucketStart;
        public long operations;
        public long rxBytes;
        public long rxPackets;
        public long txBytes;
        public long txPackets;
    }

    public static class ParcelUtils {
        public static long[] readLongArray(Parcel in) {
            int size = in.readInt();
            if (size == -1) {
                return null;
            }
            long[] values = new long[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = in.readLong();
            }
            return values;
        }

        public static void writeLongArray(Parcel out, long[] values, int size) {
            if (values == null) {
                out.writeInt(-1);
            } else if (size <= values.length) {
                out.writeInt(size);
                for (int i = 0; i < size; i++) {
                    out.writeLong(values[i]);
                }
            } else {
                throw new IllegalArgumentException("size larger than length");
            }
        }
    }

    public NetworkStatsHistory(long bucketDuration) {
        this(bucketDuration, 10, -1);
    }

    public NetworkStatsHistory(long bucketDuration, int initialSize) {
        this(bucketDuration, initialSize, -1);
    }

    public NetworkStatsHistory(long bucketDuration, int initialSize, int fields) {
        this.bucketDuration = bucketDuration;
        this.bucketStart = new long[initialSize];
        if ((fields & 1) != 0) {
            this.activeTime = new long[initialSize];
        }
        if ((fields & 2) != 0) {
            this.rxBytes = new long[initialSize];
        }
        if ((fields & 4) != 0) {
            this.rxPackets = new long[initialSize];
        }
        if ((fields & 8) != 0) {
            this.txBytes = new long[initialSize];
        }
        if ((fields & 16) != 0) {
            this.txPackets = new long[initialSize];
        }
        if ((fields & 32) != 0) {
            this.operations = new long[initialSize];
        }
        this.bucketCount = 0;
        this.totalBytes = 0;
        this.totalTxBytes = 0;
        this.totalRxBytes = 0;
    }

    public NetworkStatsHistory(NetworkStatsHistory existing, long bucketDuration) {
        this(bucketDuration, existing.estimateResizeBuckets(bucketDuration));
        recordEntireHistory(existing);
    }

    public NetworkStatsHistory(Parcel in) {
        this.bucketDuration = in.readLong();
        this.bucketStart = ParcelUtils.readLongArray(in);
        this.activeTime = ParcelUtils.readLongArray(in);
        this.rxBytes = ParcelUtils.readLongArray(in);
        this.rxPackets = ParcelUtils.readLongArray(in);
        this.txBytes = ParcelUtils.readLongArray(in);
        this.txPackets = ParcelUtils.readLongArray(in);
        this.operations = ParcelUtils.readLongArray(in);
        this.bucketCount = this.bucketStart.length;
        this.totalBytes = in.readLong();
        this.totalTxBytes = ArrayUtils.total(this.txBytes);
        this.totalRxBytes = ArrayUtils.total(this.rxBytes);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.bucketDuration);
        ParcelUtils.writeLongArray(out, this.bucketStart, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.activeTime, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.rxBytes, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.rxPackets, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.txBytes, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.txPackets, this.bucketCount);
        ParcelUtils.writeLongArray(out, this.operations, this.bucketCount);
        out.writeLong(this.totalBytes);
    }

    public NetworkStatsHistory(DataInputStream in) throws IOException {
        int version = in.readInt();
        switch (version) {
            case 1:
                this.bucketDuration = in.readLong();
                this.bucketStart = DataStreamUtils.readFullLongArray(in);
                this.rxBytes = DataStreamUtils.readFullLongArray(in);
                this.rxPackets = new long[this.bucketStart.length];
                this.txBytes = DataStreamUtils.readFullLongArray(in);
                this.txPackets = new long[this.bucketStart.length];
                this.operations = new long[this.bucketStart.length];
                this.bucketCount = this.bucketStart.length;
                this.totalBytes = ArrayUtils.total(this.rxBytes) + ArrayUtils.total(this.txBytes);
                this.totalTxBytes = ArrayUtils.total(this.txBytes);
                this.totalRxBytes = ArrayUtils.total(this.rxBytes);
                break;
            case 2:
            case 3:
                long[] readVarLongArray;
                this.bucketDuration = in.readLong();
                this.bucketStart = DataStreamUtils.readVarLongArray(in);
                if (version >= 3) {
                    readVarLongArray = DataStreamUtils.readVarLongArray(in);
                } else {
                    readVarLongArray = new long[this.bucketStart.length];
                }
                this.activeTime = readVarLongArray;
                this.rxBytes = DataStreamUtils.readVarLongArray(in);
                this.rxPackets = DataStreamUtils.readVarLongArray(in);
                this.txBytes = DataStreamUtils.readVarLongArray(in);
                this.txPackets = DataStreamUtils.readVarLongArray(in);
                this.operations = DataStreamUtils.readVarLongArray(in);
                this.bucketCount = this.bucketStart.length;
                this.totalBytes = ArrayUtils.total(this.rxBytes) + ArrayUtils.total(this.txBytes);
                this.totalTxBytes = ArrayUtils.total(this.txBytes);
                this.totalRxBytes = ArrayUtils.total(this.rxBytes);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected version: ");
                stringBuilder.append(version);
                throw new ProtocolException(stringBuilder.toString());
        }
        if (this.rxBytes == null || this.rxPackets == null || this.txBytes == null || this.txPackets == null || this.operations == null) {
            throw new ProtocolException("Invalid input values");
        } else if (this.bucketStart.length != this.bucketCount || this.rxBytes.length != this.bucketCount || this.rxPackets.length != this.bucketCount || this.txBytes.length != this.bucketCount || this.txPackets.length != this.bucketCount || this.operations.length != this.bucketCount) {
            throw new ProtocolException("Mismatched history lengths");
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(3);
        out.writeLong(this.bucketDuration);
        DataStreamUtils.writeVarLongArray(out, this.bucketStart, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.activeTime, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.rxBytes, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.rxPackets, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.txBytes, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.txPackets, this.bucketCount);
        DataStreamUtils.writeVarLongArray(out, this.operations, this.bucketCount);
    }

    public int describeContents() {
        return 0;
    }

    public int size() {
        return this.bucketCount;
    }

    public long getBucketDuration() {
        return this.bucketDuration;
    }

    public long getStart() {
        if (this.bucketCount > 0) {
            return this.bucketStart[0];
        }
        return Long.MAX_VALUE;
    }

    public long getEnd() {
        if (this.bucketCount > 0) {
            return this.bucketStart[this.bucketCount - 1] + this.bucketDuration;
        }
        return Long.MIN_VALUE;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public long getTotalTxBytes() {
        return this.totalTxBytes;
    }

    public long getTotalRxBytes() {
        return this.totalRxBytes;
    }

    public int getIndexBefore(long time) {
        int index;
        int index2 = Arrays.binarySearch(this.bucketStart, 0, this.bucketCount, time);
        if (index2 < 0) {
            index = (~index2) - 1;
        } else {
            index = index2 - 1;
        }
        return MathUtils.constrain(index, 0, this.bucketCount - 1);
    }

    public int getIndexAfter(long time) {
        int index = Arrays.binarySearch(this.bucketStart, 0, this.bucketCount, time);
        if (index < 0) {
            index = ~index;
        } else {
            index++;
        }
        return MathUtils.constrain(index, 0, this.bucketCount - 1);
    }

    public Entry getValues(int i, Entry recycle) {
        Entry entry = recycle != null ? recycle : new Entry();
        entry.bucketStart = this.bucketStart[i];
        entry.bucketDuration = this.bucketDuration;
        entry.activeTime = getLong(this.activeTime, i, -1);
        entry.rxBytes = getLong(this.rxBytes, i, -1);
        entry.rxPackets = getLong(this.rxPackets, i, -1);
        entry.txBytes = getLong(this.txBytes, i, -1);
        entry.txPackets = getLong(this.txPackets, i, -1);
        entry.operations = getLong(this.operations, i, -1);
        return entry;
    }

    public void setValues(int i, Entry entry) {
        if (this.rxBytes != null) {
            this.totalBytes -= this.rxBytes[i];
        }
        if (this.txBytes != null) {
            this.totalBytes -= this.txBytes[i];
        }
        this.bucketStart[i] = entry.bucketStart;
        setLong(this.activeTime, i, entry.activeTime);
        setLong(this.rxBytes, i, entry.rxBytes);
        setLong(this.rxPackets, i, entry.rxPackets);
        setLong(this.txBytes, i, entry.txBytes);
        setLong(this.txPackets, i, entry.txPackets);
        setLong(this.operations, i, entry.operations);
        if (this.rxBytes != null) {
            this.totalBytes += this.rxBytes[i];
        }
        if (this.txBytes != null) {
            this.totalBytes += this.txBytes[i];
        }
    }

    @Deprecated
    public void recordData(long start, long end, long rxBytes, long txBytes) {
        recordData(start, end, new android.net.NetworkStats.Entry(NetworkStats.IFACE_ALL, -1, 0, 0, rxBytes, 0, txBytes, 0, 0));
    }

    public void recordData(long start, long end, android.net.NetworkStats.Entry entry) {
        long j = start;
        long j2 = end;
        android.net.NetworkStats.Entry entry2 = entry;
        long rxBytes = entry2.rxBytes;
        long rxPackets = entry2.rxPackets;
        long txBytes = entry2.txBytes;
        long txPackets = entry2.txPackets;
        long operations = entry2.operations;
        if (entry.isNegative()) {
            Log.d("NetworkStatsHistory", "tried recording negative data.");
            return;
        }
        long rxBytes2 = rxBytes;
        if (!entry.isEmpty()) {
            long operations2;
            long j3;
            ensureBuckets(start, end);
            long rxBytes3 = rxBytes2;
            rxBytes2 = j2 - j;
            int i = getIndexAfter(j2);
            while (i >= 0) {
                operations2 = operations;
                operations = this.bucketStart[i];
                int i2 = i;
                long curEnd = this.bucketDuration + operations;
                if (curEnd < j) {
                    j3 = txPackets;
                    break;
                }
                long txPackets2;
                if (operations <= j2) {
                    j = Math.min(curEnd, j2) - Math.max(operations, j);
                    if (j > 0) {
                        j2 = (rxBytes3 * j) / rxBytes2;
                        curEnd = (rxPackets * j) / rxBytes2;
                        operations = (txBytes * j) / rxBytes2;
                        j3 = txPackets;
                        long fracTxPackets = (txPackets * j) / rxBytes2;
                        long fracOperations = (operations2 * j) / rxBytes2;
                        txPackets = i2;
                        addLong(this.activeTime, txPackets, j);
                        addLong(this.rxBytes, txPackets, j2);
                        rxBytes3 -= j2;
                        addLong(this.rxPackets, txPackets, curEnd);
                        rxPackets -= curEnd;
                        addLong(this.txBytes, txPackets, operations);
                        txBytes -= operations;
                        j2 = fracTxPackets;
                        addLong(this.txPackets, txPackets, j2);
                        txPackets2 = j3 - j2;
                        j2 = fracOperations;
                        addLong(this.operations, txPackets, j2);
                        rxBytes2 -= j;
                        operations = operations2 - j2;
                        i = txPackets - 1;
                        txPackets = txPackets2;
                        j = start;
                        j2 = end;
                        entry2 = entry;
                    }
                }
                operations = operations2;
                long j4 = txPackets;
                txPackets = i2;
                txPackets2 = j4;
                i = txPackets - 1;
                txPackets = txPackets2;
                j = start;
                j2 = end;
                entry2 = entry;
            }
            j3 = txPackets;
            operations2 = operations;
            android.net.NetworkStats.Entry entry3 = entry;
            this.totalBytes += entry3.rxBytes + entry3.txBytes;
            this.totalRxBytes += entry3.rxBytes;
            this.totalTxBytes += entry3.txBytes;
        }
    }

    public void recordEntireHistory(NetworkStatsHistory input) {
        recordHistory(input, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public void recordHistory(NetworkStatsHistory input, long start, long end) {
        NetworkStatsHistory networkStatsHistory = input;
        android.net.NetworkStats.Entry entry = new android.net.NetworkStats.Entry(NetworkStats.IFACE_ALL, -1, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < networkStatsHistory.bucketCount; i++) {
            long bucketStart = networkStatsHistory.bucketStart[i];
            long bucketEnd = networkStatsHistory.bucketDuration + bucketStart;
            if (bucketStart >= start && bucketEnd <= end) {
                entry.rxBytes = getLong(networkStatsHistory.rxBytes, i, 0);
                entry.rxPackets = getLong(networkStatsHistory.rxPackets, i, 0);
                entry.txBytes = getLong(networkStatsHistory.txBytes, i, 0);
                entry.txPackets = getLong(networkStatsHistory.txPackets, i, 0);
                entry.operations = getLong(networkStatsHistory.operations, i, 0);
                recordData(bucketStart, bucketEnd, entry);
            }
        }
    }

    private void ensureBuckets(long start, long end) {
        end += (this.bucketDuration - (end % this.bucketDuration)) % this.bucketDuration;
        long now = start - (start % this.bucketDuration);
        while (now < end) {
            int index = Arrays.binarySearch(this.bucketStart, 0, this.bucketCount, now);
            if (index < 0) {
                insertBucket(~index, now);
            }
            now += this.bucketDuration;
        }
    }

    private void insertBucket(int index, long start) {
        int newLength;
        if (this.bucketCount >= this.bucketStart.length) {
            newLength = (Math.max(this.bucketStart.length, 10) * 3) / 2;
            this.bucketStart = Arrays.copyOf(this.bucketStart, newLength);
            if (this.activeTime != null) {
                this.activeTime = Arrays.copyOf(this.activeTime, newLength);
            }
            if (this.rxBytes != null) {
                this.rxBytes = Arrays.copyOf(this.rxBytes, newLength);
            }
            if (this.rxPackets != null) {
                this.rxPackets = Arrays.copyOf(this.rxPackets, newLength);
            }
            if (this.txBytes != null) {
                this.txBytes = Arrays.copyOf(this.txBytes, newLength);
            }
            if (this.txPackets != null) {
                this.txPackets = Arrays.copyOf(this.txPackets, newLength);
            }
            if (this.operations != null) {
                this.operations = Arrays.copyOf(this.operations, newLength);
            }
        }
        if (index < this.bucketCount) {
            newLength = index + 1;
            int length = this.bucketCount - index;
            System.arraycopy(this.bucketStart, index, this.bucketStart, newLength, length);
            if (this.activeTime != null) {
                System.arraycopy(this.activeTime, index, this.activeTime, newLength, length);
            }
            if (this.rxBytes != null) {
                System.arraycopy(this.rxBytes, index, this.rxBytes, newLength, length);
            }
            if (this.rxPackets != null) {
                System.arraycopy(this.rxPackets, index, this.rxPackets, newLength, length);
            }
            if (this.txBytes != null) {
                System.arraycopy(this.txBytes, index, this.txBytes, newLength, length);
            }
            if (this.txPackets != null) {
                System.arraycopy(this.txPackets, index, this.txPackets, newLength, length);
            }
            if (this.operations != null) {
                System.arraycopy(this.operations, index, this.operations, newLength, length);
            }
        }
        this.bucketStart[index] = start;
        setLong(this.activeTime, index, 0);
        setLong(this.rxBytes, index, 0);
        setLong(this.rxPackets, index, 0);
        setLong(this.txBytes, index, 0);
        setLong(this.txPackets, index, 0);
        setLong(this.operations, index, 0);
        this.bucketCount++;
    }

    public void clear() {
        this.bucketStart = EmptyArray.LONG;
        if (this.activeTime != null) {
            this.activeTime = EmptyArray.LONG;
        }
        if (this.rxBytes != null) {
            this.rxBytes = EmptyArray.LONG;
        }
        if (this.rxPackets != null) {
            this.rxPackets = EmptyArray.LONG;
        }
        if (this.txBytes != null) {
            this.txBytes = EmptyArray.LONG;
        }
        if (this.txPackets != null) {
            this.txPackets = EmptyArray.LONG;
        }
        if (this.operations != null) {
            this.operations = EmptyArray.LONG;
        }
        this.bucketCount = 0;
        this.totalBytes = 0;
    }

    @Deprecated
    public void removeBucketsBefore(long cutoff) {
        int i = 0;
        while (i < this.bucketCount) {
            if (this.bucketDuration + this.bucketStart[i] > cutoff) {
                break;
            }
            i++;
        }
        if (i > 0) {
            int length = this.bucketStart.length;
            this.bucketStart = Arrays.copyOfRange(this.bucketStart, i, length);
            if (this.activeTime != null) {
                this.activeTime = Arrays.copyOfRange(this.activeTime, i, length);
            }
            if (this.rxBytes != null) {
                this.rxBytes = Arrays.copyOfRange(this.rxBytes, i, length);
            }
            if (this.rxPackets != null) {
                this.rxPackets = Arrays.copyOfRange(this.rxPackets, i, length);
            }
            if (this.txBytes != null) {
                this.txBytes = Arrays.copyOfRange(this.txBytes, i, length);
            }
            if (this.txPackets != null) {
                this.txPackets = Arrays.copyOfRange(this.txPackets, i, length);
            }
            if (this.operations != null) {
                this.operations = Arrays.copyOfRange(this.operations, i, length);
            }
            this.bucketCount -= i;
        }
    }

    public Entry getValues(long start, long end, Entry recycle) {
        return getValues(start, end, Long.MAX_VALUE, recycle);
    }

    public Entry getValues(long start, long end, long now, Entry recycle) {
        long j = start;
        long j2 = end;
        Entry entry = recycle != null ? recycle : new Entry();
        entry.bucketDuration = j2 - j;
        entry.bucketStart = j;
        long j3 = -1;
        entry.activeTime = this.activeTime != null ? 0 : -1;
        entry.rxBytes = this.rxBytes != null ? 0 : -1;
        entry.rxPackets = this.rxPackets != null ? 0 : -1;
        entry.txBytes = this.txBytes != null ? 0 : -1;
        entry.txPackets = this.txPackets != null ? 0 : -1;
        if (this.operations != null) {
            j3 = 0;
        }
        entry.operations = j3;
        int i = getIndexAfter(j2);
        while (i >= 0) {
            long curStart = this.bucketStart[i];
            long curEnd = curStart + this.bucketDuration;
            if (curEnd <= j) {
                break;
            }
            if (curStart < j2) {
                long overlap;
                boolean activeBucket = curStart < now && curEnd > now;
                if (activeBucket) {
                    overlap = this.bucketDuration;
                } else {
                    overlap = (curEnd < j2 ? curEnd : j2) - (curStart > j ? curStart : j);
                }
                if (overlap > 0) {
                    if (this.activeTime != null) {
                        entry.activeTime += (this.activeTime[i] * overlap) / this.bucketDuration;
                    }
                    if (this.rxBytes != null) {
                        entry.rxBytes += (this.rxBytes[i] * overlap) / this.bucketDuration;
                    }
                    if (this.rxPackets != null) {
                        entry.rxPackets += (this.rxPackets[i] * overlap) / this.bucketDuration;
                    }
                    if (this.txBytes != null) {
                        entry.txBytes += (this.txBytes[i] * overlap) / this.bucketDuration;
                    }
                    if (this.txPackets != null) {
                        entry.txPackets += (this.txPackets[i] * overlap) / this.bucketDuration;
                    }
                    if (this.operations != null) {
                        entry.operations += (this.operations[i] * overlap) / this.bucketDuration;
                    }
                }
            }
            i--;
            j = start;
            j2 = end;
        }
        return entry;
    }

    @Deprecated
    public void generateRandom(long start, long end, long bytes) {
        long j = bytes;
        Random r = new Random();
        float fractionRx = r.nextFloat();
        long rxBytes = (long) (((float) j) * fractionRx);
        long j2 = (long) (((float) j) * (1.0f - fractionRx));
        long txBytes = j2;
        generateRandom(start, end, rxBytes, rxBytes / 1024, j2, j2 / 1024, rxBytes / 2048, r);
    }

    @Deprecated
    public void generateRandom(long start, long end, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations, Random r) {
        long j = end;
        Random random = r;
        ensureBuckets(start, end);
        android.net.NetworkStats.Entry entry = new android.net.NetworkStats.Entry(NetworkStats.IFACE_ALL, -1, 0, 0, 0, 0, 0, 0, 0);
        long rxBytes2 = rxBytes;
        long rxPackets2 = rxPackets;
        long txBytes2 = txBytes;
        long txPackets2 = txPackets;
        long operations2 = operations;
        while (true) {
            android.net.NetworkStats.Entry entry2;
            android.net.NetworkStats.Entry entry3 = entry2;
            if (rxBytes2 > 1024 || rxPackets2 > 128 || txBytes2 > 1024 || txPackets2 > 128 || operations2 > 32) {
                long curStart = randomLong(random, start, j);
                long curEnd = curStart + randomLong(random, 0, (j - curStart) / 2);
                entry3.rxBytes = randomLong(random, 0, rxBytes2);
                entry3.rxPackets = randomLong(random, 0, rxPackets2);
                entry3.txBytes = randomLong(random, 0, txBytes2);
                entry3.txPackets = randomLong(random, 0, txPackets2);
                entry3.operations = randomLong(random, 0, operations2);
                rxBytes2 -= entry3.rxBytes;
                rxPackets2 -= entry3.rxPackets;
                txBytes2 -= entry3.txBytes;
                txPackets2 -= entry3.txPackets;
                operations2 -= entry3.operations;
                recordData(curStart, curEnd, entry3);
                entry2 = entry3;
                j = end;
            } else {
                return;
            }
        }
    }

    public static long randomLong(Random r, long start, long end) {
        return (long) (((float) start) + (r.nextFloat() * ((float) (end - start))));
    }

    public boolean intersects(long start, long end) {
        long dataStart = getStart();
        long dataEnd = getEnd();
        if (start >= dataStart && start <= dataEnd) {
            return true;
        }
        if (end >= dataStart && end <= dataEnd) {
            return true;
        }
        if (dataStart >= start && dataStart <= end) {
            return true;
        }
        if (dataEnd < start || dataEnd > end) {
            return false;
        }
        return true;
    }

    public void dump(IndentingPrintWriter pw, boolean fullHistory) {
        pw.print("NetworkStatsHistory: bucketDuration=");
        pw.println(this.bucketDuration / 1000);
        pw.increaseIndent();
        int start = 0;
        if (!fullHistory) {
            start = Math.max(0, this.bucketCount - 32);
        }
        if (start > 0) {
            pw.print("(omitting ");
            pw.print(start);
            pw.println(" buckets)");
        }
        for (int i = start; i < this.bucketCount; i++) {
            pw.print("st=");
            pw.print(this.bucketStart[i] / 1000);
            if (this.rxBytes != null) {
                pw.print(" rb=");
                pw.print(this.rxBytes[i]);
            }
            if (this.rxPackets != null) {
                pw.print(" rp=");
                pw.print(this.rxPackets[i]);
            }
            if (this.txBytes != null) {
                pw.print(" tb=");
                pw.print(this.txBytes[i]);
            }
            if (this.txPackets != null) {
                pw.print(" tp=");
                pw.print(this.txPackets[i]);
            }
            if (this.operations != null) {
                pw.print(" op=");
                pw.print(this.operations[i]);
            }
            pw.println();
        }
        pw.decreaseIndent();
    }

    public void dumpCheckin(PrintWriter pw) {
        pw.print("d,");
        pw.print(this.bucketDuration / 1000);
        pw.println();
        for (int i = 0; i < this.bucketCount; i++) {
            pw.print("b,");
            pw.print(this.bucketStart[i] / 1000);
            pw.print(',');
            if (this.rxBytes != null) {
                pw.print(this.rxBytes[i]);
            } else {
                pw.print("*");
            }
            pw.print(',');
            if (this.rxPackets != null) {
                pw.print(this.rxPackets[i]);
            } else {
                pw.print("*");
            }
            pw.print(',');
            if (this.txBytes != null) {
                pw.print(this.txBytes[i]);
            } else {
                pw.print("*");
            }
            pw.print(',');
            if (this.txPackets != null) {
                pw.print(this.txPackets[i]);
            } else {
                pw.print("*");
            }
            pw.print(',');
            if (this.operations != null) {
                pw.print(this.operations[i]);
            } else {
                pw.print("*");
            }
            pw.println();
        }
    }

    public void writeToProto(ProtoOutputStream proto, long tag) {
        long start = proto.start(tag);
        proto.write(AlarmClockInfoProto.TRIGGER_TIME_MS, this.bucketDuration);
        for (int i = 0; i < this.bucketCount; i++) {
            long startBucket = proto.start(2246267895810L);
            proto.write(AlarmClockInfoProto.TRIGGER_TIME_MS, this.bucketStart[i]);
            writeToProto(proto, 1112396529666L, this.rxBytes, i);
            writeToProto(proto, 1112396529667L, this.rxPackets, i);
            writeToProto(proto, ClipDescriptionProto.TIMESTAMP_MS, this.txBytes, i);
            writeToProto(proto, 1112396529669L, this.txPackets, i);
            writeToProto(proto, 1112396529670L, this.operations, i);
            proto.end(startBucket);
        }
        proto.end(start);
    }

    private static void writeToProto(ProtoOutputStream proto, long tag, long[] array, int index) {
        if (array != null) {
            proto.write(tag, array[index]);
        }
    }

    public String toString() {
        CharArrayWriter writer = new CharArrayWriter();
        dump(new IndentingPrintWriter(writer, "  "), false);
        return writer.toString();
    }

    private static long getLong(long[] array, int i, long value) {
        return array != null ? array[i] : value;
    }

    private static void setLong(long[] array, int i, long value) {
        if (array != null) {
            array[i] = value;
        }
    }

    private static void addLong(long[] array, int i, long value) {
        if (array != null) {
            array[i] = array[i] + value;
        }
    }

    public int estimateResizeBuckets(long newBucketDuration) {
        return (int) ((((long) size()) * getBucketDuration()) / newBucketDuration);
    }
}
