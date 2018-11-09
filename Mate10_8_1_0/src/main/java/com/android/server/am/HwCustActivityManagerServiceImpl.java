package com.android.server.am;

import android.os.SystemProperties;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class HwCustActivityManagerServiceImpl extends HwCustActivityManagerService {
    private static final boolean IS_IQI_Enable = SystemProperties.getBoolean("ro.config.iqi_att_support", false);
    static final String TAG = "HwCustAMSImpl";
    private boolean mAllowMemoryCompress = SystemProperties.getBoolean("ro.config.enable_rcc", false);
    private boolean mDelaySwitchUserDlg = SystemProperties.getBoolean("ro.config.DelaySwitchUserDlg", false);

    protected boolean isIQIEnable() {
        return IS_IQI_Enable;
    }

    protected boolean shouldDelaySwitchUserDlg() {
        return this.mDelaySwitchUserDlg;
    }

    protected boolean isAllowRamCompress() {
        return this.mAllowMemoryCompress;
    }

    protected void setEvent(String event) {
        IOException e;
        Throwable th;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File("sys/kernel/rcc/event");
            if (file.exists()) {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                try {
                    bw.write(event);
                    bw.flush();
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "can not close file descriptor!");
                        }
                    }
                    bufferedWriter = bw;
                } catch (IOException e3) {
                    e = e3;
                    bufferedWriter = bw;
                    try {
                        Log.e(TAG, e.toString());
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e4) {
                                Log.e(TAG, "can not close file descriptor!");
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "can not close file descriptor!");
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bufferedWriter = bw;
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    throw th;
                }
            }
            Log.e(TAG, "sys/kernel/rcc/event doesn't exist!");
        } catch (IOException e6) {
            e = e6;
            Log.e(TAG, e.toString());
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
    }

    protected int addProcesstoPersitList(ProcessRecord proc) {
        int maxAdj = proc.maxAdj;
        if (proc.processName.equals("diagandroid.iqd")) {
            return -800;
        }
        return maxAdj;
    }
}
