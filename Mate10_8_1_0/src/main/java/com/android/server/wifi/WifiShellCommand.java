package com.android.server.wifi;

import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ShellCommand;
import java.io.PrintWriter;

public class WifiShellCommand extends ShellCommand {
    private final IPackageManager mPM = AppGlobals.getPackageManager();
    private final WifiStateMachine mStateMachine;

    WifiShellCommand(WifiStateMachine stateMachine) {
        this.mStateMachine = stateMachine;
    }

    public int onCommand(String cmd) {
        checkRootPermission();
        PrintWriter pw = getOutPrintWriter();
        String str = cmd != null ? cmd : "";
        try {
            if (str.equals("set-ipreach-disconnect")) {
                boolean enabled;
                String nextArg = getNextArgRequired();
                if ("enabled".equals(nextArg)) {
                    enabled = true;
                } else if ("disabled".equals(nextArg)) {
                    enabled = false;
                } else {
                    pw.println("Invalid argument to 'set-ipreach-disconnect' - must be 'enabled' or 'disabled'");
                    return -1;
                }
                this.mStateMachine.setIpReachabilityDisconnectEnabled(enabled);
                return 0;
            } else if (str.equals("get-ipreach-disconnect")) {
                pw.println("IPREACH_DISCONNECT state is " + this.mStateMachine.getIpReachabilityDisconnectEnabled());
                return 0;
            } else if (str.equals("set-poll-rssi-interval-msecs")) {
                try {
                    int newPollIntervalMsecs = Integer.parseInt(getNextArgRequired());
                    if (newPollIntervalMsecs < 1) {
                        pw.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                        return -1;
                    }
                    this.mStateMachine.setPollRssiIntervalMsecs(newPollIntervalMsecs);
                    return 0;
                } catch (NumberFormatException e) {
                    pw.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                    return -1;
                }
            } else if (!str.equals("get-poll-rssi-interval-msecs")) {
                return handleDefaultCommands(cmd);
            } else {
                pw.println("WifiStateMachine.mPollRssiIntervalMsecs = " + this.mStateMachine.getPollRssiIntervalMsecs());
                return 0;
            }
        } catch (Exception e2) {
            pw.println("Exception: " + e2);
            return -1;
        }
    }

    private void checkRootPermission() {
        int uid = Binder.getCallingUid();
        if (uid != 0) {
            throw new SecurityException("Uid " + uid + " does not have access to wifi commands");
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Wi-Fi (wifi) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-ipreach-disconnect enabled|disabled");
        pw.println("    Sets whether CMD_IP_REACHABILITY_LOST events should trigger disconnects.");
        pw.println("  get-ipreach-disconnect");
        pw.println("    Gets setting of CMD_IP_REACHABILITY_LOST events triggering disconnects.");
        pw.println("  set-poll-rssi-interval-msecs <int>");
        pw.println("    Sets the interval between RSSI polls to <int> milliseconds.");
        pw.println("  get-poll-rssi-interval-msecs");
        pw.println("    Gets current interval between RSSI polls, in milliseconds.");
        pw.println();
    }
}
