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
                    consumers.put("delay", -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag.$INST$0);
                    consumers.put("bigData", -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag.$INST$1);
                    consumers.put("debugLog", -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag.$INST$2);
                    consumers.put("debug", -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag.$INST$3);
                    consumers.put("param", -$Lambda$4teM88sZW-FwSj5FV1nxgG8dyag.$INST$4);
                }
                Consumer<Params> func = (Consumer) consumers.get(cmd);
                if (func == null) {
                    pw.println("  Bad command: " + cmd);
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
            pw.println("  delay alarm set user:" + userId + " pkg:" + pkg + " tag:" + tag + " delay:" + delay);
        }
    }

    private static void dumpBigData(Context mContext, PrintWriter pw, String[] args) {
        if (pw != null) {
            pw.println(AlarmManagerDumpRadar.getInstance().saveBigData(false));
        }
    }
}
