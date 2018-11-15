package com.android.server.backup.transport;

import android.content.ComponentName;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TransportStats {
    private final Object mStatsLock = new Object();
    private final Map<ComponentName, Stats> mTransportStats = new HashMap();

    public static final class Stats {
        public double average;
        public long max;
        public long min;
        public int n;

        public static Stats merge(Stats a, Stats b) {
            return new Stats(b.n + a.n, ((a.average * ((double) a.n)) + (b.average * ((double) b.n))) / ((double) (a.n + b.n)), Math.max(a.max, b.max), Math.min(a.min, b.min));
        }

        public Stats() {
            this.n = 0;
            this.average = 0.0d;
            this.max = 0;
            this.min = JobStatus.NO_LATEST_RUNTIME;
        }

        private Stats(int n, double average, long max, long min) {
            this.n = n;
            this.average = average;
            this.max = max;
            this.min = min;
        }

        private Stats(Stats original) {
            this(original.n, original.average, original.max, original.min);
        }

        private void register(long sample) {
            this.average = ((this.average * ((double) this.n)) + ((double) sample)) / ((double) (this.n + 1));
            this.n++;
            this.max = Math.max(this.max, sample);
            this.min = Math.min(this.min, sample);
        }
    }

    void registerConnectionTime(ComponentName transportComponent, long timeMs) {
        synchronized (this.mStatsLock) {
            Stats stats = (Stats) this.mTransportStats.get(transportComponent);
            if (stats == null) {
                stats = new Stats();
                this.mTransportStats.put(transportComponent, stats);
            }
            stats.register(timeMs);
        }
    }

    public Stats getStatsForTransport(ComponentName transportComponent) {
        synchronized (this.mStatsLock) {
            Stats stats = (Stats) this.mTransportStats.get(transportComponent);
            if (stats == null) {
                return null;
            }
            Stats stats2 = new Stats(stats);
            return stats2;
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mStatsLock) {
            Optional<Stats> aggregatedStats = this.mTransportStats.values().stream().reduce(-$$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA.INSTANCE);
            if (aggregatedStats.isPresent()) {
                dumpStats(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (Stats) aggregatedStats.get());
            }
            if (!this.mTransportStats.isEmpty()) {
                pw.println("Per transport:");
                for (ComponentName transportComponent : this.mTransportStats.keySet()) {
                    Stats stats = (Stats) this.mTransportStats.get(transportComponent);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("    ");
                    stringBuilder.append(transportComponent.flattenToShortString());
                    pw.println(stringBuilder.toString());
                    dumpStats(pw, "        ", stats);
                }
            }
        }
    }

    private static void dumpStats(PrintWriter pw, String prefix, Stats stats) {
        pw.println(String.format(Locale.US, "%sAverage connection time: %.2f ms", new Object[]{prefix, Double.valueOf(stats.average)}));
        pw.println(String.format(Locale.US, "%sMax connection time: %d ms", new Object[]{prefix, Long.valueOf(stats.max)}));
        pw.println(String.format(Locale.US, "%sMin connection time: %d ms", new Object[]{prefix, Long.valueOf(stats.min)}));
        pw.println(String.format(Locale.US, "%sNumber of connections: %d ", new Object[]{prefix, Integer.valueOf(stats.n)}));
    }
}
