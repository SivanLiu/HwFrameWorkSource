package com.android.server.mtm.iaware.appmng.appcpulimit;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appclean.CleanSource;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.AppStatusUtils;
import com.android.server.mtm.utils.SparseSet;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.feature.CpuLimitFeature;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwareAppDefaultCpuLimit extends CleanSource {
    private static final String BG_CGROUP_PATH = "/dev/cpuctl/bg_non_interactive/cgroup.procs";
    private static final String ROOT_CGROUP_PATH = "/dev/cpuctl/cgroup.procs";
    private static final String TAG = "AwareAppDefaultCpuLimit";
    private static AwareAppDefaultCpuLimit mAwareAppDefaultCpuLimit = null;
    private static FileOutputStream mOutBg = null;
    private static FileOutputStream mOutRoot = null;
    /* access modifiers changed from: private */
    public final Object doCpuLimiLock = new Object();
    private final AtomicBoolean isExecutorAlive = new AtomicBoolean(false);
    private final Object lock = new Object();
    /* access modifiers changed from: private */
    public AppMngConstant.AppCpuLimitSource mConfig = AppMngConstant.AppCpuLimitSource.CPULIMIT;
    private Map<Integer, Integer> mCpuLimitPids = new ArrayMap();
    private Set<Integer> mCpulimitUids = new ArraySet();
    /* access modifiers changed from: private */
    public String mCurPkgName = null;
    private boolean mIsCpuLimitOptEnable = false;
    /* access modifiers changed from: private */
    public volatile boolean mIsStart = false;
    private PackageManager mPm = null;

    private AwareAppDefaultCpuLimit() {
    }

    static synchronized AwareAppDefaultCpuLimit getInstance() {
        AwareAppDefaultCpuLimit awareAppDefaultCpuLimit;
        synchronized (AwareAppDefaultCpuLimit.class) {
            if (mAwareAppDefaultCpuLimit == null) {
                mAwareAppDefaultCpuLimit = new AwareAppDefaultCpuLimit();
            }
            awareAppDefaultCpuLimit = mAwareAppDefaultCpuLimit;
        }
        return awareAppDefaultCpuLimit;
    }

    /* access modifiers changed from: package-private */
    public void init(Context ctx) {
        if (ctx != null) {
            this.mPm = ctx.getPackageManager();
        }
    }

    /* access modifiers changed from: package-private */
    public void initCpuLimitOpt() {
        getOutBgFromPath();
        if (mOutBg == null) {
            AwareLog.w(TAG, "init mOutBg fail");
            this.mIsCpuLimitOptEnable = false;
            return;
        }
        getOutRootFromPath();
        if (mOutRoot == null) {
            this.mIsCpuLimitOptEnable = false;
            closeFileStream(mOutBg);
            AwareLog.w(TAG, "init mOutRoot fail");
            return;
        }
        removeAllPids();
        this.mIsCpuLimitOptEnable = true;
        this.mIsStart = true;
        initCpuLimitOptThread();
    }

    private void getOutBgFromPath() {
        try {
            mOutBg = new FileOutputStream(BG_CGROUP_PATH);
        } catch (FileNotFoundException e) {
            AwareLog.w(TAG, "FileNotFoundException : + getOutBgFromPath");
        }
    }

    private void getOutRootFromPath() {
        try {
            mOutRoot = new FileOutputStream(ROOT_CGROUP_PATH);
        } catch (FileNotFoundException e) {
            AwareLog.w(TAG, "FileNotFoundException : + getOutRootFromPath");
        }
    }

    /* access modifiers changed from: package-private */
    public boolean getInitResult() {
        return this.mIsCpuLimitOptEnable;
    }

    /* access modifiers changed from: package-private */
    public void deInitDefaultFree() {
        this.mPm = null;
        this.mIsStart = false;
        synchronized (this.doCpuLimiLock) {
            this.doCpuLimiLock.notifyAll();
        }
        closeAllFileOutputStream();
    }

    private void closeAllFileOutputStream() {
        if (this.mIsCpuLimitOptEnable) {
            this.mIsCpuLimitOptEnable = false;
            closeFileStream(mOutBg);
            closeFileStream(mOutRoot);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isPidLimited(int pid) {
        return this.mCpuLimitPids.containsKey(Integer.valueOf(pid));
    }

    /* access modifiers changed from: package-private */
    public void setCpuLimitOptEnable(boolean enable) {
        this.mIsCpuLimitOptEnable = enable;
    }

    private void notifyDoLimitCpuOpt(String pkgName) {
        synchronized (this.doCpuLimiLock) {
            this.mCurPkgName = pkgName;
            this.doCpuLimiLock.notifyAll();
        }
    }

    private void initCpuLimitOptThread() {
        new Thread(new Runnable() {
            /* class com.android.server.mtm.iaware.appmng.appcpulimit.AwareAppDefaultCpuLimit.AnonymousClass1 */

            public void run() {
                AwareLog.i(AwareAppDefaultCpuLimit.TAG, "init CpuLimitOpt thread");
                CleanSource.setSchedPriority();
                while (AwareAppDefaultCpuLimit.this.mIsStart) {
                    synchronized (AwareAppDefaultCpuLimit.this.doCpuLimiLock) {
                        AwareAppDefaultCpuLimit.this.doCpuLimitDetail(AwareAppDefaultCpuLimit.this.mCurPkgName, AwareAppDefaultCpuLimit.this.mConfig);
                        try {
                            AwareAppDefaultCpuLimit.this.doCpuLimiLock.wait();
                        } catch (InterruptedException e) {
                            AwareLog.w(AwareAppDefaultCpuLimit.TAG, "doCpuLimiLock wait err");
                        }
                    }
                }
            }
        }, "iaware.cpulimitopt").start();
    }

    /* access modifiers changed from: package-private */
    public void doLimitCpu(String pkgName, AppMngConstant.AppCpuLimitSource config) {
        if (this.mIsCpuLimitOptEnable) {
            notifyDoLimitCpuOpt(pkgName);
        } else if (this.isExecutorAlive.compareAndSet(false, true)) {
            new Thread(new Runnable(pkgName, config) {
                /* class com.android.server.mtm.iaware.appmng.appcpulimit.$$Lambda$AwareAppDefaultCpuLimit$SRXQoyCWsi0d60q2SbYF7LyPRd8 */
                private final /* synthetic */ String f$1;
                private final /* synthetic */ AppMngConstant.AppCpuLimitSource f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    AwareAppDefaultCpuLimit.this.lambda$doLimitCpu$0$AwareAppDefaultCpuLimit(this.f$1, this.f$2);
                }
            }, "iaware.cpulimit").start();
        }
    }

    public /* synthetic */ void lambda$doLimitCpu$0$AwareAppDefaultCpuLimit(String pkgName, AppMngConstant.AppCpuLimitSource config) {
        CleanSource.setSchedPriority();
        doCpuLimitDetail(pkgName, config);
        this.isExecutorAlive.set(false);
    }

    /* access modifiers changed from: private */
    public void doCpuLimitDetail(String pkgName, AppMngConstant.AppCpuLimitSource config) {
        if (pkgName == null) {
            AwareLog.w(TAG, "The input params is invalid");
            return;
        }
        List<AwareProcessBlockInfo> awareProcessBlockInfos = getAllCpuLimitApp(config);
        if (awareProcessBlockInfos == null || awareProcessBlockInfos.isEmpty()) {
            AwareLog.w(TAG, "no pid need to limit");
            return;
        }
        synchronized (this.lock) {
            this.mCpulimitUids.clear();
            doCpuLimitAction(pkgName, awareProcessBlockInfos);
        }
    }

    private void doCpuLimitAction(String pkgName, List<AwareProcessBlockInfo> awareProcessBlockInfos) {
        List<AwareProcessInfo> infos;
        SparseSet assocWithFGPids = getAssocPidsByUid(getUidByPackageName(pkgName));
        int curUserId = AwareAppAssociate.getInstance().getCurUserId();
        if (this.mIsCpuLimitOptEnable) {
            doCpuLimitOpt(pkgName, awareProcessBlockInfos, assocWithFGPids, curUserId);
            return;
        }
        this.mCpuLimitPids.clear();
        for (AwareProcessBlockInfo processInfo : awareProcessBlockInfos) {
            if (processInfo.procCleanType == ProcessCleaner.CleanType.CPULIMIT && ((!pkgName.equals(processInfo.procPackageName) || UserHandle.getUserId(processInfo.procUid) != curUserId) && (infos = processInfo.procProcessList) != null && !infos.isEmpty())) {
                doCpuGroup(infos, assocWithFGPids, processInfo.procPackageName);
            }
        }
        doCpuLimit();
    }

    private void doCpuLimitOpt(String pkgName, List<AwareProcessBlockInfo> awareProcessBlockInfos, SparseSet assocWithFGPids, int curUserId) {
        for (AwareProcessBlockInfo processInfo : awareProcessBlockInfos) {
            if (processInfo.procCleanType == ProcessCleaner.CleanType.CPULIMIT && (!pkgName.equals(processInfo.procPackageName) || UserHandle.getUserId(processInfo.procUid) != curUserId)) {
                doCpuLimitOptDetail(processInfo.procProcessList, assocWithFGPids);
            }
        }
        if (!this.mCpuLimitPids.isEmpty()) {
            AwareLog.i(TAG, "doCpuLimitOpt: " + this.mCpuLimitPids.toString());
        }
    }

    private void doCpuLimitOptDetail(List<AwareProcessInfo> infos, SparseSet assocWithFGPids) {
        if (infos != null) {
            try {
                if (!infos.isEmpty()) {
                    for (AwareProcessInfo info : infos) {
                        this.mCpulimitUids.add(Integer.valueOf(info.procProcInfo.mUid));
                        if (!assocWithFGPids.contains(info.procPid)) {
                            if (!this.mCpuLimitPids.containsKey(Integer.valueOf(info.procPid))) {
                                this.mCpuLimitPids.put(Integer.valueOf(info.procPid), Integer.valueOf(info.procProcInfo.mUid));
                                mOutBg.write(String.valueOf(info.procPid).getBytes(StandardCharsets.UTF_8.name()));
                            }
                        } else if (this.mCpuLimitPids.containsKey(Integer.valueOf(info.procPid))) {
                            this.mCpuLimitPids.remove(Integer.valueOf(info.procPid));
                            mOutRoot.write(String.valueOf(info.procPid).getBytes(StandardCharsets.UTF_8.name()));
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "UnsupportedEncodingException : doCpuLimitOpt");
            } catch (IOException e2) {
                AwareLog.w(TAG, "IOException : doCpuLimitOpt");
            }
        }
    }

    private void doCpuGroup(List<AwareProcessInfo> infos, SparseSet filterPids, String pkgName) {
        if (infos != null && !infos.isEmpty()) {
            for (AwareProcessInfo info : infos) {
                this.mCpulimitUids.add(Integer.valueOf(info.procProcInfo.mUid));
                if (!filterPids.contains(info.procPid)) {
                    this.mCpuLimitPids.put(Integer.valueOf(info.procPid), Integer.valueOf(info.procProcInfo.mUid));
                }
            }
        }
    }

    private void doCpuLimit() {
        if (!this.mCpuLimitPids.isEmpty()) {
            CpuLimitFeature.getInstance().setCpuLimitTaskList(this.mCpuLimitPids, true);
            AwareLog.i(TAG, "do Limit cpu pids: " + this.mCpuLimitPids.toString());
        }
    }

    private List<AwareProcessBlockInfo> getAllCpuLimitApp(AppMngConstant.AppCpuLimitSource config) {
        List<AwareProcessInfo> awareProcList = AppStatusUtils.getInstance().getAllProcNeedSort();
        if (awareProcList == null || awareProcList.isEmpty()) {
            AwareLog.w(TAG, "no pid need to app filter");
            return null;
        }
        List<AwareProcessBlockInfo> awareProcessBlockInfos = DecisionMaker.getInstance().decideAll(awareProcList, getLevel(), AppMngConstant.AppMngFeature.APP_CPULIMIT, (AppMngConstant.EnumWithDesc) config);
        if (awareProcessBlockInfos != null && awareProcessBlockInfos.size() != 0) {
            return mergeBlock(awareProcessBlockInfos);
        }
        AwareLog.w(TAG, "no pid need to do cpu limit");
        return null;
    }

    private int getUidByPackageName(String pkgName) {
        PackageManager packageManager;
        if (pkgName == null || pkgName.isEmpty() || (packageManager = this.mPm) == null) {
            return 0;
        }
        try {
            return packageManager.getApplicationInfoAsUser(pkgName, 1, AwareAppAssociate.getInstance().getCurUserId()).uid;
        } catch (PackageManager.NameNotFoundException e) {
            AwareLog.w(TAG, "get application info failed");
            return 0;
        }
    }

    private SparseSet getAssocPidsByUid(int uid) {
        SparseSet assocPids = new SparseSet();
        SparseSet pids = ProcessInfoCollector.getInstance().getPidsFromUid(uid, UserHandle.getUserId(uid));
        if (pids.isEmpty()) {
            return assocPids;
        }
        assocPids.addAll(pids);
        for (int i = pids.size() - 1; i >= 0; i--) {
            int sPid = pids.keyAt(i);
            SparseSet strongPids = new SparseSet();
            AwareAppAssociate.getInstance().getAssocListForPid(sPid, strongPids);
            if (!strongPids.isEmpty()) {
                assocPids.addAll(strongPids);
            }
        }
        return assocPids;
    }

    private SparseSet getAssocPidsByPid(int pid) {
        SparseSet assocPids = new SparseSet();
        assocPids.add(pid);
        SparseSet strongPids = new SparseSet();
        AwareAppAssociate.getInstance().getAssocListForPid(pid, strongPids);
        if (!strongPids.isEmpty()) {
            assocPids.addAll(strongPids);
        }
        return assocPids;
    }

    /* access modifiers changed from: package-private */
    public void doUnLimitCPU() {
        if (this.mIsCpuLimitOptEnable) {
            doUnLimitCPUOpt();
            return;
        }
        CpuLimitFeature.getInstance().setCpuLimitTaskList(null, true);
        AwareLog.i(TAG, "do CPU UnLimit ok");
    }

    private void doUnLimitCPUOpt() {
        AwareLog.i(TAG, "do UnCpuLimitOpt ok");
        synchronized (this.lock) {
            this.mCpuLimitPids.clear();
        }
        CpuLimitFeature.getInstance().setCpuLimitTaskList(null, true);
    }

    private static void removeAllPids() {
        FileInputStream input = null;
        InputStreamReader inputReader = null;
        BufferedReader br = null;
        if (mOutRoot != null) {
            try {
                input = new FileInputStream(BG_CGROUP_PATH);
                inputReader = new InputStreamReader(input, "UTF-8");
                br = new BufferedReader(inputReader);
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    mOutRoot.write(line.trim().getBytes(StandardCharsets.UTF_8.name()));
                }
            } catch (FileNotFoundException e) {
                AwareLog.e(TAG, "FileNotFoundException " + e.getMessage());
            } catch (UnsupportedEncodingException e2) {
                AwareLog.e(TAG, "UnsupportedEncodingException " + e2.getMessage());
            } catch (IOException e3) {
                AwareLog.e(TAG, "IOException " + e3.getMessage());
            } catch (Throwable th) {
                closeFileStream(null);
                closeFileStream(null);
                closeFileStream(null);
                throw th;
            }
            closeFileStream(br);
            closeFileStream(inputReader);
            closeFileStream(input);
        }
    }

    private static void closeFileStream(Closeable fs) {
        if (fs != null) {
            try {
                fs.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeFileStream exception");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void doRemoveCpuPids(int uid, int pid, boolean flag) {
        SparseSet cpuLimitPids;
        synchronized (this.lock) {
            if (this.mCpulimitUids.contains(Integer.valueOf(uid))) {
                if (pid <= 0 || this.mCpuLimitPids.containsKey(Integer.valueOf(pid))) {
                    if (pid == 0) {
                        cpuLimitPids = getAssocPidsByUid(uid);
                        this.mCpulimitUids.remove(Integer.valueOf(uid));
                    } else {
                        cpuLimitPids = getAssocPidsByPid(pid);
                    }
                    if (cpuLimitPids != null && !cpuLimitPids.isEmpty()) {
                        if (this.mIsCpuLimitOptEnable) {
                            doRemoveCpuPidsOpt(cpuLimitPids, flag);
                            return;
                        }
                        CpuLimitFeature.getInstance().removeCpuLimitTaskList(cpuLimitPids);
                        AwareLog.i(TAG, "doRemoveCpuPids: " + cpuLimitPids.toString());
                    }
                }
            }
        }
    }

    private void doRemoveCpuPidsOpt(SparseSet cpuLimitPids, boolean flag) {
        int size = cpuLimitPids.size();
        int i = 0;
        while (i < size) {
            try {
                int sPid = cpuLimitPids.keyAt(i);
                this.mCpuLimitPids.remove(Integer.valueOf(sPid));
                if (flag) {
                    mOutRoot.write(String.valueOf(sPid).getBytes(StandardCharsets.UTF_8.name()));
                }
                i++;
            } catch (UnsupportedEncodingException e) {
                AwareLog.w(TAG, "UnsupportedEncodingException : doRemoveCpuPidsOpt");
            } catch (IOException e2) {
                AwareLog.w(TAG, "IOException : doRemoveCpuPidsOpt");
            }
        }
        AwareLog.i(TAG, "doRemoveCpuPidsOpt: " + cpuLimitPids.toString());
    }

    private int getLevel() {
        return 0;
    }
}
