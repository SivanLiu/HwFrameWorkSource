package com.android.server.rms.dump;

import android.content.Context;
import android.rms.utils.Utils;
import android.util.ArraySet;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import java.io.PrintWriter;

public final class DumpAppAssociate {
    public static final String TAG = "DumpAppAssociate";

    private static final boolean dumpIntelligentRecg(PrintWriter pw, Context context, String[] args) {
        if (!Utils.scanArgs(args, "intlrecg")) {
            return false;
        }
        DumpIntelligentRecg.dumpIntelligentRecg(pw, context, args);
        return true;
    }

    private static final boolean dumpBaseDump(AwareAppAssociate assoc, PrintWriter pw, String arg) {
        if ("enable".equals(arg)) {
            AwareAppAssociate.enable();
            return true;
        } else if ("disable".equals(arg)) {
            AwareAppAssociate.disable();
            return true;
        } else if (arg.equals("enable_log")) {
            AwareAppAssociate.enableDebug();
            return true;
        } else if (arg.equals("disable_log")) {
            AwareAppAssociate.disableDebug();
            return true;
        } else if (arg.equals("enable_record")) {
            AwareAppAssociate.enableRecord();
            return true;
        } else if (arg.equals("disable_record")) {
            AwareAppAssociate.disableRecord();
            return true;
        } else if (arg.equals("-home")) {
            assoc.dumpHome(pw);
            return true;
        } else if (arg.equals("-prev")) {
            assoc.dumpPrev(pw);
            return true;
        } else if (arg.equals("-w")) {
            assoc.dumpWidget(pw);
            return true;
        } else if (arg.equals("-vw")) {
            assoc.dumpVisibleWindow(pw);
            return true;
        } else if (!arg.equals("-pkgProc")) {
            return false;
        } else {
            assoc.dumpPkgProc(pw);
            return true;
        }
    }

    private static final void dumpAssocDump(boolean acquire, AwareAppAssociate assoc, String pid, PrintWriter pw) {
        if (pid == null) {
            assoc.dump(pw);
        } else {
            StringBuilder stringBuilder;
            try {
                int tmp = Integer.parseInt(pid);
                if (acquire) {
                    ArraySet<Integer> strong = new ArraySet();
                    assoc.getAssocListForPid(tmp, strong);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("strong:");
                    stringBuilder.append(strong);
                    stringBuilder.append("\n");
                    pw.print(stringBuilder.toString());
                } else {
                    assoc.dumpPid(tmp, pw);
                }
            } catch (NumberFormatException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad pid:");
                stringBuilder.append(pid);
                pw.println(stringBuilder.toString());
            }
        }
    }

    public static final void dumpAppAssociate(PrintWriter pw, Context context, String[] args) {
        String pid = null;
        boolean hasPid = false;
        boolean record = false;
        boolean acquire = false;
        AwareAppAssociate assoc = AwareAppAssociate.getInstance();
        if (assoc == null) {
            pw.print("Can not getInstance of AwareAppAssociate");
        } else if (!dumpIntelligentRecg(pw, context, args)) {
            if (args != null) {
                for (String arg : args) {
                    if (arg != null) {
                        if (!dumpBaseDump(assoc, pw, arg)) {
                            if (arg.equals("-p")) {
                                hasPid = true;
                            } else if (arg.equals("-r")) {
                                record = true;
                            } else if (arg.equals("-f")) {
                                assoc.dumpFore(pw);
                                return;
                            } else if (arg.equals("-fr")) {
                                assoc.dumpRecentFore(pw);
                                return;
                            } else if (arg.equals("-a")) {
                                acquire = true;
                            } else if (hasPid) {
                                pid = arg;
                                break;
                            }
                        } else {
                            return;
                        }
                    }
                }
            }
            if (record) {
                assoc.dumpRecord(pw);
            } else {
                dumpAssocDump(acquire, assoc, pid, pw);
            }
        }
    }
}
