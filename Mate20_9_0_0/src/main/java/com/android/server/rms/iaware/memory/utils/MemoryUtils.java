package com.android.server.rms.iaware.memory.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwaredConnection;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.rms.collector.ResourceCollector;
import com.android.server.rms.iaware.feature.MemoryFeature2;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MemoryUtils {
    private static final long FLUSH_TIMEOUT = 2000;
    private static final int MAX_RECV_BYTE_BUFFER_LENTH = 8;
    private static final String MEMORY_SOCKET = "iawared";
    private static final String TAG = "AwareMem_MemoryUtils";
    private static InputStream mInputStream;
    private static final Object mLock = new Object();
    private static LocalSocket mMemorySocket;
    private static OutputStream mOutputStream;

    public static AwareAppMngSortPolicy getAppMngSortPolicy(int resourceType, int groupId) {
        return getAppMngSortPolicy(resourceType, groupId, 0);
    }

    public static AwareAppMngSortPolicy getAppMngSortPolicy(int resourceType, int groupId, int subType) {
        if (!AwareAppMngSort.checkAppMngEnable() || groupId < 0 || groupId > 3) {
            return null;
        }
        AwareAppMngSort sorted = AwareAppMngSort.getInstance();
        if (sorted == null) {
            return null;
        }
        return sorted.getAppMngSortPolicy(resourceType, subType, groupId);
    }

    public static AwareAppMngSortPolicy getAppMngSortPolicyForMemRepair(int sceneType) {
        if (!AwareAppMngSort.checkAppMngEnable()) {
            return null;
        }
        AwareAppMngSort sorted = AwareAppMngSort.getInstance();
        if (sorted == null) {
            return null;
        }
        return sorted.getAppMngSortPolicyForMemRepair(sceneType);
    }

    public static List<AwareProcessInfo> getAppMngSortPolicyForSystemTrim() {
        if (!AwareAppMngSort.checkAppMngEnable()) {
            return null;
        }
        AwareAppMngSort sorted = AwareAppMngSort.getInstance();
        if (sorted == null) {
            return null;
        }
        return sorted.getAppMngSortPolicyForSystemTrim();
    }

    public static List<AwareProcessBlockInfo> getAppMngProcGroup(AwareAppMngSortPolicy policy, int groupId) {
        if (policy == null) {
            AwareLog.e(TAG, "getAppMngProcGroup sort policy null!");
            return null;
        }
        List<AwareProcessBlockInfo> processGroups = null;
        switch (groupId) {
            case 0:
                processGroups = policy.getForbidStopProcBlockList();
                break;
            case 1:
                processGroups = policy.getShortageStopProcBlockList();
                break;
            case 2:
                processGroups = policy.getAllowStopProcBlockList();
                break;
            default:
                AwareLog.w(TAG, "getAppMngProcGroup unknown group id!");
                break;
        }
        return processGroups;
    }

    public static int killProcessGroupForQuickKill(int uid, int pid) {
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            int killProcessGroupForQuickKill = ResourceCollector.killProcessGroupForQuickKill(uid, pid);
            return killProcessGroupForQuickKill;
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public static void writeSwappiness(int swappiness) {
        if (swappiness > 200 || swappiness < 0) {
            AwareLog.w(TAG, "invalid swappiness value");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSwappiness = ");
        stringBuilder.append(swappiness);
        AwareLog.i(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(302);
        buffer.putInt(swappiness);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void writeDirectSwappiness(int directswappiness) {
        if (directswappiness > 200 || directswappiness < 0) {
            AwareLog.w(TAG, "invalid directswappiness value");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDirectSwappiness = ");
        stringBuilder.append(directswappiness);
        AwareLog.i(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_DIRECT_SWAPPINESS);
        buffer.putInt(directswappiness);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static void writeExtraFreeKbytes(int extrafreekbytes) {
        if (extrafreekbytes <= 0 || extrafreekbytes >= 200000) {
            AwareLog.w(TAG, "invalid extrafreekbytes value");
            return;
        }
        int lastExtraFreeKbytes = SystemProperties.getInt("sys.sysctl.extra_free_kbytes", MemoryConstant.PROCESSLIST_EXTRA_FREE_KBYTES);
        if (lastExtraFreeKbytes == extrafreekbytes) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("extrafreekbytes is already ");
            stringBuilder.append(lastExtraFreeKbytes);
            stringBuilder.append(", no need to set");
            AwareLog.d(str, stringBuilder.toString());
            return;
        }
        SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(extrafreekbytes));
    }

    private static void configProtectLru() {
        setProtectLruLimit(MemoryConstant.getConfigProtectLruLimit());
        setProtectLruRatio(MemoryConstant.getConfigProtectLruRatio());
        AwareLog.d(TAG, "onProtectLruConfigUpdate");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE);
        buffer.putInt(0);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
        setFileProtectLru(304);
    }

    public static void enableProtectLru() {
        setProtectLruSwitch(true);
    }

    public static void disableProtectLru() {
        setProtectLruSwitch(false);
    }

    public static void onProtectLruConfigUpdate() {
        configProtectLru();
        disableProtectLru();
    }

    public static void dynamicSetProtectLru(int state) {
        if (state == 1) {
            enableProtectLru();
        } else if (state == 0) {
            disableProtectLru();
        }
    }

    private static void setProtectLruRatio(int ratio) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set ProtectLru ratio = ");
        stringBuilder.append(ratio);
        AwareLog.d(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO);
        buffer.putInt(ratio);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    private static void setFileProtectLru(int commandType) {
        ArrayMap<Integer, ArraySet<String>> filterMap = MemoryConstant.getFileCacheMap();
        if (filterMap != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set ProtectLru filterMap size:");
            stringBuilder.append(filterMap.size());
            AwareLog.i(str, stringBuilder.toString());
            ByteBuffer buffer = ByteBuffer.allocate(272);
            int i = 0;
            try {
                int mapSize = filterMap.size();
                while (i < mapSize) {
                    int index = ((Integer) filterMap.keyAt(i)).intValue();
                    int isDir = 0;
                    if (index > 50) {
                        index -= 50;
                        isDir = 1;
                    }
                    ArraySet<String> filterSet = (ArraySet) filterMap.valueAt(i);
                    if (filterSet != null) {
                        Iterator it = filterSet.iterator();
                        while (it.hasNext()) {
                            String filterStr = (String) it.next();
                            if (!TextUtils.isEmpty(filterStr)) {
                                String str2;
                                StringBuilder stringBuilder2;
                                byte[] stringBytes = filterStr.getBytes("UTF-8");
                                if (stringBytes.length >= 1) {
                                    if (stringBytes.length <= 255) {
                                        str2 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("setPackageProtectLru filterStr = ");
                                        stringBuilder2.append(filterStr);
                                        AwareLog.d(str2, stringBuilder2.toString());
                                        buffer.clear();
                                        buffer.putInt(commandType);
                                        buffer.putInt(isDir);
                                        buffer.putInt(index);
                                        buffer.putInt(stringBytes.length);
                                        buffer.put(stringBytes);
                                        buffer.putChar(0);
                                        if (sendPacket(buffer) != 0) {
                                            AwareLog.w(TAG, "setPackageProtectLru sendPacket failed");
                                        }
                                    }
                                }
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("setPackageProtectLru incorrect filterStr = ");
                                stringBuilder2.append(filterStr);
                                AwareLog.w(str2, stringBuilder2.toString());
                            }
                        }
                        i++;
                    } else {
                        return;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "setPackageProtectLru UTF-8 not supported");
            }
        }
    }

    private static void setProtectLruLimit(String lruConfigStr) {
        if (checkLimitConfigStr(lruConfigStr)) {
            ByteBuffer buffer = ByteBuffer.allocate(268);
            try {
                String str;
                StringBuilder stringBuilder;
                byte[] stringBytes = lruConfigStr.getBytes("UTF-8");
                if (stringBytes.length >= 1) {
                    if (stringBytes.length <= 255) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("setProtectLruLimit configstr=");
                        stringBuilder.append(lruConfigStr);
                        AwareLog.d(str, stringBuilder.toString());
                        buffer.clear();
                        buffer.putInt(305);
                        buffer.putInt(stringBytes.length);
                        buffer.put(stringBytes);
                        buffer.putChar(0);
                        if (sendPacket(buffer) != 0) {
                            AwareLog.w(TAG, "setProtectLruLimit sendPacket failed");
                        }
                        return;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setProtectLruLimit incorrect config = ");
                stringBuilder.append(lruConfigStr);
                AwareLog.w(str, stringBuilder.toString());
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "setProtectLruLimit UTF-8 not supported?!?");
            }
        }
    }

    private static boolean checkLimitConfigStr(String lruConfigStr) {
        if (lruConfigStr == null) {
            return false;
        }
        String[] lruConfigStrArray = lruConfigStr.split(" ");
        if (lruConfigStrArray.length != 3) {
            return false;
        }
        for (int levelValue : lruConfigStrArray) {
            int levelValue2 = Integer.parseInt(levelValue2);
            if (levelValue2 < 0 || levelValue2 > 100) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("protect lru level value is invalid: ");
                stringBuilder.append(levelValue2);
                AwareLog.w(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private static void setProtectLruSwitch(boolean isEnable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set ProtectLru switch = ");
        stringBuilder.append(isEnable);
        AwareLog.d(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_PROTECTLRU_SWITCH);
        buffer.putInt(isEnable);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static boolean checkRamSize(String ramSize, Long totalMemMb) {
        if (ramSize == null) {
            return false;
        }
        try {
            long ramSizeL = Long.parseLong(ramSize.trim());
            if (totalMemMb.longValue() > ramSizeL || totalMemMb.longValue() <= ramSizeL - 1024) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parse ramsze error: ");
            stringBuilder.append(e);
            AwareLog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static void setReclaimGPUMemory(boolean isCompress, int pid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(isCompress ? "compress" : "decompress");
        stringBuilder.append(" GPU memory, pid:");
        stringBuilder.append(pid);
        AwareLog.d(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(MemoryConstant.MSG_COMPRESS_GPU);
        buffer.putInt(isCompress);
        buffer.putInt(pid);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    public static boolean trimMemory(HwActivityManagerService hwAms, String proc, int level) {
        return trimMemory(hwAms, proc, -2, level, true);
    }

    public static boolean trimMemory(HwActivityManagerService hwAms, String proc, int userId, int level, boolean fromIAware) {
        String str;
        StringBuilder stringBuilder;
        boolean ret = false;
        if (hwAms == null) {
            return false;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("trim Memory, proc:");
        stringBuilder2.append(proc);
        stringBuilder2.append(" userId:");
        stringBuilder2.append(userId);
        stringBuilder2.append(" level:");
        stringBuilder2.append(level);
        stringBuilder2.append(" fromIAware:");
        stringBuilder2.append(fromIAware);
        AwareLog.d(str2, stringBuilder2.toString());
        try {
            ret = hwAms.setProcessMemoryTrimLevel(proc, userId, level, fromIAware);
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("trim Memory remote exception, proc:");
            stringBuilder.append(proc);
            stringBuilder.append(" userId:");
            stringBuilder.append(userId);
            stringBuilder.append(" level:");
            stringBuilder.append(level);
            stringBuilder.append(" fromIAware:");
            stringBuilder.append(fromIAware);
            AwareLog.w(str, stringBuilder.toString());
        } catch (IllegalArgumentException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("trim Memory illegal arg exception, proc:");
            stringBuilder.append(proc);
            stringBuilder.append(" userId:");
            stringBuilder.append(userId);
            stringBuilder.append(" level:");
            stringBuilder.append(level);
            stringBuilder.append(" fromIAware:");
            stringBuilder.append(fromIAware);
            AwareLog.w(str, stringBuilder.toString());
        }
        return ret;
    }

    public static void sendActivityDisplayedTime(String activityName, int pid, int time) {
        ByteBuffer buffer = ByteBuffer.allocate(276);
        try {
            String str;
            StringBuilder stringBuilder;
            byte[] stringBytes = activityName.getBytes("UTF-8");
            if (stringBytes.length >= 1) {
                if (stringBytes.length <= 255) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sendActivityDisplayedTime: ");
                    stringBuilder.append(activityName);
                    stringBuilder.append(" ");
                    stringBuilder.append(pid);
                    stringBuilder.append(" ");
                    stringBuilder.append(time);
                    AwareLog.d(str, stringBuilder.toString());
                    buffer.clear();
                    buffer.putInt(MemoryConstant.MSG_ACTIVITY_DISPLAY_STATISTICS);
                    buffer.putInt(pid);
                    buffer.putInt(time);
                    buffer.putInt(stringBytes.length);
                    buffer.put(stringBytes);
                    buffer.putChar(0);
                    if (sendPacket(buffer) != 0) {
                        AwareLog.w(TAG, "sendActivityDisplayedTime sendPacket failed");
                    }
                    return;
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendActivityDisplayedTime incorrect activityName = ");
            stringBuilder.append(activityName);
            AwareLog.w(str, stringBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            AwareLog.w(TAG, "sendActivityDisplayedTime UTF-8 not supported?!?");
        }
    }

    public static void writeMMonitorSwitch(int switchValue) {
        if (switchValue > 1 || switchValue < 0) {
            AwareLog.w(TAG, "invalid mmonitor switch value");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("writeMMonitorSwitch = ");
        stringBuilder.append(switchValue);
        AwareLog.i(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(MemoryConstant.MSG_MMONITOR_SWITCH);
        buffer.putInt(switchValue);
        IAwaredConnection.getInstance().sendPacket(buffer.array());
    }

    private static void createSocket() {
        if (mMemorySocket == null) {
            try {
                mMemorySocket = new LocalSocket(3);
                mMemorySocket.connect(new LocalSocketAddress("iawared", Namespace.RESERVED));
                mOutputStream = mMemorySocket.getOutputStream();
                mInputStream = mMemorySocket.getInputStream();
                mMemorySocket.setReceiveBufferSize(8);
                AwareLog.d(TAG, "createSocket Success!");
            } catch (IOException e) {
                AwareLog.e(TAG, "createSocket happend IOException");
                destroySocket();
            }
        }
    }

    public static void destroySocket() {
        synchronized (mLock) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (IOException e) {
                    AwareLog.e(TAG, "mOutputStream close failed! happend IOException");
                }
                mOutputStream = null;
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e2) {
                    AwareLog.e(TAG, "mInputStream close failed! happend IOException");
                }
                mInputStream = null;
            }
            if (mMemorySocket != null) {
                try {
                    mMemorySocket.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "closeSocket failed! happend IOException");
                }
                mMemorySocket = null;
            }
        }
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:4:0x0006, B:16:0x001e] */
    /* JADX WARNING: Missing block: B:22:0x0034, code skipped:
            android.rms.iaware.AwareLog.e(TAG, "mOutputStream write failed! happend IOException");
            destroySocket();
            r2 = r2 - 1;
     */
    /* JADX WARNING: Missing block: B:29:0x0040, code skipped:
            continue;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int sendPacket(ByteBuffer buffer) {
        synchronized (mLock) {
            if (buffer == null) {
                AwareLog.w(TAG, "sendPacket ByteBuffer is null!");
                return -1;
            }
            int retry = 2;
            do {
                if (mMemorySocket == null) {
                    createSocket();
                }
                if (mOutputStream != null) {
                    mOutputStream.write(buffer.array(), 0, buffer.position());
                    flush(FLUSH_TIMEOUT);
                    return 0;
                }
            } while (retry > 0);
            return -1;
        }
    }

    private static void flush(long millis) throws IOException {
        FileDescriptor myFd = mMemorySocket.getFileDescriptor();
        if (myFd != null) {
            long start = SystemClock.uptimeMillis();
            Int32Ref pending = new Int32Ref(0);
            while (true) {
                try {
                    Os.ioctlInt(myFd, OsConstants.TIOCOUTQ, pending);
                    if (pending.value > 0) {
                        if (SystemClock.uptimeMillis() - start < millis) {
                            int left = pending.value;
                            if (left <= 1000) {
                                try {
                                    Thread.sleep(0, 10);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            } else if (left <= 5000) {
                                Thread.sleep(0, 500);
                            } else {
                                Thread.sleep(1);
                            }
                        } else {
                            AwareLog.e(TAG, "Socket flush timed out !!!");
                            throw new IOException("flush timeout");
                        }
                    }
                    return;
                } catch (ErrnoException e2) {
                    throw e2.rethrowAsIOException();
                }
            }
        }
        throw new IOException("socket closed");
    }

    public static byte[] recvPacket(int byteSize) {
        byte[] emptyByte = new byte[0];
        if (byteSize <= 0 || mInputStream == null) {
            return emptyByte;
        }
        byte[] recvByte = new byte[byteSize];
        try {
            Arrays.fill(recvByte, (byte) 0);
            if (mInputStream.read(recvByte) == 0) {
                return emptyByte;
            }
            return recvByte;
        } catch (IOException e) {
            AwareLog.e(TAG, " mInputStream write failed happend IOException!");
            destroySocket();
            return emptyByte;
        }
    }

    public static void enterSpecialSceneNotify(int total_watermark, int worker_mask, int autostop_timeout) {
        String str;
        StringBuilder stringBuilder;
        if (!MemoryFeature2.isUpMemoryFeature.get() || MemoryConstant.getConfigIonSpeedupSwitch() == 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("iaware2.0 mem feature  ");
            stringBuilder.append(MemoryFeature2.isUpMemoryFeature.get());
            stringBuilder.append(",  camera ion memory speedup switch ");
            stringBuilder.append(MemoryConstant.getConfigIonSpeedupSwitch());
            AwareLog.w(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Enter special scene, total_watermark: ");
        stringBuilder.append(total_watermark);
        stringBuilder.append(", worker_mask: ");
        stringBuilder.append(worker_mask);
        stringBuilder.append(", autostop_timeout: ");
        stringBuilder.append(autostop_timeout);
        AwareLog.i(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(340);
        buffer.putInt(total_watermark);
        buffer.putInt(worker_mask);
        buffer.putInt(autostop_timeout);
        sendPacket(buffer);
    }

    public static void exitSpecialSceneNotify() {
        if (!MemoryFeature2.isUpMemoryFeature.get() || MemoryConstant.getConfigIonSpeedupSwitch() == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware2.0 mem feature ");
            stringBuilder.append(MemoryFeature2.isUpMemoryFeature.get());
            stringBuilder.append(",  camera ion memory speedup switch ");
            stringBuilder.append(MemoryConstant.getConfigIonSpeedupSwitch());
            AwareLog.w(str, stringBuilder.toString());
            return;
        }
        AwareLog.i(TAG, "Exit special scene");
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(MemoryConstant.MSG_SPECIAL_SCENE_POOL_EXIT);
        sendPacket(buffer);
    }
}
