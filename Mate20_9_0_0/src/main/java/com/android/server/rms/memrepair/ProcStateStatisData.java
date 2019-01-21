package com.android.server.rms.memrepair;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IProcessObserver.Stub;
import android.os.RemoteException;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProcStateStatisData {
    private static final String BACKGROUND = "background";
    private static final int BACKGROUND_STATE = 1;
    public static final int MAX_COUNT = 50;
    public static final int MAX_INTERVAL = 30;
    public static final int MIN_COUNT = 6;
    public static final int MIN_INTERVAL = 2;
    public static final String SEPERATOR_CHAR = "|";
    private static final String TAG = "AwareMem_PSSData";
    private static final String TOP = "top";
    private static final int TOP_STATE = 0;
    private static ProcStateStatisData mProcStateStatisData;
    private int customProcessState = 19;
    private int[] mCollectCounts = new int[]{6, 6};
    private boolean mEnabled = false;
    private long[] mIntervalTime = new long[]{120000, 900000};
    private final Object mLock = new Object();
    private IProcessObserver mProcessObserver = new Stub() {
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
        }

        public void onProcessStateChanged(int pid, int uid, int procState) {
        }

        public void onProcessDied(int pid, int uid) {
            String str = ProcStateStatisData.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProcessDied pid = ");
            stringBuilder.append(pid);
            stringBuilder.append(", uid = ");
            stringBuilder.append(uid);
            AwareLog.d(str, stringBuilder.toString());
            str = new StringBuilder();
            str.append(uid);
            str.append("|");
            str.append(pid);
            str = str.toString();
            synchronized (ProcStateStatisData.this.mLock) {
                ProcStateStatisData.this.mPssListMap.remove(str);
            }
            synchronized (ProcStateStatisData.this.mPssMap) {
                ProcStateStatisData.this.mPssMap.remove(str);
            }
        }
    };
    private Map<String, List<ProcStateData>> mPssListMap = new ArrayMap();
    private Map<String, Long> mPssMap = new ArrayMap();
    private long[] mTestIntervalTime = new long[]{2000, 15000};
    private String[] stateStatus = new String[]{TOP, "background"};

    private ProcStateStatisData() {
    }

    public static ProcStateStatisData getInstance() {
        ProcStateStatisData procStateStatisData;
        synchronized (ProcStateStatisData.class) {
            if (mProcStateStatisData == null) {
                mProcStateStatisData = new ProcStateStatisData();
            }
            procStateStatisData = mProcStateStatisData;
        }
        return procStateStatisData;
    }

    public void setEnable(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable=");
        stringBuilder.append(enable);
        AwareLog.d(str, stringBuilder.toString());
        this.mEnabled = enable;
        if (enable) {
            registerProcessObserver();
        } else {
            unregisterProcessObserver();
        }
    }

    public void updateConfig(int minFgCount, int minBgCount, long fgInterval, long bgInterval) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fgCount=");
        stringBuilder.append(minFgCount);
        stringBuilder.append(",bgCount");
        stringBuilder.append(minBgCount);
        stringBuilder.append(",fgInterval=");
        stringBuilder.append(fgInterval);
        stringBuilder.append(",bgInterval=");
        stringBuilder.append(bgInterval);
        AwareLog.d(str, stringBuilder.toString());
        this.mCollectCounts[0] = minFgCount;
        this.mCollectCounts[1] = minBgCount;
        this.mIntervalTime[0] = fgInterval;
        this.mIntervalTime[1] = bgInterval;
    }

    public int getMinCount(int procState) {
        return isValidProcState(procState) ? this.mCollectCounts[procState] : 0;
    }

    public long getProcPss(int uid, int pid) {
        String procKey = new StringBuilder();
        procKey.append(uid);
        procKey.append("|");
        procKey.append(pid);
        procKey = procKey.toString();
        synchronized (this.mPssMap) {
            Long pss = (Long) this.mPssMap.get(procKey);
            if (pss == null) {
                return 0;
            }
            long longValue = pss.longValue();
            return longValue;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x00e1 A:{Catch:{ all -> 0x019c }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00e1 A:{Catch:{ all -> 0x019c }} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x014c A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00e1 A:{Catch:{ all -> 0x019c }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00e1 A:{Catch:{ all -> 0x019c }} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0155 A:{Catch:{ all -> 0x018f }} */
    /* JADX WARNING: Missing block: B:81:0x0217, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addPssToMap(String procName, int uid, int pid, int procState, long pss, long now, boolean test) {
        Iterator it;
        boolean z;
        Iterator it2;
        long intervalTime;
        String procKey;
        Object obj;
        Object obj2;
        int i;
        boolean isExist;
        boolean isExist2;
        long j;
        Throwable th;
        String str = procName;
        long j2 = uid;
        int i2 = pid;
        int i3 = procState;
        long j3 = pss;
        int i4 = test;
        if (!this.mEnabled) {
            AwareLog.d(TAG, "not enabled");
        } else if (str == null) {
            AwareLog.d(TAG, "addPssToMap: procName is null!");
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("procName=");
            stringBuilder.append(str);
            stringBuilder.append(";uid=");
            stringBuilder.append(j2);
            stringBuilder.append(";pid=");
            stringBuilder.append(i2);
            stringBuilder.append(";procState=");
            stringBuilder.append(i3);
            stringBuilder.append(";pss=");
            stringBuilder.append(j3);
            long j4 = ";test=";
            stringBuilder.append(j4);
            stringBuilder.append(i4);
            AwareLog.d(str2, stringBuilder.toString());
            if (i3 == 2 || i3 == 11) {
                this.customProcessState = 0;
            } else {
                this.customProcessState = 1;
            }
            if (isValidProcState(this.customProcessState)) {
                String str3;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(j2);
                stringBuilder2.append("|");
                stringBuilder2.append(i2);
                long procKey2 = stringBuilder2.toString();
                long intervalTime2 = i4 != false ? this.mTestIntervalTime[this.customProcessState] : this.mIntervalTime[this.customProcessState];
                ProcStateData procStateData = this.mPssMap;
                synchronized (procStateData) {
                    try {
                        this.mPssMap.put(procKey2, Long.valueOf(pss));
                    } finally {
                        procKey2 = 
/*
Method generation error in method: com.android.server.rms.memrepair.ProcStateStatisData.addPssToMap(java.lang.String, int, int, int, long, long, boolean):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r11_7 'procKey2' long) = (r11_6 'procKey2' long), (r3_14 'j2' long) in method: com.android.server.rms.memrepair.ProcStateStatisData.addPssToMap(java.lang.String, int, int, int, long, long, boolean):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeSynchronizedRegion(RegionGen.java:230)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:67)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:130)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 42 more

*/

    public Map<String, List<ProcStateData>> getPssListMap() {
        AwareLog.d(TAG, "enter getPssListMap...");
        Map<String, List<ProcStateData>> cloneMap = new ArrayMap();
        synchronized (this.mLock) {
            cloneMap.putAll(this.mPssListMap);
        }
        return cloneMap;
    }

    public boolean isValidProcState(int procState) {
        return procState >= 0 && procState < this.stateStatus.length;
    }

    public boolean isForgroundState(int procState) {
        return procState == 0;
    }

    private void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.w(TAG, "register process observer failed");
        }
    }

    private void unregisterProcessObserver() {
        try {
            ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
            synchronized (this.mLock) {
                this.mPssListMap.clear();
            }
        } catch (RemoteException e) {
            AwareLog.w(TAG, "unregister process observer failed");
        }
    }
}
