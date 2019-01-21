package com.android.internal.app.procstats;

import com.android.internal.app.procstats.SparseMappingTable.Table;

public class PssTable extends Table {
    public PssTable(SparseMappingTable tableData) {
        super(tableData);
    }

    public void mergeStats(PssTable that) {
        PssTable pssTable = that;
        int N = that.getKeyCount();
        for (int i = 0; i < N; i++) {
            int key = pssTable.getKeyAt(i);
            mergeStats(SparseMappingTable.getIdFromKey(key), (int) pssTable.getValue(key, 0), pssTable.getValue(key, 1), pssTable.getValue(key, 2), pssTable.getValue(key, 3), pssTable.getValue(key, 4), pssTable.getValue(key, 5), pssTable.getValue(key, 6), pssTable.getValue(key, 7), pssTable.getValue(key, 8), pssTable.getValue(key, 9));
        }
    }

    public void mergeStats(int state, int inCount, long minPss, long avgPss, long maxPss, long minUss, long avgUss, long maxUss, long minRss, long avgRss, long maxRss) {
        int i = inCount;
        long j = minPss;
        long j2 = avgPss;
        long j3 = maxPss;
        long j4 = minUss;
        long j5 = avgUss;
        long j6 = maxUss;
        int key = getOrAddKey((byte) state, 10);
        j6 = getValue(key, 0);
        if (j6 == 0) {
            long count = j6;
            setValue(key, 0, (long) i);
            setValue(key, 1, j);
            setValue(key, 2, j2);
            setValue(key, 3, j3);
            setValue(key, 4, j4);
            setValue(key, 5, j5);
            j5 = count;
            setValue(key, 6, maxUss);
            setValue(key, 7, minRss);
            setValue(key, 8, avgRss);
            setValue(key, 9, maxRss);
            j2 = j5;
            j6 = maxUss;
            return;
        }
        j5 = j6;
        j6 = maxRss;
        setValue(key, 0, ((long) i) + j5);
        if (getValue(key, 1) > j) {
            setValue(key, 1, j);
        }
        j = getValue(key, 2);
        setValue(key, 2, (long) (((((double) j) * ((double) j5)) + (((double) j2) * ((double) i))) / ((double) (((long) i) + j5))));
        if (getValue(key, 3) < j3) {
            setValue(key, 3, j3);
        }
        if (getValue(key, 4) > j4) {
            setValue(key, 4, j4);
        }
        long val = getValue(key, 5);
        j2 = j5;
        j = avgUss;
        setValue(key, 5, (long) (((((double) val) * ((double) j5)) + (((double) j) * ((double) i))) / ((double) (((long) i) + j2))));
        j6 = maxUss;
        if (getValue(key, 6) < j6) {
            setValue(key, 6, j6);
        }
        if (getValue(key, 7) > j4) {
            setValue(key, 7, j4);
        }
        setValue(key, 8, (long) (((((double) getValue(key, 8)) * ((double) j2)) + (((double) j) * ((double) i))) / ((double) (((long) i) + j2))));
        if (getValue(key, 9) < j6) {
            setValue(key, 9, j6);
        }
    }
}
