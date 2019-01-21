package com.android.server.rms.iaware.memory.policy;

import android.os.Debug;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.rms.memrepair.ProcStateStatisData;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemTrimPolicy {
    private static final String TAG = "AwareMem_SysTrim";
    private static Object lock = new Object();
    private static final AtomicBoolean mIsFeatureEnable = new AtomicBoolean(false);
    private static SystemTrimPolicy mSystemTrimPolicy = null;
    private Map<String, Long> mProcThreshold = new ArrayMap();

    private SystemTrimPolicy() {
    }

    public static SystemTrimPolicy getInstance() {
        SystemTrimPolicy systemTrimPolicy;
        synchronized (lock) {
            if (mSystemTrimPolicy == null) {
                mSystemTrimPolicy = new SystemTrimPolicy();
            }
            systemTrimPolicy = mSystemTrimPolicy;
        }
        return systemTrimPolicy;
    }

    public void enable() {
        if (mIsFeatureEnable.get()) {
            AwareLog.d(TAG, "SystemTrimPolicy has already enable!");
        } else {
            mIsFeatureEnable.set(true);
        }
    }

    public void disable() {
        if (mIsFeatureEnable.get()) {
            mIsFeatureEnable.set(false);
        } else {
            AwareLog.d(TAG, "SystemTrimPolicy has already disable!");
        }
    }

    public void updateProcThreshold(String packageName, long threshold) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set sys app for trim, name=");
        stringBuilder.append(packageName);
        stringBuilder.append(", threshold=");
        stringBuilder.append(threshold);
        AwareLog.d(str, stringBuilder.toString());
        synchronized (this.mProcThreshold) {
            this.mProcThreshold.put(packageName, Long.valueOf(threshold));
        }
    }

    /* JADX WARNING: Missing block: B:13:0x002d, code skipped:
            if (r14 == null) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:15:0x0033, code skipped:
            if (r14.isEmpty() == false) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            r0 = new android.util.ArraySet();
            r1 = new android.util.ArraySet();
            r2 = new android.util.ArrayMap();
            r3 = r14.iterator();
     */
    /* JADX WARNING: Missing block: B:18:0x004e, code skipped:
            if (r3.hasNext() == false) goto L_0x0107;
     */
    /* JADX WARNING: Missing block: B:19:0x0050, code skipped:
            r4 = (com.android.server.mtm.iaware.appmng.AwareProcessInfo) r3.next();
     */
    /* JADX WARNING: Missing block: B:20:0x0057, code skipped:
            if (r4 == null) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:22:0x005b, code skipped:
            if (r4.mProcInfo == null) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:24:0x0061, code skipped:
            if (r4.mProcInfo.mPackageName == null) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:26:0x006b, code skipped:
            if (r4.mProcInfo.mPackageName.isEmpty() == false) goto L_0x006e;
     */
    /* JADX WARNING: Missing block: B:28:0x006e, code skipped:
            r5 = (java.lang.String) r4.mProcInfo.mPackageName.get(0);
     */
    /* JADX WARNING: Missing block: B:29:0x007e, code skipped:
            if (android.text.TextUtils.isEmpty(r5) == false) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:31:0x0081, code skipped:
            r8 = r13.mProcThreshold;
     */
    /* JADX WARNING: Missing block: B:32:0x0085, code skipped:
            monitor-enter(r8);
     */
    /* JADX WARNING: Missing block: B:35:0x008c, code skipped:
            if (r13.mProcThreshold.containsKey(r5) != false) goto L_0x0090;
     */
    /* JADX WARNING: Missing block: B:36:0x008e, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:38:0x0090, code skipped:
            r6 = ((java.lang.Long) r13.mProcThreshold.get(r5)).longValue();
     */
    /* JADX WARNING: Missing block: B:39:0x009d, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:41:0x00a2, code skipped:
            if (r1.contains(r5) == false) goto L_0x00c1;
     */
    /* JADX WARNING: Missing block: B:42:0x00a4, code skipped:
            r0.add(r4.mProcInfo);
            r8 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("the proc which need to trim is in the list, pkg=");
            r9.append(r5);
            android.rms.iaware.AwareLog.d(r8, r9.toString());
     */
    /* JADX WARNING: Missing block: B:43:0x00c1, code skipped:
            r8 = getPss(r4.mProcInfo);
     */
    /* JADX WARNING: Missing block: B:44:0x00cb, code skipped:
            if (r2.containsKey(r5) == false) goto L_0x00d8;
     */
    /* JADX WARNING: Missing block: B:45:0x00cd, code skipped:
            r8 = r8 + ((java.lang.Long) r2.get(r5)).longValue();
     */
    /* JADX WARNING: Missing block: B:46:0x00d8, code skipped:
            r2.put(r5, java.lang.Long.valueOf(r8));
     */
    /* JADX WARNING: Missing block: B:47:0x00e1, code skipped:
            if (r8 <= r6) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:48:0x00e3, code skipped:
            r1.add(r5);
            r0.add(r4.mProcInfo);
            r10 = TAG;
            r11 = new java.lang.StringBuilder();
            r11.append("the proc need to trim, pkg=");
            r11.append(r5);
            android.rms.iaware.AwareLog.d(r10, r11.toString());
     */
    /* JADX WARNING: Missing block: B:54:0x0107, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:55:0x0108, code skipped:
            android.rms.iaware.AwareLog.w(TAG, "no proc need to trim");
     */
    /* JADX WARNING: Missing block: B:56:0x010f, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Set<ProcessInfo> getProcNeedTrim(List<AwareProcessInfo> procsGroups) {
        AwareLog.d(TAG, "SystemTrimPolicy.getProcNeedTrim enter");
        if (mIsFeatureEnable.get()) {
            synchronized (this.mProcThreshold) {
                if (this.mProcThreshold.isEmpty()) {
                    AwareLog.w(TAG, "no app proc need to trim");
                    return null;
                }
            }
        }
        AwareLog.d(TAG, "SystemTrimPolicy has already disable!");
        return null;
    }

    private long getPss(ProcessInfo procInfo) {
        if (procInfo == null) {
            return 0;
        }
        long pss = 0;
        int pid = procInfo.mPid;
        int uid = procInfo.mUid;
        if (checkPidValid(pid, uid)) {
            pss = ProcStateStatisData.getInstance().getProcPss(uid, pid);
            if (pss <= 0) {
                pss = Debug.getPss(pid, null, null);
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the proc , pid=");
        stringBuilder.append(pid);
        stringBuilder.append(", uid=");
        stringBuilder.append(uid);
        stringBuilder.append(", pss=");
        stringBuilder.append(pss);
        AwareLog.d(str, stringBuilder.toString());
        return pss;
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
            } catch (UnsupportedOperationException e2) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("read status of ");
                stringBuilder2.append(pid);
                stringBuilder2.append(" failed");
                AwareLog.w(str, stringBuilder2.toString());
                return false;
            } catch (IllegalArgumentException e3) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("read status of ");
                stringBuilder2.append(pid);
                stringBuilder2.append(" failed");
                AwareLog.w(str, stringBuilder2.toString());
                return false;
            } catch (SecurityException e4) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("read status of ");
                stringBuilder2.append(pid);
                stringBuilder2.append(" failed");
                AwareLog.w(str, stringBuilder2.toString());
                return false;
            } catch (Exception e5) {
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
}
