package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import libcore.util.EmptyArray;

public class NetworkStats implements Parcelable {
    private static final String CLATD_INTERFACE_PREFIX = "v4-";
    public static final Creator<NetworkStats> CREATOR = new Creator<NetworkStats>() {
        public NetworkStats createFromParcel(Parcel in) {
            return new NetworkStats(in);
        }

        public NetworkStats[] newArray(int size) {
            return new NetworkStats[size];
        }
    };
    public static final int DEFAULT_NETWORK_ALL = -1;
    public static final int DEFAULT_NETWORK_NO = 0;
    public static final int DEFAULT_NETWORK_YES = 1;
    public static final String IFACE_ALL = null;
    public static final String[] INTERFACES_ALL = null;
    private static final int IPV4V6_HEADER_DELTA = 20;
    public static final int METERED_ALL = -1;
    public static final int METERED_NO = 0;
    public static final int METERED_YES = 1;
    public static final int ROAMING_ALL = -1;
    public static final int ROAMING_NO = 0;
    public static final int ROAMING_YES = 1;
    public static final int SET_ALL = -1;
    public static final int SET_DBG_VPN_IN = 1001;
    public static final int SET_DBG_VPN_OUT = 1002;
    public static final int SET_DEBUG_START = 1000;
    public static final int SET_DEFAULT = 0;
    public static final int SET_FOREGROUND = 1;
    public static final int SET_STATIC = 9;
    public static final int STATS_PER_IFACE = 0;
    public static final int STATS_PER_UID = 1;
    private static final String TAG = "NetworkStats";
    public static final int TAG_ALL = -1;
    public static final int TAG_NONE = 0;
    public static final int UID_ALL = -1;
    private int[] actUid;
    private int capacity;
    private int[] defaultNetwork;
    private long elapsedRealtime;
    private String[] iface;
    private int[] metered;
    private long[] operations;
    private String[] proc;
    private int[] roaming;
    private long[] rxBytes;
    private long[] rxPackets;
    private int[] set;
    private int size;
    private int[] tag;
    private long[] txBytes;
    private long[] txPackets;
    private int[] uid;

    public static class Entry {
        public int actUid;
        public int defaultNetwork;
        public String iface;
        public int metered;
        public long operations;
        public String proc;
        public int roaming;
        public long rxBytes;
        public long rxPackets;
        public int set;
        public int tag;
        public long txBytes;
        public long txPackets;
        public int uid;

        public Entry() {
            this(NetworkStats.IFACE_ALL, -1, 0, 0, 0, 0, 0, 0, 0);
        }

        public Entry(long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
            this(NetworkStats.IFACE_ALL, -1, 0, 0, rxBytes, rxPackets, txBytes, txPackets, operations);
        }

        public Entry(String iface, int uid, int set, int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
            this(iface, uid, set, tag, 0, 0, 0, rxBytes, rxPackets, txBytes, txPackets, operations);
        }

        public Entry(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
            this(iface, uid, set, tag, metered, roaming, defaultNetwork, rxBytes, rxPackets, txBytes, txPackets, operations, "");
        }

        public Entry(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations, String proc) {
            this(iface, uid, set, tag, metered, roaming, defaultNetwork, rxBytes, rxPackets, txBytes, txPackets, operations, proc, -1);
        }

        public Entry(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations, String proc, int actUid) {
            this.proc = "";
            this.actUid = -1;
            this.iface = iface;
            this.uid = uid;
            this.set = set;
            this.tag = tag;
            this.metered = metered;
            this.roaming = roaming;
            this.defaultNetwork = defaultNetwork;
            this.rxBytes = rxBytes;
            this.rxPackets = rxPackets;
            this.txBytes = txBytes;
            this.txPackets = txPackets;
            this.operations = operations;
            this.proc = proc;
            this.actUid = actUid;
        }

        public boolean isNegative() {
            return this.rxBytes < 0 || this.rxPackets < 0 || this.txBytes < 0 || this.txPackets < 0 || this.operations < 0;
        }

        public boolean isEmpty() {
            return this.rxBytes == 0 && this.rxPackets == 0 && this.txBytes == 0 && this.txPackets == 0 && this.operations == 0;
        }

