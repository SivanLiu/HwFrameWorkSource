package com.android.server.rms.memrepair;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IProcessObserver.Stub;
import android.os.RemoteException;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
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

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    public void addPssToMap(java.lang.String r23, int r24, int r25, int r26, long r27, long r29, boolean r31) {
        /*
        r22 = this;
        r1 = r22;
        r2 = r23;
        r3 = r24;
        r4 = r25;
        r5 = r26;
        r14 = r27;
        r13 = r31;
        r0 = r1.mEnabled;
        if (r0 != 0) goto L_0x001a;
    L_0x0012:
        r0 = "AwareMem_PSSData";
        r6 = "not enabled";
        android.rms.iaware.AwareLog.d(r0, r6);
        return;
    L_0x001a:
        if (r2 != 0) goto L_0x0024;
    L_0x001c:
        r0 = "AwareMem_PSSData";
        r6 = "addPssToMap: procName is null!";
        android.rms.iaware.AwareLog.d(r0, r6);
        return;
    L_0x0024:
        r0 = "AwareMem_PSSData";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "procName=";
        r6.append(r7);
        r6.append(r2);
        r7 = ";uid=";
        r6.append(r7);
        r6.append(r3);
        r7 = ";pid=";
        r6.append(r7);
        r6.append(r4);
        r7 = ";procState=";
        r6.append(r7);
        r6.append(r5);
        r7 = ";pss=";
        r6.append(r7);
        r6.append(r14);
        r7 = ";test=";
        r6.append(r7);
        r6.append(r13);
        r6 = r6.toString();
        android.rms.iaware.AwareLog.d(r0, r6);
        r0 = 2;
        if (r5 == r0) goto L_0x006e;
    L_0x0065:
        r0 = 11;
        if (r5 != r0) goto L_0x006a;
    L_0x0069:
        goto L_0x006e;
    L_0x006a:
        r0 = 1;
        r1.customProcessState = r0;
        goto L_0x0071;
    L_0x006e:
        r0 = 0;
        r1.customProcessState = r0;
    L_0x0071:
        r0 = r1.customProcessState;
        r0 = r1.isValidProcState(r0);
        if (r0 != 0) goto L_0x007a;
    L_0x0079:
        return;
    L_0x007a:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r0.append(r3);
        r6 = "|";
        r0.append(r6);
        r0.append(r4);
        r11 = r0.toString();
        if (r13 == 0) goto L_0x0098;
    L_0x0091:
        r0 = r1.mTestIntervalTime;
        r6 = r1.customProcessState;
        r6 = r0[r6];
        goto L_0x009e;
    L_0x0098:
        r0 = r1.mIntervalTime;
        r6 = r1.customProcessState;
        r6 = r0[r6];
    L_0x009e:
        r9 = r6;
        r6 = r1.mPssMap;
        monitor-enter(r6);
        r0 = r1.mPssMap;	 Catch:{ all -> 0x0211 }
        r7 = java.lang.Long.valueOf(r27);	 Catch:{ all -> 0x0211 }
        r0.put(r11, r7);	 Catch:{ all -> 0x0211 }
        monitor-exit(r6);	 Catch:{ all -> 0x0211 }
        r12 = r1.mLock;
        monitor-enter(r12);
        r0 = r1.mPssListMap;	 Catch:{ all -> 0x0207 }
        r0 = r0.containsKey(r11);	 Catch:{ all -> 0x0207 }
        if (r0 == 0) goto L_0x01a4;
    L_0x00b7:
        r0 = "AwareMem_PSSData";	 Catch:{ all -> 0x019c }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x019c }
        r6.<init>();	 Catch:{ all -> 0x019c }
        r7 = "addPssToMap=";	 Catch:{ all -> 0x019c }
        r6.append(r7);	 Catch:{ all -> 0x019c }
        r6.append(r11);	 Catch:{ all -> 0x019c }
        r6 = r6.toString();	 Catch:{ all -> 0x019c }
        android.rms.iaware.AwareLog.d(r0, r6);	 Catch:{ all -> 0x019c }
        r0 = r1.mPssListMap;	 Catch:{ all -> 0x019c }
        r0 = r0.get(r11);	 Catch:{ all -> 0x019c }
        r0 = (java.util.List) r0;	 Catch:{ all -> 0x019c }
        r6 = 0;	 Catch:{ all -> 0x019c }
        r7 = r0.iterator();	 Catch:{ all -> 0x019c }
        r8 = r6;	 Catch:{ all -> 0x019c }
    L_0x00db:
        r6 = r7.hasNext();	 Catch:{ all -> 0x019c }
        if (r6 == 0) goto L_0x014c;	 Catch:{ all -> 0x019c }
    L_0x00e1:
        r6 = r7.next();	 Catch:{ all -> 0x019c }
        r6 = (com.android.server.rms.memrepair.ProcStateData) r6;	 Catch:{ all -> 0x019c }
        r3 = r6.getState();	 Catch:{ all -> 0x019c }
        r5 = r1.customProcessState;	 Catch:{ all -> 0x019c }
        if (r3 != r5) goto L_0x0133;	 Catch:{ all -> 0x019c }
    L_0x00ef:
        r3 = r6.getProcName();	 Catch:{ all -> 0x019c }
        r3 = r2.equals(r3);	 Catch:{ all -> 0x019c }
        if (r3 == 0) goto L_0x0133;	 Catch:{ all -> 0x019c }
    L_0x00f9:
        r3 = "AwareMem_PSSData";	 Catch:{ all -> 0x019c }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x019c }
        r5.<init>();	 Catch:{ all -> 0x019c }
        r16 = r6;	 Catch:{ all -> 0x019c }
        r6 = "processState=";	 Catch:{ all -> 0x019c }
        r5.append(r6);	 Catch:{ all -> 0x019c }
        r6 = r1.customProcessState;	 Catch:{ all -> 0x019c }
        r5.append(r6);	 Catch:{ all -> 0x019c }
        r5 = r5.toString();	 Catch:{ all -> 0x019c }
        android.rms.iaware.AwareLog.d(r3, r5);	 Catch:{ all -> 0x019c }
        r3 = r1.mCollectCounts;	 Catch:{ all -> 0x019c }
        r5 = r1.customProcessState;	 Catch:{ all -> 0x019c }
        r3 = r3[r5];	 Catch:{ all -> 0x019c }
        r5 = r16;
        r6 = r5;
        r17 = r5;
        r16 = r7;
        r5 = r8;
        r7 = r14;
        r18 = r9;
        r9 = r29;
        r20 = r11;
        r21 = r12;
        r11 = r18;
        r13 = r3;
        r6.addPssToList(r7, r9, r11, r13);	 Catch:{ all -> 0x018f }
        r3 = 1;	 Catch:{ all -> 0x018f }
        r8 = r3;	 Catch:{ all -> 0x018f }
        goto L_0x013d;	 Catch:{ all -> 0x018f }
    L_0x0133:
        r16 = r7;	 Catch:{ all -> 0x018f }
        r5 = r8;	 Catch:{ all -> 0x018f }
        r18 = r9;	 Catch:{ all -> 0x018f }
        r20 = r11;	 Catch:{ all -> 0x018f }
        r21 = r12;	 Catch:{ all -> 0x018f }
        r8 = r5;	 Catch:{ all -> 0x018f }
    L_0x013d:
        r13 = r31;	 Catch:{ all -> 0x018f }
        r7 = r16;	 Catch:{ all -> 0x018f }
        r9 = r18;	 Catch:{ all -> 0x018f }
        r11 = r20;	 Catch:{ all -> 0x018f }
        r12 = r21;	 Catch:{ all -> 0x018f }
        r3 = r24;	 Catch:{ all -> 0x018f }
        r5 = r26;	 Catch:{ all -> 0x018f }
        goto L_0x00db;	 Catch:{ all -> 0x018f }
    L_0x014c:
        r5 = r8;	 Catch:{ all -> 0x018f }
        r18 = r9;	 Catch:{ all -> 0x018f }
        r20 = r11;	 Catch:{ all -> 0x018f }
        r21 = r12;	 Catch:{ all -> 0x018f }
        if (r5 != 0) goto L_0x0196;	 Catch:{ all -> 0x018f }
    L_0x0155:
        r3 = "AwareMem_PSSData";	 Catch:{ all -> 0x018f }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x018f }
        r6.<init>();	 Catch:{ all -> 0x018f }
        r7 = "processState=";	 Catch:{ all -> 0x018f }
        r6.append(r7);	 Catch:{ all -> 0x018f }
        r7 = r1.customProcessState;	 Catch:{ all -> 0x018f }
        r6.append(r7);	 Catch:{ all -> 0x018f }
        r7 = "exist=";	 Catch:{ all -> 0x018f }
        r6.append(r7);	 Catch:{ all -> 0x018f }
        r6.append(r5);	 Catch:{ all -> 0x018f }
        r6 = r6.toString();	 Catch:{ all -> 0x018f }
        android.rms.iaware.AwareLog.d(r3, r6);	 Catch:{ all -> 0x018f }
        r3 = new com.android.server.rms.memrepair.ProcStateData;	 Catch:{ all -> 0x018f }
        r6 = r1.customProcessState;	 Catch:{ all -> 0x018f }
        r3.<init>(r4, r2, r6);	 Catch:{ all -> 0x018f }
        r6 = r1.mCollectCounts;	 Catch:{ all -> 0x018f }
        r7 = r1.customProcessState;	 Catch:{ all -> 0x018f }
        r13 = r6[r7];	 Catch:{ all -> 0x018f }
        r6 = r3;	 Catch:{ all -> 0x018f }
        r7 = r14;	 Catch:{ all -> 0x018f }
        r9 = r29;	 Catch:{ all -> 0x018f }
        r11 = r18;	 Catch:{ all -> 0x018f }
        r6.addPssToList(r7, r9, r11, r13);	 Catch:{ all -> 0x018f }
        r0.add(r3);	 Catch:{ all -> 0x018f }
        goto L_0x0196;
    L_0x018f:
        r0 = move-exception;
        r16 = r18;
        r5 = r20;
        goto L_0x020d;
        r16 = r18;
        r5 = r20;
        goto L_0x01f7;
    L_0x019c:
        r0 = move-exception;
        r21 = r12;
        r16 = r9;
        r5 = r11;
        goto L_0x020d;
    L_0x01a4:
        r18 = r9;
        r20 = r11;
        r21 = r12;
        r0 = "AwareMem_PSSData";	 Catch:{ all -> 0x0201 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0201 }
        r3.<init>();	 Catch:{ all -> 0x0201 }
        r5 = "else addPssToMap=";	 Catch:{ all -> 0x0201 }
        r3.append(r5);	 Catch:{ all -> 0x0201 }
        r5 = r20;
        r3.append(r5);	 Catch:{ all -> 0x01fd }
        r6 = ";pss=";	 Catch:{ all -> 0x01fd }
        r3.append(r6);	 Catch:{ all -> 0x01fd }
        r3.append(r14);	 Catch:{ all -> 0x01fd }
        r6 = ";interval=";	 Catch:{ all -> 0x01fd }
        r3.append(r6);	 Catch:{ all -> 0x01fd }
        r11 = r18;
        r3.append(r11);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r3 = r3.toString();	 Catch:{ all -> 0x01f9, all -> 0x020f }
        android.rms.iaware.AwareLog.d(r0, r3);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r0 = new java.util.ArrayList;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r0.<init>();	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r3 = new com.android.server.rms.memrepair.ProcStateData;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r6 = r1.customProcessState;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r3.<init>(r4, r2, r6);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r6 = r1.mCollectCounts;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r7 = r1.customProcessState;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r13 = r6[r7];	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r6 = r3;
        r7 = r14;
        r9 = r29;
        r16 = r11;
        r6.addPssToList(r7, r9, r11, r13);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r0.add(r3);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r6 = r1.mPssListMap;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r6.put(r5, r0);	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x01f7:
        monitor-exit(r21);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        return;	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x01f9:
        r0 = move-exception;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r16 = r11;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        goto L_0x020d;	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x01fd:
        r0 = move-exception;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r16 = r18;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        goto L_0x020d;	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x0201:
        r0 = move-exception;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r16 = r18;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r5 = r20;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        goto L_0x020d;	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x0207:
        r0 = move-exception;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r16 = r9;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r5 = r11;	 Catch:{ all -> 0x01f9, all -> 0x020f }
        r21 = r12;	 Catch:{ all -> 0x01f9, all -> 0x020f }
    L_0x020d:
        monitor-exit(r21);	 Catch:{ all -> 0x01f9, all -> 0x020f }
        throw r0;
    L_0x020f:
        r0 = move-exception;
        goto L_0x020d;
    L_0x0211:
        r0 = move-exception;
        r16 = r9;
        r5 = r11;
    L_0x0215:
        monitor-exit(r6);	 Catch:{ all -> 0x0217 }
        throw r0;
    L_0x0217:
        r0 = move-exception;
        goto L_0x0215;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.rms.memrepair.ProcStateStatisData.addPssToMap(java.lang.String, int, int, int, long, long, boolean):void");
    }

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
