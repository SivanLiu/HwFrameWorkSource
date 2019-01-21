package android.system;

import libcore.util.Objects;

public final class StructRlimit {
    public final long rlim_cur;
    public final long rlim_max;

    public StructRlimit(long rlim_cur, long rlim_max) {
        this.rlim_cur = rlim_cur;
        this.rlim_max = rlim_max;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
