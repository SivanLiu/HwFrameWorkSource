package android.net.netlink;

import android.system.Os;
import android.system.OsConstants;
import java.nio.ByteBuffer;

public class StructNdaCacheInfo {
    private static final long CLOCK_TICKS_PER_SECOND = Os.sysconf(OsConstants._SC_CLK_TCK);
    public static final int STRUCT_SIZE = 16;
    public int ndm_confirmed;
    public int ndm_refcnt;
    public int ndm_updated;
    public int ndm_used;

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 16;
    }

    public static StructNdaCacheInfo parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNdaCacheInfo struct = new StructNdaCacheInfo();
        struct.ndm_used = byteBuffer.getInt();
        struct.ndm_confirmed = byteBuffer.getInt();
        struct.ndm_updated = byteBuffer.getInt();
        struct.ndm_refcnt = byteBuffer.getInt();
        return struct;
    }

    private static long ticksToMilliSeconds(int intClockTicks) {
        return (1000 * (((long) intClockTicks) & -1)) / CLOCK_TICKS_PER_SECOND;
    }

    public long lastUsed() {
        return ticksToMilliSeconds(this.ndm_used);
    }

    public long lastConfirmed() {
        return ticksToMilliSeconds(this.ndm_confirmed);
    }

    public long lastUpdated() {
        return ticksToMilliSeconds(this.ndm_updated);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NdaCacheInfo{ ndm_used{");
        stringBuilder.append(lastUsed());
        stringBuilder.append("}, ndm_confirmed{");
        stringBuilder.append(lastConfirmed());
        stringBuilder.append("}, ndm_updated{");
        stringBuilder.append(lastUpdated());
        stringBuilder.append("}, ndm_refcnt{");
        stringBuilder.append(this.ndm_refcnt);
        stringBuilder.append("} }");
        return stringBuilder.toString();
    }
}
