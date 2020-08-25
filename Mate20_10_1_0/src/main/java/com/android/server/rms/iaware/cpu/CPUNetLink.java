package com.android.server.rms.iaware.cpu;

import android.net.netlink.NetlinkSocket;
import android.net.netlink.StructNdMsg;
import android.net.netlink.StructNlMsgHdr;
import android.os.Process;
import android.rms.iaware.AwareLog;
import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.Os;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class CPUNetLink {
    private static final int BG_HIGH_MSG = 3;
    private static final int BG_LOW_MSG = 4;
    private static final int BUFF_SIZE = 8192;
    private static final int CPU_HIGH_LOAD_MSG = 1;
    private static final int CPU_LOAD_MSG = 5;
    private static final int FG_HIGH_MSG = 5;
    private static final int MAX_LENGTH = 1024;
    private static final int NATIVE_HIGH_MSG = 1;
    private static final int NATIVE_LOW_MSG = 2;
    private static final int PID_LENGTH = 16;
    private static final int PROC_AUX_COMM_FORK_MSG = 6;
    private static final int PROC_AUX_COMM_MSG = 4;
    private static final int PROC_AUX_COMM_REMOVE_MSG = 7;
    private static final int PROC_COMM_MSG = 3;
    private static final int PROC_FORK_MSG = 2;
    private static final int SOCKET_PORT = 33;
    private static final String TAG = "CPUNetLink";
    private static final long TIMEOUT = 0;
    /* access modifiers changed from: private */
    public static FileDescriptor sFileDescriptor;
    /* access modifiers changed from: private */
    public volatile boolean mIsStart;
    /* access modifiers changed from: private */
    public NetlinkSocket mLocalNetlinkSocket = null;
    private ReceiveKernelThread mReceviveKernelThread;
    private Thread mThread;

    private static class RecvData {
        List<Integer> data;
        int len;
        int what;

        private RecvData() {
        }
    }

    private byte[] newMsgStruct(int seqNo) {
        byte[] bytes = new byte[28];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_len = 28;
        nlmsghdr.nlmsg_type = 30;
        nlmsghdr.nlmsg_flags = 769;
        nlmsghdr.nlmsg_seq = seqNo;
        nlmsghdr.nlmsg_pid = Process.myPid();
        nlmsghdr.pack(byteBuffer);
        new StructNdMsg().pack(byteBuffer);
        return bytes;
    }

    /* access modifiers changed from: private */
    public void parse(ByteBuffer byteBuffer) {
        if (StructNlMsgHdr.parse(byteBuffer) != null && byteBuffer.remaining() >= 8) {
            RecvData recvData = new RecvData();
            recvData.what = byteBuffer.getInt();
            recvData.len = byteBuffer.getInt();
            int len = recvData.len;
            if (len > 0 && len <= 1024 && byteBuffer.remaining() >= len * 4) {
                recvData.data = new ArrayList(len);
                for (int i = 0; i < len; i++) {
                    recvData.data.add(Integer.valueOf(byteBuffer.getInt()));
                }
                handleData(recvData);
            }
        }
    }

    private void handleCpuLoadHighData(RecvData recvData) {
        if (recvData.len != 17) {
            AwareLog.w(TAG, "err data num:" + recvData.len + " for proc comm connector, expect: " + 17);
            return;
        }
        int msg = recvData.data.get(0).intValue();
        AwareLog.i(TAG, "parse msg :" + msg + " recvData.len :" + recvData.len);
        ArrayList<Integer> pids = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            int pid = recvData.data.get(i + 1).intValue();
            AwareLog.d(TAG, "parse pid :" + pid);
            if (pid == 0) {
                break;
            }
            pids.add(Integer.valueOf(pid));
        }
        if (msg == 1 || msg == 2 || msg == 3 || msg == 4 || msg == 5) {
            CPUHighLoadManager.getInstance().setCpuHighLoadTaskList(msg, pids);
        }
    }

    private void handleAuxCommData(RecvData recvData) {
        int msgId;
        if (recvData.len != 2) {
            AwareLog.e(TAG, "err data num:" + recvData.len + " for proc aux comm connector, expect 2");
            return;
        }
        int pid = recvData.data.get(0).intValue();
        int tgid = recvData.data.get(1).intValue();
        int delay = 0;
        if (recvData.what == 7) {
            msgId = CPUFeature.MSG_RM_AUX_THREAD;
        } else {
            msgId = 160;
            if (recvData.what == 6) {
                delay = AuxRtgSched.getInstance().getAuxForkDelay();
            }
        }
        AuxRtgSched.getInstance().sendAuxCommMessage(msgId, pid, tgid, delay);
    }

    private void handleData(RecvData recvData) {
        switch (recvData.what) {
            case 1:
                if (recvData.len != 1) {
                    AwareLog.e(TAG, "err data num:" + recvData.len + " for cpu high load, expect 1");
                    return;
                }
                CPUHighFgControl.getInstance().notifyLoadChange(recvData.data.get(0).intValue());
                return;
            case 2:
                if (recvData.len != 2) {
                    AwareLog.e(TAG, "err data num:" + recvData.len + " for proc fork connector, expect 2");
                    return;
                }
                VipCgroupControl.getInstance().notifyForkChange(recvData.data.get(0).intValue(), recvData.data.get(1).intValue());
                return;
            case 3:
                if (recvData.len != 2) {
                    AwareLog.e(TAG, "err data num:" + recvData.len + " for proc comm connector, expect 2");
                    return;
                }
                CpuThreadBoost.getInstance().notifyCommChange(recvData.data.get(0).intValue(), recvData.data.get(1).intValue());
                return;
            case 4:
            case 6:
            case 7:
                handleAuxCommData(recvData);
                return;
            case 5:
                handleCpuLoadHighData(recvData);
                return;
            default:
                AwareLog.e(TAG, "error msg what = " + recvData.what);
                return;
        }
    }

    class ReceiveKernelThread implements Runnable {
        ReceiveKernelThread() {
        }

        public void run() {
            Thread.currentThread().setPriority(10);
            while (CPUNetLink.this.mIsStart) {
                try {
                    NetlinkSocket unused = CPUNetLink.this.mLocalNetlinkSocket;
                    ByteBuffer response = NetlinkSocket.recvMessage(CPUNetLink.sFileDescriptor, 8192, 0);
                    if (response != null) {
                        if (StructNlMsgHdr.hasAvailableSpace(response)) {
                            CPUNetLink.this.parse(response);
                        }
                    } else {
                        return;
                    }
                } catch (ErrnoException e) {
                    AwareLog.e(CPUNetLink.TAG, "ReceiveKernelThread ErrnoException");
                    return;
                } catch (InterruptedIOException e2) {
                    AwareLog.e(CPUNetLink.TAG, "ReceiveKernelThread InterruptedIOException");
                    return;
                } catch (IllegalArgumentException e3) {
                    AwareLog.e(CPUNetLink.TAG, "ReceiveKernelThread IllegalArgumentException");
                    return;
                }
            }
        }
    }

    public void start() {
        if (!createImpl()) {
            AwareLog.e(TAG, "Failed to create netlink connection");
            return;
        }
        try {
            NetlinkSocketAddress localAddr = new NetlinkSocketAddress(Process.myPid(), 0);
            if (localAddr instanceof SocketAddress) {
                Os.bind(sFileDescriptor, localAddr);
            }
            try {
                byte[] request = newMsgStruct(0);
                NetlinkSocket netlinkSocket = this.mLocalNetlinkSocket;
                if (NetlinkSocket.sendMessage(sFileDescriptor, request, 0, request.length, 0) == -1) {
                    destroyImpl();
                    return;
                }
                if (this.mReceviveKernelThread == null) {
                    this.mReceviveKernelThread = new ReceiveKernelThread();
                }
                Thread thread = this.mThread;
                if (thread == null || !thread.isAlive()) {
                    this.mThread = new Thread(this.mReceviveKernelThread, "mReceviveKernelThread");
                }
                this.mIsStart = true;
                this.mThread.start();
            } catch (ErrnoException e) {
                destroyImpl();
            } catch (InterruptedIOException e2) {
                destroyImpl();
            } catch (IllegalArgumentException e3) {
                destroyImpl();
            }
        } catch (ErrnoException e4) {
            AwareLog.e(TAG, "start ErrnoException msg: " + e4.getMessage());
            destroyImpl();
        } catch (SocketException e5) {
            AwareLog.e(TAG, "start SocketException msg: " + e5.getMessage());
            destroyImpl();
        }
    }

    public void stop() {
        Thread thread = this.mThread;
        if (thread != null && thread.isAlive()) {
            this.mIsStart = false;
            this.mThread.interrupt();
        }
        destroyImpl();
        this.mThread = null;
    }

    private boolean createImpl() {
        if (this.mLocalNetlinkSocket != null) {
            return true;
        }
        try {
            this.mLocalNetlinkSocket = new NetlinkSocket();
            NetlinkSocket netlinkSocket = this.mLocalNetlinkSocket;
            sFileDescriptor = NetlinkSocket.forProto(33);
            return true;
        } catch (ErrnoException e) {
            AwareLog.e(TAG, "Failed to create connection, ErrnoException");
            destroyImpl();
            return false;
        }
    }

    private void destroyImpl() {
        if (this.mLocalNetlinkSocket != null) {
            this.mLocalNetlinkSocket = null;
        }
        IoUtils.closeQuietly(sFileDescriptor);
    }
}
