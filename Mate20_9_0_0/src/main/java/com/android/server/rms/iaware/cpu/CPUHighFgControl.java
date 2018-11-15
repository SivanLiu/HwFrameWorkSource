package com.android.server.rms.iaware.cpu;

import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.SparseArray;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CPUHighFgControl {
    private static final String CGROUP_CPUACCT = ":cpuacct";
    private static final String CGROUP_CPUSET = ":cpuset";
    private static final String CGROUP_CPUSET_BG = "background";
    private static final String CPUCTL_LIMIT_PROC_PATH = "/dev/cpuctl/limit/cgroup.procs";
    private static final String CPUCTL_PROC_PATH = "/dev/cpuctl/cgroup.procs";
    private static final String CPUSET_FG_PROC_PATH = "/dev/cpuset/foreground/cgroup.procs";
    private static final String CPUSET_TA_PROC_PATH = "/dev/cpuset/top-app/cgroup.procs";
    private static final int CPU_FG_LOAD_LIMIT_DEFAULT = 60;
    private static final int CPU_FG_LOAD_THRESHOLD_DEFAULT = 80;
    private static final int CPU_HIGHLOAD_POLLING_INTERVAL_DEFAULT = 1000;
    private static final int CPU_LOAD_LOW_THRESHOLD_DEFAULT = 60;
    private static final int CPU_LOAD_THRESHOLD_MAX = 100;
    private static final int CPU_LOAD_THRESHOLD_MIN = 20;
    private static final int CPU_POLLING_INTERVAL_MAX_VALUE = 5000;
    private static final int CPU_POLLING_INTERVAL_MIN_VALUE = 100;
    private static final String CPU_STAT_PATH = "/proc/stat";
    private static final int CPU_TA_LOAD_REGULAR_THRESHOLD_DEFAULT = 75;
    private static final int CPU_TA_LOAD_THRESHOLD_DEFAULT = 90;
    private static final long INVALID_VALUE = -1;
    private static final String ITEM_CPU_FG_LOAD_LIMIT = "cpu_fg_load_limit";
    private static final String ITEM_CPU_FG_LOAD_THRESHOLD = "cpu_fg_load_threshold";
    private static final String ITEM_CPU_HIGHLOAD_POLLING_INTERVAL = "cpu_highload_polling_interval";
    private static final String ITEM_CPU_LOAD_LOW_THRESHOLD = "cpu_load_low_threshold";
    private static final String ITEM_CPU_TA_LOAD_THRESHOLD = "cpu_ta_load_threshold";
    private static final int LOAD_DETCTOR_DELAY = 10000;
    private static final int LOAD_STATUS_DETECTOR = 4;
    private static final int LOAD_STATUS_HIGH = 2;
    private static final int LOAD_STATUS_IDLE = 3;
    private static final int LOAD_STATUS_LOW = 1;
    private static final int MSG_LOAD_DETCTOR = 1;
    private static final String PATH_COMM = "/comm";
    private static final String PATH_STAT = "/stat";
    private static final String PTAH_CGROUP = "/cgroup";
    private static final String PTAH_PROC_INFO = "/proc/";
    private static final String SYSTEMUI_NAME = "com.android.systemui";
    private static final String TAG = "CPUHighFgControl";
    private static final String UID_PTAH = "uid_";
    private static CPUHighFgControl sInstance;
    private CPUFeature mCPUFeatureInstance;
    private CPUZRHungLog mCPUZRHung;
    private int mCpuFgLoadLimit = 60;
    private int mCpuFgLoadThreshold = 80;
    private int mCpuLoadLowThreshold = 60;
    private int mCpuTaLoadThreshold = CPU_TA_LOAD_THRESHOLD_DEFAULT;
    private volatile boolean mIsStart = false;
    private CpuLoadDetectorHandler mLoadDetectorHandler;
    private Thread mLoadDetectorThread;
    private int mLoadStatus = 3;
    private Object mLock = new Object();
    private int mPollingInterval = 1000;
    private ProcLoadComparator mProcLoadComparator;
    private TALoadDetectorThread mTaLoadDetectorThread;

    private class CpuLoadDetectorHandler extends Handler {
        private CpuLoadDetectorHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what != 1) {
                String str = CPUHighFgControl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CpuLoadDetectorHandler msg = ");
                stringBuilder.append(what);
                stringBuilder.append(" not support!");
                AwareLog.e(str, stringBuilder.toString());
                return;
            }
            CPUHighFgControl.this.notifyLoadChange(4);
        }
    }

    private static class ProcLoadComparator implements Comparator<ProcLoadPair>, Serializable {
        private static final long serialVersionUID = 1;

        private ProcLoadComparator() {
        }

        public int compare(ProcLoadPair lhs, ProcLoadPair rhs) {
            return Integer.compare(rhs.load, lhs.load);
        }
    }

    private static class ProcLoadPair {
        public int load;
        public int pid;

        public ProcLoadPair(int pid, int load) {
            this.pid = pid;
            this.load = load;
        }
    }

    private static class ProcessStatInfo {
        public long idle;
        public long iowait;
        public long irq;
        public long nice;
        public long softIrq;
        public long system;
        public long userTime;

        private ProcessStatInfo() {
        }

        public long getWallTime() {
            return (((((this.userTime + this.nice) + this.system) + this.idle) + this.iowait) + this.irq) + this.softIrq;
        }

        public long getRunningTime() {
            return (this.userTime + this.nice) + this.system;
        }
    }

    private class TALoadDetectorThread implements Runnable {
        private TALoadDetectorThread() {
        }

        public void run() {
            Thread.currentThread().setPriority(10);
            while (CPUHighFgControl.this.mIsStart) {
                synchronized (CPUHighFgControl.this.mLock) {
                    while (CPUHighFgControl.this.mLoadStatus == 3) {
                        try {
                            CPUHighFgControl.this.mLock.wait();
                        } catch (InterruptedException e) {
                            AwareLog.e(CPUHighFgControl.TAG, "TALoadDetectorThread InterruptedException return");
                        }
                    }
                    if (CPUHighFgControl.this.mLoadStatus == 4) {
                        AwareLog.d(CPUHighFgControl.TAG, "DetectorThread run for load detector!");
                        CPUHighFgControl.this.startLoadDetector();
                        CPUHighFgControl.this.mLoadStatus = 3;
                    } else if (CPUHighFgControl.this.mLoadStatus == 2) {
                        AwareLog.d(CPUHighFgControl.TAG, "DetectorThread run for high load!");
                        CPUHighFgControl.this.setHighLoadPid();
                        CPUHighFgControl.this.mLoadStatus = 3;
                        CPUHighFgControl.this.rotateStatus(2);
                        CPUHighFgControl.this.sendMessageForLoadDetector();
                    } else if (CPUHighFgControl.this.mLoadStatus == 1) {
                        AwareLog.d(CPUHighFgControl.TAG, "DetectorThread run for low load!");
                        CPUHighFgControl.this.setLowLoad();
                        CPUHighFgControl.this.mLoadStatus = 3;
                        CPUHighFgControl.this.rotateStatus(1);
                        CPUHighFgControl.this.sendMessageForLoadDetector();
                    }
                }
            }
        }
    }

    private CPUHighFgControl() {
        initXml();
        this.mCPUZRHung = new CPUZRHungLog();
        this.mProcLoadComparator = new ProcLoadComparator();
        this.mLoadDetectorHandler = new CpuLoadDetectorHandler();
    }

    public static synchronized CPUHighFgControl getInstance() {
        CPUHighFgControl cPUHighFgControl;
        synchronized (CPUHighFgControl.class) {
            if (sInstance == null) {
                sInstance = new CPUHighFgControl();
            }
            cPUHighFgControl = sInstance;
        }
        return cPUHighFgControl;
    }

    private boolean isValidValue(Integer value, int min, int max) {
        if (value == null || value.intValue() <= min || value.intValue() >= max) {
            return false;
        }
        return true;
    }

    private void initXml() {
        Map<String, Integer> cpuCtlFGValueMap = new CpuCtlLimitConfig().getCpuCtlFGValueMap();
        if (!cpuCtlFGValueMap.isEmpty()) {
            Integer value = (Integer) cpuCtlFGValueMap.get(ITEM_CPU_HIGHLOAD_POLLING_INTERVAL);
            if (isValidValue(value, 100, CPU_POLLING_INTERVAL_MAX_VALUE)) {
                this.mPollingInterval = value.intValue();
            }
            value = (Integer) cpuCtlFGValueMap.get(ITEM_CPU_TA_LOAD_THRESHOLD);
            if (isValidValue(value, 20, 100)) {
                this.mCpuTaLoadThreshold = value.intValue();
            }
            value = (Integer) cpuCtlFGValueMap.get(ITEM_CPU_FG_LOAD_THRESHOLD);
            if (isValidValue(value, 20, 100)) {
                this.mCpuFgLoadThreshold = value.intValue();
            }
            value = (Integer) cpuCtlFGValueMap.get(ITEM_CPU_FG_LOAD_LIMIT);
            if (isValidValue(value, 20, 100)) {
                this.mCpuFgLoadLimit = value.intValue();
            }
            value = (Integer) cpuCtlFGValueMap.get(ITEM_CPU_LOAD_LOW_THRESHOLD);
            if (isValidValue(value, 20, 100)) {
                this.mCpuLoadLowThreshold = value.intValue();
            }
        }
    }

    public void start(CPUFeature feature) {
        this.mCPUFeatureInstance = feature;
        if (this.mTaLoadDetectorThread == null) {
            this.mTaLoadDetectorThread = new TALoadDetectorThread();
        }
        if (this.mLoadDetectorThread == null || !this.mLoadDetectorThread.isAlive()) {
            this.mLoadDetectorThread = new Thread(this.mTaLoadDetectorThread, "taLoadDetectorThread");
        }
        this.mIsStart = true;
        this.mLoadDetectorThread.start();
    }

    public void stop() {
        this.mLoadDetectorHandler.removeMessages(1);
        if (this.mLoadDetectorThread != null && this.mLoadDetectorThread.isAlive()) {
            this.mIsStart = false;
            this.mLoadDetectorThread.interrupt();
        }
        this.mLoadDetectorThread = null;
    }

    public void notifyLoadChange(int cpuLoadStatus) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyLoadChange cpuLoadStatus ");
        stringBuilder.append(cpuLoadStatus);
        AwareLog.d(str, stringBuilder.toString());
        synchronized (this.mLock) {
            this.mLoadStatus = cpuLoadStatus;
            this.mLock.notifyAll();
        }
    }

    private int taLoadCompute() {
        long totalCpuTimeLocal = getCpuLoad();
        int pid = getFileContent(CPUSET_TA_PROC_PATH);
        if (pid == -1) {
            return 0;
        }
        long processCpuTimeLocal = getProcessLoad(pid);
        if (totalCpuTimeLocal == -1 || processCpuTimeLocal == -1) {
            return 0;
        }
        try {
            Thread.sleep((long) this.mPollingInterval);
        } catch (InterruptedException e) {
            AwareLog.e(TAG, "taLoadCompute InterruptedException");
        }
        int tempPid = getFileContent(CPUSET_TA_PROC_PATH);
        if (pid != tempPid) {
            return 0;
        }
        long nextProcessLoad = getProcessLoad(pid);
        long nextCpuLoad = getCpuLoad();
        if (nextCpuLoad == -1) {
        } else if (nextProcessLoad == -1) {
            int i = tempPid;
        } else {
            long totalDeltaCpuTime = nextCpuLoad - totalCpuTimeLocal;
            long totalDeltaProcessTime = nextProcessLoad - processCpuTimeLocal;
            if (totalDeltaCpuTime == 0) {
                return 0;
            }
            return (int) ((100 * totalDeltaProcessTime) / totalDeltaCpuTime);
        }
        return 0;
    }

    private int computeFgProcLoad(List<ProcLoadPair> fgProcLoadArray) {
        if (fgProcLoadArray == null) {
            return 0;
        }
        long totalCpuTime = getCpuLoad();
        if (-1 == totalCpuTime) {
            return 0;
        }
        SparseArray<Long> pidLoadList = new SparseArray();
        getFgCpuloadPidArray(pidLoadList);
        try {
            Thread.sleep((long) this.mPollingInterval);
        } catch (InterruptedException e) {
            AwareLog.e(TAG, "computeFgProcLoad InterruptedException");
        }
        SparseArray<Long> nextPidLoadList = new SparseArray();
        getFgCpuloadPidArray(nextPidLoadList);
        long nextTotalCpuTime = getCpuLoad();
        if (-1 == nextTotalCpuTime) {
            return 0;
        }
        long totalCpuRuntime = nextTotalCpuTime - totalCpuTime;
        if (totalCpuRuntime == 0) {
            return 0;
        }
        return computeProcsLoad(pidLoadList, nextPidLoadList, totalCpuRuntime, fgProcLoadArray);
    }

    private int computeProcsLoad(SparseArray<Long> prevArray, SparseArray<Long> currArray, long totalTime, List<ProcLoadPair> fgProcLoadArray) {
        int i = 0;
        if (prevArray == null || currArray == null || totalTime == 0 || fgProcLoadArray == null) {
            AwareLog.e(TAG, "computeProcsLoad invalid params");
            return 0;
        }
        int totalLoad = 0;
        int currArraySize = currArray.size();
        while (i < currArraySize) {
            int procPid = currArray.keyAt(i);
            if (prevArray.indexOfKey(procPid) >= 0) {
                int procLoad = (int) ((100 * (((Long) currArray.get(procPid)).longValue() - ((Long) prevArray.get(procPid)).longValue())) / totalTime);
                if (procLoad == 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("pid ");
                    stringBuilder.append(procPid);
                    stringBuilder.append(" cpuload is 0 ");
                    AwareLog.d(str, stringBuilder.toString());
                } else {
                    fgProcLoadArray.add(new ProcLoadPair(procPid, procLoad));
                    totalLoad += procLoad;
                }
            }
            i++;
        }
        if (!fgProcLoadArray.isEmpty()) {
            Collections.sort(fgProcLoadArray, this.mProcLoadComparator);
        }
        return totalLoad;
    }

    private void getFgProcsloadPidArray(String filePath, SparseArray<Long> pidList) {
        List<String> procsList = getProcsList(filePath);
        if (procsList != null) {
            int size = procsList.size();
            for (int i = 0; i < size; i++) {
                int pid = parseInt((String) procsList.get(i));
                if (pid >= 0 && isFgProc(pid) && isLimitProc(pid)) {
                    long tempLoad = getProcessLoad(pid);
                    if (-1 != tempLoad) {
                        pidList.put(pid, Long.valueOf(tempLoad));
                    }
                }
            }
        }
    }

    private List<String> getProcsList(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            String groupProcs = null;
            int i = 0;
            try {
                groupProcs = FileUtils.readTextFile(file, 0, null);
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IOException + ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
            if (groupProcs == null || groupProcs.length() == 0) {
                return null;
            }
            String[] procs = groupProcs.split("\n");
            List<String> procsList = new ArrayList();
            int length = procs.length;
            while (i < length) {
                String strProc = procs[i].trim();
                if (strProc.length() != 0) {
                    procsList.add(strProc);
                }
                i++;
            }
            return procsList;
        }
        AwareLog.e(TAG, "getProcsList file not exists or canot read!");
        return null;
    }

    private void getLimitFgProcsLoadArray(String filePath, SparseArray<Long> pidList) {
        List<String> procsList = getProcsList(filePath);
        if (procsList != null) {
            int size = procsList.size();
            for (int i = 0; i < size; i++) {
                int pid = parseInt((String) procsList.get(i));
                if (pid >= 0) {
                    long tempLoad = getProcessLoad(pid);
                    if (-1 != tempLoad) {
                        pidList.put(pid, Long.valueOf(tempLoad));
                    }
                }
            }
        }
    }

    private void getFgCpuloadPidArray(SparseArray<Long> pidList) {
        getFgProcsloadPidArray(CPUSET_FG_PROC_PATH, pidList);
    }

    private void getLimitFgCpuloadPidArray(SparseArray<Long> pidList) {
        getLimitFgProcsLoadArray(CPUCTL_LIMIT_PROC_PATH, pidList);
    }

    private int computeLoad(long deltaTotaltime, long deltaRuntime) {
        if (deltaTotaltime != 0) {
            return (int) ((100 * deltaRuntime) / deltaTotaltime);
        }
        AwareLog.e(TAG, "computeLoad deltaTotaltime is zero!");
        return -1;
    }

    private void startLoadDetector() {
        ProcessStatInfo prevCpuStatInfo = getCpuStatInfo();
        if (prevCpuStatInfo == null) {
            AwareLog.e(TAG, "startLoadDetector prevStatInfo is null!");
            return;
        }
        SparseArray<Long> prevLimFgArray = new SparseArray();
        getLimitFgCpuloadPidArray(prevLimFgArray);
        try {
            Thread.sleep((long) (3 * this.mPollingInterval));
        } catch (InterruptedException e) {
            AwareLog.e(TAG, "startLoadDetector InterruptedException");
        }
        SparseArray<Long> currLimFgArray = new SparseArray();
        getLimitFgCpuloadPidArray(currLimFgArray);
        ProcessStatInfo currCpuStatInfo = getCpuStatInfo();
        if (currCpuStatInfo == null) {
            AwareLog.e(TAG, "startLoadDetector currStatInfo is null!");
            return;
        }
        long deltaTotalCpuRuntime = currCpuStatInfo.getWallTime() - prevCpuStatInfo.getWallTime();
        int cpuLoad = computeLoad(deltaTotalCpuRuntime, currCpuStatInfo.getRunningTime() - prevCpuStatInfo.getRunningTime());
        if (cpuLoad == -1) {
            AwareLog.e(TAG, "startLoadDetector computeLoad maybe error!");
            return;
        }
        loadDetectorPolicy(cpuLoad, deltaTotalCpuRuntime, prevLimFgArray, currLimFgArray);
        if (cpuLoad >= this.mCpuTaLoadThreshold) {
            loadZRHungPolicy();
        }
    }

    private boolean isLimitGroupHasPid() {
        List<String> limitPidList = getProcsList(CPUCTL_LIMIT_PROC_PATH);
        if (limitPidList == null || limitPidList.size() <= 0) {
            return false;
        }
        return true;
    }

    private void loadDetectorPolicy(int cpuLoad, long cpuTotalTime, SparseArray<Long> prevArray, SparseArray<Long> currArray) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadDetectorPolicy cpu_total_Load = ");
        stringBuilder.append(cpuLoad);
        AwareLog.d(str, stringBuilder.toString());
        if (cpuLoad >= this.mCpuTaLoadThreshold) {
            int totalLimitFgLoad = computeProcsLoad(prevArray, currArray, cpuTotalTime, new ArrayList());
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("loadDetectorPolicy limit totalLoad = ");
            stringBuilder2.append(totalLimitFgLoad);
            AwareLog.d(str2, stringBuilder2.toString());
            if (totalLimitFgLoad >= this.mCpuTaLoadThreshold) {
                AwareLog.d(TAG, "loadDetectorPolicy limit + ta load high, and a new high load process may be add in the system, so continue detector!");
            } else {
                AwareLog.d(TAG, "limit low, need to find another high load pid to control");
                setHighLoadPid();
            }
        } else if (cpuLoad <= this.mCpuLoadLowThreshold) {
            AwareLog.d(TAG, "loadDetectorPolicy setLowLoad");
            setLowLoad();
        }
        if (isLimitGroupHasPid()) {
            AwareLog.d(TAG, "loadDetectorPolicy limit group has pid, so continue detector!");
            sendMessageForLoadDetector();
        }
    }

    private void loadZRHungPolicy() {
        this.mCPUZRHung.loadPolicy();
    }

    private void rotateStatus(int loadStatus) {
        this.mCPUZRHung.rotateStatus(loadStatus);
    }

    private void chgProcessGroup(int pid, String groupPath) {
        if (pid > 0) {
            setGroup(pid, groupPath);
        }
    }

    private void chgProcessGroup(List<ProcLoadPair> procLoadArray, String groupPath) {
        if (procLoadArray != null) {
            int currLoad = 0;
            int size = procLoadArray.size();
            for (int i = 0; i < size; i++) {
                ProcLoadPair pair = (ProcLoadPair) procLoadArray.get(i);
                if (pair != null) {
                    if (currLoad >= this.mCpuFgLoadLimit) {
                        break;
                    }
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("chgProcessGroup list pid = ");
                    stringBuilder.append(pair.pid);
                    stringBuilder.append(" load = ");
                    stringBuilder.append(pair.load);
                    AwareLog.d(str, stringBuilder.toString());
                    currLoad += pair.load;
                    chgProcessGroup(pair.pid, groupPath);
                }
            }
        }
    }

    private int getFileContent(String filePath) {
        String str;
        StringBuilder stringBuilder;
        if (filePath == null) {
            return -1;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            return -1;
        }
        int pid = 0;
        FileInputStream input = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufReader = null;
        try {
            input = new FileInputStream(filePath);
            inputStreamReader = new InputStreamReader(input, "UTF-8");
            bufReader = new BufferedReader(inputStreamReader);
            while (true) {
                String readLine = bufReader.readLine();
                String content = readLine;
                if (readLine == null) {
                    break;
                }
                pid = Integer.parseInt(content.trim());
                if (isFgProc(pid)) {
                    break;
                }
                pid = 0;
            }
        } catch (NumberFormatException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        } catch (FileNotFoundException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception file not found, file path: ");
            stringBuilder.append(filePath);
            AwareLog.e(str, stringBuilder.toString());
        } catch (UnsupportedEncodingException e3) {
            AwareLog.e(TAG, "UnsupportedEncodingException ");
        } catch (IOException e4) {
            AwareLog.e(TAG, "IOException");
        } catch (Throwable th) {
            FileContent.closeBufferedReader(null);
            FileContent.closeInputStreamReader(null);
            FileContent.closeFileInputStream(null);
        }
        FileContent.closeBufferedReader(bufReader);
        FileContent.closeInputStreamReader(inputStreamReader);
        FileContent.closeFileInputStream(input);
        return pid;
    }

    private void setHighLoadPid() {
        int taLoad = taLoadCompute();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setHighLoadPid taLoadCompute = ");
        stringBuilder.append(taLoad);
        AwareLog.d(str, stringBuilder.toString());
        setHightLoad(taLoad);
    }

    private void setHightLoad(int taLoad) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setHighLoad taLoad is ");
        stringBuilder.append(taLoad);
        AwareLog.d(str, stringBuilder.toString());
        if (taLoad >= this.mCpuTaLoadThreshold) {
            chgProcessGroup(getFileContent(CPUSET_TA_PROC_PATH), CPUCTL_LIMIT_PROC_PATH);
            return;
        }
        List procLoadArray = new ArrayList();
        int totalProcLoad = computeFgProcLoad(procLoadArray);
        int procLoadArraySize = procLoadArray.size();
        for (int i = 0; i < procLoadArraySize; i++) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("fg the i= ");
            stringBuilder2.append(i);
            stringBuilder2.append(" load is ");
            stringBuilder2.append(((ProcLoadPair) procLoadArray.get(i)).load);
            stringBuilder2.append(" pid is ");
            stringBuilder2.append(((ProcLoadPair) procLoadArray.get(i)).pid);
            AwareLog.d(str2, stringBuilder2.toString());
        }
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("setHighLoadPid computeFgProcLoad = ");
        stringBuilder3.append(totalProcLoad);
        AwareLog.d(str3, stringBuilder3.toString());
        if (totalProcLoad >= this.mCpuFgLoadThreshold || taLoad + totalProcLoad >= this.mCpuTaLoadThreshold) {
            chgProcessGroup(procLoadArray, CPUCTL_LIMIT_PROC_PATH);
            return;
        }
        if (taLoad >= CPU_TA_LOAD_REGULAR_THRESHOLD_DEFAULT) {
            chgProcessGroup(getFileContent(CPUSET_TA_PROC_PATH), CPUCTL_LIMIT_PROC_PATH);
        }
    }

    private void setLowLoad() {
        File file = new File(CPUCTL_LIMIT_PROC_PATH);
        if (file.exists() && file.canRead()) {
            String groupProcs = null;
            int i = 0;
            try {
                groupProcs = FileUtils.readTextFile(file, 0, null);
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IOException + ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
            if (groupProcs == null || groupProcs.length() == 0) {
                AwareLog.d(TAG, "setLowLoad readTextFile null procs!");
                return;
            }
            String[] procs = groupProcs.split("\n");
            int length = procs.length;
            while (i < length) {
                String strProc = procs[i].trim();
                if (strProc.length() != 0) {
                    int pid = parseInt(strProc);
                    if (pid != -1) {
                        setGroup(pid, CPUCTL_PROC_PATH);
                    }
                }
                i++;
            }
            return;
        }
        AwareLog.e(TAG, "setLowLoad file not exists or canot read!");
    }

    private void setGroup(int pid, String file) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        if (CPUCTL_LIMIT_PROC_PATH.equals(file)) {
            buffer.putInt(CPUFeature.MSG_SET_LIMIT_CGROUP);
        } else if (CPUCTL_PROC_PATH.equals(file)) {
            buffer.putInt(CPUFeature.MSG_SET_FG_CGROUP);
        } else {
            return;
        }
        buffer.putInt(pid);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setGroup pid = ");
        stringBuilder.append(pid);
        stringBuilder.append(" processgroup = ");
        stringBuilder.append(file);
        AwareLog.i(str, stringBuilder.toString());
        if (this.mCPUFeatureInstance != null) {
            this.mCPUFeatureInstance.sendPacket(buffer);
        }
    }

    private boolean isLimitProc(int pid) {
        StringBuilder path = new StringBuilder();
        path.append(PTAH_PROC_INFO);
        path.append(pid);
        path.append(PATH_COMM);
        File file = new File(path.toString());
        String procname;
        if (file.exists() && file.canRead()) {
            String procname2 = null;
            try {
                procname2 = FileUtils.readTextFile(file, 0, null);
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isLimitProc IOException + ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            }
            if (procname2 == null) {
                return false;
            }
            procname = procname2.trim();
            if (!"com.android.systemui".contains(procname)) {
                return true;
            }
            procname2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("procname:");
            stringBuilder2.append(procname);
            stringBuilder2.append("is limit");
            AwareLog.d(procname2, stringBuilder2.toString());
            return false;
        }
        procname = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("isLimitProc file not exists or canot read!");
        stringBuilder3.append(path.toString());
        AwareLog.e(procname, stringBuilder3.toString());
        return false;
    }

    private boolean isFgProc(int pid) {
        StringBuilder targetPath = new StringBuilder();
        targetPath.append(PTAH_PROC_INFO);
        targetPath.append(pid);
        targetPath.append(PTAH_CGROUP);
        return isFgThirdPartProc(targetPath.toString());
    }

    private boolean isThirdPartyUid(String line) {
        if (line == null || !line.contains(CGROUP_CPUACCT)) {
            return false;
        }
        boolean flag = false;
        int indexOf = line.indexOf(UID_PTAH);
        int index = indexOf;
        if (indexOf != -1) {
            indexOf = line.indexOf(47, index);
            String strUid = null;
            if (indexOf != -1) {
                strUid = line.substring(UID_PTAH.length() + index, indexOf);
            }
            if (UserHandle.getAppId(parseInt(strUid)) > 10000) {
                flag = true;
            }
        }
        return flag;
    }

    private boolean isFgThirdPartProc(String path) {
        StringBuilder stringBuilder;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        String line;
        try {
            fis = new FileInputStream(path);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);
            line = "";
            boolean flag = false;
            while (true) {
                String readLine = br.readLine();
                line = readLine;
                if (readLine == null) {
                    break;
                }
                if (line.contains(CGROUP_CPUSET)) {
                    if (line.contains("background")) {
                        FileContent.closeBufferedReader(br);
                        FileContent.closeInputStreamReader(isr);
                        FileContent.closeFileInputStream(fis);
                        return false;
                    }
                    flag = true;
                }
                if (flag) {
                    if (isThirdPartyUid(line)) {
                        FileContent.closeBufferedReader(br);
                        FileContent.closeInputStreamReader(isr);
                        FileContent.closeFileInputStream(fis);
                        return true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            line = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(line, stringBuilder.toString());
        } catch (FileNotFoundException e2) {
            line = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("FileNotFoundException ");
            stringBuilder.append(e2.getMessage());
            AwareLog.e(line, stringBuilder.toString());
        } catch (UnsupportedEncodingException e3) {
            line = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("UnsupportedEncodingException ");
            stringBuilder.append(e3.getMessage());
            AwareLog.e(line, stringBuilder.toString());
        } catch (IOException e4) {
            line = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IOException ");
            stringBuilder.append(e4.getMessage());
            AwareLog.e(line, stringBuilder.toString());
        } catch (Throwable th) {
            FileContent.closeBufferedReader(br);
            FileContent.closeInputStreamReader(isr);
            FileContent.closeFileInputStream(fis);
        }
        FileContent.closeBufferedReader(br);
        FileContent.closeInputStreamReader(isr);
        FileContent.closeFileInputStream(fis);
        return false;
    }

    private long getCpuLoad() {
        String strContent = getContentWithOneLine(CPU_STAT_PATH, "cpu ");
        if (strContent == null) {
            AwareLog.e(TAG, "getCpuLoad null content!");
            return -1;
        }
        ProcessStatInfo info = getCpuStatInfo(strContent);
        if (info == null) {
            return -1;
        }
        return info.getWallTime();
    }

    private ProcessStatInfo getCpuStatInfo() {
        String strContent = getContentWithOneLine(CPU_STAT_PATH, "cpu ");
        if (strContent != null) {
            return getCpuStatInfo(strContent);
        }
        AwareLog.e(TAG, "getProcessLoad null content!");
        return null;
    }

    private List<String> getProcStatInfo(String content) {
        if (content == null || content.length() == 0) {
            AwareLog.e(TAG, "getWallTime null content!");
            return null;
        }
        String[] conts = content.split(" ");
        List<String> listTemp = new ArrayList();
        for (String str : conts) {
            if (!"".equals(str)) {
                listTemp.add(str);
            }
        }
        return listTemp;
    }

    private ProcessStatInfo getCpuStatInfo(String content) {
        ProcessStatInfo processStatInfo;
        List<String> listTemp = getProcStatInfo(content);
        List<String> list;
        if (listTemp == null) {
            processStatInfo = null;
        } else if (listTemp.size() < 10) {
            list = listTemp;
            processStatInfo = null;
        } else {
            String strUserTime = (String) listTemp.get(1);
            String strNice = (String) listTemp.get(2);
            String strSystem = (String) listTemp.get(3);
            String strIdle = (String) listTemp.get(4);
            String strIoWait = (String) listTemp.get(5);
            String strIrq = (String) listTemp.get(6);
            String strSoftIrq = (String) listTemp.get(7);
            long userTime = parseLong(strUserTime);
            long nice = parseLong(strNice);
            long system = parseLong(strSystem);
            long idle = parseLong(strIdle);
            long iowait = parseLong(strIoWait);
            long irq = parseLong(strIrq);
            long softIrq = parseLong(strSoftIrq);
            long j;
            if (userTime == -1 || nice == -1 || system == -1 || idle == -1 || iowait == -1 || irq == -1) {
                j = idle;
                listTemp = irq;
            } else if (softIrq == -1) {
                list = listTemp;
                j = idle;
                listTemp = irq;
            } else {
                processStatInfo = new ProcessStatInfo();
                processStatInfo.idle = idle;
                processStatInfo.iowait = iowait;
                processStatInfo.irq = irq;
                processStatInfo.nice = nice;
                processStatInfo.softIrq = softIrq;
                processStatInfo.system = system;
                processStatInfo.userTime = userTime;
                return processStatInfo;
            }
            AwareLog.e(TAG, "getWallTime inalid value!");
            return null;
        }
        return processStatInfo;
    }

    private long parseLong(String str) {
        long value = -1;
        if (str == null || str.length() == 0) {
            return -1;
        }
        try {
            value = Long.parseLong(str);
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseLong NumberFormatException e = ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str2, stringBuilder.toString());
        }
        return value;
    }

    private int parseInt(String str) {
        int value = -1;
        if (str == null || str.length() == 0) {
            return -1;
        }
        try {
            value = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseInt NumberFormatException e = ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str2, stringBuilder.toString());
        }
        return value;
    }

    private long getProcessLoad(int pid) {
        StringBuilder targetPath = new StringBuilder();
        targetPath.append(PTAH_PROC_INFO);
        targetPath.append(pid);
        targetPath.append(PATH_STAT);
        String content = getContentWithOneLine(targetPath.toString(), String.valueOf(pid));
        if (content != null) {
            return getPorcseeTime(content);
        }
        AwareLog.e(TAG, "getProcessLoad null content!");
        return -1;
    }

    private long getPorcseeTime(String content) {
        String[] conts = content.split(" ");
        String str;
        if (conts.length < 15) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getIdleTime content inalid = ");
            stringBuilder.append(content);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
        str = conts[13];
        String strSTime = conts[14];
        long utime = parseLong(str);
        long stime = parseLong(strSTime);
        if (utime != -1 && stime != -1) {
            return utime + stime;
        }
        AwareLog.e(TAG, "getPorcseeTime inalid value!");
        return -1;
    }

    private String getContentWithOneLine(String filePath, String keyword) {
        String str;
        StringBuilder stringBuilder;
        String line = null;
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(filePath);
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);
                do {
                    String readLine = br.readLine();
                    line = readLine;
                    if (readLine == null || keyword == null) {
                        break;
                    }
                } while (!line.contains(keyword));
            } catch (FileNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("FileNotFoundException ");
                stringBuilder.append(e.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            } catch (UnsupportedEncodingException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("UnsupportedEncodingException ");
                stringBuilder.append(e2.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            } catch (IOException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IOException ");
                stringBuilder.append(e3.getMessage());
                AwareLog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                FileContent.closeBufferedReader(null);
                FileContent.closeInputStreamReader(null);
                FileContent.closeFileInputStream(null);
            }
            FileContent.closeBufferedReader(br);
            FileContent.closeInputStreamReader(isr);
            FileContent.closeFileInputStream(fis);
            return line;
        }
        AwareLog.e(TAG, "getContentWithOneLine file not exists or canot read!");
        return null;
    }

    private void sendMessageForLoadDetector() {
        this.mLoadDetectorHandler.removeMessages(1);
        this.mLoadDetectorHandler.sendEmptyMessageDelayed(1, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
    }
}
