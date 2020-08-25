package com.android.server.rms.iaware.cpu;

import android.os.Bundle;
import android.os.Message;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CPUVipThread {
    private static final Object SLOCK = new Object();
    private static final String TAG = "CPUVipThread";
    private static CPUVipThread sInstance;
    private CPUFeature mCPUFeatureInstance;
    private int mCurPid = -1;
    private List<Integer> mCurThreads;
    private AtomicBoolean mVipEnable = new AtomicBoolean(false);
    private CPUFeature.CPUFeatureHandler mVipHandler;

    private CPUVipThread() {
    }

    public static CPUVipThread getInstance() {
        CPUVipThread cPUVipThread;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new CPUVipThread();
            }
            cPUVipThread = sInstance;
        }
        return cPUVipThread;
    }

    public void sendPacket(int pid, List<Integer> threads, int msg) {
        if (this.mCPUFeatureInstance != null && threads != null && pid > 0) {
            int num = threads.size();
            ByteBuffer buffer = ByteBuffer.allocate((num * 4) + 12);
            buffer.putInt(msg);
            buffer.putInt(num);
            buffer.putInt(pid);
            for (int i = 0; i < num; i++) {
                buffer.putInt(threads.get(i).intValue());
            }
            if (this.mCPUFeatureInstance.sendPacket(buffer) != 1) {
                AwareLog.e(TAG, "send failed");
            }
        }
    }

    public void notifyProcessGroupChange(int pid, int renderThreadTid, int grp) {
        CPUFeature.CPUFeatureHandler cPUFeatureHandler;
        if (this.mVipEnable.get() && grp == 3 && (cPUFeatureHandler = this.mVipHandler) != null) {
            Message msg = cPUFeatureHandler.obtainMessage(CPUFeature.MSG_SET_THREAD_TO_VIP);
            Bundle bundle = new Bundle();
            bundle.putInt(CPUFeature.KEY_BUNDLE_PID, pid);
            bundle.putInt(CPUFeature.KEY_BUNDLE_RENDER_TID, renderThreadTid);
            msg.setData(bundle);
            this.mVipHandler.sendMessage(msg);
        }
    }

    public void setAppVipThread(int pid, List<Integer> threads, boolean isSet, boolean isSetGroup) {
        synchronized (SLOCK) {
            if (isSet) {
                this.mCurThreads = threads;
                this.mCurPid = pid;
            }
            if (this.mVipEnable.get()) {
                if (threads != null) {
                    if (pid > 0) {
                        if (this.mVipHandler != null) {
                            Message msg = this.mVipHandler.obtainMessage();
                            msg.what = isSet ? CPUFeature.MSG_SET_VIP_THREAD : CPUFeature.MSG_RESET_VIP_THREAD;
                            msg.arg1 = pid;
                            msg.arg2 = isSetGroup ? 1 : -1;
                            msg.obj = threads;
                            this.mVipHandler.sendMessage(msg);
                        }
                        return;
                    }
                }
                AwareLog.e(TAG, "thread is null or pid <= 0 :" + pid);
            }
        }
    }

    public void setHandler(CPUFeature.CPUFeatureHandler mHandler) {
        synchronized (SLOCK) {
            this.mVipHandler = mHandler;
        }
    }

    public void start(CPUFeature feature) {
        List<Integer> list;
        this.mVipEnable.set(true);
        this.mCPUFeatureInstance = feature;
        int i = this.mCurPid;
        if (i > 0 && (list = this.mCurThreads) != null) {
            sendPacket(i, list, CPUFeature.MSG_SET_VIP_THREAD);
        }
    }

    public void stop() {
        List<Integer> list;
        this.mVipEnable.set(false);
        int i = this.mCurPid;
        if (i > 0 && (list = this.mCurThreads) != null) {
            sendPacket(i, list, CPUFeature.MSG_RESET_VIP_THREAD);
        }
    }
}
