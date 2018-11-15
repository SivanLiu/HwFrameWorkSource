package com.android.server.rms.dump;

import android.content.Context;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.rms.iaware.appmng.AlarmManagerDumpRadar;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DumpAlarmManager {
    public static final String TAG = "DumpAlarmManager";
    private static volatile Map<String, Consumer<Params>> consumers = new HashMap();

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
            synchronized (DumpAlarmManager.class) {
                if (consumers.isEmpty()) {
                    consumers.put("delay", -$$Lambda$DumpAlarmManager$sTyRBtkgkC1zrLkNBktCJFPBVgI.INSTANCE);
                    consumers.put("bigData", -$$Lambda$DumpAlarmManager$6WCcZOOF0NyNxY14ThrN_Sa9_K8.INSTANCE);
                    consumers.put("debugLog", -$$Lambda$DumpAlarmManager$yU5hWyGyhPsIICiAzmBOdnA7mnA.INSTANCE);
                    consumers.put("debug", -$$Lambda$DumpAlarmManager$TAuUJte3WTeURAu60wUGpaASTRk.INSTANCE);
                    consumers.put("param", -$$Lambda$DumpAlarmManager$cgr-s9dHL-55EiLtqSDRMXo9OSo.INSTANCE);
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

    private static void setDebugSwitch(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            if (PreciseIgnore.COMP_SCREEN_ON_VALUE_.equals(args[2])) {
                AwareWakeUpManager.getInstance().setDebugSwitch(true);
                pw.println("  debug on");
            } else {
                AwareWakeUpManager.getInstance().setDebugSwitch(false);
                pw.println("  debug off");
            }
        }
    }

    private static void dumpDebugLog(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            AwareWakeUpManager.getInstance().dumpDebugLog(pw);
        }
    }

    private static void delay(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            if (args == null || args.length < 6) {
                pw.println("  delay parameter error!");
                return;
            }
            int userId = Integer.parseInt(args[2]);
            String pkg = args[3];
            String tag = args[4];
            long delay = Long.parseLong(args[5]);
            AwareWakeUpManager.getInstance().setDebugParam(userId, pkg, tag, delay);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  delay alarm set user:");
            stringBuilder.append(userId);
            stringBuilder.append(" pkg:");
            stringBuilder.append(pkg);
            stringBuilder.append(" tag:");
            stringBuilder.append(tag);
            stringBuilder.append(" delay:");
            stringBuilder.append(delay);
            pw.println(stringBuilder.toString());
        }
    }

    private static void dumpBigData(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            pw.println(AlarmManagerDumpRadar.getInstance().saveBigData(false));
        }
    }
}
