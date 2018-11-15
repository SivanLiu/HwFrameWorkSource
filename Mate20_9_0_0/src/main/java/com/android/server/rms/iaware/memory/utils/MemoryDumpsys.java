package com.android.server.rms.iaware.memory.utils;

import android.rms.iaware.AwareLog;
import com.android.server.am.HwActivityManagerService;
import com.android.server.rms.iaware.feature.MemoryFeature;
import com.android.server.rms.iaware.feature.RFeature;
import com.android.server.rms.memrepair.ProcStateData;
import com.android.server.rms.memrepair.ProcStateStatisData;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class MemoryDumpsys {
    private static final String TAG = "MemoryDumpsys";

    /* JADX WARNING: Missing block: B:22:0x003d, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static final boolean doDumpsys(RFeature feature, FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args == null || args.length <= 0 || feature == null || !(feature instanceof MemoryFeature) || !"--test-Memory".equals(args[0])) {
            return false;
        }
        if (args.length == 2) {
            if (!"getSample".equals(args[1])) {
                return false;
            }
            printPssListMap(ProcStateStatisData.getInstance().getPssListMap(), pw);
            return true;
        } else if (args.length == 4) {
            return dealArgFour(args);
        } else {
            return false;
        }
    }

    private static boolean dealArgFour(String[] args) {
        if ("trim".equals(args[1])) {
            return trimMemory(args[2], args[3]);
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0060  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean trimMemory(String proc, String levelArg) {
        boolean z;
        int hashCode = levelArg.hashCode();
        if (hashCode != -847101650) {
            if (hashCode != 163769603) {
                if (hashCode != 183181625) {
                    if (hashCode == 2130809258 && levelArg.equals("HIDDEN")) {
                        z = false;
                        switch (z) {
                            case false:
                                hashCode = 20;
                                break;
                            case true:
                                hashCode = 40;
                                break;
                            case true:
                                hashCode = 60;
                                break;
                            case true:
                                hashCode = 80;
                                break;
                            default:
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Error: Unknown level option: ");
                                stringBuilder.append(levelArg);
                                AwareLog.e(str, stringBuilder.toString());
                                return false;
                        }
                        return MemoryUtils.trimMemory(HwActivityManagerService.self(), proc, hashCode);
                    }
                } else if (levelArg.equals("COMPLETE")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return MemoryUtils.trimMemory(HwActivityManagerService.self(), proc, hashCode);
                }
            } else if (levelArg.equals("MODERATE")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return MemoryUtils.trimMemory(HwActivityManagerService.self(), proc, hashCode);
            }
        } else if (levelArg.equals("BACKGROUND")) {
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return MemoryUtils.trimMemory(HwActivityManagerService.self(), proc, hashCode);
        }
        z = true;
        switch (z) {
            case false:
                break;
            case true:
                break;
            case true:
                break;
            case true:
                break;
            default:
                break;
        }
        return MemoryUtils.trimMemory(HwActivityManagerService.self(), proc, hashCode);
    }

    private static void printPssListMap(Map<String, List<ProcStateData>> pssListMap, PrintWriter pw) {
        AwareLog.d(TAG, "enter printPssListMap...");
        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<ProcStateData>> entry : pssListMap.entrySet()) {
            for (ProcStateData procStateData : (List) entry.getValue()) {
                if (procStateData.getStatePssList() != null) {
                    sb.delete(0, sb.length());
                    sb.append("procName=");
                    sb.append(procStateData.getProcName());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("key=");
                    sb.append((String) entry.getKey());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("pssList=");
                    sb.append(procStateData.getStatePssList());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("procState=");
                    sb.append(procStateData.getState());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("mergeCount=");
                    sb.append(procStateData.getMergeCount());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("minPss=");
                    sb.append(procStateData.getMinPss());
                    sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    sb.append("maxPss=");
                    sb.append(procStateData.getMaxPss());
                    pw.println(sb.toString());
                }
            }
        }
    }
}
