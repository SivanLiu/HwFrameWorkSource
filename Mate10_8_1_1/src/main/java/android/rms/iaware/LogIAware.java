package android.rms.iaware;

import com.huawei.pgmng.log.LogPower;

public final class LogIAware extends LogPower {
    private LogIAware() {
    }

    public static int report(int tag) {
        return LogPower.pushIAware(tag, "");
    }

    public static int report(int tag, String msg) {
        return LogPower.pushIAware(tag, msg);
    }
}
