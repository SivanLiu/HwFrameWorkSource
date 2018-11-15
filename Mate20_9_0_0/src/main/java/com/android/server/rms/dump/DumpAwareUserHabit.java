package com.android.server.rms.dump;

import android.os.SystemClock;
import android.rms.iaware.AppTypeRecoManager;
import com.android.server.rms.algorithm.AwareUserHabit;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class DumpAwareUserHabit {
    private static final int MINCOUNT = 3;
    private static final String TAG = "AwareUserHabit";
    private static final int TOPMAX = 10000;
    private static final int TOPN = 5;

    public static final void dumpAwareUserHabit(PrintWriter pw, String[] args) {
        PrintWriter printWriter = pw;
        String[] strArr = args;
        if (printWriter != null && strArr != null) {
            AwareUserHabit userHabit = AwareUserHabit.getInstance();
            if (userHabit == null) {
                printWriter.println("user habit is not ready");
            } else if (userHabit.isEnable()) {
                int length = strArr.length;
                boolean isGetAppType = false;
                boolean isGetAppListByType = false;
                boolean isGetPGProtectList = false;
                int isGetPGProtectList2 = 0;
                while (isGetPGProtectList2 < length) {
                    String arg = strArr[isGetPGProtectList2];
                    List<String> result;
                    if (isGetPGProtectList) {
                        length = 0;
                        try {
                            length = userHabit.getForceProtectApps(Integer.parseInt(arg));
                            if (length == 0 || length.size() <= 0) {
                                printWriter.println("invald input value or not used data");
                            } else {
                                printWriter.println(length.toString());
                            }
                            return;
                        } catch (NumberFormatException e) {
                            NumberFormatException numberFormatException = e;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad input value: ");
                            stringBuilder.append(arg);
                            printWriter.println(stringBuilder.toString());
                            return;
                        }
                    } else if ("userTrack".equals(arg)) {
                        Map<String, String> result2 = userHabit.getUserTrackAppSortDumpInfo();
                        if (result2 != null) {
                            printWriter.println(result2.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("habitProtectList".equals(arg)) {
                        userHabit.dumpHabitProtectList(printWriter);
                        return;
                    } else if ("getHabitProtectList".equals(arg)) {
                        result = userHabit.getHabitProtectList(10000, 10000);
                        if (result != null) {
                            printWriter.println(result.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("getHabitProtectListAll".equals(arg)) {
                        result = userHabit.getHabitProtectListAll(10000, 10000);
                        if (result != null) {
                            printWriter.println(result.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("getMostUsed".equals(arg)) {
                        result = userHabit.getMostFrequentUsedApp(5, 3);
                        if (result != null) {
                            printWriter.println(result.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("getTopN".equals(arg)) {
                        result = userHabit.getTopN(5);
                        if (result != null) {
                            printWriter.println(result.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("getAllTopList".equals(arg)) {
                        Map<String, Integer> result3 = userHabit.getAllTopList();
                        if (result3 != null) {
                            printWriter.println(result3.toString());
                        } else {
                            printWriter.println("result is null");
                        }
                        return;
                    } else if ("getLastPkgName".equals(arg)) {
                        printWriter.println(userHabit.getLastPkgName());
                        return;
                    } else {
                        if ("PGProtectList".equals(arg)) {
                            isGetPGProtectList = true;
                        } else if ("getLongTimeRunningApps".equals(arg)) {
                            result = userHabit.recognizeLongTimeRunningApps();
                            if (result != null) {
                                printWriter.println(result);
                            } else {
                                printWriter.println("all the apps are used recently");
                            }
                            return;
                        } else if ("getLruCache".equals(arg)) {
                            LinkedHashMap<String, Long> result4 = userHabit.getLruCache();
                            if (result4 != null) {
                                long now;
                                long now2 = SystemClock.elapsedRealtime();
                                StringBuffer s = new StringBuffer();
                                s.append("pkgName:      backgroundTime:\n");
                                for (Entry entry : result4.entrySet()) {
                                    now = now2;
                                    now2 = (now2 - ((Long) entry.getValue()).longValue()) / 1000;
                                    s.append((String) entry.getKey());
                                    LinkedHashMap<String, Long> result5 = result4;
                                    s.append("    ");
                                    s.append(String.valueOf(now2));
                                    s.append("s \n");
                                    now2 = now;
                                    result4 = result5;
                                }
                                now = now2;
                                printWriter.println(s.toString());
                            } else {
                                printWriter.println("result is null");
                            }
                            return;
                        } else if ("getClockTypeAppList".equals(arg)) {
                            printWriter.println(AppTypeRecoManager.getInstance().getAlarmApps());
                            return;
                        } else if ("getFilterApp".equals(arg)) {
                            Set<String> result6 = userHabit.getFilterApp();
                            if (result6 != null) {
                                printWriter.println(result6);
                            } else {
                                printWriter.println("result is null");
                            }
                            return;
                        } else if ("getGCMAppList".equals(arg)) {
                            result = userHabit.getGCMAppList();
                            if (result != null) {
                                printWriter.println(result);
                            } else {
                                printWriter.println("result is null");
                            }
                            return;
                        } else if ("getAppListByType".equals(arg)) {
                            isGetAppListByType = true;
                        } else {
                            if (isGetAppListByType) {
                                printgetAppListByType(printWriter, arg);
                            }
                            if ("getAppType".equals(arg)) {
                                isGetAppType = true;
                            } else if (isGetAppType) {
                                isGetPGProtectList2 = AppTypeRecoManager.getInstance().getAppType(arg);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("pkgname:");
                                stringBuilder2.append(arg);
                                stringBuilder2.append(" type:");
                                stringBuilder2.append(isGetPGProtectList2);
                                printWriter.println(stringBuilder2.toString());
                                return;
                            } else if ("getMostFreqAppByType".equals(arg)) {
                                printGetMostFreqAppByType(userHabit, printWriter, strArr);
                                return;
                            }
                        }
                        isGetPGProtectList2++;
                    }
                }
            } else {
                printWriter.println("user habit is not enable");
            }
        }
    }

    private static void printgetAppListByType(PrintWriter pw, String arg) {
        try {
            Set<String> result = AppTypeRecoManager.getInstance().getAppsByType(Integer.parseInt(arg));
            if (result.size() > 0) {
                pw.println(result.toString());
            } else {
                pw.println("invald input value or not used data");
            }
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad input value: ");
            stringBuilder.append(arg);
            pw.println(stringBuilder.toString());
        }
    }

    private static void printGetMostFreqAppByType(AwareUserHabit userHabit, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        int appType = 0;
        int appNum = -1;
        if (args.length > 2) {
            try {
                appType = Integer.parseInt(args[args.length - 2]);
                try {
                    appNum = Integer.parseInt(args[args.length - 1]);
                } catch (NumberFormatException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad input value param2: ");
                    stringBuilder.append(Arrays.toString(args));
                    pw.println(stringBuilder.toString());
                    return;
                }
            } catch (NumberFormatException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad input value param1: ");
                stringBuilder.append(Arrays.toString(args));
                pw.println(stringBuilder.toString());
                return;
            }
        }
        List<String> result = userHabit.getMostFreqAppByType(appType, appNum);
        if (result != null) {
            pw.println(result.toString());
        }
    }
}
