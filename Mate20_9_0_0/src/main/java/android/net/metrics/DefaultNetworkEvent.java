package android.net.metrics;

import android.net.NetworkCapabilities;
import com.android.internal.util.BitUtils;
import java.util.StringJoiner;

public class DefaultNetworkEvent {
    public final long creationTimeMs;
    public long durationMs;
    public int finalScore;
    public int initialScore;
    public boolean ipv4;
    public boolean ipv6;
    public int netId = 0;
    public int previousTransports;
    public int transports;
    public long validatedMs;

    public DefaultNetworkEvent(long timeMs) {
        this.creationTimeMs = timeMs;
    }

    public void updateDuration(long timeMs) {
        this.durationMs = timeMs - this.creationTimeMs;
    }

    public String toString() {
        StringJoiner j = new StringJoiner(", ", "DefaultNetworkEvent(", ")");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("netId=");
        stringBuilder.append(this.netId);
        j.add(stringBuilder.toString());
        for (int t : BitUtils.unpackBits((long) this.transports)) {
            j.add(NetworkCapabilities.transportNameOf(t));
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("ip=");
        stringBuilder.append(ipSupport());
        j.add(stringBuilder.toString());
        if (this.initialScore > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("initial_score=");
            stringBuilder.append(this.initialScore);
            j.add(stringBuilder.toString());
        }
        if (this.finalScore > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("final_score=");
            stringBuilder.append(this.finalScore);
            j.add(stringBuilder.toString());
        }
        j.add(String.format("duration=%.0fs", new Object[]{Double.valueOf(((double) this.durationMs) / 1000.0d)}));
        j.add(String.format("validation=%04.1f%%", new Object[]{Double.valueOf((((double) this.validatedMs) * 100.0d) / ((double) this.durationMs))}));
        return j.toString();
    }

    private String ipSupport() {
        if (this.ipv4 && this.ipv6) {
            return "IPv4v6";
        }
        if (this.ipv6) {
            return "IPv6";
        }
        if (this.ipv4) {
            return "IPv4";
        }
        return "NONE";
    }
}
