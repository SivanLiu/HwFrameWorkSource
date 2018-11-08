package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VipCgroupControl {
    private static final String TAG = "VipCgroupControl";
    private static VipCgroupControl sInstance;
    private CPUFeature mCPUFeatureInstance;
    private boolean mEnable;

    private VipCgroupControl() {
    }

    public void enable(CPUFeature cPUFeature) {
        this.mCPUFeatureInstance = cPUFeature;
        this.mEnable = true;
    }

    public void disable() {
        this.mEnable = false;
    }

    public static synchronized VipCgroupControl getInstance() {
        VipCgroupControl vipCgroupControl;
        synchronized (VipCgroupControl.class) {
            if (sInstance == null) {
                sInstance = new VipCgroupControl();
            }
            vipCgroupControl = sInstance;
        }
        return vipCgroupControl;
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
        if (this.mCPUFeatureInstance != null) {
            this.mCPUFeatureInstance.sendPacket(buffer);
        }
    }

    private boolean isVip(int tid, int tgid) {
        String vip = getFileString("/proc/" + tgid + "/task/" + tid + "/static_vip");
        if (vip == null || "1".equals(vip)) {
            return true;
        }
        return false;
    }

    private String getFileString(String path) {
        IOException e;
        Throwable th;
        File file = new File(path);
        FileInputStream fileInputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream inputStream = new FileInputStream(file);
            while (true) {
                try {
                    int temp = inputStream.read();
                    if (temp == -1) {
                        break;
                    }
                    char ch = (char) temp;
                    if (!(ch == '\n' || ch == '\r')) {
                        sb.append(ch);
                    }
                } catch (IOException e2) {
                    e = e2;
                    fileInputStream = inputStream;
                } catch (Throwable th2) {
                    th = th2;
                    fileInputStream = inputStream;
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e3.getMessage());
                }
            }
            fileInputStream = inputStream;
            if (sb.length() != 0) {
                return sb.toString();
            }
            AwareLog.e(TAG, "checkIsTargetGroup read fail");
            return null;
        } catch (IOException e4) {
            e3 = e4;
            try {
                AwareLog.d(TAG, "checkIsTargetGroup read catch IOException msg = " + e3.getMessage());
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e32) {
                        AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e32.getMessage());
                    }
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e322) {
                        AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e322.getMessage());
                    }
                }
                throw th;
            }
        }
    }
}
