package com.android.server.rms.iaware.appmng;

import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IProcessObserver;
import android.app.IProcessObserver.Stub;
import android.app.IUserSwitchObserver;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.internal.app.ProcessMap;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessWindowInfo;
import com.android.server.mtm.iaware.appmng.appclean.CrashClean;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.srms.AppCleanupFeature;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.view.HwWindowManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AwareAppAssociate {
    public static final int ASSOC_DECAY_MIN_TIME = 120000;
    public static final int ASSOC_REPORT_MIN_TIME = 60000;
    public static final int CLEAN_LEVEL = 0;
    private static boolean DEBUG = false;
    public static final int FIRST_START_TIMES = 1;
    private static final int FIVE_SECONDS = 5000;
    private static final String INTERNALAPP_PKGNAME = "com.huawei.android.internal.app";
    private static final int MSG_CHECK_RECENT_FORE = 3;
    private static final int MSG_CLEAN = 2;
    private static final int MSG_CLEAR_BAKUP_VISWIN = 4;
    private static final int MSG_INIT = 1;
    public static final int MS_TO_SEC = 1000;
    private static final int ONE_SECOND = 1000;
    private static final int RECENT_TIME_INTERVAL = 10000;
    private static boolean RECORD = false;
    private static final long REINIT_TIME = 2000;
    public static final int RESTART_MAX_INTERVAL = 300;
    public static final int RESTART_MAX_TIMES = 30;
    private static final int SMCS_APP_WIDGET_SERVICE_GET_BY_USERID = 2;
    private static final String SYSTEM = "system";
    private static final String TAG = "RMS.AwareAppAssociate";
    private static final int VISIBLEWINDOWS_ADD_WINDOW = 4;
    private static final int VISIBLEWINDOWS_CACHE_CHANGE_MODE = 3;
    private static final int VISIBLEWINDOWS_CACHE_CLR = 2;
    private static final int VISIBLEWINDOWS_CACHE_DEL = 1;
    private static final int VISIBLEWINDOWS_CACHE_UPDATE = 0;
    private static final int VISIBLEWINDOWS_REMOVE_WINDOW = 5;
    private static final int WIDGET_INVISIBLE = 0;
    private static final int WIDGET_VISIBLE = 1;
    private static AwareAppAssociate mAwareAppAssociate = null;
    private static boolean mEnabled = false;
    private final AwareAppLruBase mAmsPrevBase;
    private final ArrayMap<Integer, AssocPidRecord> mAssocRecordMap;
    private ArrayMap<Integer, ProcessData> mBgRecentForcePids;
    private final ArraySet<IAwareVisibleCallback> mCallbacks;
    private int mCurSwitchUser;
    private int mCurUserId;
    private ArrayMap<Integer, Integer> mForePids;
    private AppAssocHandler mHandler;
    private ArrayList<String> mHomePackageList;
    private int mHomeProcessPid;
    private int mHomeProcessUid;
    private HwActivityManagerService mHwAMS;
    private AtomicBoolean mIsInitialized;
    private LruCache<Integer, AwareAppLruBase> mLruCache;
    private MultiTaskManagerService mMtmService;
    private int mMyPid;
    private final AwareAppLruBase mPrevNonHomeBase;
    private final ProcessMap<AssocBaseRecord> mProcInfoMap;
    private Map<Integer, Map<String, Map<String, LaunchData>>> mProcLaunchMap;
    private final ArrayMap<Integer, AssocBaseRecord> mProcPidMap;
    private final ArrayMap<String, ArraySet<Integer>> mProcPkgMap;
    private final ArrayMap<Integer, ArraySet<Integer>> mProcUidMap;
    private IProcessObserver mProcessObserver;
    private final AwareAppLruBase mRecentTaskPrevBase;
    private boolean mScreenOff;
    IUserSwitchObserver mUserSwitchObserver;
    private ArraySet<Integer> mVisWinDurScreenOff;
    private ArrayMap<Integer, AwareProcessWindowInfo> mVisibleWindows;
    private ArrayMap<Integer, AwareProcessWindowInfo> mVisibleWindowsCache;
    private ArrayMap<Integer, ArrayMap<Integer, Widget>> mWidgets;

    private class AppAssocHandler extends Handler {
        public AppAssocHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (AwareAppAssociate.DEBUG) {
                String str = AwareAppAssociate.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage message ");
                stringBuilder.append(msg.what);
                AwareLog.e(str, stringBuilder.toString());
            }
            if (msg.what == 1) {
                AwareAppAssociate.this.initialize();
            } else if (msg.what == 2) {
                HashMap<String, String> cleanMsg = msg.obj;
                String pkg = (String) cleanMsg.get("pkg");
                String proc = (String) cleanMsg.get("proc");
                try {
                    int userId = Integer.parseInt((String) cleanMsg.get("userId"));
                    if (userId < 0 || AwareAppAssociate.this.mMtmService == null) {
                        AwareLog.e(AwareAppAssociate.TAG, "MSG_CLEAN, userId or mMtmService error!");
                        return;
                    }
                    CrashClean crashClean = new CrashClean(userId, 0, pkg, AwareAppAssociate.this.mMtmService.context());
                    if (AwareAppAssociate.DEBUG) {
                        String str2 = AwareAppAssociate.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pkg:");
                        stringBuilder2.append(pkg);
                        stringBuilder2.append(" will be cleaned due to high-freq-restart of proc:");
                        stringBuilder2.append(proc);
                        AwareLog.i(str2, stringBuilder2.toString());
                    }
                    crashClean.clean();
                } catch (NumberFormatException e) {
                    AwareLog.e(AwareAppAssociate.TAG, "MSG_CLEAN, userId format error!");
                }
            } else {
                if (msg.what == 3) {
                    AwareAppAssociate.this.checkRecentForce();
                } else if (msg.what == 4) {
                    AwareAppAssociate.this.clearRemoveVisWinDurScreenOff();
                }
            }
        }
    }

    private static final class AssocBaseRecord {
        public boolean isStrong = true;
        public HashSet<String> mComponents = new HashSet();
        public long miniTime;
        public int pid;
        public ArraySet<String> pkgList = new ArraySet();
        public String processName;
        public int uid;

        public AssocBaseRecord(String name, int uid, int pid) {
            this.processName = name;
            this.uid = uid;
            this.pid = pid;
            this.miniTime = SystemClock.elapsedRealtime();
        }
    }

    private final class AssocPidRecord {
        public final ProcessMap<AssocBaseRecord> mAssocBindService = new ProcessMap();
        public final ProcessMap<AssocBaseRecord> mAssocProvider = new ProcessMap();
        public int pid;
        public String processName;
        public int uid;

        public AssocPidRecord(int pid, int uid, String name) {
            this.pid = pid;
            this.uid = uid;
            this.processName = name;
        }

        public ProcessMap<AssocBaseRecord> getMap(int type) {
            switch (type) {
                case 1:
                case 3:
                    return this.mAssocBindService;
                case 2:
                    return this.mAssocProvider;
                default:
                    return null;
            }
        }

        public boolean isEmpty() {
            return this.mAssocBindService.getMap().isEmpty() && this.mAssocProvider.getMap().isEmpty();
        }

        public int size() {
            return this.mAssocBindService.getMap().size() + this.mAssocProvider.getMap().size();
        }

        public String toString() {
            int i;
            StringBuilder sb = new StringBuilder();
            sb.append("Pid:");
            sb.append(this.pid);
            sb.append(",Uid:");
            sb.append(this.uid);
            sb.append(",ProcessName:");
            sb.append(this.processName);
            sb.append("\n");
            String sameUid = AwareAppAssociate.this.sameUid(this.pid);
            if (sameUid != null) {
                sb.append(sameUid);
            }
            int NP = this.mAssocBindService.getMap().size();
            boolean flag = true;
            int i2 = 0;
            while (i2 < NP) {
                SparseArray<AssocBaseRecord> brs = (SparseArray) this.mAssocBindService.getMap().valueAt(i2);
                int NB = brs.size();
                boolean flag2 = flag;
                int j = 0;
                while (j < NB) {
                    AssocBaseRecord br = (AssocBaseRecord) brs.valueAt(j);
                    if (flag2) {
                        sb.append("    [BindService] depend on:\n");
                        flag2 = false;
                    }
                    Iterator it = br.mComponents.iterator();
                    while (it.hasNext()) {
                        String component = (String) it.next();
                        sb.append("        Pid:");
                        sb.append(br.pid);
                        sb.append(",Uid:");
                        sb.append(br.uid);
                        sb.append(",ProcessName:");
                        sb.append(br.processName);
                        sb.append(",Time:");
                        int j2 = j;
                        sb.append(SystemClock.elapsedRealtime() - br.miniTime);
                        sb.append(",Component:");
                        sb.append(component);
                        sb.append("\n");
                        j = j2;
                    }
                    j++;
                }
                i2++;
                flag = flag2;
            }
            NP = this.mAssocProvider.getMap().size();
            boolean flag3 = true;
            i2 = 0;
            while (i2 < NP) {
                SparseArray<AssocBaseRecord> brs2 = (SparseArray) this.mAssocProvider.getMap().valueAt(i2);
                int NB2 = brs2.size();
                boolean flag4 = flag3;
                for (int j3 = 0; j3 < NB2; j3++) {
                    AssocBaseRecord br2 = (AssocBaseRecord) brs2.valueAt(j3);
                    if (flag4) {
                        sb.append("    [Provider] depend on:\n");
                        flag4 = false;
                    }
                    Iterator it2 = br2.mComponents.iterator();
                    while (it2.hasNext()) {
                        String component2 = (String) it2.next();
                        String sameUid2 = sameUid;
                        i = NP;
                        if (SystemClock.elapsedRealtime() - br2.miniTime < 120000) {
                            sb.append("        Pid:");
                            sb.append(br2.pid);
                            sb.append(",Uid:");
                            sb.append(br2.uid);
                            sb.append(",ProcessName:");
                            sb.append(br2.processName);
                            sb.append(",Time:");
                            sb.append(SystemClock.elapsedRealtime() - br2.miniTime);
                            sb.append(",Component:");
                            sb.append(component2);
                            sb.append(",Strong:");
                            sb.append(br2.isStrong);
                            sb.append("\n");
                        }
                        sameUid = sameUid2;
                        NP = i;
                    }
                    i = NP;
                }
                i = NP;
                i2++;
                flag3 = flag4;
            }
            i = NP;
            return sb.toString();
        }
    }

    public interface IAwareVisibleCallback {
        void onVisibleWindowsChanged(int i, int i2, int i3);
    }

    private static class LaunchData {
        private long mFirstTime;
        private int mLaunchTimes;

        /* synthetic */ LaunchData(int x0, long x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private LaunchData(int launchTimes, long firstTime) {
            this.mLaunchTimes = launchTimes;
            this.mFirstTime = firstTime;
        }

        private LaunchData increase() {
            this.mLaunchTimes++;
            return this;
        }

        private long getFirstTime() {
            return this.mFirstTime;
        }

        private int getLaunchTimes() {
            return this.mLaunchTimes;
        }
    }

    private static class ProcessData {
        private long mTimeStamp;
        private int mUid;

        /* synthetic */ ProcessData(int x0, long x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private ProcessData(int uid, long timeStamp) {
            this.mUid = uid;
            this.mTimeStamp = timeStamp;
        }
    }

    private static final class Widget {
        int appWidgetId;
        boolean isVisible = false;
        String pkgName = "";

        public Widget(int appWidgetId, String pkgName, boolean isVisible) {
            this.appWidgetId = appWidgetId;
            this.pkgName = pkgName;
            this.isVisible = isVisible;
        }
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void removeWindow(int r15, int r16) {
        /*
        r14 = this;
        r9 = r14;
        r10 = r15;
        if (r10 > 0) goto L_0x0005;
    L_0x0004:
        return;
    L_0x0005:
        r11 = 0;
        r12 = 0;
        r13 = r9.mVisibleWindows;
        monitor-enter(r13);
        r0 = r9.mVisibleWindows;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r0 = r0.get(r1);	 Catch:{ all -> 0x00c7 }
        r0 = (com.android.server.mtm.iaware.appmng.AwareProcessWindowInfo) r0;	 Catch:{ all -> 0x00c7 }
        if (r0 != 0) goto L_0x0035;	 Catch:{ all -> 0x00c7 }
    L_0x0018:
        r1 = r9.mVisibleWindows;	 Catch:{ all -> 0x00c7 }
        r2 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r1.remove(r2);	 Catch:{ all -> 0x00c7 }
        r2 = 1;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r3 = r1.intValue();	 Catch:{ all -> 0x00c7 }
        r4 = -1;	 Catch:{ all -> 0x00c7 }
        r5 = 0;	 Catch:{ all -> 0x00c7 }
        r6 = -1;	 Catch:{ all -> 0x00c7 }
        r7 = -1;	 Catch:{ all -> 0x00c7 }
        r8 = 0;	 Catch:{ all -> 0x00c7 }
        r1 = r9;	 Catch:{ all -> 0x00c7 }
        r1.updateVisibleWindowsCache(r2, r3, r4, r5, r6, r7, r8);	 Catch:{ all -> 0x00c7 }
        monitor-exit(r13);	 Catch:{ all -> 0x00c7 }
        return;	 Catch:{ all -> 0x00c7 }
    L_0x0035:
        r1 = r0.isEvil();	 Catch:{ all -> 0x00c7 }
        r12 = r1;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r16);	 Catch:{ all -> 0x00c7 }
        r0.removeWindow(r1);	 Catch:{ all -> 0x00c7 }
        r2 = 5;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r3 = r1.intValue();	 Catch:{ all -> 0x00c7 }
        r4 = -1;	 Catch:{ all -> 0x00c7 }
        r5 = 0;	 Catch:{ all -> 0x00c7 }
        r6 = -1;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r16);	 Catch:{ all -> 0x00c7 }
        r7 = r1.intValue();	 Catch:{ all -> 0x00c7 }
        r8 = 0;	 Catch:{ all -> 0x00c7 }
        r1 = r9;	 Catch:{ all -> 0x00c7 }
        r1.updateVisibleWindowsCache(r2, r3, r4, r5, r6, r7, r8);	 Catch:{ all -> 0x00c7 }
        r1 = r0.mWindows;	 Catch:{ all -> 0x00c7 }
        r1 = r1.size();	 Catch:{ all -> 0x00c7 }
        if (r1 != 0) goto L_0x0085;	 Catch:{ all -> 0x00c7 }
    L_0x0062:
        r1 = r9.mVisibleWindows;	 Catch:{ all -> 0x00c7 }
        r2 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r1.remove(r2);	 Catch:{ all -> 0x00c7 }
        r2 = 1;	 Catch:{ all -> 0x00c7 }
        r1 = java.lang.Integer.valueOf(r10);	 Catch:{ all -> 0x00c7 }
        r3 = r1.intValue();	 Catch:{ all -> 0x00c7 }
        r4 = -1;	 Catch:{ all -> 0x00c7 }
        r5 = 0;	 Catch:{ all -> 0x00c7 }
        r6 = -1;	 Catch:{ all -> 0x00c7 }
        r7 = -1;	 Catch:{ all -> 0x00c7 }
        r8 = 0;	 Catch:{ all -> 0x00c7 }
        r1 = r9;	 Catch:{ all -> 0x00c7 }
        r1.updateVisibleWindowsCache(r2, r3, r4, r5, r6, r7, r8);	 Catch:{ all -> 0x00c7 }
        if (r12 != 0) goto L_0x0084;	 Catch:{ all -> 0x00c7 }
    L_0x007f:
        r1 = 1;	 Catch:{ all -> 0x00c7 }
        r2 = -1;	 Catch:{ all -> 0x00c7 }
        r9.notifyVisibleWindowsChange(r1, r10, r2);	 Catch:{ all -> 0x00c7 }
    L_0x0084:
        r11 = 1;	 Catch:{ all -> 0x00c7 }
    L_0x0085:
        monitor-exit(r13);	 Catch:{ all -> 0x00c7 }
        if (r11 == 0) goto L_0x009f;
    L_0x0088:
        r0 = r9.mScreenOff;
        if (r0 == 0) goto L_0x009f;
    L_0x008c:
        if (r12 != 0) goto L_0x009f;
    L_0x008e:
        r1 = r9.mVisWinDurScreenOff;
        monitor-enter(r1);
        r0 = r9.mVisWinDurScreenOff;
        r2 = java.lang.Integer.valueOf(r10);
        r0.add(r2);
        monitor-exit(r1);
        goto L_0x009f;
    L_0x009c:
        r0 = move-exception;
        monitor-exit(r1);
        throw r0;
    L_0x009f:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x00c4;
    L_0x00a3:
        r0 = "RMS.AwareAppAssociate";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "[removeVisibleWindows]:";
        r1.append(r2);
        r1.append(r10);
        r2 = " [code]:";
        r1.append(r2);
        r2 = r16;
        r1.append(r2);
        r1 = r1.toString();
        android.rms.iaware.AwareLog.d(r0, r1);
        goto L_0x00c6;
    L_0x00c4:
        r2 = r16;
    L_0x00c6:
        return;
    L_0x00c7:
        r0 = move-exception;
        r2 = r16;
    L_0x00ca:
        monitor-exit(r13);	 Catch:{ all -> 0x00cc }
        throw r0;
    L_0x00cc:
        r0 = move-exception;
        goto L_0x00ca;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.rms.iaware.appmng.AwareAppAssociate.removeWindow(int, int):void");
    }

    private void checkRecentForce() {
        int removeCount = 0;
        long curTime = SystemClock.elapsedRealtime();
        synchronized (this.mBgRecentForcePids) {
            for (int i = this.mBgRecentForcePids.size() - 1; i >= 0; i--) {
                if (curTime - ((ProcessData) this.mBgRecentForcePids.valueAt(i)).mTimeStamp > MemoryConstant.MIN_INTERVAL_OP_TIMEOUT) {
                    this.mBgRecentForcePids.removeAt(i);
                    removeCount++;
                }
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkRecentForce removeCount: ");
        stringBuilder.append(removeCount);
        AwareLog.d(str, stringBuilder.toString());
    }

    private void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "register process observer failed");
        }
    }

    private void unregisterProcessObserver() {
        try {
            ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "unregister process observer failed");
        }
    }

    private LinkedHashMap<Integer, AwareAppLruBase> getActivityLruCache() {
        LinkedHashMap<Integer, AwareAppLruBase> lru = null;
        synchronized (this.mLruCache) {
            Map<Integer, AwareAppLruBase> tmp = this.mLruCache.snapshot();
            if (tmp instanceof LinkedHashMap) {
                lru = (LinkedHashMap) tmp;
            }
        }
        return lru;
    }

    private boolean updateActivityLruCache(int pid, int uid) {
        int i = pid;
        int i2 = uid;
        long timeNow = SystemClock.elapsedRealtime();
        synchronized (this.mLruCache) {
            if (this.mLruCache.size() == 0) {
                this.mLruCache.put(Integer.valueOf(uid), new AwareAppLruBase(i, i2, timeNow));
                return false;
            }
            LinkedHashMap<Integer, AwareAppLruBase> lru = getActivityLruCache();
            if (lru == null) {
                return false;
            }
            ArrayList list = new ArrayList(lru.keySet());
            if (list.size() < 1) {
                return false;
            }
            int prevUid = ((Integer) list.get(list.size() - 1)).intValue();
            AwareAppLruBase lruBase = (AwareAppLruBase) lru.get(Integer.valueOf(prevUid));
            if (lruBase == null) {
                return false;
            } else if (prevUid == i2) {
                LruCache lruCache = this.mLruCache;
                LruCache lruCache2 = lruCache;
                lruCache2.put(Integer.valueOf(prevUid), new AwareAppLruBase(i, prevUid, lruBase.mTime));
                return false;
            } else {
                int prevUid2 = prevUid;
                AwareAppLruBase lruBase2 = lruBase;
                if (isSystemDialogProc(lruBase.mPid, prevUid, lruBase.mTime, timeNow)) {
                    this.mLruCache.remove(Integer.valueOf(prevUid2));
                } else {
                    this.mLruCache.put(Integer.valueOf(prevUid2), new AwareAppLruBase(lruBase2.mPid, prevUid2, timeNow));
                }
                this.mLruCache.put(Integer.valueOf(uid), new AwareAppLruBase(i, i2, timeNow));
                return true;
            }
        }
    }

    private void updatePrevApp(int pid, int uid) {
        if (updateActivityLruCache(pid, uid)) {
            LinkedHashMap<Integer, AwareAppLruBase> lru = getActivityLruCache();
            if (lru != null) {
                List<Integer> list = new ArrayList(lru.keySet());
                int listSize = list.size();
                if (listSize >= 2) {
                    int prevUid = ((Integer) list.get(listSize - 2)).intValue();
                    if (prevUid != this.mHomeProcessUid) {
                        AwareAppLruBase.copyLruBaseInfo((AwareAppLruBase) lru.get(Integer.valueOf(prevUid)), this.mPrevNonHomeBase);
                    } else if (listSize < 3) {
                        this.mPrevNonHomeBase.setInitValue();
                    } else {
                        AwareAppLruBase.copyLruBaseInfo((AwareAppLruBase) lru.get(Integer.valueOf(((Integer) list.get(listSize - 3)).intValue())), this.mPrevNonHomeBase);
                    }
                }
            }
        }
    }

    private void updatePreviousAppInfo(int pid, int uid, boolean foregroundActivities, Map<Integer, Integer> forePids) {
        if (this.mHwAMS != null) {
            if (foregroundActivities) {
                if (isForgroundPid(pid)) {
                    updatePrevApp(pid, uid);
                }
            } else if (forePids != null) {
                if (!(forePids.containsValue(Integer.valueOf(uid)) || pid == this.mHomeProcessPid)) {
                    this.mRecentTaskPrevBase.setValue(pid, uid, SystemClock.elapsedRealtime());
                }
                for (Entry<Integer, Integer> m : forePids.entrySet()) {
                    Integer forePid = (Integer) m.getKey();
                    if (isForgroundPid(forePid.intValue())) {
                        updatePrevApp(forePid.intValue(), ((Integer) m.getValue()).intValue());
                        return;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0038, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSystemDialogProc(int pid, int uid, long prevActTime, long curTime) {
        if (UserHandle.getAppId(uid) != 1000) {
            return false;
        }
        synchronized (this) {
            AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
            if (br == null || br.pkgList == null) {
            } else if (br.pkgList.size() != 1) {
                return false;
            } else if (br.pkgList.contains(INTERNALAPP_PKGNAME)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean isForgroundPid(int pid) {
        if (this.mHwAMS.getProcessBaseInfo(pid).mCurAdj == 0) {
            return true;
        }
        return false;
    }

    public static boolean isDealAsPkgUid(int uid) {
        int appId = UserHandle.getAppId(uid);
        return appId >= 1000 && appId <= 1001;
    }

    private AwareAppAssociate() {
        this.mMyPid = Process.myPid();
        this.mIsInitialized = new AtomicBoolean(false);
        this.mHandler = null;
        this.mForePids = new ArrayMap();
        this.mLruCache = new LruCache(4);
        this.mProcLaunchMap = new HashMap();
        this.mPrevNonHomeBase = new AwareAppLruBase();
        this.mRecentTaskPrevBase = new AwareAppLruBase();
        this.mAmsPrevBase = new AwareAppLruBase();
        this.mCurUserId = 0;
        this.mCurSwitchUser = 0;
        this.mVisibleWindows = new ArrayMap();
        this.mVisibleWindowsCache = new ArrayMap();
        this.mWidgets = new ArrayMap();
        this.mHomeProcessPid = 0;
        this.mHomeProcessUid = 0;
        this.mHomePackageList = new ArrayList();
        this.mAssocRecordMap = new ArrayMap();
        this.mProcInfoMap = new ProcessMap();
        this.mProcPidMap = new ArrayMap();
        this.mProcUidMap = new ArrayMap();
        this.mProcPkgMap = new ArrayMap();
        this.mCallbacks = new ArraySet();
        this.mVisWinDurScreenOff = new ArraySet();
        this.mScreenOff = false;
        this.mBgRecentForcePids = new ArrayMap();
        this.mProcessObserver = new Stub() {
            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
                if (AwareAppAssociate.DEBUG) {
                    String str = AwareAppAssociate.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Pid:");
                    stringBuilder.append(pid);
                    stringBuilder.append(",Uid:");
                    stringBuilder.append(uid);
                    stringBuilder.append(" come to foreground.");
                    stringBuilder.append(foregroundActivities);
                    AwareLog.i(str, stringBuilder.toString());
                }
                ArrayMap<Integer, Integer> forePidsBak = new ArrayMap();
                synchronized (AwareAppAssociate.this.mForePids) {
                    if (foregroundActivities) {
                        AwareAppAssociate.this.mForePids.put(Integer.valueOf(pid), Integer.valueOf(uid));
                        forePidsBak.putAll(AwareAppAssociate.this.mForePids);
                    } else {
                        AwareAppAssociate.this.mForePids.remove(Integer.valueOf(pid));
                        forePidsBak.putAll(AwareAppAssociate.this.mForePids);
                    }
                }
                synchronized (AwareAppAssociate.this.mBgRecentForcePids) {
                    if (foregroundActivities) {
                        AwareAppAssociate.this.mBgRecentForcePids.remove(Integer.valueOf(pid));
                    } else {
                        AwareAppAssociate.this.mBgRecentForcePids.put(Integer.valueOf(pid), new ProcessData(uid, SystemClock.elapsedRealtime(), null));
                        if (AwareAppAssociate.this.mHandler != null) {
                            AwareAppAssociate.this.mHandler.sendEmptyMessageDelayed(3, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                        }
                    }
                }
                AwareAppAssociate.this.updatePreviousAppInfo(pid, uid, foregroundActivities, forePidsBak);
            }

            public void onProcessDied(int pid, int uid) {
                synchronized (AwareAppAssociate.this.mForePids) {
                    AwareAppAssociate.this.mForePids.remove(Integer.valueOf(pid));
                }
                synchronized (AwareAppAssociate.this.mBgRecentForcePids) {
                    AwareAppAssociate.this.mBgRecentForcePids.remove(Integer.valueOf(pid));
                }
                try {
                    AwareAppAssociate.this.removeDiedProcessRelation(pid, uid);
                } catch (NullPointerException e) {
                    AwareLog.d(AwareAppAssociate.TAG, "remove died processrelation failed caused by null pointer");
                } catch (Exception e2) {
                    AwareLog.d(AwareAppAssociate.TAG, "remove died processrelation failed");
                }
                AwareAppAssociate.this.removeDiedRecordProc(uid, pid);
                HwActivityManager.reportProcessDied(pid);
            }
        };
        this.mUserSwitchObserver = new IUserSwitchObserver.Stub() {
            public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                if (reply != null) {
                    try {
                        reply.sendResult(null);
                        AwareAppAssociate.this.mCurSwitchUser = newUserId;
                    } catch (RemoteException e) {
                        AwareLog.e(AwareAppAssociate.TAG, "RemoteException onUserSwitching");
                    }
                }
            }

            public void onUserSwitchComplete(int newUserId) throws RemoteException {
                long startTime = System.currentTimeMillis();
                AwareAppAssociate.this.checkAndInitWidgetObj(newUserId);
                AwareAppAssociate.this.mCurUserId = newUserId;
                AwareAppAssociate.this.mCurSwitchUser = newUserId;
                AwareAppAssociate.this.updateWidgets(AwareAppAssociate.this.mCurUserId);
                AwareIntelligentRecg.getInstance().initUserSwitch(newUserId);
                AwareFakeActivityRecg.self().initUserSwitch(newUserId);
                if (newUserId == 0) {
                    AwareIntelligentRecg.getInstance().updateWidget(AwareAppAssociate.this.getWidgetsPkg(newUserId), null);
                }
                String str = AwareAppAssociate.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUserSwitchComplete cost: ");
                stringBuilder.append(System.currentTimeMillis() - startTime);
                AwareLog.i(str, stringBuilder.toString());
            }

            public void onForegroundProfileSwitch(int newProfileId) {
            }

            public void onLockedBootComplete(int newUserId) {
            }
        };
        this.mHwAMS = HwActivityManagerService.self();
        this.mHandler = new AppAssocHandler(BackgroundThread.get().getLooper());
    }

    public static synchronized AwareAppAssociate getInstance() {
        AwareAppAssociate awareAppAssociate;
        synchronized (AwareAppAssociate.class) {
            if (mAwareAppAssociate == null) {
                mAwareAppAssociate = new AwareAppAssociate();
            }
            awareAppAssociate = mAwareAppAssociate;
        }
        return awareAppAssociate;
    }

    public void getVisibleWindowsInRestriction(Set<Integer> windowPids) {
        if (mEnabled && windowPids != null) {
            synchronized (this.mVisibleWindows) {
                for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                    AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) window.getValue();
                    if (winInfo.mInRestriction && !winInfo.isEvil()) {
                        windowPids.add((Integer) window.getKey());
                    }
                }
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WindowPids in restriction:");
                stringBuilder.append(windowPids);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    public void getVisibleWindows(Set<Integer> windowPids, Set<Integer> evilPids) {
        if (mEnabled && windowPids != null) {
            synchronized (this.mVisibleWindows) {
                for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                    AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) window.getValue();
                    boolean allowedWindow = winInfo.mMode == 0 || winInfo.mMode == 3;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[getVisibleWindows]:");
                    stringBuilder.append(window.getKey());
                    stringBuilder.append(" [allowedWindow]:");
                    stringBuilder.append(allowedWindow);
                    stringBuilder.append(" isEvil:");
                    stringBuilder.append(winInfo.isEvil());
                    AwareLog.i(str, stringBuilder.toString());
                    if (allowedWindow && !winInfo.isEvil()) {
                        windowPids.add((Integer) window.getKey());
                    } else if (evilPids != null) {
                        evilPids.add((Integer) window.getKey());
                    }
                }
            }
            synchronized (this.mVisWinDurScreenOff) {
                if (!this.mVisWinDurScreenOff.isEmpty()) {
                    windowPids.addAll(this.mVisWinDurScreenOff);
                }
            }
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("WindowPids:");
                stringBuilder2.append(windowPids);
                stringBuilder2.append(", evilPids:");
                stringBuilder2.append(evilPids);
                AwareLog.d(str2, stringBuilder2.toString());
            }
            if (RECORD) {
                recordWindowDetail(windowPids);
            }
        }
    }

    public boolean isVisibleWindows(int userid, String pkg) {
        if (!mEnabled || pkg == null) {
            return true;
        }
        synchronized (this.mVisibleWindows) {
            for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) window.getValue();
                boolean allowedWindow = isAllowedAlertWindowOps(winInfo);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[isVisibleWindows]:");
                stringBuilder.append(window.getKey());
                stringBuilder.append(" pkg:");
                stringBuilder.append(pkg);
                stringBuilder.append(" [allowedWindow]:");
                stringBuilder.append(allowedWindow);
                stringBuilder.append(" isEvil:");
                stringBuilder.append(winInfo.isEvil());
                AwareLog.i(str, stringBuilder.toString());
                if (pkg.equals(winInfo.mPkg) && ((userid == -1 || userid == UserHandle.getUserId(winInfo.mUid)) && allowedWindow && !winInfo.isEvil())) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasWindow(int uid) {
        synchronized (this.mVisibleWindows) {
            for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                if (uid == ((AwareProcessWindowInfo) window.getValue()).mUid) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isAllowedAlertWindowOps(AwareProcessWindowInfo winInfo) {
        return winInfo.mMode == 0 || winInfo.mMode == 3;
    }

    public boolean isEvilAlertWindow(int window, int code) {
        if (!mEnabled) {
            return false;
        }
        boolean result;
        synchronized (this.mVisibleWindows) {
            AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) this.mVisibleWindows.get(Integer.valueOf(window));
            if (winInfo == null || (isAllowedAlertWindowOps(winInfo) && !winInfo.isEvil(code))) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    private void updateWidgets(int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateWidgets, userId: ");
            stringBuilder.append(userId);
            AwareLog.i(str, stringBuilder.toString());
        }
        IBinder service = ServiceManager.getService("appwidget");
        if (service != null) {
            ArrayMap<Integer, Widget> widgets = new ArrayMap();
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(2);
            data.writeInt(userId);
            try {
                service.transact(1599297111, data, reply, 0);
                int size = reply.readInt();
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateWidgets, transact finish, widgets size: ");
                    stringBuilder2.append(size);
                    AwareLog.i(str2, stringBuilder2.toString());
                }
                for (int i = 0; i < size; i++) {
                    int id = reply.readInt();
                    String pkg = reply.readString();
                    boolean visibleB = true;
                    if (reply.readInt() != 1) {
                        visibleB = false;
                    }
                    if (pkg != null && pkg.length() > 0) {
                        widgets.put(Integer.valueOf(id), new Widget(id, pkg, visibleB));
                    }
                    if (DEBUG) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("updateWidgets, widget: ");
                        stringBuilder3.append(id);
                        stringBuilder3.append(", ");
                        stringBuilder3.append(pkg);
                        stringBuilder3.append(", ");
                        stringBuilder3.append(visibleB);
                        AwareLog.i(str3, stringBuilder3.toString());
                    }
                }
            } catch (RemoteException e) {
                AwareLog.e(TAG, "getWidgetsPkg, transact error!");
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
            }
            reply.recycle();
            data.recycle();
            synchronized (this.mWidgets) {
                this.mWidgets.put(Integer.valueOf(userId), widgets);
            }
        }
    }

    public Set<String> getWidgetsPkg() {
        return getWidgetsPkg(this.mCurUserId);
    }

    public Set<String> getWidgetsPkg(int userId) {
        if (!mEnabled) {
            return null;
        }
        ArraySet<String> widgets = new ArraySet();
        synchronized (this.mWidgets) {
            ArrayMap<Integer, Widget> widgetMap = (ArrayMap) this.mWidgets.get(Integer.valueOf(userId));
            if (widgetMap != null) {
                for (Entry<Integer, Widget> entry : widgetMap.entrySet()) {
                    Widget widget = (Widget) entry.getValue();
                    if (widget.isVisible) {
                        widgets.add(widget.pkgName);
                    }
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getWidgetsPkg:");
                        stringBuilder.append(widget.appWidgetId);
                        stringBuilder.append(", ");
                        stringBuilder.append(widget.pkgName);
                        stringBuilder.append(", ");
                        stringBuilder.append(widget.isVisible);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                }
            }
        }
        return widgets;
    }

    public void getForeGroundApp(Set<Integer> forePids) {
        if (mEnabled && forePids != null) {
            synchronized (this.mForePids) {
                forePids.addAll(this.mForePids.keySet());
            }
        }
    }

    public boolean isForeGroundApp(int uid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mForePids) {
            for (Entry<Integer, Integer> map : this.mForePids.entrySet()) {
                if (uid == ((Integer) map.getValue()).intValue()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isRecentFgApp(int uid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mBgRecentForcePids) {
            for (Entry<Integer, ProcessData> map : this.mBgRecentForcePids.entrySet()) {
                ProcessData data = (ProcessData) map.getValue();
                if (data != null && data.mUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    public void getAssocListForPid(int pid, Set<Integer> strong) {
        if (mEnabled && pid > 0 && strong != null) {
            getStrongAssoc(pid, strong);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(pid);
                stringBuilder.append("]strongList:");
                stringBuilder.append(strong);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (RECORD) {
                recordAssocDetail(pid);
            }
        }
    }

    public void getAssocClientListForPid(int pid, Set<Integer> strong) {
        if (mEnabled && pid > 0 && strong != null) {
            getStrongAssocClient(pid, strong);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(pid);
                stringBuilder.append("]strongList:");
                stringBuilder.append(strong);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (RECORD) {
                recordAssocDetail(pid);
            }
        }
    }

    private void getStrongAssocClient(int pid, Set<Integer> strong) {
        if (pid > 0 && strong != null) {
            synchronized (this) {
                for (Entry<Integer, AssocPidRecord> map : this.mAssocRecordMap.entrySet()) {
                    int clientPid = ((Integer) map.getKey()).intValue();
                    AssocPidRecord record = (AssocPidRecord) map.getValue();
                    int NP = record.mAssocBindService.getMap().size();
                    for (int i = 0; i < NP; i++) {
                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                        int NB = brs.size();
                        for (int j = 0; j < NB; j++) {
                            AssocBaseRecord br = (AssocBaseRecord) brs.valueAt(j);
                            if (br != null && br.pid == pid) {
                                strong.add(Integer.valueOf(clientPid));
                            }
                        }
                    }
                }
            }
        }
    }

    public void getAssocClientListForUid(int uid, Set<String> strong) {
        if (mEnabled && uid > 0 && strong != null) {
            synchronized (this) {
                for (Entry<Integer, AssocPidRecord> map : this.mAssocRecordMap.entrySet()) {
                    AssocPidRecord record = (AssocPidRecord) map.getValue();
                    if (UserHandle.getAppId(record.uid) >= 10000) {
                        int NP = record.mAssocBindService.getMap().size();
                        boolean bfound = false;
                        for (int i = 0; i < NP; i++) {
                            SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                            int NB = brs.size();
                            for (int j = 0; j < NB; j++) {
                                AssocBaseRecord br = (AssocBaseRecord) brs.valueAt(j);
                                if (br != null && br.uid == uid) {
                                    strong.addAll(getPackageNameForUid(record.uid, record.pid));
                                    bfound = true;
                                    break;
                                }
                            }
                            if (bfound) {
                                break;
                            }
                        }
                    }
                }
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(uid);
                stringBuilder.append("]strongList:");
                stringBuilder.append(strong);
                AwareLog.i(str, stringBuilder.toString());
            }
        }
    }

    public void report(int eventId, Bundle bundleArgs) {
        int i = eventId;
        Bundle bundle = bundleArgs;
        if (mEnabled) {
            String str;
            StringBuilder stringBuilder;
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("eventId: ");
                stringBuilder.append(i);
                AwareLog.d(str, stringBuilder.toString());
            }
            if (bundle != null) {
                if (!this.mIsInitialized.get()) {
                    initialize();
                }
                int callerPid;
                switch (i) {
                    case 1:
                    case 2:
                        callerPid = bundle.getInt("callPid");
                        addProcessRelation(callerPid, bundle.getInt("callUid"), bundle.getString("callProcName"), bundle.getInt("tgtUid"), bundle.getString("tgtProcName"), bundle.getString("compName"), i);
                        break;
                    case 3:
                        callerPid = bundle.getInt("callPid");
                        removeProcessRelation(callerPid, bundle.getInt("callUid"), bundle.getString("callProcName"), bundle.getInt("tgtUid"), bundle.getString("tgtProcName"), bundle.getString("compName"), i);
                        break;
                    case 4:
                        updateProcessRelation(bundle.getInt("callPid"), bundle.getInt("callUid"), bundle.getString("callProcName"), bundle.getStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                        break;
                    case 5:
                        addWidget(bundle.getInt("userid"), bundle.getInt("widgetId", -1), bundle.getString("widget"), bundle.getBundle("widgetOpt"));
                        break;
                    case 6:
                        removeWidget(bundle.getInt("userid"), bundle.getInt("widgetId", -1), bundle.getString("widget"));
                        break;
                    case 7:
                        clearWidget();
                        break;
                    case 8:
                        addWindow(bundle.getInt("window"), bundle.getInt("windowmode"), bundle.getInt("hashcode"), bundle.getInt("width"), bundle.getInt("height"), bundle.getFloat("alpha"), bundle.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME), bundle.getInt("uid"));
                        break;
                    case 9:
                        removeWindow(bundle.getInt("window"), bundle.getInt("hashcode"));
                        break;
                    case 10:
                        updateWindowOps(bundle.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                        break;
                    case 11:
                        reportHome(bundle.getInt("pid"), bundle.getInt("tgtUid"), bundle.getStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                        break;
                    case 12:
                        reportPrevInfo(bundle.getInt("pid"), bundle.getInt("tgtUid"));
                        break;
                    default:
                        switch (i) {
                            case 24:
                                updateWidgetOptions(bundle.getInt("userid"), bundle.getInt("widgetId", -1), bundle.getString("widget"), bundle.getBundle("widgetOpt"));
                                break;
                            case 25:
                                AwareIntelligentRecg.getInstance().addScreenRecord(bundle.getInt("callUid"), bundle.getInt("callPid"));
                                break;
                            case 26:
                                AwareIntelligentRecg.getInstance().removeScreenRecord(bundle.getInt("callUid"), bundle.getInt("callPid"));
                                break;
                            case 27:
                                updateWindow(bundle.getInt("window"), bundle.getInt("windowmode"), bundle.getInt("hashcode"), bundle.getInt("width"), bundle.getInt("height"), bundle.getFloat("alpha"));
                                break;
                            default:
                                switch (i) {
                                    case 30:
                                        AwareIntelligentRecg.getInstance().addCamera(bundle.getInt("callUid"));
                                        break;
                                    case 31:
                                        AwareIntelligentRecg.getInstance().removeCamera(bundle.getInt("callUid"));
                                        break;
                                    case 32:
                                        updateWidgetFlush(bundle.getInt("userid"), bundle.getString("widget"));
                                        break;
                                    case 33:
                                        AwareIntelligentRecg.getInstance().reportGoogleConn(bundle.getBoolean("gms_conn"));
                                        break;
                                    default:
                                        if (DEBUG) {
                                            str = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Unknown EventID: ");
                                            stringBuilder.append(i);
                                            AwareLog.e(str, stringBuilder.toString());
                                            break;
                                        }
                                        break;
                                }
                        }
                }
                return;
            }
            return;
        }
        if (DEBUG) {
            AwareLog.d(TAG, "AwareAppAssociate feature disabled!");
        }
    }

    private void getStrongAssoc(int pid, Set<Integer> strong) {
        Set<Integer> set = strong;
        if (pid > 0 && set != null) {
            synchronized (this) {
                long curElapse = SystemClock.elapsedRealtime();
                AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(pid));
                if (record == null) {
                    return;
                }
                SparseArray<AssocBaseRecord> brs;
                int NB;
                int NP = record.mAssocBindService.getMap().size();
                AssocBaseRecord br = null;
                int targetPid = 0;
                int i = 0;
                while (i < NP) {
                    brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                    NB = brs.size();
                    int targetPid2 = targetPid;
                    for (targetPid = 0; targetPid < NB; targetPid++) {
                        br = (AssocBaseRecord) brs.valueAt(targetPid);
                        targetPid2 = br.pid;
                        if (targetPid2 != 0) {
                            set.add(Integer.valueOf(targetPid2));
                        }
                    }
                    i++;
                    targetPid = targetPid2;
                }
                i = record.mAssocProvider.getMap().size();
                NP = targetPid;
                targetPid = 0;
                while (targetPid < i) {
                    brs = (SparseArray) record.mAssocProvider.getMap().valueAt(targetPid);
                    NB = brs.size();
                    AssocBaseRecord br2 = br;
                    int targetPid3 = NP;
                    for (NP = 0; NP < NB; NP++) {
                        br2 = (AssocBaseRecord) brs.valueAt(NP);
                        targetPid3 = br2.pid;
                        if (targetPid3 != 0 && br2.isStrong && curElapse - br2.miniTime < 120000) {
                            set.add(Integer.valueOf(targetPid3));
                        }
                    }
                    targetPid++;
                    NP = targetPid3;
                    br = br2;
                }
            }
        }
    }

    public void getAssocProvider(int pid, Set<Integer> assocProvider) {
        Set<Integer> set = assocProvider;
        if (pid > 0 && set != null) {
            synchronized (this) {
                long curElapse = SystemClock.elapsedRealtime();
                AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(pid));
                if (record == null) {
                    return;
                }
                AssocBaseRecord br = null;
                int NP = record.mAssocProvider.getMap().size();
                int targetPid = 0;
                int i = 0;
                while (i < NP) {
                    SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocProvider.getMap().valueAt(i);
                    int NB = brs.size();
                    AssocBaseRecord br2 = br;
                    for (int j = 0; j < NB; j++) {
                        br2 = (AssocBaseRecord) brs.valueAt(j);
                        targetPid = br2.pid;
                        if (targetPid != 0 && br2.isStrong && curElapse - br2.miniTime < 120000) {
                            set.add(Integer.valueOf(targetPid));
                        }
                    }
                    i++;
                    br = br2;
                }
            }
        }
    }

    private void addWidget(int userId, int widgetId, String pkgName, Bundle options) {
        if (pkgName != null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addWidget, userId:");
                stringBuilder.append(userId);
                stringBuilder.append(", widgetId: ");
                stringBuilder.append(widgetId);
                stringBuilder.append(", pkg:");
                stringBuilder.append(pkgName);
                stringBuilder.append(", vis: ");
                stringBuilder.append(isWidgetVisible(options));
                AwareLog.i(str, stringBuilder.toString());
            }
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                if (!((ArrayMap) this.mWidgets.get(Integer.valueOf(userId))).containsKey(Integer.valueOf(widgetId))) {
                    ((ArrayMap) this.mWidgets.get(Integer.valueOf(userId))).put(Integer.valueOf(widgetId), new Widget(widgetId, pkgName, isWidgetVisible(options)));
                }
            }
            if (userId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
            }
        }
    }

    private void removeWidget(int userId, int widgetId, String pkgName) {
        if (pkgName != null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeWidget, userId:");
                stringBuilder.append(userId);
                stringBuilder.append(", widgetId: ");
                stringBuilder.append(widgetId);
                stringBuilder.append(", pkg:");
                stringBuilder.append(pkgName);
                AwareLog.i(str, stringBuilder.toString());
            }
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                ((ArrayMap) this.mWidgets.get(Integer.valueOf(userId))).remove(Integer.valueOf(widgetId));
            }
            if (userId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
            }
        }
    }

    private void updateWidgetOptions(int userId, int widgetId, String pkgName, Bundle options) {
        if (widgetId >= 0 && pkgName != null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateWidgetOptions, userId:");
                stringBuilder.append(userId);
                stringBuilder.append(", widgetId: ");
                stringBuilder.append(widgetId);
                stringBuilder.append(", pkg:");
                stringBuilder.append(pkgName);
                stringBuilder.append(", options: ");
                stringBuilder.append(options);
                AwareLog.i(str, stringBuilder.toString());
            }
            boolean visible = isWidgetVisible(options);
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                ArrayMap<Integer, Widget> widgetMap = (ArrayMap) this.mWidgets.get(Integer.valueOf(userId));
                if (widgetMap.get(Integer.valueOf(widgetId)) != null) {
                    ((Widget) widgetMap.get(Integer.valueOf(widgetId))).isVisible = visible;
                } else {
                    widgetMap.put(Integer.valueOf(widgetId), new Widget(widgetId, pkgName, visible));
                }
                if (userId == 0) {
                    AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
                }
            }
        }
    }

    private void updateWidgetFlush(int userId, String pkgName) {
        AwareIntelligentRecg.getInstance().widgetTrigUpdate(pkgName);
    }

    public boolean isWidgetVisible(Bundle options) {
        if (options == null) {
            return false;
        }
        int maxHeight = options.getInt("appWidgetMaxHeight");
        int maxWidth = options.getInt("appWidgetMaxWidth");
        int minHeight = options.getInt("appWidgetMinHeight");
        int minWidth = options.getInt("appWidgetMinWidth");
        if (maxHeight == 0 && maxWidth == 0 && minHeight == 0 && minWidth == 0) {
            return false;
        }
        return true;
    }

    private void clearWidget() {
        if (DEBUG) {
            AwareLog.d(TAG, "clearWidget");
        }
        synchronized (this.mWidgets) {
            for (Entry<Integer, ArrayMap<Integer, Widget>> m : this.mWidgets.entrySet()) {
                ArrayMap<Integer, Widget> userWdigets = (ArrayMap) m.getValue();
                if (userWdigets != null) {
                    userWdigets.clear();
                }
            }
        }
        AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(0), null);
    }

    private void initVisibleWindows() {
        List<Bundle> windowsList = HwWindowManager.getVisibleWindows(24);
        if (windowsList == null) {
            AwareLog.w(TAG, "Catch null when initVisibleWindows.");
            return;
        }
        synchronized (this.mVisibleWindows) {
            this.mVisibleWindows.clear();
            updateVisibleWindowsCache(2, -1, -1, null, -1, -1, false);
            for (Bundle windowState : windowsList) {
                AwareProcessWindowInfo winInfo;
                boolean isEvil;
                AwareProcessWindowInfo winInfo2;
                int window = windowState.getInt("window_pid");
                int mode = windowState.getInt("window_value");
                int code = windowState.getInt("window_state");
                int width = windowState.getInt("window_width");
                int height = windowState.getInt("window_height");
                float alpha = windowState.getFloat("window_alpha");
                boolean phide = windowState.getBoolean("window_hidden");
                String pkg = windowState.getString("window_package");
                int uid = windowState.getInt("window_uid");
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("initVisibleWindows pid:");
                    stringBuilder.append(window);
                    stringBuilder.append(" mode:");
                    stringBuilder.append(mode);
                    stringBuilder.append(" code:");
                    stringBuilder.append(code);
                    stringBuilder.append(" width:");
                    stringBuilder.append(width);
                    stringBuilder.append(" height:");
                    stringBuilder.append(height);
                    AwareLog.i(str, stringBuilder.toString());
                }
                boolean z = width == AwareProcessWindowInfo.getMinWindowWidth() || height == AwareProcessWindowInfo.getMinWindowHeight() || alpha == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                boolean isEvil2 = z;
                AwareProcessWindowInfo winInfo3 = (AwareProcessWindowInfo) this.mVisibleWindows.get(Integer.valueOf(window));
                if (winInfo3 == null) {
                    AwareProcessWindowInfo winInfo4 = new AwareProcessWindowInfo(mode, pkg, uid);
                    this.mVisibleWindows.put(Integer.valueOf(window), winInfo4);
                    winInfo = winInfo4;
                    boolean isEvil3 = isEvil2;
                    updateVisibleWindowsCache(0, window, mode, pkg, uid, -1, 0);
                    isEvil = isEvil3;
                    if (!isEvil) {
                        notifyVisibleWindowsChange(2, window, mode);
                    }
                    winInfo2 = winInfo;
                } else {
                    String str2 = pkg;
                    int i = height;
                    int i2 = width;
                    isEvil = isEvil2;
                    winInfo2 = winInfo3;
                }
                winInfo2.addWindow(Integer.valueOf(code), isEvil);
                winInfo = winInfo2;
                updateVisibleWindowsCache(4, window, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
            }
        }
    }

    private void deinitVisibleWindows() {
        synchronized (this.mVisibleWindows) {
            this.mVisibleWindows.clear();
            notifyVisibleWindowsChange(0, -1, -1);
            updateVisibleWindowsCache(2, -1, -1, null, -1, -1, false);
        }
    }

    private void addWindow(int window, int mode, int code, int width, int height, float alpha, String pkg, int uid) {
        Throwable th;
        int i = window;
        int i2 = mode;
        int i3 = code;
        int i4 = width;
        int i5 = height;
        float f = alpha;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[addWindow]:");
        stringBuilder.append(i);
        stringBuilder.append(" [mode]:");
        stringBuilder.append(i2);
        stringBuilder.append(" [code]:");
        stringBuilder.append(i3);
        stringBuilder.append(" width:");
        stringBuilder.append(i4);
        stringBuilder.append(" height:");
        stringBuilder.append(i5);
        stringBuilder.append(" alpha:");
        stringBuilder.append(f);
        AwareLog.i(str, stringBuilder.toString());
        if (i > 0) {
            ArrayMap arrayMap = this.mVisibleWindows;
            synchronized (arrayMap) {
                ArrayMap arrayMap2;
                try {
                    boolean isEvil;
                    AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) this.mVisibleWindows.get(Integer.valueOf(window));
                    boolean z = (i4 <= AwareProcessWindowInfo.getMinWindowWidth() && i4 > 0) || ((i5 <= AwareProcessWindowInfo.getMinWindowHeight() && i5 > 0) || f == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                    boolean isEvil2 = z;
                    if (winInfo == null) {
                        String str2 = pkg;
                        winInfo = new AwareProcessWindowInfo(i2, str2, uid);
                        this.mVisibleWindows.put(Integer.valueOf(window), winInfo);
                        boolean isEvil3 = isEvil2;
                        arrayMap2 = arrayMap;
                        try {
                            updateVisibleWindowsCache(0, i, i2, str2, uid, -1, false);
                            isEvil = isEvil3;
                            if (!isEvil) {
                                notifyVisibleWindowsChange(2, i, i2);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    arrayMap2 = arrayMap;
                    isEvil = isEvil2;
                    winInfo.addWindow(Integer.valueOf(code), isEvil);
                    boolean isEvil4 = isEvil;
                    updateVisibleWindowsCache(4, i, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
                    String str3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[addWindow]:");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" [mode]:");
                    stringBuilder2.append(i2);
                    stringBuilder2.append(" [code]:");
                    stringBuilder2.append(i3);
                    stringBuilder2.append(" isEvil:");
                    stringBuilder2.append(isEvil4);
                    AwareLog.i(str3, stringBuilder2.toString());
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[addVisibleWindows]:");
                        stringBuilder.append(i);
                        stringBuilder.append(" [mode]:");
                        stringBuilder.append(i2);
                        stringBuilder.append(" [code]:");
                        stringBuilder.append(i3);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                } catch (Throwable th3) {
                    th = th3;
                    arrayMap2 = arrayMap;
                    throw th;
                }
            }
        }
    }

    private void updateWindowOpsList() {
        synchronized (this.mVisibleWindows) {
            for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) window.getValue();
                int mode = ((AppOpsManager) this.mMtmService.context().getSystemService("appops")).checkOpNoThrow(24, winInfo.mUid, winInfo.mPkg);
                winInfo.mInRestriction = isInRestriction(winInfo.mMode, mode);
                winInfo.mMode = mode;
                updateVisibleWindowsCache(3, ((Integer) window.getKey()).intValue(), mode, null, -1, -1, false);
            }
        }
    }

    private boolean isInRestriction(int oldmode, int newmode) {
        boolean allowedOld = oldmode == 0 || oldmode == 3;
        return allowedOld && newmode == 1;
    }

    private void updateWindowOps(String pkgName) {
        String str = pkgName;
        if (this.mMtmService != null) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateWindowOps pkg:");
                stringBuilder.append(str);
                AwareLog.d(str2, stringBuilder.toString());
            }
            if (str == null) {
                updateWindowOpsList();
                return;
            }
            synchronized (this) {
                synchronized (this.mVisibleWindows) {
                    for (Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                        int pid = ((Integer) window.getKey()).intValue();
                        AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) window.getValue();
                        AssocBaseRecord record = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
                        if (!(record == null || record.pkgList == null)) {
                            if (winInfo != null) {
                                if (record.pkgList.contains(str)) {
                                    AppOpsManager mAppOps = (AppOpsManager) this.mMtmService.context().getSystemService("appops");
                                    int mode = mAppOps.checkOpNoThrow(24, record.uid, str);
                                    winInfo.mMode = mode;
                                    int mode2 = mode;
                                    updateVisibleWindowsCache(3, pid, mode, null, -1, -1, 0);
                                    if (!winInfo.isEvil()) {
                                        notifyVisibleWindowsChange(2, pid, mode2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateWindow(int window, int mode, int code, int width, int height, float alpha) {
        Throwable th;
        int i = window;
        int i2 = mode;
        int i3 = code;
        int i4 = width;
        int i5 = height;
        float f = alpha;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[updateWindow]:");
        stringBuilder.append(i);
        stringBuilder.append(" [mode]:");
        stringBuilder.append(i2);
        stringBuilder.append(" [code]:");
        stringBuilder.append(i3);
        stringBuilder.append(" width:");
        stringBuilder.append(i4);
        stringBuilder.append(" height:");
        stringBuilder.append(i5);
        stringBuilder.append(" alpha:");
        stringBuilder.append(f);
        AwareLog.i(str, stringBuilder.toString());
        if (i > 0) {
            ArrayMap arrayMap = this.mVisibleWindows;
            synchronized (arrayMap) {
                ArrayMap arrayMap2;
                try {
                    boolean isEvil;
                    AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) this.mVisibleWindows.get(Integer.valueOf(window));
                    boolean z = i4 <= AwareProcessWindowInfo.getMinWindowWidth() || i5 <= AwareProcessWindowInfo.getMinWindowHeight() || f == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                    boolean isEvil2 = z;
                    if (winInfo == null || !winInfo.containsWindow(i3)) {
                        isEvil = isEvil2;
                        arrayMap2 = arrayMap;
                    } else {
                        winInfo.addWindow(Integer.valueOf(code), isEvil2);
                        isEvil = isEvil2;
                        arrayMap2 = arrayMap;
                        try {
                            updateVisibleWindowsCache(4, i, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[updateWindow]:");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" [mode]:");
                    stringBuilder2.append(i2);
                    stringBuilder2.append(" [code]:");
                    stringBuilder2.append(i3);
                    stringBuilder2.append(" isEvil:");
                    stringBuilder2.append(isEvil);
                    AwareLog.i(str2, stringBuilder2.toString());
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[updateWindow]:");
                        stringBuilder.append(i);
                        stringBuilder.append(" [mode]:");
                        stringBuilder.append(i2);
                        stringBuilder.append(" [code]:");
                        stringBuilder.append(i3);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                } catch (Throwable th3) {
                    th = th3;
                    arrayMap2 = arrayMap;
                    throw th;
                }
            }
        }
    }

    private void reportHome(int pid, int uid, ArrayList<String> pkgname) {
        this.mHomeProcessPid = pid;
        this.mHomeProcessUid = uid;
        synchronized (this.mHomePackageList) {
            this.mHomePackageList.clear();
            if (pkgname != null && pkgname.size() > 0) {
                this.mHomePackageList.addAll(pkgname);
            }
        }
    }

    private void reportPrevInfo(int pid, int uid) {
        this.mAmsPrevBase.setValue(pid, uid, SystemClock.elapsedRealtime());
    }

    public List<String> getDefaultHomePackages() {
        ArrayList<String> pkgs = new ArrayList();
        synchronized (this.mHomePackageList) {
            pkgs.addAll(this.mHomePackageList);
        }
        return pkgs;
    }

    public int getCurHomeProcessPid() {
        return this.mHomeProcessPid;
    }

    public int getCurHomeProcessUid() {
        return this.mHomeProcessUid;
    }

    public AwareAppLruBase getRecentTaskPrevInfo() {
        return new AwareAppLruBase(this.mRecentTaskPrevBase.mPid, this.mRecentTaskPrevBase.mUid, this.mRecentTaskPrevBase.mTime);
    }

    public AwareAppLruBase getPreviousAppInfo() {
        return new AwareAppLruBase(this.mPrevNonHomeBase.mPid, this.mPrevNonHomeBase.mUid, this.mPrevNonHomeBase.mTime);
    }

    public AwareAppLruBase getPreviousByAmsInfo() {
        return new AwareAppLruBase(this.mAmsPrevBase.mPid, this.mAmsPrevBase.mUid, this.mAmsPrevBase.mTime);
    }

    private boolean checkType(int type) {
        switch (type) {
            case 1:
                return true;
            case 2:
                return true;
            case 3:
                return true;
            default:
                return false;
        }
    }

    private String typeToString(int type) {
        switch (type) {
            case 1:
                return "ADD_ASSOC_BINDSERVICE";
            case 2:
                return "ADD_ASSOC_PROVIDER";
            case 3:
                return "DEL_ASSOC_BINDSERVICE";
            case 4:
                return "APP_ASSOC_PROCESSUPDATE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[Error type]");
                stringBuilder.append(type);
                return stringBuilder.toString();
        }
    }

    /* JADX WARNING: Missing block: B:36:0x00e9, code:
            return;
     */
    /* JADX WARNING: Missing block: B:46:0x011a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addProcessRelation(int callerPid, int callerUid, String callerName, int targetUid, String targetName, String comp, int type) {
        if (!checkType(type)) {
            return;
        }
        String str;
        StringBuilder stringBuilder;
        if (callerUid == targetUid) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" in the same UID.Pass.");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else if (callerPid <= 0 || callerUid <= 0 || targetUid <= 0) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" with wrong pid or uid");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else if (callerName == null || targetName == null) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" with wrong callerName or targetName");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else {
            if (comp == null) {
                comp = "NULL";
            }
            str = comp;
            if (DEBUG) {
                comp = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(". Caller[Pid:");
                stringBuilder.append(callerPid);
                stringBuilder.append("][Uid:");
                stringBuilder.append(callerUid);
                stringBuilder.append("][Name:");
                stringBuilder.append(callerName);
                stringBuilder.append("] Target[Uid:");
                stringBuilder.append(targetUid);
                stringBuilder.append("][pName:");
                stringBuilder.append(targetName);
                stringBuilder.append("][hash:");
                stringBuilder.append(str);
                stringBuilder.append("]");
                AwareLog.i(comp, stringBuilder.toString());
            }
            comp = null;
            if (targetUid != 1000 || !targetName.equals(SYSTEM)) {
                synchronized (this) {
                    AssocBaseRecord br = (AssocBaseRecord) this.mProcInfoMap.get(targetName, targetUid);
                    if (br != null) {
                        comp = br.pid;
                    }
                    AssocPidRecord pidRecord = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(callerPid));
                    AssocBaseRecord baseRecord;
                    ProcessMap<AssocBaseRecord> relations;
                    String str2;
                    StringBuilder stringBuilder2;
                    if (pidRecord == null) {
                        pidRecord = new AssocPidRecord(callerPid, callerUid, callerName);
                        baseRecord = new AssocBaseRecord(targetName, targetUid, comp);
                        baseRecord.mComponents.add(str);
                        relations = pidRecord.getMap(type);
                        if (relations != null) {
                            relations.put(targetName, targetUid, baseRecord);
                            this.mAssocRecordMap.put(Integer.valueOf(callerPid), pidRecord);
                        } else if (DEBUG) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error type:");
                            stringBuilder2.append(type);
                            AwareLog.e(str2, stringBuilder2.toString());
                        }
                    } else {
                        relations = pidRecord.getMap(type);
                        if (relations != null) {
                            baseRecord = (AssocBaseRecord) relations.get(targetName, targetUid);
                            if (baseRecord == null) {
                                baseRecord = new AssocBaseRecord(targetName, targetUid, comp);
                                baseRecord.mComponents.add(str);
                                relations.put(targetName, targetUid, baseRecord);
                                return;
                            }
                            baseRecord.miniTime = SystemClock.elapsedRealtime();
                            baseRecord.isStrong = true;
                            baseRecord.mComponents.add(str);
                        } else if (DEBUG) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error type:");
                            stringBuilder2.append(type);
                            AwareLog.e(str2, stringBuilder2.toString());
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:34:0x00c8, code:
            return;
     */
    /* JADX WARNING: Missing block: B:45:0x00fb, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void removeProcessRelation(int callerPid, int callerUid, String callerName, int targetUid, String targetName, String comp, int type) {
        if (!checkType(type)) {
            return;
        }
        String str;
        StringBuilder stringBuilder;
        if (callerUid == targetUid) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" in the same UID.Pass.");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else if (callerPid <= 0 || callerUid <= 0 || targetUid <= 0) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" with wrong pid or uid");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else if (targetName == null) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(" with wrong targetName");
                AwareLog.i(str, stringBuilder.toString());
            }
        } else {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(typeToString(type));
                stringBuilder.append(". Caller[Pid:");
                stringBuilder.append(callerPid);
                stringBuilder.append("] target[");
                stringBuilder.append(targetUid);
                stringBuilder.append(":");
                stringBuilder.append(targetName);
                stringBuilder.append(":");
                stringBuilder.append(comp);
                stringBuilder.append("]");
                AwareLog.i(str, stringBuilder.toString());
            }
            if (comp == null) {
                comp = "NULL";
            }
            str = comp;
            synchronized (this) {
                AssocPidRecord pr = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(callerPid));
                if (pr == null) {
                    return;
                }
                ProcessMap<AssocBaseRecord> relations = pr.getMap(type);
                if (relations != null) {
                    AssocBaseRecord br = (AssocBaseRecord) relations.get(targetName, targetUid);
                    if (br != null && br.mComponents.contains(str)) {
                        br.mComponents.remove(str);
                        if (br.mComponents.isEmpty()) {
                            relations.remove(targetName, targetUid);
                            if (pr.isEmpty()) {
                                this.mAssocRecordMap.remove(Integer.valueOf(pr.pid));
                            }
                        }
                    }
                } else if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error type:");
                    stringBuilder2.append(type);
                    AwareLog.e(str2, stringBuilder2.toString());
                }
            }
        }
    }

    private void removeDiedProcessRelation(int pid, int uid) {
        if (pid <= 0 || uid <= 0) {
            if (DEBUG) {
                AwareLog.i(TAG, "removeDiedProcessRelation with wrong pid or uid");
            }
            return;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove died. Pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" Uid:");
            stringBuilder.append(uid);
            AwareLog.i(str, stringBuilder.toString());
        }
        synchronized (this) {
            AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.remove(Integer.valueOf(pid));
            if (br != null) {
                this.mProcInfoMap.remove(br.processName, br.uid);
                if (br.pkgList != null) {
                    Iterator it = br.pkgList.iterator();
                    while (it.hasNext()) {
                        String pkg = (String) it.next();
                        synchronized (this.mProcPkgMap) {
                            ArraySet<Integer> pids = (ArraySet) this.mProcPkgMap.get(pkg);
                            if (pids != null && pids.contains(Integer.valueOf(pid))) {
                                pids.remove(Integer.valueOf(pid));
                                if (pids.isEmpty()) {
                                    this.mProcPkgMap.remove(pkg);
                                }
                            }
                        }
                    }
                }
            }
            ArraySet<Integer> pids2 = (ArraySet) this.mProcUidMap.get(Integer.valueOf(uid));
            if (pids2 != null && pids2.contains(Integer.valueOf(pid))) {
                pids2.remove(Integer.valueOf(pid));
                if (pids2.isEmpty()) {
                    this.mProcUidMap.remove(Integer.valueOf(uid));
                }
            }
            Iterator<Entry<Integer, AssocPidRecord>> it2 = this.mAssocRecordMap.entrySet().iterator();
            while (it2.hasNext()) {
                AssocPidRecord record = (AssocPidRecord) ((Entry) it2.next()).getValue();
                if (record.pid == pid) {
                    it2.remove();
                } else {
                    if (br != null) {
                        record.mAssocBindService.remove(br.processName, br.uid);
                        record.mAssocProvider.remove(br.processName, br.uid);
                    }
                    if (record.isEmpty()) {
                        it2.remove();
                    }
                }
            }
        }
    }

    private void removeDiedRecordProc(int uid, int pid) {
        if (uid <= 0) {
            AwareLog.i(TAG, "removeDiedRecodrProc with wrong pid or uid");
        } else {
            AwareIntelligentRecg.getInstance().removeDiedScreenProc(uid, pid);
        }
    }

    private void updateProcLaunchData(int uid, String proc, ArrayList<String> pkgList) {
        if (AppCleanupFeature.isAppCleanEnable() && UserHandle.getAppId(uid) >= 10000 && !UserHandle.isIsolated(uid)) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateProcLaunchData, proc: ");
                stringBuilder.append(proc);
                stringBuilder.append(", uid: ");
                stringBuilder.append(uid);
                stringBuilder.append(", pkgList: ");
                stringBuilder.append(pkgList);
                AwareLog.i(str, stringBuilder.toString());
            }
            synchronized (this.mProcLaunchMap) {
                int userId = UserHandle.getUserId(uid);
                Map<String, Map<String, LaunchData>> pkgMap = (Map) this.mProcLaunchMap.get(Integer.valueOf(userId));
                if (pkgMap == null) {
                    pkgMap = new HashMap();
                    this.mProcLaunchMap.put(Integer.valueOf(userId), pkgMap);
                }
                Iterator it = pkgList.iterator();
                while (it.hasNext()) {
                    String pkg = (String) it.next();
                    if (pkg != null) {
                        Map<String, LaunchData> procMap = (Map) pkgMap.get(pkg);
                        if (procMap == null) {
                            procMap = new HashMap();
                            pkgMap.put(pkg, procMap);
                        }
                        LaunchData launchData = (LaunchData) procMap.get(proc);
                        String str2;
                        StringBuilder stringBuilder2;
                        if (launchData != null) {
                            procMap.put(proc, launchData.increase());
                            if (DEBUG) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("updateProcLaunchData, pkg: ");
                                stringBuilder2.append(pkg);
                                stringBuilder2.append(", launcTimes: ");
                                stringBuilder2.append(launchData.getLaunchTimes());
                                AwareLog.i(str2, stringBuilder2.toString());
                            }
                            if (launchData.getLaunchTimes() >= 30) {
                                if (SystemClock.elapsedRealtime() - launchData.getFirstTime() <= HwArbitrationDEFS.DelayTimeMillisB) {
                                    Map<String, String> cleanMsg = new HashMap();
                                    cleanMsg.put("proc", proc);
                                    cleanMsg.put("pkg", pkg);
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("");
                                    stringBuilder3.append(userId);
                                    cleanMsg.put("userId", stringBuilder3.toString());
                                    Message msg = this.mHandler.obtainMessage();
                                    msg.what = 2;
                                    msg.obj = cleanMsg;
                                    this.mHandler.sendMessage(msg);
                                }
                                pkgMap.remove(pkg);
                            }
                        } else {
                            launchData = new LaunchData(1, SystemClock.elapsedRealtime(), null);
                            procMap.put(proc, launchData);
                            if (DEBUG) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("updateProcLaunchData, pkg: ");
                                stringBuilder2.append(pkg);
                                stringBuilder2.append(", launcTimes: ");
                                stringBuilder2.append(launchData.getLaunchTimes());
                                AwareLog.i(str2, stringBuilder2.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateProcessRelation(int pid, int uid, String name, ArrayList<String> pkgList) {
        if (pid <= 0 || uid <= 0) {
            if (DEBUG) {
                AwareLog.i(TAG, "updateProcessRelation with wrong pid or uid");
            }
        } else if (name == null || pkgList == null) {
            if (DEBUG) {
                AwareLog.i(TAG, "updateProcessRelation with wrong name");
            }
        } else {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update relation. Pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" Uid:");
                stringBuilder.append(uid);
                stringBuilder.append(",ProcessName:");
                stringBuilder.append(name);
                AwareLog.i(str, stringBuilder.toString());
            }
            updateProcLaunchData(uid, name, pkgList);
            synchronized (this) {
                AssocBaseRecord br;
                Iterator<Entry<Integer, AssocPidRecord>> it = this.mAssocRecordMap.entrySet().iterator();
                while (it.hasNext()) {
                    AssocPidRecord record = (AssocPidRecord) ((Entry) it.next()).getValue();
                    if (record.pid == pid) {
                        it.remove();
                    } else {
                        br = (AssocBaseRecord) record.mAssocBindService.get(name, uid);
                        if (br != null) {
                            br.pid = pid;
                        }
                        br = (AssocBaseRecord) record.mAssocProvider.get(name, uid);
                        if (br != null) {
                            br.pid = pid;
                        }
                    }
                }
                br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
                if (br == null) {
                    br = new AssocBaseRecord(name, uid, pid);
                    br.pkgList.addAll(pkgList);
                    this.mProcPidMap.put(Integer.valueOf(pid), br);
                } else {
                    br.processName = name;
                    br.uid = uid;
                    br.pid = pid;
                    br.pkgList.addAll(pkgList);
                }
                br = (AssocBaseRecord) this.mProcInfoMap.get(name, uid);
                if (br == null) {
                    this.mProcInfoMap.put(name, uid, new AssocBaseRecord(name, uid, pid));
                } else {
                    br.pid = pid;
                }
                ArraySet<Integer> pids = (ArraySet) this.mProcUidMap.get(Integer.valueOf(uid));
                if (pids == null) {
                    pids = new ArraySet();
                    pids.add(Integer.valueOf(pid));
                    this.mProcUidMap.put(Integer.valueOf(uid), pids);
                } else {
                    pids.add(Integer.valueOf(pid));
                }
                int listSize = pkgList.size();
                for (int i = 0; i < listSize; i++) {
                    String pkg = (String) pkgList.get(i);
                    synchronized (this.mProcPkgMap) {
                        pids = (ArraySet) this.mProcPkgMap.get(pkg);
                        if (pids == null) {
                            pids = new ArraySet();
                            pids.add(Integer.valueOf(pid));
                            this.mProcPkgMap.put(pkg, pids);
                        } else {
                            pids.add(Integer.valueOf(pid));
                        }
                    }
                }
            }
        }
    }

    private void checkAndInitWidgetObj(int userId) {
        synchronized (this.mWidgets) {
            if (this.mWidgets.get(Integer.valueOf(userId)) == null) {
                this.mWidgets.put(Integer.valueOf(userId), new ArrayMap());
            }
        }
    }

    public int getCurUserId() {
        return this.mCurUserId;
    }

    public int getCurSwitchUser() {
        return this.mCurSwitchUser;
    }

    private void initSwitchUser() {
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (currentUser != null) {
                checkAndInitWidgetObj(currentUser.id);
                this.mCurUserId = currentUser.id;
                this.mCurSwitchUser = currentUser.id;
            }
            ActivityManagerNative.getDefault().registerUserSwitchObserver(this.mUserSwitchObserver, TAG);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "Activity manager not running, initSwitchUser error!");
        }
    }

    private void deInitSwitchUser() {
        try {
            ActivityManagerNative.getDefault().unregisterUserSwitchObserver(this.mUserSwitchObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "Activity manager not running, deInitSwitchUser error!");
        }
    }

    private void initialize() {
        if (!this.mIsInitialized.get()) {
            if (this.mMtmService == null) {
                this.mMtmService = MultiTaskManagerService.self();
            }
            if (isUserUnlocked()) {
                if (this.mMtmService != null) {
                    initAssoc();
                    registerProcessObserver();
                    this.mIsInitialized.set(true);
                } else if (DEBUG) {
                    AwareLog.w(TAG, "MultiTaskManagerService has not been started.");
                }
                return;
            }
            if (this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
            }
            this.mHandler.sendEmptyMessageDelayed(1, REINIT_TIME);
        }
    }

    private boolean isUserUnlocked() {
        if (this.mMtmService == null) {
            return false;
        }
        UserManager userManager = (UserManager) this.mMtmService.context().getSystemService("user");
        if (userManager == null) {
            return false;
        }
        return userManager.isUserUnlocked();
    }

    private synchronized void deInitialize() {
        if (this.mIsInitialized.get()) {
            unregisterProcessObserver();
            if (this.mMtmService != null) {
                this.mMtmService = null;
            }
            HwActivityManager.reportAssocDisable();
            deinitVisibleWindows();
            clearWidget();
            deinitAssoc();
            this.mIsInitialized.set(false);
        }
    }

    private void initAssoc() {
        if (this.mHwAMS != null) {
            synchronized (this) {
                AssocBaseRecord br = new AssocBaseRecord(SYSTEM, 1000, this.mMyPid);
                this.mProcPidMap.put(Integer.valueOf(this.mMyPid), br);
                this.mProcInfoMap.put(SYSTEM, 1000, br);
                ArraySet<Integer> pids = new ArraySet();
                pids.add(Integer.valueOf(this.mMyPid));
                this.mProcUidMap.put(Integer.valueOf(1000), pids);
            }
            initSwitchUser();
            initVisibleWindows();
            updateWidgets(this.mCurUserId);
            if (this.mCurUserId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(this.mCurUserId), null);
            }
            ArrayMap<Integer, Integer> forePids = new ArrayMap();
            this.mHwAMS.reportAssocEnable(forePids);
            synchronized (this.mForePids) {
                this.mForePids.clear();
                this.mForePids.putAll(forePids);
            }
            synchronized (this.mBgRecentForcePids) {
                this.mBgRecentForcePids.clear();
            }
        }
    }

    private void deinitAssoc() {
        synchronized (this.mForePids) {
            this.mForePids.clear();
        }
        synchronized (this.mBgRecentForcePids) {
            this.mBgRecentForcePids.clear();
        }
        synchronized (this) {
            this.mAssocRecordMap.clear();
            this.mProcInfoMap.getMap().clear();
            this.mProcPidMap.clear();
            this.mProcUidMap.clear();
            synchronized (this.mProcPkgMap) {
                this.mProcPkgMap.clear();
            }
        }
        deInitSwitchUser();
    }

    public void getPidsByUid(int uid, Set<Integer> pids) {
        if (mEnabled && uid > 0 && pids != null) {
            synchronized (this) {
                ArraySet<Integer> procPids = (ArraySet) this.mProcUidMap.get(Integer.valueOf(uid));
                if (procPids != null) {
                    pids.addAll(procPids);
                }
            }
        }
    }

    private String sameUid(int pid) {
        StringBuilder sb = new StringBuilder();
        boolean flag = true;
        synchronized (this) {
            AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
            if (br == null) {
                return null;
            }
            ArraySet<Integer> pids = (ArraySet) this.mProcUidMap.get(Integer.valueOf(br.uid));
            if (pids == null) {
                return null;
            }
            Iterator it = pids.iterator();
            while (it.hasNext()) {
                int tmp = ((Integer) it.next()).intValue();
                if (tmp != pid) {
                    if (flag) {
                        sb.append("    [SameUID] depend on:\n");
                        flag = false;
                    }
                    br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(tmp));
                    if (br != null) {
                        sb.append("        Pid:");
                        sb.append(br.pid);
                        sb.append(",Uid:");
                        sb.append(br.uid);
                        sb.append(",ProcessName:");
                        sb.append(br.processName);
                        sb.append("\n");
                    }
                }
            }
            return sb.toString();
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0020, code:
            return r0;
     */
    /* JADX WARNING: Missing block: B:28:0x005f, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set<String> getPackageNameForUid(int uid, int pidForUid) {
        ArraySet<String> pkgList = new ArraySet();
        synchronized (this) {
            if (pidForUid != 0) {
                AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pidForUid));
                if (!(br == null || br.pkgList == null)) {
                    pkgList.addAll(br.pkgList);
                }
            } else {
                ArraySet<Integer> pids = (ArraySet) this.mProcUidMap.get(Integer.valueOf(uid));
                if (pids == null || pids.isEmpty()) {
                } else {
                    Iterator it = pids.iterator();
                    while (it.hasNext()) {
                        AssocBaseRecord br2 = (AssocBaseRecord) this.mProcPidMap.get((Integer) it.next());
                        if (!(br2 == null || br2.pkgList == null)) {
                            pkgList.addAll(br2.pkgList);
                        }
                    }
                    return pkgList;
                }
            }
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                synchronized (this) {
                    int listSize = this.mAssocRecordMap.size();
                    for (int s = 0; s < listSize; s++) {
                        AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.valueAt(s);
                        if (record != null) {
                            pw.println(record);
                        }
                    }
                }
                dumpWidget(pw);
                dumpVisibleWindow(pw);
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpFore(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                ArraySet<Integer> tmp = new ArraySet();
                synchronized (this.mForePids) {
                    tmp.addAll(this.mForePids.keySet());
                }
                Iterator it = tmp.iterator();
                while (it.hasNext()) {
                    dumpPid(((Integer) it.next()).intValue(), pw);
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpRecentFore(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                ArraySet<Integer> tmp = new ArraySet();
                synchronized (this.mBgRecentForcePids) {
                    tmp.addAll(this.mBgRecentForcePids.keySet());
                }
                Iterator it = tmp.iterator();
                while (it.hasNext()) {
                    dumpPid(((Integer) it.next()).intValue(), pw);
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpPkgProc(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                StringBuilder stringBuilder;
                synchronized (this.mProcPkgMap) {
                    for (String pkg : this.mProcPkgMap.keySet()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(pkg);
                        stringBuilder.append(":");
                        stringBuilder.append(this.mProcPkgMap.get(pkg));
                        pw.println(stringBuilder.toString());
                    }
                }
                pw.println("proc launch data:");
                synchronized (this.mProcLaunchMap) {
                    for (Entry<Integer, Map<String, Map<String, LaunchData>>> uidEntry : this.mProcLaunchMap.entrySet()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  userId: ");
                        stringBuilder.append(uidEntry.getKey());
                        pw.println(stringBuilder.toString());
                        Map<String, Map<String, LaunchData>> pkgMap = (Map) uidEntry.getValue();
                        if (pkgMap != null) {
                            for (Entry<String, Map<String, LaunchData>> pkgEntry : pkgMap.entrySet()) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("    pkg: ");
                                stringBuilder2.append((String) pkgEntry.getKey());
                                pw.println(stringBuilder2.toString());
                                Map<String, LaunchData> procMap = (Map) pkgEntry.getValue();
                                if (procMap != null) {
                                    for (Entry<String, LaunchData> procEntry : procMap.entrySet()) {
                                        LaunchData lData = (LaunchData) procEntry.getValue();
                                        if (lData != null) {
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("      proc: ");
                                            stringBuilder3.append((String) procEntry.getKey());
                                            stringBuilder3.append(", launchTime: ");
                                            stringBuilder3.append(lData.getLaunchTimes());
                                            pw.println(stringBuilder3.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpPid(int pid, PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                synchronized (this) {
                    AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(pid));
                    if (record != null) {
                        pw.println(record);
                    } else {
                        AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
                        if (br != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Pid:");
                            stringBuilder.append(br.pid);
                            stringBuilder.append(",Uid:");
                            stringBuilder.append(br.uid);
                            stringBuilder.append(",ProcessName:");
                            stringBuilder.append(br.processName);
                            pw.println(stringBuilder.toString());
                        }
                        pw.println(sameUid(pid));
                    }
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpVisibleWindow(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                ArraySet<Integer> windows = new ArraySet();
                ArraySet<Integer> windowsEvil = new ArraySet();
                getVisibleWindows(windows, windowsEvil);
                boolean flag = true;
                pw.println("");
                synchronized (this) {
                    AssocBaseRecord br;
                    StringBuilder stringBuilder;
                    Iterator it = windows.iterator();
                    while (it.hasNext()) {
                        br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(((Integer) it.next()).intValue()));
                        if (br != null) {
                            if (flag) {
                                pw.println("[WindowList] :");
                                flag = false;
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    Pid:");
                            stringBuilder.append(br.pid);
                            stringBuilder.append(",Uid:");
                            stringBuilder.append(br.uid);
                            stringBuilder.append(",ProcessName:");
                            stringBuilder.append(br.processName);
                            stringBuilder.append(",PkgList:");
                            stringBuilder.append(br.pkgList);
                            pw.println(stringBuilder.toString());
                        }
                    }
                    flag = true;
                    it = windowsEvil.iterator();
                    while (it.hasNext()) {
                        br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(((Integer) it.next()).intValue()));
                        if (br != null) {
                            if (flag) {
                                pw.println("[WindowEvilList] :");
                                flag = false;
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    Pid:");
                            stringBuilder.append(br.pid);
                            stringBuilder.append(",Uid:");
                            stringBuilder.append(br.uid);
                            stringBuilder.append(",ProcessName:");
                            stringBuilder.append(br.processName);
                            stringBuilder.append(",PkgList:");
                            stringBuilder.append(br.pkgList);
                            pw.println(stringBuilder.toString());
                        }
                    }
                }
                ArraySet<Integer> windowsClean = new ArraySet();
                getVisibleWindowsInRestriction(windowsClean);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[WindowList in restriction] :");
                stringBuilder2.append(windowsClean);
                pw.println(stringBuilder2.toString());
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpWidget(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                Set<String> widgets = getWidgetsPkg();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[Widgets] : ");
                stringBuilder.append(widgets.size());
                pw.println(stringBuilder.toString());
                for (String w : widgets) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    ");
                    stringBuilder2.append(w);
                    pw.println(stringBuilder2.toString());
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpHome(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                synchronized (this) {
                    AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(this.mHomeProcessPid));
                    if (br != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[Home]Pid:");
                        stringBuilder.append(this.mHomeProcessPid);
                        stringBuilder.append(",Uid:");
                        stringBuilder.append(br.uid);
                        stringBuilder.append(",ProcessName:");
                        stringBuilder.append(br.processName);
                        stringBuilder.append(",pkg:");
                        stringBuilder.append(br.pkgList);
                        pw.println(stringBuilder.toString());
                    }
                }
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpPrev(PrintWriter pw) {
        if (pw != null) {
            if (mEnabled) {
                StringBuilder stringBuilder;
                int i = 0;
                Set<String> pkgList = getPackageNameForUid(this.mPrevNonHomeBase.mUid, isDealAsPkgUid(this.mPrevNonHomeBase.mUid) ? this.mPrevNonHomeBase.mPid : 0);
                String eclipseTime = "";
                if (this.mPrevNonHomeBase.mUid == 0) {
                    eclipseTime = " none";
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" ");
                    stringBuilder.append((SystemClock.elapsedRealtime() - this.mPrevNonHomeBase.mTime) / 1000);
                    eclipseTime = stringBuilder.toString();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("[Prev Non Home] Uid:");
                stringBuilder.append(this.mPrevNonHomeBase.mUid);
                stringBuilder.append(",pid:");
                stringBuilder.append(this.mPrevNonHomeBase.mPid);
                stringBuilder.append(",pkg:");
                stringBuilder.append(pkgList);
                stringBuilder.append(",eclipse(s):");
                stringBuilder.append(eclipseTime);
                pw.println(stringBuilder.toString());
                boolean isRecentTaskShow = false;
                synchronized (this.mForePids) {
                    if (this.mForePids.isEmpty()) {
                        isRecentTaskShow = true;
                    }
                }
                if (isRecentTaskShow) {
                    pkgList = getPackageNameForUid(this.mRecentTaskPrevBase.mUid, isDealAsPkgUid(this.mRecentTaskPrevBase.mUid) ? this.mRecentTaskPrevBase.mPid : 0);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[Prev Recent Task] Uid:");
                    stringBuilder2.append(this.mRecentTaskPrevBase.mUid);
                    stringBuilder2.append(",pid:");
                    stringBuilder2.append(this.mRecentTaskPrevBase.mPid);
                    stringBuilder2.append(",pkg:");
                    stringBuilder2.append(pkgList);
                    pw.println(stringBuilder2.toString());
                } else {
                    pw.println("[Prev Recent Task] Uid: None");
                }
                if (isDealAsPkgUid(this.mAmsPrevBase.mUid)) {
                    i = this.mAmsPrevBase.mPid;
                }
                Set<String> pkgList2 = getPackageNameForUid(this.mAmsPrevBase.mUid, i);
                if (this.mAmsPrevBase.mUid == 0) {
                    pkgList = " none";
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" ");
                    stringBuilder3.append((SystemClock.elapsedRealtime() - this.mAmsPrevBase.mTime) / 1000);
                    pkgList = stringBuilder3.toString();
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("[Prev By Ams] Uid:");
                stringBuilder4.append(this.mAmsPrevBase.mUid);
                stringBuilder4.append(",pid:");
                stringBuilder4.append(this.mAmsPrevBase.mPid);
                stringBuilder4.append(",pkg:");
                stringBuilder4.append(pkgList2);
                stringBuilder4.append(",eclipse(s):");
                stringBuilder4.append(pkgList);
                pw.println(stringBuilder4.toString());
                return;
            }
            pw.println("AwareAppAssociate feature disabled.");
        }
    }

    public void dumpRecord(PrintWriter pw) {
        Throwable th;
        PrintWriter printWriter = pw;
        if (printWriter != null) {
            if (mEnabled) {
                Set<String> widgets = getWidgetsPkg();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Widget Size: ");
                stringBuilder.append(widgets.size());
                printWriter.println(stringBuilder.toString());
                ArraySet<Integer> windows = new ArraySet();
                ArraySet<Integer> windowsEvil = new ArraySet();
                getVisibleWindows(windows, windowsEvil);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Window Size: ");
                stringBuilder.append(windows.size());
                stringBuilder.append(", EvilWindow Size: ");
                stringBuilder.append(windowsEvil.size());
                printWriter.println(stringBuilder.toString());
                synchronized (this) {
                    Set<String> widgets2;
                    ArraySet<Integer> windows2;
                    ArraySet<Integer> windowsEvil2;
                    try {
                        int pidSize = this.mAssocRecordMap.size();
                        int compSize = 0;
                        int pidsize = 0;
                        int s = 0;
                        while (s < pidSize) {
                            int providerSize = 0;
                            int providerSizeAll = 0;
                            int sameuid = 0;
                            try {
                                AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.valueAt(s);
                                widgets2 = widgets;
                                try {
                                    int NP;
                                    int NB;
                                    int NP2 = record.mAssocBindService.getMap().size();
                                    windows2 = windows;
                                    int bindSizeAll = 0;
                                    int bindSize = 0;
                                    int i = 0;
                                    while (i < NP2) {
                                        NP = NP2;
                                        try {
                                            NP2 = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                                            windowsEvil2 = windowsEvil;
                                            NB = NP2.size();
                                            bindSize += NB;
                                            int bindSizeAll2 = bindSizeAll;
                                            bindSizeAll = 0;
                                            while (bindSizeAll < NB) {
                                                bindSizeAll2 += ((AssocBaseRecord) NP2.valueAt(bindSizeAll)).mComponents.size();
                                                bindSizeAll++;
                                                NP2 = NP2;
                                                NB = NB;
                                            }
                                            i++;
                                            NP2 = NP;
                                            bindSizeAll = bindSizeAll2;
                                            windowsEvil = windowsEvil2;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    NP = NP2;
                                    windowsEvil2 = windowsEvil;
                                    NP2 = record.mAssocProvider.getMap().size();
                                    NB = 0;
                                    while (NB < NP2) {
                                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocProvider.getMap().valueAt(NB);
                                        int NP3 = NP2;
                                        NP2 = brs.size();
                                        providerSize += NP2;
                                        NP = providerSizeAll;
                                        providerSizeAll = 0;
                                        while (providerSizeAll < NP2) {
                                            NP += ((AssocBaseRecord) brs.valueAt(providerSizeAll)).mComponents.size();
                                            providerSizeAll++;
                                            NP2 = NP2;
                                            brs = brs;
                                        }
                                        NB++;
                                        providerSizeAll = NP;
                                        NP2 = NP3;
                                    }
                                    ArraySet<Integer> pids = (ArraySet) this.mProcUidMap.get(Integer.valueOf(record.uid));
                                    if (pids != null) {
                                        sameuid = pids.size() - 1;
                                    }
                                    NB = bindSize + providerSize;
                                    i = bindSizeAll + providerSizeAll;
                                    int curpiduidsize = NB + sameuid;
                                    compSize += i;
                                    int pidsize2 = pidsize + NB;
                                    try {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("[");
                                        stringBuilder2.append(record.uid);
                                        stringBuilder2.append("][");
                                        stringBuilder2.append(record.processName);
                                        stringBuilder2.append("]: bind[");
                                        stringBuilder2.append(bindSize);
                                        stringBuilder2.append("-");
                                        stringBuilder2.append(bindSizeAll);
                                        stringBuilder2.append("]provider[");
                                        stringBuilder2.append(providerSize);
                                        stringBuilder2.append("-");
                                        stringBuilder2.append(providerSizeAll);
                                        stringBuilder2.append("]SameUID[");
                                        stringBuilder2.append(sameuid);
                                        stringBuilder2.append("]pids:[");
                                        stringBuilder2.append(NB);
                                        stringBuilder2.append("]comps:[");
                                        stringBuilder2.append(i);
                                        stringBuilder2.append("]piduids:[");
                                        stringBuilder2.append(curpiduidsize);
                                        stringBuilder2.append("]");
                                        printWriter.println(stringBuilder2.toString());
                                        s++;
                                        widgets = widgets2;
                                        windows = windows2;
                                        windowsEvil = windowsEvil2;
                                        pidsize = pidsize2;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        pidsize = compSize;
                                        s = pidsize2;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    windows2 = windows;
                                    windowsEvil2 = windowsEvil;
                                    s = pidsize;
                                    pidsize = compSize;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                widgets2 = widgets;
                                windows2 = windows;
                                windowsEvil2 = windowsEvil;
                                s = pidsize;
                                pidsize = compSize;
                            }
                        }
                        widgets2 = widgets;
                        windows2 = windows;
                        windowsEvil2 = windowsEvil;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("PidRecord Size: ");
                        stringBuilder.append(pidSize);
                        stringBuilder.append(" ");
                        stringBuilder.append(pidsize);
                        stringBuilder.append(" ");
                        stringBuilder.append(compSize);
                        printWriter.println(stringBuilder.toString());
                        return;
                    } catch (Throwable th6) {
                        th = th6;
                        widgets2 = widgets;
                        windows2 = windows;
                        windowsEvil2 = windowsEvil;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        }
                        throw th;
                    }
                }
            }
            printWriter.println("AwareAppAssociate feature disabled.");
        }
    }

    private void recordWindowDetail(Set<Integer> list) {
        if (list != null && !list.isEmpty()) {
            synchronized (this) {
                for (Integer pid : list) {
                    AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid.intValue()));
                    if (br != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[Window]Pid:");
                        stringBuilder.append(br.pid);
                        stringBuilder.append(",Uid:");
                        stringBuilder.append(br.uid);
                        stringBuilder.append(",ProcessName:");
                        stringBuilder.append(br.processName);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private void recordAssocDetail(int pid) {
        synchronized (this) {
            AssocPidRecord record = (AssocPidRecord) this.mAssocRecordMap.get(Integer.valueOf(pid));
            if (record != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(record);
                AwareLog.i(str, stringBuilder.toString());
            } else {
                String str2;
                StringBuilder stringBuilder2;
                AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.get(Integer.valueOf(pid));
                if (br != null) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Pid:");
                    stringBuilder2.append(br.pid);
                    stringBuilder2.append(",Uid:");
                    stringBuilder2.append(br.uid);
                    stringBuilder2.append(",ProcessName:");
                    stringBuilder2.append(br.processName);
                    AwareLog.i(str2, stringBuilder2.toString());
                }
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("");
                stringBuilder2.append(sameUid(pid));
                AwareLog.i(str2, stringBuilder2.toString());
            }
        }
    }

    public static void enable() {
        mEnabled = true;
        if (mAwareAppAssociate != null) {
            mAwareAppAssociate.initialize();
        }
    }

    public static void disable() {
        mEnabled = false;
        if (mAwareAppAssociate != null) {
            mAwareAppAssociate.deInitialize();
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static void enableRecord() {
        RECORD = true;
    }

    public static void disableRecord() {
        RECORD = false;
    }

    public boolean isPkgHasProc(String pkg) {
        boolean z = true;
        if (!mEnabled) {
            return true;
        }
        synchronized (this.mProcPkgMap) {
            if (this.mProcPkgMap.get(pkg) == null) {
                z = false;
            }
        }
        return z;
    }

    public void registerVisibleCallback(IAwareVisibleCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (!this.mCallbacks.contains(callback)) {
                    this.mCallbacks.add(callback);
                }
            }
        }
    }

    public void unregisterVisibleCallback(IAwareVisibleCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (this.mCallbacks.contains(callback)) {
                    this.mCallbacks.remove(callback);
                }
            }
        }
    }

    public void notifyVisibleWindowsChange(int type, int window, int mode) {
        synchronized (this.mCallbacks) {
            if (this.mCallbacks.isEmpty()) {
                return;
            }
            int callbackSize = this.mCallbacks.size();
            for (int i = 0; i < callbackSize; i++) {
                ((IAwareVisibleCallback) this.mCallbacks.valueAt(i)).onVisibleWindowsChanged(type, window, mode);
            }
        }
    }

    public void screenStateChange(boolean screenOff) {
        this.mScreenOff = screenOff;
        if (screenOff && this.mHandler != null) {
            this.mHandler.removeMessages(4);
        }
    }

    private void clearRemoveVisWinDurScreenOff() {
        synchronized (this.mVisWinDurScreenOff) {
            if (!this.mVisWinDurScreenOff.isEmpty()) {
                this.mVisWinDurScreenOff.clear();
            }
        }
    }

    public void checkBakUpVisWin() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(4);
            this.mHandler.sendEmptyMessageDelayed(4, 5000);
        }
    }

    private void updateVisibleWindowsCache(int type, int pid, int mode, String pkg, int uid, int code, boolean evil) {
        AwareProcessWindowInfo winInfo;
        switch (type) {
            case 0:
                AwareProcessWindowInfo winInfoCache = new AwareProcessWindowInfo(mode, pkg, uid);
                synchronized (this.mVisibleWindowsCache) {
                    this.mVisibleWindowsCache.put(Integer.valueOf(pid), winInfoCache);
                }
                return;
            case 1:
                synchronized (this.mVisibleWindowsCache) {
                    this.mVisibleWindowsCache.remove(Integer.valueOf(pid));
                }
                return;
            case 2:
                synchronized (this.mVisibleWindowsCache) {
                    this.mVisibleWindowsCache.clear();
                }
                return;
            case 3:
                synchronized (this.mVisibleWindowsCache) {
                    winInfo = (AwareProcessWindowInfo) this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                    if (winInfo != null) {
                        winInfo.mMode = mode;
                    }
                }
                return;
            case 4:
                synchronized (this.mVisibleWindowsCache) {
                    winInfo = (AwareProcessWindowInfo) this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                    if (winInfo != null) {
                        winInfo.addWindow(Integer.valueOf(code), evil);
                    }
                }
                return;
            case 5:
                synchronized (this.mVisibleWindowsCache) {
                    winInfo = (AwareProcessWindowInfo) this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                    if (winInfo != null) {
                        winInfo.removeWindow(Integer.valueOf(code));
                    }
                }
                return;
            default:
                return;
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0030, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isVisibleWindow(int pid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mVisibleWindowsCache) {
            AwareProcessWindowInfo winInfo = (AwareProcessWindowInfo) this.mVisibleWindowsCache.get(Integer.valueOf(pid));
            if (winInfo != null) {
                boolean allowedWindow = winInfo.mMode == 0 || winInfo.mMode == 3;
                if (allowedWindow && !winInfo.isEvil()) {
                    return true;
                }
            }
        }
    }
}
