package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkStatsHistory.Entry;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.system.ErrnoException;
import android.system.Os;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.MathUtils;
import android.util.Range;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator.Reader;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import libcore.io.IoUtils;

public class NetworkStatsCollection implements Reader {
    private static final int FILE_MAGIC = 1095648596;
    private static final String TAG = "NetworkStatsCollection";
    private static final int VERSION_NETWORK_INIT = 1;
    private static final int VERSION_UID_INIT = 1;
    private static final int VERSION_UID_PROC_UNIFIED_INIT = 17;
    private static final int VERSION_UID_WITH_IDENT = 2;
    private static final int VERSION_UID_WITH_SET = 4;
    private static final int VERSION_UID_WITH_TAG = 3;
    private static final int VERSION_UNIFIED_INIT = 16;
    private final long mBucketDuration;
    private boolean mDirty;
    private long mEndMillis;
    private long mStartMillis;
    private ArrayMap<Key, NetworkStatsHistory> mStats = new ArrayMap();
    private long mTotalBytes;

    private static class Key implements Comparable<Key> {
        public final int actUid;
        private final int hashCode;
        public final NetworkIdentitySet ident;
        public final String proc;
        public final int set;
        public final int tag;
        public final int uid;

        public Key(NetworkIdentitySet ident, int uid, int set, int tag) {
            this(ident, uid, set, tag, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }

        public Key(NetworkIdentitySet ident, int uid, int set, int tag, String proc) {
            this(ident, uid, set, tag, proc, -1);
        }

        public Key(NetworkIdentitySet ident, int uid, int set, int tag, String proc, int actUid) {
            this.ident = ident;
            this.uid = uid;
            this.set = set;
            this.tag = tag;
            this.proc = proc;
            this.actUid = actUid;
            this.hashCode = Objects.hash(new Object[]{ident, Integer.valueOf(uid), Integer.valueOf(set), Integer.valueOf(tag), proc, Integer.valueOf(actUid)});
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            if (this.uid == key.uid && this.set == key.set && this.tag == key.tag && Objects.equals(this.ident, key.ident) && this.proc.equals(key.proc) && this.actUid == key.actUid) {
                z = true;
            }
            return z;
        }

        public int compareTo(Key another) {
            int res = 0;
            if (!(this.ident == null || another.ident == null)) {
                res = this.ident.compareTo(another.ident);
            }
            if (res == 0) {
                res = Integer.compare(this.uid, another.uid);
            }
            if (res == 0) {
                res = Integer.compare(this.set, another.set);
            }
            if (res == 0) {
                res = Integer.compare(this.tag, another.tag);
            }
            if (!(res != 0 || this.proc == null || another.proc == null)) {
                res = this.proc.compareTo(another.proc);
            }
            if (res == 0) {
                return Integer.compare(this.actUid, another.actUid);
            }
            return res;
        }
    }

    public NetworkStatsCollection(long bucketDuration) {
        this.mBucketDuration = bucketDuration;
        reset();
    }

    public void clear() {
        reset();
    }

    public void reset() {
        this.mStats.clear();
        this.mStartMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mEndMillis = Long.MIN_VALUE;
        this.mTotalBytes = 0;
        this.mDirty = false;
    }

    public long getStartMillis() {
        return this.mStartMillis;
    }

    public long getFirstAtomicBucketMillis() {
        if (this.mStartMillis == JobStatus.NO_LATEST_RUNTIME) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return this.mStartMillis + this.mBucketDuration;
    }

