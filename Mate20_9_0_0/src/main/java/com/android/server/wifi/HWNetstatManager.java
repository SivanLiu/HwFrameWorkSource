package com.android.server.wifi;

import android.content.Context;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HWNetstatManager {
    private static final int IDX_IFACE = 1;
    private static final int IDX_RXBYTES = 5;
    private static final int IDX_RXPACKETS = 6;
    private static final int IDX_SET = 4;
    private static final int IDX_TX_BYTES = 7;
    private static final int IDX_TX_PACKETS = 8;
    private static final int IDX_UID = 3;
    private static final int INITIAL_SIZE = 24;
    private static final int MAX_LOG_TRAFFIC = 11;
    private static final int MIN_SEG_LENGTH = 9;
    private static final String TAG = "HWNetstatManager";
    private static final String TAG_STAT = "hw_netstat";
    private static ArrayList<Entry> mNetworkStatsEntryList = new ArrayList();
    private static NetworkStats mStats;
    private static long mTotalRxByte = 0;
    private static long mTotalTxByte = 0;
    private Comparator<Entry> mComparator = new Comparator<Entry>() {
        public int compare(Entry left, Entry right) {
            if (left.txBytes + left.rxBytes > right.txBytes + right.rxBytes) {
                return -1;
            }
            if (left.txBytes + left.rxBytes < right.txBytes + right.rxBytes) {
                return 1;
            }
            return 0;
        }
    };
    private Context mContext = null;
    private NetworkStats mLastStats = null;
    private SparseArray<String> mPackageTables = null;

    private static native void class_init_native();

    private static native int nativeReadWifiNetworkStatsDetail();

    private native void native_init();

    static {
        class_init_native();
    }

    private static NetworkStats readNetworkStatsDetail() {
        mStats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        if (nativeReadWifiNetworkStatsDetail() == 0) {
            return mStats.groupedByUid();
        }
        return null;
    }

    private void reportNetworkStatsDetail(int uid, int set, long rxBytes, long rxPackets, long txBytes, long txPackets) {
        Entry entry = new Entry();
        entry.uid = uid;
        entry.set = set;
        entry.rxBytes = rxBytes;
        entry.rxPackets = rxPackets;
        entry.txBytes = txBytes;
        entry.txPackets = txPackets;
        mStats.addValues(entry);
    }

    public HWNetstatManager(Context cxt) {
        this.mContext = cxt;
        this.mPackageTables = new SparseArray(49);
        native_init();
    }

    public void resetNetstats() {
        this.mLastStats = null;
    }

    public void performPollAndLog() {
        try {
            NetworkStats stats = readNetworkStatsDetail();
            if (this.mLastStats != null) {
                ArrayList<Entry> entryList = getIncrementalStats(stats, this.mLastStats);
                mNetworkStatsEntryList = (ArrayList) entryList.clone();
                logNetstatTraffic(entryList);
                entryList.clear();
            } else {
                Log.d(TAG, "get base netstat");
            }
            this.mLastStats = stats;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logNetstatTraffic(ArrayList<Entry> entryList) {
        int size = entryList.size();
        int maxcount = 11;
        if (size < 11) {
            maxcount = size;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxcount; i++) {
            Entry entry = (Entry) entryList.get(i);
            StringBuilder stringBuilder;
            if (i != 0) {
                if (entry.txBytes <= 0 && entry.rxBytes <= 0) {
                    break;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(",");
                stringBuilder.append(getPackageName(entry.uid));
                stringBuilder.append("/");
                stringBuilder.append(entry.rxBytes);
                stringBuilder.append("/");
                stringBuilder.append(entry.txBytes);
                sb.append(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(getPackageName(entry.uid));
                stringBuilder.append("/");
                stringBuilder.append(entry.rxBytes);
                stringBuilder.append("/");
                stringBuilder.append(entry.txBytes);
                sb.append(stringBuilder.toString());
            }
        }
        Log.d(TAG_STAT, sb.toString());
    }

    private Entry getIncrementalStatsTotal(NetworkStats left, NetworkStats right) {
        if (left == null) {
            return new Entry();
        }
        Entry leftEntry = left.getTotal(null);
        if (right != null) {
            Entry rightEntry = right.getTotal(null);
            leftEntry.rxBytes -= rightEntry.rxBytes;
            leftEntry.rxPackets -= rightEntry.rxPackets;
            leftEntry.txBytes -= rightEntry.txBytes;
            leftEntry.txPackets -= rightEntry.txPackets;
        }
        return leftEntry;
    }

    private ArrayList<Entry> getIncrementalStats(NetworkStats left, NetworkStats right) {
        ArrayList<Entry> list = new ArrayList();
        if (left == null) {
            list.add(new Entry());
            return list;
        }
        for (int i = 0; i < left.size(); i++) {
            int idx;
            Entry entry = left.getValues(i, new Entry());
            if (right == null) {
                idx = -1;
            } else {
                idx = right.findIndex(entry.iface, entry.uid, entry.set, entry.tag, entry.metered, entry.roaming, entry.defaultNetwork);
            }
            if (idx >= 0) {
                Entry baseentry = right.getValues(idx, new Entry());
                entry.rxBytes -= baseentry.rxBytes;
                entry.rxPackets -= baseentry.rxPackets;
                entry.txBytes -= baseentry.txBytes;
                entry.txPackets -= baseentry.txPackets;
            }
            list.add(entry);
        }
        Collections.sort(list, this.mComparator);
        Entry entry2 = getIncrementalStatsTotal(left, right);
        mTotalTxByte = entry2.txBytes;
        mTotalRxByte = entry2.rxBytes;
        list.add(0, entry2);
        return list;
    }

    private String getPackageName(int uid) {
        if (uid == -1) {
            return "total";
        }
        int keyIdx = this.mPackageTables.indexOfKey(uid);
        if (keyIdx >= 0) {
            return (String) this.mPackageTables.valueAt(keyIdx);
        }
        String name = this.mContext.getPackageManager().getNameForUid(uid);
        if (TextUtils.isEmpty(name)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown:");
            stringBuilder.append(uid);
            name = stringBuilder.toString();
        }
        this.mPackageTables.put(uid, name);
        return name;
    }

    public long getUidRxBytes(int uid) {
        int size = 0;
        if (mNetworkStatsEntryList != null) {
            size = mNetworkStatsEntryList.size();
        }
        for (int i = 0; i < size; i++) {
            Entry entry = (Entry) mNetworkStatsEntryList.get(i);
            if (entry.uid == uid) {
                return entry.rxBytes;
            }
        }
        String str = TAG_STAT;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not found the uid's RxBytes ");
        stringBuilder.append(uid);
        Log.d(str, stringBuilder.toString());
        return 0;
    }

    public long getUidTxBytes(int uid) {
        int size = 0;
        if (mNetworkStatsEntryList != null) {
            size = mNetworkStatsEntryList.size();
        }
        for (int i = 0; i < size; i++) {
            Entry entry = (Entry) mNetworkStatsEntryList.get(i);
            if (entry.uid == uid) {
                return entry.txBytes;
            }
        }
        String str = TAG_STAT;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not found the uid's TxBytes ");
        stringBuilder.append(uid);
        Log.d(str, stringBuilder.toString());
        return 0;
    }

    public long getTxBytes() {
        return mTotalTxByte;
    }

    public long getRxBytes() {
        return mTotalRxByte;
    }
}
