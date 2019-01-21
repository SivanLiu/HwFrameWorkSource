package com.android.server.mtm.taskstatus;

import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.wm.HwStartWindowRecord;
import com.huawei.android.app.HwActivityManager;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class ProcessInfoCollector {
    private static final int CAPACITY = 10;
    private static final int MAX_TASKS = 100;
    private static final String TAG = "ProcessInfoCollector";
    private static ProcessInfoCollector mProcessInfoCollector = null;
    boolean DEBUG = false;
    boolean INFO = false;
    private HashMap<Integer, AwareProcessInfo> mAwareProcMap = new HashMap();
    private ProcessInfo mCacheInfo = new ProcessInfo(0, 0);
    private HwActivityManagerService mHwAMS = null;
    private AwareBroadcastPolicy mIawareBrPolicy = null;
    private int mIndex = 0;
    private ProcessInfo[] mKilledProcList = new ProcessInfo[10];
    private HashMap<Integer, ProcessInfo> mProcMap = new HashMap();

    private ProcessInfoCollector() {
        init();
    }

    public static synchronized ProcessInfoCollector getInstance() {
        ProcessInfoCollector processInfoCollector;
        synchronized (ProcessInfoCollector.class) {
            if (mProcessInfoCollector == null) {
                mProcessInfoCollector = new ProcessInfoCollector();
            }
            processInfoCollector = mProcessInfoCollector;
        }
        return processInfoCollector;
    }

    private void init() {
        this.mHwAMS = HwActivityManagerService.self();
        if (this.mHwAMS == null) {
            Slog.e(TAG, "init failed to get HwAMS handler");
        }
    }

    public synchronized void recordKilledProcess(ProcessInfo info) {
        info.mKilledTime = SystemClock.uptimeMillis();
        if (this.mIndex >= 10) {
            this.mIndex = 0;
        }
        this.mKilledProcList[this.mIndex] = info;
        this.mIndex++;
    }

    public void removeKilledProcess(int pid) {
        ProcessInfo info;
        if (pid < 0) {
            Slog.e(TAG, "removeKilledProcess: proc should not less than zero ");
        }
        synchronized (this.mProcMap) {
            info = (ProcessInfo) this.mProcMap.remove(Integer.valueOf(pid));
            this.mAwareProcMap.remove(Integer.valueOf(pid));
        }
        if (info != null && info.mProcessName != null && info.mPackageName != null && !info.mPackageName.isEmpty() && info.mProcessName.equals(info.mPackageName.get(0))) {
            HwStartWindowRecord.getInstance().removeStartWindowApp(Integer.valueOf(info.mAppUid));
        }
    }

    public ProcessInfo getProcessInfo(int pid) {
        if (pid < 0) {
            Slog.e(TAG, "getProcessInfo: proc should not less than zero ");
            return null;
        }
        ProcessInfo copyInfo = null;
        synchronized (this.mProcMap) {
            ProcessInfo info = (ProcessInfo) this.mProcMap.get(Integer.valueOf(pid));
            if (info == null) {
                Slog.e(TAG, "getProcessInfo: failed to find this proc ");
            } else {
                copyInfo = new ProcessInfo(0, 0);
                ProcessInfo.copyProcessInfo(info, copyInfo);
            }
        }
        return copyInfo;
    }

    public AwareProcessInfo getAwareProcessInfo(int pid) {
        if (pid < 0) {
            Slog.e(TAG, "getProcessInfo: proc should not less than zero ");
            return null;
        }
        AwareProcessInfo info;
        synchronized (this.mProcMap) {
            info = (AwareProcessInfo) this.mAwareProcMap.get(Integer.valueOf(pid));
        }
        return info;
    }

    public ArrayList<ProcessInfo> getProcessInfosFromPackage(String packageName, int userId) {
        ArrayList<ProcessInfo> procList = new ArrayList();
        if (userId < 0 || packageName == null) {
            return procList;
        }
        synchronized (this.mProcMap) {
            for (Entry entry : this.mProcMap.entrySet()) {
                ProcessInfo info = (ProcessInfo) entry.getValue();
                if (info != null) {
                    ArrayList<String> packageNames = info.mPackageName;
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            if (packageName.equals((String) packageNames.get(0))) {
                                if (userId == UserHandle.getUserId(info.mUid)) {
                                    ProcessInfo copyInfo = new ProcessInfo(0, 0);
                                    ProcessInfo.copyProcessInfo(info, copyInfo);
                                    procList.add(copyInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
        return procList;
    }

    public ArrayList<AwareProcessInfo> getAwareProcessInfosFromPackage(String packageName, int userId) {
        ArrayList<AwareProcessInfo> procList = new ArrayList();
        if (packageName == null) {
            return procList;
        }
        synchronized (this.mProcMap) {
            for (Entry entry : this.mAwareProcMap.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) entry.getValue();
                if (info != null) {
                    ArrayList<String> packageNames = info.mProcInfo.mPackageName;
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            if (packageName.equals((String) packageNames.get(0))) {
                                if (userId == -1 || userId == UserHandle.getUserId(info.mProcInfo.mUid)) {
                                    procList.add(info);
                                }
                            }
                        }
                    }
                }
            }
        }
        return procList;
    }

    public ArrayList<ProcessInfo> getProcessInfosFromPackageMap(ArrayMap<String, Integer> packMap) {
        ArrayList<ProcessInfo> procList = new ArrayList();
        if (packMap == null) {
            return procList;
        }
        synchronized (this.mProcMap) {
            for (Entry entry : this.mProcMap.entrySet()) {
                ProcessInfo info = (ProcessInfo) entry.getValue();
                if (info != null) {
                    ArrayList<String> packageNames = info.mPackageName;
                    if (packageNames != null) {
                        if (!packageNames.isEmpty()) {
                            String procInfoPackageName = (String) packageNames.get(0);
                            if (packMap.get(procInfoPackageName) != null) {
                                if (((Integer) packMap.get(procInfoPackageName)).intValue() == UserHandle.getUserId(info.mUid)) {
                                    ProcessInfo copyInfo = new ProcessInfo(0, 0);
                                    ProcessInfo.copyProcessInfo(info, copyInfo);
                                    procList.add(copyInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
        return procList;
    }

    public void dumpPackageTask(ArrayList<ProcessInfo> processInfos, PrintWriter pw) {
        if (pw == null) {
            Slog.e(TAG, "dump PrintWriter pw is null");
        } else if (processInfos != null && !processInfos.isEmpty()) {
            pw.println("  Package/Task Processes Information dump :");
            Iterator it = processInfos.iterator();
            while (it.hasNext()) {
                ProcessInfo procinfo = (ProcessInfo) it.next();
                pw.println("\r\n  Running Process information :");
                printProcInfo(procinfo, pw);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0051  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0050 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ArrayList<ProcessInfo> getProcessInfosFromTask(int taskId, int userId) {
        ArrayList<ProcessInfo> emptyProcList = new ArrayList();
        if (userId < 0 || taskId < 0) {
            return emptyProcList;
        }
        String packageName = null;
        for (RecentTaskInfo t : this.mHwAMS.getRecentTasks(100, 2, userId).getList()) {
            if (t.persistentId == taskId) {
                Intent intent = new Intent(t.baseIntent);
                if (t.origActivity != null) {
                    intent.setComponent(t.origActivity);
                }
                if (intent.getComponent() != null) {
                    packageName = intent.getComponent().getPackageName();
                }
                if (packageName != null) {
                    return emptyProcList;
                }
                return getProcessInfosFromPackage(packageName, userId);
            }
        }
        if (packageName != null) {
        }
    }

    public ArrayList<Integer> getPidsFromUid(int uid, int userId) {
        ArrayList<Integer> pids = new ArrayList();
        if (userId < 0 || uid < 0) {
            return pids;
        }
        synchronized (this.mProcMap) {
            for (Entry entry : this.mProcMap.entrySet()) {
                ProcessInfo info = (ProcessInfo) entry.getValue();
                if (info != null) {
                    if (info.mAppUid == uid || info.mUid == uid) {
                        if (userId == UserHandle.getUserId(info.mUid)) {
                            pids.add(Integer.valueOf(info.mPid));
                        }
                    }
                }
            }
        }
        return pids;
    }

    public ArrayList<ProcessInfo> getProcessInfoList() {
        ArrayList<ProcessInfo> procList;
        synchronized (this.mProcMap) {
            procList = new ArrayList(this.mProcMap.size());
            for (Entry entry : this.mProcMap.entrySet()) {
                ProcessInfo Info = (ProcessInfo) entry.getValue();
                ProcessInfo copyInfo = new ProcessInfo(0, 0);
                ProcessInfo.copyProcessInfo(Info, copyInfo);
                procList.add(copyInfo);
            }
        }
        return procList;
    }

    public ArrayList<AwareProcessInfo> getAwareProcessInfoList() {
        ArrayList<AwareProcessInfo> procList;
        synchronized (this.mProcMap) {
            procList = new ArrayList(this.mAwareProcMap.size());
            for (Entry entry : this.mAwareProcMap.entrySet()) {
                procList.add((AwareProcessInfo) entry.getValue());
            }
        }
        return procList;
    }

    /* JADX WARNING: Missing block: B:24:0x0081, code skipped:
            if (r1 != false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:25:0x0083, code skipped:
            reportToRms(r0);
     */
    /* JADX WARNING: Missing block: B:26:0x0086, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void recordProcessInfo(int pid, int uid) {
        boolean exist = false;
        this.mCacheInfo.initialProcessInfo(pid, uid);
        if (HwActivityManager.getProcessRecordFromMTM(this.mCacheInfo)) {
            synchronized (this.mProcMap) {
                ProcessInfo curProcessInfo = (ProcessInfo) this.mProcMap.get(Integer.valueOf(pid));
                if (curProcessInfo == null) {
                    curProcessInfo = new ProcessInfo(pid, uid);
                    this.mCacheInfo.mCreatedTime = SystemClock.elapsedRealtime();
                } else {
                    this.mCacheInfo.mCreatedTime = curProcessInfo.mCreatedTime;
                    exist = true;
                }
                if (ProcessInfo.copyProcessInfo(this.mCacheInfo, curProcessInfo)) {
                    curProcessInfo.mCount++;
                    if (!exist) {
                        this.mProcMap.put(Integer.valueOf(pid), curProcessInfo);
                        AwareProcessInfo awareProcessInfo = new AwareProcessInfo(pid, curProcessInfo);
                        this.mAwareProcMap.put(Integer.valueOf(pid), awareProcessInfo);
                        if (getIawareBrPolicy() != null) {
                            this.mIawareBrPolicy.updateProcessBrPolicy(awareProcessInfo, -1);
                        }
                    }
                } else {
                    Slog.e(TAG, "recordProcessInfo  source or target object is null");
                    return;
                }
            }
        }
        if (this.DEBUG) {
            Slog.e(TAG, "recordProcessInfo  failed to get process record");
        }
    }

    public void enableDebug() {
        this.DEBUG = true;
        this.INFO = true;
    }

    public void disableDebug() {
        this.DEBUG = false;
        this.INFO = false;
    }

    public void dump(PrintWriter pw) {
        if (pw == null) {
            Slog.e(TAG, "dump PrintWriter pw is null");
            return;
        }
        pw.println("  Process Information Collector dump :");
        synchronized (this.mProcMap) {
            for (Entry entry : this.mProcMap.entrySet()) {
                ProcessInfo Info = (ProcessInfo) entry.getValue();
                pw.println("\r\n  Running Process information :");
                printProcInfo(Info, pw);
            }
        }
        synchronized (this) {
            for (int i = 0; i < this.mIndex; i++) {
                ProcessInfo Info2 = this.mKilledProcList[i];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\r\n  Killed Process information ");
                stringBuilder.append(i);
                stringBuilder.append(":");
                pw.println(stringBuilder.toString());
                printProcInfo(Info2, pw);
            }
        }
    }

    private void reportToRms(ProcessInfo info) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC)) && info != null) {
            Bundle args = new Bundle();
            args.putInt("callPid", info.mPid);
            args.putInt("callUid", info.mUid);
            args.putString("callProcName", info.mProcessName);
            ArrayList<String> pkgs = new ArrayList();
            pkgs.addAll(info.mPackageName);
            args.putStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME, pkgs);
            args.putInt("relationType", 4);
            resManager.reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), args));
        }
    }

    private void printProcInfo(ProcessInfo Info, PrintWriter pw) {
        Date dat;
        GregorianCalendar gc;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  pid =  ");
        stringBuilder.append(Info.mPid);
        stringBuilder.append(", uid = ");
        stringBuilder.append(Info.mUid);
        stringBuilder.append(", count = ");
        stringBuilder.append(Info.mCount);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  ProcessName = ");
        stringBuilder.append(Info.mProcessName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Group = ");
        stringBuilder.append(Info.mCurSchedGroup);
        stringBuilder.append(" (-1:default, 0:backgroud, 5:top visible, 6:perceptible)");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  oom_Adj = ");
        stringBuilder.append(Info.mCurAdj);
        stringBuilder.append(", AdjType = ");
        stringBuilder.append(Info.mAdjType);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  package type = ");
        stringBuilder.append(Info.mType);
        stringBuilder.append(" (1:SYSTEM_SERVER, 2:SYSTEM_APP , 3:HW_INSTALL, 4:THIRDPARTY)");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  LRU = ");
        stringBuilder.append(Info.mLru);
        stringBuilder.append(" ( The first entry in the list is the least recently used)");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  FG Activities = ");
        stringBuilder.append(Info.mForegroundActivities);
        stringBuilder.append(", FG Services = ");
        stringBuilder.append(Info.mForegroundServices);
        stringBuilder.append(",Force FG =");
        stringBuilder.append(Info.mForceToForeground);
        pw.println(stringBuilder.toString());
        int list_size = Info.mPackageName.size();
        for (int i = 0; i < list_size; i++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  package name = ");
            stringBuilder2.append((String) Info.mPackageName.get(i));
            pw.println(stringBuilder2.toString());
        }
        if (Info.mCreatedTime > 0) {
            dat = new Date(Info.mCreatedTime);
            gc = new GregorianCalendar();
            gc.setTime(dat);
            String sb = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(gc.getTime());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("  created time = ");
            stringBuilder3.append(Info.mCreatedTime);
            stringBuilder3.append(", data:");
            stringBuilder3.append(sb);
            pw.println(stringBuilder3.toString());
        }
        if (Info.mKilledTime > 0) {
            dat = new Date(Info.mKilledTime);
            gc = new GregorianCalendar();
            gc.setTime(dat);
            String sb2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(gc.getTime());
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("  Killed time = ");
            stringBuilder4.append(Info.mKilledTime);
            stringBuilder4.append(", data:");
            stringBuilder4.append(sb2);
            pw.println(stringBuilder4.toString());
        }
    }

    /* JADX WARNING: Missing block: B:15:0x004c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setAwareProcessState(int pid, int uid, int state) {
        synchronized (this.mProcMap) {
            AwareProcessInfo info = (AwareProcessInfo) this.mAwareProcMap.get(Integer.valueOf(pid));
            if (info == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAwareProcessState: fail to set state! pid: ");
                stringBuilder.append(pid);
                stringBuilder.append(", uid:");
                stringBuilder.append(uid);
                stringBuilder.append(", state:");
                stringBuilder.append(state);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            if (state != -1) {
                info.setState(state);
            }
            if (getIawareBrPolicy() != null) {
                this.mIawareBrPolicy.updateProcessBrPolicy(info, state);
            }
        }
    }

    public void setAwareProcessStateByUid(int pid, int uid, int state) {
        synchronized (this.mProcMap) {
            for (Entry entry : this.mAwareProcMap.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) entry.getValue();
                if (info != null && info.mProcInfo.mUid == uid) {
                    if (state != -1) {
                        info.setState(state);
                    }
                    if (getIawareBrPolicy() != null) {
                        this.mIawareBrPolicy.updateProcessBrPolicy(info, state);
                    }
                }
            }
        }
    }

    public void resetAwareProcessStatePgRestart() {
        synchronized (this.mProcMap) {
            for (Entry entry : this.mAwareProcMap.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) entry.getValue();
                if (info != null && info.getState() == 1) {
                    info.setState(2);
                    if (getIawareBrPolicy() != null) {
                        this.mIawareBrPolicy.updateProcessBrPolicy(info, 2);
                    }
                }
            }
        }
    }

    private AwareBroadcastPolicy getIawareBrPolicy() {
        if (this.mIawareBrPolicy == null && MultiTaskManagerService.self() != null) {
            this.mIawareBrPolicy = MultiTaskManagerService.self().getIawareBrPolicy();
        }
        return this.mIawareBrPolicy;
    }
}
