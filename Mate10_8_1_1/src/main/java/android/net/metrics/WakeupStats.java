package android.net.metrics;

import android.os.SystemClock;

public class WakeupStats {
    private static final int NO_UID = -1;
    public long applicationWakeups = 0;
    public final long creationTimeMs = SystemClock.elapsedRealtime();
    public long durationSec = 0;
    public final String iface;
    public long noUidWakeups = 0;
    public long nonApplicationWakeups = 0;
    public long rootWakeups = 0;
    public long systemWakeups = 0;
    public long totalWakeups = 0;

    public WakeupStats(String iface) {
        this.iface = iface;
    }

    public void updateDuration() {
        this.durationSec = (SystemClock.elapsedRealtime() - this.creationTimeMs) / 1000;
    }

    public void countEvent(WakeupEvent ev) {
        this.totalWakeups++;
        switch (ev.uid) {
            case -1:
                this.noUidWakeups++;
                return;
            case 0:
                this.rootWakeups++;
                return;
            case 1000:
                this.systemWakeups++;
                return;
            default:
                if (ev.uid >= 10000) {
                    this.applicationWakeups++;
                    return;
                } else {
                    this.nonApplicationWakeups++;
                    return;
                }
        }
    }

    public String toString() {
        updateDuration();
        return "WakeupStats(" + this.iface + ", total: " + this.totalWakeups + ", root: " + this.rootWakeups + ", system: " + this.systemWakeups + ", apps: " + this.applicationWakeups + ", non-apps: " + this.nonApplicationWakeups + ", no uid: " + this.noUidWakeups + ", " + this.durationSec + "s)";
    }
}
