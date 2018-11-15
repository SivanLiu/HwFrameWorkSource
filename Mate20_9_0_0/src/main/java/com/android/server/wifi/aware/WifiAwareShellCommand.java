package com.android.server.wifi.aware;

import android.os.Binder;
import android.os.ShellCommand;
import android.text.TextUtils;
import android.util.Log;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WifiAwareShellCommand extends ShellCommand {
    private static final String TAG = "WifiAwareShellCommand";
    private Map<String, DelegatedShellCommand> mDelegatedCommands = new HashMap();

    public interface DelegatedShellCommand {
        int onCommand(ShellCommand shellCommand);

        void onHelp(String str, ShellCommand shellCommand);

        void onReset();
    }

    public void register(String command, DelegatedShellCommand shellCommand) {
        if (this.mDelegatedCommands.containsKey(command)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("register: overwriting existing command -- '");
            stringBuilder.append(command);
            stringBuilder.append("'");
            Log.e(str, stringBuilder.toString());
        }
        this.mDelegatedCommands.put(command, shellCommand);
    }

    public int onCommand(String cmd) {
        checkRootPermission();
        PrintWriter pw = getErrPrintWriter();
        try {
            if ("reset".equals(cmd)) {
                for (DelegatedShellCommand dsc : this.mDelegatedCommands.values()) {
                    dsc.onReset();
                }
                return 0;
            }
            DelegatedShellCommand delegatedCmd = null;
            if (!TextUtils.isEmpty(cmd)) {
                delegatedCmd = (DelegatedShellCommand) this.mDelegatedCommands.get(cmd);
            }
            if (delegatedCmd != null) {
                return delegatedCmd.onCommand(this);
            }
            return handleDefaultCommands(cmd);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        }
    }

    private void checkRootPermission() {
        int uid = Binder.getCallingUid();
        if (uid != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Uid ");
            stringBuilder.append(uid);
            stringBuilder.append(" does not have access to wifiaware commands");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Wi-Fi Aware (wifiaware) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  reset");
        pw.println("    Reset parameters to default values.");
        for (Entry<String, DelegatedShellCommand> sce : this.mDelegatedCommands.entrySet()) {
            ((DelegatedShellCommand) sce.getValue()).onHelp((String) sce.getKey(), this);
        }
        pw.println();
    }
}
