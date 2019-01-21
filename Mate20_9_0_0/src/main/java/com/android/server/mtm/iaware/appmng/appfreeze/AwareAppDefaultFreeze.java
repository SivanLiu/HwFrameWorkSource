package com.android.server.mtm.iaware.appmng.appfreeze;

import android.app.mtm.iaware.appmng.AppMngConstant.AppFreezeSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.Context;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.os.KernelUidCpuTimeReader;
import com.android.internal.os.KernelUidCpuTimeReader.Callback;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort.ClassRate;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appclean.CleanSource;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.SystemUnremoveUidCache;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import com.huawei.pgmng.plug.AppInfo;
import com.huawei.pgmng.plug.PGSdk;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AwareAppDefaultFreeze extends CleanSource {
    private static int DEFAULT_CAPACITY = 10;
    private static final String FREEZE_BAD = "app_freeze_bad";
    private static final long INTERVAL_TIME = 60000;
    private static final int MAX_PID_CNT = 20;
    private static final String REASON_DUMP_FREEZE = "Only for dump test";
    private static final String TAG = "mtm.AwareAppDefaultFreeze";
    private static AwareAppDefaultFreeze mAwareAppDefaultFreeze = null;
    private static List<CleanType> mPriority;
    private Context mContext;
    private ArrayList<AppInfo> mFreezeAppInfos = new ArrayList();
    private final KernelUidCpuTimeReader mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
    private long mLastSampleTime = SystemClock.uptimeMillis();
    private Set<String> mSysUnremoveBadPkgs = new ArraySet();
    private SystemUnremoveUidCache mSystemUnremoveUidCache;
    private UidCpuTimeReaderCallback mUidCpuTimeReaderCallback = new UidCpuTimeReaderCallback();
    private UidLoadComparator mUidLoadComparator = new UidLoadComparator();
    private SparseArray<UidLoadPair> mUidLoadMap = new SparseArray();

    private static class ProcessInfoComparator implements Comparator<ProcessInfo>, Serializable {
        private static final long serialVersionUID = 1;

        private ProcessInfoComparator() {
        }

        public int compare(ProcessInfo arg0, ProcessInfo arg1) {
            int i = -1;
            if (arg0 == null) {
                return arg1 == null ? 0 : -1;
            } else {
                if (arg1 == null) {
                    return 1;
                }
                if (arg0.mUid >= arg1.mUid) {
                    i = arg0.mUid == arg1.mUid ? 0 : 1;
                }
                return i;
            }
        }
    }

    private class UidCpuTimeReaderCallback implements Callback {
        private UidCpuTimeReaderCallback() {
        }

        public void onUidCpuTime(int uid, long userTimeUs, long systemTimeUs) {
            UidLoadPair pair = (UidLoadPair) AwareAppDefaultFreeze.this.mUidLoadMap.get(uid);
            if (pair != null) {
                pair.runningTime = userTimeUs + systemTimeUs;
            }
        }
    }

    private static class UidLoadComparator implements Comparator<UidLoadPair>, Serializable {
        private static final long serialVersionUID = 1;

        private UidLoadComparator() {
        }

        public int compare(UidLoadPair lhs, UidLoadPair rhs) {
            return Long.compare(rhs.runningTime, lhs.runningTime);
        }
    }

    private static class UidLoadPair {
        public AppInfo info;
        public long runningTime = 0;

        public UidLoadPair(AppInfo info) {
            this.info = info;
        }
    }

    public static synchronized AwareAppDefaultFreeze getInstance() {
        AwareAppDefaultFreeze awareAppDefaultFreeze;
        synchronized (AwareAppDefaultFreeze.class) {
            if (mAwareAppDefaultFreeze == null) {
                mAwareAppDefaultFreeze = new AwareAppDefaultFreeze();
                setPriority();
            }
            awareAppDefaultFreeze = mAwareAppDefaultFreeze;
        }
        return awareAppDefaultFreeze;
    }

    private AwareAppDefaultFreeze() {
    }

    public void init(Context ctx) {
        this.mContext = ctx;
        this.mSystemUnremoveUidCache = SystemUnremoveUidCache.getInstance(ctx);
        initSystemUnremoveBadApp();
    }

    public void deInitDefaultFree() {
        this.mContext = null;
        this.mSysUnremoveBadPkgs.clear();
    }

    private void initSystemUnremoveBadApp() {
        ArrayList<String> badList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_FREEZE.getDesc(), FREEZE_BAD);
        if (badList != null) {
            this.mSysUnremoveBadPkgs.clear();
            this.mSysUnremoveBadPkgs.addAll(badList);
        }
    }

    public void doFrozen(String pkgName, AppFreezeSource config, int duration, String reason) {
        Throwable th;
        String str = pkgName;
        if (str == null) {
            AwareLog.w(TAG, "The input params is invalid");
            return;
        }
        List<AwareProcessBlockInfo> awareProcessBlockInfos = getAllFreezeApp(config);
        int i;
        String str2;
        if (awareProcessBlockInfos == null || awareProcessBlockInfos.isEmpty()) {
            i = duration;
            str2 = reason;
            AwareLog.w(TAG, "no pid need to freeze");
            return;
        }
        int pidCnt = 0;
        this.mUidLoadMap.clear();
        synchronized (this.mFreezeAppInfos) {
            try {
                this.mFreezeAppInfos.clear();
                StringBuffer sb = new StringBuffer();
                for (AwareProcessBlockInfo processInfo : awareProcessBlockInfos) {
                    if (processInfo.mCleanType == CleanType.FREEZE_NOMAL || processInfo.mCleanType == CleanType.FREEZE_UP_DOWNLOAD) {
                        if (str.equals(processInfo.mPackageName) && UserHandle.getUserId(processInfo.mUid) == AwareAppAssociate.getInstance().getCurUserId()) {
                            str = pkgName;
                        } else {
                            List<AwareProcessInfo> infos = processInfo.mProcessList;
                            if (infos != null) {
                                if (infos.size() != 0) {
                                    AppInfo appInfo = new AppInfo(processInfo.mUid, processInfo.mPackageName);
                                    List<Integer> pids = new ArrayList(infos.size());
                                    Iterator iterator = infos.iterator();
                                    while (iterator.hasNext()) {
                                        AwareProcessInfo info = (AwareProcessInfo) iterator.next();
                                        if (info.mProcInfo == null) {
                                            iterator.remove();
                                        } else {
                                            int pid = info.mPid;
                                            if (checkPidValid(pid, info.mProcInfo.mUid)) {
                                                pids.add(Integer.valueOf(pid));
                                                sb.append(pid);
                                                sb.append(',');
                                            } else {
                                                iterator.remove();
                                            }
                                        }
                                        str = pkgName;
                                    }
                                    pidCnt += infos.size();
                                    appInfo.setPids(pids);
                                    this.mUidLoadMap.put(processInfo.mUid, new UidLoadPair(appInfo));
                                    if (processInfo.mCleanType == CleanType.FREEZE_UP_DOWNLOAD) {
                                        this.mFreezeAppInfos.add(0, appInfo);
                                    } else {
                                        this.mFreezeAppInfos.add(appInfo);
                                    }
                                }
                            }
                            str = pkgName;
                        }
                    }
                    str = pkgName;
                }
                sortFreezeAppByCPULoad(pidCnt);
                doFrozenInternel(duration, reason, sb);
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void doFrozenInternel(int duration, String reason, StringBuffer sb) {
        if (this.mFreezeAppInfos.isEmpty()) {
            AwareLog.i(TAG, "no pid need to freeze");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            PGSdk pgInstance = PGSdk.getInstance();
            if (pgInstance != null) {
                pgInstance.fastHibernation(this.mContext, this.mFreezeAppInfos, duration, reason);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Fast freeze the pid=[");
                stringBuilder.append(sb.toString());
                stringBuilder.append("]");
                AwareLog.i(str, stringBuilder.toString());
            }
        } catch (RemoteException re) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("do Frozen failed because not find the PG");
            stringBuilder.append(re.getMessage());
            AwareLog.w(str, stringBuilder.toString());
        }
    }

    private List<AwareProcessBlockInfo> getAllFreezeApp(AppFreezeSource config) {
        List<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.size() == 0) {
            AwareLog.d(TAG, "no pid exit in mtm");
            return null;
        }
        List<AwareProcessInfo> awareProcList = filterUnRemoveSystemApp(procList);
        if (awareProcList.size() == 0) {
            AwareLog.d(TAG, "no pids exit in mtm without system and system unremove app");
            return null;
        }
        List<AwareProcessBlockInfo> awareProcessBlockInfos = DecisionMaker.getInstance().decideAll(awareProcList, getLevel(), AppMngFeature.APP_FREEZE, config);
        if (awareProcessBlockInfos != null && awareProcessBlockInfos.size() != 0) {
            return CleanSource.mergeBlock(awareProcessBlockInfos, mPriority);
        }
        AwareLog.w(TAG, "no pid need to freeze");
        return null;
    }

    private static void setPriority() {
        mPriority = new ArrayList();
        mPriority.add(CleanType.FREEZE_NOMAL);
        mPriority.add(CleanType.FREEZE_UP_DOWNLOAD);
        mPriority.add(CleanType.NONE);
    }

    private List<AwareProcessInfo> filterUnRemoveSystemApp(List<ProcessInfo> infos) {
        List<AwareProcessInfo> awareProcList = new ArrayList(DEFAULT_CAPACITY);
        if (infos == null || infos.size() == 0) {
            return awareProcList;
        }
        int size = infos.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo procInfo = (ProcessInfo) infos.get(i);
            if (isSystemUnRemoveBadPkg(procInfo) || !isSystemUnRemoveApp(procInfo.mAppUid % LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS)) {
                awareProcList.add(new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo));
            }
        }
        return awareProcList;
    }

    private boolean isSystemUnRemoveBadPkg(ProcessInfo procInfo) {
        if (!(this.mSysUnremoveBadPkgs.isEmpty() || procInfo.mPackageName == null || procInfo.mPackageName.isEmpty())) {
            Iterator it = procInfo.mPackageName.iterator();
            while (it.hasNext()) {
                if (this.mSysUnremoveBadPkgs.contains((String) it.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSystemUnRemoveApp(int uid) {
        if (uid > 0 && uid < 10000) {
            return true;
        }
        if (this.mSystemUnremoveUidCache == null || !this.mSystemUnremoveUidCache.checkUidExist(uid)) {
            return false;
        }
        return true;
    }

    private int getLevel() {
        return 0;
    }

    private List<AppInfo> getAppInfoByPkg(String pkg) {
        List<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfosFromPackage(pkg, UserHandle.myUserId());
        if (procList.isEmpty()) {
            AwareLog.d(TAG, "no pid exit in mtm");
            return null;
        }
        Collections.sort(procList, new ProcessInfoComparator());
        List<AppInfo> apps = new ArrayList();
        AppInfo lastApp = null;
        for (ProcessInfo info : procList) {
            AppInfo appInfo = new AppInfo(info.mUid, pkg);
            List<Integer> pids = new ArrayList();
            pids.add(Integer.valueOf(info.mPid));
            appInfo.setPids(pids);
            if (lastApp == null) {
                lastApp = appInfo;
                apps.add(lastApp);
            } else if (lastApp.getUid() != info.mUid) {
                lastApp = appInfo;
                apps.add(lastApp);
            } else if (lastApp.getPids() != null) {
                lastApp.getPids().add(Integer.valueOf(info.mPid));
            } else {
                lastApp.setPids(pids);
            }
        }
        return apps;
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            Iterator it;
            pw.println("Lasted freeze app");
            synchronized (this.mFreezeAppInfos) {
                if (!this.mFreezeAppInfos.isEmpty()) {
                    it = this.mFreezeAppInfos.iterator();
                    while (it.hasNext()) {
                        AppInfo app = (AppInfo) it.next();
                        StringBuffer sb = new StringBuffer();
                        sb.append("    UID=");
                        sb.append(app.getUid());
                        sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        sb.append("PID=");
                        if (app.getPids() != null) {
                            for (Integer pid : app.getPids()) {
                                sb.append(pid.intValue());
                                sb.append(",");
                            }
                        }
                        sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        sb.append("PKG=");
                        sb.append(app.getPkg());
                        pw.println(sb.toString());
                    }
                }
            }
            pw.println();
            pw.println("Current can freeze app by default policy");
            List<AwareProcessBlockInfo> awareProcessBlockInfos = getAllFreezeApp(AppFreezeSource.FAST_FREEZE);
            if (awareProcessBlockInfos == null || awareProcessBlockInfos.isEmpty()) {
                pw.println("no pid need to freeze");
                return;
            }
            StringBuilder stringBuilder;
            for (AwareProcessBlockInfo processInfo : awareProcessBlockInfos) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(processInfo.toString());
                pw.println(stringBuilder.toString());
            }
            pw.println();
            pw.println("Current can freeze app by camera policy");
            awareProcessBlockInfos = getAllFreezeApp(AppFreezeSource.CAMERA_FREEZE);
            if (awareProcessBlockInfos == null || awareProcessBlockInfos.isEmpty()) {
                pw.println("no pid need to freeze");
                return;
            }
            for (AwareProcessBlockInfo processInfo2 : awareProcessBlockInfos) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(processInfo2.toString());
                pw.println(stringBuilder.toString());
            }
        }
    }

    public void dumpFreezeApp(PrintWriter pw, String pkg, int time) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Freeze current ");
            stringBuilder.append(pkg);
            stringBuilder.append(" app");
            pw.println(stringBuilder.toString());
            List<AppInfo> apps = getAppInfoByPkg(pkg);
            if (!(apps == null || apps.isEmpty())) {
                for (AppInfo app : apps) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("    UID=");
                    sb.append(app.getUid());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("PID=");
                    if (app.getPids() != null) {
                        for (Integer pid : app.getPids()) {
                            sb.append(pid.intValue());
                            sb.append(",");
                        }
                    }
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("PKG=");
                    sb.append(app.getPkg());
                    pw.println(sb.toString());
                }
                try {
                    PGSdk.getInstance().fastHibernation(this.mContext, apps, time, REASON_DUMP_FREEZE);
                } catch (RemoteException re) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("do UnFrozen failed because not find the PG");
                    stringBuilder2.append(re.getMessage());
                    AwareLog.w(str, stringBuilder2.toString());
                }
            }
        }
    }

    private void sortFreezeAppByCPULoad(int pidCnt) {
        if (pidCnt < 20 || this.mUidLoadMap.size() == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sortFreezeAppByCPULoad pidCnt =  ");
            stringBuilder.append(pidCnt);
            stringBuilder.append(" no need sort!");
            AwareLog.d(str, stringBuilder.toString());
            return;
        }
        int uidSize = this.mUidLoadMap.size();
        long nowTime = SystemClock.uptimeMillis();
        long intervalTime = nowTime - this.mLastSampleTime;
        this.mLastSampleTime = nowTime;
        getCpuTimeForUidList();
        if (intervalTime <= 60000) {
            UidLoadPair uidPair;
            List<UidLoadPair> uidLoadArray = new ArrayList();
            int i = 0;
            for (int i2 = 0; i2 < uidSize; i2++) {
                uidPair = (UidLoadPair) this.mUidLoadMap.get(this.mUidLoadMap.keyAt(i2));
                if (uidPair != null) {
                    uidLoadArray.add(uidPair);
                }
            }
            if (!uidLoadArray.isEmpty()) {
                Collections.sort(uidLoadArray, this.mUidLoadComparator);
                synchronized (this.mFreezeAppInfos) {
                    this.mFreezeAppInfos.clear();
                    int size = uidLoadArray.size();
                    while (i < size) {
                        uidPair = (UidLoadPair) uidLoadArray.get(i);
                        if (uidPair != null) {
                            this.mFreezeAppInfos.add(uidPair.info);
                        }
                        i++;
                    }
                }
            }
        }
    }

    private void getCpuTimeForUidList() {
        this.mKernelUidCpuTimeReader.readDelta(this.mUidCpuTimeReaderCallback);
    }

    private boolean checkPidValid(int pid, int uid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/acct/uid_");
        stringBuilder.append(uid);
        stringBuilder.append("/pid_");
        stringBuilder.append(pid);
        if (!new File(stringBuilder.toString()).exists()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("/proc/");
            stringBuilder.append(pid);
            stringBuilder.append("/status/");
            String str;
            StringBuilder stringBuilder2;
            try {
                int realUid = ((Integer) Files.getAttribute(Paths.get(stringBuilder.toString(), new String[0]), "unix:uid", new LinkOption[0])).intValue();
                if (realUid != uid) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("read uid ");
                    stringBuilder2.append(realUid);
                    stringBuilder2.append(" of ");
                    stringBuilder2.append(pid);
                    stringBuilder2.append(" is not match");
                    AwareLog.w(str, stringBuilder2.toString());
                    return false;
                }
            } catch (IOException e) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("read status of ");
                stringBuilder2.append(pid);
                stringBuilder2.append(" failed");
                AwareLog.w(str, stringBuilder2.toString());
                return false;
            }
        }
        return true;
    }

    public void dumpFreezeBadPid(PrintWriter pw, int pid, int uid) {
        if (pw != null && pid >= 0 && uid >= 0) {
            StringBuilder stringBuilder;
            if (checkPidValid(pid, uid)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" match uid ");
                stringBuilder.append(uid);
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" not match uid ");
                stringBuilder.append(uid);
                pw.println(stringBuilder.toString());
            }
        }
    }
}
