package com.android.server.mtm.dump;

import android.app.mtm.MultiTaskManager;
import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo;
import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppFreezeSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.AppMngConstant.EnumWithDesc;
import android.app.mtm.iaware.appmng.IAppCleanCallback.Stub;
import android.content.Context;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appclean.CrashClean;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.AppStatusUtils;
import com.android.server.mtm.utils.AppStatusUtils.Status;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.srms.AppCleanupDumpRadar;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DumpAppMngClean {
    private static final int SINGLE_REMOVE = 1;
    public static final String TAG = "DumpAppMngClean";
    private static volatile Map<String, Consumer<Params>> consumers = new HashMap();

    /* renamed from: com.android.server.mtm.dump.DumpAppMngClean$3 */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature = new int[AppMngFeature.values().length];

        static {
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_CLEAN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_FREEZE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    static class Params {
        public String[] args;
        public Context context;
        public PrintWriter pw;

        public Params(Context context, PrintWriter pw, String[] args) {
            this.context = context;
            this.pw = pw;
            this.args = args;
        }
    }

    public static final void dump(Context context, PrintWriter pw, String[] args) {
        if (pw != null) {
            if (args == null || args.length < 2 || args[1] == null) {
                pw.println("  Bad command");
                return;
            }
            String cmd = args[1];
            synchronized (DumpAppMngClean.class) {
                if (consumers.isEmpty()) {
                    consumers.put("clean", -$$Lambda$DumpAppMngClean$KfhossEyWguRgocnpArCBogtq84.INSTANCE);
                    consumers.put("dumpPackage", -$$Lambda$DumpAppMngClean$T_fIfVRl2YBzqRzphMJEA6Y4CdY.INSTANCE);
                    consumers.put("dumpPackageList", -$$Lambda$DumpAppMngClean$Jl9_eo-adNBc9X2HJLdAfLaJXZ8.INSTANCE);
                    consumers.put("dumpTask", -$$Lambda$DumpAppMngClean$lLEQOsNYCeB4AgH1PFdm9jxx05k.INSTANCE);
                    consumers.put("SMClean", -$$Lambda$DumpAppMngClean$LSGBU4ssTWtw_zn3EO-yvEVqLXc.INSTANCE);
                    consumers.put("SMQuery", -$$Lambda$DumpAppMngClean$CM_PgQDlc0bzC2qyQbvMOmEcWiE.INSTANCE);
                    consumers.put("PGClean", -$$Lambda$DumpAppMngClean$MivzXvNudTNPkOIRQyMjFdydaQ4.INSTANCE);
                    consumers.put("ThermalClean", -$$Lambda$DumpAppMngClean$0BSnoGLDii96rEbhd748D3onels.INSTANCE);
                    consumers.put("CrashClean", -$$Lambda$DumpAppMngClean$mGdKgwdx--7iIj_qg5QWFmdhbTQ.INSTANCE);
                    consumers.put("CheckStatus", -$$Lambda$DumpAppMngClean$Ss1x4RfAzZOs5FTNrQ9ZoYWraTE.INSTANCE);
                    consumers.put("dumpDecide", -$$Lambda$DumpAppMngClean$dJzY8n-dMU3LBZPZ-3gGiDbMvX4.INSTANCE);
                    consumers.put("dumpHistory", -$$Lambda$DumpAppMngClean$1tePXi-xpwElJOAA-2KllLRsX2g.INSTANCE);
                    consumers.put("help", -$$Lambda$DumpAppMngClean$xO85N5a1Vu3C3lVatQtBcimmd6Y.INSTANCE);
                    consumers.put("dumpBigData", -$$Lambda$DumpAppMngClean$p_J8O3iMY9mnYS_Ra5btmJa_hhI.INSTANCE);
                    consumers.put("dumpAppType", -$$Lambda$DumpAppMngClean$ZMwkP-ULrbRfppectXbk35vI-FA.INSTANCE);
                }
                Consumer<Params> func = (Consumer) consumers.get(cmd);
                if (func == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Bad command: ");
                    stringBuilder.append(cmd);
                    pw.println(stringBuilder.toString());
                    return;
                }
                try {
                    func.accept(new Params(context, pw, args));
                } catch (Exception e) {
                    pw.println("  Bad command:");
                    pw.println(e.toString());
                }
            }
        } else {
            return;
        }
    }

    private static void dumpPackage(Params mParams) {
        if (mParams.args.length < 4) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        ProcessInfoCollector.getInstance().dumpPackageTask(ProcessInfoCollector.getInstance().getProcessInfosFromPackage(mParams.args[2], Integer.parseInt(mParams.args[3])), mParams.pw);
    }

    private static void dumpPackageList(Params mParams) {
        if (mParams.args.length < 3) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        String[] packList = mParams.args[2].split(",");
        ArrayMap<String, Integer> packMap = new ArrayMap();
        for (String s : packList) {
            String[] pack = s.split(":");
            PrintWriter printWriter = mParams.pw;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  package name: ");
            stringBuilder.append(pack[0]);
            stringBuilder.append(", uid: ");
            stringBuilder.append(pack[1]);
            printWriter.println(stringBuilder.toString());
            packMap.put(pack[0], Integer.valueOf(Integer.parseInt(pack[1])));
        }
        ProcessInfoCollector.getInstance().dumpPackageTask(ProcessInfoCollector.getInstance().getProcessInfosFromPackageMap(packMap), mParams.pw);
    }

    private static void dumpTask(Params mParams) {
        if (mParams.args.length < 4) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        ProcessInfoCollector.getInstance().dumpPackageTask(ProcessInfoCollector.getInstance().getProcessInfosFromTask(Integer.parseInt(mParams.args[2]), Integer.parseInt(mParams.args[3])), mParams.pw);
    }

    private static void dumpSMClean(Params mParams) {
        if (mParams.args.length < 5) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        List<String> pkgNames = getStrList(mParams);
        List<Integer> userIds = getIntList(mParams.args[3]);
        List<Integer> killTypes = getIntList(mParams.args[4]);
        if (pkgNames != null && userIds != null && killTypes != null) {
            List<AppCleanInfo> appCleanInfoList = new ArrayList();
            int size = pkgNames.size();
            for (int i = 0; i < size; i++) {
                appCleanInfoList.add(new AppCleanInfo((String) pkgNames.get(i), (Integer) userIds.get(i), (Integer) killTypes.get(i)));
            }
            MultiTaskManager.getInstance().executeMultiAppClean(appCleanInfoList, new Stub() {
                public void onCleanFinish(AppCleanParam result) {
                    String str = DumpAppMngClean.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DumpResult onCleanFinish:");
                    stringBuilder.append(result);
                    AwareLog.i(str, stringBuilder.toString());
                }
            });
        }
    }

    private static void getSMCleanList(Params mParams) {
        MultiTaskManager.getInstance().getAppListForUserClean(new Stub() {
            public void onCleanFinish(AppCleanParam result) {
                List<String> pkgList = result.getStringList();
                List<Integer> uidList = result.getIntList();
                List<Integer> killTypeList = result.getIntList2();
                String str = DumpAppMngClean.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SMQuery callback called, size = ");
                stringBuilder.append(pkgList.size());
                stringBuilder.append(", in Thread: ");
                stringBuilder.append(Thread.currentThread().getName());
                AwareLog.i(str, stringBuilder.toString());
                int i = 0;
                int len = pkgList.size();
                while (i < len) {
                    int uid = i < uidList.size() ? ((Integer) uidList.get(i)).intValue() : -1;
                    String str2 = DumpAppMngClean.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SMQuery pkg = ");
                    stringBuilder2.append((String) pkgList.get(i));
                    stringBuilder2.append(", uid: ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(", mCleanType = ");
                    stringBuilder2.append(CleanType.values()[((Integer) killTypeList.get(i)).intValue()]);
                    AwareLog.i(str2, stringBuilder2.toString());
                    i++;
                }
            }
        });
        mParams.pw.println("  request sended ! please grep AwareLog for result!");
    }

    private static void dumpPGClean(Params mParams) {
        if (mParams.args.length < 5) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        List<String> pkgName = getStrList(mParams);
        List<Integer> userId = getIntList(mParams.args[3]);
        int level = Integer.parseInt(mParams.args[4]);
        if (pkgName == null || userId == null) {
            mParams.pw.println("  bad param, str int must have same size and not null!");
            return;
        }
        MultiTaskManager.getInstance().requestAppCleanFromPG(pkgName, userId, level, "DumpSys");
        mParams.pw.println("  request sended ! please grep AwareLog for result!");
    }

    private static void dumpThermalClean(Params mParams) {
        if (mParams.args.length < 5) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        List<String> pkgName = getStrList(mParams);
        List<Integer> userId = getIntList(mParams.args[3]);
        int level = Integer.parseInt(mParams.args[4]);
        if (pkgName == null || userId == null) {
            mParams.pw.println("  bad param, str int must have same size and not null!");
            return;
        }
        MultiTaskManager.getInstance().requestAppClean(pkgName, userId, level, "DumpSys", 7);
        mParams.pw.println("  request sended ! please grep AwareLog for result!");
    }

    private static List<String> getStrList(Params mParams) {
        String strList = mParams.args[2];
        if (strList == null) {
            return null;
        }
        return Arrays.asList(strList.split(","));
    }

    private static List<Integer> getIntList(String intList) {
        if (intList == null) {
            return null;
        }
        String[] list = intList.split(",");
        ArrayList<Integer> result = new ArrayList();
        for (String parseInt : list) {
            result.add(Integer.valueOf(Integer.parseInt(parseInt)));
        }
        return result;
    }

    private static void dumpCrashClean(Params mParams) {
        if (5 != mParams.args.length) {
            mParams.pw.println("  please check your param number!");
            return;
        }
        String packageName = mParams.args[2];
        try {
            int userid = Integer.parseInt(mParams.args[3]);
            int level = Integer.parseInt(mParams.args[4]);
            if (AwareProcessInfo.getAwareProcInfosFromPackage(packageName, userid).isEmpty()) {
                mParams.pw.println("  the application is not alive!");
                return;
            }
            CrashClean crashClean = new CrashClean(userid, level, packageName, mParams.context);
            crashClean.clean();
            PrintWriter printWriter;
            StringBuilder stringBuilder;
            if (crashClean.getCleanCount() == 0) {
                printWriter = mParams.pw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  CrashClean can't clean package: ");
                stringBuilder.append(mParams.args[2]);
                printWriter.println(stringBuilder.toString());
            } else {
                printWriter = mParams.pw;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  CrashClean forcestop package: ");
                stringBuilder.append(mParams.args[2]);
                printWriter.println(stringBuilder.toString());
                PrintWriter printWriter2 = mParams.pw;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  killed ");
                stringBuilder2.append(crashClean.getCleanCount());
                stringBuilder2.append(" processes");
                printWriter2.println(stringBuilder2.toString());
            }
        } catch (NumberFormatException e) {
            mParams.pw.println("  please check your param!");
        }
    }

    private static void dumpAppStatus(Params mParams) {
        if (mParams.args.length < 4) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        ArrayList<AwareProcessInfo> fakeAwareProcList = AwareProcessInfo.getAwareProcInfosFromPackage(mParams.args[2], Integer.parseInt(mParams.args[3]));
        if (fakeAwareProcList.isEmpty()) {
            mParams.pw.println("  the application is not alive!");
            return;
        }
        Status[] allStatus = Status.values();
        mParams.pw.println("  matched status: ");
        for (int i = 1; i < allStatus.length; i++) {
            Iterator it = fakeAwareProcList.iterator();
            while (it.hasNext()) {
                if (AppStatusUtils.getInstance().checkAppStatus(allStatus[i], (AwareProcessInfo) it.next())) {
                    PrintWriter printWriter = mParams.pw;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("    ");
                    stringBuilder.append(allStatus[i].toString());
                    printWriter.println(stringBuilder.toString());
                    break;
                }
            }
        }
    }

    private static void clean(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            if (args == null || args.length < 4) {
                pw.println("  clean parameter error!");
                return;
            }
            pw.println("  AppMng clean process/package/task :");
            String cleanType = args[2];
            int currentUserId = AwareAppAssociate.getInstance().getCurUserId();
            int pid;
            StringBuilder stringBuilder;
            if ("removetask".equals(cleanType)) {
                AwareProcessInfo fakeAwareProc = (AwareProcessInfo) AwareProcessInfo.getAwareProcInfosFromTask(Integer.parseInt(args[3]), currentUserId).get(0);
                AwareProcessBlockInfo fakeAwareProcBlock = new AwareProcessBlockInfo(fakeAwareProc.mProcInfo.mUid);
                fakeAwareProcBlock.add(fakeAwareProc);
                ProcessCleaner.getInstance(mContext).removetask(fakeAwareProcBlock);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  remove task:");
                stringBuilder2.append(fakeAwareProc.mTaskId);
                pw.println(stringBuilder2.toString());
            } else if ("kill-allow-start".equals(cleanType)) {
                pid = Integer.parseInt(args[3]);
                ProcessCleaner.getInstance(mContext).killProcess(pid, true, "dump-kill-allow-start");
                stringBuilder = new StringBuilder();
                stringBuilder.append("  kill process:");
                stringBuilder.append(pid);
                stringBuilder.append(", allow restart");
                pw.println(stringBuilder.toString());
            } else if ("kill-forbid-start".equals(cleanType)) {
                pid = Integer.parseInt(args[3]);
                ProcessCleaner.getInstance(mContext).killProcess(pid, false, "dump-kill-forbid-start");
                stringBuilder = new StringBuilder();
                stringBuilder.append("  kill process:");
                stringBuilder.append(pid);
                stringBuilder.append(", forbid restart");
                pw.println(stringBuilder.toString());
            } else if ("force-stop".equals(cleanType)) {
                if (ProcessCleaner.getInstance(mContext).forcestopAppsAsUser((AwareProcessInfo) AwareProcessInfo.getAwareProcInfosFromPackage(args[3], currentUserId).get(0), "Dump")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  force-stop package:");
                    stringBuilder.append(args[3]);
                    pw.println(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  force-stop package:");
                    stringBuilder.append(args[3]);
                    stringBuilder.append(" failed!");
                    pw.println(stringBuilder.toString());
                }
            } else {
                pw.println("  bad clean type!");
            }
        }
    }

    private static void dumpDecide(Params mParams) {
        Params params = mParams;
        if (params.args.length < 7) {
            params.pw.println("  Bad command: need more args!");
            return;
        }
        String packageName = params.args[2];
        int userid = Integer.parseInt(params.args[3]);
        int level = Integer.parseInt(params.args[4]);
        int feature = Integer.parseInt(params.args[5]);
        int source = Integer.parseInt(params.args[6]);
        ArrayList<AwareProcessInfo> proclist = AwareProcessInfo.getAwareProcInfosFromPackage(packageName, userid);
        long start = System.nanoTime();
        EnumWithDesc config = null;
        int i;
        if (feature < 0) {
            i = userid;
        } else if (feature >= AppMngFeature.values().length) {
            String str = packageName;
            i = userid;
        } else {
            switch (AnonymousClass3.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.values()[feature].ordinal()]) {
                case 1:
                    config = AppCleanSource.values()[source];
                    break;
                case 2:
                    config = AppFreezeSource.values()[source];
                    break;
            }
            if (config != null) {
                List<AwareProcessBlockInfo> resultInfo = DecisionMaker.getInstance().decideAll(proclist, level, AppMngFeature.values()[feature], config);
                long end = System.nanoTime();
                PrintWriter printWriter = params.pw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  time consume = ");
                stringBuilder.append(end - start);
                printWriter.println(stringBuilder.toString());
                for (AwareProcessBlockInfo userid2 : resultInfo) {
                    printWriter = params.pw;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  pkg = ");
                    stringBuilder.append(userid2.mPackageName);
                    stringBuilder.append(", uid = ");
                    stringBuilder.append(userid2.mUid);
                    stringBuilder.append(", policy = ");
                    stringBuilder.append(userid2.mCleanType);
                    stringBuilder.append(", ");
                    stringBuilder.append(config.getDesc());
                    stringBuilder.append(", reason = ");
                    stringBuilder.append(userid2.mReason);
                    printWriter.println(stringBuilder.toString());
                }
            } else {
                i = userid;
                params.pw.println("  get the config name error");
            }
            return;
        }
        params.pw.println("  Bad command: invalid feature!");
    }

    private static void dumpHistory(Params mParams) {
        if (mParams.args.length < 3) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        DecisionMaker.getInstance().dumpHistory(mParams.pw, AppCleanSource.values()[Integer.parseInt(mParams.args[2])]);
    }

    private static void dumpBigData(Params mParams) {
        AppCleanupDumpRadar.getInstance().dumpBigData(mParams.pw);
    }

    private static void dumpAppType(Params mParams) {
        if (mParams.args.length < 3) {
            mParams.pw.println("  Bad command: need more args!");
            return;
        }
        int appType = AwareIntelligentRecg.getInstance().getAppMngSpecType(mParams.args[2]);
        PrintWriter printWriter = mParams.pw;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  App Type = ");
        stringBuilder.append(appType);
        printWriter.println(stringBuilder.toString());
    }

    private static void help(Params mParams) {
        mParams.pw.println("  PGClean [pkgName-1,...,pkgName-n] [userId-1,...,userId-n] [level]");
        mParams.pw.println("  dumpDecide [pkgName] [userId] [level] [feature] [source]");
        mParams.pw.println("  CheckStatus [pkgName] [userId]");
    }
}
