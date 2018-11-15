package android_maps_conflict_avoidance.com.google.googlenav;

import android_maps_conflict_avoidance.com.google.common.Config;
import android_maps_conflict_avoidance.com.google.common.Log;
import android_maps_conflict_avoidance.com.google.common.StaticUtil;
import java.io.DataInput;
import java.io.IOException;

public class Stats {
    private static Stats currentInstance;
    private int bytesDownloaded = 0;
    private int bytesUploaded = 0;
    private int flashCacheHits = 0;
    private int flashCacheHitsSinceLastLog = 0;
    private int flashCacheMisses = 0;
    private int flashCacheMissesSinceLastLog = 0;

    private Stats() {
    }

    public static synchronized Stats getInstance() {
        Stats stats;
        synchronized (Stats.class) {
            if (currentInstance == null) {
                currentInstance = read();
                if (currentInstance == null) {
                    currentInstance = new Stats();
                }
            }
            stats = currentInstance;
        }
        return stats;
    }

    public void flashCacheHit() {
        synchronized (this) {
            this.flashCacheHits++;
            this.flashCacheHitsSinceLastLog++;
        }
        log(false);
    }

    public void flashCacheMiss() {
        synchronized (this) {
            this.flashCacheMisses++;
            this.flashCacheMissesSinceLastLog++;
        }
        log(false);
    }

    private static Stats read() {
        DataInput dis = StaticUtil.readPreferenceAsDataInput("Stats");
        if (dis == null) {
            return null;
        }
        try {
            Stats stats = new Stats();
            stats.flashCacheHits = dis.readInt();
            stats.flashCacheMisses = dis.readInt();
            stats.bytesDownloaded = dis.readInt();
            stats.bytesUploaded = dis.readInt();
            return stats;
        } catch (IOException e) {
            Log.logThrowable("STATS", e);
            Config.getInstance().getPersistentStore().deleteBlock("Stats");
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0017, code:
            if ((r2 + r3) <= r1) goto L_?;
     */
    /* JADX WARNING: Missing block: B:16:0x0019, code:
            r0 = new java.lang.StringBuffer();
     */
    /* JADX WARNING: Missing block: B:17:0x001e, code:
            if (r2 <= 0) goto L_0x0032;
     */
    /* JADX WARNING: Missing block: B:18:0x0020, code:
            r0.append("|");
            r0.append("f");
            r0.append("=");
            r0.append(r2);
     */
    /* JADX WARNING: Missing block: B:19:0x0032, code:
            if (r3 <= 0) goto L_0x0046;
     */
    /* JADX WARNING: Missing block: B:20:0x0034, code:
            r0.append("|");
            r0.append("m");
            r0.append("=");
            r0.append(r3);
     */
    /* JADX WARNING: Missing block: B:21:0x0046, code:
            r0.append("|");
            android_maps_conflict_avoidance.com.google.common.Log.addEvent((short) 22, "c", r0.toString());
     */
    /* JADX WARNING: Missing block: B:30:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:31:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void log(boolean force) {
        Throwable th;
        int threshold = force ? 0 : 50;
        synchronized (this) {
            int hits;
            try {
                hits = this.flashCacheHitsSinceLastLog;
                int misses;
                try {
                    misses = this.flashCacheMissesSinceLastLog;
                    if (hits + misses > threshold) {
                        try {
                            this.flashCacheHitsSinceLastLog = 0;
                            this.flashCacheMissesSinceLastLog = 0;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                } catch (Throwable th3) {
                    Throwable th4 = th3;
                    misses = 0;
                    th = th4;
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                hits = 0;
                throw th;
            }
        }
    }
}