        public void add(Entry another) {
            this.rxBytes += another.rxBytes;
            this.rxPackets += another.rxPackets;
            this.txBytes += another.txBytes;
            this.txPackets += another.txPackets;
            this.operations += another.operations;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("iface=");
            builder.append(this.iface);
            builder.append(" uid=");
            builder.append(this.uid);
            builder.append(" set=");
            builder.append(NetworkStats.setToString(this.set));
            builder.append(" tag=");
            builder.append(NetworkStats.tagToString(this.tag));
            builder.append(" metered=");
            builder.append(NetworkStats.meteredToString(this.metered));
            builder.append(" roaming=");
            builder.append(NetworkStats.roamingToString(this.roaming));
            builder.append(" defaultNetwork=");
            builder.append(NetworkStats.defaultNetworkToString(this.defaultNetwork));
            builder.append(" rxBytes=");
            builder.append(this.rxBytes);
            builder.append(" rxPackets=");
            builder.append(this.rxPackets);
            builder.append(" txBytes=");
            builder.append(this.txBytes);
            builder.append(" txPackets=");
            builder.append(this.txPackets);
            builder.append(" operations=");
            builder.append(this.operations);
            builder.append(" proc=");
            builder.append(this.proc);
            builder.append(" actUid=");
            builder.append(this.actUid);
            return builder.toString();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry e = (Entry) o;
            if (this.uid == e.uid && this.set == e.set && this.tag == e.tag && this.metered == e.metered && this.roaming == e.roaming && this.defaultNetwork == e.defaultNetwork && this.rxBytes == e.rxBytes && this.rxPackets == e.rxPackets && this.txBytes == e.txBytes && this.txPackets == e.txPackets && this.operations == e.operations && this.iface.equals(e.iface) && this.proc.equals(e.proc) && this.actUid == e.actUid) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(this.uid), Integer.valueOf(this.set), Integer.valueOf(this.tag), Integer.valueOf(this.metered), Integer.valueOf(this.roaming), Integer.valueOf(this.defaultNetwork), this.iface});
        }
    }

    public interface NonMonotonicObserver<C> {
        void foundNonMonotonic(NetworkStats networkStats, int i, NetworkStats networkStats2, int i2, C c);

        void foundNonMonotonic(NetworkStats networkStats, int i, C c);
    }

    public NetworkStats(long elapsedRealtime, int initialSize) {
        this.elapsedRealtime = elapsedRealtime;
        this.size = 0;
        if (initialSize > 0) {
            this.capacity = initialSize;
            this.iface = new String[initialSize];
            this.uid = new int[initialSize];
            this.set = new int[initialSize];
            this.tag = new int[initialSize];
            this.metered = new int[initialSize];
            this.roaming = new int[initialSize];
            this.defaultNetwork = new int[initialSize];
            this.rxBytes = new long[initialSize];
            this.rxPackets = new long[initialSize];
            this.txBytes = new long[initialSize];
            this.txPackets = new long[initialSize];
            this.operations = new long[initialSize];
            this.proc = new String[initialSize];
            this.actUid = new int[initialSize];
            return;
        }
        clear();
    }

    public NetworkStats(Parcel parcel) {
        this.elapsedRealtime = parcel.readLong();
        this.size = parcel.readInt();
        this.capacity = parcel.readInt();
        this.iface = parcel.createStringArray();
        this.uid = parcel.createIntArray();
        this.set = parcel.createIntArray();
        this.tag = parcel.createIntArray();
        this.metered = parcel.createIntArray();
        this.roaming = parcel.createIntArray();
        this.defaultNetwork = parcel.createIntArray();
        this.rxBytes = parcel.createLongArray();
        this.rxPackets = parcel.createLongArray();
        this.txBytes = parcel.createLongArray();
        this.txPackets = parcel.createLongArray();
        this.operations = parcel.createLongArray();
        this.proc = parcel.createStringArray();
        this.actUid = parcel.createIntArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.elapsedRealtime);
        dest.writeInt(this.size);
        dest.writeInt(this.capacity);
        dest.writeStringArray(this.iface);
        dest.writeIntArray(this.uid);
        dest.writeIntArray(this.set);
        dest.writeIntArray(this.tag);
        dest.writeIntArray(this.metered);
        dest.writeIntArray(this.roaming);
        dest.writeIntArray(this.defaultNetwork);
        dest.writeLongArray(this.rxBytes);
        dest.writeLongArray(this.rxPackets);
        dest.writeLongArray(this.txBytes);
        dest.writeLongArray(this.txPackets);
        dest.writeLongArray(this.operations);
        dest.writeStringArray(this.proc);
        dest.writeIntArray(this.actUid);
    }

    public NetworkStats clone() {
        NetworkStats clone = new NetworkStats(this.elapsedRealtime, this.size);
        Entry entry = null;
        for (int i = 0; i < this.size; i++) {
            entry = getValues(i, entry);
            clone.addValues(entry);
        }
        return clone;
    }

    public void clear() {
        this.capacity = 0;
        this.iface = EmptyArray.STRING;
        this.uid = EmptyArray.INT;
        this.set = EmptyArray.INT;
        this.tag = EmptyArray.INT;
        this.metered = EmptyArray.INT;
        this.roaming = EmptyArray.INT;
        this.defaultNetwork = EmptyArray.INT;
        this.rxBytes = EmptyArray.LONG;
        this.rxPackets = EmptyArray.LONG;
        this.txBytes = EmptyArray.LONG;
        this.txPackets = EmptyArray.LONG;
        this.operations = EmptyArray.LONG;
        this.proc = EmptyArray.STRING;
        this.actUid = EmptyArray.INT;
    }

    @VisibleForTesting
    public NetworkStats addIfaceValues(String iface, long rxBytes, long rxPackets, long txBytes, long txPackets) {
        return addValues(iface, -1, 0, 0, rxBytes, rxPackets, txBytes, txPackets, 0);
    }

    @VisibleForTesting
    public NetworkStats addValues(String iface, int uid, int set, int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        return addValues(new Entry(iface, uid, set, tag, rxBytes, rxPackets, txBytes, txPackets, operations));
    }

    @VisibleForTesting
    public NetworkStats addValues(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        Entry entry = r0;
        Entry entry2 = new Entry(iface, uid, set, tag, metered, roaming, defaultNetwork, rxBytes, rxPackets, txBytes, txPackets, operations);
        return addValues(entry);
    }

    public NetworkStats addValues(Entry entry) {
        if (this.size >= this.capacity) {
            int newLength = (Math.max(this.size, 10) * 3) / 2;
            this.iface = (String[]) Arrays.copyOf(this.iface, newLength);
            this.uid = Arrays.copyOf(this.uid, newLength);
            this.set = Arrays.copyOf(this.set, newLength);
            this.tag = Arrays.copyOf(this.tag, newLength);
            this.metered = Arrays.copyOf(this.metered, newLength);
            this.roaming = Arrays.copyOf(this.roaming, newLength);
            this.defaultNetwork = Arrays.copyOf(this.defaultNetwork, newLength);
            this.rxBytes = Arrays.copyOf(this.rxBytes, newLength);
            this.rxPackets = Arrays.copyOf(this.rxPackets, newLength);
            this.txBytes = Arrays.copyOf(this.txBytes, newLength);
            this.txPackets = Arrays.copyOf(this.txPackets, newLength);
            this.operations = Arrays.copyOf(this.operations, newLength);
            this.proc = (String[]) Arrays.copyOf(this.proc, newLength);
            this.actUid = Arrays.copyOf(this.actUid, newLength);
            this.capacity = newLength;
        }
        setValues(this.size, entry);
        this.size++;
        return this;
    }

    private void setValues(int i, Entry entry) {
        this.iface[i] = entry.iface;
        this.uid[i] = entry.uid;
        this.set[i] = entry.set;
        this.tag[i] = entry.tag;
        this.metered[i] = entry.metered;
        this.roaming[i] = entry.roaming;
        this.defaultNetwork[i] = entry.defaultNetwork;
        this.rxBytes[i] = entry.rxBytes;
        this.rxPackets[i] = entry.rxPackets;
        this.txBytes[i] = entry.txBytes;
        this.txPackets[i] = entry.txPackets;
        this.operations[i] = entry.operations;
        this.proc[i] = entry.proc;
        this.actUid[i] = entry.actUid;
    }

    public Entry getValues(int i, Entry recycle) {
        Entry entry = recycle != null ? recycle : new Entry();
        entry.iface = this.iface[i];
        entry.uid = this.uid[i];
        entry.set = this.set[i];
        entry.tag = this.tag[i];
        entry.metered = this.metered[i];
        entry.roaming = this.roaming[i];
        entry.defaultNetwork = this.defaultNetwork[i];
        entry.rxBytes = this.rxBytes[i];
        entry.rxPackets = this.rxPackets[i];
        entry.txBytes = this.txBytes[i];
        entry.txPackets = this.txPackets[i];
        entry.operations = this.operations[i];
        entry.proc = this.proc[i];
        entry.actUid = this.actUid[i];
        return entry;
    }

    public long getElapsedRealtime() {
        return this.elapsedRealtime;
    }

    public void setElapsedRealtime(long time) {
        this.elapsedRealtime = time;
    }

    public long getElapsedRealtimeAge() {
        return SystemClock.elapsedRealtime() - this.elapsedRealtime;
    }

    public int size() {
        return this.size;
    }

    @VisibleForTesting
    public int internalSize() {
        return this.capacity;
    }

    @Deprecated
    public NetworkStats combineValues(String iface, int uid, int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        return combineValues(iface, uid, 0, tag, rxBytes, rxPackets, txBytes, txPackets, operations);
    }

    public NetworkStats combineValues(String iface, int uid, int set, int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        return combineValues(new Entry(iface, uid, set, tag, rxBytes, rxPackets, txBytes, txPackets, operations));
    }

    public NetworkStats combineValues(Entry entry) {
        int i = findIndex(entry.iface, entry.uid, entry.set, entry.tag, entry.metered, entry.roaming, entry.defaultNetwork, entry.proc, entry.actUid);
        if (i == -1) {
            addValues(entry);
        } else {
            long[] jArr = this.rxBytes;
            jArr[i] = jArr[i] + entry.rxBytes;
            jArr = this.rxPackets;
            jArr[i] = jArr[i] + entry.rxPackets;
            jArr = this.txBytes;
            jArr[i] = jArr[i] + entry.txBytes;
            jArr = this.txPackets;
            jArr[i] = jArr[i] + entry.txPackets;
            jArr = this.operations;
            jArr[i] = jArr[i] + entry.operations;
        }
        return this;
    }

    public void combineAllValues(NetworkStats another) {
        Entry entry = null;
        for (int i = 0; i < another.size; i++) {
            entry = another.getValues(i, entry);
            combineValues(entry);
        }
    }

    public int findIndex(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork) {
        int i = 0;
        while (i < this.size) {
            if (uid == this.uid[i] && set == this.set[i] && tag == this.tag[i] && metered == this.metered[i] && roaming == this.roaming[i] && defaultNetwork == this.defaultNetwork[i] && Objects.equals(iface, this.iface[i])) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int findIndex(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, String proc, int actUid) {
        int i = 0;
        while (i < this.size) {
            if (uid == this.uid[i] && set == this.set[i] && tag == this.tag[i] && metered == this.metered[i] && roaming == this.roaming[i] && Objects.equals(iface, this.iface[i]) && defaultNetwork == this.defaultNetwork[i] && Objects.equals(proc, this.proc[i]) && actUid == this.actUid[i]) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int findIndexHinted(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, String proc, int actUid, int hintIndex) {
        for (int offset = 0; offset < this.size; offset++) {
            int i;
            int halfOffset = offset / 2;
            if (offset % 2 == 0) {
                i = (hintIndex + halfOffset) % this.size;
            } else {
                i = (((this.size + hintIndex) - halfOffset) - 1) % this.size;
            }
            if (uid == this.uid[i] && set == this.set[i] && tag == this.tag[i] && metered == this.metered[i] && roaming == this.roaming[i] && Objects.equals(iface, this.iface[i]) && defaultNetwork == this.defaultNetwork[i] && Objects.equals(proc, this.proc[i]) && actUid == this.actUid[i]) {
                return i;
            }
        }
        return -1;
    }

    @VisibleForTesting
    public int findIndexHinted(String iface, int uid, int set, int tag, int metered, int roaming, int defaultNetwork, int hintIndex) {
        for (int offset = 0; offset < this.size; offset++) {
            int i;
            int halfOffset = offset / 2;
            if (offset % 2 == 0) {
                i = (hintIndex + halfOffset) % this.size;
            } else {
                i = (((this.size + hintIndex) - halfOffset) - 1) % this.size;
            }
            if (uid == this.uid[i] && set == this.set[i] && tag == this.tag[i] && metered == this.metered[i] && roaming == this.roaming[i] && defaultNetwork == this.defaultNetwork[i] && Objects.equals(iface, this.iface[i])) {
                return i;
            }
        }
        return -1;
    }

    public void spliceOperationsFrom(NetworkStats stats) {
        for (int i = 0; i < this.size; i++) {
            int j = stats.findIndex(this.iface[i], this.uid[i], this.set[i], this.tag[i], this.metered[i], this.roaming[i], this.defaultNetwork[i], this.proc[i], this.actUid[i]);
            if (j == -1) {
                this.operations[i] = 0;
            } else {
                this.operations[i] = stats.operations[j];
            }
        }
    }

    public String[] getUniqueIfaces() {
        HashSet<String> ifaces = new HashSet();
        for (String iface : this.iface) {
            if (iface != IFACE_ALL) {
                ifaces.add(iface);
            }
        }
        return (String[]) ifaces.toArray(new String[ifaces.size()]);
    }

    public int[] getUniqueUids() {
        SparseBooleanArray uids = new SparseBooleanArray();
        int i = 0;
        for (int uid : this.uid) {
            uids.put(uid, true);
        }
        int size = uids.size();
        int[] result = new int[size];
        while (i < size) {
            result[i] = uids.keyAt(i);
            i++;
        }
        return result;
    }

    public long getTotalBytes() {
        Entry entry = getTotal(null);
        return entry.rxBytes + entry.txBytes;
    }

    public Entry getTotal(Entry recycle) {
        return getTotal(recycle, null, -1, false);
    }

    public Entry getTotal(Entry recycle, int limitUid) {
        return getTotal(recycle, null, limitUid, false);
    }

    public Entry getTotal(Entry recycle, HashSet<String> limitIface) {
        return getTotal(recycle, limitIface, -1, false);
    }

    public Entry getTotalIncludingTags(Entry recycle) {
        return getTotal(recycle, null, -1, true);
    }

    private Entry getTotal(Entry recycle, HashSet<String> limitIface, int limitUid, boolean includeTags) {
        Entry entry = recycle != null ? recycle : new Entry();
        entry.iface = IFACE_ALL;
        entry.uid = limitUid;
        entry.set = -1;
        entry.tag = 0;
        entry.metered = -1;
        entry.roaming = -1;
        entry.defaultNetwork = -1;
        entry.rxBytes = 0;
        entry.rxPackets = 0;
        entry.txBytes = 0;
        entry.txPackets = 0;
        entry.operations = 0;
        int i = 0;
        while (i < this.size) {
            boolean matchesIface = true;
            boolean matchesUid = limitUid == -1 || limitUid == this.uid[i];
            if (!(limitIface == null || limitIface.contains(this.iface[i]))) {
                matchesIface = false;
            }
            if (matchesUid && matchesIface && (this.tag[i] == 0 || includeTags)) {
                entry.rxBytes += this.rxBytes[i];
                entry.rxPackets += this.rxPackets[i];
                entry.txBytes += this.txBytes[i];
                entry.txPackets += this.txPackets[i];
                entry.operations += this.operations[i];
            }
            i++;
        }
        return entry;
    }

    public long getTotalPackets() {
        long total = 0;
        for (int i = this.size - 1; i >= 0; i--) {
            total += this.rxPackets[i] + this.txPackets[i];
        }
        return total;
    }

    public NetworkStats subtract(NetworkStats right) {
        return subtract(this, right, null, null);
    }

    public static <C> NetworkStats subtract(NetworkStats left, NetworkStats right, NonMonotonicObserver<C> observer, C cookie) {
        return subtract(left, right, observer, cookie, null);
    }

    public static <C> NetworkStats subtract(NetworkStats left, NetworkStats right, NonMonotonicObserver<C> observer, C cookie, NetworkStats recycle) {
        NetworkStats result;
        NetworkStats networkStats = left;
        NetworkStats networkStats2 = right;
        NetworkStats networkStats3 = recycle;
        long deltaRealtime = networkStats.elapsedRealtime - networkStats2.elapsedRealtime;
        if (deltaRealtime < 0) {
            if (observer != null) {
                observer.foundNonMonotonic(networkStats, -1, networkStats2, -1, cookie);
            }
            deltaRealtime = 0;
        }
        long deltaRealtime2 = deltaRealtime;
        Entry entry = new Entry();
        int i = 0;
        if (networkStats3 == null || networkStats3.capacity < networkStats.size) {
            result = new NetworkStats(deltaRealtime2, networkStats.size);
        } else {
            result = networkStats3;
            result.size = 0;
            result.elapsedRealtime = deltaRealtime2;
        }
        NetworkStats result2 = result;
        while (true) {
            int i2 = i;
            Entry entry2;
            long deltaRealtime3;
            if (i2 < networkStats.size) {
                int i3;
                NetworkStats result3;
                long j;
                entry.iface = networkStats.iface[i2];
                entry.uid = networkStats.uid[i2];
                entry.set = networkStats.set[i2];
                entry.tag = networkStats.tag[i2];
                entry.metered = networkStats.metered[i2];
                entry.roaming = networkStats.roaming[i2];
                entry.defaultNetwork = networkStats.defaultNetwork[i2];
                entry.rxBytes = networkStats.rxBytes[i2];
                entry.rxPackets = networkStats.rxPackets[i2];
                entry.txBytes = networkStats.txBytes[i2];
                entry.txPackets = networkStats.txPackets[i2];
                entry.operations = networkStats.operations[i2];
                entry.proc = networkStats.proc[i2];
                entry.actUid = networkStats.actUid[i2];
                String str = entry.iface;
                int i4 = entry.uid;
                int i5 = entry.set;
                int i6 = entry.tag;
                i = entry.metered;
                int i7 = entry.roaming;
                NetworkStats result4 = result2;
                int i8 = entry.defaultNetwork;
                long deltaRealtime4 = deltaRealtime2;
                int i9 = i7;
                int i10 = i8;
                result2 = networkStats2;
                i7 = networkStats2.findIndexHinted(str, i4, i5, i6, i, i9, i10, entry.proc, entry.actUid, i2);
                if (i7 != -1) {
                    entry.rxBytes -= result2.rxBytes[i7];
                    entry.rxPackets -= result2.rxPackets[i7];
                    entry.txBytes -= result2.txBytes[i7];
                    entry.txPackets -= result2.txPackets[i7];
                    entry.operations -= result2.operations[i7];
                }
                if (entry.isNegative()) {
                    if (observer != null) {
                        i3 = i2;
                        result3 = result4;
                        entry2 = entry;
                        deltaRealtime3 = deltaRealtime4;
                        observer.foundNonMonotonic(networkStats, i3, right, i7, cookie);
                    } else {
                        i3 = i2;
                        entry2 = entry;
                        result3 = result4;
                        deltaRealtime3 = deltaRealtime4;
                    }
                    j = 0;
                    entry2.rxBytes = Math.max(entry2.rxBytes, 0);
                    entry2.rxPackets = Math.max(entry2.rxPackets, 0);
                    entry2.txBytes = Math.max(entry2.txBytes, 0);
                    entry2.txPackets = Math.max(entry2.txPackets, 0);
                    entry2.operations = Math.max(entry2.operations, 0);
                } else {
                    i3 = i2;
                    entry2 = entry;
                    result3 = result4;
                    deltaRealtime3 = deltaRealtime4;
                    j = 0;
                }
                result3.addValues(entry2);
                i = i3 + 1;
                networkStats2 = right;
                networkStats3 = recycle;
                deltaRealtime2 = deltaRealtime3;
                long j2 = j;
                result2 = result3;
                entry = entry2;
            } else {
                entry2 = entry;
                deltaRealtime3 = deltaRealtime2;
                return result2;
            }
        }
    }

    public static void apply464xlatAdjustments(NetworkStats baseTraffic, NetworkStats stackedTraffic, Map<String, String> stackedIfaces) {
        Map<String, String> map;
        NetworkStats networkStats = stackedTraffic;
        NetworkStats adjustments = new NetworkStats(0, stackedIfaces.size());
        Entry entry = null;
        Entry adjust = new Entry(IFACE_ALL, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < networkStats.size; i++) {
            entry = networkStats.getValues(i, entry);
            if (entry.iface == null || !entry.iface.startsWith(CLATD_INTERFACE_PREFIX)) {
                map = stackedIfaces;
            } else {
                String baseIface = (String) stackedIfaces.get(entry.iface);
                if (baseIface != null) {
                    adjust.iface = baseIface;
                    adjust.rxBytes = -(entry.rxBytes + (entry.rxPackets * 20));
                    adjust.txBytes = -(entry.txBytes + (entry.txPackets * 20));
                    adjust.rxPackets = -entry.rxPackets;
                    adjust.txPackets = -entry.txPackets;
                    adjustments.combineValues(adjust);
                    entry.rxBytes += entry.rxPackets * 20;
                    entry.txBytes += entry.txPackets * 20;
                    networkStats.setValues(i, entry);
                }
            }
        }
        map = stackedIfaces;
        baseTraffic.combineAllValues(adjustments);
    }

    public void apply464xlatAdjustments(Map<String, String> stackedIfaces) {
        apply464xlatAdjustments(this, this, stackedIfaces);
    }

    public NetworkStats groupedByIface() {
        NetworkStats stats = new NetworkStats(this.elapsedRealtime, 10);
        Entry entry = new Entry();
        entry.uid = -1;
        entry.set = -1;
        int i = 0;
        entry.tag = 0;
        entry.metered = -1;
        entry.roaming = -1;
        entry.defaultNetwork = -1;
        entry.operations = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.size) {
                return stats;
            }
            if (this.tag[i2] == 0) {
                entry.iface = this.iface[i2];
                entry.rxBytes = this.rxBytes[i2];
                entry.rxPackets = this.rxPackets[i2];
                entry.txBytes = this.txBytes[i2];
                entry.txPackets = this.txPackets[i2];
                stats.combineValues(entry);
            }
            i = i2 + 1;
        }
    }

    public NetworkStats groupedByUid() {
        NetworkStats stats = new NetworkStats(this.elapsedRealtime, 10);
        Entry entry = new Entry();
        entry.iface = IFACE_ALL;
        entry.set = -1;
        int i = 0;
        entry.tag = 0;
        entry.metered = -1;
        entry.roaming = -1;
        entry.defaultNetwork = -1;
        while (true) {
            int i2 = i;
            if (i2 >= this.size) {
                return stats;
            }
            if (this.tag[i2] == 0) {
                entry.uid = this.uid[i2];
                entry.rxBytes = this.rxBytes[i2];
                entry.rxPackets = this.rxPackets[i2];
                entry.txBytes = this.txBytes[i2];
                entry.txPackets = this.txPackets[i2];
                entry.operations = this.operations[i2];
                stats.combineValues(entry);
            }
            i = i2 + 1;
        }
    }

    public NetworkStats withoutUids(int[] uids) {
        NetworkStats stats = new NetworkStats(this.elapsedRealtime, 10);
        Entry entry = new Entry();
        for (int i = 0; i < this.size; i++) {
            entry = getValues(i, entry);
            if (!ArrayUtils.contains(uids, entry.uid)) {
                stats.addValues(entry);
            }
        }
        return stats;
    }

    public void filter(int limitUid, String[] limitIfaces, int limitTag) {
        if (limitUid != -1 || limitTag != -1 || limitIfaces != INTERFACES_ALL) {
            int nextOutputEntry = 0;
            Entry entry = new Entry();
            for (int i = 0; i < this.size; i++) {
                entry = getValues(i, entry);
                boolean matches = (limitUid == -1 || limitUid == entry.uid) && ((limitTag == -1 || limitTag == entry.tag) && (limitIfaces == INTERFACES_ALL || ArrayUtils.contains(limitIfaces, entry.iface)));
                if (matches) {
                    setValues(nextOutputEntry, entry);
                    nextOutputEntry++;
                }
            }
            this.size = nextOutputEntry;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("NetworkStats: elapsedRealtime=");
        pw.println(this.elapsedRealtime);
        for (int i = 0; i < this.size; i++) {
            pw.print(prefix);
            pw.print("  [");
            pw.print(i);
            pw.print("]");
            pw.print(" iface=");
            pw.print(this.iface[i]);
            pw.print(" uid=");
            pw.print(this.uid[i]);
            pw.print(" set=");
            pw.print(setToString(this.set[i]));
            pw.print(" tag=");
            pw.print(tagToString(this.tag[i]));
            pw.print(" metered=");
            pw.print(meteredToString(this.metered[i]));
            pw.print(" roaming=");
            pw.print(roamingToString(this.roaming[i]));
            pw.print(" defaultNetwork=");
            pw.print(defaultNetworkToString(this.defaultNetwork[i]));
            pw.print(" rxBytes=");
            pw.print(this.rxBytes[i]);
            pw.print(" rxPackets=");
            pw.print(this.rxPackets[i]);
            pw.print(" txBytes=");
            pw.print(this.txBytes[i]);
            pw.print(" txPackets=");
            pw.print(this.txPackets[i]);
            pw.print(" operations=");
            pw.println(this.operations[i]);
            pw.print(" proc=");
            pw.println(this.proc[i]);
            pw.print(" actuid=");
            pw.println(this.actUid[i]);
        }
    }

    public static String setToString(int set) {
        switch (set) {
            case -1:
                return "ALL";
            case 0:
                return "DEFAULT";
            case 1:
                return "FOREGROUND";
            default:
                switch (set) {
                    case 1001:
                        return "DBG_VPN_IN";
                    case 1002:
                        return "DBG_VPN_OUT";
                    default:
                        return "UNKNOWN";
                }
        }
    }

    public static String setToCheckinString(int set) {
        switch (set) {
            case -1:
                return "all";
            case 0:
                return "def";
            case 1:
                return "fg";
            default:
                switch (set) {
                    case 1001:
                        return "vpnin";
                    case 1002:
                        return "vpnout";
                    default:
                        return "unk";
                }
        }
    }

    public static boolean setMatches(int querySet, int dataSet) {
        boolean z = true;
        if (querySet == dataSet) {
            return true;
        }
        if (querySet != -1 || dataSet >= 1000) {
            z = false;
        }
        return z;
    }

    public static String tagToString(int tag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(tag));
        return stringBuilder.toString();
    }

    public static String meteredToString(int metered) {
        switch (metered) {
            case -1:
                return "ALL";
            case 0:
                return "NO";
            case 1:
                return "YES";
            default:
                return "UNKNOWN";
        }
    }

    public static String roamingToString(int roaming) {
        switch (roaming) {
            case -1:
                return "ALL";
            case 0:
                return "NO";
            case 1:
                return "YES";
            default:
                return "UNKNOWN";
        }
    }

    public static String defaultNetworkToString(int defaultNetwork) {
        switch (defaultNetwork) {
            case -1:
                return "ALL";
            case 0:
                return "NO";
            case 1:
                return "YES";
            default:
                return "UNKNOWN";
        }
    }

    public String toString() {
        CharArrayWriter writer = new CharArrayWriter();
        dump("", new PrintWriter(writer));
        return writer.toString();
    }

    public int describeContents() {
        return 0;
    }

    public boolean migrateTun(int tunUid, String tunIface, String underlyingIface) {
        Entry tunIfaceTotal = new Entry();
        Entry underlyingIfaceTotal = new Entry();
        tunAdjustmentInit(tunUid, tunIface, underlyingIface, tunIfaceTotal, underlyingIfaceTotal);
        Entry pool = tunGetPool(tunIfaceTotal, underlyingIfaceTotal);
        if (pool.isEmpty()) {
            return true;
        }
        Entry moved = addTrafficToApplications(tunUid, tunIface, underlyingIface, tunIfaceTotal, pool);
        deductTrafficFromVpnApp(tunUid, underlyingIface, moved);
        if (moved.isEmpty()) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to deduct underlying network traffic from VPN package. Moved=");
        stringBuilder.append(moved);
        Slog.wtf(str, stringBuilder.toString());
        return false;
    }

    private void tunAdjustmentInit(int tunUid, String tunIface, String underlyingIface, Entry tunIfaceTotal, Entry underlyingIfaceTotal) {
        Entry recycle = new Entry();
        int i = 0;
        while (i < this.size) {
            getValues(i, recycle);
            if (recycle.uid == -1) {
                throw new IllegalStateException("Cannot adjust VPN accounting on an iface aggregated NetworkStats.");
            } else if (recycle.set == 1001 || recycle.set == 1002) {
                throw new IllegalStateException("Cannot adjust VPN accounting on a NetworkStats containing SET_DBG_VPN_*");
            } else {
                if (recycle.uid == tunUid && recycle.tag == 0 && Objects.equals(underlyingIface, recycle.iface)) {
                    underlyingIfaceTotal.add(recycle);
                }
                if (recycle.uid != tunUid && recycle.tag == 0 && Objects.equals(tunIface, recycle.iface)) {
                    tunIfaceTotal.add(recycle);
                }
                i++;
            }
        }
    }

    private static Entry tunGetPool(Entry tunIfaceTotal, Entry underlyingIfaceTotal) {
        Entry pool = new Entry();
        pool.rxBytes = Math.min(tunIfaceTotal.rxBytes, underlyingIfaceTotal.rxBytes);
        pool.rxPackets = Math.min(tunIfaceTotal.rxPackets, underlyingIfaceTotal.rxPackets);
        pool.txBytes = Math.min(tunIfaceTotal.txBytes, underlyingIfaceTotal.txBytes);
        pool.txPackets = Math.min(tunIfaceTotal.txPackets, underlyingIfaceTotal.txPackets);
        pool.operations = Math.min(tunIfaceTotal.operations, underlyingIfaceTotal.operations);
        return pool;
    }

    private Entry addTrafficToApplications(int tunUid, String tunIface, String underlyingIface, Entry tunIfaceTotal, Entry pool) {
        Entry moved = new Entry();
        Entry tmpEntry = new Entry();
        tmpEntry.iface = underlyingIface;
        int i = 0;
        while (i < this.size) {
            if (Objects.equals(this.iface[i], tunIface) && this.uid[i] != tunUid) {
                if (tunIfaceTotal.rxBytes > 0) {
                    tmpEntry.rxBytes = (pool.rxBytes * this.rxBytes[i]) / tunIfaceTotal.rxBytes;
                } else {
                    tmpEntry.rxBytes = 0;
                }
                if (tunIfaceTotal.rxPackets > 0) {
                    tmpEntry.rxPackets = (pool.rxPackets * this.rxPackets[i]) / tunIfaceTotal.rxPackets;
                } else {
                    tmpEntry.rxPackets = 0;
                }
                if (tunIfaceTotal.txBytes > 0) {
                    tmpEntry.txBytes = (pool.txBytes * this.txBytes[i]) / tunIfaceTotal.txBytes;
                } else {
                    tmpEntry.txBytes = 0;
                }
                if (tunIfaceTotal.txPackets > 0) {
                    tmpEntry.txPackets = (pool.txPackets * this.txPackets[i]) / tunIfaceTotal.txPackets;
                } else {
                    tmpEntry.txPackets = 0;
                }
                if (tunIfaceTotal.operations > 0) {
                    tmpEntry.operations = (pool.operations * this.operations[i]) / tunIfaceTotal.operations;
                } else {
                    tmpEntry.operations = 0;
                }
                tmpEntry.uid = this.uid[i];
                tmpEntry.tag = this.tag[i];
                tmpEntry.set = this.set[i];
                tmpEntry.metered = this.metered[i];
                tmpEntry.roaming = this.roaming[i];
                tmpEntry.defaultNetwork = this.defaultNetwork[i];
                combineValues(tmpEntry);
                if (this.tag[i] == 0) {
                    moved.add(tmpEntry);
                    tmpEntry.set = 1001;
                    combineValues(tmpEntry);
                }
            }
            i++;
        }
        return moved;
    }

    private void deductTrafficFromVpnApp(int tunUid, String underlyingIface, Entry moved) {
        moved.uid = tunUid;
        moved.set = 1002;
        moved.tag = 0;
        moved.iface = underlyingIface;
        moved.metered = -1;
        moved.roaming = -1;
        moved.defaultNetwork = -1;
        combineValues(moved);
        int idxVpnBackground = findIndex(underlyingIface, tunUid, 0, 0, 0, 0, 0);
        if (idxVpnBackground != -1) {
            tunSubtract(idxVpnBackground, this, moved);
        }
        int idxVpnForeground = findIndex(underlyingIface, tunUid, 1, 0, 0, 0, 0);
        if (idxVpnForeground != -1) {
            tunSubtract(idxVpnForeground, this, moved);
        }
    }

    private static void tunSubtract(int i, NetworkStats left, Entry right) {
        long rxBytes = Math.min(left.rxBytes[i], right.rxBytes);
        long[] jArr = left.rxBytes;
        jArr[i] = jArr[i] - rxBytes;
        right.rxBytes -= rxBytes;
        long rxPackets = Math.min(left.rxPackets[i], right.rxPackets);
        long[] jArr2 = left.rxPackets;
        jArr2[i] = jArr2[i] - rxPackets;
        right.rxPackets -= rxPackets;
        long txBytes = Math.min(left.txBytes[i], right.txBytes);
        long[] jArr3 = left.txBytes;
        jArr3[i] = jArr3[i] - txBytes;
        right.txBytes -= txBytes;
        long txPackets = Math.min(left.txPackets[i], right.txPackets);
        long[] jArr4 = left.txPackets;
        jArr4[i] = jArr4[i] - txPackets;
        right.txPackets -= txPackets;
    }
}
