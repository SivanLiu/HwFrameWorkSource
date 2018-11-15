package com.android.server.wifi.util;

import android.util.SparseIntArray;

public class MetricsUtils {

    public static class GenericBucket {
        public int count;
        public long end;
        public long start;
    }

    public static class LogHistParms {
        public int b;
        public double[] bb;
        public int m;
        public double mLog;
        public int n;
        public int p;
        public int s;
        public double[] sbw;

        public LogHistParms(int b, int p, int m, int s, int n) {
            this.b = b;
            this.p = p;
            this.m = m;
            this.s = s;
            this.n = n;
            this.mLog = Math.log((double) m);
            this.bb = new double[n];
            this.sbw = new double[n];
            this.bb[0] = (double) (b + p);
            this.sbw[0] = (((double) p) * (((double) m) - 1.0d)) / ((double) s);
            for (int i = 1; i < n; i++) {
                this.bb[i] = (((double) m) * (this.bb[i - 1] - ((double) b))) + ((double) b);
                this.sbw[i] = ((double) m) * this.sbw[i - 1];
            }
        }
    }

    public static int addValueToLogHistogram(long x, SparseIntArray histogram, LogHistParms hp) {
        int subBucketIndex;
        double logArg = ((double) (x - ((long) hp.b))) / ((double) hp.p);
        int bigBucketIndex = -1;
        if (logArg > 0.0d) {
            bigBucketIndex = (int) (Math.log(logArg) / hp.mLog);
        }
        if (bigBucketIndex < 0) {
            bigBucketIndex = 0;
            subBucketIndex = 0;
        } else if (bigBucketIndex >= hp.n) {
            bigBucketIndex = hp.n - 1;
            subBucketIndex = hp.s - 1;
        } else {
            subBucketIndex = (int) ((((double) x) - hp.bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
            if (subBucketIndex >= hp.s) {
                bigBucketIndex++;
                if (bigBucketIndex >= hp.n) {
                    bigBucketIndex = hp.n - 1;
                    subBucketIndex = hp.s - 1;
                } else {
                    subBucketIndex = (int) ((((double) x) - hp.bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
                }
            }
        }
        int key = (hp.s * bigBucketIndex) + subBucketIndex;
        int newValue = histogram.get(key) + 1;
        histogram.put(key, newValue);
        return newValue;
    }

    public static GenericBucket[] logHistogramToGenericBuckets(SparseIntArray histogram, LogHistParms hp) {
        GenericBucket[] protoArray = new GenericBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            int key = histogram.keyAt(i);
            protoArray[i] = new GenericBucket();
            protoArray[i].start = (long) (hp.bb[key / hp.s] + (hp.sbw[key / hp.s] * ((double) (key % hp.s))));
            protoArray[i].end = (long) (((double) protoArray[i].start) + hp.sbw[key / hp.s]);
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }

    public static int addValueToLinearHistogram(int x, SparseIntArray histogram, int[] hp) {
        int bucket = 0;
        int length = hp.length;
        int i = 0;
        while (i < length && x >= hp[i]) {
            bucket++;
            i++;
        }
        length = histogram.get(bucket) + 1;
        histogram.put(bucket, length);
        return length;
    }

    public static GenericBucket[] linearHistogramToGenericBuckets(SparseIntArray histogram, int[] linearHistParams) {
        GenericBucket[] protoArray = new GenericBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            int bucket = histogram.keyAt(i);
            protoArray[i] = new GenericBucket();
            if (bucket == 0) {
                protoArray[i].start = -2147483648L;
                protoArray[i].end = (long) linearHistParams[0];
            } else if (bucket != linearHistParams.length) {
                protoArray[i].start = (long) linearHistParams[bucket - 1];
                protoArray[i].end = (long) linearHistParams[bucket];
            } else {
                protoArray[i].start = (long) linearHistParams[linearHistParams.length - 1];
                protoArray[i].end = 2147483647L;
            }
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }
}
