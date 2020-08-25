package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VipCgroupControl {
    private static final Object SLOCK = new Object();
    private static final String TAG = "VipCgroupControl";
    private static VipCgroupControl sInstance;
    private CPUFeature mCPUFeatureInstance;
    private boolean mEnable;

    private VipCgroupControl() {
    }

    public static VipCgroupControl getInstance() {
        VipCgroupControl vipCgroupControl;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new VipCgroupControl();
            }
            vipCgroupControl = sInstance;
        }
        return vipCgroupControl;
    }

    public void enable(CPUFeature cPUFeature) {
        this.mCPUFeatureInstance = cPUFeature;
        this.mEnable = true;
    }

    public void disable() {
        this.mEnable = false;
    }

    public void notifyForkChange(int tid, int tgid) {
        if (this.mEnable && !isVip(tid, tgid)) {
            moveThreadToTa(tid);
        }
    }

    private void moveThreadToTa(int tid) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(CPUFeature.MSG_SET_THREAD_TO_TA);
        buffer.putInt(tid);
        CPUFeature cPUFeature = this.mCPUFeatureInstance;
        if (cPUFeature != null) {
            cPUFeature.sendPacket(buffer);
        }
    }

    private boolean isVip(int tid, int tgid) {
        String vip = getFileString("/proc/" + tgid + "/task/" + tid + "/static_vip");
        if (vip != null && !"1".equals(vip)) {
            return false;
        }
        return true;
    }

    private String getFileString(String path) {
        File file = new File(path);
        FileInputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream inputStream2 = new FileInputStream(file);
            while (true) {
                int temp = inputStream2.read();
                if (temp != -1) {
                    char ch = (char) temp;
                    if (!(ch == '\n' || ch == '\r')) {
                        sb.append(ch);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e.getMessage());
                    }
                }
            }
            inputStream2.close();
            if (sb.length() != 0) {
                return sb.toString();
            }
            AwareLog.e(TAG, "checkIsTargetGroup read fail");
            return null;
        } catch (IOException e2) {
            AwareLog.d(TAG, "checkIsTargetGroup read catch IOException msg = " + e2.getMessage());
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e3.getMessage());
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e4.getMessage());
                }
            }
            throw th;
        }
    }
}
