package com.android.server.power.batterysaver;

import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.util.Map;

public class CpuFrequencies {
    private static final String TAG = "CpuFrequencies";
    @GuardedBy("mLock")
    private final ArrayMap<Integer, Long> mCoreAndFrequencies = new ArrayMap();
    private final Object mLock = new Object();

    public CpuFrequencies parseString(String cpuNumberAndFrequencies) {
        synchronized (this.mLock) {
            this.mCoreAndFrequencies.clear();
            try {
                for (String pair : cpuNumberAndFrequencies.split(SliceAuthority.DELIMITER)) {
                    String pair2 = pair2.trim();
                    if (pair2.length() != 0) {
                        String[] coreAndFreq = pair2.split(":", 2);
                        if (coreAndFreq.length == 2) {
                            this.mCoreAndFrequencies.put(Integer.valueOf(Integer.parseInt(coreAndFreq[0])), Long.valueOf(Long.parseLong(coreAndFreq[1])));
                        } else {
                            throw new IllegalArgumentException("Wrong format");
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid configuration: '");
                stringBuilder.append(cpuNumberAndFrequencies);
                stringBuilder.append("'");
                Slog.wtf(str, stringBuilder.toString());
            }
        }
        return this;
    }

    public ArrayMap<String, String> toSysFileMap() {
        ArrayMap<String, String> map = new ArrayMap();
        addToSysFileMap(map);
        return map;
    }

    public void addToSysFileMap(Map<String, String> map) {
        synchronized (this.mLock) {
            int size = this.mCoreAndFrequencies.size();
            for (int i = 0; i < size; i++) {
                int core = ((Integer) this.mCoreAndFrequencies.keyAt(i)).intValue();
                long freq = ((Long) this.mCoreAndFrequencies.valueAt(i)).longValue();
                String file = new StringBuilder();
                file.append("/sys/devices/system/cpu/cpu");
                file.append(Integer.toString(core));
                file.append("/cpufreq/scaling_max_freq");
                map.put(file.toString(), Long.toString(freq));
            }
        }
    }
}
