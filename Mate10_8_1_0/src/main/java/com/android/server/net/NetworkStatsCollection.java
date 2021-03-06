package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkStatsHistory.Entry;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
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
            this(ident, uid, set, tag, "");
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

    public int[] getRelevantUids(int r1, int r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.net.NetworkStatsCollection.getRelevantUids(int, int):int[]
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.NetworkStatsCollection.getRelevantUids(int, int):int[]");
    }

    public NetworkStatsCollection(long bucketDuration) {
        this.mBucketDuration = bucketDuration;
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

    public static long multiplySafe(long value, long num, long den) {
        long x = value;
        long y = num;
        long r = value * num;
        if (((Math.abs(value) | Math.abs(num)) >>> 31) == 0 || ((num == 0 || r / num == value) && (value != Long.MIN_VALUE || num != -1))) {
            return r / den;
        }
        return (long) ((((double) num) / ((double) den)) * ((double) value));
    }

    public int[] getRelevantUids(int accessLevel) {
        return getRelevantUids(accessLevel, Binder.getCallingUid());
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, long start, long end, int accessLevel, int callerUid) {
        return getHistory(template, augmentPlan, uid, set, tag, fields, "", start, end, accessLevel, callerUid);
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, String proc, long start, long end, int accessLevel, int callerUid) {
        return getHistory(template, augmentPlan, uid, set, tag, fields, "", -1, start, end, accessLevel, callerUid);
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, String proc, int actUid, long start, long end, int accessLevel, int callerUid) {
        if (NetworkStatsAccess.isAccessibleToUser(uid, callerUid, accessLevel)) {
            int bucketEstimate = (int) ((end - start) / this.mBucketDuration);
            NetworkStatsHistory combined = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
            if (start == end) {
                return combined;
            }
            long augmentEnd;
            int i;
            long augmentStart = -1;
            if (augmentPlan != null) {
                augmentEnd = augmentPlan.getDataUsageTime();
            } else {
                augmentEnd = -1;
            }
            long collectStart = start;
            long collectEnd = end;
            if (augmentEnd != -1) {
                Iterator<Pair<ZonedDateTime, ZonedDateTime>> it = augmentPlan.cycleIterator();
                while (it.hasNext()) {
                    Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) it.next();
                    long cycleStart = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                    long cycleEnd = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                    if (cycleStart <= augmentEnd && augmentEnd < cycleEnd) {
                        augmentStart = cycleStart;
                        collectStart = Long.min(start, cycleStart);
                        collectEnd = Long.max(end, augmentEnd);
                        break;
                    }
                }
            }
            if (augmentStart != -1) {
                augmentStart = roundUp(augmentStart);
                augmentEnd = roundDown(augmentEnd);
                collectStart = roundDown(collectStart);
                collectEnd = roundUp(collectEnd);
            }
            for (i = 0; i < this.mStats.size(); i++) {
                Key key = (Key) this.mStats.keyAt(i);
                if (key.uid == uid) {
                    if (NetworkStats.setMatches(set, key.set) && key.tag == tag) {
                        if (templateMatches(template, key.ident) && Objects.equals(key.proc, proc) && key.actUid == actUid) {
                            combined.recordHistory((NetworkStatsHistory) this.mStats.valueAt(i), collectStart, collectEnd);
                        }
                    }
                }
            }
            if (augmentStart == -1) {
                return combined;
            }
            Entry entry = combined.getValues(augmentStart, augmentEnd, null);
            if (entry.rxBytes == 0 || entry.txBytes == 0) {
                combined.recordData(augmentStart, augmentEnd, new NetworkStats.Entry(1, 0, 1, 0, 0));
                combined.getValues(augmentStart, augmentEnd, entry);
            }
            long rawBytes = entry.rxBytes + entry.txBytes;
            long rawRxBytes = entry.rxBytes;
            long rawTxBytes = entry.txBytes;
            long targetBytes = augmentPlan.getDataUsageBytes();
            long targetRxBytes = multiplySafe(targetBytes, rawRxBytes, rawBytes);
            long targetTxBytes = multiplySafe(targetBytes, rawTxBytes, rawBytes);
            long beforeTotal = combined.getTotalBytes();
            for (i = 0; i < combined.size(); i++) {
                combined.getValues(i, entry);
                if (entry.bucketStart >= augmentStart && entry.bucketStart + entry.bucketDuration <= augmentEnd) {
                    entry.rxBytes = multiplySafe(targetRxBytes, entry.rxBytes, rawRxBytes);
                    entry.txBytes = multiplySafe(targetTxBytes, entry.txBytes, rawTxBytes);
                    entry.rxPackets = 0;
                    entry.txPackets = 0;
                    combined.setValues(i, entry);
                }
            }
            long deltaTotal = combined.getTotalBytes() - beforeTotal;
            if (deltaTotal != 0) {
                Slog.d(TAG, "Augmented network usage by " + deltaTotal + " bytes");
            }
            NetworkStatsHistory networkStatsHistory = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
            networkStatsHistory.recordHistory(combined, start, end);
            return networkStatsHistory;
        }
        throw new SecurityException("Network stats history of uid " + uid + " is forbidden for caller " + callerUid);
    }

    public NetworkStats getSummary(NetworkTemplate template, long start, long end, int accessLevel, int callerUid) {
        long now = System.currentTimeMillis();
        NetworkStats stats = new NetworkStats(end - start, 24);
        if (start == end) {
            return stats;
        }
        NetworkStats.Entry entry = new NetworkStats.Entry();
        Entry historyEntry = null;
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = (Key) this.mStats.keyAt(i);
            if (templateMatches(template, key.ident) && NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel) && key.set < 1000) {
                historyEntry = ((NetworkStatsHistory) this.mStats.valueAt(i)).getValues(start, end, now, historyEntry);
                entry.iface = NetworkStats.IFACE_ALL;
                entry.uid = key.uid;
                entry.set = key.set;
                entry.tag = key.tag;
                entry.metered = key.ident.isAnyMemberMetered() ? 1 : 0;
                entry.proc = key.proc;
                entry.actUid = key.actUid;
                entry.roaming = key.ident.isAnyMemberRoaming() ? 1 : 0;
                entry.rxBytes = historyEntry.rxBytes;
                entry.rxPackets = historyEntry.rxPackets;
                entry.txBytes = historyEntry.txBytes;
                entry.txPackets = historyEntry.txPackets;
                entry.operations = historyEntry.operations;
                if (!entry.isEmpty()) {
                    stats.combineValues(entry);
                }
            }
        }
        return stats;
    }

    public void recordData(NetworkIdentitySet ident, int uid, int set, int tag, long start, long end, NetworkStats.Entry entry) {
        NetworkStatsHistory history = findOrCreateHistory(ident, uid, set, tag, entry.proc, entry.actUid);
        history.recordData(start, end, entry);
        noteRecordedHistory(history.getStart(), history.getEnd(), entry.txBytes + entry.rxBytes);
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
        return findOrCreateHistory(ident, uid, set, tag, "");
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag, String proc) {
        return findOrCreateHistory(ident, uid, set, tag, "", -1);
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag, String proc, int actUid) {
        Key key = new Key(ident, uid, set, tag, proc, actUid);
        NetworkStatsHistory existing = (NetworkStatsHistory) this.mStats.get(key);
        NetworkStatsHistory networkStatsHistory = null;
        if (existing == null) {
            networkStatsHistory = new NetworkStatsHistory(this.mBucketDuration, 10);
        } else if (existing.getBucketDuration() != this.mBucketDuration) {
            networkStatsHistory = new NetworkStatsHistory(existing, this.mBucketDuration);
        }
        if (networkStatsHistory == null) {
            return existing;
        }
        this.mStats.put(key, networkStatsHistory);
        return networkStatsHistory;
    }

    public void read(InputStream in) throws IOException {
        read(new DataInputStream(in));
    }

    public void read(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        boolean uidAndProcFlag = true;
        switch (version) {
            case 16:
                uidAndProcFlag = false;
                Slog.d(TAG, "read net data,  uidAndProcFlag= " + false);
                break;
            case 17:
                break;
            default:
                throw new ProtocolException("unexpected version: " + version);
        }
        int identSize = in.readInt();
        for (int i = 0; i < identSize; i++) {
            NetworkIdentitySet ident = new NetworkIdentitySet(in);
            int size = in.readInt();
            for (int j = 0; j < size; j++) {
                int uid = in.readInt();
                int set = in.readInt();
                int tag = in.readInt();
                String proc = "";
                if (in.readByte() != (byte) 0) {
                    proc = in.readUTF();
                }
                int actUid = -1;
                if (uidAndProcFlag) {
                    actUid = in.readInt();
                }
                recordHistory(new Key(ident, uid, set, tag, proc, actUid), new NetworkStatsHistory(in));
            }
        }
    }

    public void write(DataOutputStream out) throws IOException {
        HashMap<NetworkIdentitySet, ArrayList<Key>> keysByIdent = Maps.newHashMap();
        for (Key key : this.mStats.keySet()) {
            ArrayList<Key> keys = (ArrayList) keysByIdent.get(key.ident);
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
            for (Key key2 : keys) {
                NetworkStatsHistory history = (NetworkStatsHistory) this.mStats.get(key2);
                out.writeInt(key2.uid);
                out.writeInt(key2.set);
                out.writeInt(key2.tag);
                String proc = key2.proc;
                if (proc == null || ("".equals(proc) ^ 1) == 0) {
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
        Throwable th;
        AutoCloseable autoCloseable = null;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
            try {
                int magic = in.readInt();
                if (magic != FILE_MAGIC) {
                    throw new ProtocolException("unexpected magic: " + magic);
                }
                int version = in.readInt();
                switch (version) {
                    case 1:
                        int size = in.readInt();
                        for (int i = 0; i < size; i++) {
                            NetworkIdentitySet ident = new NetworkIdentitySet(in);
                            recordHistory(new Key(ident, -1, -1, 0), new NetworkStatsHistory(in));
                        }
                        IoUtils.closeQuietly(in);
                        return;
                    default:
                        throw new ProtocolException("unexpected version: " + version);
                }
            } catch (FileNotFoundException e) {
                autoCloseable = in;
                IoUtils.closeQuietly(autoCloseable);
            } catch (Throwable th2) {
                th = th2;
                autoCloseable = in;
                IoUtils.closeQuietly(autoCloseable);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            IoUtils.closeQuietly(autoCloseable);
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(autoCloseable);
            throw th;
        }
    }

    @Deprecated
    public void readLegacyUid(File file, boolean onlyTags) throws IOException {
        Throwable th;
        AutoCloseable autoCloseable = null;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
            try {
                int magic = in.readInt();
                if (magic != FILE_MAGIC) {
                    throw new ProtocolException("unexpected magic: " + magic);
                }
                int version = in.readInt();
                switch (version) {
                    case 1:
                    case 2:
                        break;
                    case 3:
                    case 4:
                        int identSize = in.readInt();
                        for (int i = 0; i < identSize; i++) {
                            NetworkIdentitySet ident = new NetworkIdentitySet(in);
                            int size = in.readInt();
                            for (int j = 0; j < size; j++) {
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
                            }
                        }
                        break;
                    default:
                        throw new ProtocolException("unexpected version: " + version);
                }
                IoUtils.closeQuietly(in);
            } catch (FileNotFoundException e) {
                autoCloseable = in;
                IoUtils.closeQuietly(autoCloseable);
            } catch (Throwable th2) {
                th = th2;
                autoCloseable = in;
                IoUtils.closeQuietly(autoCloseable);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            IoUtils.closeQuietly(autoCloseable);
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(autoCloseable);
            throw th;
        }
    }

    public void removeUids(int[] uids) {
        ArrayList<Key> knownKeys = Lists.newArrayList();
        knownKeys.addAll(this.mStats.keySet());
        for (Key key : knownKeys) {
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
        for (Key key : getSortedKeys()) {
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
        for (Key key : getSortedKeys()) {
            long startStats = proto.start(2272037699585L);
            long startKey = proto.start(1172526071809L);
            key.ident.writeToProto(proto, 1172526071809L);
            proto.write(1112396529666L, key.uid);
            proto.write(1112396529667L, key.set);
            proto.write(1112396529668L, key.tag);
            proto.end(startKey);
            ((NetworkStatsHistory) this.mStats.get(key)).writeToProto(proto, 1172526071810L);
            proto.end(startStats);
        }
        proto.end(start);
    }

    public void dumpCheckin(PrintWriter pw, long start, long end) {
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateMobileWildcard(), "cell");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateWifiWildcard(), "wifi");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateEthernet(), "eth");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateBluetooth(), "bt");
    }

    private void dumpCheckin(PrintWriter pw, long start, long end, NetworkTemplate groupTemplate, String groupPrefix) {
        int i;
        ArrayMap<Key, NetworkStatsHistory> grouped = new ArrayMap();
        for (i = 0; i < this.mStats.size(); i++) {
            Key key = (Key) this.mStats.keyAt(i);
            NetworkStatsHistory value = (NetworkStatsHistory) this.mStats.valueAt(i);
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
        for (i = 0; i < grouped.size(); i++) {
            key = (Key) grouped.keyAt(i);
            value = (NetworkStatsHistory) grouped.valueAt(i);
            if (value.size() != 0) {
                pw.print("c,");
                pw.print(groupPrefix);
                pw.print(',');
                pw.print(key.uid);
                pw.print(',');
                pw.print(NetworkStats.setToCheckinString(key.set));
                pw.print(',');
                pw.print(key.tag);
                pw.println();
                value.dumpCheckin(pw);
            }
        }
    }

    private static boolean templateMatches(NetworkTemplate template, NetworkIdentitySet identSet) {
        for (NetworkIdentity ident : identSet) {
            if (template.matches(ident)) {
                return true;
            }
        }
        return false;
    }
}