    public long getEndMillis() {
        return this.mEndMillis;
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public boolean isDirty() {
        return this.mDirty;
    }

    public void clearDirty() {
        this.mDirty = false;
    }

    public boolean isEmpty() {
        return this.mStartMillis == JobStatus.NO_LATEST_RUNTIME && this.mEndMillis == Long.MIN_VALUE;
    }

    @VisibleForTesting
    public long roundUp(long time) {
        if (time == Long.MIN_VALUE || time == JobStatus.NO_LATEST_RUNTIME || time == -1) {
            return time;
        }
        long mod = time % this.mBucketDuration;
        if (mod > 0) {
            time = (time - mod) + this.mBucketDuration;
        }
        return time;
    }

    @VisibleForTesting
    public long roundDown(long time) {
        if (time == Long.MIN_VALUE || time == JobStatus.NO_LATEST_RUNTIME || time == -1) {
            return time;
        }
        long mod = time % this.mBucketDuration;
        if (mod > 0) {
            time -= mod;
        }
        return time;
    }

    @VisibleForTesting
    public static long multiplySafe(long value, long num, long den) {
        long den2;
        if (den == 0) {
            den2 = 1;
        } else {
            den2 = den;
        }
        long x = value;
        long y = num;
        long r = x * y;
        if (((Math.abs(x) | Math.abs(y)) >>> 31) == 0 || ((y == 0 || r / y == x) && !(x == Long.MIN_VALUE && y == -1))) {
            long j = value;
            long j2 = x;
            return r / den2;
        }
        return (long) ((((double) num) / ((double) den2)) * ((double) value));
    }

    public int[] getRelevantUids(int accessLevel) {
        return getRelevantUids(accessLevel, Binder.getCallingUid());
    }

    public int[] getRelevantUids(int accessLevel, int callerUid) {
        IntArray uids = new IntArray();
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = (Key) this.mStats.keyAt(i);
            if (NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel)) {
                int j = uids.binarySearch(key.uid);
                if (j < 0) {
                    uids.add(~j, key.uid);
                }
            }
        }
        return uids.toArray();
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, long start, long end, int accessLevel, int callerUid) {
        return getHistory(template, augmentPlan, uid, set, tag, fields, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, start, end, accessLevel, callerUid);
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, String proc, long start, long end, int accessLevel, int callerUid) {
        return getHistory(template, augmentPlan, uid, set, tag, fields, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, -1, start, end, accessLevel, callerUid);
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, String proc, int actUid, long start, long end, int accessLevel, int callerUid) {
        int i = uid;
        int i2 = fields;
        if (NetworkStatsAccess.isAccessibleToUser(i, callerUid, accessLevel)) {
            int bucketEstimate = (int) MathUtils.constrain((end - start) / this.mBucketDuration, 0, 15552000000L / this.mBucketDuration);
            NetworkStatsHistory combined = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, i2);
            if (start == end) {
                return combined;
            }
            long dataUsageTime;
            long augmentStart;
            long collectStart;
            long augmentEnd;
            int i3;
            if (augmentPlan != null) {
                dataUsageTime = augmentPlan.getDataUsageTime();
            } else {
                dataUsageTime = -1;
            }
            long collectEnd = start;
            long collectEnd2 = end;
            long augmentStart2 = -1;
            long augmentEnd2 = dataUsageTime;
            if (augmentEnd2 != -1) {
                Iterator<Range<ZonedDateTime>> it = augmentPlan.cycleIterator();
                while (it.hasNext()) {
                    Range<ZonedDateTime> cycle = (Range) it.next();
                    dataUsageTime = ((ZonedDateTime) cycle.getLower()).toInstant().toEpochMilli();
                    long cycleEnd = ((ZonedDateTime) cycle.getUpper()).toInstant().toEpochMilli();
                    if (dataUsageTime <= augmentEnd2 && augmentEnd2 < cycleEnd) {
                        augmentStart = dataUsageTime;
                        collectEnd = Long.min(collectEnd, augmentStart);
                        collectStart = collectEnd;
                        augmentEnd = Long.max(collectEnd2, augmentEnd2);
                        break;
                    }
                    collectEnd = collectEnd;
                    collectEnd2 = collectEnd2;
                    augmentStart = callerUid;
                    int i4 = accessLevel;
                }
            }
            collectStart = collectEnd;
            augmentEnd = collectEnd2;
            augmentStart = augmentStart2;
            if (augmentStart != -1) {
                augmentStart = roundUp(augmentStart);
                augmentEnd2 = roundDown(augmentEnd2);
                collectStart = roundDown(collectStart);
                augmentEnd = roundUp(augmentEnd);
            }
            long collectStart2 = collectStart;
            collectEnd = augmentEnd;
            augmentEnd = augmentEnd2;
            int i5 = 0;
            while (i5 < this.mStats.size()) {
                Key key = (Key) this.mStats.keyAt(i5);
                if (key.uid == i) {
                    if (NetworkStats.setMatches(set, key.set) && key.tag == tag) {
                        if (templateMatches(template, key.ident) && Objects.equals(key.proc, proc)) {
                            if (key.actUid == actUid) {
                                combined.recordHistory((NetworkStatsHistory) this.mStats.valueAt(i5), collectStart2, collectEnd);
                            }
                            i5++;
                            i = uid;
                        }
                    }
                } else {
                    i3 = set;
                }
                i = actUid;
                i5++;
                i = uid;
            }
            i3 = set;
            i = actUid;
            long augmentStart3;
            if (augmentStart != -1) {
                Entry entry = combined.getValues(augmentStart, augmentEnd, null);
                if (entry.rxBytes == 0 || entry.txBytes == 0) {
                    NetworkStatsHistory networkStatsHistory = combined;
                    long j = augmentStart;
                    long j2 = augmentEnd;
                    networkStatsHistory.recordData(j, j2, new NetworkStats.Entry(1, 0, 1, 0, 0));
                    networkStatsHistory.getValues(j, j2, entry);
                }
                collectEnd2 = entry.rxBytes + entry.txBytes;
                long rawRxBytes = entry.rxBytes;
                augmentEnd2 = entry.txBytes;
                dataUsageTime = augmentPlan.getDataUsageBytes();
                long j3 = collectEnd2;
                long targetRxBytes = multiplySafe(dataUsageTime, rawRxBytes, j3);
                long targetTxBytes = multiplySafe(dataUsageTime, augmentEnd2, j3);
                long beforeTotal = combined.getTotalBytes();
                int i6 = 0;
                while (true) {
                    int i7 = i6;
                    if (i7 >= combined.size()) {
                        break;
                    }
                    combined.getValues(i7, entry);
                    if (entry.bucketStart >= augmentStart) {
                        augmentStart3 = augmentStart;
                        if (entry.bucketStart + entry.bucketDuration <= augmentEnd) {
                            entry.rxBytes = multiplySafe(targetRxBytes, entry.rxBytes, rawRxBytes);
                            entry.txBytes = multiplySafe(targetTxBytes, entry.txBytes, augmentEnd2);
                            entry.rxPackets = 0;
                            entry.txPackets = 0;
                            combined.setValues(i7, entry);
                        }
                    } else {
                        augmentStart3 = augmentStart;
                    }
                    i6 = i7 + 1;
                    augmentStart = augmentStart3;
                    i = actUid;
                    i2 = fields;
                }
                long deltaTotal = combined.getTotalBytes() - beforeTotal;
                if (deltaTotal != 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Augmented network usage by ");
                    stringBuilder.append(deltaTotal);
                    stringBuilder.append(" bytes");
                    Slog.d(str, stringBuilder.toString());
                }
                augmentStart = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
                augmentStart.recordHistory(combined, start, end);
                return augmentStart;
            }
            augmentStart3 = augmentStart;
            return combined;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Network stats history of uid ");
        stringBuilder2.append(uid);
        stringBuilder2.append(" is forbidden for caller ");
        stringBuilder2.append(callerUid);
        throw new SecurityException(stringBuilder2.toString());
    }

