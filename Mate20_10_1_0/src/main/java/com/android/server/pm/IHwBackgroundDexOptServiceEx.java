package com.android.server.pm;

import android.app.job.JobParameters;

public interface IHwBackgroundDexOptServiceEx {
    int getReason(int i, int i2, int i3, String str);

    boolean runBootUpdateDelayOpt(JobParameters jobParameters);

    boolean stopBootUpdateDelayOpt(JobParameters jobParameters);
}
