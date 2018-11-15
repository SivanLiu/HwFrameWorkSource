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
        String path = new StringBuilder();
        path.append("/proc/");
        path.append(tgid);
        path.append("/task/");
        path.append(tid);
        path.append("/static_vip");
        String vip = getFileString(path.toString());
        if (vip == null || "1".equals(vip)) {
            return true;
        }
        return false;
    }

    private String getFileString(String path) {
        String str;
        StringBuilder stringBuilder;
        File file = new File(path);
        FileInputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new FileInputStream(file);
            while (true) {
                int read = inputStream.read();
                int temp = read;
                if (read != -1) {
                    char ch = (char) temp;
                    if (!(ch == 10 || ch == 13)) {
                        sb.append(ch);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("checkIsTargetGroup close catch IOException msg = ");
                        stringBuilder.append(e.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                    }
                }
            }
            inputStream.close();
            if (sb.length() != 0) {
                return sb.toString();
            }
            AwareLog.e(TAG, "checkIsTargetGroup read fail");
            return null;
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkIsTargetGroup read catch IOException msg = ");
            stringBuilder.append(e2.getMessage());
            AwareLog.d(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkIsTargetGroup close catch IOException msg = ");
                    stringBuilder2.append(e3.getMessage());
                    AwareLog.e(str2, stringBuilder2.toString());
                }
            }
            return null;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e22) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("checkIsTargetGroup close catch IOException msg = ");
                    stringBuilder3.append(e22.getMessage());
                    AwareLog.e(TAG, stringBuilder3.toString());
                }
            }
        }
    }
}
