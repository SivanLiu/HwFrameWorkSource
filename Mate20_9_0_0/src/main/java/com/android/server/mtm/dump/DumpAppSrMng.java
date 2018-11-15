package com.android.server.mtm.dump;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.Context;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public final class DumpAppSrMng {
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
            if (args == null || args.length < 3 || args[1] == null) {
                pw.println("  Bad command");
                return;
            }
            String cmd = args[1];
            synchronized (DumpAppSrMng.class) {
                if (consumers.size() == 0) {
                    consumers.put("dump_rules", -$$Lambda$DumpAppSrMng$aN0pZowdDz5s63BwOlmEY-Uv0nk.INSTANCE);
                    consumers.put("update_rules", -$$Lambda$DumpAppSrMng$Blb-4nEqJC_w1l5bbR--afVpCUY.INSTANCE);
                    consumers.put("appstart", -$$Lambda$DumpAppSrMng$3ZWRHWAat-8e_OjdGJjdp9q2kDg.INSTANCE);
                    consumers.put("dump_list", -$$Lambda$DumpAppSrMng$mjPNBXsi4wWHsGdisQYOFUX8Eko.INSTANCE);
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
                } catch (ArrayIndexOutOfBoundsException e) {
                    pw.println("  Bad command:");
                    pw.println(e.toString());
                }
            }
        } else {
            return;
        }
    }

    private static void dumpDumpRules(Params mParams) {
        if ("clean".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dump(mParams.pw, AppMngFeature.APP_CLEAN);
        } else if ("start".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dump(mParams.pw, AppMngFeature.APP_START);
        } else if ("freeze".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dump(mParams.pw, AppMngFeature.APP_FREEZE);
        } else if ("iolimit".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dump(mParams.pw, AppMngFeature.APP_IOLIMIT);
        } else if ("broadcast".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dump(mParams.pw, AppMngFeature.BROADCAST);
        } else if ("rawcfg".equals(mParams.args[2])) {
            if (mParams.args.length < 5) {
                mParams.pw.println("  Bad command: need more args!");
                return;
            }
            ArrayList<String> result = DecisionMaker.getInstance().getRawConfig(mParams.args[3], mParams.args[4]);
            if (result == null) {
                mParams.pw.println("raw config of your type is null");
                return;
            }
            Iterator it = result.iterator();
            while (it.hasNext()) {
                mParams.pw.println((String) it.next());
            }
        }
    }

    private static void dumpUpdateRules(Params mParams) {
        long start;
        long end;
        PrintWriter printWriter;
        StringBuilder stringBuilder;
        if ("clean".equals(mParams.args[2])) {
            start = System.nanoTime();
            DecisionMaker.getInstance().updateRule(AppMngFeature.APP_CLEAN, mParams.context);
            end = System.nanoTime();
            printWriter = mParams.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append("time consume update appclean = ");
            stringBuilder.append(end - start);
            printWriter.println(stringBuilder.toString());
        } else if ("start".equals(mParams.args[2])) {
            start = System.nanoTime();
            DecisionMaker.getInstance().updateRule(AppMngFeature.APP_START, mParams.context);
            end = System.nanoTime();
            printWriter = mParams.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append("time consume update appconfig = ");
            stringBuilder.append(end - start);
            printWriter.println(stringBuilder.toString());
        } else if ("freeze".equals(mParams.args[2])) {
            start = System.nanoTime();
            DecisionMaker.getInstance().updateRule(AppMngFeature.APP_FREEZE, mParams.context);
            end = System.nanoTime();
            printWriter = mParams.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append("time consume update appfree = ");
            stringBuilder.append(end - start);
            printWriter.println(stringBuilder.toString());
        } else if ("iolimit".equals(mParams.args[2])) {
            start = System.nanoTime();
            DecisionMaker.getInstance().updateRule(AppMngFeature.APP_IOLIMIT, mParams.context);
            end = System.nanoTime();
            printWriter = mParams.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append("time consume update appIoLimit = ");
            stringBuilder.append(end - start);
            printWriter.println(stringBuilder.toString());
        } else if ("broadcast".equals(mParams.args[2])) {
            start = System.nanoTime();
            DecisionMaker.getInstance().updateRule(AppMngFeature.BROADCAST, mParams.context);
            end = System.nanoTime();
            printWriter = mParams.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append("time consume update broadcast = ");
            stringBuilder.append(end - start);
            printWriter.println(stringBuilder.toString());
        } else if ("cloudupdate".equals(mParams.args[2])) {
            start = System.nanoTime();
            AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
            if (policy != null) {
                policy.updateCloudPolicy("");
            }
            long end2 = System.nanoTime();
            PrintWriter printWriter2 = mParams.pw;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("time consume update appconfig = ");
            stringBuilder2.append(end2 - start);
            printWriter2.println(stringBuilder2.toString());
        }
    }

    private static void dumpAppStart(Params mParams) {
        AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
        if (policy != null) {
            policy.dump(mParams.pw, mParams.args);
        }
    }

    private static void dumpList(Params mParams) {
        if ("clean".equals(mParams.args[2])) {
            DecisionMaker.getInstance().dumpList(mParams.pw, AppMngFeature.APP_CLEAN);
        }
    }
}
