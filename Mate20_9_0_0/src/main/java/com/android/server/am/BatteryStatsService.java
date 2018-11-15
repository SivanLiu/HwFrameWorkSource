package com.android.server.am;

import android.app.ActivityManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.util.NetworkConstants;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.BatteryStats.Uid;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManagerInternal;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiBatteryStats;
import android.os.health.HealthStatsParceler;
import android.os.health.HealthStatsWriter;
import android.os.health.UidHealthStats;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.HwLog;
import android.util.Slog;
import android.util.StatsLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BatteryStatsImpl.PlatformIdleStateCallback;
import com.android.internal.os.BatteryStatsImpl.UserInfoProvider;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.RpmStats;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public final class BatteryStatsService extends Stub implements LowPowerModeListener, PlatformIdleStateCallback {
    static final boolean DBG = false;
    private static final int MAX_LOW_POWER_STATS_SIZE = 2048;
    static final String TAG = "BatteryStatsService";
    private static final int USER_TYPE_CHINA_BETA = 3;
    private static final int USER_TYPE_OVERSEA_BETA = 5;
    private static IBatteryStats sService;
    private ActivityManagerService mActivityManagerService;
    private final Context mContext;
    private CharsetDecoder mDecoderStat = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
    private boolean mIsPowerInfoStatus = false;
    private IHwPowerInfoService mPowerInfoService;
    final BatteryStatsImpl mStats;
    private final UserInfoProvider mUserManagerUserInfoProvider;
    private CharBuffer mUtf16BufferStat = CharBuffer.allocate(2048);
    private ByteBuffer mUtf8BufferStat = ByteBuffer.allocateDirect(2048);
    private final BatteryExternalStatsWorker mWorker;
    private boolean misBetaUser = false;

    private final class LocalService extends BatteryStatsInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(BatteryStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public String[] getWifiIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getWifiIfaces().clone();
        }

        public String[] getMobileIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getMobileIfaces().clone();
        }

        public void noteJobsDeferred(int uid, int numDeferred, long sinceLast) {
            BatteryStatsService.this.noteJobsDeferred(uid, numDeferred, sinceLast);
        }
    }

    final class WakeupReasonThread extends Thread {
        private static final int MAX_REASON_SIZE = 512;
        private CharsetDecoder mDecoder;
        private CharBuffer mUtf16Buffer;
        private ByteBuffer mUtf8Buffer;

        WakeupReasonThread() {
            super("BatteryStats_wakeupReason");
        }

        public void run() {
            Process.setThreadPriority(-2);
            this.mDecoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
            this.mUtf8Buffer = ByteBuffer.allocateDirect(512);
            this.mUtf16Buffer = CharBuffer.allocate(512);
            while (true) {
                try {
                    String waitWakeup = waitWakeup();
                    String reason = waitWakeup;
                    if (waitWakeup != null) {
                        synchronized (BatteryStatsService.this.mStats) {
                            BatteryStatsService.this.mStats.noteWakeupReasonLocked(reason);
                            HwServiceFactory.reportSysWakeUp(reason);
                        }
                        if (BatteryStatsService.this.misBetaUser && BatteryStatsService.this.mPowerInfoService != null) {
                            synchronized (BatteryStatsService.this.mPowerInfoService) {
                                BatteryStatsService.this.mPowerInfoService.notePowerInfoWakeupReason(reason);
                            }
                        }
                    } else {
                        return;
                    }
                } catch (RuntimeException e) {
                    Slog.e(BatteryStatsService.TAG, "Failure reading wakeup reasons", e);
                    return;
                }
            }
            while (true) {
            }
        }

        private String waitWakeup() {
            this.mUtf8Buffer.clear();
            this.mUtf16Buffer.clear();
            this.mDecoder.reset();
            int bytesWritten = BatteryStatsService.nativeWaitWakeup(this.mUtf8Buffer);
            if (bytesWritten < 0) {
                return null;
            }
            if (bytesWritten == 0) {
                return Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            this.mUtf8Buffer.limit(bytesWritten);
            this.mDecoder.decode(this.mUtf8Buffer, this.mUtf16Buffer, true);
            this.mUtf16Buffer.flip();
            return this.mUtf16Buffer.toString();
        }
    }

    private native void getLowPowerStats(RpmStats rpmStats);

    private native int getPlatformLowPowerStats(ByteBuffer byteBuffer);

    private native int getSubsystemLowPowerStats(ByteBuffer byteBuffer);

    private static native int nativeWaitWakeup(ByteBuffer byteBuffer);

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
    protected void dump(java.io.FileDescriptor r34, java.io.PrintWriter r35, java.lang.String[] r36) {
        /*
        r33 = this;
        r1 = r33;
        r9 = r35;
        r10 = r36;
        r0 = r1.mContext;
        r2 = "BatteryStatsService";
        r0 = com.android.internal.util.DumpUtils.checkDumpAndUsageStatsPermission(r0, r2, r9);
        if (r0 != 0) goto L_0x0011;
    L_0x0010:
        return;
    L_0x0011:
        r0 = 0;
        r2 = 0;
        r3 = 0;
        r4 = 0;
        r5 = 0;
        r6 = 0;
        r7 = -1;
        r11 = -1;
        if (r10 == 0) goto L_0x020b;
    L_0x001c:
        r14 = r7;
        r7 = r6;
        r6 = r4;
        r4 = r3;
        r3 = r2;
        r2 = r0;
        r0 = 0;
    L_0x0023:
        r8 = r0;
        r0 = r10.length;
        if (r8 >= r0) goto L_0x0201;
    L_0x0027:
        r13 = r10[r8];
        r0 = "--checkin";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0035;
    L_0x0031:
        r3 = 1;
        r6 = 1;
        goto L_0x01a6;
    L_0x0035:
        r0 = "--history";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0041;
    L_0x003d:
        r2 = r2 | 8;
        goto L_0x01a6;
    L_0x0041:
        r0 = "--history-start";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0062;
    L_0x0049:
        r2 = r2 | 8;
        r8 = r8 + 1;
        r0 = r10.length;
        if (r8 < r0) goto L_0x0059;
    L_0x0050:
        r0 = "Missing time argument for --history-since";
        r9.println(r0);
        r1.dumpHelp(r9);
        return;
    L_0x0059:
        r0 = r10[r8];
        r14 = java.lang.Long.parseLong(r0);
        r7 = 1;
        goto L_0x01a6;
    L_0x0062:
        r0 = "-c";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x006f;
    L_0x006a:
        r3 = 1;
        r2 = r2 | 16;
        goto L_0x01a6;
    L_0x006f:
        r0 = "--proto";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x007a;
    L_0x0077:
        r4 = 1;
        goto L_0x01a6;
    L_0x007a:
        r0 = "--charged";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0086;
    L_0x0082:
        r2 = r2 | 2;
        goto L_0x01a6;
    L_0x0086:
        r0 = "--daily";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0092;
    L_0x008e:
        r2 = r2 | 4;
        goto L_0x01a6;
    L_0x0092:
        r0 = "--reset";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x00bf;
    L_0x009a:
        r12 = r1.mStats;
        monitor-enter(r12);
        r0 = r1.mStats;	 Catch:{ all -> 0x00b8 }
        r0.resetAllStatsCmdLocked();	 Catch:{ all -> 0x00b8 }
        r0 = "Battery stats reset.";	 Catch:{ all -> 0x00b8 }
        r9.println(r0);	 Catch:{ all -> 0x00b8 }
        r5 = 1;	 Catch:{ all -> 0x00b8 }
        monitor-exit(r12);	 Catch:{ all -> 0x00b8 }
        r0 = r1.mWorker;
        r12 = "dump";
        r19 = r3;
        r3 = 31;
        r0.scheduleSync(r12, r3);
    L_0x00b4:
        r3 = r19;
        goto L_0x01a6;
    L_0x00b8:
        r0 = move-exception;
        r19 = r3;
    L_0x00bb:
        monitor-exit(r12);	 Catch:{ all -> 0x00bd }
        throw r0;
    L_0x00bd:
        r0 = move-exception;
        goto L_0x00bb;
    L_0x00bf:
        r19 = r3;
        r0 = "--write";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x00e3;
    L_0x00c9:
        r0 = "dump";
        r3 = 31;
        r1.syncStats(r0, r3);
        r3 = r1.mStats;
        monitor-enter(r3);
        r0 = r1.mStats;
        r0.writeSyncLocked();
        r0 = "Battery stats written.";
        r9.println(r0);
        r5 = 1;
        monitor-exit(r3);
        goto L_0x00b4;
    L_0x00e0:
        r0 = move-exception;
        monitor-exit(r3);
        throw r0;
    L_0x00e3:
        r0 = "--new-daily";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x00fe;
    L_0x00eb:
        r3 = r1.mStats;
        monitor-enter(r3);
        r0 = r1.mStats;
        r0.recordDailyStatsLocked();
        r0 = "New daily stats written.";
        r9.println(r0);
        r5 = 1;
        monitor-exit(r3);
        goto L_0x00b4;
    L_0x00fb:
        r0 = move-exception;
        monitor-exit(r3);
        throw r0;
    L_0x00fe:
        r0 = "--read-daily";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0119;
    L_0x0106:
        r3 = r1.mStats;
        monitor-enter(r3);
        r0 = r1.mStats;
        r0.readDailyStatsLocked();
        r0 = "Last daily stats read.";
        r9.println(r0);
        r5 = 1;
        monitor-exit(r3);
        goto L_0x00b4;
    L_0x0116:
        r0 = move-exception;
        monitor-exit(r3);
        throw r0;
    L_0x0119:
        r0 = "--enable";
        r0 = r0.equals(r13);
        if (r0 != 0) goto L_0x01e2;
    L_0x0121:
        r0 = "enable";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x012b;
    L_0x0129:
        goto L_0x01e2;
    L_0x012b:
        r0 = "--disable";
        r0 = r0.equals(r13);
        if (r0 != 0) goto L_0x01c3;
    L_0x0133:
        r0 = "disable";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x013d;
    L_0x013b:
        goto L_0x01c3;
    L_0x013d:
        r0 = "-h";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0149;
    L_0x0145:
        r1.dumpHelp(r9);
        return;
    L_0x0149:
        r0 = "--settings";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0155;
    L_0x0151:
        r1.dumpSettings(r9);
        return;
    L_0x0155:
        r0 = "--cpu";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x0161;
    L_0x015d:
        r1.dumpCpuStats(r9);
        return;
    L_0x0161:
        r0 = "-a";
        r0 = r0.equals(r13);
        if (r0 == 0) goto L_0x016d;
    L_0x0169:
        r2 = r2 | 32;
        goto L_0x00b4;
    L_0x016d:
        r0 = r13.length();
        if (r0 <= 0) goto L_0x0194;
    L_0x0173:
        r0 = 0;
        r3 = r13.charAt(r0);
        r0 = 45;
        if (r3 != r0) goto L_0x0194;
    L_0x017c:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r3 = "Unknown option: ";
        r0.append(r3);
        r0.append(r13);
        r0 = r0.toString();
        r9.println(r0);
        r1.dumpHelp(r9);
        return;
    L_0x0194:
        r0 = r1.mContext;	 Catch:{ NameNotFoundException -> 0x01aa }
        r0 = r0.getPackageManager();	 Catch:{ NameNotFoundException -> 0x01aa }
        r3 = android.os.UserHandle.getCallingUserId();	 Catch:{ NameNotFoundException -> 0x01aa }
        r0 = r0.getPackageUidAsUser(r13, r3);	 Catch:{ NameNotFoundException -> 0x01aa }
        r11 = r0;
        goto L_0x00b4;
    L_0x01a6:
        r0 = 1;
        r0 = r0 + r8;
        goto L_0x0023;
    L_0x01aa:
        r0 = move-exception;
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r12 = "Unknown package: ";
        r3.append(r12);
        r3.append(r13);
        r3 = r3.toString();
        r9.println(r3);
        r1.dumpHelp(r9);
        return;
    L_0x01c3:
        r0 = 0;
        r0 = r1.doEnableOrDisable(r9, r8, r10, r0);
        if (r0 >= 0) goto L_0x01cb;
    L_0x01ca:
        return;
    L_0x01cb:
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r8 = "Disabled: ";
        r3.append(r8);
        r8 = r10[r0];
        r3.append(r8);
        r3 = r3.toString();
        r9.println(r3);
        return;
    L_0x01e2:
        r0 = 1;
        r0 = r1.doEnableOrDisable(r9, r8, r10, r0);
        if (r0 >= 0) goto L_0x01ea;
    L_0x01e9:
        return;
    L_0x01ea:
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r8 = "Enabled: ";
        r3.append(r8);
        r8 = r10[r0];
        r3.append(r8);
        r3 = r3.toString();
        r9.println(r3);
        return;
    L_0x0201:
        r19 = r3;
        r12 = r4;
        r13 = r6;
        r27 = r14;
        r14 = r7;
        r15 = r11;
        r11 = r5;
        goto L_0x0215;
    L_0x020b:
        r19 = r2;
        r12 = r3;
        r13 = r4;
        r14 = r6;
        r27 = r7;
        r15 = r11;
        r2 = r0;
        r11 = r5;
    L_0x0215:
        if (r11 == 0) goto L_0x0218;
    L_0x0217:
        return;
    L_0x0218:
        r3 = android.os.Binder.clearCallingIdentity();
        r7 = r3;
        r0 = r1.mContext;	 Catch:{ all -> 0x03fc }
        r0 = com.android.internal.os.BatteryStatsHelper.checkWifiOnly(r0);	 Catch:{ all -> 0x03fc }
        if (r0 == 0) goto L_0x0228;	 Catch:{ all -> 0x03fc }
    L_0x0225:
        r0 = r2 | 64;	 Catch:{ all -> 0x03fc }
        r2 = r0;	 Catch:{ all -> 0x03fc }
    L_0x0228:
        r0 = "dump";	 Catch:{ all -> 0x03fc }
        r3 = 31;	 Catch:{ all -> 0x03fc }
        r1.syncStats(r0, r3);	 Catch:{ all -> 0x03fc }
        android.os.Binder.restoreCallingIdentity(r7);
        if (r15 < 0) goto L_0x0240;
    L_0x0235:
        r0 = r2 & 10;
        if (r0 != 0) goto L_0x0240;
    L_0x0239:
        r0 = r2 | 2;
        r0 = r0 & -17;
        r17 = r0;
        goto L_0x0242;
    L_0x0240:
        r17 = r2;
    L_0x0242:
        r0 = 4325376; // 0x420000 float:6.061143E-39 double:2.1370197E-317;
        if (r12 == 0) goto L_0x0304;
    L_0x0246:
        r3 = r1.mContext;
        r3 = r3.getPackageManager();
        r3 = r3.getInstalledApplications(r0);
        if (r13 == 0) goto L_0x02da;
    L_0x0252:
        r0 = r1.mStats;
        r4 = r0.mCheckinFile;
        monitor-enter(r4);
        r0 = r1.mStats;	 Catch:{ all -> 0x02d3 }
        r0 = r0.mCheckinFile;	 Catch:{ all -> 0x02d3 }
        r0 = r0.exists();	 Catch:{ all -> 0x02d3 }
        if (r0 == 0) goto L_0x02cf;
    L_0x0261:
        r0 = r1.mStats;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r0 = r0.mCheckinFile;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r0 = r0.readFully();	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        if (r0 == 0) goto L_0x02aa;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
    L_0x026b:
        r5 = android.os.Parcel.obtain();	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r6 = r0.length;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r2 = 0;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r5.unmarshall(r0, r2, r6);	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r5.setDataPosition(r2);	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r2 = new com.android.internal.os.BatteryStatsImpl;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r6 = r1.mStats;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r6 = r6.mHandler;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r29 = r0;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r0 = r1.mUserManagerUserInfoProvider;	 Catch:{ IOException -> 0x02ad, IOException -> 0x02ad }
        r30 = r7;
        r7 = 0;
        r2.<init>(r7, r6, r7, r0);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r0 = r2;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r0.readSummaryFromParcel(r5);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5.recycle();	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r2 = r1.mContext;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r20 = r0;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r21 = r2;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r22 = r34;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r23 = r3;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r24 = r17;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r25 = r27;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r20.dumpProtoLocked(r21, r22, r23, r24, r25);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r2 = r1.mStats;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r2 = r2.mCheckinFile;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r2.delete();	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        monitor-exit(r4);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        return;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02a8:
        r0 = move-exception;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        goto L_0x02b0;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02aa:
        r30 = r7;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        goto L_0x02d1;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02ad:
        r0 = move-exception;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r30 = r7;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02b0:
        r2 = "BatteryStatsService";	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5.<init>();	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r6 = "Failure reading checkin file ";	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5.append(r6);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r6 = r1.mStats;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r6 = r6.mCheckinFile;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r6 = r6.getBaseFile();	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5.append(r6);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r5 = r5.toString();	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        android.util.Slog.w(r2, r5, r0);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        goto L_0x02d1;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02cf:
        r30 = r7;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02d1:
        monitor-exit(r4);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        goto L_0x02dc;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02d3:
        r0 = move-exception;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        r30 = r7;	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
    L_0x02d6:
        monitor-exit(r4);	 Catch:{ IOException -> 0x02a8, IOException -> 0x02a8, all -> 0x02d8 }
        throw r0;
    L_0x02d8:
        r0 = move-exception;
        goto L_0x02d6;
    L_0x02da:
        r30 = r7;
    L_0x02dc:
        r2 = r1.mStats;
        monitor-enter(r2);
        r0 = r1.mStats;
        r4 = r1.mContext;
        r20 = r0;
        r21 = r4;
        r22 = r34;
        r23 = r3;
        r24 = r17;
        r25 = r27;
        r20.dumpProtoLocked(r21, r22, r23, r24, r25);
        if (r14 == 0) goto L_0x02f9;
    L_0x02f4:
        r0 = r1.mStats;
        r0.writeAsyncLocked();
    L_0x02f9:
        monitor-exit(r2);
        r32 = r11;
        r10 = r30;
        goto L_0x03f4;
    L_0x0301:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
    L_0x0304:
        r30 = r7;
        if (r19 == 0) goto L_0x03d6;
    L_0x0308:
        r2 = r1.mContext;
        r2 = r2.getPackageManager();
        r18 = r2.getInstalledApplications(r0);
        if (r13 == 0) goto L_0x03af;
    L_0x0314:
        r0 = r1.mStats;
        r7 = r0.mCheckinFile;
        monitor-enter(r7);
        r0 = r1.mStats;	 Catch:{ all -> 0x03a4 }
        r0 = r0.mCheckinFile;	 Catch:{ all -> 0x03a4 }
        r0 = r0.exists();	 Catch:{ all -> 0x03a4 }
        if (r0 == 0) goto L_0x039c;
    L_0x0323:
        r0 = r1.mStats;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r0 = r0.mCheckinFile;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r0 = r0.readFully();	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        if (r0 == 0) goto L_0x036f;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
    L_0x032d:
        r2 = android.os.Parcel.obtain();	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r8 = r2;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r2 = r0.length;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r3 = 0;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r8.unmarshall(r0, r3, r2);	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r8.setDataPosition(r3);	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r2 = new com.android.internal.os.BatteryStatsImpl;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r3 = r1.mStats;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r3 = r3.mHandler;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r4 = r1.mUserManagerUserInfoProvider;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r5 = 0;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r2.<init>(r5, r3, r5, r4);	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r6 = r2;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r6.readSummaryFromParcel(r8);	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r8.recycle();	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r3 = r1.mContext;	 Catch:{ IOException -> 0x0376, IOException -> 0x0376 }
        r2 = r6;
        r4 = r9;
        r5 = r18;
        r16 = r6;
        r6 = r17;
        r20 = r7;
        r21 = r8;
        r32 = r11;
        r10 = r30;
        r7 = r27;
        r2.dumpCheckinLocked(r3, r4, r5, r6, r7);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r2 = r1.mStats;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r2 = r2.mCheckinFile;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r2.delete();	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        monitor-exit(r20);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        return;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x036d:
        r0 = move-exception;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        goto L_0x037d;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x036f:
        r20 = r7;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r32 = r11;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r10 = r30;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        goto L_0x03a2;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x0376:
        r0 = move-exception;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r20 = r7;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r32 = r11;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r10 = r30;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x037d:
        r2 = "BatteryStatsService";	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r3 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r3.<init>();	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r4 = "Failure reading checkin file ";	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r3.append(r4);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r4 = r1.mStats;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r4 = r4.mCheckinFile;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r4 = r4.getBaseFile();	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r3.append(r4);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r3 = r3.toString();	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        android.util.Slog.w(r2, r3, r0);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        goto L_0x03a2;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x039c:
        r20 = r7;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r32 = r11;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r10 = r30;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x03a2:
        monitor-exit(r20);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        goto L_0x03b3;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x03a4:
        r0 = move-exception;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r20 = r7;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r32 = r11;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        r10 = r30;	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
    L_0x03ab:
        monitor-exit(r20);	 Catch:{ IOException -> 0x036d, IOException -> 0x036d, all -> 0x03ad }
        throw r0;
    L_0x03ad:
        r0 = move-exception;
        goto L_0x03ab;
    L_0x03af:
        r32 = r11;
        r10 = r30;
    L_0x03b3:
        r7 = r1.mStats;
        monitor-enter(r7);
        r2 = r1.mStats;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        r3 = r1.mContext;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        r4 = r9;
        r5 = r18;
        r6 = r17;
        r16 = r7;
        r7 = r27;
        r2.dumpCheckinLocked(r3, r4, r5, r6, r7);	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        if (r14 == 0) goto L_0x03cd;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
    L_0x03c8:
        r0 = r1.mStats;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        r0.writeAsyncLocked();	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
    L_0x03cd:
        monitor-exit(r16);	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        goto L_0x03f4;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
    L_0x03cf:
        r0 = move-exception;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        r16 = r7;	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
    L_0x03d2:
        monitor-exit(r16);	 Catch:{ all -> 0x03cf, all -> 0x03d4 }
        throw r0;
    L_0x03d4:
        r0 = move-exception;
        goto L_0x03d2;
    L_0x03d6:
        r32 = r11;
        r10 = r30;
        r7 = r1.mStats;
        monitor-enter(r7);
        r2 = r1.mStats;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        r3 = r1.mContext;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        r4 = r9;
        r5 = r17;
        r6 = r15;
        r16 = r7;
        r7 = r27;
        r2.dumpLocked(r3, r4, r5, r6, r7);	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        if (r14 == 0) goto L_0x03f3;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
    L_0x03ee:
        r0 = r1.mStats;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        r0.writeAsyncLocked();	 Catch:{ all -> 0x03f5, all -> 0x03fa }
    L_0x03f3:
        monitor-exit(r16);	 Catch:{ all -> 0x03f5, all -> 0x03fa }
    L_0x03f4:
        return;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
    L_0x03f5:
        r0 = move-exception;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        r16 = r7;	 Catch:{ all -> 0x03f5, all -> 0x03fa }
    L_0x03f8:
        monitor-exit(r16);	 Catch:{ all -> 0x03f5, all -> 0x03fa }
        throw r0;
    L_0x03fa:
        r0 = move-exception;
        goto L_0x03f8;
    L_0x03fc:
        r0 = move-exception;
        r32 = r11;
        r10 = r7;
        android.os.Binder.restoreCallingIdentity(r10);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BatteryStatsService.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
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
    public void noteChangeWakelockFromSource(android.os.WorkSource r16, int r17, java.lang.String r18, java.lang.String r19, int r20, android.os.WorkSource r21, int r22, java.lang.String r23, java.lang.String r24, int r25, boolean r26) {
        /*
        r15 = this;
        r1 = r15;
        r1.enforceCallingPermission();
        r2 = r1.mStats;
        monitor-enter(r2);
        r3 = r1.mStats;	 Catch:{ all -> 0x0053 }
        r4 = r16;	 Catch:{ all -> 0x0053 }
        r5 = r17;	 Catch:{ all -> 0x0053 }
        r6 = r18;	 Catch:{ all -> 0x0053 }
        r7 = r19;	 Catch:{ all -> 0x0053 }
        r8 = r20;	 Catch:{ all -> 0x0053 }
        r9 = r21;	 Catch:{ all -> 0x0053 }
        r10 = r22;	 Catch:{ all -> 0x0053 }
        r11 = r23;	 Catch:{ all -> 0x0053 }
        r12 = r24;	 Catch:{ all -> 0x0053 }
        r13 = r25;	 Catch:{ all -> 0x0053 }
        r14 = r26;	 Catch:{ all -> 0x0053 }
        r3.noteChangeWakelockFromSourceLocked(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14);	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        r0 = r1.misBetaUser;
        if (r0 == 0) goto L_0x004a;
    L_0x0027:
        r0 = r1.mPowerInfoService;
        if (r0 == 0) goto L_0x004a;
    L_0x002b:
        r2 = r1.mPowerInfoService;
        monitor-enter(r2);
        r0 = r1.mPowerInfoService;	 Catch:{ all -> 0x003f, all -> 0x003d }
        r3 = r17;
        r4 = r18;
        r5 = r22;
        r6 = r23;
        r0.notePowerInfoChangeWakeLock(r4, r3, r6, r5);	 Catch:{ all -> 0x003f, all -> 0x003d }
        monitor-exit(r2);	 Catch:{ all -> 0x003f, all -> 0x003d }
        goto L_0x0052;	 Catch:{ all -> 0x003f, all -> 0x003d }
    L_0x003d:
        r0 = move-exception;	 Catch:{ all -> 0x003f, all -> 0x003d }
        goto L_0x0048;	 Catch:{ all -> 0x003f, all -> 0x003d }
    L_0x003f:
        r0 = move-exception;	 Catch:{ all -> 0x003f, all -> 0x003d }
        r3 = r17;	 Catch:{ all -> 0x003f, all -> 0x003d }
        r4 = r18;	 Catch:{ all -> 0x003f, all -> 0x003d }
        r5 = r22;	 Catch:{ all -> 0x003f, all -> 0x003d }
        r6 = r23;	 Catch:{ all -> 0x003f, all -> 0x003d }
    L_0x0048:
        monitor-exit(r2);	 Catch:{ all -> 0x003f, all -> 0x003d }
        throw r0;
    L_0x004a:
        r3 = r17;
        r4 = r18;
        r5 = r22;
        r6 = r23;
    L_0x0052:
        return;
    L_0x0053:
        r0 = move-exception;
        r3 = r17;
        r4 = r18;
        r5 = r22;
        r6 = r23;
    L_0x005c:
        monitor-exit(r2);	 Catch:{ all -> 0x005e }
        throw r0;
    L_0x005e:
        r0 = move-exception;
        goto L_0x005c;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BatteryStatsService.noteChangeWakelockFromSource(android.os.WorkSource, int, java.lang.String, java.lang.String, int, android.os.WorkSource, int, java.lang.String, java.lang.String, int, boolean):void");
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
    public void noteStartWakelock(int r16, int r17, java.lang.String r18, java.lang.String r19, int r20, boolean r21) {
        /*
        r15 = this;
        r1 = r15;
        r1.enforceCallingPermission();
        r2 = r1.mStats;
        monitor-enter(r2);
        r3 = r1.mStats;	 Catch:{ all -> 0x0046 }
        r6 = 0;	 Catch:{ all -> 0x0046 }
        r11 = android.os.SystemClock.elapsedRealtime();	 Catch:{ all -> 0x0046 }
        r13 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x0046 }
        r4 = r16;	 Catch:{ all -> 0x0046 }
        r5 = r17;	 Catch:{ all -> 0x0046 }
        r7 = r18;	 Catch:{ all -> 0x0046 }
        r8 = r19;	 Catch:{ all -> 0x0046 }
        r9 = r20;	 Catch:{ all -> 0x0046 }
        r10 = r21;	 Catch:{ all -> 0x0046 }
        r3.noteStartWakeLocked(r4, r5, r6, r7, r8, r9, r10, r11, r13);	 Catch:{ all -> 0x0046 }
        monitor-exit(r2);	 Catch:{ all -> 0x0046 }
        r0 = r1.misBetaUser;
        if (r0 == 0) goto L_0x0041;
    L_0x0026:
        r0 = r1.mPowerInfoService;
        if (r0 == 0) goto L_0x0041;
    L_0x002a:
        r2 = r1.mPowerInfoService;
        monitor-enter(r2);
        r0 = r1.mPowerInfoService;	 Catch:{ all -> 0x003a, all -> 0x0038 }
        r3 = r17;
        r4 = r18;
        r0.notePowerInfoAcquireWakeLock(r4, r3);	 Catch:{ all -> 0x003a, all -> 0x0038 }
        monitor-exit(r2);	 Catch:{ all -> 0x003a, all -> 0x0038 }
        goto L_0x0045;	 Catch:{ all -> 0x003a, all -> 0x0038 }
    L_0x0038:
        r0 = move-exception;	 Catch:{ all -> 0x003a, all -> 0x0038 }
        goto L_0x003f;	 Catch:{ all -> 0x003a, all -> 0x0038 }
    L_0x003a:
        r0 = move-exception;	 Catch:{ all -> 0x003a, all -> 0x0038 }
        r3 = r17;	 Catch:{ all -> 0x003a, all -> 0x0038 }
        r4 = r18;	 Catch:{ all -> 0x003a, all -> 0x0038 }
    L_0x003f:
        monitor-exit(r2);	 Catch:{ all -> 0x003a, all -> 0x0038 }
        throw r0;
    L_0x0041:
        r3 = r17;
        r4 = r18;
    L_0x0045:
        return;
    L_0x0046:
        r0 = move-exception;
        r3 = r17;
        r4 = r18;
    L_0x004b:
        monitor-exit(r2);	 Catch:{ all -> 0x004d }
        throw r0;
    L_0x004d:
        r0 = move-exception;
        goto L_0x004b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BatteryStatsService.noteStartWakelock(int, int, java.lang.String, java.lang.String, int, boolean):void");
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
    public void noteStopWakelock(int r15, int r16, java.lang.String r17, java.lang.String r18, int r19) {
        /*
        r14 = this;
        r1 = r14;
        r1.enforceCallingPermission();
        r2 = r1.mStats;
        monitor-enter(r2);
        r3 = r1.mStats;	 Catch:{ all -> 0x0043 }
        r6 = 0;	 Catch:{ all -> 0x0043 }
        r10 = android.os.SystemClock.elapsedRealtime();	 Catch:{ all -> 0x0043 }
        r12 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x0043 }
        r4 = r15;	 Catch:{ all -> 0x0043 }
        r5 = r16;	 Catch:{ all -> 0x0043 }
        r7 = r17;	 Catch:{ all -> 0x0043 }
        r8 = r18;	 Catch:{ all -> 0x0043 }
        r9 = r19;	 Catch:{ all -> 0x0043 }
        r3.noteStopWakeLocked(r4, r5, r6, r7, r8, r9, r10, r12);	 Catch:{ all -> 0x0043 }
        monitor-exit(r2);	 Catch:{ all -> 0x0043 }
        r0 = r1.misBetaUser;
        if (r0 == 0) goto L_0x003e;
    L_0x0023:
        r0 = r1.mPowerInfoService;
        if (r0 == 0) goto L_0x003e;
    L_0x0027:
        r2 = r1.mPowerInfoService;
        monitor-enter(r2);
        r0 = r1.mPowerInfoService;	 Catch:{ all -> 0x0037, all -> 0x0035 }
        r3 = r16;
        r4 = r17;
        r0.notePowerInfoReleaseWakeLock(r4, r3);	 Catch:{ all -> 0x0037, all -> 0x0035 }
        monitor-exit(r2);	 Catch:{ all -> 0x0037, all -> 0x0035 }
        goto L_0x0042;	 Catch:{ all -> 0x0037, all -> 0x0035 }
    L_0x0035:
        r0 = move-exception;	 Catch:{ all -> 0x0037, all -> 0x0035 }
        goto L_0x003c;	 Catch:{ all -> 0x0037, all -> 0x0035 }
    L_0x0037:
        r0 = move-exception;	 Catch:{ all -> 0x0037, all -> 0x0035 }
        r3 = r16;	 Catch:{ all -> 0x0037, all -> 0x0035 }
        r4 = r17;	 Catch:{ all -> 0x0037, all -> 0x0035 }
    L_0x003c:
        monitor-exit(r2);	 Catch:{ all -> 0x0037, all -> 0x0035 }
        throw r0;
    L_0x003e:
        r3 = r16;
        r4 = r17;
    L_0x0042:
        return;
    L_0x0043:
        r0 = move-exception;
        r3 = r16;
        r4 = r17;
    L_0x0048:
        monitor-exit(r2);	 Catch:{ all -> 0x004a }
        throw r0;
    L_0x004a:
        r0 = move-exception;
        goto L_0x0048;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BatteryStatsService.noteStopWakelock(int, int, java.lang.String, java.lang.String, int):void");
    }

    public void fillLowPowerStats(RpmStats rpmStats) {
        getLowPowerStats(rpmStats);
    }

    public String getPlatformLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int bytesWritten = getPlatformLowPowerStats(this.mUtf8BufferStat);
        if (bytesWritten < 0) {
            return null;
        }
        if (bytesWritten == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(bytesWritten);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    public String getSubsystemLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int bytesWritten = getSubsystemLowPowerStats(this.mUtf8BufferStat);
        if (bytesWritten < 0) {
            return null;
        }
        if (bytesWritten == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(bytesWritten);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    BatteryStatsService(Context context, File systemDir, Handler handler) {
        this.mContext = context;
        this.mUserManagerUserInfoProvider = new UserInfoProvider() {
            private UserManagerInternal umi;

            public int[] getUserIds() {
                if (this.umi == null) {
                    this.umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
                }
                return this.umi != null ? this.umi.getUserIds() : null;
            }
        };
        this.mStats = new BatteryStatsImpl(systemDir, handler, this, this.mUserManagerUserInfoProvider);
        this.mWorker = new BatteryExternalStatsWorker(context, this.mStats);
        this.mStats.setExternalStatsSyncLocked(this.mWorker);
        this.mStats.setRadioScanningTimeoutLocked(((long) this.mContext.getResources().getInteger(17694851)) * 1000);
        this.mStats.setPowerProfileLocked(new PowerProfile(context));
    }

    public void publish() {
        LocalServices.addService(BatteryStatsInternal.class, new LocalService(this, null));
        ServiceManager.addService("batterystats", asBinder());
    }

    public void systemServicesReady() {
        this.mStats.systemServicesReady(this.mContext);
    }

    private static void awaitUninterruptibly(Future<?> future) {
        while (true) {
            try {
                future.get();
                return;
            } catch (ExecutionException e) {
                return;
            } catch (InterruptedException e2) {
            }
        }
    }

    private void syncStats(String reason, int flags) {
        awaitUninterruptibly(this.mWorker.scheduleSync(reason, flags));
    }

    public void initPowerManagement() {
        PowerManagerInternal powerMgr = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        powerMgr.registerLowPowerModeObserver(this);
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(powerMgr.getLowPowerState(9).batterySaverEnabled);
        }
        new WakeupReasonThread().start();
    }

    public void shutdown() {
        Slog.w("BatteryStats", "Writing battery stats before shutdown...");
        syncStats("shutdown", 31);
        synchronized (this.mStats) {
            this.mStats.shutdownLocked();
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteShutdown();
            }
        }
        this.mWorker.shutdown();
    }

    public static IBatteryStats getService() {
        if (sService != null) {
            return sService;
        }
        sService = asInterface(ServiceManager.getService("batterystats"));
        return sService;
    }

    public int getServiceType() {
        return 9;
    }

    public void onLowPowerModeChanged(PowerSaveState result) {
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(result.batterySaverEnabled);
        }
    }

    public BatteryStatsImpl getActiveStatistics() {
        return this.mStats;
    }

    public void scheduleWriteToDisk() {
        this.mWorker.scheduleWrite();
    }

    void removeUid(int uid) {
        synchronized (this.mStats) {
            this.mStats.removeUidStatsLocked(uid);
        }
    }

    void onCleanupUser(int userId) {
        synchronized (this.mStats) {
            this.mStats.onCleanupUserLocked(userId);
        }
    }

    void onUserRemoved(int userId) {
        synchronized (this.mStats) {
            this.mStats.onUserRemovedLocked(userId);
        }
    }

    void addIsolatedUid(int isolatedUid, int appUid) {
        synchronized (this.mStats) {
            this.mStats.addIsolatedUidLocked(isolatedUid, appUid);
        }
    }

    void removeIsolatedUid(int isolatedUid, int appUid) {
        synchronized (this.mStats) {
            this.mStats.scheduleRemoveIsolatedUidLocked(isolatedUid, appUid);
        }
    }

    void noteProcessStart(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessStartLocked(name, uid);
            StatsLog.write(28, uid, name, 1);
        }
    }

    void noteProcessCrash(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessCrashLocked(name, uid);
            StatsLog.write(28, uid, name, 2);
        }
    }

    void noteProcessAnr(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessAnrLocked(name, uid);
        }
    }

    void noteProcessFinish(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessFinishLocked(name, uid);
            StatsLog.write(28, uid, name, 0);
        }
    }

    void noteUidProcessState(int uid, int state) {
        synchronized (this.mStats) {
            StatsLog.write(27, uid, ActivityManager.processStateAmToProto(state));
            try {
                this.mStats.noteUidProcessStateLocked(uid, state);
            } catch (RejectedExecutionException e) {
                Slog.w(TAG, "noteUidProcessStateLocked Failed.", e);
            }
        }
    }

    public byte[] getStatistics() {
        this.mContext.enforceCallingPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        return data;
    }

    public ParcelFileDescriptor getStatisticsStream() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        try {
            return ParcelFileDescriptor.fromData(data, "battery-stats");
        } catch (IOException e) {
            Slog.w(TAG, "Unable to create shared memory", e);
            return null;
        }
    }

    public boolean isCharging() {
        boolean isCharging;
        synchronized (this.mStats) {
            isCharging = this.mStats.isCharging();
        }
        return isCharging;
    }

    public long computeBatteryTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public long computeChargeTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeChargeTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public void noteEvent(int code, String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteEventLocked(code, name, uid);
        }
        if (this.misBetaUser && code == 32771 && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoTopApp(name, uid);
            }
        }
    }

    public void noteSyncStart(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncStartLocked(name, uid);
            StatsLog.write_non_chained(7, uid, null, name, 1);
        }
    }

    public void noteSyncFinish(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncFinishLocked(name, uid);
            StatsLog.write_non_chained(7, uid, null, name, 0);
        }
    }

    public void noteJobStart(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobStartLocked(name, uid);
            StatsLog.write_non_chained(8, uid, null, name, 1, -1);
        }
    }

    public void noteJobFinish(String name, int uid, int stopReason) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobFinishLocked(name, uid, stopReason);
            StatsLog.write_non_chained(8, uid, null, name, 0, stopReason);
        }
    }

    void noteJobsDeferred(int uid, int numDeferred, long sinceLast) {
        synchronized (this.mStats) {
            this.mStats.noteJobsDeferredLocked(uid, numDeferred, sinceLast);
        }
    }

    public void noteWakupAlarm(String name, int uid, WorkSource workSource, String tag) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakupAlarmLocked(name, uid, workSource, tag);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoStartAlarm(name, uid);
            }
        }
    }

    public void noteAlarmStart(String name, WorkSource workSource, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmStartLocked(name, workSource, uid);
        }
    }

    public void noteAlarmFinish(String name, WorkSource workSource, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmFinishLocked(name, workSource, uid);
        }
    }

    public void noteStartWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartWakeFromSourceLocked(ws, pid, name, historyName, type, unimportantForLogging);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoAcquireWakeLock(name, pid);
            }
        }
    }

    public void noteStopWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopWakeFromSourceLocked(ws, pid, name, historyName, type);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoReleaseWakeLock(name, pid);
            }
        }
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStart(name, historyName, uid);
        }
    }

    public void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStartFromSource(name, historyName, workSource);
        }
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinish(name, historyName, uid);
        }
    }

    public void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinishFromSource(name, historyName, workSource);
        }
    }

    public void noteStartSensor(int uid, int sensor) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartSensorLocked(uid, sensor);
            StatsLog.write_non_chained(5, uid, null, sensor, 1);
        }
    }

    public void noteStopSensor(int uid, int sensor) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopSensorLocked(uid, sensor);
            StatsLog.write_non_chained(5, uid, null, sensor, 0);
        }
    }

    public void noteVibratorOn(int uid, long durationMillis) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOnLocked(uid, durationMillis);
        }
        HwServiceFactory.reportVibratorToIAware(uid);
    }

    public void noteVibratorOff(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOffLocked(uid);
        }
    }

    public void noteGpsChanged(WorkSource oldWs, WorkSource newWs) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteGpsChangedLocked(oldWs, newWs);
        }
    }

    public void noteGpsSignalQuality(int signalLevel) {
        synchronized (this.mStats) {
            this.mStats.noteGpsSignalQualityLocked(signalLevel);
        }
    }

    public void noteScreenState(int state) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(29, state);
            this.mStats.noteScreenStateLocked(state);
        }
        boolean z = 3 == SystemProperties.getInt("ro.logsystem.usertype", 0) || 5 == SystemProperties.getInt("ro.logsystem.usertype", 0) || SystemProperties.getBoolean("persist.sys.huawei.debug.on", false) || SystemProperties.getBoolean("hwlog.remotedebug", false);
        this.misBetaUser = z;
        if (this.misBetaUser && !this.mIsPowerInfoStatus) {
            Slog.i(TAG, "getHwPowerInfoService instance");
            this.mPowerInfoService = HwServiceFactory.getHwPowerInfoService(this.mContext, true);
            if (this.mPowerInfoService != null) {
                this.mIsPowerInfoStatus = true;
            }
        } else if (!this.misBetaUser && this.mIsPowerInfoStatus) {
            this.mIsPowerInfoStatus = false;
            Slog.i(TAG, "getHwPowerInfoService uninstance");
            this.mPowerInfoService = HwServiceFactory.getHwPowerInfoService(this.mContext, false);
        }
    }

    public void noteScreenBrightness(int brightness) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(9, brightness);
            this.mStats.noteScreenBrightnessLocked(brightness);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoBrightness(brightness);
            }
        }
    }

    public void noteUserActivity(int uid, int event) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteUserActivityLocked(uid, event);
        }
    }

    public void noteWakeUp(String reason, int reasonUid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakeUpLocked(reason, reasonUid);
        }
    }

    public void noteInteractive(boolean interactive) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteInteractiveLocked(interactive);
        }
    }

    public void noteConnectivityChanged(int type, String extra) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteConnectivityChangedLocked(type, extra);
        }
    }

    public void noteMobileRadioPowerState(int powerState, long timestampNs, int uid) {
        boolean update;
        enforceCallingPermission();
        synchronized (this.mStats) {
            update = this.mStats.noteMobileRadioPowerStateLocked(powerState, timestampNs, uid);
        }
        if (update) {
            this.mWorker.scheduleSync("modem-data", 4);
        }
    }

    public void notePhoneOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOnLocked();
        }
    }

    public void notePhoneOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOffLocked();
        }
    }

    public void notePhoneSignalStrength(SignalStrength signalStrength) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneSignalStrengthLocked(signalStrength);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoSignalStrength(signalStrength.getLevel());
            }
        }
    }

    public void notePhoneDataConnectionState(int dataType, boolean hasData) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneDataConnectionStateLocked(dataType, hasData);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoConnectionState(dataType, hasData);
            }
        }
    }

    public void notePhoneState(int state) {
        enforceCallingPermission();
        int simState = TelephonyManager.getDefault().getSimState();
        synchronized (this.mStats) {
            this.mStats.notePhoneStateLocked(state, simState);
        }
    }

    public void noteWifiOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOnLocked();
        }
    }

    public void noteWifiOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOffLocked();
        }
    }

    public void noteStartAudio(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOnLocked(uid);
            StatsLog.write_non_chained(23, uid, null, 1);
        }
    }

    public void noteStopAudio(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOffLocked(uid);
            StatsLog.write_non_chained(23, uid, null, 0);
        }
    }

    public void noteStartVideo(int uid) {
        LogPower.push(NetworkConstants.ICMPV6_NEIGHBOR_ADVERTISEMENT, Integer.toString(Binder.getCallingUid()));
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOnLocked(uid);
            StatsLog.write_non_chained(24, uid, null, 1);
        }
    }

    public void noteStopVideo(int uid) {
        LogPower.push(137, Integer.toString(Binder.getCallingUid()));
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOffLocked(uid);
            StatsLog.write_non_chained(24, uid, null, 0);
        }
    }

    public void noteResetAudio() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetAudioLocked();
            StatsLog.write_non_chained(23, -1, null, 2);
        }
    }

    public void noteResetVideo() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetVideoLocked();
            StatsLog.write_non_chained(24, -1, null, 2);
        }
    }

    public void noteFlashlightOn(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOnLocked(uid);
            StatsLog.write_non_chained(26, uid, null, 1);
        }
    }

    public void noteFlashlightOff(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOffLocked(uid);
            StatsLog.write_non_chained(26, uid, null, 0);
        }
    }

    public void noteStartCamera(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOnLocked(uid);
            StatsLog.write_non_chained(25, uid, null, 1);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteStartCamera();
            }
        }
        this.mActivityManagerService.reportCamera(uid, 1);
        LogPower.push(204, null, String.valueOf(uid), String.valueOf(1));
    }

    public void noteStopCamera(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOffLocked(uid);
            StatsLog.write_non_chained(25, uid, null, 0);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteStopCamera();
            }
        }
        this.mActivityManagerService.reportCamera(uid, 0);
        LogPower.push(205, null, String.valueOf(uid), String.valueOf(1));
    }

    public void noteResetCamera() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetCameraLocked();
            StatsLog.write_non_chained(25, -1, null, 2);
        }
    }

    public void noteResetFlashlight() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetFlashlightLocked();
            StatsLog.write_non_chained(26, -1, null, 2);
        }
    }

    public void noteWifiRadioPowerState(int powerState, long tsNanos, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            if (this.mStats.isOnBattery()) {
                String type;
                if (powerState == 3 || powerState == 2) {
                    type = "active";
                } else {
                    type = "inactive";
                }
                BatteryExternalStatsWorker batteryExternalStatsWorker = this.mWorker;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("wifi-data: ");
                stringBuilder.append(type);
                batteryExternalStatsWorker.scheduleSync(stringBuilder.toString(), 2);
            }
            this.mStats.noteWifiRadioPowerState(powerState, tsNanos, uid);
        }
    }

    public void noteWifiRunning(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningLocked(ws);
        }
    }

    public void noteWifiRunningChanged(WorkSource oldWs, WorkSource newWs) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningChangedLocked(oldWs, newWs);
        }
    }

    public void noteWifiStopped(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStoppedLocked(ws);
        }
    }

    public void noteWifiState(int wifiState, String accessPoint) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStateLocked(wifiState, accessPoint);
        }
    }

    public void noteWifiSupplicantStateChanged(int supplState, boolean failedAuth) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiSupplicantStateChangedLocked(supplState, failedAuth);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoWifiState(supplState);
            }
        }
    }

    public void noteWifiRssiChanged(int newRssi) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRssiChangedLocked(newRssi);
        }
    }

    public void noteFullWifiLockAcquired(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredLocked(uid);
        }
    }

    public void noteFullWifiLockReleased(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedLocked(uid);
        }
    }

    public void noteWifiScanStarted(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedLocked(uid);
        }
    }

    public void noteWifiScanStopped(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedLocked(uid);
        }
    }

    public void noteWifiMulticastEnabled(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastEnabledLocked(uid);
        }
    }

    public void noteWifiMulticastDisabled(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastDisabledLocked(uid);
        }
    }

    public void noteFullWifiLockAcquiredFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredFromSourceLocked(ws);
        }
    }

    public void noteFullWifiLockReleasedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedFromSourceLocked(ws);
        }
    }

    public void noteWifiScanStartedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            for (int i = 0; i < ws.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uid=");
                stringBuilder.append(ws.get(i));
                HwLog.dubaie("DUBAI_TAG_WIFI_SCAN_STARTED", stringBuilder.toString());
            }
            this.mStats.noteWifiScanStartedFromSourceLocked(ws);
        }
    }

    public void noteWifiScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedFromSourceLocked(ws);
        }
    }

    public void noteWifiBatchedScanStartedFromSource(WorkSource ws, int csph) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStartedFromSourceLocked(ws, csph);
        }
    }

    public void noteWifiBatchedScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStoppedFromSourceLocked(ws);
        }
    }

    public void noteNetworkInterfaceType(String iface, int networkType) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteNetworkInterfaceTypeLocked(iface, networkType);
        }
    }

    public void noteNetworkStatsEnabled() {
        enforceCallingPermission();
        this.mWorker.scheduleSync("network-stats-enabled", 6);
    }

    public void noteDeviceIdleMode(int mode, String activeReason, int activeUid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteDeviceIdleModeLocked(mode, activeReason, activeUid);
        }
    }

    public void notePackageInstalled(String pkgName, long versionCode) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageInstalledLocked(pkgName, versionCode);
        }
    }

    public void notePackageUninstalled(String pkgName) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageUninstalledLocked(pkgName);
        }
    }

    public void noteBleScanStarted(WorkSource ws, boolean isUnoptimized) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStartedFromSourceLocked(ws, isUnoptimized);
        }
    }

    public void noteBleScanStopped(WorkSource ws, boolean isUnoptimized) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStoppedFromSourceLocked(ws, isUnoptimized);
        }
    }

    public void noteResetBleScan() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetBluetoothScanLocked();
        }
    }

    public void noteBleScanResults(WorkSource ws, int numNewResults) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanResultsFromSourceLocked(ws, numNewResults);
        }
    }

    public void noteWifiControllerActivity(WifiActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid wifi data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mStats.updateWifiState(info);
    }

    public void noteBluetoothControllerActivity(BluetoothActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid bluetooth data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mStats) {
            this.mStats.updateBluetoothStateLocked(info);
        }
    }

    public void noteModemControllerActivity(ModemActivityInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid modem data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mStats.updateMobileRadioState(info);
    }

    public boolean isOnBattery() {
        return this.mStats.isOnBattery();
    }

    public void setBatteryState(int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        enforceCallingPermission();
        this.mWorker.scheduleRunnable(new -$$Lambda$BatteryStatsService$ZxbqtJ7ozYmzYFkkNV3m_QRd0Sk(this, plugType, status, health, level, temp, volt, chargeUAh, chargeFullUAh));
    }

    /* JADX WARNING: Missing block: B:25:0x004b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$setBatteryState$1(BatteryStatsService batteryStatsService, int plugType, int status, int health, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        Throwable th;
        BatteryStatsService batteryStatsService2 = batteryStatsService;
        synchronized (batteryStatsService2.mStats) {
            int i;
            int i2;
            try {
                if (batteryStatsService2.mStats.isOnBattery() == BatteryStatsImpl.isOnBattery(plugType, status)) {
                    batteryStatsService2.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh);
                    if (!batteryStatsService2.misBetaUser || batteryStatsService2.mPowerInfoService == null) {
                        i = plugType;
                        i2 = level;
                    } else {
                        synchronized (batteryStatsService2.mPowerInfoService) {
                            try {
                                batteryStatsService2.mPowerInfoService.notePowerInfoBatteryState(plugType, level);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                }
                i = plugType;
                i2 = level;
                batteryStatsService2.mWorker.scheduleSync("battery-state", 31);
                batteryStatsService2.mWorker.scheduleRunnable(new -$$Lambda$BatteryStatsService$rRONgIFHr4sujxPESRmo9P5RJ6w(batteryStatsService2, status, health, i, i2, temp, volt, chargeUAh, chargeFullUAh));
            } catch (Throwable th3) {
                th = th3;
                i = plugType;
                i2 = level;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0037, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$setBatteryState$0(BatteryStatsService batteryStatsService, int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        Throwable th;
        BatteryStatsService batteryStatsService2 = batteryStatsService;
        synchronized (batteryStatsService2.mStats) {
            int i;
            int i2;
            try {
                batteryStatsService2.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh);
                if (!batteryStatsService2.misBetaUser || batteryStatsService2.mPowerInfoService == null) {
                    i = plugType;
                    i2 = level;
                } else {
                    synchronized (batteryStatsService2.mPowerInfoService) {
                        try {
                            batteryStatsService2.mPowerInfoService.notePowerInfoBatteryState(plugType, level);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public long getAwakeTimeBattery() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimeBattery();
    }

    public long getAwakeTimePlugged() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimePlugged();
    }

    public void enforceCallingPermission() {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("Battery stats (batterystats) dump options:");
        pw.println("  [--checkin] [--proto] [--history] [--history-start] [--charged] [-c]");
        pw.println("  [--daily] [--reset] [--write] [--new-daily] [--read-daily] [-h] [<package.name>]");
        pw.println("  --checkin: generate output for a checkin report; will write (and clear) the");
        pw.println("             last old completed stats when they had been reset.");
        pw.println("  -c: write the current stats in checkin format.");
        pw.println("  --proto: write the current aggregate stats (without history) in proto format.");
        pw.println("  --history: show only history data.");
        pw.println("  --history-start <num>: show only history data starting at given time offset.");
        pw.println("  --charged: only output data since last charged.");
        pw.println("  --daily: only output full daily data.");
        pw.println("  --reset: reset the stats, clearing all current data.");
        pw.println("  --write: force write current collected stats to disk.");
        pw.println("  --new-daily: immediately create and write new daily stats record.");
        pw.println("  --read-daily: read-load last written daily stats.");
        pw.println("  --settings: dump the settings key/values related to batterystats");
        pw.println("  --cpu: dump cpu stats for debugging purpose");
        pw.println("  <package.name>: optional name of package to filter output by.");
        pw.println("  -h: print this help text.");
        pw.println("Battery stats (batterystats) commands:");
        pw.println("  enable|disable <option>");
        pw.println("    Enable or disable a running option.  Option state is not saved across boots.");
        pw.println("    Options are:");
        pw.println("      full-history: include additional detailed events in battery history:");
        pw.println("          wake_lock_in, alarms and proc events");
        pw.println("      no-auto-reset: don't automatically reset stats when unplugged");
        pw.println("      pretend-screen-off: pretend the screen is off, even if screen state changes");
    }

    private void dumpSettings(PrintWriter pw) {
        synchronized (this.mStats) {
            this.mStats.dumpConstantsLocked(pw);
        }
    }

    private void dumpCpuStats(PrintWriter pw) {
        synchronized (this.mStats) {
            this.mStats.dumpCpuStatsLocked(pw);
        }
    }

    private int doEnableOrDisable(PrintWriter pw, int i, String[] args, boolean enable) {
        i++;
        StringBuilder stringBuilder;
        if (i >= args.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Missing option argument for ");
            stringBuilder.append(enable ? "--enable" : "--disable");
            pw.println(stringBuilder.toString());
            dumpHelp(pw);
            return -1;
        }
        if ("full-wake-history".equals(args[i]) || "full-history".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setRecordAllHistoryLocked(enable);
            }
        } else if ("no-auto-reset".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setNoAutoReset(enable);
            }
        } else if ("pretend-screen-off".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setPretendScreenOff(enable);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown enable/disable option: ");
            stringBuilder.append(args[i]);
            pw.println(stringBuilder.toString());
            dumpHelp(pw);
            return -1;
        }
        return i;
    }

    public CellularBatteryStats getCellularBatteryStats() {
        CellularBatteryStats cellularBatteryStats;
        synchronized (this.mStats) {
            cellularBatteryStats = this.mStats.getCellularBatteryStats();
        }
        return cellularBatteryStats;
    }

    public WifiBatteryStats getWifiBatteryStats() {
        WifiBatteryStats wifiBatteryStats;
        synchronized (this.mStats) {
            wifiBatteryStats = this.mStats.getWifiBatteryStats();
        }
        return wifiBatteryStats;
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats gpsBatteryStats;
        synchronized (this.mStats) {
            gpsBatteryStats = this.mStats.getGpsBatteryStats();
        }
        return gpsBatteryStats;
    }

    public HealthStatsParceler takeUidSnapshot(int requestUid) {
        if (requestUid != Binder.getCallingUid()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            HealthStatsParceler healthStatsForUidLocked;
            if (shouldCollectExternalStats()) {
                syncStats("get-health-stats-for-uids", 31);
            }
            synchronized (this.mStats) {
                healthStatsForUidLocked = getHealthStatsForUidLocked(requestUid);
            }
            Binder.restoreCallingIdentity(ident);
            return healthStatsForUidLocked;
        } catch (Exception ex) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Crashed while writing for takeUidSnapshot(");
                stringBuilder.append(requestUid);
                stringBuilder.append(")");
                Slog.w(str, stringBuilder.toString(), ex);
                throw ex;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public HealthStatsParceler[] takeUidSnapshots(int[] requestUids) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.BATTERYSTATS_TAKEUIDSNAPSHOTS);
        if (!onlyCaller(requestUids)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            HealthStatsParceler[] results;
            if (shouldCollectExternalStats()) {
                syncStats("get-health-stats-for-uids", 31);
            }
            synchronized (this.mStats) {
                int N = requestUids.length;
                results = new HealthStatsParceler[N];
                for (int i = 0; i < N; i++) {
                    results[i] = getHealthStatsForUidLocked(requestUids[i]);
                }
            }
            Binder.restoreCallingIdentity(ident);
            return results;
        } catch (Exception ex) {
            try {
                throw ex;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private boolean shouldCollectExternalStats() {
        return SystemClock.elapsedRealtime() - this.mWorker.getLastCollectionTimeStamp() > this.mStats.getExternalStatsCollectionRateLimitMs();
    }

    private static boolean onlyCaller(int[] requestUids) {
        int caller = Binder.getCallingUid();
        for (int i : requestUids) {
            if (i != caller) {
                return false;
            }
        }
        return true;
    }

    HealthStatsParceler getHealthStatsForUidLocked(int requestUid) {
        HealthStatsBatteryStatsWriter writer = new HealthStatsBatteryStatsWriter();
        HealthStatsWriter uidWriter = new HealthStatsWriter(UidHealthStats.CONSTANTS);
        Uid uid = (Uid) this.mStats.getUidStats().get(requestUid);
        if (uid != null) {
            writer.writeUid(uidWriter, this.mStats, uid);
        }
        return new HealthStatsParceler(uidWriter);
    }

    protected void setActivityService(ActivityManagerService ams) {
        this.mActivityManagerService = ams;
    }
}