    public NetworkStats getSummary(NetworkTemplate template, long start, long end, int accessLevel, int callerUid) {
        NetworkStatsCollection networkStatsCollection = this;
        long now = System.currentTimeMillis();
        NetworkStats stats = new NetworkStats(end - start, 24);
        if (start == end) {
            return stats;
        }
        NetworkStats.Entry entry = new NetworkStats.Entry();
        Entry historyEntry = null;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < networkStatsCollection.mStats.size()) {
                int i3;
                NetworkStats.Entry entry2;
                Key key = (Key) networkStatsCollection.mStats.keyAt(i2);
                if (templateMatches(template, key.ident)) {
                    if (NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel) && key.set < 1000) {
                        i3 = i2;
                        Key key2 = key;
                        entry2 = entry;
                        Entry historyEntry2 = ((NetworkStatsHistory) networkStatsCollection.mStats.valueAt(i2)).getValues(start, end, now, historyEntry);
                        entry2.iface = NetworkStats.IFACE_ALL;
                        entry2.uid = key2.uid;
                        entry2.set = key2.set;
                        entry2.tag = key2.tag;
                        entry2.defaultNetwork = key2.ident.areAllMembersOnDefaultNetwork() ? 1 : 0;
                        entry2.metered = key2.ident.isAnyMemberMetered();
                        entry2.proc = key2.proc;
                        entry2.actUid = key2.actUid;
                        entry2.roaming = key2.ident.isAnyMemberRoaming();
                        entry2.rxBytes = historyEntry2.rxBytes;
                        entry2.rxPackets = historyEntry2.rxPackets;
                        entry2.txBytes = historyEntry2.txBytes;
                        entry2.txPackets = historyEntry2.txPackets;
                        entry2.operations = historyEntry2.operations;
                        if (!entry2.isEmpty()) {
                            stats.combineValues(entry2);
                        }
                        historyEntry = historyEntry2;
                        i = i3 + 1;
                        entry = entry2;
                        networkStatsCollection = this;
                    }
                }
                i3 = i2;
                entry2 = entry;
                i = i3 + 1;
                entry = entry2;
                networkStatsCollection = this;
            } else {
                return stats;
            }
        }
    }

    public void recordData(NetworkIdentitySet ident, int uid, int set, int tag, long start, long end, NetworkStats.Entry entry) {
        int i = uid;
        NetworkStats.Entry entry2 = entry;
        if (i >= 19959 && i <= 19999) {
            int hbsUid = getHbsUid();
            i = hbsUid != -1 ? hbsUid : i;
        }
        NetworkStatsHistory history = findOrCreateHistory(ident, i, set, tag, entry2.proc, entry2.actUid);
        history.recordData(start, end, entry2);
        noteRecordedHistory(history.getStart(), history.getEnd(), entry2.rxBytes + entry2.txBytes);
    }

    private int getHbsUid() {
        try {
            return Os.lstat("/data/data/com.huawei.hbs.framework").st_uid;
        } catch (ErrnoException e) {
            return -1;
        }
    }

    private void recordHistory(Key key, NetworkStatsHistory history) {
        if (history.size() != 0) {
            noteRecordedHistory(history.getStart(), history.getEnd(), history.getTotalBytes());
            NetworkStatsHistory target = (NetworkStatsHistory) this.mStats.get(key);
            if (target == null) {
                target = new NetworkStatsHistory(history.getBucketDuration());
                this.mStats.put(key, target);
            }
            target.recordEntireHistory(history);
        }
    }

    public void recordCollection(NetworkStatsCollection another) {
        for (int i = 0; i < another.mStats.size(); i++) {
            recordHistory((Key) another.mStats.keyAt(i), (NetworkStatsHistory) another.mStats.valueAt(i));
        }
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag) {
        return findOrCreateHistory(ident, uid, set, tag, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag, String proc) {
        return findOrCreateHistory(ident, uid, set, tag, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, -1);
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag, String proc, int actUid) {
        Key key = new Key(ident, uid, set, tag, proc, actUid);
        NetworkStatsHistory existing = (NetworkStatsHistory) this.mStats.get(key);
        NetworkStatsHistory updated = null;
        if (existing == null) {
            updated = new NetworkStatsHistory(this.mBucketDuration, 10);
        } else if (existing.getBucketDuration() != this.mBucketDuration) {
            updated = new NetworkStatsHistory(existing, this.mBucketDuration);
        }
        if (updated == null) {
            return existing;
        }
        this.mStats.put(key, updated);
        return updated;
    }

    public void read(InputStream in) throws IOException {
        read(new DataInputStream(in));
    }

    public void read(DataInputStream in) throws IOException {
        DataInputStream dataInputStream = in;
        int magic = in.readInt();
        if (magic == FILE_MAGIC) {
            int version = in.readInt();
            boolean uidAndProcFlag = true;
            StringBuilder stringBuilder;
            switch (version) {
                case 16:
                    uidAndProcFlag = false;
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("read net data,  uidAndProcFlag= ");
                    stringBuilder.append(false);
                    Slog.d(str, stringBuilder.toString());
                    break;
                case 17:
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unexpected version: ");
                    stringBuilder.append(version);
                    throw new ProtocolException(stringBuilder.toString());
            }
            int identSize = in.readInt();
            int i = 0;
            while (i < identSize) {
                NetworkIdentitySet ident = new NetworkIdentitySet(dataInputStream);
                int size = in.readInt();
                int j = 0;
                while (true) {
                    int j2 = j;
                    if (j2 < size) {
                        int uid = in.readInt();
                        int set = in.readInt();
                        int tag = in.readInt();
                        String proc = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        if (in.readByte() != (byte) 0) {
                            proc = in.readUTF();
                        }
                        String proc2 = proc;
                        j = -1;
                        if (uidAndProcFlag) {
                            j = in.readInt();
                        }
                        recordHistory(new Key(ident, uid, set, tag, proc2, j), new NetworkStatsHistory(dataInputStream));
                        j = j2 + 1;
                    } else {
                        i++;
                    }
                }
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("unexpected magic: ");
        stringBuilder2.append(magic);
        throw new ProtocolException(stringBuilder2.toString());
    }

    public void write(DataOutputStream out) throws IOException {
        ArrayList<Key> keys;
        HashMap<NetworkIdentitySet, ArrayList<Key>> keysByIdent = Maps.newHashMap();
        for (Key key : this.mStats.keySet()) {
            keys = (ArrayList) keysByIdent.get(key.ident);
            if (keys == null) {
                keys = Lists.newArrayList();
                keysByIdent.put(key.ident, keys);
            }
            keys.add(key);
        }
        out.writeInt(FILE_MAGIC);
        out.writeInt(17);
        out.writeInt(keysByIdent.size());
        for (NetworkIdentitySet ident : keysByIdent.keySet()) {
            keys = (ArrayList) keysByIdent.get(ident);
            ident.writeToStream(out);
            out.writeInt(keys.size());
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                Key key2 = (Key) it.next();
                NetworkStatsHistory history = (NetworkStatsHistory) this.mStats.get(key2);
                out.writeInt(key2.uid);
                out.writeInt(key2.set);
                out.writeInt(key2.tag);
                String proc = key2.proc;
                if (proc == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(proc)) {
                    out.writeByte(0);
                } else {
                    out.writeByte(1);
                    out.writeUTF(proc);
                }
                out.writeInt(key2.actUid);
                history.writeToStream(out);
            }
        }
        out.flush();
    }

    @Deprecated
    public void readLegacyNetwork(File file) throws IOException {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
            int magic = in.readInt();
            if (magic == FILE_MAGIC) {
                int version = in.readInt();
                if (version == 1) {
                    int size = in.readInt();
                    for (int i = 0; i < size; i++) {
                        NetworkIdentitySet ident = new NetworkIdentitySet(in);
                        recordHistory(new Key(ident, -1, -1, 0), new NetworkStatsHistory(in));
                    }
                    IoUtils.closeQuietly(in);
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected version: ");
                stringBuilder.append(version);
                throw new ProtocolException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unexpected magic: ");
            stringBuilder2.append(magic);
            throw new ProtocolException(stringBuilder2.toString());
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
    }

    @Deprecated
    public void readLegacyUid(File file, boolean onlyTags) throws IOException {
        Throwable th;
        DataInputStream in = null;
        boolean z;
        try {
            in = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
            int magic = in.readInt();
            if (magic == FILE_MAGIC) {
                int version = in.readInt();
                switch (version) {
                    case 1:
                        z = onlyTags;
                        break;
                    case 2:
                        z = onlyTags;
                        break;
                    case 3:
                    case 4:
                        int identSize = in.readInt();
                        int i = 0;
                        while (i < identSize) {
                            File file2;
                            NetworkIdentitySet ident = new NetworkIdentitySet(in);
                            int size = in.readInt();
                            int j = 0;
                            while (j < size) {
                                int set;
                                int uid = in.readInt();
                                if (version >= 4) {
                                    set = in.readInt();
                                } else {
                                    set = 0;
                                }
                                int tag = in.readInt();
                                Key key = new Key(ident, uid, set, tag);
                                NetworkStatsHistory history = new NetworkStatsHistory(in);
                                if ((tag == 0) != onlyTags) {
                                    recordHistory(key, history);
                                }
                                j++;
                                file2 = file;
                            }
                            z = onlyTags;
                            i++;
                            file2 = file;
                        }
                        z = onlyTags;
                        break;
                    default:
                        z = onlyTags;
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unexpected version: ");
                            stringBuilder.append(version);
                            throw new ProtocolException(stringBuilder.toString());
                        } catch (FileNotFoundException e) {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                            IoUtils.closeQuietly(in);
                            throw th;
                        }
                }
            }
            z = onlyTags;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unexpected magic: ");
            stringBuilder2.append(magic);
            throw new ProtocolException(stringBuilder2.toString());
        } catch (FileNotFoundException e2) {
            z = onlyTags;
        } catch (Throwable th3) {
            th = th3;
            z = onlyTags;
            IoUtils.closeQuietly(in);
            throw th;
        }
        IoUtils.closeQuietly(in);
    }

    public void removeUids(int[] uids) {
        ArrayList<Key> knownKeys = Lists.newArrayList();
        knownKeys.addAll(this.mStats.keySet());
        Iterator it = knownKeys.iterator();
        while (it.hasNext()) {
            Key key = (Key) it.next();
            if (ArrayUtils.contains(uids, key.uid)) {
                if (key.tag == 0) {
                    findOrCreateHistory(key.ident, -4, 0, 0).recordEntireHistory((NetworkStatsHistory) this.mStats.get(key));
                }
                this.mStats.remove(key);
                this.mDirty = true;
            }
        }
    }

    private void noteRecordedHistory(long startMillis, long endMillis, long totalBytes) {
        if (startMillis < this.mStartMillis) {
            this.mStartMillis = startMillis;
        }
        if (endMillis > this.mEndMillis) {
            this.mEndMillis = endMillis;
        }
        this.mTotalBytes += totalBytes;
        this.mDirty = true;
    }

    private int estimateBuckets() {
        return (int) (Math.min(this.mEndMillis - this.mStartMillis, 3024000000L) / this.mBucketDuration);
    }

    private ArrayList<Key> getSortedKeys() {
        ArrayList<Key> keys = Lists.newArrayList();
        keys.addAll(this.mStats.keySet());
        Collections.sort(keys);
        return keys;
    }

    public void dump(IndentingPrintWriter pw) {
        Iterator it = getSortedKeys().iterator();
        while (it.hasNext()) {
            Key key = (Key) it.next();
            pw.print("ident=");
            pw.print(key.ident.toString());
            pw.print(" uid=");
            pw.print(key.uid);
            pw.print(" set=");
            pw.print(NetworkStats.setToString(key.set));
            pw.print(" tag=");
            pw.println(NetworkStats.tagToString(key.tag));
            pw.print(" proc=");
            pw.println(key.proc);
            pw.print(" actUid=");
            pw.print(key.actUid);
            NetworkStatsHistory history = (NetworkStatsHistory) this.mStats.get(key);
            pw.increaseIndent();
            history.dump(pw, true);
            pw.decreaseIndent();
        }
    }

    public void writeToProto(ProtoOutputStream proto, long tag) {
        long start = proto.start(tag);
        Iterator it = getSortedKeys().iterator();
        while (it.hasNext()) {
            Key key = (Key) it.next();
            long startStats = proto.start(2246267895809L);
            long startKey = proto.start(1146756268033L);
            key.ident.writeToProto(proto, 1146756268033L);
            proto.write(1120986464258L, key.uid);
            proto.write(1120986464259L, key.set);
            proto.write(1120986464260L, key.tag);
            proto.end(startKey);
            ((NetworkStatsHistory) this.mStats.get(key)).writeToProto(proto, 1146756268034L);
            proto.end(startStats);
        }
        proto.end(start);
    }

    public void dumpCheckin(PrintWriter pw, long start, long end) {
        PrintWriter printWriter = pw;
        long j = start;
        long j2 = end;
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateMobileWildcard(), "cell");
        PrintWriter printWriter2 = pw;
        long j3 = start;
        long j4 = end;
        dumpCheckin(printWriter2, j3, j4, NetworkTemplate.buildTemplateWifiWildcard(), "wifi");
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateEthernet(), "eth");
        dumpCheckin(printWriter2, j3, j4, NetworkTemplate.buildTemplateBluetooth(), "bt");
    }

    private void dumpCheckin(PrintWriter pw, long start, long end, NetworkTemplate groupTemplate, String groupPrefix) {
        String str;
        PrintWriter printWriter = pw;
        ArrayMap<Key, NetworkStatsHistory> grouped = new ArrayMap();
        int i = 0;
        for (int i2 = 0; i2 < this.mStats.size(); i2++) {
            Key key = (Key) this.mStats.keyAt(i2);
            NetworkStatsHistory value = (NetworkStatsHistory) this.mStats.valueAt(i2);
            if (templateMatches(groupTemplate, key.ident) && key.set < 1000) {
                Key groupKey = new Key(null, key.uid, key.set, key.tag);
                NetworkStatsHistory groupHistory = (NetworkStatsHistory) grouped.get(groupKey);
                if (groupHistory == null) {
                    groupHistory = new NetworkStatsHistory(value.getBucketDuration());
                    grouped.put(groupKey, groupHistory);
                }
                groupHistory.recordHistory(value, start, end);
            }
        }
        NetworkTemplate networkTemplate = groupTemplate;
        while (i < grouped.size()) {
            Key key2 = (Key) grouped.keyAt(i);
            NetworkStatsHistory value2 = (NetworkStatsHistory) grouped.valueAt(i);
            if (value2.size() == 0) {
                str = groupPrefix;
            } else {
                printWriter.print("c,");
                printWriter.print(groupPrefix);
                printWriter.print(',');
                printWriter.print(key2.uid);
                printWriter.print(',');
                printWriter.print(NetworkStats.setToCheckinString(key2.set));
                printWriter.print(',');
                printWriter.print(key2.tag);
                pw.println();
                value2.dumpCheckin(printWriter);
            }
            i++;
        }
        str = groupPrefix;
    }

    private static boolean templateMatches(NetworkTemplate template, NetworkIdentitySet identSet) {
        Iterator it = identSet.iterator();
        while (it.hasNext()) {
            if (template.matches((NetworkIdentity) it.next())) {
                return true;
            }
        }
        return false;
    }
}
